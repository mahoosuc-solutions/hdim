package com.healthdata.enrichment.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Request for code validation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeValidationRequest {
    @NotBlank(message = "Code cannot be empty")
    private String code;

    private String version;
    private String effectiveDate;
}
