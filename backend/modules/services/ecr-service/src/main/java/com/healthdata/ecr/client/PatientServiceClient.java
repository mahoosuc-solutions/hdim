package com.healthdata.ecr.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for Patient Service
 *
 * Provides access to patient data aggregation endpoints including:
 * - Comprehensive health records
 * - Allergies, medications, conditions
 * - Encounters and care plans
 * - Health status summaries
 */
@FeignClient(
    name = "patient-service",
    url = "${services.patient-service.url}",
    configuration = PatientServiceClientConfiguration.class
)
public interface PatientServiceClient {

    /**
     * Get comprehensive patient health record as FHIR Bundle
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return FHIR Bundle JSON string with all patient resources
     */
    @GetMapping("/patient/health-record")
    String getComprehensiveHealthRecord(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    /**
     * Get patient allergies as FHIR Bundle
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @param onlyCritical Return only critical allergies
     * @return FHIR Bundle JSON string with allergies
     */
    @GetMapping("/patient/allergies")
    String getAllergies(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam(value = "onlyCritical", defaultValue = "false") boolean onlyCritical
    );

    /**
     * Get patient medications as FHIR Bundle
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @param onlyActive Return only active medications
     * @return FHIR Bundle JSON string with medications
     */
    @GetMapping("/patient/medications")
    String getMedications(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam(value = "onlyActive", defaultValue = "true") boolean onlyActive
    );

    /**
     * Get patient conditions as FHIR Bundle
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @param onlyActive Return only active conditions
     * @return FHIR Bundle JSON string with conditions
     */
    @GetMapping("/patient/conditions")
    String getConditions(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam(value = "onlyActive", defaultValue = "true") boolean onlyActive
    );

    /**
     * Get patient encounters as FHIR Bundle
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @param onlyActive Return only active encounters
     * @return FHIR Bundle JSON string with encounters
     */
    @GetMapping("/patient/encounters")
    String getEncounters(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam(value = "onlyActive", defaultValue = "false") boolean onlyActive
    );

    /**
     * Get patient lab results as FHIR Bundle
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return FHIR Bundle JSON string with lab results
     */
    @GetMapping("/patient/labs")
    String getLabResults(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );
}
