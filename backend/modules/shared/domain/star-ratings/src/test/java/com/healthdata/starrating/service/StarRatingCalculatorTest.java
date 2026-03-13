package com.healthdata.starrating.service;

import com.healthdata.starrating.domain.DomainScore;
import com.healthdata.starrating.domain.MeasureScore;
import com.healthdata.starrating.domain.StarRatingDomain;
import com.healthdata.starrating.domain.StarRatingMeasure;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Tag("unit")
class StarRatingCalculatorTest {

    private final StarRatingCalculator calculator = new StarRatingCalculator();

    // --- calculateStarsForMeasure ---

    @Test
    void calculateStarsForMeasure_usesMiddleCutPointForStandardMeasures() {
        double[] cutPoints = {0.50, 0.58, 0.64, 0.70, 0.75};

        assertThat(calculator.calculateStarsForMeasure(0.76, cutPoints)).isEqualTo(5);
        assertThat(calculator.calculateStarsForMeasure(0.70, cutPoints)).isEqualTo(4);
        assertThat(calculator.calculateStarsForMeasure(0.64, cutPoints)).isEqualTo(3);
        assertThat(calculator.calculateStarsForMeasure(0.58, cutPoints)).isEqualTo(2);
        assertThat(calculator.calculateStarsForMeasure(0.57, cutPoints)).isEqualTo(1);
    }

    @Test
    void calculateStarsForMeasure_usesMiddleCutPointForInvertedMeasures() {
        double[] cutPoints = {0.20, 0.18, 0.16, 0.14, 0.12};

        assertThat(calculator.calculateStarsForMeasure(0.11, cutPoints)).isEqualTo(5);
        assertThat(calculator.calculateStarsForMeasure(0.14, cutPoints)).isEqualTo(4);
        assertThat(calculator.calculateStarsForMeasure(0.16, cutPoints)).isEqualTo(3);
        assertThat(calculator.calculateStarsForMeasure(0.18, cutPoints)).isEqualTo(2);
        assertThat(calculator.calculateStarsForMeasure(0.19, cutPoints)).isEqualTo(1);
    }

    // --- calculateMeasureScore ---

    @Test
    void calculateMeasureScore_computesPerformanceRateAndStars() {
        MeasureScore score = calculator.calculateMeasureScore(
            StarRatingMeasure.COLORECTAL_CANCER_SCREENING, 75, 100);

        assertThat(score.getPerformanceRate()).isCloseTo(0.75, within(0.001));
        assertThat(score.getStars()).isEqualTo(5);
        assertThat(score.getNumerator()).isEqualTo(75);
        assertThat(score.getDenominator()).isEqualTo(100);
        assertThat(score.getMeasure()).isEqualTo(StarRatingMeasure.COLORECTAL_CANCER_SCREENING);
    }

    @Test
    void calculateMeasureScore_zeroDenominatorYieldsOneStar() {
        MeasureScore score = calculator.calculateMeasureScore(
            StarRatingMeasure.BREAST_CANCER_SCREENING, 0, 0);

        assertThat(score.getPerformanceRate()).isEqualTo(0.0);
        assertThat(score.getStars()).isEqualTo(1);
    }

    @Test
    void calculateMeasureScore_unknownMeasureUsesDefaultCutPoints() {
        // ADULT_BMI_ASSESSMENT has no custom cut points, falls back to default {0.50, 0.60, 0.70, 0.80, 0.90}
        MeasureScore score = calculator.calculateMeasureScore(
            StarRatingMeasure.ADULT_BMI_ASSESSMENT, 85, 100);

        assertThat(score.getPerformanceRate()).isCloseTo(0.85, within(0.001));
        assertThat(score.getStars()).isEqualTo(4); // >= 0.80 but < 0.90
    }

    // --- calculateDomainScore ---

    @Test
    void calculateDomainScore_weightedAverageOfMeasures() {
        MeasureScore score1 = calculator.calculateMeasureScore(
            StarRatingMeasure.BREAST_CANCER_SCREENING, 76, 100);
        MeasureScore score2 = calculator.calculateMeasureScore(
            StarRatingMeasure.COLORECTAL_CANCER_SCREENING, 50, 100);

        DomainScore domain = calculator.calculateDomainScore(
            StarRatingDomain.STAYING_HEALTHY, List.of(score1, score2));

        assertThat(domain.getMeasureCount()).isEqualTo(2);
        assertThat(domain.getDomainStars()).isGreaterThan(0.0);
        assertThat(domain.getDomain()).isEqualTo(StarRatingDomain.STAYING_HEALTHY);
    }

    @Test
    void calculateDomainScore_emptyMeasuresYieldsZero() {
        DomainScore domain = calculator.calculateDomainScore(
            StarRatingDomain.MEMBER_EXPERIENCE, List.of());

        assertThat(domain.getDomainStars()).isEqualTo(0.0);
        assertThat(domain.getMeasureCount()).isEqualTo(0);
    }

    // --- calculateOverallStarRating ---

    @Test
    void calculateOverallStarRating_weightedAverageOfDomains() {
        MeasureScore measure1 = calculator.calculateMeasureScore(
            StarRatingMeasure.BREAST_CANCER_SCREENING, 76, 100);
        DomainScore domain1 = calculator.calculateDomainScore(
            StarRatingDomain.STAYING_HEALTHY, List.of(measure1));

        MeasureScore measure2 = calculator.calculateMeasureScore(
            StarRatingMeasure.DIABETES_CARE_HBA1C_CONTROL, 72, 100);
        DomainScore domain2 = calculator.calculateDomainScore(
            StarRatingDomain.MANAGING_CHRONIC_CONDITIONS, List.of(measure2));

        double overall = calculator.calculateOverallStarRating(List.of(domain1, domain2));

        assertThat(overall).isGreaterThan(0.0);
        assertThat(overall).isLessThanOrEqualTo(5.0);
    }

    @Test
    void calculateOverallStarRating_emptyDomainsYieldsZero() {
        double overall = calculator.calculateOverallStarRating(List.of());

        assertThat(overall).isEqualTo(0.0);
    }

    @Test
    void calculateOverallStarRating_cappedAtFive() {
        // Even with perfect scores, should not exceed 5.0
        MeasureScore perfect = calculator.calculateMeasureScore(
            StarRatingMeasure.BREAST_CANCER_SCREENING, 100, 100);
        DomainScore domain = calculator.calculateDomainScore(
            StarRatingDomain.STAYING_HEALTHY, List.of(perfect));

        double overall = calculator.calculateOverallStarRating(List.of(domain));

        assertThat(overall).isLessThanOrEqualTo(5.0);
    }

    // --- roundToHalfStar ---

    @Test
    void roundToHalfStar_roundsCorrectly() {
        assertThat(calculator.roundToHalfStar(3.74)).isEqualTo(3.5);
        assertThat(calculator.roundToHalfStar(3.75)).isEqualTo(4.0);
        assertThat(calculator.roundToHalfStar(3.25)).isEqualTo(3.5);
        assertThat(calculator.roundToHalfStar(3.24)).isEqualTo(3.0);
        assertThat(calculator.roundToHalfStar(4.0)).isEqualTo(4.0);
        assertThat(calculator.roundToHalfStar(0.0)).isEqualTo(0.0);
    }
}
