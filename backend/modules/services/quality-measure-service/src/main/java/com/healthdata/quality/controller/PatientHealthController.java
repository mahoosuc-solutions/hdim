package com.healthdata.quality.controller;

import com.healthdata.quality.dto.*;
import com.healthdata.quality.service.PatientHealthService;
import com.healthdata.quality.service.MentalHealthAssessmentService;
import com.healthdata.quality.service.CareGapService;
import com.healthdata.quality.service.RiskStratificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Patient Health Overview Controller
 *
 * Provides comprehensive patient health endpoints including:
 * - Mental health assessments (PHQ-9, GAD-7, PHQ-2)
 * - Care gap tracking
 * - Risk stratification
 * - Overall health score
 */
@RestController
@RequestMapping("/patient-health")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PatientHealthController {

    private final PatientHealthService healthService;
    private final MentalHealthAssessmentService mentalHealthService;
    private final CareGapService careGapService;
    private final RiskStratificationService riskService;

    /**
     * Get comprehensive patient health overview
     *
     * GET /patient-health/overview/{patientId}
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/overview/{patientId:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientHealthOverviewDTO> getPatientHealthOverview(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable UUID patientId
    ) {
        log.info("GET /patient-health/overview/{} - tenant: {}", patientId, tenantId);
        if (patientId == null) {
            throw new IllegalArgumentException("Patient ID cannot be null");
        }
        PatientHealthOverviewDTO overview = healthService.getPatientHealthOverview(tenantId, patientId);
        return ResponseEntity.ok(overview);
    }

    /**
     * Submit mental health assessment (PHQ-9, GAD-7, PHQ-2, etc.)
     *
     * POST /patient-health/mental-health/assessments
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/mental-health/assessments", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MentalHealthAssessmentDTO> submitMentalHealthAssessment(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @RequestBody @Valid MentalHealthAssessmentRequest request
    ) {
        log.info("POST /patient-health/mental-health/assessments - patient: {}, type: {}",
                request.getPatientId(), request.getAssessmentType());

        MentalHealthAssessmentDTO assessment = mentalHealthService.submitAssessment(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(assessment);
    }

    /**
     * Get patient mental health assessments
     *
     * GET /patient-health/mental-health/assessments/{patientId}
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'ANALYST', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/mental-health/assessments/{patientId:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MentalHealthAssessmentDTO>> getPatientAssessments(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable UUID patientId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        log.info("GET /patient-health/mental-health/assessments/{} - type: {}, limit: {}",
                patientId, type, limit);

        List<MentalHealthAssessmentDTO> assessments =
            mentalHealthService.getPatientAssessments(tenantId, patientId, type, limit, offset);
        return ResponseEntity.ok(assessments);
    }

    /**
     * Get mental health assessment trend
     *
     * GET /patient-health/mental-health/assessments/{patientId}/trend
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'ANALYST', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/mental-health/assessments/{patientId:.+}/trend", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MentalHealthAssessmentService.AssessmentTrend> getAssessmentTrend(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable UUID patientId,
            @RequestParam @NotBlank String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        log.info("GET /patient-health/mental-health/assessments/{}/trend - type: {}", patientId, type);

        MentalHealthAssessmentService.AssessmentTrend trend =
            mentalHealthService.getAssessmentTrend(tenantId, patientId, type, startDate, endDate);
        return ResponseEntity.ok(trend);
    }

    /**
     * Get tenant-level care gap trends for dashboard visualization.
     *
     * GET /patient-health/care-gaps/trends?days=30
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'CARE_COORDINATOR', 'ANALYST', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/care-gaps/trends", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CareGapService.CareGapTrendPoint>> getCareGapTrends(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days
    ) {
        log.info("GET /patient-health/care-gaps/trends - days: {}", days);

        List<CareGapService.CareGapTrendPoint> trends = careGapService.getCareGapTrends(tenantId, days);
        return ResponseEntity.ok(trends);
    }

    /**
     * Get patient care gaps
     *
     * GET /patient-health/care-gaps/{patientId}
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'CARE_COORDINATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/care-gaps/{patientId:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CareGapDTO>> getPatientCareGaps(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable UUID patientId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category
    ) {
        log.info("GET /patient-health/care-gaps/{} - status: {}, category: {}",
                patientId, status, category);

        List<CareGapDTO> gaps =
            careGapService.getPatientCareGaps(tenantId, patientId, status, category);
        return ResponseEntity.ok(gaps);
    }

    /**
     * Mark care gap as addressed
     *
     * PUT /patient-health/care-gaps/{gapId}/address
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'CARE_COORDINATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PutMapping(value = "/care-gaps/{gapId}/address", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<CareGapDTO> addressCareGap(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable @NotBlank String gapId,
            @RequestBody @Valid AddressCareGapRequest request
    ) {
        log.info("PUT /patient-health/care-gaps/{}/address - by: {}", gapId, request.getAddressedBy());

        java.util.UUID uuid = java.util.UUID.fromString(gapId);
        CareGapDTO gap = careGapService.addressCareGap(tenantId, uuid, request);
        return ResponseEntity.ok(gap);
    }

    /**
     * Calculate patient risk stratification
     *
     * POST /patient-health/risk-stratification/{patientId}/calculate
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'ANALYST', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/risk-stratification/{patientId:.+}/calculate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RiskAssessmentDTO> calculateRiskStratification(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable UUID patientId
    ) {
        log.info("POST /patient-health/risk-stratification/{}/calculate", patientId);

        RiskAssessmentDTO risk = riskService.calculateRiskAssessment(tenantId, patientId);
        return ResponseEntity.status(HttpStatus.CREATED).body(risk);
    }

    /**
     * Get patient risk stratification
     *
     * GET /patient-health/risk-stratification/{patientId}
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'ANALYST', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/risk-stratification/{patientId:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RiskAssessmentDTO> getRiskStratification(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable UUID patientId
    ) {
        log.info("GET /patient-health/risk-stratification/{}", patientId);

        RiskAssessmentDTO risk = riskService.getRiskAssessment(tenantId, patientId);
        return ResponseEntity.ok(risk);
    }

    /**
     * Get patient health score
     *
     * GET /patient-health/health-score/{patientId}
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'ANALYST', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/health-score/{patientId:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthScoreDTO> getHealthScore(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable UUID patientId
    ) {
        log.info("GET /patient-health/health-score/{}", patientId);

        HealthScoreDTO score = healthService.calculateHealthScore(tenantId, patientId);
        return ResponseEntity.ok(score);
    }
}
