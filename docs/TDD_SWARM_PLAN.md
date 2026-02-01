# TDD Swarm Development Plan
**Health Data In Motion - Phase 2 Completion**

**Date**: October 30, 2025
**Methodology**: Test-Driven Development (TDD) Swarm
**Target**: Concurrent team development for remaining Phase 2 requirements

---

## 📋 Executive Summary

This plan enables **3-5 concurrent development teams** to work independently using **TDD Swarm** methodology with **git worktrees** to complete the remaining Phase 2 requirements. Each team will follow a strict TDD cycle, implement their feature in an isolated worktree, and merge upon validation.

**Current Status**: 6 FHIR resources complete, Consent Service foundation complete
**Remaining Work**: 4 FHIR resources + 4 microservices + integration/testing
**Estimated Teams**: 3-5 teams working in parallel
**Estimated Timeline**: 2-3 weeks with concurrent development

---

## 🔄 TDD Swarm Methodology

### Core Principles

1. **Red-Green-Refactor Cycle**:
   - **Red**: Write failing test first
   - **Green**: Write minimum code to pass test
   - **Refactor**: Improve code while keeping tests passing

2. **Worktree Isolation**:
   - Each feature implemented in dedicated git worktree
   - No interference between concurrent teams
   - Clean merge strategy upon completion

3. **Definition of Done**:
   - All tests passing (unit + integration)
   - Code coverage ≥80%
   - Build successful
   - Documentation updated
   - PR reviewed and approved

### Workflow

```bash
# 1. Create worktree for feature
git worktree add ../feature-allergy-intolerance feature/allergy-intolerance

# 2. Implement with TDD
cd ../feature-allergy-intolerance
# Write test → Fail → Implement → Pass → Refactor

# 3. Verify
./gradlew test
./gradlew build

# 4. Merge back
git checkout master
git merge feature/allergy-intolerance
git worktree remove ../feature-allergy-intolerance
```

---

## 🎯 Team Assignments

### Team 1: AllergyIntolerance FHIR Resource
**Priority**: HIGH (Patient Safety Critical)
**Complexity**: Medium
**Estimated Time**: 3-4 days
**Dependencies**: None

**Feature Branch**: `feature/allergy-intolerance`
**Worktree Path**: `../feature-allergy-intolerance`

#### Requirements
- Track patient allergies and intolerances
- Support allergen codes (RxNorm, SNOMED CT)
- Clinical status (active, inactive, resolved)
- Verification status (unconfirmed, confirmed, refuted)
- Reaction details (manifestation, severity, onset)
- Criticality (low, high, unable-to-assess)

#### Data Model
```java
@Entity
@Table(name = "allergy_intolerances")
class AllergyIntoleranceEntity {
    UUID id;
    UUID patientId;
    String allergenCode;         // RxNorm, SNOMED CT
    String allergenSystem;
    String allergenDisplay;
    String clinicalStatus;       // active, inactive, resolved
    String verificationStatus;   // unconfirmed, confirmed, refuted
    String type;                 // allergy, intolerance
    String category;             // food, medication, environment, biologic
    String criticality;          // low, high, unable-to-assess
    LocalDateTime onsetDateTime;
    LocalDateTime recordedDate;
    String recorderId;
    String note;
    // Reaction details
    String reactionManifestation;
    String reactionSeverity;     // mild, moderate, severe
    LocalDateTime reactionOnset;
}
```

#### TDD Test Cases
1. **Unit Tests** (15-20 tests):
   ```java
   // Repository
   - testFindByPatientId()
   - testFindActiveAllergies()
   - testFindByAllergenCode()
   - testFindByCriticality()
   - testHasActiveAllergy()

   // Service
   - testCreateAllergyIntolerance()
   - testUpdateVerificationStatus()
   - testResolveAllergy()
   - testExtractAllergenCode()
   - testExtractReactionDetails()

   // Controller
   - testPostAllergyReturns201()
   - testGetAllergyReturns200()
   - testSearchByPatientReturnsBundle()
   - testCheckCriticalAllergyReturnsBoolean()
   ```

2. **Integration Tests** (5-8 tests):
   - Full CRUD operations with database
   - Search queries with pagination
   - FHIR Bundle generation
   - Cache behavior verification

#### Indexes Required
- `idx_allergy_tenant_patient`
- `idx_allergy_status`
- `idx_allergy_criticality`
- `idx_allergy_allergen_code`
- `idx_allergy_category`

#### REST Endpoints
```
POST   /fhir/AllergyIntolerance
GET    /fhir/AllergyIntolerance/{id}
PUT    /fhir/AllergyIntolerance/{id}
DELETE /fhir/AllergyIntolerance/{id}
GET    /fhir/AllergyIntolerance?patient={id}
GET    /fhir/AllergyIntolerance/active?patient={id}
GET    /fhir/AllergyIntolerance/critical?patient={id}
GET    /fhir/AllergyIntolerance/has-allergy?patient={id}&code={code}
```

---

### Team 2: Immunization FHIR Resource
**Priority**: HIGH (Quality Measures)
**Complexity**: Medium
**Estimated Time**: 3-4 days
**Dependencies**: None

**Feature Branch**: `feature/immunization`
**Worktree Path**: `../feature-immunization`

#### Requirements
- Track vaccination history
- Support CVX codes for vaccines
- Status (completed, entered-in-error, not-done)
- Dose number and series tracking
- Lot number and expiration date
- Site and route of administration
- Reaction tracking

#### Data Model
```java
@Entity
@Table(name = "immunizations")
class ImmunizationEntity {
    UUID id;
    UUID patientId;
    String vaccineCode;          // CVX codes
    String vaccineSystem;
    String vaccineDisplay;
    String status;               // completed, entered-in-error, not-done
    LocalDate occurrenceDate;
    Boolean primarySource;
    String lotNumber;
    LocalDate expirationDate;
    String site;                 // Body site
    String route;                // Route of administration
    Integer doseQuantity;
    String doseUnit;
    String performerId;
    String locationId;
    String note;
    // Reaction
    Boolean hadReaction;
    String reactionDetail;
    LocalDateTime reactionDate;
}
```

#### TDD Test Cases
1. **Unit Tests** (15-20 tests):
   ```java
   // Repository
   - testFindByPatientId()
   - testFindByVaccineCode()
   - testFindCompletedImmunizations()
   - testFindByDateRange()
   - testHasImmunization()

   // Service
   - testCreateImmunization()
   - testUpdateStatus()
   - testRecordReaction()
   - testExtractVaccineCode()
   - testCalculateNextDueDate()

   // Controller
   - testPostImmunizationReturns201()
   - testSearchByVaccineCodeReturnsBundle()
   - testCheckImmunizationStatusReturnsBoolean()
   ```

2. **Integration Tests** (5-8 tests):
   - Vaccination series tracking
   - Date range queries
   - Reaction recording and retrieval

#### Indexes Required
- `idx_immunization_tenant_patient`
- `idx_immunization_vaccine_code`
- `idx_immunization_status`
- `idx_immunization_date_range`
- `idx_immunization_performer`

#### REST Endpoints
```
POST   /fhir/Immunization
GET    /fhir/Immunization/{id}
PUT    /fhir/Immunization/{id}
DELETE /fhir/Immunization/{id}
GET    /fhir/Immunization?patient={id}
GET    /fhir/Immunization?patient={id}&vaccine={code}
GET    /fhir/Immunization/completed?patient={id}
GET    /fhir/Immunization/has-immunization?patient={id}&vaccine={code}
```

---

### Team 3: Patient Service (Aggregation Layer)
**Priority**: HIGH (Core Service)
**Complexity**: High
**Estimated Time**: 5-7 days
**Dependencies**: All FHIR resources complete

**Feature Branch**: `feature/patient-service`
**Worktree Path**: `../feature-patient-service`

#### Requirements
- Aggregate data from all FHIR resources
- Create patient summary view
- Support timeline queries
- Health status dashboard data
- Quality measure gaps aggregation
- Consent-aware data access

#### API Endpoints
```java
// Patient Summary
GET /api/patients/{id}/summary
Response: {
  patient: {...},
  activeConditions: [...],
  activeMedications: [...],
  recentEncounters: [...],
  criticalAllergies: [...],
  immunizations: [...],
  upcomingProcedures: [...]
}

// Patient Timeline
GET /api/patients/{id}/timeline?start={date}&end={date}
Response: [{
  date: "2025-01-15",
  type: "encounter",
  description: "Annual Physical",
  resource: {...}
}, ...]

// Health Status
GET /api/patients/{id}/health-status
Response: {
  chronicConditions: 3,
  activeMedications: 5,
  criticalAllergies: 1,
  immunizationCompliance: 0.85,
  lastVisit: "2025-09-15",
  openCareGaps: 2
}

// Quality Measure Data
GET /api/patients/{id}/quality-measures
```

#### TDD Test Cases
1. **Unit Tests** (20-25 tests):
   ```java
   - testGetPatientSummary()
   - testGetTimeline()
   - testGetHealthStatus()
   - testAggregateConditions()
   - testAggregateM edications()
   - testConsentAwareFiltering()
   - testCacheBehavior()
   ```

2. **Integration Tests** (10-15 tests):
   - End-to-end summary generation
   - Timeline with multiple resource types
   - Consent validation integration
   - Cache invalidation on updates

#### Dependencies
- FHIR Service (all resources)
- Consent Service
- Messaging infrastructure

---

### Team 4: Care Gap Service
**Priority**: MEDIUM (Quality Measures)
**Complexity**: High
**Estimated Time**: 6-8 days
**Dependencies**: Patient Service, CQL Engine Service

**Feature Branch**: `feature/care-gap-service`
**Worktree Path**: `../feature-care-gap-service`

#### Requirements
- Identify quality measure gaps (HEDIS)
- Calculate gap closure opportunities
- Prioritize gaps by impact
- Support multiple measures simultaneously
- Track gap closure over time
- Generate patient outreach lists

#### Data Model
```java
@Entity
@Table(name = "care_gaps")
class CareGapEntity {
    UUID id;
    String tenantId;
    UUID patientId;
    String measureId;            // HEDIS measure
    String measureName;
    String gapType;              // missing-screening, overdue-visit, etc
    String status;               // open, closed, pending
    LocalDate identifiedDate;
    LocalDate dueDate;
    String priority;             // low, medium, high, critical
    String recommendation;
    Double impactScore;
    LocalDate closedDate;
    String closureMethod;
}
```

#### API Endpoints
```java
// Care Gaps
GET /api/care-gaps/patient/{id}
GET /api/care-gaps/tenant?measure={id}
GET /api/care-gaps/open?priority=high
POST /api/care-gaps/{id}/close
GET /api/care-gaps/outreach-list?measure={id}&priority={priority}

// Analytics
GET /api/care-gaps/analytics/tenant
Response: {
  totalGaps: 450,
  openGaps: 320,
  closedThisMonth: 130,
  byMeasure: {...},
  byPriority: {...}
}
```

#### TDD Test Cases
1. **Unit Tests** (25-30 tests):
   ```java
   - testIdentifyCareGap()
   - testCalculateImpactScore()
   - testPrioritizeGaps()
   - testCloseCareGap()
   - testGenerateOutreachList()
   - testCQLIntegration()
   ```

2. **Integration Tests** (10-12 tests):
   - End-to-end gap identification
   - Multi-measure analysis
   - Gap closure workflow
   - Analytics aggregation

---

### Team 5: Quality Measure Service
**Priority**: MEDIUM (Analytics)
**Complexity**: Very High
**Estimated Time**: 8-10 days
**Dependencies**: CQL Engine, Patient Service, Care Gap Service

**Feature Branch**: `feature/quality-measure-service`
**Worktree Path**: `../feature-quality-measure-service`

#### Requirements
- Implement HEDIS quality measures
- Execute CQL libraries for measure calculation
- Support measure stratification
- Calculate numerator/denominator
- Generate measure reports
- Track measure performance over time

#### Data Model
```java
@Entity
@Table(name = "quality_measure_results")
class QualityMeasureResultEntity {
    UUID id;
    String tenantId;
    UUID patientId;
    String measureId;
    String measureName;
    String measureYear;
    Boolean inDenominator;
    Boolean inNumerator;
    Boolean exclusion;
    String exclusionReason;
    LocalDate calculationDate;
    String cqlLibraryVersion;
    String result;               // JSON with detailed results
}
```

#### API Endpoints
```java
// Measure Calculation
POST /api/quality-measures/calculate
Body: {
  measureId: "CDC-HbA1c",
  patientIds: [...],
  measurementPeriod: {start: "2025-01-01", end: "2025-12-31"}
}

// Measure Reports
GET /api/quality-measures/report/{measureId}
GET /api/quality-measures/patient/{id}/results
GET /api/quality-measures/performance?measure={id}&period={year}

// Supported Measures
GET /api/quality-measures/catalog
```

#### TDD Test Cases
1. **Unit Tests** (30-35 tests):
   ```java
   - testCalculateMeasure()
   - testDetermineDenominator()
   - testDetermineNumerator()
   - testApplyExclusions()
   - testCQLExecution()
   - testMeasureStratification()
   ```

2. **Integration Tests** (15-20 tests):
   - End-to-end measure calculation
   - Multiple patient batch processing
   - CQL library integration
   - Report generation

---

## 🏗️ Infrastructure Setup

### Git Worktree Structure

```
healthdata-in-motion/
├── backend/                          # Main repository
├── feature-allergy-intolerance/      # Team 1 worktree
├── feature-immunization/             # Team 2 worktree
├── feature-patient-service/          # Team 3 worktree
├── feature-care-gap-service/         # Team 4 worktree
└── feature-quality-measure-service/  # Team 5 worktree
```

### Branch Naming Convention
```
feature/<feature-name>        # Development branch
test/<feature-name>           # Test-only branch (if needed)
fix/<feature-name>-<issue>    # Bug fixes
```

### Commit Message Format
```
[TDD] <scope>: <description>

Examples:
[TDD] AllergyIntolerance: Add repository tests
[TDD] AllergyIntolerance: Implement entity (Red)
[TDD] AllergyIntolerance: Pass entity tests (Green)
[TDD] AllergyIntolerance: Refactor entity validation
```

---

## ✅ Definition of Done

### Code Quality
- [ ] All tests passing (unit + integration)
- [ ] Code coverage ≥80%
- [ ] No compiler warnings
- [ ] No SonarQube critical issues
- [ ] Build successful: `./gradlew build`

### Testing
- [ ] Unit tests for all public methods
- [ ] Integration tests for API endpoints
- [ ] Database migration tested
- [ ] Cache behavior verified
- [ ] Error scenarios covered

### Documentation
- [ ] README updated with new endpoints
- [ ] OpenAPI/Swagger documentation
- [ ] Database schema documented
- [ ] Code comments for complex logic

### Review
- [ ] PR created with detailed description
- [ ] At least 2 team members reviewed
- [ ] All review comments addressed
- [ ] CI/CD pipeline passing

---

## 📊 Progress Tracking

### Team Status Board (Update Daily)

| Team | Feature | Status | Tests | Coverage | Blockers |
|------|---------|--------|-------|----------|----------|
| 1 | AllergyIntolerance | 🟡 In Progress | 12/20 | 65% | None |
| 2 | Immunization | 🔴 Not Started | 0/20 | 0% | - |
| 3 | Patient Service | 🔴 Not Started | 0/25 | 0% | Needs FHIR resources |
| 4 | Care Gap Service | 🔴 Not Started | 0/30 | 0% | Needs Patient Service |
| 5 | Quality Measure | 🔴 Not Started | 0/35 | 0% | Needs CQL + Care Gap |

**Legend**: 🟢 Complete | 🟡 In Progress | 🔴 Not Started | 🟠 Blocked

---

## 🔄 Daily Standup Template

```markdown
## Team [X] - [Feature Name] - [Date]

### Yesterday
- Completed: [What was done]
- Tests passing: X/Y

### Today
- Plan: [What will be done]
- Expected tests: +Z

### Blockers
- [Any issues or dependencies]

### Needs Review
- [PRs or code needing feedback]
```

---

## 🚀 Merge Strategy

### Pre-Merge Checklist
1. All tests passing locally
2. Code coverage ≥80%
3. Build successful
4. Integration tests passing
5. Documentation updated
6. PR approved by ≥2 reviewers
7. License compliance verified (docs/compliance/THIRD_PARTY_NOTICES.md)

### Merge Process
```bash
# 1. Update feature branch from master
git checkout feature/allergy-intolerance
git fetch origin
git rebase origin/master

# 2. Run full test suite
./gradlew clean test integrationTest

# 3. Build
./gradlew build

# 4. Merge to master
git checkout master
git merge --no-ff feature/allergy-intolerance

# 5. Push
git push origin master

# 6. Clean up worktree
git worktree remove ../feature-allergy-intolerance
git branch -d feature/allergy-intolerance
```

---

## 📝 TDD Examples

### Example: AllergyIntolerance Repository Test

```java
@DataJpaTest
class AllergyIntoleranceRepositoryTest {

    @Autowired
    private AllergyIntoleranceRepository repository;

    @Test
    void testFindActiveAllergiesByPatient_returnsOnlyActiveAllergies() {
        // Arrange (Red Phase)
        UUID patientId = UUID.randomUUID();
        AllergyIntoleranceEntity active = createAllergy(patientId, "active");
        AllergyIntoleranceEntity resolved = createAllergy(patientId, "resolved");
        repository.saveAll(List.of(active, resolved));

        // Act (Green Phase)
        List<AllergyIntoleranceEntity> result =
            repository.findActiveAllergiesByPatient("tenant1", patientId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClinicalStatus()).isEqualTo("active");
    }

    @Test
    void testHasCriticalAllergy_returnsTrueWhenCriticalAllergyExists() {
        // Arrange
        UUID patientId = UUID.randomUUID();
        String allergenCode = "RxNorm-123";
        AllergyIntoleranceEntity critical = createCriticalAllergy(patientId, allergenCode);
        repository.save(critical);

        // Act
        boolean result = repository.hasCriticalAllergy("tenant1", patientId, allergenCode);

        // Assert
        assertThat(result).isTrue();
    }
}
```

### Example: Service Test with TDD

```java
@ExtendWith(MockitoExtension.class)
class AllergyIntoleranceServiceTest {

    @Mock
    private AllergyIntoleranceRepository repository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private AllergyIntoleranceService service;

    @Test
    void testCreateAllergyIntolerance_publishesKafkaEvent() {
        // Arrange (Red Phase - Write test first!)
        AllergyIntolerance fhirAllergy = createFhirAllergy();
        AllergyIntoleranceEntity entity = createEntity();
        when(repository.save(any())).thenReturn(entity);

        // Act (Green Phase - Implement to pass)
        AllergyIntolerance result = service.createAllergyIntolerance(
            "tenant1", fhirAllergy, "user1");

        // Assert
        verify(kafkaTemplate).send(
            eq("fhir.allergies.created"),
            anyString(),
            anyString()
        );
        assertThat(result.getId()).isNotNull();
    }

    @Test
    void testExtractAllergenCode_handlesMultipleCoding() {
        // Refactor Phase - Improve implementation
        AllergyIntolerance fhir = new AllergyIntolerance();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding("http://www.nlm.nih.gov/research/umls/rxnorm", "123", "Penicillin"));
        fhir.setCode(code);

        AllergyIntoleranceEntity entity = new AllergyIntoleranceEntity();
        service.extractAllergenCode(entity, fhir);

        assertThat(entity.getAllergenCode()).isEqualTo("123");
        assertThat(entity.getAllergenSystem()).contains("rxnorm");
    }
}
```

---

## 🎓 TDD Best Practices

1. **Write Test First**: Always write the test before implementation
2. **Small Steps**: One test at a time, one feature at a time
3. **Keep Tests Fast**: Unit tests should run in milliseconds
4. **Test One Thing**: Each test should verify one behavior
5. **Readable Tests**: Use descriptive names and AAA pattern (Arrange, Act, Assert)
6. **Independent Tests**: Tests should not depend on each other
7. **Test Edge Cases**: Cover happy path, error cases, and edge cases
8. **Refactor Safely**: Refactor only when tests are green

---

## 📞 Communication

### Daily Sync
- Time: 9:00 AM daily
- Duration: 15 minutes
- Format: Team status board + blockers

### Code Reviews
- Response time: <4 hours
- Required reviewers: 2
- Focus: Test quality + code clarity

### Slack Channels
- `#tdd-swarm-general`: General coordination
- `#tdd-swarm-team1-allergy`: Team 1 specific
- `#tdd-swarm-team2-immunization`: Team 2 specific
- `#tdd-swarm-team3-patient-svc`: Team 3 specific
- `#tdd-swarm-team4-care-gap`: Team 4 specific
- `#tdd-swarm-team5-quality-measure`: Team 5 specific

---

## 🏁 Success Criteria

### Phase 2 Complete When:
- [ ] All 10 FHIR resources implemented (6 done, 4 remaining)
- [ ] Patient Service operational
- [ ] Care Gap Service operational
- [ ] Quality Measure Service operational
- [ ] All services have ≥80% test coverage
- [ ] Integration tests passing for all services
- [ ] Documentation complete
- [ ] Performance benchmarks met
- [ ] Security audit passed

---

**Generated**: October 30, 2025
**Status**: Ready for Team Assignment
**Next Action**: Assign teams and create worktrees
