package com.healthdata.audit.repository.shared;

import com.healthdata.audit.entity.shared.AuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JPA repository for AuditEventEntity.
 * Provides query methods for HIPAA-compliant audit log retrieval.
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

    /**
     * Find audit events by user ID with optional date range filtering.
     * Results are ordered by timestamp descending (most recent first).
     *
     * @param userId the user ID to search for
     * @param from start of date range (inclusive)
     * @param to end of date range (inclusive)
     * @param pageable pagination parameters
     * @return page of audit events
     */
    @Query("SELECT e FROM AuditEventEntity e WHERE e.userId = :userId " +
           "AND e.timestamp >= :from AND e.timestamp <= :to " +
           "ORDER BY e.timestamp DESC")
    Page<AuditEventEntity> findByUserIdAndTimestampBetween(
            @Param("userId") String userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    /**
     * Find audit events by resource type and ID with optional date range filtering.
     * Results are ordered by timestamp descending (most recent first).
     *
     * @param resourceType the type of resource (e.g., "Patient", "Observation")
     * @param resourceId the resource ID
     * @param from start of date range (inclusive)
     * @param to end of date range (inclusive)
     * @param pageable pagination parameters
     * @return page of audit events
     */
    @Query("SELECT e FROM AuditEventEntity e WHERE e.resourceType = :resourceType " +
           "AND e.resourceId = :resourceId " +
           "AND e.timestamp >= :from AND e.timestamp <= :to " +
           "ORDER BY e.timestamp DESC")
    Page<AuditEventEntity> findByResourceTypeAndResourceIdAndTimestampBetween(
            @Param("resourceType") String resourceType,
            @Param("resourceId") String resourceId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    /**
     * Find audit events by tenant ID with optional date range filtering.
     * Results are ordered by timestamp descending (most recent first).
     * This ensures multi-tenant isolation.
     *
     * @param tenantId the tenant ID
     * @param from start of date range (inclusive)
     * @param to end of date range (inclusive)
     * @param pageable pagination parameters
     * @return page of audit events
     */
    @Query("SELECT e FROM AuditEventEntity e WHERE e.tenantId = :tenantId " +
           "AND e.timestamp >= :from AND e.timestamp <= :to " +
           "ORDER BY e.timestamp DESC")
    Page<AuditEventEntity> findByTenantIdAndTimestampBetween(
            @Param("tenantId") String tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    /**
     * Delete audit events older than the specified cutoff date.
     * This is used to enforce the HIPAA 7-year retention policy.
     *
     * @param cutoffDate events before this date will be deleted
     * @return number of records deleted
     */
    @Modifying
    @Query("DELETE FROM AuditEventEntity e WHERE e.timestamp < :cutoffDate")
    int deleteByTimestampBefore(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Count audit events older than the specified cutoff date.
     * Useful for reporting before purging.
     *
     * @param cutoffDate the cutoff date
     * @return count of events that would be deleted
     */
    @Query("SELECT COUNT(e) FROM AuditEventEntity e WHERE e.timestamp < :cutoffDate")
    long countByTimestampBefore(@Param("cutoffDate") Instant cutoffDate);
}
