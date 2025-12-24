package com.healthdata.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for completing login with MFA.
 * Used in the second step of MFA-protected login.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaLoginRequest {

    /**
     * The MFA token received from the initial login attempt.
     * This is a short-lived token that proves password authentication succeeded.
     */
    @NotBlank(message = "MFA token is required")
    private String mfaToken;

    /**
     * The TOTP code from the authenticator app.
     */
    @NotBlank(message = "MFA code is required")
    private String code;

    /**
     * If true, the code is treated as a recovery code instead of TOTP.
     */
    @Builder.Default
    private boolean useRecoveryCode = false;
}
