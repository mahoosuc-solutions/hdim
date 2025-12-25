package com.healthdata.quality.integration;

import com.healthdata.quality.QualityMeasureServiceApplication;
import com.healthdata.quality.config.TestMessagingConfiguration;
import com.healthdata.quality.config.TestWebSocketConfiguration;
import com.healthdata.quality.service.notification.TemplateRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the notification template system.
 * Tests all 6 templates (critical-alert, health-score, appointment-reminder,
 * medication-reminder, lab-result, digest) with the actual TemplateRenderer service.
 *
 * Day 7 of TDD Swarm: Integration & Mobile Testing
 */
@SpringBootTest(classes = QualityMeasureServiceApplication.class)
@ActiveProfiles("test")
@Import({TestMessagingConfiguration.class, TestWebSocketConfiguration.class})
@DisplayName("Notification Template Integration Tests")
class NotificationTemplateIntegrationTest {

    @Autowired
    private TemplateRenderer templateRenderer;

    private static final long MAX_RENDER_TIME_MS =
        Math.max(Long.getLong("template.render.maxMs", 15000L), 15000L);

    // ==================== Template 1: Critical Alert ====================

    @Test
    @DisplayName("Should render critical-alert EMAIL template with real Thymeleaf engine")
    void shouldRenderCriticalAlertEmailTemplate() {
        // Given
        Map<String, Object> variables = createCriticalAlertVariables("EMAIL");

        // When
        long startTime = System.currentTimeMillis();
        String rendered = templateRenderer.render("critical-alert", variables);
        long renderTime = System.currentTimeMillis() - startTime;

        // Then
        assertThat(rendered)
                .contains("<!DOCTYPE html>")
                .contains("CRITICAL ALERT")
                .contains("John Smith")
                .contains("blood glucose level is critically high") // Note: "Patient's" is HTML-escaped to "Patient&#39;s"
                .contains("Review patient chart immediately")
                .contains("HealthData-in-Motion");

        assertThat(renderTime).isLessThan(MAX_RENDER_TIME_MS); // Allow time for template engine warmup under full-suite load
    }

    @Test
    @DisplayName("Should render critical-alert SMS template")
    void shouldRenderCriticalAlertSmsTemplate() {
        // Given
        Map<String, Object> variables = createCriticalAlertVariables("SMS");

        // When
        String rendered = templateRenderer.render("critical-alert", variables);

        // Then
        assertThat(rendered)
                .doesNotContain("<!DOCTYPE html>")
                .contains("CRITICAL ALERT")
                .contains("John Smith")
                .contains("blood glucose level is critically high")
                .contains("View details:");

        assertThat(rendered.length()).isLessThan(500); // SMS length limit
    }

    // ==================== Template 2: Health Score ====================

    @Test
    @DisplayName("Should render health-score EMAIL template with real Thymeleaf engine")
    void shouldRenderHealthScoreEmailTemplate() {
        // Given
        Map<String, Object> variables = createHealthScoreVariables("EMAIL");

        // When
        long startTime = System.currentTimeMillis();
        String rendered = templateRenderer.render("health-score", variables);
        long renderTime = System.currentTimeMillis() - startTime;

        // Then
        assertThat(rendered)
                .contains("<!DOCTYPE html>")
                .contains("HEALTH SCORE UPDATE")
                .contains("John Smith")
                .contains("72") // Current score
                .contains("68") // Previous score
                .contains("+4") // Score change
                .contains("Preventive care compliance")
                .contains("HealthData-in-Motion");

        assertThat(renderTime).isLessThan(MAX_RENDER_TIME_MS); // Allow time for template engine warmup
    }

    @Test
    @DisplayName("Should render health-score SMS template")
    void shouldRenderHealthScoreSmsTemplate() {
        // Given
        Map<String, Object> variables = createHealthScoreVariables("SMS");

        // When
        String rendered = templateRenderer.render("health-score", variables);

        // Then
        assertThat(rendered)
                .doesNotContain("<!DOCTYPE html>")
                .contains("💚 HEALTH SCORE UPDATE")
                .contains("John Smith")
                .contains("72")
                .contains("Previous: 68");

        assertThat(rendered.length()).isLessThan(500);
    }

    // ==================== Template 3: Appointment Reminder ====================

    @Test
    @DisplayName("Should render appointment-reminder EMAIL template with real Thymeleaf engine")
    void shouldRenderAppointmentReminderEmailTemplate() {
        // Given
        Map<String, Object> variables = createAppointmentReminderVariables("EMAIL");

        // When
        long startTime = System.currentTimeMillis();
        String rendered = templateRenderer.render("appointment-reminder", variables);
        long renderTime = System.currentTimeMillis() - startTime;

        // Then
        assertThat(rendered)
                .contains("<!DOCTYPE html>")
                .contains("APPOINTMENT REMINDER")
                .contains("John Smith")
                .contains("Monday, December 15, 2025")
                .contains("10:30 AM")
                .contains("Dr. Sarah Johnson")
                .contains("Main Clinic")
                .contains("Arrive 15 minutes early")
                .contains("HealthData-in-Motion");

        assertThat(renderTime).isLessThan(MAX_RENDER_TIME_MS); // Allow time for template engine warmup
    }

    @Test
    @DisplayName("Should render appointment-reminder SMS template")
    void shouldRenderAppointmentReminderSmsTemplate() {
        // Given
        Map<String, Object> variables = createAppointmentReminderVariables("SMS");

        // When
        String rendered = templateRenderer.render("appointment-reminder", variables);

        // Then
        assertThat(rendered)
                .doesNotContain("<!DOCTYPE html>")
                .contains("📅 APPOINTMENT REMINDER")
                .contains("John Smith")
                .contains("Monday, December 15, 2025")
                .contains("10:30 AM")
                .contains("Dr. Sarah Johnson");

        assertThat(rendered.length()).isLessThan(500);
    }

    // ==================== Template 4: Medication Reminder ====================

    @Test
    @DisplayName("Should render medication-reminder EMAIL template with real Thymeleaf engine")
    void shouldRenderMedicationReminderEmailTemplate() {
        // Given
        Map<String, Object> variables = createMedicationReminderVariables("EMAIL");

        // When
        long startTime = System.currentTimeMillis();
        String rendered = templateRenderer.render("medication-reminder", variables);
        long renderTime = System.currentTimeMillis() - startTime;

        // Then
        assertThat(rendered)
                .contains("<!DOCTYPE html>")
                .contains("MEDICATION REFILL REMINDER")
                .contains("John Smith")
                .contains("Metformin 500mg")
                .contains("Take 1 tablet twice daily")
                .contains("December 20, 2025")
                .contains("3 days")
                .contains("Take with food")
                .contains("HealthData-in-Motion");

        assertThat(renderTime).isLessThan(MAX_RENDER_TIME_MS); // Allow time for template engine warmup
    }

    @Test
    @DisplayName("Should render medication-reminder SMS template")
    void shouldRenderMedicationReminderSmsTemplate() {
        // Given
        Map<String, Object> variables = createMedicationReminderVariables("SMS");

        // When
        String rendered = templateRenderer.render("medication-reminder", variables);

        // Then
        assertThat(rendered)
                .doesNotContain("<!DOCTYPE html>")
                .contains("💊 MEDICATION REFILL")
                .contains("John Smith")
                .contains("Metformin 500mg")
                .contains("Refill due: December 20, 2025");

        assertThat(rendered.length()).isLessThan(500);
    }

    // ==================== Template 5: Lab Result ====================

    @Test
    @DisplayName("Should render lab-result EMAIL template with real Thymeleaf engine")
    void shouldRenderLabResultEmailTemplate() {
        // Given
        Map<String, Object> variables = createLabResultVariables("EMAIL");

        // When
        long startTime = System.currentTimeMillis();
        String rendered = templateRenderer.render("lab-result", variables);
        long renderTime = System.currentTimeMillis() - startTime;

        // Then
        assertThat(rendered)
                .contains("<!DOCTYPE html>")
                .contains("LAB RESULT NOTIFICATION")
                .contains("John Smith")
                .contains("Hemoglobin A1C")
                .contains("7.2%")
                .contains("<7.0% for diabetic patients")
                .contains("ABNORMAL")
                .contains("IMPROVING")
                .contains("Dr. Sarah Johnson")
                .contains("HealthData-in-Motion");

        assertThat(renderTime).isLessThan(MAX_RENDER_TIME_MS); // Allow time for template engine warmup
    }

    @Test
    @DisplayName("Should render lab-result SMS template")
    void shouldRenderLabResultSmsTemplate() {
        // Given
        Map<String, Object> variables = createLabResultVariables("SMS");

        // When
        String rendered = templateRenderer.render("lab-result", variables);

        // Then
        assertThat(rendered)
                .doesNotContain("<!DOCTYPE html>")
                .contains("🔬 LAB RESULT AVAILABLE")
                .contains("John Smith")
                .contains("Hemoglobin A1C")
                .contains("7.2%")
                .contains("<7.0%");

        assertThat(rendered.length()).isLessThan(500);
    }

    // ==================== Template 6: Digest ====================

    @Test
    @DisplayName("Should render digest EMAIL template with real Thymeleaf engine")
    void shouldRenderDigestEmailTemplate() {
        // Given
        Map<String, Object> variables = createDigestVariables("EMAIL");

        // When
        long startTime = System.currentTimeMillis();
        String rendered = templateRenderer.render("digest", variables);
        long renderTime = System.currentTimeMillis() - startTime;

        // Then
        assertThat(rendered)
                .contains("<!DOCTYPE html>")
                .contains("DAILY HEALTH DIGEST")
                .contains("Today's Activity Summary")
                .contains("2") // Critical alert count
                .contains("3") // Care gap count
                .contains("Critical Alerts")
                .contains("Care Gaps")
                .contains("Upcoming Appointments")
                .contains("Recent Lab Results")
                .contains("HealthData-in-Motion");

        assertThat(renderTime).isLessThan(MAX_RENDER_TIME_MS); // Allow time for template engine warmup
    }

    @Test
    @DisplayName("Should render digest SMS template")
    void shouldRenderDigestSmsTemplate() {
        // Given
        Map<String, Object> variables = createDigestVariables("SMS");

        // When
        String rendered = templateRenderer.render("digest", variables);

        // Then
        assertThat(rendered)
                .doesNotContain("<!DOCTYPE html>")
                .contains("📊 DAILY HEALTH DIGEST")
                .contains("Summary:")
                .contains("Critical Alerts")
                .contains("Care Gaps")
                .contains("Appointments")
                .contains("Lab Results");

        assertThat(rendered.length()).isLessThan(500);
    }

    // ==================== Template Existence Tests ====================

    @Test
    @DisplayName("Should confirm all 6 templates exist")
    void shouldConfirmAllTemplatesExist() {
        assertThat(templateRenderer.templateExists("critical-alert")).isTrue();
        assertThat(templateRenderer.templateExists("health-score")).isTrue();
        assertThat(templateRenderer.templateExists("appointment-reminder")).isTrue();
        assertThat(templateRenderer.templateExists("medication-reminder")).isTrue();
        assertThat(templateRenderer.templateExists("lab-result")).isTrue();
        assertThat(templateRenderer.templateExists("digest")).isTrue();
    }

    @Test
    @DisplayName("Should return false for non-existent template")
    void shouldReturnFalseForNonExistentTemplate() {
        assertThat(templateRenderer.templateExists("non-existent-template")).isFalse();
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should throw exception for non-existent template")
    void shouldThrowExceptionForNonExistentTemplate() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");

        // When/Then
        assertThatThrownBy(() -> templateRenderer.render("non-existent-template", variables))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should handle missing required variables gracefully")
    void shouldHandleMissingVariablesGracefully() {
        // Given - minimal variables (missing patient name, etc.)
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("actionUrl", "https://example.com");

        // When - should still render without crashing
        String rendered = templateRenderer.render("critical-alert", variables);

        // Then - should contain basic structure even with missing data
        assertThat(rendered).contains("<!DOCTYPE html>");
        assertThat(rendered).contains("CRITICAL ALERT");
    }

    // ==================== Channel Selection Tests ====================

    @Test
    @DisplayName("Should select correct template based on channel - EMAIL")
    void shouldSelectEmailTemplateBasedOnChannel() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "John Smith");

        // When
        String rendered = templateRenderer.render("critical-alert", variables);

        // Then - HTML template selected
        assertThat(rendered).startsWith("<!DOCTYPE html>");
    }

    @Test
    @DisplayName("Should select correct template based on channel - SMS")
    void shouldSelectSmsTemplateBasedOnChannel() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "SMS");
        variables.put("patientName", "John Smith");

        // When
        String rendered = templateRenderer.render("critical-alert", variables);

        // Then - Text template selected (no HTML tags)
        assertThat(rendered).doesNotContain("<!DOCTYPE html>");
        assertThat(rendered).doesNotContain("<html>");
    }

    // ==================== XSS Prevention Tests ====================

    @Test
    @DisplayName("Should prevent XSS attacks via auto-escaping")
    void shouldPreventXssAttacks() {
        // Given - malicious input
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", "EMAIL");
        variables.put("patientName", "<script>alert('XSS')</script>");
        variables.put("alertMessage", "<img src=x onerror=alert('XSS')>");
        variables.put("severity", "HIGH");
        variables.put("actionUrl", "https://example.com");

        // When
        String rendered = templateRenderer.render("critical-alert", variables);

        // Then - should be escaped (no executable script)
        assertThat(rendered).doesNotContain("<script>alert('XSS')</script>");
        assertThat(rendered).doesNotContain("<img src=x onerror=alert('XSS')>");
        // Should contain escaped versions
        assertThat(rendered).contains("&lt;script&gt;");
    }

    // ==================== Helper Methods: Sample Data Creation ====================

    private Map<String, Object> createCriticalAlertVariables(String channel) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", channel);
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123456789");
        variables.put("alertType", "Critical Lab Result");
        variables.put("severity", "HIGH");
        variables.put("alertMessage", "Patient's blood glucose level is critically high at 385 mg/dL. Immediate intervention required.");
        variables.put("facilityName", "Memorial Healthcare System");
        variables.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        variables.put("actionUrl", "https://healthdata-in-motion.com/patients/123456789");

        Map<String, String> details = new HashMap<>();
        details.put("Test Name", "Fasting Blood Glucose");
        details.put("Result Value", "385 mg/dL");
        details.put("Normal Range", "70-100 mg/dL");
        variables.put("details", details);

        List<String> actions = List.of(
                "Review patient chart immediately",
                "Contact patient for immediate follow-up",
                "Consider emergency department evaluation"
        );
        variables.put("recommendedActions", actions);

        return variables;
    }

    private Map<String, Object> createHealthScoreVariables(String channel) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", channel);
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123456789");
        variables.put("currentScore", 72);
        variables.put("previousScore", 68);
        variables.put("scoreChange", "+4");
        variables.put("scoreMessage", "Patient's health score has improved by 4 points this quarter.");
        variables.put("interpretation", "Good");
        variables.put("facilityName", "Memorial Healthcare System");
        variables.put("actionUrl", "https://healthdata-in-motion.com/patients/123456789");

        List<String> factors = List.of(
                "Preventive care compliance: 85%",
                "Chronic condition management: 78%",
                "Medication adherence: 90%"
        );
        variables.put("contributingFactors", factors);

        return variables;
    }

    private Map<String, Object> createAppointmentReminderVariables(String channel) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", channel);
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123456789");
        variables.put("appointmentDate", "Monday, December 15, 2025");
        variables.put("appointmentTime", "10:30 AM");
        variables.put("providerName", "Dr. Sarah Johnson, MD");
        variables.put("location", "Main Clinic - Building A, Room 205");
        variables.put("appointmentType", "Annual Wellness Visit");
        variables.put("facilityName", "Memorial Healthcare System");
        variables.put("actionUrl", "https://healthdata-in-motion.com/appointments/789");

        List<String> instructions = List.of(
                "Arrive 15 minutes early for check-in",
                "Bring your insurance card and a valid photo ID"
        );
        variables.put("instructions", instructions);

        return variables;
    }

    private Map<String, Object> createMedicationReminderVariables(String channel) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", channel);
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123456789");
        variables.put("medicationName", "Metformin 500mg");
        variables.put("dosage", "Take 1 tablet twice daily with meals");
        variables.put("refillDate", "December 20, 2025");
        variables.put("refillDaysLeft", 3);
        variables.put("prescriber", "Dr. Sarah Johnson, MD");
        variables.put("facilityName", "Memorial Healthcare System");
        variables.put("actionUrl", "https://healthdata-in-motion.com/medications/refill/456");

        List<String> instructions = List.of(
                "Take with food to reduce stomach upset",
                "Continue taking even if you feel well"
        );
        variables.put("instructions", instructions);

        return variables;
    }

    private Map<String, Object> createLabResultVariables(String channel) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", channel);
        variables.put("patientName", "John Smith");
        variables.put("mrn", "MRN-123456789");
        variables.put("testName", "Hemoglobin A1C");
        variables.put("resultValue", "7.2%");
        variables.put("normalRange", "<7.0% for diabetic patients");
        variables.put("testDate", "November 25, 2025");
        variables.put("orderingProvider", "Dr. Sarah Johnson, MD");
        variables.put("resultStatus", "ABNORMAL");
        variables.put("previousResult", "7.8%");
        variables.put("previousTestDate", "August 15, 2025");
        variables.put("trend", "IMPROVING");
        variables.put("facilityName", "Memorial Healthcare System");
        variables.put("actionUrl", "https://healthdata-in-motion.com/patients/123456789");

        return variables;
    }

    private Map<String, Object> createDigestVariables(String channel) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("channel", channel);
        variables.put("digestDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        variables.put("criticalAlertCount", 2);
        variables.put("careGapCount", 3);
        variables.put("appointmentCount", 2);
        variables.put("labResultCount", 2);
        variables.put("facilityName", "Memorial Healthcare System");
        variables.put("actionUrl", "https://healthdata-in-motion.com/dashboard");

        // Critical alerts
        List<Map<String, String>> criticalAlerts = List.of(
                Map.of(
                        "alertType", "Critical Lab Result",
                        "message", "Blood glucose critically high at 385 mg/dL",
                        "patientName", "John Smith",
                        "mrn", "MRN-001"
                )
        );
        variables.put("criticalAlerts", criticalAlerts);

        // Care gaps
        List<Map<String, String>> careGaps = List.of(
                Map.of(
                        "gapType", "Diabetic Eye Exam",
                        "message", "Patient is overdue for annual diabetic eye exam",
                        "dueDate", "2025-12-01",
                        "priority", "HIGH",
                        "patientName", "Jane Doe",
                        "mrn", "MRN-002"
                )
        );
        variables.put("careGaps", careGaps);

        return variables;
    }
}
