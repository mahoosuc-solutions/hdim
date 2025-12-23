package com.healthdata.quality.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.healthdata.quality.persistence.ChronicDiseaseMonitoringEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Disease Deterioration Detector Tests")
class DiseaseDeteriorationDetectorTest {

    private final DiseaseDeteriorationDetector detector = new DiseaseDeteriorationDetector();

    @Test
    @DisplayName("Should detect HbA1c deterioration and alerts")
    void shouldDetectHbA1cDeteriorationAndAlerts() {
        assertThat(detector.analyzeTrend("HbA1c", 7.5, 9.2))
            .isEqualTo(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING);
        assertThat(detector.shouldTriggerAlert("HbA1c", 9.2, 8.0)).isTrue();
        assertThat(detector.getDeteriorationSeverity("HbA1c", 10.5)).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("Should detect BP improvement and alert threshold")
    void shouldDetectBloodPressureTrend() {
        assertThat(detector.analyzeTrend("BP_SYSTOLIC", 150.0, 130.0))
            .isEqualTo(ChronicDiseaseMonitoringEntity.Trend.IMPROVING);
        assertThat(detector.shouldTriggerAlert("BP_SYSTOLIC", 170.0, 140.0)).isTrue();
        assertThat(detector.getDeteriorationSeverity("BP_SYSTOLIC", 150.0)).isEqualTo("MODERATE");
    }

    @Test
    @DisplayName("Should handle LDL trends and severity")
    void shouldHandleLdlTrendsAndSeverity() {
        assertThat(detector.analyzeTrend("LDL", 180.0, 210.0))
            .isEqualTo(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING);
        assertThat(detector.shouldTriggerAlert("LDL", 230.0, 180.0)).isTrue();
        assertThat(detector.getDeteriorationSeverity("LDL", 170.0)).isEqualTo("MODERATE");
    }

    @Test
    @DisplayName("Should return stable for unknown metrics")
    void shouldReturnStableForUnknownMetrics() {
        assertThat(detector.analyzeTrend("UNKNOWN", null, 5.0))
            .isEqualTo(ChronicDiseaseMonitoringEntity.Trend.STABLE);
        assertThat(detector.shouldTriggerAlert("UNKNOWN", 5.0, 4.0)).isFalse();
        assertThat(detector.getDeteriorationSeverity("UNKNOWN", 5.0)).isEqualTo("NONE");
    }

    @Test
    @DisplayName("Should handle stable and null values")
    void shouldHandleStableAndNullValues() {
        assertThat(detector.analyzeTrend("HbA1c", 7.2, 7.1))
            .isEqualTo(ChronicDiseaseMonitoringEntity.Trend.STABLE);
        assertThat(detector.shouldTriggerAlert("HbA1c", 7.2, 7.0)).isFalse();
        assertThat(detector.shouldTriggerAlert("BP_SYSTOLIC", null, 140.0)).isFalse();
        assertThat(detector.getDeteriorationSeverity("HbA1c", null)).isEqualTo("NONE");
    }

    @Test
    @DisplayName("Should trigger alerts on significant increases")
    void shouldTriggerAlertsOnSignificantIncrease() {
        assertThat(detector.shouldTriggerAlert("HbA1c", 8.5, 7.0)).isTrue();
        assertThat(detector.shouldTriggerAlert("BP_SYSTOLIC", 150.0, 125.0)).isTrue();
        assertThat(detector.shouldTriggerAlert("LDL", 200.0, 150.0)).isTrue();
    }

    @Test
    @DisplayName("Should handle HbA1c trend with no prior value")
    void shouldHandleHbA1cTrendWithoutPrevious() {
        assertThat(detector.analyzeTrend("HbA1c", null, 6.8))
            .isEqualTo(ChronicDiseaseMonitoringEntity.Trend.STABLE);
        assertThat(detector.analyzeTrend("HbA1c", null, 7.5))
            .isEqualTo(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING);
    }

    @Test
    @DisplayName("Should handle generic trend stability")
    void shouldHandleGenericTrendStability() {
        assertThat(detector.analyzeTrend("WEIGHT", 100.0, 108.0))
            .isEqualTo(ChronicDiseaseMonitoringEntity.Trend.STABLE);
        assertThat(detector.analyzeTrend("WEIGHT", 100.0, 116.0))
            .isEqualTo(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING);
    }

    @Test
    @DisplayName("Should detect generic trend changes")
    void shouldDetectGenericTrendChanges() {
        assertThat(detector.analyzeTrend("WEIGHT", 100.0, 120.0))
            .isEqualTo(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING);
        assertThat(detector.analyzeTrend("WEIGHT", 120.0, 100.0))
            .isEqualTo(ChronicDiseaseMonitoringEntity.Trend.IMPROVING);
    }

    @Test
    @DisplayName("Should return stable when current value is null")
    void shouldReturnStableWhenCurrentValueNull() {
        assertThat(detector.analyzeTrend("HbA1c", 7.0, null))
            .isEqualTo(ChronicDiseaseMonitoringEntity.Trend.STABLE);
        assertThat(detector.shouldTriggerAlert("LDL", null, 150.0)).isFalse();
    }

    @Test
    @DisplayName("Should detect improving trends for HbA1c and LDL")
    void shouldDetectImprovingTrends() {
        assertThat(detector.analyzeTrend("HbA1c", 8.0, 7.2))
            .isEqualTo(ChronicDiseaseMonitoringEntity.Trend.IMPROVING);
        assertThat(detector.analyzeTrend("LDL", 180.0, 150.0))
            .isEqualTo(ChronicDiseaseMonitoringEntity.Trend.IMPROVING);
    }

    @Test
    @DisplayName("Should handle severity thresholds across metrics")
    void shouldHandleSeverityThresholds() {
        assertThat(detector.getDeteriorationSeverity("HbA1c", 8.0)).isEqualTo("MODERATE");
        assertThat(detector.getDeteriorationSeverity("BP_SYSTOLIC", 170.0)).isEqualTo("HIGH");
        assertThat(detector.getDeteriorationSeverity("LDL", 230.0)).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("Should return no alerts for missing previous values")
    void shouldNotTriggerAlertWithoutPreviousValue() {
        assertThat(detector.shouldTriggerAlert("BP_SYSTOLIC", 150.0, null)).isFalse();
        assertThat(detector.shouldTriggerAlert("LDL", 190.0, null)).isFalse();
        assertThat(detector.shouldTriggerAlert("HbA1c", 8.0, null)).isFalse();
    }

    @Test
    @DisplayName("Should detect deterioration thresholds for BP and LDL")
    void shouldDetectDeteriorationThresholdsForBpAndLdl() {
        assertThat(detector.analyzeTrend("BP_SYSTOLIC", 135.0, 150.0))
            .isEqualTo(ChronicDiseaseMonitoringEntity.Trend.DETERIORATING);
        assertThat(detector.analyzeTrend("LDL", null, 90.0))
            .isEqualTo(ChronicDiseaseMonitoringEntity.Trend.STABLE);
    }

    @Test
    @DisplayName("Should handle critical severity thresholds")
    void shouldHandleCriticalSeverityThresholds() {
        assertThat(detector.getDeteriorationSeverity("BP_SYSTOLIC", 181.0)).isEqualTo("CRITICAL");
        assertThat(detector.getDeteriorationSeverity("LDL", 200.0)).isEqualTo("HIGH");
    }
}
