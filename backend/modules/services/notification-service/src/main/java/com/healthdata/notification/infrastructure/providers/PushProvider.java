package com.healthdata.notification.infrastructure.providers;

import java.util.Map;

/**
 * Interface for push notification providers.
 */
public interface PushProvider {

    /**
     * Send a push notification to a device.
     *
     * @param deviceToken FCM/APNs device token
     * @param title       Notification title
     * @param body        Notification body
     * @return External message ID from the provider
     */
    String send(String deviceToken, String title, String body);

    /**
     * Send a push notification with custom data payload.
     *
     * @param deviceToken FCM/APNs device token
     * @param title       Notification title
     * @param body        Notification body
     * @param data        Custom data payload
     * @return External message ID from the provider
     */
    String sendWithData(String deviceToken, String title, String body, Map<String, String> data);

    /**
     * Send a push notification to a topic.
     *
     * @param topic Topic name (e.g., "clinical-alerts")
     * @param title Notification title
     * @param body  Notification body
     * @return External message ID from the provider
     */
    String sendToTopic(String topic, String title, String body);

    /**
     * Send a silent/data-only push notification.
     *
     * @param deviceToken FCM/APNs device token
     * @param data        Data payload
     * @return External message ID from the provider
     */
    String sendSilent(String deviceToken, Map<String, String> data);

    /**
     * Check if the provider is available.
     */
    boolean isAvailable();

    /**
     * Get the provider name.
     */
    String getProviderName();
}
