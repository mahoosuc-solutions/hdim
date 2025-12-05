package com.healthdata.quality.dto.notification;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic Notification Request
 *
 * Flexible implementation for various notification types:
 * - APPOINTMENT_REMINDER: Upcoming appointment notifications
 * - MEDICATION_REMINDER: Medication adherence reminders
 * - LAB_RESULT: New lab results available
 * - DAILY_DIGEST: Summary of care gaps, alerts, and tasks
 *
 * Use the builder pattern to construct notifications with required fields:
 * <pre>
 * GenericNotificationRequest.builder()
 *     .notificationType("APPOINTMENT_REMINDER")
 *     .templateId("appointment-reminder")
 *     .tenantId("tenant123")
 *     .patientId("patient456")
 *     .title("Appointment Reminder: Dr. Smith - Tomorrow at 2:00 PM")
 *     .message("You have an appointment with Dr. Smith tomorrow at 2:00 PM")
 *     .templateVariable("appointmentDate", "2025-01-15")
 *     .templateVariable("appointmentTime", "14:00")
 *     .templateVariable("providerName", "Dr. Smith")
 *     .recipient("EMAIL", "patient@example.com")
 *     .sendEmail(true)
 *     .sendSms(true)
 *     .build();
 * </pre>
 */
@Builder
@Getter
public class GenericNotificationRequest implements NotificationRequest {

    private final String notificationType;
    private final String templateId;
    private final String tenantId;
    private final String patientId;
    private final String title;
    private final String message;
    private final String severity;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    @Singular("recipient")
    private final Map<String, String> recipients;

    @Singular("templateVariable")
    private final Map<String, Object> templateVariables;

    @Singular("metadataEntry")
    private final Map<String, Object> metadata;

    private final Boolean sendEmail;
    private final Boolean sendSms;
    private final Boolean sendWebSocket;

    private final String notificationId;
    private final String relatedEntityId;

    @Override
    public Map<String, Object> getTemplateVariables() {
        // Add standard variables if not present
        Map<String, Object> variables = new HashMap<>(templateVariables);

        if (!variables.containsKey("patientId") && patientId != null) {
            variables.put("patientId", patientId);
        }

        if (!variables.containsKey("patientName") && patientId != null) {
            variables.put("patientName", "Patient " + patientId); // TODO: Fetch from FHIR
        }

        if (!variables.containsKey("title")) {
            variables.put("title", title);
        }

        if (!variables.containsKey("message")) {
            variables.put("message", message);
        }

        if (!variables.containsKey("severity") && severity != null) {
            variables.put("severity", severity);
        }

        if (!variables.containsKey("timestamp")) {
            variables.put("timestamp", timestamp.toString());
        }

        if (!variables.containsKey("facilityName")) {
            variables.put("facilityName", "HealthData Clinical System");
        }

        if (!variables.containsKey("actionUrl") && patientId != null) {
            variables.put("actionUrl", "https://healthdata-in-motion.com/patients/" + patientId);
        }

        return variables;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata != null ? metadata : new HashMap<>();
    }

    @Override
    public boolean shouldSendEmail() {
        return sendEmail != null && sendEmail;
    }

    @Override
    public boolean shouldSendSms() {
        return sendSms != null && sendSms;
    }

    @Override
    public boolean shouldSendWebSocket() {
        return sendWebSocket != null ? sendWebSocket : false;
    }

    /**
     * Factory method for appointment reminders
     */
    public static GenericNotificationRequest appointmentReminder(
            String tenantId,
            String patientId,
            String appointmentId,
            String providerName,
            String appointmentDate,
            String appointmentTime,
            String location,
            String recipientEmail,
            String recipientPhone) {

        return GenericNotificationRequest.builder()
                .notificationType("APPOINTMENT_REMINDER")
                .templateId("appointment-reminder")
                .tenantId(tenantId)
                .patientId(patientId)
                .title(String.format("Appointment Reminder: %s - %s at %s", providerName, appointmentDate, appointmentTime))
                .message(String.format("You have an appointment with %s on %s at %s. Location: %s",
                        providerName, appointmentDate, appointmentTime, location))
                .severity("MEDIUM")
                .templateVariable("providerName", providerName)
                .templateVariable("appointmentDate", appointmentDate)
                .templateVariable("appointmentTime", appointmentTime)
                .templateVariable("location", location)
                .templateVariable("appointmentId", appointmentId)
                .recipient("EMAIL", recipientEmail)
                .recipient("SMS", recipientPhone)
                .metadataEntry("appointmentId", appointmentId)
                .sendEmail(true)
                .sendSms(true)
                .sendWebSocket(false)
                .notificationId(appointmentId)
                .relatedEntityId(appointmentId)
                .build();
    }

    /**
     * Factory method for medication reminders
     */
    public static GenericNotificationRequest medicationReminder(
            String tenantId,
            String patientId,
            String medicationName,
            String dosage,
            String frequency,
            String nextDoseTime,
            String recipientEmail,
            String recipientPhone) {

        return GenericNotificationRequest.builder()
                .notificationType("MEDICATION_REMINDER")
                .templateId("medication-reminder")
                .tenantId(tenantId)
                .patientId(patientId)
                .title(String.format("Medication Reminder: %s %s", medicationName, dosage))
                .message(String.format("Time to take your medication: %s %s. Next dose: %s",
                        medicationName, dosage, nextDoseTime))
                .severity("HIGH")
                .templateVariable("medicationName", medicationName)
                .templateVariable("dosage", dosage)
                .templateVariable("frequency", frequency)
                .templateVariable("nextDoseTime", nextDoseTime)
                .recipient("EMAIL", recipientEmail)
                .recipient("SMS", recipientPhone)
                .metadataEntry("medicationName", medicationName)
                .metadataEntry("dosage", dosage)
                .sendEmail(true)
                .sendSms(true)
                .sendWebSocket(false)
                .build();
    }

    /**
     * Factory method for lab result notifications
     */
    public static GenericNotificationRequest labResult(
            String tenantId,
            String patientId,
            String labResultId,
            String testName,
            String resultDate,
            String providerName,
            String recipientEmail) {

        return GenericNotificationRequest.builder()
                .notificationType("LAB_RESULT")
                .templateId("lab-result")
                .tenantId(tenantId)
                .patientId(patientId)
                .title(String.format("New Lab Results Available: %s", testName))
                .message(String.format("Your lab results for %s (dated %s) are now available. " +
                        "Please review with %s.", testName, resultDate, providerName))
                .severity("MEDIUM")
                .templateVariable("testName", testName)
                .templateVariable("resultDate", resultDate)
                .templateVariable("providerName", providerName)
                .templateVariable("labResultId", labResultId)
                .recipient("EMAIL", recipientEmail)
                .metadataEntry("labResultId", labResultId)
                .metadataEntry("testName", testName)
                .sendEmail(true)
                .sendSms(false)
                .sendWebSocket(true)
                .notificationId(labResultId)
                .relatedEntityId(labResultId)
                .build();
    }

    /**
     * Factory method for daily digest notifications
     */
    public static GenericNotificationRequest dailyDigest(
            String tenantId,
            String userId,
            int careGapCount,
            int alertCount,
            int taskCount,
            String recipientEmail) {

        return GenericNotificationRequest.builder()
                .notificationType("DAILY_DIGEST")
                .templateId("digest")
                .tenantId(tenantId)
                .patientId(null) // Not patient-specific
                .title(String.format("Daily Summary: %d Care Gaps, %d Alerts, %d Tasks",
                        careGapCount, alertCount, taskCount))
                .message(String.format("Your daily summary: %d open care gaps, %d active alerts, %d pending tasks. " +
                        "Log in to review.", careGapCount, alertCount, taskCount))
                .severity("LOW")
                .templateVariable("careGapCount", careGapCount)
                .templateVariable("alertCount", alertCount)
                .templateVariable("taskCount", taskCount)
                .templateVariable("userId", userId)
                .recipient("EMAIL", recipientEmail)
                .metadataEntry("userId", userId)
                .metadataEntry("careGapCount", careGapCount)
                .metadataEntry("alertCount", alertCount)
                .metadataEntry("taskCount", taskCount)
                .sendEmail(true)
                .sendSms(false)
                .sendWebSocket(false)
                .build();
    }
}
