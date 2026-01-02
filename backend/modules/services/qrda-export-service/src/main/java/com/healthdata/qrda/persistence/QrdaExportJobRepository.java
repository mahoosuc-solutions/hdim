package com.healthdata.qrda.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for QRDA export job persistence.
 */
@Repository
public interface QrdaExportJobRepository extends JpaRepository<QrdaExportJobEntity, UUID> {

    Optional<QrdaExportJobEntity> findByIdAndTenantId(UUID id, String tenantId);

    Page<QrdaExportJobEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    List<QrdaExportJobEntity> findByTenantIdAndStatus(
        String tenantId,
        QrdaExportJobEntity.QrdaJobStatus status
    );

    List<QrdaExportJobEntity> findByTenantIdAndJobType(
        String tenantId,
        QrdaExportJobEntity.QrdaJobType jobType
    );

    @Query("SELECT j FROM QrdaExportJobEntity j WHERE j.status = :status AND j.createdAt < :cutoff")
    List<QrdaExportJobEntity> findStaleJobs(
        @Param("status") QrdaExportJobEntity.QrdaJobStatus status,
        @Param("cutoff") LocalDateTime cutoff
    );

    @Query("SELECT j FROM QrdaExportJobEntity j WHERE j.tenantId = :tenantId " +
           "AND j.periodStart = :periodStart AND j.periodEnd = :periodEnd " +
           "AND j.jobType = :jobType AND j.status = 'COMPLETED' " +
           "ORDER BY j.completedAt DESC")
    List<QrdaExportJobEntity> findRecentCompletedJobs(
        @Param("tenantId") String tenantId,
        @Param("periodStart") java.time.LocalDate periodStart,
        @Param("periodEnd") java.time.LocalDate periodEnd,
        @Param("jobType") QrdaExportJobEntity.QrdaJobType jobType
    );

    @Query("SELECT COUNT(j) FROM QrdaExportJobEntity j WHERE j.tenantId = :tenantId " +
           "AND j.status = 'RUNNING'")
    long countRunningJobs(@Param("tenantId") String tenantId);
}
