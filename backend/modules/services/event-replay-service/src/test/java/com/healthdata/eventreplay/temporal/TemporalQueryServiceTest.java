package com.healthdata.eventreplay.temporal;

import com.healthdata.eventreplay.engine.EventStore;
import com.healthdata.eventreplay.engine.ProjectionStore;
import com.healthdata.eventsourcing.event.DomainEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the event-replay-service TEMPORAL layer:
 * {@link TemporalSnapshot}, {@link AuditTrail}, and {@link TemporalQueryService}.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class TemporalQueryServiceTest {

    private static final String AGGREGATE_ID = "agg-temporal-001";
    private static final String TENANT_ID = "tenant-temporal";

    @Mock
    private EventStore eventStore;

    @Mock
    private ProjectionStore projectionStore;

    // ---------------------------------------------------------------------------
    // Helper: create a DomainEvent mock with a specific timestamp
    // ---------------------------------------------------------------------------
    private DomainEvent mockEventAt(Instant timestamp, String eventId) {
        DomainEvent event = mock(DomainEvent.class);
        lenient().when(event.getEventId()).thenReturn(eventId);
        lenient().when(event.getAggregateId()).thenReturn(AGGREGATE_ID);
        lenient().when(event.getTenantId()).thenReturn(TENANT_ID);
        lenient().when(event.getEventType()).thenReturn("TestEvent");
        lenient().when(event.getTimestamp()).thenReturn(timestamp);
        return event;
    }

    // =========================================================================
    // TemporalSnapshot tests
    // =========================================================================

    @Test
    @DisplayName("TemporalSnapshot: creation sets aggregateId, tenantId, version, and timestamp")
    void temporalSnapshot_creation() {
        Instant now = Instant.now();
        TemporalSnapshot snapshot = new TemporalSnapshot(AGGREGATE_ID, TENANT_ID, 5L, now);

        assertThat(snapshot.getAggregateId()).isEqualTo(AGGREGATE_ID);
        assertThat(snapshot.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(snapshot.getVersion()).isEqualTo(5L);
        assertThat(snapshot.getTimestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("TemporalSnapshot: hasOpenGap returns false when neither diagnosed nor prescribed")
    void temporalSnapshot_hasOpenGap_neitherCondition() {
        TemporalSnapshot snapshot = new TemporalSnapshot(AGGREGATE_ID, TENANT_ID, 1L, Instant.now());

        // Defaults: conditionDiagnosed=false, medicationPrescribed=false
        assertThat(snapshot.hasOpenGap()).isFalse();
        assertThat(snapshot.hasConditionDiagnosed()).isFalse();
        assertThat(snapshot.hasMedicationPrescribed()).isFalse();
    }

    @Test
    @DisplayName("TemporalSnapshot: hasOpenGap returns true when diagnosed but not prescribed")
    void temporalSnapshot_hasOpenGap_diagnosedOnly() {
        TemporalSnapshot snapshot = new TemporalSnapshot(AGGREGATE_ID, TENANT_ID, 1L, Instant.now());

        snapshot.setConditionDiagnosed(true);
        snapshot.setMedicationPrescribed(false);

        assertThat(snapshot.hasOpenGap()).isTrue();
    }

    @Test
    @DisplayName("TemporalSnapshot: hasOpenGap returns false when both diagnosed and prescribed")
    void temporalSnapshot_hasOpenGap_bothConditions() {
        TemporalSnapshot snapshot = new TemporalSnapshot(AGGREGATE_ID, TENANT_ID, 1L, Instant.now());

        snapshot.setConditionDiagnosed(true);
        snapshot.setMedicationPrescribed(true);

        assertThat(snapshot.hasOpenGap()).isFalse();
    }

    @Test
    @DisplayName("TemporalSnapshot: hasOpenGap returns false when prescribed but not diagnosed")
    void temporalSnapshot_hasOpenGap_prescribedOnly() {
        TemporalSnapshot snapshot = new TemporalSnapshot(AGGREGATE_ID, TENANT_ID, 1L, Instant.now());

        snapshot.setConditionDiagnosed(false);
        snapshot.setMedicationPrescribed(true);

        assertThat(snapshot.hasOpenGap()).isFalse();
    }

    // =========================================================================
    // AuditTrail tests
    // =========================================================================

    @Test
    @DisplayName("AuditTrail: creation stores aggregateId, tenantId, and immutable events copy")
    void auditTrail_creation() {
        DomainEvent e1 = mockEventAt(Instant.now(), "evt-1");
        DomainEvent e2 = mockEventAt(Instant.now(), "evt-2");
        List<DomainEvent> events = new ArrayList<>(List.of(e1, e2));

        AuditTrail trail = new AuditTrail(AGGREGATE_ID, TENANT_ID, events);

        assertThat(trail.getAggregateId()).isEqualTo(AGGREGATE_ID);
        assertThat(trail.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(trail.getEvents()).hasSize(2);
    }

    @Test
    @DisplayName("AuditTrail: events list is immutable - cannot add to returned list")
    void auditTrail_eventsImmutable() {
        DomainEvent e1 = mockEventAt(Instant.now(), "evt-1");
        AuditTrail trail = new AuditTrail(AGGREGATE_ID, TENANT_ID, List.of(e1));

        List<DomainEvent> returned = trail.getEvents();

        assertThatThrownBy(() -> returned.add(mock(DomainEvent.class)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("AuditTrail: modifying original list does not affect stored events")
    void auditTrail_defensiveCopy() {
        DomainEvent e1 = mockEventAt(Instant.now(), "evt-1");
        List<DomainEvent> mutableList = new ArrayList<>(List.of(e1));

        AuditTrail trail = new AuditTrail(AGGREGATE_ID, TENANT_ID, mutableList);

        // Mutate the original list
        mutableList.add(mockEventAt(Instant.now(), "evt-2"));

        // AuditTrail should still have only 1 event
        assertThat(trail.getEvents()).hasSize(1);
    }

    // =========================================================================
    // TemporalQueryService.getStateAsOf tests
    // =========================================================================

    @Test
    @DisplayName("getStateAsOf: returns null when no events exist before pointInTime")
    void getStateAsOf_noEventsBeforeTime_returnsNull() {
        Instant queryTime = Instant.parse("2026-01-01T00:00:00Z");
        Instant afterQuery = Instant.parse("2026-06-01T00:00:00Z");

        DomainEvent futureEvent = mockEventAt(afterQuery, "evt-future");
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID))
                .thenReturn(List.of(futureEvent));

        TemporalQueryService service = new TemporalQueryService(eventStore, projectionStore);

        TemporalSnapshot result = service.getStateAsOf(AGGREGATE_ID, TENANT_ID, queryTime);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getStateAsOf: returns null when EventStore has no events at all")
    void getStateAsOf_emptyEventStore_returnsNull() {
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID))
                .thenReturn(Collections.emptyList());

        TemporalQueryService service = new TemporalQueryService(eventStore, projectionStore);

        TemporalSnapshot result = service.getStateAsOf(AGGREGATE_ID, TENANT_ID, Instant.now());

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getStateAsOf: returns snapshot with correct version for events before pointInTime")
    void getStateAsOf_eventsBeforeTime_returnsSnapshot() {
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-02-01T00:00:00Z");
        Instant t3 = Instant.parse("2026-03-01T00:00:00Z");
        Instant queryTime = Instant.parse("2026-02-15T00:00:00Z");

        DomainEvent e1 = mockEventAt(t1, "evt-1");
        DomainEvent e2 = mockEventAt(t2, "evt-2");
        DomainEvent e3 = mockEventAt(t3, "evt-3");

        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID))
                .thenReturn(List.of(e1, e2, e3));

        TemporalQueryService service = new TemporalQueryService(eventStore, projectionStore);

        TemporalSnapshot result = service.getStateAsOf(AGGREGATE_ID, TENANT_ID, queryTime);

        assertThat(result).isNotNull();
        assertThat(result.getAggregateId()).isEqualTo(AGGREGATE_ID);
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        // e1 (Jan) and e2 (Feb) are <= queryTime (Feb 15), so version = 2
        assertThat(result.getVersion()).isEqualTo(2);
        assertThat(result.getTimestamp()).isEqualTo(queryTime);
    }

    // =========================================================================
    // TemporalQueryService.getStateRange tests
    // =========================================================================

    @Test
    @DisplayName("getStateRange: throws InvalidTemporalQueryException when startTime is after endTime")
    void getStateRange_invalidRange_throws() {
        Instant start = Instant.parse("2026-06-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-01T00:00:00Z");

        TemporalQueryService service = new TemporalQueryService(eventStore, projectionStore);

        assertThatThrownBy(() -> service.getStateRange(AGGREGATE_ID, TENANT_ID, start, end))
                .isInstanceOf(InvalidTemporalQueryException.class)
                .hasMessageContaining("Start time cannot be after end time");
    }

    @Test
    @DisplayName("getStateRange: returns snapshots only for events within [startTime, endTime]")
    void getStateRange_validRange_returnsCorrectSnapshots() {
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-02-01T00:00:00Z");
        Instant t3 = Instant.parse("2026-03-01T00:00:00Z");
        Instant t4 = Instant.parse("2026-04-01T00:00:00Z");

        Instant rangeStart = Instant.parse("2026-01-15T00:00:00Z");
        Instant rangeEnd = Instant.parse("2026-03-15T00:00:00Z");

        DomainEvent e1 = mockEventAt(t1, "evt-1"); // before range
        DomainEvent e2 = mockEventAt(t2, "evt-2"); // in range
        DomainEvent e3 = mockEventAt(t3, "evt-3"); // in range
        DomainEvent e4 = mockEventAt(t4, "evt-4"); // after range

        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID))
                .thenReturn(List.of(e1, e2, e3, e4));

        TemporalQueryService service = new TemporalQueryService(eventStore, projectionStore);

        List<TemporalSnapshot> result = service.getStateRange(AGGREGATE_ID, TENANT_ID, rangeStart, rangeEnd);

        // Only e2 (Feb) and e3 (Mar) are within [Jan 15, Mar 15]
        assertThat(result).hasSize(2);
        // e2 is the 2nd event (index 1), so version = 2; e3 is the 3rd (index 2), version = 3
        assertThat(result).extracting(TemporalSnapshot::getVersion)
                .containsExactly(2L, 3L);
        assertThat(result).extracting(TemporalSnapshot::getTimestamp)
                .containsExactly(t2, t3);
    }

    @Test
    @DisplayName("getStateRange: returns empty list when no events fall within range")
    void getStateRange_noEventsInRange_returnsEmpty() {
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant rangeStart = Instant.parse("2026-06-01T00:00:00Z");
        Instant rangeEnd = Instant.parse("2026-12-01T00:00:00Z");

        DomainEvent e1 = mockEventAt(t1, "evt-1");
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID))
                .thenReturn(List.of(e1));

        TemporalQueryService service = new TemporalQueryService(eventStore, projectionStore);

        List<TemporalSnapshot> result = service.getStateRange(AGGREGATE_ID, TENANT_ID, rangeStart, rangeEnd);

        assertThat(result).isEmpty();
    }

    // =========================================================================
    // TemporalQueryService.getAuditTrail tests
    // =========================================================================

    @Test
    @DisplayName("getAuditTrail: returns AuditTrail with events filtered by time range")
    void getAuditTrail_filtersEventsByTimeRange() {
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-02-01T00:00:00Z");
        Instant t3 = Instant.parse("2026-03-01T00:00:00Z");

        Instant rangeStart = Instant.parse("2026-01-15T00:00:00Z");
        Instant rangeEnd = Instant.parse("2026-02-15T00:00:00Z");

        DomainEvent e1 = mockEventAt(t1, "evt-1"); // before range
        DomainEvent e2 = mockEventAt(t2, "evt-2"); // in range
        DomainEvent e3 = mockEventAt(t3, "evt-3"); // after range

        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID))
                .thenReturn(List.of(e1, e2, e3));

        TemporalQueryService service = new TemporalQueryService(eventStore, projectionStore);

        AuditTrail trail = service.getAuditTrail(AGGREGATE_ID, TENANT_ID, rangeStart, rangeEnd);

        assertThat(trail).isNotNull();
        assertThat(trail.getAggregateId()).isEqualTo(AGGREGATE_ID);
        assertThat(trail.getTenantId()).isEqualTo(TENANT_ID);
        // Only e2 (Feb 1) is within [Jan 15, Feb 15]
        assertThat(trail.getEvents()).hasSize(1);
        assertThat(trail.getEvents().get(0).getEventId()).isEqualTo("evt-2");
    }

    @Test
    @DisplayName("getAuditTrail: returns AuditTrail with empty events when none match range")
    void getAuditTrail_noEventsInRange_returnsEmptyTrail() {
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant rangeStart = Instant.parse("2026-06-01T00:00:00Z");
        Instant rangeEnd = Instant.parse("2026-12-01T00:00:00Z");

        DomainEvent e1 = mockEventAt(t1, "evt-1");
        when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID))
                .thenReturn(List.of(e1));

        TemporalQueryService service = new TemporalQueryService(eventStore, projectionStore);

        AuditTrail trail = service.getAuditTrail(AGGREGATE_ID, TENANT_ID, rangeStart, rangeEnd);

        assertThat(trail).isNotNull();
        assertThat(trail.getEvents()).isEmpty();
    }
}
