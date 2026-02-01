package com.healthdata.demo.orchestrator.config;

import com.healthdata.demo.orchestrator.websocket.DevOpsLogWebSocketHandler;
import lombok.RequiredArgsConstructor;
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
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final DevOpsLogWebSocketHandler devOpsLogWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(devOpsLogWebSocketHandler, "/ws/devops/logs")
            .setAllowedOrigins("*"); // TODO: Configure for production (use environment-specific origins)
    }
}
