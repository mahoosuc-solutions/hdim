package com.healthdata.eventsourcing.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events in the event sourcing system.
 *
 * Domain events represent immutable facts about state changes in the domain.
 * They form the event store and are the source of truth for reconstructing aggregate state.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractDomainEvent implements DomainEvent {

    /**
     * Unique identifier for this event
     */
    protected String eventId;

    /**
     * Type identifier for this event (e.g., "PatientCreated", "ObservationRecorded")
     */
    protected String eventType;

    /**
     * Tenant ID for multi-tenant isolation
     */
    protected String tenantId;

    /**
     * Timestamp when the event occurred
     */
    protected Instant timestamp;

    /**
     * Version of the aggregate after this event
     */
    protected Long aggregateVersion;

    /**
     * HIPAA sensitivity level
     */
    protected String sensitivityLevel;

    /**
     * Whether this event involves PHI (Protected Health Information)
     */
    protected Boolean hipaaCompliant;

    /**
     * Initialize timestamps and generate event ID if not provided
     */
    public AbstractDomainEvent(String eventType, String tenantId) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.tenantId = tenantId;
        this.timestamp = Instant.now();
        this.sensitivityLevel = "SENSITIVE";
        this.hipaaCompliant = true;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public abstract String getAggregateId();

    @Override
    public abstract String getResourceType();
}
