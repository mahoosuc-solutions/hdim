package com.healthdata.notification.infrastructure.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time IN_APP notifications.
 *
 * Clients connect to /ws-notifications and subscribe to:
 * - /user/queue/notifications - User-specific notifications
 * - /topic/broadcasts - Tenant-wide broadcasts
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker for subscriptions
        // /topic for broadcasts, /queue for user-specific messages
        config.enableSimpleBroker("/topic", "/queue");

        // Application destination prefix for messages from clients
        config.setApplicationDestinationPrefixes("/app");

        // User destination prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint for STOMP connections
        registry.addEndpoint("/ws-notifications")
            .setAllowedOriginPatterns("*")
            .withSockJS(); // Fallback for browsers without WebSocket support
    }
}
