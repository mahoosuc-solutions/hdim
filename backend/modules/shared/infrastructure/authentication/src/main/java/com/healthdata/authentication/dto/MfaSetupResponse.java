package com.healthdata.authentication.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for MFA setup initialization.
 * Contains the secret and QR code for authenticator app configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MfaSetupResponse {

    /**
     * The TOTP secret key (Base32 encoded).
     * Should be securely stored by the user as backup.
     */
    private String secret;

    /**
     * QR code as data URI for scanning with authenticator apps.
     * Format: data:image/png;base64,...
     */
    private String qrCodeDataUri;

    /**
     * The issuer name shown in authenticator apps.
     */
    private String issuer;

    /**
     * The username/account label shown in authenticator apps.
     */
    private String username;
}
