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
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<CqlEvaluation> createAndExecuteEvaluation(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam UUID libraryId,
            @RequestParam UUID patientId) {
        logger.info("Creating evaluation for patient: {} with library: {}", patientId, libraryId);

        CqlEvaluation evaluation = evaluationService.createEvaluation(tenantId, libraryId, patientId);
        CqlEvaluation executed = evaluationService.executeEvaluation(evaluation.getId(), tenantId);

        return ResponseEntity.status(HttpStatus.CREATED).body(executed);
    }

    /**
     * Execute an existing evaluation
     * POST /api/v1/cql/evaluations/{id}/execute
     */
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/{id}/execute")
    public ResponseEntity<CqlEvaluation> executeEvaluation(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {
        logger.info("Executing evaluation: {}", id);

        CqlEvaluation executed = evaluationService.executeEvaluation(id, tenantId);
        return ResponseEntity.ok(executed);
    }

    /**
     * Get all evaluations for a tenant
     * GET /api/v1/cql/evaluations
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/by-status/{status}")
    public ResponseEntity<Page<CqlEvaluation>> getEvaluationsByStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String status,
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
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/batch")
    public ResponseEntity<List<CqlEvaluation>> batchEvaluate(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam UUID libraryId,
            @RequestBody List<UUID> patientIds) {
        // Validate that patient list is not empty
        if (patientIds == null || patientIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

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
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/failed-for-retry")
    public ResponseEntity<List<CqlEvaluation>> getFailedEvaluationsForRetry(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "24") int hoursBack) {
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
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping("/old")
    public ResponseEntity<Void> deleteOldEvaluations(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "90") int daysToRetain) {
        logger.info("Deleting evaluations older than {} days for tenant: {}", daysToRetain, tenantId);

        evaluationService.deleteOldEvaluations(tenantId, daysToRetain);
        return ResponseEntity.noContent().build();
    }
}
