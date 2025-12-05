package com.healthdata.ehr.connector.cerner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CernerTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("expires_in")
    private Long expiresIn;
    @JsonProperty("scope")
    private String scope;
    @JsonProperty("refresh_token")
    private String refreshToken;
    private Instant issuedAt;
    
    public boolean isExpired() {
        if (issuedAt == null || expiresIn == null) {
            return true;
        }
        Instant expirationTime = issuedAt.plusSeconds(expiresIn);
        return Instant.now().isAfter(expirationTime.minusSeconds(60));
    }
    
    public String getAuthorizationHeader() {
        return tokenType + " " + accessToken;
    }
}
