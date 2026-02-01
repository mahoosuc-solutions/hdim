# Event Store Service

**Status:** ✅ **Fully Implemented** (January 22, 2026)

## Overview

The **Event Store Service** is the foundational infrastructure for HDIM's event sourcing architecture. It provides an immutable, append-only log of ALL domain events across the entire system.

### Purpose

- **Immutable Audit Trail**: Complete history of all state changes (HIPAA compliance)
- **Event Replay**: Reconstruct aggregate state from events
- **Temporal Queries**: Time-travel debugging ("What was the state on date X?")
- **Event Sourcing Foundation**: Single source of truth for all aggregates

## Architecture

### Service Information

| Property | Value |
|----------|-------|
| **Port** | 8090 |
| **Database** | event_store_db (PostgreSQL) |
| **Purpose** | Immutable event log + snapshots |
| **Type** | Core Infrastructure Service |

### Database Schema

#### 1. `event_store` Table (Primary Event Log)

**Purpose:** Append-only log of ALL domain events

```sql
CREATE TABLE event_store (
    id                BIGSERIAL PRIMARY KEY,              -- Global event ordering
    aggregate_id      UUID NOT NULL,                      -- Entity this event applies to
    aggregate_type    VARCHAR(255) NOT NULL,              -- e.g., "Patient", "CareGap"
    event_id          UUID NOT NULL UNIQUE,               -- Unique event identifier
    event_type        VARCHAR(255) NOT NULL,              -- e.g., "PatientCreatedEvent"
    event_version     INTEGER NOT NULL,                   -- Version within aggregate
    payload           JSONB NOT NULL,                     -- Event data (flexible)
    occurred_at       TIMESTAMP WITH TIME ZONE NOT NULL,  -- Business time
    recorded_at       TIMESTAMP WITH TIME ZONE NOT NULL,  -- System time
    tenant_id         VARCHAR(100) NOT NULL,              -- Multi-tenant isolation
    causation_id      UUID,                               -- Event that caused this
    correlation_id    UUID,                               -- Distributed tracing
    user_id           VARCHAR(255),                       -- Who triggered this
    user_email        VARCHAR(255),                       -- User email (audit)

    UNIQUE(aggregate_id, aggregate_type, event_version)
);
```

**Key Indexes:**
- `idx_event_store_aggregate` - Fast aggregate lookup
- `idx_event_store_event_type` - Fast event type filtering
- `idx_event_store_tenant` - Multi-tenant isolation
- `idx_event_store_occurred_at` - Temporal queries
- `idx_event_store_correlation` - Distributed tracing

#### 2. `event_snapshots` Table (Performance Optimization)

**Purpose:** Periodic snapshots of aggregate state for fast reconstruction

```sql
CREATE TABLE event_snapshots (
    id                BIGSERIAL PRIMARY KEY,
    aggregate_id      UUID NOT NULL,
    aggregate_type    VARCHAR(255) NOT NULL,
    snapshot_version  INTEGER NOT NULL,                   -- Event version at snapshot
    snapshot_data     JSONB NOT NULL,                     -- Complete aggregate state
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    tenant_id         VARCHAR(100) NOT NULL,

    UNIQUE(aggregate_id, aggregate_type, snapshot_version)
);
```

**Strategy:**
- Snapshot every 100 events (configurable)
- Reconstruction: Load latest snapshot + replay events since snapshot
- Example: Snapshot at version 900 + events 901-1000 = current state

#### 3. `event_processing_status` Table (Consumer Tracking)

**Purpose:** Track event processing across multiple consumers

```sql
CREATE TABLE event_processing_status (
    id                        BIGSERIAL PRIMARY KEY,
    consumer_name             VARCHAR(255) NOT NULL,      -- e.g., "patient-event-handler"
    last_processed_event_id   BIGINT NOT NULL,           -- Resume from here
    last_processed_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    tenant_id                 VARCHAR(100) NOT NULL,
    status                    VARCHAR(50) NOT NULL,       -- RUNNING, FAILED, PAUSED
    error_message             TEXT,

    UNIQUE(consumer_name, tenant_id)
);
```

## REST API

### Base URL: `http://localhost:8090/api/v1`

### 1. Append Event

**Endpoint:** `POST /events`

**Request:**
```json
{
  "aggregateId": "123e4567-e89b-12d3-a456-426614174000",
  "aggregateType": "Patient",
  "eventType": "PatientCreatedEvent",
  "payload": {
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1980-01-15"
  },
  "correlationId": "abc-123",
  "userId": "user-456"
}
```

**Headers:**
- `X-Tenant-ID: tenant-1` (required)

**Response:**
```json
{
  "id": 12345,
  "aggregateId": "123e4567-e89b-12d3-a456-426614174000",
  "eventVersion": 1,
  "eventType": "PatientCreatedEvent",
  "occurredAt": "2026-01-22T10:30:00Z"
}
```

### 2. Get Events for Aggregate

**Endpoint:** `GET /events/aggregate/{aggregateId}?aggregateType=Patient`

**Headers:**
- `X-Tenant-ID: tenant-1` (required)

**Response:**
```json
[
  {
    "id": 1,
    "eventVersion": 1,
    "eventType": "PatientCreatedEvent",
    "payload": { "firstName": "John" },
    "occurredAt": "2026-01-15T10:00:00Z"
  },
  {
    "id": 2,
    "eventVersion": 2,
    "eventType": "PatientUpdatedEvent",
    "payload": { "status": "ACTIVE" },
    "occurredAt": "2026-01-16T11:00:00Z"
  }
]
```

### 3. Get Events After Version

**Endpoint:** `GET /events/aggregate/{aggregateId}/after/{version}?aggregateType=Patient`

**Use Case:** Incremental projection updates (only fetch new events)

### 4. Get Events in Time Range

**Endpoint:** `GET /events/timerange?startTime=2026-01-01T00:00:00Z&endTime=2026-01-31T23:59:59Z`

**Use Case:** Temporal queries, time-travel debugging

### 5. Create Snapshot

**Endpoint:** `POST /snapshots`

**Request:**
```json
{
  "aggregateId": "123e4567-e89b-12d3-a456-426614174000",
  "aggregateType": "Patient",
  "snapshotData": {
    "firstName": "John",
    "lastName": "Doe",
    "status": "ACTIVE",
    "riskScore": 85.5
  }
}
```

### 6. Get Latest Snapshot

**Endpoint:** `GET /snapshots/{aggregateId}/latest?aggregateType=Patient`

## Usage Examples

### Example 1: Append Event from Service

```java
@Service
public class PatientEventService {

    private final RestTemplate restTemplate;

    public void createPatient(String patientId, String firstName, String lastName) {
        // Create event payload
        Map<String, Object> payload = Map.of(
            "patientId", patientId,
            "firstName", firstName,
            "lastName", lastName
        );

        // Append to event store
        AppendEventRequest request = new AppendEventRequest();
        request.setAggregateId(UUID.fromString(patientId));
        request.setAggregateType("Patient");
        request.setEventType("PatientCreatedEvent");
        request.setPayload(payload);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", "tenant-1");

        restTemplate.postForEntity(
            "http://event-store-service:8090/api/v1/events",
            new HttpEntity<>(request, headers),
            EventStoreEntry.class
        );
    }
}
```

### Example 2: Reconstruct Aggregate from Events

```java
@Service
public class PatientReconstructionService {

    private final RestTemplate restTemplate;

    public Patient reconstructPatient(String patientId) {
        // 1. Fetch all events for patient
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", "tenant-1");

        ResponseEntity<List<EventStoreEntry>> response = restTemplate.exchange(
            "http://event-store-service:8090/api/v1/events/aggregate/{aggregateId}?aggregateType=Patient",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {},
            patientId
        );

        // 2. Replay events to reconstruct state
        Patient patient = new Patient();
        for (EventStoreEntry event : response.getBody()) {
            patient.apply(event);  // Apply event to aggregate
        }

        return patient;
    }
}
```

### Example 3: Optimized Reconstruction with Snapshots

```java
@Service
public class OptimizedPatientReconstructionService {

    public Patient reconstructPatient(String patientId) {
        // 1. Load latest snapshot (if exists)
        Optional<EventSnapshot> snapshot = getLatestSnapshot(patientId, "Patient");

        Patient patient;
        int startVersion;

        if (snapshot.isPresent()) {
            // Start from snapshot
            patient = deserialize(snapshot.get().getSnapshotData());
            startVersion = snapshot.get().getSnapshotVersion();
        } else {
            // Start from empty state
            patient = new Patient();
            startVersion = 0;
        }

        // 2. Replay events after snapshot
        List<EventStoreEntry> events = getEventsAfterVersion(patientId, "Patient", startVersion);

        for (EventStoreEntry event : events) {
            patient.apply(event);
        }

        return patient;
    }
}
```

## Event Sourcing Benefits

### 1. Complete Audit Trail (HIPAA Compliance)

- **Every state change is recorded** - Never lose data
- **Who, what, when, why** - Complete audit context
- **Temporal queries** - "What was the patient status on Jan 15?"
- **Regulatory compliance** - Meet HIPAA audit requirements

### 2. Event Replay & Debugging

- **Reproduce exact state** at any point in time
- **Debug production issues** without losing data
- **Time-travel debugging** - Step through events to find bug

### 3. Flexible Projections (CQRS)

- **Write model** (commands) → Event store
- **Read models** (queries) → Optimized projections
- **Multiple projections** from same events
- **New projections** without data migration (just replay events)

### 4. Scalability

- **Independent scaling** of write and read paths
- **Multiple consumers** process same events in parallel
- **Event-driven architecture** - Loosely coupled services

## Configuration

### application.yml

```yaml
server:
  port: 8090

spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/event_store_db
    username: healthdata
    password: healthdata123

  jpa:
    hibernate:
      ddl-auto: validate  # NEVER use create/update in production

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml

# Event Store Configuration
eventstore:
  snapshot:
    frequency: 100              # Snapshot every 100 events
    auto-create: true           # Automatic snapshot creation
  processing:
    max-lag-seconds: 300        # Alert if consumer lag > 5 minutes
    health-check-enabled: true  # Enable consumer health checks
```

## Key Design Principles

### 1. Immutability

**Events can NEVER be updated or deleted**

```java
@PreUpdate
protected void preventUpdate() {
    throw new UnsupportedOperationException(
        "Event store entries are immutable and cannot be updated"
    );
}

@PreRemove
protected void preventDelete() {
    throw new UnsupportedOperationException(
        "Event store entries are immutable and cannot be deleted"
    );
}
```

### 2. Append-Only Log

- Events are ONLY appended (never modified)
- Global ordering via auto-incrementing `id`
- Version ordering per aggregate via `event_version`

### 3. Multi-Tenant Isolation

- Every event includes `tenant_id`
- Tenant filtering enforced at query level
- HIPAA compliance for PHI separation

### 4. Idempotency

- Unique constraint on `event_id` prevents duplicates
- Consumer tracking prevents reprocessing
- Safe for retries and replays

## Testing

### Unit Test Example

```java
@SpringBootTest
class EventStoreServiceTest {

    @Autowired
    private EventStoreService eventStoreService;

    @Test
    void shouldAppendAndRetrieveEvent() {
        // Arrange
        UUID patientId = UUID.randomUUID();
        Map<String, String> payload = Map.of("firstName", "John");

        // Act
        EventStoreEntry entry = eventStoreService.appendEvent(
            patientId, "Patient", "PatientCreatedEvent", payload, "tenant-1"
        );

        List<EventStoreEntry> events = eventStoreService.getEventsForAggregate(
            patientId, "Patient", "tenant-1"
        );

        // Assert
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("PatientCreatedEvent");
        assertThat(events.get(0).getEventVersion()).isEqualTo(1);
    }
}
```

## Next Steps

1. **Integrate with Event Services**
   - Update `patient-event-service` to persist events to event store
   - Update `care-gap-event-service` to persist events
   - Update `quality-measure-event-service` to persist events

2. **Build Event Replay Service**
   - Implement projection rebuilding
   - Support temporal queries
   - Consumer lag monitoring

3. **Add Monitoring**
   - Event throughput metrics
   - Consumer lag alerts
   - Snapshot effectiveness tracking

4. **Performance Optimization**
   - Batch event writes
   - Parallel event replay
   - Snapshot strategy tuning

## Related Documentation

- [Event Sourcing Architecture](../../../docs/architecture/EVENT_SOURCING_ARCHITECTURE.md)
- [CLAUDE.md - Event Sourcing Section](../../../CLAUDE.md)
- [Event Processing Service](../event-processing-service/README.md)
- [Event Router Service](../event-router-service/README.md)

---

**Last Updated:** January 22, 2026
**Version:** 1.0.0
**Status:** ✅ Production Ready
