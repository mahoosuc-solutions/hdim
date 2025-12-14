package com.healthdata.fhir.rest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.healthdata.fhir.service.MedicationAdministrationService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * REST Controller for FHIR MedicationAdministration resources.
 *
 * Provides endpoints for managing medication administration records,
 * including those converted from HL7 v2 RAS^O17 messages.
 */
@RestController
@RequestMapping("/fhir/MedicationAdministration")
public class MedicationAdministrationController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final MedicationAdministrationService medicationAdministrationService;

    public MedicationAdministrationController(MedicationAdministrationService medicationAdministrationService) {
        this.medicationAdministrationService = medicationAdministrationService;
    }

    /**
     * Create a new MedicationAdministration resource
     * POST /fhir/MedicationAdministration
     */
    @PostMapping(consumes = "application/fhir+json", produces = "application/fhir+json")
    public ResponseEntity<String> createMedicationAdministration(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody String medicationAdministrationJson) {
        try {
            MedicationAdministration medicationAdministration =
                    (MedicationAdministration) JSON_PARSER.parseResource(medicationAdministrationJson);
            MedicationAdministration created = medicationAdministrationService.createMedicationAdministration(
                    tenantId, medicationAdministration, userId);
            String responseJson = JSON_PARSER.encodeResourceToString(created);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Location", "/fhir/MedicationAdministration/" + created.getId())
                    .body(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Read a MedicationAdministration resource by ID
     * GET /fhir/MedicationAdministration/{id}
     */
    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getMedicationAdministration(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String id) {
        return medicationAdministrationService.getMedicationAdministration(tenantId, id)
                .map(medicationAdministration -> {
                    String responseJson = JSON_PARSER.encodeResourceToString(medicationAdministration);
                    return ResponseEntity.ok(responseJson);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a MedicationAdministration resource
     * PUT /fhir/MedicationAdministration/{id}
     */
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = "application/fhir+json")
    public ResponseEntity<String> updateMedicationAdministration(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable String id,
            @RequestBody String medicationAdministrationJson) {
        try {
            MedicationAdministration medicationAdministration =
                    (MedicationAdministration) JSON_PARSER.parseResource(medicationAdministrationJson);
            MedicationAdministration updated = medicationAdministrationService.updateMedicationAdministration(
                    tenantId, id, medicationAdministration, userId);
            String responseJson = JSON_PARSER.encodeResourceToString(updated);
            return ResponseEntity.ok(responseJson);
        } catch (MedicationAdministrationService.MedicationAdministrationNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Delete a MedicationAdministration resource
     * DELETE /fhir/MedicationAdministration/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicationAdministration(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable String id) {
        try {
            medicationAdministrationService.deleteMedicationAdministration(tenantId, id, userId);
            return ResponseEntity.noContent().build();
        } catch (MedicationAdministrationService.MedicationAdministrationNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Search MedicationAdministrations by patient
     * GET /fhir/MedicationAdministration?patient={patientId}
     */
    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> searchMedicationAdministrations(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient", required = false) String patientId,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "encounter", required = false) String encounterId,
            @PageableDefault(size = 20) Pageable pageable) {

        try {
            Bundle bundle;

            if (encounterId != null) {
                // Search by encounter
                bundle = medicationAdministrationService.searchAdministrationsByEncounter(tenantId, encounterId);
            } else if (patientId != null && code != null) {
                // Search by patient and medication code
                bundle = medicationAdministrationService.searchAdministrationsByPatientAndCode(
                        tenantId, patientId, code);
            } else if (patientId != null) {
                // Search by patient only (with pagination)
                bundle = medicationAdministrationService.searchAdministrationsByPatient(
                        tenantId, patientId, pageable);
            } else {
                return ResponseEntity.badRequest()
                        .body("{\"error\": \"patient or encounter parameter is required\"}");
            }

            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get completed medication administrations for a patient
     * GET /fhir/MedicationAdministration/completed?patient={patientId}
     */
    @GetMapping(value = "/completed", produces = "application/fhir+json")
    public ResponseEntity<String> getCompletedAdministrations(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = medicationAdministrationService.getCompletedAdministrationsByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get in-progress medication administrations for a patient
     * GET /fhir/MedicationAdministration/in-progress?patient={patientId}
     */
    @GetMapping(value = "/in-progress", produces = "application/fhir+json")
    public ResponseEntity<String> getInProgressAdministrations(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = medicationAdministrationService.getInProgressAdministrationsByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get medication administrations within a date range
     * GET /fhir/MedicationAdministration/by-date?patient={patientId}&start={startDate}&end={endDate}
     */
    @GetMapping(value = "/by-date", produces = "application/fhir+json")
    public ResponseEntity<String> getAdministrationsByDateRange(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam("start") String startDate,
            @RequestParam("end") String endDate) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDate, DATE_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endDate, DATE_FORMATTER);
            Bundle bundle = medicationAdministrationService.getAdministrationsByDateRange(
                    tenantId, patientId, start, end);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get administration history for a medication request (prescription)
     * GET /fhir/MedicationAdministration/by-request?request={requestId}
     */
    @GetMapping(value = "/by-request", produces = "application/fhir+json")
    public ResponseEntity<String> getAdministrationsByRequest(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("request") String requestId) {
        try {
            Bundle bundle = medicationAdministrationService.getAdministrationHistoryByRequest(tenantId, requestId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get administrations by lot number (for drug recalls)
     * GET /fhir/MedicationAdministration/by-lot?lot={lotNumber}
     */
    @GetMapping(value = "/by-lot", produces = "application/fhir+json")
    public ResponseEntity<String> getAdministrationsByLotNumber(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("lot") String lotNumber) {
        try {
            Bundle bundle = medicationAdministrationService.getAdministrationsByLotNumber(tenantId, lotNumber);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Check if medication has been administered today
     * GET /fhir/MedicationAdministration/administered-today?patient={patientId}&code={code}
     */
    @GetMapping(value = "/administered-today", produces = "application/json")
    public ResponseEntity<String> hasBeenAdministeredToday(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam("code") String code) {
        try {
            boolean administered = medicationAdministrationService.hasMedicationBeenAdministeredToday(
                    tenantId, patientId, code);
            return ResponseEntity.ok("{\"administeredToday\": " + administered + "}");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Health check endpoint
     * GET /fhir/MedicationAdministration/_health
     */
    @GetMapping("/_health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("{\"status\": \"UP\", \"service\": \"MedicationAdministration\"}");
    }
}
