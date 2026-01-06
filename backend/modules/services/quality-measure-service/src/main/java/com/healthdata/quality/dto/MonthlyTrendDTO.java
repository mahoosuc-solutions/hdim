package com.healthdata.quality.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Monthly Trend DTO
 * Represents performance data for a specific month.
 *
 * Issue #146: Create Provider Performance Metrics API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrendDTO {

    /**
     * Month in YYYY-MM format (e.g., "2025-01")
     */
    private String month;

    /**
     * Compliance rate for the month (0-100)
     */
    private Double rate;

    /**
     * Number of compliant patients
     */
    private Integer numerator;

    /**
     * Number of eligible patients
     */
    private Integer denominator;

    /**
     * Practice average for comparison
     */
    private Double practiceAverage;
}
