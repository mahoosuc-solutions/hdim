package com.healthdata.cql.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all CQL evaluation events.
 * Events are published to Kafka and consumed by visualization components.
 */
public interface EvaluationEvent {

    /**
     * Unique identifier for this event
     */
    String getEventId();

    /**
     * Type of event (EVALUATION_STARTED, EVALUATION_COMPLETED, etc.)
     */
    EventType getEventType();

    /**
     * Tenant identifier for multi-tenancy
     */
    String getTenantId();

    /**
     * Timestamp when the event occurred
     */
    Instant getTimestamp();

    /**
     * Evaluation ID if applicable (may be null for batch-level events)
     */
    UUID getEvaluationId();

    /**
     * Event type enumeration
     */
    enum EventType {
        EVALUATION_STARTED,
        EVALUATION_COMPLETED,
        EVALUATION_FAILED,
        BATCH_STARTED,
        BATCH_PROGRESS,
        BATCH_COMPLETED,
        CACHE_HIT,
        CACHE_MISS,
        TEMPLATE_LOADED
    }
}
