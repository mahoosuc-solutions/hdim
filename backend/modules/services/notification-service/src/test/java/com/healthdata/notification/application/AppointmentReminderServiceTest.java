package com.healthdata.notification.application;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
                reminderSentRepository
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
        assertThat(message).contains("Saturday, February 15, 2026");
        assertThat(message).contains("2:30 PM");
        assertThat(message).contains("Reply STOP to opt out");
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
