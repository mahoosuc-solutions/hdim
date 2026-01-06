package com.healthdata.quality.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Provider Performance Response DTO
 * Returns performance metrics for a specific provider including
 * compliance rates, practice averages, trends, and rankings.
 *
 * Issue #146: Create Provider Performance Metrics API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPerformanceResponse {

    /**
     * Provider's unique identifier
     */
    private UUID providerId;

    /**
     * Provider's display name
     */
    private String providerName;

    /**
     * Time period for the metrics
     */
    private String period;

    /**
     * Performance metrics for each quality measure
     */
    private List<MeasurePerformanceDTO> measures;

    /**
     * Overall quality score across all measures (0-100)
     */
    private Double overallScore;

    /**
     * Provider's percentile ranking within the practice (0-100)
     */
    private Integer overallPercentile;

    /**
     * Total patients in provider's panel
     */
    private Integer totalPatients;

    /**
     * Total eligible patients for any measure
     */
    private Integer totalEligible;

    /**
     * Total compliant patients across all measures
     */
    private Integer totalCompliant;

    /**
     * Measures where provider is below practice average
     */
    private List<String> improvementAreas;

    /**
     * Measures where provider excels (top quartile)
     */
    private List<String> strengthAreas;

    /**
     * Date when metrics were last calculated
     */
    private String calculatedAt;
}
