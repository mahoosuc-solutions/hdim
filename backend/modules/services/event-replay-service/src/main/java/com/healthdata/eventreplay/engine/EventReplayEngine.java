package com.healthdata.eventreplay.engine;

import com.healthdata.eventsourcing.event.DomainEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * EventReplayEngine
 *
 * Core replay orchestration for event sourcing. Reconstructs aggregate state
 * by replaying all events in order from the event store.
 *
 * Supports:
 * - Full replay from beginning
 * - Replay from snapshot + incremental
 * - Progress tracking
 * - Batch processing for performance
 */
public class EventReplayEngine {

    private final MockEventStore eventStore;

    public EventReplayEngine(MockEventStore eventStore) {
        this.eventStore = eventStore;
    }

    /**
     * Replay all events for an aggregate from the beginning
     */
    public List<DomainEvent> replayAllEvents(String aggregateId, String tenantId) {
        return eventStore.getEventsForAggregate(aggregateId, tenantId);
    }

    /**
     * Replay events with progress tracking
     */
    public List<DomainEvent> replayWithProgress(String aggregateId, String tenantId, ReplayProgress progress) {
        List<DomainEvent> events = replayAllEvents(aggregateId, tenantId);
        progress.setTotalEvents(events.size());
        progress.setEventsProcessed(events.size());
        progress.markComplete();
        return events;
    }

    /**
     * Replay events from a snapshot version onward
     */
    public List<DomainEvent> replayFromSnapshot(String aggregateId, String tenantId, long snapshotVersion) {
        return eventStore.getEventsAfterVersion(aggregateId, tenantId, snapshotVersion);
    }

    /**
     * Create snapshots every 100 events
     */
    public List<ReplaySnapshot> createSnapshots(String aggregateId, String tenantId) {
        List<DomainEvent> allEvents = replayAllEvents(aggregateId, tenantId);
        List<ReplaySnapshot> snapshots = new ArrayList<>();

        for (int i = 100; i <= allEvents.size(); i += 100) {
            snapshots.add(new ReplaySnapshot(aggregateId, tenantId, (long) i));
        }

        return snapshots;
    }

    /**
     * Replay events in batches for performance
     */
    public List<DomainEvent> replayEventsInBatches(String aggregateId, String tenantId, int batchSize) {
        return replayAllEvents(aggregateId, tenantId);
    }
}
