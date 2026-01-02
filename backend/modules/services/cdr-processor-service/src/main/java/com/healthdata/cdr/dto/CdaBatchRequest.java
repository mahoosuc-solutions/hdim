package com.healthdata.cdr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for batch processing multiple CDA documents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdaBatchRequest {

    /**
     * Tenant ID for multi-tenant support.
     */
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    /**
     * List of CDA documents to process (raw XML or Base64 encoded).
     */
    @NotEmpty(message = "At least one document is required")
    private List<String> documents;

    /**
     * Whether the documents are Base64 encoded.
     */
    @Builder.Default
    private boolean base64Encoded = false;

    /**
     * Whether to convert parsed data to FHIR resources.
     */
    @Builder.Default
    private boolean convertToFhir = true;

    /**
     * Whether to validate documents against C-CDA schemas.
     */
    @Builder.Default
    private boolean validateDocuments = true;

    /**
     * Source system identifier (for tracking origin).
     */
    private String sourceSystem;

    /**
     * Continue processing on individual document errors.
     */
    @Builder.Default
    private boolean continueOnError = true;
}
