package com.healthdata.sales.service;

import com.healthdata.sales.config.EmailConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Email Sender Service
 *
 * Abstracts email sending across different providers:
 * - SMTP (via JavaMailSender)
 * - SendGrid (via REST API)
 * - Amazon SES (planned)
 *
 * Includes rate limiting and error handling.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailSenderService {

    private final EmailConfig emailConfig;
    private final JavaMailSender javaMailSender;
    private final RestTemplate emailRestTemplate;

    private final AtomicLong dailySendCount = new AtomicLong(0);
    private long lastResetTime = System.currentTimeMillis();

    @PostConstruct
    public void init() {
        log.info("Email sender initialized with provider: {}", emailConfig.getProvider());
        if (emailConfig.isSendGridProvider()) {
            log.info("SendGrid API configured");
        }
    }

    /**
     * Send an email using the configured provider
     */
    public SendResult send(EmailMessage message) {
        // Rate limiting check
        if (!checkRateLimit()) {
            return SendResult.rateLimited("Daily send limit exceeded");
        }

        try {
            if (emailConfig.isSendGridProvider()) {
                return sendViaSendGrid(message);
            } else {
                return sendViaSmtp(message);
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", message.getTo(), e.getMessage(), e);
            return SendResult.failed(e.getMessage());
        }
    }

    /**
     * Send via SMTP (JavaMailSender)
     */
    private SendResult sendViaSmtp(EmailMessage message) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(message.getTo());
        helper.setSubject(message.getSubject());

        if (message.getTextContent() != null && message.getHtmlContent() != null) {
            helper.setText(message.getTextContent(), message.getHtmlContent());
        } else if (message.getHtmlContent() != null) {
            helper.setText("", message.getHtmlContent());
        } else {
            helper.setText(message.getTextContent() != null ? message.getTextContent() : "");
        }

        String fromAddress = message.getFromAddress() != null ? message.getFromAddress() : emailConfig.getFrom().getAddress();
        String fromName = message.getFromName() != null ? message.getFromName() : emailConfig.getFrom().getName();

        if (fromAddress != null) {
            if (fromName != null) {
                helper.setFrom(fromAddress, fromName);
            } else {
                helper.setFrom(fromAddress);
            }
        }

        if (message.getReplyTo() != null) {
            helper.setReplyTo(message.getReplyTo());
        }

        javaMailSender.send(mimeMessage);
        dailySendCount.incrementAndGet();

        log.debug("Email sent via SMTP to {}", message.getTo());
        return SendResult.success(null);
    }

    /**
     * Send via SendGrid API
     */
    private SendResult sendViaSendGrid(EmailMessage message) {
        String fromAddress = message.getFromAddress() != null ? message.getFromAddress() : emailConfig.getFrom().getAddress();
        String fromName = message.getFromName() != null ? message.getFromName() : emailConfig.getFrom().getName();

        // Build SendGrid API request
        Map<String, Object> requestBody = new HashMap<>();

        // From
        Map<String, String> from = new HashMap<>();
        from.put("email", fromAddress);
        if (fromName != null) {
            from.put("name", fromName);
        }
        requestBody.put("from", from);

        // Personalizations (recipients)
        Map<String, Object> personalization = new HashMap<>();
        personalization.put("to", List.of(Map.of("email", message.getTo())));
        if (message.getSubject() != null) {
            personalization.put("subject", message.getSubject());
        }
        requestBody.put("personalizations", List.of(personalization));

        // Content
        if (message.getHtmlContent() != null) {
            requestBody.put("content", List.of(
                Map.of("type", "text/html", "value", message.getHtmlContent())
            ));
        } else if (message.getTextContent() != null) {
            requestBody.put("content", List.of(
                Map.of("type", "text/plain", "value", message.getTextContent())
            ));
        }

        // Reply-to
        if (message.getReplyTo() != null) {
            requestBody.put("reply_to", Map.of("email", message.getReplyTo()));
        }

        // Tracking settings
        Map<String, Object> trackingSettings = new HashMap<>();
        trackingSettings.put("click_tracking", Map.of("enable", false)); // We handle our own tracking
        trackingSettings.put("open_tracking", Map.of("enable", false));
        requestBody.put("tracking_settings", trackingSettings);

        // Send request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(emailConfig.getSendgrid().getApiKey());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = emailRestTemplate.exchange(
                emailConfig.getSendgrid().getApiUrl(),
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                dailySendCount.incrementAndGet();
                String messageId = response.getHeaders().getFirst("X-Message-Id");
                log.debug("Email sent via SendGrid to {}, messageId: {}", message.getTo(), messageId);
                return SendResult.success(messageId);
            } else {
                log.error("SendGrid returned error: {} - {}", response.getStatusCode(), response.getBody());
                return SendResult.failed("SendGrid error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("SendGrid API call failed: {}", e.getMessage());
            return SendResult.failed("SendGrid API error: " + e.getMessage());
        }
    }

    /**
     * Check rate limiting
     */
    private boolean checkRateLimit() {
        // Reset daily counter at midnight
        long now = System.currentTimeMillis();
        if (now - lastResetTime > 86400000) { // 24 hours
            dailySendCount.set(0);
            lastResetTime = now;
        }

        int dailyLimit = emailConfig.getRateLimit().getPerDay();
        if (dailyLimit > 0 && dailySendCount.get() >= dailyLimit) {
            log.warn("Daily email limit reached: {}", dailyLimit);
            return false;
        }

        return true;
    }

    /**
     * Get current send statistics
     */
    public SendStats getStats() {
        return new SendStats(
            dailySendCount.get(),
            emailConfig.getRateLimit().getPerDay(),
            emailConfig.getProvider()
        );
    }

    // ==================== DTOs ====================

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class EmailMessage {
        private String to;
        private String subject;
        private String textContent;
        private String htmlContent;
        private String fromAddress;
        private String fromName;
        private String replyTo;
        private Map<String, String> headers;
        private Map<String, Object> metadata;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SendResult {
        private boolean success;
        private String messageId;
        private String error;
        private boolean rateLimited;

        public static SendResult success(String messageId) {
            return new SendResult(true, messageId, null, false);
        }

        public static SendResult failed(String error) {
            return new SendResult(false, null, error, false);
        }

        public static SendResult rateLimited(String reason) {
            return new SendResult(false, null, reason, true);
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SendStats {
        private long sentToday;
        private int dailyLimit;
        private String provider;
    }
}
