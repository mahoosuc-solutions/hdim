package com.healthdata.cql.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for streaming real-time evaluation progress to connected clients.
 *
 * Features:
 * - Multi-tenant session management with tenant filtering
 * - JSON message serialization
 * - Thread-safe session tracking
 * - Automatic cleanup on disconnect
 *
 * Message Format:
 * {
 *   "eventType": "BATCH_PROGRESS",
 *   "data": { ... }
 * }
 */
@Component
public class EvaluationProgressWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(EvaluationProgressWebSocketHandler.class);

    private final ObjectMapper objectMapper;

    // Map: sessionId -> WebSocketSession
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // Map: sessionId -> tenantId (for tenant-based filtering)
    private final Map<String, String> sessionTenants = new ConcurrentHashMap<>();

    public EvaluationProgressWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);

        // Extract tenant ID from query params (e.g., /ws/evaluation-progress?tenantId=TENANT001)
        String tenantId = extractTenantId(session);
        if (tenantId != null) {
            sessionTenants.put(sessionId, tenantId);
            logger.info("WebSocket connection established: sessionId={}, tenantId={}", sessionId, tenantId);
        } else {
            logger.info("WebSocket connection established: sessionId={} (no tenant filter)", sessionId);
        }

        // Send welcome message
        Map<String, Object> welcomeMessage = Map.of(
                "type", "CONNECTION_ESTABLISHED",
                "sessionId", sessionId,
                "tenantId", tenantId != null ? tenantId : "ALL",
                "message", "Connected to evaluation progress stream"
        );
        sendMessage(session, welcomeMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        sessionTenants.remove(sessionId);
        logger.info("WebSocket connection closed: sessionId={}, status={}", sessionId, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Handle incoming messages from client (e.g., subscription filters)
        logger.debug("Received message from client {}: {}", session.getId(), message.getPayload());

        // For now, we don't process client messages, but this could be used for:
        // - Changing tenant filter
        // - Subscribing to specific measure IDs
        // - Adjusting message rate limiting
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }

    /**
     * Broadcast an event to all connected clients (with optional tenant filtering)
     *
     * @param event The event to broadcast
     * @param tenantId The tenant ID for filtering (null = broadcast to all)
     */
    public void broadcastEvent(Object event, String tenantId) {
        Map<String, Object> message = Map.of(
                "type", "EVALUATION_EVENT",
                "data", event,
                "timestamp", System.currentTimeMillis()
        );

        sessions.forEach((sessionId, session) -> {
            // Filter by tenant if both event and session have tenant IDs
            String sessionTenantId = sessionTenants.get(sessionId);
            if (tenantId != null && sessionTenantId != null && !tenantId.equals(sessionTenantId)) {
                return; // Skip this session (different tenant)
            }

            try {
                sendMessage(session, message);
            } catch (Exception e) {
                logger.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
            }
        });
    }

    /**
     * Broadcast a data flow step to connected clients
     *
     * @param step The data flow step to broadcast
     * @param evaluationId The evaluation ID for filtering
     * @param tenantId The tenant ID for filtering (null = broadcast to all)
     */
    public void broadcastDataFlowStep(Object step, String evaluationId, String tenantId) {
        Map<String, Object> message = Map.of(
                "type", "DATA_FLOW_STEP",
                "data", step,
                "evaluationId", evaluationId != null ? evaluationId : "",
                "timestamp", System.currentTimeMillis()
        );

        sessions.forEach((sessionId, session) -> {
            // Filter by tenant if both event and session have tenant IDs
            String sessionTenantId = sessionTenants.get(sessionId);
            if (tenantId != null && sessionTenantId != null && !tenantId.equals(sessionTenantId)) {
                return; // Skip this session (different tenant)
            }

            try {
                sendMessage(session, message);
            } catch (Exception e) {
                logger.error("Failed to send data flow step to session {}: {}", sessionId, e.getMessage());
            }
        });
    }

    /**
     * Send a message to a specific session
     */
    private void sendMessage(WebSocketSession session, Object message) {
        if (session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                logger.error("Failed to send WebSocket message: {}", e.getMessage());
            }
        }
    }

    /**
     * Extract tenant ID from WebSocket session query parameters
     */
    private String extractTenantId(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query != null && query.contains("tenantId=")) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("tenantId=")) {
                        return param.substring("tenantId=".length());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract tenantId from query params: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get count of connected sessions
     */
    public int getConnectionCount() {
        return sessions.size();
    }

    /**
     * Get count of connected sessions for a specific tenant
     */
    public int getConnectionCount(String tenantId) {
        return (int) sessionTenants.values().stream()
                .filter(tid -> tid.equals(tenantId))
                .count();
    }
}
