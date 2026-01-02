package com.healthdata.fhir.rest;

import java.time.LocalDateTime;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.healthdata.fhir.service.EncounterService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@RestController
@RequestMapping("/fhir/Encounter")
public class EncounterController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final EncounterService encounterService;

    public EncounterController(EncounterService encounterService) {
        this.encounterService = encounterService;
    }

    /**
     * Create a new Encounter resource
     * POST /fhir/Encounter
     */
    @PostMapping(consumes = "application/fhir+json", produces = "application/fhir+json")
    public ResponseEntity<String> createEncounter(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody String encounterJson) {
        try {
            Encounter encounter = (Encounter) JSON_PARSER.parseResource(encounterJson);
            Encounter created = encounterService.createEncounter(tenantId, encounter, userId);
            String responseJson = JSON_PARSER.encodeResourceToString(created);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Location", "/fhir/Encounter/" + created.getId())
                    .body(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Read an Encounter resource by ID
     * GET /fhir/Encounter/{id}
     */
    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getEncounter(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String id) {
        return encounterService.getEncounter(tenantId, id)
                .map(encounter -> {
                    String responseJson = JSON_PARSER.encodeResourceToString(encounter);
                    return ResponseEntity.ok(responseJson);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an Encounter resource
     * PUT /fhir/Encounter/{id}
     */
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = "application/fhir+json")
    public ResponseEntity<String> updateEncounter(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable String id,
            @RequestBody String encounterJson) {
        try {
            Encounter encounter = (Encounter) JSON_PARSER.parseResource(encounterJson);
            Encounter updated = encounterService.updateEncounter(tenantId, id, encounter, userId);
            String responseJson = JSON_PARSER.encodeResourceToString(updated);
            return ResponseEntity.ok(responseJson);
        } catch (EncounterService.EncounterNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Delete an Encounter resource
     * DELETE /fhir/Encounter/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEncounter(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable String id) {
        try {
            encounterService.deleteEncounter(tenantId, id, userId);
            return ResponseEntity.noContent().build();
        } catch (EncounterService.EncounterNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Search Encounters by patient
     * GET /fhir/Encounter?patient={patientId}
     */
    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> searchEncounters(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient", required = false) String patientId,
            @RequestParam(value = "date-start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateStart,
            @RequestParam(value = "date-end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateEnd,
            @PageableDefault(size = 20) Pageable pageable) {

        try {
            if (patientId == null) {
                return ResponseEntity.badRequest()
                        .body("{\"error\": \"patient parameter is required\"}");
            }

            Bundle bundle;

            if (dateStart != null && dateEnd != null) {
                // Search by patient and date range
                bundle = encounterService.searchEncountersByPatientAndDateRange(
                        tenantId, patientId, dateStart, dateEnd);
            } else {
                // Search by patient only (with pagination)
                bundle = encounterService.searchEncountersByPatient(
                        tenantId, patientId, pageable);
            }

            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get finished encounters for a patient
     * GET /fhir/Encounter/finished?patient={patientId}
     */
    @GetMapping(value = "/finished", produces = "application/fhir+json")
    public ResponseEntity<String> getFinishedEncounters(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = encounterService.getFinishedEncountersByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get active encounters for a patient
     * GET /fhir/Encounter/active?patient={patientId}
     */
    @GetMapping(value = "/active", produces = "application/fhir+json")
    public ResponseEntity<String> getActiveEncounters(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = encounterService.getActiveEncountersByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get inpatient encounters for a patient
     * GET /fhir/Encounter/inpatient?patient={patientId}
     */
    @GetMapping(value = "/inpatient", produces = "application/fhir+json")
    public ResponseEntity<String> getInpatientEncounters(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = encounterService.getInpatientEncountersByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get ambulatory encounters for a patient
     * GET /fhir/Encounter/ambulatory?patient={patientId}
     */
    @GetMapping(value = "/ambulatory", produces = "application/fhir+json")
    public ResponseEntity<String> getAmbulatoryEncounters(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = encounterService.getAmbulatoryEncountersByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get emergency encounters for a patient
     * GET /fhir/Encounter/emergency?patient={patientId}
     */
    @GetMapping(value = "/emergency", produces = "application/fhir+json")
    public ResponseEntity<String> getEmergencyEncounters(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = encounterService.getEmergencyEncountersByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Check if patient has encounter in date range
     * GET /fhir/Encounter/has-encounter?patient={patientId}&date-start={start}&date-end={end}
     */
    @GetMapping(value = "/has-encounter", produces = "application/json")
    public ResponseEntity<String> hasEncounterInDateRange(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam("date-start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateStart,
            @RequestParam("date-end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateEnd) {
        try {
            boolean hasEncounter = encounterService.hasEncounterInDateRange(
                    tenantId, patientId, dateStart, dateEnd);
            return ResponseEntity.ok("{\"hasEncounter\": " + hasEncounter + "}");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Count inpatient encounters in date range (for utilization measures)
     * GET /fhir/Encounter/count-inpatient?patient={patientId}&date-start={start}&date-end={end}
     */
    @GetMapping(value = "/count-inpatient", produces = "application/json")
    public ResponseEntity<String> countInpatientEncounters(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam("date-start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateStart,
            @RequestParam("date-end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateEnd) {
        try {
            long count = encounterService.countInpatientEncounters(
                    tenantId, patientId, dateStart, dateEnd);
            return ResponseEntity.ok("{\"count\": " + count + "}");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Count emergency encounters in date range
     * GET /fhir/Encounter/count-emergency?patient={patientId}&date-start={start}&date-end={end}
     */
    @GetMapping(value = "/count-emergency", produces = "application/json")
    public ResponseEntity<String> countEmergencyEncounters(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam("date-start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateStart,
            @RequestParam("date-end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateEnd) {
        try {
            long count = encounterService.countEmergencyEncounters(
                    tenantId, patientId, dateStart, dateEnd);
            return ResponseEntity.ok("{\"count\": " + count + "}");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Health check endpoint
     * GET /fhir/Encounter/_health
     */
    @GetMapping("/_health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("{\"status\": \"UP\", \"service\": \"Encounter\"}");
    }
}
