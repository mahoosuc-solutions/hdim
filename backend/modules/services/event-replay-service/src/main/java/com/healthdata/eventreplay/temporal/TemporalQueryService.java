package com.healthdata.eventreplay.temporal;

import com.healthdata.eventreplay.engine.EventStore;
import com.healthdata.eventreplay.engine.ProjectionStore;
import com.healthdata.eventsourcing.event.DomainEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * TemporalQueryService - Query state at specific points in time
 *
 * Enables "state as of [date]" queries critical for healthcare:
 * - HIPAA audit trails
 * - Care gap analysis at historical dates
 * - Quality measure evaluation at period end
 * - Compliance verification at specific times
 */
public class TemporalQueryService {
    private final EventStore eventStore;
    private final ProjectionStore projectionStore;

    public TemporalQueryService(EventStore eventStore, ProjectionStore projectionStore) {
        this.eventStore = eventStore;
        this.projectionStore = projectionStore;
    }

    /**
     * Get state as of a specific point in time
     */
    public TemporalSnapshot getStateAsOf(String aggregateId, String tenantId, Instant pointInTime) {
        List<DomainEvent> events = eventStore.getEventsForAggregate(aggregateId, tenantId);
        List<DomainEvent> beforeTime = events.stream()
            .filter(e -> !e.getTimestamp().isAfter(pointInTime))
            .toList();

        if (beforeTime.isEmpty()) {
            return null;
        }

        return new TemporalSnapshot(aggregateId, tenantId, beforeTime.size(), pointInTime);
    }

    /**
     * Get snapshots for date range
     */
    public List<TemporalSnapshot> getStateRange(String aggregateId, String tenantId, Instant startTime, Instant endTime) {
        if (startTime.isAfter(endTime)) {
            throw new InvalidTemporalQueryException("Start time cannot be after end time");
        }

        List<TemporalSnapshot> snapshots = new ArrayList<>();
        List<DomainEvent> allEvents = eventStore.getEventsForAggregate(aggregateId, tenantId);

        for (int i = 0; i < allEvents.size(); i++) {
            DomainEvent event = allEvents.get(i);
            if (!event.getTimestamp().isBefore(startTime) && !event.getTimestamp().isAfter(endTime)) {
                snapshots.add(new TemporalSnapshot(aggregateId, tenantId, (long) (i + 1), event.getTimestamp()));
            }
        }

        return snapshots;
    }

    /**
     * Get daily snapshots for a date range
     */
    public List<TemporalSnapshot> getDailySnapshots(String aggregateId, String tenantId, Instant startTime, Instant endTime) {
        return getStateRange(aggregateId, tenantId, startTime, endTime);
    }

    /**
     * Get audit trail for time range
     */
    public AuditTrail getAuditTrail(String aggregateId, String tenantId, Instant startTime, Instant endTime) {
        List<DomainEvent> events = eventStore.getEventsForAggregate(aggregateId, tenantId);
        List<DomainEvent> inRange = events.stream()
            .filter(e -> !e.getTimestamp().isBefore(startTime) && !e.getTimestamp().isAfter(endTime))
            .toList();

        return new AuditTrail(aggregateId, tenantId, inRange);
    }
}

class InvalidTemporalQueryException extends RuntimeException {
    public InvalidTemporalQueryException(String message) {
        super(message);
    }
}
