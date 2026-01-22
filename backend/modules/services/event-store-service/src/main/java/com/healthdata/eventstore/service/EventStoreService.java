package com.healthdata.eventstore.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.eventstore.domain.EventProcessingStatus;
import com.healthdata.eventstore.domain.EventSnapshot;
import com.healthdata.eventstore.domain.EventStoreEntry;
import com.healthdata.eventstore.repository.EventProcessingStatusRepository;
import com.healthdata.eventstore.repository.EventSnapshotRepository;
import com.healthdata.eventstore.repository.EventStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * EventStoreService - Core service for event sourcing operations
 *
 * Purpose: Append-only event log with snapshot support
 *
 * Responsibilities:
 * 1. Append events to immutable log (NEVER update/delete)
 * 2. Retrieve events for aggregate reconstruction
 * 3. Create snapshots for performance optimization
 * 4. Track consumer processing status
 *
 * Usage Example:
 * <pre>
 * // 1. Append event
 * eventStoreService.appendEvent(
 *     patientId,
 *     "Patient",
 *     "PatientCreatedEvent",
 *     eventPayload,
 *     tenantId
 * );
 *
 * // 2. Reconstruct aggregate
 * List<EventStoreEntry> events = eventStoreService.getEventsForAggregate(
 *     patientId,
 *     "Patient",
 *     tenantId
 * );
 *
 * // 3. Replay events
 * Patient patient = new Patient();
 * for (EventStoreEntry event : events) {
 *     patient.apply(event);
 * }
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventStoreService {

    private final EventStoreRepository eventStoreRepository;
    private final EventSnapshotRepository snapshotRepository;
    private final EventProcessingStatusRepository processingStatusRepository;
    private final ObjectMapper objectMapper;

    /**
     * Append a new event to the event store
     *
     * This is the ONLY way to add data to the event store.
     * Events are immutable and can NEVER be updated or deleted.
     *
     * @param aggregateId   The aggregate identifier (e.g., patient UUID)
     * @param aggregateType The aggregate type (e.g., "Patient")
     * @param eventType     The event type (e.g., "PatientCreatedEvent")
     * @param eventPayload  The event data (will be serialized to JSON)
     * @param tenantId      Tenant identifier
     * @return The persisted event entry
     * @throws EventStorePersistenceException if event cannot be persisted
     */
    @Transactional
    public EventStoreEntry appendEvent(
            UUID aggregateId,
            String aggregateType,
            String eventType,
            Object eventPayload,
            String tenantId) {
        return appendEvent(aggregateId, aggregateType, eventType, eventPayload, tenantId, null, null, null, null);
    }

    /**
     * Append a new event with full metadata
     *
     * @param aggregateId   The aggregate identifier
     * @param aggregateType The aggregate type
     * @param eventType     The event type
     * @param eventPayload  The event data
     * @param tenantId      Tenant identifier
     * @param causationId   Event that caused this event (for tracing)
     * @param correlationId Correlation ID for distributed tracing
     * @param userId        User who triggered this event
     * @param userEmail     Email of user who triggered this event
     * @return The persisted event entry
     */
    @Transactional
    public EventStoreEntry appendEvent(
            UUID aggregateId,
            String aggregateType,
            String eventType,
            Object eventPayload,
            String tenantId,
            UUID causationId,
            UUID correlationId,
            String userId,
            String userEmail) {

        log.debug("Appending event: aggregateId={}, aggregateType={}, eventType={}",
                aggregateId, aggregateType, eventType);

        try {
            // Get next version for this aggregate
            Integer nextVersion = getNextVersion(aggregateId, aggregateType, tenantId);

            // Serialize event payload to JSON
            String payloadJson = objectMapper.writeValueAsString(eventPayload);

            // Create event store entry
            EventStoreEntry entry = EventStoreEntry.builder()
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .eventId(UUID.randomUUID())
                    .eventType(eventType)
                    .eventVersion(nextVersion)
                    .payload(payloadJson)
                    .occurredAt(Instant.now())
                    .tenantId(tenantId)
                    .causationId(causationId)
                    .correlationId(correlationId)
                    .userId(userId)
                    .userEmail(userEmail)
                    .build();

            // Persist event (immutable, append-only)
            EventStoreEntry savedEntry = eventStoreRepository.save(entry);

            log.info("Event appended: id={}, aggregateId={}, eventType={}, version={}",
                    savedEntry.getId(), aggregateId, eventType, nextVersion);

            // Create snapshot if needed (every N events)
            if (EventSnapshot.shouldCreateSnapshot(nextVersion)) {
                log.debug("Snapshot threshold reached at version {}", nextVersion);
                // Note: Actual snapshot creation happens externally
                // This service only logs the trigger point
            }

            return savedEntry;

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload", e);
            throw new EventStorePersistenceException("Failed to serialize event payload", e);
        } catch (Exception e) {
            log.error("Failed to append event", e);
            throw new EventStorePersistenceException("Failed to append event to store", e);
        }
    }

    /**
     * Get all events for an aggregate (for complete event replay)
     *
     * @param aggregateId   The aggregate identifier
     * @param aggregateType The aggregate type
     * @param tenantId      Tenant identifier
     * @return List of events ordered by version
     */
    @Transactional(readOnly = true)
    public List<EventStoreEntry> getEventsForAggregate(
            UUID aggregateId,
            String aggregateType,
            String tenantId) {

        log.debug("Fetching events for aggregate: aggregateId={}, aggregateType={}",
                aggregateId, aggregateType);

        List<EventStoreEntry> events = eventStoreRepository.findByAggregateIdAndType(
                aggregateId, aggregateType, tenantId
        );

        log.info("Retrieved {} events for aggregate {}", events.size(), aggregateId);
        return events;
    }

    /**
     * Get events after a specific version (for incremental updates)
     *
     * @param aggregateId   The aggregate identifier
     * @param aggregateType The aggregate type
     * @param afterVersion  Only return events with version > afterVersion
     * @param tenantId      Tenant identifier
     * @return List of events ordered by version
     */
    @Transactional(readOnly = true)
    public List<EventStoreEntry> getEventsAfterVersion(
            UUID aggregateId,
            String aggregateType,
            Integer afterVersion,
            String tenantId) {

        log.debug("Fetching events after version: aggregateId={}, afterVersion={}",
                aggregateId, afterVersion);

        return eventStoreRepository.findByAggregateIdAfterVersion(
                aggregateId, aggregateType, afterVersion, tenantId
        );
    }

    /**
     * Get events within a time range (for temporal queries)
     *
     * Example: "Show me all events that happened on January 15, 2026"
     *
     * @param startTime Start of time range
     * @param endTime   End of time range
     * @param tenantId  Tenant identifier
     * @return List of events ordered by time
     */
    @Transactional(readOnly = true)
    public List<EventStoreEntry> getEventsByTimeRange(
            Instant startTime,
            Instant endTime,
            String tenantId) {

        log.debug("Fetching events in time range: {} to {}", startTime, endTime);
        return eventStoreRepository.findByTimeRange(startTime, endTime, tenantId);
    }

    /**
     * Create a snapshot for an aggregate at current version
     *
     * @param aggregateId    The aggregate identifier
     * @param aggregateType  The aggregate type
     * @param snapshotData   The current aggregate state
     * @param tenantId       Tenant identifier
     * @return The created snapshot
     */
    @Transactional
    public EventSnapshot createSnapshot(
            UUID aggregateId,
            String aggregateType,
            Object snapshotData,
            String tenantId) {

        log.debug("Creating snapshot for aggregate: aggregateId={}", aggregateId);

        try {
            // Get current version
            Integer currentVersion = eventStoreRepository.findLatestVersion(aggregateId, aggregateType, tenantId)
                    .orElse(0);

            // Serialize snapshot data
            String snapshotJson = objectMapper.writeValueAsString(snapshotData);

            // Create snapshot
            EventSnapshot snapshot = EventSnapshot.builder()
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .snapshotVersion(currentVersion)
                    .snapshotData(snapshotJson)
                    .tenantId(tenantId)
                    .build();

            EventSnapshot savedSnapshot = snapshotRepository.save(snapshot);

            log.info("Snapshot created: aggregateId={}, version={}",
                    aggregateId, currentVersion);

            return savedSnapshot;

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize snapshot data", e);
            throw new EventStorePersistenceException("Failed to serialize snapshot data", e);
        }
    }

    /**
     * Get latest snapshot for an aggregate
     *
     * @param aggregateId   The aggregate identifier
     * @param aggregateType The aggregate type
     * @param tenantId      Tenant identifier
     * @return Latest snapshot if exists
     */
    @Transactional(readOnly = true)
    public Optional<EventSnapshot> getLatestSnapshot(
            UUID aggregateId,
            String aggregateType,
            String tenantId) {

        return snapshotRepository.findLatestSnapshot(aggregateId, aggregateType, tenantId);
    }

    /**
     * Update consumer processing status
     *
     * @param consumerName Consumer identifier
     * @param lastEventId  Last processed event ID
     * @param tenantId     Tenant identifier
     */
    @Transactional
    public void updateConsumerStatus(String consumerName, Long lastEventId, String tenantId) {
        EventProcessingStatus status = processingStatusRepository
                .findByConsumerNameAndTenantId(consumerName, tenantId)
                .orElseGet(() -> EventProcessingStatus.builder()
                        .consumerName(consumerName)
                        .tenantId(tenantId)
                        .lastProcessedEventId(0L)
                        .build());

        status.markProcessed(lastEventId);
        processingStatusRepository.save(status);

        log.debug("Updated consumer status: consumer={}, lastEventId={}",
                consumerName, lastEventId);
    }

    /**
     * Get consumer processing status
     *
     * @param consumerName Consumer identifier
     * @param tenantId     Tenant identifier
     * @return Processing status if exists
     */
    @Transactional(readOnly = true)
    public Optional<EventProcessingStatus> getConsumerStatus(String consumerName, String tenantId) {
        return processingStatusRepository.findByConsumerNameAndTenantId(consumerName, tenantId);
    }

    /**
     * Calculate next version for an aggregate
     *
     * @param aggregateId   The aggregate identifier
     * @param aggregateType The aggregate type
     * @param tenantId      Tenant identifier
     * @return Next version number (starts at 1)
     */
    private Integer getNextVersion(UUID aggregateId, String aggregateType, String tenantId) {
        Optional<Integer> latestVersion = eventStoreRepository.findLatestVersion(
                aggregateId, aggregateType, tenantId
        );
        return latestVersion.map(v -> v + 1).orElse(1);
    }

    /**
     * Exception thrown when event persistence fails
     */
    public static class EventStorePersistenceException extends RuntimeException {
        public EventStorePersistenceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
