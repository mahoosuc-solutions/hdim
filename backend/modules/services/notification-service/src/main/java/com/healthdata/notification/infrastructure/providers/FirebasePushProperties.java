package com.healthdata.notification.infrastructure.providers;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Firebase Cloud Messaging push provider.
 */
@Data
@Component
@ConfigurationProperties(prefix = "notification.push.firebase")
public class FirebasePushProperties {

    /**
     * Enable/disable Firebase push provider.
     */
    private boolean enabled = false;

    /**
     * Firebase project ID.
     */
    private String projectId;

    /**
     * Firebase service account credentials JSON.
     * Can be the full JSON string or a path to file.
     */
    private String credentialsJson;

    /**
     * Android notification icon resource name.
     */
    private String androidIcon = "ic_notification";

    /**
     * Android notification color (hex format).
     */
    private String androidColor = "#1976D2";

    /**
     * Android notification channel ID.
     */
    private String androidChannelId = "healthdata_notifications";

    /**
     * Default TTL for notifications in seconds.
     */
    private int defaultTtlSeconds = 86400; // 24 hours

    /**
     * Enable dry run mode (for testing).
     */
    private boolean dryRun = false;
}
