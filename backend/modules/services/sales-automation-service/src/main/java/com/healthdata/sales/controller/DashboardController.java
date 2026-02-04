package com.healthdata.sales.controller;

import com.healthdata.sales.dto.PipelineMetricsDTO;
import com.healthdata.sales.dto.SalesDashboardDTO;
import com.healthdata.sales.service.DashboardService;
import com.healthdata.sales.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sales/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Dashboard",
    description = """
        APIs for sales dashboard and key performance metrics.

        The dashboard provides a comprehensive view of sales performance:
        - Lead metrics: total, new, qualified, converted
        - Pipeline metrics: total value, weighted value, stage breakdown
        - Activity metrics: completed, pending, overdue
        - Account metrics: total, active, by stage
        - Recent items: latest leads, opportunities, activities

        Designed for real-time sales team visibility and management reporting.

        All endpoints require JWT authentication and X-Tenant-ID header.
        """
)
@SecurityRequirement(name = "bearer-jwt")
public class DashboardController {

    private final DashboardService dashboardService;
    private final OpportunityService opportunityService;

    @GetMapping
    @Operation(
        summary = "Get sales dashboard",
        description = """
            Retrieves comprehensive sales dashboard with all metrics.

            Includes lead metrics, pipeline metrics, activity metrics,
            account metrics, and recent items in a single response.
            Optimized for dashboard rendering.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved dashboard"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<SalesDashboardDTO> getDashboard(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        return ResponseEntity.ok(dashboardService.getDashboard(tenantId));
    }

    @GetMapping("/leads")
    @Operation(
        summary = "Get lead metrics only",
        description = "Retrieves lead-specific metrics: total, new (last 7 days), qualified, converted."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved lead metrics")
    })
    public ResponseEntity<SalesDashboardDTO.LeadMetrics> getLeadMetrics(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        SalesDashboardDTO dashboard = dashboardService.getDashboard(tenantId);
        return ResponseEntity.ok(dashboard.getLeads());
    }

    @GetMapping("/pipeline")
    @Operation(
        summary = "Get pipeline metrics only",
        description = """
            Retrieves pipeline-specific metrics.

            Includes:
            - Total pipeline value (sum of all open opportunities)
            - Weighted pipeline value (adjusted by probability)
            - Stage breakdown with counts and values
            - Win rate and average deal size
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved pipeline metrics")
    })
    public ResponseEntity<PipelineMetricsDTO> getPipelineMetrics(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        return ResponseEntity.ok(opportunityService.getPipelineMetrics(tenantId));
    }

    @GetMapping("/activities")
    @Operation(
        summary = "Get activity metrics only",
        description = "Retrieves activity-specific metrics: completed, pending, overdue, by type breakdown."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved activity metrics")
    })
    public ResponseEntity<SalesDashboardDTO.ActivityMetrics> getActivityMetrics(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        SalesDashboardDTO dashboard = dashboardService.getDashboard(tenantId);
        return ResponseEntity.ok(dashboard.getActivities());
    }

    @GetMapping("/accounts")
    @Operation(
        summary = "Get account metrics only",
        description = "Retrieves account-specific metrics: total, by stage, active customers."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved account metrics")
    })
    public ResponseEntity<SalesDashboardDTO.AccountMetrics> getAccountMetrics(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        SalesDashboardDTO dashboard = dashboardService.getDashboard(tenantId);
        return ResponseEntity.ok(dashboard.getAccounts());
    }

    @GetMapping("/recent")
    @Operation(
        summary = "Get recent items",
        description = "Retrieves the most recent leads, opportunities, and activities for quick access."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved recent items")
    })
    public ResponseEntity<SalesDashboardDTO.RecentItems> getRecentItems(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        SalesDashboardDTO dashboard = dashboardService.getDashboard(tenantId);
        return ResponseEntity.ok(dashboard.getRecent());
    }
}
