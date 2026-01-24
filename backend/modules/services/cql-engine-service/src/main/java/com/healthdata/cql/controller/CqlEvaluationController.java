package com.healthdata.cql.controller;

import com.healthdata.cql.entity.CqlEvaluation;
import com.healthdata.cql.service.CqlEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

/**
 * REST Controller for CQL Evaluation Management
 *
 * Provides HTTP endpoints for executing CQL expressions and retrieving evaluation results.
 * All endpoints are multi-tenant aware via X-Tenant-ID header.
 *
 * Authorization:
 * - Execute operations (POST) require EVALUATOR, ADMIN, or SUPER_ADMIN role
 * - Read operations (GET) require ANALYST, EVALUATOR, ADMIN, or SUPER_ADMIN role
 * - Delete operations require ADMIN or SUPER_ADMIN role
 */
@RestController
@RequestMapping("/api/v1/cql/evaluations")
@Validated
public class CqlEvaluationController {

    private static final Logger logger = LoggerFactory.getLogger(CqlEvaluationController.class);

    private final CqlEvaluationService evaluationService;

    public CqlEvaluationController(CqlEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /**
     * Create and execute a CQL evaluation
     * POST /api/v1/cql/evaluations
     */
    @PreAuthorize("hasPermission('MEASURE_EXECUTE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping
    public ResponseEntity<CqlEvaluation> createAndExecuteEvaluation(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam @NotNull(message = "Library ID is required") UUID libraryId,
            @RequestParam @NotNull(message = "Patient ID is required") UUID patientId) {
        logger.info("Creating evaluation for patient: {} with library: {}", patientId, libraryId);

        CqlEvaluation evaluation = evaluationService.createEvaluation(tenantId, libraryId, patientId);
        CqlEvaluation executed = evaluationService.executeEvaluation(evaluation.getId(), tenantId);

        return ResponseEntity.status(HttpStatus.CREATED).body(executed);
    }

    /**
     * Execute an existing evaluation
     * POST /api/v1/cql/evaluations/{id}/execute
     */
    @PreAuthorize("hasPermission('MEASURE_EXECUTE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/{id}/execute")
    public ResponseEntity<CqlEvaluation> executeEvaluation(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable @NotNull(message = "Evaluation ID is required") UUID id) {
        logger.info("Executing evaluation: {}", id);

        CqlEvaluation executed = evaluationService.executeEvaluation(id, tenantId);
        return ResponseEntity.ok(executed);
    }

    /**
     * Get all evaluations for a tenant
     * GET /api/v1/cql/evaluations
     */
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping
    public ResponseEntity<Page<CqlEvaluation>> getAllEvaluations(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        logger.debug("Getting all evaluations for tenant: {}", tenantId);

        Page<CqlEvaluation> evaluations = evaluationService.getAllEvaluations(tenantId, pageable);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Get an evaluation by ID
     * GET /api/v1/cql/evaluations/{id}
     */
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/{id}")
    public ResponseEntity<CqlEvaluation> getEvaluation(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {
        logger.debug("Getting evaluation: {} for tenant: {}", id, tenantId);

        return evaluationService.getEvaluationById(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all evaluations for a patient
     * GET /api/v1/cql/evaluations/patient/{patientId}
     */
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<Page<CqlEvaluation>> getEvaluationsForPatient(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            Pageable pageable) {
        logger.debug("Getting evaluations for patient: {} in tenant: {}", patientId, tenantId);

        Page<CqlEvaluation> evaluations = evaluationService.getEvaluationsForPatient(
                tenantId, patientId, pageable);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Get evaluations for a library
     * GET /api/v1/cql/evaluations/library/{libraryId}
     */
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/library/{libraryId}")
    public ResponseEntity<Page<CqlEvaluation>> getEvaluationsForLibrary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID libraryId,
            Pageable pageable) {
        logger.debug("Getting evaluations for library: {} in tenant: {}", libraryId, tenantId);

        Page<CqlEvaluation> evaluations = evaluationService.getEvaluationsForLibrary(
                tenantId, libraryId, pageable);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Get the latest evaluation for a patient and library
     * GET /api/v1/cql/evaluations/patient/{patientId}/library/{libraryId}/latest
     */
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/patient/{patientId}/library/{libraryId}/latest")
    public ResponseEntity<CqlEvaluation> getLatestEvaluationForPatientAndLibrary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @PathVariable UUID libraryId) {
        logger.debug("Getting latest evaluation for patient: {} and library: {} in tenant: {}",
                patientId, libraryId, tenantId);

        return evaluationService.getLatestEvaluationForPatientAndLibrary(tenantId, patientId, libraryId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get evaluations by status
     * GET /api/v1/cql/evaluations/by-status/{status}
     */
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/by-status/{status}")
    public ResponseEntity<Page<CqlEvaluation>> getEvaluationsByStatus(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable @NotBlank(message = "Status is required") String status,
            Pageable pageable) {
        logger.debug("Getting evaluations with status: {} for tenant: {}", status, tenantId);

        Page<CqlEvaluation> evaluations = evaluationService.getEvaluationsByStatus(
                tenantId, status, pageable);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Get evaluations within a date range
     * GET /api/v1/cql/evaluations/date-range?start={start}&end={end}
     */
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/date-range")
    public ResponseEntity<List<CqlEvaluation>> getEvaluationsByDateRange(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        logger.debug("Getting evaluations between {} and {} for tenant: {}", start, end, tenantId);

        List<CqlEvaluation> evaluations = evaluationService.getEvaluationsByDateRange(
                tenantId, start, end);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Get evaluations for a patient within a date range
     * GET /api/v1/cql/evaluations/patient/{patientId}/date-range?start={start}&end={end}
     */
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/patient/{patientId}/date-range")
    public ResponseEntity<List<CqlEvaluation>> getEvaluationsForPatientByDateRange(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        logger.debug("Getting evaluations for patient: {} between {} and {} in tenant: {}",
                patientId, start, end, tenantId);

        List<CqlEvaluation> evaluations = evaluationService.getEvaluationsForPatientByDateRange(
                tenantId, patientId, start, end);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Get successful evaluations for a patient
     * GET /api/v1/cql/evaluations/patient/{patientId}/successful
     */
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/patient/{patientId}/successful")
    public ResponseEntity<List<CqlEvaluation>> getSuccessfulEvaluationsForPatient(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {
        logger.debug("Getting successful evaluations for patient: {} in tenant: {}",
                patientId, tenantId);

        List<CqlEvaluation> evaluations = evaluationService.getSuccessfulEvaluationsForPatient(
                tenantId, patientId);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Retry a failed evaluation
     * POST /api/v1/cql/evaluations/{id}/retry
     */
    @PreAuthorize("hasPermission('MEASURE_EXECUTE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/{id}/retry")
    public ResponseEntity<CqlEvaluation> retryEvaluation(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {
        logger.info("Retrying evaluation: {}", id);

        CqlEvaluation retried = evaluationService.retryEvaluation(id, tenantId);
        return ResponseEntity.ok(retried);
    }

    /**
     * Batch evaluate multiple patients
     * POST /api/v1/cql/evaluations/batch
     */
    @PreAuthorize("hasPermission('MEASURE_EXECUTE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/batch")
    public ResponseEntity<List<CqlEvaluation>> batchEvaluate(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam @NotNull(message = "Library ID is required") UUID libraryId,
            @RequestBody @NotEmpty(message = "Patient IDs list cannot be empty") List<@NotNull UUID> patientIds) {

        logger.info("Starting batch evaluation for {} patients with library: {}",
                patientIds.size(), libraryId);

        List<CqlEvaluation> evaluations = evaluationService.batchEvaluate(
                tenantId, libraryId, patientIds);
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluations);
    }

    /**
     * Get failed evaluations for retry
     * GET /api/v1/cql/evaluations/failed-for-retry?hoursBack={hours}
     */
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/failed-for-retry")
    public ResponseEntity<List<CqlEvaluation>> getFailedEvaluationsForRetry(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam(defaultValue = "24") @Min(value = 1, message = "Hours must be at least 1") int hoursBack) {
        logger.debug("Getting failed evaluations for retry (last {} hours) for tenant: {}",
                hoursBack, tenantId);

        List<CqlEvaluation> evaluations = evaluationService.getFailedEvaluationsForRetry(
                tenantId, hoursBack);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Count evaluations by status
     * GET /api/v1/cql/evaluations/count/by-status/{status}
     */
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/count/by-status/{status}")
    public ResponseEntity<Long> countEvaluationsByStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String status) {
        logger.debug("Counting evaluations with status: {} for tenant: {}", status, tenantId);

        long count = evaluationService.countEvaluationsByStatus(tenantId, status);
        return ResponseEntity.ok(count);
    }

    /**
     * Count evaluations for a library
     * GET /api/v1/cql/evaluations/count/library/{libraryId}
     */
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/count/library/{libraryId}")
    public ResponseEntity<Long> countEvaluationsForLibrary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID libraryId) {
        logger.debug("Counting evaluations for library: {} in tenant: {}", libraryId, tenantId);

        long count = evaluationService.countEvaluationsForLibrary(tenantId, libraryId);
        return ResponseEntity.ok(count);
    }

    /**
     * Count evaluations for a patient
     * GET /api/v1/cql/evaluations/count/patient/{patientId}
     */
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/count/patient/{patientId}")
    public ResponseEntity<Long> countEvaluationsForPatient(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {
        logger.debug("Counting evaluations for patient: {} in tenant: {}", patientId, tenantId);

        long count = evaluationService.countEvaluationsForPatient(tenantId, patientId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get average evaluation duration for a library
     * GET /api/v1/cql/evaluations/avg-duration/library/{libraryId}
     */
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/avg-duration/library/{libraryId}")
    public ResponseEntity<Double> getAverageDurationForLibrary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID libraryId) {
        logger.debug("Getting average duration for library: {} in tenant: {}", libraryId, tenantId);

        Double avgDuration = evaluationService.getAverageDurationForLibrary(tenantId, libraryId);
        return ResponseEntity.ok(avgDuration != null ? avgDuration : 0.0);
    }

    /**
     * Delete old evaluations (data retention)
     * DELETE /api/v1/cql/evaluations/old?daysToRetain={days}
     */
    @PreAuthorize("hasPermission('MEASURE_WRITE')")
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/old")
    public ResponseEntity<Void> deleteOldEvaluations(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam(defaultValue = "90") @Min(value = 1, message = "Days to retain must be at least 1") int daysToRetain) {
        logger.info("Deleting evaluations older than {} days for tenant: {}", daysToRetain, tenantId);

        evaluationService.deleteOldEvaluations(tenantId, daysToRetain);
        return ResponseEntity.noContent().build();
    }
}
