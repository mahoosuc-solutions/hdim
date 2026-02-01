package com.healthdata.eventstore.repository;

import com.healthdata.eventstore.domain.EventStoreEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * EventStoreRepository - JPA Repository for event_store table
 *
 * Purpose: Query interface for immutable event log
 *
 * Key Queries:
 * - Find all events for an aggregate (for event replay)
 * - Find events after a specific version (for projection updates)
 * - Find events by type (for event processing)
 * - Find events by tenant (for multi-tenant isolation)
 * - Temporal queries (time-travel debugging)
 */
@Repository
public interface EventStoreRepository extends JpaRepository<EventStoreEntry, Long> {

    /**
     * Find all events for a specific aggregate, ordered by version
     * Used for event replay and aggregate reconstruction
     *
     * @param aggregateId   The aggregate identifier
     * @param aggregateType The aggregate type (e.g., "Patient", "CareGap")
     * @param tenantId      Tenant identifier for multi-tenant isolation
     * @return List of events ordered by event version
     */
    @Query("""
            SELECT e FROM EventStoreEntry e
            WHERE e.aggregateId = :aggregateId
              AND e.aggregateType = :aggregateType
              AND e.tenantId = :tenantId
            ORDER BY e.eventVersion ASC
            """)
    List<EventStoreEntry> findByAggregateIdAndType(
            @Param("aggregateId") UUID aggregateId,
            @Param("aggregateType") String aggregateType,
            @Param("tenantId") String tenantId
    );

    /**
     * Find events after a specific version for an aggregate
     * Used for projection updates (apply only new events since last processed version)
     *
     * @param aggregateId   The aggregate identifier
     * @param aggregateType The aggregate type
     * @param afterVersion  Only return events with version > afterVersion
     * @param tenantId      Tenant identifier
     * @return List of events ordered by event version
     */
    @Query("""
            SELECT e FROM EventStoreEntry e
            WHERE e.aggregateId = :aggregateId
              AND e.aggregateType = :aggregateType
              AND e.eventVersion > :afterVersion
              AND e.tenantId = :tenantId
            ORDER BY e.eventVersion ASC
            """)
    List<EventStoreEntry> findByAggregateIdAfterVersion(
            @Param("aggregateId") UUID aggregateId,
            @Param("aggregateType") String aggregateType,
            @Param("afterVersion") Integer afterVersion,
            @Param("tenantId") String tenantId
    );

    /**
     * Find the latest event version for an aggregate
     * Used for optimistic locking and conflict detection
     *
     * @param aggregateId   The aggregate identifier
     * @param aggregateType The aggregate type
     * @param tenantId      Tenant identifier
     * @return Latest event version, or empty if no events exist
     */
    @Query("""
            SELECT MAX(e.eventVersion) FROM EventStoreEntry e
            WHERE e.aggregateId = :aggregateId
              AND e.aggregateType = :aggregateType
              AND e.tenantId = :tenantId
            """)
    Optional<Integer> findLatestVersion(
            @Param("aggregateId") UUID aggregateId,
            @Param("aggregateType") String aggregateType,
            @Param("tenantId") String tenantId
    );

    /**
     * Find all events of a specific type
     * Used for event processing and type-specific replays
     *
     * @param eventType The event type (e.g., "PatientCreatedEvent")
     * @param tenantId  Tenant identifier
     * @return List of events ordered by occurrence time
     */
    @Query("""
            SELECT e FROM EventStoreEntry e
            WHERE e.eventType = :eventType
              AND e.tenantId = :tenantId
            ORDER BY e.occurredAt ASC
            """)
    List<EventStoreEntry> findByEventType(
            @Param("eventType") String eventType,
            @Param("tenantId") String tenantId
    );

    /**
     * Find all events for a tenant
     * Used for tenant-specific event processing and backups
     *
     * @param tenantId Tenant identifier
     * @return List of events ordered by occurrence time
     */
    @Query("""
            SELECT e FROM EventStoreEntry e
            WHERE e.tenantId = :tenantId
            ORDER BY e.occurredAt ASC
            """)
    List<EventStoreEntry> findByTenantId(@Param("tenantId") String tenantId);

    /**
     * Find events within a time range
     * Used for temporal queries and time-travel debugging
     *
     * Example: "Show me all events that happened on January 15, 2026"
     *
     * @param startTime Start of time range (inclusive)
     * @param endTime   End of time range (inclusive)
     * @param tenantId  Tenant identifier
     * @return List of events ordered by occurrence time
     */
    @Query("""
            SELECT e FROM EventStoreEntry e
            WHERE e.occurredAt >= :startTime
              AND e.occurredAt <= :endTime
              AND e.tenantId = :tenantId
            ORDER BY e.occurredAt ASC
            """)
    List<EventStoreEntry> findByTimeRange(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("tenantId") String tenantId
    );

    /**
     * Find events after a specific global ID
     * Used for resuming event processing after restart
     *
     * @param afterId  Only return events with id > afterId
     * @param tenantId Tenant identifier
     * @return List of events ordered by global ID
     */
    @Query("""
            SELECT e FROM EventStoreEntry e
            WHERE e.id > :afterId
              AND e.tenantId = :tenantId
            ORDER BY e.id ASC
            """)
    List<EventStoreEntry> findByIdAfter(
            @Param("afterId") Long afterId,
            @Param("tenantId") String tenantId
    );

    /**
     * Find events by correlation ID
     * Used for distributed tracing and debugging multi-service flows
     *
     * @param correlationId Correlation ID linking related events
     * @param tenantId      Tenant identifier
     * @return List of events ordered by occurrence time
     */
    @Query("""
            SELECT e FROM EventStoreEntry e
            WHERE e.correlationId = :correlationId
              AND e.tenantId = :tenantId
            ORDER BY e.occurredAt ASC
            """)
    List<EventStoreEntry> findByCorrelationId(
            @Param("correlationId") UUID correlationId,
            @Param("tenantId") String tenantId
    );

    /**
     * Count total events for an aggregate
     * Used for monitoring and snapshot strategy decisions
     *
     * @param aggregateId   The aggregate identifier
     * @param aggregateType The aggregate type
     * @param tenantId      Tenant identifier
     * @return Total event count
     */
    @Query("""
            SELECT COUNT(e) FROM EventStoreEntry e
            WHERE e.aggregateId = :aggregateId
              AND e.aggregateType = :aggregateType
              AND e.tenantId = :tenantId
            """)
    long countByAggregate(
            @Param("aggregateId") UUID aggregateId,
            @Param("aggregateType") String aggregateType,
            @Param("tenantId") String tenantId
    );

    /**
     * Check if an event exists by event ID
     * Used for idempotency checks (prevent duplicate event processing)
     *
     * @param eventId The unique event identifier
     * @return true if event exists
     */
    boolean existsByEventId(UUID eventId);

    /**
     * Find event by event ID
     * Used for event lookup and duplicate detection
     *
     * @param eventId The unique event identifier
     * @return Event entry if found
     */
    Optional<EventStoreEntry> findByEventId(UUID eventId);
}
