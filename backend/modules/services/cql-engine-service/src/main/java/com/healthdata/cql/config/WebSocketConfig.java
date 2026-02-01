package com.healthdata.cql.config;

import com.healthdata.cql.websocket.EvaluationProgressWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.List;

/**
 * WebSocket configuration for real-time evaluation progress streaming.
 *
 * Endpoint: /ws/evaluation-progress
 * Protocol: WebSocket with JSON message format
 * CORS: Configured via application properties
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final EvaluationProgressWebSocketHandler webSocketHandler;

    @Value("${visualization.websocket.enabled:true}")
    private boolean websocketEnabled;

    @Value("#{'${visualization.websocket.allowed-origins:http://localhost:3001,http://localhost:4200,http://localhost:4201,http://localhost:4202,http://localhost:3000,http://localhost:8082}'.split(',')}")
    private List<String> allowedOrigins;

    public WebSocketConfig(EvaluationProgressWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        if (websocketEnabled) {
            registry.addHandler(webSocketHandler, "/ws/evaluation-progress")
                    .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
                    .addInterceptors(new HttpSessionHandshakeInterceptor());
        }
    }
}
