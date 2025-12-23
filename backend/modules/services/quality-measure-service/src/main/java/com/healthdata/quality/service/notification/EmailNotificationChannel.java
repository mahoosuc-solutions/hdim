package com.healthdata.quality.service.notification;

import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.dto.notification.NotificationRequest;
import com.healthdata.quality.persistence.NotificationHistoryEntity;
import com.healthdata.quality.persistence.NotificationHistoryRepository;
import com.healthdata.quality.service.PatientNameService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationChannel {

    private final JavaMailSender mailSender;
    private final TemplateRenderer templateRenderer;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final PatientNameService patientNameService;

    @Value("${notification.email.default-recipient:care-team@example.com}")
    private String defaultRecipient;

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
     * Send alert via email using HTML template
     * Records notification in history for HIPAA compliance
     *
     * @deprecated Use {@link #send(NotificationRequest)} instead
     */
    @Deprecated
    public boolean send(String tenantId, ClinicalAlertDTO alert) {
        String subject = formatSubject(alert);
        String htmlContent = null;
        String status = "FAILED";
        String errorMessage = null;

        try {
            // Render HTML template
            Map<String, Object> templateVariables = buildTemplateVariables(alert);
            htmlContent = templateRenderer.render("critical-alert", templateVariables);

            // Create and send email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("alerts@healthdata.com");
            helper.setTo(defaultRecipient);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = isHtml

            mailSender.send(mimeMessage);

            status = "SENT";
            log.info("HTML email notification sent for alert {} to {} using critical-alert template",
                    alert.getId(), defaultRecipient);

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
            saveNotificationHistory(tenantId, alert, subject, htmlContent, status, errorMessage);
        }
    }

    /**
     * Save notification history for HIPAA audit trail
     * Runs in finally block to ensure history is saved even if send fails
     */
    private void saveNotificationHistory(String tenantId, ClinicalAlertDTO alert,
                                         String subject, String content,
                                         String status, String errorMessage) {
        try {
            NotificationHistoryEntity history = NotificationHistoryEntity.builder()
                    .tenantId(tenantId)
                    .notificationType("CRITICAL_ALERT")
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
            log.debug("Notification history saved for alert {}", alert.getId());
        } catch (Exception e) {
            // Log but don't throw - history saving should not prevent notification
            log.error("Failed to save notification history: {}", e.getMessage());
        }
    }

    /**
     * Format email subject based on alert type and severity
     */
    private String formatSubject(ClinicalAlertDTO alert) {
        return switch (alert.getSeverity()) {
            case "CRITICAL" -> String.format("[URGENT] %s", alert.getTitle());
            case "HIGH" -> String.format("[HIGH PRIORITY] %s", alert.getTitle());
            default -> alert.getTitle();
        };
    }

    /**
     * Build template variables from ClinicalAlertDTO
     * Maps alert data to the critical-alert template structure
     */
    private Map<String, Object> buildTemplateVariables(ClinicalAlertDTO alert) {
        Map<String, Object> variables = new HashMap<>();

        // Channel (EMAIL for this channel)
        variables.put("channel", "EMAIL");

        // Alert core fields
        variables.put("alertType", formatAlertType(alert.getAlertType()));
        variables.put("severity", alert.getSeverity());
        variables.put("alertMessage", alert.getMessage());

        // Patient information
        String patientId = alert.getPatientId() != null ? alert.getPatientId().toString() : null;
        variables.put("patientName", patientNameService.getPatientName(alert.getPatientId()));
        variables.put("mrn", patientId);
        variables.put("patientId", patientId);

        // Timestamp (convert Instant to LocalDateTime for formatting)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime triggeredTime = alert.getTriggeredAt() != null
                ? LocalDateTime.ofInstant(alert.getTriggeredAt(), ZoneId.systemDefault())
                : LocalDateTime.now();
        variables.put("timestamp", triggeredTime.format(formatter));

        // Facility name
        variables.put("facilityName", "HealthData Clinical System");

        // Action URL - link to patient detail page
        variables.put("actionUrl", "https://healthdata-in-motion.com/patients/" + patientId);

        // Additional details (for the details table in template)
        Map<String, String> details = new HashMap<>();
        details.put("Alert ID", alert.getId() != null ? alert.getId() : "N/A");
        details.put("Alert Type", formatAlertType(alert.getAlertType()));
        details.put("Severity", alert.getSeverity());
        details.put("Escalated", alert.isEscalated() ? "Yes - IMMEDIATE ATTENTION REQUIRED" : "No");
        details.put("Triggered At", triggeredTime.format(formatter));
        variables.put("details", details);

        // Recommended actions (for the actions list in template)
        List<String> actions = List.of(getActionGuidance(alert).split("\n"));
        variables.put("recommendedActions", actions);

        return variables;
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
}
