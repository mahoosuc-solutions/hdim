package com.healthdata.quality.persistence;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntityDefaultsTest {

    @Test
    void shouldPopulateCustomMeasureDefaults() {
        CustomMeasureEntity entity = new CustomMeasureEntity();

        entity.onCreate();

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getStatus()).isEqualTo("DRAFT");
        assertThat(entity.getVersion()).isEqualTo("1.0.0");

        entity.onUpdate();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldPopulateCareGapDates() {
        CareGapEntity entity = new CareGapEntity();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getIdentifiedDate()).isNotNull();

        Instant before = entity.getUpdatedAt();
        entity.onUpdate();
        assertThat(entity.getUpdatedAt()).isAfter(before);
    }

    @Test
    void shouldPopulateRiskAssessmentDates() {
        RiskAssessmentEntity entity = new RiskAssessmentEntity();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getAssessmentDate()).isNotNull();

        Instant before = entity.getUpdatedAt();
        entity.onUpdate();
        assertThat(entity.getUpdatedAt()).isAfter(before);
    }

    @Test
    void shouldPopulateAlertRoutingTimestamps() {
        AlertRoutingConfigEntity entity = new AlertRoutingConfigEntity();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();

        Instant before = entity.getUpdatedAt();
        entity.onUpdate();
        assertThat(entity.getUpdatedAt()).isAfter(before);
    }

    @Test
    void shouldPopulateMentalHealthAssessmentTimestamps() {
        MentalHealthAssessmentEntity entity = new MentalHealthAssessmentEntity();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();

        Instant before = entity.getUpdatedAt();
        entity.onUpdate();
        assertThat(entity.getUpdatedAt()).isAfter(before);
    }

    @Test
    void shouldEvaluateHealthScoreChanges() {
        HealthScoreEntity entity = HealthScoreEntity.builder()
            .id(UUID.randomUUID())
            .patientId(UUID.randomUUID())
            .tenantId("tenant-1")
            .overallScore(85.0)
            .previousScore(70.0)
            .physicalHealthScore(80.0)
            .mentalHealthScore(90.0)
            .socialDeterminantsScore(70.0)
            .preventiveCareScore(75.0)
            .chronicDiseaseScore(85.0)
            .calculatedAt(Instant.now())
            .build();

        entity.evaluateSignificantChange();

        assertThat(entity.isSignificantChange()).isTrue();
        assertThat(entity.getChangeReason()).contains("Significant improvement");
        assertThat(entity.getScoreDelta()).isEqualTo(15.0);

        entity.onCreate();
        assertThat(entity.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldHandleNoPreviousScoreDelta() {
        HealthScoreEntity entity = new HealthScoreEntity();

        assertThat(entity.getScoreDelta()).isNull();
    }

    @Test
    void shouldHandleNonSignificantHealthScoreChange() {
        HealthScoreEntity entity = HealthScoreEntity.builder()
            .id(UUID.randomUUID())
            .patientId(UUID.randomUUID())
            .tenantId("tenant-1")
            .overallScore(84.0)
            .previousScore(90.0)
            .physicalHealthScore(80.0)
            .mentalHealthScore(90.0)
            .socialDeterminantsScore(70.0)
            .preventiveCareScore(75.0)
            .chronicDiseaseScore(85.0)
            .build();

        entity.evaluateSignificantChange();

        assertThat(entity.isSignificantChange()).isFalse();
        assertThat(entity.getChangeReason()).isNull();
        assertThat(entity.getScoreDelta()).isEqualTo(-6.0);
    }

    @Test
    void shouldRecordDeclineChangeReason() {
        HealthScoreEntity entity = HealthScoreEntity.builder()
            .id(UUID.randomUUID())
            .patientId(UUID.randomUUID())
            .tenantId("tenant-1")
            .overallScore(70.0)
            .previousScore(85.0)
            .physicalHealthScore(70.0)
            .mentalHealthScore(72.0)
            .socialDeterminantsScore(68.0)
            .preventiveCareScore(75.0)
            .chronicDiseaseScore(65.0)
            .build();

        entity.evaluateSignificantChange();

        assertThat(entity.isSignificantChange()).isTrue();
        assertThat(entity.getChangeReason()).contains("Significant decline");
        assertThat(entity.getScoreDelta()).isEqualTo(-15.0);
    }

    @Test
    void shouldPopulateCalculatedAtWhenMissing() {
        HealthScoreEntity entity = HealthScoreEntity.builder()
            .id(UUID.randomUUID())
            .patientId(UUID.randomUUID())
            .tenantId("tenant-1")
            .overallScore(80.0)
            .physicalHealthScore(80.0)
            .mentalHealthScore(80.0)
            .socialDeterminantsScore(80.0)
            .preventiveCareScore(80.0)
            .chronicDiseaseScore(80.0)
            .build();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getCalculatedAt()).isNotNull();
    }

    @Test
    void shouldPopulateCareTeamAssignmentDates() {
        CareTeamAssignmentEntity assignment = new CareTeamAssignmentEntity();

        assignment.onCreate();

        assertThat(assignment.getCreatedAt()).isNotNull();
        assertThat(assignment.getUpdatedAt()).isNotNull();
        assertThat(assignment.getEffectiveFrom()).isNotNull();

        Instant before = assignment.getUpdatedAt();
        assignment.onUpdate();
        assertThat(assignment.getUpdatedAt()).isAfter(before);
    }

    @Test
    void shouldPopulateClinicalAlertDates() {
        ClinicalAlertEntity alert = new ClinicalAlertEntity();

        alert.onCreate();

        assertThat(alert.getCreatedAt()).isNotNull();
        assertThat(alert.getUpdatedAt()).isNotNull();
        assertThat(alert.getTriggeredAt()).isNotNull();

        Instant before = alert.getUpdatedAt();
        alert.onUpdate();
        assertThat(alert.getUpdatedAt()).isAfter(before);
    }
}
