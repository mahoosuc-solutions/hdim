package com.healthdata.cms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OAuth2 Token Request DTO
 * 
 * Represents the client credentials grant request to CMS OAuth2 endpoint.
 * According to RFC 6749 Section 4.4 (Client Credentials Grant)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuth2TokenRequest {

    /**
     * REQUIRED. Value MUST be set to "client_credentials".
     */
    @JsonProperty("grant_type")
    private String grantType;

    /**
     * REQUIRED. The client identifier issued to the client during the registration process.
     */
    @JsonProperty("client_id")
    private String clientId;

    /**
     * REQUIRED. The client secret.
     */
    @JsonProperty("client_secret")
    private String clientSecret;

    /**
     * OPTIONAL. The scope of the access request.
     * Space-separated list of scopes.
     */
    @JsonProperty("scope")
    private String scope;

    /**
     * Create a client credentials token request
     */
    public static OAuth2TokenRequest clientCredentials(String clientId, String clientSecret, String scope) {
        return OAuth2TokenRequest.builder()
            .grantType("client_credentials")
            .clientId(clientId)
            .clientSecret(clientSecret)
            .scope(scope)
            .build();
    }

    /**
     * Validate request before sending
     */
    public boolean isValid() {
        return "client_credentials".equals(grantType)
            && clientId != null && !clientId.isEmpty()
            && clientSecret != null && !clientSecret.isEmpty();
    }
}
