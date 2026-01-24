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
import org.springframework.security.access.prepost.PreAuthorize;

import com.healthdata.fhir.service.EncounterService;
import com.healthdata.fhir.util.FhirDateRangeParser;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Encounter", description = "Healthcare encounter/visit information (HL7 FHIR R4)")
@RestController
@RequestMapping("/Encounter")
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
    @Operation(summary = "Create Encounter", description = "Creates a new FHIR R4 Encounter resource.\n\nUse for documenting patient visits, appointments, and healthcare interactions.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Encounter created", content = @Content(mediaType = "application/fhir+json")), @ApiResponse(responseCode = "400", description = "Invalid FHIR resource")})
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> createEncounter(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User ID") @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "FHIR Encounter resource (JSON)") @RequestBody String encounterJson) {
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
    @Operation(summary = "Read Encounter by ID", description = "Retrieves a specific Encounter resource.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Encounter found", content = @Content(mediaType = "application/fhir+json")), @ApiResponse(responseCode = "404", description = "Encounter not found")})
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{id}", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getEncounter(
            @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Encounter ID", required = true, example = "123e4567") @PathVariable String id) {
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
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
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
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
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
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> searchEncounters(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient", required = false) String patientId,
            @RequestParam(value = "date", required = false) java.util.List<String> dateParams,
            @RequestParam(value = "date-start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateStart,
            @RequestParam(value = "date-end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateEnd,
            @PageableDefault(size = 20) Pageable pageable) {

        try {
            if (patientId == null) {
                FhirDateRangeParser.DateRange range = FhirDateRangeParser.parseDateRange(dateParams);
                if (range != null) {
                    Bundle bundle = encounterService.searchEncountersByDateRange(
                            tenantId, range.start(), range.end());
                    String responseJson = JSON_PARSER.encodeResourceToString(bundle);
                    return ResponseEntity.ok(responseJson);
                }
                return ResponseEntity.badRequest()
                        .body("{\"error\": \"patient parameter is required\"}");
            }

            Bundle bundle;

            if (dateStart != null && dateEnd != null) {
                // Search by patient and date range
                bundle = encounterService.searchEncountersByPatientAndDateRange(
                        tenantId, patientId, dateStart, dateEnd);
            } else {
                FhirDateRangeParser.DateRange range = FhirDateRangeParser.parseDateRange(dateParams);
                if (range != null) {
                    bundle = encounterService.searchEncountersByPatientAndDateRange(
                            tenantId, patientId, range.start(), range.end());
                } else {
                    // Search by patient only (with pagination)
                    bundle = encounterService.searchEncountersByPatient(
                            tenantId, patientId, pageable);
                }
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
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/finished", produces = {"application/fhir+json", "application/json"})
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
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/active", produces = {"application/fhir+json", "application/json"})
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
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/inpatient", produces = {"application/fhir+json", "application/json"})
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
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/ambulatory", produces = {"application/fhir+json", "application/json"})
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
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/emergency", produces = {"application/fhir+json", "application/json"})
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
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
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
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
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
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
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
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/_health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("{\"status\": \"UP\", \"service\": \"Encounter\"}");
    }
}
