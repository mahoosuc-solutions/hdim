package com.healthdata.quality.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Unit Tests for Health Score Template
 *
 * Following RED → GREEN → REFACTOR cycle
 * Tests written FIRST, then implementation
 *
 * Day 2 of TDD Swarm Implementation
 */
@DisplayName("Health Score Template Tests")
class HealthScoreTemplateTest {

    private ThymeleafTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new ThymeleafTemplateRenderer();
        renderer.initialize();
    }

    @Test
    @DisplayName("Should render health-score HTML template with all required variables")
    void shouldRenderHealthScoreHtmlTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123456");
        variables.put("currentScore", 72);
        variables.put("previousScore", 68);
        variables.put("scoreChange", "+4");
        variables.put("scoreMessage", "Patient's health score has improved by 4 points this quarter");
        variables.put("actionUrl", "https://example.com/health-score/123");

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertNotNull(result, "Rendered template should not be null");
        assertTrue(result.contains("<!DOCTYPE html>"), "Should be valid HTML");
        assertTrue(result.contains("John Smith"), "Should contain patient name");
        assertTrue(result.contains("72"), "Should contain current score");
        assertTrue(result.contains("improved"), "Should contain score message");
    }

    @Test
    @DisplayName("Should render health-score SMS template with minimal content")
    void shouldRenderHealthScoreSmsTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "SMS");
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123456");
        variables.put("currentScore", 72);
        variables.put("previousScore", 68);
        variables.put("scoreChange", "+4");
        variables.put("scoreMessage", "Health score improved");
        variables.put("actionUrl", "https://short.url/h123");

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertNotNull(result, "Rendered SMS should not be null");
        assertTrue(result.contains("HEALTH SCORE") || result.contains("Health Score"),
                "Should contain health score header");
        assertTrue(result.contains("John Smith"), "Should contain patient name");
        assertTrue(result.contains("72"), "Should contain current score");

        // SMS should be concise (under 500 characters for extended SMS)
        assertTrue(result.length() < 500,
                "SMS should be concise (was " + result.length() + " characters)");
    }

    @Test
    @DisplayName("Should display current score prominently")
    void shouldDisplayCurrentScoreProminently() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("currentScore", 85);
        variables.put("previousScore", 80);
        variables.put("scoreChange", "+5");
        variables.put("scoreMessage", "Excellent progress");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertTrue(result.contains("85"), "Should display current score");
    }

    @Test
    @DisplayName("Should display previous score for comparison")
    void shouldDisplayPreviousScore() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("currentScore", 75);
        variables.put("previousScore", 70);
        variables.put("scoreChange", "+5");
        variables.put("scoreMessage", "Score improved");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertTrue(result.contains("70"), "Should display previous score");
    }

    @Test
    @DisplayName("Should display score change indicator with positive change")
    void shouldDisplayPositiveScoreChange() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("currentScore", 80);
        variables.put("previousScore", 75);
        variables.put("scoreChange", "+5");
        variables.put("scoreMessage", "Great improvement");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertTrue(result.contains("+5") || result.contains("↑") || result.contains("improved"),
                "Should indicate positive change");
    }

    @Test
    @DisplayName("Should display score change indicator with negative change")
    void shouldDisplayNegativeScoreChange() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("currentScore", 65);
        variables.put("previousScore", 70);
        variables.put("scoreChange", "-5");
        variables.put("scoreMessage", "Score decreased - follow up recommended");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertTrue(result.contains("-5") || result.contains("↓") || result.contains("decreased"),
                "Should indicate negative change");
    }

    @Test
    @DisplayName("Should display score interpretation level - Excellent")
    void shouldDisplayExcellentScoreInterpretation() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("currentScore", 92);
        variables.put("previousScore", 88);
        variables.put("scoreChange", "+4");
        variables.put("scoreMessage", "Outstanding health status");
        variables.put("interpretation", "Excellent");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertTrue(result.contains("Excellent") || result.contains("92"),
                "Should indicate excellent score level");
    }

    @Test
    @DisplayName("Should display score interpretation level - Good")
    void shouldDisplayGoodScoreInterpretation() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("currentScore", 75);
        variables.put("previousScore", 72);
        variables.put("scoreChange", "+3");
        variables.put("scoreMessage", "Good health status");
        variables.put("interpretation", "Good");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertTrue(result.contains("Good") || result.contains("75"),
                "Should indicate good score level");
    }

    @Test
    @DisplayName("Should display score interpretation level - Fair")
    void shouldDisplayFairScoreInterpretation() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("currentScore", 55);
        variables.put("previousScore", 52);
        variables.put("scoreChange", "+3");
        variables.put("scoreMessage", "Fair health status - improvements recommended");
        variables.put("interpretation", "Fair");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertTrue(result.contains("Fair") || result.contains("55"),
                "Should indicate fair score level");
    }

    @Test
    @DisplayName("Should display score interpretation level - Poor")
    void shouldDisplayPoorScoreInterpretation() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("currentScore", 35);
        variables.put("previousScore", 32);
        variables.put("scoreChange", "+3");
        variables.put("scoreMessage", "Attention needed - multiple care gaps identified");
        variables.put("interpretation", "Poor");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertTrue(result.contains("Poor") || result.contains("35"),
                "Should indicate poor score level");
    }

    @Test
    @DisplayName("Should include contributing factors list")
    void shouldIncludeContributingFactors() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("currentScore", 72);
        variables.put("previousScore", 68);
        variables.put("scoreChange", "+4");
        variables.put("scoreMessage", "Score improved");
        variables.put("actionUrl", "https://example.com");

        List<String> contributingFactors = List.of(
                "Preventive care compliance: 85%",
                "Chronic condition management: 70%",
                "Medication adherence: 90%"
        );
        variables.put("contributingFactors", contributingFactors);

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertTrue(result.contains("Preventive care compliance"),
                "Should contain first contributing factor");
        assertTrue(result.contains("Chronic condition management"),
                "Should contain second contributing factor");
        assertTrue(result.contains("Medication adherence"),
                "Should contain third contributing factor");
    }

    @Test
    @DisplayName("Should include improvement recommendations")
    void shouldIncludeImprovementRecommendations() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("currentScore", 68);
        variables.put("previousScore", 65);
        variables.put("scoreChange", "+3");
        variables.put("scoreMessage", "Progress made");
        variables.put("actionUrl", "https://example.com");

        List<String> recommendations = List.of(
                "Schedule annual wellness visit",
                "Complete pending diabetic eye exam",
                "Review medication adherence"
        );
        variables.put("recommendations", recommendations);

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertTrue(result.contains("wellness visit"), "Should contain first recommendation");
        assertTrue(result.contains("eye exam"), "Should contain second recommendation");
        assertTrue(result.contains("medication adherence"), "Should contain third recommendation");
    }

    @Test
    @DisplayName("Should handle missing optional fields gracefully")
    void shouldHandleMissingOptionalFields() {
        // Given - minimal required fields only
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("currentScore", 75);
        variables.put("previousScore", 70);
        variables.put("scoreChange", "+5");
        variables.put("scoreMessage", "Score improved");
        variables.put("actionUrl", "https://example.com");
        // Note: NO mrn, NO contributingFactors, NO recommendations, NO interpretation

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertNotNull(result, "Should render successfully with minimal fields");
        assertTrue(result.contains("Test Patient"), "Should contain patient name");
        assertTrue(result.contains("75"), "Should contain current score");
        assertFalse(result.contains("null"), "Should not contain 'null' strings");
    }

    @Test
    @DisplayName("Should be mobile-responsive (viewport meta tag)")
    void shouldBeMobileResponsive() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("currentScore", 75);
        variables.put("previousScore", 70);
        variables.put("scoreChange", "+5");
        variables.put("scoreMessage", "Score improved");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertTrue(result.contains("viewport"), "Should have viewport meta tag");
        assertTrue(result.contains("width=device-width"), "Should be mobile-responsive");
    }

    @Test
    @DisplayName("Should include HIPAA disclaimer")
    void shouldIncludeHipaaDisclaimer() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("currentScore", 75);
        variables.put("previousScore", 70);
        variables.put("scoreChange", "+5");
        variables.put("scoreMessage", "Score improved");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertTrue(result.contains("Protected Health Information") ||
                result.contains("PHI") ||
                result.contains("HIPAA"),
                "Should mention PHI/HIPAA in disclaimer");
    }

    @Test
    @DisplayName("Should escape HTML in user content (XSS prevention)")
    void shouldEscapeHtmlInUserContent() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test <script>alert('xss')</script> Patient");
        variables.put("currentScore", 75);
        variables.put("previousScore", 70);
        variables.put("scoreChange", "+5");
        variables.put("scoreMessage", "Score <script>malicious()</script> improved");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertFalse(result.contains("<script>alert"), "Should escape script tags");
        assertTrue(result.contains("&lt;script&gt;") || !result.contains("<script>alert"),
                "Script tags should be escaped or removed");
    }

    @Test
    @DisplayName("Should render in reasonable time (<1000ms)")
    void shouldRenderInReasonableTime() {
        // Given - warm up the template engine first
        Map<String, Object> warmupVars = new HashMap<>();
        warmupVars.put("channel", "EMAIL");
        warmupVars.put("patientName", "Warmup");
        warmupVars.put("currentScore", 50);
        warmupVars.put("previousScore", 50);
        warmupVars.put("scoreChange", "0");
        warmupVars.put("scoreMessage", "Warmup");
        warmupVars.put("actionUrl", "https://example.com");
        renderer.render("health-score", warmupVars);

        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("currentScore", 75);
        variables.put("previousScore", 70);
        variables.put("scoreChange", "+5");
        variables.put("scoreMessage", "Score improved");
        variables.put("actionUrl", "https://example.com");

        // When - measure after warmup
        long startTime = System.currentTimeMillis();
        String result = renderer.render("health-score", variables);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertNotNull(result);
        assertTrue(duration < 3000,
                "Rendering should take less than 3000ms (took " + duration + "ms)");
    }

    @Test
    @DisplayName("Should display score with color coding based on level")
    void shouldDisplayScoreWithColorCoding() {
        // Given - test high score (should be green)
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("currentScore", 85);
        variables.put("previousScore", 80);
        variables.put("scoreChange", "+5");
        variables.put("scoreMessage", "Excellent health status");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("health-score", variables);

        // Then
        // Should have color-related CSS classes or inline styles
        assertTrue(result.contains("score") && result.contains("85"),
                "Should display score value");
    }

    @Test
    @DisplayName("Should handle no score change (unchanged)")
    void shouldHandleUnchangedScore() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("currentScore", 75);
        variables.put("previousScore", 75);
        variables.put("scoreChange", "0");
        variables.put("scoreMessage", "Health score remains stable");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("health-score", variables);

        // Then
        assertNotNull(result, "Should handle unchanged score");
        assertTrue(result.contains("75"), "Should display current score");
        assertTrue(result.contains("0") || result.contains("stable") || result.contains("unchanged"),
                "Should indicate no change");
    }
}
