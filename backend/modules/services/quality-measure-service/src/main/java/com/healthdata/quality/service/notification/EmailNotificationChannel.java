package com.healthdata.quality.service.notification;

import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.dto.notification.NotificationRequest;
import com.healthdata.quality.persistence.NotificationHistoryEntity;
import com.healthdata.quality.persistence.NotificationHistoryRepository;
import com.healthdata.quality.service.PatientNameService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Email Notification Channel
 *
 * Sends notifications via email with beautiful HTML templates.
 * Uses Thymeleaf templates for professional, mobile-responsive emails.
 * Tracks all sent notifications for HIPAA compliance audit trail.
 * Supports all notification types via NotificationRequest abstraction.
 *
 * Only created when JavaMailSender is available (mail properties configured).
 */
@Component
@ConditionalOnBean(JavaMailSender.class)
@Slf4j
public class EmailNotificationChannel {

    private final JavaMailSender mailSender;
    private final TemplateRenderer templateRenderer;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final PatientNameService patientNameService;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${notification.email.default-recipient:care-team@example.com}")
    private String defaultRecipient;

    @Autowired
    public EmailNotificationChannel(JavaMailSender mailSender,
                                    TemplateRenderer templateRenderer,
                                    NotificationHistoryRepository notificationHistoryRepository,
                                    @Autowired(required = false) PatientNameService patientNameService) {
        this.mailSender = mailSender;
        this.templateRenderer = templateRenderer;
        this.notificationHistoryRepository = notificationHistoryRepository;
        this.patientNameService = patientNameService;
    }

    /**
     * Send notification via email using NotificationRequest
     * Universal method that works with all notification types
     *
     * @param request NotificationRequest containing all notification data
     * @return true if sent successfully, false otherwise
     */
    public boolean send(NotificationRequest request) {
        String subject = request.getTitle();
        String htmlContent = null;
        String status = "FAILED";
        String errorMessage = null;
        String tenantId = request.getTenantId();

        // Get recipient email address
        String recipientEmail = request.getRecipients() != null
                ? request.getRecipients().getOrDefault("EMAIL", defaultRecipient)
                : defaultRecipient;

        try {
            // Render HTML template using template variables from request
            htmlContent = templateRenderer.render(request.getTemplateId(), request.getTemplateVariables());

            // Create and send email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("alerts@healthdata.com");
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = isHtml

            mailSender.send(mimeMessage);

            status = "SENT";
            log.info("{} email notification sent to {} using {} template",
                    request.getNotificationType(), recipientEmail, request.getTemplateId());

            return true;
        } catch (MessagingException e) {
            errorMessage = "Failed to create email message: " + e.getMessage();
            log.error(errorMessage);
            return false;
        } catch (Exception e) {
            errorMessage = "Failed to send email notification: " + e.getMessage();
            log.error(errorMessage);
            return false;
        } finally {
            // Always save notification history for HIPAA compliance
            saveNotificationHistory(tenantId, request, subject, htmlContent,
                    recipientEmail, status, errorMessage);
        }
    }

    /**
     * Save notification history for NotificationRequest
     * Runs in finally block to ensure history is saved even if send fails
     */
    private void saveNotificationHistory(String tenantId, NotificationRequest request,
                                         String subject, String content, String recipientEmail,
                                         String status, String errorMessage) {
        try {
            NotificationHistoryEntity history = NotificationHistoryEntity.builder()
                    .tenantId(tenantId)
                    .notificationType(request.getNotificationType())
                    .channel("EMAIL")
                    .templateId(request.getTemplateId())
                    .patientId(request.getPatientId())
                    .recipientId(recipientEmail)
                    .subject(subject)
                    .content(content)
                    .status(status)
                    .errorMessage(errorMessage)
                    .alertId(request.getRelatedEntityId())
                    .severity(request.getSeverity())
                    .sentAt(Instant.now())
                    .metadata(request.getMetadata() != null ?
                            request.getMetadata().toString() : null)
                    .build();

            notificationHistoryRepository.save(history);
            log.debug("Notification history saved for {} notification",
                    request.getNotificationType());
        } catch (Exception e) {
            // Log but don't throw - history saving should not prevent notification
            log.error("Failed to save notification history: {}", e.getMessage());
        }
    }

    /**
     * Send notification for a clinical alert (deprecated - use NotificationRequest).
     *
     * @param tenantId The tenant ID
     * @param alert    The clinical alert DTO
     * @return true if sent successfully, false otherwise
     * @deprecated Use {@link #send(NotificationRequest)} instead
     */
    @Deprecated
    public boolean send(String tenantId, ClinicalAlertDTO alert) {
        String subject = formatSubject(alert);
        String htmlContent = null;
        String status = "FAILED";
        String errorMessage = null;

        try {
            Map<String, Object> templateVariables = buildTemplateVariables(alert);
            htmlContent = templateRenderer.render("critical-alert", templateVariables);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("alerts@healthdata.com");
            helper.setTo(defaultRecipient);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);

            status = "SENT";
            log.info("Clinical alert email sent for alert: {}", alert.getId());
            return true;

        } catch (MessagingException e) {
            errorMessage = "Failed to create email message: " + e.getMessage();
            log.error(errorMessage);
            return false;
        } catch (Exception e) {
            errorMessage = "Failed to send email notification: " + e.getMessage();
            log.error(errorMessage);
            return false;
        } finally {
            saveAlertNotificationHistory(tenantId, alert, subject, htmlContent, status, errorMessage);
        }
    }

    private void saveAlertNotificationHistory(String tenantId, ClinicalAlertDTO alert,
                                              String subject, String content,
                                              String status, String errorMessage) {
        try {
            NotificationHistoryEntity history = NotificationHistoryEntity.builder()
                .tenantId(tenantId)
                .notificationType("CLINICAL_ALERT")
                .channel("EMAIL")
                .templateId("critical-alert")
                .patientId(alert.getPatientId())
                .recipientId(defaultRecipient)
                .subject(subject)
                .content(content)
                .status(status)
                .errorMessage(errorMessage)
                .alertId(alert.getId())
                .severity(alert.getSeverity())
                .sentAt(Instant.now())
                .build();

            notificationHistoryRepository.save(history);
        } catch (Exception e) {
            log.error("Failed to save notification history: {}", e.getMessage());
        }
    }

    String formatSubject(ClinicalAlertDTO alert) {
        String severity = alert.getSeverity();
        String title = alert.getTitle();

        if ("CRITICAL".equalsIgnoreCase(severity)) {
            return "[URGENT] " + title;
        } else if ("HIGH".equalsIgnoreCase(severity)) {
            return "[HIGH PRIORITY] " + title;
        }
        return title;
    }

    Map<String, Object> buildTemplateVariables(ClinicalAlertDTO alert) {
        Map<String, Object> variables = new LinkedHashMap<>();

        // Get patient name if service is available
        String patientName = "Unknown Patient";
        if (patientNameService != null && alert.getPatientId() != null) {
            try {
                patientName = patientNameService.getPatientName(alert.getPatientId());
            } catch (Exception e) {
                log.warn("Failed to get patient name: {}", e.getMessage());
            }
        }

        variables.put("patientName", patientName);
        variables.put("severity", alert.getSeverity());
        variables.put("alertType", alert.getAlertType());
        variables.put("formattedAlertType", formatAlertType(alert.getAlertType()));
        variables.put("title", alert.getTitle());
        variables.put("message", alert.getMessage());
        variables.put("alertId", alert.getId());

        // Format timestamp
        if (alert.getTriggeredAt() != null) {
            String formattedTimestamp = LocalDateTime.ofInstant(
                alert.getTriggeredAt(), ZoneId.systemDefault()
            ).format(TIMESTAMP_FORMATTER);
            variables.put("timestamp", formattedTimestamp);
        } else {
            variables.put("timestamp", "N/A");
        }

        // Build details map
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Alert ID", alert.getId());
        details.put("Escalated", alert.isEscalated()
            ? "Yes - IMMEDIATE ATTENTION REQUIRED"
            : "No");
        variables.put("details", details);

        // Build recommended actions
        variables.put("recommendedActions", getRecommendedActions(alert));
        variables.put("actionGuidance", getActionGuidance(alert));

        return variables;
    }

    String formatAlertType(String alertType) {
        if (alertType == null) {
            return "Unknown";
        }
        return switch (alertType) {
            case "MENTAL_HEALTH_CRISIS" -> "Mental Health Crisis";
            case "RISK_ESCALATION" -> "Risk Escalation";
            case "HEALTH_DECLINE" -> "Health Score Decline";
            case "CHRONIC_DETERIORATION" -> "Chronic Disease Deterioration";
            default -> alertType.replace("_", " ");
        };
    }

    String getActionGuidance(ClinicalAlertDTO alert) {
        String severity = alert.getSeverity();
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            return "This alert requires immediate attention. Contact patient within 24 hours.";
        } else if ("HIGH".equalsIgnoreCase(severity)) {
            return "Review alert details and take appropriate action within 48 hours.";
        }
        return "Review alert during next scheduled patient contact.";
    }

    private List<String> getRecommendedActions(ClinicalAlertDTO alert) {
        List<String> actions = new ArrayList<>();

        if (alert.isEscalated() ||
            "MENTAL_HEALTH_CRISIS".equals(alert.getAlertType())) {
            actions.add("Contact patient IMMEDIATELY for safety assessment");
            actions.add("Ensure crisis intervention resources are available");
        } else if ("CRITICAL".equalsIgnoreCase(alert.getSeverity())) {
            actions.add("Contact patient within 24 hours");
            actions.add("Review recent clinical data");
        } else {
            actions.add("Review patient record");
            actions.add("Schedule follow-up if needed");
        }

        return actions;
    }

}
