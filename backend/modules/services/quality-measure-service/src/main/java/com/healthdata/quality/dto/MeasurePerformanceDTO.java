package com.healthdata.quality.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Measure Performance DTO
 * Contains performance metrics for a single quality measure
 * including provider rate, practice average, percentile, and trends.
 *
 * Issue #146: Create Provider Performance Metrics API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasurePerformanceDTO {

    /**
     * Quality measure identifier (e.g., "CMS122v11")
     */
    private String measureId;

    /**
     * Display name of the measure
     */
    private String measureName;

    /**
     * Measure category (HEDIS, CMS, CUSTOM)
     */
    private String category;

    /**
     * Provider's compliance rate for this measure (0-100)
     */
    private Double providerRate;

    /**
     * Practice average compliance rate (anonymized)
     */
    private Double practiceAverage;

    /**
     * National benchmark rate (if available)
     */
    private Double nationalBenchmark;

    /**
     * Provider's percentile ranking within practice (0-100)
     * e.g., 75 means provider is at 75th percentile
     */
    private Integer percentile;

    /**
     * Number of patients meeting numerator criteria
     */
    private Integer numerator;

    /**
     * Number of patients meeting denominator criteria (eligible)
     */
    private Integer denominator;

    /**
     * Historical trend data (last 12 months)
     */
    private List<MonthlyTrendDTO> trend;

    /**
     * Change from previous period
     */
    private Double changeFromPrevious;

    /**
     * Direction of trend: "improving", "declining", "stable"
     */
    private String trendDirection;

    /**
     * Performance status: "above_average", "average", "below_average"
     */
    private String performanceStatus;

    /**
     * Whether this measure requires attention (below threshold)
     */
    private Boolean requiresAttention;

    /**
     * Gap to reach next performance tier
     */
    private Integer gapToNextTier;
}
