package com.healthdata.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for SMS code send request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaSmsSendResponse {

    /**
     * Masked phone number (e.g., ****1234).
     */
    private String phoneNumber;

    /**
     * Success message.
     */
    private String message;
}
