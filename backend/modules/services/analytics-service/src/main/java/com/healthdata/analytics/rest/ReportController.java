package com.healthdata.analytics.rest;

import com.healthdata.analytics.dto.ReportDto;
import com.healthdata.analytics.dto.ReportExecutionDto;
import com.healthdata.analytics.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Analytics Reports", description = "APIs for generating, scheduling, and exporting quality measure reports for HEDIS submissions and value-based care contracts.")
@RestController
@RequestMapping("/api/analytics/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    @Operation(
        summary = "List all reports",
        description = "Retrieves all quality measure report definitions for the specified tenant. Includes HEDIS submission reports, Stars ratings summaries, care gap analysis reports, and value-based care contract deliverables.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reports retrieved successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied — insufficient role for report access")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<ReportDto>> getReports(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(reportService.getReports(tenantId));
    }

    @Operation(
        summary = "List reports with pagination",
        description = "Retrieves quality measure report definitions with pagination support. Useful for organizations managing many reports across multiple HEDIS measurement years and CMS submission cycles.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated reports retrieved successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied — insufficient role for report access")
    })
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<Page<ReportDto>> getReportsPaginated(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Pagination parameters (page, size, sort)")
            Pageable pageable) {
        return ResponseEntity.ok(reportService.getReportsPaginated(tenantId, pageable));
    }

    @Operation(
        summary = "Get report by ID",
        description = "Retrieves a specific quality measure report definition by its unique identifier. Returns the report configuration including measure selections, date ranges, population filters, and output format settings.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report retrieved successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Report not found for the given ID and tenant"),
        @ApiResponse(responseCode = "403", description = "Access denied — insufficient role for report access")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<ReportDto> getReport(
            @Parameter(description = "Report unique identifier", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return reportService.getReport(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Create a new report",
        description = "Creates a new quality measure report definition for HEDIS submissions, Stars ratings analysis, or value-based care contract deliverables. The authenticated user is recorded as the report creator.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Report created successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid report configuration"),
        @ApiResponse(responseCode = "403", description = "Access denied — ANALYST or ADMIN role required")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ReportDto> createReport(
            @Valid @RequestBody ReportDto dto,
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            Authentication authentication) {
        String userId = authentication.getName();
        ReportDto created = reportService.createReport(dto, tenantId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
        summary = "Update an existing report",
        description = "Updates a quality measure report definition including measure selections, population filters, date ranges, and output format. Use this to adjust reports for new HEDIS measurement years or updated CMS submission requirements.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report updated successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid report configuration"),
        @ApiResponse(responseCode = "404", description = "Report not found for the given ID and tenant"),
        @ApiResponse(responseCode = "403", description = "Access denied — ANALYST or ADMIN role required")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ReportDto> updateReport(
            @Parameter(description = "Report unique identifier", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody ReportDto dto,
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return reportService.updateReport(id, dto, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Delete a report",
        description = "Permanently deletes a quality measure report definition and its execution history. This action cannot be undone. ADMIN role required to prevent accidental deletion of shared organizational reports used for CMS submissions.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Report deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Report not found for the given ID and tenant"),
        @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReport(
            @Parameter(description = "Report unique identifier", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {
        if (reportService.deleteReport(id, tenantId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(
        summary = "Execute a report",
        description = "Triggers execution of a quality measure report with optional runtime parameters. Generates HEDIS measure results, Stars ratings calculations, or care gap analysis data. Execution runs asynchronously and returns an execution tracking object.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Report execution accepted and processing", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Report not found for the given ID and tenant"),
        @ApiResponse(responseCode = "403", description = "Access denied — ANALYST or ADMIN role required")
    })
    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ReportExecutionDto> executeReport(
            @Parameter(description = "Report unique identifier", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Optional runtime parameters such as measurement year, population filters, or measure overrides")
            @RequestBody(required = false) Map<String, Object> parameters,
            Authentication authentication) {
        String userId = authentication.getName();
        ReportExecutionDto execution = reportService.executeReport(id, tenantId, userId, parameters);
        return ResponseEntity.accepted().body(execution);
    }

    @Operation(
        summary = "List report executions",
        description = "Retrieves all execution records for a specific report. Provides execution history including status, timestamps, and result summaries for audit trails and HEDIS submission tracking.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report executions retrieved successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Report not found for the given ID and tenant"),
        @ApiResponse(responseCode = "403", description = "Access denied — insufficient role for execution access")
    })
    @GetMapping("/{id}/executions")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<ReportExecutionDto>> getExecutions(
            @Parameter(description = "Report unique identifier", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(reportService.getExecutions(id, tenantId));
    }

    @Operation(
        summary = "List report executions with pagination",
        description = "Retrieves paginated execution records for a specific report. Supports sorting by execution date, status, or duration for compliance auditing and historical analysis of quality measure results.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated report executions retrieved successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Report not found for the given ID and tenant"),
        @ApiResponse(responseCode = "403", description = "Access denied — insufficient role for execution access")
    })
    @GetMapping("/{id}/executions/paginated")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<Page<ReportExecutionDto>> getExecutionsPaginated(
            @Parameter(description = "Report unique identifier", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Pagination parameters (page, size, sort)")
            Pageable pageable) {
        return ResponseEntity.ok(reportService.getExecutionsPaginated(id, tenantId, pageable));
    }

    @Operation(
        summary = "Get a specific report execution",
        description = "Retrieves a single report execution record by its execution ID. Returns execution status, start/end timestamps, result data, and any error details. Use this to check the progress of an asynchronous HEDIS report generation or CMS submission export.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report execution retrieved successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Execution not found for the given ID and tenant"),
        @ApiResponse(responseCode = "403", description = "Access denied — insufficient role for execution access")
    })
    @GetMapping("/executions/{executionId}")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<ReportExecutionDto> getExecution(
            @Parameter(description = "Report execution unique identifier", required = true)
            @PathVariable UUID executionId,
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return reportService.getExecution(executionId, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
