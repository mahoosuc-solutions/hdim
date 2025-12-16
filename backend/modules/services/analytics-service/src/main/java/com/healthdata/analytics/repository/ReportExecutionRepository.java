package com.healthdata.analytics.repository;

import com.healthdata.analytics.persistence.ReportExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportExecutionRepository extends JpaRepository<ReportExecutionEntity, UUID> {

    List<ReportExecutionEntity> findByReportIdAndTenantId(UUID reportId, String tenantId);

    Page<ReportExecutionEntity> findByReportIdAndTenantId(UUID reportId, String tenantId, Pageable pageable);

    Optional<ReportExecutionEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<ReportExecutionEntity> findByTenantIdAndStatus(String tenantId, String status);

    @Query("SELECT e FROM ReportExecutionEntity e WHERE e.reportId = :reportId ORDER BY e.startedAt DESC")
    List<ReportExecutionEntity> findLatestExecutions(@Param("reportId") UUID reportId, Pageable pageable);

    @Query("SELECT e FROM ReportExecutionEntity e WHERE e.reportId = :reportId AND e.status = 'COMPLETED' ORDER BY e.completedAt DESC")
    Optional<ReportExecutionEntity> findLatestSuccessfulExecution(@Param("reportId") UUID reportId);

    @Query("SELECT e FROM ReportExecutionEntity e WHERE e.status = 'RUNNING' AND e.startedAt < :threshold")
    List<ReportExecutionEntity> findStaleRunningExecutions(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT COUNT(e) FROM ReportExecutionEntity e WHERE e.reportId = :reportId AND e.status = :status")
    long countByReportIdAndStatus(@Param("reportId") UUID reportId, @Param("status") String status);

    @Modifying
    @Query("DELETE FROM ReportExecutionEntity e WHERE e.completedAt < :threshold")
    int deleteOldExecutions(@Param("threshold") LocalDateTime threshold);

    boolean existsByIdAndTenantId(UUID id, String tenantId);
}
