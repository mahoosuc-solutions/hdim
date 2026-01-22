package com.healthdata.auditquery.controller;

import com.healthdata.auditquery.dto.AuditEventResponse;
import com.healthdata.auditquery.dto.AuditSearchRequest;
import com.healthdata.auditquery.dto.AuditStatisticsResponse;
import com.healthdata.auditquery.service.AuditQueryService;
import com.healthdata.auditquery.service.AuditExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * REST controller for querying HIPAA-compliant audit logs.
 *
 * <p>All endpoints require AUDITOR or ADMIN role and enforce multi-tenant isolation.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/audit/logs/search - Complex multi-criteria search</li>
 *   <li>GET /api/v1/audit/logs/{eventId} - Get specific event by ID</li>
 *   <li>GET /api/v1/audit/logs/statistics - Aggregated statistics</li>
 *   <li>GET /api/v1/audit/logs/export - Export to CSV/JSON/PDF</li>
 * </ul>
 *
 * <p>Security:
 * <ul>
 *   <li>Gateway trust authentication validates X-Auth-* headers</li>
 *   <li>All queries filter by X-Tenant-ID header</li>
 *   <li>Encrypted payloads require decrypt permission (future enhancement)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/audit/logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audit Query API", description = "HIPAA-compliant audit log query and reporting")
@SecurityRequirement(name = "gateway-trust")
public class AuditQueryController {

    private final AuditQueryService auditQueryService;
    private final AuditExportService auditExportService;

    /**
     * Search audit logs with multi-criteria filtering.
     *
     * <p>Supports complex queries combining user, resource, action, and time filters.
     * Results are paginated and sorted by timestamp (descending) by default.
     *
     * @param tenantId tenant ID from gateway header
     * @param request search criteria
     * @return page of matching audit events
     */
    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('AUDITOR', 'ADMIN')")
    @Operation(
        summary = "Search audit logs",
        description = "Multi-criteria search with pagination. Combines filters with AND logic.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Search results returned"),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
        }
    )
    public ResponseEntity<Page<AuditEventResponse>> searchAuditLogs(
        @Parameter(description = "Tenant ID (from gateway header)", required = true)
        @RequestHeader("X-Tenant-ID") String tenantId,

        @Parameter(description = "Search criteria", required = true)
        @Valid @RequestBody AuditSearchRequest request
    ) {
        log.info("Audit log search request from tenant {}", tenantId);

        Page<AuditEventResponse> results = auditQueryService.searchAuditEvents(tenantId, request);

        return ResponseEntity.ok(results);
    }

    /**
     * Get a specific audit event by ID.
     *
     * @param tenantId tenant ID from gateway header
     * @param eventId audit event ID
     * @return audit event details
     */
    @GetMapping("/{eventId}")
    @PreAuthorize("hasAnyRole('AUDITOR', 'ADMIN')")
    @Operation(
        summary = "Get audit event by ID",
        description = "Retrieve detailed information for a specific audit event",
        responses = {
            @ApiResponse(responseCode = "200", description = "Audit event found"),
            @ApiResponse(responseCode = "404", description = "Audit event not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
        }
    )
    public ResponseEntity<AuditEventResponse> getAuditEvent(
        @Parameter(description = "Tenant ID (from gateway header)", required = true)
        @RequestHeader("X-Tenant-ID") String tenantId,

        @Parameter(description = "Audit event ID", required = true)
        @PathVariable UUID eventId
    ) {
        log.info("Fetching audit event {} for tenant {}", eventId, tenantId);

        return auditQueryService.getAuditEvent(tenantId, eventId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get aggregated audit log statistics.
     *
     * <p>Provides counts by action, outcome, resource type, service, and top users/resources.
     * Used for compliance dashboards and security monitoring.
     *
     * @param tenantId tenant ID from gateway header
     * @param startTime start of time range (optional, defaults to 30 days ago)
     * @param endTime end of time range (optional, defaults to now)
     * @return aggregated statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('AUDITOR', 'ADMIN')")
    @Operation(
        summary = "Get audit log statistics",
        description = "Aggregated statistics for compliance reporting and security monitoring",
        responses = {
            @ApiResponse(responseCode = "200", description = "Statistics generated"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
        }
    )
    public ResponseEntity<AuditStatisticsResponse> getStatistics(
        @Parameter(description = "Tenant ID (from gateway header)", required = true)
        @RequestHeader("X-Tenant-ID") String tenantId,

        @Parameter(description = "Start of time range (ISO-8601)", example = "2026-01-15T00:00:00Z")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant startTime,

        @Parameter(description = "End of time range (ISO-8601)", example = "2026-01-22T23:59:59Z")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant endTime
    ) {
        log.info("Generating audit statistics for tenant {} from {} to {}", tenantId, startTime, endTime);

        AuditStatisticsResponse statistics = auditQueryService.getStatistics(tenantId, startTime, endTime);

        return ResponseEntity.ok(statistics);
    }

    /**
     * Export audit logs to CSV, JSON, or PDF format.
     *
     * <p>Supports the same search criteria as the search endpoint.
     * Limited to 100,000 records to prevent memory overflow.
     *
     * @param tenantId tenant ID from gateway header
     * @param request search criteria
     * @param format export format (CSV, JSON, or PDF)
     * @return file download response
     */
    @PostMapping("/export")
    @PreAuthorize("hasAnyRole('AUDITOR', 'ADMIN')")
    @Operation(
        summary = "Export audit logs",
        description = "Export search results to CSV, JSON, or PDF format (max 100,000 records)",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Export file generated",
                content = @Content(mediaType = "application/octet-stream")
            ),
            @ApiResponse(responseCode = "400", description = "Invalid export parameters"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
        }
    )
    public ResponseEntity<byte[]> exportAuditLogs(
        @Parameter(description = "Tenant ID (from gateway header)", required = true)
        @RequestHeader("X-Tenant-ID") String tenantId,

        @Parameter(description = "Search criteria", required = true)
        @Valid @RequestBody AuditSearchRequest request,

        @Parameter(description = "Export format", required = true, schema = @Schema(allowableValues = {"CSV", "JSON", "PDF"}))
        @RequestParam String format
    ) {
        log.info("Exporting audit logs for tenant {} in format {}", tenantId, format);

        byte[] exportData;
        String contentType;
        String filename;

        switch (format.toUpperCase()) {
            case "CSV" -> {
                exportData = auditExportService.exportToCsv(tenantId, request);
                contentType = "text/csv";
                filename = "audit-logs-" + Instant.now().getEpochSecond() + ".csv";
            }
            case "JSON" -> {
                exportData = auditExportService.exportToJson(tenantId, request);
                contentType = MediaType.APPLICATION_JSON_VALUE;
                filename = "audit-logs-" + Instant.now().getEpochSecond() + ".json";
            }
            case "PDF" -> {
                exportData = auditExportService.exportToPdf(tenantId, request);
                contentType = MediaType.APPLICATION_PDF_VALUE;
                filename = "audit-logs-" + Instant.now().getEpochSecond() + ".pdf";
            }
            default -> {
                return ResponseEntity.badRequest().build();
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(exportData.length);

        return new ResponseEntity<>(exportData, headers, HttpStatus.OK);
    }
}
