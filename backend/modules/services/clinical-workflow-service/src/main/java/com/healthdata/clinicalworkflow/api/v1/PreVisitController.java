package com.healthdata.clinicalworkflow.api.v1;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.clinicalworkflow.api.v1.dto.*;
import com.healthdata.clinicalworkflow.api.v1.mapper.PreVisitChecklistMapper;
import com.healthdata.clinicalworkflow.application.PreVisitChecklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for pre-visit checklist management
 *
 * Handles creation and management of pre-visit checklists based on appointment types.
 * Tracks completion of required workflow steps before provider can see patient.
 *
 * Standard checklist items include:
 * - Patient check-in
 * - Insurance verification
 * - Consent forms
 * - Vital signs
 * - Medical history review
 * - Medication reconciliation
 *
 * Security: All endpoints require authentication and X-Tenant-ID header
 * HIPAA: All operations are audited for compliance
 *
 * @author HDIM Platform Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/pre-visit")
@RequiredArgsConstructor
@Validated
@Tag(name = "Pre-Visit Checklist", description = "Pre-visit workflow checklist management")
@SecurityRequirement(name = "bearer-jwt")
public class PreVisitController {

    private final PreVisitChecklistService checklistService;
    private final PreVisitChecklistMapper checklistMapper;

    /**
     * Get patient's pre-visit checklist
     */
    @GetMapping("/patient/{patientId}")
    @Operation(
        summary = "Get patient checklist",
        description = "Retrieve current pre-visit checklist for a patient with completion status"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Checklist retrieved",
            content = @Content(schema = @Schema(implementation = ChecklistResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No active checklist found for patient",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT', 'PROVIDER', 'RECEPTIONIST')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<ChecklistResponse> getPatientChecklist(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient FHIR ID", required = true)
            @PathVariable String patientId) {

        return ResponseEntity.ok(
            checklistMapper.toChecklistResponse(
                checklistService.getPatientChecklist(tenantId, patientId)));
    }

    /**
     * Get checklist template by appointment type
     */
    @GetMapping("/type/{appointmentType}")
    @Operation(
        summary = "Get checklist template",
        description = "Retrieve standard checklist template for a specific appointment type"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Template retrieved",
            content = @Content(schema = @Schema(implementation = ChecklistResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No template found for appointment type",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<ChecklistResponse> getChecklistTemplate(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Appointment type", required = true,
                      example = "ANNUAL_PHYSICAL")
            @PathVariable String appointmentType) {

        return ResponseEntity.ok(
            checklistMapper.toChecklistResponse(
                checklistService.getChecklistTemplate(tenantId, appointmentType)));
    }

    /**
     * Create new pre-visit checklist
     *
     * Example request:
     * <pre>
     * POST /api/v1/pre-visit
     * X-Tenant-ID: TENANT001
     * {
     *   "patientId": "PATIENT001",
     *   "encounterId": "ENC001",
     *   "appointmentType": "ANNUAL_PHYSICAL",
     *   "useCustomTemplate": false
     * }
     * </pre>
     */
    @PostMapping
    @Operation(
        summary = "Create checklist",
        description = "Create new pre-visit checklist from template based on appointment type"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Checklist created",
            content = @Content(schema = @Schema(implementation = ChecklistResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Checklist already exists for patient",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT', 'RECEPTIONIST')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<ChecklistResponse> createChecklist(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User creating checklist", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Checklist creation details", required = true)
            @Valid @RequestBody CreateChecklistRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(checklistMapper.toChecklistResponse(
                checklistService.createChecklist(tenantId, request, userId)));
    }

    /**
     * Complete a checklist item
     */
    @PutMapping("/{id}/item")
    @Operation(
        summary = "Complete checklist item",
        description = "Mark a checklist item as complete with optional notes"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Item completed",
            content = @Content(schema = @Schema(implementation = ChecklistResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Checklist or item not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT', 'RECEPTIONIST')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<ChecklistResponse> completeChecklistItem(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User completing item", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Checklist ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Item update details", required = true)
            @Valid @RequestBody ChecklistItemUpdateRequest request) {

        return ResponseEntity.ok(
            checklistMapper.toChecklistResponse(
                checklistService.completeChecklistItem(tenantId, id, request, userId)));
    }

    /**
     * Add custom checklist item
     */
    @PutMapping("/{id}/custom-item")
    @Operation(
        summary = "Add custom item",
        description = "Add a custom checklist item specific to this patient's needs"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Custom item added",
            content = @Content(schema = @Schema(implementation = ChecklistResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Checklist not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'PROVIDER')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<ChecklistResponse> addCustomItem(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User adding custom item", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Checklist ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Custom item details", required = true)
            @Valid @RequestBody CustomChecklistItemRequest request) {

        return ResponseEntity.ok(
            checklistMapper.toChecklistResponse(
                checklistService.addCustomItem(tenantId, id, request, userId)));
    }

    /**
     * Get checklist completion progress
     */
    @GetMapping("/{id}/progress")
    @Operation(
        summary = "Get completion progress",
        description = "Retrieve detailed completion progress for a checklist including category breakdowns"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Progress retrieved",
            content = @Content(schema = @Schema(implementation = ChecklistProgressResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Checklist not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT', 'PROVIDER', 'RECEPTIONIST')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<ChecklistProgressResponse> getChecklistProgress(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Checklist ID", required = true)
            @PathVariable UUID id) {

        return ResponseEntity.ok(
            checklistMapper.toChecklistProgressResponse(
                checklistService.getChecklistProgress(tenantId, id)));
    }

    /**
     * Get incomplete critical items
     */
    @GetMapping("/{id}/critical-items")
    @Operation(
        summary = "Get incomplete critical items",
        description = "Retrieve list of required/critical items that are not yet completed"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Critical items retrieved",
            content = @Content(schema = @Schema(implementation = ChecklistItemResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Checklist not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT', 'PROVIDER')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<List<ChecklistItemResponse>> getIncompleteCriticalItems(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Checklist ID", required = true)
            @PathVariable UUID id) {

        return ResponseEntity.ok(
            checklistService.getIncompleteCriticalItems(tenantId, id).stream()
                .map(checklistMapper::toChecklistItemResponse)
                .toList());
    }
}
