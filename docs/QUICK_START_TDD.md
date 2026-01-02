# Health Data In Motion - TDD Quick Start Guide

## 🎯 Mission

Transform the current monorepo scaffold into a **fully functional, production-ready SaaS platform** for event-driven healthcare data processing using **Test-Driven Development (TDD)**.

---

## 📊 Current State vs Target State

| Aspect | Current (0%) | Target (100%) | Timeline |
|--------|--------------|---------------|----------|
| **Backend Services** | Scaffolds only | 9 fully implemented | 14 weeks |
| **API Endpoints** | 0 implemented | 50+ complete | 14 weeks |
| **Frontend Screens** | 0 implemented | 20+ complete | 18 weeks |
| **Test Coverage** | 0% | 80%+ | Continuous |
| **FHIR Resources** | 0 supported | 150+ complete | 6 weeks |
| **HEDIS Measures** | 0 implemented | 52 complete | 10 weeks |
| **Performance SLAs** | 0% met | 100% met | 22 weeks |
| **Production Ready** | No | Yes | 26 weeks |

---

## 🏗️ TDD Development Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    TDD CYCLE (RED-GREEN-REFACTOR)           │
└─────────────────────────────────────────────────────────────┘

1. 🔴 RED Phase: Write Failing Test
   │
   ├─ Define requirements as test cases
   ├─ Write test that fails (feature doesn't exist yet)
   └─ Run test suite (expect failure)
   │
   ↓
2. 🟢 GREEN Phase: Make Test Pass
   │
   ├─ Write MINIMAL code to pass test
   ├─ Don't worry about perfection
   └─ Run test suite (expect success)
   │
   ↓
3. 🔵 REFACTOR Phase: Improve Code
   │
   ├─ Clean up code (remove duplication, improve naming)
   ├─ Keep tests passing during refactoring
   └─ Run test suite (expect success)
   │
   ↓
4. ♻️ REPEAT for next requirement
```

---

## 📋 26-Week Implementation Roadmap

### **Phase 1: Foundation** (Weeks 1-2)
- ✅ Shared domain models (FHIR, HEDIS, CQL)
- ✅ Infrastructure modules (security, audit, persistence)
- ✅ Database schemas (7 databases)
- ✅ Test data generators
- ✅ CI/CD pipeline

**Example First Test**:
```java
@Test
void testFhirResourceWrapper_shouldWrapPatient() {
    Patient patient = new Patient();
    patient.setId("patient-123");

    FhirResourceWrapper<Patient> wrapper =
        FhirResourceWrapper.wrap(patient, "user-1", "org-1");

    assertNotNull(wrapper.getCreatedAt());
    assertEquals("user-1", wrapper.getCreatedBy());
    assertTrue(wrapper.isContainsPHI());
}
```

---

### **Phase 2: FHIR Service** (Weeks 3-5)

**TDD Approach: Patient Resource CRUD**

#### Week 3: Create & Read
```java
// 1. RED: Write failing test
@Test
void createPatient_withValidData_shouldReturn201() {
    Patient patient = buildTestPatient("John Smith");

    // This will fail - endpoint doesn't exist yet
    ResponseEntity<Patient> response =
        restTemplate.postForEntity("/fhir/Patient", patient, Patient.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody().getId());
}

// 2. GREEN: Implement minimal endpoint
@PostMapping("/fhir/Patient")
public ResponseEntity<Patient> createPatient(@RequestBody Patient patient) {
    patient.setId(UUID.randomUUID().toString());
    patientRepository.save(patient);
    return ResponseEntity.status(HttpStatus.CREATED).body(patient);
}

// 3. REFACTOR: Add validation, audit logging, etc.
```

#### Week 4: Search & Update
```java
@Test
void searchPatients_byName_shouldReturnMatches() {
    // Create test patients
    createPatient("John Smith");
    createPatient("Jane Smith");
    createPatient("Bob Wilson");

    // Search
    ResponseEntity<Bundle> response =
        restTemplate.getForEntity("/fhir/Patient?name=Smith", Bundle.class);

    assertEquals(2, response.getBody().getTotal());
}
```

#### Week 5: Remaining 147 FHIR resources
- Use code generation for repetitive patterns
- Focus on most common resources: Observation, Condition, MedicationRequest

**Deliverable**: ✅ Complete FHIR R4 API with 150+ resources

---

### **Phase 3: CQL Engine Service** (Weeks 6-8)

**TDD Approach: HEDIS Blood Pressure Measure**

#### Week 6: CQL Expression Evaluation
```java
// 1. RED: Write test for simplest measure
@Test
void testHedisCBP_controlledBloodPressure_shouldPass() {
    String patientId = createPatientWithHypertension();

    // Add BP readings <140/90
    createObservation(patientId, "85354-9", "130/85", LocalDate.now().minusMonths(3));
    createObservation(patientId, "85354-9", "125/80", LocalDate.now().minusMonths(6));

    // Evaluate measure
    MeasureResult result = cqlService.evaluate("HEDIS_CBP", patientId);

    // Assert
    assertEquals(1, result.getScore());
    assertEquals("PASS", result.getResult());
}

// 2. GREEN: Implement CQL evaluation
// 3. REFACTOR: Optimize, cache, handle edge cases
```

#### Week 7-8: Implement all 52 HEDIS measures
- 1 measure = 3 tests (pass, fail, excluded)
- Total: 156 measure tests

**Deliverable**: ✅ 52 HEDIS measures, 95% STAR prediction accuracy

---

### **Phase 4: Consent Service** (Weeks 9-10)

**TDD Approach: RBAC & Field-Level Filtering**

```java
@Test
void testConsentFiltering_nurseShouldNotSeeSSN() {
    // Given: User with NURSE role
    String userId = "nurse-1";
    assignRole(userId, "NURSE");

    // And: Patient with SSN
    Patient patient = createPatient("123-45-6789");

    // When: Nurse reads patient
    ConsentEvaluation consent = consentService.evaluate(
        userId, patient.getId(), "read");

    // Then: SSN should be denied
    assertTrue(consent.isAllowed());
    assertTrue(consent.getDeniedFields().contains("ssn"));

    // And: Audit log created
    assertEquals(1, auditRepository.countByUserId(userId));
}
```

**Deliverable**: ✅ HIPAA-compliant consent with 13 roles, 31 permissions

---

### **Phase 5: Event Processing** (Weeks 11-12)

**TDD Approach: Real-Time Care Gap Detection**

```java
@Test
void testCareGapDetection_shouldDetectIn5Seconds() {
    // Given: Diabetic patient with no HbA1c test
    String patientId = createDiabeticPatient();

    // When: New observation event arrives
    long start = System.currentTimeMillis();

    ObservationEvent event = new ObservationEvent(patientId, "blood-glucose", ...);
    kafkaTemplate.send("fhir.resources.observation", event);

    // Then: Care gap detected within 5 seconds
    await().atMost(5, SECONDS).untilAsserted(() -> {
        List<CareGap> gaps = careGapRepository.findByPatientId(patientId);
        assertEquals(1, gaps.size());
        assertEquals("HEDIS_HbA1c", gaps.get(0).getMeasureId());
    });

    long duration = System.currentTimeMillis() - start;
    assertTrue(duration < 5000, "Detection took " + duration + "ms");
}
```

**Deliverable**: ✅ Real-time care gap detection (<5 seconds)

---

### **Phase 6: Remaining Services** (Weeks 13-14)
- Patient Service
- Quality Measure Service
- Care Gap Service
- Analytics Service
- Gateway Service

**Approach**: Apply same TDD pattern from Phases 2-5

---

### **Phase 7: Angular Admin Portal** (Weeks 15-18)

**TDD Approach: Component-Driven Development**

#### Week 15: Shared Components
```typescript
// 1. RED: Write failing component test
describe('ServiceCardComponent', () => {
  it('should display green indicator for healthy service', () => {
    const component = createComponent(ServiceCardComponent, {
      service: { name: 'FHIR', status: 'UP', responseTime: 45 }
    });

    expect(component.healthIndicator.color).toBe('green');
  });
});

// 2. GREEN: Implement component
@Component({
  selector: 'healthdata-service-card',
  template: `
    <div class="health-indicator" [ngClass]="healthColor"></div>
  `
})
export class ServiceCardComponent {
  @Input() service!: Service;

  get healthColor(): string {
    return this.service.status === 'UP' ? 'bg-green-500' : 'bg-red-500';
  }
}

// 3. REFACTOR: Add styling, animations
```

#### Week 16-17: Service Management UIs
- Platform Dashboard
- PostgreSQL, Redis, Kafka UIs
- FHIR, CQL, Consent UIs

#### Week 18: API Playground
- 20+ quick actions
- Request history
- Variable substitution

**Deliverable**: ✅ Complete admin portal with all screens

---

### **Phase 8: API Documentation** (Weeks 19-20)

**TDD Approach: Documentation Tests**

```typescript
@Test
void testOpenAPISpec_shouldBeValid() {
    // Read generated OpenAPI spec
    String spec = readFile("openapi/fhir-api.yaml");

    // Validate against OpenAPI 3.0 schema
    OpenAPIV3Parser parser = new OpenAPIV3Parser();
    SwaggerParseResult result = parser.readContents(spec, null, null);

    assertNull(result.getMessages(), "OpenAPI spec has errors");
    assertNotNull(result.getOpenAPI());
}

@Test
void testSwaggerUI_shouldLoadCorrectly() {
    // E2E test
    page.goto("/docs/api/fhir-api");

    expect(page.locator('text=FHIR R4 API')).toBeVisible();
    expect(page.locator('.opblock-tag')).toHaveCount(5); // 5 resource types
}
```

**Deliverable**: ✅ 5 OpenAPI specs, 6 integration examples

---

### **Phase 9: Performance Optimization** (Weeks 21-22)

**TDD Approach: Performance Test-Driven**

```java
@Test
void testFhirRead_withCache_shouldMeetSLA() {
    // Load test: 1000 requests
    List<Long> times = new ArrayList<>();

    for (int i = 0; i < 1000; i++) {
        long start = System.nanoTime();
        fhirService.readPatient("patient-123");
        times.add((System.nanoTime() - start) / 1_000_000); // ms
    }

    // Verify SLA: p95 <50ms (cache hit)
    Collections.sort(times);
    long p95 = times.get(949);
    assertTrue(p95 < 50, "p95: " + p95 + "ms");
}
```

**Deliverable**: ✅ All SLAs met (<500ms response, 67%+ cache hit)

---

### **Phase 10: Integration & E2E Testing** (Weeks 23-24)

**E2E Test: Full Patient Data Flow**

```java
@Test
void fullPatientDataFlow_shouldWorkEndToEnd() {
    // 1. Create patient via API
    Patient patient = fhirClient.createPatient("John Smith");

    // 2. Verify Kafka event
    PatientEvent event = consumeKafkaEvent("fhir.resources.patient", 5000);
    assertEquals("PATIENT_CREATED", event.getEventType());

    // 3. Read patient (verify cached)
    Patient retrieved = fhirClient.readPatient(patient.getId());
    assertEquals(patient.getId(), retrieved.getId());

    // 4. Search patient
    Bundle results = fhirClient.searchPatients("name=Smith");
    assertTrue(results.getTotal() >= 1);

    // 5. Create observation (triggers care gap detection)
    createObservation(patient.getId(), "blood-glucose", "120 mg/dL");

    // 6. Verify care gap detected
    await().atMost(5, SECONDS).until(() ->
        careGapRepository.countByPatientId(patient.getId()) > 0);
}
```

**Deliverable**: ✅ All critical paths tested end-to-end

---

### **Phase 11: Deployment** (Weeks 25-26)

- Kubernetes manifests
- Terraform infrastructure
- Production deployment
- Smoke tests
- Monitoring setup

**Deliverable**: ✅ Production-ready platform on Kubernetes

---

## 📊 Test Pyramid

```
                /\
               /  \     E2E Tests (10% - 50 tests)
              /____\    - Full user journeys
             /      \   - Cross-service workflows
            /________\  Integration Tests (30% - 500 tests)
           /          \ - API endpoints
          /____________\- Database, Kafka, Redis
         /              \ Unit Tests (60% - 1000 tests)
        /________________\- Business logic
                           - Validators
                           - Utilities
```

**Total Test Count**: ~1,550 tests across all layers

---

## 🚀 Getting Started (Day 1)

### 1. Clone & Setup

```bash
git clone <repo-url>
cd healthdata-in-motion
./scripts/setup.sh
```

### 2. Run First Test

```bash
# Backend
cd backend
./gradlew test

# Frontend
npm run test:frontend
```

### 3. Write Your First TDD Test

**File**: `backend/modules/shared/domain/common/src/test/java/com/healthdata/common/AuditMetadataTest.java`

```java
package com.healthdata.common;

import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuditMetadataTest {

    @Test
    void forCreate_shouldSetTimestamps() {
        // RED: Write test (will fail initially)
        AuditMetadata metadata = AuditMetadata.forCreate("user-1", "org-1");

        assertNotNull(metadata.getCreatedAt());
        assertNotNull(metadata.getLastModifiedAt());
        assertEquals("user-1", metadata.getCreatedBy());
        assertEquals("user-1", metadata.getLastModifiedBy());
        assertEquals("org-1", metadata.getOrganizationId());
    }

    @Test
    void markModified_shouldUpdateTimestamp() {
        // Arrange
        AuditMetadata metadata = AuditMetadata.forCreate("user-1", "org-1");
        Instant originalModified = metadata.getLastModifiedAt();

        // Wait 10ms
        try { Thread.sleep(10); } catch (Exception e) {}

        // Act
        metadata.markModified("user-2");

        // Assert
        assertTrue(metadata.getLastModifiedAt().isAfter(originalModified));
        assertEquals("user-2", metadata.getLastModifiedBy());
        assertEquals("user-1", metadata.getCreatedBy()); // Should not change
    }
}
```

### 4. Run Test (RED)

```bash
./gradlew test --tests AuditMetadataTest
# Test will FAIL (methods don't exist yet)
```

### 5. Implement Code (GREEN)

**File**: `backend/modules/shared/domain/common/src/main/java/com/healthdata/common/AuditMetadata.java`

```java
package com.healthdata.common;

import lombok.Data;
import lombok.Builder;
import java.time.Instant;

@Data
@Builder
public class AuditMetadata {

    private Instant createdAt;
    private String createdBy;
    private Instant lastModifiedAt;
    private String lastModifiedBy;
    private String organizationId;

    public static AuditMetadata forCreate(String userId, String organizationId) {
        Instant now = Instant.now();
        return AuditMetadata.builder()
                .createdAt(now)
                .createdBy(userId)
                .lastModifiedAt(now)
                .lastModifiedBy(userId)
                .organizationId(organizationId)
                .build();
    }

    public void markModified(String userId) {
        this.lastModifiedAt = Instant.now();
        this.lastModifiedBy = userId;
    }
}
```

### 6. Run Test Again (GREEN)

```bash
./gradlew test --tests AuditMetadataTest
# All tests PASS ✅
```

### 7. Refactor (if needed)

- Add JavaDoc comments
- Extract constants
- Improve naming

### 8. Commit & Push

```bash
git add .
git commit -m "feat: implement AuditMetadata with TDD"
git push
```

---

## 📈 Success Metrics

| Metric | Target | How to Measure |
|--------|--------|----------------|
| **Test Coverage** | 80%+ | Codecov, JaCoCo |
| **Build Success Rate** | 95%+ | GitHub Actions |
| **Response Time (p95)** | <500ms | Load tests (JMeter) |
| **Cache Hit Rate** | 67-98% | Redis INFO command |
| **Care Gap Detection** | <5 seconds | Integration tests |
| **HEDIS Accuracy** | 95%+ | CMS test cases |
| **Zero Critical Bugs** | 100% | Snyk, Trivy |

---

## 🎓 TDD Best Practices

### Do's ✅
- ✅ Write test FIRST, then code
- ✅ Write smallest test that fails
- ✅ Write minimal code to pass test
- ✅ Refactor after test passes
- ✅ Run tests frequently (every 5 minutes)
- ✅ Keep tests fast (<1 second each)
- ✅ One assertion per test (generally)
- ✅ Test behavior, not implementation

### Don'ts ❌
- ❌ Write code before tests
- ❌ Skip refactoring step
- ❌ Test private methods
- ❌ Copy-paste tests
- ❌ Ignore failing tests
- ❌ Write slow tests
- ❌ Test framework code (Spring, Angular)
- ❌ Over-engineer solutions

---

## 🛠️ Tools & Technologies

### Backend Testing
- **JUnit 5**: Unit testing framework
- **Mockito**: Mocking framework
- **Testcontainers**: Integration testing with Docker
- **REST Assured**: API testing
- **JMeter**: Load testing
- **JaCoCo**: Code coverage

### Frontend Testing
- **Jest**: Unit testing framework
- **Jasmine**: BDD testing
- **Playwright**: E2E testing
- **Karma**: Test runner
- **Istanbul**: Code coverage

### CI/CD
- **GitHub Actions**: CI/CD pipeline
- **Codecov**: Coverage reporting
- **Snyk**: Security scanning
- **SonarQube**: Code quality

---

## 📚 Resources

### Documentation
- [Complete TDD Implementation Plan](./TDD_IMPLEMENTATION_PLAN.md)
- [Architecture Documentation](./architecture/ARCHITECTURE.md)
- [README](../README.md)

### External Resources
- [Test-Driven Development by Example (Kent Beck)](https://www.amazon.com/Test-Driven-Development-Kent-Beck/dp/0321146530)
- [Growing Object-Oriented Software, Guided by Tests](https://www.amazon.com/Growing-Object-Oriented-Software-Guided-Tests/dp/0321503627)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [Angular Testing Guide](https://angular.io/guide/testing)

---

## 🤝 Team Collaboration

### Daily Standup (15 min)
- What did I complete yesterday?
- What will I work on today?
- Any blockers?

### Pair Programming
- 2 developers, 1 computer
- Driver writes code, navigator reviews
- Switch roles every 25 minutes
- Great for TDD learning

### Code Reviews
- All PRs require 1 approval
- Check: tests exist, tests pass, code readable
- Turnaround time: <24 hours

### Retrospectives (bi-weekly)
- What went well?
- What needs improvement?
- Action items for next sprint

---

## 🎯 Week 1 Goals (Get Started!)

### Day 1-2: Setup & Training
- [ ] Clone repository
- [ ] Run `./scripts/setup.sh`
- [ ] Attend TDD workshop
- [ ] Write first test together

### Day 3-4: Shared Domain Models
- [ ] Write tests for `AuditMetadata`
- [ ] Write tests for `FhirResourceWrapper`
- [ ] Write tests for `HedisMeasure`
- [ ] Implement classes (TDD)

### Day 5: Database Schema
- [ ] Create test for patient table
- [ ] Create migration script
- [ ] Verify schema in tests
- [ ] Sprint planning for Week 2

---

## 📞 Support

**Questions?** Create an issue in the repository or contact the tech lead.

**Bugs?** Open a GitHub issue with:
- Steps to reproduce
- Expected vs actual behavior
- Test that demonstrates the bug

---

**Ready to build? Let's go! 🚀**

---

**Document Version**: 1.0
**Created**: October 28, 2025
**Status**: Ready for Team
**Next Review**: End of Week 1
