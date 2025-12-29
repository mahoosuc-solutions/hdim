package com.healthdata.notification.infrastructure.providers;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * SMTP-based email provider using Spring Mail.
 */
@Component
@ConditionalOnProperty(name = "notification.email.provider", havingValue = "smtp", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailProvider implements EmailProvider {

    private final JavaMailSender mailSender;
    private final NotificationEmailProperties emailProperties;

    @Override
    public String send(String to, String subject, String body) {
        return sendHtml(to, subject, body, body);
    }

    @Override
    public String sendHtml(String to, String subject, String htmlBody, String textBody) {
        String messageId = UUID.randomUUID().toString();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailProperties.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);

            if (emailProperties.getReplyTo() != null) {
                helper.setReplyTo(emailProperties.getReplyTo());
            }

            mailSender.send(message);
            log.info("Email sent successfully to: {}, messageId: {}", to, messageId);

            return messageId;

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // Simple availability check - try to create a test connection
            mailSender.createMimeMessage();
            return true;
        } catch (Exception e) {
            log.warn("Email provider not available: {}", e.getMessage());
            return false;
        }
    }
}
