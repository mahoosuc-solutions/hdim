package com.healthdata.demo.orchestrator.config;

import com.healthdata.demo.orchestrator.websocket.DevOpsLogWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for DevOps agent log streaming.
 *
 * Endpoints:
 * - /ws/devops/logs - Real-time deployment and operation logs
 *
 * Connection:
 * ws://localhost:8090/api/v1/ws/devops/logs?tenant={tenantId}&level={logLevel}
 *
 * Log Levels: DEBUG, INFO, WARN, ERROR (defaults to INFO)
 *
 * CORS Configuration:
 * Set hdim.websocket.allowed-origins in application.yml to restrict origins.
 * Default: http://localhost:4200 (Angular dev server)
 * Production example: hdim.websocket.allowed-origins=https://portal.hdim.health,https://admin.hdim.health
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final DevOpsLogWebSocketHandler devOpsLogWebSocketHandler;

    /**
     * Comma-separated list of allowed origins for WebSocket connections.
     * Configure in application.yml for production security.
     */
    @Value("${hdim.websocket.allowed-origins:http://localhost:4200,http://localhost:4201}")
    private String allowedOriginsConfig;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] allowedOrigins = allowedOriginsConfig.split(",");
        registry.addHandler(devOpsLogWebSocketHandler, "/ws/devops/logs")
            .setAllowedOrigins(allowedOrigins);
    }
}
