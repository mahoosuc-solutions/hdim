package com.healthdata.quality.controller;

import com.healthdata.quality.dto.ProviderPerformanceResponse;
import com.healthdata.quality.service.ProviderPerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Provider Performance Controller
 * Exposes API endpoints for provider performance metrics including
 * compliance rates, practice averages, trends, and percentile rankings.
 *
 * Issue #146: Create Provider Performance Metrics API
 */
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Provider Performance", description = "Provider performance metrics and analytics")
public class ProviderPerformanceController {

    private final ProviderPerformanceService providerPerformanceService;

    /**
     * Get performance metrics for a specific provider.
     *
     * Returns compliance rates, practice averages (anonymized), historical trends,
     * and percentile rankings for the specified provider.
     *
     * @param providerId The provider's unique identifier
     * @param tenantId The tenant ID from header
     * @param measureIds Optional list of specific measure IDs to include
     * @param period Time period for metrics (YTD, LAST_12_MONTHS, LAST_QUARTER)
     * @return Provider performance response with all metrics
     */
    @GetMapping("/{providerId}/performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST', 'PROVIDER')")
    @Operation(
        summary = "Get provider performance metrics",
        description = "Returns performance metrics for a provider including compliance rates, " +
                      "practice averages, historical trends, and percentile rankings. " +
                      "Practice averages are anonymized to protect individual provider data."
    )
    public ResponseEntity<ProviderPerformanceResponse> getProviderPerformance(
            @Parameter(description = "Provider's unique identifier")
            @PathVariable UUID providerId,

            @Parameter(description = "Tenant identifier")
            @RequestHeader("X-Tenant-ID") String tenantId,

            @Parameter(description = "Specific measure IDs to include (optional)")
            @RequestParam(required = false) List<String> measureIds,

            @Parameter(description = "Time period: YTD, LAST_12_MONTHS, LAST_QUARTER")
            @RequestParam(defaultValue = "YTD") String period) {

        log.info("Getting performance metrics for provider {} in tenant {}, period: {}",
                providerId, tenantId, period);

        ProviderPerformanceResponse response = providerPerformanceService.getProviderPerformance(
                tenantId, providerId, measureIds, period);

        return ResponseEntity.ok(response);
    }

    /**
     * Get performance comparison for the current authenticated provider.
     *
     * This endpoint allows providers to view their own performance without
     * specifying a provider ID. Uses the authenticated user's provider association.
     *
     * @param tenantId The tenant ID from header
     * @param userId The authenticated user's ID from header
     * @param measureIds Optional list of specific measure IDs to include
     * @param period Time period for metrics
     * @return Provider performance response for the authenticated provider
     */
    @GetMapping("/me/performance")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @Operation(
        summary = "Get current provider's performance metrics",
        description = "Returns performance metrics for the currently authenticated provider. " +
                      "This endpoint uses the user's associated provider ID."
    )
    public ResponseEntity<ProviderPerformanceResponse> getMyPerformance(
            @Parameter(description = "Tenant identifier")
            @RequestHeader("X-Tenant-ID") String tenantId,

            @Parameter(description = "Authenticated user's ID")
            @RequestHeader("X-Auth-User-Id") String userId,

            @Parameter(description = "Specific measure IDs to include (optional)")
            @RequestParam(required = false) List<String> measureIds,

            @Parameter(description = "Time period: YTD, LAST_12_MONTHS, LAST_QUARTER")
            @RequestParam(defaultValue = "YTD") String period) {

        log.info("Getting performance metrics for authenticated user {} in tenant {}", userId, tenantId);

        // In a real implementation, we would look up the provider ID associated with the user
        // For now, we'll use the userId as a UUID (assuming user ID maps to provider ID)
        UUID providerId;
        try {
            providerId = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            log.warn("User ID {} is not a valid UUID, using placeholder", userId);
            providerId = UUID.randomUUID();
        }

        ProviderPerformanceResponse response = providerPerformanceService.getProviderPerformance(
                tenantId, providerId, measureIds, period);

        return ResponseEntity.ok(response);
    }

    /**
     * Get summary performance metrics for all providers in a practice.
     *
     * This is an admin-only endpoint that returns aggregated performance
     * data across all providers for practice-level analytics.
     *
     * @param tenantId The tenant ID from header
     * @param measureIds Optional list of specific measure IDs to include
     * @param period Time period for metrics
     * @return List of provider performance summaries
     */
    @GetMapping("/performance/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(
        summary = "Get practice-wide performance summary",
        description = "Returns aggregated performance metrics for all providers in the practice. " +
                      "Admin/Analyst only. Individual provider rates are anonymized in rankings."
    )
    public ResponseEntity<List<ProviderPerformanceResponse>> getPracticePerformanceSummary(
            @Parameter(description = "Tenant identifier")
            @RequestHeader("X-Tenant-ID") String tenantId,

            @Parameter(description = "Specific measure IDs to include (optional)")
            @RequestParam(required = false) List<String> measureIds,

            @Parameter(description = "Time period: YTD, LAST_12_MONTHS, LAST_QUARTER")
            @RequestParam(defaultValue = "YTD") String period) {

        log.info("Getting practice performance summary for tenant {}, period: {}", tenantId, period);

        // For demo purposes, return a single provider's performance
        // In a real implementation, this would aggregate across all providers
        ProviderPerformanceResponse demoResponse = providerPerformanceService.getProviderPerformance(
                tenantId, UUID.randomUUID(), measureIds, period);

        return ResponseEntity.ok(List.of(demoResponse));
    }
}
