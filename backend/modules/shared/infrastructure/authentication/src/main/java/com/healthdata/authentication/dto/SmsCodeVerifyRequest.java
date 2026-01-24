package com.healthdata.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for SMS code verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsCodeVerifyRequest {

    /**
     * 6-digit SMS verification code.
     */
    @NotBlank(message = "Verification code is required")
    @Pattern(
        regexp = "^\\d{6}$",
        message = "Verification code must be exactly 6 digits"
    )
    private String code;
}
