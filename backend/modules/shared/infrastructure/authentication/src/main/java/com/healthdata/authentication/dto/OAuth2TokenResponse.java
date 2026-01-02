package com.healthdata.authentication.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OAuth2 Token Response DTO.
 *
 * Contains the tokens returned after successful OAuth2 authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2TokenResponse {

    /**
     * Access token for API authentication.
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * Refresh token for obtaining new access tokens.
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * Token type (typically "Bearer").
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * Access token expiration time in seconds.
     */
    @JsonProperty("expires_in")
    private Integer expiresIn;

    /**
     * ID token (OIDC).
     */
    @JsonProperty("id_token")
    private String idToken;

    /**
     * OAuth2 provider used for authentication.
     */
    private String provider;

    /**
     * User ID in the local system.
     */
    @JsonProperty("user_id")
    private String userId;

    /**
     * Username in the local system.
     */
    private String username;

    /**
     * Scopes granted.
     */
    private String scope;
}
