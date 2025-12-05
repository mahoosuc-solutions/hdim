package com.healthdata.caregap.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for CQL Engine Service
 *
 * Provides access to CQL (Clinical Quality Language) rule evaluation
 * for quality measure gap identification and HEDIS measure calculation.
 */
@FeignClient(
    name = "cql-engine-service",
    url = "${cql.engine.url}"
)
public interface CqlEngineServiceClient {

    /**
     * Evaluate CQL library for a patient
     *
     * @param tenantId Tenant ID
     * @param libraryName CQL library name (e.g., "HEDIS_CDC_A1C", "HEDIS_BCS")
     * @param patientId Patient ID
     * @param parameters Additional parameters (JSON)
     * @return CQL evaluation results (JSON)
     */
    @PostMapping(value = "/evaluate", produces = "application/json", consumes = "application/json")
    String evaluateCql(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("library") String libraryName,
        @RequestParam("patient") String patientId,
        @RequestBody(required = false) String parameters
    );

    /**
     * Evaluate multiple CQL libraries for a patient
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param request Batch evaluation request (JSON with library names)
     * @return Batch CQL evaluation results (JSON)
     */
    @PostMapping(value = "/evaluate/batch", produces = "application/json", consumes = "application/json")
    String evaluateCqlBatch(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestBody String request
    );

    /**
     * Get available CQL libraries
     *
     * @return List of available CQL libraries (JSON)
     */
    @GetMapping(value = "/libraries", produces = "application/json")
    String getAvailableLibraries();

    /**
     * Get CQL library details
     *
     * @param libraryName CQL library name
     * @return Library details including expressions and parameters (JSON)
     */
    @GetMapping(value = "/libraries/{libraryName}", produces = "application/json")
    String getLibraryDetails(@PathVariable("libraryName") String libraryName);

    /**
     * Health check endpoint
     *
     * @return Health status
     */
    @GetMapping(value = "/_health", produces = "application/json")
    String healthCheck();
}
