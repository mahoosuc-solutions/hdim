package com.healthdata.cdr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for processing a single CDA document.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdaProcessRequest {

    /**
     * Tenant ID for multi-tenant support.
     */
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    /**
     * CDA document content (raw XML or Base64 encoded).
     */
    @NotBlank(message = "CDA document is required")
    private String document;

    /**
     * Whether the document is Base64 encoded.
     */
    @Builder.Default
    private boolean base64Encoded = false;

    /**
     * Whether to convert parsed data to FHIR resources.
     */
    @Builder.Default
    private boolean convertToFhir = true;

    /**
     * Whether to validate the document against C-CDA schemas.
     */
    @Builder.Default
    private boolean validateDocument = true;

    /**
     * Source system identifier (for tracking origin).
     */
    private String sourceSystem;

    /**
     * Patient identifier hint (to correlate with existing patient).
     */
    private String patientIdentifier;
}
