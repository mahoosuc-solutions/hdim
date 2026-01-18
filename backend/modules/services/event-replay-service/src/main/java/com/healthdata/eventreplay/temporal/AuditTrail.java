package com.healthdata.eventreplay.temporal;

import com.healthdata.eventsourcing.event.DomainEvent;
import java.util.List;

/**
 * AuditTrail - Immutable sequence of events for audit purposes
 */
public class AuditTrail {
    private final String aggregateId;
    private final String tenantId;
    private final List<DomainEvent> events;

    public AuditTrail(String aggregateId, String tenantId, List<DomainEvent> events) {
        this.aggregateId = aggregateId;
        this.tenantId = tenantId;
        this.events = List.copyOf(events); // Immutable copy
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public List<DomainEvent> getEvents() {
        return events;
    }
}
