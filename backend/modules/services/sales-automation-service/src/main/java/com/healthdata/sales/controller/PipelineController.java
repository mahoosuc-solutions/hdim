package com.healthdata.sales.controller;

import com.healthdata.sales.dto.*;
import com.healthdata.sales.entity.OpportunityStage;
import com.healthdata.sales.service.OpportunityService;
import com.healthdata.sales.service.PipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(
    name = "Pipeline",
    description = """
        APIs for sales pipeline management, Kanban visualization, and forecasting.

        The pipeline view provides:
        - Kanban board: Visual representation of deals by stage
        - Forecasting: Monthly and quarterly revenue projections
        - Conversion funnel: Lead-to-close conversion metrics
        - At-risk deals: Identification of stagnant or problematic opportunities
        - Stage transitions: Move deals through the pipeline with optional automation

        Pipeline stages follow the standard sales flow:
        Discovery → Demo → Proposal → Negotiation → Contract → Closed (Won/Lost)

        All endpoints require JWT authentication and X-Tenant-ID header.
        """
)
@SecurityRequirement(name = "bearer-jwt")
public class PipelineController {

    private final OpportunityService opportunityService;
    private final PipelineService pipelineService;

    // ==================== Kanban View ====================

    @GetMapping
    @Operation(
        summary = "Get pipeline Kanban view (simple)",
        description = """
            Retrieves opportunities grouped by stage for Kanban board visualization.

            Returns a map with stage names as keys and paginated opportunities as values.
            Excludes closed stages (CLOSED_WON, CLOSED_LOST) from the main view.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved Kanban view"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Page<OpportunityDTO>>> getSimpleKanbanView(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Pagination parameters for each stage")
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
    @Operation(
        summary = "Get pipeline Kanban view (detailed)",
        description = """
            Retrieves a detailed Kanban view with enriched opportunity data.

            Each opportunity includes:
            - Account name and contact information
            - Value and probability
            - Days in current stage
            - Next activity scheduled
            - Pipeline summary with totals
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved detailed Kanban view")
    })
    public ResponseEntity<PipelineKanbanDTO> getKanbanView(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        return ResponseEntity.ok(pipelineService.getKanbanView(tenantId));
    }

    // ==================== Metrics & Forecasting ====================

    @GetMapping("/metrics")
    @Operation(
        summary = "Get pipeline metrics",
        description = """
            Retrieves key pipeline performance metrics.

            Includes:
            - Total pipeline value and weighted value
            - Stage-by-stage breakdown
            - Win rate and average deal size
            - Sales velocity metrics
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved pipeline metrics")
    })
    public ResponseEntity<PipelineMetricsDTO> getMetrics(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        return ResponseEntity.ok(opportunityService.getPipelineMetrics(tenantId));
    }

    @GetMapping("/forecast")
    @Operation(
        summary = "Get sales forecast",
        description = """
            Retrieves comprehensive sales forecast with projections.

            Includes:
            - Monthly forecasts for next 6 months
            - Quarterly rollup projections
            - Conversion funnel metrics
            - Historical comparisons
            - At-risk deals identification
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved forecast")
    })
    public ResponseEntity<PipelineForecastDTO> getForecast(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        return ResponseEntity.ok(pipelineService.getForecast(tenantId));
    }

    @GetMapping("/forecast/monthly")
    @Operation(
        summary = "Get monthly forecasts",
        description = "Retrieves forecast breakdown by month for the next 6 months based on expected close dates."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved monthly forecasts")
    })
    public ResponseEntity<List<PipelineForecastDTO.MonthlyForecast>> getMonthlyForecasts(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        PipelineForecastDTO forecast = pipelineService.getForecast(tenantId);
        return ResponseEntity.ok(forecast.getMonthlyForecasts());
    }

    @GetMapping("/funnel")
    @Operation(
        summary = "Get conversion funnel",
        description = """
            Retrieves lead-to-close conversion funnel metrics.

            Shows conversion rates between each stage:
            Lead → Discovery → Demo → Proposal → Negotiation → Contract → Won
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved conversion funnel")
    })
    public ResponseEntity<PipelineForecastDTO.ConversionFunnel> getConversionFunnel(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        PipelineForecastDTO forecast = pipelineService.getForecast(tenantId);
        return ResponseEntity.ok(forecast.getFunnel());
    }

    @GetMapping("/historical")
    @Operation(
        summary = "Get historical comparison",
        description = "Retrieves historical revenue comparison: last month, last quarter, and year-over-year."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved historical comparison")
    })
    public ResponseEntity<PipelineForecastDTO.HistoricalComparison> getHistoricalComparison(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        PipelineForecastDTO forecast = pipelineService.getForecast(tenantId);
        return ResponseEntity.ok(forecast.getHistorical());
    }

    // ==================== Stage Management ====================

    @PostMapping("/opportunities/{opportunityId}/move")
    @Operation(
        summary = "Move opportunity to new stage",
        description = """
            Transitions an opportunity to a new pipeline stage with workflow automation.

            Can optionally:
            - Add a note about the transition
            - Create a follow-up activity
            - Update probability based on new stage
            - Send notifications to stakeholders
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stage transition successful"),
        @ApiResponse(responseCode = "400", description = "Invalid stage transition"),
        @ApiResponse(responseCode = "404", description = "Opportunity not found")
    })
    public ResponseEntity<OpportunityDTO> moveOpportunityToStage(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Opportunity ID", required = true)
        @PathVariable UUID opportunityId,
        @Parameter(description = "Stage transition details", required = true)
        @Valid @RequestBody StageTransitionRequest request
    ) {
        OpportunityDTO result = pipelineService.moveToStage(tenantId, opportunityId, request);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/opportunities/{opportunityId}/stage")
    @Operation(
        summary = "Quick stage update",
        description = "Simple stage update without the full transition workflow. Use for quick drag-and-drop operations."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stage updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid stage value"),
        @ApiResponse(responseCode = "404", description = "Opportunity not found")
    })
    public ResponseEntity<OpportunityDTO> updateStage(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Opportunity ID", required = true)
        @PathVariable UUID opportunityId,
        @Parameter(description = "New stage", required = true,
            schema = @Schema(allowableValues = {"DISCOVERY", "DEMO", "PROPOSAL", "NEGOTIATION", "CONTRACT", "CLOSED_WON", "CLOSED_LOST"}))
        @RequestParam OpportunityStage stage
    ) {
        return ResponseEntity.ok(opportunityService.updateStage(tenantId, opportunityId, stage));
    }

    // ==================== Deal Analysis ====================

    @GetMapping("/closing-soon")
    @Operation(
        summary = "Get opportunities closing soon",
        description = "Retrieves open opportunities with expected close dates within the specified number of days."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved closing soon opportunities")
    })
    public ResponseEntity<List<OpportunityDTO>> getClosingSoon(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Number of days to look ahead", example = "30",
            schema = @Schema(defaultValue = "30", minimum = "1", maximum = "365"))
        @RequestParam(defaultValue = "30") int withinDays
    ) {
        return ResponseEntity.ok(pipelineService.getClosingSoon(tenantId, withinDays));
    }

    @GetMapping("/stagnant")
    @Operation(
        summary = "Get stagnant opportunities",
        description = """
            Retrieves opportunities with no activity in the specified number of days.

            Stagnant deals may indicate:
            - Lost prospect interest
            - Need for follow-up
            - Incorrect close date estimates
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved stagnant opportunities")
    })
    public ResponseEntity<List<OpportunityDTO>> getStagnantOpportunities(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Number of days without activity", example = "14",
            schema = @Schema(defaultValue = "14", minimum = "1", maximum = "90"))
        @RequestParam(defaultValue = "14") int stagnantDays
    ) {
        return ResponseEntity.ok(pipelineService.getStagnantOpportunities(tenantId, stagnantDays));
    }

    @GetMapping("/at-risk")
    @Operation(
        summary = "Get at-risk deals",
        description = """
            Retrieves deals identified as at-risk with reasons and recommended actions.

            Risk factors include:
            - Overdue close date
            - Stagnant activity
            - Large deal value with low probability
            - Long time in current stage
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved at-risk deals")
    })
    public ResponseEntity<List<PipelineForecastDTO.AtRiskDeal>> getAtRiskDeals(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        PipelineForecastDTO forecast = pipelineService.getForecast(tenantId);
        return ResponseEntity.ok(forecast.getAtRiskDeals());
    }

    // ==================== Summary Endpoints ====================

    @GetMapping("/summary")
    @Operation(
        summary = "Get pipeline summary",
        description = "Retrieves high-level pipeline summary including totals, stage breakdown, and key metrics."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved pipeline summary")
    })
    public ResponseEntity<PipelineKanbanDTO.PipelineSummary> getPipelineSummary(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        PipelineKanbanDTO kanban = pipelineService.getKanbanView(tenantId);
        return ResponseEntity.ok(kanban.getSummary());
    }

    // ==================== Closed Deals ====================

    @GetMapping("/closed-won")
    @Operation(
        summary = "Get closed won opportunities",
        description = "Retrieves successfully closed deals for reporting and analysis."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved closed won opportunities")
    })
    public ResponseEntity<Page<OpportunityDTO>> getClosedWon(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(opportunityService.findByStage(tenantId, OpportunityStage.CLOSED_WON, pageable));
    }

    @GetMapping("/closed-lost")
    @Operation(
        summary = "Get closed lost opportunities",
        description = "Retrieves lost deals for loss analysis and improvement opportunities."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved closed lost opportunities")
    })
    public ResponseEntity<Page<OpportunityDTO>> getClosedLost(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(opportunityService.findByStage(tenantId, OpportunityStage.CLOSED_LOST, pageable));
    }
}
