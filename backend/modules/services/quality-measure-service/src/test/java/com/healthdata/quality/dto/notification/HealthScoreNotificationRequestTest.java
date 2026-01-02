package com.healthdata.quality.dto.notification;

import com.healthdata.quality.dto.HealthScoreDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HealthScoreNotificationRequestTest {

    @Test
    void shouldBuildTitleMessageSeverityForDecrease() {
        UUID patientId = UUID.randomUUID();
        HealthScoreDTO score = HealthScoreDTO.builder()
            .id(UUID.randomUUID())
            .calculatedAt(Instant.parse("2024-02-01T00:00:00Z"))
            .physicalHealthScore(70.0)
            .mentalHealthScore(80.0)
            .socialDeterminantsScore(60.0)
            .preventiveCareScore(75.0)
            .chronicDiseaseScore(65.0)
            .build();

        HealthScoreNotificationRequest request = HealthScoreNotificationRequest.builder()
            .healthScore(score)
            .tenantId("tenant-1")
            .patientId(patientId)
            .previousScore(80.0)
            .newScore(68.0)
            .changeAmount(-12.0)
            .changeDirection("DECREASED")
            .patientName("Alex Patient")
            .build();

        assertThat(request.getTitle()).isEqualTo("Health Score Decreased: 80.0 → 68.0");
        assertThat(request.getMessage()).contains("Significant health score change detected");
        assertThat(request.getSeverity()).isEqualTo("MEDIUM");
        assertThat(request.shouldSendEmail()).isTrue();
        assertThat(request.shouldSendSms()).isFalse();
        assertThat(request.shouldSendWebSocket()).isTrue();
        assertThat(request.getNotificationId()).isEqualTo(score.getId().toString());

        Map<String, Object> variables = request.getTemplateVariables();
        assertThat(variables).containsKeys("patientId", "patientName", "components", "timestamp");
        assertThat(variables.get("patientName")).isEqualTo("Alex Patient");
    }

    @Test
    void shouldHandleIncreaseAndLowSeverity() {
        UUID patientId = UUID.randomUUID();

        HealthScoreNotificationRequest request = HealthScoreNotificationRequest.builder()
            .tenantId("tenant-1")
            .patientId(patientId)
            .previousScore(75.0)
            .newScore(80.0)
            .changeAmount(5.0)
            .changeDirection("INCREASED")
            .build();

        assertThat(request.getTitle()).isEqualTo("Health Score Improved: 75.0 → 80.0");
        assertThat(request.getMessage()).contains("Health score updated for patient");
        assertThat(request.getSeverity()).isEqualTo("LOW");
        assertThat(request.shouldSendEmail()).isFalse();
        assertThat(request.shouldSendSms()).isFalse();
        assertThat(request.getNotificationId()).isNull();
    }

    @Test
    void shouldHandleStableChangeAndDefaultsWhenMissingHealthScore() {
        UUID patientId = UUID.randomUUID();

        HealthScoreNotificationRequest request = HealthScoreNotificationRequest.builder()
            .tenantId("tenant-1")
            .patientId(patientId)
            .previousScore(70.0)
            .newScore(70.5)
            .changeAmount(0.5)
            .changeDirection("STABLE")
            .build();

        assertThat(request.getTitle()).isEqualTo("Health Score Update: 70.5");
        assertThat(request.getSeverity()).isEqualTo("LOW");
        assertThat(request.shouldSendEmail()).isFalse();
        assertThat(request.shouldSendSms()).isFalse();

        Map<String, Object> variables = request.getTemplateVariables();
        assertThat(variables.get("patientName")).isEqualTo("Patient " + patientId);
        assertThat(variables).doesNotContainKey("components");
    }

    @Test
    void shouldSendEmailAndSmsForCriticalDecrease() {
        UUID patientId = UUID.randomUUID();

        HealthScoreNotificationRequest request = HealthScoreNotificationRequest.builder()
            .tenantId("tenant-1")
            .patientId(patientId)
            .previousScore(90.0)
            .newScore(70.0)
            .changeAmount(-20.0)
            .changeDirection("DECREASED")
            .build();

        assertThat(request.getSeverity()).isEqualTo("HIGH");
        assertThat(request.shouldSendEmail()).isTrue();
        assertThat(request.shouldSendSms()).isTrue();
        assertThat(request.getMessage()).contains("Significant health score change detected");
    }

    @Test
    void shouldIncludeMetadataWhenHealthScoreIdMissing() {
        UUID patientId = UUID.randomUUID();
        HealthScoreDTO score = HealthScoreDTO.builder()
            .calculatedAt(Instant.parse("2024-02-01T00:00:00Z"))
            .physicalHealthScore(70.0)
            .mentalHealthScore(80.0)
            .socialDeterminantsScore(60.0)
            .preventiveCareScore(75.0)
            .chronicDiseaseScore(65.0)
            .build();

        HealthScoreNotificationRequest request = HealthScoreNotificationRequest.builder()
            .healthScore(score)
            .tenantId("tenant-1")
            .patientId(patientId)
            .previousScore(70.0)
            .newScore(85.0)
            .changeAmount(15.0)
            .changeDirection("INCREASED")
            .build();

        Map<String, Object> metadata = request.getMetadata();
        assertThat(metadata).containsEntry("healthScoreId", null);
        assertThat(metadata).containsEntry("physicalHealthScore", 70.0);
    }
}
