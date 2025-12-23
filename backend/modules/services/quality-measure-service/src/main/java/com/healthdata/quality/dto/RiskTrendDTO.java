package com.healthdata.quality.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Risk Trend DTO
 *
 * Represents historical risk trends for a patient in a specific category
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskTrendDTO {

    private UUID patientId;
    private String riskCategory;
    private String trendDirection; // IMPROVING, STABLE, DETERIORATING
    private Double scoreChange; // Total change over period
    private Double averageScore;
    private Integer periodDays;
    private List<DataPoint> dataPoints;
    private List<LevelChange> levelChanges;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        private Instant timestamp;
        private Integer riskScore;
        private String riskLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelChange {
        private Instant timestamp;
        private String fromLevel;
        private String toLevel;
        private String changeType; // ESCALATION or IMPROVEMENT
    }
}
