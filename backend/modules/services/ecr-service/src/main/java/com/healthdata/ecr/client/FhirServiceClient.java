package com.healthdata.ecr.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for FHIR Service
 *
 * Provides access to FHIR resources for eICR generation including:
 * - Encounter details
 * - Conditions (diagnoses)
 * - Observations (lab results)
 * - MedicationRequests
 * - AllergyIntolerances
 */
@FeignClient(
    name = "fhir-service",
    url = "${services.fhir-service.url}",
    configuration = FhirServiceClientConfiguration.class
)
public interface FhirServiceClient {

    // ==================== Encounter ====================

    /**
     * Get a specific encounter by ID
     *
     * @param tenantId Tenant identifier
     * @param encounterId Encounter identifier
     * @return FHIR Encounter resource as JSON string
     */
    @GetMapping("/fhir/Encounter/{encounterId}")
    String getEncounter(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable("encounterId") String encounterId
    );

    /**
     * Get encounters for a patient
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return FHIR Bundle with encounters
     */
    @GetMapping("/fhir/Encounter")
    String getEncounters(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    // ==================== Condition (Diagnoses) ====================

    /**
     * Get all conditions for a patient
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return FHIR Bundle with conditions
     */
    @GetMapping("/fhir/Condition")
    String getConditions(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    /**
     * Get active conditions for a patient
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return FHIR Bundle with active conditions
     */
    @GetMapping("/fhir/Condition/active")
    String getActiveConditions(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    // ==================== Observation (Lab Results) ====================

    /**
     * Get all observations for a patient
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return FHIR Bundle with observations
     */
    @GetMapping("/fhir/Observation")
    String getObservations(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    /**
     * Get laboratory results for a patient
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return FHIR Bundle with lab observations
     */
    @GetMapping("/fhir/Observation/laboratory")
    String getLabResults(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    /**
     * Get vital signs for a patient
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return FHIR Bundle with vital sign observations
     */
    @GetMapping("/fhir/Observation/vital-signs")
    String getVitalSigns(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    // ==================== MedicationRequest ====================

    /**
     * Get medication requests for a patient
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return FHIR Bundle with medication requests
     */
    @GetMapping("/fhir/MedicationRequest")
    String getMedicationRequests(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    /**
     * Get active medications for a patient
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return FHIR Bundle with active medication requests
     */
    @GetMapping("/fhir/MedicationRequest/active")
    String getActiveMedications(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    // ==================== AllergyIntolerance ====================

    /**
     * Get allergy intolerances for a patient
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return FHIR Bundle with allergy intolerances
     */
    @GetMapping("/fhir/AllergyIntolerance")
    String getAllergyIntolerances(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    /**
     * Get active allergies for a patient
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return FHIR Bundle with active allergy intolerances
     */
    @GetMapping("/fhir/AllergyIntolerance/active")
    String getActiveAllergies(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    // ==================== Health Check ====================

    /**
     * Health check for FHIR service
     *
     * @return Health status
     */
    @GetMapping("/fhir/_health")
    String healthCheck();
}
