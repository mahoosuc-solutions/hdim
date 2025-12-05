package com.healthdata.quality.dto;

import com.healthdata.quality.persistence.HealthScoreEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Health Score Data Transfer Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthScoreDTO {

    private UUID id;
    private String patientId;
    private String tenantId;

    /**
     * Overall health score (0-100)
     */
    private Double overallScore;

    /**
     * Component scores
     */
    private Double physicalHealthScore;
    private Double mentalHealthScore;
    private Double socialDeterminantsScore;
    private Double preventiveCareScore;
    private Double chronicDiseaseScore;

    /**
     * Metadata
     */
    private Instant calculatedAt;
    private Double previousScore;
    private Double scoreDelta;
    private boolean significantChange;
    private String changeReason;

    /**
     * Score interpretation
     */
    private String scoreLevel;      // "excellent", "good", "fair", "poor", "critical"
    private String interpretation;  // Human-readable interpretation

    /**
     * Backward compatibility - Component Scores nested DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentScoresDTO {
        private Integer physical;
        private Integer mental;
        private Integer social;
        private Integer preventive;
        private Integer chronicDisease;
    }

    /**
     * Get component scores in nested format for backward compatibility
     */
    public ComponentScoresDTO getComponentScores() {
        return ComponentScoresDTO.builder()
            .physical(physicalHealthScore != null ? physicalHealthScore.intValue() : null)
            .mental(mentalHealthScore != null ? mentalHealthScore.intValue() : null)
            .social(socialDeterminantsScore != null ? socialDeterminantsScore.intValue() : null)
            .preventive(preventiveCareScore != null ? preventiveCareScore.intValue() : null)
            .chronicDisease(chronicDiseaseScore != null ? chronicDiseaseScore.intValue() : null)
            .build();
    }

    /**
     * Get trend
     */
    public String getTrend() {
        if (scoreDelta == null) {
            return "new";
        }
        if (scoreDelta > 5.0) return "improving";
        if (scoreDelta < -5.0) return "declining";
        return "stable";
    }

    /**
     * Convert from entity
     */
    public static HealthScoreDTO fromEntity(HealthScoreEntity entity) {
        HealthScoreDTO dto = HealthScoreDTO.builder()
            .id(entity.getId())
            .patientId(entity.getPatientId())
            .tenantId(entity.getTenantId())
            .overallScore(entity.getOverallScore())
            .physicalHealthScore(entity.getPhysicalHealthScore())
            .mentalHealthScore(entity.getMentalHealthScore())
            .socialDeterminantsScore(entity.getSocialDeterminantsScore())
            .preventiveCareScore(entity.getPreventiveCareScore())
            .chronicDiseaseScore(entity.getChronicDiseaseScore())
            .calculatedAt(entity.getCalculatedAt())
            .previousScore(entity.getPreviousScore())
            .scoreDelta(entity.getScoreDelta())
            .significantChange(entity.isSignificantChange())
            .changeReason(entity.getChangeReason())
            .build();

        // Add interpretation
        dto.setScoreLevel(getScoreLevel(entity.getOverallScore()));
        dto.setInterpretation(getInterpretation(entity.getOverallScore()));

        return dto;
    }

    /**
     * Determine score level
     */
    private static String getScoreLevel(Double score) {
        if (score >= 90.0) return "excellent";
        if (score >= 75.0) return "good";
        if (score >= 60.0) return "fair";
        if (score >= 40.0) return "poor";
        return "critical";
    }

    /**
     * Get human-readable interpretation
     */
    private static String getInterpretation(Double score) {
        if (score >= 90.0) {
            return "Excellent overall health. Continue current health management practices.";
        } else if (score >= 75.0) {
            return "Good overall health. Minor improvements may be beneficial.";
        } else if (score >= 60.0) {
            return "Fair health status. Several areas could benefit from attention.";
        } else if (score >= 40.0) {
            return "Poor health status. Multiple care gaps require immediate attention.";
        } else {
            return "Critical health status. Urgent intervention recommended.";
        }
    }
}
