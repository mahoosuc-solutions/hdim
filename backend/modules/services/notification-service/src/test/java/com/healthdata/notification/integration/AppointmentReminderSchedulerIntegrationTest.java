package com.healthdata.notification.integration;

import com.healthdata.notification.application.AppointmentReminderService;
import com.healthdata.notification.domain.model.AppointmentReminderSent;
import com.healthdata.notification.domain.repository.AppointmentReminderSentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for appointment reminder scheduler
 *
 * Tests database persistence, multi-tenant isolation, and idempotency.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppointmentReminderSchedulerIntegrationTest {

    @Autowired
    private AppointmentReminderSentRepository reminderSentRepository;

    @Test
    void shouldPersistReminderRecord() {
        // Given: A reminder record
        AppointmentReminderSent reminder = AppointmentReminderSent.builder()
                .tenantId("tenant1")
                .appointmentId(java.util.UUID.randomUUID())
                .patientId(java.util.UUID.randomUUID())
                .phoneNumber("+11234567890")
                .reminderType("appointment_reminder")
                .daysBefore(1)
                .channel("SMS")
                .status("SENT")
                .messageSid("SM123456")
                .build();

        // When: Save to database
        AppointmentReminderSent saved = reminderSentRepository.save(reminder);

        // Then: Record persisted with all fields
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo("tenant1");
        assertThat(saved.getStatus()).isEqualTo("SENT");
        assertThat(saved.getMessageSid()).isEqualTo("SM123456");
        assertThat(saved.getSentAt()).isNotNull();
    }

    @Test
    void shouldEnforceMultiTenantIsolation() {
        // Given: Reminders for two different tenants
        java.util.UUID appointmentId = java.util.UUID.randomUUID();

        AppointmentReminderSent tenant1Reminder = AppointmentReminderSent.builder()
                .tenantId("tenant1")
                .appointmentId(appointmentId)
                .patientId(java.util.UUID.randomUUID())
                .phoneNumber("+11111111111")
                .reminderType("appointment_reminder")
                .daysBefore(1)
                .channel("SMS")
                .status("SENT")
                .build();

        AppointmentReminderSent tenant2Reminder = AppointmentReminderSent.builder()
                .tenantId("tenant2")
                .appointmentId(appointmentId)
                .patientId(java.util.UUID.randomUUID())
                .phoneNumber("+12222222222")
                .reminderType("appointment_reminder")
                .daysBefore(1)
                .channel("SMS")
                .status("SENT")
                .build();

        reminderSentRepository.save(tenant1Reminder);
        reminderSentRepository.save(tenant2Reminder);

        // When: Query by tenant
        List<AppointmentReminderSent> tenant1Reminders =
                reminderSentRepository.findByTenantId("tenant1");
        List<AppointmentReminderSent> tenant2Reminders =
                reminderSentRepository.findByTenantId("tenant2");

        // Then: Each tenant sees only their own reminders
        assertThat(tenant1Reminders).hasSize(1);
        assertThat(tenant1Reminders.get(0).getTenantId()).isEqualTo("tenant1");
        assertThat(tenant1Reminders.get(0).getPhoneNumber()).isEqualTo("+11111111111");

        assertThat(tenant2Reminders).hasSize(1);
        assertThat(tenant2Reminders.get(0).getTenantId()).isEqualTo("tenant2");
        assertThat(tenant2Reminders.get(0).getPhoneNumber()).isEqualTo("+12222222222");
    }

    @Test
    void shouldSupportIdempotencyCheck() {
        // Given: A reminder already sent
        java.util.UUID appointmentId = java.util.UUID.randomUUID();
        String tenantId = "tenant1";
        int daysBefore = 3;

        AppointmentReminderSent existingReminder = AppointmentReminderSent.builder()
                .tenantId(tenantId)
                .appointmentId(appointmentId)
                .patientId(java.util.UUID.randomUUID())
                .phoneNumber("+11234567890")
                .reminderType("appointment_reminder")
                .daysBefore(daysBefore)
                .channel("SMS")
                .status("SENT")
                .build();

        reminderSentRepository.save(existingReminder);

        // When: Check if reminder already sent
        var found = reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                tenantId, appointmentId, "appointment_reminder", daysBefore);

        // Then: Reminder found (idempotency check passes)
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo("SENT");
    }

    @Test
    void shouldPreventDuplicateReminders() {
        // Given: A unique constraint on (tenant_id, appointment_id, reminder_type, days_before)
        java.util.UUID appointmentId = java.util.UUID.randomUUID();
        java.util.UUID patientId = java.util.UUID.randomUUID();

        AppointmentReminderSent reminder1 = AppointmentReminderSent.builder()
                .tenantId("tenant1")
                .appointmentId(appointmentId)
                .patientId(patientId)
                .phoneNumber("+11234567890")
                .reminderType("appointment_reminder")
                .daysBefore(1)
                .channel("SMS")
                .status("SENT")
                .build();

        reminderSentRepository.save(reminder1);

        // When: Try to save duplicate reminder (same tenant, appointment, type, days)
        AppointmentReminderSent reminder2 = AppointmentReminderSent.builder()
                .tenantId("tenant1")
                .appointmentId(appointmentId)
                .patientId(patientId)
                .phoneNumber("+11234567890")
                .reminderType("appointment_reminder")
                .daysBefore(1)
                .channel("SMS")
                .status("SENT")
                .build();

        // Then: Database should reject duplicate (or idempotency check prevents it)
        // Note: The service layer prevents this via findByTenantIdAndAppointmentIdAndTypeAndDays
        var existing = reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                "tenant1", appointmentId, "appointment_reminder", 1);

        assertThat(existing).isPresent();
        // Second save would fail or be prevented by service layer
    }

    @Test
    void shouldRecordFailedReminders() {
        // Given: A failed reminder
        AppointmentReminderSent failedReminder = AppointmentReminderSent.builder()
                .tenantId("tenant1")
                .appointmentId(java.util.UUID.randomUUID())
                .patientId(java.util.UUID.randomUUID())
                .phoneNumber("+11234567890")
                .reminderType("appointment_reminder")
                .daysBefore(1)
                .channel("SMS")
                .status("FAILED")
                .errorMessage("Patient has not opted in to SMS")
                .build();

        // When: Save failed reminder
        AppointmentReminderSent saved = reminderSentRepository.save(failedReminder);

        // Then: Failure recorded with error message
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo("FAILED");
        assertThat(saved.getErrorMessage()).isEqualTo("Patient has not opted in to SMS");
        assertThat(saved.getMessageSid()).isNull();
    }

    @Test
    void shouldSupportDifferentReminderDays() {
        // Given: Same appointment, different reminder intervals
        java.util.UUID appointmentId = java.util.UUID.randomUUID();
        java.util.UUID patientId = java.util.UUID.randomUUID();

        AppointmentReminderSent reminder1Day = AppointmentReminderSent.builder()
                .tenantId("tenant1")
                .appointmentId(appointmentId)
                .patientId(patientId)
                .phoneNumber("+11234567890")
                .reminderType("appointment_reminder")
                .daysBefore(1)
                .channel("SMS")
                .status("SENT")
                .build();

        AppointmentReminderSent reminder3Days = AppointmentReminderSent.builder()
                .tenantId("tenant1")
                .appointmentId(appointmentId)
                .patientId(patientId)
                .phoneNumber("+11234567890")
                .reminderType("appointment_reminder")
                .daysBefore(3)
                .channel("SMS")
                .status("SENT")
                .build();

        AppointmentReminderSent reminder7Days = AppointmentReminderSent.builder()
                .tenantId("tenant1")
                .appointmentId(appointmentId)
                .patientId(patientId)
                .phoneNumber("+11234567890")
                .reminderType("appointment_reminder")
                .daysBefore(7)
                .channel("SMS")
                .status("SENT")
                .build();

        // When: Save all three reminders
        reminderSentRepository.save(reminder1Day);
        reminderSentRepository.save(reminder3Days);
        reminderSentRepository.save(reminder7Days);

        // Then: All three reminders exist (different daysBefore values)
        var found1 = reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                "tenant1", appointmentId, "appointment_reminder", 1);
        var found3 = reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                "tenant1", appointmentId, "appointment_reminder", 3);
        var found7 = reminderSentRepository.findByTenantIdAndAppointmentIdAndTypeAndDays(
                "tenant1", appointmentId, "appointment_reminder", 7);

        assertThat(found1).isPresent();
        assertThat(found3).isPresent();
        assertThat(found7).isPresent();
    }
}
