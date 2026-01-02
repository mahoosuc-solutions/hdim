package com.healthdata.quality.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for risk assessment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessmentDTO {

    /**
     * Assessment ID
     */
    private String id;

    /**
     * Patient FHIR ID
     */
    private UUID patientId;

    /**
     * Risk category (CARDIOVASCULAR, DIABETES, RESPIRATORY, MENTAL_HEALTH)
     */
    private String riskCategory;

    /**
     * Overall risk score (0-100)
     */
    private Integer riskScore;

    /**
     * Risk level (low, moderate, high, very-high)
     */
    private String riskLevel;

    /**
     * Risk factors contributing to score
     */
    private List<RiskFactorDTO> riskFactors;

    /**
     * Predicted outcomes
     */
    private List<PredictedOutcomeDTO> predictedOutcomes;

    /**
     * Recommended interventions
     */
    private List<String> recommendations;

    /**
     * Date the assessment was calculated
     */
    private Instant assessmentDate;

    /**
     * Date the record was created
     */
    private Instant createdAt;

    /**
     * Risk Factor nested DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskFactorDTO {
        private String factor;
        private String category;
        private Integer weight;
        private String severity;
        private String evidence;
    }

    /**
     * Predicted Outcome nested DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictedOutcomeDTO {
        private String outcome;
        private Double probability;
        private String timeframe;
    }
}
