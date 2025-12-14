package com.healthdata.migration.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.healthdata.migration.websocket.MigrationWebSocketHandler;

import lombok.RequiredArgsConstructor;

/**
 * WebSocket configuration for migration progress streaming
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final MigrationWebSocketHandler migrationWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(migrationWebSocketHandler, "/api/v1/migrations/*/stream")
                .setAllowedOrigins("*");
    }
}
