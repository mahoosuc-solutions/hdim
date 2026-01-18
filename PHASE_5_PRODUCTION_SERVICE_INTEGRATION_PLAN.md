# Phase 5: Production Service Integration - Event Handlers → Spring Boot Microservices

**Status**: 🔴 PLANNING PHASE
**Date**: January 18, 2026
**Objective**: Integrate Phase 4 event handler libraries into production Spring Boot microservices with REST APIs, Kafka messaging, and database persistence

---

## Executive Summary

Phase 5 builds on Phase 4's event handler libraries by wrapping them in production-grade Spring Boot microservices. Each of 4 parallel teams will implement a complete service stack:

1. **REST Controller Layer** - HTTP endpoints for event submission
2. **Service Layer** - Business logic orchestration
3. **Event Handler Integration** - Use Phase 4 event handler libraries
4. **Kafka Integration** - Event publishing and consumption
5. **Database Layer** - Liquibase migrations for event store and projections
6. **Error Handling & Validation** - Production-grade request validation
7. **Health Checks & Monitoring** - Readiness probes and metrics

**Methodology**: TDD Swarm (RED phase tests first, then GREEN phase implementation)
**Teams**: 4 parallel teams using git worktrees
**Target**: All services merged to master with end-to-end integration tests passing

---

## Team Breakdown

### Team 5.1: Patient Event Service
**Module**: `patient-event-service`
**Foundation**: Phase 4 `patient-event-handler-service` library

**Deliverables**:
- REST API for patient lifecycle events
- POST /api/v1/patients/events/create
- POST /api/v1/patients/events/enroll
- POST /api/v1/patients/events/demographics
- Database: Liquibase migrations for patient_events table
- Kafka: Topic `patient.events` for event publishing
- Tests: 25+ integration tests validating REST → EventHandler → Database flow

**Key Features**:
- @RestController with @RequestBody PatientEventRequest
- Liquibase migration: 0001-create-patient-events-table.xml
- KafkaTemplate for event publishing
- TransactionManagement for event atomicity
- Comprehensive request validation
- Error handling with structured responses

---

### Team 5.2: Quality Measure Event Service
**Module**: `quality-measure-event-service`
**Foundation**: Phase 4 `quality-measure-event-handler-service` library

**Deliverables**:
- REST API for quality measure evaluation
- POST /api/v1/measures/evaluate
- POST /api/v1/measures/scores/calculate
- POST /api/v1/measures/risk/calculate
- Database: Liquibase migrations for measure_events and projections tables
- Kafka: Topics `measure.evaluated`, `measure.scored`, `risk.calculated`
- Tests: 25+ integration tests validating scoring logic end-to-end

**Key Features**:
- Measure evaluation request handling
- Score calculation (>75% threshold logic)
- Risk stratification by severity levels
- Cohort aggregation queries
- Redis caching for projection read performance
- Liquibase migrations for measure_evaluation_projection, risk_score_projection, cohort_measure_rate_projection

---

### Team 5.3: Care Gap Event Service
**Module**: `care-gap-event-service`
**Foundation**: Phase 4 `care-gap-event-handler-service` library

**Deliverables**:
- REST API for care gap detection and management
- POST /api/v1/gaps/detect
- POST /api/v1/gaps/qualify
- POST /api/v1/gaps/intervene
- POST /api/v1/gaps/close
- Database: Liquibase migrations for care_gap_events and projections tables
- Kafka: Topic `care.gaps` for event publishing
- Tests: 20+ integration tests validating gap lifecycle end-to-end

**Key Features**:
- Gap detection with severity classification
- Patient qualification tracking
- Intervention recommendation engine
- Population health aggregation and analytics
- Liquibase migrations for care_gap_projection, population_health_projection
- Query endpoints for gap analytics

---

### Team 5.4: Workflow Event Service
**Module**: `clinical-workflow-event-service`
**Foundation**: Phase 4 `clinical-workflow-event-handler-service` library

**Deliverables**:
- REST API for clinical workflow orchestration
- POST /api/v1/workflows/initiate
- POST /api/v1/workflows/steps/execute
- POST /api/v1/workflows/steps/complete
- POST /api/v1/workflows/approvals/decide
- Database: Liquibase migrations for workflow_events table
- Kafka: Topic `workflows` for event publishing
- Tests: 20+ integration tests validating workflow state machine end-to-end

**Key Features**:
- Workflow instance management
- Step execution and completion tracking
- Approval workflow support with decision tracking
- Task assignment and routing
- Duration tracking and state transitions
- Liquibase migration for workflow_projection

---

## Parallel Development Setup

### Git Worktrees

```
/home/webemo-aaron/projects/
├── phase5-team1-patient/           (feature/phase5-team1-patient-service)
├── phase5-team2-quality/           (feature/phase5-team2-quality-service)
├── phase5-team3-caregap/           (feature/phase5-team3-caregap-service)
└── phase5-team4-workflow/          (feature/phase5-team4-workflow-service)
```

**Setup Commands**:
```bash
# From hdim-master root
git worktree add ../phase5-team1-patient feature/phase5-team1-patient-service
git worktree add ../phase5-team2-quality feature/phase5-team2-quality-service
git worktree add ../phase5-team3-caregap feature/phase5-team3-caregap-service
git worktree add ../phase5-team4-workflow feature/phase5-team4-workflow-service
```

### Module Registration

Add to `backend/settings.gradle.kts`:
```kotlin
include(
    "modules:services:patient-event-service",
    "modules:services:quality-measure-event-service",
    "modules:services:care-gap-event-service",
    "modules:services:clinical-workflow-event-service"
)
```

---

## TDD Swarm Pattern

### RED Phase (Week 1)

**Per Team: Write Comprehensive Integration Tests**

```java
// Example: PatientEventServiceTest.java
@SpringBootTest
@AutoConfigureMockMvc
class PatientEventServiceTest {

    @Test
    @DisplayName("Should accept patient created event via REST and store in database")
    void testCreatePatientEvent() {
        // Given: PatientCreatedEvent request
        // When: POST /api/v1/patients/events/create
        // Then: Event stored, Kafka published, projection updated
    }

    @Test
    @DisplayName("Should calculate compliance rate from patient cohort")
    void testCohortComplianceCalculation() {
        // Given: 10 patients with 7 meeting criteria
        // When: Events processed
        // Then: Projection shows 70% compliance
    }
}
```

**Test Scope per Team**:
- REST controller integration (MockMvc)
- Request validation and error handling
- Event handler library integration
- Database persistence (using Testcontainers PostgreSQL)
- Kafka publishing (using Testcontainers Kafka)
- Transaction management
- Multi-tenant isolation
- Projection updates
- Query performance

**Target**: 25+ tests per team (100 total across 4 teams)

---

### GREEN Phase (Week 2)

**Per Team: Implement Production Services**

**1. REST Controller**
```java
@RestController
@RequestMapping("/api/v1/patients/events")
@RequiredArgsConstructor
public class PatientEventController {

    private final PatientEventApplicationService patientEventService;
    private final TenantContext tenantContext;

    @PostMapping("/create")
    @PreAuthorize("hasRole('EVALUATOR')")
    public ResponseEntity<EventResponse> createPatient(
            @Valid @RequestBody CreatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        PatientCreatedEvent event = patientEventService.createPatient(request, tenantId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(new EventResponse(event.getPatientId(), "CREATED"));
    }
}
```

**2. Service Layer**
```java
@Service
@RequiredArgsConstructor
@Transactional
public class PatientEventApplicationService {

    private final PatientEventHandler eventHandler;
    private final PatientEventRepository eventRepository;
    private final KafkaTemplate<String, PatientCreatedEvent> kafkaTemplate;

    public PatientCreatedEvent createPatient(CreatePatientRequest request, String tenantId) {
        // Validation
        // Create domain event
        PatientCreatedEvent event = new PatientCreatedEvent(
            UUID.randomUUID().toString(),
            tenantId,
            request.getFirstName(),
            request.getLastName(),
            request.getDateOfBirth()
        );

        // Handle event (updates projection)
        eventHandler.handle(event);

        // Persist event to database
        eventRepository.save(event);

        // Publish to Kafka
        kafkaTemplate.send("patient.events", event.getPatientId(), event);

        return event;
    }
}
```

**3. Kafka Integration**
```java
@Configuration
public class KafkaConfig {

    @Bean
    public KafkaTemplate<String, PatientCreatedEvent> patientEventKafkaTemplate(
            ProducerFactory<String, PatientCreatedEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic patientEventsTopic() {
        return TopicBuilder.name("patient.events")
            .partitions(3)
            .replicas(1)
            .build();
    }
}
```

**4. Database Migrations (Liquibase)**
```xml
<!-- 0001-create-patient-events-table.xml -->
<changeSet id="0001-create-patient-events" author="team-5-1">
    <createTable tableName="patient_events">
        <column name="id" type="UUID" defaultValueComputed="gen_random_uuid()">
            <constraints primaryKey="true"/>
        </column>
        <column name="tenant_id" type="VARCHAR(100)">
            <constraints nullable="false"/>
        </column>
        <column name="patient_id" type="VARCHAR(100)">
            <constraints nullable="false"/>
        </column>
        <column name="event_type" type="VARCHAR(100)">
            <constraints nullable="false"/>
        </column>
        <column name="event_data" type="JSONB">
            <constraints nullable="false"/>
        </column>
        <column name="created_at" type="TIMESTAMP WITH TIME ZONE" defaultValueComputed="CURRENT_TIMESTAMP">
            <constraints nullable="false"/>
        </column>
    </createTable>

    <createIndex indexName="idx_patient_events_tenant" tableName="patient_events">
        <column name="tenant_id"/>
    </createIndex>

    <rollback>
        <dropTable tableName="patient_events"/>
    </rollback>
</changeSet>
```

**5. Repository Layer**
```java
@Repository
public interface PatientEventRepository extends JpaRepository<PatientEventEntity, UUID> {
    List<PatientEventEntity> findByPatientIdAndTenantId(String patientId, String tenantId);
}
```

---

### REFACTOR Phase (Week 3)

**Integration & Production Hardening**:
- Code review across all 4 teams
- Performance optimization and caching
- Security hardening (CORS, rate limiting)
- Production deployment readiness
- Documentation and runbooks

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    API Clients                           │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────v──────────────────────────────────┐
│         Spring Boot REST Controllers (Team 5.x)          │
│  POST /api/v1/patients/events/create (Team 5.1)         │
│  POST /api/v1/measures/evaluate (Team 5.2)              │
│  POST /api/v1/gaps/detect (Team 5.3)                    │
│  POST /api/v1/workflows/initiate (Team 5.4)             │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────v──────────────────────────────────┐
│      Application Services (Business Logic Orchestration) │
│  PatientEventApplicationService (Team 5.1)              │
│  QualityMeasureEventApplicationService (Team 5.2)       │
│  CareGapEventApplicationService (Team 5.3)              │
│  WorkflowEventApplicationService (Team 5.4)             │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────v──────────────────────────────────┐
│     Phase 4 Event Handler Libraries (Imported)           │
│  PatientEventHandler (Team 5.1)                          │
│  QualityMeasureEventHandler (Team 5.2)                   │
│  CareGapEventHandler (Team 5.3)                          │
│  ClinicalWorkflowEventHandler (Team 5.4)                 │
└──────────────────────┬──────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
    ┌───v────┐     ┌───v────┐    ┌───v────┐
    │Database │    │  Kafka  │    │ Redis  │
    │ Events  │    │ Topics  │    │ Cache  │
    │& Projns │    │         │    │        │
    └────────┘     └────────┘    └────────┘
```

---

## Success Criteria

### Phase 5.0: RED Phase (Complete by EOD Week 1)

- [ ] Team 5.1: 25+ integration tests created (patient-event-service)
- [ ] Team 5.2: 25+ integration tests created (quality-measure-event-service)
- [ ] Team 5.3: 20+ integration tests created (care-gap-event-service)
- [ ] Team 5.4: 20+ integration tests created (clinical-workflow-event-service)
- [ ] All tests follow Spring Boot integration testing patterns
- [ ] Test coverage: REST → Service → EventHandler → Database flows
- [ ] Database test fixtures prepared
- [ ] Kafka test infrastructure ready

### Phase 5.1: GREEN Phase (Complete by EOD Week 2)

- [ ] Team 5.1: PatientEventService fully implemented (25+ tests passing)
- [ ] Team 5.2: QualityMeasureEventService fully implemented (25+ tests passing)
- [ ] Team 5.3: CareGapEventService fully implemented (20+ tests passing)
- [ ] Team 5.4: WorkflowEventService fully implemented (20+ tests passing)
- [ ] All 4 services registered in Gradle build
- [ ] REST endpoints validated via Swagger/OpenAPI
- [ ] Kafka topics created and messages flowing
- [ ] Database migrations validated

### Phase 5.2: Integration (Complete by EOD Week 3)

- [ ] Sequential merge: Team 5.1 → Team 5.2 → Team 5.3 → Team 5.4
- [ ] End-to-end integration tests validating cross-service event flow
- [ ] All 90+ tests passing in CI/CD
- [ ] Production deployment checklist complete
- [ ] Documentation and runbooks ready

---

## Repository Setup

### Feature Branches

```bash
# Per-team feature branches
feature/phase5-team1-patient-service
feature/phase5-team2-quality-service
feature/phase5-team3-caregap-service
feature/phase5-team4-workflow-service
```

### Module Naming Convention

```
modules:services:patient-event-service              (Team 5.1)
modules:services:quality-measure-event-service      (Team 5.2)
modules:services:care-gap-event-service             (Team 5.3)
modules:services:clinical-workflow-event-service    (Team 5.4)
```

---

## Dependencies

### Phase 4 Library Dependencies

Each Phase 5 service depends on its corresponding Phase 4 event handler library:

```kotlin
// Team 5.1 build.gradle.kts
dependencies {
    implementation(project(":modules:services:patient-event-handler-service"))
}

// Team 5.2 build.gradle.kts
dependencies {
    implementation(project(":modules:services:quality-measure-event-handler-service"))
}

// Team 5.3 build.gradle.kts
dependencies {
    implementation(project(":modules:services:care-gap-event-handler-service"))
}

// Team 5.4 build.gradle.kts
dependencies {
    implementation(project(":modules:services:clinical-workflow-event-handler-service"))
}
```

### Spring Boot Stack

All teams share:
```kotlin
implementation(libs.bundles.spring.boot.web)
implementation(libs.bundles.spring.boot.data)
implementation(libs.spring.boot.starter.security)
implementation("org.springframework.kafka:spring-kafka")
implementation("org.liquibase:liquibase-core")
testImplementation(libs.testcontainers.postgresql)
testImplementation(libs.testcontainers.kafka)
```

---

## Timeline

| Phase | Duration | Deliverable |
|-------|----------|-------------|
| RED Phase | Week 1 (2 days) | 90+ integration tests |
| GREEN Phase | Week 2 (3 days) | 4 production services |
| Integration | Week 3 (2 days) | Master merge + deployment |

---

## Quality Gates

**Before each merge to master:**

- ✅ All tests passing (90+ tests)
- ✅ Code coverage > 80%
- ✅ SonarQube quality gates passed
- ✅ No security vulnerabilities
- ✅ Load testing passed (1000 req/sec per service)
- ✅ Database migrations validated
- ✅ Kafka topics verified

---

## Deployment Strategy

### Local Development
```bash
docker-compose up -d
./gradlew :modules:services:*-event-service:bootRun
```

### Docker Deployment
```bash
# Build images
./gradlew :modules:services:*-event-service:bootBuildImage

# Deploy to compose
docker-compose -f docker-compose.production.yml up -d
```

### Kubernetes Deployment
```bash
# Deploy to K8s
kubectl apply -f k8s/patient-event-service.yaml
kubectl apply -f k8s/quality-measure-event-service.yaml
kubectl apply -f k8s/care-gap-event-service.yaml
kubectl apply -f k8s/clinical-workflow-event-service.yaml
```

---

## Monitoring & Observability

### Health Checks
```java
@RestController
@RequestMapping("/actuator")
public class HealthController {
    @GetMapping("/health")
    public ResponseEntity<Health> health() {
        // Check database connectivity
        // Check Kafka connectivity
        // Check event handler status
    }
}
```

### Metrics
```java
@Timed(value = "patient.event.creation")
public void createPatient(CreatePatientRequest request) {
    // Measured metrics: latency, throughput
}
```

### Distributed Tracing
```java
// OpenTelemetry integration for request tracing across services
@Traced
public void handle(Event event) {
    // Automatic trace correlation
}
```

---

_Document created: January 18, 2026_
_Phase 5: Production Service Integration - Ready to commence_
