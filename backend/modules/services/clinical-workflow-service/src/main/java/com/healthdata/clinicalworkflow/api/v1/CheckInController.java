package com.healthdata.clinicalworkflow.api.v1;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.clinicalworkflow.api.v1.dto.*;
import com.healthdata.clinicalworkflow.api.v1.mapper.PatientCheckInMapper;
import com.healthdata.clinicalworkflow.application.PatientCheckInService;
import com.healthdata.clinicalworkflow.domain.model.PatientCheckInEntity;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for patient check-in operations
 *
 * Handles patient arrival, check-in workflow, insurance verification,
 * consent collection, and demographics confirmation.
 *
 * Security: All endpoints require authentication and X-Tenant-ID header
 * HIPAA: All operations are audited for compliance
 *
 * @author HDIM Platform Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/check-in")
@RequiredArgsConstructor
@Validated
@Tag(name = "Check-In", description = "Patient check-in and arrival management")
@SecurityRequirement(name = "bearer-jwt")
public class CheckInController {

    private final PatientCheckInService checkInService;
    private final PatientCheckInMapper checkInMapper;

    /**
     * Check in a patient for their appointment
     *
     * Example request:
     * <pre>
     * POST /api/v1/check-in
     * X-Tenant-ID: TENANT001
     * {
     *   "patientId": "PATIENT001",
     *   "appointmentId": "APPT001",
     *   "checkInTime": "2026-01-17T09:30:00",
     *   "insuranceVerified": false,
     *   "consentSigned": false,
     *   "demographicsConfirmed": false,
     *   "checkInMethod": "FRONT_DESK"
     * }
     * </pre>
     */
    @PostMapping
    @Operation(
        summary = "Check in patient",
        description = "Record patient arrival and initiate check-in workflow. Creates initial check-in record with pending verification steps."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Check-in recorded successfully",
            content = @Content(schema = @Schema(implementation = CheckInResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - missing required fields or invalid data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Patient or appointment not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Patient already checked in",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<CheckInResponse> checkInPatient(
            @Parameter(description = "Tenant identifier", required = true, example = "TENANT001")
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User performing check-in", required = false, example = "USER001")
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Check-in details", required = true)
            @Valid @RequestBody CheckInRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(checkInMapper.toCheckInResponse(
                checkInService.checkInPatient(tenantId, request, userId)));
    }

    /**
     * Get check-in details by ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get check-in details",
        description = "Retrieve complete check-in record including all verification statuses"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Check-in found",
            content = @Content(schema = @Schema(implementation = CheckInResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Check-in record not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<CheckInResponse> getCheckIn(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Check-in record ID", required = true)
            @PathVariable UUID id) {

        return ResponseEntity.ok(
            checkInMapper.toCheckInResponse(
                checkInService.getCheckIn(tenantId, id)));
    }

    /**
     * Get today's check-in for a patient
     */
    @GetMapping("/patient/{patientId}/today")
    @Operation(
        summary = "Get patient's check-in for today",
        description = "Retrieve today's check-in record for a specific patient. Returns 404 if patient has not checked in today."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Check-in found for today",
            content = @Content(schema = @Schema(implementation = CheckInResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No check-in found for patient today",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<CheckInResponse> getTodaysCheckIn(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient FHIR ID", required = true)
            @PathVariable String patientId) {

        return ResponseEntity.ok(
            checkInMapper.toCheckInResponse(
                checkInService.getTodaysCheckIn(tenantId, patientId)));
    }

    /**
     * Get check-in history with pagination
     */
    @GetMapping("/history")
    @Operation(
        summary = "Get check-in history",
        description = "Retrieve paginated check-in history with optional date range filtering"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Check-in history retrieved",
            content = @Content(schema = @Schema(implementation = CheckInHistoryResponse.class))
        )
    })
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<CheckInHistoryResponse> getCheckInHistory(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient FHIR ID (optional)")
            @RequestParam(required = false) String patientId,
            @Parameter(description = "Start date (optional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (optional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Pagination parameters")
            @PageableDefault(size = 20) Pageable pageable) {

        Page<PatientCheckInEntity> page = checkInService.getCheckInHistory(
            tenantId, patientId, startDate, endDate, pageable);

        return ResponseEntity.ok(
            checkInMapper.toCheckInHistoryResponse(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize()));
    }

    /**
     * Update insurance verification status
     */
    @PutMapping("/{id}/insurance")
    @Operation(
        summary = "Verify insurance",
        description = "Update insurance verification status and details for a check-in record"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Insurance verification updated",
            content = @Content(schema = @Schema(implementation = CheckInResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Check-in record not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<CheckInResponse> verifyInsurance(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User performing verification", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Check-in record ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Insurance verification details", required = true)
            @Valid @RequestBody InsuranceVerificationRequest request) {

        return ResponseEntity.ok(
            checkInMapper.toCheckInResponse(
                checkInService.verifyInsurance(tenantId, id, request, userId)));
    }

    /**
     * Record patient consent
     */
    @PutMapping("/{id}/consent")
    @Operation(
        summary = "Record consent",
        description = "Update consent status for treatment, HIPAA, or other consent types"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Consent recorded",
            content = @Content(schema = @Schema(implementation = CheckInResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Check-in record not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<CheckInResponse> recordConsent(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User recording consent", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Check-in record ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Consent details", required = true)
            @Valid @RequestBody ConsentRequest request) {

        return ResponseEntity.ok(
            checkInMapper.toCheckInResponse(
                checkInService.recordConsent(tenantId, id, request, userId)));
    }

    /**
     * Update demographics confirmation
     */
    @PutMapping("/{id}/demographics")
    @Operation(
        summary = "Confirm demographics",
        description = "Update demographics confirmation status and record any changes"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Demographics updated",
            content = @Content(schema = @Schema(implementation = CheckInResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Check-in record not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<CheckInResponse> updateDemographics(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User updating demographics", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Check-in record ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Demographics update details", required = true)
            @Valid @RequestBody DemographicsUpdateRequest request) {

        return ResponseEntity.ok(
            checkInMapper.toCheckInResponse(
                checkInService.updateDemographics(tenantId, id, request, userId)));
    }
}
