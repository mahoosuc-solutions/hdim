package com.healthdata.sdoh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Composite SDOH Risk Score (0-100)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SdohRiskScore {
    private String scoreId;
    private String patientId;
    private String tenantId;
    private Double totalScore; // 0-100
    private Map<SdohCategory, Double> categoryScores;
    private RiskLevel riskLevel;
    private String assessmentId;
    private LocalDateTime calculatedAt;
    private String notes;

    public enum RiskLevel {
        LOW(0, 25),
        MODERATE(26, 50),
        HIGH(51, 75),
        CRITICAL(76, 100);

        private final int minScore;
        private final int maxScore;

        RiskLevel(int minScore, int maxScore) {
            this.minScore = minScore;
            this.maxScore = maxScore;
        }

        public static RiskLevel fromScore(double score) {
            for (RiskLevel level : values()) {
                if (score >= level.minScore && score <= level.maxScore) {
                    return level;
                }
            }
            return LOW;
        }
    }
}
