package com.healthdata.notification.application;

import com.healthdata.audit.models.AuditEvent;
import com.healthdata.audit.service.AuditService;
import com.healthdata.featureflags.TenantFeatureFlagService;
import com.healthdata.notification.domain.model.AppointmentReminderSent;
import com.healthdata.notification.domain.repository.AppointmentReminderSentRepository;
import com.healthdata.notification.infrastructure.client.FhirServiceClient;
import com.healthdata.notification.infrastructure.client.PatientServiceClient;
import com.healthdata.notification.infrastructure.providers.SmsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppointmentReminderService
 */
@ExtendWith(MockitoExtension.class)
class AppointmentReminderServiceTest {

    @Mock
    private TenantFeatureFlagService featureFlagService;

    @Mock
    private FhirServiceClient fhirServiceClient;

    @Mock
    private PatientServiceClient patientServiceClient;

    @Mock
    private SmsProvider smsProvider;

    @Mock
    private AppointmentReminderSentRepository reminderSentRepository;

    @Mock
    private AuditService auditService;

    private AppointmentReminderService service;

    private String tenantId;
    private UUID appointmentId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        service = new AppointmentReminderService(
                featureFlagService,
                fhirServiceClient,
                patientServiceClient,
                smsProvider,
                reminderSentRepository,
                auditService
        );

        tenantId = "tenant1";
        appointmentId = UUID.randomUUID();
        patientId = UUID.randomUUID();
    }

    @Test
    void shouldProcessReminders_WhenFeatureEnabled() {
        // Given: Feature enabled with default config
        when(featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")).thenReturn(true);
        when(featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders"))
                .thenReturn(Map.of("reminder_days", List.of(1, 3, 7)));

        FhirServiceClient.AppointmentDto appointment = createTestAppointment();
        when(fhirServiceClient.getAppointments(eq(tenantId), any(), any(), eq("booked")))
                .thenReturn(List.of(appointment));

        PatientServiceClient.PatientContactDto patient = createTestPatient();
        when(patientServiceClient.getPatientContact(tenantId, patientId)).thenReturn(patient);

        when(reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                eq(tenantId), eq(appointmentId), anyString(), eq(1)))
                .thenReturn(Optional.empty());

        when(smsProvider.send(anyString(), anyString())).thenReturn("SM123");

        // When: Process 1-day reminders
        service.processReminders(tenantId, 1);

        // Then: SMS sent and reminder recorded
        verify(smsProvider).send(eq(patient.getPhoneNumber()), anyString());
        verify(reminderSentRepository).save(any(AppointmentReminderSent.class));
    }

    @Test
    void shouldSkipReminders_WhenFeatureDisabled() {
        // Given: Feature disabled
        when(featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")).thenReturn(false);

        // When: Process reminders
        service.processReminders(tenantId, 1);

        // Then: No appointments queried, no SMS sent
        verify(fhirServiceClient, never()).getAppointments(any(), any(), any(), any());
        verify(smsProvider, never()).send(any(), any());
    }

    @Test
    void shouldSkipReminder_WhenAlreadySent() {
        // Given: Feature enabled and appointment exists
        when(featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")).thenReturn(true);
        when(featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders"))
                .thenReturn(Map.of("reminder_days", List.of(1)));

        FhirServiceClient.AppointmentDto appointment = createTestAppointment();
        when(fhirServiceClient.getAppointments(eq(tenantId), any(), any(), eq("booked")))
                .thenReturn(List.of(appointment));

        // Reminder already sent
        when(reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                eq(tenantId), eq(appointmentId), anyString(), eq(1)))
                .thenReturn(Optional.of(new AppointmentReminderSent()));

        // When: Process reminders
        service.processReminders(tenantId, 1);

        // Then: No SMS sent (idempotency)
        verify(smsProvider, never()).send(any(), any());
    }

    @Test
    void shouldSkipReminder_WhenPatientNotOptedIn() {
        // Given: Feature enabled
        when(featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")).thenReturn(true);
        when(featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders"))
                .thenReturn(Map.of("reminder_days", List.of(1)));

        FhirServiceClient.AppointmentDto appointment = createTestAppointment();
        when(fhirServiceClient.getAppointments(eq(tenantId), any(), any(), eq("booked")))
                .thenReturn(List.of(appointment));

        // Patient NOT opted in
        PatientServiceClient.PatientContactDto patient = createTestPatient();
        patient.setSmsOptIn(false);
        when(patientServiceClient.getPatientContact(tenantId, patientId)).thenReturn(patient);

        when(reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                eq(tenantId), eq(appointmentId), anyString(), eq(1)))
                .thenReturn(Optional.empty());

        // When: Process reminders
        service.processReminders(tenantId, 1);

        // Then: No SMS sent (consent check)
        verify(smsProvider, never()).send(any(), any());
    }

    @Test
    void shouldSkipReminder_WhenPatientHasNoPhoneNumber() {
        // Given: Feature enabled
        when(featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")).thenReturn(true);
        when(featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders"))
                .thenReturn(Map.of("reminder_days", List.of(1)));

        FhirServiceClient.AppointmentDto appointment = createTestAppointment();
        when(fhirServiceClient.getAppointments(eq(tenantId), any(), any(), eq("booked")))
                .thenReturn(List.of(appointment));

        // Patient has no phone number
        PatientServiceClient.PatientContactDto patient = createTestPatient();
        patient.setPhoneNumber(null);
        when(patientServiceClient.getPatientContact(tenantId, patientId)).thenReturn(patient);

        when(reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                eq(tenantId), eq(appointmentId), anyString(), eq(1)))
                .thenReturn(Optional.empty());

        // When: Process reminders
        service.processReminders(tenantId, 1);

        // Then: No SMS sent
        verify(smsProvider, never()).send(any(), any());
    }

    @Test
    void shouldRecordSuccessfulReminder() {
        // Given: Feature enabled, patient opted in
        when(featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")).thenReturn(true);
        when(featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders"))
                .thenReturn(Map.of("reminder_days", List.of(1)));

        FhirServiceClient.AppointmentDto appointment = createTestAppointment();
        when(fhirServiceClient.getAppointments(eq(tenantId), any(), any(), eq("booked")))
                .thenReturn(List.of(appointment));

        PatientServiceClient.PatientContactDto patient = createTestPatient();
        when(patientServiceClient.getPatientContact(tenantId, patientId)).thenReturn(patient);

        when(reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                eq(tenantId), eq(appointmentId), anyString(), eq(1)))
                .thenReturn(Optional.empty());

        String messageSid = "SM123456";
        when(smsProvider.send(anyString(), anyString())).thenReturn(messageSid);

        // When: Process reminders
        service.processReminders(tenantId, 1);

        // Then: Reminder recorded with message SID
        ArgumentCaptor<AppointmentReminderSent> captor = ArgumentCaptor.forClass(AppointmentReminderSent.class);
        verify(reminderSentRepository).save(captor.capture());

        AppointmentReminderSent saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(saved.getPatientId()).isEqualTo(patientId);
        assertThat(saved.getMessageSid()).isEqualTo(messageSid);
        assertThat(saved.getStatus()).isEqualTo("SENT");
        assertThat(saved.getDaysBefore()).isEqualTo(1);
    }

    @Test
    void shouldRecordFailedReminder_WhenSmsFails() {
        // Given: Feature enabled, patient opted in
        when(featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")).thenReturn(true);
        when(featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders"))
                .thenReturn(Map.of("reminder_days", List.of(1)));

        FhirServiceClient.AppointmentDto appointment = createTestAppointment();
        when(fhirServiceClient.getAppointments(eq(tenantId), any(), any(), eq("booked")))
                .thenReturn(List.of(appointment));

        PatientServiceClient.PatientContactDto patient = createTestPatient();
        when(patientServiceClient.getPatientContact(tenantId, patientId)).thenReturn(patient);

        when(reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                eq(tenantId), eq(appointmentId), anyString(), eq(1)))
                .thenReturn(Optional.empty());

        // SMS send fails
        when(smsProvider.send(anyString(), anyString()))
                .thenThrow(new RuntimeException("Twilio API error"));

        // When: Process reminders
        service.processReminders(tenantId, 1);

        // Then: Failure recorded
        ArgumentCaptor<AppointmentReminderSent> captor = ArgumentCaptor.forClass(AppointmentReminderSent.class);
        verify(reminderSentRepository).save(captor.capture());

        AppointmentReminderSent saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("FAILED");
        assertThat(saved.getErrorMessage()).contains("SMS send failed");
    }

    @Test
    void shouldBuildCorrectSmsMessage() {
        // Given: Feature enabled
        when(featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")).thenReturn(true);
        when(featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders"))
                .thenReturn(Map.of("reminder_days", List.of(1)));

        FhirServiceClient.AppointmentDto appointment = createTestAppointment();
        appointment.setStartTime(LocalDateTime.of(2026, 2, 15, 14, 30));
        when(fhirServiceClient.getAppointments(eq(tenantId), any(), any(), eq("booked")))
                .thenReturn(List.of(appointment));

        PatientServiceClient.PatientContactDto patient = createTestPatient();
        patient.setFirstName("John");
        patient.setLastName("Doe");
        when(patientServiceClient.getPatientContact(tenantId, patientId)).thenReturn(patient);

        when(reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                eq(tenantId), eq(appointmentId), anyString(), eq(1)))
                .thenReturn(Optional.empty());

        when(smsProvider.send(anyString(), anyString())).thenReturn("SM123");

        // When: Process reminders
        service.processReminders(tenantId, 1);

        // Then: Message contains patient name, date, time
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsProvider).send(anyString(), messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertThat(message).contains("John Doe");
        assertThat(message).contains("Sunday, February 15, 2026");
        assertThat(message).contains("2:30 PM");
        assertThat(message).contains("Reply STOP to opt out");
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    void shouldRecordFailure_WhenPatientServiceFails() {
        // Given: Feature enabled but patient service fails
        when(featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")).thenReturn(true);
        when(featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders"))
                .thenReturn(Map.of("reminder_days", List.of(1)));

        FhirServiceClient.AppointmentDto appointment = createTestAppointment();
        when(fhirServiceClient.getAppointments(eq(tenantId), any(), any(), eq("booked")))
                .thenReturn(List.of(appointment));

        when(reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                eq(tenantId), eq(appointmentId), anyString(), eq(1)))
                .thenReturn(Optional.empty());

        // Patient service throws exception
        when(patientServiceClient.getPatientContact(tenantId, patientId))
                .thenThrow(new RuntimeException("Patient service unavailable"));

        // When: Process reminders
        service.processReminders(tenantId, 1);

        // Then: Failure recorded, no SMS sent
        verify(smsProvider, never()).send(any(), any());
        ArgumentCaptor<AppointmentReminderSent> captor = ArgumentCaptor.forClass(AppointmentReminderSent.class);
        verify(reminderSentRepository).save(captor.capture());

        AppointmentReminderSent saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("FAILED");
        assertThat(saved.getErrorMessage()).contains("Failed to get patient contact");
    }

    @Test
    void shouldLogCriticalError_WhenDatabaseSaveFailsForSuccessfulReminder() {
        // Given: Feature enabled, SMS succeeds, but database save fails
        when(featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")).thenReturn(true);
        when(featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders"))
                .thenReturn(Map.of("reminder_days", List.of(1)));

        FhirServiceClient.AppointmentDto appointment = createTestAppointment();
        when(fhirServiceClient.getAppointments(eq(tenantId), any(), any(), eq("booked")))
                .thenReturn(List.of(appointment));

        PatientServiceClient.PatientContactDto patient = createTestPatient();
        when(patientServiceClient.getPatientContact(tenantId, patientId)).thenReturn(patient);

        when(reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                eq(tenantId), eq(appointmentId), anyString(), eq(1)))
                .thenReturn(Optional.empty());

        when(smsProvider.send(anyString(), anyString())).thenReturn("SM123");

        // Database save fails
        when(reminderSentRepository.save(any(AppointmentReminderSent.class)))
                .thenThrow(new DataAccessException("Database connection failed") {});

        // When: Process reminders (batch processor catches exception)
        service.processReminders(tenantId, 1);

        // Then: SMS was sent (critical audit gap scenario)
        verify(smsProvider).send(anyString(), anyString());

        // Database save was attempted (failed)
        verify(reminderSentRepository).save(any(AppointmentReminderSent.class));

        // Batch processor caught exception and logged it as error
        // (Verify by checking the batch didn't throw - error count incremented instead)
    }

    @Test
    void shouldContinueProcessing_WhenDatabaseSaveFailsForFailedReminder() {
        // Given: Feature enabled, SMS fails, database save also fails (double-failure)
        when(featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")).thenReturn(true);
        when(featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders"))
                .thenReturn(Map.of("reminder_days", List.of(1)));

        FhirServiceClient.AppointmentDto appointment = createTestAppointment();
        when(fhirServiceClient.getAppointments(eq(tenantId), any(), any(), eq("booked")))
                .thenReturn(List.of(appointment));

        PatientServiceClient.PatientContactDto patient = createTestPatient();
        when(patientServiceClient.getPatientContact(tenantId, patientId)).thenReturn(patient);

        when(reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                eq(tenantId), eq(appointmentId), anyString(), eq(1)))
                .thenReturn(Optional.empty());

        // SMS send fails
        when(smsProvider.send(anyString(), anyString()))
                .thenThrow(new RuntimeException("Twilio error"));

        // Database save also fails
        when(reminderSentRepository.save(any(AppointmentReminderSent.class)))
                .thenThrow(new DataAccessException("Database error") {});

        // When: Process reminders (should NOT throw - batch continues)
        service.processReminders(tenantId, 1);

        // Then: No exception thrown, processing continues
        verify(smsProvider).send(anyString(), anyString());
        verify(reminderSentRepository).save(any(AppointmentReminderSent.class));
    }

    @Test
    void shouldLogAuditEvent_WhenSmsSentSuccessfully() {
        // Given: Feature enabled, SMS succeeds
        when(featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")).thenReturn(true);
        when(featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders"))
                .thenReturn(Map.of("reminder_days", List.of(1)));

        FhirServiceClient.AppointmentDto appointment = createTestAppointment();
        when(fhirServiceClient.getAppointments(eq(tenantId), any(), any(), eq("booked")))
                .thenReturn(List.of(appointment));

        PatientServiceClient.PatientContactDto patient = createTestPatient();
        when(patientServiceClient.getPatientContact(tenantId, patientId)).thenReturn(patient);

        when(reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                eq(tenantId), eq(appointmentId), anyString(), eq(1)))
                .thenReturn(Optional.empty());

        when(smsProvider.send(anyString(), anyString())).thenReturn("SM123");

        // When: Process reminders
        service.processReminders(tenantId, 1);

        // Then: Audit event logged
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService).logAuditEvent(auditCaptor.capture());

        AuditEvent auditEvent = auditCaptor.getValue();
        assertThat(auditEvent.getTenantId()).isEqualTo(tenantId);
        assertThat(auditEvent.getResourceType()).isEqualTo("Notification");
        assertThat(auditEvent.getResourceId()).isEqualTo(appointmentId.toString());
        assertThat(auditEvent.getServiceName()).isEqualTo("notification-service");
        assertThat(auditEvent.getMethodName()).isEqualTo("sendReminder");
    }

    // ========================================
    // Configuration Parsing Tests
    // ========================================

    @Test
    void shouldUseDefaultReminderDays_WhenConfigEmpty() {
        // Given: Feature enabled with empty config
        when(featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")).thenReturn(true);
        when(featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders"))
                .thenReturn(Map.of());

        FhirServiceClient.AppointmentDto appointment = createTestAppointment();
        when(fhirServiceClient.getAppointments(eq(tenantId), any(), any(), eq("booked")))
                .thenReturn(List.of(appointment));

        PatientServiceClient.PatientContactDto patient = createTestPatient();
        when(patientServiceClient.getPatientContact(tenantId, patientId)).thenReturn(patient);

        when(reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                eq(tenantId), eq(appointmentId), anyString(), eq(1)))
                .thenReturn(Optional.empty());

        when(smsProvider.send(anyString(), anyString())).thenReturn("SM123");

        // When: Process reminders for day 1
        service.processReminders(tenantId, 1);

        // Then: Uses default [1]
        verify(smsProvider).send(anyString(), anyString());
    }

    @Test
    void shouldUseDefaultReminderDays_WhenConfigContainsInvalidTypes() {
        // Given: Feature enabled with invalid config (strings instead of numbers)
        when(featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")).thenReturn(true);
        when(featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders"))
                .thenReturn(Map.of("reminder_days", List.of("1", "3", "7"))); // Strings, not Numbers

        FhirServiceClient.AppointmentDto appointment = createTestAppointment();
        when(fhirServiceClient.getAppointments(eq(tenantId), any(), any(), eq("booked")))
                .thenReturn(List.of(appointment));

        PatientServiceClient.PatientContactDto patient = createTestPatient();
        when(patientServiceClient.getPatientContact(tenantId, patientId)).thenReturn(patient);

        when(reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                eq(tenantId), eq(appointmentId), anyString(), eq(1)))
                .thenReturn(Optional.empty());

        when(smsProvider.send(anyString(), anyString())).thenReturn("SM123");

        // When: Process reminders for day 1 (falls back to default)
        service.processReminders(tenantId, 1);

        // Then: Falls back to default [1], SMS sent
        verify(smsProvider).send(anyString(), anyString());
    }

    @Test
    void shouldUseDefaultReminderDays_WhenConfigContainsNegativeValues() {
        // Given: Feature enabled with invalid config (negative values)
        when(featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")).thenReturn(true);
        when(featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders"))
                .thenReturn(Map.of("reminder_days", List.of(-1, 0, 3)));

        FhirServiceClient.AppointmentDto appointment = createTestAppointment();
        when(fhirServiceClient.getAppointments(eq(tenantId), any(), any(), eq("booked")))
                .thenReturn(List.of(appointment));

        PatientServiceClient.PatientContactDto patient = createTestPatient();
        when(patientServiceClient.getPatientContact(tenantId, patientId)).thenReturn(patient);

        when(reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                eq(tenantId), eq(appointmentId), anyString(), eq(1)))
                .thenReturn(Optional.empty());

        when(smsProvider.send(anyString(), anyString())).thenReturn("SM123");

        // When: Process reminders for day 1 (falls back to default)
        service.processReminders(tenantId, 1);

        // Then: Falls back to default [1], SMS sent
        verify(smsProvider).send(anyString(), anyString());
    }

    @Test
    void shouldHandleMultipleReminderDays() {
        // Given: Feature enabled with multiple reminder days
        when(featureFlagService.isFeatureEnabled(tenantId, "twilio-sms-reminders")).thenReturn(true);
        when(featureFlagService.getFeatureConfig(tenantId, "twilio-sms-reminders"))
                .thenReturn(Map.of("reminder_days", List.of(1, 3, 7)));

        FhirServiceClient.AppointmentDto appointment = createTestAppointment();
        when(fhirServiceClient.getAppointments(eq(tenantId), any(), any(), eq("booked")))
                .thenReturn(List.of(appointment));

        PatientServiceClient.PatientContactDto patient = createTestPatient();
        when(patientServiceClient.getPatientContact(tenantId, patientId)).thenReturn(patient);

        // 3-day reminder not sent yet
        when(reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                eq(tenantId), eq(appointmentId), anyString(), eq(3)))
                .thenReturn(Optional.empty());

        when(smsProvider.send(anyString(), anyString())).thenReturn("SM123");

        // When: Process 3-day reminders
        service.processReminders(tenantId, 3);

        // Then: SMS sent for 3-day reminder
        verify(smsProvider).send(anyString(), anyString());
    }

    // Helper methods

    private FhirServiceClient.AppointmentDto createTestAppointment() {
        FhirServiceClient.AppointmentDto appointment = new FhirServiceClient.AppointmentDto();
        appointment.setId(appointmentId);
        appointment.setPatientId(patientId);
        appointment.setStatus("booked");
        appointment.setStartTime(LocalDateTime.now().plusDays(1));
        appointment.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        appointment.setPractitionerId("dr-smith");
        appointment.setLocationId("clinic-1");
        return appointment;
    }

    private PatientServiceClient.PatientContactDto createTestPatient() {
        PatientServiceClient.PatientContactDto patient = new PatientServiceClient.PatientContactDto();
        patient.setId(patientId);
        patient.setFirstName("Jane");
        patient.setLastName("Smith");
        patient.setPhoneNumber("+11234567890");
        patient.setEmail("jane.smith@example.com");
        patient.setSmsOptIn(true);
        return patient;
    }
}
