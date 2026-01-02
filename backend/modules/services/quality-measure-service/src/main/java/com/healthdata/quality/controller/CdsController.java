package com.healthdata.quality.controller;

import com.healthdata.quality.dto.*;
import com.healthdata.quality.service.CdsService;
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
import java.util.Map;
import java.util.UUID;

/**
 * Clinical Decision Support Controller
 *
 * Provides REST endpoints for CDS functionality including:
 * - Rule retrieval
 * - Rule evaluation for patients
 * - Recommendation management
 * - Acknowledgment tracking
 */
@RestController
@RequestMapping("/cds")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CdsController {

    private final CdsService cdsService;

    // ============================================
    // Rule Endpoints
    // ============================================

    /**
     * Get all available CDS rules
     *
     * GET /cds/rules
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'ANALYST', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/rules", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CdsRuleDTO>> getRules(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly
    ) {
        log.info("GET /cds/rules - tenant: {}, activeOnly: {}", tenantId, activeOnly);

        List<CdsRuleDTO> rules = activeOnly
            ? cdsService.getActiveRules(tenantId)
            : cdsService.getAllRules(tenantId);

        return ResponseEntity.ok(rules);
    }

    /**
     * Get CDS rules by category
     *
     * GET /cds/rules/category/{category}
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'ANALYST', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/rules/category/{category}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CdsRuleDTO>> getRulesByCategory(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable @NotBlank String category
    ) {
        log.info("GET /cds/rules/category/{} - tenant: {}", category, tenantId);
        List<CdsRuleDTO> rules = cdsService.getRulesByCategory(tenantId, category);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get a specific CDS rule by code
     *
     * GET /cds/rules/{ruleCode}
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'ANALYST', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/rules/{ruleCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CdsRuleDTO> getRuleByCode(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable @NotBlank String ruleCode
    ) {
        log.info("GET /cds/rules/{} - tenant: {}", ruleCode, tenantId);
        return cdsService.getRuleByCode(tenantId, ruleCode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ============================================
    // Recommendation Endpoints
    // ============================================

    /**
     * Get active CDS recommendations for a patient
     *
     * GET /cds/recommendations/{patientId}
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'CARE_COORDINATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/recommendations/{patientId:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CdsRecommendationDTO>> getRecommendations(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable UUID patientId
    ) {
        log.info("GET /cds/recommendations/{} - tenant: {}", patientId, tenantId);
        List<CdsRecommendationDTO> recommendations = cdsService.getActiveRecommendations(tenantId, patientId);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get urgent recommendations count for a patient
     *
     * GET /cds/recommendations/{patientId}/count
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'CARE_COORDINATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/recommendations/{patientId:.+}/count", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RecommendationCountResponse> getRecommendationCount(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable UUID patientId
    ) {
        log.info("GET /cds/recommendations/{}/count - tenant: {}", patientId, tenantId);

        Map<String, Long> countsByUrgency = cdsService.getRecommendationCountsByUrgency(tenantId, patientId);
        Long totalActive = cdsService.getActiveRecommendationCount(tenantId, patientId);

        RecommendationCountResponse response = RecommendationCountResponse.builder()
            .patientId(patientId)
            .totalActive(totalActive)
            .emergent(countsByUrgency.getOrDefault("EMERGENT", 0L))
            .urgent(countsByUrgency.getOrDefault("URGENT", 0L))
            .soon(countsByUrgency.getOrDefault("SOON", 0L))
            .routine(countsByUrgency.getOrDefault("ROUTINE", 0L))
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get overdue recommendations for a patient
     *
     * GET /cds/recommendations/{patientId}/overdue
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'CARE_COORDINATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/recommendations/{patientId:.+}/overdue", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CdsRecommendationDTO>> getOverdueRecommendations(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @PathVariable UUID patientId
    ) {
        log.info("GET /cds/recommendations/{}/overdue - tenant: {}", patientId, tenantId);
        List<CdsRecommendationDTO> recommendations = cdsService.getOverdueRecommendations(tenantId, patientId);
        return ResponseEntity.ok(recommendations);
    }

    // ============================================
    // Evaluation Endpoints
    // ============================================

    /**
     * Evaluate CDS rules for a patient
     *
     * POST /cds/evaluate
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/evaluate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CdsEvaluateResponse> evaluateRules(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @RequestBody @Valid CdsEvaluateRequest request
    ) {
        log.info("POST /cds/evaluate - patient: {}, tenant: {}", request.getPatientId(), tenantId);
        CdsEvaluateResponse response = cdsService.evaluateRules(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ============================================
    // Acknowledgment Endpoints
    // ============================================

    /**
     * Acknowledge or act on a CDS recommendation
     *
     * POST /cds/acknowledge
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'NURSE', 'CARE_COORDINATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/acknowledge", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CdsRecommendationDTO> acknowledgeRecommendation(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @RequestBody @Valid CdsAcknowledgeRequest request
    ) {
        log.info("POST /cds/acknowledge - recommendation: {}, action: {}, tenant: {}",
            request.getRecommendationId(), request.getAction(), tenantId);
        CdsRecommendationDTO recommendation = cdsService.acknowledgeRecommendation(tenantId, request);
        return ResponseEntity.ok(recommendation);
    }

    // ============================================
    // Response DTOs
    // ============================================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RecommendationCountResponse {
        private UUID patientId;
        private Long totalActive;
        private Long emergent;
        private Long urgent;
        private Long soon;
        private Long routine;
    }
}
