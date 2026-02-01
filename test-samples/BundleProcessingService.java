package com.hdim.fhir.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

/**
 * Test FHIR bundle processing service with intentional violations.
 * This file tests the FHIR Agent validation capabilities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BundleProcessingService {

    private final FhirContext fhirContext;
    private final IParser fhirParser;
    private final FhirResourceRepository repository;

    // VIOLATION 1: Missing @Transactional annotation on transaction bundle processing
    // Transaction bundles REQUIRE @Transactional for atomicity and rollback
    public Bundle processTransactionBundle(Bundle bundle, String tenantId) {
        if (!Bundle.BundleType.TRANSACTION.equals(bundle.getType())) {
            throw new IllegalArgumentException("Expected transaction bundle");
        }

        Bundle response = new Bundle();
        response.setType(Bundle.BundleType.TRANSACTIONRESPONSE);

        // Process all entries - if any fail, partial data may be committed (VIOLATION!)
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Bundle.BundleEntryComponent responseEntry = processEntry(entry, tenantId);
            response.addEntry(responseEntry);
        }

        return response;
    }

    // Correct example for comparison - uses @Transactional
    @Transactional
    public Bundle processTransactionBundleCorrect(Bundle bundle, String tenantId) {
        if (!Bundle.BundleType.TRANSACTION.equals(bundle.getType())) {
            throw new IllegalArgumentException("Expected transaction bundle");
        }

        try {
            Bundle response = new Bundle();
            response.setType(Bundle.BundleType.TRANSACTIONRESPONSE);

            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                Bundle.BundleEntryComponent responseEntry = processEntry(entry, tenantId);
                response.addEntry(responseEntry);
            }

            return response;
        } catch (Exception e) {
            // Rollback handled automatically by @Transactional
            log.error("Transaction bundle processing failed: {}", e.getMessage());
            throw new BundleProcessingException("Transaction failed", e);
        }
    }

    // Batch bundle processing (correct - no @Transactional needed)
    public Bundle processBatchBundle(Bundle bundle, String tenantId) {
        Bundle response = new Bundle();
        response.setType(Bundle.BundleType.BATCHRESPONSE);

        // Each entry executes independently
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            try {
                Bundle.BundleEntryComponent responseEntry = processEntry(entry, tenantId);
                response.addEntry(responseEntry);
            } catch (Exception e) {
                // Individual entry failure doesn't affect others
                Bundle.BundleEntryComponent errorEntry = new Bundle.BundleEntryComponent();
                errorEntry.getResponse()
                    .setStatus("400 Bad Request")
                    .setOutcome(createOperationOutcome(e.getMessage()));
                response.addEntry(errorEntry);
            }
        }

        return response;
    }

    private Bundle.BundleEntryComponent processEntry(Bundle.BundleEntryComponent entry, String tenantId) {
        // Entry processing logic
        return entry;
    }

    private OperationOutcome createOperationOutcome(String message) {
        // Create FHIR OperationOutcome
        return new OperationOutcome();
    }
}
