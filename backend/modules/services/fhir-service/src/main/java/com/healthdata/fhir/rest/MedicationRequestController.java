package com.healthdata.fhir.rest;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.healthdata.fhir.service.MedicationRequestService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

@RestController
@RequestMapping("/MedicationRequest")
public class MedicationRequestController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final MedicationRequestService medicationRequestService;

    public MedicationRequestController(MedicationRequestService medicationRequestService) {
        this.medicationRequestService = medicationRequestService;
    }

    /**
     * Create a new MedicationRequest resource
     * POST /fhir/MedicationRequest
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_WRITE\')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> createMedicationRequest(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody String medicationRequestJson) {
        try {
            MedicationRequest medicationRequest = (MedicationRequest) JSON_PARSER.parseResource(medicationRequestJson);
            MedicationRequest created = medicationRequestService.createMedicationRequest(
                    tenantId, medicationRequest, userId);
            String responseJson = JSON_PARSER.encodeResourceToString(created);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Location", "/fhir/MedicationRequest/" + created.getId())
                    .body(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Read a MedicationRequest resource by ID
     * GET /fhir/MedicationRequest/{id}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{id}", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getMedicationRequest(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String id) {
        return medicationRequestService.getMedicationRequest(tenantId, id)
                .map(medicationRequest -> {
                    String responseJson = JSON_PARSER.encodeResourceToString(medicationRequest);
                    return ResponseEntity.ok(responseJson);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a MedicationRequest resource
     * PUT /fhir/MedicationRequest/{id}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_WRITE\')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> updateMedicationRequest(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable String id,
            @RequestBody String medicationRequestJson) {
        try {
            MedicationRequest medicationRequest = (MedicationRequest) JSON_PARSER.parseResource(medicationRequestJson);
            MedicationRequest updated = medicationRequestService.updateMedicationRequest(
                    tenantId, id, medicationRequest, userId);
            String responseJson = JSON_PARSER.encodeResourceToString(updated);
            return ResponseEntity.ok(responseJson);
        } catch (MedicationRequestService.MedicationRequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Delete a MedicationRequest resource
     * DELETE /fhir/MedicationRequest/{id}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_WRITE\')")
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicationRequest(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable String id) {
        try {
            medicationRequestService.deleteMedicationRequest(tenantId, id, userId);
            return ResponseEntity.noContent().build();
        } catch (MedicationRequestService.MedicationRequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Search MedicationRequests by patient
     * GET /fhir/MedicationRequest?patient={patientId}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> searchMedicationRequests(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient", required = false) String patientId,
            @RequestParam(value = "code", required = false) String code,
            @PageableDefault(size = 20) Pageable pageable) {

        try {
            Bundle bundle;

            if (patientId != null && code != null) {
                // Search by patient and medication code
                bundle = medicationRequestService.searchMedicationRequestsByPatientAndCode(
                        tenantId, patientId, code);
            } else if (patientId != null) {
                // Search by patient only (with pagination)
                bundle = medicationRequestService.searchMedicationRequestsByPatient(
                        tenantId, patientId, pageable);
            } else {
                return ResponseEntity.badRequest()
                        .body("{\"error\": \"patient parameter is required\"}");
            }

            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get active medication requests for a patient
     * GET /fhir/MedicationRequest/active?patient={patientId}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/active", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getActiveMedicationRequests(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = medicationRequestService.getActiveRequestsByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get prescriptions for a patient (intent=order)
     * GET /fhir/MedicationRequest/prescriptions?patient={patientId}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/prescriptions", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getPrescriptions(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = medicationRequestService.getPrescriptionsByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get medication requests with refills remaining
     * GET /fhir/MedicationRequest/with-refills?patient={patientId}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/with-refills", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getRequestsWithRefills(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = medicationRequestService.getRequestsWithRefills(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Check if patient has active medication by code
     * GET /fhir/MedicationRequest/has-medication?patient={patientId}&code={code}
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/has-medication", produces = "application/json")
    public ResponseEntity<String> hasActiveMedication(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam("code") String code) {
        try {
            boolean hasMedication = medicationRequestService.hasActiveMedication(
                    tenantId, patientId, code);
            return ResponseEntity.ok("{\"hasMedication\": " + hasMedication + "}");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Health check endpoint
     * GET /fhir/MedicationRequest/_health
     */
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/_health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("{\"status\": \"UP\", \"service\": \"MedicationRequest\"}");
    }
}
