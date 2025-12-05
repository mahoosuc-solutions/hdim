package com.healthdata.predictive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Predicted costs by category (inpatient, outpatient, pharmacy, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostBreakdown {

    /**
     * Patient identifier
     */
    private String patientId;

    /**
     * Tenant identifier
     */
    private String tenantId;

    /**
     * Total predicted cost
     */
    private double totalPredictedCost;

    /**
     * Inpatient care costs
     */
    private double inpatientCost;

    /**
     * Outpatient care costs
     */
    private double outpatientCost;

    /**
     * Pharmacy/medication costs
     */
    private double pharmacyCost;

    /**
     * Emergency department costs
     */
    private double emergencyCost;

    /**
     * Lab and diagnostic costs
     */
    private double labCost;

    /**
     * Imaging costs
     */
    private double imagingCost;

    /**
     * Other ancillary service costs
     */
    private double otherCost;

    /**
     * Prediction period in months
     */
    private int predictionPeriodMonths;

    /**
     * Confidence interval (lower bound)
     */
    private double confidenceLower;

    /**
     * Confidence interval (upper bound)
     */
    private double confidenceUpper;

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

    /**
     * Get total predicted cost
     */
    public double calculateTotalCost() {
        return inpatientCost + outpatientCost + pharmacyCost + emergencyCost
                + labCost + imagingCost + otherCost;
    }
}
