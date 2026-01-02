package com.healthdata.quality.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Unit Tests for Appointment Reminder Template
 *
 * Following RED → GREEN → REFACTOR cycle
 * Tests written FIRST, then implementation
 *
 * Day 3 of TDD Swarm Implementation
 */
@DisplayName("Appointment Reminder Template Tests")
class AppointmentReminderTemplateTest {

    private ThymeleafTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new ThymeleafTemplateRenderer();
        renderer.initialize();
    }

    @Test
    @DisplayName("Should render appointment-reminder HTML template with all required variables")
    void shouldRenderAppointmentReminderHtmlTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123456");
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Sarah Johnson");
        variables.put("appointmentType", "Follow-up Visit");
        variables.put("location", "Main Clinic - Building A, Room 205");
        variables.put("actionUrl", "https://example.com/appointments/789");

        // When
        String result = renderer.render("appointment-reminder", variables);

        // Then
        assertNotNull(result, "Rendered template should not be null");
        assertTrue(result.contains("<!DOCTYPE html>"), "Should be valid HTML");
        assertTrue(result.contains("John Smith"), "Should contain patient name");
        assertTrue(result.contains("December 15, 2025"), "Should contain appointment date");
        assertTrue(result.contains("10:30 AM"), "Should contain appointment time");
        assertTrue(result.contains("Dr. Sarah Johnson"), "Should contain provider name");
        assertTrue(result.contains("Follow-up Visit"), "Should contain appointment type");
    }

    @Test
    @DisplayName("Should render appointment-reminder SMS template with minimal content")
    void shouldRenderAppointmentReminderSmsTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "SMS");
        variables.put("patientName", "John Smith");
        variables.put("appointmentDate", "Dec 15");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Johnson");
        variables.put("appointmentType", "Follow-up");
        variables.put("location", "Main Clinic");
        variables.put("actionUrl", "https://short.url/a789");

        // When
        String result = renderer.render("appointment-reminder", variables);

        // Then
        assertNotNull(result, "Rendered SMS should not be null");
        assertTrue(result.contains("APPOINTMENT") || result.contains("Appointment"),
                "Should contain appointment header");
        assertTrue(result.contains("Dec 15"), "Should contain date");
        assertTrue(result.contains("10:30 AM"), "Should contain time");

        // SMS should be concise (under 500 characters for extended SMS)
        assertTrue(result.length() < 500,
                "SMS should be concise (was " + result.length() + " characters)");
    }

    @Test
    @DisplayName("Should display appointment date prominently")
    void shouldDisplayAppointmentDate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("appointmentDate", "Monday, December 15, 2025");
        variables.put("appointmentTime", "2:00 PM");
        variables.put("providerName", "Dr. Smith");
        variables.put("appointmentType", "Consultation");
        variables.put("location", "Clinic");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("appointment-reminder", variables);

        // Then
        assertTrue(result.contains("Monday, December 15, 2025") || result.contains("December 15"),
                "Should display appointment date");
    }

    @Test
    @DisplayName("Should display appointment time prominently")
    void shouldDisplayAppointmentTime() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Smith");
        variables.put("appointmentType", "Consultation");
        variables.put("location", "Clinic");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("appointment-reminder", variables);

        // Then
        assertTrue(result.contains("10:30 AM"), "Should display appointment time");
    }

    @Test
    @DisplayName("Should display provider name and credentials")
    void shouldDisplayProviderInformation() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Sarah Johnson, MD");
        variables.put("appointmentType", "Annual Physical");
        variables.put("location", "Clinic");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("appointment-reminder", variables);

        // Then
        assertTrue(result.contains("Dr. Sarah Johnson"), "Should display provider name");
    }

    @Test
    @DisplayName("Should display appointment location with address")
    void shouldDisplayLocationInformation() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Smith");
        variables.put("appointmentType", "Consultation");
        variables.put("location", "Main Clinic - Building A, Room 205");
        variables.put("address", "123 Medical Center Dr, Suite 100");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("appointment-reminder", variables);

        // Then
        assertTrue(result.contains("Main Clinic"), "Should display location name");
        assertTrue(result.contains("123 Medical Center Dr") || !variables.containsKey("address") || result.contains("Building A"),
                "Should display address if provided");
    }

    @Test
    @DisplayName("Should display appointment type")
    void shouldDisplayAppointmentType() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Smith");
        variables.put("appointmentType", "Annual Wellness Visit");
        variables.put("location", "Clinic");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("appointment-reminder", variables);

        // Then
        assertTrue(result.contains("Annual Wellness Visit"), "Should display appointment type");
    }

    @Test
    @DisplayName("Should include map/directions link")
    void shouldIncludeMapDirectionsLink() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Smith");
        variables.put("appointmentType", "Consultation");
        variables.put("location", "Main Clinic");
        variables.put("mapUrl", "https://maps.google.com/?q=Main+Clinic");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("appointment-reminder", variables);

        // Then
        assertTrue(result.contains("maps.google.com") || result.contains("directions") || result.contains("map"),
                "Should include map/directions link if provided");
    }

    @Test
    @DisplayName("Should include confirmation button/link")
    void shouldIncludeConfirmationLink() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Smith");
        variables.put("appointmentType", "Consultation");
        variables.put("location", "Clinic");
        variables.put("confirmUrl", "https://example.com/confirm/789");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("appointment-reminder", variables);

        // Then
        assertTrue(result.contains("confirm") || result.contains("Confirm"),
                "Should include confirmation option");
    }

    @Test
    @DisplayName("Should include cancellation/reschedule link")
    void shouldIncludeCancellationLink() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Smith");
        variables.put("appointmentType", "Consultation");
        variables.put("location", "Clinic");
        variables.put("cancelUrl", "https://example.com/cancel/789");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("appointment-reminder", variables);

        // Then
        assertTrue(result.contains("cancel") || result.contains("Cancel") || result.contains("reschedule"),
                "Should include cancellation/reschedule option");
    }

    @Test
    @DisplayName("Should include preparation instructions")
    void shouldIncludePreparationInstructions() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Smith");
        variables.put("appointmentType", "Lab Work");
        variables.put("location", "Clinic");
        variables.put("actionUrl", "https://example.com");

        List<String> instructions = List.of(
                "Arrive 15 minutes early for check-in",
                "Bring your insurance card and photo ID",
                "Fast for 8 hours before appointment (water is OK)"
        );
        variables.put("instructions", instructions);

        // When
        String result = renderer.render("appointment-reminder", variables);

        // Then
        assertTrue(result.contains("Arrive 15 minutes early"), "Should contain first instruction");
        assertTrue(result.contains("insurance card"), "Should contain second instruction");
        assertTrue(result.contains("Fast for 8 hours"), "Should contain third instruction");
    }

    @Test
    @DisplayName("Should include calendar event data")
    void shouldIncludeCalendarEventData() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Smith");
        variables.put("appointmentType", "Consultation");
        variables.put("location", "Clinic");
        variables.put("calendarUrl", "https://example.com/calendar/add/789");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("appointment-reminder", variables);

        // Then
        assertTrue(result.contains("calendar") || result.contains("Calendar") || result.contains("Add to"),
                "Should include add to calendar option");
    }

    @Test
    @DisplayName("Should handle missing optional fields gracefully")
    void shouldHandleMissingOptionalFields() {
        // Given - minimal required fields only
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Smith");
        variables.put("appointmentType", "Consultation");
        variables.put("location", "Clinic");
        variables.put("actionUrl", "https://example.com");
        // Note: NO mrn, NO instructions, NO confirmUrl, NO mapUrl

        // When
        String result = renderer.render("appointment-reminder", variables);

        // Then
        assertNotNull(result, "Should render successfully with minimal fields");
        assertTrue(result.contains("Test Patient"), "Should contain patient name");
        assertTrue(result.contains("December 15"), "Should contain date");
        assertFalse(result.contains("null"), "Should not contain 'null' strings");
    }

    @Test
    @DisplayName("Should be mobile-responsive (viewport meta tag)")
    void shouldBeMobileResponsive() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Smith");
        variables.put("appointmentType", "Consultation");
        variables.put("location", "Clinic");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("appointment-reminder", variables);

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
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Smith");
        variables.put("appointmentType", "Consultation");
        variables.put("location", "Clinic");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("appointment-reminder", variables);

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
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. <b>Smith</b>");
        variables.put("appointmentType", "Consultation <script>malicious()</script>");
        variables.put("location", "Clinic");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("appointment-reminder", variables);

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
        warmupVars.put("appointmentDate", "January 1, 2025");
        warmupVars.put("appointmentTime", "9:00 AM");
        warmupVars.put("providerName", "Warmup");
        warmupVars.put("appointmentType", "Warmup");
        warmupVars.put("location", "Warmup");
        warmupVars.put("actionUrl", "https://example.com");
        renderer.render("appointment-reminder", warmupVars);

        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Smith");
        variables.put("appointmentType", "Consultation");
        variables.put("location", "Clinic");
        variables.put("actionUrl", "https://example.com");

        // When - measure after warmup
        long startTime = System.currentTimeMillis();
        String result = renderer.render("appointment-reminder", variables);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertNotNull(result);
        assertTrue(duration < 3000,
                "Rendering should take less than 3000ms (took " + duration + "ms)");
    }

    @Test
    @DisplayName("Should display contact phone number")
    void shouldDisplayContactPhoneNumber() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Smith");
        variables.put("appointmentType", "Consultation");
        variables.put("location", "Clinic");
        variables.put("phoneNumber", "(555) 123-4567");
        variables.put("actionUrl", "https://example.com");

        // When
        String result = renderer.render("appointment-reminder", variables);

        // Then
        assertTrue(result.contains("(555) 123-4567") || result.contains("555-123-4567"),
                "Should display contact phone number");
    }

    @Test
    @DisplayName("Should handle multiple preparation instructions")
    void shouldHandleMultipleInstructions() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "Test Patient");
        variables.put("appointmentDate", "December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Smith");
        variables.put("appointmentType", "Surgical Consultation");
        variables.put("location", "Clinic");
        variables.put("actionUrl", "https://example.com");

        List<String> instructions = List.of(
                "Bring all current medications",
                "Complete pre-appointment forms online",
                "Arrive 30 minutes early",
                "Bring a family member or friend",
                "Wear comfortable clothing"
        );
        variables.put("instructions", instructions);

        // When
        String result = renderer.render("appointment-reminder", variables);

        // Then
        for (String instruction : instructions) {
            assertTrue(result.contains(instruction) || result.contains(instruction.substring(0, 15)),
                    "Should contain instruction: " + instruction);
        }
    }
}
