package com.healthdata.audit.controller.mpi;

import com.healthdata.audit.dto.mpi.*;
import com.healthdata.audit.service.mpi.MPIAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST Controller for MPI audit operations
 */
@RestController
@RequestMapping("/api/v1/mpi")
@RequiredArgsConstructor
@Tag(name = "MPI Audit", description = "Master Patient Index audit and validation operations")
@SecurityRequirement(name = "bearer-jwt")
public class MPIAuditController {
    
    private final MPIAuditService mpiAuditService;
    
    @GetMapping("/merges")
    @PreAuthorize("hasAnyRole('MPI_ANALYST', 'DATA_STEWARD', 'QUALITY_OFFICER', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Get MPI merge history", 
               description = "Returns paginated list of MPI merges with optional filtering")
    public ResponseEntity<Page<MPIMergeEvent>> getMergeHistory(
            @RequestParam(required = false) String mergeStatus,
            @RequestParam(required = false) String validationStatus,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String mergeType,
            @RequestParam(required = false) Double minConfidenceScore,
            @RequestParam(required = false) Double maxConfidenceScore,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "mergeTimestamp,desc") String sort,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        
        MPIMergeFilter filter = MPIMergeFilter.builder()
            .mergeStatus(mergeStatus)
            .validationStatus(validationStatus)
            .startDate(startDate)
            .endDate(endDate)
            .mergeType(mergeType)
            .minConfidenceScore(minConfidenceScore)
            .maxConfidenceScore(maxConfidenceScore)
            .build();
        
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && "asc".equalsIgnoreCase(sortParams[1]) 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
        
        Page<MPIMergeEvent> merges = mpiAuditService.getMergeHistory(tenantId, filter, pageable);
        return ResponseEntity.ok(merges);
    }
    
    @GetMapping("/merges/{id}")
    @PreAuthorize("hasAnyRole('MPI_ANALYST', 'DATA_STEWARD', 'QUALITY_OFFICER', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Get merge details", 
               description = "Returns detailed information about a specific MPI merge including patient data comparison")
    public ResponseEntity<MPIMergeDetail> getMergeDetail(
            @PathVariable String id,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        MPIMergeDetail detail = mpiAuditService.getMergeDetail(id, tenantId);
        return ResponseEntity.ok(detail);
    }
    
    @PostMapping("/merges/{id}/validate")
    @PreAuthorize("hasAnyRole('MPI_ANALYST', 'DATA_STEWARD', 'QUALITY_OFFICER', 'ADMIN')")
    @Operation(summary = "Validate MPI merge", 
               description = "Validate that a merge was performed correctly and mark it as validated")
    public ResponseEntity<MPIValidationResult> validateMerge(
            @PathVariable String id,
            @Valid @RequestBody MPIValidationRequest request,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        String validatedBy = authentication.getName();
        
        MPIValidationResult result = mpiAuditService.validateMerge(id, tenantId, request, validatedBy);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/merges/{id}/rollback")
    @PreAuthorize("hasAnyRole('MPI_ANALYST', 'DATA_STEWARD', 'QUALITY_OFFICER', 'ADMIN')")
    @Operation(summary = "Rollback MPI merge", 
               description = "Rollback an incorrect merge and optionally restore source patient records")
    public ResponseEntity<MPIRollbackResult> rollbackMerge(
            @PathVariable String id,
            @Valid @RequestBody MPIRollbackRequest request,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        String rolledBackBy = authentication.getName();
        
        MPIRollbackResult result = mpiAuditService.rollbackMerge(id, tenantId, request, rolledBackBy);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/data-quality/issues")
    @PreAuthorize("hasAnyRole('MPI_ANALYST', 'DATA_STEWARD', 'QUALITY_OFFICER', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Get data quality issues", 
               description = "Returns paginated list of data quality issues with optional filtering")
    public ResponseEntity<Page<DataQualityIssueDTO>> getDataQualityIssues(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<DataQualityIssueDTO> issues = mpiAuditService.getDataQualityIssues(tenantId, status, severity, pageable);
        return ResponseEntity.ok(issues);
    }
    
    @PostMapping("/data-quality/issues/{id}/resolve")
    @PreAuthorize("hasAnyRole('MPI_ANALYST', 'DATA_STEWARD', 'QUALITY_OFFICER', 'ADMIN')")
    @Operation(summary = "Resolve data quality issue", 
               description = "Mark a data quality issue as resolved with resolution notes")
    public ResponseEntity<DataQualityIssueDTO> resolveDataQualityIssue(
            @PathVariable String id,
            @Valid @RequestBody DataQualityResolveRequest request,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        String resolvedBy = authentication.getName();
        
        DataQualityIssueDTO result = mpiAuditService.resolveDataQualityIssue(id, tenantId, request, resolvedBy);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('MPI_ANALYST', 'DATA_STEWARD', 'QUALITY_OFFICER', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Get MPI metrics", 
               description = "Returns aggregated metrics for MPI merge operations and data quality")
    public ResponseEntity<MPIMetrics> getMetrics(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        MPIMetrics metrics = mpiAuditService.getMetrics(tenantId, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('MPI_ANALYST', 'DATA_STEWARD', 'QUALITY_OFFICER', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Get accuracy trends", 
               description = "Returns historical trend data for MPI merge validation and rollback rates")
    public ResponseEntity<MPITrendData> getAccuracyTrends(
            @RequestParam(required = false, defaultValue = "30") int days,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        MPITrendData trends = mpiAuditService.getAccuracyTrends(tenantId, days);
        return ResponseEntity.ok(trends);
    }
    
    private String getTenantId(Authentication authentication) {
        // Extract tenant ID from authentication
        // In a real implementation, this would come from the authenticated user's context
        return "default-tenant";
    }
}
