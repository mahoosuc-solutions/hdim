package com.healthdata.authentication.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * API Key entity for third-party API authentication.
 *
 * API Keys provide programmatic access to the HDIM API without requiring
 * user authentication. They are scoped to specific permissions and tenants.
 *
 * Security considerations:
 * - Keys are stored as SHA-256 hashes
 * - Raw key is only shown once at creation time
 * - Keys can be rotated without downtime
 * - Keys have expiration dates
 * - Keys are scoped to specific tenants and permissions
 */
@Entity
@Table(name = "api_keys", indexes = {
    @Index(name = "idx_api_keys_key_hash", columnList = "keyHash", unique = true),
    @Index(name = "idx_api_keys_tenant_id", columnList = "tenantId"),
    @Index(name = "idx_api_keys_active", columnList = "active"),
    @Index(name = "idx_api_keys_prefix", columnList = "keyPrefix")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Human-readable name for the API key.
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Description of the API key's purpose.
     */
    @Column(length = 500)
    private String description;

    /**
     * SHA-256 hash of the API key.
     * The raw key is never stored, only the hash for validation.
     */
    @Column(nullable = false, unique = true, length = 64)
    private String keyHash;

    /**
     * Key prefix for identification (e.g., "hdim_live_abc123").
     * Allows identifying keys without revealing the full key.
     */
    @Column(nullable = false, length = 20)
    private String keyPrefix;

    /**
     * Tenant ID that this API key belongs to.
     * API keys are scoped to a single tenant.
     */
    @Column(nullable = false)
    private String tenantId;

    /**
     * User ID that created this API key.
     */
    @Column(nullable = false)
    private UUID createdBy;

    /**
     * Scopes/permissions granted to this API key.
     * Examples: "fhir:read", "fhir:write", "measures:read", "patients:read"
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "api_key_scopes",
        joinColumns = @JoinColumn(name = "api_key_id")
    )
    @Column(name = "scope", nullable = false)
    @Builder.Default
    private Set<String> scopes = new HashSet<>();

    /**
     * Whether the API key is active.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Expiration date of the API key.
     * Null means no expiration.
     */
    @Column
    private Instant expiresAt;

    /**
     * Last time the API key was used.
     */
    @Column
    private Instant lastUsedAt;

    /**
     * IP address that last used the API key.
     */
    @Column(length = 45)
    private String lastUsedIp;

    /**
     * Number of times the API key has been used.
     */
    @Column
    @Builder.Default
    private Long usageCount = 0L;

    /**
     * Rate limit (requests per minute).
     * Null means default rate limit applies.
     */
    @Column
    private Integer rateLimitPerMinute;

    /**
     * Allowed IP addresses (CIDR notation).
     * Null or empty means all IPs are allowed.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "api_key_allowed_ips",
        joinColumns = @JoinColumn(name = "api_key_id")
    )
    @Column(name = "ip_cidr", nullable = false)
    @Builder.Default
    private Set<String> allowedIps = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * When the API key was revoked (soft delete).
     */
    @Column
    private Instant revokedAt;

    /**
     * User ID that revoked this API key.
     */
    @Column
    private UUID revokedBy;

    /**
     * Reason for revocation.
     */
    @Column(length = 500)
    private String revocationReason;

    /**
     * Check if the API key is valid (active and not expired).
     */
    public boolean isValid() {
        if (!active || revokedAt != null) {
            return false;
        }
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            return false;
        }
        return true;
    }

    /**
     * Check if the API key has a specific scope.
     */
    public boolean hasScope(String scope) {
        if (scopes == null || scopes.isEmpty()) {
            return false;
        }
        // Check exact match
        if (scopes.contains(scope)) {
            return true;
        }
        // Check wildcard match (e.g., "fhir:*" matches "fhir:read")
        String[] parts = scope.split(":");
        if (parts.length >= 1) {
            String wildcardScope = parts[0] + ":*";
            if (scopes.contains(wildcardScope)) {
                return true;
            }
        }
        // Check global wildcard
        return scopes.contains("*");
    }

    /**
     * Check if the API key has any of the specified scopes.
     */
    public boolean hasAnyScope(String... scopesToCheck) {
        for (String scope : scopesToCheck) {
            if (hasScope(scope)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Record API key usage.
     */
    public void recordUsage(String ipAddress) {
        this.lastUsedAt = Instant.now();
        this.lastUsedIp = ipAddress;
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
    }

    /**
     * Revoke the API key.
     */
    public void revoke(UUID revokedByUserId, String reason) {
        this.active = false;
        this.revokedAt = Instant.now();
        this.revokedBy = revokedByUserId;
        this.revocationReason = reason;
    }
}
