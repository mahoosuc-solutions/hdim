package com.healthdata.auditquery.controller;

import com.healthdata.auditquery.dto.clinical.ClinicalDecisionResponse;
import com.healthdata.auditquery.dto.clinical.ClinicalMetricsResponse;
import com.healthdata.auditquery.dto.clinical.ClinicalReviewRequest;
import com.healthdata.auditquery.service.ClinicalAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for Clinical Audit Dashboard.
 * Provides endpoints for clinical staff to review AI clinical recommendations.
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clinical Audit API", description = "Clinical staff review of AI clinical recommendations")
public class ClinicalAuditController {

    private final ClinicalAuditService clinicalAuditService;

    /**
     * Fetch clinical AI decisions for review.
     *
     * @param tenantId Tenant ID from header
     * @param agentType Optional filter by agent type (CLINICAL_DECISION_SUPPORT, MEDICATION_ALERT, etc.)
     * @param decisionType Optional filter by decision type
     * @param alertSeverity Optional filter by severity (CRITICAL, HIGH, MODERATE, LOW)
     * @param reviewStatus Optional filter by review status (PENDING, APPROVED, REJECTED, NEEDS_REVISION)
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param authentication Current user
     * @return Page of clinical decisions
     */
    @GetMapping("/ai/decisions")
    @PreAuthorize("hasPermission(null, 'AUDIT_READ')")
    @Operation(
        summary = "Get clinical decisions for review",
        description = "Fetch AI clinical recommendations with optional filtering by agent type, decision type, severity, review status, and date range"
    )
    public ResponseEntity<Page<ClinicalDecisionResponse>> getClinicalDecisions(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @Parameter(description = "Agent type filter (CLINICAL_DECISION_SUPPORT, MEDICATION_ALERT, CARE_GAP_RECOMMENDATION, RISK_STRATIFICATION)")
        @RequestParam(required = false) String agentType,
        @Parameter(description = "Decision type filter")
        @RequestParam(required = false) String decisionType,
        @Parameter(description = "Alert severity filter (CRITICAL, HIGH, MODERATE, LOW)")
        @RequestParam(required = false) String alertSeverity,
        @Parameter(description = "Review status filter (PENDING, APPROVED, REJECTED, NEEDS_REVISION)")
        @RequestParam(required = false) String reviewStatus,
        @Parameter(description = "Start date (ISO-8601)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
        @Parameter(description = "End date (ISO-8601)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
        @Parameter(description = "Page number (0-indexed)")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        log.info("Fetching clinical decisions for tenant: {}, agentType: {}, decisionType: {}, alertSeverity: {}, reviewStatus: {}, user: {}",
            tenantId, agentType, decisionType, alertSeverity, reviewStatus, authentication.getName());

        Page<ClinicalDecisionResponse> decisions = clinicalAuditService.getClinicalDecisions(
            tenantId, agentType, decisionType, alertSeverity, reviewStatus,
            startDate, endDate, page, size
        );

        return ResponseEntity.ok(decisions);
    }

    /**
     * Get clinical audit metrics.
     *
     * @param tenantId Tenant ID from header
     * @param startDate Optional start date (defaults to 30 days ago)
     * @param endDate Optional end date (defaults to now)
     * @return Clinical metrics
     */
    @GetMapping("/clinical/metrics")
    @PreAuthorize("hasPermission(null, 'AUDIT_READ')")
    @Operation(
        summary = "Get clinical audit metrics",
        description = "Retrieve aggregated metrics for clinical AI decisions (acceptance rates, severity distribution, evidence grades, override rates)"
    )
    public ResponseEntity<ClinicalMetricsResponse> getClinicalMetrics(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @Parameter(description = "Start date (ISO-8601, defaults to 30 days ago)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
        @Parameter(description = "End date (ISO-8601, defaults to now)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
        Authentication authentication
    ) {
        log.info("Fetching clinical metrics for tenant: {}, startDate: {}, endDate: {}, user: {}",
            tenantId, startDate, endDate, authentication.getName());

        ClinicalMetricsResponse metrics = clinicalAuditService.getClinicalMetrics(tenantId, startDate, endDate);

        return ResponseEntity.ok(metrics);
    }

    /**
     * Accept AI clinical recommendation.
     *
     * @param tenantId Tenant ID from header
     * @param decisionId Decision ID
     * @param request Review request with notes
     * @param authentication Current user
     * @return Updated decision
     */
    @PostMapping("/clinical/decisions/{id}/accept")
    @PreAuthorize("hasPermission(null, 'AUDIT_READ')")
    @Operation(
        summary = "Accept clinical recommendation",
        description = "Approve AI clinical recommendation and mark as accepted"
    )
    public ResponseEntity<ClinicalDecisionResponse> acceptRecommendation(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @Parameter(description = "Decision ID")
        @PathVariable("id") UUID decisionId,
        @Valid @RequestBody ClinicalReviewRequest request,
        Authentication authentication
    ) {
        log.info("Accepting clinical decision: {} for tenant: {}, user: {}",
            decisionId, tenantId, authentication.getName());

        ClinicalDecisionResponse decision = clinicalAuditService.acceptRecommendation(
            tenantId, decisionId, request, authentication.getName()
        );

        return ResponseEntity.ok(decision);
    }

    /**
     * Reject AI clinical recommendation.
     *
     * @param tenantId Tenant ID from header
     * @param decisionId Decision ID
     * @param request Review request with rejection rationale
     * @param authentication Current user
     * @return Updated decision
     */
    @PostMapping("/clinical/decisions/{id}/reject")
    @PreAuthorize("hasPermission(null, 'AUDIT_READ')")
    @Operation(
        summary = "Reject clinical recommendation",
        description = "Reject AI clinical recommendation with clinical rationale"
    )
    public ResponseEntity<ClinicalDecisionResponse> rejectRecommendation(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @Parameter(description = "Decision ID")
        @PathVariable("id") UUID decisionId,
        @Valid @RequestBody ClinicalReviewRequest request,
        Authentication authentication
    ) {
        log.info("Rejecting clinical decision: {} for tenant: {}, user: {}",
            decisionId, tenantId, authentication.getName());

        ClinicalDecisionResponse decision = clinicalAuditService.rejectRecommendation(
            tenantId, decisionId, request, authentication.getName()
        );

        return ResponseEntity.ok(decision);
    }

    /**
     * Modify AI clinical recommendation.
     *
     * @param tenantId Tenant ID from header
     * @param decisionId Decision ID
     * @param request Review request with modifications
     * @param authentication Current user
     * @return Updated decision
     */
    @PostMapping("/clinical/decisions/{id}/modify")
    @PreAuthorize("hasPermission(null, 'AUDIT_READ')")
    @Operation(
        summary = "Modify clinical recommendation",
        description = "Accept AI recommendation with modifications and clinical notes"
    )
    public ResponseEntity<ClinicalDecisionResponse> modifyRecommendation(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @Parameter(description = "Decision ID")
        @PathVariable("id") UUID decisionId,
        @Valid @RequestBody ClinicalReviewRequest request,
        Authentication authentication
    ) {
        log.info("Modifying clinical decision: {} for tenant: {}, user: {}",
            decisionId, tenantId, authentication.getName());

        ClinicalDecisionResponse decision = clinicalAuditService.modifyRecommendation(
            tenantId, decisionId, request, authentication.getName()
        );

        return ResponseEntity.ok(decision);
    }

    /**
     * Export clinical audit report as Excel.
     *
     * @param tenantId Tenant ID from header
     * @param startDate Optional start date (defaults to 30 days ago)
     * @param endDate Optional end date (defaults to now)
     * @param authentication Current user
     * @return Excel file as byte array
     */
    @GetMapping("/clinical/report/export")
    @PreAuthorize("hasPermission(null, 'AUDIT_READ')")
    @Operation(
        summary = "Export clinical audit report",
        description = "Generate Excel report of clinical AI decisions for specified date range"
    )
    public ResponseEntity<byte[]> exportClinicalReport(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @Parameter(description = "Start date (ISO-8601, defaults to 30 days ago)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
        @Parameter(description = "End date (ISO-8601, defaults to now)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
        Authentication authentication
    ) {
        log.info("Exporting clinical audit report for tenant: {}, startDate: {}, endDate: {}, user: {}",
            tenantId, startDate, endDate, authentication.getName());

        byte[] report = clinicalAuditService.exportClinicalReport(tenantId, startDate, endDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "clinical-audit-report.xlsx");

        return ResponseEntity.ok()
            .headers(headers)
            .body(report);
    }
}
