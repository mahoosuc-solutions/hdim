package com.healthdata.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for refresh token operation.
 *
 * Used to request a new access token using a refresh token.
 * The refresh token must be:
 * - Valid (not expired)
 * - Not revoked
 * - Associated with an active user account
 *
 * Endpoint: POST /api/v1/auth/refresh
 *
 * Example request:
 * <pre>
 * {
 *   "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
 * }
 * </pre>
 *
 * Example response (JwtAuthenticationResponse):
 * <pre>
 * {
 *   "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
 *   "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
 *   "tokenType": "Bearer",
 *   "expiresIn": 900,
 *   "username": "john.doe",
 *   "roles": ["USER", "ADMIN"],
 *   "tenantIds": ["tenant-1", "tenant-2"]
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequest {

    /**
     * The refresh token to use for obtaining a new access token.
     * Must be a valid, non-expired, non-revoked refresh token.
     */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
