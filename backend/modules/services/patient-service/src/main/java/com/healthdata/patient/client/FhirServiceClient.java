package com.healthdata.patient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for FHIR Service
 *
 * Provides access to FHIR resources including AllergyIntolerance, Immunization,
 * MedicationRequest, Condition, Procedure, Observation, Encounter, and more.
 */
@FeignClient(
    name = "fhir-service",
    url = "${fhir.server.url}",
    configuration = FhirServiceClientConfiguration.class
)
public interface FhirServiceClient {

    // ==================== AllergyIntolerance ====================

    @GetMapping("/fhir/AllergyIntolerance")
    String getAllergyIntolerances(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping("/fhir/AllergyIntolerance/active")
    String getActiveAllergies(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping("/fhir/AllergyIntolerance/critical")
    String getCriticalAllergies(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    // ==================== Immunization ====================

    @GetMapping("/fhir/Immunization")
    String getImmunizations(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping("/fhir/Immunization/completed")
    String getCompletedImmunizations(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    // ==================== MedicationRequest ====================

    @GetMapping("/fhir/MedicationRequest")
    String getMedicationRequests(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping("/fhir/MedicationRequest/active")
    String getActiveMedications(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    // ==================== Condition ====================

    @GetMapping("/fhir/Condition")
    String getConditions(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping("/fhir/Condition/active")
    String getActiveConditions(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    // ==================== Procedure ====================

    @GetMapping("/fhir/Procedure")
    String getProcedures(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping("/fhir/Procedure/completed")
    String getCompletedProcedures(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    // ==================== Observation ====================

    @GetMapping("/fhir/Observation")
    String getObservations(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping("/fhir/Observation/vital-signs")
    String getVitalSigns(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping("/fhir/Observation/laboratory")
    String getLabResults(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    // ==================== Encounter ====================

    @GetMapping("/fhir/Encounter")
    String getEncounters(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping("/fhir/Encounter/in-progress")
    String getActiveEncounters(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    // ==================== DiagnosticReport ====================

    @GetMapping("/fhir/DiagnosticReport")
    String getDiagnosticReports(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    // ==================== CarePlan ====================

    @GetMapping("/fhir/CarePlan")
    String getCarePlans(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping("/fhir/CarePlan/active")
    String getActiveCarePlans(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    // ==================== Goal ====================

    @GetMapping("/fhir/Goal")
    String getGoals(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    @GetMapping("/fhir/Goal/active")
    String getActiveGoals(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId
    );

    // ==================== Health Check ====================

    @GetMapping("/fhir/_health")
    String healthCheck();
}
