package com.healthdata.quality.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for measure version history.
 *
 * HIPAA COMPLIANCE: This repository provides read-heavy operations for audit trails.
 * DELETE operations are intentionally not exposed to maintain immutable version history.
 *
 * Issue #152: Measure Versioning and Audit Trail
 */
public interface MeasureVersionRepository extends JpaRepository<MeasureVersionEntity, UUID> {

    /**
     * Find all versions for a measure, ordered by creation time (newest first).
     */
    List<MeasureVersionEntity> findByTenantIdAndMeasureIdOrderByCreatedAtDesc(
            String tenantId, UUID measureId);

    /**
     * Find all versions for a measure with pagination.
     */
    Page<MeasureVersionEntity> findByTenantIdAndMeasureIdOrderByCreatedAtDesc(
            String tenantId, UUID measureId, Pageable pageable);

    /**
     * Find a specific version by measure ID and version string.
     */
    Optional<MeasureVersionEntity> findByTenantIdAndMeasureIdAndVersion(
            String tenantId, UUID measureId, String version);

    /**
     * Find the current active version for a measure.
     */
    Optional<MeasureVersionEntity> findByTenantIdAndMeasureIdAndIsCurrentTrue(
            String tenantId, UUID measureId);

    /**
     * Find the latest version for a measure (regardless of current flag).
     */
    @Query("SELECT v FROM MeasureVersionEntity v " +
           "WHERE v.tenantId = :tenantId AND v.measureId = :measureId " +
           "ORDER BY v.createdAt DESC LIMIT 1")
    Optional<MeasureVersionEntity> findLatestVersion(
            @Param("tenantId") String tenantId,
            @Param("measureId") UUID measureId);

    /**
     * Find all published versions for a measure.
     */
    List<MeasureVersionEntity> findByTenantIdAndMeasureIdAndIsPublishedTrueOrderByCreatedAtDesc(
            String tenantId, UUID measureId);

    /**
     * Count versions for a measure.
     */
    long countByTenantIdAndMeasureId(String tenantId, UUID measureId);

    /**
     * Check if a version string already exists for a measure.
     */
    boolean existsByTenantIdAndMeasureIdAndVersion(String tenantId, UUID measureId, String version);

    /**
     * Clear the current flag for all versions of a measure.
     * Used before setting a new version as current.
     */
    @Modifying
    @Query("UPDATE MeasureVersionEntity v SET v.isCurrent = false " +
           "WHERE v.tenantId = :tenantId AND v.measureId = :measureId")
    int clearCurrentFlag(@Param("tenantId") String tenantId, @Param("measureId") UUID measureId);

    /**
     * Set a specific version as current.
     */
    @Modifying
    @Query("UPDATE MeasureVersionEntity v SET v.isCurrent = true " +
           "WHERE v.id = :versionId AND v.tenantId = :tenantId")
    int setCurrentVersion(@Param("tenantId") String tenantId, @Param("versionId") UUID versionId);

    /**
     * Find versions created by a specific user.
     */
    List<MeasureVersionEntity> findByTenantIdAndCreatedByOrderByCreatedAtDesc(
            String tenantId, UUID createdBy);

    /**
     * Find versions by tenant with pagination (for admin audit views).
     */
    Page<MeasureVersionEntity> findByTenantIdOrderByCreatedAtDesc(
            String tenantId, Pageable pageable);

    /**
     * Get version history summary (lightweight query for dropdowns/lists).
     */
    @Query("SELECT new map(v.id as id, v.version as version, v.versionType as versionType, " +
           "v.changeSummary as changeSummary, v.createdAt as createdAt, " +
           "v.createdByName as createdByName, v.isCurrent as isCurrent, v.isPublished as isPublished) " +
           "FROM MeasureVersionEntity v " +
           "WHERE v.tenantId = :tenantId AND v.measureId = :measureId " +
           "ORDER BY v.createdAt DESC")
    List<java.util.Map<String, Object>> findVersionSummaries(
            @Param("tenantId") String tenantId,
            @Param("measureId") UUID measureId);
}
