package com.healthdata.notification.infrastructure.providers;

/**
 * Interface for email sending providers.
 */
public interface EmailProvider {

    /**
     * Send an email.
     *
     * @param to      Recipient email address
     * @param subject Email subject
     * @param body    Email body (HTML supported)
     * @return External message ID from the provider
     */
    String send(String to, String subject, String body);

    /**
     * Send an email with HTML content.
     *
     * @param to       Recipient email address
     * @param subject  Email subject
     * @param htmlBody HTML email body
     * @param textBody Plain text fallback
     * @return External message ID from the provider
     */
    String sendHtml(String to, String subject, String htmlBody, String textBody);

    /**
     * Check if the provider is available.
     */
    boolean isAvailable();
}
