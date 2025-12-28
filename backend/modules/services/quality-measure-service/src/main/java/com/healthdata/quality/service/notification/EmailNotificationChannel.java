package com.healthdata.quality.service.notification;

import com.healthdata.quality.dto.notification.NotificationRequest;
import com.healthdata.quality.persistence.NotificationHistoryEntity;
import com.healthdata.quality.persistence.NotificationHistoryRepository;
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

}
