package com.healthdata.cdr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;

import java.util.List;

/**
 * Response DTO for batch CDA document processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdaBatchResponse {

    /**
     * Total number of documents submitted.
     */
    private int totalDocuments;

    /**
     * Number of successfully processed documents.
     */
    private int successCount;

    /**
     * Number of failed documents.
     */
    private int failureCount;

    /**
     * Processing time in milliseconds.
     */
    private long processingTimeMs;

    /**
     * Parsed CDA documents.
     */
    private List<CdaDocument> processedDocuments;

    /**
     * FHIR bundles (if conversion was requested).
     */
    private transient List<Bundle> fhirBundles;

    /**
     * Error messages for failed documents.
     */
    private List<String> errors;
}
