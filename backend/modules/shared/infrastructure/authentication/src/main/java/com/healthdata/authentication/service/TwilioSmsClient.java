package com.healthdata.authentication.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Twilio API client for sending SMS messages.
 *
 * Configuration:
 * - twilio.account-sid: Twilio Account SID
 * - twilio.auth-token: Twilio Auth Token
 * - twilio.from-phone-number: Twilio phone number to send from
 *
 * Environment Variables:
 * - TWILIO_ACCOUNT_SID
 * - TWILIO_AUTH_TOKEN
 * - TWILIO_FROM_PHONE_NUMBER
 */
@Component
@Slf4j
public class TwilioSmsClient {

    @Value("${twilio.account-sid:${TWILIO_ACCOUNT_SID:}}")
    private String accountSid;

    @Value("${twilio.auth-token:${TWILIO_AUTH_TOKEN:}}")
    private String authToken;

    @Value("${twilio.from-phone-number:${TWILIO_FROM_PHONE_NUMBER:}}")
    private String fromPhoneNumber;

    @Value("${twilio.enabled:false}")
    private boolean twilioEnabled;

    @PostConstruct
    public void init() {
        if (twilioEnabled && accountSid != null && !accountSid.isBlank() && authToken != null && !authToken.isBlank()) {
            try {
                Twilio.init(accountSid, authToken);
                log.info("Twilio SMS client initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize Twilio client", e);
                throw new IllegalStateException("Failed to initialize Twilio client", e);
            }
        } else {
            log.warn("Twilio SMS client disabled or not configured. Set twilio.enabled=true and provide credentials.");
        }
    }

    /**
     * Send SMS message via Twilio.
     *
     * @param toPhoneNumber Destination phone number (E.164 format: +15555551234)
     * @param messageBody Message text to send
     * @throws IllegalStateException if Twilio is not enabled or configured
     * @throws RuntimeException if SMS send fails
     */
    public void sendSms(String toPhoneNumber, String messageBody) {
        if (!twilioEnabled) {
            log.warn("Twilio disabled. SMS not sent to: {}", maskPhoneNumber(toPhoneNumber));
            log.info("Message content: {}", messageBody); // Log for testing
            return;
        }

        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            throw new IllegalStateException("Twilio credentials not configured");
        }

        if (fromPhoneNumber == null || fromPhoneNumber.isBlank()) {
            throw new IllegalStateException("Twilio from phone number not configured");
        }

        try {
            Message message = Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(fromPhoneNumber),
                messageBody
            ).create();

            log.info("SMS sent successfully. SID: {}, To: {}, Status: {}",
                message.getSid(),
                maskPhoneNumber(toPhoneNumber),
                message.getStatus());

        } catch (Exception e) {
            log.error("Failed to send SMS to: {}", maskPhoneNumber(toPhoneNumber), e);
            throw new RuntimeException("Failed to send SMS: " + e.getMessage(), e);
        }
    }

    /**
     * Mask phone number for logging (show last 4 digits only).
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
