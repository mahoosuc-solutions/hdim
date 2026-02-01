# Event Sourcing Architecture

---
**Navigation:** [CLAUDE.md](../../CLAUDE.md) | [Documentation Portal](../README.md) | [Architecture Index](./decisions/)
---

## Overview

HDIM implements **Event Sourcing** and **CQRS (Command Query Responsibility Segregation)** patterns as its primary architecture for state management across microservices. This represents a major architectural shift completed in **Phases 4-5 (October 2025 - January 2026)**.

**What It Means:**
Instead of storing only the current state of entities in the database, HDIM stores a complete **audit trail of all events** that happened. The current state is derived by replaying all events.

**Example:**
```
Traditional Database:
┌──────────────────┐
│ Patient Record   │
├──────────────────┤
│ Name: John Doe   │
│ Status: ACTIVE   │
│ Score: 85        │
└──────────────────┘

Event Sourcing:
┌─────────────────────────────────┐
│ Event Store (Immutable Log)     │
├─────────────────────────────────┤
│ 1. PatientCreatedEvent          │
│    └─ Name: John Doe            │
│ 2. PatientStatusChangedEvent    │
│    └─ Status: ACTIVE            │
│ 3. PatientScoreUpdatedEvent     │
│    └─ Score: 85                 │
│ 4. PatientStatusChangedEvent    │
│    └─ Status: INACTIVE          │
│ 5. PatientScoreUpdatedEvent     │
│    └─ Score: 90                 │
└─────────────────────────────────┘

Current State = Replay all events in order
```

---

## Architecture Layers

### 1. Event Services (REST API Layer)

**Definition:** Spring Boot microservices that expose REST APIs for domain operations

**Examples:**
- `patient-event-service` (port 8084)
- `quality-measure-event-service` (port 8087)
- `care-gap-event-service` (port 8086)
- `clinical-workflow-event-service` (TBD)

**Responsibilities:**
- Receive REST API requests (POST, PUT, DELETE)
- Validate input (business logic validation)
- Emit events to Kafka
- Query read models (projections)
- Return current state to client

**Example Controller:**
```java
@RestController
@RequestMapping("/api/v1/patients")
public class PatientEventController {

    @PostMapping
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody CreatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        // 1. Validate input
        // 2. Create PatientCreatedEvent
        // 3. Publish to Kafka: "patient-events" topic
        // 4. Write to Event Store
        // 5. Update projections
        // 6. Return PatientResponse from read model
    }

    @PutMapping("/{patientId}")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable String patientId,
            @Valid @RequestBody UpdatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        // 1. Validate current state
        // 2. Create PatientUpdatedEvent
        // 3. Publish to Kafka
        // 4. Write to Event Store
        // 5. Update projections
        // 6. Return updated PatientResponse
    }
}
```

### 2. Event Handler Services (Business Logic Libraries)

**Definition:** Shared library modules that contain event processing logic

**Examples:**
- `patient-event-handler-service` (library, not deployed separately)
- `quality-measure-event-handler-service` (library)
- `care-gap-event-handler-service` (library)
- `clinical-workflow-event-handler-service` (library)

**Responsibilities:**
- Consume events from Kafka
- Apply business logic (CQRS commands)
- Update projections (read models)
- Publish derived events

**Example Handler:**
```java
@Component
public class PatientEventHandler {

    private final PatientProjection patientProjection;
    private final EventStore eventStore;
    private final KafkaTemplate kafkaTemplate;

    @KafkaListener(topics = "patient-events")
    public void handlePatientCreated(PatientCreatedEvent event) {
        // 1. Persist to Event Store
        eventStore.append(event);

        // 2. Update Patient Projection (Read Model)
        PatientReadModel patient = new PatientReadModel();
        patient.setId(event.getPatientId());
        patient.setFirstName(event.getFirstName());
        patient.setStatus("ACTIVE");
        patientProjection.save(patient);

        // 3. Publish derived events
        kafkaTemplate.send("patient-updated", new PatientStatusChangedEvent(...));
    }

    @KafkaListener(topics = "patient-events")
    public void handlePatientUpdated(PatientUpdatedEvent event) {
        // Update projections
        // Publish derived events
    }
}
```

### 3. Event Store (Event Persistence)

**Service:** `event-store-service`

**Purpose:** Immutable log of all events that ever happened

**Implementation:**
- PostgreSQL database with event_store table (immutable)
- Each event stored with:
  - ID (auto-incrementing)
  - Aggregate ID (e.g., patientId)
  - Aggregate Type (e.g., "Patient")
  - Event Type (e.g., "PatientCreatedEvent")
  - Payload (JSON data)
  - Timestamp (when event happened)
  - Version (event sequence)

**Database Schema:**
```sql
CREATE TABLE event_store (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_version INTEGER NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id VARCHAR(100) NOT NULL,
    UNIQUE(aggregate_id, aggregate_type, event_version)
);

CREATE INDEX idx_event_store_aggregate
    ON event_store(aggregate_id, aggregate_type);
CREATE INDEX idx_event_store_type
    ON event_store(event_type);
CREATE INDEX idx_event_store_tenant
    ON event_store(tenant_id);
```

### 4. Event Projections (Read Models)

**Definition:** Denormalized copies of data optimized for queries

**Services:**
- Patient Projection (`patient-event-handler-service`)
- Quality Measure Projection (`quality-measure-event-handler-service`)
- Care Gap Projection (`care-gap-event-handler-service`)
- Cohort Measure Rate Projection (`quality-measure-event-handler-service`)

**Purpose:**
- Fast queries (no event replaying needed)
- Pre-aggregated data (counts, sums, averages)
- UI-optimized format (ready to display)

**Example Projection Table:**
```sql
CREATE TABLE patient_projection (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    date_of_birth DATE,
    status VARCHAR(50),
    risk_score DECIMAL(5,2),
    last_updated TIMESTAMP,
    version INTEGER  -- projection version for conflict detection
);
```

### 5. Event Processing & Routing

**Service:** `event-router-service`

**Responsibilities:**
- Multi-tenant event isolation (tenant filtering)
- Event transformation (format conversion)
- Priority-based routing (high-priority events processed first)
- Dead-letter queue (DLQ) management
- Rate limiting per tenant

**Example Routing Logic:**
```java
@Service
public class EventRouter {

    private final KafkaTemplate kafkaTemplate;

    public void routeEvent(DomainEvent event) {
        // 1. Validate tenant access
        validateTenant(event.getTenantId());

        // 2. Transform if needed
        ProcessedEvent processed = transform(event);

        // 3. Route to appropriate topic based on priority
        if (processed.isPriority()) {
            kafkaTemplate.send("high-priority-events", processed);
        } else {
            kafkaTemplate.send("standard-events", processed);
        }
    }
}
```

### 6. Event Processing & DLQ Management

**Service:** `event-processing-service`

**Responsibilities:**
- Kafka consumer orchestration
- Retry logic (exponential backoff)
- Dead-letter queue (DLQ) handling
- Event acknowledgment management
- Consumer group coordination

**Example Processing:**
```java
@Service
public class EventProcessor {

    @KafkaListener(topics = "standard-events")
    public void processEvent(ProcessedEvent event) throws Exception {
        try {
            // Process event (call appropriate handler)
            eventHandler.handle(event);
        } catch (TemporaryException e) {
            // Retry logic: republish with exponential backoff
            retryQueue.add(event, retryCount + 1);
        } catch (PermanentException e) {
            // Send to Dead-Letter Queue for manual review
            dlqService.sendToDLQ(event, e);
            // Create alert for operations team
        }
    }
}
```

### 7. Event Replay & Projection Rebuilding

**Service:** `event-replay-service`

**Purpose:** Rebuild projections from scratch by replaying all events

**Use Cases:**
- New projection type added (replay all historical events)
- Projection corrupted (rebuild from event store)
- Business logic changed (rebuild with new rules)
- Data migration (transform historical data)

**Example Replay:**
```java
@Service
public class EventReplayService {

    public void rebuildProjection(String projectionName) {
        // 1. Clear existing projection
        projectionRepository.deleteAll();

        // 2. Fetch all events in order
        List<Event> allEvents = eventStore.findAll();

        // 3. Replay events one by one
        for (Event event : allEvents) {
            eventHandler.handle(event);
            // Handler updates projection as if event just happened
        }

        // 4. Verify projection consistency
        verifyProjectionConsistency();
    }
}
```

---

## Event Sourcing Data Flow

### Command Flow (Write Path)

```
┌─────────────────────────────────────────────────────────────┐
│ Client sends REST request to Event Service                  │
│ POST /api/v1/patients { name: "John", ... }                │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ Event Service Controller                                    │
│ 1. Validate input                                           │
│ 2. Check current state from projection                      │
│ 3. Validate business rules                                  │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ Create Domain Event: PatientCreatedEvent                    │
│ {                                                           │
│   patientId: uuid,                                          │
│   firstName: "John",                                        │
│   tenantId: "tenant-123",                                   │
│   timestamp: 2026-01-19T10:30:00Z,                         │
│   version: 1                                                │
│ }                                                           │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ Publish to Kafka Topic: "patient-events"                    │
└────────────────┬────────────────────────────────────────────┘
                 │
         ┌───────┴────────┐
         │                │
         ▼                ▼
    ┌─────────┐      ┌──────────────┐
    │Event    │      │Event Router  │
    │Store    │      │Service       │
    │Service  │      │              │
    └─────────┘      └────────┬─────┘
         │                    │
         │          ┌─────────┴─────────┐
         │          │                   │
         ▼          ▼                   ▼
    Event Store  High-Priority      Standard-Events
    (immutable    Topic              Topic
     log)
```

### Query Flow (Read Path)

```
┌─────────────────────────────────────────────────────────────┐
│ Client sends REST request to Event Service                  │
│ GET /api/v1/patients/123                                   │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ Event Service (Query Operation)                             │
│ NO EVENT SOURCING - Direct query to projection              │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ Patient Projection (Read Model)                             │
│ Query: SELECT * FROM patient_projection WHERE id = 123     │
│ Returns: { id, name, status, riskScore, ... }             │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ Return PatientResponse to Client                            │
│ No event replaying - fast!                                  │
└─────────────────────────────────────────────────────────────┘
```

### Projection Update Flow

```
┌──────────────────────────────────────────────────────────┐
│ Kafka Topic: "patient-events"                            │
└──────────────┬───────────────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────────┐
│ Event Handler Service (Consumer Group)                   │
│ @KafkaListener(topics = "patient-events")                │
└──────────────┬───────────────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────────┐
│ Handle PatientCreatedEvent                               │
│ 1. Extract event data                                    │
│ 2. Create PatientReadModel                              │
│ 3. Set properties from event                             │
│ 4. Save to patient_projection table                      │
└──────────────┬───────────────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────────┐
│ Patient Projection Updated                               │
│ Ready for queries!                                        │
└──────────────────────────────────────────────────────────┘
```

---

## Service Naming Conventions

### Service Suffix Meaning

| Suffix | Meaning | Type | Deployable |
|--------|---------|------|-----------|
| `-event-service` | REST API for domain | Microservice | Yes (deployed in Docker) |
| `-event-handler-service` | Event processing logic | Library | No (imported by event-service) |
| `-projection` | Read model for queries | Database | N/A (not a service) |

### Examples

**Patient Domain:**
```
patient-event-service
├── Exposes: POST /api/v1/patients, GET /api/v1/patients/...
├── Depends on: patient-event-handler-service library
└── Updates: patient_projection table

patient-event-handler-service (Library)
├── @KafkaListener(topics = "patient-events")
├── Handles: PatientCreatedEvent, PatientUpdatedEvent, ...
└── Updates: patient_projection via patientRepository
```

**Quality Measure Domain:**
```
quality-measure-event-service
├── Exposes: POST /api/v1/measures, GET /api/v1/measures/...
├── Depends on: quality-measure-event-handler-service library
└── Updates: measure_projection table

quality-measure-event-handler-service (Library)
├── @KafkaListener(topics = "quality-events")
├── Handles: MeasureCreatedEvent, MeasureEvaluatedEvent, ...
└── Updates: measure_projection, cohort_measure_rate_projection
```

---

## Event Sourcing Benefits

### 1. Complete Audit Trail
- Every change to every entity is recorded
- Know who changed what, when, and why
- Perfect for compliance and auditing (HIPAA)

### 2. Event Replay & Debugging
- Replay events to reproduce exact state at any point in time
- Debug production issues without losing data
- Time-travel debugging capability

### 3. CQRS (Command Query Responsibility Separation)
- **Commands** (write operations) go to event store
- **Queries** (read operations) go to optimized projections
- Scale reads and writes independently

### 4. Temporal Queries
- Ask "what was the state of entity X on date Y?"
- Build time-series analytics
- Historical reporting

### 5. Event-Driven Integration
- Other services subscribe to events
- Loosely coupled microservices
- Easy to add new consumers without changing producers

### 6. Eventual Consistency
- Projections are eventually consistent with events
- Allows horizontal scaling (multiple handlers)
- Better fault tolerance

---

## Event Sourcing Tradeoffs

### ✅ Advantages
- Complete audit trail
- Temporal queries
- Event-driven architecture
- Debugging capability
- Scalability

### ❌ Tradeoffs
- **Complexity:** More moving parts than traditional databases
- **Consistency:** Projections are eventually consistent (not immediately)
- **Storage:** Events stored forever (large storage)
- **Testing:** More complex to unit test (need mock Kafka)
- **Debugging:** Harder to understand (need to understand event flow)

---

## Best Practices

### 1. Event Naming

```java
// ✅ Good - Clear, past tense, specific
public class PatientCreatedEvent { }
public class PatientStatusChangedEvent { }
public class PatientMergedEvent { }

// ❌ Bad - Unclear, not past tense
public class PatientEvent { }  // Too generic
public class UpdatePatient { }  // Not past tense (looks like command)
public class PatientInfo { }  // Not an event
```

### 2. Event Immutability

```java
// ✅ Good - Immutable event
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientCreatedEvent {
    private final UUID patientId;
    private final String firstName;
    private final String lastName;
    private final Instant timestamp;
    // No setters, fields are final
}

// ❌ Bad - Mutable event
@Data
public class PatientCreatedEvent {
    private UUID patientId;
    private String firstName;
    // Has setters, modifiable
}
```

### 3. Event Versioning

```java
// ✅ Good - Version events
@Data
public class PatientCreatedEvent implements DomainEvent {
    private static final long serialVersionUID = 1L;  // Version 1

    private final UUID patientId;
    private final String firstName;
    private final LocalDate dateOfBirth;
}

// If you need to add fields later:
@Data
public class PatientCreatedEventV2 implements DomainEvent {
    private final UUID patientId;
    private final String firstName;
    private final LocalDate dateOfBirth;
    private final String phoneNumber;  // New field
}
```

### 4. Tenant Isolation

```java
// ✅ Good - Always include tenantId
@Data
public class PatientCreatedEvent {
    private final UUID patientId;
    private final String tenantId;  // IMPORTANT!
    private final String firstName;
}

// ❌ Bad - No tenant isolation
@Data
public class PatientCreatedEvent {
    private final UUID patientId;
    private final String firstName;
    // Tenant context lost!
}
```

### 5. Event Handler Idempotency

```java
// ✅ Good - Idempotent handler
@KafkaListener(topics = "patient-events")
public void handlePatientCreated(PatientCreatedEvent event) {
    // Check if already processed
    if (patientProjection.existsById(event.getPatientId())) {
        log.info("Event already processed: {}", event.getPatientId());
        return;  // Idempotent!
    }

    // Create projection
    PatientReadModel patient = new PatientReadModel();
    patient.setId(event.getPatientId());
    patientProjection.save(patient);
}

// ❌ Bad - Not idempotent
@KafkaListener(topics = "patient-events")
public void handlePatientCreated(PatientCreatedEvent event) {
    // Always creates, even if duplicate
    PatientReadModel patient = new PatientReadModel();
    patient.setId(event.getPatientId());
    patientProjection.save(patient);  // Duplicate if replayed!
}
```

---

## Related Guides

- [CLAUDE.md - Event Sourcing](../../CLAUDE.md) - Quick reference
- [GATEWAY_ARCHITECTURE.md](./GATEWAY_ARCHITECTURE.md) - How events are routed
- [Testing Guide](../development/TESTING_GUIDE.md) - Testing event handlers
- [TDD Swarm Methodology](../development/TDD_SWARM.md) - How Phase 5 was built

---

## Questions & Troubleshooting

### Q: Why separate `-event-service` and `-event-handler-service`?

**A:** Separation of concerns:
- `-event-service` handles REST API and synchronous operations
- `-event-handler-service` handles async event processing

They can be deployed separately, scaled independently, and reused by multiple services.

### Q: What if I need to add a new projection?

**A:** Use event replay service:
1. Create new projection table
2. Run event-replay-service to replay all historical events
3. New projection is populated with historical data

### Q: How do I debug event processing issues?

**A:** Event sourcing helps debugging:
1. Check Event Store for all events (immutable log)
2. Replay events in order to see exact state at any point
3. Check DLQ (dead-letter queue) for failed events

### Q: What happens if handler crashes?

**A:** Kafka manages this:
1. Handler crashes during processing
2. Event not acknowledged (Kafka offset not committed)
3. On restart, handler processes same event again
4. Idempotent handlers prevent duplicate processing

---

_Last Updated: January 19, 2026_
_Version: 1.0_ - Event Sourcing Architecture Guide documenting Phase 4/5 implementation
