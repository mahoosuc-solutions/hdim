package com.healthdata.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for token refresh endpoint (Phase 2.0 Team 3.1)
 *
 * Contains new access and refresh tokens with expiration information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshResponse {

    /**
     * New access token valid for 15 minutes
     */
    @JsonProperty("accessToken")
    private String accessToken;

    /**
     * New refresh token valid for 30 days
     * Replaces the old refresh token
     */
    @JsonProperty("refreshToken")
    private String refreshToken;

    /**
     * Token type (always "Bearer" for JWT)
     */
    @JsonProperty("tokenType")
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Access token expiration time in seconds (900 = 15 minutes)
     */
    @JsonProperty("expiresIn")
    @Builder.Default
    private Integer expiresIn = 900;
}
