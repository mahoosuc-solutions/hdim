package com.healthdata.clinicalworkflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time vital sign alerts.
 *
 * Enables STOMP protocol over WebSocket for bidirectional communication between
 * clinical-workflow-service and Angular frontend.
 *
 * Architecture:
 * - Endpoint: /ws (SockJS-enabled for browser compatibility)
 * - Message Broker: /topic (for publish-subscribe pattern)
 * - Application Prefix: /app (for client-to-server messages)
 *
 * Topic Structure:
 * - /topic/vitals-alerts/{providerId} - Provider-specific vital sign alerts
 * - /topic/vitals-alerts/critical - Broadcast critical alerts to all providers
 *
 * Security:
 * - Uses Spring Security authentication from HTTP session
 * - Tenant isolation enforced via providerId routing
 * - CORS configured for frontend domain
 *
 * Issue: #288 - Real-time vital sign alerts via WebSocket
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Register STOMP endpoints for WebSocket connection.
     *
     * SockJS fallback ensures compatibility with browsers that don't support WebSocket.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:4200", "https://*.healthdata.com")
                .withSockJS();  // Enable SockJS fallback for older browsers
    }

    /**
     * Configure message broker for pub-sub messaging.
     *
     * - /topic: Simple in-memory broker for real-time alerts
     * - /app: Application destination prefix for client-to-server messages
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory message broker for /topic destinations
        registry.enableSimpleBroker("/topic");

        // Set prefix for messages bound for @MessageMapping-annotated methods
        registry.setApplicationDestinationPrefixes("/app");
    }
}
