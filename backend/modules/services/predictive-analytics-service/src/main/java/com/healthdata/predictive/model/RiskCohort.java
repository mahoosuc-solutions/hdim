package com.healthdata.predictive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Patient cohort with shared risk characteristics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskCohort {

    /**
     * Cohort identifier
     */
    private String cohortId;

    /**
     * Tenant identifier
     */
    private String tenantId;

    /**
     * Risk tier of this cohort
     */
    private RiskTier riskTier;

    /**
     * Patient IDs in this cohort
     */
    private List<String> patientIds;

    /**
     * Number of patients in cohort
     */
    private int patientCount;

    /**
     * Average risk score for cohort
     */
    private double averageRiskScore;

    /**
     * Common risk factors in this cohort
     */
    private Map<String, Double> commonRiskFactors;

    /**
     * Cohort characteristics
     */
    private Map<String, Object> characteristics;

    /**
     * Timestamp when cohort was generated
     */
    private LocalDateTime generatedAt;

    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;
}
