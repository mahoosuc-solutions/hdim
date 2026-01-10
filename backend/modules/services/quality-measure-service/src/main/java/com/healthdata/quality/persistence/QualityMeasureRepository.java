package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for QualityMeasure entities.
 * Provides tenant-isolated access to quality measure definitions.
 */
@Repository
public interface QualityMeasureRepository extends JpaRepository<QualityMeasureEntity, UUID> {

    /**
     * Find measure by ID and tenant for isolation.
     */
    @Query("SELECT m FROM QualityMeasureEntity m WHERE m.id = :id AND m.tenantId = :tenantId")
    Optional<QualityMeasureEntity> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") String tenantId);

    /**
     * Find measure by measure code (e.g., BCS, COL).
     */
    @Query("SELECT m FROM QualityMeasureEntity m WHERE m.measureId = :measureId AND m.tenantId = :tenantId")
    Optional<QualityMeasureEntity> findByMeasureIdAndTenantId(
        @Param("measureId") String measureId,
        @Param("tenantId") String tenantId);

    /**
     * Find all active measures for a tenant.
     */
    @Query("SELECT m FROM QualityMeasureEntity m WHERE m.tenantId = :tenantId AND m.active = true ORDER BY m.measureId")
    List<QualityMeasureEntity> findActiveByTenantId(@Param("tenantId") String tenantId);

    /**
     * Find measures by set (HEDIS, CMS, custom) for a tenant.
     */
    @Query("SELECT m FROM QualityMeasureEntity m WHERE m.tenantId = :tenantId AND m.measureSet = :measureSet AND m.active = true ORDER BY m.measureId")
    List<QualityMeasureEntity> findByTenantIdAndMeasureSet(
        @Param("tenantId") String tenantId,
        @Param("measureSet") String measureSet);

    /**
     * Find measures by domain for a tenant.
     */
    @Query("SELECT m FROM QualityMeasureEntity m WHERE m.tenantId = :tenantId AND m.domain = :domain AND m.active = true ORDER BY m.measureId")
    List<QualityMeasureEntity> findByTenantIdAndDomain(
        @Param("tenantId") String tenantId,
        @Param("domain") String domain);

    /**
     * Check if measure exists by measure code and tenant.
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM QualityMeasureEntity m WHERE m.measureId = :measureId AND m.tenantId = :tenantId")
    boolean existsByMeasureIdAndTenantId(
        @Param("measureId") String measureId,
        @Param("tenantId") String tenantId);

    /**
     * Count measures by set for a tenant.
     */
    @Query("SELECT COUNT(m) FROM QualityMeasureEntity m WHERE m.tenantId = :tenantId AND m.measureSet = :measureSet AND m.active = true")
    long countByTenantIdAndMeasureSet(
        @Param("tenantId") String tenantId,
        @Param("measureSet") String measureSet);

    /**
     * Find all measures regardless of tenant (for admin use only).
     */
    @Query("SELECT m FROM QualityMeasureEntity m WHERE m.active = true ORDER BY m.tenantId, m.measureId")
    List<QualityMeasureEntity> findAllActive();
}
