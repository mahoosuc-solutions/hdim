package com.healthdata.payer.service;

import com.healthdata.payer.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for StarRatingCalculator - Medicare Advantage Star Ratings calculation.
 *
 * Tests cover:
 * - Individual measure scoring with CMS cut points
 * - Domain-level weighted calculations
 * - Overall star rating calculation
 * - Improvement tracking
 * - Edge cases and validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Star Rating Calculator Tests")
class StarRatingCalculatorTest {

    private StarRatingCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new StarRatingCalculator();
    }

    // ==================== Individual Measure Scoring Tests ====================

    @Test
    @DisplayName("Should calculate 5 stars for measure performance above 5-star cut point")
    void shouldCalculateFiveStarsForHighPerformance() {
        // Given: Performance rate of 85% for CBP with cut points [0.55, 0.60, 0.65, 0.70, 0.75]
        double performanceRate = 0.85;
        double[] cutPoints = {0.55, 0.60, 0.65, 0.70, 0.75};

        // When
        int stars = calculator.calculateStarsForMeasure(performanceRate, cutPoints);

        // Then
        assertThat(stars).isEqualTo(5);
    }

    @Test
    @DisplayName("Should calculate 4 stars for performance between 4-star and 5-star cut points")
    void shouldCalculateFourStars() {
        // Given
        double performanceRate = 0.72;
        double[] cutPoints = {0.55, 0.60, 0.65, 0.70, 0.75};

        // When
        int stars = calculator.calculateStarsForMeasure(performanceRate, cutPoints);

        // Then
        assertThat(stars).isEqualTo(4);
    }

    @Test
    @DisplayName("Should calculate 3 stars for mid-range performance")
    void shouldCalculateThreeStars() {
        // Given
        double performanceRate = 0.62;
        double[] cutPoints = {0.55, 0.60, 0.65, 0.70, 0.75};

        // When
        int stars = calculator.calculateStarsForMeasure(performanceRate, cutPoints);

        // Then
        assertThat(stars).isEqualTo(3);
    }

    @Test
    @DisplayName("Should calculate 2 stars for performance between 2-star and 3-star cut points")
    void shouldCalculateTwoStars() {
        // Given
        double performanceRate = 0.57;
        double[] cutPoints = {0.55, 0.60, 0.65, 0.70, 0.75};

        // When
        int stars = calculator.calculateStarsForMeasure(performanceRate, cutPoints);

        // Then
        assertThat(stars).isEqualTo(2);
    }

    @Test
    @DisplayName("Should calculate 1 star for performance below 2-star cut point")
    void shouldCalculateOneStarForLowPerformance() {
        // Given
        double performanceRate = 0.50;
        double[] cutPoints = {0.55, 0.60, 0.65, 0.70, 0.75};

        // When
        int stars = calculator.calculateStarsForMeasure(performanceRate, cutPoints);

        // Then
        assertThat(stars).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle performance exactly at cut point threshold")
    void shouldHandleExactCutPointMatch() {
        // Given
        double performanceRate = 0.70;
        double[] cutPoints = {0.55, 0.60, 0.65, 0.70, 0.75};

        // When
        int stars = calculator.calculateStarsForMeasure(performanceRate, cutPoints);

        // Then - At threshold should get the star level
        assertThat(stars).isEqualTo(4);
    }

    @Test
    @DisplayName("Should calculate measure score with all components")
    void shouldCalculateCompleteMeasureScore() {
        // Given
        int numerator = 850;
        int denominator = 1000;
        StarRatingMeasure measure = StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE;

        // When
        MeasureScore score = calculator.calculateMeasureScore(measure, numerator, denominator, null);

        // Then
        assertThat(score.getMeasure()).isEqualTo(measure);
        assertThat(score.getPerformanceRate()).isEqualTo(0.85);
        assertThat(score.getNumerator()).isEqualTo(850);
        assertThat(score.getDenominator()).isEqualTo(1000);
        assertThat(score.getWeight()).isEqualTo(measure.getWeight());
        assertThat(score.getStars()).isGreaterThan(0).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Should handle zero denominator gracefully")
    void shouldHandleZeroDenominator() {
        // Given
        int numerator = 0;
        int denominator = 0;
        StarRatingMeasure measure = StarRatingMeasure.BREAST_CANCER_SCREENING;

        // When
        MeasureScore score = calculator.calculateMeasureScore(measure, numerator, denominator, null);

        // Then
        assertThat(score.getPerformanceRate()).isEqualTo(0.0);
        assertThat(score.getStars()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should calculate improvement when prior year data available")
    void shouldCalculateImprovement() {
        // Given
        int numerator = 850;
        int denominator = 1000;
        Double priorYearRate = 0.80;
        StarRatingMeasure measure = StarRatingMeasure.DIABETES_CARE_HBA1C_POOR_CONTROL;

        // When
        MeasureScore score = calculator.calculateMeasureScore(measure, numerator, denominator, priorYearRate);

        // Then
        assertThat(score.getImprovement()).isNotNull();
        assertThat(score.getImprovement()).isEqualTo(0.05);  // 85% - 80%
    }

    // ==================== Domain Scoring Tests ====================

    @Test
    @DisplayName("Should calculate domain score from multiple measures")
    void shouldCalculateDomainScore() {
        // Given
        MeasureScore score1 = MeasureScore.builder()
            .measure(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE)
            .performanceRate(0.85)
            .stars(5)
            .weight(3.0)
            .build();

        MeasureScore score2 = MeasureScore.builder()
            .measure(StarRatingMeasure.DIABETES_CARE_HBA1C_POOR_CONTROL)
            .performanceRate(0.75)
            .stars(4)
            .weight(3.0)
            .build();

        List<MeasureScore> scores = List.of(score1, score2);

        // When
        DomainScore domainScore = calculator.calculateDomainScore(
            StarRatingDomain.MANAGING_CHRONIC_CONDITIONS,
            scores
        );

        // Then
        assertThat(domainScore.getDomain()).isEqualTo(StarRatingDomain.MANAGING_CHRONIC_CONDITIONS);
        assertThat(domainScore.getMeasureCount()).isEqualTo(2);
        assertThat(domainScore.getDomainStars()).isBetween(4.0, 5.0);
        assertThat(domainScore.getAveragePerformanceRate()).isEqualTo(0.80);
    }

    @Test
    @DisplayName("Should apply measure weights correctly in domain calculation")
    void shouldApplyMeasureWeights() {
        // Given: One 3x weighted measure at 5 stars, one 1x weighted measure at 3 stars
        MeasureScore highWeightScore = MeasureScore.builder()
            .measure(StarRatingMeasure.COLORECTAL_CANCER_SCREENING)
            .stars(5)
            .weight(3.0)
            .build();

        MeasureScore lowWeightScore = MeasureScore.builder()
            .measure(StarRatingMeasure.BREAST_CANCER_SCREENING)
            .stars(3)
            .weight(1.0)
            .build();

        List<MeasureScore> scores = List.of(highWeightScore, lowWeightScore);

        // When
        DomainScore domainScore = calculator.calculateDomainScore(
            StarRatingDomain.STAYING_HEALTHY,
            scores
        );

        // Then: Weighted average should be closer to 5 than 3
        // (5 * 3.0 + 3 * 1.0) / (3.0 + 1.0) = 18 / 4 = 4.5
        assertThat(domainScore.getDomainStars()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("Should handle empty measure list for domain")
    void shouldHandleEmptyDomain() {
        // Given
        List<MeasureScore> scores = List.of();

        // When
        DomainScore domainScore = calculator.calculateDomainScore(
            StarRatingDomain.DRUG_PLAN,
            scores
        );

        // Then
        assertThat(domainScore.getMeasureCount()).isEqualTo(0);
        assertThat(domainScore.getDomainStars()).isEqualTo(0.0);
        assertThat(domainScore.getAveragePerformanceRate()).isEqualTo(0.0);
    }

    // ==================== Overall Star Rating Tests ====================

    @Test
    @DisplayName("Should calculate overall star rating from domain scores")
    void shouldCalculateOverallStarRating() {
        // Given
        DomainScore domain1 = DomainScore.builder()
            .domain(StarRatingDomain.STAYING_HEALTHY)
            .domainStars(4.5)
            .domainWeight(1.0)
            .build();

        DomainScore domain2 = DomainScore.builder()
            .domain(StarRatingDomain.MANAGING_CHRONIC_CONDITIONS)
            .domainStars(3.5)
            .domainWeight(1.5)
            .build();

        List<DomainScore> domainScores = List.of(domain1, domain2);

        // When
        double overallRating = calculator.calculateOverallStarRating(domainScores);

        // Then: (4.5 * 1.0 + 3.5 * 1.5) / (1.0 + 1.5) = 9.75 / 2.5 = 3.9
        assertThat(overallRating).isBetween(3.8, 4.0);
    }

    @Test
    @DisplayName("Should round overall star rating to nearest half-star")
    void shouldRoundToNearestHalfStar() {
        // Given
        DomainScore domain = DomainScore.builder()
            .domain(StarRatingDomain.STAYING_HEALTHY)
            .domainStars(3.73)
            .domainWeight(1.0)
            .build();

        // When
        double rounded = calculator.roundToHalfStar(3.73);

        // Then
        assertThat(rounded).isEqualTo(3.5);
    }

    @Test
    @DisplayName("Should round up to next half-star when appropriate")
    void shouldRoundUpToNextHalfStar() {
        // Given/When
        double rounded = calculator.roundToHalfStar(3.76);

        // Then
        assertThat(rounded).isEqualTo(4.0);
    }

    @Test
    @DisplayName("Should not exceed 5 stars in overall rating")
    void shouldCapAt5Stars() {
        // Given
        DomainScore domain = DomainScore.builder()
            .domain(StarRatingDomain.STAYING_HEALTHY)
            .domainStars(5.0)
            .domainWeight(1.0)
            .build();

        // When
        double overallRating = calculator.calculateOverallStarRating(List.of(domain));

        // Then
        assertThat(overallRating).isLessThanOrEqualTo(5.0);
    }

    // ==================== Full Star Rating Report Tests ====================

    @Test
    @DisplayName("Should generate complete star rating report")
    void shouldGenerateCompleteStarRatingReport() {
        // Given
        String planId = "H1234-001";
        Map<StarRatingMeasure, MeasureData> measureData = createSampleMeasureData();

        // When
        StarRatingReport report = calculator.calculateStarRatingReport(
            planId,
            "Sample Medicare Advantage Plan",
            "H1234",
            2024,
            measureData,
            null
        );

        // Then
        assertThat(report).isNotNull();
        assertThat(report.getPlanId()).isEqualTo(planId);
        assertThat(report.getReportingYear()).isEqualTo(2024);
        assertThat(report.getOverallStarRating()).isBetween(1.0, 5.0);
        assertThat(report.getRoundedStarRating()).isBetween(1, 5);
        assertThat(report.getDomainScores()).isNotEmpty();
        assertThat(report.getAllMeasureScores()).isNotEmpty();
        assertThat(report.getGeneratedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should set Quality Bonus Payment eligibility for 4+ stars")
    void shouldSetQualityBonusPaymentEligibility() {
        // Given
        String planId = "H5678-002";
        Map<StarRatingMeasure, MeasureData> measureData = createHighPerformanceMeasureData();

        // When
        StarRatingReport report = calculator.calculateStarRatingReport(
            planId,
            "High Performance Plan",
            "H5678",
            2024,
            measureData,
            null
        );

        // Then
        assertThat(report.getRoundedStarRating()).isGreaterThanOrEqualTo(4);
        assertThat(report.isQualityBonusPaymentEligible()).isTrue();
        assertThat(report.getBonusPaymentPercentage()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should calculate 5% bonus for 5-star plans")
    void shouldCalculateFivePercentBonusForFiveStars() {
        // Given
        String planId = "H9999-001";
        Map<StarRatingMeasure, MeasureData> measureData = createExcellentMeasureData();

        // When
        StarRatingReport report = calculator.calculateStarRatingReport(
            planId,
            "Excellent Plan",
            "H9999",
            2024,
            measureData,
            null
        );

        // Then
        assertThat(report.getRoundedStarRating()).isEqualTo(5);
        assertThat(report.getBonusPaymentPercentage()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should not set QBP eligibility for 3-star plans")
    void shouldNotSetQBPFor3StarPlans() {
        // Given
        String planId = "H3000-001";
        Map<StarRatingMeasure, MeasureData> measureData = createAverageMeasureData();

        // When
        StarRatingReport report = calculator.calculateStarRatingReport(
            planId,
            "Average Plan",
            "H3000",
            2024,
            measureData,
            null
        );

        // Then
        assertThat(report.getRoundedStarRating()).isLessThan(4);
        assertThat(report.isQualityBonusPaymentEligible()).isFalse();
        assertThat(report.getBonusPaymentPercentage()).isEqualTo(0.0);
    }

    // ==================== Improvement Opportunity Tests ====================

    @Test
    @DisplayName("Should identify improvement opportunities for measures below 5 stars")
    void shouldIdentifyImprovementOpportunities() {
        // Given
        String planId = "H4000-001";
        Map<StarRatingMeasure, MeasureData> measureData = createMixedPerformanceMeasureData();

        // When
        StarRatingReport report = calculator.calculateStarRatingReport(
            planId,
            "Mixed Performance Plan",
            "H4000",
            2024,
            measureData,
            null
        );

        // Then
        assertThat(report.getImprovementOpportunities()).isNotEmpty();
    }

    @Test
    @DisplayName("Should calculate patients needed for improvement")
    void shouldCalculatePatientsNeeded() {
        // Given
        MeasureScore score = MeasureScore.builder()
            .measure(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE)
            .performanceRate(0.68)
            .stars(3)
            .numerator(680)
            .denominator(1000)
            .cutPoints(new double[]{0.55, 0.60, 0.65, 0.70, 0.75})
            .build();

        // When
        ImprovementOpportunity opportunity = calculator.calculateImprovementOpportunity(score);

        // Then
        assertThat(opportunity.getCurrentStars()).isEqualTo(3);
        assertThat(opportunity.getNextStars()).isEqualTo(4);
        assertThat(opportunity.getNextStarThreshold()).isEqualTo(0.70);
        assertThat(opportunity.getPerformanceGap()).isEqualTo(0.02);
        assertThat(opportunity.getPatientsNeeded()).isEqualTo(20);  // Need 700 - 680 = 20 more
    }

    @Test
    @DisplayName("Should prioritize high-impact, low-effort improvements")
    void shouldPrioritizeImprovements() {
        // Given: 3x weighted measure close to threshold
        MeasureScore score = MeasureScore.builder()
            .measure(StarRatingMeasure.COLORECTAL_CANCER_SCREENING)  // 3x weight
            .performanceRate(0.68)
            .stars(3)
            .numerator(680)
            .denominator(1000)
            .weight(3.0)
            .cutPoints(new double[]{0.55, 0.60, 0.65, 0.70, 0.75})
            .build();

        // When
        ImprovementOpportunity opportunity = calculator.calculateImprovementOpportunity(score);

        // Then
        assertThat(opportunity.getPriority()).isEqualTo(ImprovementOpportunity.ImprovementPriority.HIGH);
        assertThat(opportunity.getRoiScore()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should not create improvement opportunity for 5-star measures")
    void shouldNotCreateOpportunityForFiveStarMeasures() {
        // Given
        MeasureScore score = MeasureScore.builder()
            .measure(StarRatingMeasure.BREAST_CANCER_SCREENING)
            .performanceRate(0.85)
            .stars(5)
            .numerator(850)
            .denominator(1000)
            .cutPoints(new double[]{0.55, 0.60, 0.65, 0.70, 0.75})
            .build();

        // When
        ImprovementOpportunity opportunity = calculator.calculateImprovementOpportunity(score);

        // Then
        assertThat(opportunity).isNull();
    }

    // ==================== Year-over-Year Improvement Tests ====================

    @Test
    @DisplayName("Should track year-over-year improvement")
    void shouldTrackYearOverYearImprovement() {
        // Given
        String planId = "H5000-001";
        Map<StarRatingMeasure, MeasureData> measureData = createImprovedMeasureData();
        Map<StarRatingMeasure, Double> priorYearData = createPriorYearData();

        // When
        StarRatingReport report = calculator.calculateStarRatingReport(
            planId,
            "Improving Plan",
            "H5000",
            2024,
            measureData,
            priorYearData
        );

        // Then
        assertThat(report.getPriorYearStarRating()).isNotNull();
        assertThat(report.getOverallImprovement()).isNotNull();
        assertThat(report.getOverallImprovement()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should identify measures with significant improvement")
    void shouldIdentifySignificantImprovement() {
        // Given
        StarRatingMeasure measure = StarRatingMeasure.DIABETES_CARE_HBA1C_POOR_CONTROL;
        int numerator = 850;
        int denominator = 1000;
        Double priorYearRate = 0.75;

        // When
        MeasureScore score = calculator.calculateMeasureScore(measure, numerator, denominator, priorYearRate);

        // Then
        assertThat(score.getImprovement()).isEqualTo(0.10);  // 85% - 75% = 10% improvement
    }

    // ==================== Helper Methods ====================

    private Map<StarRatingMeasure, MeasureData> createSampleMeasureData() {
        Map<StarRatingMeasure, MeasureData> data = new HashMap<>();
        data.put(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE, new MeasureData(750, 1000));
        data.put(StarRatingMeasure.BREAST_CANCER_SCREENING, new MeasureData(700, 1000));
        data.put(StarRatingMeasure.DIABETES_CARE_HBA1C_POOR_CONTROL, new MeasureData(800, 1000));
        data.put(StarRatingMeasure.COLORECTAL_CANCER_SCREENING, new MeasureData(650, 1000));
        return data;
    }

    private Map<StarRatingMeasure, MeasureData> createHighPerformanceMeasureData() {
        Map<StarRatingMeasure, MeasureData> data = new HashMap<>();
        data.put(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE, new MeasureData(820, 1000));
        data.put(StarRatingMeasure.BREAST_CANCER_SCREENING, new MeasureData(810, 1000));
        data.put(StarRatingMeasure.DIABETES_CARE_HBA1C_POOR_CONTROL, new MeasureData(850, 1000));
        data.put(StarRatingMeasure.COLORECTAL_CANCER_SCREENING, new MeasureData(800, 1000));
        return data;
    }

    private Map<StarRatingMeasure, MeasureData> createExcellentMeasureData() {
        Map<StarRatingMeasure, MeasureData> data = new HashMap<>();
        data.put(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE, new MeasureData(900, 1000));
        data.put(StarRatingMeasure.BREAST_CANCER_SCREENING, new MeasureData(920, 1000));
        data.put(StarRatingMeasure.DIABETES_CARE_HBA1C_POOR_CONTROL, new MeasureData(910, 1000));
        data.put(StarRatingMeasure.COLORECTAL_CANCER_SCREENING, new MeasureData(895, 1000));
        return data;
    }

    private Map<StarRatingMeasure, MeasureData> createAverageMeasureData() {
        Map<StarRatingMeasure, MeasureData> data = new HashMap<>();
        data.put(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE, new MeasureData(620, 1000));
        data.put(StarRatingMeasure.BREAST_CANCER_SCREENING, new MeasureData(630, 1000));
        data.put(StarRatingMeasure.DIABETES_CARE_HBA1C_POOR_CONTROL, new MeasureData(650, 1000));
        data.put(StarRatingMeasure.COLORECTAL_CANCER_SCREENING, new MeasureData(610, 1000));
        return data;
    }

    private Map<StarRatingMeasure, MeasureData> createMixedPerformanceMeasureData() {
        Map<StarRatingMeasure, MeasureData> data = new HashMap<>();
        data.put(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE, new MeasureData(850, 1000));  // High
        data.put(StarRatingMeasure.BREAST_CANCER_SCREENING, new MeasureData(650, 1000));    // Low
        data.put(StarRatingMeasure.DIABETES_CARE_HBA1C_POOR_CONTROL, new MeasureData(750, 1000));  // Medium
        return data;
    }

    private Map<StarRatingMeasure, MeasureData> createImprovedMeasureData() {
        Map<StarRatingMeasure, MeasureData> data = new HashMap<>();
        data.put(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE, new MeasureData(800, 1000));
        data.put(StarRatingMeasure.DIABETES_CARE_HBA1C_POOR_CONTROL, new MeasureData(850, 1000));
        return data;
    }

    private Map<StarRatingMeasure, Double> createPriorYearData() {
        Map<StarRatingMeasure, Double> data = new HashMap<>();
        data.put(StarRatingMeasure.CONTROLLING_BLOOD_PRESSURE, 0.75);
        data.put(StarRatingMeasure.DIABETES_CARE_HBA1C_POOR_CONTROL, 0.75);
        return data;
    }

    /**
     * Helper class to hold measure numerator and denominator
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
