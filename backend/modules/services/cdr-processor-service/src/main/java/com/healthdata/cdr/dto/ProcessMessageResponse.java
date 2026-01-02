package com.healthdata.cdr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;

/**
 * Response DTO for processing a single HL7 message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessMessageResponse {

    /**
     * Parsed HL7 message.
     */
    private Hl7v2Message parsedMessage;

    /**
     * FHIR bundle (if conversion was requested).
     */
    private Bundle fhirBundle;

    /**
     * Processing time in milliseconds.
     */
    private long processingTimeMs;

    /**
     * Success indicator.
     */
    private boolean success;

    /**
     * Error message if processing failed.
     */
    private String errorMessage;
}
