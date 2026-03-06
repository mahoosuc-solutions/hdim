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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Tag(name = "Analytics Dashboards", description = "APIs for managing quality analytics dashboards, widgets, and data visualizations for payer/ACO quality reporting.")
@RestController
@RequestMapping("/api/analytics/dashboards")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
        summary = "List all dashboards",
        description = "Retrieves all quality analytics dashboards for the specified tenant. Returns dashboards configured for HEDIS measure tracking, Stars ratings monitoring, and care gap analysis visualizations.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboards retrieved successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied — insufficient role for dashboard access")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<DashboardDto>> getDashboards(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(dashboardService.getDashboards(tenantId));
    }

    @Operation(
        summary = "List dashboards with pagination",
        description = "Retrieves quality analytics dashboards with pagination support. Useful for organizations with many dashboards spanning multiple HEDIS measurement years or CMS reporting periods.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated dashboards retrieved successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied — insufficient role for dashboard access")
    })
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<Page<DashboardDto>> getDashboardsPaginated(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Pagination parameters (page, size, sort)")
            Pageable pageable) {
        return ResponseEntity.ok(dashboardService.getDashboardsPaginated(tenantId, pageable));
    }

    @Operation(
        summary = "List dashboards accessible to the current user",
        description = "Retrieves dashboards the authenticated user has access to based on their role and permissions. Ensures analysts only see dashboards relevant to their assigned quality measures or care gap programs.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Accessible dashboards retrieved successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied — insufficient role for dashboard access")
    })
    @GetMapping("/accessible")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<DashboardDto>> getAccessibleDashboards(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(dashboardService.getAccessibleDashboards(tenantId, userId));
    }

    @Operation(
        summary = "Get dashboard by ID",
        description = "Retrieves a specific quality analytics dashboard by its unique identifier. Returns the dashboard configuration including layout, widgets, measure filters, and visualization settings.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard retrieved successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Dashboard not found for the given ID and tenant"),
        @ApiResponse(responseCode = "403", description = "Access denied — insufficient role for dashboard access")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<DashboardDto> getDashboard(
            @Parameter(description = "Dashboard unique identifier", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return dashboardService.getDashboard(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Create a new dashboard",
        description = "Creates a new quality analytics dashboard for tracking HEDIS measures, Stars ratings, or care gap closure rates. The authenticated user is recorded as the dashboard owner.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Dashboard created successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid dashboard configuration"),
        @ApiResponse(responseCode = "403", description = "Access denied — ANALYST or ADMIN role required")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<DashboardDto> createDashboard(
            @Valid @RequestBody DashboardDto dto,
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            Authentication authentication) {
        String userId = authentication.getName();
        DashboardDto created = dashboardService.createDashboard(dto, tenantId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
        summary = "Update an existing dashboard",
        description = "Updates a quality analytics dashboard configuration including layout, title, description, and measure filters. Use this to adjust dashboards for new HEDIS measurement years or reporting periods.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard updated successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid dashboard configuration"),
        @ApiResponse(responseCode = "404", description = "Dashboard not found for the given ID and tenant"),
        @ApiResponse(responseCode = "403", description = "Access denied — ANALYST or ADMIN role required")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<DashboardDto> updateDashboard(
            @Parameter(description = "Dashboard unique identifier", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody DashboardDto dto,
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return dashboardService.updateDashboard(id, dto, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Delete a dashboard",
        description = "Permanently deletes a quality analytics dashboard and its associated configuration. This action cannot be undone. ADMIN role required to prevent accidental deletion of shared organizational dashboards.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Dashboard deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Dashboard not found for the given ID and tenant"),
        @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDashboard(
            @Parameter(description = "Dashboard unique identifier", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {
        if (dashboardService.deleteDashboard(id, tenantId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(
        summary = "Add a widget to a dashboard",
        description = "Creates a new data visualization widget for a quality analytics dashboard. Widgets can display HEDIS measure performance charts, care gap closure trends, Stars ratings comparisons, or CMS submission status indicators.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Widget created successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid widget configuration"),
        @ApiResponse(responseCode = "403", description = "Access denied — ANALYST or ADMIN role required")
    })
    @PostMapping("/widgets")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<WidgetDto> addWidget(
            @Valid @RequestBody WidgetDto dto,
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {
        WidgetDto created = dashboardService.addWidget(dto, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
        summary = "Update a widget",
        description = "Updates an existing dashboard widget configuration including chart type, data source, measure filters, date ranges, and display options for quality analytics visualizations.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Widget updated successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid widget configuration"),
        @ApiResponse(responseCode = "404", description = "Widget not found for the given ID and tenant"),
        @ApiResponse(responseCode = "403", description = "Access denied — ANALYST or ADMIN role required")
    })
    @PutMapping("/widgets/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<WidgetDto> updateWidget(
            @Parameter(description = "Widget unique identifier", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody WidgetDto dto,
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return dashboardService.updateWidget(id, dto, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Delete a widget",
        description = "Removes a data visualization widget from a quality analytics dashboard. This action cannot be undone.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Widget deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Widget not found for the given ID and tenant"),
        @ApiResponse(responseCode = "403", description = "Access denied — ANALYST or ADMIN role required")
    })
    @DeleteMapping("/widgets/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<Void> deleteWidget(
            @Parameter(description = "Widget unique identifier", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {
        if (dashboardService.deleteWidget(id, tenantId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
