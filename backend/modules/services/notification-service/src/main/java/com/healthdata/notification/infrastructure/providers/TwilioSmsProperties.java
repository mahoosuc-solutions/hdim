package com.healthdata.notification.infrastructure.providers;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Twilio SMS provider.
 */
@Data
@Component
@ConfigurationProperties(prefix = "notification.sms.twilio")
public class TwilioSmsProperties {

    /**
     * Enable/disable Twilio SMS provider.
     */
    private boolean enabled = false;

    /**
     * Twilio Account SID.
     */
    private String accountSid;

    /**
     * Twilio Auth Token.
     */
    private String authToken;

    /**
     * Default "From" phone number (E.164 format).
     */
    private String fromNumber;

    /**
     * Optional Messaging Service SID for advanced features.
     */
    private String messagingServiceSid;

    /**
     * Default sender ID for alphanumeric sender (max 11 chars).
     */
    private String defaultSenderId = "HealthData";

    /**
     * Maximum message length before truncation warning.
     */
    private int maxMessageLength = 1600;
}
