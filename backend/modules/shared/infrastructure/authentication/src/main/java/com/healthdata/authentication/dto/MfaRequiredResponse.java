package com.healthdata.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
     * Available MFA methods for this user (e.g., ["TOTP", "SMS"]).
     * Client can present options to user.
     */
    private List<String> availableMethods;

    /**
     * Masked phone number if SMS MFA is available (e.g., "****1234").
     */
    private String smsPhoneNumber;

    /**
     * Message for the client.
     */
    @Builder.Default
    private String message = "MFA verification required. Please provide your authenticator code.";
}
