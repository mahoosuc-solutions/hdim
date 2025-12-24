package com.healthdata.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for MFA recovery code verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaRecoveryRequest {

    /**
     * The recovery code.
     * Format: 8 alphanumeric characters, may include dashes.
     */
    @NotBlank(message = "Recovery code is required")
    private String recoveryCode;
}
