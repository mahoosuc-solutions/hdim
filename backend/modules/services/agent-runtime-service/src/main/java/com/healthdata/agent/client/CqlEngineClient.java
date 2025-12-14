package com.healthdata.agent.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign client for CQL Engine Service communication.
 */
@FeignClient(
    name = "cql-engine-service",
    url = "${hdim.services.cql.url:http://cql-engine-service:8080}"
)
public interface CqlEngineClient {

    /**
     * Evaluate a quality measure for a patient.
     */
    @PostMapping("/api/v1/cql/{tenantId}/evaluate")
    Object evaluateMeasure(
        @PathVariable("tenantId") String tenantId,
        @RequestParam("measureId") String measureId,
        @RequestParam(value = "patientId", required = false) String patientId,
        @RequestBody Map<String, Object> measurementPeriod,
        @RequestParam Map<String, Object> parameters,
        @RequestParam(value = "includeDetails", defaultValue = "true") boolean includeDetails
    );

    /**
     * Execute an ad-hoc CQL expression.
     */
    @PostMapping("/api/v1/cql/{tenantId}/execute")
    Object executeExpression(
        @PathVariable("tenantId") String tenantId,
        @RequestBody String expression,
        @RequestParam(value = "patientId", required = false) String patientId,
        @RequestParam Map<String, Object> parameters
    );

    /**
     * List available measures.
     */
    @GetMapping("/api/v1/cql/{tenantId}/measures")
    Object listMeasures(
        @PathVariable("tenantId") String tenantId
    );

    /**
     * Get measure definition.
     */
    @GetMapping("/api/v1/cql/{tenantId}/measures/{measureId}")
    Object getMeasure(
        @PathVariable("tenantId") String tenantId,
        @PathVariable("measureId") String measureId
    );

    /**
     * Evaluate multiple measures in batch.
     */
    @PostMapping("/api/v1/cql/{tenantId}/batch-evaluate")
    Object batchEvaluate(
        @PathVariable("tenantId") String tenantId,
        @RequestBody Object request
    );
}
