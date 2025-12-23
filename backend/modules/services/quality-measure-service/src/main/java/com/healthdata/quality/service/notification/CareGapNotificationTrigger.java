package com.healthdata.quality.service.notification;

import com.healthdata.quality.dto.CareGapDTO;
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
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Care Gap Notification Trigger
 *
 * Automatically sends notifications when care gaps are identified or addressed.
 * Integrates with CareGapService to trigger real-time notifications.
 *
 * Notification Rules:
 * - WebSocket: All care gap events (real-time dashboard updates)
 * - Email: High priority gaps, gaps approaching due date, gaps addressed
 * - SMS: Critical priority gaps only
 *
 * Usage:
 * <pre>
 * careGapNotificationTrigger.onCareGapIdentified(tenantId, careGap);
 * careGapNotificationTrigger.onCareGapAddressed(tenantId, careGap);
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CareGapNotificationTrigger {

    private final NotificationService notificationService;
    private final RecipientResolutionService recipientResolutionService;
    private final PatientNameService patientNameService;

    /**
     * Trigger notification when care gap is identified
     * Called by CareGapService after creating a new care gap
     *
     * @param tenantId Tenant ID
     * @param careGap Newly identified care gap
     */
    public void onCareGapIdentified(String tenantId, CareGapDTO careGap) {
        try {
            // Skip notification for low priority gaps that aren't due soon
            if (!shouldNotifyOnIdentification(careGap)) {
                log.debug("Skipping care gap identification notification for low priority gap: {}",
                        careGap.getId());
                return;
            }

            // Get recipients for this patient/tenant
            Map<String, String> recipients = getRecipients(tenantId, careGap.getPatientId());

            // Determine notification channels based on priority
            boolean sendEmail = shouldSendEmailForIdentification(careGap);
            boolean sendSms = shouldSendSmsForIdentification(careGap);

            // Build notification request
            GenericNotificationRequest request = GenericNotificationRequest.builder()
                    .notificationType("CARE_GAP_IDENTIFIED")
                    .templateId("care-gap")
                    .tenantId(tenantId)
                    .patientId(careGap.getPatientId())
                    .title(buildIdentificationTitle(careGap))
                    .message(buildIdentificationMessage(careGap))
                    .severity(mapPriorityToSeverity(careGap.getPriority()))
                    .sendWebSocket(true)  // Always send via WebSocket
                    .sendEmail(sendEmail)
                    .sendSms(sendSms)
                    .recipients(recipients)
                    .templateVariables(buildIdentificationTemplateVariables(careGap))
                    .metadata(buildMetadata(careGap))
                    .build();

            // Send notification via NotificationService
            NotificationService.NotificationStatus status = notificationService.sendNotification(request);

            if (status.isAllSuccessful()) {
                log.info("Care gap identification notification sent successfully for gap {} (priority: {})",
                        careGap.getId(), careGap.getPriority());
            } else {
                log.warn("Care gap identification notification partially failed for gap {}: {}",
                        careGap.getId(), status.getChannelStatus());
            }

        } catch (Exception e) {
            log.error("Failed to send care gap identification notification for gap {}: {}",
                    careGap.getId(), e.getMessage(), e);
        }
    }

    /**
     * Trigger notification when care gap is addressed
     * Called by CareGapService after addressing a care gap
     *
     * @param tenantId Tenant ID
     * @param careGap Addressed care gap
     */
    public void onCareGapAddressed(String tenantId, CareGapDTO careGap) {
        try {
            // Always notify when care gaps are addressed (positive feedback)
            Map<String, String> recipients = getRecipients(tenantId, careGap.getPatientId());

            // Build notification request
            GenericNotificationRequest request = GenericNotificationRequest.builder()
                    .notificationType("CARE_GAP_ADDRESSED")
                    .templateId("care-gap")
                    .tenantId(tenantId)
                    .patientId(careGap.getPatientId())
                    .title(buildAddressedTitle(careGap))
                    .message(buildAddressedMessage(careGap))
                    .severity("LOW")  // Positive event, low urgency
                    .sendWebSocket(true)
                    .sendEmail(true)  // Send email for documentation
                    .sendSms(false)   // SMS not needed for addressed gaps
                    .recipients(recipients)
                    .templateVariables(buildAddressedTemplateVariables(careGap))
                    .metadata(buildMetadata(careGap))
                    .build();

            // Send notification via NotificationService
            NotificationService.NotificationStatus status = notificationService.sendNotification(request);

            if (status.isAllSuccessful()) {
                log.info("Care gap addressed notification sent successfully for gap {}", careGap.getId());
            } else {
                log.warn("Care gap addressed notification partially failed for gap {}: {}",
                        careGap.getId(), status.getChannelStatus());
            }

        } catch (Exception e) {
            log.error("Failed to send care gap addressed notification for gap {}: {}",
                    careGap.getId(), e.getMessage(), e);
        }
    }

    /**
     * Determine if we should notify on gap identification
     * Prevents notification fatigue for low-priority gaps not due soon
     */
    private boolean shouldNotifyOnIdentification(CareGapDTO careGap) {
        // Always notify HIGH and CRITICAL priority
        if ("HIGH".equals(careGap.getPriority()) || "CRITICAL".equals(careGap.getPriority())) {
            return true;
        }

        // For MEDIUM priority, only notify if due within 30 days
        if ("MEDIUM".equals(careGap.getPriority())) {
            if (careGap.getDueDate() != null) {
                long daysUntilDue = ChronoUnit.DAYS.between(
                        LocalDateTime.now(),
                        LocalDateTime.ofInstant(careGap.getDueDate(), ZoneId.systemDefault())
                );
                return daysUntilDue <= 30;
            }
            return true; // Notify if no due date set
        }

        // For LOW priority, only notify if due within 7 days
        if ("LOW".equals(careGap.getPriority())) {
            if (careGap.getDueDate() != null) {
                long daysUntilDue = ChronoUnit.DAYS.between(
                        LocalDateTime.now(),
                        LocalDateTime.ofInstant(careGap.getDueDate(), ZoneId.systemDefault())
                );
                return daysUntilDue <= 7;
            }
            return false; // Don't notify LOW priority without due date
        }

        return true; // Default: notify
    }

    /**
     * Determine if email should be sent for gap identification
     */
    private boolean shouldSendEmailForIdentification(CareGapDTO careGap) {
        // Send email for HIGH and CRITICAL priority
        return "HIGH".equals(careGap.getPriority()) || "CRITICAL".equals(careGap.getPriority());
    }

    /**
     * Determine if SMS should be sent for gap identification
     */
    private boolean shouldSendSmsForIdentification(CareGapDTO careGap) {
        // Only send SMS for CRITICAL priority
        return "CRITICAL".equals(careGap.getPriority());
    }

    /**
     * Build notification title for gap identification
     */
    private String buildIdentificationTitle(CareGapDTO careGap) {
        return String.format("[%s] Care Gap Identified: %s",
                careGap.getPriority(), careGap.getTitle());
    }

    /**
     * Build notification message for gap identification
     */
    private String buildIdentificationMessage(CareGapDTO careGap) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("Care gap identified for patient %s: %s. ",
                careGap.getPatientId(), careGap.getTitle()));

        if (careGap.getDueDate() != null) {
            long daysUntilDue = ChronoUnit.DAYS.between(
                    LocalDateTime.now(),
                    LocalDateTime.ofInstant(careGap.getDueDate(), ZoneId.systemDefault())
            );
            message.append(String.format("Due in %d days. ", daysUntilDue));
        }

        message.append(String.format("Priority: %s. ", careGap.getPriority()));
        message.append("Action recommended.");

        return message.toString();
    }

    /**
     * Build notification title for gap addressed
     */
    private String buildAddressedTitle(CareGapDTO careGap) {
        return String.format("Care Gap Addressed: %s", careGap.getTitle());
    }

    /**
     * Build notification message for gap addressed
     */
    private String buildAddressedMessage(CareGapDTO careGap) {
        return String.format("Care gap successfully addressed for patient %s: %s. Status: %s.",
                careGap.getPatientId(), careGap.getTitle(), careGap.getStatus());
    }

    /**
     * Build template variables for identification notification
     */
    private Map<String, Object> buildIdentificationTemplateVariables(CareGapDTO careGap) {
        Map<String, Object> variables = new HashMap<>();

        // Patient information
        variables.put("patientId", careGap.getPatientId());
        variables.put("patientName", patientNameService.getPatientName(careGap.getPatientId()));

        // Care gap details
        variables.put("gapTitle", careGap.getTitle());
        variables.put("gapDescription", careGap.getDescription());
        variables.put("gapCategory", formatCategory(careGap.getCategory()));
        variables.put("gapType", careGap.getGapType());
        variables.put("priority", careGap.getPriority());
        variables.put("status", careGap.getStatus());

        // Dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (careGap.getDueDate() != null) {
            LocalDateTime dueDate = LocalDateTime.ofInstant(careGap.getDueDate(), ZoneId.systemDefault());
            variables.put("dueDate", dueDate.format(formatter));

            long daysUntilDue = ChronoUnit.DAYS.between(LocalDateTime.now(), dueDate);
            variables.put("daysUntilDue", daysUntilDue);
            variables.put("isOverdue", daysUntilDue < 0);
        }

        if (careGap.getIdentifiedDate() != null) {
            LocalDateTime identifiedDate = LocalDateTime.ofInstant(careGap.getIdentifiedDate(), ZoneId.systemDefault());
            variables.put("identifiedDate", identifiedDate.format(formatter));
        }

        // Quality measure
        variables.put("qualityMeasure", careGap.getQualityMeasure());

        // Action URL
        variables.put("actionUrl", "https://healthdata-in-motion.com/patients/" + careGap.getPatientId() + "/care-gaps/" + careGap.getId());

        // Facility name
        variables.put("facilityName", "HealthData Clinical System");

        // Event type
        variables.put("eventType", "IDENTIFIED");

        return variables;
    }

    /**
     * Build template variables for addressed notification
     */
    private Map<String, Object> buildAddressedTemplateVariables(CareGapDTO careGap) {
        Map<String, Object> variables = buildIdentificationTemplateVariables(careGap);

        // Override event type
        variables.put("eventType", "ADDRESSED");

        // Add addressed-specific fields
        if (careGap.getAddressedBy() != null) {
            variables.put("addressedBy", careGap.getAddressedBy());
        }

        if (careGap.getAddressedDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime addressedDate = LocalDateTime.ofInstant(careGap.getAddressedDate(), ZoneId.systemDefault());
            variables.put("addressedDate", addressedDate.format(formatter));
        }

        if (careGap.getAddressedNotes() != null) {
            variables.put("addressedNotes", careGap.getAddressedNotes());
        }

        return variables;
    }

    /**
     * Build metadata for notification tracking
     */
    private Map<String, Object> buildMetadata(CareGapDTO careGap) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("careGapId", careGap.getId() != null ? careGap.getId().toString() : null);
        metadata.put("gapType", careGap.getGapType());
        metadata.put("category", careGap.getCategory());
        metadata.put("priority", careGap.getPriority());
        metadata.put("qualityMeasure", careGap.getQualityMeasure());
        return metadata;
    }

    /**
     * Map care gap priority to notification severity
     */
    private String mapPriorityToSeverity(String priority) {
        return switch (priority) {
            case "CRITICAL" -> "HIGH";
            case "HIGH" -> "HIGH";
            case "MEDIUM" -> "MEDIUM";
            case "LOW" -> "LOW";
            default -> "MEDIUM";
        };
    }

    /**
     * Format category for display
     */
    private String formatCategory(String category) {
        return category != null ? category.replace("_", " ").toLowerCase() : "unknown";
    }

    /**
     * Get recipients for care gap notifications
     * Resolves recipients from patient's care team and user preferences
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Map of channel -> recipient ID
     */
    private Map<String, String> getRecipients(String tenantId, UUID patientId) {
        Map<String, String> recipients = new HashMap<>();

        // Determine notification severity for recipient resolution
        NotificationEntity.NotificationSeverity severity = NotificationEntity.NotificationSeverity.MEDIUM;

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

        log.debug("Resolved {} recipients for patient {} care gap", recipients.size(), patientId);

        return recipients;
    }
}
