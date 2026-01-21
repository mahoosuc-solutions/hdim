package com.healthdata.clinicalworkflow.api.v1;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.clinicalworkflow.api.v1.dto.*;
import com.healthdata.clinicalworkflow.api.v1.mapper.WaitingQueueMapper;
import com.healthdata.clinicalworkflow.application.WaitingQueueService;
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
import java.util.Map;

/**
 * REST Controller for patient waiting queue management
 *
 * Handles patient queue operations across different workflow stages:
 * - CHECK_IN: Patients waiting to check in
 * - VITALS: Patients waiting for vital signs
 * - PROVIDER: Patients waiting to see provider
 * - CHECKOUT: Patients waiting to check out
 *
 * Security: All endpoints require authentication and X-Tenant-ID header
 * HIPAA: All operations are audited for compliance
 *
 * @author HDIM Platform Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
@Validated
@Tag(name = "Queue Management", description = "Patient waiting queue tracking")
@SecurityRequirement(name = "bearer-jwt")
public class QueueController {

    private final WaitingQueueService queueService;
    private final WaitingQueueMapper queueMapper;

    /**
     * Get current queue status across all queue types
     */
    @GetMapping
    @Operation(
        summary = "Get queue status",
        description = "Retrieve current queue status with all active queue entries and statistics"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Queue status retrieved",
            content = @Content(schema = @Schema(implementation = QueueStatusResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT', 'PROVIDER', 'RECEPTIONIST')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<QueueStatusResponse> getQueueStatus(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {

        QueueStatusResponse response = queueService.getQueueStatus(tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Add patient to queue
     *
     * Example request:
     * <pre>
     * POST /api/v1/queue/entry
     * X-Tenant-ID: TENANT001
     * {
     *   "patientId": "PATIENT001",
     *   "encounterId": "ENC001",
     *   "queueType": "VITALS",
     *   "enteredQueueAt": "2026-01-17T09:40:00",
     *   "priority": "ROUTINE",
     *   "visitType": "Annual Physical",
     *   "providerId": "PROV001"
     * }
     * </pre>
     */
    @PostMapping("/entry")
    @Operation(
        summary = "Add patient to queue",
        description = "Add patient to a specific queue type. Position is determined by priority and arrival time."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Patient added to queue",
            content = @Content(schema = @Schema(implementation = QueuePositionResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Patient already in queue",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT', 'RECEPTIONIST')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<QueuePositionResponse> addToQueue(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User adding to queue", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Queue entry details", required = true)
            @Valid @RequestBody QueueEntryRequest request) {

        QueuePositionResponse response = queueService.addToQueue(tenantId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get patient's queue information
     */
    @GetMapping("/patient/{patientId}")
    @Operation(
        summary = "Get patient queue info",
        description = "Retrieve patient's current queue position and estimated wait time"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Queue position retrieved",
            content = @Content(schema = @Schema(implementation = QueuePositionResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Patient not in any queue",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT', 'PROVIDER', 'RECEPTIONIST')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<QueuePositionResponse> getPatientQueueInfo(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient FHIR ID", required = true)
            @PathVariable String patientId) {

        QueuePositionResponse response = queueService.getPatientQueueInfo(tenantId, patientId);
        return ResponseEntity.ok(response);
    }

    /**
     * Call next patient in queue
     */
    @PutMapping("/patient/{patientId}/call")
    @Operation(
        summary = "Call next patient",
        description = "Call patient from queue and update status to CALLED. Used when ready to see patient."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Patient called",
            content = @Content(schema = @Schema(implementation = QueuePositionResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Patient not in queue",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT', 'PROVIDER')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<QueuePositionResponse> callPatient(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User calling patient", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Patient FHIR ID", required = true)
            @PathVariable String patientId) {

        QueuePositionResponse response = queueService.callPatient(tenantId, patientId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove patient from queue
     */
    @PutMapping("/patient/{patientId}/exit")
    @Operation(
        summary = "Remove patient from queue",
        description = "Remove patient from current queue (e.g., when moving to next workflow stage)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Patient removed from queue"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Patient not in queue",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT', 'PROVIDER')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<Void> removeFromQueue(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User removing from queue", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Patient FHIR ID", required = true)
            @PathVariable String patientId) {

        queueService.removeFromQueue(tenantId, patientId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get estimated wait times by queue type
     */
    @GetMapping("/wait-time")
    @Operation(
        summary = "Get estimated wait times",
        description = "Retrieve estimated wait times for each queue type based on current volumes"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Wait times retrieved",
            content = @Content(schema = @Schema(implementation = QueueWaitTimeResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT', 'RECEPTIONIST')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<QueueWaitTimeResponse> getWaitTimes(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {

        QueueWaitTimeResponse response = queueService.getWaitTimes(tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get queue entries grouped by priority
     */
    @GetMapping("/by-priority")
    @Operation(
        summary = "Get queue by priority",
        description = "Retrieve queue entries grouped by priority level (STAT, URGENT, ROUTINE)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Queue entries grouped by priority"
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT', 'PROVIDER')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<Map<String, List<QueuePositionResponse>>> getQueueByPriority(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Queue type filter (optional)")
            @RequestParam(required = false) String queueType) {

        Map<String, List<QueuePositionResponse>> response = queueService.getQueueByPriority(tenantId, queueType);
        return ResponseEntity.ok(response);
    }

    /**
     * Reorder queue by priority
     */
    @PostMapping("/reorder")
    @Operation(
        summary = "Reorder queue",
        description = "Recalculate queue positions based on priority and wait time. Used after priority changes."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Queue reordered successfully",
            content = @Content(schema = @Schema(implementation = QueueStatusResponse.class))
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<QueueStatusResponse> reorderQueue(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User triggering reorder", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId) {

        QueueStatusResponse response = queueService.reorderQueue(tenantId, userId);
        return ResponseEntity.ok(response);
    }
}
