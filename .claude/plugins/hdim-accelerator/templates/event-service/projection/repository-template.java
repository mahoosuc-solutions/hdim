package com.healthdata.{{DOMAIN}}event.repository;

import com.healthdata.{{DOMAIN}}event.projection.{{DOMAIN_PASCAL}}Projection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {{DOMAIN_PASCAL}}Projection (CQRS Read Model).
 *
 * CRITICAL: All queries MUST include tenantId filter for multi-tenant isolation.
 * Return empty Optional / empty list for unauthorized access (404, not 403).
 *
 * Performance:
 * - Queries use indexes on tenant_id, updated_at
 * - Denormalized data enables single-table queries (no joins)
 * - Target: < 100ms query response time (99th percentile)
 */
@Repository
public interface {{DOMAIN_PASCAL}}ProjectionRepository extends JpaRepository<{{DOMAIN_PASCAL}}Projection, Long> {

    // ========================================
    // Core Lookup Methods (Required)
    // ========================================

    /**
     * Find projection by domain ID with tenant isolation.
     * Returns empty Optional if not found or tenant mismatch (404, not 403).
     *
     * @param tenantId Tenant ID for isolation
     * @param {{DOMAIN}}Id Domain entity ID
     * @return Optional containing projection if found and authorized
     */
    @Query("SELECT p FROM {{DOMAIN_PASCAL}}Projection p WHERE p.tenantId = :tenantId AND p.{{DOMAIN}}Id = :{{DOMAIN}}Id")
    Optional<{{DOMAIN_PASCAL}}Projection> findByTenantIdAnd{{DOMAIN_PASCAL}}Id(
            @Param("tenantId") String tenantId,
            @Param("{{DOMAIN}}Id") UUID {{DOMAIN}}Id
    );

    /**
     * Check if projection exists with tenant isolation.
     *
     * @param tenantId Tenant ID for isolation
     * @param {{DOMAIN}}Id Domain entity ID
     * @return true if projection exists and belongs to tenant
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM {{DOMAIN_PASCAL}}Projection p WHERE p.tenantId = :tenantId AND p.{{DOMAIN}}Id = :{{DOMAIN}}Id")
    boolean existsByTenantIdAnd{{DOMAIN_PASCAL}}Id(
            @Param("tenantId") String tenantId,
            @Param("{{DOMAIN}}Id") UUID {{DOMAIN}}Id
    );

    // ========================================
    // List & Pagination (Required)
    // ========================================

    /**
     * Find all projections for tenant with pagination.
     * Sorted by last updated (most recent first).
     *
     * @param tenantId Tenant ID for isolation
     * @param pageable Pagination parameters
     * @return Page of projections
     */
    @Query("SELECT p FROM {{DOMAIN_PASCAL}}Projection p WHERE p.tenantId = :tenantId ORDER BY p.lastUpdatedAt DESC")
    Page<{{DOMAIN_PASCAL}}Projection> findByTenantIdOrderByLastUpdatedAtDesc(
            @Param("tenantId") String tenantId,
            Pageable pageable
    );

    /**
     * Find all projections for tenant (non-paginated).
     * Use with caution - prefer paginated version for large datasets.
     *
     * @param tenantId Tenant ID for isolation
     * @return List of projections
     */
    @Query("SELECT p FROM {{DOMAIN_PASCAL}}Projection p WHERE p.tenantId = :tenantId ORDER BY p.lastUpdatedAt DESC")
    List<{{DOMAIN_PASCAL}}Projection> findAllByTenantId(@Param("tenantId") String tenantId);

    // ========================================
    // Statistics & Aggregates (Required)
    // ========================================

    /**
     * Count total projections for tenant.
     *
     * @param tenantId Tenant ID for isolation
     * @return Count of projections
     */
    @Query("SELECT COUNT(p) FROM {{DOMAIN_PASCAL}}Projection p WHERE p.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") String tenantId);

    // ========================================
    // Domain-Specific Queries (Add as needed)
    // ========================================

    // TODO: Add custom query methods here
    // Examples:
    //
    // Find by status:
    // @Query("SELECT p FROM {{DOMAIN_PASCAL}}Projection p WHERE p.tenantId = :tenantId AND p.status = :status")
    // Page<{{DOMAIN_PASCAL}}Projection> findByTenantIdAndStatus(@Param("tenantId") String tenantId, @Param("status") String status, Pageable pageable);
    //
    // Find overdue items:
    // @Query("SELECT p FROM {{DOMAIN_PASCAL}}Projection p WHERE p.tenantId = :tenantId AND p.dueDate < CURRENT_DATE AND p.status = 'OPEN'")
    // List<{{DOMAIN_PASCAL}}Projection> findOverdueByTenantId(@Param("tenantId") String tenantId);
    //
    // Count by status:
    // @Query("SELECT COUNT(p) FROM {{DOMAIN_PASCAL}}Projection p WHERE p.tenantId = :tenantId AND p.status = :status")
    // long countByTenantIdAndStatus(@Param("tenantId") String tenantId, @Param("status") String status);
    //
    // Find by priority:
    // @Query("SELECT p FROM {{DOMAIN_PASCAL}}Projection p WHERE p.tenantId = :tenantId AND p.priority = :priority ORDER BY p.createdAt ASC")
    // Page<{{DOMAIN_PASCAL}}Projection> findByTenantIdAndPriority(@Param("tenantId") String tenantId, @Param("priority") String priority, Pageable pageable);
    //
    // Remember: ALL queries MUST include tenantId filter
}
