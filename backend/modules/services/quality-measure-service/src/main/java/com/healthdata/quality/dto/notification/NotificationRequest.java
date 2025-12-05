package com.healthdata.quality.dto.notification;

import java.time.Instant;
import java.util.Map;

/**
 * Notification Request Abstraction
 *
 * Common interface for all notification types in the HealthData system.
 * Supports 6 notification types:
 * - CRITICAL_ALERT: Mental health crisis, risk escalation, health decline
 * - HEALTH_SCORE_UPDATE: Real-time health score changes via WebSocket
 * - APPOINTMENT_REMINDER: Upcoming appointment notifications
 * - MEDICATION_REMINDER: Medication adherence reminders
 * - LAB_RESULT: New lab results available
 * - DAILY_DIGEST: Summary of care gaps, alerts, and tasks
 *
 * Each notification type implements this interface to provide consistent
 * data for template rendering and notification history tracking.
 */
public interface NotificationRequest {

    /**
     * Get the notification type
     * @return Type: CRITICAL_ALERT, HEALTH_SCORE_UPDATE, APPOINTMENT_REMINDER,
     *         MEDICATION_REMINDER, LAB_RESULT, DAILY_DIGEST
     */
    String getNotificationType();

    /**
     * Get the template ID to use for rendering
     * @return Template ID (e.g., "critical-alert", "health-score", "appointment-reminder")
     */
    String getTemplateId();

    /**
     * Get the tenant ID for multi-tenancy isolation
     * @return Tenant ID
     */
    String getTenantId();

    /**
     * Get the patient ID (optional - null for non-patient-specific notifications)
     * @return Patient FHIR ID or null
     */
    String getPatientId();

    /**
     * Get the notification title
     * @return Title for email subject or notification header
     */
    String getTitle();

    /**
     * Get the notification message
     * @return Main message content
     */
    String getMessage();

    /**
     * Get the notification severity (optional - null if not applicable)
     * @return Severity: CRITICAL, HIGH, MEDIUM, LOW, or null
     */
    String getSeverity();

    /**
     * Get the timestamp when the notification should be sent
     * @return Timestamp (typically now or scheduled time)
     */
    Instant getTimestamp();

    /**
     * Get recipient identifiers (email addresses, phone numbers, or user IDs)
     * Multiple recipients supported for different channels
     * @return Map of channel -> recipient ID (e.g., "EMAIL" -> "doctor@hospital.com")
     */
    Map<String, String> getRecipients();

    /**
     * Get template variables for rendering
     * Contains all data needed to populate the notification template
     * @return Map of variable name -> value
     */
    Map<String, Object> getTemplateVariables();

    /**
     * Get additional metadata for notification history tracking
     * @return Map of metadata key -> value
     */
    Map<String, Object> getMetadata();

    /**
     * Whether this notification should be sent via email
     * @return true if email channel should be used
     */
    boolean shouldSendEmail();

    /**
     * Whether this notification should be sent via SMS
     * @return true if SMS channel should be used
     */
    boolean shouldSendSms();

    /**
     * Whether this notification should be sent via WebSocket
     * @return true if WebSocket channel should be used
     */
    boolean shouldSendWebSocket();

    /**
     * Get unique identifier for this notification (for deduplication)
     * @return Unique ID or null if not applicable
     */
    default String getNotificationId() {
        return null;
    }

    /**
     * Get related entity ID (alert ID, health score ID, appointment ID, etc.)
     * @return Related entity ID or null
     */
    default String getRelatedEntityId() {
        return null;
    }
}
