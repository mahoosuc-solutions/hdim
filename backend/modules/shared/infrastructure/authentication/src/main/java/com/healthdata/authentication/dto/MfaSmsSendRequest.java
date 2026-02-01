package com.healthdata.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending SMS code during login.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaSmsSendRequest {

    /**
     * MFA token from login response.
     */
    @NotBlank(message = "MFA token is required")
    private String mfaToken;
}
