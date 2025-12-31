package com.healthdata.quality.websocket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Session Timeout Manager
 *
 * HIPAA Security Rule Compliance:
 * - §164.312(a)(2)(iii) - Automatic Logoff
 * - §164.308(a)(5)(ii)(C) - Log-in Monitoring
 * - §164.312(b) - Audit Controls
 *
 * Security Features:
 * - Enforces maximum session duration (15 minutes recommended for HIPAA)
 * - Tracks last activity time for each session
 * - Automatically disconnects inactive sessions
 * - Logs all timeout events for audit trail
 * - Configurable timeout period via application properties
 *
 * Implementation:
 * - Runs scheduled check every 60 seconds
 * - Disconnects sessions exceeding timeout threshold
 * - Gracefully closes connections with appropriate status code
 * - Triggers audit logging on timeout
 */
@ConditionalOnProperty(name = "websocket.enabled", havingValue = "true", matchIfMissing = true)
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionTimeoutManager {

    @Value("${websocket.security.session-timeout-minutes:15}")
    private int sessionTimeoutMinutes;

    // Map: sessionId -> last activity timestamp
    private final Map<String, Long> sessionLastActivity = new ConcurrentHashMap<>();

    // Map: sessionId -> WebSocketSession (shared with HealthScoreWebSocketHandler)
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Register a new WebSocket session for timeout monitoring
     *
     * @param sessionId Session ID
     * @param session WebSocket session
     */
    public void registerSession(String sessionId, WebSocketSession session) {
        activeSessions.put(sessionId, session);
        updateLastActivity(sessionId);
        log.debug("Registered session for timeout monitoring: sessionId={}", sessionId);
    }

    /**
     * Unregister a WebSocket session from timeout monitoring
     *
     * @param sessionId Session ID
     */
    public void unregisterSession(String sessionId) {
        activeSessions.remove(sessionId);
        sessionLastActivity.remove(sessionId);
        log.debug("Unregistered session from timeout monitoring: sessionId={}", sessionId);
    }

    /**
     * Update last activity timestamp for a session
     *
     * Call this method whenever a session sends or receives a message
     *
     * @param sessionId Session ID
     */
    public void updateLastActivity(String sessionId) {
        sessionLastActivity.put(sessionId, System.currentTimeMillis());
    }

    /**
     * Scheduled task to check for and disconnect timed-out sessions
     *
     * Runs every 60 seconds (1 minute)
     * HIPAA requirement: §164.312(a)(2)(iii) - Automatic Logoff
     */
    @Scheduled(fixedRate = 60000) // 60 seconds
    public void checkSessionTimeouts() {
        long now = System.currentTimeMillis();
        long timeoutThreshold = sessionTimeoutMinutes * 60 * 1000; // Convert minutes to milliseconds

        List<String> timedOutSessions = new ArrayList<>();

        // Check each active session for timeout
        sessionLastActivity.forEach((sessionId, lastActivity) -> {
            long inactiveDuration = now - lastActivity;

            if (inactiveDuration > timeoutThreshold) {
                timedOutSessions.add(sessionId);
            }
        });

        // Disconnect timed-out sessions
        for (String sessionId : timedOutSessions) {
            disconnectSession(sessionId, "Session timeout after " + sessionTimeoutMinutes + " minutes of inactivity");
        }

        if (!timedOutSessions.isEmpty()) {
            log.info("Disconnected {} timed-out WebSocket session(s)", timedOutSessions.size());
        }
    }

    /**
     * Gracefully disconnect a session
     *
     * @param sessionId Session ID
     * @param reason Reason for disconnection
     */
    private void disconnectSession(String sessionId, String reason) {
        WebSocketSession session = activeSessions.get(sessionId);

        if (session != null && session.isOpen()) {
            try {
                // Extract user info for audit logging
                Map<String, Object> attributes = session.getAttributes();
                String username = (String) attributes.get("username");
                String tenantId = (String) attributes.get("tenantId");
                Long lastActivity = sessionLastActivity.get(sessionId);

                long inactiveDuration = 0;
                if (lastActivity != null) {
                    inactiveDuration = (System.currentTimeMillis() - lastActivity) / 1000; // Convert to seconds
                }

                log.warn("HIPAA AUTOMATIC LOGOFF: Disconnecting session due to timeout - " +
                        "sessionId={}, user={}, tenant={}, inactiveDuration={}s, reason={}",
                        sessionId, username, tenantId, inactiveDuration, reason);

                // Close session with appropriate status
                session.close(CloseStatus.SESSION_NOT_RELIABLE.withReason(reason));

                // Cleanup
                unregisterSession(sessionId);

            } catch (IOException e) {
                log.error("Failed to close timed-out session {}: {}", sessionId, e.getMessage());
                // Force cleanup even if close fails
                unregisterSession(sessionId);
            }
        } else {
            // Session already closed, just cleanup
            unregisterSession(sessionId);
        }
    }

    /**
     * Get session timeout configuration
     *
     * @return Session timeout in minutes
     */
    public int getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }

    /**
     * Get count of active sessions being monitored
     *
     * @return Number of active sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * Get session inactivity duration
     *
     * @param sessionId Session ID
     * @return Inactivity duration in milliseconds, or -1 if session not found
     */
    public long getSessionInactivityDuration(String sessionId) {
        Long lastActivity = sessionLastActivity.get(sessionId);
        if (lastActivity == null) {
            return -1;
        }
        return System.currentTimeMillis() - lastActivity;
    }
}
