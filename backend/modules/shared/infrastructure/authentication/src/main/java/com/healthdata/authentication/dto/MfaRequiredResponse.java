package com.healthdata.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO when MFA is required to complete login.
 * Returned instead of JWT tokens when MFA is enabled.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaRequiredResponse {

    /**
     * Indicates that MFA verification is required.
     */
    @Builder.Default
    private boolean mfaRequired = true;

    /**
     * Temporary token that proves password authentication succeeded.
     * Must be provided with MFA code to complete login.
     * Valid for 5 minutes.
     */
    private String mfaToken;

    /**
     * Message for the client.
     */
    @Builder.Default
    private String message = "MFA verification required. Please provide your authenticator code.";
}
