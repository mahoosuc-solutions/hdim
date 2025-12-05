package com.healthdata.quality.service.notification;

import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.dto.notification.GenericNotificationRequest;
import com.healthdata.quality.model.NotificationRecipient;
import com.healthdata.quality.persistence.NotificationEntity;
import com.healthdata.quality.service.NotificationService;
import com.healthdata.quality.service.PatientNameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Clinical Alert Notification Trigger
 *
 * Automatically sends notifications when clinical alerts are triggered.
 * Integrates with ClinicalAlertService to deliver real-time critical alerts.
 *
 * Notification Routing by Severity:
 * - CRITICAL: WebSocket + Email + SMS (immediate attention required)
 * - HIGH: WebSocket + Email (urgent attention)
 * - MEDIUM: WebSocket + Email (attention needed)
 * - LOW: WebSocket only (informational)
 *
 * Usage:
 * <pre>
 * clinicalAlertNotificationTrigger.onAlertTriggered(tenantId, alert);
 * clinicalAlertNotificationTrigger.onAlertAcknowledged(tenantId, alert);
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClinicalAlertNotificationTrigger {

    private final NotificationService notificationService;
    private final RecipientResolutionService recipientResolutionService;
    private final PatientNameService patientNameService;

    /**
     * Trigger notification when clinical alert is created
     * Called by ClinicalAlertService after creating an alert
     *
     * @param tenantId Tenant ID
     * @param alert Newly created clinical alert
     */
    public void onAlertTriggered(String tenantId, ClinicalAlertDTO alert) {
        try {
            // Get recipients for this patient/tenant
            Map<String, String> recipients = getRecipients(tenantId, alert.getPatientId());

            // Determine notification channels based on severity
            boolean sendEmail = shouldSendEmail(alert);
            boolean sendSms = shouldSendSms(alert);

            // Build notification request
            GenericNotificationRequest request = GenericNotificationRequest.builder()
                    .notificationType("CLINICAL_ALERT_TRIGGERED")
                    .templateId("critical-alert")
                    .tenantId(tenantId)
                    .patientId(alert.getPatientId())
                    .title(buildAlertTitle(alert))
                    .message(alert.getMessage())
                    .severity(mapAlertSeverityToNotificationSeverity(alert.getSeverity()))
                    .sendWebSocket(true)  // Always send via WebSocket for real-time updates
                    .sendEmail(sendEmail)
                    .sendSms(sendSms)
                    .recipients(recipients)
                    .templateVariables(buildAlertTemplateVariables(alert))
                    .metadata(buildAlertMetadata(alert))
                    .build();

            // Send notification via NotificationService
            NotificationService.NotificationStatus status = notificationService.sendNotification(request);

            if (status.isAllSuccessful()) {
                log.info("Clinical alert notification sent successfully for alert {} (severity: {})",
                        alert.getId(), alert.getSeverity());
            } else {
                log.warn("Clinical alert notification partially failed for alert {}: {}",
                        alert.getId(), status.getChannelStatus());
            }

        } catch (Exception e) {
            log.error("Failed to send clinical alert notification for alert {}: {}",
                    alert.getId(), e.getMessage(), e);
        }
    }

    /**
     * Trigger notification when alert is acknowledged
     * Called by ClinicalAlertService after acknowledging an alert
     *
     * @param tenantId Tenant ID
     * @param alert Acknowledged clinical alert
     */
    public void onAlertAcknowledged(String tenantId, ClinicalAlertDTO alert) {
        try {
            // Only send acknowledgment notifications for CRITICAL and HIGH severity alerts
            if (!shouldNotifyOnAcknowledgment(alert)) {
                log.debug("Skipping acknowledgment notification for {} severity alert: {}",
                        alert.getSeverity(), alert.getId());
                return;
            }

            Map<String, String> recipients = getRecipients(tenantId, alert.getPatientId());

            // Build notification request
            GenericNotificationRequest request = GenericNotificationRequest.builder()
                    .notificationType("CLINICAL_ALERT_ACKNOWLEDGED")
                    .templateId("critical-alert")
                    .tenantId(tenantId)
                    .patientId(alert.getPatientId())
                    .title(buildAcknowledgedTitle(alert))
                    .message(buildAcknowledgedMessage(alert))
                    .severity("LOW")  // Acknowledgments are informational
                    .sendWebSocket(true)
                    .sendEmail(true)  // Send email for documentation
                    .sendSms(false)   // SMS not needed for acknowledgments
                    .recipients(recipients)
                    .templateVariables(buildAcknowledgedTemplateVariables(alert))
                    .metadata(buildAlertMetadata(alert))
                    .build();

            // Send notification via NotificationService
            NotificationService.NotificationStatus status = notificationService.sendNotification(request);

            if (status.isAllSuccessful()) {
                log.info("Alert acknowledgment notification sent successfully for alert {}", alert.getId());
            } else {
                log.warn("Alert acknowledgment notification partially failed for alert {}: {}",
                        alert.getId(), status.getChannelStatus());
            }

        } catch (Exception e) {
            log.error("Failed to send alert acknowledgment notification for alert {}: {}",
                    alert.getId(), e.getMessage(), e);
        }
    }

    /**
     * Determine if email should be sent based on severity
     */
    private boolean shouldSendEmail(ClinicalAlertDTO alert) {
        // Send email for CRITICAL, HIGH, and MEDIUM severity
        return "CRITICAL".equals(alert.getSeverity()) ||
               "HIGH".equals(alert.getSeverity()) ||
               "MEDIUM".equals(alert.getSeverity());
    }

    /**
     * Determine if SMS should be sent based on severity
     */
    private boolean shouldSendSms(ClinicalAlertDTO alert) {
        // Only send SMS for CRITICAL severity (suicide risk, severe depression)
        return "CRITICAL".equals(alert.getSeverity());
    }

    /**
     * Determine if we should notify on acknowledgment
     */
    private boolean shouldNotifyOnAcknowledgment(ClinicalAlertDTO alert) {
        // Only notify for CRITICAL and HIGH severity acknowledgments
        return "CRITICAL".equals(alert.getSeverity()) || "HIGH".equals(alert.getSeverity());
    }

    /**
     * Build notification title for triggered alert
     */
    private String buildAlertTitle(ClinicalAlertDTO alert) {
        String prefix = getAlertPrefix(alert.getSeverity());
        return String.format("%s %s", prefix, alert.getTitle());
    }

    /**
     * Build notification title for acknowledged alert
     */
    private String buildAcknowledgedTitle(ClinicalAlertDTO alert) {
        return String.format("Alert Acknowledged: %s", alert.getTitle());
    }

    /**
     * Build notification message for acknowledged alert
     */
    private String buildAcknowledgedMessage(ClinicalAlertDTO alert) {
        return String.format("Clinical alert for patient %s has been acknowledged by %s. " +
                        "Alert: %s. Status: %s.",
                alert.getPatientId(), alert.getAcknowledgedBy(),
                alert.getTitle(), alert.getStatus());
    }

    /**
     * Get alert prefix based on severity
     */
    private String getAlertPrefix(String severity) {
        return switch (severity) {
            case "CRITICAL" -> "🚨 CRITICAL ALERT:";
            case "HIGH" -> "⚠️ HIGH PRIORITY:";
            case "MEDIUM" -> "⚡ ALERT:";
            case "LOW" -> "ℹ️ NOTICE:";
            default -> "ALERT:";
        };
    }

    /**
     * Build template variables for alert notification
     */
    private Map<String, Object> buildAlertTemplateVariables(ClinicalAlertDTO alert) {
        Map<String, Object> variables = new HashMap<>();

        // Patient information
        variables.put("patientId", alert.getPatientId());
        variables.put("patientName", patientNameService.getPatientName(alert.getPatientId()));

        // Alert details
        variables.put("alertId", alert.getId());
        variables.put("alertType", formatAlertType(alert.getAlertType()));
        variables.put("alertTitle", alert.getTitle());
        variables.put("alertMessage", alert.getMessage());
        variables.put("severity", alert.getSeverity());
        variables.put("severityLabel", formatSeverity(alert.getSeverity()));

        // Escalation information
        variables.put("escalated", alert.isEscalated());
        if (alert.isEscalated() && alert.getEscalatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime escalatedAt = LocalDateTime.ofInstant(alert.getEscalatedAt(), ZoneId.systemDefault());
            variables.put("escalatedAt", escalatedAt.format(formatter));
        }

        // Triggered date
        if (alert.getTriggeredAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime triggeredAt = LocalDateTime.ofInstant(alert.getTriggeredAt(), ZoneId.systemDefault());
            variables.put("triggeredAt", triggeredAt.format(formatter));
        }

        // Source information
        variables.put("sourceEventType", alert.getSourceEventType());
        variables.put("sourceEventId", alert.getSourceEventId());

        // Action URL
        variables.put("actionUrl", "https://healthdata-in-motion.com/patients/" +
                alert.getPatientId() + "/alerts/" + alert.getId());

        // Facility name
        variables.put("facilityName", "HealthData Clinical System");

        // Status
        variables.put("status", alert.getStatus());

        // Event type
        variables.put("eventType", "TRIGGERED");

        // Urgency indicators
        variables.put("requiresImmediateAction", "CRITICAL".equals(alert.getSeverity()));
        variables.put("isMentalHealthCrisis", "MENTAL_HEALTH_CRISIS".equals(alert.getAlertType()));

        return variables;
    }

    /**
     * Build template variables for acknowledgment notification
     */
    private Map<String, Object> buildAcknowledgedTemplateVariables(ClinicalAlertDTO alert) {
        Map<String, Object> variables = buildAlertTemplateVariables(alert);

        // Override event type
        variables.put("eventType", "ACKNOWLEDGED");

        // Add acknowledgment-specific fields
        if (alert.getAcknowledgedBy() != null) {
            variables.put("acknowledgedBy", alert.getAcknowledgedBy());
        }

        if (alert.getAcknowledgedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime acknowledgedAt = LocalDateTime.ofInstant(alert.getAcknowledgedAt(), ZoneId.systemDefault());
            variables.put("acknowledgedAt", acknowledgedAt.format(formatter));
        }

        return variables;
    }

    /**
     * Build metadata for notification tracking
     */
    private Map<String, Object> buildAlertMetadata(ClinicalAlertDTO alert) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("alertId", alert.getId());
        metadata.put("alertType", alert.getAlertType());
        metadata.put("severity", alert.getSeverity());
        metadata.put("sourceEventType", alert.getSourceEventType());
        metadata.put("sourceEventId", alert.getSourceEventId());
        metadata.put("escalated", alert.isEscalated());
        return metadata;
    }

    /**
     * Map alert severity to notification severity
     */
    private String mapAlertSeverityToNotificationSeverity(String alertSeverity) {
        return switch (alertSeverity) {
            case "CRITICAL" -> "HIGH";  // Map to HIGH for notification system
            case "HIGH" -> "HIGH";
            case "MEDIUM" -> "MEDIUM";
            case "LOW" -> "LOW";
            default -> "MEDIUM";
        };
    }

    /**
     * Format alert type for display
     */
    private String formatAlertType(String alertType) {
        if (alertType == null) return "Unknown";
        return alertType.replace("_", " ").toLowerCase();
    }

    /**
     * Format severity for display
     */
    private String formatSeverity(String severity) {
        if (severity == null) return "Unknown";
        return severity.substring(0, 1).toUpperCase() + severity.substring(1).toLowerCase();
    }

    /**
     * Get recipients for clinical alert notifications
     * Resolves recipients from patient's care team and user preferences
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Map of channel -> recipient ID
     */
    private Map<String, String> getRecipients(String tenantId, String patientId) {
        Map<String, String> recipients = new HashMap<>();

        // Determine notification severity for recipient resolution
        NotificationEntity.NotificationSeverity severity = NotificationEntity.NotificationSeverity.HIGH;

        // Resolve recipients for EMAIL channel
        List<NotificationRecipient> emailRecipients = recipientResolutionService.resolveRecipients(
            tenantId, patientId, NotificationEntity.NotificationChannel.EMAIL, severity
        );
        if (!emailRecipients.isEmpty()) {
            // Use primary care provider's email, or first recipient if no primary
            String email = emailRecipients.stream()
                .filter(NotificationRecipient::isPrimary)
                .findFirst()
                .orElse(emailRecipients.get(0))
                .getEmailAddress();
            recipients.put("EMAIL", email);
        }

        // Resolve recipients for SMS channel
        List<NotificationRecipient> smsRecipients = recipientResolutionService.resolveRecipients(
            tenantId, patientId, NotificationEntity.NotificationChannel.SMS, severity
        );
        if (!smsRecipients.isEmpty()) {
            // Use primary care provider's phone, or first recipient if no primary
            String phone = smsRecipients.stream()
                .filter(NotificationRecipient::isPrimary)
                .findFirst()
                .orElse(smsRecipients.get(0))
                .getPhoneNumber();
            recipients.put("SMS", phone);
        }

        log.debug("Resolved {} recipients for patient {} clinical alert", recipients.size(), patientId);

        return recipients;
    }
}
