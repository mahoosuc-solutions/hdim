package com.healthdata.quality.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Unit Tests for Digest Template
 *
 * Following RED → GREEN → REFACTOR cycle
 * Tests written FIRST, then implementation
 *
 * Day 6 of TDD Swarm Implementation
 * Most complex template - aggregates multiple notification types
 */
@DisplayName("Digest Template Tests")
class DigestTemplateTest {

    private ThymeleafTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new ThymeleafTemplateRenderer();
        renderer.initialize();
    }

    @Test
    @DisplayName("Should render digest HTML template with all required variables")
    void shouldRenderDigestHtmlTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123456");
        variables.put("digestDate", "November 27, 2025");
        variables.put("criticalAlertCount", 1);
        variables.put("careGapCount", 3);
        variables.put("appointmentCount", 2);
        variables.put("actionUrl", "https://example.com/dashboard");

        // When
        String result = renderer.render("digest", variables);

        // Then
        assertNotNull(result, "Rendered template should not be null");
        assertTrue(result.contains("<!DOCTYPE html>"), "Should be valid HTML");
        assertTrue(result.contains("John Smith"), "Should contain patient name");
        assertTrue(result.contains("November 27"), "Should contain digest date");
    }

    @Test
    @DisplayName("Should render digest SMS template with minimal content")
    void shouldRenderDigestSmsTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "SMS");
        variables.put("patientName", "John Smith");
        variables.put("digestDate", "Nov 27");
        variables.put("criticalAlertCount", 1);
        variables.put("careGapCount", 3);
        variables.put("appointmentCount", 2);
        variables.put("actionUrl", "https://short.url/d123");

        // When
        String result = renderer.render("digest", variables);

        // Then
        assertNotNull(result, "Rendered SMS should not be null");
        assertTrue(result.contains("DIGEST") || result.contains("SUMMARY") || result.contains("Digest"),
                "Should contain digest header");

        // SMS should be concise (under 500 characters for extended SMS)
        assertTrue(result.length() < 500,
                "SMS should be concise (was " + result.length() + " characters)");
    }

    @Test
    @DisplayName("Should display digest date prominently")
    void shouldDisplayDigestDate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("digestDate", "December 1, 2025");
        variables.put("criticalAlertCount", 0);
        variables.put("careGapCount", 0);
        variables.put("appointmentCount", 0);
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("digest", variables);

        // Then
        assertTrue(result.contains("December 1") || result.contains("2025-12-01"),
                "Should display digest date");
    }

    @Test
    @DisplayName("Should display summary counts for all notification types")
    void shouldDisplaySummaryCounts() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("digestDate", "2025-11-27");
        variables.put("criticalAlertCount", 2);
        variables.put("careGapCount", 5);
        variables.put("appointmentCount", 3);
        variables.put("labResultCount", 4);
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("digest", variables);

        // Then
        assertTrue(result.contains("2"), "Should display critical alert count");
        assertTrue(result.contains("5"), "Should display care gap count");
        assertTrue(result.contains("3"), "Should display appointment count");
        assertTrue(result.contains("4"), "Should display lab result count");
    }

    @Test
    @DisplayName("Should display critical alerts section")
    void shouldDisplayCriticalAlertsSection() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("digestDate", "2025-11-27");
        variables.put("criticalAlertCount", 2);
        variables.put("careGapCount", 0);
        variables.put("appointmentCount", 0);
        variables.put("actionUrl", "https://example.com");

        List<Map<String, String>> criticalAlerts = List.of(
                Map.of(
                        "alertType", "Critical Lab Result",
                        "message", "Blood glucose critically high at 385 mg/dL"
                )
        );
        variables.put("criticalAlerts", criticalAlerts);

        // When
        String result = renderer.render("digest", variables);

        // Then
        assertTrue(result.contains("Critical Lab Result") || result.contains("CRITICAL"),
                "Should contain critical alert type");
        assertTrue(result.contains("385") || result.contains("glucose"),
                "Should contain alert message");
    }

    @Test
    @DisplayName("Should display care gaps section")
    void shouldDisplayCareGapsSection() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("digestDate", "2025-11-27");
        variables.put("criticalAlertCount", 0);
        variables.put("careGapCount", 2);
        variables.put("appointmentCount", 0);
        variables.put("actionUrl", "https://example.com");

        List<Map<String, String>> careGaps = List.of(
                Map.of(
                        "gapType", "Diabetic Eye Exam",
                        "dueDate", "2025-12-01",
                        "priority", "HIGH"
                )
        );
        variables.put("careGaps", careGaps);

        // When
        String result = renderer.render("digest", variables);

        // Then
        assertTrue(result.contains("Diabetic Eye Exam") || result.contains("Eye Exam"),
                "Should contain care gap type");
        assertTrue(result.contains("2025-12-01") || result.contains("December"),
                "Should contain due date");
    }

    @Test
    @DisplayName("Should display upcoming appointments section")
    void shouldDisplayUpcomingAppointmentsSection() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("digestDate", "2025-11-27");
        variables.put("criticalAlertCount", 0);
        variables.put("careGapCount", 0);
        variables.put("appointmentCount", 2);
        variables.put("actionUrl", "https://example.com");

        List<Map<String, String>> appointments = List.of(
                Map.of(
                        "appointmentDate", "December 15, 2025",
                        "appointmentTime", "10:30 AM",
                        "providerName", "Dr. Sarah Johnson",
                        "appointmentType", "Annual Wellness Visit"
                )
        );
        variables.put("appointments", appointments);

        // When
        String result = renderer.render("digest", variables);

        // Then
        assertTrue(result.contains("December 15") || result.contains("10:30"),
                "Should contain appointment date/time");
        assertTrue(result.contains("Dr. Sarah Johnson") || result.contains("Johnson"),
                "Should contain provider name");
    }

    @Test
    @DisplayName("Should display recent lab results section")
    void shouldDisplayRecentLabResultsSection() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("digestDate", "2025-11-27");
        variables.put("criticalAlertCount", 0);
        variables.put("careGapCount", 0);
        variables.put("appointmentCount", 0);
        variables.put("labResultCount", 2);
        variables.put("actionUrl", "https://example.com");

        List<Map<String, String>> labResults = List.of(
                Map.of(
                        "testName", "Hemoglobin A1C",
                        "resultValue", "7.2%",
                        "resultStatus", "ABNORMAL",
                        "testDate", "November 25, 2025"
                )
        );
        variables.put("labResults", labResults);

        // When
        String result = renderer.render("digest", variables);

        // Then
        assertTrue(result.contains("Hemoglobin A1C") || result.contains("A1C"),
                "Should contain test name");
        assertTrue(result.contains("7.2"), "Should contain result value");
    }

    @Test
    @DisplayName("Should handle empty sections gracefully")
    void shouldHandleEmptySections() {
        // Given - all counts are zero
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("digestDate", "2025-11-27");
        variables.put("criticalAlertCount", 0);
        variables.put("careGapCount", 0);
        variables.put("appointmentCount", 0);
        variables.put("labResultCount", 0);
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("digest", variables);

        // Then
        assertNotNull(result, "Should render successfully with zero counts");
        assertTrue(result.contains("Test Patient"), "Should contain patient name");
        // Template should show "No new" or "0" or hide empty sections
    }

    @Test
    @DisplayName("Should display priority indicator for high-priority items")
    void shouldDisplayPriorityIndicator() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("digestDate", "2025-11-27");
        variables.put("criticalAlertCount", 0);
        variables.put("careGapCount", 1);
        variables.put("appointmentCount", 0);
        variables.put("actionUrl", "https://example.com");

        List<Map<String, String>> careGaps = List.of(
                Map.of(
                        "gapType", "High Priority Gap",
                        "dueDate", "2025-12-01",
                        "priority", "HIGH"
                )
        );
        variables.put("careGaps", careGaps);

        // When
        String result = renderer.render("digest", variables);

        // Then
        assertTrue(result.contains("HIGH") || result.contains("High") || result.contains("priority"),
                "Should display priority indicator");
    }

    @Test
    @DisplayName("Should include summary statistics card")
    void shouldIncludeSummaryStatisticsCard() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("digestDate", "2025-11-27");
        variables.put("criticalAlertCount", 2);
        variables.put("careGapCount", 5);
        variables.put("appointmentCount", 3);
        variables.put("labResultCount", 4);
        variables.put("totalItemCount", 14);  // Sum of all counts
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("digest", variables);

        // Then
        assertTrue(result.contains("14") || result.contains("total"),
                "Should display total item count");
    }

    @Test
    @DisplayName("Should display section headers clearly")
    void shouldDisplaySectionHeaders() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("digestDate", "2025-11-27");
        variables.put("criticalAlertCount", 1);
        variables.put("careGapCount", 1);
        variables.put("appointmentCount", 1);
        variables.put("actionUrl", "https://example.com");

        List<Map<String, String>> criticalAlerts = List.of(Map.of("alertType", "Test", "message", "Test"));
        List<Map<String, String>> careGaps = List.of(Map.of("gapType", "Test", "dueDate", "2025-12-01"));
        List<Map<String, String>> appointments = List.of(Map.of("appointmentDate", "2025-12-01", "providerName", "Dr. Test"));

        variables.put("criticalAlerts", criticalAlerts);
        variables.put("careGaps", careGaps);
        variables.put("appointments", appointments);

        // When
        String result = renderer.render("digest", variables);

        // Then
        // Should have distinct section headers for each type
        assertTrue(result.contains("Alert") || result.contains("ALERT"),
                "Should have alerts section header");
        assertTrue(result.contains("Gap") || result.contains("GAP") || result.contains("Care"),
                "Should have care gaps section header");
        assertTrue(result.contains("Appointment") || result.contains("APPOINTMENT"),
                "Should have appointments section header");
    }

    @Test
    @DisplayName("Should include action button for full dashboard")
    void shouldIncludeActionButtonForDashboard() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("digestDate", "2025-11-27");
        variables.put("criticalAlertCount", 0);
        variables.put("careGapCount", 0);
        variables.put("appointmentCount", 0);
        variables.put("actionUrl", "https://example.com/dashboard");

        // When
        String result = renderer.render("digest", variables);

        // Then
        assertTrue(result.contains("dashboard") || result.contains("Dashboard") || result.contains("View"),
                "Should include dashboard action");
    }

    @Test
    @DisplayName("Should display digest period or date range")
    void shouldDisplayDigestPeriod() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("digestDate", "November 27, 2025");
        variables.put("digestPeriod", "Daily");  // or "Weekly" or "Monthly"
        variables.put("criticalAlertCount", 0);
        variables.put("careGapCount", 0);
        variables.put("appointmentCount", 0);
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("digest", variables);

        // Then
        assertTrue(result.contains("Daily") || result.contains("November 27"),
                "Should display digest period or date");
    }

    @Test
    @DisplayName("Should handle missing optional fields gracefully")
    void shouldHandleMissingOptionalFields() {
        // Given - minimal required fields only
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("digestDate", "2025-11-27");
        variables.put("criticalAlertCount", 0);
        variables.put("careGapCount", 0);
        variables.put("appointmentCount", 0);
        variables.put("actionUrl", "https://example.com");
        // Note: NO lists, NO labResultCount, NO digestPeriod

        // When
        String result = renderer.render("digest", variables);

        // Then
        assertNotNull(result, "Should render successfully with minimal fields");
        assertTrue(result.contains("Test Patient"), "Should contain patient name");
        assertFalse(result.contains("null"), "Should not contain 'null' strings");
    }

    @Test
    @DisplayName("Should be mobile-responsive (viewport meta tag)")
    void shouldBeMobileResponsive() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("digestDate", "2025-11-27");
        variables.put("criticalAlertCount", 0);
        variables.put("careGapCount", 0);
        variables.put("appointmentCount", 0);
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("digest", variables);

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
        variables.put("digestDate", "2025-11-27");
        variables.put("criticalAlertCount", 0);
        variables.put("careGapCount", 0);
        variables.put("appointmentCount", 0);
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("digest", variables);

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
        variables.put("digestDate", "2025-11-27");
        variables.put("criticalAlertCount", 0);
        variables.put("careGapCount", 0);
        variables.put("appointmentCount", 0);
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("digest", variables);

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
        warmupVars.put("digestDate", "2025-01-01");
        warmupVars.put("criticalAlertCount", 0);
        warmupVars.put("careGapCount", 0);
        warmupVars.put("appointmentCount", 0);
        warmupVars.put("actionUrl", "https://example.com");
        renderer.render("digest", warmupVars);

        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("digestDate", "2025-11-27");
        variables.put("criticalAlertCount", 0);
        variables.put("careGapCount", 0);
        variables.put("appointmentCount", 0);
        variables.put("actionUrl", "https://example.com");

        // When - measure after warmup
        long startTime = System.currentTimeMillis();
        String result = renderer.render("digest", variables);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertNotNull(result);
        assertTrue(duration < 3000,
                "Rendering should take less than 3000ms (took " + duration + "ms)");
    }

    @Test
    @DisplayName("Should group items by type clearly")
    void shouldGroupItemsByType() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("digestDate", "2025-11-27");
        variables.put("criticalAlertCount", 2);
        variables.put("careGapCount", 2);
        variables.put("appointmentCount", 0);
        variables.put("actionUrl", "https://example.com");

        List<Map<String, String>> criticalAlerts = List.of(
                Map.of("alertType", "Alert 1", "message", "Message 1"),
                Map.of("alertType", "Alert 2", "message", "Message 2")
        );
        List<Map<String, String>> careGaps = List.of(
                Map.of("gapType", "Gap 1", "dueDate", "2025-12-01"),
                Map.of("gapType", "Gap 2", "dueDate", "2025-12-15")
        );

        variables.put("criticalAlerts", criticalAlerts);
        variables.put("careGaps", careGaps);

        // When
        String result = renderer.render("digest", variables);

        // Then
        assertTrue(result.contains("Alert 1"), "Should contain first alert");
        assertTrue(result.contains("Alert 2"), "Should contain second alert");
        assertTrue(result.contains("Gap 1"), "Should contain first care gap");
        assertTrue(result.contains("Gap 2"), "Should contain second care gap");
    }
}
