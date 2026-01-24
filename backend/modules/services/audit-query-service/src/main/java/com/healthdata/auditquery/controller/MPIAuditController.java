package com.healthdata.auditquery.controller;

import com.healthdata.auditquery.dto.mpi.MPIMergeEventResponse;
import com.healthdata.auditquery.dto.mpi.MPIMetricsResponse;
import com.healthdata.auditquery.dto.mpi.MPIReviewRequest;
import com.healthdata.auditquery.service.MPIAuditService;
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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST controller for MPI Audit Dashboard.
 * Provides endpoints for reviewing and validating Master Patient Index (MPI) merge operations.
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "MPI Audit API", description = "Review and validation of MPI patient merge operations")
public class MPIAuditController {

    private final MPIAuditService mpiAuditService;

    /**
     * Fetch MPI merge events for review.
     *
     * @param tenantId Tenant ID from header
     * @param mergeType Optional filter by merge type (AUTOMATIC, MANUAL, ASSISTED)
     * @param mergeStatus Optional filter by merge status (PENDING, VALIDATED, ROLLED_BACK, FAILED)
     * @param validationStatus Optional filter by validation status (NOT_VALIDATED, VALIDATED, VALIDATION_FAILED)
     * @param minConfidence Optional minimum confidence score
     * @param maxConfidence Optional maximum confidence score
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @param hasErrors Optional filter for merges with errors
     * @param hasDataQualityIssues Optional filter for data quality issues
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param authentication Current user
     * @return Page of MPI merge events
     */
    @GetMapping("/ai/user-actions")
    @PreAuthorize("hasPermission('AUDIT_READ')")
    @Operation(
        summary = "Get MPI merge events",
        description = "Fetch MPI patient merge operations with optional filtering by merge type, status, confidence, and data quality"
    )
    public ResponseEntity<Page<MPIMergeEventResponse>> getMPIMergeEvents(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @Parameter(description = "Merge type filter (AUTOMATIC, MANUAL, ASSISTED)")
        @RequestParam(required = false) String mergeType,
        @Parameter(description = "Merge status filter (PENDING, VALIDATED, ROLLED_BACK, FAILED)")
        @RequestParam(required = false) String mergeStatus,
        @Parameter(description = "Validation status filter (NOT_VALIDATED, VALIDATED, VALIDATION_FAILED)")
        @RequestParam(required = false) String validationStatus,
        @Parameter(description = "Minimum confidence score (0-1)")
        @RequestParam(required = false) Double minConfidence,
        @Parameter(description = "Maximum confidence score (0-1)")
        @RequestParam(required = false) Double maxConfidence,
        @Parameter(description = "Start date (ISO-8601)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @Parameter(description = "End date (ISO-8601)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @Parameter(description = "Filter merges with errors")
        @RequestParam(required = false) Boolean hasErrors,
        @Parameter(description = "Filter merges with data quality issues")
        @RequestParam(required = false) Boolean hasDataQualityIssues,
        @Parameter(description = "Page number (0-indexed)")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        log.info("Fetching MPI merge events for tenant: {}, mergeType: {}, mergeStatus: {}, validationStatus: {}, user: {}",
            tenantId, mergeType, mergeStatus, validationStatus, authentication.getName());

        Page<MPIMergeEventResponse> mergeEvents = mpiAuditService.getMPIMergeEvents(
            tenantId, mergeType, mergeStatus, validationStatus,
            minConfidence, maxConfidence, startDate, endDate,
            hasErrors, hasDataQualityIssues, page, size
        );

        return ResponseEntity.ok(mergeEvents);
    }

    /**
     * Get MPI audit metrics.
     *
     * @param tenantId Tenant ID from header
     * @param startDate Optional start date (defaults to 30 days ago)
     * @param endDate Optional end date (defaults to now)
     * @return MPI metrics
     */
    @GetMapping("/mpi/metrics")
    @PreAuthorize("hasPermission('AUDIT_READ')")
    @Operation(
        summary = "Get MPI audit metrics",
        description = "Retrieve aggregated metrics for MPI merge operations (merge types, validation rates, confidence scores, data quality)"
    )
    public ResponseEntity<MPIMetricsResponse> getMPIMetrics(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @Parameter(description = "Start date (ISO-8601, defaults to 30 days ago)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @Parameter(description = "End date (ISO-8601, defaults to now)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        Authentication authentication
    ) {
        log.info("Fetching MPI metrics for tenant: {}, startDate: {}, endDate: {}, user: {}",
            tenantId, startDate, endDate, authentication.getName());

        MPIMetricsResponse metrics = mpiAuditService.getMPIMetrics(tenantId, startDate, endDate);

        return ResponseEntity.ok(metrics);
    }

    /**
     * Validate MPI merge operation.
     *
     * @param tenantId Tenant ID from header
     * @param mergeId Merge event ID
     * @param request Validation request with notes
     * @param authentication Current user
     * @return Updated merge event
     */
    @PostMapping("/mpi/merges/{id}/validate")
    @PreAuthorize("hasPermission('AUDIT_REVIEW')")
    @Operation(
        summary = "Validate MPI merge",
        description = "Approve and validate MPI patient merge operation"
    )
    public ResponseEntity<MPIMergeEventResponse> validateMerge(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @Parameter(description = "Merge event ID")
        @PathVariable("id") UUID mergeId,
        @Valid @RequestBody MPIReviewRequest request,
        Authentication authentication
    ) {
        log.info("Validating MPI merge: {} for tenant: {}, user: {}",
            mergeId, tenantId, authentication.getName());

        MPIMergeEventResponse mergeEvent = mpiAuditService.validateMerge(
            tenantId, mergeId, request, authentication.getName()
        );

        return ResponseEntity.ok(mergeEvent);
    }

    /**
     * Rollback MPI merge operation.
     *
     * @param tenantId Tenant ID from header
     * @param mergeId Merge event ID
     * @param request Rollback request with reason
     * @param authentication Current user
     * @return Updated merge event
     */
    @PostMapping("/mpi/merges/{id}/rollback")
    @PreAuthorize("hasPermission('AUDIT_REVIEW')")
    @Operation(
        summary = "Rollback MPI merge",
        description = "Rollback MPI patient merge operation and restore original records"
    )
    public ResponseEntity<MPIMergeEventResponse> rollbackMerge(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @Parameter(description = "Merge event ID")
        @PathVariable("id") UUID mergeId,
        @Valid @RequestBody MPIReviewRequest request,
        Authentication authentication
    ) {
        log.info("Rolling back MPI merge: {} for tenant: {}, user: {}, reason: {}",
            mergeId, tenantId, authentication.getName(), request.getRollbackReason());

        MPIMergeEventResponse mergeEvent = mpiAuditService.rollbackMerge(
            tenantId, mergeId, request, authentication.getName()
        );

        return ResponseEntity.ok(mergeEvent);
    }

    /**
     * Resolve data quality issue for MPI merge.
     *
     * @param tenantId Tenant ID from header
     * @param mergeId Merge event ID
     * @param request Resolution request with notes
     * @param authentication Current user
     * @return Updated merge event
     */
    @PostMapping("/mpi/data-quality/{id}/resolve")
    @PreAuthorize("hasPermission('AUDIT_REVIEW')")
    @Operation(
        summary = "Resolve data quality issue",
        description = "Mark data quality issue as resolved with resolution notes"
    )
    public ResponseEntity<MPIMergeEventResponse> resolveDataQualityIssue(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @Parameter(description = "Merge event ID")
        @PathVariable("id") UUID mergeId,
        @Valid @RequestBody MPIReviewRequest request,
        Authentication authentication
    ) {
        log.info("Resolving data quality issue for MPI merge: {} for tenant: {}, user: {}",
            mergeId, tenantId, authentication.getName());

        MPIMergeEventResponse mergeEvent = mpiAuditService.resolveDataQualityIssue(
            tenantId, mergeId, request, authentication.getName()
        );

        return ResponseEntity.ok(mergeEvent);
    }

    /**
     * Export MPI audit report as Excel.
     *
     * @param tenantId Tenant ID from header
     * @param startDate Optional start date (defaults to 30 days ago)
     * @param endDate Optional end date (defaults to now)
     * @param authentication Current user
     * @return Excel file as byte array
     */
    @GetMapping("/mpi/report/export")
    @PreAuthorize("hasPermission('AUDIT_READ')")
    @Operation(
        summary = "Export MPI audit report",
        description = "Generate Excel report of MPI merge operations for specified date range"
    )
    public ResponseEntity<byte[]> exportMPIReport(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @Parameter(description = "Start date (ISO-8601, defaults to 30 days ago)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @Parameter(description = "End date (ISO-8601, defaults to now)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        Authentication authentication
    ) {
        log.info("Exporting MPI audit report for tenant: {}, startDate: {}, endDate: {}, user: {}",
            tenantId, startDate, endDate, authentication.getName());

        byte[] report = mpiAuditService.exportMPIReport(tenantId, startDate, endDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "mpi-audit-report.xlsx");

        return ResponseEntity.ok()
            .headers(headers)
            .body(report);
    }
}
