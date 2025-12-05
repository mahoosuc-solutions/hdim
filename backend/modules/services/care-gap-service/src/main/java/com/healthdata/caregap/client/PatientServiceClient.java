package com.healthdata.caregap.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for Care Gap Service
 *
 * Provides access to patient aggregation, timeline, and health status data
 * for care gap identification and analysis.
 */
@FeignClient(
    name = "patient-service",
    url = "${patient.service.url}"
)
public interface PatientServiceClient {

    /**
     * Get comprehensive patient health record
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return FHIR Bundle with all patient resources
     */
    @GetMapping(value = "/health-record", produces = "application/fhir+json")
    String getComprehensiveHealthRecord(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    /**
     * Get patient allergies
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param onlyCritical Return only critical allergies
     * @return FHIR Bundle with allergies
     */
    @GetMapping(value = "/allergies", produces = "application/fhir+json")
    String getAllergies(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam(value = "onlyCritical", defaultValue = "false") boolean onlyCritical
    );

    /**
     * Get patient immunizations
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param onlyCompleted Return only completed immunizations
     * @return FHIR Bundle with immunizations
     */
    @GetMapping(value = "/immunizations", produces = "application/fhir+json")
    String getImmunizations(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam(value = "onlyCompleted", defaultValue = "false") boolean onlyCompleted
    );

    /**
     * Get patient medications
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param onlyActive Return only active medications
     * @return FHIR Bundle with medications
     */
    @GetMapping(value = "/medications", produces = "application/fhir+json")
    String getMedications(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam(value = "onlyActive", defaultValue = "true") boolean onlyActive
    );

    /**
     * Get patient conditions
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param onlyActive Return only active conditions
     * @return FHIR Bundle with conditions
     */
    @GetMapping(value = "/conditions", produces = "application/fhir+json")
    String getConditions(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam(value = "onlyActive", defaultValue = "true") boolean onlyActive
    );

    /**
     * Get patient procedures
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return FHIR Bundle with procedures
     */
    @GetMapping(value = "/procedures", produces = "application/fhir+json")
    String getProcedures(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    /**
     * Get patient vital signs
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return FHIR Bundle with vital signs
     */
    @GetMapping(value = "/vitals", produces = "application/fhir+json")
    String getVitalSigns(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    /**
     * Get patient lab results
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return FHIR Bundle with lab results
     */
    @GetMapping(value = "/labs", produces = "application/fhir+json")
    String getLabResults(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    /**
     * Get patient encounters
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param onlyActive Return only active encounters
     * @return FHIR Bundle with encounters
     */
    @GetMapping(value = "/encounters", produces = "application/fhir+json")
    String getEncounters(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam(value = "onlyActive", defaultValue = "false") boolean onlyActive
    );

    /**
     * Get health status dashboard
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Health status summary (JSON)
     */
    @GetMapping(value = "/health-status", produces = "application/json")
    String getHealthStatusSummary(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    /**
     * Get medication summary
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Medication summary (JSON)
     */
    @GetMapping(value = "/medication-summary", produces = "application/json")
    String getMedicationSummary(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    /**
     * Get immunization summary
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Immunization summary (JSON)
     */
    @GetMapping(value = "/immunization-summary", produces = "application/json")
    String getImmunizationSummary(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    /**
     * Health check endpoint
     *
     * @return Health status
     */
    @GetMapping(value = "/_health", produces = "application/json")
    String healthCheck();
}
