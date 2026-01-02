package com.healthdata.authentication.entity;

import com.healthdata.authentication.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a refresh token for JWT authentication.
 *
 * Refresh tokens are long-lived tokens used to obtain new access tokens
 * without requiring the user to re-authenticate. They are stored in the
 * database to enable revocation and audit tracking.
 *
 * Security Features:
 * - Unique token value (indexed for fast lookup)
 * - Expiration tracking
 * - Revocation support
 * - User association (foreign key to users table)
 * - IP address and user agent tracking for audit
 * - Automatic creation timestamp
 *
 * Lifecycle:
 * 1. Created when user logs in
 * 2. Used to obtain new access tokens
 * 3. Can be revoked by user or admin
 * 4. Automatically cleaned up when expired
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_token", columnList = "token", unique = true),
    @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The actual JWT refresh token value.
     * Must be unique and indexed for fast lookup.
     */
    @Column(nullable = false, unique = true, length = 1000)
    private String token;

    /**
     * Hash of the refresh token value.
     * Used for secure storage - the actual JWT is never stored in plaintext.
     * SHA-256 or similar hash algorithm should be used.
     */
    @Column(nullable = false, length = 255, name = "token_hash")
    private String tokenHash;

    /**
     * User who owns this refresh token.
     * Foreign key to users table.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * When this token expires.
     * Tokens are automatically cleaned up after expiration.
     */
    @Column(nullable = false, name = "expires_at")
    private Instant expiresAt;

    /**
     * When this token was created.
     * Automatically set on entity creation.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;

    /**
     * Flag indicating if this token has been revoked.
     * Revoked tokens cannot be used to refresh access tokens.
     */
    @Column(nullable = false, name = "revoked")
    @Builder.Default
    private Boolean revoked = false;

    /**
     * When this token was revoked (if revoked).
     * Null if token is still active.
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * IP address from which the token was issued.
     * Used for audit and security tracking.
     */
    @Column(length = 45, name = "ip_address")
    private String ipAddress;

    /**
     * User agent (browser/client) that requested the token.
     * Used for audit and security tracking.
     */
    @Column(length = 500, name = "user_agent")
    private String userAgent;

    /**
     * Check if this refresh token is expired.
     *
     * @return true if token is expired, false otherwise
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    /**
     * Check if this refresh token is revoked.
     *
     * @return true if token is revoked, false otherwise
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Check if this refresh token is valid (not expired and not revoked).
     *
     * @return true if token is valid, false otherwise
     */
    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }

    /**
     * Revoke this refresh token.
     * Sets the revokedAt timestamp to the current time.
     */
    public void revoke() {
        this.revokedAt = Instant.now();
    }
}
