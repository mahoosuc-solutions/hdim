package com.healthdata.notification.infrastructure.providers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-op SMS provider used when Twilio is not enabled.
 * Logs the attempt and returns a placeholder message ID.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "notification.sms.twilio.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpSmsProvider implements SmsProvider {

    @Override
    public String send(String to, String message) {
        log.warn("SMS sending disabled — no provider configured. Would send to: {}", to);
        return "noop-" + System.currentTimeMillis();
    }

    @Override
    public String sendWithSenderId(String to, String message, String senderId) {
        log.warn("SMS sending disabled — no provider configured. Would send to: {} from: {}", to, senderId);
        return "noop-" + System.currentTimeMillis();
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String getProviderName() {
        return "noop";
    }
}
