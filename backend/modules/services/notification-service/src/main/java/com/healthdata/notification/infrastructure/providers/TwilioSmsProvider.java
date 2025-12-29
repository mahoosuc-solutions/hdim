package com.healthdata.notification.infrastructure.providers;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Twilio SMS provider implementation.
 *
 * Supports:
 * - Standard SMS messaging
 * - Alphanumeric sender IDs (where supported by carrier)
 * - Automatic retry with circuit breaker
 * - Message status tracking via SID
 */
@Component
@ConditionalOnProperty(name = "notification.sms.provider", havingValue = "twilio")
@RequiredArgsConstructor
@Slf4j
public class TwilioSmsProvider implements SmsProvider {

    private final TwilioSmsProperties properties;

    private volatile boolean initialized = false;

    @PostConstruct
    public void init() {
        if (properties.getAccountSid() != null && !properties.getAccountSid().isEmpty()
                && properties.getAuthToken() != null && !properties.getAuthToken().isEmpty()) {
            try {
                Twilio.init(properties.getAccountSid(), properties.getAuthToken());
                initialized = true;
                log.info("Twilio SMS provider initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize Twilio: {}", e.getMessage());
                initialized = false;
            }
        } else {
            log.warn("Twilio credentials not configured, SMS provider disabled");
        }
    }

    @Override
    @Retry(name = "twilio-sms")
    @CircuitBreaker(name = "twilio-sms", fallbackMethod = "sendFallback")
    public String send(String to, String message) {
        if (!isAvailable()) {
            throw new SmsProviderException("Twilio SMS provider not available");
        }

        try {
            log.debug("Sending SMS to {} via Twilio", maskPhoneNumber(to));

            Message twilioMessage = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(properties.getFromNumber()),
                    message
            ).create();

            String sid = twilioMessage.getSid();
            log.info("SMS sent successfully, SID: {}", sid);

            return sid;

        } catch (ApiException e) {
            log.error("Twilio API error: {} - {}", e.getCode(), e.getMessage());
            throw new SmsProviderException("Twilio API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to send SMS: {}", e.getMessage());
            throw new SmsProviderException("Failed to send SMS: " + e.getMessage(), e);
        }
    }

    @Override
    @Retry(name = "twilio-sms")
    @CircuitBreaker(name = "twilio-sms", fallbackMethod = "sendWithSenderIdFallback")
    public String sendWithSenderId(String to, String message, String senderId) {
        if (!isAvailable()) {
            throw new SmsProviderException("Twilio SMS provider not available");
        }

        try {
            log.debug("Sending SMS to {} with sender ID {} via Twilio", maskPhoneNumber(to), senderId);

            Message twilioMessage;

            // Use messaging service SID if configured, otherwise use alphanumeric sender ID
            if (properties.getMessagingServiceSid() != null && !properties.getMessagingServiceSid().isEmpty()) {
                twilioMessage = Message.creator(
                        new PhoneNumber(to),
                        properties.getMessagingServiceSid(),
                        message
                ).create();
            } else {
                // Alphanumeric sender ID (not supported in all countries, e.g., US)
                twilioMessage = Message.creator(
                        new PhoneNumber(to),
                        senderId,
                        message
                ).create();
            }

            String sid = twilioMessage.getSid();
            log.info("SMS sent successfully with sender ID, SID: {}", sid);

            return sid;

        } catch (ApiException e) {
            log.error("Twilio API error: {} - {}", e.getCode(), e.getMessage());
            throw new SmsProviderException("Twilio API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to send SMS with sender ID: {}", e.getMessage());
            throw new SmsProviderException("Failed to send SMS: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return initialized && properties.isEnabled();
    }

    @Override
    public String getProviderName() {
        return "twilio";
    }

    /**
     * Fallback method for circuit breaker.
     */
    public String sendFallback(String to, String message, Throwable t) {
        log.warn("SMS send fallback triggered for {}: {}", maskPhoneNumber(to), t.getMessage());
        throw new SmsProviderException("SMS service temporarily unavailable: " + t.getMessage(), t);
    }

    /**
     * Fallback method for circuit breaker.
     */
    public String sendWithSenderIdFallback(String to, String message, String senderId, Throwable t) {
        log.warn("SMS send fallback triggered for {} with sender {}: {}",
            maskPhoneNumber(to), senderId, t.getMessage());
        throw new SmsProviderException("SMS service temporarily unavailable: " + t.getMessage(), t);
    }

    /**
     * Mask phone number for logging (show last 4 digits).
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    /**
     * Exception for SMS provider errors.
     */
    public static class SmsProviderException extends RuntimeException {
        public SmsProviderException(String message) {
            super(message);
        }

        public SmsProviderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
