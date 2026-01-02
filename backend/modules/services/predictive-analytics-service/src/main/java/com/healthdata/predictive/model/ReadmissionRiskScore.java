package com.healthdata.predictive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Readmission risk score output (0-100 scale with risk tier)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadmissionRiskScore {

    /**
     * Patient identifier
     */
    private String patientId;

    /**
     * Tenant identifier
     */
    private String tenantId;

    /**
     * Risk score (0-100 scale)
     */
    private double score;

    /**
     * Risk tier classification
     */
    private RiskTier riskTier;

    /**
     * Prediction period (30 or 90 days)
     */
    private int predictionPeriodDays;

    /**
     * Probability of readmission (0.0 - 1.0)
     */
    private double readmissionProbability;

    /**
     * LACE index score (Length, Acuity, Comorbidity, ED visits)
     */
    private Integer laceIndex;

    /**
     * Contributing risk factors with their importance scores
     */
    private ReadmissionRiskFactors riskFactors;

    /**
     * Model confidence score (0.0 - 1.0)
     */
    private double confidence;

    /**
     * Model version used for prediction
     */
    private String modelVersion;

    /**
     * Timestamp when prediction was made
     */
    private LocalDateTime predictedAt;

    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;
}
