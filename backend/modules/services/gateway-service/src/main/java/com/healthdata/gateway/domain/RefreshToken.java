package com.healthdata.gateway.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for Refresh Tokens (Phase 2.0 Team 3.1)
 *
 * Stores refresh token metadata for validation and revocation
 * PHI is NOT stored - only token identifiers and metadata
 */
@Entity
@Table(name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_token_user_id", columnList = "user_id"),
        @Index(name = "idx_refresh_token_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_refresh_token_jti", columnList = "token_jti"),
        @Index(name = "idx_refresh_token_hash", columnList = "token_hash"),
        @Index(name = "idx_refresh_token_expires_at", columnList = "expires_at")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Tenant ID for multi-tenant isolation
     */
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /**
     * User ID who owns this refresh token
     */
    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    /**
     * JWT ID claim (unique identifier in JWT payload)
     */
    @Column(name = "token_jti", nullable = false, unique = true, length = 255)
    private String tokenJti;

    /**
     * SHA-256 hash of the token for secure lookup
     * Stored instead of plaintext to prevent database compromise
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    /**
     * Token expiration timestamp
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Last time this refresh token was used
     * Supports sliding window session extension
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /**
     * When this token was revoked (null if not revoked)
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Reason for revocation if applicable
     * Values: LOGOUT, TOKEN_REFRESH, COMPROMISE, ADMIN_REVOKE, INACTIVITY
     */
    @Column(name = "revocation_reason", length = 50)
    private String revocationReason;

    /**
     * Timestamp when record was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when record was last updated
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if token is revoked
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Check if token is valid (not expired and not revoked)
     */
    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
