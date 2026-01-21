# CQRS + Event Sourcing Architecture

**Difficulty Level:** ⭐⭐⭐⭐ (Advanced)
**Time Investment:** 1-2 weeks
**Prerequisite Skills:** Spring Boot basics, database design, message queues
**Related Skills:** Microservices patterns, Kafka event streaming, multi-tenant architecture

---

## Overview

### What is This Skill?

**CQRS (Command Query Responsibility Segregation) + Event Sourcing** is an architectural pattern that separates write operations (commands) from read operations (queries) using an immutable event log as the system of record.

**Event Sourcing** stores all state changes as a sequence of immutable events in a PostgreSQL event store. Instead of updating records, we append events. **CQRS** uses these events to build separate, optimized read models (projections).

**In Plain English:** Instead of updating a database record directly, we record "what happened" (an event), then use those events to both:
1. **Write side:** Build the audit trail (event store)
2. **Read side:** Update optimized views (projections) for queries

### Why is This Important for HDIM?

HDIM processes quality measures for 1,000+ tenants with 10,000+ patients per tenant, generating 56+ measures simultaneously. This creates massive concurrent load on both writes (measure evaluations) and reads (reporting, analytics).

**CQRS + Event Sourcing solves this by:**

| Challenge | Solution | HDIM Benefit |
|-----------|----------|-------------|
| High write concurrency | Separate write model (command) | 200+ concurrent measure evaluations |
| High read concurrency | Separate read models (projections) | Real-time reporting without blocking writes |
| Audit requirements | Immutable event log | Complete HIPAA compliance trail |
| Temporal queries | Event replay capability | "What was the state at date X?" |
| Service decoupling | Event-driven architecture | Measure service independent of gap service |
| Data consistency | Eventual consistency within tolerance | <200ms p95 evaluation latency |

### Business Impact

- ✅ **10,000+ patients/tenant** - Horizontal scalability of reads and writes independently
- ✅ **<200ms p95 measure evaluation** - Optimized write path
- ✅ **Real-time analytics** - Independent read model scaling
- ✅ **Complete audit trail** - All changes recorded immutably
- ✅ **99.9% uptime SLA** - Event replay for disaster recovery
- ✅ **Multi-tenant isolation** - Events filtered by tenant

### Key Services Using This Skill

HDIM implements CQRS + Event Sourcing across 4 event services:

- **patient-event-service** (8084) - Patient lifecycle events
- **quality-measure-event-service** (8087) - Measure evaluation events
- **care-gap-event-service** (8086) - Care gap detection events
- **clinical-workflow-event-service** - Workflow state events

---

## Key Concepts

### Concept 1: Event Sourcing

**Definition:** Store every state change as an immutable event in a log, making the log the system of record rather than the current state.

**Traditional Approach:**
```sql
-- Update current state directly
UPDATE patients SET first_name = 'John' WHERE id = 'p-123';
-- History lost - no audit trail
```

**Event Sourcing Approach:**
```sql
-- Record what happened
INSERT INTO event_store
  (id, tenant_id, event_type, event_data, created_at)
VALUES
  ('evt-1', 'tenant-001', 'PatientCreated', '{"firstName":"John"...}', NOW());

-- Rebuild current state from events if needed
SELECT * FROM event_store
WHERE tenant_id = 'tenant-001' AND entity_id = 'p-123'
ORDER BY created_at;
```

**Why it matters:**
- Complete audit trail (HIPAA requirement)
- Temporal queries ("What was state at date X?")
- Event replay for debugging
- Domain-driven events

**Real-world example in HDIM:**
```
1. POST /api/v1/patients (Create patient)
   ↓ (AppService records)
2. PatientCreatedEvent stored in event_store
   ↓ (Kafka publishes)
3. Kafka topic: patient.events
   ↓ (PatientEventHandler consumes)
4. patient_projection table updated (read model)
```

---

### Concept 2: CQRS (Command Query Responsibility Segregation)

**Definition:** Separate the write model (commands) from the read model (queries) into different databases/models optimized for each purpose.

**Traditional Approach (CRUD):**
```
Request → Single Model ← Queries
           (Create/Read/Update/Delete)
           ↓
         Database
```

**CQRS Approach:**
```
Commands (Writes)           Queries (Reads)
      ↓                           ↑
Command Model              Query Model
(Optimized for writes)    (Optimized for reads)
      ↓                           ↑
Event Store                 Projections
(Event log)                (Denormalized views)
```

**Why it matters:**
- Independent scaling of reads and writes
- Different optimization strategies per side
- Clear separation of concerns
- Supports eventual consistency model

**Real-world example in HDIM:**
```
COMMAND SIDE:
1. Receive measure evaluation request
2. Execute CQL logic (write operation)
3. Create MeasureEvaluatedEvent
4. Append to event_store
5. Publish to Kafka
→ Returns immediately (fast write)

QUERY SIDE (Meanwhile):
1. CareGapService consumes event
2. Updates care_gap_projection
3. Queries use projection (fast reads)
→ Eventual consistency (100ms lag acceptable)
```

---

### Concept 3: Event Store

**Definition:** PostgreSQL table storing immutable events as the system of record.

**Schema Design:**
```sql
CREATE TABLE event_store (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,        -- Multi-tenant
    aggregate_id UUID NOT NULL,              -- Patient, Measure, etc.
    aggregate_type VARCHAR(100) NOT NULL,   -- "Patient", "Measure"
    event_type VARCHAR(255) NOT NULL,       -- "PatientCreated", "MeasureEvaluated"
    version INT NOT NULL,                    -- Event version
    event_data JSONB NOT NULL,              -- Full event payload
    metadata JSONB,                          -- Created by, timestamp, etc.
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),

    -- Indexes
    UNIQUE(tenant_id, aggregate_id, version),
    INDEX idx_event_store_tenant ON event_store(tenant_id, aggregate_id),
    INDEX idx_event_store_type ON event_store(event_type),
    INDEX idx_event_store_created ON event_store(created_at)
);
```

**Key Characteristics:**
- **Immutable:** No updates or deletes after creation
- **Append-only:** Only inserts allowed
- **Audit trail:** Every change recorded
- **Multi-tenant:** Tenant ID on every event
- **Type-safe:** Event type identifies what happened

**Why it matters:**
- HIPAA audit requirement
- Enables temporal queries
- Event replay for recovery
- Complete state history

---

### Concept 4: Projections (Read Models)

**Definition:** Denormalized views built from events, optimized for query performance.

**Projection Example:**
```sql
-- Event store has raw events:
PatientCreatedEvent: {firstName: "John", lastName: "Doe"}
PatientNameChangedEvent: {firstName: "Jane"}

-- Projection has current state (optimized for queries):
CREATE TABLE patient_projection (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    updated_at TIMESTAMP,

    INDEX idx_projection_tenant ON patient_projection(tenant_id, id)
);

-- Projection data:
id: p-123
first_name: Jane        ← Latest from events
last_name: Doe
updated_at: 2024-01-15
```

**Why it matters:**
- Fast queries (denormalized)
- Can rebuild from events
- Independent scaling
- Supports multiple projections

**Real-world HDIM projections:**
- `patient_projection` - Patient list queries
- `measure_evaluation_result` - Measure results
- `care_gap_summary` - Dashboard counts
- `audit_log_projection` - Compliance reporting

---

### Concept 5: Event Versioning & Migration

**Definition:** Handle schema changes in events over time without breaking event replay.

**Problem:** What if event schema changes?
```
V1: PatientCreatedEvent {firstName, lastName}
V2: PatientCreatedEvent {firstName, lastName, dateOfBirth}
    (Now old V1 events are missing dateOfBirth)
```

**Solution: Event Versioning**
```java
@Data
public class PatientCreatedEvent {
    private static final int VERSION = 2;

    private UUID id;
    private String tenantId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;  // Added in V2

    // Deserialize with migration
    public static PatientCreatedEvent fromJson(String json, int version) {
        if (version == 1) {
            PatientCreatedEventV1 v1 = mapper.readValue(json, PatientCreatedEventV1.class);
            return new PatientCreatedEvent(
                v1.id, v1.tenantId, v1.firstName, v1.lastName,
                LocalDate.now()  // Default for missing field
            );
        }
        return mapper.readValue(json, PatientCreatedEvent.class);
    }
}
```

**Why it matters:**
- Events are immutable (can't change old events)
- Must handle old event formats in code
- Enables safe schema evolution

---

### Concept 6: Event Replay & Temporal Queries

**Definition:** Reconstruct system state at any point in time by replaying events.

**Use Case: Temporal Query**
```
Question: "What was the patient's status on 2024-01-15?"

Solution:
1. Query event_store WHERE created_at <= '2024-01-15'
2. Filter by aggregate_id (patient ID)
3. Replay events in order
4. State as of that date
```

**Use Case: Disaster Recovery**
```
Scenario: Projection database corrupted

Solution:
1. Truncate projection table
2. Replay all events from event_store
3. Rebuild projections
4. Back to complete state
```

**Why it matters:**
- Audit trail for compliance
- Debugging (replay to see what happened)
- Recovery from failures
- Time-travel queries

---

## Architecture Pattern

### How It Works

```
┌─────────────────────────────────────────────────────────────┐
│                         REQUEST                              │
│              POST /api/v1/patients (Create)                  │
└──────────────────────────┬──────────────────────────────────┘
                           ↓
         ┌─────────────────────────────────┐
         │   COMMAND SIDE (Write Model)    │
         └─────────────────────────────────┘
                           ↓
         ┌─────────────────────────────────┐
         │   1. Validate (business logic)  │
         │   2. Execute command            │
         │   3. Generate event             │
         │   4. Persist to event_store     │
         └─────────────────────────────────┘
                           ↓
         ┌─────────────────────────────────┐
         │   PatientCreatedEvent           │
         │   {id, tenantId, firstName...}  │
         │   Appended to event_store       │
         └─────────────────────────────────┘
                           ↓
         ┌─────────────────────────────────┐
         │  2. Publish Event to Kafka      │
         │  Topic: patient.events          │
         │  Partition: tenant_id           │
         └─────────────────────────────────┘
                           ↓
         ┌─────────────────────────────────┐
         │  3. Return Response             │
         │  200 OK                         │
         └─────────────────────────────────┘


Meanwhile (Asynchronous):

         ┌─────────────────────────────────┐
         │   QUERY SIDE (Read Model)       │
         │   Event Handler (Consumer)      │
         └─────────────────────────────────┘
                           ↓
         ┌─────────────────────────────────┐
         │  @KafkaListener                 │
         │  Consume PatientCreatedEvent    │
         │  From patient.events topic      │
         └─────────────────────────────────┘
                           ↓
         ┌─────────────────────────────────┐
         │  Update Projection              │
         │  INSERT INTO patient_projection │
         │  (id, tenant_id, first_name...)│
         └─────────────────────────────────┘
                           ↓
         ┌─────────────────────────────────┐
         │  Query reads from projection    │
         │  GET /api/v1/patients (fast!)   │
         └─────────────────────────────────┘
```

### Diagram: Complete Event Sourcing Flow

```
┌──────────────────────────────────────────────────────────────────┐
│                      HDIM Quality Measure Flow                    │
└──────────────────────────────────────────────────────────────────┘

1. MEASURE EVALUATION
   ┌────────────────────────┐
   │ POST /evaluate         │
   │ Measure ID: m-456      │
   │ Patient ID: p-123      │
   └────────────┬───────────┘
                ↓
   ┌────────────────────────┐
   │ Quality Measure Service│
   │ Execute CQL            │
   │ Query FHIR data        │
   └────────────┬───────────┘
                ↓
   ┌────────────────────────┐
   │ Result: numerator=true │
   │ denominator=true       │
   └────────────┬───────────┘
                ↓

2. CREATE EVENT & PERSIST
   ┌─────────────────────────────────────────┐
   │ MeasureEvaluatedEvent {                 │
   │   measureId: m-456,                     │
   │   patientId: p-123,                     │
   │   tenantId: tenant-001,                 │
   │   numerator: true,                      │
   │   denominator: true,                    │
   │   timestamp: 2024-01-20T10:30:00Z       │
   │ }                                       │
   │                                         │
   │ INSERT INTO event_store                 │
   └────────────┬────────────────────────────┘
                ↓

3. PUBLISH TO KAFKA
   ┌────────────────────────┐
   │ Kafka Topic:           │
   │ measure.evaluation     │
   │ .complete              │
   │                        │
   │ Key: tenant-001        │
   │ Partition: 0           │
   └────────────┬───────────┘
                ↓

4. CARE GAP SERVICE CONSUMES
   ┌────────────────────────┐
   │ @KafkaListener         │
   │ CareGapEventHandler    │
   │                        │
   │ Check:                 │
   │ numerator=true?        │
   │ denominator=true?      │
   │                        │
   │ Result: No gap         │
   └────────────┬───────────┘
                ↓

5. UPDATE PROJECTIONS
   ┌────────────────────────┐
   │ UPDATE                 │
   │ measure_result_        │
   │ projection             │
   │                        │
   │ UPDATE care_gap_       │
   │ projection (no gap)    │
   └────────────┬───────────┘
                ↓

6. QUERY USES PROJECTION
   ┌────────────────────────┐
   │ GET /measurements      │
   │ (fast read from        │
   │  projection, not       │
   │  events)               │
   └────────────────────────┘
```

### Design Decisions

**Decision 1: Why event sourcing instead of traditional updates?**

| Aspect | Event Sourcing | Traditional CRUD |
|--------|---|---|
| Audit trail | ✅ Complete | ❌ None |
| Temporal queries | ✅ Yes | ❌ No |
| Event-driven | ✅ Native | ❌ Requires triggers |
| HIPAA compliance | ✅ Easy | ❌ Difficult |
| Scalability | ✅ Append-only (fast) | ❌ Update contention |

**Rationale:** HEDIS compliance and HIPAA audit requirements mandate immutable audit trails. Event sourcing is the natural fit.

**Decision 2: Why separate write and read models (CQRS)?**

| Aspect | CQRS | Single Model |
|--------|------|---|
| Write latency | ✅ Optimized (append) | ❌ Update + index |
| Read latency | ✅ Optimized (denormalized) | ❌ Normalized joins |
| Concurrent scaling | ✅ Independent | ❌ Locked together |
| Consistency | ⚠️ Eventual | ✅ Strong |
| Complexity | ❌ Higher | ✅ Lower |

**Rationale:** HDIM needs 200+ concurrent writes (measures) and 1000+ concurrent reads (dashboards). CQRS allows independent optimization.

**Decision 3: Why PostgreSQL event store instead of NoSQL?**

| Aspect | PostgreSQL | MongoDB |
|--------|---|---|
| ACID transactions | ✅ Strong | ⚠️ Limited |
| SQL queries | ✅ Powerful | ❌ Basic |
| Multi-tenant support | ✅ Row-level security | ⚠️ Manual |
| Event ordering | ✅ Guaranteed | ⚠️ Complex |

**Rationale:** HDIM requires guaranteed event ordering per tenant (audit trail integrity) and strong consistency for immutable events.

### Trade-offs

| Aspect | Pro | Con | Mitigation |
|--------|-----|-----|-----------|
| **Complexity** | Clear architecture | Harder to understand | Training guides (this!) |
| **Storage** | Complete history | Event store grows large | Archive old events |
| **Consistency** | Audit trail | Eventual consistency lag | 100ms lag is acceptable |
| **Event schema** | Flexible | Must handle versioning | Version in events |
| **Debugging** | Event replay | More debugging tools needed | Use event inspection tools |

---

## Implementation Guide

### Step 1: Define Your Events

Events are the source of truth. Design them carefully.

```java
// Base event class
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class DomainEvent {
    private UUID id;
    private String tenantId;
    private UUID aggregateId;           // Patient, Measure, etc.
    private String aggregateType;       // "Patient", "Measure"
    private int version;                // Event version
    private Instant timestamp;
    private String createdBy;

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "eventType"
    )
    public abstract String getEventType();
}

// Specific events
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("PatientCreated")
public class PatientCreatedEvent extends DomainEvent {
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;

    @Override
    public String getEventType() {
        return "PatientCreated";
    }
}

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("MeasureEvaluated")
public class MeasureEvaluatedEvent extends DomainEvent {
    private UUID measureId;
    private UUID patientId;
    private boolean numerator;
    private boolean denominator;
    private LocalDate evaluationDate;

    @Override
    public String getEventType() {
        return "MeasureEvaluated";
    }
}
```

**Event Design Best Practices:**
- ✅ Include all data needed to rebuild state
- ✅ Use immutable fields
- ✅ Include tenant_id (multi-tenant)
- ✅ Include timestamp (audit trail)
- ✅ Name events as past tense (PatientCreated, not CreatePatient)
- ❌ Don't include data only for convenience (can be derived)

---

### Step 2: Create Event Store

Store events in PostgreSQL as immutable log.

```java
@Entity
@Table(name = "event_store")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStoreEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private int version;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String eventData;        // JSON serialized event

    @Column(columnDefinition = "jsonb")
    private String metadata;         // Created by, headers, etc.

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

@Repository
public interface EventStoreRepository extends JpaRepository<EventStoreEntry, UUID> {
    // Find all events for an aggregate
    @Query("SELECT e FROM EventStoreEntry e " +
           "WHERE e.tenantId = :tenantId " +
           "AND e.aggregateId = :aggregateId " +
           "ORDER BY e.version ASC")
    List<EventStoreEntry> findEvents(
        @Param("tenantId") String tenantId,
        @Param("aggregateId") UUID aggregateId
    );

    // Find events after timestamp (for projection update)
    @Query("SELECT e FROM EventStoreEntry e " +
           "WHERE e.tenantId = :tenantId " +
           "AND e.createdAt > :since " +
           "ORDER BY e.createdAt ASC")
    List<EventStoreEntry> findEventsSince(
        @Param("tenantId") String tenantId,
        @Param("since") Instant since
    );
}
```

**Liquibase Migration:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog">
    <changeSet id="0001-create-event-store" author="dev">
        <createTable tableName="event_store">
            <column name="id" type="UUID">
                <constraints primaryKey="true"/>
            </column>
            <column name="tenant_id" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="aggregate_id" type="UUID">
                <constraints nullable="false"/>
            </column>
            <column name="aggregate_type" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="event_type" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="event_data" type="JSONB">
                <constraints nullable="false"/>
            </column>
            <column name="metadata" type="JSONB"/>
            <column name="created_at" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="created_by" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createIndex tableName="event_store" indexName="idx_event_store_tenant_agg">
            <column name="tenant_id"/>
            <column name="aggregate_id"/>
        </createIndex>

        <createIndex tableName="event_store" indexName="idx_event_store_type">
            <column name="event_type"/>
        </createIndex>

        <createIndex tableName="event_store" indexName="idx_event_store_created">
            <column name="created_at"/>
        </createIndex>

        <rollback>
            <dropTable tableName="event_store"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

---

### Step 3: Publish Events

When business logic completes, publish event.

```java
@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository patientRepository;
    private final EventStoreRepository eventStoreRepository;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final SecurityContext securityContext;

    @Transactional
    public PatientResponse createPatient(
            CreatePatientRequest request,
            String tenantId) {

        // 1. Business logic - validate
        validatePatientRequest(request);

        // 2. Create entity
        Patient patient = Patient.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .dateOfBirth(request.getDateOfBirth())
            .build();

        // 3. Persist entity
        patient = patientRepository.save(patient);

        // 4. Create event
        PatientCreatedEvent event = new PatientCreatedEvent(
            UUID.randomUUID(),           // Event ID
            tenantId,                     // Multi-tenant
            patient.getId(),              // Aggregate ID
            "Patient",                    // Aggregate type
            1,                            // Version
            Instant.now(),
            securityContext.getCurrentUserId()
        );
        event.setFirstName(request.getFirstName());
        event.setLastName(request.getLastName());
        event.setDateOfBirth(request.getDateOfBirth());

        // 5. Store event (immutable)
        EventStoreEntry eventEntry = EventStoreEntry.builder()
            .tenantId(tenantId)
            .aggregateId(patient.getId())
            .aggregateType("Patient")
            .eventType(event.getEventType())
            .version(1)
            .eventData(objectMapper.writeValueAsString(event))
            .createdBy(securityContext.getCurrentUserId())
            .build();

        eventStoreRepository.save(eventEntry);

        // 6. Publish event to Kafka
        kafkaTemplate.send("patient.events", tenantId, event);

        // 7. Return response
        return mapToResponse(patient);
    }

    private void validatePatientRequest(CreatePatientRequest request) {
        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            throw new HdimValidationException("First name required");
        }
        if (request.getLastName() == null || request.getLastName().isBlank()) {
            throw new HdimValidationException("Last name required");
        }
    }
}
```

**Key Points:**
- ✅ Transaction wraps BOTH database writes (entity) AND event store writes
- ✅ Event published AFTER database commit (transactional outbox pattern)
- ✅ Kafka publish happens as separate step (async)
- ✅ Include all data needed to rebuild state
- ✅ Include tenant_id for multi-tenant isolation

---

### Step 4: Consume Events

Build projections from events using Kafka listeners.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientEventHandler {
    private final PatientProjectionRepository projectionRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "patient.events",
        groupId = "patient-projection-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePatientCreated(PatientCreatedEvent event) {
        try {
            log.info("Processing PatientCreatedEvent: {} for tenant: {}",
                     event.getAggregateId(), event.getTenantId());

            // Create projection record
            PatientProjection projection = PatientProjection.builder()
                .id(event.getAggregateId())
                .tenantId(event.getTenantId())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .dateOfBirth(event.getDateOfBirth())
                .createdAt(event.getTimestamp())
                .updatedAt(event.getTimestamp())
                .build();

            // Update projection
            projectionRepository.save(projection);

            log.info("Patient projection created: {}",
                     projection.getId());

        } catch (Exception ex) {
            log.error("Error handling PatientCreatedEvent: " +
                     event.getAggregateId(), ex);
            // Send to DLQ for manual review
            throw new HdimEventProcessingException(
                "Failed to process PatientCreatedEvent", ex);
        }
    }

    @KafkaListener(
        topics = "patient.events",
        groupId = "patient-projection-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePatientNameChanged(PatientNameChangedEvent event) {
        PatientProjection projection = projectionRepository
            .findByIdAndTenantId(event.getAggregateId(), event.getTenantId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Patient", event.getAggregateId()));

        projection.setFirstName(event.getFirstName());
        projection.setLastName(event.getLastName());
        projection.setUpdatedAt(event.getTimestamp());

        projectionRepository.save(projection);
    }
}

// Projection entity (optimized for reads)
@Entity
@Table(name = "patient_projection")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientProjection {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;

    private Instant createdAt;
    private Instant updatedAt;

    // Index for queries
    @Index(columnList = "tenant_id, id")
}

@Repository
public interface PatientProjectionRepository
    extends JpaRepository<PatientProjection, UUID> {

    Optional<PatientProjection> findByIdAndTenantId(UUID id, String tenantId);

    List<PatientProjection> findByTenantIdOrderByCreatedAtDesc(
        String tenantId,
        Pageable pageable
    );
}
```

**Key Points:**
- ✅ Each event type has separate handler method
- ✅ Exception handling sends to DLQ for retry
- ✅ Idempotent: safe to process same event twice
- ✅ Projection optimized for queries (denormalized)
- ✅ Tenant ID enforced in all queries

---

### Step 5: Query Projections (Not Event Store!)

Queries use fast projections, not slow event store.

```java
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientProjectionRepository projectionRepository;
    private final PatientService patientService;

    // ✅ CORRECT: Query projection (fast)
    @GetMapping
    public ResponseEntity<Page<PatientResponse>> listPatients(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {

        Page<PatientProjection> projections =
            projectionRepository.findByTenantIdOrderByCreatedAtDesc(
                tenantId, pageable);

        return ResponseEntity.ok(
            projections.map(this::mapToResponse)
        );
    }

    // ❌ WRONG: Query event store (slow!)
    // DON'T DO THIS:
    // List<DomainEvent> events = eventStoreRepository.findEvents(tenantId, patientId);
    // Instead, query projection!

    @PostMapping
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody CreatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        return ResponseEntity
            .created(URI.create("/api/v1/patients/" + request.getId()))
            .body(patientService.createPatient(request, tenantId));
    }

    private PatientResponse mapToResponse(PatientProjection projection) {
        return PatientResponse.builder()
            .id(projection.getId())
            .firstName(projection.getFirstName())
            .lastName(projection.getLastName())
            .dateOfBirth(projection.getDateOfBirth())
            .build();
    }
}
```

**Key Points:**
- ✅ Queries use projection (fast reads)
- ✅ Event store used only for write-side and audit
- ✅ Eventual consistency lag acceptable (100ms)
- ❌ Never query event store directly (slow)

---

## Real-World Examples from HDIM

### Example 1: patient-event-service (Port 8084)

**File:** `backend/modules/services/patient-event-service/`

**What it does:** REST API for patient operations using event sourcing.

**Event flow:**
```
POST /api/v1/patients
  ↓
PatientService.createPatient()
  ├─ Validate request
  ├─ Create Patient entity
  ├─ Create PatientCreatedEvent
  ├─ Store in event_store
  └─ Publish to patient.events Kafka topic
```

**Key files:**
- `PatientService.java` - Business logic + event publishing
- `Patient.java` - JPA entity
- `PatientProjection.java` - Read model
- `PatientEventHandler.java` - Kafka consumer
- `0001-create-patient-projections-table.xml` - Liquibase migration

---

### Example 2: quality-measure-event-service (Port 8087)

**File:** `backend/modules/services/quality-measure-event-service/`

**What it does:** Measure evaluation with complete event trail.

**Event flow:**
```
POST /api/v1/measures/evaluate
  ↓
MeasureService.evaluate()
  ├─ Load FHIR data for patient
  ├─ Execute CQL logic
  ├─ Determine numerator/denominator
  ├─ Create MeasureEvaluatedEvent
  ├─ Store in event_store
  └─ Publish to measure.evaluation.complete topic
        ↓
CareGapEventHandler consumes
  ├─ Check: numerator AND denominator?
  ├─ If numerator=false, denominator=true → CARE GAP
  ├─ Create CareGapDetectedEvent
  └─ Publish to care-gap.detected topic
```

**Events:**
- `MeasureEvaluatedEvent` - Measure logic result
- `ResultsGeneratedEvent` - Batch processing complete

---

### Example 3: care-gap-event-service (Port 8086)

**File:** `backend/modules/services/care-gap-event-service/`

**What it does:** Care gap detection from measure events.

**Event flow:**
```
Consumes: measure.evaluation.complete
          ↓
CareGapDetectionService.detectGaps()
  ├─ If measure.numerator == false AND measure.denominator == true
  ├─ Create CareGapDetectedEvent
  ├─ Store in event_store
  ├─ Update care_gap_projection
  └─ Publish to care-gap.detected topic
        ↓
NotificationService consumes
  └─ Send provider alert
```

---

## Best Practices

### DO's ✅

- ✅ **Design events carefully** - They're immutable
- ✅ **Include all rebuild data** - Everything needed to reconstruct state
- ✅ **Version events** - Handle schema changes gracefully
- ✅ **Include tenant_id** - Multi-tenant isolation
- ✅ **Use transactional outbox** - Guarantee publishing
- ✅ **Make projections idempotent** - Safe to replay
- ✅ **Include timestamps** - Audit trail
- ✅ **Test event replay** - Ensure rebuild works
- ✅ **Archive old events** - Event store can grow large
- ✅ **Monitor lag** - Track projection staleness

### DON'Ts ❌

- ❌ **Update events** - They're immutable (create new version)
- ❌ **Include derived data** - Can be computed from base events
- ❌ **Query event store for reads** - Use projections (too slow)
- ❌ **Forget tenant filtering** - Multi-tenant violations
- ❌ **Skip error handling** - Use DLQ for failed events
- ❌ **Assume strong consistency** - Plan for eventual consistency
- ❌ **Complex event schemas** - Keep events simple and explicit
- ❌ **Lose event ordering** - Critical for audit trail
- ❌ **Delete events** - Never (destroys audit trail)
- ❌ **Publish without persistence** - Lost events = lost data

---

## Testing Strategies

### Unit Testing: Event Creation

```java
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {
    @Mock
    private PatientRepository patientRepository;

    @Mock
    private EventStoreRepository eventStoreRepository;

    @Mock
    private KafkaTemplate<String, DomainEvent> kafkaTemplate;

    @InjectMocks
    private PatientService patientService;

    @Test
    void shouldCreatePatientAndPublishEvent() {
        // ARRANGE
        CreatePatientRequest request = new CreatePatientRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setDateOfBirth(LocalDate.of(1980, 1, 1));

        Patient expected = Patient.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant-001")
            .firstName("John")
            .lastName("Doe")
            .build();

        when(patientRepository.save(any(Patient.class)))
            .thenReturn(expected);

        // ACT
        PatientResponse result = patientService.createPatient(request, "tenant-001");

        // ASSERT
        assertThat(result.getId()).isEqualTo(expected.getId());
        assertThat(result.getFirstName()).isEqualTo("John");

        // Verify event stored
        verify(eventStoreRepository).save(argThat(entry ->
            entry.getEventType().equals("PatientCreated") &&
            entry.getTenantId().equals("tenant-001")
        ));

        // Verify event published to Kafka
        verify(kafkaTemplate).send(
            eq("patient.events"),
            eq("tenant-001"),
            argThat(event -> event instanceof PatientCreatedEvent)
        );
    }

    @Test
    void shouldThrowException_WhenFirstNameMissing() {
        CreatePatientRequest request = new CreatePatientRequest();
        request.setLastName("Doe");

        assertThatThrownBy(() ->
            patientService.createPatient(request, "tenant-001")
        ).isInstanceOf(HdimValidationException.class)
         .hasMessageContaining("First name required");

        // Verify no event published on error
        verify(eventStoreRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }
}
```

### Integration Testing: Event Publishing & Projection

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PatientEventIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private EventStoreRepository eventStoreRepository;

    @Autowired
    private PatientProjectionRepository projectionRepository;

    @Container
    static KafkaContainer kafka = new KafkaContainer();

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
        eventStoreRepository.deleteAll();
        projectionRepository.deleteAll();
    }

    @Test
    void shouldCreatePatientAndUpdateProjection() throws Exception {
        // ARRANGE
        CreatePatientRequest request = new CreatePatientRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setDateOfBirth(LocalDate.of(1990, 5, 15));

        // ACT
        mockMvc.perform(post("/api/v1/patients")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Tenant-ID", "tenant-001")
            .content(new ObjectMapper().writeValueAsString(request)))
            .andExpect(status().isCreated());

        // Wait for async event processing
        Thread.sleep(1000);

        // ASSERT
        // Event should be in event store
        List<EventStoreEntry> events = eventStoreRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("PatientCreated");

        // Projection should be updated
        List<PatientProjection> projections =
            projectionRepository.findAll();
        assertThat(projections).hasSize(1);
        assertThat(projections.get(0).getFirstName()).isEqualTo("Jane");
    }

    @Test
    void shouldIsolateTenants() throws Exception {
        // Create patient in tenant-001
        CreatePatientRequest request1 = new CreatePatientRequest();
        request1.setFirstName("John");
        request1.setLastName("Doe");

        mockMvc.perform(post("/api/v1/patients")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Tenant-ID", "tenant-001")
            .content(new ObjectMapper().writeValueAsString(request1)))
            .andExpect(status().isCreated());

        // Create patient in tenant-002
        CreatePatientRequest request2 = new CreatePatientRequest();
        request2.setFirstName("Jane");
        request2.setLastName("Smith");

        mockMvc.perform(post("/api/v1/patients")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Tenant-ID", "tenant-002")
            .content(new ObjectMapper().writeValueAsString(request2)))
            .andExpect(status().isCreated());

        Thread.sleep(1000);

        // Verify isolation
        List<PatientProjection> tenant1 =
            projectionRepository.findByTenantIdOrderByCreatedAtDesc(
                "tenant-001", Pageable.unpaged()).getContent();

        List<PatientProjection> tenant2 =
            projectionRepository.findByTenantIdOrderByCreatedAtDesc(
                "tenant-002", Pageable.unpaged()).getContent();

        assertThat(tenant1).hasSize(1);
        assertThat(tenant2).hasSize(1);
        assertThat(tenant1.get(0).getFirstName()).isEqualTo("John");
        assertThat(tenant2.get(0).getFirstName()).isEqualTo("Jane");
    }
}
```

### Testing Checklist

- [ ] Event creation with correct data
- [ ] Multi-tenant isolation (tenant_id filtering)
- [ ] Error handling (invalid input → no event)
- [ ] Event publication to Kafka
- [ ] Event store persistence
- [ ] Projection update from events
- [ ] Event versioning and migration
- [ ] Duplicate event handling (idempotency)
- [ ] Large batch event processing
- [ ] Event replay and state reconstruction

---

## Troubleshooting

### Issue 1: Events Not Appearing in Projection

**Symptoms:**
- Event created (visible in event_store)
- But projection not updated
- Queries return old data

**Root cause:**
- Kafka consumer not running
- Consumer group offset misconfigured
- Event handler exception (events sent to DLQ)

**Solution:**
```bash
# Check consumer status
docker exec -it kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9093 \
  --group patient-projection-service \
  --describe

# Check for DLQ (dead letter queue)
docker exec -it kafka kafka-topics.sh \
  --bootstrap-server localhost:9093 \
  --list | grep dlq

# View DLQ messages
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9093 \
  --topic patient-projection-service.DLT \
  --from-beginning

# Check service logs
docker compose logs -f patient-event-service | grep -i error
```

**Prevention:**
- ✅ Add metrics to track consumer lag
- ✅ Alert on DLQ messages
- ✅ Monitor projection staleness
- ✅ Test event handlers thoroughly

---

### Issue 2: Duplicate Event Processing

**Symptoms:**
- Same event processed multiple times
- Projection has duplicate records

**Root cause:**
- Kafka broker sends duplicate (exactly-once semantics not guaranteed)
- Consumer restart while processing

**Solution:**
Make event handlers idempotent:

```java
@Service
public class PatientEventHandler {
    private final PatientProjectionRepository repository;

    @KafkaListener(topics = "patient.events")
    public void handlePatientCreated(PatientCreatedEvent event) {
        // ✅ CORRECT: Idempotent (upsert, not insert)
        PatientProjection existing = repository
            .findByIdAndTenantId(event.getAggregateId(), event.getTenantId())
            .orElse(null);

        if (existing != null) {
            // Already processed - idempotent update
            log.debug("Event already processed: {}", event.getAggregateId());
            return;
        }

        // Process event
        PatientProjection projection = new PatientProjection();
        // ... populate ...
        repository.save(projection);
    }
}
```

---

### Issue 3: Event Ordering Issues

**Symptoms:**
- Events processed out of order
- Final state incorrect despite all events processed

**Root cause:**
- Multiple Kafka partitions
- Concurrent event handlers
- Missing partition key

**Solution:**
```java
// Ensure events for same aggregate go to same partition
@Service
public class PatientService {
    public void publishEvent(DomainEvent event) {
        // Key = aggregateId ensures same partition
        kafkaTemplate.send(
            "patient.events",
            event.getAggregateId().toString(),  // Partition key!
            event
        );
    }
}

// Kafka configuration
@Configuration
public class KafkaConfig {
    @Bean
    public ProducerFactory<String, DomainEvent> producerFactory() {
        return new DefaultProducerFactory<>(
            producerConfigs(),
            new StringSerializer(),
            new JsonSerializer<>(DomainEvent.class)
        );
    }

    private Map<String, Object> producerConfigs() {
        return Map.of(
            "bootstrap.servers", "kafka:9093",
            "key.serializer", StringSerializer.class,
            "value.serializer", JsonSerializer.class,
            "partitioner.class", DefaultPartitioner.class  // Sticky partitioner
        );
    }
}
```

---

## References & Resources

### HDIM Documentation

- [Event Sourcing Architecture Guide](../../architecture/EVENT_SOURCING_ARCHITECTURE.md)
- [Kafka Event Streaming](./kafka-event-streaming.md)
- [Multi-Tenant Architecture](./multi-tenant-architecture.md)
- [PostgreSQL + Liquibase](../04-database/liquibase-migrations.md)

### External Resources

- **Event Sourcing Pattern:** martinfowler.com/eaaDev/EventSourcing.html
- **CQRS Pattern:** martinfowler.com/bliki/CQRS.html
- **Kafka Documentation:** kafka.apache.org
- **Spring Kafka:** spring.io/projects/spring-kafka
- **"Building Event-Driven Microservices"** by Adam Bellemare

### Related Skills

- **Prerequisite:** Spring Boot basics, database design
- **Complement:** Kafka event streaming, multi-tenant architecture
- **Advanced:** Event versioning, saga pattern, temporal queries

---

## Quick Reference Checklist

### Before You Start
- [ ] Understand immutable events vs mutable records
- [ ] Know why multi-tenant isolation is critical
- [ ] Understand event-driven architecture benefits
- [ ] Have read Event Sourcing pattern documentation

### While Implementing
- [ ] Events designed with all rebuild data
- [ ] Event versioning strategy in place
- [ ] Event store created with Liquibase
- [ ] Projections created and indexed
- [ ] Kafka consumer configured correctly
- [ ] Error handling sends to DLQ
- [ ] Projections are idempotent
- [ ] Multi-tenant filtering on all queries
- [ ] Tests verify event publishing
- [ ] Tests verify projection updates

### After Implementation
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Event replay works correctly
- [ ] Projection can be rebuilt from events
- [ ] Multi-tenant isolation verified
- [ ] Consumer lag monitored
- [ ] Documentation updated
- [ ] Ready for code review

---

## Key Takeaways

1. **Core Concept:** Events are the source of truth; current state is derived
2. **Implementation:** Event store (immutable) + projections (mutable)
3. **Common Pitfall:** Querying event store directly (too slow)
4. **Why It Matters:** HIPAA audit trail + scalable architecture

---

## Next Steps

After mastering CQRS + Event Sourcing:

1. **Learn:** Kafka Event Streaming (how events flow)
2. **Learn:** Multi-Tenant Architecture (isolation patterns)
3. **Practice:** Implement event in real HDIM service
4. **Review:** Have peer review event-driven code
5. **Test:** Write comprehensive event tests

**Your Next Guide:** [Kafka Event Streaming](../05-messaging/kafka-event-streaming.md)

---

**Last Updated:** January 20, 2026
**Version:** 1.0 - Foundation Release
**Status:** ✅ Complete

**← Previous: [Skills Hub](../README.md)** | **Next: [HEDIS Quality Measures](../02-healthcare-domain/hedis-quality-measures.md) →**
