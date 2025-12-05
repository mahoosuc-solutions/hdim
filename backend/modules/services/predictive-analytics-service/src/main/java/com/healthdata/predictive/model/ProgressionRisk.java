package com.healthdata.predictive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Risk of disease advancement (diabetes to complications, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressionRisk {

    /**
     * Patient identifier
     */
    private String patientId;

    /**
     * Tenant identifier
     */
    private String tenantId;

    /**
     * Disease/condition being tracked
     */
    private String condition;

    /**
     * Current disease stage
     */
    private String currentStage;

    /**
     * Predicted next stage
     */
    private String predictedStage;

    /**
     * Probability of progression (0.0 - 1.0)
     */
    private double progressionProbability;

    /**
     * Risk score (0-100 scale)
     */
    private double riskScore;

    /**
     * Risk tier classification
     */
    private RiskTier riskTier;

    /**
     * Predicted time to progression
     */
    private TimeToEvent timeToEvent;

    /**
     * Contributing risk factors
     */
    private Map<String, Double> riskFactors;

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
