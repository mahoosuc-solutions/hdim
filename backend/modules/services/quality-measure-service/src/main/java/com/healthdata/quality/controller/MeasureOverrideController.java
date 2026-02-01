package com.healthdata.quality.controller;

import com.healthdata.quality.persistence.PatientMeasureOverrideEntity;
import com.healthdata.quality.service.MeasureOverrideService;
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
 * Measure Override API Controller
 *
 * Manages patient-specific measure parameter overrides with clinical justification.
 *
 * HIPAA Compliance: All overrides require clinical justification and supporting evidence.
 *
 * Endpoints:
 * - GET    /quality-measure/patients/{patientId}/measure-overrides - List overrides
 * - POST   /quality-measure/patients/{patientId}/measure-overrides - Create override
 * - POST   /quality-measure/measure-overrides/{overrideId}/approve - Approve override
 * - POST   /quality-measure/measure-overrides/{overrideId}/review - Mark reviewed
 * - DELETE /quality-measure/measure-overrides/{overrideId} - Deactivate override
 * - GET    /quality-measure/measure-overrides/pending-approval - List pending approvals
 * - GET    /quality-measure/measure-overrides/due-for-review - List overrides needing review
 * - POST   /quality-measure/patients/{patientId}/measures/{measureId}/resolve-overrides - Resolve all overrides
 *
 * All endpoints require X-Tenant-ID header and appropriate role-based authorization.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class MeasureOverrideController {

    private final MeasureOverrideService overrideService;

    /**
     * Get active overrides for a patient and measure
     *
     * GET /quality-measure/patients/{patientId}/measure-overrides?measureId={measureId}
     */
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/patients/{patientId}/measure-overrides", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PatientMeasureOverrideEntity>> getPatientOverrides(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable UUID patientId,
            @RequestParam(value = "measureId", required = false) UUID measureId,
            @RequestParam(value = "effectiveDate", required = false) LocalDate effectiveDate
    ) {
        log.info("GET /quality-measure/patients/{}/measure-overrides - tenant: {}, measure: {}",
                patientId, tenantId, measureId);

        if (measureId == null) {
            return ResponseEntity.badRequest().build();
        }

        List<PatientMeasureOverrideEntity> overrides;
        if (effectiveDate != null) {
            overrides = overrideService.getEffectiveOverrides(tenantId, patientId, measureId, effectiveDate);
        } else {
            overrides = overrideService.getActiveOverrides(tenantId, patientId, measureId);
        }

        return ResponseEntity.ok(overrides);
    }

    /**
     * Create a new patient measure override
     * HIPAA: Clinical justification is REQUIRED
     *
     * POST /quality-measure/patients/{patientId}/measure-overrides
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/patients/{patientId}/measure-overrides", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientMeasureOverrideEntity> createOverride(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @RequestHeader("X-Auth-User-Id") UUID createdBy,
            @PathVariable UUID patientId,
            @RequestBody @Validated CreateOverrideRequest request
    ) {
        log.info("POST /quality-measure/patients/{}/measure-overrides - tenant: {}, measure: {}, field: {}",
                patientId, tenantId, request.getMeasureId(), request.getOverrideField());

        try {
            PatientMeasureOverrideEntity override = overrideService.createOverride(
                    tenantId,
                    patientId,
                    request.getMeasureId(),
                    request.getOverrideType(),
                    request.getOverrideField(),
                    request.getOriginalValue(),
                    request.getOverrideValue(),
                    request.getValueType(),
                    request.getClinicalReason(),
                    request.getSupportingEvidence(),
                    createdBy,
                    request.getEffectiveFrom(),
                    request.getEffectiveUntil(),
                    request.getRequiresApproval()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(override);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid override request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            log.warn("Override conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * Approve a pending override
     *
     * POST /quality-measure/measure-overrides/{overrideId}/approve
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/measure-overrides/{overrideId}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientMeasureOverrideEntity> approveOverride(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @RequestHeader("X-Auth-User-Id") UUID approvedBy,
            @PathVariable UUID overrideId,
            @RequestBody(required = false) ApprovalRequest request
    ) {
        log.info("POST /quality-measure/measure-overrides/{}/approve - tenant: {}", overrideId, tenantId);

        try {
            PatientMeasureOverrideEntity override = overrideService.approveOverride(
                    tenantId,
                    overrideId,
                    approvedBy,
                    request != null ? request.getApprovalNotes() : null
            );
            return ResponseEntity.ok(override);
        } catch (IllegalArgumentException e) {
            log.warn("Override not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Mark override as reviewed (for periodic review)
     *
     * POST /quality-measure/measure-overrides/{overrideId}/review
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/measure-overrides/{overrideId}/review", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientMeasureOverrideEntity> markReviewed(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @RequestHeader("X-Auth-User-Id") UUID reviewedBy,
            @PathVariable UUID overrideId
    ) {
        log.info("POST /quality-measure/measure-overrides/{}/review - tenant: {}", overrideId, tenantId);

        try {
            PatientMeasureOverrideEntity override = overrideService.markReviewed(tenantId, overrideId, reviewedBy);
            return ResponseEntity.ok(override);
        } catch (IllegalArgumentException e) {
            log.warn("Override not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate an override
     *
     * DELETE /quality-measure/measure-overrides/{overrideId}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping(value = "/measure-overrides/{overrideId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientMeasureOverrideEntity> deactivateOverride(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @RequestHeader("X-Auth-User-Id") UUID deactivatedBy,
            @PathVariable UUID overrideId
    ) {
        log.info("DELETE /quality-measure/measure-overrides/{} - tenant: {}", overrideId, tenantId);

        try {
            PatientMeasureOverrideEntity override = overrideService.deactivateOverride(
                    tenantId, overrideId, deactivatedBy);
            return ResponseEntity.ok(override);
        } catch (IllegalArgumentException e) {
            log.warn("Override not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get overrides pending approval
     *
     * GET /quality-measure/measure-overrides/pending-approval
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/measure-overrides/pending-approval", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PatientMeasureOverrideEntity>> getPendingApprovals(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId
    ) {
        log.info("GET /quality-measure/measure-overrides/pending-approval - tenant: {}", tenantId);

        List<PatientMeasureOverrideEntity> overrides = overrideService.getPendingApprovals(tenantId);
        return ResponseEntity.ok(overrides);
    }

    /**
     * Get overrides due for periodic review
     *
     * GET /quality-measure/measure-overrides/due-for-review
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/measure-overrides/due-for-review", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PatientMeasureOverrideEntity>> getDueForReview(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @RequestParam(value = "asOfDate", required = false) LocalDate asOfDate
    ) {
        log.info("GET /quality-measure/measure-overrides/due-for-review - tenant: {}", tenantId);

        List<PatientMeasureOverrideEntity> overrides = overrideService.getOverridesDueForReview(tenantId, asOfDate);
        return ResponseEntity.ok(overrides);
    }

    /**
     * Resolve all applicable overrides for a patient and measure
     * Returns final parameter values after applying patient overrides and profiles
     *
     * POST /quality-measure/patients/{patientId}/measures/{measureId}/resolve-overrides
     */
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/patients/{patientId}/measures/{measureId}/resolve-overrides",
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> resolveOverrides(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable UUID patientId,
            @PathVariable UUID measureId,
            @RequestParam(value = "evaluationDate", required = false) LocalDate evaluationDate
    ) {
        log.info("POST /quality-measure/patients/{}/measures/{}/resolve-overrides - tenant: {}",
                patientId, measureId, tenantId);

        Map<String, Object> resolvedValues = overrideService.resolveOverrides(
                tenantId,
                patientId,
                measureId,
                evaluationDate != null ? evaluationDate : LocalDate.now()
        );

        return ResponseEntity.ok(resolvedValues);
    }

    // DTO Classes

    @Data
    public static class CreateOverrideRequest {
        @NotNull(message = "Measure ID is required")
        private UUID measureId;

        @NotBlank(message = "Override type is required")
        private String overrideType; // THRESHOLD, PARAMETER, EXCLUSION, etc.

        @NotBlank(message = "Override field is required")
        private String overrideField;

        private String originalValue;

        @NotBlank(message = "Override value is required")
        private String overrideValue;

        private String valueType; // NUMERIC, DATE, BOOLEAN, TEXT, JSON

        @NotBlank(message = "Clinical reason is required (HIPAA compliance)")
        private String clinicalReason;

        private Map<String, Object> supportingEvidence;

        private LocalDate effectiveFrom;
        private LocalDate effectiveUntil;
        private Boolean requiresApproval;
    }

    @Data
    public static class ApprovalRequest {
        private String approvalNotes;
    }
}
