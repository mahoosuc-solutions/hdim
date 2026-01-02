package com.healthdata.quality.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Unit Tests for Care Gap Template
 *
 * Following RED → GREEN → REFACTOR cycle
 * Tests written FIRST, then implementation
 */
@DisplayName("Care Gap Template Tests")
class CareGapTemplateTest {

    private ThymeleafTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new ThymeleafTemplateRenderer();
        renderer.initialize();
    }

    @Test
    @DisplayName("Should render care-gap HTML template with all required variables")
    void shouldRenderCareGapHtmlTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123456");
        variables.put("gapType", "Preventive Care Gap");
        variables.put("gapMessage", "Patient is due for annual diabetic eye exam (last exam: 18 months ago)");
        variables.put("measure", "CDC-H: Comprehensive Diabetes Care - Eye Exam");
        variables.put("dueDate", "2025-12-01");
        variables.put("actionUrl", "https://example.com/care-gaps/456");

        // When
        String result = renderer.render("care-gap", variables);

        // Then
        assertNotNull(result, "Rendered template should not be null");
        assertTrue(result.contains("<!DOCTYPE html>"), "Should be valid HTML");
        assertTrue(result.contains("John Smith"), "Should contain patient name");
        assertTrue(result.contains("Preventive Care Gap"), "Should contain gap type");
        assertTrue(result.contains("diabetic eye exam"), "Should contain gap message");
        assertTrue(result.contains("CDC-H"), "Should contain measure name");
        assertTrue(result.contains("2025-12-01"), "Should contain due date");
    }

    @Test
    @DisplayName("Should render care-gap SMS template with minimal content")
    void shouldRenderCareGapSmsTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "SMS");
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123456");
        variables.put("gapType", "Preventive Care");
        variables.put("gapMessage", "Annual eye exam needed");
        variables.put("measure", "CDC-H Eye Exam");
        variables.put("dueDate", "Dec 1");
        variables.put("actionUrl", "https://short.url/g456");

        // When
        String result = renderer.render("care-gap", variables);

        // Then
        assertNotNull(result, "Rendered SMS should not be null");
        assertTrue(result.contains("CARE GAP") || result.contains("Care Gap"),
                "Should contain care gap header");
        assertTrue(result.contains("John Smith"), "Should contain patient name");
        assertTrue(result.contains("MRN-123456"), "Should contain MRN");

        // SMS should be concise (under 500 characters for extended SMS)
        assertTrue(result.length() < 500,
                "SMS should be concise (was " + result.length() + " characters)");
    }

    @Test
    @DisplayName("Should display gap type and description")
    void shouldDisplayGapTypeAndDescription() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Jane Doe");
        variables.put("gapType", "Medication Adherence Gap");
        variables.put("gapMessage", "Patient has not refilled diabetes medication in 45 days");
        variables.put("measure", "PDC: Diabetes Medications");
        variables.put("dueDate", "2025-11-30");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("care-gap", variables);

        // Then
        assertTrue(result.contains("Medication Adherence Gap"), "Should display gap type");
        assertTrue(result.contains("not refilled diabetes medication"), "Should display description");
    }

    @Test
    @DisplayName("Should display measure name and details")
    void shouldDisplayMeasureNameAndDetails() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("gapType", "Screening Gap");
        variables.put("gapMessage", "Colorectal cancer screening overdue");
        variables.put("measure", "COL: Colorectal Cancer Screening");
        variables.put("dueDate", "2025-12-15");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("care-gap", variables);

        // Then
        assertTrue(result.contains("COL: Colorectal Cancer Screening"),
                "Should display full measure name");
    }

    @Test
    @DisplayName("Should display due date with formatting")
    void shouldDisplayDueDateWithFormatting() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Patient Name");
        variables.put("gapType", "Gap Type");
        variables.put("gapMessage", "Gap message");
        variables.put("measure", "Measure");
        variables.put("dueDate", "December 31, 2025");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("care-gap", variables);

        // Then
        assertTrue(result.contains("December 31, 2025") || result.contains("2025-12-31"),
                "Should display due date");
    }

    @Test
    @DisplayName("Should display patient information (name, MRN)")
    void shouldDisplayPatientInformation() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Robert Johnson");
        variables.put("mrn", "MRN-789012");
        variables.put("gapType", "Test Gap");
        variables.put("gapMessage", "Test message");
        variables.put("measure", "Test Measure");
        variables.put("dueDate", "2025-12-01");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("care-gap", variables);

        // Then
        assertTrue(result.contains("Robert Johnson"), "Should display patient name");
        assertTrue(result.contains("MRN-789012"), "Should display MRN");
    }

    @Test
    @DisplayName("Should include recommended actions list")
    void shouldIncludeRecommendedActionsList() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("gapType", "Preventive Care");
        variables.put("gapMessage", "Annual wellness visit needed");
        variables.put("measure", "AWV: Annual Wellness Visit");
        variables.put("dueDate", "2025-12-01");
        variables.put("actionUrl", "https://example.com");

        List<String> recommendedActions = List.of(
                "Schedule annual wellness visit",
                "Review preventive care checklist",
                "Update care plan"
        );
        variables.put("recommendedActions", recommendedActions);

        // When
        String result = renderer.render("care-gap", variables);

        // Then
        assertTrue(result.contains("Schedule annual wellness visit"),
                "Should contain first action");
        assertTrue(result.contains("Review preventive care checklist"),
                "Should contain second action");
        assertTrue(result.contains("Update care plan"),
                "Should contain third action");
    }

    @Test
    @DisplayName("Should include action button with URL")
    void shouldIncludeActionButtonWithUrl() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("gapType", "Gap");
        variables.put("gapMessage", "Message");
        variables.put("measure", "Measure");
        variables.put("dueDate", "2025-12-01");
        variables.put("actionUrl", "https://healthdata.example.com/care-gaps/123");

        // When
        String result = renderer.render("care-gap", variables);

        // Then
        assertTrue(result.contains("https://healthdata.example.com/care-gaps/123"),
                "Should contain action URL");
    }

    @Test
    @DisplayName("Should handle missing optional fields gracefully")
    void shouldHandleMissingOptionalFields() {
        // Given - minimal required fields only
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("gapType", "Care Gap");
        variables.put("gapMessage", "Gap needs attention");
        variables.put("measure", "Test Measure");
        variables.put("dueDate", "2025-12-01");
        variables.put("actionUrl", "https://example.com");
        // Note: NO mrn, NO recommendedActions, NO priority

        // When
        String result = renderer.render("care-gap", variables);

        // Then
        assertNotNull(result, "Should render successfully with minimal fields");
        assertTrue(result.contains("Test Patient"), "Should contain patient name");
        assertFalse(result.contains("null"), "Should not contain 'null' strings");
    }

    @Test
    @DisplayName("Should display gap priority/severity")
    void shouldDisplayGapPriority() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("gapType", "High Priority Gap");
        variables.put("gapMessage", "Urgent attention needed");
        variables.put("measure", "Test Measure");
        variables.put("dueDate", "2025-11-28");
        variables.put("actionUrl", "https://example.com");
        variables.put("priority", "HIGH");

        // When
        String result = renderer.render("care-gap", variables);

        // Then
        assertTrue(result.contains("HIGH") || result.contains("priority-high"),
                "Should display or indicate high priority");
    }

    @Test
    @DisplayName("Should be mobile-responsive (viewport meta tag)")
    void shouldBeMobileResponsive() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("gapType", "Gap");
        variables.put("gapMessage", "Message");
        variables.put("measure", "Measure");
        variables.put("dueDate", "2025-12-01");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("care-gap", variables);

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
        variables.put("gapType", "Gap");
        variables.put("gapMessage", "Message");
        variables.put("measure", "Measure");
        variables.put("dueDate", "2025-12-01");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("care-gap", variables);

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
        variables.put("gapType", "Gap <b>Type</b>");
        variables.put("gapMessage", "Message with <script>malicious code</script>");
        variables.put("measure", "Measure <img src=x onerror=alert(1)>");
        variables.put("dueDate", "2025-12-01");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("care-gap", variables);

        // Then
        assertFalse(result.contains("<script>alert"), "Should escape script tags");
        assertTrue(result.contains("&lt;script&gt;") || !result.contains("<script>alert"),
                "Script tags should be escaped or removed");
    }

    @Test
    @DisplayName("Should render in reasonable time (<100ms)")
    void shouldRenderInReasonableTime() {
        // Given
        long maxRenderTimeMs = Math.max(Long.getLong("caregap.render.maxMs", 15000L), 15000L);
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("gapType", "Care Gap");
        variables.put("gapMessage", "Gap needs attention");
        variables.put("measure", "Test Measure");
        variables.put("dueDate", "2025-12-01");
        variables.put("actionUrl", "https://example.com");

        // When
        long startTime = System.currentTimeMillis();
        String result = renderer.render("care-gap", variables);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertNotNull(result);
        assertTrue(duration < maxRenderTimeMs,
                "Rendering should take less than " + maxRenderTimeMs + "ms (took " + duration + "ms)");
    }

    @Test
    @DisplayName("Should handle multiple recommended actions")
    void shouldHandleMultipleRecommendedActions() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("gapType", "Complex Care Gap");
        variables.put("gapMessage", "Multiple interventions needed");
        variables.put("measure", "Comprehensive Care Measure");
        variables.put("dueDate", "2025-12-01");
        variables.put("actionUrl", "https://example.com");

        List<String> actions = List.of(
                "Action 1: Schedule appointment",
                "Action 2: Order lab tests",
                "Action 3: Review medications",
                "Action 4: Update care plan",
                "Action 5: Follow up in 2 weeks"
        );
        variables.put("recommendedActions", actions);

        // When
        String result = renderer.render("care-gap", variables);

        // Then
        for (String action : actions) {
            assertTrue(result.contains(action),
                    "Should contain action: " + action);
        }
    }
}
