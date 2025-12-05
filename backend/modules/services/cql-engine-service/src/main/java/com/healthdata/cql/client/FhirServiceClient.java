package com.healthdata.cql.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign Client for FHIR Service
 *
 * Provides access to FHIR resources for clinical data retrieval
 * needed for quality measure calculations.
 */
@FeignClient(name = "fhir-service", url = "${fhir.server.url}")
public interface FhirServiceClient {

    /**
     * Get Patient resource by ID
     */
    @GetMapping(value = "/Patient/{id}", produces = "application/fhir+json")
    String getPatient(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable("id") String patientId
    );

    /**
     * Search Observations for a patient
     */
    @GetMapping(value = "/Observation", produces = "application/fhir+json")
    String searchObservations(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam(value = "code", required = false) String code,
        @RequestParam(value = "category", required = false) String category,
        @RequestParam(value = "date", required = false) String date,
        @RequestParam(value = "_count", required = false) Integer count
    );

    /**
     * Search Conditions for a patient
     */
    @GetMapping(value = "/Condition", produces = "application/fhir+json")
    String searchConditions(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam(value = "code", required = false) String code,
        @RequestParam(value = "clinical-status", required = false) String clinicalStatus
    );

    /**
     * Search MedicationRequests for a patient
     */
    @GetMapping(value = "/MedicationRequest", produces = "application/fhir+json")
    String searchMedicationRequests(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam(value = "status", required = false) String status
    );

    /**
     * Search Procedures for a patient
     */
    @GetMapping(value = "/Procedure", produces = "application/fhir+json")
    String searchProcedures(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam(value = "code", required = false) String code,
        @RequestParam(value = "date", required = false) String date
    );

    /**
     * Search Encounters for a patient
     */
    @GetMapping(value = "/Encounter", produces = "application/fhir+json")
    String searchEncounters(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam(value = "date", required = false) String date,
        @RequestParam(value = "type", required = false) String type
    );

    /**
     * Search Immunizations for a patient
     */
    @GetMapping(value = "/Immunization", produces = "application/fhir+json")
    String searchImmunizations(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam(value = "vaccine-code", required = false) String vaccineCode,
        @RequestParam(value = "date", required = false) String date
    );

    /**
     * Search AllergyIntolerances for a patient
     */
    @GetMapping(value = "/AllergyIntolerance", produces = "application/fhir+json")
    String searchAllergyIntolerances(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam("patient") String patientId,
        @RequestParam(value = "clinical-status", required = false) String clinicalStatus
    );
}
