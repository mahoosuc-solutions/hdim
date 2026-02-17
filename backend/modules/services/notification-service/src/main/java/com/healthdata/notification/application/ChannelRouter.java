package com.healthdata.notification.application;

import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.infrastructure.providers.NotificationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes notifications to the appropriate provider based on channel.
 */
@Component
@Slf4j
public class ChannelRouter {

    private final Map<NotificationChannel, NotificationProvider> providers = new HashMap<>();

    public ChannelRouter(List<NotificationProvider> notificationProviders) {
        for (NotificationProvider provider : notificationProviders) {
            for (NotificationChannel channel : provider.getSupportedChannels()) {
                providers.put(channel, provider);
                log.info("Registered {} provider for channel {}", 
                    provider.getClass().getSimpleName(), channel);
            }
        }
    }

    /**
     * Get the provider for a specific channel.
     */
    public NotificationProvider getProvider(NotificationChannel channel) {
        NotificationProvider provider = providers.get(channel);
        if (provider == null) {
            log.warn("No provider registered for notification channel: {}. Skipping.", channel);
            return null;
        }
        return provider;
    }

    /**
     * Check if a channel is supported.
     */
    public boolean isChannelSupported(NotificationChannel channel) {
        return providers.containsKey(channel);
    }
}
