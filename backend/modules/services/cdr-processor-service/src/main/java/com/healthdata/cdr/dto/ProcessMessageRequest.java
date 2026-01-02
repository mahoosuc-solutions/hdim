package com.healthdata.cdr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for processing a single HL7 message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessMessageRequest {

    /**
     * Tenant ID for multi-tenant support.
     */
    @NotNull(message = "Tenant ID is required")
    private String tenantId;

    /**
     * Raw HL7 v2 message.
     */
    @NotBlank(message = "HL7 message is required")
    private String message;

    /**
     * Convert to FHIR resources.
     */
    private boolean convertToFhir;
}
