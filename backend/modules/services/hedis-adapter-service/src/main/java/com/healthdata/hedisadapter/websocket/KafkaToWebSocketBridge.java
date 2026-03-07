package com.healthdata.hedisadapter.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges HDIM Kafka events to WebSocket connections for the hedis
 * Next.js dashboard. Supports real-time streaming of patient events,
 * care gap updates, and quality measure results.
 */
@Component
@ConditionalOnProperty(name = "external.hedis.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class KafkaToWebSocketBridge extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket client connected, total sessions={}", sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket client disconnected, total sessions={}", sessions.size());
    }

    @KafkaListener(
            topics = {
                    "external.hdim.caregaps",
                    "external.hdim.quality-measures",
                    "external.hdim.patient-events"
            },
            groupId = "hedis-websocket-bridge"
    )
    public void onHdimEvent(Map<String, Object> event) {
        if (sessions.isEmpty()) return;

        try {
            String message = objectMapper.writeValueAsString(event);
            TextMessage textMessage = new TextMessage(message);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("Failed to send WebSocket message, removing session");
                        sessions.remove(session);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error broadcasting Kafka event to WebSocket: {}", e.getMessage());
        }
    }

    public int getActiveConnectionCount() {
        return sessions.size();
    }
}
