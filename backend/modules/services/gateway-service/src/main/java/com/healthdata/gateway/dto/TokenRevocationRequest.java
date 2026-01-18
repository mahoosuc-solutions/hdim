package com.healthdata.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for token revocation endpoint (Phase 2.0 Team 3.2)
 *
 * Used by POST /api/v1/auth/revoke to revoke a specific token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRevocationRequest {

    /**
     * The token to revoke (access or refresh token)
     * Required for selective revocation
     */
    @NotBlank(message = "Token is required")
    private String token;

    /**
     * Reason for revocation
     * Valid values: LOGOUT, COMPROMISE, ADMIN_REVOKE, PASSWORD_CHANGE
     */
    @NotBlank(message = "Revocation reason is required")
    @Pattern(regexp = "LOGOUT|COMPROMISE|ADMIN_REVOKE|PASSWORD_CHANGE",
        message = "Invalid revocation reason")
    private String reason;
}
