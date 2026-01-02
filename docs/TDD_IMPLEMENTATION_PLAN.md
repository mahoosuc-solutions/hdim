# Health Data In Motion - Complete TDD Implementation Plan

**Version**: 1.0
**Date**: October 28, 2025
**Approach**: Test-Driven Development (TDD)
**Target**: Fully Implemented SaaS Platform for Event-Driven Healthcare Data Processing

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [TDD Strategy & Principles](#2-tdd-strategy--principles)
3. [Development Phases](#3-development-phases)
4. [Backend Implementation Plan](#4-backend-implementation-plan)
5. [Frontend Implementation Plan](#5-frontend-implementation-plan)
6. [Integration & E2E Testing](#6-integration--e2e-testing)
7. [CI/CD Pipeline](#7-cicd-pipeline)
8. [Deployment Strategy](#8-deployment-strategy)
9. [Success Criteria](#9-success-criteria)
10. [Timeline & Resources](#10-timeline--resources)

---

## 1. Executive Summary

### 1.1 Objective

Transform the current **monorepo scaffold** into a **fully functional, production-ready SaaS platform** for event-driven healthcare data processing using Test-Driven Development (TDD) methodology.

### 1.2 Current State

✅ **Workspace**: Monorepo structure in place (Nx + Gradle) with shared domain models and validation utilities.
✅ **Frontend Shell**: Angular admin portal layout and pages scaffolded; screens read from static fallback data.
🟡 **Shared Infrastructure**: Security, persistence, cache, and messaging modules defined but missing concrete implementations.
🟡 **Backend Services**: Nine Spring Boot services scaffolded with build files only—no controllers, repositories, or messaging logic.
🟡 **Documentation**: Comprehensive roadmap and reference docs drafted; execution plan needs re-baselining.
❌ **Integrations**: No live service endpoints, database schemas, or Kafka topics wired up.
❌ **Automated Tests**: Limited to domain/unit examples; no integration, contract, or E2E coverage.

### 1.3 Target State

✅ **Backend**: 9 fully implemented microservices
✅ **Frontend**: Complete Angular admin portal with all screens
✅ **Tests**: 80%+ code coverage (unit + integration + E2E)
✅ **Documentation**: Interactive API docs, integration examples
✅ **Performance**: All SLAs met (<500ms response, 67%+ cache hit)
✅ **Compliance**: HIPAA audit ready, full audit trail
✅ **Deployment**: Docker + Kubernetes ready

### 1.4 Key Metrics

| Metric | Current Snapshot | Target | Notes |
|--------|-----------------|--------|-------|
| **Backend Services** | Build scaffolds only; 0 production endpoints | Deliver patient-centric FHIR vertical slice, then expand to remaining services | Prioritise vertical slices over breadth |
| **Shared Infrastructure Modules** | Interfaces/build files without implementations | Security, persistence, cache, messaging modules production-ready with tests | Blocks service development until complete |
| **UI Screens** | Dashboard, System Health, Service Catalog, API Playground using fallback JSON | Fully integrated admin portal backed by live APIs | Replace seed data with live responses iteratively |
| **Automated Tests** | Domain unit tests only | 80%+ coverage split across unit/integration/E2E | Add contracts alongside each delivered feature |
| **Operational Data Stores** | No schemas or migrations | PostgreSQL schemas, Redis config, Kafka topics defined and automated | Needed for integration and infra automation |
| **Performance SLAs** | Untested | <500 ms median API latency, 67%+ cache hit | Validate as part of vertical slice hardening |

---

## 2. TDD Strategy & Principles

### 2.1 TDD Cycle

```
1. RED Phase: Write a failing test
   ↓
2. GREEN Phase: Write minimal code to pass test
   ↓
3. REFACTOR Phase: Clean up code while keeping tests passing
   ↓
4. REPEAT for next requirement
```

### 2.2 Test Pyramid Strategy

```
           /\
          /  \     E2E Tests (10%)
         /____\    - Critical user journeys
        /      \   - Cross-service workflows
       /________\  Integration Tests (30%)
      /          \ - API endpoints
     /____________\- Database operations
    /              \ - Kafka events
   /________________\ Unit Tests (60%)
                      - Business logic
                      - Validators
                      - Utilities
```

### 2.3 Test Categories

#### 2.3.1 Unit Tests (60% of tests)
- **Framework**: JUnit 5 (Java), Jest (TypeScript)
- **Coverage Target**: 80%
- **Scope**: Individual methods, functions, components
- **Examples**:
  - FHIR resource validation
  - CQL expression evaluation logic
  - Consent rule evaluation
  - Data model serialization/deserialization

#### 2.3.2 Integration Tests (30% of tests)
- **Framework**: Spring Boot Test, Testcontainers
- **Coverage Target**: 75%
- **Scope**: Service-to-service, database, cache, messaging
- **Examples**:
  - REST API endpoints (full request/response cycle)
  - PostgreSQL CRUD operations
  - Redis caching behavior
  - Kafka event publishing/consuming
  - Consent filtering in FHIR API

#### 2.3.3 E2E Tests (10% of tests)
- **Framework**: Playwright (Angular), REST Assured (API)
- **Coverage Target**: Critical paths only
- **Scope**: Complete user workflows
- **Examples**:
  - Admin portal login → dashboard → service details
  - API playground: create patient → search → view
  - Care gap detection: new observation → gap detected → webhook sent
  - HEDIS measure evaluation: evaluate → cache → re-evaluate (cached)

### 2.4 TDD Best Practices for Healthcare

#### 2.4.1 FHIR Testing
- Use official FHIR validation suite
- Test all resource types with valid/invalid examples
- Verify FHIR search parameter behavior
- Test Bundle transactions (all-or-nothing semantics)

#### 2.4.2 HEDIS Measure Testing
- Test all 52 HEDIS measures with known good/bad cases
- Use CMS test cases where available
- Verify measure scoring (numerator/denominator logic)
- Test edge cases (e.g., patient turns 18 during measurement period)

#### 2.4.3 HIPAA Compliance Testing
- Test audit log creation for every PHI access
- Verify encryption at rest (database) and in transit (TLS)
- Test consent filtering (field-level access control)
- Verify 7-year audit retention
- Test breach notification workflow

#### 2.4.4 Performance Testing
- Load test every endpoint to verify SLAs
- Test cache hit rates (target: 67-98%)
- Test concurrent user scenarios
- Test database connection pool under load
- Test Kafka throughput (target: 12.4 events/sec)

---

## 3. Development Phases

### Phase 1: Foundation (Weeks 1-2)
**Goal**: Core infrastructure and shared modules

**Deliverables**:
1. ✅ Shared domain models (FHIR, HEDIS, CQL)
2. ✅ Infrastructure modules (security, audit, persistence)
3. ✅ Database schemas (all 7 databases)
4. ✅ Test data generators (synthetic patients, observations)
5. ✅ CI/CD pipeline setup

**TDD Approach**:
- Start with domain model unit tests
- Test serialization/deserialization
- Test validation rules
- No dependencies on external services yet

**Example Tests**:
```java
@Test
void testFhirResourceWrapperCreation() {
    Patient patient = new Patient();
    patient.setId("patient-123");

    FhirResourceWrapper<Patient> wrapper =
        FhirResourceWrapper.wrap(patient, "user-1", "org-1");

    assertNotNull(wrapper.getCreatedAt());
    assertEquals("user-1", wrapper.getCreatedBy());
    assertTrue(wrapper.isContainsPHI());
}
```

### Phase 2: FHIR Service (Weeks 3-5)
**Goal**: Complete FHIR R4 resource management

**TDD Order**:
1. **Week 3**: Patient resource CRUD
   - Test: Create patient (invalid → valid)
   - Test: Read patient by ID (not found → found)
   - Test: Update patient (optimistic locking)
   - Test: Delete patient (cascade behavior)
   - Test: Search patients (name, birthdate, identifier)

2. **Week 4**: Observation, Condition, MedicationRequest
   - Repeat CRUD pattern for each resource type
   - Test: Patient → Observation relationships
   - Test: Bundle transactions (atomic operations)

3. **Week 5**: Remaining 147 FHIR resources
   - Use code generation for repetitive CRUD
   - Test: Generic FHIR resource handler
   - Test: FHIR validation engine integration
   - Test: Kafka event publishing

**Key Tests**:
```java
@SpringBootTest
@Testcontainers
class FhirServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private FhirResourceRepository repository;

    @Test
    void testCreatePatient_shouldPublishKafkaEvent() {
        // Given
        Patient patient = buildTestPatient();

        // When
        Patient created = fhirService.createPatient(patient);

        // Then
        assertNotNull(created.getId());
        verify(kafkaTemplate).send(eq("fhir.resources.patient"), any());
    }
}
```

### Phase 3: CQL Engine Service (Weeks 6-8)
**Goal**: HEDIS measure evaluation engine

**TDD Order**:
1. **Week 6**: CQL expression parser and evaluator
   - Test: Parse CQL expression (invalid → valid)
   - Test: Evaluate simple CQL (1 + 1 = 2)
   - Test: CQL with FHIR data context
   - Test: Error handling (syntax errors, runtime errors)

2. **Week 7**: HEDIS measures (batch 1-26)
   - Test: Each measure with known good case
   - Test: Each measure with known bad case
   - Test: Edge cases (e.g., data at boundary)
   - Test: Measure caching (TTL: 24 hours)

3. **Week 8**: HEDIS measures (batch 27-52) + STAR ratings
   - Test: Remaining 26 measures
   - Test: STAR rating prediction algorithm
   - Test: Batch evaluation (1,000 patients)
   - Test: Performance (<178ms average)

**Key Tests**:
```java
@Test
void testHedisCBP_bloodPressureControlled_shouldPass() {
    // Given: Patient with BP readings <140/90 in last 12 months
    String patientId = "patient-123";
    createObservation(patientId, "blood-pressure", "130/85",
        LocalDate.now().minusMonths(3));

    // When: Evaluate HEDIS_CBP measure
    MeasureResult result = cqlService.evaluate("HEDIS_CBP", patientId);

    // Then: Should pass (score = 1)
    assertEquals(1, result.getScore());
    assertEquals("PASS", result.getResult());
    assertTrue(result.getRationale().contains("BP readings within threshold"));
}
```

### Phase 4: Consent Service (Weeks 9-10)
**Goal**: HIPAA-compliant consent management

**TDD Order**:
1. **Week 9**: Consent policy CRUD and RBAC
   - Test: Create consent policy (invalid → valid)
   - Test: 13 roles creation
   - Test: 31 permissions assignment
   - Test: Role-permission matrix evaluation

2. **Week 10**: Consent evaluation and filtering
   - Test: Consent evaluation (<5ms)
   - Test: Field-level filtering (SSN, DOB redaction)
   - Test: Minimum necessary rule
   - Test: Audit logging for consent decisions

**Key Tests**:
```java
@Test
void testConsentFiltering_nurseShouldNotSeeSSN() {
    // Given: User with NURSE role
    String userId = "nurse-1";
    assignRole(userId, "NURSE");

    // And: Patient resource with SSN
    Patient patient = createPatient("123-45-6789");

    // When: Nurse reads patient
    ConsentEvaluation consent = consentService.evaluate(
        userId, patient.getId(), "read");

    // Then: SSN should be denied
    assertTrue(consent.isAllowed());
    assertTrue(consent.getDeniedFields().contains("ssn"));

    // And: Audit log should be created
    List<AuditEvent> logs = auditRepository.findByUserId(userId);
    assertEquals(1, logs.size());
}
```

### Phase 5: Event Processing Service (Weeks 11-12)
**Goal**: Real-time care gap detection

**TDD Order**:
1. **Week 11**: Kafka event consumers
   - Test: Consume fhir.resources.patient events
   - Test: Consume fhir.resources.observation events
   - Test: Dead letter queue for failures
   - Test: Exactly-once semantics

2. **Week 12**: Care gap detection logic
   - Test: Detect diabetes HbA1c gap
   - Test: Detect mammography gap
   - Test: Prioritization (high/medium/low)
   - Test: Webhook notification delivery
   - Test: Performance (<5 seconds)

**Key Tests**:
```java
@Test
void testCareGapDetection_diabeticPatientMissingHbA1c_shouldDetectGap() {
    // Given: Patient with diabetes diagnosis
    String patientId = createDiabeticPatient();

    // And: No HbA1c test in last 12 months
    // (no observations)

    // When: New observation event arrives (not HbA1c)
    ObservationEvent event = new ObservationEvent(patientId, "blood-glucose", ...);
    eventProcessor.processObservation(event);

    // Then: Care gap should be detected within 5 seconds
    await().atMost(5, SECONDS).until(() ->
        careGapRepository.findByPatientId(patientId).isPresent());

    CareGap gap = careGapRepository.findByPatientId(patientId).get();
    assertEquals("HEDIS_HbA1c", gap.getMeasureId());
    assertEquals("HIGH", gap.getSeverity());
}
```

### Phase 6: Remaining Backend Services (Weeks 13-14)
**Goal**: Complete all 9 microservices

**Services**:
- Patient Service (patient demographics, enrollment)
- Quality Measure Service (measure orchestration)
- Care Gap Service (gap closure workflows)
- Analytics Service (data analytics, reporting)
- Gateway Service (Kong configuration)

**TDD Approach**: Apply same pattern as Phases 2-5

### Phase 7: Angular Admin Portal (Weeks 15-18)
**Goal**: Complete frontend UI

**TDD Order (Component-Driven Development)**:

#### Week 15: Shared Components & Layout
- Test: AdminLayout renders correctly
- Test: ServiceCard displays health status (green/yellow/red)
- Test: MetricCard displays metric value
- Test: HealthIndicator changes color based on status
- Test: Navigation menu loads correctly

#### Week 16: Service Management UIs (Part 1)
- Test: Platform Dashboard loads all service statuses
- Test: PostgreSQL UI displays 7 databases
- Test: Redis UI shows cache metrics
- Test: Kafka UI lists 8 topics

#### Week 17: Service Management UIs (Part 2)
- Test: FHIR UI lists resource types
- Test: CQL UI displays 52 measures
- Test: Consent UI shows RBAC matrix
- Test: Event Processing UI displays queue status

#### Week 18: Developer Tools
- Test: API Playground sends requests
- Test: Request history stores last 20 requests
- Test: Variable substitution works {{patientId}}
- Test: Code generation (cURL, JS, Python)

**Key Frontend Tests**:
```typescript
describe('ServiceCard Component', () => {
  it('should display green health indicator for healthy service', () => {
    const component = createComponent(ServiceCard, {
      service: {
        name: 'FHIR Service',
        status: 'UP',
        responseTime: 45,
        uptime: 99.9
      }
    });

    expect(component.healthIndicator.color).toBe('green');
    expect(component.statusText).toBe('Healthy');
  });

  it('should display red health indicator for down service', () => {
    const component = createComponent(ServiceCard, {
      service: {
        name: 'CQL Engine',
        status: 'DOWN',
        responseTime: null,
        uptime: 0
      }
    });

    expect(component.healthIndicator.color).toBe('red');
    expect(component.statusText).toBe('Down');
  });
});
```

### Phase 8: API Documentation & Examples (Weeks 19-20)
**Goal**: Interactive API docs and integration examples

**TDD Order**:
1. **Week 19**: OpenAPI documentation generation
   - Test: Generate OpenAPI spec from annotations
   - Test: Swagger UI loads correctly
   - Test: "Try it out" functionality works
   - Test: Code examples render correctly

2. **Week 20**: Integration examples
   - Test: All 6 examples run successfully
   - Test: Example code is syntactically correct
   - Test: Examples produce expected output
   - Test: Examples handle errors gracefully

### Phase 9: Performance Optimization (Weeks 21-22)
**Goal**: Meet all performance SLAs

**TDD Approach (Performance Test-Driven)**:
1. Write performance test with SLA target
2. Run test (expect failure)
3. Optimize code
4. Run test again (expect pass)
5. Repeat until all SLAs met

**Performance Tests**:
```java
@Test
void testFhirRead_withCacheHit_shouldRespondIn10ms() {
    // Given: Patient in cache
    String patientId = "patient-123";
    cachePatient(patientId);

    // When: Read patient 100 times
    List<Long> responseTimes = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
        long start = System.nanoTime();
        fhirService.readPatient(patientId);
        long end = System.nanoTime();
        responseTimes.add((end - start) / 1_000_000); // Convert to ms
    }

    // Then: p95 should be < 10ms
    Collections.sort(responseTimes);
    long p95 = responseTimes.get(94); // 95th percentile
    assertTrue(p95 < 10, "p95 response time: " + p95 + "ms");
}
```

### Phase 10: Integration & E2E Testing (Weeks 23-24)
**Goal**: Verify complete system works end-to-end

**E2E Test Scenarios**:

1. **Patient Data Flow** (Beginner)
   ```gherkin
   Feature: Patient Data Flow
     Scenario: Create and retrieve patient
       Given I am an authenticated user
       When I create a patient with name "John Smith"
       Then I should receive a 201 Created response
       And the patient should have a generated ID
       When I retrieve the patient by ID
       Then I should receive the same patient data
       And a Kafka event should be published
   ```

2. **HEDIS Measure Evaluation** (Intermediate)
   ```gherkin
   Feature: HEDIS Measure Evaluation
     Scenario: Evaluate blood pressure control measure
       Given a patient with diabetes diagnosis
       And blood pressure observations <140/90 in last 12 months
       When I evaluate the HEDIS_CBP measure
       Then the measure should pass (score = 1)
       And the evaluation should complete in <178ms
       And the result should be cached for 24 hours
   ```

3. **Care Gap Detection** (Advanced)
   ```gherkin
   Feature: Real-time Care Gap Detection
     Scenario: Detect missing HbA1c test
       Given a patient with diabetes diagnosis
       And no HbA1c test in last 12 months
       When a new blood glucose observation is created
       Then a care gap should be detected within 5 seconds
       And a CareGapDetectedEvent should be published to Kafka
       And a webhook notification should be sent to the EHR
       And the care gap should appear in the UI
   ```

4. **Consent-Aware Access** (Intermediate)
   ```gherkin
   Feature: Consent-Aware Data Access
     Scenario: Nurse reads patient with field-level consent
       Given a user with NURSE role
       And a patient with SSN field marked as denied for nurses
       When the nurse reads the patient
       Then the response should include all allowed fields
       And the SSN field should be redacted
       And an audit log entry should be created
   ```

---

## 4. Backend Implementation Plan

### 4.1 Module-by-Module TDD Breakdown

#### 4.1.1 FHIR Service

**Test File Structure**:
```
backend/modules/services/fhir-service/src/test/java/
├── com/healthdata/fhir/
│   ├── controller/
│   │   ├── PatientControllerTest.java          # Unit tests
│   │   ├── ObservationControllerTest.java
│   │   ├── PatientControllerIT.java            # Integration tests
│   │   └── ObservationControllerIT.java
│   ├── service/
│   │   ├── FhirResourceServiceTest.java
│   │   ├── FhirValidationServiceTest.java
│   │   └── FhirResourceServiceIT.java
│   ├── repository/
│   │   ├── PatientRepositoryTest.java
│   │   └── PatientRepositoryIT.java
│   └── FhirServiceE2ETest.java                 # E2E tests
```

**Test Count Estimate**: 250 tests

**Test Examples**:

```java
// Unit Test
@Test
void createPatient_withInvalidData_shouldThrowValidationException() {
    Patient patient = new Patient();
    // Missing required fields

    assertThrows(FhirValidationException.class, () -> {
        fhirService.createPatient(patient);
    });
}

// Integration Test
@SpringBootTest
@Testcontainers
class PatientControllerIT {
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void createPatient_shouldPersistToDatabase() {
        Patient patient = buildValidPatient();

        ResponseEntity<Patient> response =
            restTemplate.postForEntity("/fhir/Patient", patient, Patient.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // Verify in database
        Patient saved = patientRepository.findById(
            response.getBody().getId()).orElseThrow();
        assertEquals(patient.getNameFirstRep().getFamily(),
            saved.getNameFirstRep().getFamily());
    }
}

// E2E Test
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class FhirServiceE2ETest {
    @Test
    void fullPatientLifecycle_shouldWork() {
        // Create
        Patient patient = createPatientViaAPI();
        assertNotNull(patient.getId());

        // Read
        Patient retrieved = getPatientViaAPI(patient.getId());
        assertEquals(patient.getId(), retrieved.getId());

        // Update
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        Patient updated = updatePatientViaAPI(patient);
        assertEquals(Enumerations.AdministrativeGender.MALE, updated.getGender());

        // Delete
        deletePatientViaAPI(patient.getId());

        // Verify deleted
        assertThrows(ResourceNotFoundException.class, () -> {
            getPatientViaAPI(patient.getId());
        });
    }
}
```

#### 4.1.2 CQL Engine Service

**Test File Structure**:
```
backend/modules/services/cql-engine-service/src/test/java/
├── com/healthdata/cql/
│   ├── controller/
│   │   ├── CqlEvaluationControllerTest.java
│   │   └── CqlEvaluationControllerIT.java
│   ├── service/
│   │   ├── CqlEvaluatorTest.java
│   │   ├── HedisMeasureServiceTest.java
│   │   ├── StarRatingPredictorTest.java
│   │   └── CqlEvaluatorIT.java
│   ├── measures/                                # 52 measure tests
│   │   ├── HedisCBPMeasureTest.java            # Blood Pressure Control
│   │   ├── HedisHbA1cMeasureTest.java          # Diabetes HbA1c
│   │   ├── HedisBreastCancerScreeningTest.java
│   │   └── ... (49 more)
│   └── CqlEngineE2ETest.java
```

**Test Count Estimate**: 300 tests (52 measures × 3 tests each + 144 other tests)

**Test Examples**:

```java
// Measure Test
@Test
void testHedisCBP_controlledBloodPressure_shouldPass() {
    // Given: Patient with hypertension diagnosis
    String patientId = createPatientWithHypertension();

    // And: BP readings <140/90 in measurement period
    createObservation(patientId, "85354-9", "130/85", period.getStart());
    createObservation(patientId, "85354-9", "125/80", period.getStart().plusMonths(6));

    // When: Evaluate HEDIS_CBP measure
    MeasureResult result = cqlService.evaluate("HEDIS_CBP", patientId, period);

    // Then: Should pass
    assertEquals(1, result.getScore());
    assertEquals("PASS", result.getResult());
    assertEquals(178, result.getEvaluationTimeMs(), 50); // ±50ms
}

@Test
void testHedisCBP_uncontrolledBloodPressure_shouldFail() {
    // Given: Patient with hypertension diagnosis
    String patientId = createPatientWithHypertension();

    // And: BP readings >140/90
    createObservation(patientId, "85354-9", "150/95", period.getStart());
    createObservation(patientId, "85354-9", "145/92", period.getStart().plusMonths(6));

    // When: Evaluate HEDIS_CBP measure
    MeasureResult result = cqlService.evaluate("HEDIS_CBP", patientId, period);

    // Then: Should fail
    assertEquals(0, result.getScore());
    assertEquals("FAIL", result.getResult());
}

@Test
void testHedisCBP_noData_shouldBeExcluded() {
    // Given: Patient with hypertension diagnosis
    String patientId = createPatientWithHypertension();

    // And: No BP observations

    // When: Evaluate HEDIS_CBP measure
    MeasureResult result = cqlService.evaluate("HEDIS_CBP", patientId, period);

    // Then: Should be excluded (not in denominator)
    assertEquals(MeasureResult.Status.EXCLUDED, result.getStatus());
}
```

#### 4.1.3 Consent Service

**Test File Structure**:
```
backend/modules/services/consent-service/src/test/java/
├── com/healthdata/consent/
│   ├── controller/
│   │   ├── ConsentPolicyControllerTest.java
│   │   ├── RBACControllerTest.java
│   │   └── ConsentControllerIT.java
│   ├── service/
│   │   ├── ConsentEvaluationServiceTest.java
│   │   ├── FieldLevelFilteringServiceTest.java
│   │   ├── RBACServiceTest.java
│   │   └── ConsentServiceIT.java
│   ├── rbac/
│   │   ├── RolePermissionMatrixTest.java
│   │   ├── PermissionEvaluatorTest.java
│   │   └── MinimumNecessaryRuleTest.java
│   └── ConsentE2ETest.java
```

**Test Count Estimate**: 200 tests

**Test Examples**:

```java
// RBAC Test
@Test
void testRBAC_nurseRole_shouldNotHaveDeletePermission() {
    // Given: User with NURSE role
    String userId = "nurse-1";
    assignRole(userId, RoleType.NURSE);

    // When: Check delete:fhir permission
    boolean hasPermission = rbacService.hasPermission(
        userId, Permission.DELETE_FHIR);

    // Then: Should not have permission
    assertFalse(hasPermission);
}

// Field-Level Filtering Test
@Test
void testFieldFiltering_researcherRole_shouldRedactPHI() {
    // Given: User with RESEARCHER role (de-identified data only)
    String userId = "researcher-1";
    assignRole(userId, RoleType.RESEARCHER);

    // And: Patient with PHI fields
    Patient patient = createPatient();
    patient.addIdentifier()
        .setSystem("http://hl7.org/fhir/sid/us-ssn")
        .setValue("123-45-6789");
    patient.setBirthDate(Date.from(Instant.parse("1990-01-01T00:00:00Z")));

    // When: Apply field-level filtering
    Patient filtered = fieldFilteringService.applyConsent(patient, userId);

    // Then: PHI fields should be redacted
    assertTrue(filtered.getIdentifier().isEmpty());
    assertNull(filtered.getBirthDate());
}

// Consent Evaluation Performance Test
@Test
void testConsentEvaluation_shouldCompleteIn5ms() {
    // Given: Consent policy and user
    String userId = "clinician-1";
    String patientId = "patient-123";

    // When: Evaluate consent 100 times
    List<Long> times = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
        long start = System.nanoTime();
        consentService.evaluate(userId, patientId, Action.READ);
        long end = System.nanoTime();
        times.add((end - start) / 1_000_000); // ms
    }

    // Then: p95 should be <5ms
    Collections.sort(times);
    long p95 = times.get(94);
    assertTrue(p95 < 5, "p95: " + p95 + "ms");
}
```

#### 4.1.4 Event Processing Service

**Test File Structure**:
```
backend/modules/services/event-processing-service/src/test/java/
├── com/healthdata/events/
│   ├── controller/
│   │   ├── EventPublishControllerTest.java
│   │   └── CareGapControllerTest.java
│   ├── consumer/
│   │   ├── FhirEventConsumerTest.java
│   │   ├── ObservationEventConsumerTest.java
│   │   └── EventConsumerIT.java
│   ├── processor/
│   │   ├── CareGapDetectorTest.java
│   │   ├── CareGapDetectorIT.java
│   │   └── CareGapDetectorPerformanceTest.java
│   ├── webhook/
│   │   ├── WebhookDeliveryServiceTest.java
│   │   └── WebhookRetryLogicTest.java
│   └── EventProcessingE2ETest.java
```

**Test Count Estimate**: 150 tests

**Test Examples**:

```java
// Kafka Consumer Test
@SpringBootTest
@EmbeddedKafka(topics = {"fhir.resources.observation"})
class ObservationEventConsumerTest {

    @Autowired
    private KafkaTemplate<String, ObservationEvent> kafkaTemplate;

    @Autowired
    private CareGapRepository careGapRepository;

    @Test
    void consumeObservationEvent_diabeticPatient_shouldDetectCareGap()
            throws Exception {
        // Given: Diabetic patient with no HbA1c test
        String patientId = createDiabeticPatient();

        // When: Publish observation event (not HbA1c)
        ObservationEvent event = new ObservationEvent(
            patientId, "blood-glucose", "120 mg/dL", Instant.now());
        kafkaTemplate.send("fhir.resources.observation", event);

        // Then: Care gap should be detected within 5 seconds
        await().atMost(5, SECONDS).untilAsserted(() -> {
            List<CareGap> gaps = careGapRepository.findByPatientId(patientId);
            assertEquals(1, gaps.size());
            assertEquals("HEDIS_HbA1c", gaps.get(0).getMeasureId());
        });
    }
}

// Webhook Delivery Test
@Test
void testWebhookDelivery_shouldRetryOn5xxError() {
    // Given: Webhook configuration
    WebhookConfig webhook = createWebhook("https://ehr.example.com/webhook");

    // And: Mock server returning 500 (server error)
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));
    mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // Success on 3rd try

    // When: Deliver webhook
    CareGapDetectedEvent event = new CareGapDetectedEvent(...);
    webhookService.deliver(webhook, event);

    // Then: Should retry 3 times
    assertEquals(3, mockWebServer.getRequestCount());

    // And: Delivery log should show success
    WebhookDeliveryLog log = webhookLogRepository.findByEventId(event.getEventId());
    assertEquals(DeliveryStatus.SUCCESS, log.getStatus());
    assertEquals(3, log.getAttempts());
}

// Performance Test
@Test
void testCareGapDetection_shouldDetectIn5Seconds() {
    // Given: 100 diabetic patients without HbA1c tests
    List<String> patientIds = createDiabeticPatients(100);

    // When: Publish 100 observation events simultaneously
    long start = System.currentTimeMillis();
    patientIds.forEach(patientId -> {
        ObservationEvent event = new ObservationEvent(patientId, "blood-glucose", ...);
        kafkaTemplate.send("fhir.resources.observation", event);
    });

    // Then: All 100 care gaps should be detected within 5 seconds
    await().atMost(5, SECONDS).untilAsserted(() -> {
        long detectedCount = careGapRepository.countByStatus(CareGapStatus.OPEN);
        assertEquals(100, detectedCount);
    });

    long duration = System.currentTimeMillis() - start;
    assertTrue(duration < 5000, "Detection took " + duration + "ms");
}
```

### 4.2 Test Data Generation Strategy

#### 4.2.1 FHIR Test Data Generator

**Location**: `backend/platform/test-fixtures/src/main/java/com/healthdata/testdata/`

```java
public class FhirTestDataGenerator {

    public static Patient createPatient(String name, String birthDate) {
        Patient patient = new Patient();
        patient.addName()
            .setFamily(name.split(" ")[1])
            .addGiven(name.split(" ")[0]);
        patient.setBirthDate(Date.from(Instant.parse(birthDate + "T00:00:00Z")));
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.addIdentifier()
            .setSystem("http://hospital.org/mrn")
            .setValue(UUID.randomUUID().toString());
        return patient;
    }

    public static Observation createObservation(
            String patientId, String loincCode, String value) {
        Observation observation = new Observation();
        observation.setSubject(new Reference("Patient/" + patientId));
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.getCode().addCoding()
            .setSystem("http://loinc.org")
            .setCode(loincCode);
        // Set value based on type
        return observation;
    }

    public static List<Patient> createSyntheticPatients(int count) {
        List<Patient> patients = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Patient patient = createPatient(
                "Patient" + i,
                LocalDate.now().minusYears(30 + i % 50).toString());
            patients.add(patient);
        }
        return patients;
    }
}
```

#### 4.2.2 HEDIS Test Cases

**Location**: `backend/platform/test-fixtures/src/main/resources/hedis-test-cases/`

```
hedis-test-cases/
├── HEDIS_CBP/
│   ├── pass-case-1.json
│   ├── pass-case-2.json
│   ├── fail-case-1.json
│   ├── fail-case-2.json
│   └── excluded-case-1.json
├── HEDIS_HbA1c/
│   ├── pass-case-1.json
│   └── ... (52 measures total)
```

**Example Test Case** (`HEDIS_CBP/pass-case-1.json`):
```json
{
  "description": "Patient with controlled blood pressure",
  "patient": {
    "id": "patient-cbp-pass-1",
    "birthDate": "1975-01-01",
    "gender": "male"
  },
  "conditions": [
    {
      "code": "I10",
      "system": "http://hl7.org/fhir/sid/icd-10",
      "display": "Essential hypertension",
      "onsetDate": "2020-01-01"
    }
  ],
  "observations": [
    {
      "loincCode": "85354-9",
      "display": "Blood pressure",
      "value": "130/85",
      "date": "2024-06-15"
    },
    {
      "loincCode": "85354-9",
      "display": "Blood pressure",
      "value": "125/80",
      "date": "2024-12-15"
    }
  ],
  "expectedResult": {
    "score": 1,
    "result": "PASS",
    "rationale": "Patient has 2 BP readings <140/90 in measurement period"
  }
}
```

### 4.3 Continuous Integration (CI) Setup

#### 4.3.1 GitHub Actions Workflow

**File**: `.github/workflows/backend-ci.yml`

```yaml
name: Backend CI

on:
  push:
    branches: [ main, develop ]
    paths:
      - 'backend/**'
  pull_request:
    branches: [ main, develop ]
    paths:
      - 'backend/**'

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_PASSWORD: test_password
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      kafka:
        image: confluentinc/cp-kafka:7.5.0
        ports:
          - 9092:9092
        env:
          KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
          KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092

    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}

      - name: Run Tests
        working-directory: ./backend
        run: |
          export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
          ./gradlew test --info

      - name: Generate Coverage Report
        working-directory: ./backend
        run: ./gradlew jacocoTestReport

      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./backend/build/reports/jacoco/test/jacocoTestReport.xml
          flags: backend

      - name: Publish Test Results
        uses: EnricoMi/publish-unit-test-result-action@v2
        if: always()
        with:
          files: |
            backend/**/build/test-results/test/*.xml

  integration-test:
    runs-on: ubuntu-latest
    needs: test

    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run Integration Tests
        working-directory: ./backend
        run: |
          export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
          ./gradlew integrationTest --info

  build:
    runs-on: ubuntu-latest
    needs: [test, integration-test]

    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build Services
        working-directory: ./backend
        run: |
          export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
          ./gradlew buildAllServices

      - name: Build Docker Images
        run: |
          docker-compose build fhir-service
          docker-compose build cql-engine-service
          docker-compose build consent-service
          docker-compose build event-processing-service
```

---

## 5. Frontend Implementation Plan

### 5.1 Angular Component TDD Strategy

#### 5.1.1 Component Test Structure

```
frontend/libs/shared/ui/src/lib/components/service-card/
├── service-card.component.ts
├── service-card.component.html
├── service-card.component.scss
├── service-card.component.spec.ts          # Unit tests
└── service-card.component.e2e.spec.ts      # E2E tests (Playwright)
```

#### 5.1.2 Test-First Component Development

**Step 1: Write failing test**
```typescript
// service-card.component.spec.ts
describe('ServiceCardComponent', () => {
  it('should create', () => {
    const fixture = TestBed.createComponent(ServiceCardComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('should display service name', () => {
    const fixture = TestBed.createComponent(ServiceCardComponent);
    const component = fixture.componentInstance;
    component.service = {
      name: 'FHIR Service',
      status: 'UP',
      responseTime: 45,
      uptime: 99.9
    };
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('h3').textContent).toContain('FHIR Service');
  });

  it('should display green indicator for healthy service', () => {
    const fixture = TestBed.createComponent(ServiceCardComponent);
    const component = fixture.componentInstance;
    component.service = { name: 'FHIR', status: 'UP', ... };
    fixture.detectChanges();

    const indicator = fixture.nativeElement.querySelector('.health-indicator');
    expect(indicator.classList.contains('bg-green-500')).toBe(true);
  });
});
```

**Step 2: Implement component (minimal to pass tests)**
```typescript
// service-card.component.ts
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface Service {
  name: string;
  status: 'UP' | 'DOWN' | 'DEGRADED';
  responseTime: number | null;
  uptime: number;
}

@Component({
  selector: 'healthdata-service-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './service-card.component.html',
  styleUrls: ['./service-card.component.scss']
})
export class ServiceCardComponent {
  @Input() service!: Service;

  get healthColor(): string {
    switch (this.service.status) {
      case 'UP': return 'bg-green-500';
      case 'DOWN': return 'bg-red-500';
      case 'DEGRADED': return 'bg-yellow-500';
      default: return 'bg-gray-500';
    }
  }
}
```

**Step 3: Write template**
```html
<!-- service-card.component.html -->
<div class="service-card">
  <div class="flex items-center justify-between">
    <h3 class="text-lg font-semibold">{{ service.name }}</h3>
    <div class="health-indicator w-4 h-4 rounded-full"
         [ngClass]="healthColor"></div>
  </div>
  <div class="mt-2">
    <p class="text-sm text-gray-600">Status: {{ service.status }}</p>
    <p class="text-sm text-gray-600" *ngIf="service.responseTime">
      Response Time: {{ service.responseTime }}ms
    </p>
    <p class="text-sm text-gray-600">Uptime: {{ service.uptime }}%</p>
  </div>
</div>
```

**Step 4: Run tests (should pass), refactor if needed**

### 5.2 Service TDD Strategy

#### 5.2.1 HTTP Service Testing

```typescript
// fhir.service.spec.ts
describe('FhirService', () => {
  let service: FhirService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [FhirService]
    });
    service = TestBed.inject(FhirService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create patient', () => {
    const mockPatient: Patient = {
      resourceType: 'Patient',
      name: [{ family: 'Smith', given: ['John'] }]
    };

    service.createPatient(mockPatient).subscribe(patient => {
      expect(patient.id).toBeTruthy();
      expect(patient.name[0].family).toBe('Smith');
    });

    const req = httpMock.expectOne('http://localhost:8085/fhir/Patient');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(mockPatient);

    req.flush({ ...mockPatient, id: 'patient-123' });
  });

  it('should handle 404 error', () => {
    service.getPatient('nonexistent').subscribe(
      () => fail('should have failed with 404'),
      (error: HttpErrorResponse) => {
        expect(error.status).toBe(404);
        expect(error.error).toContain('Patient not found');
      }
    );

    const req = httpMock.expectOne('http://localhost:8085/fhir/Patient/nonexistent');
    req.flush('Patient not found', { status: 404, statusText: 'Not Found' });
  });
});
```

### 5.3 Screen-by-Screen TDD Implementation

#### 5.3.1 Platform Dashboard

**Test File**: `frontend/admin-portal/src/app/pages/platform-dashboard/platform-dashboard.component.spec.ts`

```typescript
describe('PlatformDashboardComponent', () => {
  let component: PlatformDashboardComponent;
  let fixture: ComponentFixture<PlatformDashboardComponent>;
  let platformService: jasmine.SpyObj<PlatformService>;

  beforeEach(async () => {
    const platformServiceSpy = jasmine.createSpyObj('PlatformService',
      ['getPlatformHealth']);

    await TestBed.configureTestingModule({
      imports: [PlatformDashboardComponent],
      providers: [
        { provide: PlatformService, useValue: platformServiceSpy }
      ]
    }).compileComponents();

    platformService = TestBed.inject(PlatformService) as jasmine.SpyObj<PlatformService>;
    fixture = TestBed.createComponent(PlatformDashboardComponent);
    component = fixture.componentInstance;
  });

  it('should load 9 service statuses', fakeAsync(() => {
    const mockHealth: PlatformHealth = {
      services: [
        { name: 'PostgreSQL', status: 'UP', ... },
        { name: 'Redis', status: 'UP', ... },
        { name: 'Kafka', status: 'UP', ... },
        { name: 'FHIR Service', status: 'UP', ... },
        { name: 'CQL Engine', status: 'UP', ... },
        { name: 'Consent Service', status: 'UP', ... },
        { name: 'Event Processing', status: 'UP', ... },
        { name: 'Kong Gateway', status: 'UP', ... },
        { name: 'Kafka UI', status: 'UP', ... }
      ]
    };

    platformService.getPlatformHealth.and.returnValue(of(mockHealth));

    fixture.detectChanges();
    tick();

    expect(component.services.length).toBe(9);
    expect(component.allServicesHealthy).toBe(true);
  }));

  it('should auto-refresh every 30 seconds', fakeAsync(() => {
    platformService.getPlatformHealth.and.returnValue(of({ services: [] }));

    fixture.detectChanges();

    expect(platformService.getPlatformHealth).toHaveBeenCalledTimes(1);

    tick(30000);
    expect(platformService.getPlatformHealth).toHaveBeenCalledTimes(2);

    tick(30000);
    expect(platformService.getPlatformHealth).toHaveBeenCalledTimes(3);

    component.ngOnDestroy(); // Stop auto-refresh
  }));

  it('should display alert when service is down', fakeAsync(() => {
    const mockHealth: PlatformHealth = {
      services: [
        { name: 'FHIR Service', status: 'DOWN', ... }
      ]
    };

    platformService.getPlatformHealth.and.returnValue(of(mockHealth));

    fixture.detectChanges();
    tick();

    const alert = fixture.nativeElement.querySelector('.alert-danger');
    expect(alert).toBeTruthy();
    expect(alert.textContent).toContain('FHIR Service is down');
  }));
});
```

#### 5.3.2 API Playground

**Test File**: `frontend/admin-portal/src/app/pages/api-playground/api-playground.component.spec.ts`

```typescript
describe('ApiPlaygroundComponent', () => {
  let component: ApiPlaygroundComponent;
  let fixture: ComponentFixture<ApiPlaygroundComponent>;
  let apiService: jasmine.SpyObj<ApiPlaygroundService>;

  beforeEach(async () => {
    const apiServiceSpy = jasmine.createSpyObj('ApiPlaygroundService',
      ['sendRequest', 'getRequestHistory', 'saveToFavorites']);

    await TestBed.configureTestingModule({
      imports: [ApiPlaygroundComponent],
      providers: [
        { provide: ApiPlaygroundService, useValue: apiServiceSpy }
      ]
    }).compileComponents();

    apiService = TestBed.inject(ApiPlaygroundService) as jasmine.SpyObj<ApiPlaygroundService>;
    fixture = TestBed.createComponent(ApiPlaygroundComponent);
    component = fixture.componentInstance;
  });

  it('should have 20+ quick actions', () => {
    fixture.detectChanges();

    expect(component.quickActions.length).toBeGreaterThanOrEqual(20);

    // Verify key quick actions exist
    const actionNames = component.quickActions.map(a => a.name);
    expect(actionNames).toContain('Create Patient');
    expect(actionNames).toContain('Search Patients');
    expect(actionNames).toContain('Evaluate HEDIS Measure');
    expect(actionNames).toContain('Create Consent Policy');
  });

  it('should send API request and display response', fakeAsync(() => {
    const mockRequest: ApiRequest = {
      method: 'POST',
      url: 'http://localhost:8085/fhir/Patient',
      headers: { 'Content-Type': 'application/fhir+json' },
      body: { resourceType: 'Patient', name: [{ family: 'Smith' }] }
    };

    const mockResponse: ApiResponse = {
      status: 201,
      statusText: 'Created',
      headers: {},
      body: { ...mockRequest.body, id: 'patient-123' },
      duration: 45
    };

    apiService.sendRequest.and.returnValue(of(mockResponse));

    component.request = mockRequest;
    component.sendRequest();
    tick();

    expect(component.response).toEqual(mockResponse);
    expect(component.responseStatus).toBe(201);
    expect(component.responseDuration).toBe(45);
  }));

  it('should store request in history', fakeAsync(() => {
    const mockRequest: ApiRequest = { ... };
    const mockResponse: ApiResponse = { ... };

    apiService.sendRequest.and.returnValue(of(mockResponse));
    apiService.getRequestHistory.and.returnValue(of([mockRequest]));

    component.request = mockRequest;
    component.sendRequest();
    tick();

    expect(apiService.getRequestHistory).toHaveBeenCalled();
    expect(component.requestHistory.length).toBeGreaterThan(0);
  }));

  it('should substitute variables in request', () => {
    component.request = {
      method: 'GET',
      url: 'http://localhost:8085/fhir/Patient/{{patientId}}',
      headers: {},
      body: null
    };
    component.variables = { patientId: 'patient-123' };

    const substituted = component.substituteVariables(component.request);

    expect(substituted.url).toBe('http://localhost:8085/fhir/Patient/patient-123');
  });

  it('should limit history to 20 requests', fakeAsync(() => {
    // Add 25 requests to history
    for (let i = 0; i < 25; i++) {
      component.addToHistory({ method: 'GET', url: `/api/${i}`, ... });
    }

    expect(component.requestHistory.length).toBe(20);
    expect(component.requestHistory[0].url).toBe('/api/24'); // Most recent first
  }));
});
```

### 5.4 E2E Testing with Playwright

#### 5.4.1 E2E Test Structure

```
frontend/e2e/admin-portal-e2e/
├── src/
│   ├── fixtures/
│   │   ├── test-data.ts
│   │   └── api-mocks.ts
│   ├── pages/
│   │   ├── platform-dashboard.page.ts
│   │   ├── api-playground.page.ts
│   │   └── fhir-management.page.ts
│   └── tests/
│       ├── platform-dashboard.spec.ts
│       ├── api-playground.spec.ts
│       ├── service-management.spec.ts
│       └── integration-examples.spec.ts
└── playwright.config.ts
```

#### 5.4.2 Page Object Pattern

```typescript
// pages/platform-dashboard.page.ts
export class PlatformDashboardPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/admin/platform-dashboard');
  }

  async getServiceCards() {
    return await this.page.locator('.service-card').all();
  }

  async getServiceStatus(serviceName: string): Promise<string> {
    const card = this.page.locator(`.service-card:has-text("${serviceName}")`);
    return await card.locator('.status-text').textContent();
  }

  async getHealthIndicatorColor(serviceName: string): Promise<string> {
    const card = this.page.locator(`.service-card:has-text("${serviceName}")`);
    const indicator = card.locator('.health-indicator');
    const classes = await indicator.getAttribute('class');
    if (classes?.includes('bg-green')) return 'green';
    if (classes?.includes('bg-yellow')) return 'yellow';
    if (classes?.includes('bg-red')) return 'red';
    return 'unknown';
  }

  async waitForRefresh() {
    await this.page.waitForTimeout(30000); // Wait for auto-refresh
  }
}
```

#### 5.4.3 E2E Test Examples

```typescript
// tests/platform-dashboard.spec.ts
import { test, expect } from '@playwright/test';
import { PlatformDashboardPage } from '../pages/platform-dashboard.page';

test.describe('Platform Dashboard', () => {
  let dashboardPage: PlatformDashboardPage;

  test.beforeEach(async ({ page }) => {
    dashboardPage = new PlatformDashboardPage(page);
    await dashboardPage.goto();
  });

  test('should display 9 service cards', async () => {
    const cards = await dashboardPage.getServiceCards();
    expect(cards.length).toBe(9);
  });

  test('should show all services as healthy', async ({ page }) => {
    // Mock API to return all services UP
    await page.route('**/api/admin/platform-health', route => {
      route.fulfill({
        status: 200,
        body: JSON.stringify({
          services: [
            { name: 'PostgreSQL', status: 'UP', responseTime: 10, uptime: 99.9 },
            { name: 'Redis', status: 'UP', responseTime: 5, uptime: 99.9 },
            // ... 7 more services
          ]
        })
      });
    });

    await dashboardPage.goto();

    const postgresStatus = await dashboardPage.getServiceStatus('PostgreSQL');
    expect(postgresStatus).toBe('UP');

    const redisColor = await dashboardPage.getHealthIndicatorColor('Redis');
    expect(redisColor).toBe('green');
  });

  test('should auto-refresh every 30 seconds', async ({ page }) => {
    let requestCount = 0;

    await page.route('**/api/admin/platform-health', route => {
      requestCount++;
      route.fulfill({
        status: 200,
        body: JSON.stringify({ services: [] })
      });
    });

    await dashboardPage.goto();

    expect(requestCount).toBe(1);

    await page.waitForTimeout(30000);
    expect(requestCount).toBeGreaterThanOrEqual(2);
  });

  test('full user journey: view dashboard → click service → see details', async ({ page }) => {
    await dashboardPage.goto();

    // Click on FHIR Service card
    await page.click('text=FHIR Service');

    // Should navigate to FHIR service details page
    await expect(page).toHaveURL('/admin/services/fhir');

    // Should display resource types
    await expect(page.locator('text=150+ FHIR R4 Resources')).toBeVisible();

    // Should display metrics
    await expect(page.locator('.metric-card')).toHaveCount(4);
  });
});
```

---

## 6. Integration & E2E Testing

### 6.1 Cross-Service Integration Tests

#### 6.1.1 Patient Data Flow (Beginner Example)

**Test File**: `backend/integration-tests/src/test/java/com/healthdata/integration/PatientDataFlowIT.java`

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class PatientDataFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private KafkaTemplate<String, PatientEvent> kafkaTemplate;

    @Test
    void fullPatientDataFlow_shouldWorkEndToEnd() throws Exception {
        // 1. Create patient via FHIR API
        Patient patient = FhirTestDataGenerator.createPatient("John Smith", "1990-01-01");

        ResponseEntity<Patient> createResponse = restTemplate.postForEntity(
            "/fhir/Patient", patient, Patient.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody().getId());
        String patientId = createResponse.getBody().getId();

        // 2. Verify Kafka event published
        ConsumerRecord<String, PatientEvent> event = consumeKafkaEvent(
            "fhir.resources.patient", patientId, 5000);

        assertNotNull(event);
        assertEquals("PATIENT_CREATED", event.value().getEventType());
        assertEquals(patientId, event.value().getPatientId());

        // 3. Retrieve patient by ID
        ResponseEntity<Patient> getResponse = restTemplate.getForEntity(
            "/fhir/Patient/" + patientId, Patient.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals(patientId, getResponse.getBody().getId());

        // 4. Verify cached in Redis (second read should be faster)
        long start = System.nanoTime();
        restTemplate.getForEntity("/fhir/Patient/" + patientId, Patient.class);
        long cachedTime = (System.nanoTime() - start) / 1_000_000; // ms

        assertTrue(cachedTime < 50, "Cached read took " + cachedTime + "ms");

        // 5. Update patient
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        restTemplate.put("/fhir/Patient/" + patientId, patient);

        // 6. Verify update event published
        PatientEvent updateEvent = consumeKafkaEvent(
            "fhir.resources.patient", patientId, 5000).value();

        assertEquals("PATIENT_UPDATED", updateEvent.getEventType());

        // 7. Search for patient
        ResponseEntity<Bundle> searchResponse = restTemplate.getForEntity(
            "/fhir/Patient?name=Smith", Bundle.class);

        assertEquals(HttpStatus.OK, searchResponse.getStatusCode());
        assertTrue(searchResponse.getBody().getTotal() >= 1);
    }
}
```

#### 6.1.2 HEDIS Measure Evaluation (Intermediate Example)

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class HedisMeasureEvaluationIT {

    @Test
    void fullMeasureEvaluationFlow_shouldWorkEndToEnd() throws Exception {
        // 1. Create patient with hypertension
        Patient patient = createPatient("Jane Doe", "1975-01-01");
        String patientId = fhirClient.createPatient(patient);

        Condition hypertension = createCondition(patientId, "I10", "Essential hypertension");
        fhirClient.createCondition(hypertension);

        // 2. Create blood pressure observations
        createObservation(patientId, "85354-9", "130/85", LocalDate.now().minusMonths(3));
        createObservation(patientId, "85354-9", "125/80", LocalDate.now().minusMonths(6));

        // 3. Evaluate HEDIS_CBP measure
        MeasureEvaluationRequest request = new MeasureEvaluationRequest();
        request.setMeasureId("HEDIS_CBP");
        request.setPatientId(patientId);
        request.setPeriodStart(LocalDate.now().minusYears(1));
        request.setPeriodEnd(LocalDate.now());

        long start = System.currentTimeMillis();

        ResponseEntity<MeasureResult> response = restTemplate.postForEntity(
            "/cql/evaluate", request, MeasureResult.class);

        long duration = System.currentTimeMillis() - start;

        // 4. Verify result
        assertEquals(HttpStatus.OK, response.getStatusCode());

        MeasureResult result = response.getBody();
        assertEquals(1, result.getScore());
        assertEquals("PASS", result.getResult());
        assertTrue(result.getRationale().contains("BP readings within threshold"));

        // 5. Verify performance SLA (<178ms average)
        assertTrue(duration < 500, "Evaluation took " + duration + "ms");

        // 6. Verify cached (second evaluation should be faster)
        start = System.currentTimeMillis();
        restTemplate.postForEntity("/cql/evaluate", request, MeasureResult.class);
        long cachedDuration = System.currentTimeMillis() - start;

        assertTrue(cachedDuration < 50, "Cached evaluation took " + cachedDuration + "ms");

        // 7. Verify Kafka event published
        ConsumerRecord<String, MeasureEvaluatedEvent> event = consumeKafkaEvent(
            "cql.measure.evaluation", patientId, 5000);

        assertNotNull(event);
        assertEquals("HEDIS_CBP", event.value().getMeasureId());
        assertEquals(1, event.value().getScore());
    }
}
```

#### 6.1.3 Care Gap Detection (Advanced Example)

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@AutoConfigureWebTestClient
class CareGapDetectionIT {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private CareGapRepository careGapRepository;

    @Test
    void realTimeCareGapDetection_shouldDetectGapIn5Seconds() throws Exception {
        // 1. Create diabetic patient
        Patient patient = createPatient("Bob Wilson", "1980-01-01");
        String patientId = fhirClient.createPatient(patient);

        Condition diabetes = createCondition(patientId, "E11", "Type 2 diabetes");
        fhirClient.createCondition(diabetes);

        // 2. No HbA1c test in last 12 months (creates care gap)

        // 3. Create new observation (triggers care gap detection)
        Observation bloodGlucose = createObservation(
            patientId, "2339-0", "120 mg/dL", LocalDate.now());

        long start = System.currentTimeMillis();

        fhirClient.createObservation(bloodGlucose);

        // 4. Wait for care gap to be detected (max 5 seconds)
        await().atMost(5, SECONDS).untilAsserted(() -> {
            List<CareGap> gaps = careGapRepository.findByPatientId(patientId);
            assertFalse(gaps.isEmpty());

            CareGap gap = gaps.get(0);
            assertEquals("HEDIS_HbA1c", gap.getMeasureId());
            assertEquals("HIGH", gap.getSeverity());
            assertEquals(CareGapStatus.OPEN, gap.getStatus());
        });

        long detectionTime = System.currentTimeMillis() - start;

        // 5. Verify performance (<5 seconds)
        assertTrue(detectionTime < 5000,
            "Care gap detection took " + detectionTime + "ms");

        // 6. Verify webhook notification sent
        // (Mock webhook server to capture request)
        MockWebServer webhookServer = new MockWebServer();
        webhookServer.enqueue(new MockResponse().setResponseCode(200));

        configureWebhook("https://ehr.example.com/webhook", webhookServer.url("/").toString());

        RecordedRequest webhookRequest = webhookServer.takeRequest(5, SECONDS);
        assertNotNull(webhookRequest);

        String body = webhookRequest.getBody().readUtf8();
        assertTrue(body.contains("CareGapDetected"));
        assertTrue(body.contains(patientId));

        // 7. Close care gap
        CareGap gap = careGapRepository.findByPatientId(patientId).get(0);

        CareGapClosureRequest closureRequest = new CareGapClosureRequest();
        closureRequest.setReason("HbA1c test scheduled for next week");
        closureRequest.setClosedBy("care-coordinator-1");

        webClient.post()
            .uri("/care-gaps/{id}/close", gap.getId())
            .bodyValue(closureRequest)
            .exchange()
            .expectStatus().isOk();

        // 8. Verify gap closed
        CareGap closedGap = careGapRepository.findById(gap.getId()).orElseThrow();
        assertEquals(CareGapStatus.CLOSED, closedGap.getStatus());
        assertNotNull(closedGap.getClosedAt());
        assertEquals("care-coordinator-1", closedGap.getClosedBy());
    }
}
```

### 6.2 Full-Stack E2E Tests

#### 6.2.1 Admin Portal End-to-End

```typescript
// frontend/e2e/tests/full-stack-integration.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Full-Stack Integration', () => {

  test('admin portal → FHIR API → database → cache flow', async ({ page }) => {
    // 1. Navigate to API Playground
    await page.goto('/admin/tools/api-playground');

    // 2. Select "Create Patient" quick action
    await page.click('text=Create Patient');

    // 3. Verify request pre-filled
    const method = await page.locator('[data-testid="request-method"]').textContent();
    expect(method).toBe('POST');

    const url = await page.locator('[data-testid="request-url"]').inputValue();
    expect(url).toContain('/fhir/Patient');

    // 4. Modify patient name
    const bodyEditor = page.locator('[data-testid="request-body"]');
    await bodyEditor.fill(JSON.stringify({
      resourceType: 'Patient',
      name: [{ family: 'TestUser', given: ['E2E'] }],
      gender: 'male',
      birthDate: '1990-01-01'
    }));

    // 5. Send request
    await page.click('[data-testid="send-request"]');

    // 6. Verify response
    await page.waitForSelector('[data-testid="response-status"]');
    const status = await page.locator('[data-testid="response-status"]').textContent();
    expect(status).toBe('201');

    const responseBody = await page.locator('[data-testid="response-body"]').textContent();
    const response = JSON.parse(responseBody);
    expect(response.id).toBeTruthy();

    const patientId = response.id;

    // 7. Verify request appears in history
    const historyItems = await page.locator('[data-testid="history-item"]').all();
    expect(historyItems.length).toBeGreaterThan(0);

    // 8. Navigate to FHIR Management UI
    await page.goto('/admin/services/fhir');

    // 9. Search for created patient
    await page.fill('[data-testid="search-input"]', 'TestUser');
    await page.click('[data-testid="search-button"]');

    // 10. Verify patient appears in results
    await page.waitForSelector(`text=${patientId}`);
    const patientRow = page.locator(`[data-patient-id="${patientId}"]`);
    await expect(patientRow).toBeVisible();

    // 11. Click on patient to view details
    await patientRow.click();

    // 12. Verify patient details page
    await expect(page).toHaveURL(`/admin/services/fhir/Patient/${patientId}`);
    await expect(page.locator('text=E2E TestUser')).toBeVisible();
    await expect(page.locator('text=1990-01-01')).toBeVisible();

    // 13. Navigate to System Health to verify cache
    await page.goto('/admin/tools/system-health');

    // 14. Check Redis metrics
    const redisCacheHit = await page.locator('[data-testid="redis-cache-hit-rate"]').textContent();
    const hitRate = parseFloat(redisCacheHit);
    expect(hitRate).toBeGreaterThan(0);
  });

  test('care gap detection: observation → event → gap → webhook', async ({ page, context }) => {
    // This test requires backend services running

    // 1. Set up webhook listener (mock EHR endpoint)
    const webhookPromise = context.waitForEvent('request', {
      predicate: request => request.url().includes('/webhook')
    });

    // 2. Create diabetic patient via API Playground
    await page.goto('/admin/tools/api-playground');

    // Create patient
    const patientId = await createPatientViaPlayground(page, {
      name: 'Diabetic Patient',
      birthDate: '1975-01-01'
    });

    // Create diabetes condition
    await createConditionViaPlayground(page, patientId, 'E11', 'Type 2 diabetes');

    // 3. Create observation (triggers care gap detection)
    const observationTime = Date.now();

    await createObservationViaPlayground(page, patientId, {
      code: '2339-0',
      value: '120 mg/dL',
      date: new Date().toISOString()
    });

    // 4. Navigate to Event Processing UI
    await page.goto('/admin/services/event-processing');

    // 5. Wait for care gap to appear (max 5 seconds)
    await page.waitForSelector(`[data-patient-id="${patientId}"][data-gap-measure="HEDIS_HbA1c"]`,
      { timeout: 5000 });

    const detectionTime = Date.now() - observationTime;
    expect(detectionTime).toBeLessThan(5000);

    // 6. Verify care gap details
    const gapRow = page.locator(`[data-patient-id="${patientId}"]`);
    await expect(gapRow.locator('text=HIGH')).toBeVisible(); // Severity
    await expect(gapRow.locator('text=HbA1c test overdue')).toBeVisible();

    // 7. Verify webhook was called
    const webhookRequest = await webhookPromise;
    expect(webhookRequest.url()).toContain('/webhook');

    const webhookBody = await webhookRequest.postDataJSON();
    expect(webhookBody.eventType).toBe('CareGapDetected');
    expect(webhookBody.patientId).toBe(patientId);
    expect(webhookBody.measureId).toBe('HEDIS_HbA1c');
  });
});
```

---

## 7. CI/CD Pipeline

### 7.1 Complete CI/CD Workflow

**File**: `.github/workflows/full-cicd.yml`

```yaml
name: Full CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

env:
  JAVA_VERSION: '21'
  NODE_VERSION: '20'
  DOCKER_REGISTRY: ghcr.io
  IMAGE_NAME: healthdata-in-motion

jobs:
  # Stage 1: Code Quality
  code-quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Checkstyle
        working-directory: ./backend
        run: ./gradlew checkstyleMain checkstyleTest

      - name: Set up Node
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}

      - name: ESLint & Prettier
        run: |
          npm ci
          npm run lint

  # Stage 2: Unit Tests (Backend)
  backend-unit-tests:
    runs-on: ubuntu-latest
    needs: code-quality

    steps:
      - uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Cache Gradle
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}

      - name: Run Unit Tests
        working-directory: ./backend
        run: |
          export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
          ./gradlew test --parallel

      - name: Generate Coverage Report
        working-directory: ./backend
        run: ./gradlew jacocoTestReport

      - name: Upload Coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./backend/build/reports/jacoco/test/jacocoTestReport.xml
          flags: backend-unit

      - name: Publish Test Results
        uses: EnricoMi/publish-unit-test-result-action@v2
        if: always()
        with:
          files: backend/**/build/test-results/test/*.xml

  # Stage 3: Unit Tests (Frontend)
  frontend-unit-tests:
    runs-on: ubuntu-latest
    needs: code-quality

    steps:
      - uses: actions/checkout@v4

      - name: Set up Node
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}

      - name: Cache npm
        uses: actions/cache@v3
        with:
          path: ~/.npm
          key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}

      - name: Install Dependencies
        run: npm ci

      - name: Run Unit Tests
        run: npm run test:frontend -- --coverage

      - name: Upload Coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./coverage/lcov.info
          flags: frontend-unit

  # Stage 4: Integration Tests (Backend)
  backend-integration-tests:
    runs-on: ubuntu-latest
    needs: [backend-unit-tests, frontend-unit-tests]

    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: healthdata_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      kafka:
        image: confluentinc/cp-kafka:7.5.0
        ports:
          - 9092:9092
        env:
          KAFKA_BROKER_ID: 1
          KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
          KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092

    steps:
      - uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Run Integration Tests
        working-directory: ./backend
        run: |
          export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
          ./gradlew integrationTest --parallel
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/healthdata_test
          SPRING_REDIS_HOST: localhost
          SPRING_KAFKA_BOOTSTRAP_SERVERS: localhost:9092

      - name: Upload Test Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: integration-test-results
          path: backend/**/build/test-results/integrationTest/*.xml

  # Stage 5: E2E Tests
  e2e-tests:
    runs-on: ubuntu-latest
    needs: backend-integration-tests

    steps:
      - uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Set up Node
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}

      - name: Start Infrastructure
        run: docker-compose -f docker-compose.ci.yml up -d

      - name: Wait for Services
        run: |
          sleep 30
          curl --retry 10 --retry-delay 5 http://localhost:8085/fhir/actuator/health

      - name: Install Playwright
        run: npx playwright install --with-deps

      - name: Run E2E Tests
        run: npm run test:e2e

      - name: Upload Playwright Report
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: playwright-report
          path: playwright-report/

      - name: Stop Infrastructure
        if: always()
        run: docker-compose -f docker-compose.ci.yml down

  # Stage 6: Performance Tests
  performance-tests:
    runs-on: ubuntu-latest
    needs: backend-integration-tests
    if: github.ref == 'refs/heads/main'

    steps:
      - uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Start Infrastructure
        run: docker-compose -f docker-compose.ci.yml up -d

      - name: Run JMeter Tests
        working-directory: ./backend
        run: |
          wget https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.2.tgz
          tar -xzf apache-jmeter-5.6.2.tgz
          apache-jmeter-5.6.2/bin/jmeter -n -t performance-tests/fhir-load-test.jmx \
            -l results.jtl -e -o report/

      - name: Upload Performance Report
        uses: actions/upload-artifact@v3
        with:
          name: performance-report
          path: backend/report/

      - name: Check Performance SLAs
        run: |
          # Parse results.jtl and verify SLAs
          python scripts/check-slas.py backend/results.jtl

  # Stage 7: Security Scanning
  security-scan:
    runs-on: ubuntu-latest
    needs: [backend-unit-tests, frontend-unit-tests]

    steps:
      - uses: actions/checkout@v4

      - name: Run Trivy (Container Scan)
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          format: 'sarif'
          output: 'trivy-results.sarif'

      - name: Upload Trivy Results
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: 'trivy-results.sarif'

      - name: Run Snyk (Dependency Scan)
        uses: snyk/actions/gradle@master
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
        with:
          args: --severity-threshold=high

  # Stage 8: Build & Push Images
  build-images:
    runs-on: ubuntu-latest
    needs: [e2e-tests, performance-tests, security-scan]
    if: github.ref == 'refs/heads/main'

    strategy:
      matrix:
        service:
          - fhir-service
          - cql-engine-service
          - consent-service
          - event-processing-service
          - patient-service
          - quality-measure-service
          - care-gap-service
          - analytics-service
          - gateway-service

    steps:
      - uses: actions/checkout@v4

      - name: Log in to Container Registry
        uses: docker/login-action@v2
        with:
          registry: ${{ env.DOCKER_REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract Metadata
        id: meta
        uses: docker/metadata-action@v4
        with:
          images: ${{ env.DOCKER_REGISTRY }}/${{ github.repository }}/${{ matrix.service }}
          tags: |
            type=ref,event=branch
            type=semver,pattern={{version}}
            type=sha

      - name: Build and Push
        uses: docker/build-push-action@v4
        with:
          context: ./backend
          file: ./backend/Dockerfile
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          build-args: |
            SERVICE_NAME=${{ matrix.service }}

  # Stage 9: Deploy to Staging
  deploy-staging:
    runs-on: ubuntu-latest
    needs: build-images
    if: github.ref == 'refs/heads/main'
    environment: staging

    steps:
      - uses: actions/checkout@v4

      - name: Set up kubectl
        uses: azure/setup-kubectl@v3

      - name: Configure kubectl
        run: |
          echo "${{ secrets.KUBECONFIG_STAGING }}" | base64 -d > kubeconfig
          export KUBECONFIG=kubeconfig

      - name: Deploy to Staging
        run: |
          kubectl apply -f infrastructure/kubernetes/overlays/staging/
          kubectl rollout status deployment/fhir-service -n healthdata-staging

      - name: Run Smoke Tests
        run: |
          curl https://staging.healthdata.com/fhir/actuator/health
          curl https://staging.healthdata.com/cql/actuator/health

  # Stage 10: Deploy to Production (Manual Approval)
  deploy-production:
    runs-on: ubuntu-latest
    needs: deploy-staging
    if: github.ref == 'refs/heads/main'
    environment: production

    steps:
      - uses: actions/checkout@v4

      - name: Set up kubectl
        uses: azure/setup-kubectl@v3

      - name: Configure kubectl
        run: |
          echo "${{ secrets.KUBECONFIG_PRODUCTION }}" | base64 -d > kubeconfig
          export KUBECONFIG=kubeconfig

      - name: Deploy to Production
        run: |
          kubectl apply -f infrastructure/kubernetes/overlays/prod/
          kubectl rollout status deployment/fhir-service -n healthdata-prod

      - name: Run Smoke Tests
        run: |
          curl https://api.healthdata.com/fhir/actuator/health
          curl https://api.healthdata.com/cql/actuator/health

      - name: Notify Slack
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          text: 'Production deployment successful!'
          webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

---

## 8. Deployment Strategy

### 8.1 Kubernetes Deployment

#### 8.1.1 Directory Structure

```
infrastructure/kubernetes/
├── base/                           # Base configurations
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secrets.yaml
│   ├── fhir-service/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── hpa.yaml               # Horizontal Pod Autoscaler
│   ├── cql-engine-service/
│   ├── consent-service/
│   ├── event-processing-service/
│   ├── postgresql/
│   │   ├── statefulset.yaml
│   │   ├── service.yaml
│   │   └── pvc.yaml               # Persistent Volume Claim
│   ├── redis/
│   └── kafka/
├── overlays/                       # Environment-specific
│   ├── dev/
│   │   └── kustomization.yaml
│   ├── staging/
│   │   └── kustomization.yaml
│   └── prod/
│       └── kustomization.yaml
└── README.md
```

#### 8.1.2 Example: FHIR Service Deployment

**File**: `infrastructure/kubernetes/base/fhir-service/deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: fhir-service
  labels:
    app: fhir-service
    tier: backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: fhir-service
  template:
    metadata:
      labels:
        app: fhir-service
        version: v1
    spec:
      containers:
      - name: fhir-service
        image: ghcr.io/healthdata-in-motion/fhir-service:latest
        ports:
        - containerPort: 8085
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            configMapKeyRef:
              name: healthdata-config
              key: postgres.fhir.url
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: healthdata-secrets
              key: postgres.password
        - name: SPRING_DATA_REDIS_HOST
          valueFrom:
            configMapKeyRef:
              name: healthdata-config
              key: redis.host
        - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
          valueFrom:
            configMapKeyRef:
              name: healthdata-config
              key: kafka.bootstrap.servers
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /fhir/actuator/health/liveness
            port: 8085
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /fhir/actuator/health/readiness
            port: 8085
          initialDelaySeconds: 30
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
---
apiVersion: v1
kind: Service
metadata:
  name: fhir-service
spec:
  selector:
    app: fhir-service
  ports:
  - port: 8085
    targetPort: 8085
    protocol: TCP
  type: ClusterIP
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: fhir-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: fhir-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

#### 8.1.3 Ingress Configuration

**File**: `infrastructure/kubernetes/base/ingress.yaml`

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: healthdata-ingress
  annotations:
    kubernetes.io/ingress.class: "nginx"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  tls:
  - hosts:
    - api.healthdata.com
    secretName: healthdata-tls
  rules:
  - host: api.healthdata.com
    http:
      paths:
      - path: /fhir
        pathType: Prefix
        backend:
          service:
            name: fhir-service
            port:
              number: 8085
      - path: /cql
        pathType: Prefix
        backend:
          service:
            name: cql-engine-service
            port:
              number: 8084
      - path: /consent
        pathType: Prefix
        backend:
          service:
            name: consent-service
            port:
              number: 8090
      - path: /events
        pathType: Prefix
        backend:
          service:
            name: event-processing-service
            port:
              number: 8081
```

---

## 9. Success Criteria

### 9.1 Definition of Done (DoD)

Each user story is considered "Done" when:

✅ **Code Complete**:
- Implementation matches acceptance criteria
- Code follows style guidelines (Checkstyle, ESLint)
- No compiler warnings

✅ **Tests Pass**:
- All unit tests pass (80%+ coverage)
- All integration tests pass
- E2E tests pass for critical paths

✅ **Performance Met**:
- Response times within SLAs
- Cache hit rates meet targets
- No memory leaks detected

✅ **Security Verified**:
- No critical vulnerabilities (Snyk, Trivy)
- HIPAA audit logging in place
- Consent filtering works correctly

✅ **Documentation Updated**:
- API documentation generated
- README updated
- Code comments added

✅ **Peer Reviewed**:
- Code review approved
- No unresolved comments

✅ **Deployed to Staging**:
- Successfully deployed
- Smoke tests pass

### 9.2 Acceptance Criteria Checklist

#### 9.2.1 Backend Service Acceptance

**FHIR Service**:
- [ ] All 150+ FHIR resource types supported
- [ ] CRUD operations work for Patient, Observation, Condition, MedicationRequest
- [ ] Search parameters work (name, birthdate, identifier, _count, _offset)
- [ ] Transaction bundles work (all-or-nothing)
- [ ] Kafka events published for create/update/delete
- [ ] Redis caching works (TTL: 1 hour, 67%+ hit rate)
- [ ] Response time <500ms (p95)
- [ ] HIPAA audit log created for every operation
- [ ] Consent filtering applied automatically

**CQL Engine Service**:
- [ ] All 52 HEDIS measures implemented
- [ ] Evaluation time <178ms (average)
- [ ] Results cached for 24 hours
- [ ] Batch evaluation works (1,000 patients in <3 minutes)
- [ ] STAR rating prediction 95%+ accuracy
- [ ] CQL syntax errors handled gracefully

**Consent Service**:
- [ ] 13 roles defined
- [ ] 31 permissions assigned
- [ ] RBAC evaluation <5ms
- [ ] Field-level filtering works
- [ ] Minimum necessary rule enforced
- [ ] Audit log for every consent decision

**Event Processing Service**:
- [ ] Kafka consumers for 8 topics
- [ ] Care gap detection <5 seconds
- [ ] Webhook delivery with retry (5 attempts)
- [ ] Dead letter queue for failures
- [ ] Exactly-once semantics

#### 9.2.2 Frontend Acceptance

**Admin Portal**:
- [ ] Platform dashboard displays 9 service statuses
- [ ] Auto-refresh every 30 seconds
- [ ] Service management UIs for all 9 services
- [ ] API playground with 20+ quick actions
- [ ] Request history (last 20)
- [ ] Variable substitution works
- [ ] cURL command generation
- [ ] Mobile responsive (iPhone, iPad, desktop)

**API Documentation**:
- [ ] 5 OpenAPI specs generated
- [ ] Swagger UI loads correctly
- [ ] "Try it out" works for all endpoints
- [ ] Code examples render (cURL, JS, Python)

**Integration Examples**:
- [ ] 6 examples implemented
- [ ] All examples run successfully
- [ ] Code is syntactically correct
- [ ] Examples handle errors gracefully

### 9.3 Performance Benchmarks

#### 9.3.1 Load Testing Targets

**FHIR Service**:
- [ ] 50 rps sustained for 1 hour (no errors)
- [ ] 100 rps peak for 5 minutes (99% success rate)
- [ ] Response time p95 <500ms under load
- [ ] Memory usage <1GB per instance

**CQL Engine Service**:
- [ ] 20 rps sustained for 1 hour
- [ ] 40 rps peak for 5 minutes
- [ ] Evaluation time p95 <300ms under load
- [ ] Batch evaluation: 1,000 patients in <3 minutes

**Consent Service**:
- [ ] 100 rps sustained for 1 hour
- [ ] 200 rps peak for 5 minutes
- [ ] Evaluation time p95 <10ms under load

**Event Processing Service**:
- [ ] 12.4 events/sec sustained for 1 hour
- [ ] 50 events/sec peak for 5 minutes
- [ ] Care gap detection within 5 seconds

#### 9.3.2 Cache Performance

- [ ] Redis hit rate 67%+ for FHIR resources
- [ ] Redis hit rate 85%+ for CQL results
- [ ] Redis hit rate 98%+ for user sessions
- [ ] Cache invalidation <100ms

#### 9.3.3 Database Performance

- [ ] Connection pool utilization <70%
- [ ] Query execution time <50ms (average)
- [ ] Index usage 95%+
- [ ] No slow queries >1 second

---

## 10. Timeline & Resources

### 10.1 Revised Implementation Timeline

| Phase | Duration | Start | End | Deliverables |
|-------|----------|-------|-----|--------------|
| **Phase 1: Foundation Hardening** | 2 weeks | Week 1 | Week 2 | Implement shared infrastructure modules, database schemas, seed data, and reusable test fixtures |
| **Phase 2: FHIR Vertical Slice** | 3 weeks | Week 3 | Week 5 | Patient CRUD with PostgreSQL + Redis + Kafka wiring, REST endpoints, contract & integration tests |
| **Phase 3: Admin Integration** | 2 weeks | Week 6 | Week 7 | Replace frontend fallbacks with live APIs, add BFF/aggregators, Playwright smoke suites |
| **Phase 4: Service Expansion** | 4 weeks | Week 8 | Week 11 | Extend FHIR resources, introduce consent/rules stubs, begin event processing pipeline |
| **Phase 5: Quality & Release Readiness** | 2 weeks | Week 12 | Week 13 | Harden observability, performance validation, end-to-end regression pack, deployment automation |
| **TOTAL** | **13 weeks** | **Week 1** | **Week 13** | **Production-ready vertical slice foundation** |

### 10.2 Team Structure

#### 10.2.1 Recommended Team

| Role | Count | Responsibilities |
|------|-------|------------------|
| **Tech Lead** | 1 | Architecture, code reviews, technical decisions |
| **Backend Developers** | 3 | Java/Spring Boot microservices |
| **Frontend Developers** | 2 | Angular admin portal |
| **QA Engineers** | 2 | Test automation, E2E tests |
| **DevOps Engineer** | 1 | CI/CD, Kubernetes, monitoring |
| **Healthcare SME** | 1 (part-time) | HEDIS measures, FHIR validation |
| **TOTAL** | 10 FTEs | |

#### 10.2.2 Sprint Structure (2-week sprints)

**Sprint Goals**:
- Sprint 1 (Weeks 1-2): Implement shared infrastructure modules, migrations, and Testcontainers utilities.
- Sprint 2 (Weeks 3-4): Deliver Patient CRUD with persistence, validation, and unit coverage.
- Sprint 3 (Weeks 5-6): Wire Kafka/Redis integrations and add contract + integration tests.
- Sprint 4 (Weeks 7-8): Expose aggregation/BFF endpoints and integrate the admin portal with live data.
- Sprint 5 (Weeks 9-10): Expand FHIR resource support and introduce consent/rules stubs.
- Sprint 6 (Weeks 11-12): Harden observability, build E2E coverage, and prep performance harness.
- Release Week (Week 13): Final regression, deployment automation, and backlog triage.

### 10.3 Resource Requirements

#### 10.3.1 Development Infrastructure

**Local Development**:
- Java 21 JDK
- Node.js 20
- Docker Desktop (16GB RAM recommended)
- IDE licenses (IntelliJ IDEA, VSCode)

**CI/CD Infrastructure**:
- GitHub Actions (included in GitHub)
- Codecov (test coverage)
- Snyk (security scanning)
- Docker Hub or GitHub Container Registry

**Staging Environment** (Kubernetes):
- 3 worker nodes (8 CPU, 32GB RAM each)
- PostgreSQL (RDS or equivalent)
- Redis (ElastiCache or equivalent)
- Kafka (MSK or self-hosted)
- Load balancer
- **Estimated Cost**: $500-800/month

**Production Environment** (Kubernetes):
- 6 worker nodes (16 CPU, 64GB RAM each)
- PostgreSQL (RDS Multi-AZ)
- Redis (ElastiCache with replication)
- Kafka (MSK Multi-AZ)
- Load balancer
- CDN for static assets
- **Estimated Cost**: $2,000-3,000/month

#### 10.3.2 Third-Party Services

| Service | Purpose | Cost |
|---------|---------|------|
| GitHub | Code repository, CI/CD | $0 (open source) or $21/user/month |
| Codecov | Test coverage reporting | $0 (open source) or $10/month |
| Snyk | Security scanning | $0 (open source) or $25/month |
| Sentry | Error tracking | $26/month |
| Datadog | APM & monitoring | $15/host/month |
| PagerDuty | On-call alerts | $21/user/month |

#### 10.3.3 Training & Onboarding

**Healthcare Standards Training**:
- FHIR R4 fundamentals (40 hours)
- HEDIS measures overview (20 hours)
- HIPAA compliance (16 hours)

**Technical Training**:
- Java 21 features (8 hours)
- Spring Boot 3.3 (16 hours)
- Angular 17 (16 hours)
- Kubernetes (24 hours)

**Total Training**: ~140 hours per team member

### 10.4 Risk Management

#### 10.4.1 Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **HAPI FHIR integration issues** | Medium | High | Prototype in Week 1, fallback to custom impl |
| **CQL engine performance** | Medium | Medium | Optimize caching, use batch processing |
| **Kafka throughput bottleneck** | Low | Medium | Scale Kafka cluster, increase partitions |
| **Database scaling issues** | Low | High | Use read replicas, optimize indexes |
| **Security vulnerabilities** | Medium | High | Regular scans, prompt patching |

#### 10.4.2 Schedule Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **HEDIS measures take longer** | High | Medium | Start with 10 core measures, add rest later |
| **E2E testing delayed** | Medium | Low | Start E2E earlier (Week 15) |
| **Team turnover** | Medium | High | Good documentation, pair programming |
| **Scope creep** | High | High | Strict acceptance criteria, prioritize MVP |

#### 10.4.3 Compliance Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **HIPAA audit findings** | Low | High | External audit before production |
| **FHIR validation failures** | Medium | Medium | Use official FHIR validator |
| **Consent filtering bugs** | Low | High | Extensive consent testing, penetration test |

---

## 11. Next Steps

### 11.1 Immediate Actions (Week 1)

**Day 1-2**: Team Onboarding
- Review PRD and architecture
- Set up development environments
- Clone repository, run `./scripts/setup.sh`
- Familiarize with monorepo structure

**Day 3-4**: TDD Training
- TDD workshop (Red-Green-Refactor)
- Write first shared domain tests together
- Practice on FhirResourceWrapper class

**Day 5**: Sprint Planning
- Break down Phase 1 into tasks
- Assign ownership
- Set up project board (GitHub Projects or Jira)

### 11.2 Weekly Cadence

**Monday**:
- Sprint planning (for 2-week sprints, every other week)
- Review previous sprint
- Demo completed features

**Tuesday-Thursday**:
- Development (TDD cycle)
- Daily standups (15 min)
- Pair programming sessions

**Friday**:
- Code reviews
- Retrospective
- Knowledge sharing (brown bag lunch)

### 11.3 Monitoring Progress

**Metrics to Track**:
- Test coverage (target: 80%)
- Build success rate (target: 95%)
- Code review turnaround time (target: <24 hours)
- Sprint velocity (story points completed)
- Bug escape rate (target: <5%)
- Technical debt ratio (target: <10%)

**Tools**:
- SonarQube for code quality
- Codecov for coverage
- GitHub Actions for CI/CD metrics
- Jira/GitHub Projects for sprint tracking

---

## 12. Conclusion

This TDD implementation plan provides a comprehensive roadmap to transform the Health Data In Motion monorepo scaffold into a fully functional, production-ready SaaS platform for event-driven healthcare data processing.

**Key Success Factors**:
1. ✅ **Discipline**: Follow TDD Red-Green-Refactor cycle religiously
2. ✅ **Automation**: Automate everything (tests, builds, deployments)
3. ✅ **Collaboration**: Daily standups, pair programming, code reviews
4. ✅ **Focus**: Prioritize MVP, avoid scope creep
5. ✅ **Quality**: 80%+ test coverage, meet all SLAs
6. ✅ **Compliance**: HIPAA audit-ready from day one

**Expected Outcomes** (Week 26):
- ✅ 9 fully implemented microservices
- ✅ Complete Angular admin portal
- ✅ 80%+ test coverage (unit + integration + E2E)
- ✅ All performance SLAs met
- ✅ HIPAA compliant
- ✅ Production deployed on Kubernetes
- ✅ Comprehensive documentation
- ✅ **Ready for commercial launch**

---

**Let's build the future of healthcare data processing! 🚀**

---

**Document Version**: 1.0
**Last Updated**: October 28, 2025
**Status**: Ready for Implementation
**Next Review**: End of Week 2 (after Phase 1 completion)
