package com.healthdata.quality.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Population Metrics DTO
 *
 * Aggregated metrics for population health dashboard
 */
@Data
@Builder
public class PopulationMetricsDTO {
    private Integer totalPatients;
    private Double averageHealthScore;
    private Integer highRiskCount;
    private Integer mediumRiskCount;
    private Integer lowRiskCount;
    private Integer totalCareGaps;
    private Double gapClosureRate;
}
