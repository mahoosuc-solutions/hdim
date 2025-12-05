package com.healthdata.authentication.dto;

import com.healthdata.authentication.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * Response DTO for user information.
 * Used for registration responses and "get current user" endpoint.
 * Does not include sensitive information like password hash.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {

    /**
     * User's unique identifier.
     */
    private UUID id;

    /**
     * Username.
     */
    private String username;

    /**
     * Email address.
     */
    private String email;

    /**
     * User's first name.
     */
    private String firstName;

    /**
     * User's last name.
     */
    private String lastName;

    /**
     * User's roles for RBAC.
     */
    private Set<UserRole> roles;

    /**
     * Tenant IDs the user has access to.
     */
    private Set<String> tenantIds;

    /**
     * Whether the account is active.
     */
    private Boolean active;

    /**
     * Whether the email has been verified.
     */
    private Boolean emailVerified;
}
