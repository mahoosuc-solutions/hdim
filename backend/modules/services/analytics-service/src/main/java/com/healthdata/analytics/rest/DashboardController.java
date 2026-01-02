package com.healthdata.analytics.rest;

import com.healthdata.analytics.dto.DashboardDto;
import com.healthdata.analytics.dto.WidgetDto;
import com.healthdata.analytics.service.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics/dashboards")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<DashboardDto>> getDashboards(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(dashboardService.getDashboards(tenantId));
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<Page<DashboardDto>> getDashboardsPaginated(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(dashboardService.getDashboardsPaginated(tenantId, pageable));
    }

    @GetMapping("/accessible")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<DashboardDto>> getAccessibleDashboards(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(dashboardService.getAccessibleDashboards(tenantId, userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<DashboardDto> getDashboard(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return dashboardService.getDashboard(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<DashboardDto> createDashboard(
            @Valid @RequestBody DashboardDto dto,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Authentication authentication) {
        String userId = authentication.getName();
        DashboardDto created = dashboardService.createDashboard(dto, tenantId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<DashboardDto> updateDashboard(
            @PathVariable UUID id,
            @Valid @RequestBody DashboardDto dto,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return dashboardService.updateDashboard(id, dto, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDashboard(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        if (dashboardService.deleteDashboard(id, tenantId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/widgets")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<WidgetDto> addWidget(
            @Valid @RequestBody WidgetDto dto,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        WidgetDto created = dashboardService.addWidget(dto, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/widgets/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<WidgetDto> updateWidget(
            @PathVariable UUID id,
            @Valid @RequestBody WidgetDto dto,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return dashboardService.updateWidget(id, dto, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/widgets/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<Void> deleteWidget(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        if (dashboardService.deleteWidget(id, tenantId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
