package com.healthdata.clinicalworkflow.api.v1;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.clinicalworkflow.api.v1.dto.*;
import com.healthdata.clinicalworkflow.application.VitalSignsService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for vital signs management
 *
 * Handles recording, retrieval, and monitoring of patient vital signs.
 * Automatically generates alerts for abnormal values.
 *
 * Security: All endpoints require authentication and X-Tenant-ID header
 * HIPAA: All PHI operations are audited for compliance
 *
 * @author HDIM Platform Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/vitals")
@RequiredArgsConstructor
@Validated
@Tag(name = "Vital Signs", description = "Patient vital signs recording and monitoring")
@SecurityRequirement(name = "bearer-jwt")
public class VitalsController {

    private final VitalSignsService vitalsService;

    /**
     * Record vital signs for a patient
     *
     * Example request:
     * <pre>
     * POST /api/v1/vitals
     * X-Tenant-ID: TENANT001
     * {
     *   "patientId": "PATIENT001",
     *   "encounterId": "ENC001",
     *   "measuredAt": "2026-01-17T09:35:00",
     *   "systolicBP": 120,
     *   "diastolicBP": 80,
     *   "heartRate": 72,
     *   "respiratoryRate": 16,
     *   "temperature": 98.6,
     *   "oxygenSaturation": 98,
     *   "weight": 175.5,
     *   "height": 68.0,
     *   "painLevel": 0
     * }
     * </pre>
     */
    @PostMapping
    @Operation(
        summary = "Record vital signs",
        description = "Record patient vital signs. Automatically calculates BMI and generates alerts for abnormal values."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Vital signs recorded successfully",
            content = @Content(schema = @Schema(implementation = VitalSignsResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - missing required fields or values out of range",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Patient or encounter not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<VitalSignsResponse> recordVitalSigns(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User recording vitals", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Vital signs data", required = true)
            @Valid @RequestBody VitalSignsRequest request) {

        VitalSignsResponse response = vitalsService.recordVitalSigns(tenantId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get vital signs record by ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get vital signs record",
        description = "Retrieve complete vital signs record including alerts"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Vital signs found",
            content = @Content(schema = @Schema(implementation = VitalSignsResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Vital signs record not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<VitalSignsResponse> getVitalSigns(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Vital signs record ID", required = true)
            @PathVariable UUID id) {

        VitalSignsResponse response = vitalsService.getVitalSigns(tenantId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get patient's vital signs history
     */
    @GetMapping("/patient/{patientId}/history")
    @Operation(
        summary = "Get patient vitals history",
        description = "Retrieve paginated vital signs history for a patient, ordered by measurement time (newest first)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Vitals history retrieved",
            content = @Content(schema = @Schema(implementation = VitalsHistoryResponse.class))
        )
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<VitalsHistoryResponse> getVitalsHistory(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient FHIR ID", required = true)
            @PathVariable String patientId,
            @Parameter(description = "Pagination parameters")
            @PageableDefault(size = 20) Pageable pageable) {

        VitalsHistoryResponse response = vitalsService.getVitalsHistory(tenantId, patientId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all abnormal vital sign alerts
     */
    @GetMapping("/alerts")
    @Operation(
        summary = "Get all vital alerts",
        description = "Retrieve all active vital sign alerts for abnormal values across all patients"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Alerts retrieved",
            content = @Content(schema = @Schema(implementation = VitalAlertResponse.class))
        )
    })
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<List<VitalAlertResponse>> getVitalAlerts(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Include acknowledged alerts")
            @RequestParam(defaultValue = "false") boolean includeAcknowledged) {

        List<VitalAlertResponse> alerts = vitalsService.getVitalAlerts(tenantId, includeAcknowledged);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get latest vital signs for a patient
     */
    @GetMapping("/patient/{patientId}/latest")
    @Operation(
        summary = "Get latest vital signs",
        description = "Retrieve the most recent vital signs record for a patient"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Latest vitals found",
            content = @Content(schema = @Schema(implementation = VitalSignsResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No vital signs found for patient",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<VitalSignsResponse> getLatestVitals(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient FHIR ID", required = true)
            @PathVariable String patientId) {

        VitalSignsResponse response = vitalsService.getLatestVitals(tenantId, patientId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get critical alerts only
     */
    @GetMapping("/alerts/critical")
    @Operation(
        summary = "Get critical vital alerts",
        description = "Retrieve only critical-severity vital sign alerts requiring immediate attention"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Critical alerts retrieved",
            content = @Content(schema = @Schema(implementation = VitalAlertResponse.class))
        )
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<List<VitalAlertResponse>> getCriticalAlerts(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {

        List<VitalAlertResponse> alerts = vitalsService.getCriticalAlerts(tenantId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Acknowledge a vital sign alert
     */
    @PostMapping("/{id}/acknowledge")
    @Operation(
        summary = "Acknowledge vital alert",
        description = "Mark a vital sign alert as acknowledged by clinical staff"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Alert acknowledged",
            content = @Content(schema = @Schema(implementation = VitalAlertResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Vital signs record not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<VitalAlertResponse> acknowledgeAlert(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User acknowledging alert", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Vital signs record ID", required = true)
            @PathVariable UUID id) {

        VitalAlertResponse response = vitalsService.acknowledgeAlert(tenantId, id, userId);
        return ResponseEntity.ok(response);
    }
}
