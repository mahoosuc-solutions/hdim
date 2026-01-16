package com.healthdata.gateway.clinical.compliance.controller;

import com.healthdata.gateway.clinical.compliance.dto.ErrorSyncRequest;
import com.healthdata.gateway.clinical.compliance.dto.ErrorSyncResponse;
import com.healthdata.gateway.clinical.compliance.service.ComplianceErrorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST API for compliance error tracking
 * 
 * Endpoints:
 * - POST /api/v1/compliance/errors - Sync errors from frontend
 * - GET /api/v1/compliance/errors - Query errors (ADMIN only)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
@Tag(name = "Compliance Error Tracking", description = "API for tracking and querying compliance errors")
public class ComplianceErrorController {

    private final ComplianceErrorService complianceErrorService;

    /**
     * Sync errors from frontend
     * Public endpoint (authenticated via gateway)
     */
    @PostMapping("/errors")
    @Operation(summary = "Sync compliance errors from frontend", 
               description = "Accepts batch of errors from frontend for compliance tracking")
    public ResponseEntity<ErrorSyncResponse> syncErrors(
        @RequestBody ErrorSyncRequest request,
        HttpServletRequest httpRequest
    ) {
        try {
            // Extract tenant ID from request header or use default
            String tenantId = httpRequest.getHeader("X-Tenant-ID");
            if (tenantId == null || tenantId.isEmpty()) {
                tenantId = "default-tenant";
            }

            int synced = complianceErrorService.syncErrors(request, tenantId);

            ErrorSyncResponse response = ErrorSyncResponse.builder()
                .synced(synced)
                .timestamp(Instant.now().toString())
                .message("Successfully synced " + synced + " errors")
                .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to sync errors: {}", e.getMessage(), e);
            ErrorSyncResponse errorResponse = ErrorSyncResponse.builder()
                .synced(0)
                .timestamp(Instant.now().toString())
                .message("Failed to sync errors: " + e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get errors for tenant (ADMIN only)
     */
    @GetMapping("/errors")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    @Operation(summary = "Query compliance errors", 
               description = "Get compliance errors with pagination and filtering (ADMIN/DEVELOPER only)")
    public ResponseEntity<Page<?>> getErrors(
        @RequestParam(required = false) String tenantId,
        @RequestParam(required = false) String severity,
        @RequestParam(required = false) String service,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        HttpServletRequest httpRequest
    ) {
        // Use tenant from header if not provided
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = httpRequest.getHeader("X-Tenant-ID");
            if (tenantId == null || tenantId.isEmpty()) {
                tenantId = "default-tenant";
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<?> errors;
        if (severity != null && !severity.isEmpty()) {
            errors = complianceErrorService.getErrorsBySeverity(tenantId, severity, pageable);
        } else if (service != null && !service.isEmpty()) {
            errors = complianceErrorService.getErrorsByService(tenantId, service, pageable);
        } else {
            errors = complianceErrorService.getErrors(tenantId, pageable);
        }

        return ResponseEntity.ok(errors);
    }

    /**
     * Get error statistics
     */
    @GetMapping("/errors/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    @Operation(summary = "Get error statistics", 
               description = "Get error counts by severity and time range (ADMIN/DEVELOPER only)")
    public ResponseEntity<?> getErrorStats(
        @RequestParam(required = false) String tenantId,
        @RequestParam(required = false) Integer hours,
        HttpServletRequest httpRequest
    ) {
        // Use tenant from header if not provided
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = httpRequest.getHeader("X-Tenant-ID");
            if (tenantId == null || tenantId.isEmpty()) {
                tenantId = "default-tenant";
            }
        }

        int hoursBack = hours != null ? hours : 24;
        Instant startDate = Instant.now().minus(hoursBack, java.time.temporal.ChronoUnit.HOURS);
        Instant endDate = Instant.now();

        return ResponseEntity.ok(new ErrorStatsResponse(
            complianceErrorService.getErrorCountInRange(tenantId, startDate, endDate),
            complianceErrorService.getErrorCountBySeverity(tenantId, "CRITICAL"),
            complianceErrorService.getErrorCountBySeverity(tenantId, "ERROR"),
            complianceErrorService.getErrorCountBySeverity(tenantId, "WARNING"),
            complianceErrorService.getErrorCountBySeverity(tenantId, "INFO")
        ));
    }

    /**
     * Cleanup old errors (retention policy)
     */
    @DeleteMapping("/errors/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cleanup old errors", 
               description = "Delete errors older than retention period (ADMIN only)")
    public ResponseEntity<?> cleanupOldErrors(
        @RequestParam(required = false) String tenantId,
        @RequestParam(defaultValue = "90") int retentionDays,
        HttpServletRequest httpRequest
    ) {
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = httpRequest.getHeader("X-Tenant-ID");
            if (tenantId == null || tenantId.isEmpty()) {
                tenantId = "default-tenant";
            }
        }

        int deleted = complianceErrorService.cleanupOldErrors(tenantId, retentionDays);
        return ResponseEntity.ok(new CleanupResponse(deleted, retentionDays));
    }

    // Response DTOs
    record ErrorStatsResponse(
        long total,
        long critical,
        long error,
        long warning,
        long info
    ) {}

    record CleanupResponse(
        int deleted,
        int retentionDays
    ) {}
}
