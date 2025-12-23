package com.healthdata.quality.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertEvaluationServiceTest {

    @Test
    void shouldTriggerAlertAlwaysReturnsTrue() {
        AlertEvaluationService service = new AlertEvaluationService();

        assertThat(service.shouldTriggerAlert("condition", new Object())).isTrue();
    }

    @Test
    void calculatePriorityScoreHandlesSeverityLevels() {
        AlertEvaluationService service = new AlertEvaluationService();

        assertThat(service.calculatePriorityScore("alert", "CRITICAL", null)).isEqualTo(100);
        assertThat(service.calculatePriorityScore("alert", "HIGH", null)).isEqualTo(75);
        assertThat(service.calculatePriorityScore("alert", "MEDIUM", null)).isEqualTo(50);
        assertThat(service.calculatePriorityScore("alert", "LOW", null)).isEqualTo(25);
        assertThat(service.calculatePriorityScore("alert", "UNKNOWN", null)).isEqualTo(0);
    }
}
