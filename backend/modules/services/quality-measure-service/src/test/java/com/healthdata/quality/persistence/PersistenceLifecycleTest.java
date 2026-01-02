package com.healthdata.quality.persistence;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceLifecycleTest {

    @Test
    void shouldSetDefaultsOnCreate() {
        NotificationEntity notification = new NotificationEntity();
        notification.onCreate();
        assertThat(notification.getCreatedAt()).isNotNull();
        assertThat(notification.getStatus()).isEqualTo(NotificationEntity.NotificationStatus.PENDING);

        EventSnapshotEntity snapshot = new EventSnapshotEntity();
        snapshot.onCreate();
        assertThat(snapshot.getCreatedAt()).isNotNull();

        PopulationMetricsEntity metrics = new PopulationMetricsEntity();
        metrics.setCalculatedAt(null);
        metrics.setMetricDate(null);
        metrics.onCreate();
        assertThat(metrics.getCalculatedAt()).isNotNull();
        assertThat(metrics.getMetricDate()).isNotNull();

        CdsAcknowledgmentEntity acknowledgment = new CdsAcknowledgmentEntity();
        acknowledgment.onCreate();
        assertThat(acknowledgment.getCreatedAt()).isNotNull();

        HealthEventEntity healthEvent = new HealthEventEntity();
        healthEvent.onCreate();
        assertThat(healthEvent.getOccurredAt()).isNotNull();
        assertThat(healthEvent.getRecordedAt()).isNotNull();

        CdsRuleEntity rule = new CdsRuleEntity();
        rule.onCreate();
        assertThat(rule.getCreatedAt()).isNotNull();
        assertThat(rule.getUpdatedAt()).isNotNull();

        CdsRecommendationEntity recommendation = new CdsRecommendationEntity();
        recommendation.onCreate();
        assertThat(recommendation.getCreatedAt()).isNotNull();
        assertThat(recommendation.getUpdatedAt()).isNotNull();
        assertThat(recommendation.getEvaluatedAt()).isNotNull();

        ChronicDiseaseMonitoringEntity monitoring = new ChronicDiseaseMonitoringEntity();
        monitoring.onCreate();
        assertThat(monitoring.getCreatedAt()).isNotNull();
        assertThat(monitoring.getUpdatedAt()).isNotNull();
        assertThat(monitoring.getMonitoredAt()).isNotNull();

        JobExecutionEntity jobExecution = new JobExecutionEntity();
        jobExecution.onCreate();
        assertThat(jobExecution.getStartedAt()).isNotNull();
        assertThat(jobExecution.getCreatedAt()).isNotNull();
        assertThat(jobExecution.getUpdatedAt()).isNotNull();

        MlPredictionEntity prediction = new MlPredictionEntity();
        prediction.onCreate();
        assertThat(prediction.getPredictedAt()).isNotNull();
        assertThat(prediction.getCreatedAt()).isNotNull();
        assertThat(prediction.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldUpdateAuditFieldsOnUpdate() {
        Instant before = Instant.now().minusSeconds(10);

        NotificationHistoryEntity history = new NotificationHistoryEntity();
        history.setUpdatedAt(null);
        history.onUpdate();
        assertThat(history.getUpdatedAt()).isAfter(before);

        PatientHealthSummaryEntity summary = new PatientHealthSummaryEntity();
        summary.setProjectionVersion(1L);
        summary.onUpdate();
        assertThat(summary.getLastUpdatedAt()).isAfter(before);
        assertThat(summary.getProjectionVersion()).isEqualTo(2L);

        CdsRuleEntity rule = new CdsRuleEntity();
        rule.setUpdatedAt(null);
        rule.onUpdate();
        assertThat(rule.getUpdatedAt()).isAfter(before);

        CdsRecommendationEntity recommendation = new CdsRecommendationEntity();
        recommendation.setUpdatedAt(null);
        recommendation.onUpdate();
        assertThat(recommendation.getUpdatedAt()).isAfter(before);

        ChronicDiseaseMonitoringEntity monitoring = new ChronicDiseaseMonitoringEntity();
        monitoring.setUpdatedAt(null);
        monitoring.onUpdate();
        assertThat(monitoring.getUpdatedAt()).isAfter(before);

        JobExecutionEntity jobExecution = new JobExecutionEntity();
        jobExecution.setUpdatedAt(null);
        jobExecution.onUpdate();
        assertThat(jobExecution.getUpdatedAt()).isAfter(before);

        MlPredictionEntity prediction = new MlPredictionEntity();
        prediction.setUpdatedAt(null);
        prediction.onUpdate();
        assertThat(prediction.getUpdatedAt()).isAfter(before);
    }
}
