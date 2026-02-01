# TDD Swarm v1.3.0 - Comprehensive Implementation Plan

**Created**: 2026-01-21
**Status**: ACTIVE
**Methodology**: TDD Swarm (RED → GREEN → REFACTOR cycles)
**Duration**: 5-7 days
**Teams**: 6 parallel workstreams

---

## Executive Summary

Using TDD Swarm methodology with specialized agents to systematically fix all issues before v1.3.0 release.

**Test Suite Results** (20m 36s execution):
- **Status**: BUILD FAILED (39 task failures)
- **Compilation Errors**: 3 services (patient ✅, fhir 🔄, hcc ⏸️)
- **Test Failures**: Multiple services (detailed analysis pending)

---

## TDD Swarm Team Structure

### Team 1: Test Compilation Fixes (CRITICAL)
**Agent**: Code-reviewer + Testing specialist
**Duration**: 2 days
**Status**: IN PROGRESS

#### Phase: RED (Identify Failures)
✅ patient-service: 3 errors identified
🔄 fhir-service: 28 errors identified
⏸️ hcc-service: 40 errors pending analysis

#### Phase: GREEN (Fix Compilation)
**Day 1**: fhir-service fixes
- ✅ Fix Coding→CodeableConcept type conversions (2 errors)
- ✅ Fix ConditionService constructor (2 errors)
- 🔄 Fix KafkaTemplate<String,String> → <String,Object> (12 errors)
- ⏸️ Fix ProcedureService constructor (12 errors)
- ⏸️ Fix ObservationServiceTest constructor (2 errors)
- ⏸️ Fix PatientServiceTest constructor (2 errors)

**Day 2**: hcc-service fixes
- ⏸️ Analyze 40 symbol resolution errors
- ⏸️ Fix HccRiskAdjustmentE2ETest

#### Phase: REFACTOR
- Run compilation validation
- Document architectural mismatches
- Update test patterns documentation

---

### Team 2: Test Failure Analysis (HIGH)
**Agent**: Testing specialist + Architecture analyst
**Duration**: 1 day
**Status**: PENDING

#### Phase: RED
- Parse /tmp/test-output.log
- Categorize failures by type:
  - Testcontainers connectivity
  - Configuration issues
  - Database constraints
  - Timing/race conditions
- Generate failure matrix

#### Phase: GREEN
- Fix critical failures (blocking services)
- Document environment-specific failures
- Create CI/CD-specific test profiles

#### Phase: REFACTOR
- Optimize test execution
- Add retry logic for flaky tests
- Update test documentation

---

### Team 3: CareGapClosureEventConsumer (HIGH)
**Agent**: Kafka specialist + CQRS expert
**Duration**: 2-3 days
**Status**: PENDING

#### Phase: RED (Write Tests First)
**File**: `CareGapClosureEventConsumerTest.java`
```java
@DisplayName("Care Gap Closure Event Consumer Tests")
class CareGapClosureEventConsumerTest {

    @Test
    void shouldConsumeObservationCreatedEvent() {
        // RED: Write failing test
    }

    @Test
    void shouldConsumeProcedureCreatedEvent() {
        // RED: Write failing test
    }

    @Test
    void shouldAutoCloseCareGapWhenCriteriaMet() {
        // RED: Write failing test
    }
}
```

#### Phase: GREEN (Implement Consumer)
**File**: `CareGapClosureEventConsumer.java`
```java
@Service
@RequiredArgsConstructor
public class CareGapClosureEventConsumer {

    private final CareGapService careGapService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "fhir.observations.created")
    public void handleObservationCreated(String event) {
        // Implement logic
    }

    @KafkaListener(topics = "fhir.procedures.created")
    public void handleProcedureCreated(String event) {
        // Implement logic
    }
}
```

#### Phase: REFACTOR
- Re-enable CareGapDetectionE2ETest
- Integration testing with Kafka
- Performance optimization

---

### Team 4: HIPAA Cache-Control Headers (HIGH)
**Agent**: Security specialist + Code-reviewer
**Duration**: 2 days
**Status**: PENDING

#### Phase: RED (Identify Missing Headers)
**Script**: `scripts/hipaa-validation/find-missing-cache-control.sh`
```bash
#!/bin/bash
# Find all controllers missing Cache-Control headers
find modules/services -name "*Controller.java" | \
  xargs grep -L "Cache-Control" | \
  xargs grep "@GetMapping\|@PostMapping"
```

**Expected**: 54 controllers

#### Phase: GREEN (Add Headers)
**Pattern**:
```java
@GetMapping("/{id}")
public ResponseEntity<PatientResponse> getPatient(@PathVariable String id) {
    PatientResponse response = service.getPatient(id);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header("Pragma", "no-cache")
        .body(response);
}
```

**Automation**:
```bash
# Bulk apply pattern to all identified controllers
./scripts/hipaa-validation/add-cache-control-headers.sh
```

#### Phase: REFACTOR
- Run HIPAA compliance validation script
- Document exceptions (if any)
- Update security guidelines

---

### Team 5: HIPAA Audit Annotations (HIGH)
**Agent**: Security specialist + Audit expert
**Duration**: 2 days
**Status**: PENDING

#### Phase: RED (Identify Missing Annotations)
**Script**: `scripts/hipaa-validation/find-missing-audited.sh`
```bash
#!/bin/bash
# Find all PHI access methods missing @Audited
find modules/services -name "*Service.java" | \
  xargs grep -L "@Audited" | \
  xargs grep "Patient\|Condition\|Observation\|MedicationRequest"
```

**Expected**: 59 services

#### Phase: GREEN (Add Annotations)
**Pattern**:
```java
@Service
@RequiredArgsConstructor
public class PatientService {

    @Audited(eventType = "PATIENT_ACCESS", resourceType = "Patient")
    public PatientResponse getPatient(String patientId, String tenantId) {
        return patientRepository.findByIdAndTenant(patientId, tenantId)
            .map(this::mapToResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
    }
}
```

**Automation**:
```bash
# Bulk apply pattern
./scripts/hipaa-validation/add-audited-annotations.sh
```

#### Phase: REFACTOR
- Run audit logging validation
- Verify audit events in database
- Update audit documentation

---

### Team 6: Cache TTL Configuration (MEDIUM)
**Agent**: Configuration specialist + Redis expert
**Duration**: 1 day
**Status**: PENDING

#### Phase: RED (Identify Missing Config)
**Services**: ai-assistant-service, ecr-service

**Test**:
```java
@Test
void shouldConfigureCacheTTLUnder5Minutes() {
    CacheManager cacheManager = context.getBean(CacheManager.class);
    Cache cache = cacheManager.getCache("phi-data");

    // Verify TTL <= 300 seconds
    assertThat(cache.getNativeCache().getTimeToLive()).isLessThanOrEqualTo(300);
}
```

#### Phase: GREEN (Add Configuration)
**File**: `application.yml`
```yaml
spring:
  cache:
    redis:
      time-to-live: 300000  # 5 minutes in ms
      cache-null-values: false
    cache-names:
      - phi-data
      - patient-data
      - clinical-data
```

#### Phase: REFACTOR
- Run HIPAA compliance validation
- Document cache strategy
- Update monitoring dashboards

---

## Integration & Validation Phase

### Day 5-6: Integration Testing
**Agent**: Integration specialist + QA lead

1. **Run Full Test Suite**
   ```bash
   cd backend
   ./gradlew clean test --continue
   ```
   **Target**: ≥95% pass rate (1,500+/1,577 tests)

2. **HIPAA Compliance Validation**
   ```bash
   ./scripts/release-validation/validate-hipaa-compliance.sh
   ```
   **Target**: 100% compliance

3. **Entity-Migration Sync**
   ```bash
   ./scripts/release-validation/test-entity-migration-sync.sh
   ```
   **Target**: All services synchronized

4. **Docker Image Security**
   ```bash
   ./scripts/release-validation/build-and-validate-images.sh
   ```
   **Target**: All images build successfully

5. **Health Checks**
   ```bash
   ./scripts/release-validation/validate-health-checks.sh
   ```
   **Target**: All services healthy

---

### Day 7: Release Preparation

1. **Generate Release Documentation**
   ```bash
   ./scripts/release-validation/generate-release-docs.sh v1.3.0
   ```

2. **Create Git Tag**
   ```bash
   git tag -a v1.3.0 -m "Release v1.3.0: CQRS + Test Stabilization + HIPAA Compliance"
   git push origin v1.3.0
   ```

3. **Production Deployment**
   - Follow `docs/releases/v1.3.0/PRODUCTION_DEPLOYMENT_CHECKLIST_v1.3.0.md`
   - Monitor health metrics
   - Validate CQRS event flow
   - Confirm HIPAA compliance

---

## Team Coordination

### Daily Standup (9:00 AM)
- Progress updates from each team
- Blocker identification
- Cross-team dependencies
- Risk assessment

### Integration Checkpoints
- **Day 2 EOD**: Team 1 (compilation) complete
- **Day 3 EOD**: Team 2 (test failures) complete
- **Day 4 EOD**: Teams 3-6 (features/compliance) complete
- **Day 5-6**: Integration testing
- **Day 7**: Release

### Success Criteria
- ✅ 100% test compilation success
- ✅ ≥95% test pass rate (1,500+/1,577 tests)
- ✅ 100% HIPAA compliance (cache, audit, TTL)
- ✅ All release validation scripts pass
- ✅ CareGapClosureEventConsumer implemented and tested
- ✅ All services healthy in Docker Compose

---

## Risk Mitigation

### High Risk
| Risk | Mitigation | Owner |
|------|------------|-------|
| Test compilation takes longer than 2 days | Focus on critical services first, defer hcc-service if needed | Team 1 |
| CareGapClosureEventConsumer blocks release | Document as "coming in v1.3.1", keep tests disabled | Team 3 |
| HIPAA bulk updates introduce regressions | Test each service after updates, rollback if needed | Teams 4-5 |

### Medium Risk
| Risk | Mitigation | Owner |
|------|------------|-------|
| Test failures exceed 5% threshold | Analyze failures, separate environment-specific vs. code defects | Team 2 |
| Integration testing reveals new issues | Add buffer day (Day 6.5) for unexpected fixes | QA Lead |

---

## Specialized Agent Assignments

### Code-Reviewer Agent
- **Tasks**: Test compilation fixes, code quality validation
- **Teams**: Team 1, Team 4, Team 5
- **Tools**: Static analysis, pattern matching, refactoring

### Testing Specialist Agent
- **Tasks**: Test failure analysis, test optimization
- **Teams**: Team 1, Team 2
- **Tools**: Test runners, coverage analysis, flaky test detection

### Security Specialist Agent
- **Tasks**: HIPAA compliance, audit validation
- **Teams**: Team 4, Team 5
- **Tools**: Security scanners, compliance checkers

### Kafka/CQRS Specialist Agent
- **Tasks**: Event consumer implementation
- **Teams**: Team 3
- **Tools**: Kafka testing, event sourcing patterns

### Configuration Specialist Agent
- **Tasks**: Cache configuration, Redis setup
- **Teams**: Team 6
- **Tools**: Configuration validation, Redis CLI

### Integration Specialist Agent
- **Tasks**: End-to-end validation, deployment preparation
- **Teams**: Integration Phase (Day 5-7)
- **Tools**: Docker, health checks, monitoring

---

## Documentation Updates

### Required Documentation
1. **CHANGELOG.md** - v1.3.0 changes
2. **docs/releases/v1.3.0/RELEASE_NOTES_v1.3.0.md** - Final updates
3. **docs/releases/v1.3.0/KNOWN_ISSUES_v1.3.0.md** - Any deferred items
4. **backend/docs/TESTING_GUIDE.md** - Test pattern updates
5. **backend/HIPAA-CACHE-COMPLIANCE.md** - Compliance verification

---

## Metrics & Reporting

### Daily Metrics
- Test compilation status (errors remaining)
- Test pass rate (%)
- HIPAA compliance score (%)
- Team velocity (tasks completed)

### Final Report
- Total test count: 1,577 expected
- Pass rate: ≥95% target
- HIPAA compliance: 100% target
- Build time: <25 minutes target
- Deployment readiness: Yes/No

---

## Next Steps - Immediate Actions

1. **Start Team 1 Work**: Continue fhir-service compilation fixes
2. **Prepare Team 2**: Parse test output log for failure analysis
3. **Design Team 3 Tests**: Write RED tests for CareGapClosureEventConsumer
4. **Script Team 4/5 Work**: Create automation scripts for HIPAA compliance
5. **Configure Team 6**: Prepare cache TTL configurations

---

**Status**: Plan approved, execution begins immediately.
**Expected Completion**: 2026-01-28 (7 days from start)
**Release Target**: v1.3.0 production deployment by 2026-01-29
