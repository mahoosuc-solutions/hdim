package com.healthdata.migration.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.healthdata.migration.dto.JobStatus;
import com.healthdata.migration.persistence.MigrationJobEntity;

/**
 * Repository for migration job operations
 */
@Repository
public interface MigrationJobRepository extends JpaRepository<MigrationJobEntity, UUID> {

    // Find by tenant
    Page<MigrationJobEntity> findByTenantId(String tenantId, Pageable pageable);

    // Find by tenant and status
    Page<MigrationJobEntity> findByTenantIdAndStatus(String tenantId, JobStatus status, Pageable pageable);

    // Find by tenant and multiple statuses
    @Query("SELECT j FROM MigrationJobEntity j WHERE j.tenantId = :tenantId AND j.status IN :statuses")
    Page<MigrationJobEntity> findByTenantIdAndStatusIn(
            @Param("tenantId") String tenantId,
            @Param("statuses") List<JobStatus> statuses,
            Pageable pageable);

    // Find jobs by tenant with optional filters
    @Query("SELECT j FROM MigrationJobEntity j WHERE j.tenantId = :tenantId " +
           "AND (:status IS NULL OR j.status = :status) " +
           "AND (:jobName IS NULL OR j.jobName ILIKE CONCAT('%', :jobName, '%'))")
    Page<MigrationJobEntity> findByTenantIdWithFilters(
            @Param("tenantId") String tenantId,
            @Param("status") JobStatus status,
            @Param("jobName") String jobName,
            Pageable pageable);

    // Find running jobs
    List<MigrationJobEntity> findByStatus(JobStatus status);

    // Find jobs ready for retry
    @Query("SELECT j FROM MigrationJobEntity j WHERE j.status = 'RETRYING' " +
           "AND j.nextRetryAt <= :now AND j.retryCount < j.maxRetries")
    List<MigrationJobEntity> findRetryEligible(@Param("now") Instant now);

    // Find by tenant and ID (for security check)
    Optional<MigrationJobEntity> findByIdAndTenantId(UUID id, String tenantId);

    // Count by status
    long countByTenantIdAndStatus(String tenantId, JobStatus status);

    // Count active jobs (not terminal)
    @Query("SELECT COUNT(j) FROM MigrationJobEntity j WHERE j.tenantId = :tenantId " +
           "AND j.status NOT IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    long countActiveByTenantId(@Param("tenantId") String tenantId);

    // Find recently completed jobs
    @Query("SELECT j FROM MigrationJobEntity j WHERE j.tenantId = :tenantId " +
           "AND j.status IN ('COMPLETED', 'FAILED') AND j.completedAt >= :since " +
           "ORDER BY j.completedAt DESC")
    List<MigrationJobEntity> findRecentlyCompleted(
            @Param("tenantId") String tenantId,
            @Param("since") Instant since);

    // Update job status
    @Modifying
    @Query("UPDATE MigrationJobEntity j SET j.status = :status, j.updatedAt = :now WHERE j.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") JobStatus status, @Param("now") Instant now);

    // Update job progress
    @Modifying
    @Query("UPDATE MigrationJobEntity j SET " +
           "j.processedCount = :processed, j.successCount = :success, " +
           "j.failureCount = :failure, j.skippedCount = :skipped, j.updatedAt = :now " +
           "WHERE j.id = :id")
    int updateProgress(
            @Param("id") UUID id,
            @Param("processed") long processed,
            @Param("success") long success,
            @Param("failure") long failure,
            @Param("skipped") long skipped,
            @Param("now") Instant now);

    // Delete old completed jobs (data retention)
    @Modifying
    @Query("DELETE FROM MigrationJobEntity j WHERE j.status IN ('COMPLETED', 'CANCELLED') " +
           "AND j.completedAt < :before")
    int deleteOldCompleted(@Param("before") Instant before);
}
