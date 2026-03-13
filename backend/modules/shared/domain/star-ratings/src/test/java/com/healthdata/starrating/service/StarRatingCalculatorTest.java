package com.healthdata.starrating.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StarRatingCalculatorTest {

    private final StarRatingCalculator calculator = new StarRatingCalculator();

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
}
