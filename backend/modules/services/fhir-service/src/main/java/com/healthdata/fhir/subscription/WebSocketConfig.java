package com.healthdata.fhir.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for FHIR Subscription notifications.
 *
 * Endpoints:
 * - /ws/subscriptions - Real-time subscription notifications
 *
 * Connection:
 * ws://localhost:8085/fhir/ws/subscriptions?tenant={tenantId}
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final SubscriptionWebSocketHandler subscriptionWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(subscriptionWebSocketHandler, "/ws/subscriptions")
            .setAllowedOrigins("*"); // Configure appropriately for production
    }
}
