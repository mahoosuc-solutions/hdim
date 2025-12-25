package com.healthdata.sales.controller;

import com.healthdata.sales.dto.PipelineMetricsDTO;
import com.healthdata.sales.dto.SalesDashboardDTO;
import com.healthdata.sales.service.DashboardService;
import com.healthdata.sales.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Dashboard", description = "Sales dashboard and metrics endpoints")
public class DashboardController {

    private final DashboardService dashboardService;
    private final OpportunityService opportunityService;

    @GetMapping
    @Operation(summary = "Get sales dashboard", description = "Comprehensive sales dashboard metrics")
    public ResponseEntity<SalesDashboardDTO> getDashboard(
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        return ResponseEntity.ok(dashboardService.getDashboard(tenantId));
    }

    @GetMapping("/leads")
    @Operation(summary = "Get lead metrics only")
    public ResponseEntity<SalesDashboardDTO.LeadMetrics> getLeadMetrics(
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        SalesDashboardDTO dashboard = dashboardService.getDashboard(tenantId);
        return ResponseEntity.ok(dashboard.getLeads());
    }

    @GetMapping("/pipeline")
    @Operation(summary = "Get pipeline metrics only")
    public ResponseEntity<PipelineMetricsDTO> getPipelineMetrics(
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        return ResponseEntity.ok(opportunityService.getPipelineMetrics(tenantId));
    }

    @GetMapping("/activities")
    @Operation(summary = "Get activity metrics only")
    public ResponseEntity<SalesDashboardDTO.ActivityMetrics> getActivityMetrics(
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        SalesDashboardDTO dashboard = dashboardService.getDashboard(tenantId);
        return ResponseEntity.ok(dashboard.getActivities());
    }

    @GetMapping("/accounts")
    @Operation(summary = "Get account metrics only")
    public ResponseEntity<SalesDashboardDTO.AccountMetrics> getAccountMetrics(
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        SalesDashboardDTO dashboard = dashboardService.getDashboard(tenantId);
        return ResponseEntity.ok(dashboard.getAccounts());
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent items", description = "Recent leads, opportunities, and activities")
    public ResponseEntity<SalesDashboardDTO.RecentItems> getRecentItems(
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        SalesDashboardDTO dashboard = dashboardService.getDashboard(tenantId);
        return ResponseEntity.ok(dashboard.getRecent());
    }
}
