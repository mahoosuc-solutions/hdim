package com.healthdata.authentication.context;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable user context extracted from trusted gateway headers.
 *
 * NOT a JPA entity - purely a data carrier for audit/authorization.
 * This class can be safely used by any service without triggering
 * Hibernate entity scanning.
 *
 * HIPAA Compliance:
 * - 45 CFR 164.312(b): Provides user identification for audit controls
 * - 45 CFR 164.312(d): Supports person/entity authentication verification
 *
 * Usage:
 * <pre>
 * UserContext context = UserContextHolder.getContext();
 * if (context != null) {
 *     String userId = context.userId().toString();
 *     boolean hasAccess = context.hasAccessToTenant("tenant-001");
 * }
 * </pre>
 *
 * @param userId      Unique user identifier (UUID)
 * @param username    User's login name
 * @param tenantIds   Set of tenant IDs the user can access
 * @param roles       Set of role names assigned to the user
 * @param ipAddress   Client IP address (for audit logging)
 * @param userAgent   Client user agent string (for audit logging)
 * @param tokenId     JWT token ID for session tracking and revocation
 */
public record UserContext(
    UUID userId,
    String username,
    Set<String> tenantIds,
    Set<String> roles,
    String ipAddress,
    String userAgent,
    String tokenId
) {

    /**
     * Creates a UserContext with defensive copies of collections.
     */
    public UserContext {
        // Defensive copies to ensure immutability
        tenantIds = tenantIds != null ? Set.copyOf(tenantIds) : Collections.emptySet();
        roles = roles != null ? Set.copyOf(roles) : Collections.emptySet();
    }

    /**
     * Check if user has a specific role.
     *
     * @param role the role to check
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Check if user has any of the specified roles.
     *
     * @param requiredRoles the roles to check
     * @return true if user has at least one of the roles
     */
    public boolean hasAnyRole(String... requiredRoles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (String role : requiredRoles) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user has access to a specific tenant.
     *
     * @param tenantId the tenant ID to check
     * @return true if user can access the tenant
     */
    public boolean hasAccessToTenant(String tenantId) {
        return tenantIds != null && tenantIds.contains(tenantId);
    }

    /**
     * Check if user is a super admin (has SUPER_ADMIN role).
     *
     * @return true if user is a super admin
     */
    public boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN");
    }

    /**
     * Check if user is an admin (has ADMIN or SUPER_ADMIN role).
     *
     * @return true if user is an admin
     */
    public boolean isAdmin() {
        return hasAnyRole("SUPER_ADMIN", "ADMIN");
    }

    /**
     * Get the user ID as a string (for logging/audit).
     *
     * @return user ID as string, or "anonymous" if null
     */
    public String userIdAsString() {
        return userId != null ? userId.toString() : "anonymous";
    }

    /**
     * Get the first tenant ID (for single-tenant operations).
     *
     * @return first tenant ID, or null if no tenants
     */
    public String primaryTenantId() {
        return tenantIds != null && !tenantIds.isEmpty()
            ? tenantIds.iterator().next()
            : null;
    }

    /**
     * Create a builder for constructing UserContext instances.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for UserContext.
     */
    public static class Builder {
        private UUID userId;
        private String username;
        private Set<String> tenantIds;
        private Set<String> roles;
        private String ipAddress;
        private String userAgent;
        private String tokenId;

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId != null && !userId.isBlank()
                ? UUID.fromString(userId)
                : null;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder tenantIds(Set<String> tenantIds) {
            this.tenantIds = tenantIds;
            return this;
        }

        public Builder roles(Set<String> roles) {
            this.roles = roles;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder tokenId(String tokenId) {
            this.tokenId = tokenId;
            return this;
        }

        public UserContext build() {
            return new UserContext(
                userId,
                username,
                tenantIds,
                roles,
                ipAddress,
                userAgent,
                tokenId
            );
        }
    }
}
