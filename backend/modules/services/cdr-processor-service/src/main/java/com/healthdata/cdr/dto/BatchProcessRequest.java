package com.healthdata.cdr.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for batch processing HL7 messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProcessRequest {

    /**
     * Tenant ID for multi-tenant support.
     */
    @NotNull(message = "Tenant ID is required")
    private String tenantId;

    /**
     * List of raw HL7 messages to process.
     */
    @NotEmpty(message = "At least one message is required")
    private List<String> messages;

    /**
     * Convert to FHIR resources.
     */
    private boolean convertToFhir;
}
