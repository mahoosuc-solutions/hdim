package com.healthdata.notification.infrastructure.providers;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Twilio SMS Provider Implementation
 *
 * Sends SMS messages via Twilio's Programmable SMS API.
 *
 * Features:
 * - Circuit breaker pattern for Twilio API resilience
 * - Exponential backoff retry (max 3 attempts)
 * - E.164 phone number validation
 * - Message length validation (1600 chars for concatenated SMS)
 * - Alphanumeric sender ID support
 * - Messaging Service SID support (for advanced features)
 *
 * HIPAA Compliance:
 * - SMS may contain PHI - ensure proper consent obtained
 * - Audit logging required for all SMS sent (handled by service layer)
 * - TLS/HTTPS used for all Twilio API calls
 *
 * Configuration:
 * notification.sms.twilio.enabled=true
 * notification.sms.twilio.account-sid=ACxxx
 * notification.sms.twilio.auth-token=xxx
 * notification.sms.twilio.from-number=+11234567890
 * notification.sms.twilio.messaging-service-sid=MGxxx (optional)
 */
@Component
@ConditionalOnProperty(prefix = "notification.sms.twilio", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class TwilioSmsProvider implements SmsProvider {

    private final TwilioSmsProperties properties;

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.warn("Twilio SMS provider is disabled. Set notification.sms.twilio.enabled=true to enable.");
            return;
        }

        if (properties.getAccountSid() == null || properties.getAuthToken() == null) {
            log.error("Twilio SMS provider is enabled but account-sid or auth-token is missing");
            throw new IllegalStateException("Twilio credentials are required");
        }

        if (properties.getFromNumber() == null && properties.getMessagingServiceSid() == null) {
            log.error("Twilio SMS provider requires either from-number or messaging-service-sid");
            throw new IllegalStateException("Twilio from-number or messaging-service-sid is required");
        }

        Twilio.init(properties.getAccountSid(), properties.getAuthToken());
        log.info("Twilio SMS provider initialized successfully (from: {})",
                maskPhoneNumber(properties.getFromNumber()));
    }

    @PreDestroy
    public void destroy() {
        Twilio.destroy();
        log.info("Twilio SMS provider destroyed");
    }

    /**
     * Send an SMS message via Twilio
     *
     * Circuit Breaker:
     * - Opens after 5 failures in 60 seconds
     * - Half-open after 30 seconds
     * - Fallback: Logs error and returns "CIRCUIT_BREAKER_OPEN"
     *
     * Retry:
     * - Max 3 attempts
     * - Exponential backoff: 1s, 2s, 4s
     * - Retries only on transient errors (rate limit, network issues)
     *
     * @param to      Recipient phone number (E.164 format, e.g., +1234567890)
     * @param message SMS message body (max 1600 characters)
     * @return Twilio message SID (e.g., "SM1234567890abcdef") or error code
     */
    @Override
    @CircuitBreaker(name = "twilio-sms", fallbackMethod = "sendFallback")
    @Retry(name = "twilio-sms")
    public String send(String to, String message) {
        validatePhoneNumber(to);
        validateMessage(message);

        log.debug("Sending SMS to {} (length: {} chars)", maskPhoneNumber(to), message.length());

        try {
            Message twilioMessage;

            if (properties.getMessagingServiceSid() != null) {
                // Use Messaging Service (advanced features: copilot, geo permissions)
                twilioMessage = Message.creator(
                        new PhoneNumber(to),
                        properties.getMessagingServiceSid(),
                        message
                ).create();
            } else {
                // Use simple from number
                twilioMessage = Message.creator(
                        new PhoneNumber(to),
                        new PhoneNumber(properties.getFromNumber()),
                        message
                ).create();
            }

            log.info("SMS sent successfully: to={}, sid={}, status={}",
                    maskPhoneNumber(to),
                    twilioMessage.getSid(),
                    twilioMessage.getStatus());

            return twilioMessage.getSid();

        } catch (ApiException e) {
            log.error("Twilio API error: code={}, message={}, to={}",
                    e.getCode(), e.getMessage(), maskPhoneNumber(to));
            throw e; // Circuit breaker will catch and invoke fallback
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}",
                    maskPhoneNumber(to), e.getMessage(), e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }

    /**
     * Send SMS with custom sender ID
     *
     * Alphanumeric sender IDs:
     * - Max 11 characters
     * - A-Z, a-z, 0-9, spaces
     * - Not supported in all countries (works in EU, not in US/Canada)
     *
     * @param to       Recipient phone number
     * @param message  SMS message body
     * @param senderId Alphanumeric sender ID (e.g., "HealthData")
     * @return Twilio message SID or error code
     */
    @Override
    @CircuitBreaker(name = "twilio-sms", fallbackMethod = "sendWithSenderIdFallback")
    @Retry(name = "twilio-sms")
    public String sendWithSenderId(String to, String message, String senderId) {
        validatePhoneNumber(to);
        validateMessage(message);
        validateSenderId(senderId);

        log.debug("Sending SMS with sender ID '{}' to {}", senderId, maskPhoneNumber(to));

        try {
            Message twilioMessage = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(senderId), // Alphanumeric sender ID
                    message
            ).create();

            log.info("SMS sent successfully with sender ID: to={}, sid={}, status={}",
                    maskPhoneNumber(to),
                    twilioMessage.getSid(),
                    twilioMessage.getStatus());

            return twilioMessage.getSid();

        } catch (ApiException e) {
            log.error("Twilio API error with sender ID '{}': code={}, message={}, to={}",
                    senderId, e.getCode(), e.getMessage(), maskPhoneNumber(to));
            throw e;
        } catch (Exception e) {
            log.error("Failed to send SMS with sender ID '{}' to {}: {}",
                    senderId, maskPhoneNumber(to), e.getMessage(), e);
            throw new RuntimeException("Failed to send SMS with sender ID", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled() && Twilio.getRestClient() != null;
    }

    @Override
    public String getProviderName() {
        return "Twilio";
    }

    // Fallback methods for circuit breaker

    private String sendFallback(String to, String message, Throwable throwable) {
        log.error("Circuit breaker fallback: Failed to send SMS to {} after retries: {}",
                maskPhoneNumber(to), throwable.getMessage());
        return "CIRCUIT_BREAKER_OPEN";
    }

    private String sendWithSenderIdFallback(String to, String message, String senderId, Throwable throwable) {
        log.error("Circuit breaker fallback: Failed to send SMS with sender ID '{}' to {} after retries: {}",
                senderId, maskPhoneNumber(to), throwable.getMessage());
        return "CIRCUIT_BREAKER_OPEN";
    }

    // Validation methods

    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        // E.164 format validation: +[country code][number]
        if (!phoneNumber.matches("^\\+[1-9]\\d{1,14}$")) {
            throw new IllegalArgumentException(
                    "Invalid phone number format. Must be E.164 format (e.g., +1234567890)");
        }
    }

    private void validateMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message is required");
        }

        if (message.length() > properties.getMaxMessageLength()) {
            throw new IllegalArgumentException(
                    String.format("Message length %d exceeds maximum %d characters",
                            message.length(), properties.getMaxMessageLength()));
        }
    }

    private void validateSenderId(String senderId) {
        if (senderId == null || senderId.isBlank()) {
            throw new IllegalArgumentException("Sender ID is required");
        }

        if (senderId.length() > 11) {
            throw new IllegalArgumentException("Sender ID must be 11 characters or less");
        }

        if (!senderId.matches("^[A-Za-z0-9 ]+$")) {
            throw new IllegalArgumentException("Sender ID must contain only A-Z, a-z, 0-9, and spaces");
        }
    }

    // Utility methods

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "***" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
