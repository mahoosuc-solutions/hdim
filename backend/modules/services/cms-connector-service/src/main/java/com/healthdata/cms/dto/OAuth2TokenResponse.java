package com.healthdata.cms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OAuth2 Token Response DTO
 * 
 * Represents the successful response from CMS OAuth2 token endpoint.
 * According to RFC 6749 Section 5.1 (Successful Response)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuth2TokenResponse {

    /**
     * REQUIRED. The access token issued by the authorization server.
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * REQUIRED. The type of the token issued (typically "Bearer").
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * RECOMMENDED. The lifetime in seconds of the access token.
     * Default: 3600 (1 hour)
     */
    @JsonProperty("expires_in")
    private Long expiresIn;

    /**
     * OPTIONAL. The scope of the access token.
     */
    @JsonProperty("scope")
    private String scope;

    /**
     * Validates the response
     */
    public boolean isValid() {
        return accessToken != null && !accessToken.isEmpty()
            && tokenType != null && !tokenType.isEmpty()
            && expiresIn != null && expiresIn > 0;
    }

    /**
     * Get expiration time in milliseconds
     */
    public long getExpirationTimeMs() {
        return System.currentTimeMillis() + (expiresIn * 1000);
    }

    /**
     * Check if token is about to expire (within 5 minutes)
     */
    public boolean isExpiringSoon(int bufferSeconds) {
        long bufferMs = bufferSeconds * 1000L;
        long expiresAtMs = getExpirationTimeMs();
        return expiresAtMs - System.currentTimeMillis() <= bufferMs;
    }
}
