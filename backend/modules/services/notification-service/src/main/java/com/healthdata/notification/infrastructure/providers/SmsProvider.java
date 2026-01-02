package com.healthdata.notification.infrastructure.providers;

/**
 * Interface for SMS sending providers.
 */
public interface SmsProvider {

    /**
     * Send an SMS message.
     *
     * @param to      Recipient phone number (E.164 format, e.g., +1234567890)
     * @param message SMS message body (max 1600 characters for concatenated SMS)
     * @return External message ID from the provider
     */
    String send(String to, String message);

    /**
     * Send an SMS with a sender ID.
     *
     * @param to       Recipient phone number
     * @param message  SMS message body
     * @param senderId Alphanumeric sender ID (max 11 characters)
     * @return External message ID from the provider
     */
    String sendWithSenderId(String to, String message, String senderId);

    /**
     * Check if the provider is available.
     */
    boolean isAvailable();

    /**
     * Get the provider name.
     */
    String getProviderName();
}
