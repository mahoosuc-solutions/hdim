package com.healthdata.quality.dto.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.healthdata.quality.dto.ClinicalAlertDTO;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Clinical Alert Notification Request Tests")
class ClinicalAlertNotificationRequestTest {

    @Test
    @DisplayName("Should build template variables and channel flags for critical alerts")
    void shouldBuildTemplateVariablesForCriticalAlert() {
        UUID patientId = UUID.randomUUID();
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-1")
            .tenantId("tenant-1")
            .patientId(patientId)
            .alertType("MENTAL_HEALTH_CRISIS")
            .severity("CRITICAL")
            .title("Suicide Risk")
            .message("Immediate intervention required")
            .triggeredAt(Instant.parse("2024-01-01T00:00:00Z"))
            .build();

        ClinicalAlertNotificationRequest request = new ClinicalAlertNotificationRequest(
            alert,
            Map.of("EMAIL", "doc@test.com"),
            "Jane Doe"
        );

        assertThat(request.getNotificationType()).isEqualTo("CRITICAL_ALERT");
        assertThat(request.shouldSendEmail()).isTrue();
        assertThat(request.shouldSendSms()).isTrue();
        assertThat(request.shouldSendWebSocket()).isTrue();
        assertThat(request.getNotificationId()).isEqualTo("alert-1");

        Map<String, Object> variables = request.getTemplateVariables();
        assertThat(variables.get("patientName")).isEqualTo("Jane Doe");
        assertThat(variables.get("alertType")).isEqualTo("Mental Health Crisis");
        assertThat(variables.get("facilityName")).isEqualTo("HealthData Clinical System");

        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) variables.get("recommendedActions");
        assertThat(actions).anyMatch(action -> action.contains("Contact patient IMMEDIATELY"));

        Map<String, Object> metadata = request.getMetadata();
        assertThat(metadata.get("alertType")).isEqualTo("MENTAL_HEALTH_CRISIS");
    }

    @Test
    @DisplayName("Should use fallback patient name and SMS rules for HIGH alerts")
    void shouldUseFallbackNameForHighAlerts() {
        UUID patientId = UUID.randomUUID();
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-2")
            .tenantId("tenant-1")
            .patientId(patientId)
            .alertType("RISK_ESCALATION")
            .severity("HIGH")
            .title("Risk Escalation")
            .message("High risk escalation")
            .build();

        ClinicalAlertNotificationRequest request = new ClinicalAlertNotificationRequest(
            alert,
            Map.of("SMS", "+15551234567")
        );

        assertThat(request.shouldSendEmail()).isTrue();
        assertThat(request.shouldSendSms()).isFalse();

        Map<String, Object> variables = request.getTemplateVariables();
        assertThat(variables.get("patientName")).isEqualTo("Patient " + patientId);
        assertThat(variables.get("alertType")).isEqualTo("Risk Escalation");
    }

    @Test
    @DisplayName("Should handle medium severity without email or SMS")
    void shouldHandleMediumSeverityWithoutEmailOrSms() {
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-3")
            .tenantId("tenant-1")
            .patientId(UUID.randomUUID())
            .alertType("HEALTH_DECLINE")
            .severity("MEDIUM")
            .title("Health Decline")
            .message("Decline detected")
            .build();

        ClinicalAlertNotificationRequest request = new ClinicalAlertNotificationRequest(
            alert,
            Map.of()
        );

        assertThat(request.shouldSendEmail()).isFalse();
        assertThat(request.shouldSendSms()).isFalse();
        assertThat(request.shouldSendWebSocket()).isTrue();
    }

    @Test
    @DisplayName("Should populate defaults when patient ID or timestamp missing")
    void shouldPopulateDefaultsWhenPatientIdOrTimestampMissing() {
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-4")
            .tenantId("tenant-1")
            .patientId(null)
            .alertType("CHRONIC_DETERIORATION")
            .severity("LOW")
            .title("Chronic Deterioration")
            .message("Monitor condition")
            .triggeredAt(null)
            .build();

        ClinicalAlertNotificationRequest request = new ClinicalAlertNotificationRequest(
            alert,
            Map.of()
        );

        Map<String, Object> variables = request.getTemplateVariables();
        assertThat(variables.get("patientName")).isEqualTo("Patient null");
        assertThat(variables.get("timestamp")).isNotNull();
        assertThat(variables.get("alertType")).isEqualTo("Chronic Disease Deterioration");
    }

    @Test
    @DisplayName("Should include guidance for HIGH severity alerts")
    void shouldIncludeGuidanceForHighSeverityAlerts() {
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-5")
            .tenantId("tenant-1")
            .patientId(UUID.randomUUID())
            .alertType("RISK_ESCALATION")
            .severity("HIGH")
            .title("Risk Escalation")
            .message("High risk")
            .build();

        ClinicalAlertNotificationRequest request = new ClinicalAlertNotificationRequest(
            alert,
            Map.of()
        );

        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) request.getTemplateVariables().get("recommendedActions");
        assertThat(actions).anyMatch(action -> action.contains("2-3 business days"));
    }

    @Test
    @DisplayName("Should include guidance for LOW severity alerts and preserve unknown type")
    void shouldIncludeGuidanceForLowSeverityAlerts() {
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-6")
            .tenantId("tenant-1")
            .patientId(UUID.randomUUID())
            .alertType("CUSTOM_ALERT")
            .severity("LOW")
            .title("Custom Alert")
            .message("Low severity")
            .build();

        ClinicalAlertNotificationRequest request = new ClinicalAlertNotificationRequest(
            alert,
            Map.of()
        );

        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) request.getTemplateVariables().get("recommendedActions");
        assertThat(actions).anyMatch(action -> action.contains("Review alert during next scheduled"));
        assertThat(request.getTemplateVariables().get("alertType")).isEqualTo("CUSTOM_ALERT");
        assertThat(request.getMetadata()).containsEntry("status", null);
    }

    @Test
    @DisplayName("Should expose alert fields and timestamps")
    void shouldExposeAlertFieldsAndTimestamps() {
        Instant triggeredAt = Instant.parse("2024-02-02T10:15:30Z");
        UUID patientId = UUID.randomUUID();
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-7")
            .tenantId("tenant-7")
            .patientId(patientId)
            .alertType("RISK_ESCALATION")
            .severity("HIGH")
            .title("Escalation")
            .message("Details")
            .triggeredAt(triggeredAt)
            .build();

        ClinicalAlertNotificationRequest request = new ClinicalAlertNotificationRequest(alert, Map.of());

        assertThat(request.getTemplateId()).isEqualTo("critical-alert");
        assertThat(request.getTenantId()).isEqualTo("tenant-7");
        assertThat(request.getPatientId()).isEqualTo(patientId);
        assertThat(request.getTitle()).isEqualTo("Escalation");
        assertThat(request.getMessage()).isEqualTo("Details");
        assertThat(request.getSeverity()).isEqualTo("HIGH");
        assertThat(request.getTimestamp()).isEqualTo(triggeredAt);
    }

    @Test
    @DisplayName("Should include guidance for critical non-suicide alerts")
    void shouldIncludeGuidanceForCriticalNonSuicideAlerts() {
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-8")
            .tenantId("tenant-1")
            .patientId(UUID.randomUUID())
            .alertType("HEALTH_DECLINE")
            .severity("CRITICAL")
            .title("Critical Health Decline")
            .message("Immediate follow-up")
            .build();

        ClinicalAlertNotificationRequest request = new ClinicalAlertNotificationRequest(alert, Map.of());

        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) request.getTemplateVariables().get("recommendedActions");
        assertThat(actions).anyMatch(action -> action.contains("Contact patient within 24 hours"));
    }
}
