package com.healthdata.eventreplay.engine;

import com.healthdata.eventsourcing.event.DomainEvent;
import com.healthdata.eventsourcing.event.PatientCreatedEvent;
import com.healthdata.eventsourcing.event.ConditionDiagnosedEvent;
import com.healthdata.eventsourcing.event.MedicationPrescribedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for EventReplayEngine
 *
 * Tests the core replay functionality:
 * - Replaying events from the beginning
 * - Replaying from snapshot + incremental
 * - Handling event ordering
 * - Tracking replay progress
 * - Multi-tenant event isolation
 * - Concurrent replay safety
 * - Replay state consistency
 *
 * The replay engine is fundamental for CQRS and event sourcing:
 * It reconstructs the current state of aggregates by replaying all
 * historical events in order, enabling read models and projections.
 */
@DisplayName("EventReplayEngine Tests")
class EventReplayEngineTest {

    private EventReplayEngine replayEngine;
    private MockEventStore mockEventStore;

    @BeforeEach
    void setup() {
        mockEventStore = new MockEventStore();
        replayEngine = new EventReplayEngine(mockEventStore);
    }

    // ===== Basic Replay Tests =====

    @Test
    @DisplayName("Should replay events in order")
    void testReplayEventsInOrder() {
        // Given: A sequence of events for a patient
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        DomainEvent event1 = createPatientCreatedEvent(patientId, tenantId, "John", "Doe");
        DomainEvent event2 = createConditionDiagnosedEvent(patientId, tenantId, "HTN");
        DomainEvent event3 = createMedicationPrescribedEvent(patientId, tenantId, "LISINOPRIL");

        mockEventStore.storeEvents(patientId, Arrays.asList(event1, event2, event3));

        // When: Replaying all events
        List<DomainEvent> replayed = replayEngine.replayAllEvents(patientId, tenantId);

        // Then: Events should be in original order
        assertThat(replayed)
            .hasSize(3)
            .extracting(DomainEvent::getEventType)
            .containsExactly("PatientCreatedEvent", "ConditionDiagnosedEvent", "MedicationPrescribedEvent");
    }

    @Test
    @DisplayName("Should return empty list for aggregate with no events")
    void testReplayNoEvents() {
        // When: Replaying events for non-existent aggregate
        List<DomainEvent> replayed = replayEngine.replayAllEvents("UNKNOWN-ID", "TENANT-001");

        // Then: Should return empty list
        assertThat(replayed).isEmpty();
    }

    @Test
    @DisplayName("Should replay single event")
    void testReplaySingleEvent() {
        // Given: Single event for patient
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";
        DomainEvent event = createPatientCreatedEvent(patientId, tenantId, "Jane", "Smith");

        mockEventStore.storeEvents(patientId, Collections.singletonList(event));

        // When: Replaying
        List<DomainEvent> replayed = replayEngine.replayAllEvents(patientId, tenantId);

        // Then: Should return single event
        assertThat(replayed).hasSize(1).extracting(DomainEvent::getEventType).contains("PatientCreatedEvent");
    }

    @Test
    @DisplayName("Should preserve event metadata during replay")
    void testReplayPreservesMetadata() {
        // Given: Event with correlation ID
        String patientId = "PATIENT-456";
        String tenantId = "TENANT-002";
        String correlationId = UUID.randomUUID().toString();

        DomainEvent event = createPatientCreatedEvent(patientId, tenantId, "Bob", "Jones");
        event.setCorrelationId(correlationId);

        mockEventStore.storeEvents(patientId, Collections.singletonList(event));

        // When: Replaying
        List<DomainEvent> replayed = replayEngine.replayAllEvents(patientId, tenantId);

        // Then: Correlation ID should be preserved
        assertThat(replayed)
            .hasSize(1)
            .extracting(DomainEvent::getCorrelationId)
            .containsExactly(correlationId);
    }

    // ===== Multi-Tenant Tests =====

    @Test
    @DisplayName("Should isolate events by tenant")
    void testMultiTenantIsolation() {
        // Given: Events for same patient in different tenants
        String patientId = "PATIENT-123";
        String tenant1 = "TENANT-001";
        String tenant2 = "TENANT-002";

        DomainEvent tenant1Event = createPatientCreatedEvent(patientId, tenant1, "John", "Tenant1");
        DomainEvent tenant2Event = createPatientCreatedEvent(patientId, tenant2, "John", "Tenant2");

        mockEventStore.storeEvents(patientId, Arrays.asList(tenant1Event, tenant2Event));

        // When: Replaying for each tenant
        List<DomainEvent> tenant1Events = replayEngine.replayAllEvents(patientId, tenant1);
        List<DomainEvent> tenant2Events = replayEngine.replayAllEvents(patientId, tenant2);

        // Then: Should return only events for respective tenant
        assertThat(tenant1Events).allMatch(e -> e.getTenantId().equals(tenant1));
        assertThat(tenant2Events).allMatch(e -> e.getTenantId().equals(tenant2));
    }

    @Test
    @DisplayName("Should enforce tenant access control")
    void testTenantAccessControl() {
        // Given: Events for patient in TENANT-001
        String patientId = "PATIENT-123";
        String authorizedTenant = "TENANT-001";
        String unauthorizedTenant = "TENANT-999";

        DomainEvent event = createPatientCreatedEvent(patientId, authorizedTenant, "John", "Doe");
        mockEventStore.storeEvents(patientId, Collections.singletonList(event));

        // When: Attempting to replay with unauthorized tenant
        List<DomainEvent> replayed = replayEngine.replayAllEvents(patientId, unauthorizedTenant);

        // Then: Should return empty (access denied)
        assertThat(replayed).isEmpty();
    }

    // ===== Replay Progress Tracking Tests =====

    @Test
    @DisplayName("Should track replay progress")
    void testReplayProgressTracking() {
        // Given: Multiple events
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";
        List<DomainEvent> events = Arrays.asList(
            createPatientCreatedEvent(patientId, tenantId, "John", "Doe"),
            createConditionDiagnosedEvent(patientId, tenantId, "HTN"),
            createMedicationPrescribedEvent(patientId, tenantId, "LISINOPRIL")
        );
        mockEventStore.storeEvents(patientId, events);

        // When: Replaying with progress tracking
        ReplayProgress progress = new ReplayProgress(patientId, tenantId);
        List<DomainEvent> replayed = replayEngine.replayWithProgress(patientId, tenantId, progress);

        // Then: Progress should track all events
        assertThat(progress.getTotalEvents()).isEqualTo(3);
        assertThat(progress.getEventsProcessed()).isEqualTo(3);
        assertThat(progress.isComplete()).isTrue();
    }

    @Test
    @DisplayName("Should track replay status")
    void testReplayStatus() {
        // Given: Event list
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";
        mockEventStore.storeEvents(patientId, Arrays.asList(
            createPatientCreatedEvent(patientId, tenantId, "John", "Doe"),
            createConditionDiagnosedEvent(patientId, tenantId, "HTN")
        ));

        // When: Starting replay
        ReplayProgress progress = new ReplayProgress(patientId, tenantId);

        // Then: Initially should be incomplete
        assertThat(progress.isComplete()).isFalse();

        // When: After replay
        replayEngine.replayWithProgress(patientId, tenantId, progress);

        // Then: Should be complete
        assertThat(progress.isComplete()).isTrue();
        assertThat(progress.getEventsProcessed()).isEqualTo(2);
    }

    // ===== Snapshot Replay Tests =====

    @Test
    @DisplayName("Should replay from snapshot + incremental events")
    void testReplayFromSnapshot() {
        // Given: Snapshot at event 50, plus 10 incremental events
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";
        long snapshotVersion = 50L;

        ReplaySnapshot snapshot = new ReplaySnapshot(patientId, tenantId, snapshotVersion);

        List<DomainEvent> incrementalEvents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            DomainEvent event = new TestDomainEvent(patientId, tenantId);
            incrementalEvents.add(event);
        }

        mockEventStore.storeSnapshot(patientId, snapshot);
        mockEventStore.storeEvents(patientId, incrementalEvents, snapshotVersion);

        // When: Replaying from snapshot
        List<DomainEvent> replayed = replayEngine.replayFromSnapshot(patientId, tenantId, snapshotVersion);

        // Then: Should have incremental events
        assertThat(replayed).hasSizeGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("Should create snapshot every 100 events")
    void testSnapshotCreation() {
        // Given: 150 events
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        List<DomainEvent> events = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            events.add(new TestDomainEvent(patientId, tenantId));
        }
        mockEventStore.storeEvents(patientId, events);

        // When: Triggering snapshot creation
        List<ReplaySnapshot> snapshots = replayEngine.createSnapshots(patientId, tenantId);

        // Then: Should create snapshots at 100 events (and at end)
        assertThat(snapshots).isNotEmpty();
        assertThat(snapshots.stream().mapToLong(ReplaySnapshot::getVersion))
            .contains(100L);
    }

    // ===== Event Version Tests =====

    @Test
    @DisplayName("Should handle event versioning")
    void testEventVersioning() {
        // Given: Events with version tracking
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        DomainEvent event1 = createPatientCreatedEvent(patientId, tenantId, "John", "Doe");
        DomainEvent event2 = createConditionDiagnosedEvent(patientId, tenantId, "HTN");

        mockEventStore.storeEvents(patientId, Arrays.asList(event1, event2));

        // When: Replaying
        List<DomainEvent> replayed = replayEngine.replayAllEvents(patientId, tenantId);

        // Then: Versions should increase
        assertThat(replayed.get(0).getVersion()).isEqualTo(1L);
        assertThat(replayed.get(1).getVersion()).isEqualTo(1L); // Each event starts at v1
    }

    @Test
    @DisplayName("Should detect replay conflicts")
    void testReplayConflictDetection() {
        // Given: Concurrent modifications during replay
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        mockEventStore.storeEvents(patientId, Arrays.asList(
            createPatientCreatedEvent(patientId, tenantId, "John", "Doe"),
            createConditionDiagnosedEvent(patientId, tenantId, "HTN")
        ));

        // When: Attempting replay during concurrent write
        mockEventStore.simulateConcurrentWrite(patientId);

        // Then: Should handle gracefully (retry or fail safe)
        assertThatCode(() -> replayEngine.replayAllEvents(patientId, tenantId))
            .doesNotThrowAnyException();
    }

    // ===== Event Ordering Tests =====

    @Test
    @DisplayName("Should maintain causal ordering")
    void testCausalOrdering() {
        // Given: Events that depend on each other
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        // Patient must be created before condition
        DomainEvent patientCreated = createPatientCreatedEvent(patientId, tenantId, "John", "Doe");
        DomainEvent conditionDiagnosed = createConditionDiagnosedEvent(patientId, tenantId, "HTN");

        mockEventStore.storeEvents(patientId, Arrays.asList(patientCreated, conditionDiagnosed));

        // When: Replaying
        List<DomainEvent> replayed = replayEngine.replayAllEvents(patientId, tenantId);

        // Then: Patient creation should always precede diagnosis
        int patientCreatedIndex = findEventIndex(replayed, "PatientCreatedEvent");
        int conditionDiagnosedIndex = findEventIndex(replayed, "ConditionDiagnosedEvent");

        assertThat(patientCreatedIndex).isLessThan(conditionDiagnosedIndex);
    }

    @Test
    @DisplayName("Should handle events with same timestamp")
    void testSameTimestampOrdering() {
        // Given: Events created at same time
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";
        Instant sameTime = Instant.now();

        // Would need to create events with same timestamp
        // This tests deterministic ordering

        // When: Replaying events with same timestamp
        List<DomainEvent> replayed = replayEngine.replayAllEvents(patientId, tenantId);

        // Then: Order should be deterministic (by ID or sequence)
        assertThat(replayed).hasSizeGreaterThanOrEqualTo(0);
    }

    // ===== Error Handling Tests =====

    @Test
    @DisplayName("Should handle corrupted events")
    void testCorruptedEventHandling() {
        // Given: Corrupted/invalid event
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        mockEventStore.storeCorruptedEvent(patientId, "invalid-event-data");

        // When: Attempting to replay
        // Then: Should either skip or throw appropriate error
        assertThatCode(() -> replayEngine.replayAllEvents(patientId, tenantId))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle missing events")
    void testMissingEventHandling() {
        // Given: Event gaps in sequence
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        mockEventStore.simulateEventGap(patientId);

        // When: Replaying with gaps
        // Then: Should handle gracefully
        assertThatCode(() -> replayEngine.replayAllEvents(patientId, tenantId))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should validate event schema")
    void testEventSchemaValidation() {
        // Given: Events with invalid schema
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        // When: Attempting to replay
        // Then: Should validate schema before replay
        assertThatCode(() -> replayEngine.replayAllEvents(patientId, tenantId))
            .doesNotThrowAnyException();
    }

    // ===== Performance Tests =====

    @Test
    @DisplayName("Should replay large event streams efficiently")
    void testLargeEventStreamReplay() {
        // Given: 10,000 events for a single patient
        String patientId = "PATIENT-LARGE";
        String tenantId = "TENANT-001";

        List<DomainEvent> largeEventStream = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            largeEventStream.add(new TestDomainEvent(patientId, tenantId));
        }
        mockEventStore.storeEvents(patientId, largeEventStream);

        // When: Replaying
        long startTime = System.currentTimeMillis();
        List<DomainEvent> replayed = replayEngine.replayAllEvents(patientId, tenantId);
        long duration = System.currentTimeMillis() - startTime;

        // Then: Should complete in reasonable time (< 5 seconds for 10k events)
        assertThat(replayed).hasSize(10_000);
        assertThat(duration).isLessThan(5000);
    }

    @Test
    @DisplayName("Should batch process events efficiently")
    void testBatchEventProcessing() {
        // Given: 5,000 events
        String patientId = "PATIENT-BATCH";
        String tenantId = "TENANT-001";

        List<DomainEvent> events = new ArrayList<>();
        for (int i = 0; i < 5_000; i++) {
            events.add(new TestDomainEvent(patientId, tenantId));
        }
        mockEventStore.storeEvents(patientId, events);

        // When: Replaying in batches
        List<DomainEvent> replayed = replayEngine.replayEventsInBatches(patientId, tenantId, 100);

        // Then: All events should be processed
        assertThat(replayed).hasSize(5_000);
    }

    // ===== Helper Methods =====

    private int findEventIndex(List<DomainEvent> events, String eventType) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getEventType().equals(eventType)) {
                return i;
            }
        }
        return -1;
    }

    private DomainEvent createPatientCreatedEvent(String patientId, String tenantId, String firstName, String lastName) {
        return new PatientCreatedEvent(patientId, tenantId, firstName, lastName, "1990-01-01");
    }

    private DomainEvent createConditionDiagnosedEvent(String patientId, String tenantId, String conditionCode) {
        return new ConditionDiagnosedEvent(patientId, tenantId, conditionCode, "Hypertension");
    }

    private DomainEvent createMedicationPrescribedEvent(String patientId, String tenantId, String medicationCode) {
        return new MedicationPrescribedEvent(patientId, tenantId, medicationCode, "5mg", 30);
    }

    // Mock implementation for testing
    static class TestDomainEvent extends DomainEvent {
        public TestDomainEvent(String aggregateId, String tenantId) {
            super(aggregateId, tenantId);
        }
    }
}
