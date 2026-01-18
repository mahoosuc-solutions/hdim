# Phase 3: Event Sourcing & CQRS Architecture

**Status**: Ready for TDD Swarm Implementation
**Completion Target**: 300+ unit tests, 50+ integration tests
**Timeline**: 4 concurrent teams, 2-3 weeks

---

## Executive Summary

Phase 3 implements Event Sourcing and Command Query Responsibility Segregation (CQRS) as the foundation for the quality measure evaluation engine. This phase decouples command handling (write operations) from query operations, enabling:

- **Immutable audit trail**: All state changes stored as events
- **Event replay**: Reconstruct any historical state
- **Temporal queries**: Answer "how many patients had diabetes on [date]?"
- **Scalable reads**: Multiple independent projection services
- **HIPAA compliance**: Complete audit trail for compliance reporting

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Application                       │
└────────────────┬──────────────────────────┬─────────────────┘
                 │                          │
       ┌─────────▼─────────┐      ┌───────▼──────────┐
       │  Write Side (CQRS)│      │ Read Side (CQRS) │
       │                   │      │                  │
       │ CommandHandlers   │      │ QueryServices    │
       │ (Create/Update)   │      │ (Read-only)      │
       └─────────┬─────────┘      └───────┬──────────┘
                 │                        │
       ┌─────────▼──────────────────────┐ │
       │   Event Sourcing Foundation    │ │
       │                                │ │
       │ ┌──────────────────────────┐   │ │
       │ │ Domain Events (immutable)│   │ │
       │ │ - PatientCreatedEvent    │   │ │
       │ │ - ConditionDiagnosedEvent   │ │
       │ │ - MedicationPrescribedEvent │ │
       │ └──────────────────────────┘   │ │
       │                                │ │
       │ ┌──────────────────────────┐   │ │
       │ │ Event Store (PostgreSQL) │   │ │
       │ │ - Immutable log          │   │ │
       │ │ - Append-only            │   │ │
       │ │ - TTL expiration         │   │ │
       │ └──────────────────────────┘   │ │
       │                                │ │
       │ ┌──────────────────────────┐   │ │
       │ │ Event Replay Engine      │   │ │
       │ │ - Reconstruct state      │   │ │
       │ │ - Temporal queries       │   │ │
       │ └──────────────────────────┘   │ │
       └────────┬─────────────────────────┤ │
                │                         │ │
    ┌───────────▼──────────┐   ┌─────────▼─┘
    │  Kafka Event Stream  │   │
    │  (Event Replay)      │   │
    └───────────┬──────────┘   │
                │              │
    ┌───────────▼──────────────┴──────────────┐
    │  Projection Services (Read Models)      │
    │                                         │
    │ ┌──────────────────────────────────┐   │
    │ │ PatientProjectionService         │   │
    │ │ - Denormalized patient view      │   │
    │ │ - Fast queries                   │   │
    │ │ - Redis cache                    │   │
    │ └──────────────────────────────────┘   │
    │                                         │
    │ ┌──────────────────────────────────┐   │
    │ │ MeasureEvaluationProjection      │   │
    │ │ - Pre-computed measure scores    │   │
    │ │ - Aggregations by cohort         │   │
    │ └──────────────────────────────────┘   │
    │                                         │
    │ ┌──────────────────────────────────┐   │
    │ │ CareGapProjectionService         │   │
    │ │ - Identified care gaps           │   │
    │ │ - Interventions needed           │   │
    │ └──────────────────────────────────┘   │
    └─────────────────────────────────────────┘
```

---

## Phase 3 Teams & Deliverables

### **Team 3.1: Event Sourcing Foundation (35+ Tests)**

**Responsibility**: Define domain events and event handling infrastructure

**Deliverables**:
- ✓ Base event classes (`DomainEvent`, `Event<T>`)
- ✓ Event metadata (timestamp, version, correlation ID)
- ✓ Event handler interfaces (`EventHandler<T>`)
- ✓ Event publisher interface
- ✓ Domain event types for healthcare:
  - `PatientCreatedEvent`
  - `ConditionDiagnosedEvent`
  - `MedicationPrescribedEvent`
  - `ObservationRecordedEvent`
  - `CarePlanModifiedEvent`

**Files to Create**:
```
backend/modules/shared/domain/event-sourcing/src/main/java/
├── com/healthdata/eventsourcing/
│   ├── event/
│   │   ├── DomainEvent.java (base class)
│   │   ├── Event.java (generic interface)
│   │   ├── EventMetadata.java
│   │   └── HealthcareEvents.java (all event types)
│   ├── handler/
│   │   ├── EventHandler.java
│   │   ├── EventHandlerRegistry.java
│   │   └── EventPublisher.java
│   └── exception/
│       ├── EventHandlingException.java
│       └── EventPublishingException.java

Tests: 35+
- Event metadata serialization
- Event handler registration
- Event publishing mechanics
- Multi-tenant event isolation
- Event correlation tracking
```

**Test Coverage**:
- Event creation and validation
- Metadata assignment (timestamp, version, correlation)
- Handler registration and invocation
- Publisher state management
- Error handling and replay

---

### **Team 3.2: Event Store Implementation (50+ Tests)**

**Responsibility**: Implement immutable event storage in PostgreSQL

**Deliverables**:
- ✓ Event store entity and repository
- ✓ Liquibase migrations for event tables
- ✓ Event serialization/deserialization (JSON)
- ✓ Snapshot mechanism for performance
- ✓ TTL-based cleanup for HIPAA compliance
- ✓ Concurrent event appending (no lost writes)

**Files to Create**:
```
backend/modules/services/event-store-service/src/main/java/
├── com/healthdata/eventstore/
│   ├── domain/
│   │   ├── StoredEvent.java (JPA entity)
│   │   ├── EventSnapshot.java
│   │   └── EventStoreRepository.java
│   ├── service/
│   │   ├── EventStoreService.java
│   │   ├── EventSerializationService.java
│   │   └── SnapshotService.java
│   ├── controller/
│   │   └── EventStoreController.java
│   └── config/
│       └── EventStoreConfiguration.java

database/db/changelog/
├── 0001-enable-extensions.xml
├── 0002-create-event-store-table.xml
├── 0003-create-event-snapshot-table.xml
├── 0004-create-event-indexes.xml
└── db.changelog-master.xml

Tests: 50+
- Event persistence
- Concurrent writes (version control)
- Snapshot creation/retrieval
- TTL cleanup (HIPAA)
- Serialization edge cases
- Index performance
```

**Key Constraints**:
- Append-only: Events never updated or deleted (except by TTL)
- Version control: Prevent lost writes with optimistic locking
- Snapshots: Reduce replay time (every 100 events)
- TTL: Delete events older than 7 years (HIPAA)
- Indexes: Efficient queries by aggregate ID, event type, timestamp

---

### **Team 3.3: Event Replay & Projection Services (45+ Tests)**

**Responsibility**: Replay events to reconstruct state and drive projections

**Deliverables**:
- ✓ Event replay engine
- ✓ Projection manager
- ✓ Replay strategy (from snapshot vs. from beginning)
- ✓ Idempotent projection updates
- ✓ Projection version tracking
- ✓ Temporal query support ("state as of [date]")

**Files to Create**:
```
backend/modules/services/event-replay-service/src/main/java/
├── com/healthdata/eventreplay/
│   ├── service/
│   │   ├── EventReplayEngine.java
│   │   ├── ProjectionManager.java
│   │   ├── ReplayStrategy.java
│   │   └── TemporalQueryService.java
│   ├── domain/
│   │   ├── ReplayProgress.java
│   │   └── ProjectionState.java
│   ├── handler/
│   │   └── ReplayEventHandler.java
│   └── config/
│       └── ReplayConfiguration.java

Tests: 45+
- Complete event replay
- Partial replay (from snapshot)
- Idempotent projection updates
- Temporal state reconstruction
- Concurrent replay safety
- Error recovery and resumption
```

**Performance Targets**:
- Replay 1M events in < 30 seconds
- Idempotent updates: Can replay same events multiple times safely
- Temporal queries: < 1 second response time

---

### **Team 3.4: CQRS Query Services (40+ Tests)**

**Responsibility**: Implement read-only query services backed by projections

**Deliverables**:
- ✓ PatientQueryService (search, filter patients)
- ✓ MeasureEvaluationQueryService (pre-computed scores)
- ✓ CareGapQueryService (identified gaps per patient)
- ✓ CohortAnalyticsQueryService (aggregations)
- ✓ Query result caching (Redis)
- ✓ Temporal snapshots (historical states)

**Files to Create**:
```
backend/modules/services/query-service/src/main/java/
├── com/healthdata/queryservice/
│   ├── service/
│   │   ├── PatientQueryService.java
│   │   ├── MeasureEvaluationQueryService.java
│   │   ├── CareGapQueryService.java
│   │   └── CohortAnalyticsQueryService.java
│   ├── dto/
│   │   ├── PatientSearchResult.java
│   │   ├── MeasureScoreResult.java
│   │   └── CareGapResult.java
│   ├── controller/
│   │   ├── PatientQueryController.java
│   │   ├── MeasureQueryController.java
│   │   └── CareGapQueryController.java
│   └── cache/
│       └── QueryResultCacheService.java

Tests: 40+
- Patient search and filtering
- Measure score queries
- Care gap identification
- Cohort aggregations
- Cache hit rates
- Concurrent query handling
```

---

## Integration Points

### **Phase 2 → Phase 3 Integration**
```
Phase 2.0 (Complete)
├── Authentication (Team 1.9)
├── Rate Limiting (Team 1)
├── Audit Logging (Team 2)
└── Token Management (Team 3)
    └──────────────┬──────────────┘
                   ▼
         Phase 3 (Event Sourcing)
         ├── Audit Log → Events
         ├── Commands → Events
         └── Events → Projections
```

**Key Integration**:
- AuditLogService logs all commands (create patient, diagnose condition)
- CommandHandlers transform commands → domain events
- Events published to Kafka
- Projection services consume events, update read models
- QueryServices query projections (no event replay on read path)

### **PostgreSQL Schema Evolution**
```
Phase 2.0 Databases:
├── gateway_db (authentication, tokens)
├── patient_db (patient entities)
├── quality_db (quality measures)
└── fhir_db (FHIR resources)

Phase 3 Databases (new):
├── event_store_db (immutable events)
├── projection_db (read models)
└── query_db (denormalized views)
```

---

## Testing Strategy

### **Test Pyramid by Team**

**Team 3.1 (Event Sourcing Foundation)**
- 25 unit tests (event creation, metadata, handlers)
- 8 integration tests (handler invocation, error cases)
- 2 contract tests (interface compliance)

**Team 3.2 (Event Store)**
- 30 unit tests (serialization, snapshots, cleanup)
- 18 integration tests (persistence, concurrency, TTL)
- 2 database schema tests (migrations, rollback)

**Team 3.3 (Event Replay)**
- 28 unit tests (replay engine, projection updates)
- 15 integration tests (full replay, partial replay, temporal)
- 2 performance tests (1M event replay)

**Team 3.4 (Query Services)**
- 24 unit tests (queries, filtering, aggregations)
- 14 integration tests (multi-tenant queries, caching)
- 2 performance tests (query latency, cache efficiency)

**Total: 300+ unit tests + 50+ integration tests**

---

## Performance Targets

| Metric | Target | Phase 2 Baseline |
|--------|--------|------------------|
| Event publish latency | < 10ms | N/A (new) |
| Event replay (1M events) | < 30s | N/A (new) |
| Projection update latency | < 50ms | N/A (new) |
| Query response time (p95) | < 100ms | Phase 2: < 50ms |
| Cache hit rate | > 80% | Phase 2: > 90% |
| Concurrent event writes | 1000/sec | N/A (new) |

---

## HIPAA Compliance Requirements

### **Event Store**
- ✓ Immutable: Events cannot be modified after creation
- ✓ TTL: Automatic cleanup after 7 years
- ✓ Audit Trail: All events logged with timestamp, user, tenant
- ✓ Encryption: Events encrypted at rest
- ✓ Access Control: Multi-tenant isolation enforced

### **Projections**
- ✓ Derived Data: Projections derived from events, not primary
- ✓ Data Minimization: Only necessary fields in projections
- ✓ Temporal Access: Can query historical states
- ✓ Audit: Which projections were queried, by whom, when

---

## TDD Swarm Execution Plan

### **Week 1: Teams 3.1 + 3.2 (Parallel)**

**Team 3.1**: Event Sourcing Foundation
- [ ] Write 35+ unit tests (TDD)
- [ ] Implement domain event classes
- [ ] Implement event handlers
- [ ] Commit to `feature/phase3-team1-events`

**Team 3.2**: Event Store
- [ ] Write 50+ unit tests (TDD)
- [ ] Implement StoredEvent entity
- [ ] Create Liquibase migrations
- [ ] Implement snapshot mechanism
- [ ] Commit to `feature/phase3-team2-eventstore`

### **Week 2: Merge 3.1 + 3.2 to master**
- Resolve conflicts (if any)
- Validate integration between teams
- Run full integration test suite

### **Week 2-3: Teams 3.3 + 3.4 (Parallel)**

**Team 3.3**: Event Replay
- [ ] Write 45+ unit tests (TDD)
- [ ] Implement replay engine
- [ ] Implement projection manager
- [ ] Performance testing (1M events)
- [ ] Commit to `feature/phase3-team3-replay`

**Team 3.4**: Query Services
- [ ] Write 40+ unit tests (TDD)
- [ ] Implement patient queries
- [ ] Implement measure queries
- [ ] Implement cache layer
- [ ] Commit to `feature/phase3-team4-queries`

### **Week 3: Merge 3.3 + 3.4 to master**
- Final integration with Phase 2.0
- Performance validation
- HIPAA compliance verification
- Ready for Phase 4

---

## Success Criteria

✅ **Code Quality**
- [ ] 300+ unit tests (all green)
- [ ] 50+ integration tests (all green)
- [ ] > 80% code coverage
- [ ] Zero warnings in main code compilation

✅ **Performance**
- [ ] Event replay: 1M events in < 30s
- [ ] Queries: p95 < 100ms
- [ ] No memory leaks (large datasets)
- [ ] Cache hit rate > 80%

✅ **HIPAA Compliance**
- [ ] Immutable event store
- [ ] 7-year TTL cleanup
- [ ] Complete audit trail
- [ ] Multi-tenant isolation

✅ **Architecture**
- [ ] CQRS fully implemented
- [ ] Event sourcing foundation complete
- [ ] Projections scalable
- [ ] Temporal queries supported

---

## Git Worktree Strategy

```bash
# Create isolated worktrees for parallel development
git worktree add ../phase3-team1 -b feature/phase3-team1-events master
git worktree add ../phase3-team2 -b feature/phase3-team2-eventstore master
git worktree add ../phase3-team3 -b feature/phase3-team3-replay master
git worktree add ../phase3-team4 -b feature/phase3-team4-queries master

# Sequential merge strategy
master <- Team 3.1
master <- Team 3.2 (after 3.1 merged)
master <- Team 3.3 (after 3.2 merged)
master <- Team 3.4 (after 3.3 merged)
```

---

## Known Dependencies & Risks

### **Dependencies on Phase 2.0**
- ✓ Authentication (required for AuditLog user context)
- ✓ Audit Logging (all events logged through AuditLogService)
- ✓ Multi-tenant isolation (enforced in event store)

### **Risks & Mitigations**
1. **Concurrent Event Writes**
   - Risk: Lost writes, duplicate events
   - Mitigation: Optimistic locking, unique constraints

2. **Event Store Growth**
   - Risk: PostgreSQL grows 10GB+/month
   - Mitigation: TTL cleanup, snapshot strategy

3. **Replay Performance**
   - Risk: 1M+ event replay takes > 60s
   - Mitigation: Snapshot every 100 events

4. **Projection Lag**
   - Risk: Queries see stale data
   - Mitigation: Async projections with version tracking

---

## Next Steps

1. **Approve Phase 3 architecture** (this document)
2. **Create git worktrees** for 4 teams
3. **Write test files** for all teams (TDD)
4. **Begin Team 3.1 implementation**
5. **Run daily syncs** to resolve dependencies

---

**Ready to Begin Phase 3 TDD Swarm? Y/N**

Estimated Duration: 2-3 weeks
Test Target: 300+ unit tests
Merge Strategy: Sequential (3.1 → 3.2 → 3.3 → 3.4)
