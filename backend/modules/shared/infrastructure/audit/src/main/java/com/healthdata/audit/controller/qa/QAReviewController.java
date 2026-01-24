package com.healthdata.audit.controller.qa;

import com.healthdata.audit.dto.qa.*;
import com.healthdata.audit.service.qa.QAReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * QA Review Controller
 * 
 * REST API for Quality Assurance analysts to review, approve, reject, and flag
 * AI-generated decisions. Supports workflow management for QA validation processes.
 * 
 * Security: Restricted to QA_ANALYST, QUALITY_OFFICER, ADMIN, and AUDITOR roles
 * 
 * Endpoints:
 * - GET /queue - Get pending AI decisions for review
 * - GET /{id} - Get specific decision details
 * - POST /{id}/approve - Approve AI decision
 * - POST /{id}/reject - Reject AI decision
 * - POST /{id}/flag - Flag decision for further review
 * - GET /flagged - Get flagged decisions
 * - GET /metrics - Get QA metrics and statistics
 * - GET /trends - Get accuracy trend data
 */
@RestController
@RequestMapping("/api/v1/audit/ai/qa")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "QA Review", description = "Quality Assurance review workflow APIs")
@SecurityRequirement(name = "bearer-auth")
public class QAReviewController {

    private final QAReviewService qaReviewService;

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'QUALITY_OFFICER', 'QA_ANALYST')")
    @Operation(summary = "Get QA review queue", 
               description = "Retrieve pending AI decisions awaiting QA review with filtering and pagination")
    public ResponseEntity<Page<QADecisionReview>> getReviewQueue(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) String agentType,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Double minConfidence,
            @RequestParam(required = false) Double maxConfidence,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "false") boolean includeReviewed,
            Pageable pageable
    ) {
        log.info("Getting QA review queue for tenant: {} with filters - agentType: {}, priority: {}", 
                tenantId, agentType, priority);

        QAReviewFilter filter = QAReviewFilter.builder()
                .tenantId(tenantId)
                .agentType(agentType)
                .priority(priority)
                .minConfidence(minConfidence)
                .maxConfidence(maxConfidence)
                .startDate(startDate)
                .endDate(endDate)
                .includeReviewed(includeReviewed)
                .build();

        Page<QADecisionReview> queue = qaReviewService.getReviewQueue(filter, pageable);
        return ResponseEntity.ok(queue);
    }

    @GetMapping("/{decisionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'QUALITY_OFFICER', 'QA_ANALYST')")
    @Operation(summary = "Get decision details", 
               description = "Retrieve detailed information about a specific AI decision for QA review")
    public ResponseEntity<QADecisionDetail> getDecisionDetail(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String decisionId
    ) {
        log.info("Getting decision detail for: {} in tenant: {}", decisionId, tenantId);
        QADecisionDetail detail = qaReviewService.getDecisionDetail(tenantId, decisionId);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{decisionId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'QA_ANALYST')")
    @Operation(summary = "Approve AI decision", 
               description = "Mark an AI decision as approved after QA review")
    public ResponseEntity<QAReviewResult> approveDecision(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String decisionId,
            @Valid @RequestBody QAReviewRequest request
    ) {
        log.info("Approving decision: {} by: {} in tenant: {}", decisionId, request.getReviewedBy(), tenantId);
        QAReviewResult result = qaReviewService.approveDecision(tenantId, decisionId, request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{decisionId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'QA_ANALYST')")
    @Operation(summary = "Reject AI decision", 
               description = "Mark an AI decision as rejected after QA review with reason")
    public ResponseEntity<QAReviewResult> rejectDecision(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String decisionId,
            @Valid @RequestBody QAReviewRequest request
    ) {
        log.info("Rejecting decision: {} by: {} in tenant: {}", decisionId, request.getReviewedBy(), tenantId);
        QAReviewResult result = qaReviewService.rejectDecision(tenantId, decisionId, request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{decisionId}/flag")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'QA_ANALYST')")
    @Operation(summary = "Flag decision for review", 
               description = "Flag an AI decision for additional review or escalation")
    public ResponseEntity<QAReviewResult> flagDecision(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String decisionId,
            @Valid @RequestBody QAFlagRequest request
    ) {
        log.info("Flagging decision: {} as {} by: {} in tenant: {}", 
                decisionId, request.getFlagType(), request.getReviewedBy(), tenantId);
        QAReviewResult result = qaReviewService.flagDecision(tenantId, decisionId, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/flagged")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'QUALITY_OFFICER', 'QA_ANALYST')")
    @Operation(summary = "Get flagged decisions", 
               description = "Retrieve all decisions flagged for additional review")
    public ResponseEntity<Page<QAFlaggedDecision>> getFlaggedDecisions(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) String flagType,
            @RequestParam(required = false) String agentType,
            Pageable pageable
    ) {
        log.info("Getting flagged decisions for tenant: {} with flagType: {}", tenantId, flagType);
        Page<QAFlaggedDecision> flagged = qaReviewService.getFlaggedDecisions(tenantId, flagType, agentType, pageable);
        return ResponseEntity.ok(flagged);
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'QUALITY_OFFICER', 'QA_ANALYST')")
    @Operation(summary = "Get QA metrics", 
               description = "Retrieve QA review metrics including approval rates, accuracy statistics, and performance indicators")
    public ResponseEntity<QAMetrics> getMetrics(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) String agentType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("Getting QA metrics for tenant: {} from {} to {}", tenantId, startDate, endDate);
        QAMetrics metrics = qaReviewService.getMetrics(tenantId, agentType, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'QUALITY_OFFICER', 'QA_ANALYST')")
    @Operation(summary = "Get accuracy trends", 
               description = "Retrieve historical accuracy trend data for AI decisions over time")
    public ResponseEntity<QATrendData> getAccuracyTrends(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) String agentType,
            @RequestParam(required = false, defaultValue = "30") int days
    ) {
        log.info("Getting accuracy trends for tenant: {} for last {} days", tenantId, days);
        QATrendData trends = qaReviewService.getAccuracyTrends(tenantId, agentType, days);
        return ResponseEntity.ok(trends);
    }

    @PostMapping("/batch/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER')")
    @Operation(summary = "Batch approve decisions", 
               description = "Approve multiple AI decisions in a single operation")
    public ResponseEntity<BatchReviewResult> batchApprove(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody BatchReviewRequest request
    ) {
        log.info("Batch approving {} decisions by: {} in tenant: {}", 
                request.getDecisionIds().size(), request.getReviewedBy(), tenantId);
        BatchReviewResult result = qaReviewService.batchApprove(tenantId, request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/batch/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER')")
    @Operation(summary = "Batch reject decisions",
               description = "Reject multiple AI decisions in a single operation")
    public ResponseEntity<BatchReviewResult> batchReject(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody BatchReviewRequest request
    ) {
        log.info("Batch rejecting {} decisions by: {} in tenant: {}",
                request.getDecisionIds().size(), request.getReviewedBy(), tenantId);
        BatchReviewResult result = qaReviewService.batchReject(tenantId, request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{decisionId}/false-positive")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'QA_ANALYST')")
    @Operation(summary = "Mark decision as false positive",
               description = "Mark an AI decision as a false positive (incorrectly flagged as positive when it should be negative)")
    public ResponseEntity<QAReviewResult> markFalsePositive(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String decisionId,
            @Valid @RequestBody QAReviewRequest request
    ) {
        log.info("Marking decision {} as false positive by: {} in tenant: {}",
                decisionId, request.getReviewedBy(), tenantId);
        QAReviewResult result = qaReviewService.markFalsePositive(tenantId, decisionId, request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{decisionId}/false-negative")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'QA_ANALYST')")
    @Operation(summary = "Mark decision as false negative",
               description = "Mark an AI decision as a false negative (missed detection - should have been flagged but wasn't)")
    public ResponseEntity<QAReviewResult> markFalseNegative(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String decisionId,
            @Valid @RequestBody QAReviewRequest request
    ) {
        log.info("Marking decision {} as false negative by: {} in tenant: {}",
                decisionId, request.getReviewedBy(), tenantId);
        QAReviewResult result = qaReviewService.markFalseNegative(tenantId, decisionId, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/report/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'QUALITY_OFFICER')")
    @Operation(summary = "Export QA report",
               description = "Export comprehensive QA review report with metrics, trends, and decision history")
    public ResponseEntity<QAReportExport> exportReport(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) String agentType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "JSON") String format
    ) {
        log.info("Exporting QA report for tenant: {} from {} to {} in format: {}",
                tenantId, startDate, endDate, format);
        QAReportExport report = qaReviewService.exportReport(tenantId, agentType, startDate, endDate, format);
        return ResponseEntity.ok(report);
    }
}
