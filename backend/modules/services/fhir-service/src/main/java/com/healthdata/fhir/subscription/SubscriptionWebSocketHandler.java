package com.healthdata.fhir.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * WebSocket handler for FHIR Subscription notifications.
 *
 * Manages WebSocket connections and broadcasts subscription
 * notifications to connected clients.
 *
 * Connection URL: /ws/subscriptions?tenant={tenantId}&token={accessToken}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    // Map of tenant ID to connected sessions
    private final Map<String, Set<WebSocketSession>> tenantSessions = new ConcurrentHashMap<>();

    // Map of session ID to tenant ID
    private final Map<String, String> sessionTenants = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String tenantId = extractTenantId(session);
        if (tenantId == null) {
            log.warn("WebSocket connection without tenant ID, closing");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // Add session to tenant's session set
        tenantSessions.computeIfAbsent(tenantId, k -> new CopyOnWriteArraySet<>()).add(session);
        sessionTenants.put(session.getId(), tenantId);

        log.info("WebSocket connection established: session={}, tenant={}", session.getId(), tenantId);

        // Send connection acknowledgment
        sendMessage(session, createConnectionAck(tenantId));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String tenantId = sessionTenants.remove(session.getId());
        if (tenantId != null) {
            Set<WebSocketSession> sessions = tenantSessions.get(tenantId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    tenantSessions.remove(tenantId);
                }
            }
        }

        log.info("WebSocket connection closed: session={}, status={}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String tenantId = sessionTenants.get(session.getId());
        if (tenantId == null) {
            return;
        }

        try {
            WebSocketCommand command = objectMapper.readValue(message.getPayload(), WebSocketCommand.class);
            handleCommand(session, tenantId, command);
        } catch (Exception e) {
            log.warn("Failed to parse WebSocket message: {}", e.getMessage());
            sendError(session, "Invalid message format");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
    }

    /**
     * Send notification to all sessions for a tenant.
     */
    public void sendNotification(String tenantId, SubscriptionNotification notification) {
        Set<WebSocketSession> sessions = tenantSessions.get(tenantId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("No WebSocket sessions for tenant: {}", tenantId);
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(notification);
            TextMessage message = new TextMessage(payload);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    sendMessage(session, message);
                }
            }

            log.debug("Sent notification to {} sessions for tenant {}", sessions.size(), tenantId);

        } catch (Exception e) {
            log.error("Failed to serialize notification: {}", e.getMessage());
        }
    }

    /**
     * Send notification to specific subscription's sessions.
     */
    public void sendNotificationToSubscription(String tenantId, String subscriptionId,
            SubscriptionNotification notification) {
        // In a more sophisticated implementation, track which sessions
        // are subscribed to which subscriptions
        sendNotification(tenantId, notification);
    }

    /**
     * Broadcast a message to all connected sessions.
     */
    public void broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);
        tenantSessions.values().stream()
            .flatMap(Set::stream)
            .filter(WebSocketSession::isOpen)
            .forEach(session -> sendMessage(session, textMessage));
    }

    /**
     * Get count of connected sessions for a tenant.
     */
    public int getSessionCount(String tenantId) {
        Set<WebSocketSession> sessions = tenantSessions.get(tenantId);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * Get total connected session count.
     */
    public int getTotalSessionCount() {
        return tenantSessions.values().stream()
            .mapToInt(Set::size)
            .sum();
    }

    /**
     * Handle incoming command from client.
     */
    private void handleCommand(WebSocketSession session, String tenantId, WebSocketCommand command) {
        switch (command.getType()) {
            case "ping" -> sendPong(session);
            case "subscribe" -> handleSubscribe(session, tenantId, command);
            case "unsubscribe" -> handleUnsubscribe(session, tenantId, command);
            default -> sendError(session, "Unknown command: " + command.getType());
        }
    }

    /**
     * Handle subscribe command.
     */
    private void handleSubscribe(WebSocketSession session, String tenantId, WebSocketCommand command) {
        // Client can register interest in specific subscription IDs
        // For now, all clients for a tenant receive all notifications
        log.debug("Subscribe command from session {}: {}", session.getId(), command.getPayload());
        sendMessage(session, new TextMessage("{\"type\":\"subscribed\",\"success\":true}"));
    }

    /**
     * Handle unsubscribe command.
     */
    private void handleUnsubscribe(WebSocketSession session, String tenantId, WebSocketCommand command) {
        log.debug("Unsubscribe command from session {}: {}", session.getId(), command.getPayload());
        sendMessage(session, new TextMessage("{\"type\":\"unsubscribed\",\"success\":true}"));
    }

    /**
     * Send pong response.
     */
    private void sendPong(WebSocketSession session) {
        sendMessage(session, new TextMessage("{\"type\":\"pong\",\"timestamp\":" + System.currentTimeMillis() + "}"));
    }

    /**
     * Send error message.
     */
    private void sendError(WebSocketSession session, String error) {
        sendMessage(session, new TextMessage("{\"type\":\"error\",\"message\":\"" + error + "\"}"));
    }

    /**
     * Create connection acknowledgment message.
     */
    private TextMessage createConnectionAck(String tenantId) {
        return new TextMessage(String.format(
            "{\"type\":\"connected\",\"tenantId\":\"%s\",\"timestamp\":%d}",
            tenantId, System.currentTimeMillis()
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
     * Extract tenant ID from WebSocket session.
     */
    private String extractTenantId(WebSocketSession session) {
        // Try query parameter first
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] parts = param.split("=");
                if (parts.length == 2 && "tenant".equals(parts[0])) {
                    return parts[1];
                }
            }
        }

        // Try header
        String tenantHeader = session.getHandshakeHeaders().getFirst("X-Tenant-ID");
        if (tenantHeader != null) {
            return tenantHeader;
        }

        return null;
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
