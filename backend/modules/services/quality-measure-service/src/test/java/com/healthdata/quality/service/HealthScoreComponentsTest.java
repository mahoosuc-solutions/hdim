package com.healthdata.quality.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Health Score Components Tests")
class HealthScoreComponentsTest {

    @Test
    @DisplayName("Should calculate overall score from weighted components")
    void shouldCalculateOverallScore() {
        HealthScoreComponents components = HealthScoreComponents.builder()
            .physicalHealthScore(80.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(60.0)
            .preventiveCareScore(90.0)
            .chronicDiseaseScore(50.0)
            .build();

        Double overall = components.calculateOverallScore();

        assertThat(overall).isEqualTo(
            (80.0 * 0.30) +
            (70.0 * 0.25) +
            (60.0 * 0.15) +
            (90.0 * 0.15) +
            (50.0 * 0.15)
        );
    }

    @Test
    @DisplayName("Should reject null component scores")
    void shouldRejectNullComponentScores() {
        HealthScoreComponents components = HealthScoreComponents.builder()
            .physicalHealthScore(null)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(60.0)
            .preventiveCareScore(90.0)
            .chronicDiseaseScore(50.0)
            .build();

        assertThatThrownBy(components::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Physical Health score is required");
    }

    @Test
    @DisplayName("Should reject out-of-range component scores")
    void shouldRejectOutOfRangeScores() {
        HealthScoreComponents components = HealthScoreComponents.builder()
            .physicalHealthScore(-1.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(60.0)
            .preventiveCareScore(90.0)
            .chronicDiseaseScore(50.0)
            .build();

        assertThatThrownBy(components::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Physical Health score must be between 0 and 100");
    }
}
