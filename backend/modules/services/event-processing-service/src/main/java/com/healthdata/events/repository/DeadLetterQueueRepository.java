package com.healthdata.events.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.healthdata.events.entity.DeadLetterQueueEntity;
import com.healthdata.events.entity.DeadLetterQueueEntity.DLQStatus;

/**
 * Repository for Dead Letter Queue operations
 */
@Repository
public interface DeadLetterQueueRepository extends JpaRepository<DeadLetterQueueEntity, UUID> {

    /**
     * Find all failed events for a tenant
     */
    Page<DeadLetterQueueEntity> findByTenantIdAndStatus(
        String tenantId,
        DLQStatus status,
        Pageable pageable
    );

    /**
     * Find all failed events for a patient
     */
    List<DeadLetterQueueEntity> findByPatientIdAndStatus(
        String patientId,
        DLQStatus status
    );

    /**
     * Find all failed events for a topic
     */
    List<DeadLetterQueueEntity> findByTopicAndStatus(
        String topic,
        DLQStatus status
    );

    /**
     * Find events eligible for retry
     */
    @Query("SELECT d FROM DeadLetterQueueEntity d WHERE d.status = 'FAILED' " +
           "AND d.retryCount < d.maxRetryCount " +
           "AND (d.nextRetryAt IS NULL OR d.nextRetryAt <= :now)")
    List<DeadLetterQueueEntity> findRetryEligible(@Param("now") Instant now);

    /**
     * Find events that have exhausted retries
     */
    @Query("SELECT d FROM DeadLetterQueueEntity d WHERE d.status = 'FAILED' " +
           "AND d.retryCount >= d.maxRetryCount")
    List<DeadLetterQueueEntity> findExhausted();

    /**
     * Count events by status (for metrics and health checks)
     */
    long countByStatus(DLQStatus status);

    /**
     * Count failed events by tenant
     */
    long countByTenantIdAndStatus(String tenantId, DLQStatus status);

    /**
     * Count failed events by topic
     */
    long countByTopicAndStatus(String topic, DLQStatus status);

    /**
     * Find recent failures (last N hours)
     */
    @Query("SELECT d FROM DeadLetterQueueEntity d WHERE d.firstFailureAt >= :since " +
           "ORDER BY d.firstFailureAt DESC")
    List<DeadLetterQueueEntity> findRecentFailures(@Param("since") Instant since);

    /**
     * Delete old resolved/discarded events (data retention)
     */
    @Query("DELETE FROM DeadLetterQueueEntity d WHERE d.status IN ('RESOLVED', 'DISCARDED') " +
           "AND d.resolvedAt < :before")
    void deleteOldResolved(@Param("before") Instant before);
}
