package com.healthdata.notification.infrastructure.providers;

import com.healthdata.notification.domain.model.Notification;
import com.healthdata.notification.domain.model.NotificationChannel;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * SMTP-based email notification provider.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "notification.email.enabled", havingValue = "true", matchIfMissing = true)
public class SmtpEmailProvider implements NotificationProvider {

    private final JavaMailSender mailSender;
    private final EmailConfig emailConfig;

    @Override
    public void send(Notification notification) throws NotificationDeliveryException {
        if (notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank()) {
            throw new NotificationDeliveryException("Recipient email is required", false);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailConfig.getFromAddress(), emailConfig.getFromName());
            helper.setTo(notification.getRecipientEmail());
            helper.setSubject(notification.getSubject() != null ? 
                notification.getSubject() : "Notification from HDIM");
            helper.setText(notification.getBody(), true);

            if (emailConfig.getReplyTo() != null) {
                helper.setReplyTo(emailConfig.getReplyTo());
            }

            mailSender.send(message);
            log.info("Email sent successfully to {}", notification.getRecipientEmail());

        } catch (MailException e) {
            log.error("Failed to send email to {}: {}", 
                notification.getRecipientEmail(), e.getMessage());
            throw new NotificationDeliveryException("Failed to send email: " + e.getMessage(), e);
        } catch (MessagingException e) {
            log.error("Failed to create email message: {}", e.getMessage());
            throw new NotificationDeliveryException("Failed to create email: " + e.getMessage(), e, false);
        } catch (Exception e) {
            log.error("Unexpected error sending email: {}", e.getMessage());
            throw new NotificationDeliveryException("Unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public Set<NotificationChannel> getSupportedChannels() {
        return Set.of(NotificationChannel.EMAIL);
    }

    @Override
    public boolean isAvailable() {
        return emailConfig.isEnabled();
    }
}
