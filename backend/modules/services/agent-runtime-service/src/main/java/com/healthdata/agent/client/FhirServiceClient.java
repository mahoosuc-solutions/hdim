package com.healthdata.agent.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign client for FHIR Service communication.
 */
@FeignClient(
    name = "fhir-service",
    url = "${hdim.services.fhir.url:http://fhir-service:8080}"
)
public interface FhirServiceClient {

    /**
     * Get a specific FHIR resource by ID.
     */
    @GetMapping("/api/v1/fhir/{tenantId}/{resourceType}/{resourceId}")
    Object getResource(
        @PathVariable("tenantId") String tenantId,
        @PathVariable("resourceType") String resourceType,
        @PathVariable("resourceId") String resourceId
    );

    /**
     * Search FHIR resources with parameters.
     */
    @GetMapping("/api/v1/fhir/{tenantId}/{resourceType}")
    Object searchResources(
        @PathVariable("tenantId") String tenantId,
        @PathVariable("resourceType") String resourceType,
        @RequestParam Map<String, String> searchParams
    );

    /**
     * Execute a batch/bundle operation.
     */
    @PostMapping("/api/v1/fhir/{tenantId}")
    Object executeBundle(
        @PathVariable("tenantId") String tenantId,
        @RequestBody Object bundle
    );

    /**
     * Get patient summary.
     */
    @GetMapping("/api/v1/fhir/{tenantId}/Patient/{patientId}/$summary")
    Object getPatientSummary(
        @PathVariable("tenantId") String tenantId,
        @PathVariable("patientId") String patientId
    );
}
