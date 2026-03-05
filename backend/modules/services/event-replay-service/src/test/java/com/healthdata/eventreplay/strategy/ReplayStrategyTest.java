package com.healthdata.eventreplay.strategy;

import com.healthdata.eventreplay.engine.EventStore;
import com.healthdata.eventsourcing.event.DomainEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;
import java.util.stream.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the event-replay-service STRATEGY layer:
 * {@link ReplayMetrics}, {@link ReplayStrategyFactory}, and all
 * strategy implementations (Full, Snapshot, Parallel, Conditional).
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ReplayStrategyTest {

    private static final String AGGREGATE_ID = "agg-strategy-001";
    private static final String TENANT_ID = "tenant-strategy";

    @Mock
    private EventStore eventStore;

    // ---------------------------------------------------------------------------
    // Helper: create N mock DomainEvents with timestamps
    // ---------------------------------------------------------------------------
    private List<DomainEvent> mockEvents(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> {
                    DomainEvent event = mock(DomainEvent.class);
                    lenient().when(event.getEventId()).thenReturn("evt-" + i);
                    lenient().when(event.getAggregateId()).thenReturn(AGGREGATE_ID);
                    lenient().when(event.getTenantId()).thenReturn(TENANT_ID);
                    lenient().when(event.getEventType()).thenReturn("TestEvent");
                    lenient().when(event.getTimestamp()).thenReturn(Instant.now());
                    return event;
                })
                .collect(Collectors.toList());
    }

    // =========================================================================
    // ReplayMetrics tests
    // =========================================================================

    @Test
    @DisplayName("ReplayMetrics: defaults are zero/null before any setter is called")
    void replayMetrics_defaults() {
        ReplayMetrics metrics = new ReplayMetrics();

        assertThat(metrics.getEventsReplayed()).isZero();
        assertThat(metrics.getDurationMs()).isZero();
        assertThat(metrics.getStrategy()).isNull();
    }

    @Test
    @DisplayName("ReplayMetrics: setters and getters round-trip correctly")
    void replayMetrics_settersAndGetters() {
        ReplayMetrics metrics = new ReplayMetrics();

        metrics.setEventsReplayed(42);
        metrics.setDurationMs(1500L);
        metrics.setStrategy("FULL_REPLAY");

        assertThat(metrics.getEventsReplayed()).isEqualTo(42);
        assertThat(metrics.getDurationMs()).isEqualTo(1500L);
        assertThat(metrics.getStrategy()).isEqualTo("FULL_REPLAY");
    }

    // =========================================================================
    // ReplayStrategyFactory: creation methods return non-null strategies
    // =========================================================================

    @Test
    @DisplayName("Factory: createFullReplayStrategy returns non-null ReplayStrategy")
    void factory_createFullReplayStrategy() {
        ReplayStrategyFactory factory = new ReplayStrategyFactory(eventStore);

        ReplayStrategy strategy = factory.createFullReplayStrategy();

        assertThat(strategy).isNotNull();
    }

    @Test
    @DisplayName("Factory: createSnapshotReplayStrategy returns non-null ReplayStrategy")
    void factory_createSnapshotReplayStrategy() {
        ReplayStrategyFactory factory = new ReplayStrategyFactory(eventStore);

        ReplayStrategy strategy = factory.createSnapshotReplayStrategy(50L);

        assertThat(strategy).isNotNull();
    }

    @Test
    @DisplayName("Factory: createParallelReplayStrategy returns non-null ReplayStrategy")
    void factory_createParallelReplayStrategy() {
        ReplayStrategyFactory factory = new ReplayStrategyFactory(eventStore);

        ReplayStrategy strategy = factory.createParallelReplayStrategy(4);

        assertThat(strategy).isNotNull();
    }

    @Test
    @DisplayName("Factory: createConditionalReplayStrategy returns non-null ReplayStrategy")
    void factory_createConditionalReplayStrategy() {
        ReplayStrategyFactory factory = new ReplayStrategyFactory(eventStore);

        ReplayStrategy strategy = factory.createConditionalReplayStrategy(e -> true);

        assertThat(strategy).isNotNull();
    }

    // =========================================================================
    // ReplayStrategyFactory: selectBestStrategy logic
    // =========================================================================

    @Test
    @DisplayName("selectBestStrategy: small event count without snapshot returns FullReplayStrategy")
    void selectBestStrategy_smallCount_returnsFullStrategy() {
        ReplayStrategyFactory factory = new ReplayStrategyFactory(eventStore);

        // totalEvents=50, no snapshot → FullReplayStrategy
        ReplayStrategy strategy = factory.selectBestStrategy(AGGREGATE_ID, TENANT_ID, 50, false);

        assertThat(strategy).isNotNull();

        // Verify it behaves as FullReplayStrategy by exercising replayWithMetrics
        List<DomainEvent> events = mockEvents(3);
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID)).thenReturn(events);
        ReplayMetrics metrics = new ReplayMetrics();
        strategy.replayWithMetrics(AGGREGATE_ID, TENANT_ID, metrics);

        assertThat(metrics.getStrategy()).isEqualTo("FULL_REPLAY");
    }

    @Test
    @DisplayName("selectBestStrategy: snapshot available with >100 events returns SnapshotReplayStrategy")
    void selectBestStrategy_snapshotAvailableLargeCount_returnsSnapshotStrategy() {
        ReplayStrategyFactory factory = new ReplayStrategyFactory(eventStore);

        // totalEvents=200, snapshot available → SnapshotReplayStrategy(200/2 = 100)
        ReplayStrategy strategy = factory.selectBestStrategy(AGGREGATE_ID, TENANT_ID, 200, true);

        assertThat(strategy).isNotNull();

        List<DomainEvent> events = mockEvents(5);
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID)).thenReturn(events);
        lenient().when(eventStore.getSnapshot(AGGREGATE_ID)).thenReturn(new Object());
        ReplayMetrics metrics = new ReplayMetrics();
        strategy.replayWithMetrics(AGGREGATE_ID, TENANT_ID, metrics);

        assertThat(metrics.getStrategy()).isEqualTo("SNAPSHOT_REPLAY");
    }

    @Test
    @DisplayName("selectBestStrategy: >1000 events without snapshot returns ParallelReplayStrategy")
    void selectBestStrategy_veryLargeCount_returnsParallelStrategy() {
        ReplayStrategyFactory factory = new ReplayStrategyFactory(eventStore);

        // totalEvents=5000, no snapshot → ParallelReplayStrategy(4)
        ReplayStrategy strategy = factory.selectBestStrategy(AGGREGATE_ID, TENANT_ID, 5000, false);

        assertThat(strategy).isNotNull();

        List<DomainEvent> events = mockEvents(2);
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID)).thenReturn(events);
        ReplayMetrics metrics = new ReplayMetrics();
        strategy.replayWithMetrics(AGGREGATE_ID, TENANT_ID, metrics);

        assertThat(metrics.getStrategy()).isEqualTo("PARALLEL_REPLAY");
    }

    // =========================================================================
    // FullReplayStrategy: replay and metrics
    // =========================================================================

    @Test
    @DisplayName("FullReplayStrategy: replay returns all events from EventStore")
    void fullReplayStrategy_replay_returnsAllEvents() {
        ReplayStrategyFactory factory = new ReplayStrategyFactory(eventStore);
        ReplayStrategy strategy = factory.createFullReplayStrategy();

        List<DomainEvent> events = mockEvents(10);
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID)).thenReturn(events);

        List<DomainEvent> result = strategy.replay(AGGREGATE_ID, TENANT_ID);

        assertThat(result).hasSize(10).containsExactlyElementsOf(events);
        verify(eventStore).getEventsForAggregate(AGGREGATE_ID, TENANT_ID);
    }

    @Test
    @DisplayName("FullReplayStrategy: replayWithMetrics populates metrics correctly")
    void fullReplayStrategy_replayWithMetrics_populatesMetrics() {
        ReplayStrategyFactory factory = new ReplayStrategyFactory(eventStore);
        ReplayStrategy strategy = factory.createFullReplayStrategy();

        List<DomainEvent> events = mockEvents(7);
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID)).thenReturn(events);
        ReplayMetrics metrics = new ReplayMetrics();

        List<DomainEvent> result = strategy.replayWithMetrics(AGGREGATE_ID, TENANT_ID, metrics);

        assertThat(result).hasSize(7);
        assertThat(metrics.getEventsReplayed()).isEqualTo(7);
        assertThat(metrics.getDurationMs()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.getStrategy()).isEqualTo("FULL_REPLAY");
    }

    // =========================================================================
    // ConditionalReplayStrategy: filtering
    // =========================================================================

    @Test
    @DisplayName("ConditionalReplayStrategy: filters events by predicate")
    void conditionalReplayStrategy_filtersEvents() {
        // Create events where only even-numbered ones have eventType "KeepMe"
        List<DomainEvent> events = IntStream.rangeClosed(1, 6)
                .mapToObj(i -> {
                    DomainEvent event = mock(DomainEvent.class);
                    String type = (i % 2 == 0) ? "KeepMe" : "DiscardMe";
                    when(event.getEventType()).thenReturn(type);
                    lenient().when(event.getEventId()).thenReturn("evt-" + i);
                    return event;
                })
                .collect(Collectors.toList());

        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID)).thenReturn(events);

        ReplayStrategyFactory factory = new ReplayStrategyFactory(eventStore);
        ReplayStrategy strategy = factory.createConditionalReplayStrategy(
                e -> "KeepMe".equals(e.getEventType())
        );

        List<DomainEvent> result = strategy.replay(AGGREGATE_ID, TENANT_ID);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(e -> "KeepMe".equals(e.getEventType()));
    }

    @Test
    @DisplayName("ConditionalReplayStrategy: replayWithMetrics sets CONDITIONAL_REPLAY strategy")
    void conditionalReplayStrategy_metricsSetCorrectStrategy() {
        List<DomainEvent> events = mockEvents(4);
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID)).thenReturn(events);

        ReplayStrategyFactory factory = new ReplayStrategyFactory(eventStore);
        ReplayStrategy strategy = factory.createConditionalReplayStrategy(e -> true);
        ReplayMetrics metrics = new ReplayMetrics();

        strategy.replayWithMetrics(AGGREGATE_ID, TENANT_ID, metrics);

        assertThat(metrics.getStrategy()).isEqualTo("CONDITIONAL_REPLAY");
        assertThat(metrics.getEventsReplayed()).isEqualTo(4);
    }

    // =========================================================================
    // SnapshotReplayStrategy: replay and metrics
    // =========================================================================

    @Test
    @DisplayName("SnapshotReplayStrategy: replay returns events and populates SNAPSHOT_REPLAY metrics")
    void snapshotReplayStrategy_replayWithMetrics() {
        List<DomainEvent> events = mockEvents(8);
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID)).thenReturn(events);
        lenient().when(eventStore.getSnapshot(AGGREGATE_ID)).thenReturn(new Object());

        ReplayStrategyFactory factory = new ReplayStrategyFactory(eventStore);
        ReplayStrategy strategy = factory.createSnapshotReplayStrategy(50L);
        ReplayMetrics metrics = new ReplayMetrics();

        List<DomainEvent> result = strategy.replayWithMetrics(AGGREGATE_ID, TENANT_ID, metrics);

        assertThat(result).hasSize(8);
        assertThat(metrics.getEventsReplayed()).isEqualTo(8);
        assertThat(metrics.getDurationMs()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.getStrategy()).isEqualTo("SNAPSHOT_REPLAY");
    }
}
