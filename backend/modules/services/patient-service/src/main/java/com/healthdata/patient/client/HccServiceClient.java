package com.healthdata.patient.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Feign client for HCC Service
 *
 * Provides access to patient HCC profiles and RAF scores for risk assessment.
 * Used by the risk stratification feature to display actual HCC-based risk levels
 * instead of simplified calculated scores.
 */
@FeignClient(
    name = "hcc-service",
    url = "${hcc.server.url:http://hcc-service:8088}",
    configuration = HccServiceClientConfiguration.class
)
public interface HccServiceClient {

    /**
     * Get HCC profile for a specific patient.
     *
     * @param tenantId tenant identifier
     * @param patientId patient UUID
     * @param year profile year (defaults to current year if null)
     * @return patient HCC profile with RAF scores and captured HCCs
     */
    @GetMapping("/api/v1/hcc/patient/{patientId}/profile")
    HccProfileResponse getPatientHccProfile(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable("patientId") UUID patientId,
        @RequestParam(value = "year", required = false) Integer year
    );

    /**
     * Get documentation gaps for a specific patient.
     *
     * @param tenantId tenant identifier
     * @param patientId patient UUID
     * @param year profile year (defaults to current year if null)
     * @return list of documentation gaps that could improve RAF score
     */
    @GetMapping("/api/v1/hcc/patient/{patientId}/documentation-gaps")
    List<DocumentationGapResponse> getDocumentationGaps(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable("patientId") UUID patientId,
        @RequestParam(value = "year", required = false) Integer year
    );

    /**
     * Health check endpoint.
     */
    @GetMapping("/actuator/health")
    String healthCheck();

    // ==================== Response DTOs ====================

    /**
     * HCC Profile response from hcc-service.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class HccProfileResponse {
        private UUID id;
        private String tenantId;
        private UUID patientId;
        private Integer profileYear;

        // RAF Scores
        private BigDecimal rafScoreV24;
        private BigDecimal rafScoreV28;
        private BigDecimal rafScoreBlended;

        // HCC Lists
        private List<String> hccsV24;
        private List<String> hccsV28;
        private List<String> diagnosisCodes;

        // Documentation Gaps
        private List<DocumentationGapSummary> documentationGaps;
        private Integer documentationGapCount;
        private BigDecimal potentialRafUplift;

        // Recapture
        private Integer recaptureOpportunitiesCount;
        private BigDecimal recaptureRafValue;

        // Metadata
        private LocalDateTime lastCalculatedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * Documentation gap summary for risk assessment display.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class DocumentationGapSummary {
        private String currentIcd10;
        private String recommendedIcd10;
        private String gapType;
        private BigDecimal rafImpact;
        private String guidance;
    }

    /**
     * Full documentation gap response from hcc-service.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class DocumentationGapResponse {
        private UUID id;
        private String tenantId;
        private UUID patientId;
        private Integer profileYear;
        private String gapType;
        private String currentIcd10;
        private String recommendedIcd10;
        private BigDecimal rafImpact;
        private String clinicalGuidance;
        private String status;
        private LocalDateTime identifiedAt;
        private LocalDateTime addressedAt;
    }
}
