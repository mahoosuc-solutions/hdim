package com.healthdata.quality.websocket;

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
 * HIPAA-Compliant WebSocket Handler for Real-Time Health Score Updates
 *
 * HIPAA Security Rule Compliance:
 * - §164.312(d) - Person or Entity Authentication (enforced by interceptors)
 * - §164.312(a)(1) - Access Control (tenant isolation)
 * - §164.312(b) - Audit Controls (connection tracking and logging)
 * - §164.308(a)(5)(ii)(C) - Log-in Monitoring (session tracking)
 *
 * Security Features:
 * - Authenticated sessions only (validated by JwtWebSocketHandshakeInterceptor)
 * - Multi-tenant isolation (validated by TenantAccessInterceptor)
 * - Connection audit logging (AuditLoggingInterceptor)
 * - Session metadata tracking for security analysis
 * - Automatic cleanup on disconnect with audit trail
 *
 * Message Types:
 * 1. CONNECTION_ESTABLISHED - Welcome message on connect
 * 2. HEALTH_SCORE_UPDATE - Regular health score update
 * 3. SIGNIFICANT_CHANGE - Alert for significant health score changes
 * 4. CLINICAL_ALERT - Clinical alert notification
 *
 * Connection URL format:
 * wss://host/quality-measure/ws/health-scores?tenantId=TENANT001
 * Header: Authorization: Bearer <jwt-token>
 */
@Component
public class HealthScoreWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(HealthScoreWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final AuditLoggingInterceptor auditLoggingInterceptor;
    private final SessionTimeoutManager sessionTimeoutManager;

    // Map: sessionId -> WebSocketSession
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // Map: sessionId -> tenantId (for tenant-based filtering)
    private final Map<String, String> sessionTenants = new ConcurrentHashMap<>();

    // Map: sessionId -> username (for audit logging)
    private final Map<String, String> sessionUsers = new ConcurrentHashMap<>();

    // Map: sessionId -> connection start time (for audit logging)
    private final Map<String, Long> sessionStartTimes = new ConcurrentHashMap<>();

    public HealthScoreWebSocketHandler(
            ObjectMapper objectMapper,
            AuditLoggingInterceptor auditLoggingInterceptor,
            SessionTimeoutManager sessionTimeoutManager) {
        this.objectMapper = objectMapper;
        this.auditLoggingInterceptor = auditLoggingInterceptor;
        this.sessionTimeoutManager = sessionTimeoutManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();

        // Extract security attributes set by interceptors
        Map<String, Object> attributes = session.getAttributes();
        Boolean authenticated = (Boolean) attributes.get("authenticated");
        String username = (String) attributes.get("username");
        String tenantId = (String) attributes.get("tenantId");

        // Security validation - should never happen if interceptors work correctly
        if (authenticated == null || !authenticated) {
            logger.error("SECURITY VIOLATION: Unauthenticated WebSocket connection attempted: sessionId={}", sessionId);
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"));
            return;
        }

        if (tenantId == null || tenantId.isEmpty()) {
            logger.error("SECURITY VIOLATION: WebSocket connection without tenantId: sessionId={}, user={}",
                sessionId, username);
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Tenant ID required"));
            return;
        }

        // Store session metadata
        sessions.put(sessionId, session);
        sessionTenants.put(sessionId, tenantId);
        sessionUsers.put(sessionId, username);
        sessionStartTimes.put(sessionId, System.currentTimeMillis());

        // Register session for timeout monitoring (HIPAA §164.312(a)(2)(iii))
        sessionTimeoutManager.registerSession(sessionId, session);

        logger.info("Health Score WebSocket connection established: sessionId={}, user={}, tenantId={}, timeout={}min",
            sessionId, username, tenantId, sessionTimeoutManager.getSessionTimeoutMinutes());

        // Send welcome message with security context
        Map<String, Object> welcomeMessage = Map.of(
                "type", "CONNECTION_ESTABLISHED",
                "sessionId", sessionId,
                "username", username,
                "tenantId", tenantId,
                "message", "Securely connected to health score real-time stream",
                "sessionTimeoutMinutes", sessionTimeoutManager.getSessionTimeoutMinutes(),
                "timestamp", System.currentTimeMillis(),
                "authenticated", true
        );
        sendMessage(session, welcomeMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();

        // Retrieve session metadata for audit logging
        String username = sessionUsers.remove(sessionId);
        String tenantId = sessionTenants.remove(sessionId);
        Long startTime = sessionStartTimes.remove(sessionId);
        sessions.remove(sessionId);

        // Calculate connection duration for audit trail
        long connectionDuration = 0;
        if (startTime != null) {
            connectionDuration = System.currentTimeMillis() - startTime;
        }

        // Unregister session from timeout monitoring
        sessionTimeoutManager.unregisterSession(sessionId);

        logger.info("Health Score WebSocket connection closed: sessionId={}, user={}, tenantId={}, duration={}ms, status={}",
            sessionId, username, tenantId, connectionDuration, status);

        // Log disconnection event for HIPAA audit trail
        if (username != null && tenantId != null) {
            auditLoggingInterceptor.logDisconnectEvent(sessionId, username, tenantId, connectionDuration);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();

        // Update last activity time for session timeout tracking
        sessionTimeoutManager.updateLastActivity(sessionId);

        logger.debug("Received message from client {}: {}", sessionId, message.getPayload());

        // For now, we don't process client messages, but this could be used for:
        // - Changing tenant filter
        // - Subscribing to specific patient IDs
        // - Adjusting message rate limiting
        // - Ping/pong for connection keepalive (RECOMMENDED: Send periodic pings to prevent timeout)
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }

    /**
     * Broadcast a health score update to connected clients
     *
     * @param healthScoreData The health score data to broadcast
     * @param tenantId The tenant ID for filtering (null = broadcast to all)
     */
    public void broadcastHealthScoreUpdate(Map<String, Object> healthScoreData, String tenantId) {
        Map<String, Object> message = Map.of(
                "type", "HEALTH_SCORE_UPDATE",
                "data", healthScoreData,
                "timestamp", System.currentTimeMillis()
        );

        broadcastToSessions(message, tenantId);
        logger.debug("Broadcasted health score update for patient: {} to {} clients",
                healthScoreData.get("patientId"),
                countMatchingSessions(tenantId));
    }

    /**
     * Broadcast a significant health score change alert to connected clients
     *
     * @param significantChangeData The significant change data to broadcast
     * @param tenantId The tenant ID for filtering (null = broadcast to all)
     */
    public void broadcastSignificantChange(Map<String, Object> significantChangeData, String tenantId) {
        Map<String, Object> message = Map.of(
                "type", "SIGNIFICANT_CHANGE",
                "data", significantChangeData,
                "timestamp", System.currentTimeMillis(),
                "priority", "high"
        );

        broadcastToSessions(message, tenantId);
        logger.info("Broadcasted significant change alert for patient: {} to {} clients",
                significantChangeData.get("patientId"),
                countMatchingSessions(tenantId));
    }

    /**
     * Internal method to broadcast a message to sessions with optional tenant filtering
     */
    private void broadcastToSessions(Map<String, Object> message, String tenantId) {
        sessions.forEach((sessionId, session) -> {
            // Filter by tenant if both message and session have tenant IDs
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
     * Send a message to a specific session
     */
    private void sendMessage(WebSocketSession session, Object message) {
        if (session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                logger.error("Failed to send WebSocket message to session {}: {}",
                        session.getId(), e.getMessage());
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
     * Get count of all connected sessions
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

    /**
     * Count sessions matching the tenant filter
     */
    private int countMatchingSessions(String tenantId) {
        if (tenantId == null) {
            return sessions.size();
        }
        return (int) sessionTenants.values().stream()
                .filter(tid -> tid.equals(tenantId))
                .count();
    }

    /**
     * Broadcast a clinical alert to connected clients
     *
     * @param alert The clinical alert data to broadcast
     * @param tenantId The tenant ID for filtering
     * @return true if message was sent successfully to at least one client
     */
    public boolean broadcastClinicalAlert(Object alert, String tenantId) {
        Map<String, Object> message = Map.of(
                "type", "CLINICAL_ALERT",
                "data", alert,
                "timestamp", System.currentTimeMillis(),
                "priority", "alert"
        );

        int targetSessions = countMatchingSessions(tenantId);
        if (targetSessions == 0) {
            logger.warn("No WebSocket sessions found for tenant: {}", tenantId);
            return false;
        }

        broadcastToSessions(message, tenantId);
        logger.info("Broadcasted clinical alert to {} clients for tenant: {}",
                targetSessions, tenantId);
        return true;
    }

    /**
     * Broadcast a clinical alert to a specific patient channel
     *
     * @param alert The clinical alert data to broadcast
     * @param tenantId The tenant ID
     * @param patientId The patient ID for filtering
     * @return true if message was sent successfully
     */
    public boolean broadcastClinicalAlertToPatient(Object alert, String tenantId, String patientId) {
        Map<String, Object> message = Map.of(
                "type", "CLINICAL_ALERT",
                "data", alert,
                "patientId", patientId,
                "timestamp", System.currentTimeMillis(),
                "priority", "alert"
        );

        // For now, broadcast to all sessions of the tenant
        // In future, could filter by patient-specific subscriptions
        int targetSessions = countMatchingSessions(tenantId);
        if (targetSessions == 0) {
            logger.warn("No WebSocket sessions found for tenant: {} patient: {}", tenantId, patientId);
            return false;
        }

        broadcastToSessions(message, tenantId);
        logger.info("Broadcasted clinical alert to {} clients for tenant: {} patient: {}",
                targetSessions, tenantId, patientId);
        return true;
    }

    /**
     * Broadcast a generic notification to connected clients
     *
     * @param tenantId The tenant ID for filtering
     * @param message The notification message map (pre-formatted)
     * @return true if message was sent successfully to at least one client
     */
    public boolean broadcastGenericNotification(String tenantId, Map<String, Object> message) {
        int targetSessions = countMatchingSessions(tenantId);
        if (targetSessions == 0) {
            logger.warn("No WebSocket sessions found for tenant: {}", tenantId);
            return false;
        }

        broadcastToSessions(message, tenantId);
        logger.info("Broadcasted generic notification to {} clients for tenant: {}",
                targetSessions, tenantId);
        return true;
    }

    /**
     * Broadcast notification to a specific user
     *
     * @param userId User ID to send notification to
     * @param tenantId Tenant ID for security validation
     * @param message The notification message map (pre-formatted)
     * @return true if message was sent successfully to the user
     */
    public boolean broadcastToUser(String userId, String tenantId, Map<String, Object> message) {
        // Find sessions for the specific user within the tenant
        int sentCount = 0;
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            String sessionId = entry.getKey();
            WebSocketSession session = entry.getValue();

            // Check tenant match
            String sessionTenantId = sessionTenants.get(sessionId);
            if (tenantId != null && !tenantId.equals(sessionTenantId)) {
                continue; // Skip - different tenant
            }

            // Check user match
            String sessionUser = sessionUsers.get(sessionId);
            if (sessionUser != null && sessionUser.equals(userId)) {
                try {
                    sendMessage(session, message);
                    sentCount++;
                } catch (Exception e) {
                    logger.error("Failed to send message to user session {}: {}", sessionId, e.getMessage());
                }
            }
        }

        if (sentCount == 0) {
            logger.warn("No WebSocket sessions found for user: {} in tenant: {}", userId, tenantId);
            return false;
        }

        logger.info("Broadcasted notification to user {} ({} sessions) in tenant: {}",
                userId, sentCount, tenantId);
        return true;
    }

    /**
     * Broadcast notification to all users with a specific role within a tenant
     *
     * @param tenantId Tenant ID for filtering
     * @param role Role to broadcast to (e.g., "DOCTOR", "NURSE", "CARE_COORDINATOR")
     * @param message The notification message map (pre-formatted)
     * @return true if message was sent successfully to at least one user with the role
     */
    public boolean broadcastToRole(String tenantId, String role, Map<String, Object> message) {
        // Find sessions for users with the specified role within the tenant
        int sentCount = 0;
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            String sessionId = entry.getKey();
            WebSocketSession session = entry.getValue();

            // Check tenant match
            String sessionTenantId = sessionTenants.get(sessionId);
            if (!tenantId.equals(sessionTenantId)) {
                continue; // Skip - different tenant
            }

            // Check role match (from session attributes)
            Map<String, Object> attributes = session.getAttributes();
            String sessionRole = (String) attributes.get("role");
            if (role.equals(sessionRole)) {
                try {
                    sendMessage(session, message);
                    sentCount++;
                } catch (Exception e) {
                    logger.error("Failed to send message to role session {}: {}", sessionId, e.getMessage());
                }
            }
        }

        if (sentCount == 0) {
            logger.warn("No WebSocket sessions found for role: {} in tenant: {}", role, tenantId);
            return false;
        }

        logger.info("Broadcasted notification to role {} ({} sessions) in tenant: {}",
                role, sentCount, tenantId);
        return true;
    }
}
