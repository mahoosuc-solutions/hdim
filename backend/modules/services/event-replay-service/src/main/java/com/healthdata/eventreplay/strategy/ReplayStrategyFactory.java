package com.healthdata.eventreplay.strategy;

import com.healthdata.eventsourcing.event.DomainEvent;
import java.util.function.Predicate;

/**
 * ReplayStrategyFactory - Creates appropriate replay strategies
 */
public class ReplayStrategyFactory {
    private final MockEventStore eventStore;

    public ReplayStrategyFactory(MockEventStore eventStore) {
        this.eventStore = eventStore;
    }

    public ReplayStrategy createFullReplayStrategy() {
        return new FullReplayStrategy(eventStore);
    }

    public ReplayStrategy createSnapshotReplayStrategy(long snapshotVersion) {
        return new SnapshotReplayStrategy(eventStore, snapshotVersion);
    }

    public ReplayStrategy createParallelReplayStrategy(int threadCount) {
        return new ParallelReplayStrategy(eventStore, threadCount);
    }

    public ReplayStrategy createConditionalReplayStrategy(Predicate<DomainEvent> condition) {
        return new ConditionalReplayStrategy(eventStore, condition);
    }

    public ReplayStrategy selectBestStrategy(String aggregateId, String tenantId, long totalEvents, boolean snapshotAvailable) {
        if (snapshotAvailable && totalEvents > 100) {
            return createSnapshotReplayStrategy(totalEvents / 2);
        }

        if (totalEvents > 1000) {
            return createParallelReplayStrategy(4);
        }

        return createFullReplayStrategy();
    }
}
