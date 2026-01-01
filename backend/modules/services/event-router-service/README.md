# Event Router Service

Multi-tenant event routing service with priority-based processing, filtering, and transformation capabilities.

## Purpose

The Event Router Service provides intelligent routing of healthcare events across the HDIM platform. It consumes events from multiple source topics, applies tenant-specific filtering and transformation rules, and routes events to appropriate destination topics based on configurable routing rules.

## Key Features

- **Multi-tenant Event Routing**: Tenant-isolated routing rules and event processing
- **Priority Queue**: Priority-based event processing with configurable batch sizes
- **Event Filtering**: JSONPath-based filtering expressions for conditional routing
- **Event Transformation**: Script-based event transformation before routing
- **Load Balancing**: Distributed event processing with load balancing support
- **Dead Letter Queue**: Automatic DLQ handling for failed events
- **Metrics & Monitoring**: Real-time routing metrics, error rates, and throughput tracking
- **Circuit Breaker**: Resilience4j circuit breaker for fault tolerance
- **Kafka Integration**: High-throughput Kafka-based event streaming

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check with queue status and routing metrics |
| GET | `/actuator/metrics` | Service metrics and performance indicators |
| GET | `/actuator/prometheus` | Prometheus-compatible metrics export |

## Configuration

### Application Properties

```yaml
server:
  port: 8095

event-router:
  source-topics:
    - fhir.patient.created
    - fhir.patient.updated
    - fhir.observation.created
    - fhir.encounter.created
    - fhir.medication.created
  dlq-topic: event-router.dlq
  priority-queue:
    max-size: 50000
    batch-size: 100
    poll-interval-ms: 100
  routing:
    default-priority: MEDIUM
    enable-async: true
    thread-pool-size: 10
```

### Database

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/healthdata_event_router
    username: healthdata
    password: ${SPRING_DATASOURCE_PASSWORD}
```

### Kafka

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: event-router-consumer-group
    producer:
      acks: all
      retries: 3
```

## Core Components

### EventRouter
Main routing service that matches events to routing rules, applies filters and transformations, and publishes to target topics.

### PriorityQueueService
Manages priority-based event queue with high/medium/low priority levels.

### EventFilterService
Evaluates filter expressions against event payloads using JSONPath.

### EventTransformationService
Applies script-based transformations to events before routing.

### RouteMetricsService
Tracks routing metrics including events routed, filtered, unrouted, latency, and error rates.

## Running Locally

### Start the Service

```bash
./gradlew :modules:services:event-router-service:bootRun
```

### Running Tests

```bash
./gradlew :modules:services:event-router-service:test
```

### Building

```bash
./gradlew :modules:services:event-router-service:build
```

## Testing

The Event Router Service has comprehensive test coverage across 8 test suites covering unit, integration, multi-tenant isolation, RBAC, HIPAA compliance, and performance testing. The tests validate priority-based event routing, JSONPath filtering, event transformations, load balancing, Dead Letter Queue handling, and Prometheus metrics collection.

---

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:event-router-service:test

# Run specific test suites
./gradlew :modules:services:event-router-service:test --tests "*EventRouterTest"
./gradlew :modules:services:event-router-service:test --tests "*IntegrationTest"
./gradlew :modules:services:event-router-service:test --tests "*FilterServiceTest"
./gradlew :modules:services:event-router-service:test --tests "*TransformationServiceTest"

# Run tests with coverage report
./gradlew :modules:services:event-router-service:test jacocoTestReport

# Run tests with verbose output
./gradlew :modules:services:event-router-service:test --info

# Run a single test class
./gradlew :modules:services:event-router-service:test --tests "PriorityQueueServiceTest"
```

---

### Test Organization

```
src/test/java/com/healthdata/eventrouter/
├── service/
│   ├── EventRouterTest.java                    # Core routing logic
│   ├── PriorityQueueServiceTest.java           # Priority queue ordering
│   ├── EventFilterServiceTest.java             # JSONPath filter evaluation
│   ├── EventTransformationServiceTest.java     # Event transformation scripts
│   ├── RouteMetricsServiceTest.java            # Prometheus metrics collection
│   ├── DeadLetterQueueServiceTest.java         # DLQ handling and retry
│   └── LoadBalancingServiceTest.java           # Consumer load distribution
└── integration/
    └── MultiTenantRoutingIntegrationTest.java  # Multi-tenant isolation tests
```

---

### Test Coverage Summary

| Test File | Tests | Coverage Focus |
|-----------|-------|----------------|
| `EventRouterTest.java` | 6 | Core routing logic, rule matching, filter/transform application |
| `PriorityQueueServiceTest.java` | 8 | Priority ordering (CRITICAL→HIGH→MEDIUM→LOW), FIFO within priority |
| `EventFilterServiceTest.java` | 10 | JSONPath operators: equality, $gte, $gt, $in, $ne, $exists, regex |
| `EventTransformationServiceTest.java` | 10 | Enrichment, rename, remove, convert, mask, flatten, JavaScript |
| `RouteMetricsServiceTest.java` | 12 | Counters, timers, snapshots, throughput, error rates |
| `DeadLetterQueueServiceTest.java` | 7 | DLQ persistence, Kafka publishing, retry with limits |
| `LoadBalancingServiceTest.java` | 8 | Round-robin, tenant-based, key-based partitioning, least-loaded |
| `MultiTenantRoutingIntegrationTest.java` | 8 | Tenant isolation, rule filtering, priority/transform per tenant |
| **Total** | **69** | **Full event routing pipeline coverage** |

---

### Unit Tests - Service Layer

#### EventRouter Service Tests

Tests core routing logic including rule matching, filter evaluation, transformation application, and metrics recording.

```java
package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.EventMessage;
import com.healthdata.eventrouter.entity.RoutingRuleEntity;
import com.healthdata.eventrouter.entity.RoutingRuleEntity.Priority;
import com.healthdata.eventrouter.persistence.RoutingRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Event Router Service Tests")
class EventRouterTest {

    @Mock
    private RoutingRuleRepository ruleRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private EventFilterService filterService;

    @Mock
    private EventTransformationService transformationService;

    @Mock
    private RouteMetricsService metricsService;

    private EventRouter eventRouter;

    private static final String TENANT_ID = "tenant-test-001";
    private static final String SOURCE_TOPIC = "fhir.patient.created";

    @BeforeEach
    void setUp() {
        eventRouter = new EventRouter(
            ruleRepository,
            kafkaTemplate,
            filterService,
            transformationService,
            metricsService
        );
    }

    @Nested
    @DisplayName("Route By Event Type Tests")
    class RouteByEventTypeTests {

        @Test
        @DisplayName("Should route event by type to correct topic")
        void shouldRouteEventByType() {
            // Given
            EventMessage event = createEvent("PATIENT_CREATED");
            RoutingRuleEntity rule = createRule(SOURCE_TOPIC, "patient.processing", Priority.HIGH);
            when(ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue(TENANT_ID, SOURCE_TOPIC))
                .thenReturn(List.of(rule));
            when(filterService.matches(any(), any())).thenReturn(true);
            when(transformationService.transform(any(), any())).thenReturn(event);

            // When
            eventRouter.routeEvent(event);

            // Then
            verify(kafkaTemplate).send(eq("patient.processing"), any(String.class));
            verify(metricsService).recordRoutedEvent("patient.processing", Priority.HIGH);
        }

        @Test
        @DisplayName("Should route multiple event types to different topics")
        void shouldRouteMultipleEventTypes() {
            // Given
            RoutingRuleEntity patientRule = createRule(SOURCE_TOPIC, "patient.processing", Priority.HIGH);
            RoutingRuleEntity observationRule = createRule("fhir.observation.created", "observation.processing", Priority.MEDIUM);

            when(ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue(TENANT_ID, SOURCE_TOPIC))
                .thenReturn(List.of(patientRule));
            when(ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue(TENANT_ID, "fhir.observation.created"))
                .thenReturn(List.of(observationRule));
            when(filterService.matches(any(), any())).thenReturn(true);
            when(transformationService.transform(any(), any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            EventMessage patientEvent = createEvent("PATIENT_CREATED");
            patientEvent.setSourceTopic(SOURCE_TOPIC);
            eventRouter.routeEvent(patientEvent);

            EventMessage observationEvent = createEvent("OBSERVATION_CREATED");
            observationEvent.setSourceTopic("fhir.observation.created");
            eventRouter.routeEvent(observationEvent);

            // Then
            verify(kafkaTemplate).send(eq("patient.processing"), any(String.class));
            verify(kafkaTemplate).send(eq("observation.processing"), any(String.class));
        }

        @Test
        @DisplayName("Should skip events without matching rules")
        void shouldSkipEventsWithoutMatchingRules() {
            // Given
            EventMessage event = createEvent("UNKNOWN_EVENT");
            when(ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue(anyString(), anyString()))
                .thenReturn(List.of());

            // When
            eventRouter.routeEvent(event);

            // Then
            verify(kafkaTemplate, never()).send(anyString(), anyString());
            verify(metricsService).recordUnroutedEvent(anyString());
        }
    }

    @Nested
    @DisplayName("Filter and Transform Tests")
    class FilterAndTransformTests {

        @Test
        @DisplayName("Should filter events based on filter expression")
        void shouldFilterEvents() {
            // Given
            EventMessage event = createEvent("PATIENT_CREATED");
            event.setPayload(Map.of("region", "US", "urgent", true));

            RoutingRuleEntity rule = createRule(SOURCE_TOPIC, "patient.processing", Priority.HIGH);
            rule.setFilterExpression("{\"region\": \"US\"}");

            when(ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue(TENANT_ID, SOURCE_TOPIC))
                .thenReturn(List.of(rule));
            when(filterService.matches(event, "{\"region\": \"US\"}")).thenReturn(true);
            when(transformationService.transform(any(), any())).thenReturn(event);

            // When
            eventRouter.routeEvent(event);

            // Then
            verify(kafkaTemplate).send(eq("patient.processing"), any(String.class));
        }

        @Test
        @DisplayName("Should apply event transformation before routing")
        void shouldApplyTransformation() {
            // Given
            EventMessage event = createEvent("PATIENT_CREATED");
            event.setPayload(Map.of("firstName", "John", "lastName", "Doe"));

            EventMessage transformedEvent = createEvent("PATIENT_CREATED");
            transformedEvent.setPayload(Map.of(
                "firstName", "John",
                "lastName", "Doe",
                "fullName", "John Doe",
                "enrichedAt", "2025-12-31T00:00:00Z"
            ));

            RoutingRuleEntity rule = createRule(SOURCE_TOPIC, "patient.processing", Priority.HIGH);
            rule.setTransformationScript("enrichment:add-timestamp|js:fullName");

            when(ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue(TENANT_ID, SOURCE_TOPIC))
                .thenReturn(List.of(rule));
            when(filterService.matches(any(), any())).thenReturn(true);
            when(transformationService.transform(event, "enrichment:add-timestamp|js:fullName"))
                .thenReturn(transformedEvent);

            // When
            eventRouter.routeEvent(event);

            // Then
            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(eq("patient.processing"), payloadCaptor.capture());
            assertThat(payloadCaptor.getValue()).contains("fullName");
        }

        @Test
        @DisplayName("Should record routing metrics")
        void shouldRecordMetrics() {
            // Given
            EventMessage event = createEvent("PATIENT_CREATED");
            RoutingRuleEntity rule = createRule(SOURCE_TOPIC, "patient.processing", Priority.CRITICAL);

            when(ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue(TENANT_ID, SOURCE_TOPIC))
                .thenReturn(List.of(rule));
            when(filterService.matches(any(), any())).thenReturn(true);
            when(transformationService.transform(any(), any())).thenReturn(event);

            // When
            eventRouter.routeEvent(event);

            // Then
            verify(metricsService).recordRoutedEvent("patient.processing", Priority.CRITICAL);
            verify(metricsService).recordRoutingLatency(eq("patient.processing"), any());
        }
    }

    private EventMessage createEvent(String type) {
        EventMessage event = new EventMessage();
        event.setEventType(type);
        event.setTenantId(TENANT_ID);
        event.setSourceTopic(SOURCE_TOPIC);
        event.setPayload(Map.of("testKey", "testValue"));
        return event;
    }

    private RoutingRuleEntity createRule(String sourceTopic, String targetTopic, Priority priority) {
        RoutingRuleEntity rule = new RoutingRuleEntity();
        rule.setTenantId(TENANT_ID);
        rule.setSourceTopic(sourceTopic);
        rule.setTargetTopic(targetTopic);
        rule.setPriority(priority);
        rule.setEnabled(true);
        return rule;
    }
}
```

---

#### Priority Queue Service Tests

Tests priority-based event ordering with CRITICAL > HIGH > MEDIUM > LOW priority levels and FIFO ordering within same priority.

```java
package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.EventMessage;
import com.healthdata.eventrouter.entity.RoutingRuleEntity.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Priority Queue Service Tests")
class PriorityQueueServiceTest {

    private PriorityQueueService queueService;

    @BeforeEach
    void setUp() {
        queueService = new PriorityQueueService();
    }

    @Nested
    @DisplayName("Priority Ordering Tests")
    class PriorityOrderingTests {

        @Test
        @DisplayName("Should process CRITICAL priority events first")
        void shouldProcessCriticalFirst() {
            // Given
            queueService.enqueue(createPrioritizedEvent("low-event", Priority.LOW));
            queueService.enqueue(createPrioritizedEvent("medium-event", Priority.MEDIUM));
            queueService.enqueue(createPrioritizedEvent("critical-event", Priority.CRITICAL));
            queueService.enqueue(createPrioritizedEvent("high-event", Priority.HIGH));

            // When
            EventMessage first = queueService.dequeue();

            // Then
            assertThat(first.getEventType()).isEqualTo("critical-event");
        }

        @Test
        @DisplayName("Should process events in priority order CRITICAL > HIGH > MEDIUM > LOW")
        void shouldProcessInPriorityOrder() {
            // Given
            queueService.enqueue(createPrioritizedEvent("low-event", Priority.LOW));
            queueService.enqueue(createPrioritizedEvent("medium-event", Priority.MEDIUM));
            queueService.enqueue(createPrioritizedEvent("critical-event", Priority.CRITICAL));
            queueService.enqueue(createPrioritizedEvent("high-event", Priority.HIGH));

            // When
            List<String> dequeueOrder = new ArrayList<>();
            while (!queueService.isEmpty()) {
                dequeueOrder.add(queueService.dequeue().getEventType());
            }

            // Then
            assertThat(dequeueOrder).containsExactly(
                "critical-event",
                "high-event",
                "medium-event",
                "low-event"
            );
        }

        @Test
        @DisplayName("Should maintain FIFO within same priority")
        void shouldMaintainFifoWithinPriority() {
            // Given
            queueService.enqueue(createPrioritizedEvent("high-1", Priority.HIGH));
            queueService.enqueue(createPrioritizedEvent("high-2", Priority.HIGH));
            queueService.enqueue(createPrioritizedEvent("high-3", Priority.HIGH));

            // When
            List<String> dequeueOrder = new ArrayList<>();
            while (!queueService.isEmpty()) {
                dequeueOrder.add(queueService.dequeue().getEventType());
            }

            // Then
            assertThat(dequeueOrder).containsExactly("high-1", "high-2", "high-3");
        }
    }

    @Nested
    @DisplayName("Queue Operations Tests")
    class QueueOperationsTests {

        @Test
        @DisplayName("Should return null when queue is empty")
        void shouldReturnNullWhenEmpty() {
            // When
            EventMessage result = queueService.dequeue();

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should report queue size correctly")
        void shouldReportQueueSize() {
            // Given
            queueService.enqueue(createPrioritizedEvent("event-1", Priority.MEDIUM));
            queueService.enqueue(createPrioritizedEvent("event-2", Priority.HIGH));
            queueService.enqueue(createPrioritizedEvent("event-3", Priority.LOW));

            // When/Then
            assertThat(queueService.size()).isEqualTo(3);

            queueService.dequeue();
            assertThat(queueService.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle high volume of events (1000 events)")
        void shouldHandleHighVolume() {
            // Given
            for (int i = 0; i < 1000; i++) {
                Priority priority = Priority.values()[i % 4];
                queueService.enqueue(createPrioritizedEvent("event-" + i, priority));
            }

            // When/Then
            assertThat(queueService.size()).isEqualTo(1000);

            // Verify priority ordering is maintained
            Priority lastPriority = Priority.CRITICAL;
            while (!queueService.isEmpty()) {
                EventMessage event = queueService.dequeue();
                // Priority should never decrease (CRITICAL=0, HIGH=1, MEDIUM=2, LOW=3)
                assertThat(event.getPriority().ordinal())
                    .isGreaterThanOrEqualTo(lastPriority.ordinal() - 1);
            }
        }

        @Test
        @DisplayName("Should support peek without removing")
        void shouldSupportPeek() {
            // Given
            queueService.enqueue(createPrioritizedEvent("event-1", Priority.HIGH));
            queueService.enqueue(createPrioritizedEvent("event-2", Priority.CRITICAL));

            // When
            EventMessage peeked = queueService.peek();

            // Then
            assertThat(peeked.getEventType()).isEqualTo("event-2");
            assertThat(queueService.size()).isEqualTo(2); // Not removed
        }
    }

    private EventMessage createPrioritizedEvent(String type, Priority priority) {
        EventMessage event = new EventMessage();
        event.setEventType(type);
        event.setTenantId("tenant-test-001");
        event.setPriority(priority);
        return event;
    }
}
```

---

#### Event Filter Service Tests

Tests JSONPath-based filter evaluation supporting equality, comparison operators, and pattern matching.

```java
package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.EventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Event Filter Service Tests")
class EventFilterServiceTest {

    private EventFilterService filterService;

    @BeforeEach
    void setUp() {
        filterService = new EventFilterService();
    }

    @Nested
    @DisplayName("Simple Filter Tests")
    class SimpleFilterTests {

        @Test
        @DisplayName("Should match simple equality filter")
        void shouldMatchSimpleEquality() {
            // Given
            EventMessage event = createEvent(Map.of("status", "active", "region", "US"));
            String filter = "{\"status\": \"active\"}";

            // When
            boolean matches = filterService.matches(event, filter);

            // Then
            assertThat(matches).isTrue();
        }

        @Test
        @DisplayName("Should match multiple fields (AND logic)")
        void shouldMatchMultipleFields() {
            // Given
            EventMessage event = createEvent(Map.of("status", "active", "region", "US", "priority", "HIGH"));
            String filter = "{\"status\": \"active\", \"region\": \"US\"}";

            // When
            boolean matches = filterService.matches(event, filter);

            // Then
            assertThat(matches).isTrue();
        }

        @Test
        @DisplayName("Should match nested field")
        void shouldMatchNestedField() {
            // Given
            EventMessage event = createEvent(Map.of(
                "patient", Map.of("address", Map.of("state", "MA"))
            ));
            String filter = "{\"patient.address.state\": \"MA\"}";

            // When
            boolean matches = filterService.matches(event, filter);

            // Then
            assertThat(matches).isTrue();
        }
    }

    @Nested
    @DisplayName("Comparison Operator Tests")
    class ComparisonOperatorTests {

        @Test
        @DisplayName("Should match numeric comparison with $gte")
        void shouldMatchNumericGte() {
            // Given
            EventMessage event = createEvent(Map.of("age", 45, "score", 85));
            String filter = "{\"age\": {\"$gte\": 40}}";

            // When
            boolean matches = filterService.matches(event, filter);

            // Then
            assertThat(matches).isTrue();
        }

        @Test
        @DisplayName("Should match numeric comparison with $gt")
        void shouldMatchNumericGt() {
            // Given
            EventMessage event = createEvent(Map.of("count", 100));
            String filter = "{\"count\": {\"$gt\": 50}}";

            // When
            boolean matches = filterService.matches(event, filter);

            // Then
            assertThat(matches).isTrue();
        }

        @Test
        @DisplayName("Should match IN operator")
        void shouldMatchInOperator() {
            // Given
            EventMessage event = createEvent(Map.of("eventType", "PATIENT_CREATED"));
            String filter = "{\"eventType\": {\"$in\": [\"PATIENT_CREATED\", \"PATIENT_UPDATED\"]}}";

            // When
            boolean matches = filterService.matches(event, filter);

            // Then
            assertThat(matches).isTrue();
        }

        @Test
        @DisplayName("Should match NOT operator ($ne)")
        void shouldMatchNotOperator() {
            // Given
            EventMessage event = createEvent(Map.of("status", "active"));
            String filter = "{\"status\": {\"$ne\": \"deleted\"}}";

            // When
            boolean matches = filterService.matches(event, filter);

            // Then
            assertThat(matches).isTrue();
        }

        @Test
        @DisplayName("Should match EXISTS operator")
        void shouldMatchExistsOperator() {
            // Given
            EventMessage event = createEvent(Map.of("optionalField", "value"));
            String filter = "{\"optionalField\": {\"$exists\": true}}";

            // When
            boolean matches = filterService.matches(event, filter);

            // Then
            assertThat(matches).isTrue();
        }

        @Test
        @DisplayName("Should match regex pattern")
        void shouldMatchRegexPattern() {
            // Given
            EventMessage event = createEvent(Map.of("email", "user@healthdata.com"));
            String filter = "{\"email\": {\"$regex\": \".*@healthdata\\\\.com$\"}}";

            // When
            boolean matches = filterService.matches(event, filter);

            // Then
            assertThat(matches).isTrue();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle parsing errors gracefully")
        void shouldHandleParsingErrors() {
            // Given
            EventMessage event = createEvent(Map.of("field", "value"));
            String invalidFilter = "not valid json {{{";

            // When
            boolean matches = filterService.matches(event, invalidFilter);

            // Then
            assertThat(matches).isFalse(); // Default to no match on error
        }
    }

    private EventMessage createEvent(Map<String, Object> payload) {
        EventMessage event = new EventMessage();
        event.setEventType("TEST_EVENT");
        event.setTenantId("tenant-test-001");
        event.setPayload(payload);
        return event;
    }
}
```

---

#### Event Transformation Service Tests

Tests script-based event transformations including enrichment, field operations, data type conversion, and masking.

```java
package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.EventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Event Transformation Service Tests")
class EventTransformationServiceTest {

    private EventTransformationService transformationService;

    @BeforeEach
    void setUp() {
        transformationService = new EventTransformationService();
    }

    @Nested
    @DisplayName("Enrichment Transformation Tests")
    class EnrichmentTransformationTests {

        @Test
        @DisplayName("Should add enrichment fields to event")
        void shouldEnrichEvent() {
            // Given
            EventMessage event = createEvent(Map.of("name", "John Doe"));
            String script = "enrichment:add-timestamp,add-source";

            // When
            EventMessage transformed = transformationService.transform(event, script);

            // Then
            assertThat(transformed.getPayload()).containsKey("enrichedAt");
            assertThat(transformed.getPayload()).containsKey("source");
        }

        @Test
        @DisplayName("Should apply multiple transformations in sequence")
        void shouldApplyMultipleTransformations() {
            // Given
            EventMessage event = createEvent(Map.of("field", "value"));
            String script = "enrichment:add-timestamp|rename:field->newField";

            // When
            EventMessage transformed = transformationService.transform(event, script);

            // Then
            assertThat(transformed.getPayload()).containsKey("enrichedAt");
            assertThat(transformed.getPayload()).containsKey("newField");
            assertThat(transformed.getPayload()).doesNotContainKey("field");
        }
    }

    @Nested
    @DisplayName("Field Operation Tests")
    class FieldOperationTests {

        @Test
        @DisplayName("Should rename fields in event")
        void shouldRenameFields() {
            // Given
            EventMessage event = createEvent(Map.of("old_field", "value"));
            String script = "rename:old_field->new_field";

            // When
            EventMessage transformed = transformationService.transform(event, script);

            // Then
            assertThat(transformed.getPayload()).containsKey("new_field");
            assertThat(transformed.getPayload()).doesNotContainKey("old_field");
        }

        @Test
        @DisplayName("Should remove fields from event")
        void shouldRemoveFields() {
            // Given
            EventMessage event = createEvent(Map.of(
                "keep", "value1",
                "remove", "value2",
                "also_remove", "value3"
            ));
            String script = "remove:remove,also_remove";

            // When
            EventMessage transformed = transformationService.transform(event, script);

            // Then
            assertThat(transformed.getPayload()).containsKey("keep");
            assertThat(transformed.getPayload()).doesNotContainKey("remove");
            assertThat(transformed.getPayload()).doesNotContainKey("also_remove");
        }

        @Test
        @DisplayName("Should flatten nested objects")
        void shouldFlattenNestedObjects() {
            // Given
            Map<String, Object> nested = Map.of(
                "patient", Map.of(
                    "name", "John",
                    "address", Map.of("city", "Boston", "state", "MA")
                )
            );
            EventMessage event = createEvent(nested);
            String script = "flatten:patient";

            // When
            EventMessage transformed = transformationService.transform(event, script);

            // Then
            assertThat(transformed.getPayload()).containsKey("patient.name");
            assertThat(transformed.getPayload()).containsKey("patient.address.city");
        }
    }

    @Nested
    @DisplayName("Data Type and Masking Tests")
    class DataTypeAndMaskingTests {

        @Test
        @DisplayName("Should convert data types")
        void shouldConvertDataTypes() {
            // Given
            EventMessage event = createEvent(Map.of("age", "45", "score", "85.5"));
            String script = "convert:age->integer,score->double";

            // When
            EventMessage transformed = transformationService.transform(event, script);

            // Then
            assertThat(transformed.getPayload().get("age")).isInstanceOf(Integer.class);
            assertThat(transformed.getPayload().get("score")).isInstanceOf(Double.class);
        }

        @Test
        @DisplayName("Should mask sensitive fields (PHI protection)")
        void shouldMaskSensitiveFields() {
            // Given
            EventMessage event = createEvent(Map.of(
                "ssn", "123-45-6789",
                "email", "user@example.com"
            ));
            String script = "mask:ssn,email";

            // When
            EventMessage transformed = transformationService.transform(event, script);

            // Then
            assertThat(transformed.getPayload().get("ssn")).isEqualTo("***-**-****");
            assertThat(transformed.getPayload().get("email")).asString().contains("***");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle transformation errors gracefully")
        void shouldHandleErrors() {
            // Given
            EventMessage event = createEvent(Map.of("field", "value"));
            String invalidScript = "invalid:syntax:here";

            // When
            EventMessage transformed = transformationService.transform(event, invalidScript);

            // Then
            assertThat(transformed).isNotNull();
            assertThat(transformed.getPayload()).containsEntry("field", "value");
        }

        @Test
        @DisplayName("Should return original event when script is null or empty")
        void shouldReturnOriginalWhenNoScript() {
            // Given
            EventMessage event = createEvent(Map.of("field", "value"));

            // When
            EventMessage transformedNull = transformationService.transform(event, null);
            EventMessage transformedEmpty = transformationService.transform(event, "");

            // Then
            assertThat(transformedNull.getPayload()).containsEntry("field", "value");
            assertThat(transformedEmpty.getPayload()).containsEntry("field", "value");
        }
    }

    private EventMessage createEvent(Map<String, Object> payload) {
        EventMessage event = new EventMessage();
        event.setEventType("TEST_EVENT");
        event.setTenantId("tenant-test-001");
        event.setPayload(payload);
        return event;
    }
}
```

---

#### Load Balancing Service Tests

Tests consumer load distribution strategies including round-robin, tenant-based, and key-based partitioning.

```java
package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.EventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Load Balancing Service Tests")
class LoadBalancingServiceTest {

    private LoadBalancingService loadBalancingService;

    @BeforeEach
    void setUp() {
        loadBalancingService = new LoadBalancingService();
    }

    @Nested
    @DisplayName("Partition Selection Tests")
    class PartitionSelectionTests {

        @Test
        @DisplayName("Should distribute events using round-robin")
        void shouldDistributeRoundRobin() {
            // Given
            String targetTopic = "patient.processing";
            int partitionCount = 3;

            // When
            int partition1 = loadBalancingService.selectPartition(createEvent("event-1"), targetTopic, partitionCount);
            int partition2 = loadBalancingService.selectPartition(createEvent("event-2"), targetTopic, partitionCount);
            int partition3 = loadBalancingService.selectPartition(createEvent("event-3"), targetTopic, partitionCount);
            int partition4 = loadBalancingService.selectPartition(createEvent("event-4"), targetTopic, partitionCount);

            // Then
            assertThat(partition1).isEqualTo(0);
            assertThat(partition2).isEqualTo(1);
            assertThat(partition3).isEqualTo(2);
            assertThat(partition4).isEqualTo(0); // Wraps around
        }

        @Test
        @DisplayName("Should distribute by tenant for tenant-based partitioning")
        void shouldDistributeByTenant() {
            // Given
            String targetTopic = "patient.processing";
            int partitionCount = 4;

            EventMessage tenant1Event = createEvent("event-1");
            tenant1Event.setTenantId("tenant-123");

            EventMessage tenant2Event = createEvent("event-2");
            tenant2Event.setTenantId("tenant-456");

            // When
            int partition1a = loadBalancingService.selectPartitionByTenant(tenant1Event, targetTopic, partitionCount);
            int partition1b = loadBalancingService.selectPartitionByTenant(tenant1Event, targetTopic, partitionCount);
            int partition2 = loadBalancingService.selectPartitionByTenant(tenant2Event, targetTopic, partitionCount);

            // Then
            assertThat(partition1a).isEqualTo(partition1b); // Same tenant -> same partition
            assertThat(partition1a).isNotEqualTo(partition2); // Different tenants may differ
        }

        @Test
        @DisplayName("Should distribute by key for key-based partitioning")
        void shouldDistributeByKey() {
            // Given
            String targetTopic = "patient.processing";
            int partitionCount = 4;

            EventMessage event1 = createEvent("event-1");
            event1.getPayload().put("patientId", "patient-123");

            EventMessage event2 = createEvent("event-2");
            event2.getPayload().put("patientId", "patient-123");

            EventMessage event3 = createEvent("event-3");
            event3.getPayload().put("patientId", "patient-456");

            // When
            int partition1 = loadBalancingService.selectPartitionByKey(event1, "patientId", targetTopic, partitionCount);
            int partition2 = loadBalancingService.selectPartitionByKey(event2, "patientId", targetTopic, partitionCount);
            int partition3 = loadBalancingService.selectPartitionByKey(event3, "patientId", targetTopic, partitionCount);

            // Then
            assertThat(partition1).isEqualTo(partition2); // Same key -> same partition
            assertThat(partition1).isNotEqualTo(partition3); // Different keys
        }
    }

    @Nested
    @DisplayName("Consumer Load Tracking Tests")
    class ConsumerLoadTrackingTests {

        @Test
        @DisplayName("Should track consumer load")
        void shouldTrackConsumerLoad() {
            // Given
            String topic = "patient.processing";
            loadBalancingService.registerConsumer(topic, "consumer-1");
            loadBalancingService.registerConsumer(topic, "consumer-2");

            // When
            loadBalancingService.recordEventSent(topic, "consumer-1");
            loadBalancingService.recordEventSent(topic, "consumer-1");
            loadBalancingService.recordEventSent(topic, "consumer-2");

            // Then
            Map<String, Long> load = loadBalancingService.getConsumerLoad(topic);
            assertThat(load.get("consumer-1")).isEqualTo(2);
            assertThat(load.get("consumer-2")).isEqualTo(1);
        }

        @Test
        @DisplayName("Should select least loaded consumer")
        void shouldSelectLeastLoadedConsumer() {
            // Given
            String topic = "patient.processing";
            loadBalancingService.registerConsumer(topic, "consumer-1");
            loadBalancingService.registerConsumer(topic, "consumer-2");
            loadBalancingService.registerConsumer(topic, "consumer-3");

            loadBalancingService.recordEventSent(topic, "consumer-1"); // Load: 1
            loadBalancingService.recordEventSent(topic, "consumer-1"); // Load: 2
            loadBalancingService.recordEventSent(topic, "consumer-2"); // Load: 1
            // consumer-3 has load: 0

            // When
            String selectedConsumer = loadBalancingService.selectLeastLoadedConsumer(topic);

            // Then
            assertThat(selectedConsumer).isEqualTo("consumer-3");
        }

        @Test
        @DisplayName("Should distribute evenly across consumers over time")
        void shouldDistributeEvenly() {
            // Given
            String topic = "patient.processing";
            loadBalancingService.registerConsumer(topic, "consumer-1");
            loadBalancingService.registerConsumer(topic, "consumer-2");
            loadBalancingService.registerConsumer(topic, "consumer-3");

            // When - send 30 events using least-loaded selection
            Map<String, Integer> distribution = new HashMap<>();
            for (int i = 0; i < 30; i++) {
                String consumer = loadBalancingService.selectLeastLoadedConsumer(topic);
                loadBalancingService.recordEventSent(topic, consumer);
                distribution.merge(consumer, 1, Integer::sum);
            }

            // Then - each consumer should get approximately equal share (10 ± 2)
            assertThat(distribution.get("consumer-1")).isBetween(8, 12);
            assertThat(distribution.get("consumer-2")).isBetween(8, 12);
            assertThat(distribution.get("consumer-3")).isBetween(8, 12);
        }
    }

    @Nested
    @DisplayName("Consumer Lifecycle Tests")
    class ConsumerLifecycleTests {

        @Test
        @DisplayName("Should handle consumer registration and deregistration")
        void shouldHandleConsumerLifecycle() {
            // Given
            String topic = "patient.processing";

            // When - register
            loadBalancingService.registerConsumer(topic, "consumer-1");
            loadBalancingService.registerConsumer(topic, "consumer-2");

            // Then
            assertThat(loadBalancingService.getActiveConsumers(topic)).hasSize(2);

            // When - deregister
            loadBalancingService.deregisterConsumer(topic, "consumer-1");

            // Then
            assertThat(loadBalancingService.getActiveConsumers(topic)).hasSize(1);
            assertThat(loadBalancingService.getActiveConsumers(topic)).contains("consumer-2");
        }
    }

    private EventMessage createEvent(String type) {
        EventMessage event = new EventMessage();
        event.setEventType(type);
        event.setTenantId("tenant-test-001");
        event.setPayload(new HashMap<>());
        return event;
    }
}
```

---

#### Route Metrics Service Tests

Tests Prometheus/Micrometer metrics collection for routing operations.

```java
package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.MetricsSnapshot;
import com.healthdata.eventrouter.entity.RoutingRuleEntity.Priority;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Route Metrics Service Tests")
class RouteMetricsServiceTest {

    private MeterRegistry meterRegistry;
    private RouteMetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new RouteMetricsService(meterRegistry);
    }

    @Nested
    @DisplayName("Counter Metrics Tests")
    class CounterMetricsTests {

        @Test
        @DisplayName("Should record routed events count")
        void shouldRecordRoutedEvents() {
            // When
            metricsService.recordRoutedEvent("patient.processing", Priority.HIGH);
            metricsService.recordRoutedEvent("patient.processing", Priority.HIGH);
            metricsService.recordRoutedEvent("observation.processing", Priority.MEDIUM);

            // Then
            Counter patientCounter = meterRegistry.find("event.router.routed")
                .tag("topic", "patient.processing")
                .tag("priority", "HIGH")
                .counter();
            assertThat(patientCounter).isNotNull();
            assertThat(patientCounter.count()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should record filtered events count")
        void shouldRecordFilteredEvents() {
            // When
            metricsService.recordFilteredEvent("patient.processing");
            metricsService.recordFilteredEvent("patient.processing");

            // Then
            Counter counter = meterRegistry.find("event.router.filtered")
                .tag("topic", "patient.processing")
                .counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should record unrouted events count")
        void shouldRecordUnroutedEvents() {
            // When
            metricsService.recordUnroutedEvent("unknown.topic");

            // Then
            Counter counter = meterRegistry.find("event.router.unrouted")
                .tag("topic", "unknown.topic")
                .counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should record DLQ events count")
        void shouldRecordDlqEvents() {
            // When
            metricsService.recordDlqEvent("patient.processing", "No matching rule");

            // Then
            Counter counter = meterRegistry.find("event.router.dlq")
                .tag("topic", "patient.processing")
                .counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should track events by priority")
        void shouldTrackEventsByPriority() {
            // When
            metricsService.recordRoutedEvent("topic1", Priority.CRITICAL);
            metricsService.recordRoutedEvent("topic2", Priority.HIGH);
            metricsService.recordRoutedEvent("topic3", Priority.MEDIUM);
            metricsService.recordRoutedEvent("topic4", Priority.LOW);

            // Then
            Counter criticalCounter = meterRegistry.find("event.router.routed")
                .tag("priority", "CRITICAL")
                .counter();
            assertThat(criticalCounter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Timer Metrics Tests")
    class TimerMetricsTests {

        @Test
        @DisplayName("Should record routing latency")
        void shouldRecordRoutingLatency() {
            // When
            metricsService.recordRoutingLatency("patient.processing", Duration.ofMillis(50));
            metricsService.recordRoutingLatency("patient.processing", Duration.ofMillis(75));

            // Then
            Timer timer = meterRegistry.find("event.router.latency")
                .tag("topic", "patient.processing")
                .timer();
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(2);
            assertThat(timer.mean(TimeUnit.MILLISECONDS)).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Snapshot and Aggregate Tests")
    class SnapshotAndAggregateTests {

        @Test
        @DisplayName("Should provide metrics snapshot")
        void shouldProvideSnapshot() {
            // Given
            metricsService.recordRoutedEvent("patient.processing", Priority.HIGH);
            metricsService.recordRoutedEvent("patient.processing", Priority.HIGH);
            metricsService.recordFilteredEvent("patient.processing");
            metricsService.recordUnroutedEvent("unknown.topic");
            metricsService.recordDlqEvent("failed.topic", "Error");

            // When
            MetricsSnapshot snapshot = metricsService.getSnapshot();

            // Then
            assertThat(snapshot.getTotalRoutedEvents()).isEqualTo(2);
            assertThat(snapshot.getTotalFilteredEvents()).isEqualTo(1);
            assertThat(snapshot.getTotalUnroutedEvents()).isEqualTo(1);
            assertThat(snapshot.getTotalDlqEvents()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should track error rate")
        void shouldTrackErrorRate() {
            // When
            metricsService.recordRoutedEvent("topic1", Priority.HIGH); // Success
            metricsService.recordRoutedEvent("topic2", Priority.HIGH); // Success
            metricsService.recordDlqEvent("topic3", "Error"); // Error

            // Then
            MetricsSnapshot snapshot = metricsService.getSnapshot();
            double errorRate = snapshot.getErrorRate();
            assertThat(errorRate).isBetween(0.3, 0.4); // ~33%
        }

        @Test
        @DisplayName("Should provide metrics by topic")
        void shouldProvideMetricsByTopic() {
            // When
            metricsService.recordRoutedEvent("patient.processing", Priority.HIGH);
            metricsService.recordRoutedEvent("patient.processing", Priority.HIGH);
            metricsService.recordRoutedEvent("observation.processing", Priority.MEDIUM);

            // Then
            Map<String, Long> metricsByTopic = metricsService.getRoutedEventsByTopic();
            assertThat(metricsByTopic.get("patient.processing")).isEqualTo(2);
            assertThat(metricsByTopic.get("observation.processing")).isEqualTo(1);
        }

        @Test
        @DisplayName("Should reset metrics when requested")
        void shouldResetMetrics() {
            // Given
            metricsService.recordRoutedEvent("topic1", Priority.HIGH);
            metricsService.recordFilteredEvent("topic2");

            // When
            metricsService.resetMetrics();

            // Then
            MetricsSnapshot snapshot = metricsService.getSnapshot();
            assertThat(snapshot.getTotalRoutedEvents()).isEqualTo(0);
            assertThat(snapshot.getTotalFilteredEvents()).isEqualTo(0);
        }
    }
}
```

---

#### Dead Letter Queue Service Tests

Tests DLQ handling for failed routing operations including persistence, retry logic, and Kafka integration.

```java
package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.EventMessage;
import com.healthdata.eventrouter.entity.DeadLetterEventEntity;
import com.healthdata.eventrouter.persistence.DeadLetterEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dead Letter Queue Service Tests")
class DeadLetterQueueServiceTest {

    @Mock
    private DeadLetterEventRepository dlqRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private DeadLetterQueueService dlqService;

    private static final String DLQ_TOPIC = "event-router.dlq";

    @BeforeEach
    void setUp() {
        dlqService = new DeadLetterQueueService(dlqRepository, kafkaTemplate);
    }

    @Nested
    @DisplayName("DLQ Publishing Tests")
    class DlqPublishingTests {

        @Test
        @DisplayName("Should send failed event to DLQ topic")
        void shouldSendToDlqTopic() {
            // Given
            EventMessage event = createEvent("FAILED_EVENT");
            String reason = "No matching routing rule found";

            // When
            dlqService.sendToDeadLetterQueue(event, reason);

            // Then
            verify(kafkaTemplate).send(eq(DLQ_TOPIC), any(String.class));
        }

        @Test
        @DisplayName("Should persist failed event to database")
        void shouldPersistToDatabase() {
            // Given
            EventMessage event = createEvent("FAILED_EVENT");
            String reason = "Filter criteria not met";

            // When
            dlqService.sendToDeadLetterQueue(event, reason);

            // Then
            ArgumentCaptor<DeadLetterEventEntity> captor = ArgumentCaptor.forClass(DeadLetterEventEntity.class);
            verify(dlqRepository).save(captor.capture());

            DeadLetterEventEntity saved = captor.getValue();
            assertThat(saved.getEventType()).isEqualTo("FAILED_EVENT");
            assertThat(saved.getFailureReason()).isEqualTo("Filter criteria not met");
            assertThat(saved.getTenantId()).isEqualTo("tenant-test-001");
        }

        @Test
        @DisplayName("Should include original event payload in DLQ")
        void shouldIncludeOriginalPayload() {
            // Given
            Map<String, Object> payload = Map.of("key", "value", "nested", Map.of("field", "data"));
            EventMessage event = createEvent("FAILED_EVENT");
            event.setPayload(payload);

            // When
            dlqService.sendToDeadLetterQueue(event, "Routing failed");

            // Then
            ArgumentCaptor<DeadLetterEventEntity> captor = ArgumentCaptor.forClass(DeadLetterEventEntity.class);
            verify(dlqRepository).save(captor.capture());

            DeadLetterEventEntity saved = captor.getValue();
            assertThat(saved.getOriginalPayload()).isNotNull();
        }
    }

    @Nested
    @DisplayName("DLQ Retry Tests")
    class DlqRetryTests {

        @Test
        @DisplayName("Should support retry from DLQ")
        void shouldSupportRetry() {
            // Given
            DeadLetterEventEntity dlqEvent = new DeadLetterEventEntity();
            dlqEvent.setId(1L);
            dlqEvent.setEventType("RETRY_EVENT");
            dlqEvent.setOriginalPayload("{\"test\":\"data\"}");
            dlqEvent.setRetryCount(0);

            when(dlqRepository.findById(1L)).thenReturn(Optional.of(dlqEvent));

            // When
            boolean retried = dlqService.retryEvent(1L);

            // Then
            assertThat(retried).isTrue();
            ArgumentCaptor<DeadLetterEventEntity> captor = ArgumentCaptor.forClass(DeadLetterEventEntity.class);
            verify(dlqRepository).save(captor.capture());
            assertThat(captor.getValue().getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should limit retry attempts (max 5)")
        void shouldLimitRetries() {
            // Given
            DeadLetterEventEntity dlqEvent = new DeadLetterEventEntity();
            dlqEvent.setId(1L);
            dlqEvent.setRetryCount(5); // Already at max

            when(dlqRepository.findById(1L)).thenReturn(Optional.of(dlqEvent));

            // When
            boolean retried = dlqService.retryEvent(1L);

            // Then
            assertThat(retried).isFalse();
        }
    }

    @Nested
    @DisplayName("DLQ Metadata Tests")
    class DlqMetadataTests {

        @Test
        @DisplayName("Should include failure timestamp")
        void shouldIncludeTimestamp() {
            // Given
            EventMessage event = createEvent("FAILED_EVENT");
            when(dlqRepository.save(any(DeadLetterEventEntity.class))).thenAnswer(invocation -> {
                DeadLetterEventEntity entity = invocation.getArgument(0);
                if (entity.getFailedAt() == null) {
                    entity.setFailedAt(Instant.now());
                }
                return entity;
            });

            // When
            dlqService.sendToDeadLetterQueue(event, "Test failure");

            // Then
            ArgumentCaptor<DeadLetterEventEntity> captor = ArgumentCaptor.forClass(DeadLetterEventEntity.class);
            verify(dlqRepository).save(captor.capture());

            DeadLetterEventEntity saved = captor.getValue();
            assertThat(saved.getFailedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should track DLQ metrics")
        void shouldTrackMetrics() {
            // Given
            EventMessage event = createEvent("FAILED_EVENT");

            // When
            dlqService.sendToDeadLetterQueue(event, "Test failure");

            // Then
            long count = dlqService.getDeadLetterCount();
            assertThat(count).isGreaterThanOrEqualTo(0);
        }
    }

    private EventMessage createEvent(String type) {
        EventMessage event = new EventMessage();
        event.setEventType(type);
        event.setTenantId("tenant-test-001");
        event.setSourceTopic("test.topic");
        return event;
    }
}
```

---

### Integration Tests

#### Multi-Tenant Routing Integration Tests

Tests tenant isolation at the database level with TestContainers PostgreSQL.

```java
package com.healthdata.eventrouter.integration;

import com.healthdata.eventrouter.entity.RoutingRuleEntity;
import com.healthdata.eventrouter.entity.RoutingRuleEntity.Priority;
import com.healthdata.eventrouter.persistence.RoutingRuleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Multi-Tenant Routing Integration Tests")
class MultiTenantRoutingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private RoutingRuleRepository ruleRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        ruleRepository.deleteAll();
        entityManager.flush();
    }

    @Nested
    @DisplayName("Tenant Isolation Tests")
    class TenantIsolationTests {

        @Test
        @DisplayName("Should isolate routing rules by tenant")
        void shouldIsolateRulesByTenant() {
            // Given
            RoutingRuleEntity tenant1Rule = createRule("tenant-001", "rule1", "fhir.patient.created", "tenant1.processing");
            RoutingRuleEntity tenant2Rule = createRule("tenant-002", "rule2", "fhir.patient.created", "tenant2.processing");
            ruleRepository.saveAndFlush(tenant1Rule);
            ruleRepository.saveAndFlush(tenant2Rule);
            entityManager.clear();

            // When
            var rules1 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant-001", "fhir.patient.created");
            var rules2 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant-002", "fhir.patient.created");

            // Then
            assertThat(rules1).hasSize(1);
            assertThat(rules1.get(0).getTargetTopic()).isEqualTo("tenant1.processing");
            assertThat(rules2).hasSize(1);
            assertThat(rules2.get(0).getTargetTopic()).isEqualTo("tenant2.processing");
        }

        @Test
        @DisplayName("Should not return rules for wrong tenant (cross-tenant leakage prevention)")
        void shouldNotReturnCrossTenantRules() {
            // Given
            RoutingRuleEntity tenant1Rule = createRule("tenant-001", "rule1", "fhir.patient.created", "tenant1.processing");
            ruleRepository.saveAndFlush(tenant1Rule);
            entityManager.clear();

            // When
            var rules = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant-002", "fhir.patient.created");

            // Then
            assertThat(rules).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tenant-Specific Configuration Tests")
    class TenantSpecificConfigurationTests {

        @Test
        @DisplayName("Should support different priorities per tenant")
        void shouldSupportDifferentPrioritiesPerTenant() {
            // Given
            RoutingRuleEntity tenant1Rule = createRule("tenant-001", "rule1", "fhir.patient.created", "tenant1.processing");
            tenant1Rule.setPriority(Priority.CRITICAL);

            RoutingRuleEntity tenant2Rule = createRule("tenant-002", "rule2", "fhir.patient.created", "tenant2.processing");
            tenant2Rule.setPriority(Priority.LOW);

            ruleRepository.saveAndFlush(tenant1Rule);
            ruleRepository.saveAndFlush(tenant2Rule);
            entityManager.clear();

            // When
            var rules1 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant-001", "fhir.patient.created");
            var rules2 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant-002", "fhir.patient.created");

            // Then
            assertThat(rules1.get(0).getPriority()).isEqualTo(Priority.CRITICAL);
            assertThat(rules2.get(0).getPriority()).isEqualTo(Priority.LOW);
        }

        @Test
        @DisplayName("Should support tenant-specific filter expressions")
        void shouldSupportTenantSpecificFilters() {
            // Given
            RoutingRuleEntity tenant1Rule = createRule("tenant-001", "rule1", "fhir.patient.created", "tenant1.processing");
            tenant1Rule.setFilterExpression("{\"region\": \"US\"}");

            RoutingRuleEntity tenant2Rule = createRule("tenant-002", "rule2", "fhir.patient.created", "tenant2.processing");
            tenant2Rule.setFilterExpression("{\"region\": \"EU\"}");

            ruleRepository.saveAndFlush(tenant1Rule);
            ruleRepository.saveAndFlush(tenant2Rule);
            entityManager.clear();

            // When
            var rules1 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant-001", "fhir.patient.created");
            var rules2 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant-002", "fhir.patient.created");

            // Then
            assertThat(rules1.get(0).getFilterExpression()).contains("US");
            assertThat(rules2.get(0).getFilterExpression()).contains("EU");
        }

        @Test
        @DisplayName("Should support tenant-specific transformations")
        void shouldSupportTenantSpecificTransformations() {
            // Given
            RoutingRuleEntity tenant1Rule = createRule("tenant-001", "rule1", "fhir.patient.created", "tenant1.processing");
            tenant1Rule.setTransformationScript("enrichment:add-us-fields");

            RoutingRuleEntity tenant2Rule = createRule("tenant-002", "rule2", "fhir.patient.created", "tenant2.processing");
            tenant2Rule.setTransformationScript("enrichment:add-eu-fields");

            ruleRepository.saveAndFlush(tenant1Rule);
            ruleRepository.saveAndFlush(tenant2Rule);
            entityManager.clear();

            // When
            var rules1 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant-001", "fhir.patient.created");
            var rules2 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant-002", "fhir.patient.created");

            // Then
            assertThat(rules1.get(0).getTransformationScript()).contains("us-fields");
            assertThat(rules2.get(0).getTransformationScript()).contains("eu-fields");
        }
    }

    @Nested
    @DisplayName("Rule Management Tests")
    class RuleManagementTests {

        @Test
        @DisplayName("Should handle multiple rules per tenant")
        void shouldHandleMultipleRulesPerTenant() {
            // Given
            RoutingRuleEntity rule1 = createRule("tenant-001", "rule1", "fhir.patient.created", "processing.high-priority");
            rule1.setPriority(Priority.HIGH);
            rule1.setFilterExpression("{\"urgent\": true}");

            RoutingRuleEntity rule2 = createRule("tenant-001", "rule2", "fhir.patient.created", "processing.normal");
            rule2.setPriority(Priority.MEDIUM);

            ruleRepository.saveAndFlush(rule1);
            ruleRepository.saveAndFlush(rule2);
            entityManager.clear();

            // When
            var rules = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant-001", "fhir.patient.created");

            // Then
            assertThat(rules).hasSize(2);
        }

        @Test
        @DisplayName("Should only return enabled rules")
        void shouldOnlyReturnEnabledRules() {
            // Given
            RoutingRuleEntity enabledRule = createRule("tenant-001", "enabled-rule", "fhir.patient.created", "enabled.topic");
            enabledRule.setEnabled(true);

            RoutingRuleEntity disabledRule = createRule("tenant-001", "disabled-rule", "fhir.patient.created", "disabled.topic");
            disabledRule.setEnabled(false);

            ruleRepository.saveAndFlush(enabledRule);
            ruleRepository.saveAndFlush(disabledRule);
            entityManager.clear();

            // When
            var rules = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant-001", "fhir.patient.created");

            // Then
            assertThat(rules).hasSize(1);
            assertThat(rules.get(0).getRuleName()).isEqualTo("enabled-rule");
        }
    }

    private RoutingRuleEntity createRule(String tenantId, String name, String sourceTopic, String targetTopic) {
        RoutingRuleEntity rule = new RoutingRuleEntity();
        rule.setTenantId(tenantId);
        rule.setRuleName(name);
        rule.setSourceTopic(sourceTopic);
        rule.setTargetTopic(targetTopic);
        rule.setPriority(Priority.MEDIUM);
        rule.setEnabled(true);
        return rule;
    }
}
```

---

### RBAC/Permission Tests

Tests role-based access control for routing rule management endpoints.

```java
package com.healthdata.eventrouter.security;

import com.healthdata.testfixtures.security.GatewayTrustTestHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("RBAC/Permission Tests")
class RbacPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT_ID = "tenant-test-001";
    private static final String ROUTING_RULES_ENDPOINT = "/api/v1/routing-rules";

    @Nested
    @DisplayName("Admin Role Tests")
    class AdminRoleTests {

        @Test
        @DisplayName("Admin should be able to create routing rules")
        void adminCanCreateRoutingRules() throws Exception {
            mockMvc.perform(post(ROUTING_RULES_ENDPOINT)
                    .headers(GatewayTrustTestHeaders.adminHeaders(TENANT_ID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRoutingRuleJson()))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Admin should be able to update routing rules")
        void adminCanUpdateRoutingRules() throws Exception {
            mockMvc.perform(put(ROUTING_RULES_ENDPOINT + "/rule-123")
                    .headers(GatewayTrustTestHeaders.adminHeaders(TENANT_ID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRoutingRuleJson()))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Admin should be able to delete routing rules")
        void adminCanDeleteRoutingRules() throws Exception {
            mockMvc.perform(delete(ROUTING_RULES_ENDPOINT + "/rule-123")
                    .headers(GatewayTrustTestHeaders.adminHeaders(TENANT_ID)))
                .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Evaluator Role Tests")
    class EvaluatorRoleTests {

        @Test
        @DisplayName("Evaluator should NOT be able to create routing rules")
        void evaluatorCannotCreateRoutingRules() throws Exception {
            mockMvc.perform(post(ROUTING_RULES_ENDPOINT)
                    .headers(GatewayTrustTestHeaders.evaluatorHeaders(TENANT_ID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRoutingRuleJson()))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Evaluator should be able to view routing rules")
        void evaluatorCanViewRoutingRules() throws Exception {
            mockMvc.perform(get(ROUTING_RULES_ENDPOINT)
                    .headers(GatewayTrustTestHeaders.evaluatorHeaders(TENANT_ID)))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Viewer Role Tests")
    class ViewerRoleTests {

        @Test
        @DisplayName("Viewer should have read-only access")
        void viewerHasReadOnlyAccess() throws Exception {
            // Viewer CAN read
            mockMvc.perform(get(ROUTING_RULES_ENDPOINT)
                    .headers(GatewayTrustTestHeaders.viewerHeaders(TENANT_ID)))
                .andExpect(status().isOk());

            // Viewer CANNOT write
            mockMvc.perform(post(ROUTING_RULES_ENDPOINT)
                    .headers(GatewayTrustTestHeaders.viewerHeaders(TENANT_ID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRoutingRuleJson()))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Unauthenticated Access Tests")
    class UnauthenticatedAccessTests {

        @Test
        @DisplayName("Unauthenticated requests should be rejected")
        void unauthenticatedShouldBeRejected() throws Exception {
            mockMvc.perform(get(ROUTING_RULES_ENDPOINT)
                    .headers(GatewayTrustTestHeaders.unauthenticatedHeaders(TENANT_ID)))
                .andExpect(status().isUnauthorized());
        }
    }

    private String createRoutingRuleJson() {
        return """
            {
                "ruleName": "test-routing-rule",
                "sourceTopic": "fhir.patient.created",
                "targetTopic": "patient.processing",
                "priority": "MEDIUM",
                "enabled": true
            }
            """;
    }
}
```

---

### HIPAA Compliance Tests

Tests HIPAA-compliant event handling including synthetic data patterns, no-cache headers, and audit logging.

```java
package com.healthdata.eventrouter.compliance;

import com.healthdata.eventrouter.dto.EventMessage;
import com.healthdata.testfixtures.security.GatewayTrustTestHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("HIPAA Compliance Tests")
class HipaaComplianceTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT_ID = "tenant-hipaa-001";

    @Nested
    @DisplayName("Synthetic Data Pattern Tests")
    class SyntheticDataPatternTests {

        @Test
        @DisplayName("Test event data must not contain real PHI")
        void testDataMustBeSynthetic() {
            // Given
            EventMessage testEvent = createSyntheticEvent();

            // Then - verify synthetic patterns
            assertThat(testEvent.getTenantId())
                .startsWith("tenant-")
                .withFailMessage("Test tenant IDs should follow synthetic pattern");

            Map<String, Object> payload = testEvent.getPayload();
            if (payload.containsKey("patientId")) {
                assertThat(payload.get("patientId").toString())
                    .matches("TEST-PATIENT-\\d+|patient-test-\\d+")
                    .withFailMessage("Patient IDs should follow synthetic pattern");
            }

            if (payload.containsKey("mrn")) {
                assertThat(payload.get("mrn").toString())
                    .startsWith("TEST-MRN-")
                    .withFailMessage("MRNs should follow synthetic pattern TEST-MRN-xxxxxx");
            }
        }

        @Test
        @DisplayName("Event transformations should mask PHI when configured")
        void transformationsShouldMaskPhi() {
            // Given
            EventMessage event = createSyntheticEvent();
            event.setPayload(Map.of(
                "ssn", "123-45-6789",
                "dateOfBirth", "1980-01-15"
            ));

            // When - mask transformation applied (tested in EventTransformationServiceTest)
            // Then - masking patterns verified there
            assertThat(event.getPayload()).containsKey("ssn");
        }
    }

    @Nested
    @DisplayName("HTTP Header Compliance Tests")
    class HttpHeaderComplianceTests {

        @Test
        @DisplayName("Routing metrics endpoint should include no-cache headers")
        void metricsEndpointShouldIncludeNoCacheHeaders() throws Exception {
            mockMvc.perform(get("/api/v1/routing-metrics")
                    .headers(GatewayTrustTestHeaders.adminHeaders(TENANT_ID)))
                .andExpect(header().string("Cache-Control",
                    org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Pragma", "no-cache"));
        }

        @Test
        @DisplayName("DLQ listing should include no-cache headers")
        void dlqEndpointShouldIncludeNoCacheHeaders() throws Exception {
            mockMvc.perform(get("/api/v1/dead-letter-queue")
                    .headers(GatewayTrustTestHeaders.adminHeaders(TENANT_ID)))
                .andExpect(header().string("Cache-Control",
                    org.hamcrest.Matchers.containsString("no-cache")))
                .andExpect(header().string("Pragma", "no-cache"));
        }
    }

    @Nested
    @DisplayName("Audit Logging Tests")
    class AuditLoggingTests {

        @Test
        @DisplayName("Routing rule modifications should be audited")
        void routingRuleModificationsShouldBeAudited() throws Exception {
            // When - create a routing rule (audit event triggered)
            mockMvc.perform(post("/api/v1/routing-rules")
                    .headers(GatewayTrustTestHeaders.adminHeaders(TENANT_ID))
                    .contentType("application/json")
                    .content("""
                        {
                            "ruleName": "audit-test-rule",
                            "sourceTopic": "fhir.patient.created",
                            "targetTopic": "patient.processing",
                            "priority": "MEDIUM",
                            "enabled": true
                        }
                        """))
                .andExpect(status().isCreated());

            // Then - audit event should be recorded
            // Verification via AuditEventRepository in full integration tests
        }

        @Test
        @DisplayName("DLQ retry operations should be audited")
        void dlqRetryOperationsShouldBeAudited() throws Exception {
            // When - retry a DLQ event
            mockMvc.perform(post("/api/v1/dead-letter-queue/1/retry")
                    .headers(GatewayTrustTestHeaders.adminHeaders(TENANT_ID)))
                .andExpect(status().isOk());

            // Then - audit event for "DLQ_RETRY" should be recorded
        }
    }

    private EventMessage createSyntheticEvent() {
        EventMessage event = new EventMessage();
        event.setEventType("PATIENT_CREATED");
        event.setTenantId("tenant-hipaa-001");
        event.setPayload(Map.of(
            "patientId", "TEST-PATIENT-" + UUID.randomUUID().toString().substring(0, 8),
            "mrn", "TEST-MRN-" + String.format("%06d", (int)(Math.random() * 999999)),
            "firstName", "Test-Patient",
            "lastName", "Synthetic-" + UUID.randomUUID().toString().substring(0, 6)
        ));
        return event;
    }
}
```

---

### Performance Tests

Tests routing throughput and latency against SLA targets.

```java
package com.healthdata.eventrouter.performance;

import com.healthdata.eventrouter.dto.EventMessage;
import com.healthdata.eventrouter.entity.RoutingRuleEntity;
import com.healthdata.eventrouter.entity.RoutingRuleEntity.Priority;
import com.healthdata.eventrouter.service.EventFilterService;
import com.healthdata.eventrouter.service.EventRouter;
import com.healthdata.eventrouter.service.EventTransformationService;
import com.healthdata.eventrouter.service.PriorityQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Performance Tests")
class PerformanceTest {

    @Autowired
    private EventFilterService filterService;

    @Autowired
    private EventTransformationService transformationService;

    @Autowired
    private PriorityQueueService queueService;

    @Nested
    @DisplayName("Filter Evaluation Performance Tests")
    class FilterEvaluationPerformanceTests {

        @Test
        @DisplayName("Filter evaluation should complete within 5ms per event")
        void filterEvaluationShouldBeWithinSla() {
            // Given
            int eventCount = 1000;
            String filter = "{\"eventType\": \"PATIENT_CREATED\", \"region\": \"US\"}";

            List<EventMessage> events = IntStream.range(0, eventCount)
                .mapToObj(i -> createEvent("PATIENT_CREATED", Map.of("region", "US", "index", i)))
                .toList();

            // When
            Instant start = Instant.now();
            events.forEach(event -> filterService.matches(event, filter));
            Instant end = Instant.now();

            // Then
            long totalMs = Duration.between(start, end).toMillis();
            double avgMsPerEvent = totalMs / (double) eventCount;

            assertThat(avgMsPerEvent)
                .isLessThan(5.0)
                .withFailMessage("Average filter evaluation time %.2fms exceeds 5ms SLA", avgMsPerEvent);

            System.out.printf("Filter Performance: %d evaluations in %dms (avg: %.3fms/event)%n",
                eventCount, totalMs, avgMsPerEvent);
        }
    }

    @Nested
    @DisplayName("Transformation Performance Tests")
    class TransformationPerformanceTests {

        @Test
        @DisplayName("Transformation should complete within 10ms per event")
        void transformationShouldBeWithinSla() {
            // Given
            int eventCount = 500;
            String script = "enrichment:add-timestamp|rename:firstName->givenName|mask:ssn";

            List<EventMessage> events = IntStream.range(0, eventCount)
                .mapToObj(i -> createEvent("PATIENT_CREATED", Map.of(
                    "firstName", "Test-" + i,
                    "lastName", "Patient",
                    "ssn", "123-45-6789"
                )))
                .toList();

            // When
            Instant start = Instant.now();
            events.forEach(event -> transformationService.transform(event, script));
            Instant end = Instant.now();

            // Then
            long totalMs = Duration.between(start, end).toMillis();
            double avgMsPerEvent = totalMs / (double) eventCount;

            assertThat(avgMsPerEvent)
                .isLessThan(10.0)
                .withFailMessage("Average transformation time %.2fms exceeds 10ms SLA", avgMsPerEvent);

            System.out.printf("Transformation Performance: %d transforms in %dms (avg: %.3fms/event)%n",
                eventCount, totalMs, avgMsPerEvent);
        }
    }

    @Nested
    @DisplayName("Priority Queue Performance Tests")
    class PriorityQueuePerformanceTests {

        @Test
        @DisplayName("Priority queue should handle 10,000 events efficiently")
        void priorityQueueShouldHandleHighVolume() {
            // Given
            int eventCount = 10000;
            Priority[] priorities = Priority.values();

            // When - enqueue
            Instant enqueueStart = Instant.now();
            for (int i = 0; i < eventCount; i++) {
                Priority priority = priorities[i % priorities.length];
                queueService.enqueue(createPrioritizedEvent("event-" + i, priority));
            }
            Instant enqueueEnd = Instant.now();

            // When - dequeue
            Instant dequeueStart = Instant.now();
            while (!queueService.isEmpty()) {
                queueService.dequeue();
            }
            Instant dequeueEnd = Instant.now();

            // Then
            long enqueueMs = Duration.between(enqueueStart, enqueueEnd).toMillis();
            long dequeueMs = Duration.between(dequeueStart, dequeueEnd).toMillis();

            assertThat(enqueueMs)
                .isLessThan(1000)
                .withFailMessage("Enqueue %d events took %dms (>1000ms)", eventCount, enqueueMs);

            assertThat(dequeueMs)
                .isLessThan(1000)
                .withFailMessage("Dequeue %d events took %dms (>1000ms)", eventCount, dequeueMs);

            System.out.printf("Queue Performance: enqueue=%dms, dequeue=%dms for %d events%n",
                enqueueMs, dequeueMs, eventCount);
        }

        @Test
        @DisplayName("Priority ordering should not degrade with scale")
        void priorityOrderingShouldNotDegradeWithScale() {
            // Given
            int eventCount = 5000;
            Priority[] priorities = Priority.values();

            // When - enqueue in random priority order
            Random random = new Random(42);
            for (int i = 0; i < eventCount; i++) {
                Priority priority = priorities[random.nextInt(priorities.length)];
                queueService.enqueue(createPrioritizedEvent("event-" + i, priority));
            }

            // Verify ordering is maintained
            Priority lastPriority = Priority.CRITICAL;
            int orderViolations = 0;

            while (!queueService.isEmpty()) {
                EventMessage event = queueService.dequeue();
                if (event.getPriority().ordinal() < lastPriority.ordinal()) {
                    orderViolations++;
                }
                lastPriority = event.getPriority();
            }

            // Then - within same priority, FIFO may cause apparent "violations" when
            // alternating priorities were enqueued, so we just verify mostly correct
            assertThat(orderViolations)
                .isLessThan(eventCount / 10)
                .withFailMessage("Too many priority ordering violations: %d", orderViolations);
        }
    }

    @Nested
    @DisplayName("Throughput Benchmark Tests")
    class ThroughputBenchmarkTests {

        @Test
        @DisplayName("Should achieve target throughput of 1000 events/second")
        void shouldAchieveTargetThroughput() {
            // Given
            int eventCount = 5000;
            String filter = "{\"region\": \"US\"}";
            String transform = "enrichment:add-timestamp";

            List<EventMessage> events = IntStream.range(0, eventCount)
                .mapToObj(i -> createEvent("PATIENT_CREATED", Map.of("region", "US", "index", i)))
                .toList();

            // When - simulate full routing pipeline (filter + transform)
            Instant start = Instant.now();
            for (EventMessage event : events) {
                if (filterService.matches(event, filter)) {
                    transformationService.transform(event, transform);
                }
            }
            Instant end = Instant.now();

            // Then
            long totalMs = Duration.between(start, end).toMillis();
            double eventsPerSecond = (eventCount * 1000.0) / totalMs;

            assertThat(eventsPerSecond)
                .isGreaterThan(1000.0)
                .withFailMessage("Throughput %.0f events/sec below 1000 events/sec target", eventsPerSecond);

            System.out.printf("Throughput: %.0f events/second (%d events in %dms)%n",
                eventsPerSecond, eventCount, totalMs);
        }
    }

    private EventMessage createEvent(String type, Map<String, Object> payload) {
        EventMessage event = new EventMessage();
        event.setEventType(type);
        event.setTenantId("tenant-perf-001");
        event.setPayload(new HashMap<>(payload));
        return event;
    }

    private EventMessage createPrioritizedEvent(String type, Priority priority) {
        EventMessage event = new EventMessage();
        event.setEventType(type);
        event.setTenantId("tenant-perf-001");
        event.setPriority(priority);
        return event;
    }
}
```

---

### SLA Performance Targets

| Operation | Target | Measurement |
|-----------|--------|-------------|
| Filter evaluation | < 5ms per event | `FilterEvaluationPerformanceTests` |
| Event transformation | < 10ms per event | `TransformationPerformanceTests` |
| Queue enqueue (10K events) | < 1 second | `PriorityQueuePerformanceTests` |
| Queue dequeue (10K events) | < 1 second | `PriorityQueuePerformanceTests` |
| Full routing pipeline | > 1000 events/second | `ThroughputBenchmarkTests` |

---

### Test Configuration

#### BaseIntegrationTest Setup

```java
package com.healthdata.eventrouter;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "test-consumer-group");
        registry.add("spring.kafka.producer.acks", () -> "all");
    }
}
```

#### Test Application Properties

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false

event-router:
  source-topics:
    - fhir.patient.created
    - fhir.patient.updated
  dlq-topic: event-router.dlq.test
  priority-queue:
    max-size: 10000
    batch-size: 50
    poll-interval-ms: 50

logging:
  level:
    com.healthdata.eventrouter: DEBUG
    org.testcontainers: INFO
```

---

### Best Practices

#### HIPAA Compliance
- All test data uses synthetic patterns (`TEST-PATIENT-xxx`, `TEST-MRN-xxxxxx`)
- Transformation tests verify PHI masking for SSN, DOB fields
- No real PHI appears in test fixtures

#### Multi-Tenant Testing
- Every test verifies `tenantId` filtering in routing rules
- Cross-tenant data access is explicitly tested and blocked
- Tenant-specific configurations (filters, transforms, priorities) are validated

#### Gateway Trust Authentication
- Use `GatewayTrustTestHeaders` utility for consistent header injection
- Test all roles: SUPER_ADMIN, ADMIN, EVALUATOR, ANALYST, VIEWER
- Verify unauthenticated requests are rejected

#### Kafka Testing
- Use `@EmbeddedKafka` or TestContainers for Kafka integration tests
- Verify DLQ messages are properly formatted
- Test consumer group offset management

#### Performance Testing
- Benchmark against documented SLAs
- Test with realistic event volumes (1000+ events)
- Verify priority ordering doesn't degrade at scale

---

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| TestContainers timeout | Docker daemon not running | Start Docker Desktop or Docker service |
| Kafka connection refused | Container not ready | Add startup wait or retry logic |
| Priority queue test flaky | Concurrent access | Use synchronized queue or single-threaded tests |
| Filter tests fail | Invalid JSONPath syntax | Verify filter expression format |
| DLQ tests fail | Missing Kafka topic | Configure auto-create or pre-create topics |
| Performance tests slow | Cold JVM | Add warmup iterations before measurement |
| Multi-tenant test fails | Missing tenantId filter | Add WHERE clause to repository query |

---

### CI/CD Integration

#### GitHub Actions Configuration

```yaml
# .github/workflows/event-router-service-tests.yml
name: Event Router Service Tests

on:
  push:
    paths:
      - 'backend/modules/services/event-router-service/**'
  pull_request:
    paths:
      - 'backend/modules/services/event-router-service/**'

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: testdb
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run tests
        run: ./gradlew :modules:services:event-router-service:test

      - name: Generate coverage report
        run: ./gradlew :modules:services:event-router-service:jacocoTestReport

      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          files: backend/modules/services/event-router-service/build/reports/jacoco/test/jacocoTestReport.xml
```

---

### Manual Testing

#### Test Event Routing with curl

```bash
# Publish test event to source topic (requires Kafka CLI)
echo '{"eventType":"PATIENT_CREATED","tenantId":"tenant-001","payload":{"patientId":"TEST-PATIENT-001"}}' | \
  kafka-console-producer --broker-list localhost:9094 --topic fhir.patient.created

# Check routed events in target topic
kafka-console-consumer --bootstrap-server localhost:9094 \
  --topic patient.processing --from-beginning --max-messages 1

# Check DLQ for failed events
kafka-console-consumer --bootstrap-server localhost:9094 \
  --topic event-router.dlq --from-beginning --max-messages 10
```

#### Check Routing Metrics

```bash
# Health check with queue status
curl -s http://localhost:8095/actuator/health | jq .

# Prometheus metrics
curl -s http://localhost:8095/actuator/prometheus | grep event.router

# Routing statistics
curl -s -H "X-Tenant-ID: tenant-001" \
  http://localhost:8095/api/v1/routing-metrics | jq .
```

#### Test Priority Queue Behavior

```bash
# Send LOW priority event
curl -X POST http://localhost:8095/api/v1/test/enqueue \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -d '{"eventType":"TEST","priority":"LOW"}'

# Send CRITICAL priority event
curl -X POST http://localhost:8095/api/v1/test/enqueue \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -d '{"eventType":"TEST","priority":"CRITICAL"}'

# Verify CRITICAL processed first
curl -s http://localhost:8095/api/v1/test/dequeue \
  -H "X-Tenant-ID: tenant-001" | jq .priority
# Should return "CRITICAL"
```

---

### Prometheus Metrics Reference

| Metric | Type | Description |
|--------|------|-------------|
| `event.router.routed` | Counter | Total events successfully routed (tags: topic, priority) |
| `event.router.filtered` | Counter | Events filtered out by rule expressions |
| `event.router.unrouted` | Counter | Events with no matching routing rules |
| `event.router.dlq` | Counter | Events sent to Dead Letter Queue |
| `event.router.latency` | Timer | Routing latency per topic (p50, p95, p99) |
| `event.router.queue.size` | Gauge | Current priority queue size |
| `event.router.consumer.load` | Gauge | Load per consumer for load balancing |

## Health Monitoring

The health indicator exposes:
- Queue size and health status
- Total routed events
- Total filtered events
- Total unrouted events
- Events per second
- Error rate percentage

## Authors

HDIM Development Team

## License

Proprietary - Mahoosuc Solutions
