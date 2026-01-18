package com.healthdata.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for token revocation endpoints (Phase 2.0 Team 3.2)
 *
 * Used by:
 * - POST /api/v1/auth/logout
 * - POST /api/v1/auth/revoke
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRevocationResponse {

    /**
     * Status message (logout) or success message (revoke)
     */
    @JsonProperty("message")
    private String message;

    /**
     * Number of tokens revoked (logout only)
     */
    @JsonProperty("tokensRevoked")
    private Integer tokensRevoked;

    /**
     * When the token was revoked (revoke only)
     */
    @JsonProperty("revokedAt")
    private Instant revokedAt;
}
