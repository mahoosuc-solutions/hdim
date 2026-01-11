package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Measure Config Profile Repository
 * Manages reusable configuration templates for patient populations.
 */
@Repository
public interface MeasureConfigProfileRepository extends JpaRepository<MeasureConfigProfileEntity, UUID> {

    // Tenant-isolated queries
    Optional<MeasureConfigProfileEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<MeasureConfigProfileEntity> findByTenantIdAndMeasureId(String tenantId, UUID measureId);

    Optional<MeasureConfigProfileEntity> findByTenantIdAndMeasureIdAndProfileCode(
        String tenantId,
        UUID measureId,
        String profileCode
    );

    // Active profiles
    @Query("SELECT p FROM MeasureConfigProfileEntity p WHERE p.tenantId = :tenantId AND p.measureId = :measureId AND p.active = true")
    List<MeasureConfigProfileEntity> findActiveByMeasure(
        @Param("tenantId") String tenantId,
        @Param("measureId") UUID measureId
    );

    // Effective profiles (by date)
    @Query("SELECT p FROM MeasureConfigProfileEntity p WHERE p.tenantId = :tenantId AND p.measureId = :measureId " +
           "AND p.active = true AND p.effectiveFrom <= :date AND (p.effectiveUntil IS NULL OR p.effectiveUntil >= :date)")
    List<MeasureConfigProfileEntity> findEffectiveProfiles(
        @Param("tenantId") String tenantId,
        @Param("measureId") UUID measureId,
        @Param("date") LocalDate date
    );

    // Ordered by priority (for override resolution)
    @Query("SELECT p FROM MeasureConfigProfileEntity p WHERE p.tenantId = :tenantId AND p.measureId = :measureId " +
           "AND p.active = true ORDER BY p.priority DESC")
    List<MeasureConfigProfileEntity> findActiveByMeasureOrderedByPriority(
        @Param("tenantId") String tenantId,
        @Param("measureId") UUID measureId
    );

    // Pending approval
    @Query("SELECT p FROM MeasureConfigProfileEntity p WHERE p.tenantId = :tenantId AND p.approvedBy IS NULL AND p.active = true")
    List<MeasureConfigProfileEntity> findPendingApproval(
        @Param("tenantId") String tenantId
    );

    // Count profiles
    @Query("SELECT COUNT(p) FROM MeasureConfigProfileEntity p WHERE p.tenantId = :tenantId AND p.measureId = :measureId AND p.active = true")
    long countActiveByMeasure(
        @Param("tenantId") String tenantId,
        @Param("measureId") UUID measureId
    );
}
