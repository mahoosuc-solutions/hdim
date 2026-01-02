package com.healthdata.quality.service;

import com.healthdata.quality.persistence.ChronicDiseaseMonitoringEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Disease Deterioration Detector
 *
 * Analyzes trends and determines if alerts should be triggered based on:
 * - Clinical thresholds for specific metrics
 * - Rate of change between measurements
 * - Disease-specific deterioration criteria
 *
 * Thresholds:
 * - HbA1c: >9% = deteriorating, increase >1% = alert
 * - BP Systolic: >140 = deteriorating, >160 = alert
 * - LDL Cholesterol: >190 = deteriorating, >220 = alert
 *
 * Part of Phase 4.2: Chronic Disease Deterioration Detection
 */
@Component
@Slf4j
public class DiseaseDeteriorationDetector {

    // HbA1c thresholds
    private static final double HBA1C_TARGET = 7.0;
    private static final double HBA1C_DETERIORATING_THRESHOLD = 9.0;
    private static final double HBA1C_CHANGE_ALERT_THRESHOLD = 1.0;

    // Blood Pressure thresholds
    private static final double BP_SYSTOLIC_TARGET = 130.0;
    private static final double BP_SYSTOLIC_DETERIORATING = 140.0;
    private static final double BP_SYSTOLIC_ALERT = 160.0;

    // LDL Cholesterol thresholds
    private static final double LDL_TARGET = 100.0;
    private static final double LDL_DETERIORATING = 190.0;
    private static final double LDL_ALERT = 220.0;

    /**
     * Analyze trend for a specific metric
     *
     * @param metric Metric name (HbA1c, BP_SYSTOLIC, LDL, etc.)
     * @param previousValue Previous measurement (null if first measurement)
     * @param currentValue Current measurement
     * @return Trend (IMPROVING, STABLE, or DETERIORATING)
     */
    public ChronicDiseaseMonitoringEntity.Trend analyzeTrend(
        String metric,
        Double previousValue,
        Double currentValue
    ) {
        log.debug("Analyzing trend for {}: previous={}, current={}", metric, previousValue, currentValue);

        if (currentValue == null) {
            return ChronicDiseaseMonitoringEntity.Trend.STABLE;
        }

        switch (metric) {
            case "HbA1c":
                return analyzeHbA1cTrend(previousValue, currentValue);

            case "BP_SYSTOLIC":
                return analyzeBloodPressureTrend(previousValue, currentValue);

            case "LDL":
                return analyzeLDLTrend(previousValue, currentValue);

            default:
                return analyzeGenericTrend(previousValue, currentValue);
        }
    }

    /**
     * Determine if an alert should be triggered
     *
     * @param metric Metric name
     * @param currentValue Current measurement
     * @param previousValue Previous measurement (null if first measurement)
     * @return true if alert should be triggered
     */
    public boolean shouldTriggerAlert(String metric, Double currentValue, Double previousValue) {
        log.debug("Checking alert trigger for {}: current={}, previous={}", metric, currentValue, previousValue);

        if (currentValue == null) {
            return false;
        }

        switch (metric) {
            case "HbA1c":
                return shouldTriggerHbA1cAlert(currentValue, previousValue);

            case "BP_SYSTOLIC":
                return shouldTriggerBPAlert(currentValue, previousValue);

            case "LDL":
                return shouldTriggerLDLAlert(currentValue, previousValue);

            default:
                return false;
        }
    }

    /**
     * Analyze HbA1c trend
     * - IMPROVING: Moving toward target (<7.0%)
     * - STABLE: Within acceptable range or minor changes
     * - DETERIORATING: Moving away from target or >9.0%
     */
    private ChronicDiseaseMonitoringEntity.Trend analyzeHbA1cTrend(Double previousValue, Double currentValue) {
        // If current value is >9%, always deteriorating
        if (currentValue > HBA1C_DETERIORATING_THRESHOLD) {
            return ChronicDiseaseMonitoringEntity.Trend.DETERIORATING;
        }

        // If no previous value, check against target
        if (previousValue == null) {
            return currentValue > HBA1C_TARGET
                ? ChronicDiseaseMonitoringEntity.Trend.DETERIORATING
                : ChronicDiseaseMonitoringEntity.Trend.STABLE;
        }

        // Compare to previous value
        double change = currentValue - previousValue;

        if (change > 0.5) {
            // Significant increase
            return ChronicDiseaseMonitoringEntity.Trend.DETERIORATING;
        } else if (change < -0.5) {
            // Significant decrease (improvement)
            return ChronicDiseaseMonitoringEntity.Trend.IMPROVING;
        } else {
            // Stable
            return ChronicDiseaseMonitoringEntity.Trend.STABLE;
        }
    }

    /**
     * Determine if HbA1c alert should trigger
     * - Alert if >9% (critical threshold)
     * - Alert if increased by >1% from previous
     */
    private boolean shouldTriggerHbA1cAlert(Double currentValue, Double previousValue) {
        // Critical threshold
        if (currentValue > HBA1C_DETERIORATING_THRESHOLD) {
            log.info("HbA1c alert: {} exceeds critical threshold {}", currentValue, HBA1C_DETERIORATING_THRESHOLD);
            return true;
        }

        // Significant increase
        if (previousValue != null) {
            double change = currentValue - previousValue;
            if (change > HBA1C_CHANGE_ALERT_THRESHOLD) {
                log.info("HbA1c alert: increase of {} exceeds threshold {}", change, HBA1C_CHANGE_ALERT_THRESHOLD);
                return true;
            }
        }

        return false;
    }

    /**
     * Analyze Blood Pressure trend
     * - IMPROVING: Moving toward target (<130 mmHg)
     * - STABLE: Within acceptable range
     * - DETERIORATING: >140 mmHg or increasing significantly
     */
    private ChronicDiseaseMonitoringEntity.Trend analyzeBloodPressureTrend(Double previousValue, Double currentValue) {
        // If current BP is >140, deteriorating
        if (currentValue > BP_SYSTOLIC_DETERIORATING) {
            return ChronicDiseaseMonitoringEntity.Trend.DETERIORATING;
        }

        if (previousValue == null) {
            return currentValue > BP_SYSTOLIC_TARGET
                ? ChronicDiseaseMonitoringEntity.Trend.DETERIORATING
                : ChronicDiseaseMonitoringEntity.Trend.STABLE;
        }

        double change = currentValue - previousValue;

        if (change > 10) {
            return ChronicDiseaseMonitoringEntity.Trend.DETERIORATING;
        } else if (change < -10) {
            return ChronicDiseaseMonitoringEntity.Trend.IMPROVING;
        } else {
            return ChronicDiseaseMonitoringEntity.Trend.STABLE;
        }
    }

    /**
     * Determine if BP alert should trigger
     * - Alert if systolic >160 mmHg (critical)
     * - Alert if increase >20 mmHg from previous
     */
    private boolean shouldTriggerBPAlert(Double currentValue, Double previousValue) {
        // Critical threshold
        if (currentValue > BP_SYSTOLIC_ALERT) {
            log.info("BP alert: {} exceeds critical threshold {}", currentValue, BP_SYSTOLIC_ALERT);
            return true;
        }

        // Significant increase
        if (previousValue != null) {
            double change = currentValue - previousValue;
            if (change > 20) {
                log.info("BP alert: increase of {} mmHg exceeds threshold", change);
                return true;
            }
        }

        return false;
    }

    /**
     * Analyze LDL Cholesterol trend
     * - IMPROVING: Moving toward target (<100 mg/dL)
     * - STABLE: Within acceptable range
     * - DETERIORATING: >190 mg/dL or increasing significantly
     */
    private ChronicDiseaseMonitoringEntity.Trend analyzeLDLTrend(Double previousValue, Double currentValue) {
        if (currentValue > LDL_DETERIORATING) {
            return ChronicDiseaseMonitoringEntity.Trend.DETERIORATING;
        }

        if (previousValue == null) {
            return currentValue > LDL_TARGET
                ? ChronicDiseaseMonitoringEntity.Trend.DETERIORATING
                : ChronicDiseaseMonitoringEntity.Trend.STABLE;
        }

        double change = currentValue - previousValue;

        if (change > 20) {
            return ChronicDiseaseMonitoringEntity.Trend.DETERIORATING;
        } else if (change < -20) {
            return ChronicDiseaseMonitoringEntity.Trend.IMPROVING;
        } else {
            return ChronicDiseaseMonitoringEntity.Trend.STABLE;
        }
    }

    /**
     * Determine if LDL alert should trigger
     * - Alert if >220 mg/dL (very high)
     * - Alert if increase >40 mg/dL from previous
     */
    private boolean shouldTriggerLDLAlert(Double currentValue, Double previousValue) {
        if (currentValue > LDL_ALERT) {
            log.info("LDL alert: {} exceeds critical threshold {}", currentValue, LDL_ALERT);
            return true;
        }

        if (previousValue != null) {
            double change = currentValue - previousValue;
            if (change > 40) {
                log.info("LDL alert: increase of {} mg/dL exceeds threshold", change);
                return true;
            }
        }

        return false;
    }

    /**
     * Generic trend analysis for unknown metrics
     */
    private ChronicDiseaseMonitoringEntity.Trend analyzeGenericTrend(Double previousValue, Double currentValue) {
        if (previousValue == null) {
            return ChronicDiseaseMonitoringEntity.Trend.STABLE;
        }

        double percentChange = Math.abs((currentValue - previousValue) / previousValue);

        if (percentChange > 0.15) { // >15% change
            return currentValue > previousValue
                ? ChronicDiseaseMonitoringEntity.Trend.DETERIORATING
                : ChronicDiseaseMonitoringEntity.Trend.IMPROVING;
        }

        return ChronicDiseaseMonitoringEntity.Trend.STABLE;
    }

    /**
     * Get deterioration severity level
     */
    public String getDeteriorationSeverity(String metric, Double value) {
        if (value == null) {
            return "NONE";
        }

        return switch (metric) {
            case "HbA1c" -> {
                if (value > 10.0) yield "CRITICAL";
                if (value > 9.0) yield "HIGH";
                if (value > 7.0) yield "MODERATE";
                yield "NONE";
            }
            case "BP_SYSTOLIC" -> {
                if (value > 180) yield "CRITICAL";
                if (value > 160) yield "HIGH";
                if (value > 140) yield "MODERATE";
                yield "NONE";
            }
            case "LDL" -> {
                if (value > 220) yield "CRITICAL";
                if (value > 190) yield "HIGH";
                if (value > 160) yield "MODERATE";
                yield "NONE";
            }
            default -> "NONE";
        };
    }
}
