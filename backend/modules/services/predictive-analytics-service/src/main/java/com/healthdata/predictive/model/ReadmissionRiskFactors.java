package com.healthdata.predictive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Contributing factors breakdown for readmission risk
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadmissionRiskFactors {

    /**
     * Length of stay in days
     */
    private Integer lengthOfStay;

    /**
     * Acuity of admission (e.g., emergency vs. elective)
     */
    private String acuity;

    /**
     * Charlson Comorbidity Index score
     */
    private Integer charlsonComorbidityIndex;

    /**
     * Number of emergency department visits in past 6 months
     */
    private Integer edVisitsPast6Months;

    /**
     * Number of active chronic conditions
     */
    private Integer activeChronicConditions;

    /**
     * Number of active medications
     */
    private Integer activeMedications;

    /**
     * Recent medication changes (past 30 days)
     */
    private Integer recentMedicationChanges;

    /**
     * Age of patient
     */
    private Integer age;

    /**
     * History of readmissions (past year)
     */
    private Integer previousReadmissions;

    /**
     * Social determinants of health risk indicators
     */
    private Integer socialRiskFactors;

    /**
     * Feature importance scores for each factor
     * Map of feature name to importance (0.0 - 1.0)
     */
    private Map<String, Double> featureImportance;
}
