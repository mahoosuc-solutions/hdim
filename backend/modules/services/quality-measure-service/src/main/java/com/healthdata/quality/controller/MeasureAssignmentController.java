package com.healthdata.quality.controller;

import com.healthdata.quality.persistence.PatientMeasureAssignmentEntity;
import com.healthdata.quality.service.MeasureAssignmentService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Measure Assignment API Controller
 *
 * Manages patient-specific quality measure assignments (manual and automatic).
 *
 * Endpoints:
 * - GET    /quality-measure/patients/{patientId}/measure-assignments - List assignments
 * - POST   /quality-measure/patients/{patientId}/measure-assignments - Create assignment
 * - DELETE /quality-measure/measure-assignments/{assignmentId} - Deactivate assignment
 * - PUT    /quality-measure/measure-assignments/{assignmentId}/dates - Update effective dates
 *
 * All endpoints require X-Tenant-ID header and appropriate role-based authorization.
 */
@RestController
@RequestMapping("/quality-measure")
@RequiredArgsConstructor
@Slf4j
@Validated
public class MeasureAssignmentController {

    private final MeasureAssignmentService assignmentService;

    /**
     * Get active measure assignments for a patient
     *
     * GET /quality-measure/patients/{patientId}/measure-assignments
     */
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/patients/{patientId}/measure-assignments", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PatientMeasureAssignmentEntity>> getPatientAssignments(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable UUID patientId,
            @RequestParam(value = "effectiveDate", required = false) LocalDate effectiveDate
    ) {
        log.info("GET /quality-measure/patients/{}/measure-assignments - tenant: {}", patientId, tenantId);

        List<PatientMeasureAssignmentEntity> assignments;
        if (effectiveDate != null) {
            assignments = assignmentService.getEffectiveAssignments(tenantId, patientId, effectiveDate);
        } else {
            assignments = assignmentService.getActiveAssignments(tenantId, patientId);
        }

        return ResponseEntity.ok(assignments);
    }

    /**
     * Manually assign a measure to a patient
     *
     * POST /quality-measure/patients/{patientId}/measure-assignments
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/patients/{patientId}/measure-assignments", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientMeasureAssignmentEntity> assignMeasure(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @RequestHeader("X-Auth-User-Id") UUID assignedBy,
            @PathVariable UUID patientId,
            @RequestBody @Validated AssignMeasureRequest request
    ) {
        log.info("POST /quality-measure/patients/{}/measure-assignments - tenant: {}, measure: {}",
                patientId, tenantId, request.getMeasureId());

        try {
            PatientMeasureAssignmentEntity assignment = assignmentService.assignMeasure(
                    tenantId,
                    patientId,
                    request.getMeasureId(),
                    assignedBy,
                    request.getAssignmentReason(),
                    request.getEffectiveFrom(),
                    request.getEffectiveUntil(),
                    request.getEligibilityCriteria()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
        } catch (IllegalStateException e) {
            log.warn("Assignment conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * Deactivate a measure assignment
     *
     * DELETE /quality-measure/measure-assignments/{assignmentId}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping(value = "/measure-assignments/{assignmentId}")
    public ResponseEntity<PatientMeasureAssignmentEntity> deactivateAssignment(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @RequestHeader("X-Auth-User-Id") UUID deactivatedBy,
            @PathVariable UUID assignmentId
    ) {
        log.info("DELETE /quality-measure/measure-assignments/{} - tenant: {}", assignmentId, tenantId);

        try {
            PatientMeasureAssignmentEntity assignment = assignmentService.deactivateAssignment(
                    tenantId, assignmentId, deactivatedBy);
            return ResponseEntity.ok(assignment);
        } catch (IllegalArgumentException e) {
            log.warn("Assignment not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update assignment effective dates
     *
     * PUT /quality-measure/measure-assignments/{assignmentId}/dates
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping(value = "/measure-assignments/{assignmentId}/dates", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientMeasureAssignmentEntity> updateEffectiveDates(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable UUID assignmentId,
            @RequestBody @Validated UpdateDatesRequest request
    ) {
        log.info("PUT /quality-measure/measure-assignments/{}/dates - tenant: {}", assignmentId, tenantId);

        try {
            PatientMeasureAssignmentEntity assignment = assignmentService.updateEffectiveDates(
                    tenantId, assignmentId, request.getEffectiveFrom(), request.getEffectiveUntil());
            return ResponseEntity.ok(assignment);
        } catch (IllegalArgumentException e) {
            log.warn("Assignment not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Count active assignments for a patient
     *
     * GET /quality-measure/patients/{patientId}/measure-assignments/count
     */
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/patients/{patientId}/measure-assignments/count")
    public ResponseEntity<CountResponse> countAssignments(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable UUID patientId
    ) {
        log.info("GET /quality-measure/patients/{}/measure-assignments/count - tenant: {}", patientId, tenantId);

        long count = assignmentService.countActiveAssignments(tenantId, patientId);
        return ResponseEntity.ok(new CountResponse(count));
    }

    // DTO Classes

    @Data
    public static class AssignMeasureRequest {
        @NotNull(message = "Measure ID is required")
        private UUID measureId;

        @NotBlank(message = "Assignment reason is required")
        private String assignmentReason;

        private LocalDate effectiveFrom;
        private LocalDate effectiveUntil;
        private Map<String, Object> eligibilityCriteria;
    }

    @Data
    public static class UpdateDatesRequest {
        @NotNull(message = "Effective from date is required")
        private LocalDate effectiveFrom;

        private LocalDate effectiveUntil;
    }

    @Data
    @RequiredArgsConstructor
    public static class CountResponse {
        private final long count;
    }
}
