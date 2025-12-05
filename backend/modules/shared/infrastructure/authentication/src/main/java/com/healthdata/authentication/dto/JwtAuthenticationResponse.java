package com.healthdata.authentication.dto;

import com.healthdata.authentication.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Response DTO for JWT authentication.
 * Contains access token, refresh token, and user information.
 *
 * This response is returned after successful login or token refresh.
 * The client should:
 * 1. Store the access token (in memory or session storage)
 * 2. Store the refresh token securely (httpOnly cookie or secure storage)
 * 3. Use access token in Authorization header: "Bearer <access_token>"
 * 4. Use refresh token to obtain new access token when expired
 *
 * Token Lifecycle:
 * - Access token: Short-lived (15 minutes default), used for API requests
 * - Refresh token: Long-lived (7 days default), used to get new access tokens
 *
 * Security Best Practices:
 * - Access tokens should be stored in memory (not localStorage)
 * - Refresh tokens should be stored in httpOnly cookies or secure storage
 * - Never expose tokens in URLs or logs
 * - Implement token rotation for refresh tokens
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtAuthenticationResponse {

    /**
     * JWT access token.
     * Short-lived token for API authentication.
     * Include in Authorization header: "Bearer <access_token>"
     */
    private String accessToken;

    /**
     * JWT refresh token.
     * Long-lived token for obtaining new access tokens.
     * Should be stored securely and never exposed in URLs.
     */
    private String refreshToken;

    /**
     * Token type (always "Bearer" for JWT).
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Access token expiration time in seconds.
     * Client should refresh token before this time.
     */
    private Long expiresIn;

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
     * Additional message or information.
     */
    private String message;
}
