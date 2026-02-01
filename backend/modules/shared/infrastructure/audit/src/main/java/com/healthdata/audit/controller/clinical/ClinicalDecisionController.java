package com.healthdata.audit.controller.clinical;

import com.healthdata.audit.dto.clinical.*;
import com.healthdata.audit.service.clinical.ClinicalDecisionService;
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
 * REST Controller for clinical decision audit operations
 */
@RestController
@RequestMapping("/api/v1/clinical")
@RequiredArgsConstructor
@Tag(name = "Clinical Decision Audit", description = "Clinical decision support audit and review operations")
@SecurityRequirement(name = "bearer-jwt")
public class ClinicalDecisionController {
    
    private final ClinicalDecisionService clinicalDecisionService;
    
    @GetMapping("/decisions")
    @PreAuthorize("hasAnyRole('CLINICIAN', 'CLINICAL_REVIEWER', 'QUALITY_OFFICER', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Get clinical decision history", 
               description = "Returns paginated list of clinical decisions with optional filtering")
    public ResponseEntity<Page<ClinicalDecisionEvent>> getDecisionHistory(
            @RequestParam(required = false) String decisionType,
            @RequestParam(required = false) String alertSeverity,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String evidenceGrade,
            @RequestParam(required = false) Boolean hasOverride,
            @RequestParam(required = false) String specialtyArea,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "decisionTimestamp,desc") String sort,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        
        ClinicalDecisionFilter filter = ClinicalDecisionFilter.builder()
            .decisionType(decisionType)
            .alertSeverity(alertSeverity)
            .reviewStatus(reviewStatus)
            .startDate(startDate)
            .endDate(endDate)
            .evidenceGrade(evidenceGrade)
            .hasOverride(hasOverride)
            .specialtyArea(specialtyArea)
            .build();
        
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && "asc".equalsIgnoreCase(sortParams[1]) 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
        
        Page<ClinicalDecisionEvent> decisions = clinicalDecisionService.getDecisionHistory(tenantId, filter, pageable);
        return ResponseEntity.ok(decisions);
    }
    
    @GetMapping("/decisions/{id}")
    @PreAuthorize("hasAnyRole('CLINICIAN', 'CLINICAL_REVIEWER', 'QUALITY_OFFICER', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Get clinical decision details", 
               description = "Returns comprehensive details for a specific clinical decision including evidence and recommendations")
    public ResponseEntity<ClinicalDecisionDetail> getDecisionDetail(
            @PathVariable String id,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        ClinicalDecisionDetail detail = clinicalDecisionService.getDecisionDetail(id, tenantId);
        return ResponseEntity.ok(detail);
    }
    
    @PostMapping("/decisions/{id}/review")
    @PreAuthorize("hasAnyRole('CLINICIAN', 'CLINICAL_REVIEWER', 'QUALITY_OFFICER', 'ADMIN')")
    @Operation(summary = "Review clinical decision", 
               description = "Approve, reject, or request revision for a clinical decision with optional override")
    public ResponseEntity<ClinicalReviewResult> reviewDecision(
            @PathVariable String id,
            @Valid @RequestBody ClinicalReviewRequest request,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        String reviewedBy = authentication.getName();
        
        ClinicalReviewResult result = clinicalDecisionService.reviewDecision(id, tenantId, request, reviewedBy);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/medication-alerts")
    @PreAuthorize("hasAnyRole('CLINICIAN', 'PHARMACIST', 'CLINICAL_REVIEWER', 'QUALITY_OFFICER', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Get medication alerts", 
               description = "Returns paginated list of medication safety alerts and drug interactions")
    public ResponseEntity<Page<MedicationAlertDTO>> getMedicationAlerts(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<MedicationAlertDTO> alerts = clinicalDecisionService.getMedicationAlerts(tenantId, severity, pageable);
        return ResponseEntity.ok(alerts);
    }
    
    @GetMapping("/care-gaps")
    @PreAuthorize("hasAnyRole('CLINICIAN', 'CARE_COORDINATOR', 'CLINICAL_REVIEWER', 'QUALITY_OFFICER', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Get care gaps", 
               description = "Returns paginated list of identified care gaps and preventive care opportunities")
    public ResponseEntity<Page<CareGapDTO>> getCareGaps(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<CareGapDTO> careGaps = clinicalDecisionService.getCareGaps(tenantId, status, pageable);
        return ResponseEntity.ok(careGaps);
    }
    
    @GetMapping("/risk-stratifications")
    @PreAuthorize("hasAnyRole('CLINICIAN', 'CLINICAL_REVIEWER', 'QUALITY_OFFICER', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Get risk stratifications", 
               description = "Returns paginated list of patient risk assessments and stratifications")
    public ResponseEntity<Page<RiskStratificationDTO>> getRiskStratifications(
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<RiskStratificationDTO> stratifications = clinicalDecisionService.getRiskStratifications(tenantId, riskLevel, pageable);
        return ResponseEntity.ok(stratifications);
    }
    
    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('CLINICIAN', 'CLINICAL_REVIEWER', 'QUALITY_OFFICER', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Get clinical decision metrics", 
               description = "Returns aggregated metrics for clinical decision support performance")
    public ResponseEntity<ClinicalMetrics> getMetrics(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        ClinicalMetrics metrics = clinicalDecisionService.getMetrics(tenantId, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('CLINICIAN', 'CLINICAL_REVIEWER', 'QUALITY_OFFICER', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Get performance trends", 
               description = "Returns historical trend data for clinical decision support outcomes")
    public ResponseEntity<ClinicalTrendData> getPerformanceTrends(
            @RequestParam(required = false, defaultValue = "30") int days,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        ClinicalTrendData trends = clinicalDecisionService.getPerformanceTrends(tenantId, days);
        return ResponseEntity.ok(trends);
    }
    
    private String getTenantId(Authentication authentication) {
        // Extract tenant ID from authentication
        // In a real implementation, this would come from the authenticated user's context
        return "default-tenant";
    }
}
