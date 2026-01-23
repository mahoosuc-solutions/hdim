package com.healthdata.authentication.dto;

import com.healthdata.authentication.domain.TenantStatus;
import com.healthdata.authentication.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Response DTO for tenant registration.
 * Contains tenant metadata and admin user details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantRegistrationResponse {

    /**
     * Tenant identifier.
     */
    private String tenantId;

    /**
     * Tenant name.
     */
    private String tenantName;

    /**
     * Tenant status (should be ACTIVE for new tenants).
     */
    private TenantStatus status;

    /**
     * Timestamp when tenant was created.
     */
    private Instant createdAt;

    /**
     * Admin user information.
     */
    private AdminUserInfo adminUser;

    /**
     * Nested DTO for admin user information.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminUserInfo {

        /**
         * User ID (UUID).
         */
        private UUID userId;

        /**
         * Username.
         */
        private String username;

        /**
         * Email address.
         */
        private String email;

        /**
         * User roles (should include ADMIN).
         */
        private Set<UserRole> roles;

        /**
         * Tenant IDs user has access to (should include the new tenant).
         */
        private Set<String> tenantIds;
    }
}
