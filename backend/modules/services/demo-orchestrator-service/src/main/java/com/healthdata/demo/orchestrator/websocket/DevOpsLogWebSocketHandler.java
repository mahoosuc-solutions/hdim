package com.healthdata.demo.orchestrator.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.demo.orchestrator.model.DevOpsLogMessage;
import com.healthdata.demo.orchestrator.model.DevOpsStatusUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket handler for DevOps agent log streaming.
 *
 * Manages WebSocket connections and broadcasts deployment logs,
 * data seeding progress, clearing operations, and FHIR validation
 * results to connected clients in real-time.
 *
 * Connection URL: /ws/devops/logs?tenant={tenantId}&level={logLevel}
 *
 * Features:
 * - Log level filtering (DEBUG, INFO, WARN, ERROR)
 * - Tenant-based isolation
 * - Ping/pong keepalive
 * - Automatic reconnection support
 *
 * ★ Insight ─────────────────────────────────────
 * - Session management: ConcurrentHashMap + CopyOnWriteArraySet for thread-safe multi-client broadcasting
 * - Log filtering: Client-side level filtering reduces bandwidth
 * - Tenant isolation: Each tenant only receives their own logs
 * - Error resilience: Failed sends don't break other sessions
 * ─────────────────────────────────────────────────
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DevOpsLogWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    // Map of tenant ID to connected sessions
    private final Map<String, Set<WebSocketSession>> tenantSessions = new ConcurrentHashMap<>();

    // Map of session ID to session metadata
    private final Map<String, SessionMetadata> sessionMetadata = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        SessionMetadata metadata = extractSessionMetadata(session);
        if (metadata.tenantId == null) {
            log.warn("WebSocket connection without tenant ID, closing: session={}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // Add session to tenant's session set
        tenantSessions.computeIfAbsent(metadata.tenantId, k -> new CopyOnWriteArraySet<>()).add(session);
        sessionMetadata.put(session.getId(), metadata);

        log.info("WebSocket connection established: session={}, tenant={}, level={}",
            session.getId(), metadata.tenantId, metadata.logLevel);

        // Send connection acknowledgment
        sendMessage(session, createConnectionAck(metadata.tenantId, metadata.logLevel));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SessionMetadata metadata = sessionMetadata.remove(session.getId());
        if (metadata != null && metadata.tenantId != null) {
            Set<WebSocketSession> sessions = tenantSessions.get(metadata.tenantId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    tenantSessions.remove(metadata.tenantId);
                }
            }
        }

        log.info("WebSocket connection closed: session={}, status={}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SessionMetadata metadata = sessionMetadata.get(session.getId());
        if (metadata == null) {
            return;
        }

        try {
            WebSocketCommand command = objectMapper.readValue(message.getPayload(), WebSocketCommand.class);
            handleCommand(session, metadata, command);
        } catch (Exception e) {
            log.warn("Failed to parse WebSocket message from session {}: {}", session.getId(), e.getMessage());
            sendError(session, "Invalid message format");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
    }

    /**
     * Publish log message to all subscribed sessions for a tenant.
     *
     * @param logMessage Log message to publish
     */
    public void publishLog(DevOpsLogMessage logMessage) {
        if (logMessage.getTenantId() == null) {
            log.warn("Cannot publish log without tenant ID: {}", logMessage.getMessage());
            return;
        }

        Set<WebSocketSession> sessions = tenantSessions.get(logMessage.getTenantId());
        if (sessions == null || sessions.isEmpty()) {
            log.debug("No WebSocket sessions for tenant: {}", logMessage.getTenantId());
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "type", "log",
                "data", logMessage
            ));
            TextMessage message = new TextMessage(payload);

            int sentCount = 0;
            for (WebSocketSession session : sessions) {
                if (session.isOpen() && shouldSendLog(session, logMessage)) {
                    sendMessage(session, message);
                    sentCount++;
                }
            }

            log.trace("Published log to {} sessions for tenant {}: level={}, category={}",
                sentCount, logMessage.getTenantId(), logMessage.getLevel(), logMessage.getCategory());

        } catch (Exception e) {
            log.error("Failed to serialize log message: {}", e.getMessage());
        }
    }

    /**
     * Publish status update to all subscribed sessions for a tenant.
     *
     * @param statusUpdate Status update to publish
     */
    public void publishStatus(DevOpsStatusUpdate statusUpdate) {
        if (statusUpdate.getTenantId() == null) {
            log.warn("Cannot publish status without tenant ID: component={}", statusUpdate.getComponent());
            return;
        }

        Set<WebSocketSession> sessions = tenantSessions.get(statusUpdate.getTenantId());
        if (sessions == null || sessions.isEmpty()) {
            log.debug("No WebSocket sessions for tenant: {}", statusUpdate.getTenantId());
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "type", "status",
                "data", statusUpdate
            ));
            TextMessage message = new TextMessage(payload);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    sendMessage(session, message);
                }
            }

            log.debug("Published status update to {} sessions for tenant {}: component={}, status={}",
                sessions.size(), statusUpdate.getTenantId(), statusUpdate.getComponent(), statusUpdate.getStatus());

        } catch (Exception e) {
            log.error("Failed to serialize status update: {}", e.getMessage());
        }
    }

    /**
     * Get count of connected sessions for a tenant.
     */
    public int getSessionCount(String tenantId) {
        Set<WebSocketSession> sessions = tenantSessions.get(tenantId);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * Get total connected session count across all tenants.
     */
    public int getTotalSessionCount() {
        return tenantSessions.values().stream()
            .mapToInt(Set::size)
            .sum();
    }

    /**
     * Check if log should be sent to session based on level filtering.
     */
    private boolean shouldSendLog(WebSocketSession session, DevOpsLogMessage logMessage) {
        SessionMetadata metadata = sessionMetadata.get(session.getId());
        if (metadata == null || metadata.logLevel == null) {
            return true; // No filter, send all
        }

        LogLevel sessionLevel = metadata.logLevel;
        LogLevel messageLevel = LogLevel.fromString(logMessage.getLevel());

        // Send if message level >= session level
        return messageLevel.ordinal() >= sessionLevel.ordinal();
    }

    /**
     * Handle incoming command from client.
     */
    private void handleCommand(WebSocketSession session, SessionMetadata metadata, WebSocketCommand command) {
        switch (command.getType()) {
            case "ping" -> sendPong(session);
            case "setLevel" -> handleSetLevel(session, metadata, command);
            case "subscribe" -> handleSubscribe(session, metadata, command);
            default -> sendError(session, "Unknown command: " + command.getType());
        }
    }

    /**
     * Handle setLevel command to change log level filter.
     */
    private void handleSetLevel(WebSocketSession session, SessionMetadata metadata, WebSocketCommand command) {
        try {
            String levelStr = command.getPayload().toString();
            LogLevel newLevel = LogLevel.fromString(levelStr);
            metadata.logLevel = newLevel;

            log.debug("Session {} changed log level to {}", session.getId(), newLevel);
            sendMessage(session, new TextMessage(String.format(
                "{\"type\":\"levelChanged\",\"level\":\"%s\",\"success\":true}", newLevel
            )));
        } catch (Exception e) {
            sendError(session, "Invalid log level: " + command.getPayload());
        }
    }

    /**
     * Handle subscribe command.
     */
    private void handleSubscribe(WebSocketSession session, SessionMetadata metadata, WebSocketCommand command) {
        log.debug("Subscribe command from session {}: {}", session.getId(), command.getPayload());
        sendMessage(session, new TextMessage("{\"type\":\"subscribed\",\"success\":true}"));
    }

    /**
     * Send pong response.
     */
    private void sendPong(WebSocketSession session) {
        sendMessage(session, new TextMessage(String.format(
            "{\"type\":\"pong\",\"timestamp\":%d}", System.currentTimeMillis()
        )));
    }

    /**
     * Send error message.
     */
    private void sendError(WebSocketSession session, String error) {
        sendMessage(session, new TextMessage(String.format(
            "{\"type\":\"error\",\"message\":\"%s\"}", error.replace("\"", "\\\"")
        )));
    }

    /**
     * Create connection acknowledgment message.
     */
    private TextMessage createConnectionAck(String tenantId, LogLevel level) {
        return new TextMessage(String.format(
            "{\"type\":\"connected\",\"tenantId\":\"%s\",\"level\":\"%s\",\"timestamp\":%d}",
            tenantId, level, System.currentTimeMillis()
        ));
    }

    /**
     * Send message to session with error handling.
     */
    private void sendMessage(WebSocketSession session, TextMessage message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        } catch (IOException e) {
            log.warn("Failed to send WebSocket message to session {}: {}", session.getId(), e.getMessage());
        }
    }

    /**
     * Extract session metadata from WebSocket connection.
     */
    private SessionMetadata extractSessionMetadata(WebSocketSession session) {
        SessionMetadata metadata = new SessionMetadata();

        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] parts = param.split("=");
                if (parts.length == 2) {
                    switch (parts[0]) {
                        case "tenant" -> metadata.tenantId = parts[1];
                        case "level" -> metadata.logLevel = LogLevel.fromString(parts[1]);
                    }
                }
            }
        }

        // Try header for tenant ID if not in query params
        if (metadata.tenantId == null) {
            String tenantHeader = session.getHandshakeHeaders().getFirst("X-Tenant-ID");
            if (tenantHeader != null) {
                metadata.tenantId = tenantHeader;
            }
        }

        // Default log level to INFO
        if (metadata.logLevel == null) {
            metadata.logLevel = LogLevel.INFO;
        }

        return metadata;
    }

    /**
     * Session metadata for filtering and tracking.
     */
    private static class SessionMetadata {
        String tenantId;
        LogLevel logLevel;
    }

    /**
     * Log level enumeration for filtering.
     */
    enum LogLevel {
        DEBUG, INFO, WARN, ERROR;

        static LogLevel fromString(String level) {
            if (level == null) {
                return INFO;
            }
            try {
                return valueOf(level.toUpperCase());
            } catch (IllegalArgumentException e) {
                return INFO;
            }
        }
    }

    /**
     * WebSocket command from client.
     */
    @lombok.Data
    public static class WebSocketCommand {
        private String type;
        private Object payload;
    }
}
