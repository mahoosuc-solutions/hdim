package com.healthdata.analytics.rest;

import com.healthdata.analytics.dto.KpiSummaryDto;
import com.healthdata.analytics.service.KpiService;
import com.healthdata.analytics.service.MetricAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics/kpis")
@RequiredArgsConstructor
@Slf4j
public class KpiController {

    private final KpiService kpiService;
    private final MetricAggregationService aggregationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllKpis(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(kpiService.getAllKpis(tenantId));
    }

    @GetMapping("/quality")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<KpiSummaryDto>> getQualityKpis(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(kpiService.getQualityKpis(tenantId));
    }

    @GetMapping("/hcc")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<KpiSummaryDto>> getHccKpis(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(kpiService.getHccKpis(tenantId));
    }

    @GetMapping("/care-gaps")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<KpiSummaryDto>> getCareGapKpis(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(kpiService.getCareGapKpis(tenantId));
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<KpiSummaryDto>> getTrends(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "QUALITY_SCORE") String metricType,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(kpiService.getTrends(tenantId, metricType, days));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getSnapshotStatistics(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(aggregationService.getSnapshotStatistics(tenantId));
    }

    @PostMapping("/capture")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> captureSnapshots(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        aggregationService.captureKpiSnapshots(tenantId);
        return ResponseEntity.accepted().build();
    }
}
