# Apache Kafka Event Streaming - Asynchronous Communication Guide

> **This is a comprehensive guide for event-driven messaging across HDIM microservices.**
> **Apache Kafka enables loosely coupled, scalable, asynchronous service-to-service communication.**

---

## Overview

### What is This Skill?

Apache Kafka is a distributed event streaming platform. Services publish events to topics; other services subscribe and react asynchronously. Kafka guarantees message ordering and durability—critical for healthcare event workflows.

**Example:**
- Patient Service publishes `PatientCreated` event
- Quality Measure Service subscribes, evaluates measures
- Care Gap Service subscribes, detects gaps
- All without direct service-to-service calls

### Why is This Important for HDIM?

Healthcare systems have complex workflows spanning multiple services. Synchronous APIs (Feign) create tight coupling and cascading failures:

**Problem (Synchronous):**
```
Patient Service → Quality Service → Care Gap Service
   ✅ Patient created
                       ❌ Quality Service down
                       → Creates failure cascade
```

**Solution (Asynchronous with Kafka):**
```
Patient Service → Kafka Topic: patient.events
                      ↓
            [Quality Service subscribes]
            [Care Gap Service subscribes]
            [Notification Service subscribes]

Each service processes independently—if one is down, others continue.
```

### Business Impact

- **Resilience:** Service failures don't cascade; other services continue processing
- **Scalability:** Handle high event volumes; consumers process at their own pace
- **Auditability:** Event log provides complete history for compliance
- **Decoupling:** Services work independently; easier to test and deploy
- **Order Guarantee:** Process events in predictable order within partition

### Key Services Using Kafka

All HDIM event services use Kafka:
- Patient Event Service - Publishes patient lifecycle events
- Quality Measure Event Service - Publishes measure evaluation events
- Care Gap Event Service - Publishes gap detection events
- Clinical Workflow Event Service - Publishes workflow events
- Notification Service - Consumes all events; sends alerts

### Estimated Learning Time

1-2 weeks (hands-on event producer/consumer implementation)

---

## Key Concepts

### Concept 1: Topics, Producers, Consumers

**Topic:** Named event stream (like a broadcast channel)
```
Topic: patient.events
├─ Partition 0: [PatientCreated, PatientUpdated, PatientCreated, ...]
├─ Partition 1: [PatientCreated, PatientDeleted, ...]
└─ Partition 2: [PatientUpdated, ...]
```

**Producer:** Service that publishes events to topic
```java
KafkaTemplate<String, PatientEvent> kafkaTemplate;
kafkaTemplate.send("patient.events", tenantId, event);
```

**Consumer:** Service that subscribes to topic and processes events
```java
@KafkaListener(topics = "patient.events", groupId = "quality-service")
public void handlePatientEvent(PatientEvent event) {
    // Process event
}
```

### Concept 2: Partitions & Consumer Groups

**Partitions:** Enable parallel processing within topic
```
Topic: patient.events (3 partitions)

Partition 0: PatientCreated(p1), PatientUpdated(p2), ...
Partition 1: PatientCreated(p3), PatientDeleted(p4), ...
Partition 2: PatientUpdated(p5), PatientCreated(p6), ...

Consumer Group: quality-service
├─ Consumer 1 → Partition 0
├─ Consumer 2 → Partition 1
└─ Consumer 3 → Partition 2
```

Messages with same key always go to same partition → guarantees ordering.

### Concept 3: Offsets & Message Durability

**Offset:** Position of message in partition (like line number)
```
Partition 0: [msg@0, msg@1, msg@2, msg@3, msg@4, ...]
                                    ↑
                    Quality Service offset=3 (processed up to here)
```

Kafka stores offset per consumer group—if service restarts, resumes from last offset. Messages persist for configurable period (default 7 days).

### Concept 4: Transactional Outbox Pattern

**Problem:** Database transaction commits but Kafka publish fails → orphaned event

**Solution:** Store event in database outbox table; separate process publishes to Kafka

```
1. Start transaction
2. Insert patient into PATIENT table
3. Insert event into OUTBOX table (same transaction)
4. Commit (both or nothing)
5. Outbox polling service publishes to Kafka
6. Remove from outbox on success
```

### Concept 5: Schema Evolution & Versioning

**Problem:** Producer adds new field; old consumers crash

**Solution:** Use versioned schemas; consumers ignore unknown fields
```
v1: { id, tenantId, firstName, lastName }
v2: { id, tenantId, firstName, lastName, dateOfBirth }  ← New field

Old consumers see v2 → Ignore dateOfBirth → Continue working
New consumers see v2 → Use dateOfBirth → Work correctly
```

---

## Architecture Pattern

### HDIM Kafka Architecture

```
┌─── Event Producers ───┐
│ Patient Service       │
│ Quality Service       │
│ Care Gap Service      │
└───────────┬───────────┘
            │
            ▼
┌─────────────────────────────────────┐
│      Apache Kafka Cluster           │
├─────────────────────────────────────┤
│ Topic: patient.events               │
│ Topic: measure.evaluation.complete  │
│ Topic: care-gap.detected            │
│ Topic: audit.events                 │
│ Topic: notification.requests        │
└───────────┬───────────────────────────┘
            │
            ▼
┌─── Event Consumers ───┐
│ Quality Service       │
│ Care Gap Service      │
│ Notification Service  │
│ Analytics Service     │
└─────────────────────────┘
```

### Key Design Decisions

1. **Partition Strategy:**
   - Key: tenant_id → All tenant events go to same partition
   - Guarantees: Tenant's events processed in order
   - Example: `"tenant-001"` always → Partition 2

2. **Consumer Strategy:**
   - One consumer group per service
   - Multiple consumers within group for parallelism
   - Partition assignment automatic

3. **Topic Naming Convention:**
   - Format: `{domain}.{entity}.{action}`
   - Examples: `patient.lifecycle`, `measure.evaluation`, `gap.detection`

---

## Implementation Guide

### Step 1: Add Kafka Dependencies

```gradle
dependencies {
    implementation 'org.springframework.kafka:spring-kafka'

    // For testing
    testImplementation 'org.springframework.kafka:spring-kafka-test'
}
```

### Step 2: Configure Kafka Properties

**application.yml:**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9094
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all  # Wait for all replicas
      retries: 3
      properties:
        linger.ms: 10  # Batch messages for efficiency
    consumer:
      bootstrap-servers: localhost:9094
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      group-id: ${spring.application.name}
      auto-offset-reset: earliest  # Start from beginning if no offset
      properties:
        spring.json.trusted.packages: "*"
```

### Step 3: Define Event Classes

```java
// Base event interface
public interface DomainEvent {
    String getEventType();
    String getTenantId();
    Instant getTimestamp();
}

// Patient event
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeName("PatientCreated")
public class PatientCreatedEvent implements DomainEvent {
    private UUID patientId;
    private String tenantId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Instant timestamp;

    @Override
    public String getEventType() {
        return "PatientCreated";
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeName("PatientUpdated")
public class PatientUpdatedEvent implements DomainEvent {
    private UUID patientId;
    private String tenantId;
    private String firstName;
    private String lastName;
    private Instant timestamp;

    @Override
    public String getEventType() {
        return "PatientUpdated";
    }
}
```

### Step 4: Implement Event Producer

```java
@Service
@RequiredArgsConstructor
public class PatientEventPublisher {
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    public void publishPatientCreated(Patient patient) {
        PatientCreatedEvent event = new PatientCreatedEvent(
            patient.getId(),
            patient.getTenantId(),
            patient.getFirstName(),
            patient.getLastName(),
            patient.getDateOfBirth(),
            Instant.now()
        );

        // Key = tenantId ensures all tenant events → same partition
        kafkaTemplate.send(
            "patient.events",           // topic
            patient.getTenantId(),      // key
            event                       // value
        );
    }

    public void publishPatientUpdated(Patient patient) {
        PatientUpdatedEvent event = new PatientUpdatedEvent(
            patient.getId(),
            patient.getTenantId(),
            patient.getFirstName(),
            patient.getLastName(),
            Instant.now()
        );

        kafkaTemplate.send("patient.events", patient.getTenantId(), event);
    }
}
```

### Step 5: Implement Event Consumer

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MeasureEvaluationEventListener {
    private final QualityMeasureService measureService;

    @KafkaListener(
        topics = "patient.events",
        groupId = "quality-measure-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePatientEvent(
            PatientCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received event: {} from partition {} offset {}",
            event.getEventType(), partition, offset);

        try {
            // Auto-evaluate measures when patient created
            measureService.evaluateMeasuresForPatient(
                event.getPatientId(),
                event.getTenantId()
            );
        } catch (Exception e) {
            log.error("Error processing patient event", e);
            throw e;  // Redeliver on failure
        }
    }
}
```

### Step 6: Integrate into Service Layer

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {
    private final PatientRepository patientRepository;
    private final PatientEventPublisher eventPublisher;

    @Transactional
    public PatientResponse createPatient(
            CreatePatientRequest request,
            String tenantId) {

        // Create patient
        Patient patient = Patient.builder()
            .tenantId(tenantId)
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .dateOfBirth(request.getDateOfBirth())
            .build();

        patient = patientRepository.save(patient);

        // Publish event (outside transaction if possible)
        eventPublisher.publishPatientCreated(patient);

        return mapToResponse(patient);
    }

    @Transactional
    public PatientResponse updatePatient(
            String patientId,
            UpdatePatientRequest request,
            String tenantId) {

        Patient patient = patientRepository
            .findByIdAndTenantId(patientId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());

        patient = patientRepository.save(patient);

        // Publish update event
        eventPublisher.publishPatientUpdated(patient);

        return mapToResponse(patient);
    }
}
```

---

## Best Practices

- ✅ **DO use tenant_id as message key**
  - Why: Ensures all tenant events go to same partition → guaranteed ordering per tenant
  - Example: `kafkaTemplate.send(topic, tenantId, event)`

- ✅ **DO add timestamps and version info to events**
  - Why: Enables temporal queries; supports schema evolution
  - Example: Include `timestamp: Instant.now()` and `eventVersion: 2`

- ✅ **DO handle errors gracefully**
  - Why: Failed message processing shouldn't crash consumer
  - Example: Log, store in dead-letter topic, continue

- ✅ **DO use consumer groups per service**
  - Why: Multiple services can process same event independently
  - Example: `groupId: "quality-measure-service"`

- ✅ **DO implement idempotent consumers**
  - Why: Kafka guarantees "at least once" delivery; reprocessing must be safe
  - Example: Check if event already processed before updating

- ❌ **DON'T include large binary data in events**
  - Why: Kafka not designed for large payloads; use references instead
  - Example: Send patientId, not entire patient record

- ❌ **DON'T ignore offset management**
  - Why: Automatic offset management can cause message loss
  - Example: Use manual offset commit for critical operations

- ❌ **DON'T publish directly in @Transactional method**
  - Why: If publish succeeds but transaction rolls back, orphaned event
  - Example: Use Outbox pattern or publish after transaction

---

## Testing Strategies

### Unit Test: Event Publisher

```java
@ExtendWith(MockitoExtension.class)
class PatientEventPublisherTest {
    @Mock
    private KafkaTemplate<String, DomainEvent> kafkaTemplate;

    @InjectMocks
    private PatientEventPublisher publisher;

    @Test
    void shouldPublishPatientCreatedEvent() {
        // ARRANGE
        Patient patient = Patient.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant-001")
            .firstName("John")
            .build();

        when(kafkaTemplate.send(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        // ACT
        publisher.publishPatientCreated(patient);

        // ASSERT
        verify(kafkaTemplate, times(1)).send(
            eq("patient.events"),
            eq("tenant-001"),  // key
            argThat(event -> event instanceof PatientCreatedEvent)
        );
    }
}
```

### Integration Test: Event Consumer

```java
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = { "listeners=PLAINTEXT://localhost:29092" }
)
class MeasureEvaluationEventListenerTest {
    @Autowired
    private MeasureEvaluationEventListener listener;

    @Autowired
    private KafkaTemplate<String, DomainEvent> kafkaTemplate;

    @Test
    void shouldProcessPatientCreatedEvent() throws InterruptedException {
        // ARRANGE
        PatientCreatedEvent event = new PatientCreatedEvent(
            UUID.randomUUID(),
            "tenant-001",
            "Jane",
            "Doe",
            LocalDate.of(1990, 1, 1),
            Instant.now()
        );

        // ACT
        kafkaTemplate.send("patient.events", "tenant-001", event);

        // ASSERT - wait for async processing
        Thread.sleep(2000);  // Allow consumer to process

        // Verify measures evaluated (mock assertions)
    }
}
```

---

## HDIM Kafka Topics Reference

| Topic | Purpose | Producer | Consumers |
|-------|---------|----------|-----------|
| patient.events | Patient lifecycle | Patient Service | Quality Service, Care Gap Service, Notification Service |
| measure.evaluation.complete | Measure results | Quality Service | Care Gap Service, Analytics Service, Audit Service |
| care-gap.detected | Gap identification | Care Gap Service | Notification Service, Care Plan Service |
| audit.events | All access/modifications | All services | Compliance Service, Audit Log Service |
| notification.requests | Notifications | Multiple | Notification Service |
| clinical.workflow.events | Workflow transitions | Clinical Workflow Service | Notification Service, Analytics |

---

## Troubleshooting

### Issue: Consumer not receiving messages

**Cause:** Consumer group already processed messages (offset advanced)

**Solution:** Reset offset
```bash
# Reset to earliest
kafka-consumer-groups --bootstrap-server localhost:9094 \
  --group quality-measure-service \
  --reset-offsets --to-earliest --execute
```

### Issue: Message ordering violated

**Cause:** Different partition keys used for related events

**Solution:** Use consistent key (tenant_id)
```java
// ✅ Correct: Same key
kafkaTemplate.send(topic, tenantId, event);

// ❌ Wrong: Different keys
kafkaTemplate.send(topic, patientId, event);  // Events scattered across partitions
```

### Issue: "No partition leaders available"

**Cause:** Kafka broker down or misconfigured

**Solution:** Check broker status
```bash
docker compose logs kafka
docker compose ps
```

---

## References

- Apache Kafka Documentation: https://kafka.apache.org/documentation
- Spring Kafka: https://spring.io/projects/spring-kafka
- HDIM Event Sourcing Guide: `docs/architecture/EVENT_SOURCING_ARCHITECTURE.md`
- Kafka Docker Setup: `docker-compose.yml`

---

**Last Updated:** January 20, 2026
**Difficulty Level:** ⭐⭐⭐⭐ (4/5 stars)
**Time Investment:** 1-2 weeks
**Prerequisite Skills:** Spring Boot 3.x, Microservices patterns

---

**← [Skills Hub](../README.md)** | **→ [Next: Docker & Infrastructure](../08-infrastructure/docker-kubernetes.md)**
