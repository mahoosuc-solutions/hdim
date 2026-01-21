package com.healthdata.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for token refresh endpoint (Phase 2.0 Team 3.1)
 *
 * Contains the refresh token to exchange for new access/refresh tokens
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequest {

    /**
     * Refresh token from previous authentication
     * Required for token refresh
     */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
