package com.healthdata.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for MFA code verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaVerifyRequest {

    /**
     * The TOTP code from the authenticator app.
     * Must be 6 digits.
     */
    @NotBlank(message = "MFA code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "MFA code must be 6 digits")
    private String code;
}
