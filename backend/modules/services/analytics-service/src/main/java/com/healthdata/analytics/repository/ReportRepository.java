package com.healthdata.analytics.repository;

import com.healthdata.analytics.persistence.ReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, UUID> {

    List<ReportEntity> findByTenantId(String tenantId);

    Page<ReportEntity> findByTenantId(String tenantId, Pageable pageable);

    Optional<ReportEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<ReportEntity> findByTenantIdAndReportType(String tenantId, String reportType);

    List<ReportEntity> findByTenantIdAndCreatedBy(String tenantId, String createdBy);

    @Query("SELECT r FROM ReportEntity r WHERE r.scheduleEnabled = true AND r.scheduleCron IS NOT NULL")
    List<ReportEntity> findScheduledReports();

    @Query("SELECT r FROM ReportEntity r WHERE r.tenantId = :tenantId AND r.scheduleEnabled = true")
    List<ReportEntity> findScheduledReportsByTenant(@Param("tenantId") String tenantId);

    @Query("SELECT COUNT(r) FROM ReportEntity r WHERE r.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") String tenantId);

    boolean existsByIdAndTenantId(UUID id, String tenantId);
}
