package com.healthdata.eventreplay.engine;

import com.healthdata.eventsourcing.event.DomainEvent;
import java.util.List;

/**
 * EventStore - Repository interface for event sourced aggregates
 *
 * Abstraction for accessing immutable event log.
 * Implementations may use database, cache, or other storage.
 */
public interface EventStore {

    /**
     * Get all events for an aggregate
     */
    List<DomainEvent> getEventsForAggregate(String aggregateId, String tenantId);

    /**
     * Get snapshot for an aggregate
     */
    Object getSnapshot(String aggregateId);

    /**
     * Store events for an aggregate
     */
    void storeEvents(String aggregateId, List<DomainEvent> events);

    /**
     * Store a snapshot for an aggregate
     */
    void storeSnapshot(String aggregateId, Object snapshot);
}
