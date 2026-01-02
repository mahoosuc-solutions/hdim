package com.healthdata.quality.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for CDS rule evaluation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdsEvaluateResponse {

    private UUID patientId;
    private Instant evaluatedAt;
    private Integer rulesEvaluated;
    private Integer recommendationsGenerated;
    private Integer existingRecommendationsSkipped;
    private List<CdsRecommendationDTO> newRecommendations;
    private List<CdsRecommendationDTO> existingRecommendations;
    private Map<String, Integer> recommendationsByCategory;
    private Map<String, Integer> recommendationsByUrgency;
    private List<EvaluationDetail> evaluationDetails;

    /**
     * Details about each rule evaluation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationDetail {
        private String ruleCode;
        private String ruleName;
        private Boolean triggered;
        private String result;
        private String errorMessage;
        private Long evaluationTimeMs;
    }
}
