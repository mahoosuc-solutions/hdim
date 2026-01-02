package com.healthdata.migration.client;

import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;

import com.healthdata.cdr.dto.CdaProcessResponse;
import com.healthdata.cdr.dto.ProcessMessageResponse;

import lombok.Builder;
import lombok.Data;

/**
 * Result of processing a record through CDR Processor Service.
 */
@Data
@Builder
public class CdrProcessingResult {

    /**
     * Whether processing was successful.
     */
    private boolean success;

    /**
     * Error message if processing failed.
     */
    private String errorMessage;

    /**
     * Map of FHIR resource types to counts created.
     */
    @Builder.Default
    private Map<String, Long> resourceCounts = new HashMap<>();

    /**
     * The FHIR bundle created (if conversion was requested).
     */
    private transient Bundle fhirBundle;

    /**
     * Processing time in milliseconds.
     */
    private long processingTimeMs;

    /**
     * Create a successful result from an HL7 response.
     */
    public static CdrProcessingResult fromHl7Response(ProcessMessageResponse response) {
        if (response == null || !response.isSuccess()) {
            return failure(response != null ? response.getErrorMessage() : "Null response from CDR Processor");
        }

        Map<String, Long> counts = new HashMap<>();
        Bundle bundle = response.getFhirBundle();
        if (bundle != null && bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource()) {
                    String resourceType = entry.getResource().fhirType();
                    counts.merge(resourceType, 1L, Long::sum);
                }
            }
        }

        return CdrProcessingResult.builder()
                .success(true)
                .resourceCounts(counts)
                .fhirBundle(bundle)
                .processingTimeMs(response.getProcessingTimeMs())
                .build();
    }

    /**
     * Create a successful result from a CDA response.
     */
    public static CdrProcessingResult fromCdaResponse(CdaProcessResponse response) {
        if (response == null || !response.isSuccess()) {
            return failure(response != null ? response.getErrorMessage() : "Null response from CDR Processor");
        }

        Map<String, Long> counts = new HashMap<>();
        Bundle bundle = response.getFhirBundle();
        if (bundle != null && bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource()) {
                    String resourceType = entry.getResource().fhirType();
                    counts.merge(resourceType, 1L, Long::sum);
                }
            }
        }

        return CdrProcessingResult.builder()
                .success(true)
                .resourceCounts(counts)
                .fhirBundle(bundle)
                .processingTimeMs(response.getProcessingTimeMs())
                .build();
    }

    /**
     * Create a successful result with a single resource type.
     */
    public static CdrProcessingResult success(String resourceType, long count) {
        Map<String, Long> counts = new HashMap<>();
        counts.put(resourceType, count);
        return CdrProcessingResult.builder()
                .success(true)
                .resourceCounts(counts)
                .build();
    }

    /**
     * Create a failure result.
     */
    public static CdrProcessingResult failure(String errorMessage) {
        return CdrProcessingResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
