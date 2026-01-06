package com.healthdata.predictive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Predicted Care Gap
 *
 * Represents a predicted care gap that is likely to occur within a specified time window.
 * The prediction model uses historical patterns, appointment adherence, medication refills,
 * and similar patient behavior to identify gaps before they happen.
 *
 * Issue #157: Implement Predictive Care Gap Detection
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictedCareGap {

    private String id;
    private String tenantId;
    private String patientId;
    private String patientName;

    // Measure information
    private String measureId;
    private String measureName;
    private String measureCategory; // HEDIS, CMS, preventive, chronic

    // Prediction details
    private double riskScore;           // 0-100 scale
    private RiskTier riskTier;          // LOW, MODERATE, HIGH, VERY_HIGH
    private double confidence;          // 0-1 scale
    private LocalDate predictedGapDate; // When the gap is predicted to occur
    private int daysUntilGap;           // Days until the predicted gap

    // Prediction factors with weighted contributions
    private List<PredictionFactor> predictionFactors;

    // Recommended interventions
    private List<String> recommendedInterventions;
    private String priorityIntervention;
    private double interventionSuccessRate;

    // Historical context
    private int previousGapsForMeasure;
    private LocalDate lastComplianceDate;
    private int daysSinceLastCompliance;

    // Similar patient behavior
    private int similarPatientPoolSize;
    private double similarPatientGapRate;

    // Metadata
    private LocalDateTime predictedAt;
    private String modelVersion;
    private Map<String, Object> metadata;

    /**
     * Check if this gap needs urgent attention (within 7 days)
     */
    public boolean isUrgent() {
        return daysUntilGap <= 7 && riskScore >= 70;
    }

    /**
     * Check if intervention window is closing (within 14 days)
     */
    public boolean isInterventionWindowClosing() {
        return daysUntilGap <= 14 && riskScore >= 50;
    }

    /**
     * Get the primary contributing factor
     */
    public PredictionFactor getPrimaryFactor() {
        if (predictionFactors == null || predictionFactors.isEmpty()) {
            return null;
        }
        return predictionFactors.stream()
            .max((a, b) -> Double.compare(a.getContribution(), b.getContribution()))
            .orElse(null);
    }
}
