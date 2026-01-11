package com.healthdata.quality.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Measure Modification Audit Repository
 * Tracks all changes to measure definitions for compliance and rollback.
 */
@Repository
public interface MeasureModificationAuditRepository extends JpaRepository<MeasureModificationAuditEntity, UUID> {

    // Tenant-isolated queries
    Optional<MeasureModificationAuditEntity> findByIdAndTenantId(UUID id, String tenantId);

    // Entity audit trail
    @Query("SELECT a FROM MeasureModificationAuditEntity a WHERE a.tenantId = :tenantId AND a.entityType = :entityType " +
           "AND a.entityId = :entityId ORDER BY a.modifiedAt DESC")
    List<MeasureModificationAuditEntity> findByEntity(
        @Param("tenantId") String tenantId,
        @Param("entityType") String entityType,
        @Param("entityId") UUID entityId,
        Pageable pageable
    );

    @Query("SELECT a FROM MeasureModificationAuditEntity a WHERE a.tenantId = :tenantId AND a.entityType = :entityType " +
           "AND a.entityId = :entityId AND a.fieldName = :fieldName ORDER BY a.modifiedAt DESC")
    List<MeasureModificationAuditEntity> findByEntityAndField(
        @Param("tenantId") String tenantId,
        @Param("entityType") String entityType,
        @Param("entityId") UUID entityId,
        @Param("fieldName") String fieldName,
        Pageable pageable
    );

    // By entity type
    @Query("SELECT a FROM MeasureModificationAuditEntity a WHERE a.tenantId = :tenantId AND a.entityType = :entityType " +
           "ORDER BY a.modifiedAt DESC")
    List<MeasureModificationAuditEntity> findByEntityType(
        @Param("tenantId") String tenantId,
        @Param("entityType") String entityType,
        Pageable pageable
    );

    // By operation
    @Query("SELECT a FROM MeasureModificationAuditEntity a WHERE a.tenantId = :tenantId AND a.operation = :operation " +
           "ORDER BY a.modifiedAt DESC")
    List<MeasureModificationAuditEntity> findByOperation(
        @Param("tenantId") String tenantId,
        @Param("operation") String operation,
        Pageable pageable
    );

    // By modifier
    @Query("SELECT a FROM MeasureModificationAuditEntity a WHERE a.tenantId = :tenantId AND a.modifiedBy = :userId " +
           "ORDER BY a.modifiedAt DESC")
    List<MeasureModificationAuditEntity> findByModifiedBy(
        @Param("tenantId") String tenantId,
        @Param("userId") UUID userId,
        Pageable pageable
    );

    // Pending approval
    @Query("SELECT a FROM MeasureModificationAuditEntity a WHERE a.tenantId = :tenantId AND a.requiresApproval = true " +
           "AND a.approvalStatus = 'PENDING' ORDER BY a.modifiedAt DESC")
    List<MeasureModificationAuditEntity> findPendingApproval(
        @Param("tenantId") String tenantId
    );

    // Rollback-capable changes
    @Query("SELECT a FROM MeasureModificationAuditEntity a WHERE a.tenantId = :tenantId AND a.entityType = :entityType " +
           "AND a.entityId = :entityId AND a.rollbackAvailable = true ORDER BY a.modifiedAt DESC")
    List<MeasureModificationAuditEntity> findRollbackCandidates(
        @Param("tenantId") String tenantId,
        @Param("entityType") String entityType,
        @Param("entityId") UUID entityId
    );

    // Time range queries
    @Query("SELECT a FROM MeasureModificationAuditEntity a WHERE a.tenantId = :tenantId " +
           "AND a.modifiedAt BETWEEN :startTime AND :endTime ORDER BY a.modifiedAt DESC")
    List<MeasureModificationAuditEntity> findByTimeRange(
        @Param("tenantId") String tenantId,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    // Count modifications
    @Query("SELECT COUNT(a) FROM MeasureModificationAuditEntity a WHERE a.tenantId = :tenantId " +
           "AND a.entityType = :entityType AND a.entityId = :entityId")
    long countByEntity(
        @Param("tenantId") String tenantId,
        @Param("entityType") String entityType,
        @Param("entityId") UUID entityId
    );
}
