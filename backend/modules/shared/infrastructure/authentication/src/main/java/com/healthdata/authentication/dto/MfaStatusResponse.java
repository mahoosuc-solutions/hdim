package com.healthdata.authentication.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for MFA status check.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MfaStatusResponse {

    /**
     * Whether MFA is enabled for this user.
     */
    private boolean mfaEnabled;

    /**
     * When MFA was enabled (if enabled).
     */
    private Instant enabledAt;

    /**
     * Number of remaining recovery codes.
     */
    private Integer remainingRecoveryCodes;
}
