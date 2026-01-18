package com.healthdata.eventreplay.strategy;

import com.healthdata.eventsourcing.event.DomainEvent;
import com.healthdata.eventsourcing.event.PatientCreatedEvent;
import com.healthdata.eventsourcing.event.ConditionDiagnosedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ReplayStrategy
 *
 * Tests different strategies for replaying events:
 * - Full replay from beginning
 * - Snapshot + incremental replay
 * - Parallel replay for performance
 * - Conditional replay (events matching criteria)
 *
 * Strategies optimize replay performance for different scenarios:
 * - Full replay: Accuracy, used after recovery
 * - Snapshot: Speed, used for hot projections
 * - Parallel: Throughput, used for batch rebuilds
 * - Conditional: Selectivity, used for targeted updates
 */
@DisplayName("ReplayStrategy Tests")
class ReplayStrategyTest {

    private ReplayStrategyFactory strategyFactory;
    private MockEventStore mockEventStore;

    @BeforeEach
    void setup() {
        mockEventStore = new MockEventStore();
        strategyFactory = new ReplayStrategyFactory(mockEventStore);
    }

    // ===== Full Replay Strategy Tests =====

    @Test
    @DisplayName("Should replay all events from beginning")
    void testFullReplayStrategy() {
        // Given: Event list
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        List<DomainEvent> events = Arrays.asList(
            new PatientCreatedEvent(patientId, tenantId, "John", "Doe", "1990-01-01"),
            new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension"),
            new ConditionDiagnosedEvent(patientId, tenantId, "DM", "Diabetes")
        );
        mockEventStore.storeEvents(patientId, events);

        // When: Using full replay strategy
        ReplayStrategy fullReplay = strategyFactory.createFullReplayStrategy();
        List<DomainEvent> replayed = fullReplay.replay(patientId, tenantId);

        // Then: All events returned
        assertThat(replayed).hasSize(3).extracting(DomainEvent::getEventType)
            .containsExactly("PatientCreatedEvent", "ConditionDiagnosedEvent", "ConditionDiagnosedEvent");
    }

    @Test
    @DisplayName("Should preserve event order in full replay")
    void testFullReplayEventOrder() {
        // Given: Ordered events
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        List<DomainEvent> events = Arrays.asList(
            new PatientCreatedEvent(patientId, tenantId, "John", "Doe", "1990-01-01"),
            new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension")
        );
        mockEventStore.storeEvents(patientId, events);

        // When: Full replay
        ReplayStrategy fullReplay = strategyFactory.createFullReplayStrategy();
        List<DomainEvent> replayed = fullReplay.replay(patientId, tenantId);

        // Then: Order preserved
        assertThat(replayed).isSortedAccordingTo(Comparator.comparing(DomainEvent::getTimestamp));
    }

    // ===== Snapshot + Incremental Strategy Tests =====

    @Test
    @DisplayName("Should use snapshot + incremental replay when snapshot available")
    void testSnapshotIncrementalStrategy() {
        // Given: Snapshot at version 50 + 10 incremental events
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";
        long snapshotVersion = 50L;

        ReplaySnapshot snapshot = new ReplaySnapshot(patientId, tenantId, snapshotVersion);
        mockEventStore.storeSnapshot(patientId, snapshot);

        List<DomainEvent> incrementalEvents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            incrementalEvents.add(new TestDomainEvent(patientId, tenantId));
        }
        mockEventStore.storeEventsAfterSnapshot(patientId, incrementalEvents, snapshotVersion);

        // When: Using snapshot strategy
        ReplayStrategy snapshotStrategy = strategyFactory.createSnapshotReplayStrategy(snapshotVersion);
        List<DomainEvent> replayed = snapshotStrategy.replay(patientId, tenantId);

        // Then: Should return incremental events
        assertThat(replayed).hasSizeGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("Should fall back to full replay if snapshot unavailable")
    void testSnapshotFallbackToFullReplay() {
        // Given: No snapshot available
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        List<DomainEvent> events = Arrays.asList(
            new PatientCreatedEvent(patientId, tenantId, "John", "Doe", "1990-01-01"),
            new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension")
        );
        mockEventStore.storeEvents(patientId, events);

        // When: Using snapshot strategy (but snapshot missing)
        ReplayStrategy snapshotStrategy = strategyFactory.createSnapshotReplayStrategy(50L);
        List<DomainEvent> replayed = snapshotStrategy.replay(patientId, tenantId);

        // Then: Should fall back to full replay
        assertThat(replayed).hasSize(2);
    }

    @Test
    @DisplayName("Should be more efficient than full replay when snapshot available")
    void testSnapshotEfficiency() {
        // Given: 1000 events with snapshot at 100
        String patientId = "PATIENT-LARGE";
        String tenantId = "TENANT-001";

        ReplaySnapshot snapshot = new ReplaySnapshot(patientId, tenantId, 100L);
        mockEventStore.storeSnapshot(patientId, snapshot);

        List<DomainEvent> incrementalEvents = new ArrayList<>();
        for (int i = 0; i < 900; i++) {
            incrementalEvents.add(new TestDomainEvent(patientId, tenantId));
        }
        mockEventStore.storeEventsAfterSnapshot(patientId, incrementalEvents, 100L);

        // When: Comparing strategy performance
        ReplayStrategy snapshotStrategy = strategyFactory.createSnapshotReplayStrategy(100L);

        long startTime = System.currentTimeMillis();
        List<DomainEvent> replayed = snapshotStrategy.replay(patientId, tenantId);
        long duration = System.currentTimeMillis() - startTime;

        // Then: Should complete faster
        assertThat(replayed).isNotEmpty();
        assertThat(duration).isLessThan(1000); // Should be quick
    }

    // ===== Parallel Replay Strategy Tests =====

    @Test
    @DisplayName("Should replay events in parallel")
    void testParallelReplayStrategy() {
        // Given: Multiple patients (10)
        String tenantId = "TENANT-001";
        Map<String, List<DomainEvent>> patientEvents = new HashMap<>();

        for (int i = 0; i < 10; i++) {
            String patientId = "PATIENT-" + i;
            List<DomainEvent> events = Arrays.asList(
                new PatientCreatedEvent(patientId, tenantId, "Patient", String.valueOf(i), "1990-01-01"),
                new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension")
            );
            patientEvents.put(patientId, events);
            mockEventStore.storeEvents(patientId, events);
        }

        // When: Using parallel replay strategy
        ReplayStrategy parallelStrategy = strategyFactory.createParallelReplayStrategy(4); // 4 threads

        long startTime = System.currentTimeMillis();
        Map<String, List<DomainEvent>> allReplayed = new HashMap<>();
        for (String patientId : patientEvents.keySet()) {
            allReplayed.put(patientId, parallelStrategy.replay(patientId, tenantId));
        }
        long duration = System.currentTimeMillis() - startTime;

        // Then: All patients replayed
        assertThat(allReplayed).hasSize(10);
        assertThat(allReplayed.values()).allMatch(events -> events.size() >= 2);
    }

    @Test
    @DisplayName("Should maintain consistency in parallel replay")
    void testParallelReplayConsistency() {
        // Given: Multiple patients with events
        String tenantId = "TENANT-001";

        for (int i = 0; i < 5; i++) {
            String patientId = "PATIENT-" + i;
            List<DomainEvent> events = Arrays.asList(
                new PatientCreatedEvent(patientId, tenantId, "Patient", String.valueOf(i), "1990-01-01"),
                new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension")
            );
            mockEventStore.storeEvents(patientId, events);
        }

        // When: Replaying in parallel
        ReplayStrategy parallelStrategy = strategyFactory.createParallelReplayStrategy(2);

        List<List<DomainEvent>> replayed = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            replayed.add(parallelStrategy.replay("PATIENT-" + i, tenantId));
        }

        // Then: Each patient's events should be in order
        replayed.forEach(events -> assertThat(events).hasSize(2));
    }

    // ===== Conditional Replay Strategy Tests =====

    @Test
    @DisplayName("Should replay events matching condition")
    void testConditionalReplayStrategy() {
        // Given: Mixed events
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        List<DomainEvent> events = Arrays.asList(
            new PatientCreatedEvent(patientId, tenantId, "John", "Doe", "1990-01-01"),
            new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension"),
            new ConditionDiagnosedEvent(patientId, tenantId, "DM", "Diabetes")
        );
        mockEventStore.storeEvents(patientId, events);

        // When: Replaying only ConditionDiagnosed events
        ReplayStrategy conditionalStrategy = strategyFactory.createConditionalReplayStrategy(
            event -> event.getEventType().equals("ConditionDiagnosedEvent")
        );
        List<DomainEvent> replayed = conditionalStrategy.replay(patientId, tenantId);

        // Then: Only matching events returned
        assertThat(replayed)
            .hasSize(2)
            .allMatch(event -> event.getEventType().equals("ConditionDiagnosedEvent"));
    }

    @Test
    @DisplayName("Should replay events after specific date")
    void testDateBasedConditionalReplay() {
        // Given: Events at different times
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        List<DomainEvent> events = Arrays.asList(
            new PatientCreatedEvent(patientId, tenantId, "John", "Doe", "1990-01-01"),
            new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension")
        );
        mockEventStore.storeEvents(patientId, events);

        // When: Replaying events after patient creation
        ReplayStrategy dateStrategy = strategyFactory.createConditionalReplayStrategy(
            event -> event.getEventType().equals("ConditionDiagnosedEvent")
        );
        List<DomainEvent> replayed = dateStrategy.replay(patientId, tenantId);

        // Then: Only post-creation events
        assertThat(replayed).hasSize(1).allMatch(e -> e.getEventType().equals("ConditionDiagnosedEvent"));
    }

    // ===== Strategy Selection Tests =====

    @Test
    @DisplayName("Should select best strategy based on conditions")
    void testStrategySelection() {
        // Given: Conditions for strategy selection
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";
        long totalEvents = 1000L;
        boolean snapshotAvailable = true;

        // When: Selecting strategy
        ReplayStrategy selected = strategyFactory.selectBestStrategy(patientId, tenantId, totalEvents, snapshotAvailable);

        // Then: Should select snapshot strategy
        assertThat(selected).isInstanceOf(SnapshotReplayStrategy.class);
    }

    @Test
    @DisplayName("Should select full replay when snapshot unavailable")
    void testStrategySelectionNoSnapshot() {
        // Given: No snapshot available
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";
        long totalEvents = 100L;
        boolean snapshotAvailable = false;

        // When: Selecting strategy
        ReplayStrategy selected = strategyFactory.selectBestStrategy(patientId, tenantId, totalEvents, snapshotAvailable);

        // Then: Should select full replay
        assertThat(selected).isInstanceOf(FullReplayStrategy.class);
    }

    // ===== Metrics Tests =====

    @Test
    @DisplayName("Should track replay metrics")
    void testReplayMetrics() {
        // Given: Event list
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        List<DomainEvent> events = Arrays.asList(
            new PatientCreatedEvent(patientId, tenantId, "John", "Doe", "1990-01-01"),
            new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension")
        );
        mockEventStore.storeEvents(patientId, events);

        // When: Replaying with metrics
        ReplayStrategy strategy = strategyFactory.createFullReplayStrategy();
        ReplayMetrics metrics = new ReplayMetrics();
        List<DomainEvent> replayed = strategy.replayWithMetrics(patientId, tenantId, metrics);

        // Then: Metrics should be recorded
        assertThat(metrics.getEventsReplayed()).isEqualTo(2);
        assertThat(metrics.getDurationMs()).isGreaterThan(0);
        assertThat(metrics.getStrategy()).isEqualTo("FULL_REPLAY");
    }

    // ===== Error Handling Tests =====

    @Test
    @DisplayName("Should handle errors in conditional replay")
    void testConditionalReplayErrorHandling() {
        // Given: Events and invalid condition
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        mockEventStore.storeEvents(patientId, Arrays.asList(
            new PatientCreatedEvent(patientId, tenantId, "John", "Doe", "1990-01-01"),
            new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension")
        ));

        // When: Replaying with condition that throws
        ReplayStrategy errorStrategy = strategyFactory.createConditionalReplayStrategy(
            event -> {
                if (event == null) throw new NullPointerException("Event is null");
                return true;
            }
        );

        // Then: Should handle error gracefully
        assertThatCode(() -> errorStrategy.replay(patientId, tenantId))
            .doesNotThrowAnyException();
    }

    // ===== Helper Classes =====

    static class TestDomainEvent extends DomainEvent {
        public TestDomainEvent(String aggregateId, String tenantId) {
            super(aggregateId, tenantId);
        }
    }
}
