package com.healthdata.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * LinkedIn OAuth Token Storage
 *
 * Stores OAuth 2.0 tokens for LinkedIn API access.
 * Tokens are scoped per user per tenant to support multi-user scenarios.
 *
 * Security Note: Access tokens and refresh tokens should be encrypted
 * at rest in production environments.
 */
@Entity
@Table(name = "linkedin_tokens",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "user_id"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedInToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * LinkedIn member ID (sub claim from ID token)
     */
    @Column(name = "linkedin_member_id")
    private String linkedInMemberId;

    /**
     * User's display name from LinkedIn profile
     */
    @Column(name = "display_name")
    private String displayName;

    /**
     * User's email from LinkedIn profile
     */
    @Column(name = "email")
    private String email;

    /**
     * OAuth 2.0 access token
     * Used for API calls. Typically expires in 60 days.
     */
    @Column(name = "access_token", nullable = false, length = 2000)
    private String accessToken;

    /**
     * OAuth 2.0 refresh token
     * Used to obtain new access tokens. Expires in 12 months.
     */
    @Column(name = "refresh_token", length = 2000)
    private String refreshToken;

    /**
     * Granted OAuth scopes (space-separated)
     */
    @Column(name = "scope", length = 500)
    private String scope;

    /**
     * Access token expiration timestamp
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Refresh token expiration timestamp (if provided)
     */
    @Column(name = "refresh_expires_at")
    private Instant refreshExpiresAt;

    /**
     * Token status for lifecycle management
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TokenStatus status = TokenStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Last time the token was used for API calls
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /**
     * Error message if token is in ERROR status
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    public enum TokenStatus {
        ACTIVE,     // Token is valid and can be used
        EXPIRED,    // Access token expired, needs refresh
        REVOKED,    // User revoked access in LinkedIn
        ERROR       // Error refreshing token
    }

    /**
     * Check if the access token is expired or about to expire
     */
    public boolean isAccessTokenExpired() {
        // Consider expired if less than 5 minutes remaining
        return expiresAt.isBefore(Instant.now().plusSeconds(300));
    }

    /**
     * Check if the refresh token is expired
     */
    public boolean isRefreshTokenExpired() {
        if (refreshExpiresAt == null) {
            return false; // Assume not expired if no expiry set
        }
        return refreshExpiresAt.isBefore(Instant.now());
    }

    /**
     * Mark the token as used
     */
    public void markUsed() {
        this.lastUsedAt = Instant.now();
    }

    /**
     * Update token after successful refresh
     */
    public void updateToken(String newAccessToken, String newRefreshToken,
                           long expiresInSeconds, Long refreshExpiresInSeconds) {
        this.accessToken = newAccessToken;
        this.expiresAt = Instant.now().plusSeconds(expiresInSeconds);
        this.status = TokenStatus.ACTIVE;
        this.errorMessage = null;

        if (newRefreshToken != null) {
            this.refreshToken = newRefreshToken;
        }
        if (refreshExpiresInSeconds != null) {
            this.refreshExpiresAt = Instant.now().plusSeconds(refreshExpiresInSeconds);
        }
    }

    /**
     * Mark token as having an error
     */
    public void markError(String message) {
        this.status = TokenStatus.ERROR;
        this.errorMessage = message;
    }

    /**
     * Mark token as revoked
     */
    public void markRevoked() {
        this.status = TokenStatus.REVOKED;
        this.accessToken = null;
        this.refreshToken = null;
    }
}
