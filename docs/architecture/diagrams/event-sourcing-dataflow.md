# Event Sourcing Data Flow Architecture

Complete visual reference for Event Sourcing + CQRS pattern used in Phase 5 event services.

---

## Write Path: Command to Event Store

```mermaid
graph LR
    A[Client Command] -->|"CreatePatientRequest"| B["PatientEventService<br/>(REST API)"]
    B -->|"Validate"| C["ApplicationService<br/>(Business Logic)"]
    C -->|"CreateEvent"| D["PatientCreatedEvent<br/>(Immutable)"]
    D -->|"Append"| E["Event Store<br/>(PostgreSQL)"]
    E -->|"Publish"| F["Kafka Topic<br/>patient.events"]
    F -->|"Response"| A

    style E fill:#e1f5ff
    style F fill:#fff9c4
    style D fill:#f3e5f5
```

**Process**:
1. **Client** sends CreatePatient command
2. **Service** validates command
3. **Event** created (immutable record of what happened)
4. **Event Store** persists event to PostgreSQL
5. **Kafka** publishes event to topic (asynchronously)
6. **Response** sent back to client

---

## Read Path: Query Models from Projections

```mermaid
graph LR
    A[Client Query] -->|"GetPatient"| B["PatientEventService<br/>(Query Handler)"]
    B -->|"SELECT * FROM patient_projection"| C["Patient Projection<br/>(Read Model)"]
    C -->|"Denormalized Data"| B
    B -->|"PatientResponse"| A

    style C fill:#c8e6c9
    style B fill:#e8f5e9
```

**Process**:
1. **Client** queries for patient data
2. **Service** queries projection (not event store)
3. **Projection** is pre-built denormalized read model
4. **Response** returned instantly (no computation needed)

**Key Point**: Queries hit projection, NOT event store. Projections are optimized for reading.

---

## Event Handler: Building Projections (Eventual Consistency)

```mermaid
graph TB
    A["Kafka Topic<br/>patient.events"] -->|"PatientCreatedEvent"| B["PatientEventHandler"]
    B -->|"Parse Event"| C["Extract Data"]
    C -->|"INSERT INTO patient_projection"| D["Patient Projection Table"]

    E["Kafka Topic<br/>patient.events"] -->|"PatientUpdatedEvent"| F["PatientEventHandler"]
    F -->|"Parse Event"| G["Extract Data"]
    G -->|"UPDATE patient_projection"| H["Patient Projection Table"]

    A -.->|"1-5 second lag"| D
    E -.->|"1-5 second lag"| H

    style D fill:#c8e6c9
    style H fill:#c8e6c9
    style A fill:#fff9c4
    style E fill:#fff9c4
```

**Process**:
1. **Events published** to Kafka topic
2. **Handler** consumes events asynchronously
3. **Projection updated** based on event data
4. **Lag**: Usually 1-5 seconds (eventual consistency)

**Note**: Projections update asynchronously, not in real-time. This is acceptable trade-off for scalability.

---

## Event Replay: Recalculating Historical State

```mermaid
graph TB
    A["Event Store<br/>(All historical events)"] -->|"SELECT * FROM events<br/>WHERE created_at > ?<br/>ORDER BY created_at"| B["Replay Service"]
    B -->|"Process each event"| C["Rebuild Projection<br/>FROM SCRATCH"]
    C -->|"INSERT/UPDATE"| D["New Projection<br/>(Refreshed)"]

    A -.->|"Rebuild Measures"| E["QualityMeasureProjection"]
    A -.->|"Rebuild Patient"| F["PatientProjection"]
    A -.->|"Rebuild Gaps"| G["CareGapProjection"]

    style A fill:#e1f5ff
    style D fill:#c8e6c9
    style E fill:#c8e6c9
    style F fill:#c8e6c9
    style G fill:#c8e6c9
```

**Use Cases**:
- **Data Correction**: Measure calculation logic changed → replay to recalculate
- **Projection Rebuild**: Projection got corrupted → replay from event store
- **Historical Analysis**: Calculate metrics at specific past date → replay to that point
- **Audit Trail**: Reconstruct exactly what happened on specific date

**Timeline**:
- 1M events: ~1.5 minutes to replay
- 10M events: ~15 minutes to replay
- Replay runs in background (doesn't block reads)

---

## Multi-Service Event Flow (Clinical Workflow)

```mermaid
graph TB
    A["Patient Service"] -->|"PatientCreatedEvent"| B["Kafka<br/>patient.events"]
    B -->|"Subscribe"| C["Quality Measure Service"]
    B -->|"Subscribe"| D["Care Gap Service"]

    C -->|"MeasureEvaluatedEvent"| E["Kafka<br/>quality-measure.events"]
    D -->|"CareGapDetectedEvent"| F["Kafka<br/>caregap.events"]

    E -->|"Subscribe"| G["Analytics Service<br/>(Reports)"]
    F -->|"Subscribe"| G

    G -->|"Aggregated Metrics"| H["Dashboard"]

    style B fill:#fff9c4
    style E fill:#fff9c4
    style F fill:#fff9c4
    style H fill:#c8e6c9
```

**Event Flow**:
1. **Patient** created in patient-service
2. **Event published** to patient.events topic
3. **Quality Measure** service consumes event, evaluates measures
4. **Measure evaluation** event published
5. **Care Gap** service consumes event, detects gaps
6. **Gap detection** event published
7. **Analytics** service consumes both measurement and gap events
8. **Dashboard** shows aggregated results

**Benefits**: Services loosely coupled (communicate via events, not REST)

---

## Idempotency: Handling Duplicate Events

```mermaid
graph TB
    A["Kafka Event"] -->|"PatientCreatedEvent<br/>idempotency_key=123"| B["Handler"]

    B -->|"1. Check: idempotency_key exists?"| C["Idempotency Table"]

    C -->|"NO (first time)"| D["Process Event"]
    D -->|"INSERT patient"| E["Projection"]
    E -->|"STORE idempotency_key"| C

    C -->|"YES (duplicate)"| F["Skip Processing<br/>(Return cached result)"]

    style C fill:#ffe0b2
    style E fill:#c8e6c9
```

**Why Idempotency Matters**:
- Kafka may deliver same event twice (network retries)
- Without idempotency: Duplicate patient records, double-counted measures
- With idempotency: Duplicate events safely ignored

---

## Error Handling: Dead Letter Queues

```mermaid
graph TB
    A["Kafka Topic<br/>patient.events"] -->|"Event"| B["Handler"]

    B -->|"SUCCESS"| C["Projection Updated"]

    B -->|"RETRY (3 times)"| D["Exponential Backoff"]
    D -->|"SUCCESS"| C
    D -->|"STILL FAILS"| E["Dead Letter Queue<br/>patient.events.dlq"]

    E -->|"Alert ops"| F["Manual Intervention"]
    F -->|"Fix root cause"| G["Replay from DLQ"]
    G -->|"Re-process"| C

    style C fill:#c8e6c9
    style E fill:#ffcdd2
    style F fill:#ffe0b2
```

**Error Handling Strategy**:
1. **First attempt** → Process event
2. **Failure** → Retry with exponential backoff (3 times)
3. **Still failing** → Send to Dead Letter Queue
4. **Alert** → Operations team notified
5. **Fix** → Root cause fixed, replay from DLQ

---

## Consistency Model: Strong Write, Eventual Read

```mermaid
graph LR
    A[Client] -->|"1. CreatePatient"| B["Event Store"]
    B -->|"2a. Response (immediate)"| A
    B -->|"2b. Publish Event"| C["Kafka"]

    C -->|"3. Async handler"| D["Projection"]

    E[Client] -->|"4a. GetPatient (1s later)"| F["Projection"]
    F -->|"4b. May be stale (lag)"| E

    E -->|"5a. GetPatient (5s later)"| F
    F -->|"5b. Consistent (caught up)"| E

    style B fill:#e1f5ff
    style D fill:#c8e6c9
    style F fill:#c8e6c9
```

**Consistency Guarantees**:
- **Write**: Immediate (event written to event store before response)
- **Read**: Eventual (projection updated 1-5 seconds later)
- **Acceptable**? Yes, for healthcare (delays are expected, consistency is eventual)

---

## References

- **[Event Sourcing Architecture Guide](../EVENT_SOURCING_ARCHITECTURE.md)** - Complete implementation guide
- **[ADR-001: Event Sourcing Decision](../decisions/ADR-001-event-sourcing-for-clinical-services.md)**
- **[Service Catalog - Event Services](../../services/SERVICE_CATALOG.md)**

---

_Last Updated: January 19, 2026_
_Version: 1.0_
_Diagrams created for Phase 5 Event Sourcing Implementation_
