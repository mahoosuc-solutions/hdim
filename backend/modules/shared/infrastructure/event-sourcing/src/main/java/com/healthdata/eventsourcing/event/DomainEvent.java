package com.healthdata.eventsourcing.event;

import java.time.Instant;

/**
 * Contract for all domain events in the event sourcing system.
 *
 * Domain events represent facts about what has happened in the domain.
 * They are immutable records of state changes and serve as the single source of truth
 * for reconstructing aggregate state.
 */
public interface DomainEvent {

    /**
     * Get the unique identifier for this event
     */
    String getEventId();

    /**
     * Get the type of this event
     */
    String getEventType();

    /**
     * Get the tenant ID for multi-tenant isolation
     */
    String getTenantId();

    /**
     * Get the timestamp when the event occurred
     */
    Instant getTimestamp();

    /**
     * Get the aggregate root ID that this event applies to
     */
    String getAggregateId();

    /**
     * Get the resource type this event represents (e.g., "Patient", "Observation")
     */
    String getResourceType();
}
