package com.healthdata.notification.infrastructure.providers;

import com.healthdata.notification.domain.model.Notification;
import com.healthdata.notification.domain.model.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * In-app notification provider.
 * Stores notifications for retrieval by clients.
 */
@Component
@Slf4j
public class InAppNotificationProvider implements NotificationProvider {

    @Override
    public void send(Notification notification) throws NotificationDeliveryException {
        // In-app notifications are stored in the database and retrieved by clients
        // The notification is already saved, so this is essentially a no-op
        log.info("In-app notification created for user {}: {}", 
            notification.getRecipientId(), notification.getId());
    }

    @Override
    public Set<NotificationChannel> getSupportedChannels() {
        return Set.of(NotificationChannel.IN_APP);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
