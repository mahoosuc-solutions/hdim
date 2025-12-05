package com.healthdata.quality.dto.notification;

import com.healthdata.quality.dto.ClinicalAlertDTO;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clinical Alert Notification Request
 *
 * Wraps ClinicalAlertDTO to provide NotificationRequest interface.
 * Used for mental health crisis, risk escalation, health decline, and
 * chronic deterioration alerts.
 *
 * Routing logic:
 * - CRITICAL severity: WebSocket + Email + SMS
 * - HIGH severity: WebSocket + Email
 * - MEDIUM/LOW severity: WebSocket only
 */
@RequiredArgsConstructor
public class ClinicalAlertNotificationRequest implements NotificationRequest {

    private final ClinicalAlertDTO alert;
    private final Map<String, String> recipients;

    @Override
    public String getNotificationType() {
        return "CRITICAL_ALERT";
    }

    @Override
    public String getTemplateId() {
        return "critical-alert";
    }

    @Override
    public String getTenantId() {
        return alert.getTenantId();
    }

    @Override
    public String getPatientId() {
        return alert.getPatientId();
    }

    @Override
    public String getTitle() {
        return alert.getTitle();
    }

    @Override
    public String getMessage() {
        return alert.getMessage();
    }

    @Override
    public String getSeverity() {
        return alert.getSeverity();
    }

    @Override
    public Instant getTimestamp() {
        return alert.getTriggeredAt() != null ? alert.getTriggeredAt() : Instant.now();
    }

    @Override
    public Map<String, String> getRecipients() {
        return recipients;
    }

    @Override
    public Map<String, Object> getTemplateVariables() {
        Map<String, Object> variables = new HashMap<>();

        // Alert core fields
        variables.put("alertType", formatAlertType(alert.getAlertType()));
        variables.put("severity", alert.getSeverity());
        variables.put("alertMessage", alert.getMessage());

        // Patient information
        // TODO: Fetch actual patient name from FHIR service
        variables.put("patientName", "Patient " + alert.getPatientId());
        variables.put("mrn", alert.getPatientId());
        variables.put("patientId", alert.getPatientId());

        // Timestamp (convert Instant to LocalDateTime for formatting)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime triggeredTime = alert.getTriggeredAt() != null
                ? LocalDateTime.ofInstant(alert.getTriggeredAt(), ZoneId.systemDefault())
                : LocalDateTime.now();
        variables.put("timestamp", triggeredTime.format(formatter));

        // Facility name
        variables.put("facilityName", "HealthData Clinical System");

        // Action URL - link to patient detail page
        variables.put("actionUrl", "https://healthdata-in-motion.com/patients/" + alert.getPatientId());

        // Additional details (for email template)
        Map<String, String> details = new HashMap<>();
        details.put("Alert ID", alert.getId() != null ? alert.getId() : "N/A");
        details.put("Alert Type", formatAlertType(alert.getAlertType()));
        details.put("Severity", alert.getSeverity());
        details.put("Escalated", alert.isEscalated() ? "Yes - IMMEDIATE ATTENTION REQUIRED" : "No");
        details.put("Triggered At", triggeredTime.format(formatter));
        variables.put("details", details);

        // Recommended actions (for email template)
        List<String> actions = List.of(getActionGuidance(alert).split("\n"));
        variables.put("recommendedActions", actions);

        return variables;
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("alertType", alert.getAlertType());
        metadata.put("sourceEventType", alert.getSourceEventType());
        metadata.put("sourceEventId", alert.getSourceEventId());
        metadata.put("escalated", alert.isEscalated());
        metadata.put("status", alert.getStatus());
        return metadata;
    }

    @Override
    public boolean shouldSendEmail() {
        // CRITICAL and HIGH severity alerts should be sent via email
        return "CRITICAL".equals(alert.getSeverity()) || "HIGH".equals(alert.getSeverity());
    }

    @Override
    public boolean shouldSendSms() {
        // Only CRITICAL severity alerts should be sent via SMS
        return "CRITICAL".equals(alert.getSeverity());
    }

    @Override
    public boolean shouldSendWebSocket() {
        // All alerts should be sent via WebSocket for real-time updates
        return true;
    }

    @Override
    public String getNotificationId() {
        return alert.getId();
    }

    @Override
    public String getRelatedEntityId() {
        return alert.getId();
    }

    /**
     * Format alert type for display
     */
    private String formatAlertType(String alertType) {
        return switch (alertType) {
            case "MENTAL_HEALTH_CRISIS" -> "Mental Health Crisis";
            case "RISK_ESCALATION" -> "Risk Escalation";
            case "HEALTH_DECLINE" -> "Health Score Decline";
            case "CHRONIC_DETERIORATION" -> "Chronic Disease Deterioration";
            default -> alertType;
        };
    }

    /**
     * Get action guidance based on alert type and severity
     */
    private String getActionGuidance(ClinicalAlertDTO alert) {
        if (alert.getSeverity().equals("CRITICAL")) {
            if (alert.getTitle().contains("Suicide")) {
                return """
                    1. Contact patient IMMEDIATELY to assess safety
                    2. Perform crisis intervention protocol
                    3. Consider emergency services if unable to reach patient
                    4. Document all interventions in patient record
                    """;
            }
            return """
                1. Review patient status and assessment details
                2. Contact patient within 24 hours
                3. Schedule urgent follow-up appointment
                4. Update care plan as needed
                """;
        }

        if (alert.getSeverity().equals("HIGH")) {
            return """
                1. Review alert details and patient history
                2. Contact patient within 2-3 business days
                3. Address underlying issues identified
                4. Update care coordination plan
                """;
        }

        return """
            1. Review alert during next scheduled patient contact
            2. Monitor for additional changes
            3. Update care plan if needed
            """;
    }

    /**
     * Get the underlying ClinicalAlertDTO
     * @return The alert DTO
     */
    public ClinicalAlertDTO getAlert() {
        return alert;
    }
}
