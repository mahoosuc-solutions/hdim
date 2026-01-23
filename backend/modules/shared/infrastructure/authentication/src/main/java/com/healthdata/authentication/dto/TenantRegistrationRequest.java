package com.healthdata.authentication.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for tenant registration.
 * Creates a new tenant and an admin user atomically.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantRegistrationRequest {

    /**
     * Unique tenant identifier (e.g., "acme-health", "valley-clinic").
     * Must be lowercase alphanumeric with hyphens, 3-63 characters.
     * Used in API calls via X-Tenant-ID header.
     */
    @NotBlank(message = "Tenant ID is required")
    @Pattern(
        regexp = "^[a-z0-9-]{3,63}$",
        message = "Tenant ID must be 3-63 lowercase alphanumeric characters or hyphens"
    )
    private String tenantId;

    /**
     * Human-readable tenant name (e.g., "Acme Health Systems").
     * Displayed in UI and reports.
     */
    @NotBlank(message = "Tenant name is required")
    @Size(max = 255, message = "Tenant name must not exceed 255 characters")
    private String tenantName;

    /**
     * Admin user details for the new tenant.
     * This user will be created with ADMIN role and access to this tenant.
     */
    @NotNull(message = "Admin user details are required")
    @Valid
    private AdminUserRequest adminUser;

    /**
     * Nested DTO for admin user creation.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminUserRequest {

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        private String lastName;
    }
}
