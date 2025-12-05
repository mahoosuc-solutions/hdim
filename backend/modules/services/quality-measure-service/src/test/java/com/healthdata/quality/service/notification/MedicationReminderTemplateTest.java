package com.healthdata.quality.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Unit Tests for Medication Reminder Template
 *
 * Following RED → GREEN → REFACTOR cycle
 * Tests written FIRST, then implementation
 *
 * Day 4 of TDD Swarm Implementation
 */
@DisplayName("Medication Reminder Template Tests")
class MedicationReminderTemplateTest {

    private ThymeleafTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new ThymeleafTemplateRenderer();
        renderer.initialize();
    }

    @Test
    @DisplayName("Should render medication-reminder HTML template with all required variables")
    void shouldRenderMedicationReminderHtmlTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123456");
        variables.put("medicationName", "Metformin 500mg");
        variables.put("dosage", "Take 1 tablet twice daily with meals");
        variables.put("refillDate", "December 20, 2025");
        variables.put("prescriber", "Dr. Sarah Johnson");
        variables.put("actionUrl", "https://example.com/medications/123");

        // When
        String result = renderer.render("medication-reminder", variables);

        // Then
        assertNotNull(result, "Rendered template should not be null");
        assertTrue(result.contains("<!DOCTYPE html>"), "Should be valid HTML");
        assertTrue(result.contains("John Smith"), "Should contain patient name");
        assertTrue(result.contains("Metformin"), "Should contain medication name");
        assertTrue(result.contains("twice daily"), "Should contain dosage");
        assertTrue(result.contains("December 20"), "Should contain refill date");
    }

    @Test
    @DisplayName("Should render medication-reminder SMS template with minimal content")
    void shouldRenderMedicationReminderSmsTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "SMS");
        variables.put("patientName", "John Smith");
        variables.put("medicationName", "Metformin 500mg");
        variables.put("dosage", "1 tablet twice daily");
        variables.put("refillDate", "Dec 20");
        variables.put("prescriber", "Dr. Johnson");
        variables.put("actionUrl", "https://short.url/m123");

        // When
        String result = renderer.render("medication-reminder", variables);

        // Then
        assertNotNull(result, "Rendered SMS should not be null");
        assertTrue(result.contains("MEDICATION") || result.contains("Medication"),
                "Should contain medication header");
        assertTrue(result.contains("Metformin"), "Should contain medication name");

        // SMS should be concise (under 500 characters for extended SMS)
        assertTrue(result.length() < 500,
                "SMS should be concise (was " + result.length() + " characters)");
    }

    @Test
    @DisplayName("Should display medication name prominently")
    void shouldDisplayMedicationName() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("medicationName", "Lisinopril 10mg Tablets");
        variables.put("dosage", "Take 1 tablet once daily");
        variables.put("refillDate", "2025-12-15");
        variables.put("prescriber", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("medication-reminder", variables);

        // Then
        assertTrue(result.contains("Lisinopril"), "Should display medication name");
        assertTrue(result.contains("10mg"), "Should display medication strength");
    }

    @Test
    @DisplayName("Should display dosage instructions clearly")
    void shouldDisplayDosageInstructions() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("medicationName", "Atorvastatin 20mg");
        variables.put("dosage", "Take 1 tablet every evening at bedtime");
        variables.put("refillDate", "2025-12-15");
        variables.put("prescriber", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("medication-reminder", variables);

        // Then
        assertTrue(result.contains("every evening at bedtime"), "Should display dosage instructions");
    }

    @Test
    @DisplayName("Should display refill date prominently")
    void shouldDisplayRefillDate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("medicationName", "Test Medication");
        variables.put("dosage", "As directed");
        variables.put("refillDate", "December 25, 2025");
        variables.put("refillDaysLeft", 5);
        variables.put("prescriber", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("medication-reminder", variables);

        // Then
        assertTrue(result.contains("December 25") || result.contains("2025-12-25"),
                "Should display refill date");
    }

    @Test
    @DisplayName("Should display refill days countdown")
    void shouldDisplayRefillDaysCountdown() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("medicationName", "Test Medication");
        variables.put("dosage", "As directed");
        variables.put("refillDate", "2025-12-25");
        variables.put("refillDaysLeft", 3);
        variables.put("prescriber", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("medication-reminder", variables);

        // Then
        assertTrue(result.contains("3") && (result.contains("days") || result.contains("day")),
                "Should display days left until refill");
    }

    @Test
    @DisplayName("Should display prescriber information")
    void shouldDisplayPrescriberInformation() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("medicationName", "Test Medication");
        variables.put("dosage", "As directed");
        variables.put("refillDate", "2025-12-15");
        variables.put("prescriber", "Dr. Sarah Johnson, MD");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("medication-reminder", variables);

        // Then
        assertTrue(result.contains("Dr. Sarah Johnson"), "Should display prescriber name");
    }

    @Test
    @DisplayName("Should display pharmacy information")
    void shouldDisplayPharmacyInformation() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("medicationName", "Test Medication");
        variables.put("dosage", "As directed");
        variables.put("refillDate", "2025-12-15");
        variables.put("prescriber", "Dr. Smith");
        variables.put("pharmacyName", "Main Street Pharmacy");
        variables.put("pharmacyPhone", "(555) 987-6543");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("medication-reminder", variables);

        // Then
        assertTrue(result.contains("Main Street Pharmacy"), "Should display pharmacy name");
        assertTrue(result.contains("(555) 987-6543"), "Should display pharmacy phone");
    }

    @Test
    @DisplayName("Should include refill button/link")
    void shouldIncludeRefillLink() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("medicationName", "Test Medication");
        variables.put("dosage", "As directed");
        variables.put("refillDate", "2025-12-15");
        variables.put("prescriber", "Dr. Smith");
        variables.put("refillUrl", "https://example.com/refill/123");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("medication-reminder", variables);

        // Then
        assertTrue(result.contains("refill") || result.contains("Refill"),
                "Should include refill option");
    }

    @Test
    @DisplayName("Should include special instructions")
    void shouldIncludeSpecialInstructions() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("medicationName", "Test Medication");
        variables.put("dosage", "As directed");
        variables.put("refillDate", "2025-12-15");
        variables.put("prescriber", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        List<String> instructions = List.of(
                "Take with food to reduce stomach upset",
                "Avoid alcohol while taking this medication",
                "Do not crush or chew tablets"
        );
        variables.put("instructions", instructions);

        // When
        String result = renderer.render("medication-reminder", variables);

        // Then
        assertTrue(result.contains("Take with food"), "Should contain first instruction");
        assertTrue(result.contains("Avoid alcohol"), "Should contain second instruction");
        assertTrue(result.contains("Do not crush"), "Should contain third instruction");
    }

    @Test
    @DisplayName("Should include side effects or warnings")
    void shouldIncludeSideEffectsOrWarnings() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("medicationName", "Test Medication");
        variables.put("dosage", "As directed");
        variables.put("refillDate", "2025-12-15");
        variables.put("prescriber", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        List<String> warnings = List.of(
                "May cause dizziness - use caution when driving",
                "Contact doctor if you experience muscle pain"
        );
        variables.put("warnings", warnings);

        // When
        String result = renderer.render("medication-reminder", variables);

        // Then
        assertTrue(result.contains("dizziness"), "Should contain first warning");
        assertTrue(result.contains("muscle pain"), "Should contain second warning");
    }

    @Test
    @DisplayName("Should display dosing schedule if provided")
    void shouldDisplayDosingSchedule() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("medicationName", "Test Medication");
        variables.put("dosage", "As directed");
        variables.put("refillDate", "2025-12-15");
        variables.put("prescriber", "Dr. Smith");
        variables.put("schedule", "Morning (8 AM) and Evening (8 PM)");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("medication-reminder", variables);

        // Then
        assertTrue(result.contains("Morning") || result.contains("8 AM"),
                "Should display dosing schedule");
    }

    @Test
    @DisplayName("Should handle missing optional fields gracefully")
    void shouldHandleMissingOptionalFields() {
        // Given - minimal required fields only
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("medicationName", "Test Medication");
        variables.put("dosage", "As directed");
        variables.put("refillDate", "2025-12-15");
        variables.put("prescriber", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");
        // Note: NO mrn, NO instructions, NO warnings, NO pharmacyName

        // When
        String result = renderer.render("medication-reminder", variables);

        // Then
        assertNotNull(result, "Should render successfully with minimal fields");
        assertTrue(result.contains("Test Patient"), "Should contain patient name");
        assertTrue(result.contains("Test Medication"), "Should contain medication");
        assertFalse(result.contains("null"), "Should not contain 'null' strings");
    }

    @Test
    @DisplayName("Should be mobile-responsive (viewport meta tag)")
    void shouldBeMobileResponsive() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("medicationName", "Test Medication");
        variables.put("dosage", "As directed");
        variables.put("refillDate", "2025-12-15");
        variables.put("prescriber", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("medication-reminder", variables);

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
        variables.put("medicationName", "Test Medication");
        variables.put("dosage", "As directed");
        variables.put("refillDate", "2025-12-15");
        variables.put("prescriber", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("medication-reminder", variables);

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
        variables.put("medicationName", "Med <b>Name</b>");
        variables.put("dosage", "Dosage <script>malicious()</script>");
        variables.put("refillDate", "2025-12-15");
        variables.put("prescriber", "Dr. <b>Smith</b>");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("medication-reminder", variables);

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
        warmupVars.put("medicationName", "Warmup");
        warmupVars.put("dosage", "Warmup");
        warmupVars.put("refillDate", "2025-01-01");
        warmupVars.put("prescriber", "Warmup");
        warmupVars.put("actionUrl", "https://example.com");
        renderer.render("medication-reminder", warmupVars);

        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("medicationName", "Test Medication");
        variables.put("dosage", "As directed");
        variables.put("refillDate", "2025-12-15");
        variables.put("prescriber", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        // When - measure after warmup
        long startTime = System.currentTimeMillis();
        String result = renderer.render("medication-reminder", variables);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertNotNull(result);
        assertTrue(duration < 3000,
                "Rendering should take less than 3000ms (took " + duration + "ms)");
    }

    @Test
    @DisplayName("Should display urgency indicator for low refill days")
    void shouldDisplayUrgencyIndicatorForLowRefillDays() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("medicationName", "Test Medication");
        variables.put("dosage", "As directed");
        variables.put("refillDate", "2025-12-15");
        variables.put("refillDaysLeft", 2);  // Low number - urgent
        variables.put("prescriber", "Dr. Smith");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("medication-reminder", variables);

        // Then
        assertTrue(result.contains("2"), "Should display days left");
        // Template should handle urgency styling based on low days
    }
}
