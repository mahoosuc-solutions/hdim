package com.healthdata.quality.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HealthScoreHistoryEntity Tests")
class HealthScoreHistoryEntityTest {

    @Test
    @DisplayName("Should populate createdAt on create")
    void shouldPopulateCreatedAtOnCreate() {
        HealthScoreHistoryEntity entity = new HealthScoreHistoryEntity();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should build history from current health score")
    void shouldBuildHistoryFromHealthScore() {
        UUID patientId = UUID.randomUUID();
        Instant calculatedAt = Instant.now();

        HealthScoreEntity score = HealthScoreEntity.builder()
            .patientId(patientId)
            .tenantId("tenant-1")
            .overallScore(88.0)
            .physicalHealthScore(80.0)
            .mentalHealthScore(90.0)
            .socialDeterminantsScore(70.0)
            .preventiveCareScore(85.0)
            .chronicDiseaseScore(92.0)
            .calculatedAt(calculatedAt)
            .previousScore(75.0)
            .changeReason("Improved vitals")
            .build();

        HealthScoreHistoryEntity history = HealthScoreHistoryEntity.fromHealthScore(score);

        assertThat(history.getPatientId()).isEqualTo(patientId);
        assertThat(history.getTenantId()).isEqualTo("tenant-1");
        assertThat(history.getOverallScore()).isEqualTo(88.0);
        assertThat(history.getCalculatedAt()).isEqualTo(calculatedAt);
        assertThat(history.getPreviousScore()).isEqualTo(75.0);
        assertThat(history.getScoreDelta()).isEqualTo(13.0);
        assertThat(history.getChangeReason()).isEqualTo("Improved vitals");
    }
}
