package com.healthdata.auditquery.controller;

import com.healthdata.auditquery.dto.qa.QAMetricsResponse;
import com.healthdata.auditquery.dto.qa.QAReviewQueueResponse;
import com.healthdata.auditquery.dto.qa.QAReviewRequest;
import com.healthdata.auditquery.service.QAAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for QA Audit Dashboard.
 *
 * <p>Provides endpoints for QA analysts to review AI decisions, view metrics,
 * and export QA audit reports.
 *
 * <p>All endpoints require QA_ANALYST, QUALITY_OFFICER, ADMIN, or AUDITOR role.
 */
@RestController
@RequestMapping("/api/v1/audit/ai/qa")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "QA Audit API", description = "Quality assurance review of AI decisions")
@SecurityRequirement(name = "gateway-trust")
public class QAAuditController {

    private final QAAuditService qaAuditService;

    /**
     * Get QA review queue with filtering.
     *
     * @param tenantId tenant ID from gateway header
     * @param agentType filter by AI agent type (optional)
     * @param minConfidence minimum confidence score (optional)
     * @param maxConfidence maximum confidence score (optional)
     * @param startDate start of date range (optional)
     * @param endDate end of date range (optional)
     * @param includeReviewed include already reviewed items (optional, defaults to false)
     * @param page page number (0-indexed)
     * @param size page size
     * @param authentication authenticated user
     * @return paginated review queue
     */
    @GetMapping("/review-queue")
    @PreAuthorize("hasPermission('AUDIT_READ')")
    @Operation(
        summary = "Get QA review queue",
        description = "Fetch AI decisions pending QA review with optional filtering",
        responses = {
            @ApiResponse(responseCode = "200", description = "Review queue returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
        }
    )
    public ResponseEntity<Page<QAReviewQueueResponse>> getReviewQueue(
        @Parameter(description = "Tenant ID", required = true)
        @RequestHeader("X-Tenant-ID") String tenantId,

        @Parameter(description = "AI agent type filter", example = "CARE_GAP_DETECTION")
        @RequestParam(required = false) String agentType,

        @Parameter(description = "Minimum confidence score", example = "0.5")
        @RequestParam(required = false) Double minConfidence,

        @Parameter(description = "Maximum confidence score", example = "0.8")
        @RequestParam(required = false) Double maxConfidence,

        @Parameter(description = "Start date (ISO-8601)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant startDate,

        @Parameter(description = "End date (ISO-8601)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant endDate,

        @Parameter(description = "Include already reviewed items")
        @RequestParam(required = false, defaultValue = "false") Boolean includeReviewed,

        @Parameter(description = "Page number (0-indexed)")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") int size,

        Authentication authentication
    ) {
        log.info("Fetching QA review queue for tenant {} by user {}",
                 tenantId, authentication.getName());

        Page<QAReviewQueueResponse> reviewQueue = qaAuditService.getReviewQueue(
            tenantId, agentType, minConfidence, maxConfidence,
            startDate, endDate, includeReviewed, page, size
        );

        return ResponseEntity.ok(reviewQueue);
    }

    /**
     * Get QA audit metrics.
     *
     * @param tenantId tenant ID from gateway header
     * @param startDate start of date range (optional)
     * @param endDate end of date range (optional)
     * @return QA metrics
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasPermission('AUDIT_READ')")
    @Operation(
        summary = "Get QA audit metrics",
        description = "Aggregated statistics for QA dashboard",
        responses = {
            @ApiResponse(responseCode = "200", description = "Metrics returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
        }
    )
    public ResponseEntity<QAMetricsResponse> getQAMetrics(
        @Parameter(description = "Tenant ID", required = true)
        @RequestHeader("X-Tenant-ID") String tenantId,

        @Parameter(description = "Start date (ISO-8601)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant startDate,

        @Parameter(description = "End date (ISO-8601)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant endDate
    ) {
        log.info("Fetching QA metrics for tenant {}", tenantId);

        QAMetricsResponse metrics = qaAuditService.getQAMetrics(tenantId, startDate, endDate);

        return ResponseEntity.ok(metrics);
    }

    /**
     * Get QA trend data over time.
     *
     * @param tenantId tenant ID from gateway header
     * @param startDate start of date range (optional)
     * @param endDate end of date range (optional)
     * @return trend data (daily/weekly aggregations)
     */
    @GetMapping("/trends")
    @PreAuthorize("hasPermission('AUDIT_READ')")
    @Operation(
        summary = "Get QA trend data",
        description = "Historical trend analysis for QA metrics",
        responses = {
            @ApiResponse(responseCode = "200", description = "Trend data returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
        }
    )
    public ResponseEntity<Map<String, Object>> getTrendData(
        @Parameter(description = "Tenant ID", required = true)
        @RequestHeader("X-Tenant-ID") String tenantId,

        @Parameter(description = "Start date (ISO-8601)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant startDate,

        @Parameter(description = "End date (ISO-8601)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant endDate
    ) {
        log.info("Fetching QA trend data for tenant {}", tenantId);

        Map<String, Object> trendData = qaAuditService.getTrendData(tenantId, startDate, endDate);

        return ResponseEntity.ok(trendData);
    }

    /**
     * Approve an AI decision.
     *
     * @param tenantId tenant ID from gateway header
     * @param id AI decision event ID
     * @param request review request
     * @param authentication authenticated user
     * @return success response
     */
    @PostMapping("/review/{id}/approve")
    @PreAuthorize("hasPermission('AUDIT_READ')")
    @Operation(
        summary = "Approve AI decision",
        description = "Mark AI decision as approved after QA review",
        responses = {
            @ApiResponse(responseCode = "200", description = "Decision approved"),
            @ApiResponse(responseCode = "404", description = "Decision not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
        }
    )
    public ResponseEntity<Void> approveDecision(
        @Parameter(description = "Tenant ID", required = true)
        @RequestHeader("X-Tenant-ID") String tenantId,

        @Parameter(description = "AI decision event ID", required = true)
        @PathVariable UUID id,

        @Parameter(description = "Review request")
        @Valid @RequestBody QAReviewRequest request,

        Authentication authentication
    ) {
        log.info("Approving AI decision {} for tenant {} by user {}",
                 id, tenantId, authentication.getName());

        qaAuditService.approveDecision(tenantId, id, request, authentication.getName());

        return ResponseEntity.ok().build();
    }

    /**
     * Reject an AI decision.
     *
     * @param tenantId tenant ID from gateway header
     * @param id AI decision event ID
     * @param request review request with rejection reason
     * @param authentication authenticated user
     * @return success response
     */
    @PostMapping("/review/{id}/reject")
    @PreAuthorize("hasPermission('AUDIT_READ')")
    @Operation(
        summary = "Reject AI decision",
        description = "Mark AI decision as rejected after QA review",
        responses = {
            @ApiResponse(responseCode = "200", description = "Decision rejected"),
            @ApiResponse(responseCode = "404", description = "Decision not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
        }
    )
    public ResponseEntity<Void> rejectDecision(
        @Parameter(description = "Tenant ID", required = true)
        @RequestHeader("X-Tenant-ID") String tenantId,

        @Parameter(description = "AI decision event ID", required = true)
        @PathVariable UUID id,

        @Parameter(description = "Review request with rejection reason", required = true)
        @Valid @RequestBody QAReviewRequest request,

        Authentication authentication
    ) {
        log.info("Rejecting AI decision {} for tenant {} by user {}",
                 id, tenantId, authentication.getName());

        qaAuditService.rejectDecision(tenantId, id, request, authentication.getName());

        return ResponseEntity.ok().build();
    }

    /**
     * Flag an AI decision for manual review.
     *
     * @param tenantId tenant ID from gateway header
     * @param id AI decision event ID
     * @param request review request with flag reason
     * @param authentication authenticated user
     * @return success response
     */
    @PostMapping("/review/{id}/flag")
    @PreAuthorize("hasPermission('AUDIT_READ')")
    @Operation(
        summary = "Flag AI decision for manual review",
        description = "Mark AI decision as requiring additional manual review",
        responses = {
            @ApiResponse(responseCode = "200", description = "Decision flagged"),
            @ApiResponse(responseCode = "404", description = "Decision not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
        }
    )
    public ResponseEntity<Void> flagDecision(
        @Parameter(description = "Tenant ID", required = true)
        @RequestHeader("X-Tenant-ID") String tenantId,

        @Parameter(description = "AI decision event ID", required = true)
        @PathVariable UUID id,

        @Parameter(description = "Review request with flag reason", required = true)
        @Valid @RequestBody QAReviewRequest request,

        Authentication authentication
    ) {
        log.info("Flagging AI decision {} for tenant {} by user {}",
                 id, tenantId, authentication.getName());

        qaAuditService.flagDecision(tenantId, id, request, authentication.getName());

        return ResponseEntity.ok().build();
    }

    /**
     * Mark an AI decision as false positive.
     *
     * @param tenantId tenant ID from gateway header
     * @param id AI decision event ID
     * @param request review request with context
     * @param authentication authenticated user
     * @return success response
     */
    @PostMapping("/review/{id}/false-positive")
    @PreAuthorize("hasPermission('AUDIT_READ')")
    @Operation(
        summary = "Mark as false positive",
        description = "Mark AI decision as false positive for model improvement",
        responses = {
            @ApiResponse(responseCode = "200", description = "Marked as false positive"),
            @ApiResponse(responseCode = "404", description = "Decision not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
        }
    )
    public ResponseEntity<Void> markFalsePositive(
        @Parameter(description = "Tenant ID", required = true)
        @RequestHeader("X-Tenant-ID") String tenantId,

        @Parameter(description = "AI decision event ID", required = true)
        @PathVariable UUID id,

        @Parameter(description = "Review request with context", required = true)
        @Valid @RequestBody QAReviewRequest request,

        Authentication authentication
    ) {
        log.info("Marking AI decision {} as false positive for tenant {} by user {}",
                 id, tenantId, authentication.getName());

        qaAuditService.markFalsePositive(tenantId, id, request, authentication.getName());

        return ResponseEntity.ok().build();
    }

    /**
     * Mark an AI decision as false negative.
     *
     * @param tenantId tenant ID from gateway header
     * @param id AI decision event ID
     * @param request review request with context
     * @param authentication authenticated user
     * @return success response
     */
    @PostMapping("/review/{id}/false-negative")
    @PreAuthorize("hasPermission('AUDIT_READ')")
    @Operation(
        summary = "Mark as false negative",
        description = "Mark AI decision as false negative for model improvement",
        responses = {
            @ApiResponse(responseCode = "200", description = "Marked as false negative"),
            @ApiResponse(responseCode = "404", description = "Decision not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
        }
    )
    public ResponseEntity<Void> markFalseNegative(
        @Parameter(description = "Tenant ID", required = true)
        @RequestHeader("X-Tenant-ID") String tenantId,

        @Parameter(description = "AI decision event ID", required = true)
        @PathVariable UUID id,

        @Parameter(description = "Review request with context", required = true)
        @Valid @RequestBody QAReviewRequest request,

        Authentication authentication
    ) {
        log.info("Marking AI decision {} as false negative for tenant {} by user {}",
                 id, tenantId, authentication.getName());

        qaAuditService.markFalseNegative(tenantId, id, request, authentication.getName());

        return ResponseEntity.ok().build();
    }

    /**
     * Get review details for a specific AI decision.
     *
     * @param tenantId tenant ID from gateway header
     * @param id AI decision event ID
     * @return review details
     */
    @GetMapping("/review/{id}")
    @PreAuthorize("hasPermission('AUDIT_READ')")
    @Operation(
        summary = "Get review details",
        description = "Fetch detailed information for a specific AI decision review",
        responses = {
            @ApiResponse(responseCode = "200", description = "Review details returned"),
            @ApiResponse(responseCode = "404", description = "Decision not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
        }
    )
    public ResponseEntity<QAReviewQueueResponse> getReviewDetail(
        @Parameter(description = "Tenant ID", required = true)
        @RequestHeader("X-Tenant-ID") String tenantId,

        @Parameter(description = "AI decision event ID", required = true)
        @PathVariable UUID id
    ) {
        log.info("Fetching review detail {} for tenant {}", id, tenantId);

        return qaAuditService.getReviewDetail(tenantId, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Export QA audit report.
     *
     * @param tenantId tenant ID from gateway header
     * @param startDate start of date range (optional)
     * @param endDate end of date range (optional)
     * @return Excel file download
     */
    @GetMapping("/report/export")
    @PreAuthorize("hasPermission('AUDIT_READ')")
    @Operation(
        summary = "Export QA audit report",
        description = "Export QA audit data to Excel format",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Report generated",
                content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            ),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
        }
    )
    public ResponseEntity<byte[]> exportQAReport(
        @Parameter(description = "Tenant ID", required = true)
        @RequestHeader("X-Tenant-ID") String tenantId,

        @Parameter(description = "Start date (ISO-8601)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant startDate,

        @Parameter(description = "End date (ISO-8601)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant endDate
    ) {
        log.info("Exporting QA audit report for tenant {}", tenantId);

        byte[] reportData = qaAuditService.exportQAReport(tenantId, startDate, endDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment",
            "qa-audit-report-" + Instant.now().getEpochSecond() + ".xlsx");
        headers.setContentLength(reportData.length);

        return new ResponseEntity<>(reportData, headers, HttpStatus.OK);
    }
}
