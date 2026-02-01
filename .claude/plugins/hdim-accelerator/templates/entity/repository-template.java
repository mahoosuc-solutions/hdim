package com.healthdata.{{SERVICE_NAME}}.domain.repository;

import com.healthdata.{{SERVICE_NAME}}.domain.model.{{ENTITY_CLASS_NAME}};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {{ENTITY_CLASS_NAME}} entity.
 *
 * CRITICAL: All queries MUST include tenantId filter for multi-tenant isolation.
 * Return 404 (not 403) for unauthorized access to prevent information disclosure.
 */
@Repository
public interface {{ENTITY_CLASS_NAME}}Repository extends JpaRepository<{{ENTITY_CLASS_NAME}}, UUID> {

    /**
     * Find entity by ID with tenant isolation.
     * Returns empty Optional if not found or tenant mismatch (404, not 403).
     *
     * @param id Entity ID
     * @param tenantId Tenant ID for isolation
     * @return Optional containing entity if found and authorized
     */
    @Query("SELECT e FROM {{ENTITY_CLASS_NAME}} e WHERE e.id = :id AND e.tenantId = :tenantId")
    Optional<{{ENTITY_CLASS_NAME}}> findByIdAndTenant(
            @Param("id") UUID id,
            @Param("tenantId") String tenantId
    );

    /**
     * Find all entities for a specific tenant.
     *
     * @param tenantId Tenant ID for isolation
     * @return List of entities for the tenant
     */
    @Query("SELECT e FROM {{ENTITY_CLASS_NAME}} e WHERE e.tenantId = :tenantId ORDER BY e.createdAt DESC")
    List<{{ENTITY_CLASS_NAME}}> findAllByTenant(@Param("tenantId") String tenantId);

    /**
     * Count entities for a specific tenant.
     *
     * @param tenantId Tenant ID for isolation
     * @return Count of entities
     */
    @Query("SELECT COUNT(e) FROM {{ENTITY_CLASS_NAME}} e WHERE e.tenantId = :tenantId")
    long countByTenant(@Param("tenantId") String tenantId);

    /**
     * Check if entity exists with tenant isolation.
     *
     * @param id Entity ID
     * @param tenantId Tenant ID for isolation
     * @return true if entity exists and belongs to tenant
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM {{ENTITY_CLASS_NAME}} e WHERE e.id = :id AND e.tenantId = :tenantId")
    boolean existsByIdAndTenant(
            @Param("id") UUID id,
            @Param("tenantId") String tenantId
    );

    // TODO: Add custom query methods here
    // Remember: ALL queries MUST include tenantId filter
    // Example:
    // @Query("SELECT e FROM {{ENTITY_CLASS_NAME}} e WHERE e.tenantId = :tenantId AND e.fieldName = :fieldValue")
    // List<{{ENTITY_CLASS_NAME}}> findByFieldNameAndTenant(@Param("fieldName") String fieldName, @Param("tenantId") String tenantId);
}
