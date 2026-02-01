package com.healthdata.eventstore.repository;

import com.healthdata.eventstore.domain.EventSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * EventSnapshotRepository - JPA Repository for event_snapshots table
 *
 * Purpose: Query interface for aggregate snapshots
 *
 * Key Queries:
 * - Find latest snapshot for an aggregate (for fast event replay)
 * - Find snapshot at specific version (for temporal queries)
 */
@Repository
public interface EventSnapshotRepository extends JpaRepository<EventSnapshot, Long> {

    /**
     * Find the latest snapshot for an aggregate
     * Used for event replay optimization
     *
     * Strategy:
     * 1. Load latest snapshot (e.g., version 900)
     * 2. Replay events from version 901 onwards
     * 3. Result: Fast aggregate reconstruction
     *
     * @param aggregateId   The aggregate identifier
     * @param aggregateType The aggregate type (e.g., "Patient")
     * @param tenantId      Tenant identifier
     * @return Latest snapshot if exists
     */
    @Query("""
            SELECT s FROM EventSnapshot s
            WHERE s.aggregateId = :aggregateId
              AND s.aggregateType = :aggregateType
              AND s.tenantId = :tenantId
            ORDER BY s.snapshotVersion DESC
            LIMIT 1
            """)
    Optional<EventSnapshot> findLatestSnapshot(
            @Param("aggregateId") UUID aggregateId,
            @Param("aggregateType") String aggregateType,
            @Param("tenantId") String tenantId
    );

    /**
     * Find snapshot at or before a specific version
     * Used for temporal queries ("What was the state at version X?")
     *
     * Example: "What was the patient state after event 150?"
     * Result: Find snapshot at version 100, then replay events 101-150
     *
     * @param aggregateId      The aggregate identifier
     * @param aggregateType    The aggregate type
     * @param maxVersion       Maximum snapshot version to return
     * @param tenantId         Tenant identifier
     * @return Snapshot at or before maxVersion
     */
    @Query("""
            SELECT s FROM EventSnapshot s
            WHERE s.aggregateId = :aggregateId
              AND s.aggregateType = :aggregateType
              AND s.snapshotVersion <= :maxVersion
              AND s.tenantId = :tenantId
            ORDER BY s.snapshotVersion DESC
            LIMIT 1
            """)
    Optional<EventSnapshot> findSnapshotAtOrBeforeVersion(
            @Param("aggregateId") UUID aggregateId,
            @Param("aggregateType") String aggregateType,
            @Param("maxVersion") Integer maxVersion,
            @Param("tenantId") String tenantId
    );

    /**
     * Find snapshot at exact version
     * Used for testing and debugging
     *
     * @param aggregateId      The aggregate identifier
     * @param aggregateType    The aggregate type
     * @param snapshotVersion  Exact snapshot version
     * @param tenantId         Tenant identifier
     * @return Snapshot if exists
     */
    @Query("""
            SELECT s FROM EventSnapshot s
            WHERE s.aggregateId = :aggregateId
              AND s.aggregateType = :aggregateType
              AND s.snapshotVersion = :snapshotVersion
              AND s.tenantId = :tenantId
            """)
    Optional<EventSnapshot> findByAggregateAndVersion(
            @Param("aggregateId") UUID aggregateId,
            @Param("aggregateType") String aggregateType,
            @Param("snapshotVersion") Integer snapshotVersion,
            @Param("tenantId") String tenantId
    );

    /**
     * Check if snapshot exists for aggregate
     * Used for snapshot strategy decisions
     *
     * @param aggregateId   The aggregate identifier
     * @param aggregateType The aggregate type
     * @param tenantId      Tenant identifier
     * @return true if any snapshot exists
     */
    boolean existsByAggregateIdAndAggregateTypeAndTenantId(
            UUID aggregateId,
            String aggregateType,
            String tenantId
    );

    /**
     * Count snapshots for an aggregate
     * Used for monitoring and cleanup
     *
     * @param aggregateId   The aggregate identifier
     * @param aggregateType The aggregate type
     * @param tenantId      Tenant identifier
     * @return Total snapshot count
     */
    long countByAggregateIdAndAggregateTypeAndTenantId(
            UUID aggregateId,
            String aggregateType,
            String tenantId
    );
}
