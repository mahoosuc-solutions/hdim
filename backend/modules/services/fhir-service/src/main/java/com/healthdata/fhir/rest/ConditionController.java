package com.healthdata.fhir.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

/**
 * FHIR R4 Condition Resource Controller.
 *
 * Provides CRUD and search operations for Condition resources including
 * problems, diagnoses, and chronic conditions.
 */
@RestController
@RequestMapping("/fhir/Condition")
@Tag(name = "Condition", description = "Patient conditions, problems, and diagnoses")
@SecurityRequirement(name = "smart-oauth2")
public class ConditionController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final ConditionService conditionService;

    public ConditionController(ConditionService conditionService) {
        this.conditionService = conditionService;
    }

    @Operation(
        summary = "Create a new Condition",
        description = "Creates a new Condition resource (problem, diagnosis, or health concern).",
        operationId = "createCondition"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Condition created successfully",
            headers = @Header(name = "Location", description = "URL of the created Condition"),
            content = @Content(mediaType = "application/fhir+json")
        ),
        @ApiResponse(responseCode = "400", description = "Invalid Condition resource"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(consumes = "application/fhir+json", produces = "application/fhir+json")
    public ResponseEntity<String> createCondition(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User ID performing the action")
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "FHIR Condition resource in JSON format",
                required = true,
                content = @Content(mediaType = "application/fhir+json")
            )
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

    @Operation(
        summary = "Read a Condition by ID",
        description = "Retrieves a specific Condition resource by its logical ID.",
        operationId = "readCondition"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Condition found", content = @Content(mediaType = "application/fhir+json")),
        @ApiResponse(responseCode = "404", description = "Condition not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getCondition(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Logical ID of the Condition", required = true)
            @PathVariable String id) {
        return conditionService.getCondition(tenantId, id)
                .map(condition -> {
                    String responseJson = JSON_PARSER.encodeResourceToString(condition);
                    return ResponseEntity.ok(responseJson);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Update a Condition",
        description = "Updates an existing Condition resource.",
        operationId = "updateCondition"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Condition updated successfully", content = @Content(mediaType = "application/fhir+json")),
        @ApiResponse(responseCode = "400", description = "Invalid Condition resource"),
        @ApiResponse(responseCode = "404", description = "Condition not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = "application/fhir+json")
    public ResponseEntity<String> updateCondition(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User ID performing the action")
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Logical ID of the Condition to update", required = true)
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated FHIR Condition resource",
                required = true,
                content = @Content(mediaType = "application/fhir+json")
            )
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

    @Operation(
        summary = "Delete a Condition",
        description = "Deletes a Condition resource by its logical ID.",
        operationId = "deleteCondition"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Condition deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Condition not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCondition(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User ID performing the action")
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Logical ID of the Condition to delete", required = true)
            @PathVariable String id) {
        try {
            conditionService.deleteCondition(tenantId, id, userId);
            return ResponseEntity.noContent().build();
        } catch (ConditionService.ConditionNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Search for Conditions",
        description = "Searches for Condition resources. Supports filtering by patient, code, and category.",
        operationId = "searchConditions"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search completed successfully", content = @Content(mediaType = "application/fhir+json")),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> searchConditions(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient reference (required)", example = "Patient/123")
            @RequestParam(value = "patient", required = false) String patientId,
            @Parameter(description = "Condition code (ICD-10 or SNOMED)", example = "E11.9")
            @RequestParam(value = "code", required = false) String code,
            @Parameter(description = "Condition category", example = "encounter-diagnosis")
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

    @Operation(
        summary = "Get active conditions",
        description = "Retrieves all active conditions for a specific patient.",
        operationId = "getActiveConditions"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Active conditions found", content = @Content(mediaType = "application/fhir+json")),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/active", produces = "application/fhir+json")
    public ResponseEntity<String> getActiveConditions(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true)
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

    @Operation(
        summary = "Get chronic conditions",
        description = "Retrieves all chronic conditions for a specific patient (long-term health issues).",
        operationId = "getChronicConditions"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chronic conditions found", content = @Content(mediaType = "application/fhir+json")),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/chronic", produces = "application/fhir+json")
    public ResponseEntity<String> getChronicConditions(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true)
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

    @Operation(
        summary = "Get patient diagnoses",
        description = "Retrieves all encounter diagnoses for a specific patient.",
        operationId = "getDiagnoses"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Diagnoses found", content = @Content(mediaType = "application/fhir+json")),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/diagnoses", produces = "application/fhir+json")
    public ResponseEntity<String> getDiagnoses(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true)
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

    @Operation(
        summary = "Get patient problem list",
        description = "Retrieves the problem list for a specific patient (ongoing health concerns).",
        operationId = "getProblemList"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Problem list found", content = @Content(mediaType = "application/fhir+json")),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/problem-list", produces = "application/fhir+json")
    public ResponseEntity<String> getProblemList(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true)
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

    @Operation(
        summary = "Check if patient has active condition",
        description = "Checks whether a patient has an active condition with the specified code.",
        operationId = "hasActiveCondition"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Check completed", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/has-condition", produces = "application/json")
    public ResponseEntity<String> hasActiveCondition(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true)
            @RequestParam("patient") String patientId,
            @Parameter(description = "Condition code (ICD-10 or SNOMED)", required = true, example = "E11.9")
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
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/_health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("{\"status\": \"UP\", \"service\": \"Condition\"}");
    }
}
