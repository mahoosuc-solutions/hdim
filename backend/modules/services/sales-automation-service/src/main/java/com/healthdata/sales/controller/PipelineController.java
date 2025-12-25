package com.healthdata.sales.controller;

import com.healthdata.sales.dto.*;
import com.healthdata.sales.entity.OpportunityStage;
import com.healthdata.sales.service.OpportunityService;
import com.healthdata.sales.service.PipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pipeline management controller for Kanban views, forecasting, and stage management
 */
@RestController
@RequestMapping("/api/sales/pipeline")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pipeline", description = "Sales pipeline management, Kanban view, and forecasting")
public class PipelineController {

    private final OpportunityService opportunityService;
    private final PipelineService pipelineService;

    // ==================== Kanban View ====================

    @GetMapping
    @Operation(summary = "Get pipeline Kanban view (simple)",
               description = "Get opportunities grouped by stage for Kanban board")
    public ResponseEntity<Map<String, Page<OpportunityDTO>>> getSimpleKanbanView(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        Pageable pageable
    ) {
        Map<String, Page<OpportunityDTO>> kanbanData = new LinkedHashMap<>();

        // Get opportunities for each stage (excluding closed stages for main view)
        kanbanData.put("DISCOVERY", opportunityService.findByStage(tenantId, OpportunityStage.DISCOVERY, pageable));
        kanbanData.put("DEMO", opportunityService.findByStage(tenantId, OpportunityStage.DEMO, pageable));
        kanbanData.put("PROPOSAL", opportunityService.findByStage(tenantId, OpportunityStage.PROPOSAL, pageable));
        kanbanData.put("NEGOTIATION", opportunityService.findByStage(tenantId, OpportunityStage.NEGOTIATION, pageable));
        kanbanData.put("CONTRACT", opportunityService.findByStage(tenantId, OpportunityStage.CONTRACT, pageable));

        return ResponseEntity.ok(kanbanData);
    }

    @GetMapping("/kanban")
    @Operation(summary = "Get pipeline Kanban view (detailed)",
               description = "Returns opportunities organized by stage with account/contact info and metrics")
    public ResponseEntity<PipelineKanbanDTO> getKanbanView(
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        return ResponseEntity.ok(pipelineService.getKanbanView(tenantId));
    }

    // ==================== Metrics & Forecasting ====================

    @GetMapping("/metrics")
    @Operation(summary = "Get pipeline metrics", description = "Get sales metrics and forecasting data")
    public ResponseEntity<PipelineMetricsDTO> getMetrics(
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        return ResponseEntity.ok(opportunityService.getPipelineMetrics(tenantId));
    }

    @GetMapping("/forecast")
    @Operation(summary = "Get sales forecast",
               description = "Returns comprehensive pipeline forecast with monthly/quarterly projections")
    public ResponseEntity<PipelineForecastDTO> getForecast(
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        return ResponseEntity.ok(pipelineService.getForecast(tenantId));
    }

    @GetMapping("/forecast/monthly")
    @Operation(summary = "Get monthly forecasts",
               description = "Returns forecast breakdown by month for next 6 months")
    public ResponseEntity<List<PipelineForecastDTO.MonthlyForecast>> getMonthlyForecasts(
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        PipelineForecastDTO forecast = pipelineService.getForecast(tenantId);
        return ResponseEntity.ok(forecast.getMonthlyForecasts());
    }

    @GetMapping("/funnel")
    @Operation(summary = "Get conversion funnel",
               description = "Returns lead-to-close conversion funnel metrics")
    public ResponseEntity<PipelineForecastDTO.ConversionFunnel> getConversionFunnel(
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        PipelineForecastDTO forecast = pipelineService.getForecast(tenantId);
        return ResponseEntity.ok(forecast.getFunnel());
    }

    @GetMapping("/historical")
    @Operation(summary = "Get historical comparison",
               description = "Returns historical revenue comparison (last month, quarter, year)")
    public ResponseEntity<PipelineForecastDTO.HistoricalComparison> getHistoricalComparison(
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        PipelineForecastDTO forecast = pipelineService.getForecast(tenantId);
        return ResponseEntity.ok(forecast.getHistorical());
    }

    // ==================== Stage Management ====================

    @PostMapping("/opportunities/{opportunityId}/move")
    @Operation(summary = "Move opportunity to new stage",
               description = "Transitions opportunity to a new pipeline stage with optional workflow automation")
    public ResponseEntity<OpportunityDTO> moveOpportunityToStage(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID opportunityId,
        @Valid @RequestBody StageTransitionRequest request
    ) {
        OpportunityDTO result = pipelineService.moveToStage(tenantId, opportunityId, request);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/opportunities/{opportunityId}/stage")
    @Operation(summary = "Quick stage update",
               description = "Simple stage update without full transition workflow")
    public ResponseEntity<OpportunityDTO> updateStage(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID opportunityId,
        @RequestParam OpportunityStage stage
    ) {
        return ResponseEntity.ok(opportunityService.updateStage(tenantId, opportunityId, stage));
    }

    // ==================== Deal Analysis ====================

    @GetMapping("/closing-soon")
    @Operation(summary = "Get opportunities closing soon",
               description = "Returns open opportunities with expected close date within specified days")
    public ResponseEntity<List<OpportunityDTO>> getClosingSoon(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @RequestParam(defaultValue = "30") int withinDays
    ) {
        return ResponseEntity.ok(pipelineService.getClosingSoon(tenantId, withinDays));
    }

    @GetMapping("/stagnant")
    @Operation(summary = "Get stagnant opportunities",
               description = "Returns opportunities with no activity in the specified number of days")
    public ResponseEntity<List<OpportunityDTO>> getStagnantOpportunities(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @RequestParam(defaultValue = "14") int stagnantDays
    ) {
        return ResponseEntity.ok(pipelineService.getStagnantOpportunities(tenantId, stagnantDays));
    }

    @GetMapping("/at-risk")
    @Operation(summary = "Get at-risk deals",
               description = "Returns deals identified as at-risk with reasons and recommended actions")
    public ResponseEntity<List<PipelineForecastDTO.AtRiskDeal>> getAtRiskDeals(
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        PipelineForecastDTO forecast = pipelineService.getForecast(tenantId);
        return ResponseEntity.ok(forecast.getAtRiskDeals());
    }

    // ==================== Summary Endpoints ====================

    @GetMapping("/summary")
    @Operation(summary = "Get pipeline summary",
               description = "Returns high-level pipeline summary including totals and stage breakdown")
    public ResponseEntity<PipelineKanbanDTO.PipelineSummary> getPipelineSummary(
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        PipelineKanbanDTO kanban = pipelineService.getKanbanView(tenantId);
        return ResponseEntity.ok(kanban.getSummary());
    }

    // ==================== Closed Deals ====================

    @GetMapping("/closed-won")
    @Operation(summary = "Get closed won opportunities")
    public ResponseEntity<Page<OpportunityDTO>> getClosedWon(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(opportunityService.findByStage(tenantId, OpportunityStage.CLOSED_WON, pageable));
    }

    @GetMapping("/closed-lost")
    @Operation(summary = "Get closed lost opportunities")
    public ResponseEntity<Page<OpportunityDTO>> getClosedLost(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(opportunityService.findByStage(tenantId, OpportunityStage.CLOSED_LOST, pageable));
    }
}
