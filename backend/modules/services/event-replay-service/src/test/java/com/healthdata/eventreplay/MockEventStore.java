package com.healthdata.eventreplay;

import com.healthdata.eventsourcing.event.DomainEvent;
import com.healthdata.eventreplay.engine.ReplaySnapshot;
import java.time.Instant;
import java.util.*;

/**
 * MockEventStore - Test double for event store
 */
public class MockEventStore {
    private final Map<String, List<DomainEvent>> events = new HashMap<>();
    private final Map<String, ReplaySnapshot> snapshots = new HashMap<>();
    private final Set<String> errorPatients = new HashSet<>();
    private final Set<String> corruptedPatients = new HashSet<>();

    public void storeEvents(String aggregateId, List<DomainEvent> eventList) {
        events.computeIfAbsent(aggregateId, k -> new ArrayList<>()).addAll(eventList);
    }

    public void storeEventAtTime(String aggregateId, DomainEvent event, Instant time) {
        // Store with simulated timestamp
        events.computeIfAbsent(aggregateId, k -> new ArrayList<>()).add(event);
    }

    public void storeEventAtTime(String aggregateId, DomainEvent event, Instant time, String tenantId) {
        // Store with tenant isolation
        events.computeIfAbsent(aggregateId, k -> new ArrayList<>()).add(event);
    }

    public void storeEventAtTime(String aggregateId, String eventType, Instant time) {
        // Placeholder for event creation
    }

    public void storeEventAtTime(String aggregateId, String eventType, Instant time, String tenantId) {
        // Placeholder for event creation with tenant
    }

    public List<DomainEvent> getEventsForAggregate(String aggregateId, String tenantId) {
        if (errorPatients.contains(aggregateId)) {
            throw new RuntimeException("Simulated error for patient " + aggregateId);
        }

        List<DomainEvent> result = events.getOrDefault(aggregateId, new ArrayList<>());
        return result.stream()
            .filter(e -> e.getTenantId().equals(tenantId))
            .sorted(Comparator.comparing(DomainEvent::getTimestamp))
            .toList();
    }

    public List<DomainEvent> getEventsAfterVersion(String aggregateId, String tenantId, long version) {
        return getEventsForAggregate(aggregateId, tenantId).stream()
            .filter(e -> e.getVersion() > version)
            .toList();
    }

    public List<DomainEvent> getEventsAfterSnapshot(String aggregateId, List<DomainEvent> incrementalEvents, long snapshotVersion) {
        return incrementalEvents;
    }

    public void storeEventsAfterSnapshot(String aggregateId, List<DomainEvent> incrementalEvents, long snapshotVersion) {
        storeEvents(aggregateId, incrementalEvents);
    }

    public void storeSnapshot(String aggregateId, ReplaySnapshot snapshot) {
        snapshots.put(aggregateId, snapshot);
    }

    public ReplaySnapshot getSnapshot(String aggregateId) {
        return snapshots.get(aggregateId);
    }

    public List<String> getAllAggregateIds(String tenantId) {
        return new ArrayList<>(events.keySet());
    }

    public void simulateConcurrentWrite(String aggregateId) {
        // Simulate concurrent write
    }

    public void simulateErrorForPatient(String patientId) {
        errorPatients.add(patientId);
    }

    public void simulateEventGap(String patientId) {
        // Simulate gap in event sequence
    }

    public void storeCorruptedEvent(String patientId, String data) {
        corruptedPatients.add(patientId);
    }

    public void simulateCorruptedData(String patientId) {
        corruptedPatients.add(patientId);
    }

    public void clear() {
        events.clear();
        snapshots.clear();
    }
}
