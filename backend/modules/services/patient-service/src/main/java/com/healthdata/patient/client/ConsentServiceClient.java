package com.healthdata.patient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign client for Consent Service
 *
 * Provides access to consent management for HIPAA 42 CFR Part 2
 * compliance and patient data access controls.
 */
@FeignClient(
    name = "consent-service",
    url = "${consent.server.url}"
)
public interface ConsentServiceClient {

    /**
     * Get patient consent status
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Consent status (active, inactive, expired)
     */
    @GetMapping("/consent/status")
    ConsentStatus getConsentStatus(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    /**
     * Get restricted resource types for patient
     *
     * Returns list of FHIR resource types that should be filtered
     * based on consent rules (e.g., Substance Abuse data under 42 CFR Part 2)
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return List of restricted resource types
     */
    @GetMapping("/consent/restrictions")
    List<String> getRestrictedResourceTypes(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    /**
     * Check if access to specific resource is allowed
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param resourceType FHIR resource type (e.g., "Condition", "MedicationRequest")
     * @param resourceId Resource ID
     * @return true if access allowed, false otherwise
     */
    @GetMapping("/consent/check")
    boolean checkAccess(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam("resourceType") String resourceType,
        @RequestParam("resourceId") String resourceId
    );

    /**
     * Get sensitive categories that require filtering
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return List of sensitive categories (e.g., "substance-abuse", "mental-health")
     */
    @GetMapping("/consent/sensitive-categories")
    List<String> getSensitiveCategories(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    // ==================== Health Check ====================

    @GetMapping("/consent/_health")
    String healthCheck();

    /**
     * Consent status response
     */
    record ConsentStatus(
        String status,          // active, inactive, expired
        String effectiveDate,   // ISO 8601 date
        String expirationDate,  // ISO 8601 date
        boolean hasRestrictions // true if patient has specific restrictions
    ) {}
}
