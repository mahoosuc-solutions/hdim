package com.healthdata.predictive.controller;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.predictive.dto.*;
import com.healthdata.predictive.entity.InsightDismissalEntity;
import com.healthdata.predictive.service.PopulationHealthInsightsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Issue #19: Population Health Insights Engine
 *
 * REST API for population health insights.
 *
 * Endpoints:
 * - GET  /api/v1/providers/{providerId}/insights - Get population health insights
 * - POST /api/v1/providers/{providerId}/insights/dismiss - Dismiss an insight
 * - POST /api/v1/providers/{providerId}/insights/restore - Restore a dismissed insight
 * - GET  /api/v1/providers/{providerId}/insights/dismissed - Get dismissed insights
 *
 * HIPAA Compliance:
 * - All responses include Cache-Control: no-store headers
 * - All PHI access is audited
 * - Multi-tenant filtering enforced via X-Tenant-ID header
 */
@RestController
@RequestMapping("/api/v1/providers/{providerId}/insights")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Population Insights", description = "AI-powered population health insights for provider panels")
public class PopulationInsightsController {

    private final PopulationHealthInsightsService insightsService;

    /**
     * Get population health insights for a provider's patient panel.
     *
     * Returns AI-generated insights about:
     * - Care Gap Clusters: >10 patients with the same gap
     * - Performance Trends: >5% change in metrics over 30 days
     * - At-Risk Populations: Patients with increasing risk scores
     * - Intervention Opportunities: Similar gaps across similar patients
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission('report:read') or @providerSecurity.isProvider(#providerId)")
    @Operation(summary = "Get population health insights",
               description = "Returns AI-generated insights about the provider's patient panel")
    @Audited(action = AuditAction.READ, resourceType = "PopulationInsights",
             description = "View population health insights")
    public ResponseEntity<PopulationInsightsResponse> getInsights(
            @Parameter(description = "Provider's unique identifier")
            @PathVariable String providerId,
            @Parameter(description = "Tenant identifier")
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody(required = false) Map<String, Object> panelData,
            HttpServletResponse response) {

        // HIPAA: Set cache control headers
        setCacheControlHeaders(response);

        log.info("GET /api/v1/providers/{}/insights - tenant: {}", providerId, tenantId);

        if (tenantId == null || tenantId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // If no panel data provided, use empty map
        if (panelData == null) {
            panelData = Map.of();
        }

        PopulationInsightsResponse insights = insightsService.generateInsights(
                tenantId, providerId, panelData
        );

        return ResponseEntity.ok(insights);
    }

    /**
     * Generate insights with panel data.
     * POST endpoint for cases where panel data needs to be provided.
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission('report:create') or @providerSecurity.isProvider(#providerId)")
    @Operation(summary = "Generate population health insights with panel data",
               description = "Analyzes provided panel data to generate AI-powered insights")
    @Audited(action = AuditAction.READ, resourceType = "PopulationInsights",
             description = "Generate population health insights")
    public ResponseEntity<PopulationInsightsResponse> generateInsights(
            @Parameter(description = "Provider's unique identifier")
            @PathVariable String providerId,
            @Parameter(description = "Tenant identifier")
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Panel data including care gaps, metrics, and risk scores")
            @RequestBody Map<String, Object> panelData,
            HttpServletResponse response) {

        setCacheControlHeaders(response);

        log.info("POST /api/v1/providers/{}/insights - tenant: {}", providerId, tenantId);

        if (tenantId == null || tenantId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        PopulationInsightsResponse insights = insightsService.generateInsights(
                tenantId, providerId, panelData
        );

        return ResponseEntity.ok(insights);
    }

    /**
     * Dismiss an insight with reason tracking.
     */
    @PostMapping(value = "/dismiss", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission('report:write') or @providerSecurity.isProvider(#providerId)")
    @Operation(summary = "Dismiss an insight",
               description = "Dismisses an insight with a required reason for tracking")
    @Audited(action = AuditAction.UPDATE, resourceType = "PopulationInsight",
             description = "Dismiss population insight")
    public ResponseEntity<Map<String, String>> dismissInsight(
            @PathVariable String providerId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-Auth-User-Id", required = false) String userId,
            @Valid @RequestBody InsightDismissalRequest request,
            HttpServletResponse response) {

        setCacheControlHeaders(response);

        log.info("POST /api/v1/providers/{}/insights/dismiss - tenant: {}, insightId: {}",
                providerId, tenantId, request.getInsightId());

        if (tenantId == null || tenantId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        insightsService.dismissInsight(tenantId, providerId, userId, request);

        return ResponseEntity.ok(Map.of(
                "status", "dismissed",
                "insightId", request.getInsightId().toString()
        ));
    }

    /**
     * Restore a dismissed insight.
     */
    @PostMapping(value = "/restore/{insightId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission('report:write') or @providerSecurity.isProvider(#providerId)")
    @Operation(summary = "Restore a dismissed insight",
               description = "Restores a previously dismissed insight")
    @Audited(action = AuditAction.UPDATE, resourceType = "PopulationInsight",
             description = "Restore dismissed insight")
    public ResponseEntity<Map<String, String>> restoreInsight(
            @PathVariable String providerId,
            @PathVariable UUID insightId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            HttpServletResponse response) {

        setCacheControlHeaders(response);

        log.info("POST /api/v1/providers/{}/insights/restore/{} - tenant: {}",
                providerId, insightId, tenantId);

        if (tenantId == null || tenantId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        insightsService.restoreInsight(tenantId, providerId, insightId);

        return ResponseEntity.ok(Map.of(
                "status", "restored",
                "insightId", insightId.toString()
        ));
    }

    /**
     * Get dismissed insights for tracking.
     */
    @GetMapping(value = "/dismissed", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission('report:read') or @providerSecurity.isProvider(#providerId)")
    @Operation(summary = "Get dismissed insights",
               description = "Returns list of dismissed insights for the provider")
    public ResponseEntity<List<InsightDismissalEntity>> getDismissedInsights(
            @PathVariable String providerId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            HttpServletResponse response) {

        setCacheControlHeaders(response);

        log.info("GET /api/v1/providers/{}/insights/dismissed - tenant: {}", providerId, tenantId);

        if (tenantId == null || tenantId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<InsightDismissalEntity> dismissed = insightsService.getDismissedInsights(tenantId, providerId);
        return ResponseEntity.ok(dismissed);
    }

    /**
     * Set HIPAA-compliant cache control headers.
     */
    private void setCacheControlHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
    }
}
