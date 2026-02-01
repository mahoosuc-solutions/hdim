package com.healthdata.gateway.clinical.compliance.repository;

import com.healthdata.gateway.clinical.compliance.entity.ComplianceErrorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ComplianceErrorRepository extends JpaRepository<ComplianceErrorEntity, UUID> {
    Page<ComplianceErrorEntity> findByTenantIdOrderByTimestampDesc(String tenantId, Pageable pageable);

    @Query("SELECT e FROM ComplianceErrorEntity e WHERE e.tenantId = :tenantId " +
           "AND e.timestamp >= :startDate AND e.timestamp <= :endDate " +
           "ORDER BY e.timestamp DESC")
    Page<ComplianceErrorEntity> findByTenantIdAndDateRange(
        @Param("tenantId") String tenantId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );

    Page<ComplianceErrorEntity> findByTenantIdAndSeverityOrderByTimestampDesc(
        String tenantId, String severity, Pageable pageable);

    Page<ComplianceErrorEntity> findByTenantIdAndServiceOrderByTimestampDesc(
        String tenantId, String service, Pageable pageable);

    long countByTenantIdAndSeverity(String tenantId, String severity);

    @Query("SELECT COUNT(e) FROM ComplianceErrorEntity e WHERE e.tenantId = :tenantId " +
           "AND e.timestamp >= :startDate AND e.timestamp <= :endDate")
    long countByTenantIdAndDateRange(
        @Param("tenantId") String tenantId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    @Query("SELECT COUNT(e) FROM ComplianceErrorEntity e WHERE e.tenantId = :tenantId " +
           "AND e.severity = :severity AND e.timestamp >= :startDate AND e.timestamp <= :endDate")
    long countByTenantIdAndSeverityAndDateRange(
        @Param("tenantId") String tenantId,
        @Param("severity") String severity,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    long deleteByTenantIdAndTimestampBefore(String tenantId, Instant cutoffDate);
}
