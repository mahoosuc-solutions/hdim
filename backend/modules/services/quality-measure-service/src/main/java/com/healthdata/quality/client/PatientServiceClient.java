package com.healthdata.quality.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for Patient Service
 * Access patient health data for quality measure calculations
 */
@FeignClient(name = "patient-service", url = "${patient.service.url}")
public interface PatientServiceClient {

    @GetMapping(value = "/health-status", produces = "application/json")
    String getHealthStatusSummary(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping(value = "/immunization-summary", produces = "application/json")
    String getImmunizationSummary(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping(value = "/medication-summary", produces = "application/json")
    String getMedicationSummary(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping(value = "/_health", produces = "application/json")
    String healthCheck();
}
