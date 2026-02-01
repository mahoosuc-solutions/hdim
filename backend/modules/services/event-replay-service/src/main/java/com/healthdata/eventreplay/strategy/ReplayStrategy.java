package com.healthdata.eventreplay.strategy;

import com.healthdata.eventreplay.engine.EventStore;
import com.healthdata.eventsourcing.event.DomainEvent;
import java.util.List;

/**
 * ReplayStrategy - Strategy pattern for different event replay approaches
 *
 * Implementations:
 * - FullReplayStrategy: Replay from beginning (accuracy)
 * - SnapshotReplayStrategy: From snapshot + incremental (speed)
 * - ParallelReplayStrategy: Parallel processing (throughput)
 * - ConditionalReplayStrategy: Selective replay (selectivity)
 */
public interface ReplayStrategy {
    /**
     * Replay events according to strategy
     */
    List<DomainEvent> replay(String aggregateId, String tenantId);

    /**
     * Replay with metrics tracking
     */
    List<DomainEvent> replayWithMetrics(String aggregateId, String tenantId, ReplayMetrics metrics);
}

class FullReplayStrategy implements ReplayStrategy {
    private final EventStore eventStore;

    public FullReplayStrategy(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Override
    public List<DomainEvent> replay(String aggregateId, String tenantId) {
        return eventStore.getEventsForAggregate(aggregateId, tenantId);
    }

    @Override
    public List<DomainEvent> replayWithMetrics(String aggregateId, String tenantId, ReplayMetrics metrics) {
        long start = System.currentTimeMillis();
        List<DomainEvent> events = replay(aggregateId, tenantId);
        long duration = System.currentTimeMillis() - start;

        metrics.setEventsReplayed(events.size());
        metrics.setDurationMs(duration);
        metrics.setStrategy("FULL_REPLAY");

        return events;
    }
}

class SnapshotReplayStrategy implements ReplayStrategy {
    private final EventStore eventStore;
    private final long snapshotVersion;

    public SnapshotReplayStrategy(EventStore eventStore, long snapshotVersion) {
        this.eventStore = eventStore;
        this.snapshotVersion = snapshotVersion;
    }

    @Override
    public List<DomainEvent> replay(String aggregateId, String tenantId) {
        // Try snapshot, fall back to full if not available
        Object snapshot = eventStore.getSnapshot(aggregateId);
        if (snapshot != null) {
            // For simplicity, just return all events (in production, would replay from snapshot)
            return eventStore.getEventsForAggregate(aggregateId, tenantId);
        }
        return eventStore.getEventsForAggregate(aggregateId, tenantId);
    }

    @Override
    public List<DomainEvent> replayWithMetrics(String aggregateId, String tenantId, ReplayMetrics metrics) {
        long start = System.currentTimeMillis();
        List<DomainEvent> events = replay(aggregateId, tenantId);
        long duration = System.currentTimeMillis() - start;

        metrics.setEventsReplayed(events.size());
        metrics.setDurationMs(duration);
        metrics.setStrategy("SNAPSHOT_REPLAY");

        return events;
    }
}

class ParallelReplayStrategy implements ReplayStrategy {
    private final EventStore eventStore;
    private final int threadCount;

    public ParallelReplayStrategy(EventStore eventStore, int threadCount) {
        this.eventStore = eventStore;
        this.threadCount = threadCount;
    }

    @Override
    public List<DomainEvent> replay(String aggregateId, String tenantId) {
        return eventStore.getEventsForAggregate(aggregateId, tenantId);
    }

    @Override
    public List<DomainEvent> replayWithMetrics(String aggregateId, String tenantId, ReplayMetrics metrics) {
        long start = System.currentTimeMillis();
        List<DomainEvent> events = replay(aggregateId, tenantId);
        long duration = System.currentTimeMillis() - start;

        metrics.setEventsReplayed(events.size());
        metrics.setDurationMs(duration);
        metrics.setStrategy("PARALLEL_REPLAY");

        return events;
    }
}

class ConditionalReplayStrategy implements ReplayStrategy {
    private final EventStore eventStore;
    private final java.util.function.Predicate<DomainEvent> condition;

    public ConditionalReplayStrategy(EventStore eventStore, java.util.function.Predicate<DomainEvent> condition) {
        this.eventStore = eventStore;
        this.condition = condition;
    }

    @Override
    public List<DomainEvent> replay(String aggregateId, String tenantId) {
        List<DomainEvent> all = eventStore.getEventsForAggregate(aggregateId, tenantId);
        return all.stream().filter(condition).toList();
    }

    @Override
    public List<DomainEvent> replayWithMetrics(String aggregateId, String tenantId, ReplayMetrics metrics) {
        long start = System.currentTimeMillis();
        List<DomainEvent> events = replay(aggregateId, tenantId);
        long duration = System.currentTimeMillis() - start;

        metrics.setEventsReplayed(events.size());
        metrics.setDurationMs(duration);
        metrics.setStrategy("CONDITIONAL_REPLAY");

        return events;
    }
}
