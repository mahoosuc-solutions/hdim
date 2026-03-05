package com.healthdata.eventreplay.engine;

import com.healthdata.eventsourcing.event.DomainEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;
import java.util.stream.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the event-replay-service ENGINE layer:
 * {@link EventReplayEngine}, {@link ReplayProgress}, and {@link ReplaySnapshot}.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EventReplayEngineTest {

    private static final String AGGREGATE_ID = "agg-001";
    private static final String TENANT_ID = "tenant-alpha";

    @Mock
    private EventStore eventStore;

    @InjectMocks
    private EventReplayEngine engine;

    // ---------------------------------------------------------------------------
    // Helper: create N mock DomainEvents
    // ---------------------------------------------------------------------------
    private List<DomainEvent> mockEvents(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> {
                    DomainEvent event = mock(DomainEvent.class);
                    lenient().when(event.getEventId()).thenReturn("evt-" + i);
                    lenient().when(event.getAggregateId()).thenReturn(AGGREGATE_ID);
                    lenient().when(event.getTenantId()).thenReturn(TENANT_ID);
                    return event;
                })
                .collect(Collectors.toList());
    }

    // =========================================================================
    // ReplayProgress tests
    // =========================================================================

    @Test
    @DisplayName("ReplayProgress: creation sets aggregateId, tenantId, startTime, and not complete")
    void replayProgress_creation() {
        Instant before = Instant.now();
        ReplayProgress progress = new ReplayProgress(AGGREGATE_ID, TENANT_ID);
        Instant after = Instant.now();

        assertThat(progress.getAggregateId()).isEqualTo(AGGREGATE_ID);
        assertThat(progress.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(progress.isComplete()).isFalse();
        assertThat(progress.getStartTime()).isBetween(before, after);
        assertThat(progress.getEndTime()).isNull();
        assertThat(progress.getTotalEvents()).isZero();
        assertThat(progress.getEventsProcessed()).isZero();
    }

    @Test
    @DisplayName("ReplayProgress: setTotalEvents and setEventsProcessed update fields")
    void replayProgress_setters() {
        ReplayProgress progress = new ReplayProgress(AGGREGATE_ID, TENANT_ID);

        progress.setTotalEvents(500);
        progress.setEventsProcessed(250);

        assertThat(progress.getTotalEvents()).isEqualTo(500);
        assertThat(progress.getEventsProcessed()).isEqualTo(250);
    }

    @Test
    @DisplayName("ReplayProgress: markComplete sets complete flag and endTime")
    void replayProgress_markComplete() {
        ReplayProgress progress = new ReplayProgress(AGGREGATE_ID, TENANT_ID);

        progress.markComplete();

        assertThat(progress.isComplete()).isTrue();
        assertThat(progress.getEndTime()).isNotNull();
        assertThat(progress.getEndTime()).isAfterOrEqualTo(progress.getStartTime());
    }

    @Test
    @DisplayName("ReplayProgress: getDurationMs returns non-negative value after markComplete")
    void replayProgress_durationMs() {
        ReplayProgress progress = new ReplayProgress(AGGREGATE_ID, TENANT_ID);
        progress.markComplete();

        assertThat(progress.getDurationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("ReplayProgress: getDurationMs returns 0 when endTime is null")
    void replayProgress_durationMs_beforeComplete() {
        ReplayProgress progress = new ReplayProgress(AGGREGATE_ID, TENANT_ID);

        assertThat(progress.getDurationMs()).isZero();
    }

    // =========================================================================
    // ReplaySnapshot tests
    // =========================================================================

    @Test
    @DisplayName("ReplaySnapshot: creation sets all fields correctly")
    void replaySnapshot_creation() {
        Instant before = Instant.now();
        ReplaySnapshot snapshot = new ReplaySnapshot(AGGREGATE_ID, TENANT_ID, 100L);
        Instant after = Instant.now();

        assertThat(snapshot.getAggregateId()).isEqualTo(AGGREGATE_ID);
        assertThat(snapshot.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(snapshot.getVersion()).isEqualTo(100L);
        assertThat(snapshot.getSnapshotId()).isNotNull().isNotBlank();
        assertThat(snapshot.getCreatedAt()).isBetween(before, after);
        assertThat(snapshot.getSnapshotData()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("ReplaySnapshot: each instance has a unique snapshotId")
    void replaySnapshot_uniqueIds() {
        ReplaySnapshot s1 = new ReplaySnapshot(AGGREGATE_ID, TENANT_ID, 100L);
        ReplaySnapshot s2 = new ReplaySnapshot(AGGREGATE_ID, TENANT_ID, 100L);

        assertThat(s1.getSnapshotId()).isNotEqualTo(s2.getSnapshotId());
    }

    @Test
    @DisplayName("ReplaySnapshot: snapshotId is a valid UUID string")
    void replaySnapshot_validUuid() {
        ReplaySnapshot snapshot = new ReplaySnapshot(AGGREGATE_ID, TENANT_ID, 200L);

        assertThatCode(() -> UUID.fromString(snapshot.getSnapshotId()))
                .doesNotThrowAnyException();
    }

    // =========================================================================
    // EventReplayEngine tests
    // =========================================================================

    @Test
    @DisplayName("replayAllEvents: returns empty list when no events exist")
    void replayAllEvents_empty() {
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID))
                .thenReturn(Collections.emptyList());

        List<DomainEvent> result = engine.replayAllEvents(AGGREGATE_ID, TENANT_ID);

        assertThat(result).isEmpty();
        verify(eventStore).getEventsForAggregate(AGGREGATE_ID, TENANT_ID);
    }

    @Test
    @DisplayName("replayAllEvents: returns single event")
    void replayAllEvents_singleEvent() {
        List<DomainEvent> events = mockEvents(1);
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID)).thenReturn(events);

        List<DomainEvent> result = engine.replayAllEvents(AGGREGATE_ID, TENANT_ID);

        assertThat(result).hasSize(1).isEqualTo(events);
    }

    @Test
    @DisplayName("replayAllEvents: returns many events preserving order")
    void replayAllEvents_manyEvents() {
        List<DomainEvent> events = mockEvents(50);
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID)).thenReturn(events);

        List<DomainEvent> result = engine.replayAllEvents(AGGREGATE_ID, TENANT_ID);

        assertThat(result).hasSize(50).containsExactlyElementsOf(events);
    }

    @Test
    @DisplayName("replayWithProgress: updates progress tracking fields and marks complete")
    void replayWithProgress_updatesProgress() {
        List<DomainEvent> events = mockEvents(10);
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID)).thenReturn(events);
        ReplayProgress progress = new ReplayProgress(AGGREGATE_ID, TENANT_ID);

        List<DomainEvent> result = engine.replayWithProgress(AGGREGATE_ID, TENANT_ID, progress);

        assertThat(result).hasSize(10);
        assertThat(progress.getTotalEvents()).isEqualTo(10);
        assertThat(progress.getEventsProcessed()).isEqualTo(10);
        assertThat(progress.isComplete()).isTrue();
        assertThat(progress.getEndTime()).isNotNull();
        assertThat(progress.getDurationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("replayFromSnapshot: returns all events (simplified implementation)")
    void replayFromSnapshot_returnsAllEvents() {
        List<DomainEvent> events = mockEvents(25);
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID)).thenReturn(events);

        List<DomainEvent> result = engine.replayFromSnapshot(AGGREGATE_ID, TENANT_ID, 10L);

        assertThat(result).hasSize(25).containsExactlyElementsOf(events);
    }

    @Test
    @DisplayName("replayEventsInBatches: returns all events (simplified implementation)")
    void replayEventsInBatches_returnsAllEvents() {
        List<DomainEvent> events = mockEvents(30);
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID)).thenReturn(events);

        List<DomainEvent> result = engine.replayEventsInBatches(AGGREGATE_ID, TENANT_ID, 10);

        assertThat(result).hasSize(30).containsExactlyElementsOf(events);
    }

    @Test
    @DisplayName("createSnapshots: returns empty list when no events exist")
    void createSnapshots_noEvents() {
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID))
                .thenReturn(Collections.emptyList());

        List<ReplaySnapshot> snapshots = engine.createSnapshots(AGGREGATE_ID, TENANT_ID);

        assertThat(snapshots).isEmpty();
    }

    @Test
    @DisplayName("createSnapshots: returns empty list when fewer than 100 events")
    void createSnapshots_fewerThan100() {
        List<DomainEvent> events = mockEvents(50);
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID))
                .thenReturn(events);

        List<ReplaySnapshot> snapshots = engine.createSnapshots(AGGREGATE_ID, TENANT_ID);

        assertThat(snapshots).isEmpty();
    }

    @Test
    @DisplayName("createSnapshots: returns one snapshot at exactly 100 events")
    void createSnapshots_exactly100() {
        List<DomainEvent> events = mockEvents(100);
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID))
                .thenReturn(events);

        List<ReplaySnapshot> snapshots = engine.createSnapshots(AGGREGATE_ID, TENANT_ID);

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).getVersion()).isEqualTo(100L);
        assertThat(snapshots.get(0).getAggregateId()).isEqualTo(AGGREGATE_ID);
        assertThat(snapshots.get(0).getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    @DisplayName("createSnapshots: returns snapshots at version 100 and 200 for 250 events")
    void createSnapshots_250events() {
        List<DomainEvent> events = mockEvents(250);
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID))
                .thenReturn(events);

        List<ReplaySnapshot> snapshots = engine.createSnapshots(AGGREGATE_ID, TENANT_ID);

        assertThat(snapshots).hasSize(2);
        assertThat(snapshots).extracting(ReplaySnapshot::getVersion)
                .containsExactly(100L, 200L);
    }
}
