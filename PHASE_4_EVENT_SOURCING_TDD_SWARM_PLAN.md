# Phase 4 Event Sourcing TDD Swarm - Service-Specific Event Handlers

**Status**: 🚀 READY TO BEGIN
**Date**: January 18, 2026
**Build upon**: Phase 3 Event Sourcing Foundation (216+ tests, 4 services)
**Methodology**: Test-Driven Development (TDD) with Git Worktrees
**Target**: 4 Teams × 30+ tests each = **140+ healthcare-specific event handlers**

---

## Vision

Phase 3 built the **event sourcing infrastructure** (event store, replay engine, projections, CQRS queries).

Phase 4 builds the **healthcare domain logic** - services that emit, handle, and project healthcare-specific events into actionable business queries.

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│         Healthcare Domain Events (Phase 4)              │
│                                                         │
│  PatientCreated       MeasureEvaluated   CareGapFound  │
│  DemographicsUpdated  ScoreCalculated    ActionCreated │
│  EnrollmentChanged    RiskScored         GapClosed     │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│      Event Handlers (Create Projections) [Phase 4]      │
│                                                         │
│  PatientEventHandler → PatientProjection               │
│  MeasureEventHandler → MeasureProjection               │
│  CareGapEventHandler → CareGapProjection               │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│   Event Sourcing Infrastructure (Phase 3) ✅ COMPLETE   │
│                                                         │
│  EventStore  ReplayEngine  ProjectionManager  Queries  │
└─────────────────────────────────────────────────────────┘
```

---

## Team Breakdown

### Phase 4 Team 4.1: Patient Lifecycle Events
**Focus**: Patient creation, demographics, enrollment events
**Services**: patient-event-handler-service
**Tests Target**: 35+ (RED → GREEN)

**Events to Implement**:
- PatientCreatedEvent
- PatientDemographicsUpdatedEvent
- PatientEnrollmentChangedEvent
- PatientActivatedEvent
- PatientDeactivatedEvent

**Event Handlers**:
- PatientEventHandler (processes above events)
- PatientProjectionUpdater (maintains read model)
- PatientLifecycleService (orchestrates)

**Projections Created**:
- PatientActiveProjection (patient_active table)
- PatientDemographicsProjection (patient_demographics table)
- EnrollmentStatusProjection (enrollment_status table)

**Domain Queries Enabled** (Phase 3 CQRS):
- PatientQueryService.searchByFirstName()
- PatientQueryService.filterByEnrollmentStatus()

---

### Phase 4 Team 4.2: Quality Measure Events
**Focus**: Measure evaluation, scoring, quality metrics
**Services**: quality-measure-event-handler-service
**Tests Target**: 35+ (RED → GREEN)

**Events to Implement**:
- MeasureEvaluatedEvent
- MeasureScoreCalculatedEvent
- MeasureNumeratorStatusEvent
- MeasureDenominatorStatusEvent
- RiskScoreCalculatedEvent

**Event Handlers**:
- MeasureEventHandler (processes evaluations)
- QualityMetricsUpdater (maintains aggregates)
- RiskScoringService (calculates risk)

**Projections Created**:
- MeasureEvaluationProjection (measure_evaluations table)
- QualityCohortProjection (cohort_quality_metrics table)
- RiskScoreProjection (patient_risk_scores table)

**Domain Queries Enabled** (Phase 3 CQRS):
- MeasureEvaluationQueryService.getMeasureScores()
- MeasureEvaluationQueryService.getMeasureRate()
- MeasureEvaluationQueryService.getNumeratorPercentage()

---

### Phase 4 Team 4.3: Care Gap Events
**Focus**: Care gap identification, action assignment, gap closure
**Services**: care-gap-event-handler-service
**Tests Target**: 35+ (RED → GREEN)

**Events to Implement**:
- CareGapIdentifiedEvent
- CareGapActionCreatedEvent
- CareGapActionAssignedEvent
- CareGapActionCompletedEvent
- CareGapClosedEvent

**Event Handlers**:
- CareGapEventHandler (processes gap events)
- CareGapActionHandler (manages actions)
- NotificationService (alerts clinicians)

**Projections Created**:
- CareGapProjection (care_gaps table)
- CareGapActionProjection (care_gap_actions table)
- CareGapStatusProjection (gap_status_tracking table)

**Domain Queries Enabled**:
- CareGapQueryService.getOpenGapsByPatient()
- CareGapQueryService.getGapsByStatus()
- CareGapQueryService.getActionsByDueDate()

---

### Phase 4 Team 4.4: Clinical Workflow Events
**Focus**: Workflow execution, step completion, outcomes
**Services**: clinical-workflow-event-handler-service
**Tests Target**: 35+ (RED → GREEN)

**Events to Implement**:
- WorkflowInitiatedEvent
- WorkflowStepStartedEvent
- WorkflowStepCompletedEvent
- WorkflowOutcomeRecordedEvent
- WorkflowClosedEvent

**Event Handlers**:
- WorkflowEventHandler (processes workflow events)
- WorkflowOrchestrator (manages workflow state)
- OutcomeAggregator (tracks clinical outcomes)

**Projections Created**:
- WorkflowInstanceProjection (workflow_instances table)
- WorkflowStepProjection (workflow_steps table)
- ClinicalOutcomeProjection (clinical_outcomes table)

**Domain Queries Enabled**:
- WorkflowQueryService.getActiveWorkflows()
- WorkflowQueryService.getOutcomesByType()
- WorkflowQueryService.getPerformanceMetrics()

---

## TDD Swarm Execution Plan

### Timeline

```
Week 1: RED Phase + Module Setup
├─ Team 4.1: Write 35+ patient event tests
├─ Team 4.2: Write 35+ quality measure tests
├─ Team 4.3: Write 35+ care gap tests
└─ Team 4.4: Write 35+ workflow tests

Week 2: GREEN Phase + Implementation
├─ Team 4.1: Implement PatientEventHandler, projections, queries
├─ Team 4.2: Implement MeasureEventHandler, risk scoring
├─ Team 4.3: Implement CareGapEventHandler, action management
└─ Team 4.4: Implement WorkflowEventHandler, outcome aggregation

Week 3: Integration & Merge
├─ Verify cross-service event flow
├─ Merge all 4 teams to master (sequential)
├─ Run integration test suite
└─ Validate Phase 4 complete: 140+ tests, 4 services
```

### Parallel Development

All 4 teams run **simultaneously**:
- **No dependencies** between teams (all depend on Phase 3, which is complete)
- **Isolated worktrees** prevent merge conflicts
- **Independent databases** (queue_db, quality_db, caregap_db, workflow_db)
- **Parallel feature branches** ready for sequential merge

### Git Worktree Setup

```bash
# Create isolated worktrees for each team
git worktree add phase4-team1 master  # Patient events
git worktree add phase4-team2 master  # Quality measure events
git worktree add phase4-team3 master  # Care gap events
git worktree add phase4-team4 master  # Clinical workflow events

# Each team develops independently:
cd phase4-team1 && git checkout -b feature/phase4-team1-patient-events
cd phase4-team2 && git checkout -b feature/phase4-team2-quality-events
cd phase4-team3 && git checkout -b feature/phase4-team3-caregap-events
cd phase4-team4 && git checkout -b feature/phase4-team4-workflow-events
```

---

## Module Structure (for each team)

```
backend/modules/services/{service-name}/
├── build.gradle.kts
│   └── Dependencies on Phase 3 infrastructure (event-sourcing, persistence)
│
├── src/main/java/com/healthdata/{service}/
│   ├── event/                          # Domain events
│   │   ├── PatientCreatedEvent.java
│   │   ├── PatientDemographicsUpdatedEvent.java
│   │   └── ...
│   │
│   ├── eventhandler/                   # Event handlers
│   │   ├── PatientEventHandler.java
│   │   ├── EventHandlerConfig.java
│   │   └── ...
│   │
│   ├── projection/                     # Projection updaters
│   │   ├── PatientProjectionUpdater.java
│   │   ├── EnrollmentProjectionUpdater.java
│   │   └── ...
│   │
│   ├── domain/                         # Domain logic
│   │   ├── PatientLifecycleService.java
│   │   ├── repository/
│   │   └── model/
│   │
│   └── api/
│       └── EventPublisherController.java  # Emit test events
│
└── src/test/java/com/healthdata/{service}/
    ├── {Service}EventHandlerTest.java
    ├── {Service}ProjectionUpdaterTest.java
    ├── {Service}LifecycleTest.java
    ├── {Service}IntegrationTest.java
    └── mock/
        └── Mock{Service}EventStore.java
```

---

## Test-Driven Development Pattern

### RED Phase (Write Tests First)

Example: PatientEventHandlerTest

```java
@Test
@DisplayName("Should create patient projection from PatientCreatedEvent")
void testPatientCreationProjection() {
    // Given: Patient creation event
    String patientId = "PATIENT-001";
    PatientCreatedEvent event = new PatientCreatedEvent(
        patientId, "TENANT-001", "John", "Doe", "1965-01-01"
    );

    // When: Event is handled
    PatientEventHandler handler = new PatientEventHandler(projectionStore, eventStore);
    handler.handle(event);

    // Then: Projection should be created
    PatientActiveProjection projection = projectionStore.getPatientProjection(patientId, "TENANT-001");
    assertThat(projection.getFirstName()).isEqualTo("John");
    assertThat(projection.getLastName()).isEqualTo("Doe");
    assertThat(projection.getStatus()).isEqualTo("ACTIVE");
}
```

### GREEN Phase (Implement Minimal Code)

```java
public class PatientEventHandler {
    private final ProjectionStore projectionStore;
    private final EventStore eventStore;

    public void handle(PatientCreatedEvent event) {
        // Create projection from event
        PatientActiveProjection projection = new PatientActiveProjection(
            event.getPatientId(),
            event.getTenantId(),
            event.getFirstName(),
            event.getLastName(),
            "ACTIVE"
        );
        projectionStore.saveProjection(projection);

        // Store event (append-only)
        eventStore.storeEvent(event);
    }
}
```

---

## Key Design Principles

### 1. Event Immutability
All domain events are final, immutable value objects

### 2. Idempotency
All event handlers are idempotent (safe to replay multiple times)

### 3. Temporal Consistency
All projections include version/timestamp for consistency verification

### 4. Multi-Tenant Isolation
Every event includes tenantId; all queries filtered by tenant

### 5. HIPAA Compliance
- Event store with 7-year retention
- Projection cache TTL ≤ 5 minutes
- Audit trails for all clinical events
- No PHI in non-clinical events

### 6. Performance
- Events published to Kafka (async)
- Projections updated incrementally
- Cache invalidation on event replay

---

## Success Criteria

| Criterion | Target | Measurement |
|-----------|--------|-------------|
| **Test Coverage** | 140+ tests | 35+ per team |
| **Event Handler Coverage** | 100% | All domain events handled |
| **Projection Accuracy** | 100% | Projections match event history |
| **Temporal Consistency** | 100% | All replay scenarios valid |
| **Multi-Tenant Isolation** | 100% | No cross-tenant data leakage |
| **HIPAA Compliance** | 100% | All audits pass |
| **Performance** | Event handling < 100ms | P99 latency |
| **Code Quality** | 0 errors | ESLint, TypeScript strict |

---

## Integration with Phase 3

Phase 4 services depend on Phase 3 components:

```
Phase 4 Services (Event Handlers)
         ↓
Phase 3 EventStore (immutable append-only)
Phase 3 Replay Engine (reconstruct state)
Phase 3 Projection Manager (idempotent updates)
Phase 3 Query Services (read-optimized)
         ↓
PostgreSQL (backend/modules/services/{service}-db)
Redis (projection cache)
Kafka (event streaming)
```

**Gradle Dependencies** (all Phase 4 services):

```kotlin
dependencies {
    implementation(project(":modules:shared:infrastructure:event-sourcing"))  // Phase 3
    implementation(project(":modules:shared:infrastructure:persistence"))    // Phase 3
    implementation(project(":modules:shared:infrastructure:messaging"))      // Kafka

    // Spring
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}
```

---

## Next Steps

### Immediate Actions (This Session)
1. ✅ Complete Phase 3 (DONE: All 4 teams merged)
2. ✅ Plan Phase 4 (THIS DOCUMENT)
3. 🚀 Create Phase 4 module structure (ready to begin)
4. 🚀 Set up git worktrees (ready to execute)
5. 🚀 Begin Team 4.1 RED phase (write 35+ tests)

### Team 4.1 First Steps
1. Create `patient-event-handler-service` module
2. Write `PatientEventHandlerTest.java` (RED phase)
3. Create domain event classes (PatientCreatedEvent, etc.)
4. Create projection classes (PatientActiveProjection, etc.)
5. Run tests (all fail - expected in RED phase)

---

## Why Phase 4 Matters

**Phase 3** provided the *infrastructure* (how to store/retrieve events).

**Phase 4** provides the *domain logic* (what events mean in healthcare).

Together, they enable:
- ✅ Event-driven architecture
- ✅ Complete audit trails for HIPAA
- ✅ Replay-able business operations
- ✅ Real-time quality metrics
- ✅ Care gap identification
- ✅ Clinical outcome tracking

---

**Ready to Begin Phase 4 TDD Swarm?** 🚀

Next action: Create Phase 4 Team 4.1 module and write first 35+ RED phase tests for patient lifecycle events.

