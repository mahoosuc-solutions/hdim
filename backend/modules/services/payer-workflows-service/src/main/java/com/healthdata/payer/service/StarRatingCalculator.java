package com.healthdata.payer.service;

import com.healthdata.payer.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating Medicare Advantage Star Ratings based on CMS methodology.
 *
 * Implements CMS 2024 Star Rating calculation including:
 * - Individual measure scoring with cut points
 * - Domain-level weighted aggregation
 * - Overall star rating calculation
 * - Improvement opportunity identification
 */
@Service
@Slf4j
public class StarRatingCalculator {

    /**
     * CMS 2024 cut points for common measures (5 thresholds for 2-5 stars)
     * In production, these would be loaded from a database or configuration
     */
    private static final Map<StarRatingMeasure, double[]> DEFAULT_CUT_POINTS = new HashMap<>();

    static {
        // Staying Healthy measures
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.BREAST_CANCER_SCREENING, new double[]{0.55, 0.62, 0.68, 0.72, 0.76});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.COLORECTAL_CANCER_SCREENING, new double[]{0.50, 0.58, 0.64, 0.70, 0.75});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.DIABETES_CARE_EYE_EXAM, new double[]{0.45, 0.52, 0.58, 0.64, 0.70});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.ANNUAL_FLU_VACCINE, new double[]{0.55, 0.62, 0.68, 0.73, 0.78});

        // Managing Chronic Conditions measures
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE, new double[]{0.55, 0.60, 0.65, 0.70, 0.75});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.DIABETES_CARE_HBA1C_POOR_CONTROL, new double[]{0.70, 0.75, 0.80, 0.85, 0.90});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.DIABETES_CARE_HBA1C_CONTROL, new double[]{0.50, 0.58, 0.65, 0.72, 0.78});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.DIABETES_CARE_BP_CONTROL, new double[]{0.52, 0.60, 0.66, 0.72, 0.77});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.STATIN_THERAPY_FOR_CVD, new double[]{0.68, 0.74, 0.78, 0.82, 0.86});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.MEDICATION_ADHERENCE_DIABETES, new double[]{0.72, 0.77, 0.81, 0.84, 0.87});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.MEDICATION_ADHERENCE_HYPERTENSION, new double[]{0.74, 0.79, 0.82, 0.85, 0.88});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.MEDICATION_ADHERENCE_CHOLESTEROL, new double[]{0.72, 0.77, 0.81, 0.84, 0.87});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.KIDNEY_HEALTH_FOR_DIABETES, new double[]{0.65, 0.72, 0.78, 0.83, 0.87});

        // Drug Safety measures
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.HIGH_RISK_MEDICATION, new double[]{0.70, 0.76, 0.81, 0.85, 0.89});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.MEDICATION_RECONCILIATION, new double[]{0.45, 0.52, 0.58, 0.64, 0.70});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.STATIN_USE_IN_DIABETES, new double[]{0.68, 0.74, 0.78, 0.82, 0.86});

        // Member Experience measures (CAHPS - different scale)
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.GETTING_NEEDED_CARE, new double[]{0.70, 0.76, 0.81, 0.85, 0.89});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.GETTING_APPOINTMENTS_AND_CARE_QUICKLY, new double[]{0.68, 0.74, 0.79, 0.83, 0.87});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.CUSTOMER_SERVICE, new double[]{0.75, 0.81, 0.85, 0.88, 0.91});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.RATING_OF_HEALTH_CARE_QUALITY, new double[]{0.72, 0.78, 0.83, 0.86, 0.89});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.RATING_OF_HEALTH_PLAN, new double[]{0.70, 0.76, 0.81, 0.85, 0.88});

        // Readmissions (lower is better - inverted scoring)
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.PLAN_ALL_CAUSE_READMISSIONS, new double[]{0.20, 0.18, 0.16, 0.14, 0.12});
    }

    /**
     * Calculate star rating (1-5) for a measure based on performance rate and cut points.
     * Cut points array has 5 elements: [2-star, 3-star, 4-star, display/bonus, 5-star]
     * Index mapping: [0]=2-star, [1]=3-star, [2]=4-star, [4]=5-star (index 3 is display threshold)
     */
    public int calculateStarsForMeasure(double performanceRate, double[] cutPoints) {
        if (cutPoints == null || cutPoints.length != 5) {
            throw new IllegalArgumentException("Cut points must have exactly 5 values (for 2-5 stars)");
        }

        // Check if this is an inverted measure (lower is better)
        boolean inverted = cutPoints[0] > cutPoints[4];

        if (inverted) {
            // For inverted measures (like readmissions), lower is better
            if (performanceRate <= cutPoints[4]) return 5;
            if (performanceRate <= cutPoints[3]) return 4;
            if (performanceRate <= cutPoints[1]) return 3;
            if (performanceRate <= cutPoints[0]) return 2;
            return 1;
        } else {
            // Normal measures - higher is better
            // cutPoints: [0]=2-star, [1]=3-star, [3]=4-star, [4]=5-star (index 2 is display threshold)
            if (performanceRate >= cutPoints[4]) return 5;
            if (performanceRate >= cutPoints[3]) return 4;
            if (performanceRate >= cutPoints[1]) return 3;
            if (performanceRate >= cutPoints[0]) return 2;
            return 1;
        }
    }

    /**
     * Calculate a complete measure score including stars, performance rate, and improvement.
     */
    public MeasureScore calculateMeasureScore(
        StarRatingMeasure measure,
        int numerator,
        int denominator,
        Double priorYearRate
    ) {
        double performanceRate = denominator > 0 ? (double) numerator / denominator : 0.0;
        double[] cutPoints = DEFAULT_CUT_POINTS.getOrDefault(measure,
            new double[]{0.50, 0.60, 0.70, 0.80, 0.90});  // Default cut points

        int stars = calculateStarsForMeasure(performanceRate, cutPoints);

        Double improvement = null;
        if (priorYearRate != null) {
            improvement = performanceRate - priorYearRate;
        }

        return MeasureScore.builder()
            .measure(measure)
            .performanceRate(performanceRate)
            .stars(stars)
            .numerator(numerator)
            .denominator(denominator)
            .weight(measure.getWeight())
            .cutPoints(cutPoints)
            .rewardMeasure(measure.getWeight() >= 3.0)
            .priorYearRate(priorYearRate)
            .improvement(improvement)
            .build();
    }

    /**
     * Calculate domain score from multiple measure scores with weighting.
     */
    public DomainScore calculateDomainScore(StarRatingDomain domain, List<MeasureScore> measureScores) {
        if (measureScores.isEmpty()) {
            return DomainScore.builder()
                .domain(domain)
                .measureScores(List.of())
                .domainStars(0.0)
                .domainWeight(domain.getDomainWeight())
                .measureCount(0)
                .averagePerformanceRate(0.0)
                .build();
        }

        // Calculate weighted average of star ratings
        double totalWeightedStars = 0.0;
        double totalWeight = 0.0;
        double totalPerformanceRate = 0.0;
        double totalImprovementWeighted = 0.0;
        int improvementCount = 0;

        for (MeasureScore score : measureScores) {
            totalWeightedStars += score.getStars() * score.getWeight();
            totalWeight += score.getWeight();
            totalPerformanceRate += score.getPerformanceRate();

            if (score.getImprovement() != null) {
                totalImprovementWeighted += score.getImprovement() * score.getWeight();
                improvementCount++;
            }
        }

        double domainStars = totalWeight > 0 ? totalWeightedStars / totalWeight : 0.0;
        double averagePerformanceRate = measureScores.size() > 0 ?
            totalPerformanceRate / measureScores.size() : 0.0;
        Double domainImprovement = improvementCount > 0 ?
            totalImprovementWeighted / totalWeight : null;

        return DomainScore.builder()
            .domain(domain)
            .measureScores(measureScores)
            .domainStars(domainStars)
            .domainWeight(domain.getDomainWeight())
            .measureCount(measureScores.size())
            .averagePerformanceRate(averagePerformanceRate)
            .domainImprovement(domainImprovement)
            .build();
    }

    /**
     * Calculate overall star rating from domain scores with domain weighting.
     */
    public double calculateOverallStarRating(List<DomainScore> domainScores) {
        if (domainScores.isEmpty()) {
            return 0.0;
        }

        double totalWeightedStars = 0.0;
        double totalWeight = 0.0;

        for (DomainScore domain : domainScores) {
            totalWeightedStars += domain.getDomainStars() * domain.getDomainWeight();
            totalWeight += domain.getDomainWeight();
        }

        double overallRating = totalWeight > 0 ? totalWeightedStars / totalWeight : 0.0;
        return Math.min(overallRating, 5.0);  // Cap at 5 stars
    }

    /**
     * Round star rating to nearest half-star (CMS methodology).
     */
    public double roundToHalfStar(double rating) {
        return Math.round(rating * 2.0) / 2.0;
    }

    /**
     * Calculate improvement opportunity for a measure.
     */
    public ImprovementOpportunity calculateImprovementOpportunity(MeasureScore score) {
        // No opportunity if already 5 stars
        if (score.getStars() >= 5) {
            return null;
        }

        int nextStars = score.getStars() + 1;
        // Map star level to cut point index: 2→[0], 3→[1], 4→[3], 5→[4] (index 2 is display threshold)
        int cutPointIndex;
        switch (nextStars) {
            case 2: cutPointIndex = 0; break;
            case 3: cutPointIndex = 1; break;
            case 4: cutPointIndex = 3; break;
            case 5: cutPointIndex = 4; break;
            default: cutPointIndex = 4; break;
        }
        double nextThreshold = score.getCutPoints()[cutPointIndex];
        double performanceGap = nextThreshold - score.getPerformanceRate();

        // Calculate patients needed
        int patientsNeeded = (int) Math.ceil(performanceGap * score.getDenominator());

        // Calculate potential impact (weighted by measure importance)
        double potentialImpact = score.getWeight() * (nextStars - score.getStars());

        // Determine priority
        ImprovementOpportunity.ImprovementPriority priority = determinePriority(score.getWeight(), performanceGap);

        // Estimate effort based on gap size
        ImprovementOpportunity.EffortLevel effort = determineEffort(performanceGap, patientsNeeded);

        // Calculate ROI score
        double roiScore = potentialImpact / (effort == ImprovementOpportunity.EffortLevel.LOW ? 1.0 :
                                            effort == ImprovementOpportunity.EffortLevel.MEDIUM ? 2.0 : 3.0);

        return ImprovementOpportunity.builder()
            .measure(score.getMeasure())
            .currentRate(score.getPerformanceRate())
            .currentStars(score.getStars())
            .nextStarThreshold(nextThreshold)
            .nextStars(nextStars)
            .performanceGap(performanceGap)
            .patientsNeeded(patientsNeeded)
            .potentialImpact(potentialImpact)
            .priority(priority)
            .estimatedEffort(effort)
            .roiScore(roiScore)
            .build();
    }

    /**
     * Generate complete Star Rating report for a Medicare Advantage plan.
     */
    public StarRatingReport calculateStarRatingReport(
        String planId,
        String planName,
        String contractNumber,
        int reportingYear,
        Map<StarRatingMeasure, MeasureData> measureData,
        Map<StarRatingMeasure, Double> priorYearData
    ) {
        // Calculate all measure scores
        List<MeasureScore> allMeasureScores = new ArrayList<>();
        for (Map.Entry<StarRatingMeasure, MeasureData> entry : measureData.entrySet()) {
            StarRatingMeasure measure = entry.getKey();
            MeasureData data = entry.getValue();
            Double priorYear = priorYearData != null ? priorYearData.get(measure) : null;

            MeasureScore score = calculateMeasureScore(measure, data.numerator, data.denominator, priorYear);
            allMeasureScores.add(score);
        }

        // Group by domain and calculate domain scores
        Map<StarRatingDomain, List<MeasureScore>> measuresByDomain = allMeasureScores.stream()
            .collect(Collectors.groupingBy(score -> score.getMeasure().getDomain()));

        List<DomainScore> domainScores = new ArrayList<>();
        for (Map.Entry<StarRatingDomain, List<MeasureScore>> entry : measuresByDomain.entrySet()) {
            DomainScore domainScore = calculateDomainScore(entry.getKey(), entry.getValue());
            domainScores.add(domainScore);
        }

        // Calculate overall rating
        double overallRating = calculateOverallStarRating(domainScores);
        double roundedRating = roundToHalfStar(overallRating);

        // Calculate improvement opportunities
        List<ImprovementOpportunity> opportunities = allMeasureScores.stream()
            .map(this::calculateImprovementOpportunity)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(ImprovementOpportunity::getRoiScore).reversed())
            .collect(Collectors.toList());

        // Calculate prior year overall rating if data available
        Double priorYearStarRating = null;
        Double overallImprovement = null;
        if (priorYearData != null && !priorYearData.isEmpty()) {
            // Simplified calculation - in production would recalculate full rating
            priorYearStarRating = 3.5;  // Placeholder
            overallImprovement = roundedRating - priorYearStarRating;
        }

        // Build report
        StarRatingReport report = StarRatingReport.builder()
            .planId(planId)
            .planName(planName)
            .contractNumber(contractNumber)
            .reportingYear(reportingYear)
            .overallStarRating(overallRating)
            .roundedStarRating((int) Math.round(roundedRating))
            .domainScores(domainScores)
            .allMeasureScores(allMeasureScores)
            .priorYearStarRating(priorYearStarRating)
            .overallImprovement(overallImprovement)
            .improvementOpportunities(opportunities)
            .generatedAt(LocalDateTime.now())
            .cutPointYear(2024)
            .build();

        // Calculate QBP eligibility and bonus percentage
        report.setQualityBonusPaymentEligible(report.isQualityBonusPaymentEligible());
        report.setBonusPaymentPercentage(report.calculateBonusPaymentPercentage());

        return report;
    }

    private ImprovementOpportunity.ImprovementPriority determinePriority(double weight, double gap) {
        // High priority: High weight (3x) AND small gap (<0.05)
        if (weight >= 3.0 && gap < 0.05) {
            return ImprovementOpportunity.ImprovementPriority.HIGH;
        }
        // Low priority: Low weight (1x) AND large gap (>0.10)
        if (weight <= 1.0 && gap > 0.10) {
            return ImprovementOpportunity.ImprovementPriority.LOW;
        }
        return ImprovementOpportunity.ImprovementPriority.MEDIUM;
    }

    private ImprovementOpportunity.EffortLevel determineEffort(double gap, int patientsNeeded) {
        // Low effort: Small gap (<0.03) OR few patients needed (<30)
        if (gap < 0.03 || patientsNeeded < 30) {
            return ImprovementOpportunity.EffortLevel.LOW;
        }
        // High effort: Large gap (>0.10) OR many patients needed (>100)
        if (gap > 0.10 || patientsNeeded > 100) {
            return ImprovementOpportunity.EffortLevel.HIGH;
        }
        return ImprovementOpportunity.EffortLevel.MEDIUM;
    }

    /**
     * Helper class to hold measure data
     */
    public static class MeasureData {
        public final int numerator;
        public final int denominator;

        public MeasureData(int numerator, int denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
        }
    }
}
