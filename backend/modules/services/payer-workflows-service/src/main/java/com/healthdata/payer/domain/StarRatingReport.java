package com.healthdata.payer.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive Star Rating report for a Medicare Advantage plan.
 * Contains overall rating, domain scores, and improvement opportunities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StarRatingReport {

    /**
     * Medicare Advantage plan identifier
     */
    private String planId;

    /**
     * Plan name
     */
    private String planName;

    /**
     * Contract number (CMS contract ID)
     */
    private String contractNumber;

    /**
     * Reporting year (measurement year)
     */
    private int reportingYear;

    /**
     * Overall Star Rating (1-5, can be half-star increments like 3.5)
     */
    private double overallStarRating;

    /**
     * Rounded overall Star Rating (1-5 stars)
     */
    private int roundedStarRating;

    /**
     * Domain-level scores
     */
    private List<DomainScore> domainScores;

    /**
     * All individual measure scores
     */
    private List<MeasureScore> allMeasureScores;

    /**
     * Total number of enrollees in the plan
     */
    private int totalEnrollees;

    /**
     * Prior year overall Star Rating for comparison
     */
    private Double priorYearStarRating;

    /**
     * Year-over-year improvement in overall rating
     */
    private Double overallImprovement;

    /**
     * Improvement opportunities - measures where performance can be improved
     */
    private List<ImprovementOpportunity> improvementOpportunities;

    /**
     * Quality Bonus Payment (QBP) eligibility
     * Plans with 4+ stars qualify for bonus payments
     */
    private boolean qualityBonusPaymentEligible;

    /**
     * Estimated bonus payment percentage (0%, 3%, 5%)
     */
    private double bonusPaymentPercentage;

    /**
     * Report generation timestamp
     */
    private LocalDateTime generatedAt;

    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;

    /**
     * Cut-Point Year used for calculations (CMS publishes annually)
     */
    private int cutPointYear;

    /**
     * Check if plan qualifies for Quality Bonus Payment (4+ stars)
     */
    public boolean isQualityBonusPaymentEligible() {
        return roundedStarRating >= 4;
    }

    /**
     * Calculate estimated bonus payment percentage based on star rating
     */
    public double calculateBonusPaymentPercentage() {
        if (roundedStarRating >= 5) {
            return 5.0;
        } else if (roundedStarRating >= 4) {
            return 3.0;
        }
        return 0.0;
    }
}
