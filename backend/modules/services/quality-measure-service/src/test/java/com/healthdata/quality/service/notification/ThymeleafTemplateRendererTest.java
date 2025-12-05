package com.healthdata.quality.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ThymeleafTemplateRenderer
 */
@DisplayName("ThymeleafTemplateRenderer Tests")
class ThymeleafTemplateRendererTest {

    private ThymeleafTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new ThymeleafTemplateRenderer();
        renderer.initialize();
    }

    @Test
    @DisplayName("Should initialize template engines successfully")
    void shouldInitializeTemplateEngines() {
        assertNotNull(renderer, "Renderer should be initialized");
    }

    @Test
    @DisplayName("Should render critical-alert HTML template with all variables")
    void shouldRenderCriticalAlertHtmlTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123456");
        variables.put("dob", "01/15/1965");
        variables.put("age", "58 years");
        variables.put("alertType", "Critical Lab Result");
        variables.put("severity", "HIGH");
        variables.put("alertMessage", "Blood glucose level is critically high at 385 mg/dL");
        variables.put("actionUrl", "https://example.com/patients/123");
        variables.put("timestamp", "2025-11-27 14:30:00");
        variables.put("facilityName", "Memorial Healthcare");

        // When
        String result = renderer.render("critical-alert", variables);

        // Then
        assertNotNull(result, "Rendered template should not be null");
        assertFalse(result.isEmpty(), "Rendered template should not be empty");
        assertTrue(result.contains("<!DOCTYPE html>"), "Should be valid HTML");
        assertTrue(result.contains("John Smith"), "Should contain patient name");
        assertTrue(result.contains("MRN-123456"), "Should contain MRN");
        assertTrue(result.contains("Critical Lab Result"), "Should contain alert type");
        assertTrue(result.contains("Blood glucose level is critically high"), "Should contain alert message");
        assertTrue(result.contains("CRITICAL ALERT"), "Should contain header title");
    }

    @Test
    @DisplayName("Should render critical-alert SMS template")
    void shouldRenderCriticalAlertSmsTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "SMS");
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123456");
        variables.put("alertType", "Critical Lab");
        variables.put("alertMessage", "Glucose 385 mg/dL");
        variables.put("actionUrl", "https://short.url/p123");

        // When
        String result = renderer.render("critical-alert", variables);

        // Then
        assertNotNull(result, "Rendered SMS should not be null");
        assertFalse(result.isEmpty(), "Rendered SMS should not be empty");
        assertTrue(result.contains("CRITICAL ALERT"), "Should contain alert header");
        assertTrue(result.contains("John Smith"), "Should contain patient name");
        assertTrue(result.contains("MRN-123456"), "Should contain MRN");
        assertTrue(result.contains("Critical Lab"), "Should contain alert type");

        // SMS should be reasonably short (under 500 characters for extended SMS)
        assertTrue(result.length() < 500,
                "SMS should be concise (was " + result.length() + " characters)");
    }

    @Test
    @DisplayName("Should render template with optional details map")
    void shouldRenderTemplateWithDetailsMap() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Jane Doe");
        variables.put("mrn", "MRN-789");
        variables.put("alertType", "Test Alert");
        variables.put("severity", "MEDIUM");
        variables.put("alertMessage", "Test message");
        variables.put("actionUrl", "https://example.com");

        Map<String, String> details = new HashMap<>();
        details.put("Test Name", "Hemoglobin A1C");
        details.put("Result", "7.2%");
        details.put("Normal Range", "<7.0%");
        variables.put("details", details);

        // When
        String result = renderer.render("critical-alert", variables);

        // Then
        assertTrue(result.contains("Hemoglobin A1C"), "Should contain detail key");
        assertTrue(result.contains("7.2%"), "Should contain detail value");
        assertTrue(result.contains("Normal Range"), "Should contain another detail key");
    }

    @Test
    @DisplayName("Should render template with recommended actions list")
    void shouldRenderTemplateWithRecommendedActions() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123");
        variables.put("alertType", "Critical Alert");
        variables.put("severity", "HIGH");
        variables.put("alertMessage", "Urgent action needed");
        variables.put("actionUrl", "https://example.com");

        List<String> actions = List.of(
                "Review patient chart immediately",
                "Contact patient for follow-up",
                "Schedule urgent consultation"
        );
        variables.put("recommendedActions", actions);

        // When
        String result = renderer.render("critical-alert", variables);

        // Then
        assertTrue(result.contains("Review patient chart"), "Should contain first action");
        assertTrue(result.contains("Contact patient"), "Should contain second action");
        assertTrue(result.contains("Schedule urgent"), "Should contain third action");
        assertTrue(result.contains("Recommended Actions"), "Should contain section header");
    }

    @Test
    @DisplayName("Should handle missing optional fields gracefully")
    void shouldHandleMissingOptionalFields() {
        // Given - minimal required fields only
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "John Smith");
        variables.put("alertType", "Alert");
        variables.put("alertMessage", "Test message");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("critical-alert", variables);

        // Then
        assertNotNull(result, "Should render successfully with minimal fields");
        assertTrue(result.contains("John Smith"), "Should contain patient name");
        assertTrue(result.contains("Test message"), "Should contain alert message");

        // Optional fields should not appear if not provided
        assertFalse(result.contains("null"), "Should not contain 'null' strings");
    }

    @Test
    @DisplayName("Should handle different severity levels with correct CSS classes")
    void shouldHandleDifferentSeverityLevels() {
        // Test HIGH severity
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("alertType", "Alert");
        variables.put("severity", "HIGH");
        variables.put("alertMessage", "High severity message");
        variables.put("actionUrl", "https://example.com");

        String resultHigh = renderer.render("critical-alert", variables);
        assertTrue(resultHigh.contains("severity-high"), "Should have high severity class");

        // Test MEDIUM severity
        variables.put("severity", "MEDIUM");
        String resultMedium = renderer.render("critical-alert", variables);
        assertTrue(resultMedium.contains("severity-medium"), "Should have medium severity class");

        // Test LOW severity
        variables.put("severity", "LOW");
        String resultLow = renderer.render("critical-alert", variables);
        assertTrue(resultLow.contains("severity-low"), "Should have low severity class");
    }

    @Test
    @DisplayName("Should throw exception for non-existent template")
    void shouldThrowExceptionForNonExistentTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("test", "value");

        // When/Then
        assertThrows(ThymeleafTemplateRenderer.TemplateRenderException.class,
                () -> renderer.render("non-existent-template", variables),
                "Should throw exception for non-existent template");
    }

    @Test
    @DisplayName("Should throw exception for null template ID")
    void shouldThrowExceptionForNullTemplateId() {
        // Given
        Map<String, Object> variables = new HashMap<>();

        // When/Then
        assertThrows(Exception.class,
                () -> renderer.render(null, variables),
                "Should throw exception for null template ID");
    }

    @Test
    @DisplayName("Should throw exception for null variables")
    void shouldThrowExceptionForNullVariables() {
        // When/Then
        assertThrows(Exception.class,
                () -> renderer.render("critical-alert", null),
                "Should throw exception for null variables");
    }

    @Test
    @DisplayName("Should return correct default template for notification types")
    void shouldReturnCorrectDefaultTemplates() {
        assertEquals("critical-alert",
                renderer.getDefaultTemplate("EMAIL", "CLINICAL_ALERT"));

        assertEquals("critical-alert",
                renderer.getDefaultTemplate("SMS", "CRITICAL_ALERT"));

        assertEquals("care-gap",
                renderer.getDefaultTemplate("EMAIL", "CARE_GAP"));

        assertEquals("health-score",
                renderer.getDefaultTemplate("EMAIL", "HEALTH_SCORE_UPDATE"));

        assertEquals("appointment-reminder",
                renderer.getDefaultTemplate("EMAIL", "APPOINTMENT_REMINDER"));

        assertEquals("medication-reminder",
                renderer.getDefaultTemplate("EMAIL", "MEDICATION_REMINDER"));

        assertEquals("lab-result",
                renderer.getDefaultTemplate("EMAIL", "LAB_RESULT"));

        assertEquals("digest",
                renderer.getDefaultTemplate("EMAIL", "DIGEST"));
    }

    @Test
    @DisplayName("Should return default template for unknown notification type")
    void shouldReturnDefaultTemplateForUnknownType() {
        String result = renderer.getDefaultTemplate("EMAIL", "UNKNOWN_TYPE");
        assertEquals("default", result, "Should return 'default' for unknown types");
    }

    @Test
    @DisplayName("Should render HTML with mobile-responsive viewport meta tag")
    void shouldRenderHtmlWithMobileViewport() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123");
        variables.put("alertType", "Test");
        variables.put("alertMessage", "Message");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("critical-alert", variables);

        // Then
        assertTrue(result.contains("viewport"), "Should have viewport meta tag");
        assertTrue(result.contains("width=device-width"), "Should be mobile-responsive");
    }

    @Test
    @DisplayName("Should render HTML with HIPAA-compliant footer disclaimer")
    void shouldRenderHtmlWithHipaaDisclaimer() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123");
        variables.put("alertType", "Test");
        variables.put("alertMessage", "Message");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("critical-alert", variables);

        // Then
        assertTrue(result.contains("Protected Health Information"),
                "Should mention PHI in disclaimer");
        assertTrue(result.contains("HIPAA") || result.contains("automated alert"),
                "Should have compliance messaging");
    }

    @Test
    @DisplayName("Should escape HTML in user-provided content to prevent XSS")
    void shouldEscapeHtmlInUserContent() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "John <script>alert('xss')</script> Smith");
        variables.put("mrn", "MRN-123");
        variables.put("alertType", "Test <b>Alert</b>");
        variables.put("alertMessage", "Message with <script> tag");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("critical-alert", variables);

        // Then
        assertFalse(result.contains("<script>alert"), "Should escape script tags");
        // Thymeleaf escapes by default, so the content should be safe
        assertTrue(result.contains("&lt;script&gt;") || !result.contains("<script>alert"),
                "Script tags should be escaped or removed");
    }

    @Test
    @DisplayName("Should render template in reasonable time")
    void shouldRenderTemplateInReasonableTime() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123456");
        variables.put("alertType", "Critical Alert");
        variables.put("severity", "HIGH");
        variables.put("alertMessage", "Test message");
        variables.put("actionUrl", "https://example.com");

        // When
        long startTime = System.currentTimeMillis();
        String result = renderer.render("critical-alert", variables);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertNotNull(result);
        assertTrue(duration < 500, "Rendering should take less than 500ms (took " + duration + "ms)");
    }
}
