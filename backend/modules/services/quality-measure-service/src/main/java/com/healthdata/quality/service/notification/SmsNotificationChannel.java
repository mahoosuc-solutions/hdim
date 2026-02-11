package com.healthdata.quality.service.notification;

import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.dto.notification.NotificationRequest;
import com.healthdata.quality.persistence.NotificationHistoryEntity;
import com.healthdata.quality.persistence.NotificationHistoryRepository;
import com.healthdata.quality.service.PatientNameService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * SMS Notification Channel
 *
 * Sends notifications via SMS using concise text templates.
 * Tracks all sent notifications for HIPAA compliance audit trail.
 * Ready for Twilio or similar SMS service integration.
 * Supports all notification types via NotificationRequest abstraction.
 *
 * SMS messages are automatically kept concise for mobile delivery.
 */
@Component
@Slf4j
public class SmsNotificationChannel {

    private final TemplateRenderer templateRenderer;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final PatientNameService patientNameService;

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-phone:}")
    private String fromPhone;

    @Value("${twilio.enabled:false}")
    private boolean twilioEnabled;

    private boolean twilioInitialized = false;

    @Autowired
    public SmsNotificationChannel(TemplateRenderer templateRenderer,
                                  NotificationHistoryRepository notificationHistoryRepository,
                                  @Autowired(required = false) PatientNameService patientNameService) {
        this.templateRenderer = templateRenderer;
        this.notificationHistoryRepository = notificationHistoryRepository;
        this.patientNameService = patientNameService;
    }

    @PostConstruct
    public void initTwilio() {
        if (twilioEnabled && accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()) {
            try {
                Twilio.init(accountSid, authToken);
                twilioInitialized = true;
                log.info("Twilio SMS service initialized successfully");
            } catch (Exception e) {
                log.warn("Failed to initialize Twilio: {}. SMS notifications will be mocked.", e.getMessage());
            }
        } else {
            log.info("Twilio SMS service not configured. SMS notifications will be mocked.");
        }
    }

    // Default phone for fallback/testing
    private static final String DEFAULT_PHONE = "+12345678900";

    /**
     * Send notification via SMS using NotificationRequest
     * Universal method that works with all notification types
     *
     * @param request NotificationRequest containing all notification data
     * @return true if sent successfully, false otherwise
     */
    public boolean send(NotificationRequest request) {
        String message = null;
        String status = "FAILED";
        String errorMessage = null;
        String tenantId = request.getTenantId();

        // Get recipient phone number
        String recipientPhone = request.getRecipients() != null
                ? request.getRecipients().getOrDefault("SMS", DEFAULT_PHONE)
                : DEFAULT_PHONE;

        try {
            // Build template variables and render SMS template
            Map<String, Object> templateVariables = request.getTemplateVariables();
            // Add channel indicator for SMS-specific formatting
            templateVariables.put("channel", "SMS");

            message = templateRenderer.render(request.getTemplateId(), templateVariables);

            // Send via Twilio if configured, otherwise mock
            if (twilioInitialized && fromPhone != null && !fromPhone.isBlank()) {
                Message twilioMessage = Message.creator(
                    new PhoneNumber(recipientPhone),
                    new PhoneNumber(fromPhone),
                    message
                ).create();

                status = "SENT";
                log.info("{} SMS notification sent to {} via Twilio. SID: {}",
                        request.getNotificationType(), recipientPhone, twilioMessage.getSid());
            } else {
                status = "SENT";  // Mock mode
                log.info("{} SMS notification sent to {} using {} template (MOCK)",
                        request.getNotificationType(), recipientPhone, request.getTemplateId());
            }
            log.debug("SMS content ({} chars): {}", message.length(), message);

            return true;

        } catch (Exception e) {
            errorMessage = "Failed to send SMS notification: " + e.getMessage();
            log.error(errorMessage);
            return false;
        } finally {
            // Always save notification history for HIPAA compliance
            saveNotificationHistory(tenantId, request, message, recipientPhone, status, errorMessage);
        }
    }

    /**
     * Save notification history for NotificationRequest
     * Runs in finally block to ensure history is saved even if send fails
     */
    private void saveNotificationHistory(String tenantId, NotificationRequest request,
                                         String content, String recipientPhone,
                                         String status, String errorMessage) {
        try {
            NotificationHistoryEntity history = NotificationHistoryEntity.builder()
                    .tenantId(tenantId)
                    .notificationType(request.getNotificationType())
                    .channel("SMS")
                    .templateId(request.getTemplateId())
                    .patientId(request.getPatientId())
                    .recipientId(recipientPhone)
                    .subject(null)  // SMS doesn't have subjects
                    .content(content)  // Encrypted via @Encrypted annotation on entity
                    .status(status)
                    .errorMessage(errorMessage)
                    .alertId(request.getRelatedEntityId())
                    .severity(request.getSeverity())
                    .sentAt(Instant.now())
                    .metadata(request.getMetadata() != null ?
                            request.getMetadata().toString() : null)
                    .build();

            notificationHistoryRepository.save(history);
            log.debug("Notification history saved for {} SMS notification",
                    request.getNotificationType());
        } catch (Exception e) {
            // Log but don't throw - history saving should not prevent notification
            log.error("Failed to save notification history: {}", e.getMessage());
        }
    }

    // ==================== DEPRECATED METHODS FOR BACKWARD COMPATIBILITY ====================

    /**
     * @deprecated Use {@link #send(NotificationRequest)} instead.
     * Maintained for backward compatibility with clinical alert workflow.
     */
    @Deprecated
    public boolean send(String tenantId, ClinicalAlertDTO alert) {
        String smsContent = null;
        String status = "FAILED";
        String errorMessage = null;

        try {
            // Build template variables
            Map<String, Object> templateVariables = buildAlertTemplateVariables(alert);
            templateVariables.put("channel", "SMS");

            // Render SMS template
            smsContent = templateRenderer.render("critical-alert", templateVariables);

            // Send via Twilio if configured, otherwise mock
            if (twilioInitialized && fromPhone != null && !fromPhone.isBlank()) {
                Message twilioMessage = Message.creator(
                    new PhoneNumber(DEFAULT_PHONE),
                    new PhoneNumber(fromPhone),
                    smsContent
                ).create();

                status = "SENT";
                log.info("SMS notification sent for alert {} via Twilio. SID: {}",
                        alert.getId(), twilioMessage.getSid());
            } else {
                status = "SENT";  // Mock mode
                log.info("SMS notification sent for alert {} (MOCK)", alert.getId());
            }
            log.debug("SMS content ({} chars): {}", smsContent.length(), smsContent);

            return true;
        } catch (Exception e) {
            errorMessage = "Failed to send SMS notification: " + e.getMessage();
            log.error(errorMessage);
            return false;
        } finally {
            saveAlertNotificationHistory(tenantId, alert, smsContent, DEFAULT_PHONE, status, errorMessage);
        }
    }

    /**
     * Build template variables for alert SMS
     */
    private Map<String, Object> buildAlertTemplateVariables(ClinicalAlertDTO alert) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("alertType", formatAlertType(alert.getAlertType()));
        variables.put("severity", alert.getSeverity());
        variables.put("message", alert.getMessage());

        // Format timestamp
        if (alert.getTriggeredAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String timestamp = LocalDateTime.ofInstant(alert.getTriggeredAt(), ZoneId.systemDefault())
                    .format(formatter);
            variables.put("timestamp", timestamp);
        } else {
            variables.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        // Get patient name if available
        String patientName = "Unknown Patient";
        if (patientNameService != null && alert.getPatientId() != null) {
            try {
                patientName = patientNameService.getPatientName(alert.getPatientId());
            } catch (Exception e) {
                log.warn("Failed to get patient name for {}: {}", alert.getPatientId(), e.getMessage());
            }
        }
        variables.put("patientName", patientName);

        return variables;
    }

    /**
     * Format alert type for display
     */
    private String formatAlertType(String alertType) {
        if (alertType == null) return "Unknown";
        return switch (alertType) {
            case "MENTAL_HEALTH_CRISIS" -> "Mental Health Crisis";
            case "RISK_ESCALATION" -> "Risk Escalation";
            case "HEALTH_DECLINE" -> "Health Score Decline";
            case "CHRONIC_DETERIORATION" -> "Chronic Disease Deterioration";
            default -> alertType.replace("_", " ").toLowerCase()
                    .substring(0, 1).toUpperCase() +
                    alertType.replace("_", " ").toLowerCase().substring(1);
        };
    }

    /**
     * Save notification history for deprecated alert method
     */
    private void saveAlertNotificationHistory(String tenantId, ClinicalAlertDTO alert,
                                              String content, String recipientPhone,
                                              String status, String errorMessage) {
        try {
            NotificationHistoryEntity history = NotificationHistoryEntity.builder()
                    .tenantId(tenantId)
                    .notificationType("CLINICAL_ALERT")
                    .channel("SMS")
                    .templateId("critical-alert")
                    .patientId(alert.getPatientId())
                    .recipientId(recipientPhone)
                    .subject(null)
                    .content(content)
                    .status(status)
                    .errorMessage(errorMessage)
                    .alertId(alert.getId())
                    .severity(alert.getSeverity())
                    .sentAt(Instant.now())
                    .build();

            notificationHistoryRepository.save(history);
            log.debug("Notification history saved for clinical alert SMS {}", alert.getId());
        } catch (Exception e) {
            log.error("Failed to save notification history: {}", e.getMessage());
        }
    }

}
