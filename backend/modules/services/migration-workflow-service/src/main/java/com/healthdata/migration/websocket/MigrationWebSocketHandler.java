package com.healthdata.migration.websocket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * WebSocket handler for migration progress streaming
 */
@Component
@RequiredArgsConstructor
public class MigrationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MigrationWebSocketHandler.class);

    private final MigrationProgressPublisher progressPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Track session -> jobId mapping for cleanup
    private final Map<String, UUID> sessionJobs = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());

        // Extract jobId from URL path (e.g., /api/v1/migrations/{jobId}/stream)
        String path = session.getUri().getPath();
        UUID jobId = extractJobId(path);

        if (jobId != null) {
            sessionJobs.put(session.getId(), jobId);
            progressPublisher.subscribe(jobId, session);
            log.info("Session {} subscribed to job {}", session.getId(), jobId);

            // Send initial acknowledgment
            sendAck(session, jobId);
        } else {
            log.warn("No job ID found in path: {}", path);
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String action = json.has("action") ? json.get("action").asText() : null;

            if ("ping".equals(action)) {
                // Respond to ping
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            } else if ("subscribe".equals(action) && json.has("jobId")) {
                // Allow re-subscription to a different job
                UUID newJobId = UUID.fromString(json.get("jobId").asText());
                UUID oldJobId = sessionJobs.get(session.getId());

                if (oldJobId != null && !oldJobId.equals(newJobId)) {
                    progressPublisher.unsubscribe(oldJobId, session);
                }

                sessionJobs.put(session.getId(), newJobId);
                progressPublisher.subscribe(newJobId, session);
                sendAck(session, newJobId);
            } else if ("unsubscribe".equals(action)) {
                UUID jobId = sessionJobs.remove(session.getId());
                if (jobId != null) {
                    progressPublisher.unsubscribe(jobId, session);
                }
            }
        } catch (Exception e) {
            log.warn("Error handling message from session {}: {}", session.getId(), e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {}, status: {}", session.getId(), status);

        UUID jobId = sessionJobs.remove(session.getId());
        if (jobId != null) {
            progressPublisher.unsubscribe(jobId, session);
        }
        progressPublisher.unsubscribeAll(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}",
                session.getId(), exception.getMessage());

        UUID jobId = sessionJobs.remove(session.getId());
        if (jobId != null) {
            progressPublisher.unsubscribe(jobId, session);
        }
    }

    private UUID extractJobId(String path) {
        // Path format: /api/v1/migrations/{jobId}/stream
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("migrations".equals(parts[i]) && i + 1 < parts.length) {
                try {
                    return UUID.fromString(parts[i + 1]);
                } catch (IllegalArgumentException e) {
                    // Not a valid UUID
                }
            }
        }
        return null;
    }

    private void sendAck(WebSocketSession session, UUID jobId) throws Exception {
        String ack = objectMapper.writeValueAsString(Map.of(
                "type", "subscribed",
                "jobId", jobId.toString(),
                "message", "Successfully subscribed to progress updates"
        ));
        session.sendMessage(new TextMessage(ack));
    }
}
