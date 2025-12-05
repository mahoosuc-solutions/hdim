package com.healthdata.quality.controller;

import com.healthdata.quality.dto.HealthScoreDTO;
import com.healthdata.quality.service.HealthScoreService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Health Score API Controller
 *
 * Provides endpoints for:
 * - Current health score for individual patients
 * - Health score history and trends
 * - At-risk patients (scores below threshold)
 * - Significant score changes
 *
 * All endpoints require X-Tenant-ID header for multi-tenant isolation
 * and appropriate role-based authorization.
 */
@RestController
@RequestMapping("/quality-measure/patients")
@RequiredArgsConstructor
@Slf4j
@Validated
public class HealthScoreController {

    private final HealthScoreService healthScoreService;

    // Default threshold for at-risk patients
    private static final double DEFAULT_AT_RISK_THRESHOLD = 60.0;

    // Default lookback period for significant changes (7 days)
    private static final int DEFAULT_SIGNIFICANT_CHANGE_DAYS = 7;

    /**
     * Get current health score for a patient
     *
     * GET /quality-measure/patients/{patientId}/health-score
     *
     * Returns the most recent health score for the specified patient,
     * including overall score and component breakdowns.
     *
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @param patientId Patient identifier (can contain slashes, e.g., Patient/123)
     * @return Current health score or 404 if not found
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/{patientId:.+}/health-score", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthScoreDTO> getCurrentHealthScore(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable String patientId
    ) {
        log.info("GET /quality-measure/patients/{}/health-score - tenant: {}", patientId, tenantId);

        return healthScoreService.getCurrentHealthScore(tenantId, patientId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get health score history for a patient
     *
     * GET /quality-measure/patients/{patientId}/health-score/history
     *
     * Returns historical health scores showing trends over time.
     * Results are ordered by calculation date (most recent first).
     *
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @param patientId Patient identifier (can contain slashes)
     * @param limit Maximum number of records to return (default: 50, max: 100)
     * @return List of historical health scores
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/{patientId:.+}/health-score/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<HealthScoreDTO>> getHealthScoreHistory(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable String patientId,
            @RequestParam(value = "limit", defaultValue = "50") @PositiveOrZero int limit
    ) {
        log.info("GET /quality-measure/patients/{}/health-score/history - tenant: {}, limit: {}",
                patientId, tenantId, limit);

        // Cap limit at 100 to prevent excessive data retrieval
        int effectiveLimit = Math.min(limit, 100);

        List<HealthScoreDTO> history = healthScoreService.getHealthScoreHistory(tenantId, patientId);

        // Apply limit
        List<HealthScoreDTO> limitedHistory = history.stream()
                .limit(effectiveLimit)
                .toList();

        return ResponseEntity.ok(limitedHistory);
    }

    /**
     * Get patients with health scores below threshold (at-risk patients)
     *
     * GET /quality-measure/patients/health-scores/at-risk
     *
     * Returns patients whose current health score is below the specified threshold,
     * indicating they may need additional care intervention.
     *
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @param threshold Score threshold (default: 60.0, range: 0-100)
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 20, max: 100)
     * @return Paginated list of patients with scores below threshold
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/health-scores/at-risk", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<HealthScoreDTO>> getAtRiskPatients(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam(value = "threshold", defaultValue = "60.0") double threshold,
            @RequestParam(value = "page", defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(value = "size", defaultValue = "20") @PositiveOrZero int size
    ) {
        log.info("GET /quality-measure/patients/health-scores/at-risk - tenant: {}, threshold: {}, page: {}, size: {}",
                tenantId, threshold, page, size);

        // Validate threshold
        if (threshold < 0.0 || threshold > 100.0) {
            throw new IllegalArgumentException("Threshold must be between 0 and 100");
        }

        // Cap page size at 100
        int effectiveSize = Math.min(size, 100);

        Pageable pageable = PageRequest.of(page, effectiveSize, Sort.by("overallScore").ascending());
        Page<HealthScoreDTO> atRiskPatients = healthScoreService.getAtRiskPatients(tenantId, threshold, pageable);

        return ResponseEntity.ok(atRiskPatients);
    }

    /**
     * Get patients with significant recent health score changes
     *
     * GET /quality-measure/patients/health-scores/significant-changes
     *
     * Returns patients who have experienced significant health score changes
     * (>= 10 point change) within the specified time period. Useful for
     * identifying patients requiring immediate attention.
     *
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @param days Lookback period in days (default: 7, max: 90)
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 20, max: 100)
     * @return Paginated list of patients with significant score changes
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/health-scores/significant-changes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<HealthScoreDTO>> getSignificantChanges(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam(value = "days", defaultValue = "7") @PositiveOrZero int days,
            @RequestParam(value = "page", defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(value = "size", defaultValue = "20") @PositiveOrZero int size
    ) {
        log.info("GET /quality-measure/patients/health-scores/significant-changes - tenant: {}, days: {}, page: {}, size: {}",
                tenantId, days, page, size);

        // Cap days at 90 to prevent excessive lookback
        int effectiveDays = Math.min(days, 90);

        // Cap page size at 100
        int effectiveSize = Math.min(size, 100);

        // Calculate since timestamp
        Instant since = Instant.now().minus(effectiveDays, ChronoUnit.DAYS);

        Pageable pageable = PageRequest.of(page, effectiveSize, Sort.by("calculatedAt").descending());
        Page<HealthScoreDTO> significantChanges = healthScoreService.getSignificantChanges(tenantId, since, pageable);

        return ResponseEntity.ok(significantChanges);
    }
}
