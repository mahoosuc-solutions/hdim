package com.healthdata.payer.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Score for an individual HEDIS measure in the context of Star Ratings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasureScore {

    private StarRatingMeasure measure;

    /**
     * Raw performance rate (e.g., 0.85 for 85%)
     */
    private double performanceRate;

    /**
     * Star rating for this individual measure (1-5)
     */
    private int stars;

    /**
     * Numerator (number of patients meeting the measure)
     */
    private int numerator;

    /**
     * Denominator (total eligible patients)
     */
    private int denominator;

    /**
     * Weight of this measure in domain calculation
     */
    private double weight;

    /**
     * CMS cut points for this measure (5 values for 2-star through 5-star thresholds)
     */
    private double[] cutPoints;

    /**
     * Whether this measure is a reward measure (higher weight in improvement calculation)
     */
    private boolean rewardMeasure;

    /**
     * Prior year performance rate for improvement tracking
     */
    private Double priorYearRate;

    /**
     * Year-over-year improvement (positive = improvement)
     */
    private Double improvement;
}
