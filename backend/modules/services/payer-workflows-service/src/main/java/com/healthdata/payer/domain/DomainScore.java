package com.healthdata.payer.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregated score for a Star Rating domain (e.g., Staying Healthy, Managing Chronic Conditions).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainScore {

    private StarRatingDomain domain;

    /**
     * List of individual measure scores in this domain
     */
    private List<MeasureScore> measureScores;

    /**
     * Weighted average star rating for this domain (1-5)
     */
    private double domainStars;

    /**
     * Domain weight in overall calculation
     */
    private double domainWeight;

    /**
     * Number of measures in this domain
     */
    private int measureCount;

    /**
     * Average performance rate across all measures in domain
     */
    private double averagePerformanceRate;

    /**
     * Year-over-year improvement for this domain
     */
    private Double domainImprovement;
}
