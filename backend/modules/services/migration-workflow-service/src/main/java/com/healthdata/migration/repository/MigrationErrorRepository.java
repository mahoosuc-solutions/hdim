package com.healthdata.migration.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.healthdata.migration.dto.MigrationErrorCategory;
import com.healthdata.migration.persistence.MigrationErrorEntity;

/**
 * Repository for migration error operations
 */
@Repository
public interface MigrationErrorRepository extends JpaRepository<MigrationErrorEntity, UUID> {

    // Find errors by job
    Page<MigrationErrorEntity> findByJobId(UUID jobId, Pageable pageable);

    // Find errors by job and category
    Page<MigrationErrorEntity> findByJobIdAndErrorCategory(
            UUID jobId, MigrationErrorCategory category, Pageable pageable);

    // Find errors by job with search
    @Query("SELECT e FROM MigrationErrorEntity e WHERE e.job.id = :jobId " +
           "AND (:category IS NULL OR e.errorCategory = :category) " +
           "AND (:search IS NULL OR LOWER(e.errorMessage) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<MigrationErrorEntity> findByJobIdWithFilters(
            @Param("jobId") UUID jobId,
            @Param("category") MigrationErrorCategory category,
            @Param("search") String search,
            Pageable pageable);

    // Count by job
    long countByJobId(UUID jobId);

    // Count by job and category
    long countByJobIdAndErrorCategory(UUID jobId, MigrationErrorCategory category);

    // Get error counts by category for a job
    @Query("SELECT e.errorCategory, COUNT(e) FROM MigrationErrorEntity e " +
           "WHERE e.job.id = :jobId GROUP BY e.errorCategory")
    List<Object[]> countByJobIdGroupByCategory(@Param("jobId") UUID jobId);

    // Find recent errors for a job (for live display)
    @Query("SELECT e FROM MigrationErrorEntity e WHERE e.job.id = :jobId " +
           "ORDER BY e.createdAt DESC")
    List<MigrationErrorEntity> findRecentByJobId(
            @Param("jobId") UUID jobId, Pageable pageable);

    // Find top errors by frequency
    @Query("SELECT e.errorCategory, e.errorMessage, COUNT(e) as cnt FROM MigrationErrorEntity e " +
           "WHERE e.job.id = :jobId GROUP BY e.errorCategory, e.errorMessage " +
           "ORDER BY cnt DESC")
    List<Object[]> findTopErrorsByJobId(@Param("jobId") UUID jobId, Pageable pageable);

    // Find errors by patient ID
    List<MigrationErrorEntity> findByJobIdAndPatientId(UUID jobId, String patientId);

    // Find errors by source file
    Page<MigrationErrorEntity> findByJobIdAndSourceFile(UUID jobId, String sourceFile, Pageable pageable);

    // Delete errors for a job (cascade delete should handle this, but explicit option)
    @Modifying
    @Query("DELETE FROM MigrationErrorEntity e WHERE e.job.id = :jobId")
    int deleteByJobId(@Param("jobId") UUID jobId);

    // Delete old errors (data retention)
    @Modifying
    @Query("DELETE FROM MigrationErrorEntity e WHERE e.createdAt < :before")
    int deleteOldErrors(@Param("before") Instant before);
}
