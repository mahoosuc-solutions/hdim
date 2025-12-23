package com.healthdata.quality.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * Feign client for CQL Engine Service
 * Execute CQL quality measures
 */
@FeignClient(name = "cql-engine-service", url = "${cql.engine.url}")
public interface CqlEngineServiceClient {

    @PostMapping(value = "/evaluate", produces = "application/json", consumes = "application/json")
    String evaluateCql(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("library") String libraryName,
        @RequestParam("patient") UUID patientId,
        @RequestBody(required = false) String parameters
    );

    @PostMapping(value = "/evaluate/batch", produces = "application/json", consumes = "application/json")
    String evaluateCqlBatch(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") UUID patientId,
        @RequestBody String request
    );

    @GetMapping(value = "/_health", produces = "application/json")
    String healthCheck();
}
