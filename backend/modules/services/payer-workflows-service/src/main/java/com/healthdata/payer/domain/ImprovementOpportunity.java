package com.healthdata.payer.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an opportunity to improve Star Rating through better measure performance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImprovementOpportunity {

    private StarRatingMeasure measure;

    /**
     * Current performance rate
     */
    private double currentRate;

    /**
     * Current star rating for this measure
     */
    private int currentStars;

    /**
     * Performance rate needed to achieve next star level
     */
    private double nextStarThreshold;

    /**
     * Next achievable star rating
     */
    private int nextStars;

    /**
     * Gap between current and next threshold
     */
    private double performanceGap;

    /**
     * Number of additional patients needed to meet next threshold
     */
    private int patientsNeeded;

    /**
     * Potential impact on overall Star Rating if improved
     */
    private double potentialImpact;

    /**
     * Priority level (HIGH, MEDIUM, LOW) based on weight and gap
     */
    private ImprovementPriority priority;

    /**
     * Estimated effort to achieve improvement (LOW, MEDIUM, HIGH)
     */
    private EffortLevel estimatedEffort;

    /**
     * Return on Investment score (impact / effort)
     */
    private double roiScore;

    public enum ImprovementPriority {
        HIGH, MEDIUM, LOW
    }

    public enum EffortLevel {
        LOW, MEDIUM, HIGH
    }
}
