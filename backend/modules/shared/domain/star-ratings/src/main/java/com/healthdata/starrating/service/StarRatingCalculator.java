package com.healthdata.starrating.service;

import com.healthdata.starrating.domain.DomainScore;
import com.healthdata.starrating.domain.MeasureScore;
import com.healthdata.starrating.domain.StarRatingDomain;
import com.healthdata.starrating.domain.StarRatingMeasure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StarRatingCalculator {

    private static final Map<StarRatingMeasure, double[]> DEFAULT_CUT_POINTS = new HashMap<>();

    static {
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.BREAST_CANCER_SCREENING, new double[]{0.55, 0.62, 0.68, 0.72, 0.76});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.COLORECTAL_CANCER_SCREENING, new double[]{0.50, 0.58, 0.64, 0.70, 0.75});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.DIABETES_CARE_EYE_EXAM, new double[]{0.45, 0.52, 0.58, 0.64, 0.70});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.ANNUAL_FLU_VACCINE, new double[]{0.55, 0.62, 0.68, 0.73, 0.78});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE, new double[]{0.55, 0.60, 0.65, 0.70, 0.75});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.DIABETES_CARE_HBA1C_POOR_CONTROL, new double[]{0.70, 0.75, 0.80, 0.85, 0.90});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.DIABETES_CARE_HBA1C_CONTROL, new double[]{0.50, 0.58, 0.65, 0.72, 0.78});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.DIABETES_CARE_BP_CONTROL, new double[]{0.52, 0.60, 0.66, 0.72, 0.77});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.STATIN_THERAPY_FOR_CVD, new double[]{0.68, 0.74, 0.78, 0.82, 0.86});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.MEDICATION_ADHERENCE_DIABETES, new double[]{0.72, 0.77, 0.81, 0.84, 0.87});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.MEDICATION_ADHERENCE_HYPERTENSION, new double[]{0.74, 0.79, 0.82, 0.85, 0.88});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.MEDICATION_ADHERENCE_CHOLESTEROL, new double[]{0.72, 0.77, 0.81, 0.84, 0.87});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.KIDNEY_HEALTH_FOR_DIABETES, new double[]{0.65, 0.72, 0.78, 0.83, 0.87});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.HIGH_RISK_MEDICATION, new double[]{0.70, 0.76, 0.81, 0.85, 0.89});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.MEDICATION_RECONCILIATION, new double[]{0.45, 0.52, 0.58, 0.64, 0.70});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.STATIN_USE_IN_DIABETES, new double[]{0.68, 0.74, 0.78, 0.82, 0.86});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.GETTING_NEEDED_CARE, new double[]{0.70, 0.76, 0.81, 0.85, 0.89});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.GETTING_APPOINTMENTS_AND_CARE_QUICKLY, new double[]{0.68, 0.74, 0.79, 0.83, 0.87});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.CUSTOMER_SERVICE, new double[]{0.75, 0.81, 0.85, 0.88, 0.91});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.RATING_OF_HEALTH_CARE_QUALITY, new double[]{0.72, 0.78, 0.83, 0.86, 0.89});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.RATING_OF_HEALTH_PLAN, new double[]{0.70, 0.76, 0.81, 0.85, 0.88});
        DEFAULT_CUT_POINTS.put(StarRatingMeasure.PLAN_ALL_CAUSE_READMISSIONS, new double[]{0.20, 0.18, 0.16, 0.14, 0.12});
    }

    public int calculateStarsForMeasure(double performanceRate, double[] cutPoints) {
        boolean inverted = cutPoints[0] > cutPoints[4];
        if (inverted) {
            if (performanceRate <= cutPoints[4]) return 5;
            if (performanceRate <= cutPoints[3]) return 4;
            if (performanceRate <= cutPoints[2]) return 3;
            if (performanceRate <= cutPoints[1]) return 2;
            return 1;
        }

        if (performanceRate >= cutPoints[4]) return 5;
        if (performanceRate >= cutPoints[3]) return 4;
        if (performanceRate >= cutPoints[2]) return 3;
        if (performanceRate >= cutPoints[1]) return 2;
        return 1;
    }

    public MeasureScore calculateMeasureScore(StarRatingMeasure measure, int numerator, int denominator) {
        double performanceRate = denominator > 0 ? (double) numerator / denominator : 0.0;
        double[] cutPoints = DEFAULT_CUT_POINTS.getOrDefault(measure, new double[]{0.50, 0.60, 0.70, 0.80, 0.90});

        return MeasureScore.builder()
            .measure(measure)
            .performanceRate(performanceRate)
            .stars(calculateStarsForMeasure(performanceRate, cutPoints))
            .numerator(numerator)
            .denominator(denominator)
            .weight(measure.getWeight())
            .cutPoints(cutPoints)
            .build();
    }

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

        double totalWeightedStars = 0.0;
        double totalWeight = 0.0;
        double totalPerformanceRate = 0.0;

        for (MeasureScore score : measureScores) {
            totalWeightedStars += score.getStars() * score.getWeight();
            totalWeight += score.getWeight();
            totalPerformanceRate += score.getPerformanceRate();
        }

        return DomainScore.builder()
            .domain(domain)
            .measureScores(measureScores)
            .domainStars(totalWeight > 0 ? totalWeightedStars / totalWeight : 0.0)
            .domainWeight(domain.getDomainWeight())
            .measureCount(measureScores.size())
            .averagePerformanceRate(totalPerformanceRate / measureScores.size())
            .build();
    }

    public double calculateOverallStarRating(List<DomainScore> domainScores) {
        if (domainScores.isEmpty()) {
            return 0.0;
        }

        double totalWeightedStars = 0.0;
        double totalWeight = 0.0;
        for (DomainScore score : domainScores) {
            totalWeightedStars += score.getDomainStars() * score.getDomainWeight();
            totalWeight += score.getDomainWeight();
        }
        return Math.min(totalWeight > 0 ? totalWeightedStars / totalWeight : 0.0, 5.0);
    }

    public double roundToHalfStar(double rating) {
        return Math.round(rating * 2.0) / 2.0;
    }
}
