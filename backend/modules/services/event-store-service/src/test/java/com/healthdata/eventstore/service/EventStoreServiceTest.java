package com.healthdata.eventstore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.eventstore.domain.EventProcessingStatus;
import com.healthdata.eventstore.domain.EventSnapshot;
import com.healthdata.eventstore.domain.EventStoreEntry;
import com.healthdata.eventstore.repository.EventProcessingStatusRepository;
import com.healthdata.eventstore.repository.EventSnapshotRepository;
import com.healthdata.eventstore.repository.EventStoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventStoreService.
 * Tests event sourcing operations including append, replay, snapshots, and consumer tracking.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Event Store Service Tests")
@Tag("unit")
class EventStoreServiceTest {

    @Mock
    private EventStoreRepository eventStoreRepository;

    @Mock
    private EventSnapshotRepository snapshotRepository;

    @Mock
    private EventProcessingStatusRepository processingStatusRepository;

    @Captor
    private ArgumentCaptor<EventStoreEntry> entryCaptor;

    @Captor
    private ArgumentCaptor<EventSnapshot> snapshotCaptor;

    @Captor
    private ArgumentCaptor<EventProcessingStatus> statusCaptor;

    private EventStoreService eventStoreService;
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID AGGREGATE_ID = UUID.randomUUID();
    private static final String AGGREGATE_TYPE = "Patient";
    private static final String EVENT_TYPE = "PatientCreatedEvent";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        eventStoreService = new EventStoreService(
                eventStoreRepository,
                snapshotRepository,
                processingStatusRepository,
                objectMapper
        );
    }

    @Nested
    @DisplayName("Append Event Tests")
    class AppendEventTests {

        @Test
        @DisplayName("Should append event with correct version for new aggregate")
        void shouldAppendEventWithCorrectVersionForNewAggregate() {
            // Given
            Map<String, Object> payload = Map.of("name", "John Doe", "mrn", "TEST-MRN-001");
            when(eventStoreRepository.findLatestVersion(AGGREGATE_ID, AGGREGATE_TYPE, TENANT_ID))
                    .thenReturn(Optional.empty()); // No existing events
            when(eventStoreRepository.save(any(EventStoreEntry.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            EventStoreEntry result = eventStoreService.appendEvent(
                    AGGREGATE_ID, AGGREGATE_TYPE, EVENT_TYPE, payload, TENANT_ID);

            // Then
            assertThat(result).isNotNull();
            verify(eventStoreRepository).save(entryCaptor.capture());
            EventStoreEntry saved = entryCaptor.getValue();
            assertThat(saved.getAggregateId()).isEqualTo(AGGREGATE_ID);
            assertThat(saved.getAggregateType()).isEqualTo(AGGREGATE_TYPE);
            assertThat(saved.getEventType()).isEqualTo(EVENT_TYPE);
            assertThat(saved.getEventVersion()).isEqualTo(1); // First event
            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Should increment version for existing aggregate")
        void shouldIncrementVersionForExistingAggregate() {
            // Given
            Map<String, Object> payload = Map.of("status", "updated");
            when(eventStoreRepository.findLatestVersion(AGGREGATE_ID, AGGREGATE_TYPE, TENANT_ID))
                    .thenReturn(Optional.of(5)); // Existing version is 5
            when(eventStoreRepository.save(any(EventStoreEntry.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            eventStoreService.appendEvent(AGGREGATE_ID, AGGREGATE_TYPE, "PatientUpdatedEvent", payload, TENANT_ID);

            // Then
            verify(eventStoreRepository).save(entryCaptor.capture());
            assertThat(entryCaptor.getValue().getEventVersion()).isEqualTo(6); // 5 + 1
        }

        @Test
        @DisplayName("Should serialize payload to JSON")
        void shouldSerializePayloadToJson() {
            // Given
            Map<String, Object> payload = Map.of("name", "Jane Doe", "age", 30);
            when(eventStoreRepository.findLatestVersion(any(), any(), any()))
                    .thenReturn(Optional.empty());
            when(eventStoreRepository.save(any(EventStoreEntry.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            eventStoreService.appendEvent(AGGREGATE_ID, AGGREGATE_TYPE, EVENT_TYPE, payload, TENANT_ID);

            // Then
            verify(eventStoreRepository).save(entryCaptor.capture());
            String payloadJson = entryCaptor.getValue().getPayload();
            assertThat(payloadJson).contains("\"name\"");
            assertThat(payloadJson).contains("Jane Doe");
            assertThat(payloadJson).contains("30");
        }

        @Test
        @DisplayName("Should generate unique event ID")
        void shouldGenerateUniqueEventId() {
            // Given
            Map<String, Object> payload = Map.of("data", "test");
            when(eventStoreRepository.findLatestVersion(any(), any(), any()))
                    .thenReturn(Optional.empty());
            when(eventStoreRepository.save(any(EventStoreEntry.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            eventStoreService.appendEvent(AGGREGATE_ID, AGGREGATE_TYPE, EVENT_TYPE, payload, TENANT_ID);

            // Then
            verify(eventStoreRepository).save(entryCaptor.capture());
            assertThat(entryCaptor.getValue().getEventId()).isNotNull();
        }

        @Test
        @DisplayName("Should set occurrence timestamp")
        void shouldSetOccurrenceTimestamp() {
            // Given
            Instant beforeTest = Instant.now();
            Map<String, Object> payload = Map.of("data", "test");
            when(eventStoreRepository.findLatestVersion(any(), any(), any()))
                    .thenReturn(Optional.empty());
            when(eventStoreRepository.save(any(EventStoreEntry.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            eventStoreService.appendEvent(AGGREGATE_ID, AGGREGATE_TYPE, EVENT_TYPE, payload, TENANT_ID);

            // Then
            verify(eventStoreRepository).save(entryCaptor.capture());
            Instant occurredAt = entryCaptor.getValue().getOccurredAt();
            assertThat(occurredAt).isNotNull();
            assertThat(occurredAt).isAfterOrEqualTo(beforeTest);
        }

        @Test
        @DisplayName("Should include causation and correlation IDs when provided")
        void shouldIncludeCausationAndCorrelationIds() {
            // Given
            UUID causationId = UUID.randomUUID();
            UUID correlationId = UUID.randomUUID();
            String userId = "user-123";
            String userEmail = "user@test.com";

            when(eventStoreRepository.findLatestVersion(any(), any(), any()))
                    .thenReturn(Optional.empty());
            when(eventStoreRepository.save(any(EventStoreEntry.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            eventStoreService.appendEvent(
                    AGGREGATE_ID, AGGREGATE_TYPE, EVENT_TYPE,
                    Map.of("data", "test"), TENANT_ID,
                    causationId, correlationId, userId, userEmail);

            // Then
            verify(eventStoreRepository).save(entryCaptor.capture());
            EventStoreEntry saved = entryCaptor.getValue();
            assertThat(saved.getCausationId()).isEqualTo(causationId);
            assertThat(saved.getCorrelationId()).isEqualTo(correlationId);
            assertThat(saved.getUserId()).isEqualTo(userId);
            assertThat(saved.getUserEmail()).isEqualTo(userEmail);
        }
    }

    @Nested
    @DisplayName("Event Retrieval Tests")
    class EventRetrievalTests {

        @Test
        @DisplayName("Should retrieve all events for aggregate")
        void shouldRetrieveAllEventsForAggregate() {
            // Given
            List<EventStoreEntry> events = List.of(
                    createEvent(1, "PatientCreatedEvent"),
                    createEvent(2, "PatientUpdatedEvent"),
                    createEvent(3, "PatientUpdatedEvent")
            );
            when(eventStoreRepository.findByAggregateIdAndType(AGGREGATE_ID, AGGREGATE_TYPE, TENANT_ID))
                    .thenReturn(events);

            // When
            List<EventStoreEntry> result = eventStoreService.getEventsForAggregate(
                    AGGREGATE_ID, AGGREGATE_TYPE, TENANT_ID);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getEventVersion()).isEqualTo(1);
            assertThat(result.get(2).getEventVersion()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return empty list when no events exist")
        void shouldReturnEmptyListWhenNoEventsExist() {
            // Given
            when(eventStoreRepository.findByAggregateIdAndType(AGGREGATE_ID, AGGREGATE_TYPE, TENANT_ID))
                    .thenReturn(List.of());

            // When
            List<EventStoreEntry> result = eventStoreService.getEventsForAggregate(
                    AGGREGATE_ID, AGGREGATE_TYPE, TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should retrieve events after specific version")
        void shouldRetrieveEventsAfterSpecificVersion() {
            // Given
            List<EventStoreEntry> events = List.of(
                    createEvent(3, "PatientUpdatedEvent"),
                    createEvent(4, "PatientUpdatedEvent")
            );
            when(eventStoreRepository.findByAggregateIdAfterVersion(AGGREGATE_ID, AGGREGATE_TYPE, 2, TENANT_ID))
                    .thenReturn(events);

            // When
            List<EventStoreEntry> result = eventStoreService.getEventsAfterVersion(
                    AGGREGATE_ID, AGGREGATE_TYPE, 2, TENANT_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getEventVersion()).isGreaterThan(2);
        }

        @Test
        @DisplayName("Should retrieve events by time range")
        void shouldRetrieveEventsByTimeRange() {
            // Given
            Instant startTime = Instant.parse("2026-01-01T00:00:00Z");
            Instant endTime = Instant.parse("2026-01-31T23:59:59Z");
            List<EventStoreEntry> events = List.of(createEvent(1, "SomeEvent"));

            when(eventStoreRepository.findByTimeRange(startTime, endTime, TENANT_ID))
                    .thenReturn(events);

            // When
            List<EventStoreEntry> result = eventStoreService.getEventsByTimeRange(
                    startTime, endTime, TENANT_ID);

            // Then
            assertThat(result).hasSize(1);
            verify(eventStoreRepository).findByTimeRange(startTime, endTime, TENANT_ID);
        }
    }

    @Nested
    @DisplayName("Snapshot Tests")
    class SnapshotTests {

        @Test
        @DisplayName("Should create snapshot with current version")
        void shouldCreateSnapshotWithCurrentVersion() {
            // Given
            Map<String, Object> snapshotData = Map.of(
                    "name", "John Doe",
                    "status", "active"
            );
            when(eventStoreRepository.findLatestVersion(AGGREGATE_ID, AGGREGATE_TYPE, TENANT_ID))
                    .thenReturn(Optional.of(10));
            when(snapshotRepository.save(any(EventSnapshot.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            EventSnapshot result = eventStoreService.createSnapshot(
                    AGGREGATE_ID, AGGREGATE_TYPE, snapshotData, TENANT_ID);

            // Then
            assertThat(result).isNotNull();
            verify(snapshotRepository).save(snapshotCaptor.capture());
            EventSnapshot saved = snapshotCaptor.getValue();
            assertThat(saved.getAggregateId()).isEqualTo(AGGREGATE_ID);
            assertThat(saved.getSnapshotVersion()).isEqualTo(10);
            assertThat(saved.getSnapshotData()).contains("John Doe");
        }

        @Test
        @DisplayName("Should create snapshot with version 0 when no events exist")
        void shouldCreateSnapshotWithVersionZeroWhenNoEvents() {
            // Given
            Map<String, Object> snapshotData = Map.of("initial", "state");
            when(eventStoreRepository.findLatestVersion(AGGREGATE_ID, AGGREGATE_TYPE, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(snapshotRepository.save(any(EventSnapshot.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            eventStoreService.createSnapshot(AGGREGATE_ID, AGGREGATE_TYPE, snapshotData, TENANT_ID);

            // Then
            verify(snapshotRepository).save(snapshotCaptor.capture());
            assertThat(snapshotCaptor.getValue().getSnapshotVersion()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should get latest snapshot")
        void shouldGetLatestSnapshot() {
            // Given
            EventSnapshot snapshot = EventSnapshot.builder()
                    .id(1L)
                    .aggregateId(AGGREGATE_ID)
                    .aggregateType(AGGREGATE_TYPE)
                    .snapshotVersion(15)
                    .snapshotData("{\"name\":\"Test\"}")
                    .tenantId(TENANT_ID)
                    .build();
            when(snapshotRepository.findLatestSnapshot(AGGREGATE_ID, AGGREGATE_TYPE, TENANT_ID))
                    .thenReturn(Optional.of(snapshot));

            // When
            Optional<EventSnapshot> result = eventStoreService.getLatestSnapshot(
                    AGGREGATE_ID, AGGREGATE_TYPE, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getSnapshotVersion()).isEqualTo(15);
        }

        @Test
        @DisplayName("Should return empty when no snapshot exists")
        void shouldReturnEmptyWhenNoSnapshotExists() {
            // Given
            when(snapshotRepository.findLatestSnapshot(AGGREGATE_ID, AGGREGATE_TYPE, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<EventSnapshot> result = eventStoreService.getLatestSnapshot(
                    AGGREGATE_ID, AGGREGATE_TYPE, TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Consumer Status Tests")
    class ConsumerStatusTests {

        @Test
        @DisplayName("Should update existing consumer status")
        void shouldUpdateExistingConsumerStatus() {
            // Given
            String consumerName = "care-gap-processor";
            EventProcessingStatus existingStatus = EventProcessingStatus.builder()
                    .consumerName(consumerName)
                    .tenantId(TENANT_ID)
                    .lastProcessedEventId(100L)
                    .build();

            when(processingStatusRepository.findByConsumerNameAndTenantId(consumerName, TENANT_ID))
                    .thenReturn(Optional.of(existingStatus));
            when(processingStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            eventStoreService.updateConsumerStatus(consumerName, 150L, TENANT_ID);

            // Then
            verify(processingStatusRepository).save(statusCaptor.capture());
            assertThat(statusCaptor.getValue().getLastProcessedEventId()).isEqualTo(150L);
        }

        @Test
        @DisplayName("Should create new consumer status if not exists")
        void shouldCreateNewConsumerStatusIfNotExists() {
            // Given
            String consumerName = "new-consumer";
            when(processingStatusRepository.findByConsumerNameAndTenantId(consumerName, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(processingStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            eventStoreService.updateConsumerStatus(consumerName, 50L, TENANT_ID);

            // Then
            verify(processingStatusRepository).save(statusCaptor.capture());
            EventProcessingStatus saved = statusCaptor.getValue();
            assertThat(saved.getConsumerName()).isEqualTo(consumerName);
            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Should get consumer status")
        void shouldGetConsumerStatus() {
            // Given
            String consumerName = "test-consumer";
            EventProcessingStatus status = EventProcessingStatus.builder()
                    .consumerName(consumerName)
                    .tenantId(TENANT_ID)
                    .lastProcessedEventId(75L)
                    .build();

            when(processingStatusRepository.findByConsumerNameAndTenantId(consumerName, TENANT_ID))
                    .thenReturn(Optional.of(status));

            // When
            Optional<EventProcessingStatus> result = eventStoreService.getConsumerStatus(consumerName, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getLastProcessedEventId()).isEqualTo(75L);
        }
    }

    private EventStoreEntry createEvent(int version, String eventType) {
        return EventStoreEntry.builder()
                .id((long) version)
                .aggregateId(AGGREGATE_ID)
                .aggregateType(AGGREGATE_TYPE)
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .eventVersion(version)
                .payload("{\"test\":\"data\"}")
                .occurredAt(Instant.now())
                .tenantId(TENANT_ID)
                .build();
    }
}
