package com.healthdata.quality.controller;

import com.healthdata.quality.service.notification.TemplateRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Template Preview API Controller
 *
 * Provides endpoints to preview notification templates with sample data.
 * Useful for testing and verifying template rendering without sending actual notifications.
 *
 * Endpoints (with context-path /quality-measure):
 * - GET /quality-measure/templates/preview/{templateId} - Preview template with default sample data
 * - POST /quality-measure/templates/preview/{templateId} - Preview template with custom data
 * - GET /quality-measure/templates/list - List all available templates
 */
@Slf4j
@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplatePreviewController {

    private final TemplateRenderer templateRenderer;

    /**
     * Preview template with default sample data
     *
     * @param templateId Template identifier (e.g., "critical-alert")
     * @param channel    Channel type: EMAIL or SMS (default: EMAIL)
     * @return Rendered template HTML/text
     */
    @GetMapping(value = "/preview/{templateId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewTemplateWithDefaults(
            @PathVariable String templateId,
            @RequestParam(defaultValue = "EMAIL") String channel) {

        log.info("Previewing template '{}' for channel '{}'", templateId, channel);

        try {
            Map<String, Object> sampleData = createSampleData(templateId, channel);
            String rendered = templateRenderer.render(templateId, sampleData);

            return ResponseEntity.ok(rendered);
        } catch (Exception e) {
            log.error("Failed to preview template '{}': {}", templateId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<html><body><h1>Error</h1><p>" + e.getMessage() + "</p></body></html>");
        }
    }

    /**
     * Preview template with custom data
     *
     * @param templateId Template identifier
     * @param variables  Map of variables to substitute in the template
     * @return Rendered template HTML/text
     */
    @PostMapping(value = "/preview/{templateId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewTemplateWithCustomData(
            @PathVariable String templateId,
            @RequestBody Map<String, Object> variables) {

        log.info("Previewing template '{}' with {} custom variables", templateId, variables.size());

        try {
            // Ensure channel is specified
            if (!variables.containsKey("channel")) {
                variables.put("channel", "EMAIL");
            }

            String rendered = templateRenderer.render(templateId, variables);
            return ResponseEntity.ok(rendered);
        } catch (Exception e) {
            log.error("Failed to preview template '{}': {}", templateId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<html><body><h1>Error</h1><p>" + e.getMessage() + "</p></body></html>");
        }
    }

    /**
     * Check if a template exists
     *
     * @param templateId Template identifier
     * @return Status indicating if template exists
     */
    @GetMapping("/exists/{templateId}")
    public ResponseEntity<Map<String, Object>> checkTemplateExists(@PathVariable String templateId) {
        boolean exists = templateRenderer.templateExists(templateId);

        Map<String, Object> response = new HashMap<>();
        response.put("templateId", templateId);
        response.put("exists", exists);

        return ResponseEntity.ok(response);
    }

    /**
     * List all available templates
     *
     * @return List of available template IDs
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listTemplates() {
        // Hardcoded list for now - could be enhanced to scan template directory
        List<String> templates = List.of(
                "critical-alert",
                "care-gap",
                "health-score",
                "appointment-reminder",
                "medication-reminder",
                "lab-result",
                "digest"
        );

        Map<String, Object> response = new HashMap<>();
        response.put("templates", templates);
        response.put("count", templates.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get template sample data structure
     *
     * @param templateId Template identifier
     * @return Sample data structure for the template
     */
    @GetMapping("/sample-data/{templateId}")
    public ResponseEntity<Map<String, Object>> getSampleDataStructure(@PathVariable String templateId) {
        Map<String, Object> sampleData = createSampleData(templateId, "EMAIL");
        return ResponseEntity.ok(sampleData);
    }

    /**
     * Create sample data for template preview
     */
    private Map<String, Object> createSampleData(String templateId, String channel) {
        Map<String, Object> data = new HashMap<>();
        data.put("channel", channel);

        // Common fields for all templates
        data.put("patientName", "John Smith");
        data.put("mrn", "MRN-123456789");
        data.put("dob", "01/15/1965");
        data.put("age", "58 years");
        data.put("facilityName", "Memorial Healthcare System");
        data.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.put("actionUrl", "https://healthdata-in-motion.com/patients/123456789");

        // Template-specific fields
        switch (templateId) {
            case "critical-alert":
                data.put("alertType", "Critical Lab Result");
                data.put("severity", "HIGH");
                data.put("alertMessage",
                        "Patient's blood glucose level is critically high at 385 mg/dL. " +
                                "Immediate intervention required to prevent diabetic complications.");

                // Additional details table
                Map<String, String> details = new HashMap<>();
                details.put("Test Name", "Fasting Blood Glucose");
                details.put("Result Value", "385 mg/dL");
                details.put("Normal Range", "70-100 mg/dL");
                details.put("Previous Result", "180 mg/dL (2 weeks ago)");
                details.put("Ordering Provider", "Dr. Sarah Johnson");
                data.put("details", details);

                // Recommended actions
                List<String> actions = List.of(
                        "Review patient chart immediately",
                        "Contact patient for immediate follow-up",
                        "Consider emergency department evaluation",
                        "Review current medication regimen",
                        "Schedule urgent endocrinology consultation"
                );
                data.put("recommendedActions", actions);
                break;

            case "care-gap":
                data.put("gapType", "Preventive Care Gap");
                data.put("gapMessage", "Patient is due for annual diabetic eye exam (last exam: 18 months ago)");
                data.put("measure", "CDC-H: Comprehensive Diabetes Care - Eye Exam");
                data.put("dueDate", "2025-12-01");
                data.put("priority", "MEDIUM");

                // Recommended actions for closing the gap
                List<String> gapActions = List.of(
                        "Schedule ophthalmology appointment within 30 days",
                        "Ensure patient has active referral to eye care provider",
                        "Review diabetic retinopathy screening protocol",
                        "Update care plan to include annual eye exam reminder"
                );
                data.put("recommendedActions", gapActions);
                break;

            case "health-score":
                data.put("currentScore", 72);
                data.put("previousScore", 68);
                data.put("scoreChange", "+4");
                data.put("scoreMessage", "Patient's health score has improved by 4 points this quarter. Great progress on preventive care compliance!");
                data.put("interpretation", "Good");

                // Contributing factors to the health score
                List<String> contributingFactors = List.of(
                        "Preventive care compliance: 85%",
                        "Chronic condition management: 78%",
                        "Medication adherence: 90%",
                        "Lab work completion: 70%",
                        "Care gap closure rate: 65%"
                );
                data.put("contributingFactors", contributingFactors);

                // Recommendations to improve score
                List<String> scoreRecommendations = List.of(
                        "Complete pending diabetic eye exam to close care gap",
                        "Schedule annual wellness visit (due in 30 days)",
                        "Review and update chronic disease care plan",
                        "Ensure all preventive screenings are up to date"
                );
                data.put("recommendations", scoreRecommendations);
                break;

            case "appointment-reminder":
                data.put("appointmentDate", "Monday, December 15, 2025");
                data.put("appointmentTime", "10:30 AM");
                data.put("providerName", "Dr. Sarah Johnson, MD");
                data.put("location", "Main Clinic - Building A, Room 205");
                data.put("address", "123 Medical Center Drive, Suite 100, Cityville, ST 12345");
                data.put("appointmentType", "Annual Wellness Visit");
                data.put("phoneNumber", "(555) 123-4567");

                // Map and calendar URLs
                data.put("mapUrl", "https://maps.google.com/?q=123+Medical+Center+Drive");
                data.put("confirmUrl", "https://healthdata-in-motion.com/appointments/789/confirm");
                data.put("cancelUrl", "https://healthdata-in-motion.com/appointments/789/cancel");
                data.put("calendarUrl", "https://healthdata-in-motion.com/appointments/789/calendar");

                // Preparation instructions
                List<String> appointmentInstructions = List.of(
                        "Arrive 15 minutes early for check-in and registration",
                        "Bring your insurance card and a valid photo ID",
                        "Bring a list of all current medications and supplements",
                        "Complete pre-appointment forms online (link sent via email)",
                        "Fast for 8 hours before appointment (water is OK)"
                );
                data.put("instructions", appointmentInstructions);
                break;

            case "medication-reminder":
                data.put("medicationName", "Metformin 500mg");
                data.put("dosage", "Take 1 tablet twice daily with meals");
                data.put("refillDate", "December 20, 2025");
                data.put("refillDaysLeft", 3);  // Low number to show urgency styling
                data.put("prescriber", "Dr. Sarah Johnson, MD");

                // Pharmacy information
                data.put("pharmacyName", "Main Street Pharmacy");
                data.put("pharmacyPhone", "(555) 987-6543");

                // Dosing schedule
                data.put("schedule", "Morning (8 AM) and Evening (8 PM) with meals");

                // Special instructions
                List<String> medicationInstructions = List.of(
                        "Take with food to reduce stomach upset",
                        "Avoid alcohol while taking this medication",
                        "Do not crush or chew tablets - swallow whole",
                        "Continue taking even if you feel well"
                );
                data.put("instructions", medicationInstructions);

                // Warnings and side effects
                List<String> medicationWarnings = List.of(
                        "May cause dizziness - use caution when driving or operating machinery",
                        "Contact doctor if you experience muscle pain or unusual fatigue",
                        "Monitor blood sugar levels regularly as directed",
                        "Seek immediate medical attention if you have signs of lactic acidosis (weakness, muscle pain, difficulty breathing)"
                );
                data.put("warnings", medicationWarnings);

                // Refill action URL
                data.put("refillUrl", "https://healthdata-in-motion.com/medications/refill/456");
                break;

            case "lab-result":
                data.put("testName", "Hemoglobin A1C");
                data.put("resultValue", "7.2%");
                data.put("normalRange", "<7.0% for diabetic patients");
                data.put("testDate", "November 25, 2025");
                data.put("orderingProvider", "Dr. Sarah Johnson, MD");

                // Result status (NORMAL, ABNORMAL, CRITICAL)
                data.put("resultStatus", "ABNORMAL");  // Slightly elevated

                // Previous result for comparison
                data.put("previousResult", "7.8%");
                data.put("previousTestDate", "August 15, 2025");

                // Trend indicator (IMPROVING, WORSENING, STABLE)
                data.put("trend", "IMPROVING");  // Down from 7.8% to 7.2%

                // Clinical interpretation
                data.put("interpretation",
                        "Your A1C has improved from 7.8% to 7.2%, showing positive progress in diabetes management. " +
                                "Continue current treatment plan and lifestyle modifications.");

                // Recommended next steps
                List<String> labNextSteps = List.of(
                        "Schedule follow-up appointment within 3 months",
                        "Continue current medication regimen",
                        "Maintain healthy diet and regular exercise",
                        "Monitor blood glucose levels at home as directed"
                );
                data.put("nextSteps", labNextSteps);

                // Lab contact information
                data.put("labName", "Memorial Lab Services");
                data.put("labPhone", "(555) 234-5678");
                break;

            case "digest":
                data.put("digestDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));

                // Summary counts
                data.put("criticalAlertCount", 2);
                data.put("careGapCount", 3);
                data.put("appointmentCount", 2);
                data.put("labResultCount", 2);

                // Critical Alerts List
                List<Map<String, String>> criticalAlerts = List.of(
                        Map.of(
                                "alertType", "Critical Lab Result",
                                "message", "Patient's blood glucose level is critically high at 385 mg/dL. Immediate intervention required.",
                                "patientName", "John Smith",
                                "mrn", "MRN-001"
                        ),
                        Map.of(
                                "alertType", "Medication Adherence Alert",
                                "message", "Patient has missed 4 consecutive doses of critical heart medication (Metoprolol). Contact immediately.",
                                "patientName", "Mary Johnson",
                                "mrn", "MRN-002"
                        )
                );
                data.put("criticalAlerts", criticalAlerts);

                // Care Gaps List
                List<Map<String, String>> careGaps = List.of(
                        Map.of(
                                "gapType", "Diabetic Eye Exam",
                                "message", "Patient is overdue for annual diabetic eye exam (last exam: 18 months ago)",
                                "dueDate", "2025-12-01",
                                "priority", "HIGH",
                                "patientName", "Robert Davis",
                                "mrn", "MRN-003"
                        ),
                        Map.of(
                                "gapType", "Colorectal Cancer Screening",
                                "message", "Patient is due for colorectal cancer screening (age 52, no prior screening)",
                                "dueDate", "2025-12-15",
                                "priority", "MEDIUM",
                                "patientName", "Sarah Wilson",
                                "mrn", "MRN-004"
                        ),
                        Map.of(
                                "gapType", "Annual Wellness Visit",
                                "message", "Patient has not completed annual wellness visit in 14 months",
                                "dueDate", "2025-11-30",
                                "priority", "LOW",
                                "patientName", "James Brown",
                                "mrn", "MRN-005"
                        )
                );
                data.put("careGaps", careGaps);

                // Upcoming Appointments List
                List<Map<String, String>> appointments = List.of(
                        Map.of(
                                "appointmentDate", "Monday, December 2, 2025",
                                "appointmentTime", "10:30 AM",
                                "providerName", "Dr. Sarah Johnson, MD",
                                "location", "Main Clinic - Building A, Room 205",
                                "patientName", "Emily Taylor",
                                "mrn", "MRN-006"
                        ),
                        Map.of(
                                "appointmentDate", "Tuesday, December 3, 2025",
                                "appointmentTime", "2:00 PM",
                                "providerName", "Dr. Michael Chen, MD",
                                "location", "Specialty Clinic - Building B, Room 301",
                                "patientName", "David Anderson",
                                "mrn", "MRN-007"
                        )
                );
                data.put("appointments", appointments);

                // Recent Lab Results List
                List<Map<String, String>> labResults = List.of(
                        Map.of(
                                "testName", "Hemoglobin A1C",
                                "resultValue", "7.2%",
                                "normalRange", "<7.0% for diabetic patients",
                                "resultStatus", "ABNORMAL",
                                "patientName", "Jennifer Martinez",
                                "mrn", "MRN-008"
                        ),
                        Map.of(
                                "testName", "Total Cholesterol",
                                "resultValue", "185 mg/dL",
                                "normalRange", "<200 mg/dL",
                                "resultStatus", "NORMAL",
                                "patientName", "William Garcia",
                                "mrn", "MRN-009"
                        )
                );
                data.put("labResults", labResults);
                break;
        }

        return data;
    }
}
