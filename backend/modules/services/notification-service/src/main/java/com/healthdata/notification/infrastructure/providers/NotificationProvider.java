package com.healthdata.notification.infrastructure.providers;

import com.healthdata.notification.domain.model.Notification;
import com.healthdata.notification.domain.model.NotificationChannel;

import java.util.Set;

/**
 * Interface for notification delivery providers.
 */
public interface NotificationProvider {

    /**
     * Send a notification.
     */
    void send(Notification notification) throws NotificationDeliveryException;

    /**
     * Get supported channels for this provider.
     */
    Set<NotificationChannel> getSupportedChannels();

    /**
     * Check if the provider is available/configured.
     */
    boolean isAvailable();
}
