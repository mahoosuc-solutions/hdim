package com.healthdata.quality.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for Care Gap Service
 * Access care gap data for quality measure reporting
 */
@FeignClient(name = "care-gap-service", url = "${care-gap.service.url}")
public interface CareGapServiceClient {

    @GetMapping(value = "/stats", produces = "application/json")
    String getCareGapStats(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping(value = "/summary", produces = "application/json")
    String getCareGapSummary(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping(value = "/by-category", produces = "application/json")
    String getGapsByCategory(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping(value = "/_health", produces = "application/json")
    String healthCheck();
}
