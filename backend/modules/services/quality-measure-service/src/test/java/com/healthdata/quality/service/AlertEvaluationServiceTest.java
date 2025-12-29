package com.healthdata.quality.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AlertEvaluationService intelligent suppression logic.
 *
 * Validates:
 * - Priority-based filtering (HIGH/CRITICAL pass, LOW suppressed)
 * - Time-based suppression (prevent duplicate alerts)
 * - Daily limit per patient
 * - Actionability checks
 */
class AlertEvaluationServiceTest {

    private AlertEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new AlertEvaluationService();
        // Set default threshold to 50 (HIGH and CRITICAL pass)
        ReflectionTestUtils.setField(service, "minPriorityThreshold", 50);
        ReflectionTestUtils.setField(service, "suppressionWindowHours", 24);
        ReflectionTestUtils.setField(service, "maxDailyAlertsPerPatient", 5);
        service.clearSuppressionCache();
    }

    @Nested
    @DisplayName("shouldTriggerAlert() tests")
    class ShouldTriggerAlertTests {

        @Test
        @DisplayName("Should suppress alert when condition is null")
        void shouldSuppressWhenConditionNull() {
            assertThat(service.shouldTriggerAlert(null, createAlertData("patient1", "HIGH")))
                .isFalse();
        }

        @Test
        @DisplayName("Should suppress alert when data is null")
        void shouldSuppressWhenDataNull() {
            assertThat(service.shouldTriggerAlert("care-gap.critical", null))
                .isFalse();
        }

        @Test
        @DisplayName("Should trigger alert for HIGH severity")
        void shouldTriggerForHighSeverity() {
            Map<String, Object> data = createAlertData("patient1", "HIGH");
            assertThat(service.shouldTriggerAlert("care-gap.critical", data)).isTrue();
        }

        @Test
        @DisplayName("Should trigger alert for CRITICAL severity")
        void shouldTriggerForCriticalSeverity() {
            Map<String, Object> data = createAlertData("patient1", "CRITICAL");
            assertThat(service.shouldTriggerAlert("lab.critical", data)).isTrue();
        }

        @Test
        @DisplayName("Should suppress alert for LOW severity below threshold")
        void shouldSuppressLowSeverity() {
            Map<String, Object> data = createAlertData("patient1", "LOW");
            // LOW = 25, threshold = 50, so should be suppressed
            assertThat(service.shouldTriggerAlert("care-gap.open", data)).isFalse();
        }

        @Test
        @DisplayName("Should suppress duplicate alerts within suppression window")
        void shouldSuppressDuplicateAlerts() {
            Map<String, Object> data = createAlertData("patient1", "HIGH");

            // First alert should trigger
            assertThat(service.shouldTriggerAlert("care-gap.critical", data)).isTrue();

            // Duplicate should be suppressed
            assertThat(service.shouldTriggerAlert("care-gap.critical", data)).isFalse();
        }

        @Test
        @DisplayName("Should allow alerts for different conditions on same patient")
        void shouldAllowDifferentConditions() {
            Map<String, Object> data = createAlertData("patient1", "HIGH");

            assertThat(service.shouldTriggerAlert("care-gap.critical", data)).isTrue();
            assertThat(service.shouldTriggerAlert("lab.critical", data)).isTrue();
        }

        @Test
        @DisplayName("Should suppress non-actionable alerts")
        void shouldSuppressNonActionableAlerts() {
            Map<String, Object> data = createAlertData("patient1", "HIGH");
            // informational alerts are not actionable
            assertThat(service.shouldTriggerAlert("informational", data)).isFalse();
        }

        @Test
        @DisplayName("CRITICAL alerts should bypass daily limit")
        void criticalBypassesDailyLimit() {
            // Set very low daily limit
            ReflectionTestUtils.setField(service, "maxDailyAlertsPerPatient", 1);

            Map<String, Object> data = createAlertData("patient1", "CRITICAL");

            // First alert
            assertThat(service.shouldTriggerAlert("lab.critical", data)).isTrue();

            // Second CRITICAL alert should still pass (different condition)
            assertThat(service.shouldTriggerAlert("medication.interaction", data)).isTrue();
        }
    }

    @Nested
    @DisplayName("calculatePriorityScore() tests")
    class CalculatePriorityScoreTests {

        @Test
        @DisplayName("Should return correct base scores for severity levels")
        void shouldReturnCorrectBaseSeverityScores() {
            assertThat(service.calculatePriorityScore("alert", "CRITICAL", null)).isEqualTo(100);
            assertThat(service.calculatePriorityScore("alert", "HIGH", null)).isEqualTo(75);
            assertThat(service.calculatePriorityScore("alert", "MEDIUM", null)).isEqualTo(50);
            assertThat(service.calculatePriorityScore("alert", "LOW", null)).isEqualTo(25);
            assertThat(service.calculatePriorityScore("alert", "UNKNOWN", null)).isEqualTo(10);
        }

        @Test
        @DisplayName("Should apply positive modifier for critical alert types")
        void shouldApplyPositiveModifiers() {
            // lab.critical adds +30 to base score
            assertThat(service.calculatePriorityScore("lab.critical", "HIGH", null))
                .isEqualTo(100); // 75 + 30 capped at 100

            // medication.interaction adds +25
            assertThat(service.calculatePriorityScore("medication.interaction", "HIGH", null))
                .isEqualTo(100); // 75 + 25 = 100
        }

        @Test
        @DisplayName("Should apply negative modifier for low-priority alert types")
        void shouldApplyNegativeModifiers() {
            // compliance.overdue subtracts 10
            assertThat(service.calculatePriorityScore("compliance.overdue", "HIGH", null))
                .isEqualTo(65); // 75 - 10 = 65
        }

        @Test
        @DisplayName("Should handle null severity gracefully")
        void shouldHandleNullSeverity() {
            assertThat(service.calculatePriorityScore("alert", null, null))
                .isEqualTo(25); // defaults to LOW
        }

        @Test
        @DisplayName("Should handle null alert type gracefully")
        void shouldHandleNullAlertType() {
            assertThat(service.calculatePriorityScore(null, "HIGH", null))
                .isEqualTo(75); // base score only, no modifier
        }
    }

    @Nested
    @DisplayName("Cache management tests")
    class CacheManagementTests {

        @Test
        @DisplayName("Should clear suppression cache")
        void shouldClearCache() {
            Map<String, Object> data = createAlertData("patient1", "HIGH");

            // Trigger first alert
            service.shouldTriggerAlert("care-gap.critical", data);

            // Should be suppressed (duplicate)
            assertThat(service.shouldTriggerAlert("care-gap.critical", data)).isFalse();

            // Clear cache
            service.clearSuppressionCache();

            // Should trigger again after cache clear
            assertThat(service.shouldTriggerAlert("care-gap.critical", data)).isTrue();
        }
    }

    /**
     * Helper to create alert data map.
     */
    private Map<String, Object> createAlertData(String patientId, String severity) {
        Map<String, Object> data = new HashMap<>();
        data.put("patientId", patientId);
        data.put("severity", severity);
        return data;
    }
}
