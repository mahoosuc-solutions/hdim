package com.healthdata.qualityevent.api;

import com.healthdata.qualityevent.projection.MeasureEvaluationProjection;
import com.healthdata.qualityevent.repository.MeasureEvaluationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Measure Evaluation Projection Query API
 *
 * Provides fast read-only queries on denormalized measure evaluation data.
 * Part of CQRS pattern - this service contains the read model.
 */
@RestController
@RequestMapping("/api/v1/measure-evaluations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Measure Evaluations", description = "CQRS Read Model - Measure Evaluations")
public class MeasureEvaluationController {

    private final MeasureEvaluationRepository measureEvaluationRepository;

    /**
     * Get measure evaluation by measure and patient
     */
    @GetMapping("/measure/{measureId}/patient/{patientId}")
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Operation(summary = "Get measure evaluation", description = "Retrieve evaluation for specific measure and patient")
    public ResponseEntity<MeasureEvaluationProjection> getMeasureEvaluation(
            @PathVariable String measureId,
            @PathVariable UUID patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching measure evaluation for measure {} / patient {} in tenant {}", measureId, patientId, tenantId);

        return measureEvaluationRepository.findByTenantIdAndMeasureIdAndPatientId(tenantId, measureId, patientId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all evaluations for a patient
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Operation(summary = "Get patient evaluations", description = "Retrieve all measure evaluations for patient")
    public ResponseEntity<List<MeasureEvaluationProjection>> getPatientEvaluations(
            @PathVariable UUID patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching measure evaluations for patient {} in tenant {}", patientId, tenantId);

        List<MeasureEvaluationProjection> evaluations = measureEvaluationRepository
            .findByTenantIdAndPatientIdOrderByMeasureIdAsc(tenantId, patientId);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Get all evaluations for a measure (paginated)
     */
    @GetMapping("/measure/{measureId}")
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Operation(summary = "Get measure evaluations", description = "Retrieve all evaluations for measure")
    public ResponseEntity<Page<MeasureEvaluationProjection>> getMeasureEvaluations(
            @PathVariable String measureId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        log.debug("Fetching evaluations for measure {} in tenant {}", measureId, tenantId);

        Page<MeasureEvaluationProjection> evaluations = measureEvaluationRepository
            .findByTenantIdAndMeasureIdOrderByPatientIdAsc(tenantId, measureId, pageable);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Get non-compliant patients for a measure
     */
    @GetMapping("/measure/{measureId}/non-compliant")
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Operation(summary = "Get non-compliant patients", description = "Retrieve non-compliant patients for measure")
    public ResponseEntity<List<MeasureEvaluationProjection>> getNonCompliantForMeasure(
            @PathVariable String measureId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching non-compliant patients for measure {} in tenant {}", measureId, tenantId);

        List<MeasureEvaluationProjection> patients = measureEvaluationRepository
            .findNonCompliantForMeasure(tenantId, measureId);
        return ResponseEntity.ok(patients);
    }

    /**
     * Get compliant patients for a measure
     */
    @GetMapping("/measure/{measureId}/compliant")
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Operation(summary = "Get compliant patients", description = "Retrieve compliant patients for measure")
    public ResponseEntity<List<MeasureEvaluationProjection>> getCompliantForMeasure(
            @PathVariable String measureId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching compliant patients for measure {} in tenant {}", measureId, tenantId);

        List<MeasureEvaluationProjection> patients = measureEvaluationRepository
            .findCompliantForMeasure(tenantId, measureId);
        return ResponseEntity.ok(patients);
    }

    /**
     * Get compliance statistics for a measure
     */
    @GetMapping("/measure/{measureId}/compliance-stats")
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Operation(summary = "Get measure compliance stats", description = "Retrieve compliance statistics for measure")
    public ResponseEntity<MeasureComplianceStats> getComplianceStats(
            @PathVariable String measureId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Calculating compliance stats for measure {} in tenant {}", measureId, tenantId);

        long compliant = measureEvaluationRepository.countCompliantForMeasure(tenantId, measureId);
        long total = measureEvaluationRepository.countTotalForMeasure(tenantId, measureId);
        Double complianceRate = measureEvaluationRepository.calculateComplianceRate(tenantId, measureId);
        Double avgScore = measureEvaluationRepository.getAverageScore(tenantId, measureId);

        MeasureComplianceStats stats = MeasureComplianceStats.builder()
            .measureId(measureId)
            .compliantCount(compliant)
            .totalCount(total)
            .complianceRate(complianceRate != null ? complianceRate * 100 : 0.0)
            .averageScore(avgScore != null ? avgScore : 0.0)
            .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * Get all available measures
     */
    @GetMapping("/measures")
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Operation(summary = "Get all measures", description = "Retrieve all measures evaluated for tenant")
    public ResponseEntity<List<String>> getAllMeasures(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching all measures for tenant {}", tenantId);

        List<String> measures = measureEvaluationRepository.findDistinctMeasures(tenantId);
        return ResponseEntity.ok(measures);
    }

    /**
     * Get measures not meeting threshold
     */
    @GetMapping("/measures/below-threshold")
    @PreAuthorize("hasPermission('MEASURE_READ')")
    @Operation(summary = "Get below-threshold measures", description = "Retrieve measures not meeting compliance threshold")
    public ResponseEntity<List<String>> getMeasuresBelowThreshold(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching measures below threshold for tenant {}", tenantId);

        List<String> measures = measureEvaluationRepository.findMeasuresNotMeetingThreshold(tenantId);
        return ResponseEntity.ok(measures);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Service health status")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Quality measure event service is healthy");
    }
}
