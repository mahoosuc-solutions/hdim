package com.healthdata.authentication.dto;

import com.healthdata.authentication.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Response DTO for successful user login.
 * Contains user information and authentication status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    /**
     * Username of the authenticated user.
     */
    private String username;

    /**
     * Email address of the authenticated user.
     */
    private String email;

    /**
     * User's roles for RBAC.
     */
    private Set<UserRole> roles;

    /**
     * Tenant IDs the user has access to.
     */
    private Set<String> tenantIds;

    /**
     * Success message or additional information.
     */
    private String message;
}
