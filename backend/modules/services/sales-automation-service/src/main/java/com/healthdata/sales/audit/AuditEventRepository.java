package com.healthdata.sales.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Page<AuditEvent> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<AuditEvent> findByTenantIdAndEntityTypeOrderByCreatedAtDesc(
        UUID tenantId, String entityType, Pageable pageable);

    Page<AuditEvent> findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
        UUID tenantId, String entityType, UUID entityId, Pageable pageable);

    Page<AuditEvent> findByTenantIdAndUserIdOrderByCreatedAtDesc(
        UUID tenantId, UUID userId, Pageable pageable);

    @Query("SELECT a FROM AuditEvent a WHERE a.tenantId = :tenantId " +
           "AND a.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY a.createdAt DESC")
    Page<AuditEvent> findByTenantIdAndDateRange(
        @Param("tenantId") UUID tenantId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);

    @Query("SELECT a FROM AuditEvent a WHERE a.tenantId = :tenantId " +
           "AND a.action IN :actions ORDER BY a.createdAt DESC")
    Page<AuditEvent> findByTenantIdAndActions(
        @Param("tenantId") UUID tenantId,
        @Param("actions") List<AuditEvent.AuditAction> actions,
        Pageable pageable);

    Long countByTenantIdAndCreatedAtAfter(UUID tenantId, LocalDateTime after);
}
