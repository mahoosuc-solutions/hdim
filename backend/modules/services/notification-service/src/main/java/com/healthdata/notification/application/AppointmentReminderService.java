package com.healthdata.notification.application;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.featureflags.TenantFeatureFlagService;
import com.healthdata.notification.domain.model.AppointmentReminderSent;
import com.healthdata.notification.domain.repository.AppointmentReminderSentRepository;
import com.healthdata.notification.infrastructure.client.FhirServiceClient;
import com.healthdata.notification.infrastructure.client.PatientServiceClient;
import com.healthdata.notification.infrastructure.providers.SmsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Appointment Reminder Service
 *
 * Sends SMS reminders for upcoming appointments using Twilio.
 *
 * Features:
 * - Configurable reminder intervals (1, 3, 7 days before appointment)
 * - Idempotency: Tracks reminders sent to prevent duplicates
 * - Feature flag integration: Only sends if twilio-sms-reminders enabled
 * - SMS opt-in check: Only sends to patients who opted in
 * - Template-based messages with patient/provider/time variables
 * - Audit logging for HIPAA compliance
 *
 * HIPAA Compliance:
 * - Multi-tenant isolation enforced
 * - Audit logging via @Audited annotation
 * - PHI minimization: Only logs masked phone numbers
 * - Patient consent required (smsOptIn check)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderService {

    private final TenantFeatureFlagService featureFlagService;
    private final FhirServiceClient fhirServiceClient;
    private final PatientServiceClient patientServiceClient;
    private final SmsProvider smsProvider;
    private final AppointmentReminderSentRepository reminderSentRepository;

    private static final String FEATURE_KEY = "twilio-sms-reminders";
    private static final String REMINDER_TYPE = "APPOINTMENT_REMINDER";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    /**
     * Process appointment reminders for a tenant
     *
     * Called by scheduled job. Queries upcoming appointments and sends reminders
     * based on tenant configuration.
     *
     * @param tenantId   Tenant ID (HIPAA §164.312(d))
     * @param daysBefore Days before appointment to send reminder (1, 3, or 7)
     */
    @Transactional
    @Audited(
        action = AuditAction.EXECUTE,
        resourceType = "Appointment",
        description = "Process appointment reminder batch"
    )
    public void processReminders(String tenantId, int daysBefore) {
        log.info("Processing appointment reminders for tenant {} ({} days before)", tenantId, daysBefore);

        // Check if feature enabled for tenant
        if (!featureFlagService.isFeatureEnabled(tenantId, FEATURE_KEY)) {
            log.info("Twilio SMS reminders disabled for tenant {}, skipping", tenantId);
            return;
        }

        // Get tenant configuration
        Map<String, Object> config = featureFlagService.getFeatureConfig(tenantId, FEATURE_KEY);
        List<Integer> reminderDays = getReminderDays(config);

        if (!reminderDays.contains(daysBefore)) {
            log.debug("Tenant {} does not have {}-day reminders configured, skipping", tenantId, daysBefore);
            return;
        }

        // Calculate time window for appointments
        LocalDateTime targetDate = LocalDateTime.now().plusDays(daysBefore);
        LocalDateTime startTime = targetDate.toLocalDate().atStartOfDay();
        LocalDateTime endTime = targetDate.toLocalDate().atTime(23, 59, 59);

        // Query appointments
        List<FhirServiceClient.AppointmentDto> appointments;
        try {
            appointments = fhirServiceClient.getAppointments(tenantId, startTime, endTime, "booked");
            log.info("Found {} appointments for tenant {} on {}",
                    appointments.size(), tenantId, targetDate.toLocalDate());
        } catch (Exception e) {
            log.error("Failed to query appointments for tenant {}: {}", tenantId, e.getMessage(), e);
            return;
        }

        // Process each appointment
        int sentCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        for (FhirServiceClient.AppointmentDto appointment : appointments) {
            try {
                boolean sent = sendReminder(tenantId, appointment, daysBefore);
                if (sent) {
                    sentCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to send reminder for appointment {} in tenant {}: {}",
                        appointment.getId(), tenantId, e.getMessage(), e);
                errorCount++;
            }
        }

        log.info("Appointment reminder batch complete for tenant {}: sent={}, skipped={}, errors={}",
                tenantId, sentCount, skippedCount, errorCount);
    }

    /**
     * Send reminder for a single appointment
     *
     * @param tenantId    Tenant ID
     * @param appointment Appointment details
     * @param daysBefore  Days before appointment
     * @return true if sent, false if skipped
     */
    @Audited(
        action = AuditAction.CREATE,
        resourceType = "Notification",
        description = "Send appointment reminder SMS"
    )
    private boolean sendReminder(String tenantId, FhirServiceClient.AppointmentDto appointment, int daysBefore) {
        UUID appointmentId = appointment.getId();
        UUID patientId = appointment.getPatientId();

        // Check if reminder already sent (idempotency)
        if (reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                tenantId, appointmentId, REMINDER_TYPE, daysBefore).isPresent()) {
            log.debug("Reminder already sent for appointment {} ({} days), skipping", appointmentId, daysBefore);
            return false;
        }

        // Get patient contact information
        PatientServiceClient.PatientContactDto patient;
        try {
            patient = patientServiceClient.getPatientContact(tenantId, patientId);
        } catch (Exception e) {
            log.error("Failed to get patient contact info for patient {} in tenant {}: {}",
                    patientId, tenantId, e.getMessage());
            recordFailedReminder(tenantId, appointment, daysBefore, null,
                    "Failed to get patient contact: " + e.getMessage());
            return false;
        }

        // Check SMS opt-in consent
        if (patient.getSmsOptIn() == null || !patient.getSmsOptIn()) {
            log.debug("Patient {} has not opted in to SMS, skipping reminder", patientId);
            return false;
        }

        // Validate phone number
        if (patient.getPhoneNumber() == null || patient.getPhoneNumber().isBlank()) {
            log.warn("Patient {} has no phone number, skipping reminder", patientId);
            return false;
        }

        // Build SMS message
        String message = buildReminderMessage(patient, appointment);

        // Send SMS
        String messageSid;
        try {
            messageSid = smsProvider.send(patient.getPhoneNumber(), message);
            log.info("Sent appointment reminder SMS for appointment {} to patient {} in tenant {} (SID: {})",
                    appointmentId, patientId, tenantId, messageSid);
        } catch (Exception e) {
            log.error("Failed to send SMS reminder for appointment {} to patient {}: {}",
                    appointmentId, patientId, e.getMessage());
            recordFailedReminder(tenantId, appointment, daysBefore, patient.getPhoneNumber(),
                    "SMS send failed: " + e.getMessage());
            return false;
        }

        // Record successful send
        recordSuccessfulReminder(tenantId, appointment, daysBefore, patient.getPhoneNumber(), messageSid);

        return true;
    }

    /**
     * Build SMS reminder message from template
     */
    private String buildReminderMessage(
            PatientServiceClient.PatientContactDto patient,
            FhirServiceClient.AppointmentDto appointment) {

        String patientName = patient.getFirstName() + " " + patient.getLastName();
        String providerName = appointment.getPractitionerId() != null
                ? "Dr. " + appointment.getPractitionerId() // TODO: Resolve practitioner name
                : "your provider";
        String appointmentDate = appointment.getStartTime().format(DATE_FORMATTER);
        String appointmentTime = appointment.getStartTime().format(TIME_FORMATTER);
        String location = appointment.getLocationId() != null
                ? appointment.getLocationId() // TODO: Resolve location name
                : "our office";

        // Template: Appointment Reminder: ${patientName}, you have an appointment with ${providerName}
        // on ${appointmentDate} at ${appointmentTime} at ${location}. Reply STOP to opt out.
        return String.format("Appointment Reminder: %s, you have an appointment with %s on %s at %s at %s. Reply STOP to opt out.",
                patientName, providerName, appointmentDate, appointmentTime, location);
    }

    /**
     * Record successful reminder send
     */
    private void recordSuccessfulReminder(
            String tenantId,
            FhirServiceClient.AppointmentDto appointment,
            int daysBefore,
            String phoneNumber,
            String messageSid) {

        AppointmentReminderSent record = AppointmentReminderSent.builder()
                .tenantId(tenantId)
                .appointmentId(appointment.getId())
                .patientId(appointment.getPatientId())
                .phoneNumber(phoneNumber)
                .reminderType(REMINDER_TYPE)
                .daysBefore(daysBefore)
                .channel("SMS")
                .status("SENT")
                .messageSid(messageSid)
                .build();

        reminderSentRepository.save(record);
    }

    /**
     * Record failed reminder send
     */
    private void recordFailedReminder(
            String tenantId,
            FhirServiceClient.AppointmentDto appointment,
            int daysBefore,
            String phoneNumber,
            String errorMessage) {

        AppointmentReminderSent record = AppointmentReminderSent.builder()
                .tenantId(tenantId)
                .appointmentId(appointment.getId())
                .patientId(appointment.getPatientId())
                .phoneNumber(phoneNumber != null ? phoneNumber : "UNKNOWN")
                .reminderType(REMINDER_TYPE)
                .daysBefore(daysBefore)
                .channel("SMS")
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();

        reminderSentRepository.save(record);
    }

    /**
     * Extract reminder days from tenant configuration
     *
     * Default: [1] (1 day before only)
     * Can be configured to [1, 3, 7] for multiple reminders
     */
    @SuppressWarnings("unchecked")
    private List<Integer> getReminderDays(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return List.of(1); // Default: 1 day before only
        }

        Object reminderDays = config.get("reminder_days");
        if (reminderDays instanceof List) {
            return (List<Integer>) reminderDays;
        }

        return List.of(1);
    }
}
