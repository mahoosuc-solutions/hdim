package com.healthdata.quality.dto.notification;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GenericNotificationRequestTest {

    @Test
    void shouldPopulateTemplateVariablesWithDefaults() {
        UUID patientId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2024-01-01T00:00:00Z");

        GenericNotificationRequest request = GenericNotificationRequest.builder()
            .notificationType("LAB_RESULT")
            .templateId("lab-result")
            .tenantId("tenant-1")
            .patientId(patientId)
            .patientName(null)
            .title("Lab Result")
            .message("New lab result available")
            .severity("MEDIUM")
            .timestamp(timestamp)
            .sendEmail(true)
            .sendSms(false)
            .sendWebSocket(true)
            .build();

        Map<String, Object> variables = request.getTemplateVariables();

        assertThat(variables.get("patientId")).isEqualTo(patientId);
        assertThat(variables.get("patientName")).isEqualTo("Patient " + patientId);
        assertThat(variables.get("title")).isEqualTo("Lab Result");
        assertThat(variables.get("message")).isEqualTo("New lab result available");
        assertThat(variables.get("severity")).isEqualTo("MEDIUM");
        assertThat(variables.get("timestamp")).isEqualTo(timestamp.toString());
        assertThat(variables.get("facilityName")).isEqualTo("HealthData Clinical System");
        assertThat(variables.get("actionUrl")).isEqualTo("https://healthdata-in-motion.com/patients/" + patientId);
    }

    @Test
    void shouldHandleMetadataAndChannelFlags() {
        GenericNotificationRequest request = GenericNotificationRequest.builder()
            .notificationType("DAILY_DIGEST")
            .templateId("digest")
            .tenantId("tenant-1")
            .sendEmail(false)
            .sendSms(true)
            .sendWebSocket(false)
            .build();

        assertThat(request.getMetadata()).isEmpty();
        assertThat(request.shouldSendEmail()).isFalse();
        assertThat(request.shouldSendSms()).isTrue();
        assertThat(request.shouldSendWebSocket()).isFalse();
    }

    @Test
    void appointmentReminderFactoryBuildsExpectedRequest() {
        UUID patientId = UUID.randomUUID();

        GenericNotificationRequest request = GenericNotificationRequest.appointmentReminder(
            "tenant-1",
            patientId,
            "appt-1",
            "Dr. Smith",
            "2025-01-15",
            "14:00",
            "Main Clinic",
            "patient@example.com",
            "555-0100"
        );

        assertThat(request.getNotificationType()).isEqualTo("APPOINTMENT_REMINDER");
        assertThat(request.getTemplateId()).isEqualTo("appointment-reminder");
        assertThat(request.getRecipients())
            .containsEntry("EMAIL", "patient@example.com")
            .containsEntry("SMS", "555-0100");
        assertThat(request.shouldSendEmail()).isTrue();
        assertThat(request.shouldSendSms()).isTrue();
        assertThat(request.shouldSendWebSocket()).isFalse();
        assertThat(request.getNotificationId()).isEqualTo("appt-1");
    }

    @Test
    void medicationReminderFactoryBuildsExpectedRequest() {
        UUID patientId = UUID.randomUUID();

        GenericNotificationRequest request = GenericNotificationRequest.medicationReminder(
            "tenant-1",
            patientId,
            "Atorvastatin",
            "10mg",
            "daily",
            "08:00",
            "patient@example.com",
            "555-0200"
        );

        assertThat(request.getNotificationType()).isEqualTo("MEDICATION_REMINDER");
        assertThat(request.getTemplateId()).isEqualTo("medication-reminder");
        assertThat(request.getRecipients()).containsKeys("EMAIL", "SMS");
        assertThat(request.getMetadata()).containsEntry("medicationName", "Atorvastatin");
    }

    @Test
    void labResultFactoryBuildsExpectedRequest() {
        UUID patientId = UUID.randomUUID();

        GenericNotificationRequest request = GenericNotificationRequest.labResult(
            "tenant-1",
            patientId,
            "lab-1",
            "HbA1c",
            "2025-01-01",
            "Dr. Adams",
            "patient@example.com"
        );

        assertThat(request.getNotificationType()).isEqualTo("LAB_RESULT");
        assertThat(request.shouldSendWebSocket()).isTrue();
        assertThat(request.getNotificationId()).isEqualTo("lab-1");
        assertThat(request.getTemplateVariables()).containsEntry("labResultId", "lab-1");
    }

    @Test
    void dailyDigestFactoryBuildsExpectedRequest() {
        GenericNotificationRequest request = GenericNotificationRequest.dailyDigest(
            "tenant-1",
            "user-1",
            3,
            2,
            1,
            "user@example.com"
        );

        assertThat(request.getNotificationType()).isEqualTo("DAILY_DIGEST");
        assertThat(request.getRecipients()).containsEntry("EMAIL", "user@example.com");
        assertThat(request.getMetadata()).containsEntry("taskCount", 1);
        assertThat(request.shouldSendSms()).isFalse();
    }
}
