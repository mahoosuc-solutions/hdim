package com.healthdata.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Session metadata DTO for tracking active user sessions.
 *
 * Contains:
 * - Session identification (ID, user)
 * - Device information (browser, OS, device type)
 * - Network information (IP address)
 * - Timestamps (created, last accessed)
 *
 * Use Cases:
 * - Display active sessions in user settings
 * - Security audit trail
 * - Device-based session management
 * - "Logout from other devices" feature
 *
 * Example Response:
 * {
 *   "sessionId": "a1b2c3d4-...",
 *   "userId": "user-uuid",
 *   "username": "john.doe@example.com",
 *   "ipAddress": "192.168.1.100",
 *   "userAgent": "Mozilla/5.0...",
 *   "deviceInfo": "Desktop",
 *   "createdAt": "2025-01-24T10:00:00Z",
 *   "lastAccessedAt": "2025-01-24T14:30:00Z"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Spring Session ID (UUID format).
     */
    private String sessionId;

    /**
     * User ID (UUID).
     */
    private UUID userId;

    /**
     * Username (email or login identifier).
     */
    private String username;

    /**
     * IP address of the client.
     */
    private String ipAddress;

    /**
     * User agent string (browser, OS, device).
     * Example: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36..."
     */
    private String userAgent;

    /**
     * Parsed device information (Desktop, Mobile, Tablet, Unknown).
     */
    private String deviceInfo;

    /**
     * Browser name (Chrome, Firefox, Safari, Edge, etc.).
     * Parsed from user agent.
     */
    private String browserName;

    /**
     * Operating system (Windows, macOS, Linux, iOS, Android).
     * Parsed from user agent.
     */
    private String operatingSystem;

    /**
     * Session creation timestamp.
     */
    private Instant createdAt;

    /**
     * Last activity timestamp.
     */
    private Instant lastAccessedAt;

    /**
     * Session expiration timestamp.
     * Calculated as: lastAccessedAt + session timeout (4 hours).
     */
    private Instant expiresAt;

    /**
     * Whether this is the current session.
     * Set to true when session ID matches the current request.
     */
    private boolean currentSession;
}
