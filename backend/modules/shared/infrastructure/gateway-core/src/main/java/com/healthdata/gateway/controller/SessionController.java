package com.healthdata.gateway.controller;

import com.healthdata.gateway.dto.SessionMetadata;
import com.healthdata.gateway.service.SessionManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * REST controller for session management operations.
 *
 * Endpoints:
 * - GET /api/v1/auth/sessions - List active sessions for current user
 * - POST /api/v1/auth/sessions/{sessionId}/revoke - Revoke specific session
 * - POST /api/v1/auth/sessions/revoke-all - Logout from all devices
 * - POST /api/v1/auth/sessions/revoke-others - Logout from other devices
 * - GET /api/v1/auth/sessions/count - Get session count for current user
 *
 * Admin Endpoints:
 * - POST /api/v1/auth/admin/sessions/{username}/revoke-all - Revoke all sessions for a user (admin only)
 *
 * Security:
 * - All endpoints require authentication
 * - Users can only manage their own sessions
 * - Admin endpoints require SUPER_ADMIN or ADMIN role
 *
 * HIPAA Compliance:
 * - All session operations are audit logged
 * - Session metadata includes IP address and device for security tracking
 *
 * Use Cases:
 * - User views active sessions in settings
 * - User logs out from all devices after password change
 * - User logs out from other devices (keep current session)
 * - Admin revokes compromised user sessions
 */
@RestController
@RequestMapping("/api/v1/auth/sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Session Management", description = "Manage user sessions and concurrent logins")
@ConditionalOnBean(SessionManagementService.class)
public class SessionController {

    private final SessionManagementService sessionManagementService;

    /**
     * Get all active sessions for the current user.
     *
     * @param authentication Current user authentication
     * @param currentSession Current HTTP session
     * @return List of active sessions with metadata
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "List active sessions",
        description = "Get all active sessions for the current user with device and location info"
    )
    public ResponseEntity<List<SessionMetadata>> getActiveSessions(
        Authentication authentication,
        HttpSession currentSession
    ) {
        String username = authentication.getName();
        String currentSessionId = currentSession.getId();

        log.info("Fetching active sessions for user: {}", username);

        List<SessionMetadata> sessions = sessionManagementService.getActiveSessions(username);

        // Mark current session
        sessions.forEach(session -> {
            if (session.getSessionId().equals(currentSessionId)) {
                session.setCurrentSession(true);
            }
        });

        log.info("Found {} active sessions for user: {}", sessions.size(), username);

        return ResponseEntity.ok(sessions);
    }

    /**
     * Get active session count for the current user.
     *
     * @param authentication Current user authentication
     * @return Session count
     */
    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get session count",
        description = "Get the number of active sessions for the current user"
    )
    public ResponseEntity<Map<String, Integer>> getSessionCount(Authentication authentication) {
        String username = authentication.getName();

        int count = sessionManagementService.getActiveSessionCount(username);

        log.info("User {} has {} active sessions", username, count);

        return ResponseEntity.ok(Map.of(
            "activeSessionCount", count,
            "maxSessionsAllowed", 5
        ));
    }

    /**
     * Revoke a specific session.
     * Users can only revoke their own sessions.
     *
     * @param sessionId Session ID to revoke
     * @param authentication Current user authentication
     * @return Success message
     */
    @PostMapping("/{sessionId}/revoke")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Revoke specific session",
        description = "Revoke a specific session by ID. Users can only revoke their own sessions."
    )
    public ResponseEntity<Map<String, String>> revokeSession(
        @Parameter(description = "Session ID to revoke")
        @PathVariable String sessionId,
        Authentication authentication
    ) {
        String username = authentication.getName();

        log.info("Revoking session: sessionId={}, username={}", sessionId, username);

        sessionManagementService.revokeSession(sessionId, username);

        return ResponseEntity.ok(Map.of(
            "message", "Session revoked successfully",
            "sessionId", sessionId
        ));
    }

    /**
     * Revoke all sessions for the current user (logout from all devices).
     *
     * @param authentication Current user authentication
     * @return Number of sessions revoked
     */
    @PostMapping("/revoke-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Logout from all devices",
        description = "Revoke all active sessions for the current user (including current session)"
    )
    public ResponseEntity<Map<String, Object>> revokeAllSessions(Authentication authentication) {
        String username = authentication.getName();

        log.info("Revoking all sessions for user: {}", username);

        int revokedCount = sessionManagementService.revokeAllSessions(username);

        return ResponseEntity.ok(Map.of(
            "message", "All sessions revoked successfully",
            "sessionsRevoked", revokedCount
        ));
    }

    /**
     * Revoke all sessions except the current one (logout from other devices).
     *
     * @param authentication Current user authentication
     * @param currentSession Current HTTP session
     * @return Number of sessions revoked
     */
    @PostMapping("/revoke-others")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Logout from other devices",
        description = "Revoke all sessions except the current one (keep current device logged in)"
    )
    public ResponseEntity<Map<String, Object>> revokeOtherSessions(
        Authentication authentication,
        HttpSession currentSession
    ) {
        String username = authentication.getName();
        String currentSessionId = currentSession.getId();

        log.info("Revoking other sessions for user: {} (current session: {})", username, currentSessionId);

        int revokedCount = sessionManagementService.revokeOtherSessions(username, currentSessionId);

        return ResponseEntity.ok(Map.of(
            "message", "Other sessions revoked successfully",
            "sessionsRevoked", revokedCount,
            "currentSessionPreserved", true
        ));
    }

    /**
     * Admin endpoint: Revoke all sessions for a specific user.
     * Used for security incidents, account compromise, or account deactivation.
     *
     * @param username Username
     * @param authentication Current admin user
     * @return Number of sessions revoked
     */
    @PostMapping("/admin/{username}/revoke-all")
    @PreAuthorize("hasPermission('USER_MANAGE_ROLES')")
    @Operation(
        summary = "Admin: Revoke all user sessions",
        description = "Admin endpoint to revoke all sessions for a specific user (security response)"
    )
    public ResponseEntity<Map<String, Object>> adminRevokeAllSessions(
        @Parameter(description = "Username")
        @PathVariable String username,
        Authentication authentication
    ) {
        String adminUsername = authentication.getName();

        log.warn("Admin session revocation: admin={}, targetUser={}", adminUsername, username);

        int revokedCount = sessionManagementService.revokeAllSessions(username);

        log.info("Admin {} revoked {} sessions for user: {}", adminUsername, revokedCount, username);

        return ResponseEntity.ok(Map.of(
            "message", "All sessions revoked by admin",
            "targetUser", username,
            "sessionsRevoked", revokedCount,
            "revokedBy", adminUsername
        ));
    }
}
