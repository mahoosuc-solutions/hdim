package com.healthdata.cdr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;

/**
 * Response DTO for single CDA document processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdaProcessResponse {

    /**
     * Parsed CDA document with extracted data.
     */
    private CdaDocument parsedDocument;

    /**
     * FHIR bundle (if conversion was requested).
     */
    private transient Bundle fhirBundle;

    /**
     * Whether processing was successful.
     */
    private boolean success;

    /**
     * Error message if processing failed.
     */
    private String errorMessage;

    /**
     * Processing time in milliseconds.
     */
    private long processingTimeMs;
}
