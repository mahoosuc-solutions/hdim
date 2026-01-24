package com.healthdata.gateway.service;

import com.healthdata.gateway.dto.SessionMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing user sessions across distributed gateway instances.
 *
 * Features:
 * - Track active sessions per user
 * - Concurrent session limiting (max 5 per user)
 * - Session revocation (individual or bulk)
 * - Session metadata (device, IP, location)
 * - Multi-device session management
 *
 * HIPAA Compliance:
 * - Audit logging for all session operations
 * - Session timeout enforcement (4 hours idle)
 * - Secure session storage (Redis with TLS)
 * - Session revocation on password change
 *
 * Use Cases:
 * - Prevent account sharing (limit concurrent sessions)
 * - Security response (revoke all sessions on compromise)
 * - User convenience (view active sessions, logout from other devices)
 * - Admin control (revoke user sessions)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagementService {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_CONCURRENT_SESSIONS = 5;
    private static final String SESSION_METADATA_PREFIX = "session:metadata:";

    /**
     * Get all active sessions for a user.
     *
     * @param username Username (principal name)
     * @return List of active sessions with metadata
     */
    public List<SessionMetadata> getActiveSessions(String username) {
        Map<String, ? extends Session> sessions = sessionRepository
            .findByPrincipalName(username);

        return sessions.entrySet().stream()
            .map(entry -> buildSessionMetadata(entry.getKey(), entry.getValue(), username))
            .sorted(Comparator.comparing(SessionMetadata::getLastAccessedAt).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Get session count for a user.
     *
     * @param username Username
     * @return Number of active sessions
     */
    public int getActiveSessionCount(String username) {
        return sessionRepository.findByPrincipalName(username).size();
    }

    /**
     * Check if user has reached concurrent session limit.
     *
     * @param username Username
     * @return true if at session limit
     */
    public boolean isAtSessionLimit(String username) {
        return getActiveSessionCount(username) >= MAX_CONCURRENT_SESSIONS;
    }

    /**
     * Enforce concurrent session limit by removing oldest session.
     * Called before creating a new session when limit is reached.
     *
     * @param username Username
     */
    public void enforceSessionLimit(String username) {
        Map<String, ? extends Session> sessions = sessionRepository
            .findByPrincipalName(username);

        if (sessions.size() >= MAX_CONCURRENT_SESSIONS) {
            // Find oldest session by last accessed time
            Map.Entry<String, ? extends Session> oldestSession = sessions.entrySet().stream()
                .min(Comparator.comparing(entry -> entry.getValue().getLastAccessedTime()))
                .orElse(null);

            if (oldestSession != null) {
                String oldestSessionId = oldestSession.getKey();
                log.warn("User {} reached session limit ({}). Removing oldest session: {}",
                    username, MAX_CONCURRENT_SESSIONS, oldestSessionId);

                sessionRepository.deleteById(oldestSessionId);
                deleteSessionMetadata(oldestSessionId);

                log.info("Oldest session removed for user: {} (sessionId: {})", username, oldestSessionId);
            }
        }
    }

    /**
     * Revoke a specific session.
     *
     * @param sessionId Session ID to revoke
     * @param username Username (for audit logging)
     */
    public void revokeSession(String sessionId, String username) {
        sessionRepository.deleteById(sessionId);
        deleteSessionMetadata(sessionId);

        log.info("Session revoked: sessionId={}, username={}", sessionId, username);
    }

    /**
     * Revoke all sessions for a user.
     * Used for:
     * - Logout from all devices
     * - Password change
     * - Account compromise response
     * - Admin revocation
     *
     * @param username Username
     * @return Number of sessions revoked
     */
    public int revokeAllSessions(String username) {
        Map<String, ? extends Session> sessions = sessionRepository
            .findByPrincipalName(username);

        int revokedCount = sessions.size();

        sessions.keySet().forEach(sessionId -> {
            sessionRepository.deleteById(sessionId);
            deleteSessionMetadata(sessionId);
        });

        log.info("All sessions revoked for user: {} ({} sessions removed)", username, revokedCount);

        return revokedCount;
    }

    /**
     * Revoke all sessions except the current one.
     * Used for "logout from other devices" feature.
     *
     * @param username Username
     * @param currentSessionId Current session ID to preserve
     * @return Number of sessions revoked
     */
    public int revokeOtherSessions(String username, String currentSessionId) {
        Map<String, ? extends Session> sessions = sessionRepository
            .findByPrincipalName(username);

        int revokedCount = 0;

        for (String sessionId : sessions.keySet()) {
            if (!sessionId.equals(currentSessionId)) {
                sessionRepository.deleteById(sessionId);
                deleteSessionMetadata(sessionId);
                revokedCount++;
            }
        }

        log.info("Other sessions revoked for user: {} ({} sessions removed, current session preserved)",
            username, revokedCount);

        return revokedCount;
    }

    /**
     * Store session metadata (device, IP, user agent).
     * Called when session is created.
     *
     * @param sessionId Session ID
     * @param userId User ID
     * @param username Username
     * @param ipAddress IP address
     * @param userAgent User agent string
     */
    public void storeSessionMetadata(
        String sessionId,
        UUID userId,
        String username,
        String ipAddress,
        String userAgent
    ) {
        SessionMetadata metadata = SessionMetadata.builder()
            .sessionId(sessionId)
            .userId(userId)
            .username(username)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .deviceInfo(parseDeviceInfo(userAgent))
            .createdAt(Instant.now())
            .lastAccessedAt(Instant.now())
            .build();

        String key = SESSION_METADATA_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, metadata);

        log.debug("Session metadata stored: sessionId={}, username={}, device={}",
            sessionId, username, metadata.getDeviceInfo());
    }

    /**
     * Update session last accessed time.
     *
     * @param sessionId Session ID
     */
    public void updateLastAccessedTime(String sessionId) {
        String key = SESSION_METADATA_PREFIX + sessionId;
        SessionMetadata metadata = (SessionMetadata) redisTemplate.opsForValue().get(key);

        if (metadata != null) {
            metadata.setLastAccessedAt(Instant.now());
            redisTemplate.opsForValue().set(key, metadata);
        }
    }

    /**
     * Get session metadata.
     *
     * @param sessionId Session ID
     * @return Session metadata or null if not found
     */
    public SessionMetadata getSessionMetadata(String sessionId) {
        String key = SESSION_METADATA_PREFIX + sessionId;
        return (SessionMetadata) redisTemplate.opsForValue().get(key);
    }

    // --- Private Helper Methods ---

    private SessionMetadata buildSessionMetadata(
        String sessionId,
        Session session,
        String username
    ) {
        SessionMetadata storedMetadata = getSessionMetadata(sessionId);

        if (storedMetadata != null) {
            // Update with latest session data
            storedMetadata.setLastAccessedAt(session.getLastAccessedTime());
            return storedMetadata;
        }

        // Fallback if metadata not found
        return SessionMetadata.builder()
            .sessionId(sessionId)
            .username(username)
            .createdAt(session.getCreationTime())
            .lastAccessedAt(session.getLastAccessedTime())
            .deviceInfo("Unknown")
            .build();
    }

    private void deleteSessionMetadata(String sessionId) {
        String key = SESSION_METADATA_PREFIX + sessionId;
        redisTemplate.delete(key);
    }

    private String parseDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        // Simple device detection (can be enhanced with user-agent parser library)
        if (userAgent.contains("Mobile") || userAgent.contains("Android") || userAgent.contains("iPhone")) {
            return "Mobile";
        } else if (userAgent.contains("Tablet") || userAgent.contains("iPad")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }
}
