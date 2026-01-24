package com.healthdata.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for SMS MFA setup.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsMfaSetupRequest {

    /**
     * Phone number in E.164 format (+15555551234).
     */
    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^\\+[1-9]\\d{1,14}$",
        message = "Phone number must be in E.164 format (e.g., +15555551234)"
    )
    private String phoneNumber;
}
