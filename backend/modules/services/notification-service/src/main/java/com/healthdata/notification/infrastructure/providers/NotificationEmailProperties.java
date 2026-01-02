package com.healthdata.notification.infrastructure.providers;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Email configuration properties.
 */
@Component
@ConfigurationProperties(prefix = "notification.email")
@Data
public class NotificationEmailProperties {

    /**
     * From address for outgoing emails.
     */
    private String from = "notifications@healthdatainmotion.com";

    /**
     * Reply-to address (optional).
     */
    private String replyTo;

    /**
     * Email provider type (smtp, ses).
     */
    private String provider = "smtp";

    /**
     * Whether to enable email sending.
     */
    private boolean enabled = true;
}
