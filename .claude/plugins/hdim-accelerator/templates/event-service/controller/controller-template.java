package com.healthdata.{{DOMAIN}}event.api;

import com.healthdata.{{DOMAIN}}event.dto.{{DOMAIN_PASCAL}}Statistics;
import com.healthdata.{{DOMAIN}}event.projection.{{DOMAIN_PASCAL}}Projection;
import com.healthdata.{{DOMAIN}}event.repository.{{DOMAIN_PASCAL}}ProjectionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * {{DOMAIN_PASCAL}}ProjectionController - CQRS Read Model API
 *
 * Provides query endpoints for {{DOMAIN_PASCAL}}Projection read model.
 * Data is eventually consistent (< 500ms from event publication).
 *
 * Security:
 * - All endpoints require authentication via gateway trust headers
 * - Role-based access control via @PreAuthorize
 * - Multi-tenant isolation via X-Tenant-ID header
 *
 * Performance:
 * - Query response time: < 100ms (99th percentile)
 * - Denormalized data enables fast single-table queries
 * - Indexed on tenant_id for multi-tenant performance
 *
 * HIPAA Compliance:
 * - May contain PHI - Cache-Control: no-store header enforced
 * - Audit logging recommended for PHI access
 */
@RestController
@RequestMapping("/api/v1/{{DOMAIN}}-projections")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "{{DOMAIN_PASCAL}} Projections", description = "CQRS Read Model for {{DOMAIN}} domain")
public class {{DOMAIN_PASCAL}}ProjectionController {

    private final {{DOMAIN_PASCAL}}ProjectionRepository projectionRepository;

    /**
     * Get projection by domain ID with tenant isolation.
     *
     * @param id Domain entity ID
     * @param tenantId Tenant ID from gateway trust header
     * @return Projection if found, 404 if not found or unauthorized
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST', 'VIEWER')")
    @Operation(summary = "Get {{DOMAIN}} projection by ID",
               description = "Returns denormalized read model for specified {{DOMAIN}}")
    public ResponseEntity<{{DOMAIN_PASCAL}}Projection> get{{DOMAIN_PASCAL}}Projection(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.debug("Getting {{DOMAIN}} projection with id={} for tenant={}", id, tenantId);

        return projectionRepository.findByTenantIdAnd{{DOMAIN_PASCAL}}Id(tenantId, id)
                .map(projection -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())  // HIPAA: No caching of PHI
                        .header("X-Content-Type-Options", "nosniff")
                        .body(projection))
                .orElseGet(() -> {
                    log.debug("Projection not found for id={} in tenant={} (or unauthorized)", id, tenantId);
                    return ResponseEntity.notFound().build();  // 404, not 403 (prevents info disclosure)
                });
    }

    /**
     * List all projections for tenant with pagination.
     *
     * @param tenantId Tenant ID from gateway trust header
     * @param pageable Pagination parameters (page, size, sort)
     * @return Page of projections
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST', 'VIEWER')")
    @Operation(summary = "List {{DOMAIN}} projections",
               description = "Returns paginated list of projections for tenant")
    public ResponseEntity<Page<{{DOMAIN_PASCAL}}Projection>> list{{DOMAIN_PASCAL}}Projections(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {

        log.debug("Listing {{DOMAIN}} projections for tenant={}, page={}, size={}",
                  tenantId, pageable.getPageNumber(), pageable.getPageSize());

        Page<{{DOMAIN_PASCAL}}Projection> projections =
            projectionRepository.findByTenantIdOrderByLastUpdatedAtDesc(tenantId, pageable);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())  // HIPAA: No caching
                .header("X-Content-Type-Options", "nosniff")
                .body(projections);
    }

    /**
     * Get aggregate statistics for tenant.
     *
     * @param tenantId Tenant ID from gateway trust header
     * @return Statistics DTO with counts and aggregates
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST', 'VIEWER')")
    @Operation(summary = "Get {{DOMAIN}} statistics",
               description = "Returns aggregate statistics for tenant")
    public ResponseEntity<{{DOMAIN_PASCAL}}Statistics> getStatistics(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.debug("Getting {{DOMAIN}} statistics for tenant={}", tenantId);

        long totalCount = projectionRepository.countByTenantId(tenantId);

        // TODO: Add domain-specific statistics
        // Examples:
        // - Count by status
        // - Count by priority
        // - Average scores
        // - Overdue counts

        {{DOMAIN_PASCAL}}Statistics stats = {{DOMAIN_PASCAL}}Statistics.builder()
                .tenantId(tenantId)
                .totalCount(totalCount)
                // .activeCount(...)
                // .completedCount(...)
                .build();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(stats);
    }

    // ========================================
    // Domain-Specific Query Endpoints
    // ========================================

    // TODO: Add domain-specific query endpoints here
    // Examples:
    //
    // @GetMapping("/by-status/{status}")
    // @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST', 'VIEWER')")
    // public ResponseEntity<Page<{{DOMAIN_PASCAL}}Projection>> findByStatus(
    //         @PathVariable String status,
    //         @RequestHeader("X-Tenant-ID") String tenantId,
    //         Pageable pageable) {
    //     // ...
    // }
    //
    // @GetMapping("/overdue")
    // @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST')")
    // public ResponseEntity<List<{{DOMAIN_PASCAL}}Projection>> findOverdue(
    //         @RequestHeader("X-Tenant-ID") String tenantId) {
    //     // ...
    // }
    //
    // Remember: All endpoints MUST include X-Tenant-ID header and @PreAuthorize
}
