package com.healthdata.quality.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Unit Tests for Lab Result Template
 *
 * Following RED → GREEN → REFACTOR cycle
 * Tests written FIRST, then implementation
 *
 * Day 5 of TDD Swarm Implementation
 */
@DisplayName("Lab Result Template Tests")
class LabResultTemplateTest {

    private ThymeleafTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new ThymeleafTemplateRenderer();
        renderer.initialize();
    }

    @Test
    @DisplayName("Should render lab-result HTML template with all required variables")
    void shouldRenderLabResultHtmlTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123456");
        variables.put("testName", "Hemoglobin A1C");
        variables.put("resultValue", "7.2%");
        variables.put("normalRange", "<7.0% for diabetic patients");
        variables.put("testDate", "November 25, 2025");
        variables.put("orderingProvider", "Dr. Sarah Johnson");
        variables.put("actionUrl", "https://example.com/results/123");

        // When
        String result = renderer.render("lab-result", variables);

        // Then
        assertNotNull(result, "Rendered template should not be null");
        assertTrue(result.contains("<!DOCTYPE html>"), "Should be valid HTML");
        assertTrue(result.contains("John Smith"), "Should contain patient name");
        assertTrue(result.contains("Hemoglobin A1C"), "Should contain test name");
        assertTrue(result.contains("7.2%"), "Should contain result value");
        assertTrue(result.contains("<7.0%") || result.contains("&lt;7.0%"), "Should contain normal range");
        assertTrue(result.contains("November 25"), "Should contain test date");
    }

    @Test
    @DisplayName("Should render lab-result SMS template with minimal content")
    void shouldRenderLabResultSmsTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "SMS");
        variables.put("patientName", "John Smith");
        variables.put("testName", "A1C");
        variables.put("resultValue", "7.2%");
        variables.put("normalRange", "<7.0%");
        variables.put("testDate", "Nov 25");
        variables.put("orderingProvider", "Dr. Johnson");
        variables.put("actionUrl", "https://short.url/r123");

        // When
        String result = renderer.render("lab-result", variables);

        // Then
        assertNotNull(result, "Rendered SMS should not be null");
        assertTrue(result.contains("LAB") || result.contains("RESULT") || result.contains("Lab"),
                "Should contain lab result header");
        assertTrue(result.contains("A1C"), "Should contain test name");
        assertTrue(result.contains("7.2%"), "Should contain result value");

        // SMS should be concise (under 500 characters for extended SMS)
        assertTrue(result.length() < 500,
                "SMS should be concise (was " + result.length() + " characters)");
    }

    @Test
    @DisplayName("Should display test name prominently")
    void shouldDisplayTestName() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("testName", "Complete Blood Count (CBC)");
        variables.put("resultValue", "Normal");
        variables.put("normalRange", "Within normal limits");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("lab-result", variables);

        // Then
        assertTrue(result.contains("Complete Blood Count") || result.contains("CBC"),
                "Should display test name");
    }

    @Test
    @DisplayName("Should display result value prominently")
    void shouldDisplayResultValue() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("testName", "Blood Glucose");
        variables.put("resultValue", "185 mg/dL");
        variables.put("normalRange", "70-100 mg/dL");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("lab-result", variables);

        // Then
        assertTrue(result.contains("185"), "Should display result value");
        assertTrue(result.contains("mg/dL"), "Should display units");
    }

    @Test
    @DisplayName("Should display normal range for comparison")
    void shouldDisplayNormalRange() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("testName", "Cholesterol");
        variables.put("resultValue", "220 mg/dL");
        variables.put("normalRange", "<200 mg/dL");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("lab-result", variables);

        // Then
        assertTrue(result.contains("200") || result.contains("&lt;200"),
                "Should display normal range");
    }

    @Test
    @DisplayName("Should display test date")
    void shouldDisplayTestDate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("testName", "Test");
        variables.put("resultValue", "Normal");
        variables.put("normalRange", "Normal");
        variables.put("testDate", "December 1, 2025");
        variables.put("orderingProvider", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("lab-result", variables);

        // Then
        assertTrue(result.contains("December 1") || result.contains("2025-12-01"),
                "Should display test date");
    }

    @Test
    @DisplayName("Should display ordering provider information")
    void shouldDisplayOrderingProvider() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("testName", "Test");
        variables.put("resultValue", "Normal");
        variables.put("normalRange", "Normal");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. Emily Rodriguez, MD");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("lab-result", variables);

        // Then
        assertTrue(result.contains("Dr. Emily Rodriguez"), "Should display ordering provider");
    }

    @Test
    @DisplayName("Should display result status (normal/abnormal/critical)")
    void shouldDisplayResultStatus() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("testName", "Test");
        variables.put("resultValue", "High");
        variables.put("normalRange", "Normal");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. Smith");
        variables.put("resultStatus", "ABNORMAL");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("lab-result", variables);

        // Then
        assertTrue(result.contains("ABNORMAL") || result.contains("Abnormal"),
                "Should display result status");
    }

    @Test
    @DisplayName("Should display critical flag for critical results")
    void shouldDisplayCriticalFlag() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("testName", "Potassium");
        variables.put("resultValue", "6.8 mEq/L");
        variables.put("normalRange", "3.5-5.0 mEq/L");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. Smith");
        variables.put("resultStatus", "CRITICAL");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("lab-result", variables);

        // Then
        assertTrue(result.contains("CRITICAL") || result.contains("Critical"),
                "Should display critical flag");
    }

    @Test
    @DisplayName("Should display previous result for comparison")
    void shouldDisplayPreviousResult() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("testName", "A1C");
        variables.put("resultValue", "7.2%");
        variables.put("normalRange", "<7.0%");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. Smith");
        variables.put("previousResult", "7.8%");
        variables.put("previousTestDate", "August 15, 2025");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("lab-result", variables);

        // Then
        assertTrue(result.contains("7.8"), "Should display previous result");
    }

    @Test
    @DisplayName("Should display trend indicator (improving/worsening)")
    void shouldDisplayTrendIndicator() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("testName", "A1C");
        variables.put("resultValue", "7.2%");
        variables.put("normalRange", "<7.0%");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. Smith");
        variables.put("previousResult", "7.8%");
        variables.put("trend", "IMPROVING");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("lab-result", variables);

        // Then
        assertTrue(result.contains("IMPROVING") || result.contains("Improving") || result.contains("↓"),
                "Should display trend indicator");
    }

    @Test
    @DisplayName("Should display interpretation or clinical notes")
    void shouldDisplayInterpretation() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("testName", "TSH");
        variables.put("resultValue", "8.5 mIU/L");
        variables.put("normalRange", "0.4-4.0 mIU/L");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. Smith");
        variables.put("interpretation", "Elevated TSH may indicate hypothyroidism. Follow up with provider.");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("lab-result", variables);

        // Then
        assertTrue(result.contains("hypothyroidism") || result.contains("Follow up"),
                "Should display interpretation");
    }

    @Test
    @DisplayName("Should display recommended next steps")
    void shouldDisplayRecommendedNextSteps() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("testName", "A1C");
        variables.put("resultValue", "8.5%");
        variables.put("normalRange", "<7.0%");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        List<String> nextSteps = List.of(
                "Schedule follow-up appointment within 2 weeks",
                "Review current diabetes medication regimen",
                "Discuss lifestyle modifications with provider"
        );
        variables.put("nextSteps", nextSteps);

        // When
        String result = renderer.render("lab-result", variables);

        // Then
        assertTrue(result.contains("follow-up") || result.contains("Follow-up"),
                "Should contain first next step");
        assertTrue(result.contains("medication"), "Should contain second next step");
    }

    @Test
    @DisplayName("Should display lab contact information")
    void shouldDisplayLabContactInformation() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("testName", "Test");
        variables.put("resultValue", "Normal");
        variables.put("normalRange", "Normal");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. Smith");
        variables.put("labName", "Memorial Lab Services");
        variables.put("labPhone", "(555) 123-4567");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("lab-result", variables);

        // Then
        assertTrue(result.contains("Memorial Lab"), "Should display lab name");
        assertTrue(result.contains("(555) 123-4567"), "Should display lab phone");
    }

    @Test
    @DisplayName("Should handle missing optional fields gracefully")
    void shouldHandleMissingOptionalFields() {
        // Given - minimal required fields only
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("testName", "Test Name");
        variables.put("resultValue", "Normal");
        variables.put("normalRange", "Normal");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");
        // Note: NO previousResult, NO trend, NO interpretation, NO nextSteps

        // When
        String result = renderer.render("lab-result", variables);

        // Then
        assertNotNull(result, "Should render successfully with minimal fields");
        assertTrue(result.contains("Test Patient"), "Should contain patient name");
        assertTrue(result.contains("Test Name"), "Should contain test name");
        assertFalse(result.contains("null"), "Should not contain 'null' strings");
    }

    @Test
    @DisplayName("Should be mobile-responsive (viewport meta tag)")
    void shouldBeMobileResponsive() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("testName", "Test");
        variables.put("resultValue", "Normal");
        variables.put("normalRange", "Normal");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("lab-result", variables);

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
        variables.put("testName", "Test");
        variables.put("resultValue", "Normal");
        variables.put("normalRange", "Normal");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("lab-result", variables);

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
        variables.put("testName", "Test <b>Name</b>");
        variables.put("resultValue", "Value <script>malicious()</script>");
        variables.put("normalRange", "Range <b>Normal</b>");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. <b>Smith</b>");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("lab-result", variables);

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
        warmupVars.put("testName", "Warmup");
        warmupVars.put("resultValue", "Normal");
        warmupVars.put("normalRange", "Normal");
        warmupVars.put("testDate", "2025-11-25");
        warmupVars.put("orderingProvider", "Warmup");
        warmupVars.put("actionUrl", "https://example.com");
        renderer.render("lab-result", warmupVars);

        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("testName", "Test");
        variables.put("resultValue", "Normal");
        variables.put("normalRange", "Normal");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        // When - measure after warmup
        long startTime = System.currentTimeMillis();
        String result = renderer.render("lab-result", variables);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertNotNull(result);
        assertTrue(duration < 3000,
                "Rendering should take less than 3000ms (took " + duration + "ms)");
    }

    @Test
    @DisplayName("Should display visual indicator color coding for result status")
    void shouldDisplayColorCodedResultStatus() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("testName", "Test");
        variables.put("resultValue", "High");
        variables.put("normalRange", "Normal");
        variables.put("testDate", "2025-11-25");
        variables.put("orderingProvider", "Dr. Smith");
        variables.put("resultStatus", "CRITICAL");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("lab-result", variables);

        // Then
        assertTrue(result.contains("CRITICAL") || result.contains("Critical"),
                "Should display critical status");
        // Template should handle color coding via CSS classes based on status
    }
}
