package com.healthdata.shared.security.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Login Response DTO - Represents successful authentication response
 *
 * Returns JWT tokens, user information, and metadata on successful login.
 * Spring Boot 3.3.5 compatible - Uses Jakarta EE (jakarta.*)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    /**
     * Authentication status
     * Values: "success", "error"
     */
    private String status;

    /**
     * Descriptive message about authentication result
     */
    private String message;

    /**
     * JWT access token for API requests. Use as: Authorization: Bearer {token}
     * This token typically expires quickly (e.g., 15 minutes)
     */
    private String accessToken;

    /**
     * JWT refresh token for obtaining new access tokens
     * (e.g., 7 days).
     */
    private String refreshToken;

    /**
     * Token type
     * Values: "Bearer"
     */
    private String tokenType;

    /**
     * Access token expiration time in seconds
     * Client should refresh token before this time elapses
     */
    private Long expiresIn;

    /**
     * Refresh token expiration time in seconds
     */
    private Long refreshExpiresIn;

    /**
     * Authenticated user's ID (UUID)
     */
    private String userId;

    /**
     * Authenticated user's username
     */
    private String username;

    /**
     * Authenticated user's email address
     */
    private String email;

    /**
     * Authenticated user's full name
     */
    private String fullName;

    /**
     * List of user's roles
     * Examples: ["ADMIN", "PROVIDER", "CARE_MANAGER", "PATIENT"]
     */
    private List<String> roles;

    /**
     * List of user's permissions/authorities
     * These are derived from user's roles
     */
    private List<String> authorities;

    /**
     * Tenant ID for multi-tenant systems
     */
    private String tenantId;

    /**
     * Whether user's account is active
     */
    private Boolean active;

    /**
     * Whether user's email has been verified
     */
    private Boolean emailVerified;

    /**
     * Timestamp when user last logged in
     */
    private Instant lastLoginAt;

    /**
     * Timestamp when response was generated
     */
    private Instant timestamp;

    /**
     * Additional user metadata
     * Can contain custom user attributes
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> metadata;

    /**
     * Error details (only in error responses)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorDetail;

    /**
     * Error code (only in error responses)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;

    /**
     * Add metadata to response
     */
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    /**
     * Check if response indicates success
     */
    @JsonIgnore
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }

    /**
     * Check if response indicates error
     */
    @JsonIgnore
    public boolean isError() {
        return "error".equalsIgnoreCase(status);
    }
}
