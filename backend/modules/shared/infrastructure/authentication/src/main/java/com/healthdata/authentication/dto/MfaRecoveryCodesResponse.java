package com.healthdata.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO containing MFA recovery codes.
 * Returned after MFA setup completion or recovery code regeneration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaRecoveryCodesResponse {

    /**
     * List of recovery codes.
     * Each code can only be used once.
     */
    private List<String> recoveryCodes;

    /**
     * Warning message about storing codes securely.
     */
    @Builder.Default
    private String message = "Store these recovery codes in a safe place. " +
        "Each code can only be used once. If you lose access to your authenticator " +
        "and these codes, you will lose access to your account.";
}
