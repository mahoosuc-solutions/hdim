package com.healthdata.hedisadapter.websocket;

import com.healthdata.hedisadapter.config.HedisProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@ConditionalOnProperty(name = "external.hedis.enabled", havingValue = "true")
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final KafkaToWebSocketBridge kafkaToWebSocketBridge;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(kafkaToWebSocketBridge, "/ws/events")
                .setAllowedOrigins("*"); // tightened per-deployment via CORS config
    }
}
