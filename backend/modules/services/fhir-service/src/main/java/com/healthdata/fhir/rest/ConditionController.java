package com.healthdata.fhir.rest;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.healthdata.fhir.service.ConditionService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@RestController
@RequestMapping("/fhir/Condition")
public class ConditionController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final ConditionService conditionService;

    public ConditionController(ConditionService conditionService) {
        this.conditionService = conditionService;
    }

    /**
     * Create a new Condition resource
     * POST /fhir/Condition
     */
    @PostMapping(consumes = "application/fhir+json", produces = "application/fhir+json")
    public ResponseEntity<String> createCondition(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody String conditionJson) {
        try {
            Condition condition = (Condition) JSON_PARSER.parseResource(conditionJson);
            Condition created = conditionService.createCondition(tenantId, condition, userId);
            String responseJson = JSON_PARSER.encodeResourceToString(created);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Location", "/fhir/Condition/" + created.getId())
                    .body(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Read a Condition resource by ID
     * GET /fhir/Condition/{id}
     */
    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getCondition(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String id) {
        return conditionService.getCondition(tenantId, id)
                .map(condition -> {
                    String responseJson = JSON_PARSER.encodeResourceToString(condition);
                    return ResponseEntity.ok(responseJson);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a Condition resource
     * PUT /fhir/Condition/{id}
     */
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = "application/fhir+json")
    public ResponseEntity<String> updateCondition(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable String id,
            @RequestBody String conditionJson) {
        try {
            Condition condition = (Condition) JSON_PARSER.parseResource(conditionJson);
            Condition updated = conditionService.updateCondition(tenantId, id, condition, userId);
            String responseJson = JSON_PARSER.encodeResourceToString(updated);
            return ResponseEntity.ok(responseJson);
        } catch (ConditionService.ConditionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Delete a Condition resource
     * DELETE /fhir/Condition/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCondition(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable String id) {
        try {
            conditionService.deleteCondition(tenantId, id, userId);
            return ResponseEntity.noContent().build();
        } catch (ConditionService.ConditionNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Search Conditions by patient
     * GET /fhir/Condition?patient={patientId}
     */
    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> searchConditions(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient", required = false) String patientId,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "category", required = false) String category,
            @PageableDefault(size = 20) Pageable pageable) {

        try {
            Bundle bundle;

            if (patientId != null && code != null) {
                // Search by patient and code
                bundle = conditionService.searchConditionsByPatientAndCode(
                        tenantId, patientId, code);
            } else if (patientId != null && category != null) {
                // Search by patient and category
                bundle = conditionService.searchConditionsByPatientAndCategory(
                        tenantId, patientId, category);
            } else if (patientId != null) {
                // Search by patient only (with pagination)
                bundle = conditionService.searchConditionsByPatient(tenantId, patientId, pageable);
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
     * Get active conditions for a patient
     * GET /fhir/Condition/active?patient={patientId}
     */
    @GetMapping(value = "/active", produces = "application/fhir+json")
    public ResponseEntity<String> getActiveConditions(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = conditionService.getActiveConditionsByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get chronic conditions for a patient
     * GET /fhir/Condition/chronic?patient={patientId}
     */
    @GetMapping(value = "/chronic", produces = "application/fhir+json")
    public ResponseEntity<String> getChronicConditions(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = conditionService.getChronicConditionsByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get diagnoses for a patient (encounter-diagnosis category)
     * GET /fhir/Condition/diagnoses?patient={patientId}
     */
    @GetMapping(value = "/diagnoses", produces = "application/fhir+json")
    public ResponseEntity<String> getDiagnoses(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = conditionService.getDiagnosesByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get problem list for a patient
     * GET /fhir/Condition/problem-list?patient={patientId}
     */
    @GetMapping(value = "/problem-list", produces = "application/fhir+json")
    public ResponseEntity<String> getProblemList(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = conditionService.getProblemListByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Check if patient has an active condition by code
     * GET /fhir/Condition/has-condition?patient={patientId}&code={code}
     */
    @GetMapping(value = "/has-condition", produces = "application/json")
    public ResponseEntity<String> hasActiveCondition(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam("code") String code) {
        try {
            boolean hasCondition = conditionService.hasActiveCondition(tenantId, patientId, code);
            return ResponseEntity.ok("{\"hasCondition\": " + hasCondition + "}");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Health check endpoint
     * GET /fhir/Condition/_health
     */
    @GetMapping("/_health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("{\"status\": \"UP\", \"service\": \"Condition\"}");
    }
}
