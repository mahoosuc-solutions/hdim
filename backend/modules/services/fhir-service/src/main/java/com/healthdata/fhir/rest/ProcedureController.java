package com.healthdata.fhir.rest;

import java.time.LocalDate;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Procedure;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.healthdata.fhir.service.ProcedureService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

@RestController
@RequestMapping("/Procedure")
public class ProcedureController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final ProcedureService procedureService;

    public ProcedureController(ProcedureService procedureService) {
        this.procedureService = procedureService;
    }

    /**
     * Create a new Procedure resource
     * POST /fhir/Procedure
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_WRITE\')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> createProcedure(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody String procedureJson) {
        try {
            Procedure procedure = (Procedure) JSON_PARSER.parseResource(procedureJson);
            Procedure created = procedureService.createProcedure(tenantId, procedure, userId);
            String responseJson = JSON_PARSER.encodeResourceToString(created);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Location", "/fhir/Procedure/" + created.getId())
                    .body(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Read a Procedure resource by ID
     * GET /fhir/Procedure/{id}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{id}", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getProcedure(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String id) {
        return procedureService.getProcedure(tenantId, id)
                .map(procedure -> {
                    String responseJson = JSON_PARSER.encodeResourceToString(procedure);
                    return ResponseEntity.ok(responseJson);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a Procedure resource
     * PUT /fhir/Procedure/{id}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_WRITE\')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> updateProcedure(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable String id,
            @RequestBody String procedureJson) {
        try {
            Procedure procedure = (Procedure) JSON_PARSER.parseResource(procedureJson);
            Procedure updated = procedureService.updateProcedure(tenantId, id, procedure, userId);
            String responseJson = JSON_PARSER.encodeResourceToString(updated);
            return ResponseEntity.ok(responseJson);
        } catch (ProcedureService.ProcedureNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Delete a Procedure resource
     * DELETE /fhir/Procedure/{id}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_WRITE\')")
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProcedure(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable String id) {
        try {
            procedureService.deleteProcedure(tenantId, id, userId);
            return ResponseEntity.noContent().build();
        } catch (ProcedureService.ProcedureNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Search Procedures by patient
     * GET /fhir/Procedure?patient={patientId}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> searchProcedures(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient", required = false) String patientId,
            @RequestParam(value = "date-start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateStart,
            @RequestParam(value = "date-end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEnd,
            @PageableDefault(size = 20) Pageable pageable) {

        try {
            if (patientId == null) {
                return ResponseEntity.badRequest()
                        .body("{\"error\": \"patient parameter is required\"}");
            }

            Bundle bundle;

            if (dateStart != null && dateEnd != null) {
                // Search by patient and date range
                bundle = procedureService.searchProceduresByPatientAndDateRange(
                        tenantId, patientId, dateStart, dateEnd);
            } else {
                // Search by patient only (with pagination)
                bundle = procedureService.searchProceduresByPatient(
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
     * Get completed procedures for a patient
     * GET /fhir/Procedure/completed?patient={patientId}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/completed", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getCompletedProcedures(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = procedureService.getCompletedProceduresByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get surgical procedures for a patient
     * GET /fhir/Procedure/surgical?patient={patientId}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/surgical", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getSurgicalProcedures(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = procedureService.getSurgicalProceduresByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get diagnostic procedures for a patient
     * GET /fhir/Procedure/diagnostic?patient={patientId}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/diagnostic", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getDiagnosticProcedures(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = procedureService.getDiagnosticProceduresByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get procedures with complications for a patient
     * GET /fhir/Procedure/with-complications?patient={patientId}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/with-complications", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getProceduresWithComplications(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = procedureService.getProceduresWithComplications(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Check if patient has completed specific procedure
     * GET /fhir/Procedure/has-procedure?patient={patientId}&code={code}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/has-procedure", produces = "application/json")
    public ResponseEntity<String> hasCompletedProcedure(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam("code") String code) {
        try {
            boolean hasProcedure = procedureService.hasCompletedProcedure(
                    tenantId, patientId, code);
            return ResponseEntity.ok("{\"hasProcedure\": " + hasProcedure + "}");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Check if patient has procedure in date range
     * GET /fhir/Procedure/has-procedure-in-range?patient={patientId}&date-start={start}&date-end={end}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/has-procedure-in-range", produces = "application/json")
    public ResponseEntity<String> hasProcedureInDateRange(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam("date-start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateStart,
            @RequestParam("date-end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEnd) {
        try {
            boolean hasProcedure = procedureService.hasProcedureInDateRange(
                    tenantId, patientId, dateStart, dateEnd);
            return ResponseEntity.ok("{\"hasProcedure\": " + hasProcedure + "}");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Health check endpoint
     * GET /fhir/Procedure/_health
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/_health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("{\"status\": \"UP\", \"service\": \"Procedure\"}");
    }
}
