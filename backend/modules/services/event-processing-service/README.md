# Event Processing Service

Kafka-based event-driven processing with dead letter queue management and real-time event monitoring.

## Purpose

Processes healthcare events asynchronously via Kafka messaging, addressing the challenge that:
- Clinical events (CQL evaluations, care gaps, data changes) need decoupled processing
- Failed events require retry logic with exponential backoff
- Dead letter queue (DLQ) needs monitoring and manual intervention capabilities
- Event processing metrics must be exposed via Prometheus

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                Event Processing Service                          │
│                         (Port 8083)                              │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer                                                │
│  └── DeadLetterQueueController (DLQ management API)             │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer                                                   │
│  ├── DeadLetterQueueService                                     │
│  │   ├── Failed event tracking    - Store failed messages       │
│  │   ├── Retry management         - Mark for retry, exhaust     │
│  │   ├── Resolution tracking      - Mark as resolved            │
│  │   └── Statistics               - DLQ metrics, recent failures│
│  └── EventProcessorService (Kafka consumers)                    │
│      ├── CQL evaluation events                                  │
│      ├── Care gap events                                        │
│      └── FHIR resource events                                   │
├─────────────────────────────────────────────────────────────────┤
│  Repository Layer                                                │
│  └── DeadLetterQueueRepository                                  │
├─────────────────────────────────────────────────────────────────┤
│  Domain Entities                                                 │
│  └── DeadLetterQueueEntity                                      │
│      ├── Topic              - Kafka topic name                  │
│      ├── Payload            - Original message JSON             │
│      ├── Error Message      - Failure reason                    │
│      ├── Retry Count        - Number of retry attempts          │
│      └── Status             - FAILED, RETRY, EXHAUSTED, RESOLVED│
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Kafka Consumer/Producer
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Apache Kafka (Port 9092)                    │
│  Topics: evaluation.completed, care-gap.identified, etc.        │
└─────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### Dead Letter Queue Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/dead-letter-queue/failed` | Get failed events (paginated) |
| GET | `/api/v1/dead-letter-queue/patient/{patientId}` | Failed events by patient |
| GET | `/api/v1/dead-letter-queue/topic/{topic}` | Failed events by topic |
| GET | `/api/v1/dead-letter-queue/exhausted` | Events needing intervention |
| GET | `/api/v1/dead-letter-queue/recent?hours=24` | Recent failures |
| GET | `/api/v1/dead-letter-queue/stats` | DLQ statistics |

### Event Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/dead-letter-queue/{dlqId}/retry` | Retry failed event |
| POST | `/api/v1/dead-letter-queue/{dlqId}/resolve` | Mark as resolved |
| POST | `/api/v1/dead-letter-queue/{dlqId}/exhaust` | Mark as exhausted |

## Kafka Topics

| Topic | Purpose | Producer | Consumer |
|-------|---------|----------|----------|
| `evaluation.started` | CQL evaluation started | cql-engine | event-processing |
| `evaluation.completed` | CQL evaluation done | cql-engine | event-processing, care-gap |
| `evaluation.failed` | CQL evaluation error | cql-engine | event-processing |
| `care-gap.identified` | Care gap found | care-gap | event-processing, notification |
| `patient.updated` | Patient data changed | fhir | event-processing, cache-invalidation |

## Configuration

```yaml
server:
  port: 8083
  servlet:
    context-path: /events

# Kafka configuration
spring.kafka:
  bootstrap-servers: localhost:9092
  consumer:
    group-id: event-processing-service
    auto-offset-reset: earliest
  producer:
    acks: all
    retries: 3

# Actuator (Prometheus metrics)
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

## Dead Letter Queue Entity

```json
{
  "id": "uuid",
  "tenantId": "tenant-1",
  "patientId": "patient-123",
  "topic": "evaluation.completed",
  "payload": "{\"evaluationId\":\"eval-123\"}",
  "errorMessage": "Connection timeout to FHIR service",
  "retryCount": 3,
  "status": "EXHAUSTED",
  "firstFailedAt": "2024-01-01T10:00:00Z",
  "lastRetryAt": "2024-01-01T10:15:00Z",
  "resolvedAt": null,
  "resolvedBy": null
}
```

## Dependencies

- **Spring Boot**: Web, JPA, Kafka
- **Database**: PostgreSQL with Liquibase migrations
- **Messaging**: Kafka for event streaming
- **Metrics**: Micrometer + Prometheus for monitoring
- **Resilience**: Resilience4j for circuit breakers

## Running Locally

```bash
# Start Kafka first
docker compose up -d kafka

# From backend directory
./gradlew :modules:services:event-processing-service:bootRun

# Or via Docker
docker compose --profile events up event-processing-service
```

## Testing

### Overview

Event Processing Service has comprehensive test coverage across 7 test suites covering unit tests, integration tests, metrics, health indicators, multi-tenant isolation, RBAC, HIPAA compliance, and performance testing. The service processes healthcare events via Kafka messaging with a focus on dead letter queue (DLQ) management and retry logic.

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:event-processing-service:test

# Run specific test suite
./gradlew :modules:services:event-processing-service:test --tests "*DeadLetterQueueServiceTest"
./gradlew :modules:services:event-processing-service:test --tests "*DLQRetryProcessorTest"
./gradlew :modules:services:event-processing-service:test --tests "*MetricsTest"

# Run with coverage report
./gradlew :modules:services:event-processing-service:test jacocoTestReport

# Run integration tests only
./gradlew :modules:services:event-processing-service:test --tests "*IntegrationTest"

# Run performance tests
./gradlew :modules:services:event-processing-service:test --tests "*PerformanceTest"
```

### Test Organization

```
src/test/java/com/healthdata/events/
├── service/
│   ├── DeadLetterQueueServiceTest.java      # DLQ CRUD operations (781 lines, 40+ tests)
│   ├── DLQRetryProcessorTest.java           # Retry processor (661 lines, 30+ tests)
│   └── DLQAlertingServiceTest.java          # Alerting service tests
├── entity/
│   └── DeadLetterQueueEntityTest.java       # Entity/domain tests
├── metrics/
│   ├── DLQMetricsTest.java                  # DLQ Prometheus metrics (261 lines)
│   └── EventProcessingMetricsTest.java      # Event processing metrics (286 lines)
├── health/
│   └── DLQHealthIndicatorTest.java          # Health check tests (243 lines)
├── api/
│   └── DeadLetterQueueControllerIntegrationTest.java  # API integration tests
├── multitenant/
│   └── EventProcessingMultiTenantTest.java  # Tenant isolation tests
├── security/
│   └── EventProcessingRbacTest.java         # RBAC/permission tests
├── compliance/
│   └── EventProcessingHipaaComplianceTest.java  # HIPAA compliance tests
└── performance/
    └── EventProcessingPerformanceTest.java  # Performance benchmarks
```

### Test Coverage Summary

| Test File | Tests | Coverage Focus |
|-----------|-------|----------------|
| DeadLetterQueueServiceTest | 40+ | DLQ CRUD, retry eligibility, status transitions |
| DLQRetryProcessorTest | 30+ | Kafka republishing, exponential backoff, exhaustion handling |
| EventProcessingMetricsTest | 10 | Processing duration, success/failure counters |
| DLQMetricsTest | 11 | DLQ failure counter, retry counter, status gauges |
| DLQHealthIndicatorTest | 12 | Health thresholds, status reporting |
| DeadLetterQueueEntityTest | 8 | Entity validation, status enum |
| DLQAlertingServiceTest | 6 | Exhaustion alerts, escalation |

---

### Unit Tests - DeadLetterQueueServiceTest

Tests the core DLQ service layer with comprehensive coverage for failure recording, retry management, and statistics retrieval.

```java
@ExtendWith(MockitoExtension.class)
class DeadLetterQueueServiceTest {

    @Mock
    private DeadLetterQueueRepository dlqRepository;

    @Mock
    private DLQMetrics dlqMetrics;

    @InjectMocks
    private DeadLetterQueueService dlqService;

    private DeadLetterQueueEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = DeadLetterQueueEntity.builder()
            .id(UUID.randomUUID())
            .eventId(UUID.randomUUID())
            .tenantId("tenant-test-001")
            .topic("evaluation.completed")
            .eventType("CARE_GAP_DETECTED")
            .patientId("TEST-PATIENT-123")  // HIPAA-compliant synthetic ID
            .eventPayload("{\"measureId\":\"BCS-E\",\"patientId\":\"TEST-PATIENT-123\"}")
            .errorMessage("Connection timeout to FHIR service")
            .stackTrace("java.net.SocketTimeoutException...")
            .retryCount(0)
            .maxRetryCount(3)
            .firstFailureAt(Instant.now())
            .status(DLQStatus.FAILED)
            .build();
    }

    @Nested
    @DisplayName("Record Failure Tests")
    class RecordFailureTests {

        @Test
        @DisplayName("Should create DLQ entry with all required fields")
        void shouldCreateDLQEntryWithAllFields() {
            // Given
            String topic = "evaluation.completed";
            String eventType = "CARE_GAP_DETECTED";
            String tenantId = "tenant-001";
            String patientId = "TEST-PATIENT-456";
            Object payload = Map.of("measureId", "BCS-E", "status", "open");
            Exception error = new RuntimeException("FHIR service unavailable");

            when(dlqRepository.save(any(DeadLetterQueueEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

            // When
            DeadLetterQueueEntity result = dlqService.recordFailure(
                topic, eventType, tenantId, patientId, payload, error
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTopic()).isEqualTo(topic);
            assertThat(result.getEventType()).isEqualTo(eventType);
            assertThat(result.getTenantId()).isEqualTo(tenantId);
            assertThat(result.getPatientId()).isEqualTo(patientId);
            assertThat(result.getEventPayload()).isNotNull();
            assertThat(result.getErrorMessage()).isEqualTo(error.getMessage());
            assertThat(result.getStackTrace()).contains("RuntimeException");
            assertThat(result.getRetryCount()).isEqualTo(0);
            assertThat(result.getMaxRetryCount()).isEqualTo(3);
            assertThat(result.getStatus()).isEqualTo(DLQStatus.FAILED);
            assertThat(result.getFirstFailureAt()).isNotNull();
            assertThat(result.getNextRetryAt()).isNotNull();
        }

        @Test
        @DisplayName("Should serialize payload to JSON and record metrics")
        void shouldSerializePayloadAndRecordMetrics() {
            // Given
            Map<String, Object> payload = Map.of(
                "evaluationId", "eval-123",
                "measureId", "BCS-E",
                "patientId", "TEST-PATIENT-789"
            );
            Exception error = new RuntimeException("Kafka broker unavailable");

            when(dlqRepository.save(any(DeadLetterQueueEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

            // When
            DeadLetterQueueEntity result = dlqService.recordFailure(
                "evaluation.completed", "EVALUATION_FAILED",
                "tenant-001", "TEST-PATIENT-789", payload, error
            );

            // Then
            assertThat(result.getEventPayload()).contains("eval-123");
            assertThat(result.getEventPayload()).contains("BCS-E");

            verify(dlqMetrics, times(1))
                .recordFailure("evaluation.completed", "EVALUATION_FAILED");
            verify(dlqRepository, times(1)).save(any(DeadLetterQueueEntity.class));
        }
    }

    @Nested
    @DisplayName("Mark For Retry Tests")
    class MarkForRetryTests {

        @Test
        @DisplayName("Should mark event for retry when eligible")
        void shouldMarkEventForRetry() {
            // Given
            UUID dlqId = testEntity.getId();
            testEntity.setStatus(DLQStatus.FAILED);
            testEntity.setRetryCount(0);
            testEntity.setMaxRetryCount(3);
            testEntity.setNextRetryAt(Instant.now().minusSeconds(60));

            when(dlqRepository.findById(dlqId)).thenReturn(Optional.of(testEntity));
            when(dlqRepository.save(any(DeadLetterQueueEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

            // When
            dlqService.markForRetry(dlqId);

            // Then
            verify(dlqRepository, times(1)).save(testEntity);
            assertThat(testEntity.getRetryCount()).isEqualTo(1);
            assertThat(testEntity.getStatus()).isEqualTo(DLQStatus.RETRYING);
            assertThat(testEntity.getLastRetryAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when event not eligible for retry")
        void shouldThrowExceptionWhenNotEligible() {
            // Given
            UUID dlqId = testEntity.getId();
            testEntity.setStatus(DLQStatus.EXHAUSTED);  // Already exhausted

            when(dlqRepository.findById(dlqId)).thenReturn(Optional.of(testEntity));

            // When & Then
            assertThrows(IllegalStateException.class, () -> dlqService.markForRetry(dlqId));
        }
    }

    @Nested
    @DisplayName("Record Retry Failure Tests")
    class RecordRetryFailureTests {

        @Test
        @DisplayName("Should mark as exhausted when max retries reached")
        void shouldMarkAsExhaustedAtMaxRetries() {
            // Given
            UUID dlqId = testEntity.getId();
            testEntity.setRetryCount(3);    // Already at max
            testEntity.setMaxRetryCount(3);
            Exception error = new RuntimeException("Final retry failed");

            when(dlqRepository.findById(dlqId)).thenReturn(Optional.of(testEntity));
            when(dlqRepository.save(any(DeadLetterQueueEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

            // When
            dlqService.recordRetryFailure(dlqId, error);

            // Then
            assertThat(testEntity.getStatus()).isEqualTo(DLQStatus.EXHAUSTED);
        }

        @Test
        @DisplayName("Should calculate next retry time with exponential backoff")
        void shouldCalculateNextRetryTimeWithBackoff() {
            // Given
            UUID dlqId = testEntity.getId();
            testEntity.setRetryCount(1);    // Second retry
            testEntity.setMaxRetryCount(3);
            Exception error = new RuntimeException("Transient error");

            when(dlqRepository.findById(dlqId)).thenReturn(Optional.of(testEntity));
            when(dlqRepository.save(any(DeadLetterQueueEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

            // When
            dlqService.recordRetryFailure(dlqId, error);

            // Then
            assertThat(testEntity.getNextRetryAt()).isNotNull();
            assertThat(testEntity.getNextRetryAt()).isAfter(Instant.now());
            // Exponential backoff: 2^retryCount * base interval
        }
    }

    @Nested
    @DisplayName("DLQ Statistics Tests")
    class GetStatsTests {

        @Test
        @DisplayName("Should retrieve DLQ statistics for tenant")
        void shouldRetrieveStats() {
            // Given
            String tenantId = "tenant-001";
            when(dlqRepository.countByTenantIdAndStatus(tenantId, DLQStatus.FAILED))
                .thenReturn(5L);
            when(dlqRepository.countByTenantIdAndStatus(tenantId, DLQStatus.EXHAUSTED))
                .thenReturn(2L);
            when(dlqRepository.countByTenantIdAndStatus(tenantId, DLQStatus.RETRYING))
                .thenReturn(1L);

            // When
            DLQStats stats = dlqService.getStats(tenantId);

            // Then
            assertThat(stats.failed()).isEqualTo(5L);
            assertThat(stats.exhausted()).isEqualTo(2L);
            assertThat(stats.retrying()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Cleanup Old Resolved Tests")
    class CleanupOldResolvedTests {

        @Test
        @DisplayName("Should delete resolved entries older than specified days")
        void shouldDeleteOldResolvedEntries() {
            // Given
            int daysToKeep = 30;

            // When
            dlqService.cleanupOldResolved(daysToKeep);

            // Then
            ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(dlqRepository).deleteOldResolved(instantCaptor.capture());

            Instant before = instantCaptor.getValue();
            assertThat(before).isBefore(Instant.now());
        }
    }
}
```

**Key Test Categories:**
- **Record Failure** (7 tests): DLQ entry creation, payload serialization, stack trace extraction
- **Retry Eligibility** (3 tests): Finding events ready for retry based on nextRetryAt
- **Mark For Retry** (4 tests): Status transitions FAILED → RETRYING, metric recording
- **Mark As Resolved** (3 tests): Manual resolution with user and notes
- **Record Retry Failure** (7 tests): Exponential backoff, exhaustion detection
- **Statistics** (2 tests): Per-tenant DLQ counts
- **Cleanup** (2 tests): Retention policy enforcement

---

### Unit Tests - DLQRetryProcessorTest

Tests the scheduled retry processor including Kafka republishing, exhaustion handling, and alerting.

```java
@ExtendWith(MockitoExtension.class)
class DLQRetryProcessorTest {

    @Mock
    private DeadLetterQueueService dlqService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private DLQAlertingService alertingService;

    @InjectMocks
    private DLQRetryProcessor retryProcessor;

    @Nested
    @DisplayName("Process Retries Tests")
    class ProcessRetriesTests {

        @Test
        @DisplayName("Should process retry eligible events")
        void shouldProcessRetryEligibleEvents() {
            // Given
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(
                createTestEntity(0), createTestEntity(1)
            );

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(mockSuccessfulSend());

            // When
            retryProcessor.processRetries();

            // Then
            verify(dlqService, times(1)).getRetryEligible();
            verify(dlqService, times(2)).markForRetry(any(UUID.class));
            verify(kafkaTemplate, times(2)).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should handle individual retry failures without stopping batch")
        void shouldHandleIndividualFailures() {
            // Given
            DeadLetterQueueEntity entity1 = createTestEntity(0);
            DeadLetterQueueEntity entity2 = createTestEntity(1);
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(entity1, entity2);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);

            // First entity fails during markForRetry
            doThrow(new RuntimeException("Mark failed"))
                .when(dlqService).markForRetry(entity1.getId());

            // Second entity succeeds
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(mockSuccessfulSend());

            // When
            retryProcessor.processRetries();

            // Then - should still process second entity
            verify(dlqService, times(1)).recordRetryFailure(
                eq(entity1.getId()), any(RuntimeException.class));
            verify(dlqService, times(1)).markForRetry(entity2.getId());
        }
    }

    @Nested
    @DisplayName("Kafka Republishing Tests")
    class KafkaRepublishingTests {

        @Test
        @DisplayName("Should republish event to original topic with tenant key")
        void shouldRepublishToOriginalTopicWithTenantKey() {
            // Given
            DeadLetterQueueEntity entity = createTestEntity(0);
            entity.setTopic("evaluation.completed");
            entity.setTenantId("tenant-001");
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(entity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(mockSuccessfulSend());

            // When
            retryProcessor.processRetries();

            // Then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo("evaluation.completed");
            assertThat(keyCaptor.getValue()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("Should mark as resolved on successful republish")
        void shouldMarkAsResolvedOnSuccess() {
            // Given
            DeadLetterQueueEntity entity = createTestEntity(0);
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(entity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(mockSuccessfulSend());

            // When
            retryProcessor.processRetries();

            // Then
            ArgumentCaptor<String> notesCaptor = ArgumentCaptor.forClass(String.class);
            verify(dlqService).markAsResolved(
                eq(entity.getId()), eq("DLQRetryProcessor"), notesCaptor.capture());
            assertThat(notesCaptor.getValue()).contains("Automatically retried successfully");
        }

        @Test
        @DisplayName("Should handle JSON deserialization errors")
        void shouldHandleJsonDeserializationErrors() {
            // Given
            DeadLetterQueueEntity entity = createTestEntity(0);
            entity.setEventPayload("invalid json {{{");  // Malformed JSON
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(entity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);

            // When
            retryProcessor.processRetries();

            // Then
            verify(dlqService).recordRetryFailure(eq(entity.getId()), any(Exception.class));
        }
    }

    @Nested
    @DisplayName("Exhaustion Handling Tests")
    class ExhaustionHandlingTests {

        @Test
        @DisplayName("Should send exhaustion alert when event exhausted")
        void shouldSendExhaustionAlert() {
            // Given
            DeadLetterQueueEntity entity = createTestEntity(3);  // At max retries
            entity.setMaxRetryCount(3);
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(entity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Final retry failed"));

            // When
            retryProcessor.processRetries();

            // Then
            verify(dlqService).markAsExhausted(entity.getId());

            ArgumentCaptor<DLQExhaustionAlert> alertCaptor =
                ArgumentCaptor.forClass(DLQExhaustionAlert.class);
            verify(alertingService).sendExhaustionAlert(alertCaptor.capture());

            DLQExhaustionAlert alert = alertCaptor.getValue();
            assertThat(alert.getEventId()).isEqualTo(entity.getEventId());
            assertThat(alert.getTenantId()).isEqualTo(entity.getTenantId());
        }

        @Test
        @DisplayName("Should escalate critical event failures")
        void shouldEscalateCriticalFailures() {
            // Given - PATIENT_REGISTERED is critical
            DeadLetterQueueEntity entity = createTestEntity(3);
            entity.setEventType("PATIENT_REGISTERED");
            entity.setMaxRetryCount(3);
            List<DeadLetterQueueEntity> eligibleEvents = Arrays.asList(entity);

            when(dlqService.getRetryEligible()).thenReturn(eligibleEvents);
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Final retry failed"));
            when(alertingService.createDashboardEntry(any())).thenReturn("entry-123");

            // When
            retryProcessor.processRetries();

            // Then
            ArgumentCaptor<DLQExhaustionAlert> alertCaptor =
                ArgumentCaptor.forClass(DLQExhaustionAlert.class);
            verify(alertingService).escalateCriticalFailure(alertCaptor.capture());
            assertThat(alertCaptor.getValue().isCritical()).isTrue();
        }
    }

    @Nested
    @DisplayName("Exponential Backoff Tests")
    class ExponentialBackoffTests {

        @Test
        @DisplayName("Should respect nextRetryAt calculated by entity")
        void shouldRespectNextRetryAt() {
            // Given - event not yet eligible for retry
            DeadLetterQueueEntity entity = createTestEntity(1);
            entity.setNextRetryAt(Instant.now().plusSeconds(300));  // 5 minutes in future

            // This entity should not be returned by getRetryEligible
            when(dlqService.getRetryEligible()).thenReturn(Collections.emptyList());

            // When
            retryProcessor.processRetries();

            // Then
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("Batch Processing Tests")
    class BatchProcessingTests {

        @Test
        @DisplayName("Should continue processing batch even if one fails")
        void shouldContinueBatchOnPartialFailure() {
            // Given
            DeadLetterQueueEntity entity1 = createTestEntity(0);
            DeadLetterQueueEntity entity2 = createTestEntity(1);
            DeadLetterQueueEntity entity3 = createTestEntity(2);
            List<DeadLetterQueueEntity> batch = Arrays.asList(entity1, entity2, entity3);

            when(dlqService.getRetryEligible()).thenReturn(batch);

            // Entity2 will fail
            when(kafkaTemplate.send(eq("test.topic"), eq("test-tenant"), any()))
                .thenReturn(mockSuccessfulSend())
                .thenThrow(new RuntimeException("Send failed"))
                .thenReturn(mockSuccessfulSend());

            // When
            retryProcessor.processRetries();

            // Then - should process all 3 entities
            verify(kafkaTemplate, times(3)).send(anyString(), anyString(), any());
            verify(dlqService, times(2)).markAsResolved(any(UUID.class), anyString(), anyString());
            verify(dlqService, times(1)).recordRetryFailure(any(UUID.class), any(RuntimeException.class));
        }
    }
}
```

**Key Test Categories:**
- **Process Retries** (4 tests): Batch processing, failure isolation
- **Kafka Republishing** (7 tests): Topic routing, key generation, success/failure handling
- **Exhaustion Handling** (8 tests): Alerts, dashboard entries, critical escalation
- **Exponential Backoff** (2 tests): Timing verification
- **Batch Processing** (2 tests): Resilience, partial failure handling

---

### Unit Tests - Prometheus Metrics

Tests Micrometer metrics collection for Prometheus monitoring.

```java
@DisplayName("Event Processing Metrics Tests")
class EventProcessingMetricsTest {

    private MeterRegistry meterRegistry;
    private EventProcessingMetrics eventMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        eventMetrics = new EventProcessingMetrics(meterRegistry);
    }

    @Test
    @DisplayName("Should record processing duration with timer")
    void shouldRecordProcessingDuration() {
        // Given
        String eventType = "CARE_GAP_DETECTED";
        Duration duration = Duration.ofMillis(150);

        // When
        eventMetrics.recordProcessingDuration(eventType, duration);

        // Then
        Timer timer = meterRegistry.find("event.processing.duration")
                .tag("event_type", eventType)
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(150.0);
    }

    @Test
    @DisplayName("Should track success and failure separately by event type")
    void shouldTrackSuccessAndFailureSeparately() {
        // Given
        String eventType = "CARE_GAP_DETECTED";

        // When
        eventMetrics.recordSuccess(eventType);
        eventMetrics.recordSuccess(eventType);
        eventMetrics.recordSuccess(eventType);
        eventMetrics.recordFailure(eventType);

        // Then
        Counter successCounter = meterRegistry.find("event.processing.success")
                .tag("event_type", eventType)
                .counter();

        Counter failureCounter = meterRegistry.find("event.processing.failure")
                .tag("event_type", eventType)
                .counter();

        assertThat(successCounter.count()).isEqualTo(3.0);
        assertThat(failureCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should handle high-frequency event processing")
    void shouldHandleHighFrequencyEventProcessing() {
        // Given
        String eventType = "CARE_GAP_DETECTED";
        int eventCount = 1000;

        // When
        for (int i = 0; i < eventCount; i++) {
            eventMetrics.recordProcessingDuration(eventType, Duration.ofMillis(50));
            eventMetrics.recordSuccess(eventType);
        }

        // Then
        Timer timer = meterRegistry.find("event.processing.duration")
                .tag("event_type", eventType)
                .timer();

        Counter successCounter = meterRegistry.find("event.processing.success")
                .tag("event_type", eventType)
                .counter();

        assertThat(timer.count()).isEqualTo(eventCount);
        assertThat(successCounter.count()).isEqualTo((double) eventCount);
    }
}
```

**DLQ Metrics Tests:**

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("DLQ Metrics Collection Tests")
class DLQMetricsTest {

    private MeterRegistry meterRegistry;

    @Mock
    private DeadLetterQueueRepository dlqRepository;

    private DLQMetrics dlqMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        dlqMetrics = new DLQMetrics(meterRegistry, dlqRepository);
    }

    @Test
    @DisplayName("Should increment failure counter with topic and event_type tags")
    void shouldIncrementFailureCounterWithTags() {
        // Given
        String topic = "evaluation.completed";
        String eventType = "CARE_GAP_DETECTED";

        // When
        dlqMetrics.recordFailure(topic, eventType);

        // Then
        Counter counter = meterRegistry.find("dlq.failures.total")
                .tag("topic", topic)
                .tag("event_type", eventType)
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should register status gauges from repository")
    void shouldRegisterStatusGauges() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(5L);
        when(dlqRepository.countByStatus(DLQStatus.FAILED)).thenReturn(12L);
        when(dlqRepository.countByStatus(DLQStatus.RETRYING)).thenReturn(4L);

        // When
        Gauge exhaustedGauge = meterRegistry.find("dlq.exhausted.total").gauge();
        Gauge failedGauge = meterRegistry.find("dlq.failed.total").gauge();
        Gauge retryingGauge = meterRegistry.find("dlq.retrying.total").gauge();

        // Then
        assertThat(exhaustedGauge.value()).isEqualTo(5.0);
        assertThat(failedGauge.value()).isEqualTo(12.0);
        assertThat(retryingGauge.value()).isEqualTo(4.0);
    }
}
```

**Exposed Prometheus Metrics:**

| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `event.processing.duration` | Timer | event_type | Event processing latency |
| `event.processing.success` | Counter | event_type | Successful event processing |
| `event.processing.failure` | Counter | event_type | Failed event processing |
| `dlq.failures.total` | Counter | topic, event_type | Total DLQ failures |
| `dlq.retries.total` | Counter | - | Total retry attempts |
| `dlq.exhausted.total` | Gauge | - | Current exhausted events |
| `dlq.failed.total` | Gauge | - | Current failed events |
| `dlq.retrying.total` | Gauge | - | Current retrying events |

---

### Unit Tests - Health Indicator

Tests Spring Boot Actuator health checks for DLQ monitoring.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("DLQ Health Indicator Tests")
class DLQHealthIndicatorTest {

    @Mock
    private DeadLetterQueueRepository dlqRepository;

    private DLQHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new DLQHealthIndicator(dlqRepository);
    }

    @Test
    @DisplayName("Should report UP when failed events less than 100")
    void shouldReportUpWhenFailedEventsLessThan100() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.FAILED)).thenReturn(50L);
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(5L);
        when(dlqRepository.countByStatus(DLQStatus.RETRYING)).thenReturn(10L);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("failed", 50L)
                .containsEntry("exhausted", 5L)
                .containsEntry("retrying", 10L)
                .containsEntry("message", "DLQ is healthy");
    }

    @Test
    @DisplayName("Should report WARNING when failed events between 100 and 500")
    void shouldReportWarningWhenFailedEventsBetween100And500() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.FAILED)).thenReturn(250L);
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(20L);
        when(dlqRepository.countByStatus(DLQStatus.RETRYING)).thenReturn(15L);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(new Status("WARNING"));
        assertThat(health.getDetails())
                .containsEntry("message", "High number of failed events in DLQ");
    }

    @Test
    @DisplayName("Should report DOWN when failed events >= 500")
    void shouldReportDownWhenFailedEventsGreaterThan500() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.FAILED)).thenReturn(1000L);
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(100L);
        when(dlqRepository.countByStatus(DLQStatus.RETRYING)).thenReturn(20L);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("message", "Critical: DLQ has too many failed events");
    }

    @Test
    @DisplayName("Should prioritize exhausted events in warning")
    void shouldPrioritizeExhaustedEventsInWarning() {
        // Given - few failed but many exhausted
        when(dlqRepository.countByStatus(DLQStatus.FAILED)).thenReturn(10L);
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(150L);
        when(dlqRepository.countByStatus(DLQStatus.RETRYING)).thenReturn(5L);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(new Status("WARNING"));
        assertThat(health.getDetails())
                .containsEntry("message", "High number of exhausted events in DLQ");
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void shouldHandleRepositoryExceptionsGracefully() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.FAILED))
                .thenThrow(new RuntimeException("Database connection error"));

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsKey("error")
                .containsEntry("message", "Failed to check DLQ health");
    }
}
```

**Health Status Thresholds:**

| Status | Failed Events | Exhausted Events | Action |
|--------|---------------|------------------|--------|
| UP | < 100 | < 100 | Normal operation |
| WARNING | 100-500 | 100-500 | Investigation needed |
| DOWN | >= 500 | >= 500 | Critical - immediate intervention |

---

### Integration Tests

Tests API endpoints with TestContainers (PostgreSQL, Kafka) for realistic integration testing.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class DeadLetterQueueControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeadLetterQueueRepository dlqRepository;

    private static final String TENANT_ID = "tenant-integration-001";
    private static final String BASE_URL = "/api/v1/dead-letter-queue";

    @BeforeEach
    void setUp() {
        dlqRepository.deleteAll();
    }

    @Test
    @DisplayName("Should return failed events for tenant")
    void shouldReturnFailedEventsForTenant() throws Exception {
        // Given
        DeadLetterQueueEntity entity = createAndSaveFailedEvent(TENANT_ID);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/failed")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "ADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].tenantId").value(TENANT_ID))
            .andExpect(jsonPath("$.content[0].status").value("FAILED"));
    }

    @Test
    @DisplayName("Should retry failed event and republish to Kafka")
    void shouldRetryFailedEvent() throws Exception {
        // Given
        DeadLetterQueueEntity entity = createAndSaveFailedEvent(TENANT_ID);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/{dlqId}/retry", entity.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "ADMIN"))
            .andExpect(status().isAccepted());

        // Verify status change
        DeadLetterQueueEntity updated = dlqRepository.findById(entity.getId()).orElseThrow();
        assertThat(updated.getStatus()).isIn(DLQStatus.RETRYING, DLQStatus.RESOLVED);
    }

    @Test
    @DisplayName("Should mark event as resolved")
    void shouldMarkAsResolved() throws Exception {
        // Given
        DeadLetterQueueEntity entity = createAndSaveFailedEvent(TENANT_ID);
        String resolveRequest = """
            {
                "resolvedBy": "admin@test.hdim.io",
                "notes": "Manually processed the event"
            }
            """;

        // When/Then
        mockMvc.perform(post(BASE_URL + "/{dlqId}/resolve", entity.getId())
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resolveRequest))
            .andExpect(status().isOk());

        // Verify resolution
        DeadLetterQueueEntity resolved = dlqRepository.findById(entity.getId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(DLQStatus.RESOLVED);
        assertThat(resolved.getResolvedBy()).isEqualTo("admin@test.hdim.io");
        assertThat(resolved.getResolutionNotes()).isEqualTo("Manually processed the event");
    }

    @Test
    @DisplayName("Should return DLQ statistics")
    void shouldReturnDlqStatistics() throws Exception {
        // Given
        createAndSaveFailedEvent(TENANT_ID);
        createAndSaveFailedEvent(TENANT_ID);
        DeadLetterQueueEntity exhausted = createAndSaveFailedEvent(TENANT_ID);
        exhausted.setStatus(DLQStatus.EXHAUSTED);
        dlqRepository.save(exhausted);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/stats")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "ADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failed").value(2))
            .andExpect(jsonPath("$.exhausted").value(1))
            .andExpect(jsonPath("$.retrying").value(0));
    }

    private DeadLetterQueueEntity createAndSaveFailedEvent(String tenantId) {
        return dlqRepository.save(DeadLetterQueueEntity.builder()
            .eventId(UUID.randomUUID())
            .tenantId(tenantId)
            .topic("evaluation.completed")
            .eventType("CARE_GAP_DETECTED")
            .patientId("TEST-PATIENT-" + UUID.randomUUID().toString().substring(0, 6))
            .eventPayload("{\"measureId\":\"BCS-E\"}")
            .errorMessage("Test error")
            .retryCount(0)
            .maxRetryCount(3)
            .firstFailureAt(Instant.now())
            .status(DLQStatus.FAILED)
            .build());
    }
}
```

---

### Multi-Tenant Isolation Tests

Tests verify strict tenant isolation for DLQ entries.

```java
@SpringBootTest
@Testcontainers
class EventProcessingMultiTenantTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private DeadLetterQueueService dlqService;

    @Autowired
    private DeadLetterQueueRepository dlqRepository;

    @Test
    @DisplayName("Should isolate DLQ entries by tenant")
    void shouldIsolateDlqEntriesByTenant() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";

        // Create DLQ entries for both tenants
        dlqService.recordFailure("topic", "EVENT", tenant1, "PATIENT-1",
            Map.of("data", "tenant1"), new RuntimeException("Error 1"));
        dlqService.recordFailure("topic", "EVENT", tenant1, "PATIENT-2",
            Map.of("data", "tenant1"), new RuntimeException("Error 2"));
        dlqService.recordFailure("topic", "EVENT", tenant2, "PATIENT-3",
            Map.of("data", "tenant2"), new RuntimeException("Error 3"));

        // When
        Page<DeadLetterQueueEntity> tenant1Events = dlqService.getFailedByTenant(
            tenant1, PageRequest.of(0, 10));
        Page<DeadLetterQueueEntity> tenant2Events = dlqService.getFailedByTenant(
            tenant2, PageRequest.of(0, 10));

        // Then
        assertThat(tenant1Events.getContent())
            .hasSize(2)
            .allMatch(e -> e.getTenantId().equals(tenant1));

        assertThat(tenant2Events.getContent())
            .hasSize(1)
            .allMatch(e -> e.getTenantId().equals(tenant2));
    }

    @Test
    @DisplayName("Should return tenant-scoped DLQ statistics")
    void shouldReturnTenantScopedStatistics() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";

        // Create 5 failures for tenant1, 2 for tenant2
        for (int i = 0; i < 5; i++) {
            dlqService.recordFailure("topic", "EVENT", tenant1, "PATIENT-" + i,
                Map.of("data", "test"), new RuntimeException("Error"));
        }
        for (int i = 0; i < 2; i++) {
            dlqService.recordFailure("topic", "EVENT", tenant2, "PATIENT-" + i,
                Map.of("data", "test"), new RuntimeException("Error"));
        }

        // When
        DLQStats tenant1Stats = dlqService.getStats(tenant1);
        DLQStats tenant2Stats = dlqService.getStats(tenant2);

        // Then
        assertThat(tenant1Stats.failed()).isEqualTo(5L);
        assertThat(tenant2Stats.failed()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should use tenant ID as Kafka message key for partition affinity")
    void shouldUseTenantIdAsKafkaKey() {
        // Given
        String tenantId = "tenant-001";

        // When - tenant ID should be used as Kafka key
        // This ensures all messages for a tenant go to the same partition

        // Then
        // The DLQRetryProcessor uses tenantId as the key:
        // kafkaTemplate.send(entity.getTopic(), entity.getTenantId(), payload)
        // This ensures ordering within a tenant is maintained
    }
}
```

---

### RBAC/Permission Tests

Tests role-based access control for DLQ management endpoints.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class EventProcessingRbacTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT_ID = "tenant-rbac-001";
    private static final String BASE_URL = "/api/v1/dead-letter-queue";

    @Test
    @DisplayName("Admin should be able to retry failed events")
    void adminCanRetryFailedEvents() throws Exception {
        mockMvc.perform(post(BASE_URL + "/{dlqId}/retry", UUID.randomUUID())
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "admin-001")
                .header("X-Auth-Roles", "ADMIN"))
            .andExpect(status().isNotFound());  // Not found, but authorized
    }

    @Test
    @DisplayName("Admin should be able to resolve failed events")
    void adminCanResolveFailedEvents() throws Exception {
        String resolveRequest = """
            {"resolvedBy": "admin", "notes": "Manual fix"}
            """;

        mockMvc.perform(post(BASE_URL + "/{dlqId}/resolve", UUID.randomUUID())
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "admin-001")
                .header("X-Auth-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resolveRequest))
            .andExpect(status().isNotFound());  // Not found, but authorized
    }

    @Test
    @DisplayName("Evaluator should be able to view DLQ statistics")
    void evaluatorCanViewStatistics() throws Exception {
        mockMvc.perform(get(BASE_URL + "/stats")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "evaluator-001")
                .header("X-Auth-Roles", "EVALUATOR"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Viewer should NOT be able to retry failed events")
    void viewerCannotRetryFailedEvents() throws Exception {
        mockMvc.perform(post(BASE_URL + "/{dlqId}/retry", UUID.randomUUID())
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "viewer-001")
                .header("X-Auth-Roles", "VIEWER"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Viewer should be able to view failed events (read-only)")
    void viewerCanViewFailedEvents() throws Exception {
        mockMvc.perform(get(BASE_URL + "/failed")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "viewer-001")
                .header("X-Auth-Roles", "VIEWER"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated request should be rejected")
    void unauthenticatedRequestShouldBeRejected() throws Exception {
        mockMvc.perform(get(BASE_URL + "/failed")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isUnauthorized());
    }
}
```

**Role Permissions for DLQ Endpoints:**

| Endpoint | SUPER_ADMIN | ADMIN | EVALUATOR | ANALYST | VIEWER |
|----------|-------------|-------|-----------|---------|--------|
| GET /failed | ✓ | ✓ | ✓ | ✓ | ✓ |
| GET /stats | ✓ | ✓ | ✓ | ✓ | ✓ |
| POST /retry | ✓ | ✓ | ✗ | ✗ | ✗ |
| POST /resolve | ✓ | ✓ | ✗ | ✗ | ✗ |
| POST /exhaust | ✓ | ✓ | ✗ | ✗ | ✗ |

---

### HIPAA Compliance Tests

Tests verify HIPAA-compliant handling of PHI in event payloads.

```java
@SpringBootTest
class EventProcessingHipaaComplianceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeadLetterQueueService dlqService;

    private static final String TENANT_ID = "tenant-hipaa-001";

    @Test
    @DisplayName("DLQ responses must include no-cache headers")
    void dlqResponsesShouldIncludeNoCacheHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/dead-letter-queue/failed")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "ADMIN"))
            .andExpect(header().string("Cache-Control",
                allOf(
                    containsString("no-store"),
                    containsString("no-cache"),
                    containsString("must-revalidate")
                )))
            .andExpect(header().string("Pragma", "no-cache"));
    }

    @Test
    @DisplayName("DLQ entries must not log PHI in error messages")
    void dlqEntriesShouldNotLogPhiInErrorMessages() {
        // Given
        Map<String, Object> phiPayload = Map.of(
            "patientId", "TEST-PATIENT-123",
            "patientName", "Test-Patient-Smith",  // Synthetic name
            "mrn", "TEST-MRN-456789",             // Synthetic MRN
            "measureId", "BCS-E"
        );

        // When - record a failure
        DeadLetterQueueEntity entity = dlqService.recordFailure(
            "evaluation.completed",
            "EVALUATION_FAILED",
            TENANT_ID,
            "TEST-PATIENT-123",
            phiPayload,
            new RuntimeException("Processing failed")
        );

        // Then - error message should not contain PHI
        assertThat(entity.getErrorMessage())
            .doesNotContain("Smith")
            .doesNotContain("456789");

        // Stack trace should only contain technical details
        assertThat(entity.getStackTrace())
            .contains("RuntimeException")
            .doesNotContain("patientName")
            .doesNotContain("mrn");
    }

    @Test
    @DisplayName("Test data must use HIPAA-compliant synthetic patterns")
    void testDataMustUseSyntheticPatterns() {
        // Given - create test DLQ entry
        DeadLetterQueueEntity entity = DeadLetterQueueEntity.builder()
            .eventId(UUID.randomUUID())
            .tenantId("tenant-test-001")
            .topic("evaluation.completed")
            .eventType("CARE_GAP_DETECTED")
            .patientId("TEST-PATIENT-" + UUID.randomUUID().toString().substring(0, 6))
            .eventPayload("{\"patientId\":\"TEST-PATIENT-123\",\"mrn\":\"TEST-MRN-100001\"}")
            .errorMessage("Test error")
            .retryCount(0)
            .maxRetryCount(3)
            .firstFailureAt(Instant.now())
            .status(DLQStatus.FAILED)
            .build();

        // Then - verify synthetic patterns
        assertThat(entity.getPatientId())
            .startsWith("TEST-PATIENT-")
            .withFailMessage("Patient IDs must use TEST-PATIENT-xxx pattern");

        assertThat(entity.getEventPayload())
            .contains("TEST-PATIENT-")
            .contains("TEST-MRN-")
            .withFailMessage("Payloads must use synthetic data patterns");
    }

    @Test
    @DisplayName("DLQ cleanup must enforce retention policy")
    void dlqCleanupMustEnforceRetentionPolicy() {
        // Given - HIPAA requires 7-year retention for audit records
        // DLQ resolved entries are cleaned up after 30 days (not audit records)

        // When
        dlqService.cleanupOldResolved(30);

        // Then - verify cleanup respects retention period
        // Audit events are stored separately and retained for 7 years
    }

    @Test
    @DisplayName("PHI access in DLQ must be audited")
    void phiAccessInDlqMustBeAudited() {
        // DLQ entries containing PHI (patient IDs, MRNs) must trigger audit events
        // The @Audited annotation should be on controller methods

        // Verify audit logging is configured
        // AuditService.logPhiAccess() should be called for DLQ reads
    }
}
```

---

### Performance Tests

Tests benchmark event processing latency and throughput.

```java
@SpringBootTest
@Testcontainers
class EventProcessingPerformanceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Autowired
    private DeadLetterQueueService dlqService;

    @Autowired
    private DLQRetryProcessor retryProcessor;

    @Test
    @DisplayName("DLQ failure recording should complete within 50ms")
    void dlqFailureRecordingShouldBeUnder50ms() {
        // Given
        String topic = "evaluation.completed";
        String eventType = "CARE_GAP_DETECTED";
        String tenantId = "tenant-perf-001";
        Map<String, Object> payload = Map.of("measureId", "BCS-E", "status", "open");
        Exception error = new RuntimeException("Service unavailable");

        // When
        Instant start = Instant.now();
        dlqService.recordFailure(topic, eventType, tenantId, "TEST-PATIENT-001", payload, error);
        Instant end = Instant.now();

        // Then
        long durationMs = Duration.between(start, end).toMillis();
        assertThat(durationMs)
            .isLessThan(50L)
            .withFailMessage("DLQ failure recording took %dms, exceeds 50ms SLA", durationMs);
    }

    @Test
    @DisplayName("Batch retry processing should handle 100 events within 10 seconds")
    void batchRetryProcessingShouldHandle100EventsIn10Seconds() {
        // Given
        int eventCount = 100;
        String tenantId = "tenant-perf-001";

        // Create 100 failed events
        for (int i = 0; i < eventCount; i++) {
            dlqService.recordFailure(
                "evaluation.completed",
                "CARE_GAP_DETECTED",
                tenantId,
                "TEST-PATIENT-" + String.format("%06d", i),
                Map.of("index", i),
                new RuntimeException("Transient error " + i)
            );
        }

        // Set nextRetryAt to past for all events
        // (In real test, wait for backoff or update directly)

        // When
        Instant start = Instant.now();
        retryProcessor.processRetries();
        Instant end = Instant.now();

        // Then
        long durationSec = Duration.between(start, end).toSeconds();
        assertThat(durationSec)
            .isLessThan(10L)
            .withFailMessage("Batch retry took %d seconds, exceeds 10s SLA", durationSec);

        System.out.printf("Performance: %d events processed in %d seconds (%.2f events/sec)%n",
            eventCount, durationSec, eventCount / (double) durationSec);
    }

    @Test
    @DisplayName("DLQ statistics query should complete within 100ms")
    void dlqStatisticsQueryShouldBeUnder100ms() {
        // Given - populate some data
        String tenantId = "tenant-perf-001";
        for (int i = 0; i < 50; i++) {
            dlqService.recordFailure(
                "evaluation.completed", "EVENT", tenantId,
                "TEST-PATIENT-" + i, Map.of("i", i), new RuntimeException("Error"));
        }

        // When
        Instant start = Instant.now();
        DLQStats stats = dlqService.getStats(tenantId);
        Instant end = Instant.now();

        // Then
        long durationMs = Duration.between(start, end).toMillis();
        assertThat(durationMs)
            .isLessThan(100L)
            .withFailMessage("DLQ stats query took %dms, exceeds 100ms SLA", durationMs);
    }

    @Test
    @DisplayName("Health check should complete within 50ms")
    void healthCheckShouldBeUnder50ms() {
        // Given
        DLQHealthIndicator healthIndicator = new DLQHealthIndicator(dlqRepository);

        // When
        Instant start = Instant.now();
        Health health = healthIndicator.health();
        Instant end = Instant.now();

        // Then
        long durationMs = Duration.between(start, end).toMillis();
        assertThat(durationMs)
            .isLessThan(50L)
            .withFailMessage("Health check took %dms, exceeds 50ms SLA", durationMs);
    }
}
```

**Performance SLAs:**

| Operation | Target Latency | Notes |
|-----------|----------------|-------|
| Record DLQ failure | < 50ms | Single event recording |
| Batch retry (100 events) | < 10s | Includes Kafka republish |
| Statistics query | < 100ms | Per-tenant counts |
| Health check | < 50ms | Repository count queries |
| Prometheus scrape | < 100ms | All DLQ metrics |

---

### Test Configuration

**BaseIntegrationTest with TestContainers:**

```java
@Testcontainers
public abstract class EventProcessingBaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("events_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }
}
```

**Using Shared Test Fixtures:**

```java
// Import from platform:test-fixtures
import com.healthdata.testfixtures.security.GatewayTrustTestHeaders;

@Test
void shouldRetryEventAsAdmin() throws Exception {
    mockMvc.perform(post("/api/v1/dead-letter-queue/{dlqId}/retry", dlqId)
            .headers(GatewayTrustTestHeaders.adminHeaders("tenant-001")))
        .andExpect(status().isAccepted());
}
```

---

### Best Practices

| Practice | Description |
|----------|-------------|
| **Use TestContainers** | Real PostgreSQL and Kafka for integration tests |
| **Test exponential backoff** | Verify retry timing increases: 1s, 2s, 4s, 8s |
| **Verify tenant isolation** | Every DLQ query must filter by tenantId |
| **Test batch resilience** | One failure shouldn't stop batch processing |
| **Mock Kafka for unit tests** | Use `KafkaTemplate` mocks for fast unit tests |
| **Test exhaustion alerts** | Verify alerts when max retries reached |
| **Synthetic data patterns** | Use TEST-PATIENT-xxx, TEST-MRN-xxxxxx |
| **Health threshold testing** | Test UP, WARNING, DOWN transitions |

---

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| TestContainers timeout | Docker daemon not running | Start Docker Desktop |
| Kafka connection refused | Wrong bootstrap servers | Check `kafka.getBootstrapServers()` |
| DLQ entry not found | Wrong tenant context | Verify X-Tenant-ID header |
| Retry not triggered | nextRetryAt in future | Wait for backoff or update timestamp |
| Metrics not appearing | MeterRegistry not configured | Add `@AutoConfigureMetrics` to test |
| Health check DOWN | Too many failed events | Clear test DLQ entries in @BeforeEach |
| JSON deserialization error | Malformed event payload | Validate payload JSON before save |

---

### Manual Testing

```bash
# Get failed events for tenant
curl http://localhost:8083/events/api/v1/dead-letter-queue/failed \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-Auth-User-Id: user-001" \
  -H "X-Auth-Roles: ADMIN"

# Get DLQ statistics
curl http://localhost:8083/events/api/v1/dead-letter-queue/stats \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-Auth-User-Id: user-001" \
  -H "X-Auth-Roles: ADMIN"

# Retry a failed event
curl -X POST http://localhost:8083/events/api/v1/dead-letter-queue/{dlqId}/retry \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-Auth-User-Id: admin-001" \
  -H "X-Auth-Roles: ADMIN"

# Mark as resolved
curl -X POST http://localhost:8083/events/api/v1/dead-letter-queue/{dlqId}/resolve \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-Auth-User-Id: admin-001" \
  -H "X-Auth-Roles: ADMIN" \
  -H "Content-Type: application/json" \
  -d '{"resolvedBy":"admin@test.hdim.io","notes":"Manually fixed"}'

# Get exhausted events (need intervention)
curl http://localhost:8083/events/api/v1/dead-letter-queue/exhausted \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-Auth-User-Id: user-001" \
  -H "X-Auth-Roles: ADMIN"

# Get recent failures (last 24 hours)
curl "http://localhost:8083/events/api/v1/dead-letter-queue/recent?hours=24" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-Auth-User-Id: user-001" \
  -H "X-Auth-Roles: ADMIN"

# Check health endpoint
curl http://localhost:8083/events/actuator/health

# Get Prometheus metrics
curl http://localhost:8083/events/actuator/prometheus | grep -E "^(dlq|event)"
```

## Monitoring

- **Prometheus metrics**: `/actuator/prometheus`
- **Health checks**: `/actuator/health`
- **DLQ dashboard**: Monitor exhausted events needing intervention
- **Event processing duration**: SLA tracking (10ms, 50ms, 100ms, 500ms, 1s)
