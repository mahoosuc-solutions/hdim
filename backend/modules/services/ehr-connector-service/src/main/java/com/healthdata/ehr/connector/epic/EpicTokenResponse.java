package com.healthdata.ehr.connector.epic;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Response model for Epic OAuth2 token endpoint.
 */
public class EpicTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private int expiresIn;

    @JsonProperty("scope")
    private String scope;

    private Instant obtainedAt;

    public EpicTokenResponse() {
        this.obtainedAt = Instant.now();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Instant getObtainedAt() {
        return obtainedAt;
    }

    public void setObtainedAt(Instant obtainedAt) {
        this.obtainedAt = obtainedAt;
    }

    public boolean isExpired() {
        if (obtainedAt == null) {
            return true;
        }
        // Check if token is expired (with 5 minute buffer)
        Instant expiryTime = obtainedAt.plusSeconds(expiresIn - 300);
        return Instant.now().isAfter(expiryTime);
    }
}
