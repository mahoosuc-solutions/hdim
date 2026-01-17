# Nurse Dashboard TDD Implementation Guide

## Overview

This guide documents the **Test-Driven Development (TDD)** approach used to implement the Nurse Dashboard feature for HDIM. It serves as both a **completed example** (OutreachLog) and a **template** for implementing the remaining services.

---

## TDD Pattern Applied to Nurse Workflow Services

### The 3-Step TDD Cycle for Each Service

```
┌─────────────────────────────────────────────────────┐
│ 1. WRITE TEST (Red)                                 │
│    - Define expected behavior                        │
│    - Write assertions before implementation          │
│    - Tests fail initially                            │
├─────────────────────────────────────────────────────┤
│ 2. WRITE CODE (Green)                               │
│    - Implement just enough to pass tests            │
│    - Focus on behavior, not perfect code            │
│    - Tests pass                                      │
├─────────────────────────────────────────────────────┤
│ 3. REFACTOR (Blue)                                  │
│    - Improve code quality                           │
│    - Extract common patterns                        │
│    - Maintain test coverage                         │
└─────────────────────────────────────────────────────┘
```

---

## Complete Example: OutreachLogService Implementation

### Step 1: Write Test First (OutreachLogServiceTest.java)

```java
@Test
@DisplayName("should create outreach log with valid data")
void testCreateOutreachLog_Success() {
    // Given - Setup test data
    when(outreachLogRepository.save(any(OutreachLogEntity.class)))
        .thenReturn(testOutreachLog);

    // When - Execute the method being tested
    OutreachLogEntity result = outreachLogService.createOutreachLog(testOutreachLog);

    // Then - Assert expected behavior
    assertThat(result)
        .isNotNull()
        .hasFieldOrPropertyWithValue("tenantId", tenantId)
        .hasFieldOrPropertyWithValue("patientId", patientId)
        .hasFieldOrPropertyWithValue("outcomeType", OutreachLogEntity.OutcomeType.SUCCESSFUL_CONTACT);

    verify(outreachLogRepository, times(1)).save(any(OutreachLogEntity.class));
}
```

**Key Testing Patterns:**
- **@ExtendWith(MockitoExtension.class)** - Unit test isolation
- **@Mock repository** - Dependencies injected
- **@DisplayName** - Clear test descriptions
- **Given/When/Then** - Test structure clarity
- **assertThat()** - Fluent assertions
- **verify()** - Interaction verification

### Step 2: Implement Service

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutreachLogService {

    private final OutreachLogRepository outreachLogRepository;

    @Transactional
    public OutreachLogEntity createOutreachLog(OutreachLogEntity outreachLog) {
        log.debug("Creating outreach log for patient {} by nurse {}",
            outreachLog.getPatientId(), outreachLog.getNurseId());

        if (outreachLog.getId() == null) {
            outreachLog.setId(UUID.randomUUID());
        }

        OutreachLogEntity saved = outreachLogRepository.save(outreachLog);

        log.info("Outreach log created: {} for patient {} with outcome: {}",
            saved.getId(), saved.getPatientId(), saved.getOutcomeType());

        return saved;
    }
}
```

**Implementation Notes:**
- Minimal implementation to pass tests
- Clear logging for debugging and audit trails
- Auto-generates UUID if not set
- HIPAA-compliant audit information

### Step 3: Write REST Tests (OutreachLogControllerTest.java)

```java
@Test
@DisplayName("POST /nurse-workflow/outreach-logs - should create outreach log")
void testCreateOutreachLog_Success() throws Exception {
    // Given
    when(outreachLogService.createOutreachLog(any(OutreachLogEntity.class)))
        .thenReturn(testOutreachLog);

    String requestBody = """
        {
            "patientId": "%s",
            "nurseId": "%s",
            "outcomeType": "SUCCESSFUL_CONTACT",
            "contactMethod": "PHONE",
            "reason": "post-discharge",
            "notes": "Patient doing well, no concerns"
        }
        """.formatted(patientId, nurseId);

    // When & Then
    mockMvc.perform(post("/outreach-logs")
            .header("X-Tenant-ID", tenantId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(testOutreachLog.getId().toString()))
        .andExpect(jsonPath("$.patientId").value(patientId.toString()))
        .andExpect(jsonPath("$.outcomeType").value("SUCCESSFUL_CONTACT"));

    verify(outreachLogService, times(1)).createOutreachLog(any(OutreachLogEntity.class));
}
```

**REST Testing Patterns:**
- **@WebMvcTest** - Tests HTTP layer only
- **MockMvc** - HTTP request simulation
- **jsonPath()** - JSON response validation
- **status()** - HTTP status verification
- **Tenant header validation** - Multi-tenant safety

### Step 4: Implement Controller

```java
@RestController
@RequestMapping("/api/v1/outreach-logs")
@RequiredArgsConstructor
@Validated
public class OutreachLogController {

    private final OutreachLogService outreachLogService;

    @PostMapping
    @PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
    @Operation(summary = "Create outreach log")
    public ResponseEntity<OutreachLogEntity> createOutreachLog(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @Valid @RequestBody OutreachLogEntity outreachLog) {
        log.info("Creating outreach log for patient {} in tenant {}",
            outreachLog.getPatientId(), tenantId);

        outreachLog.setTenantId(tenantId);
        OutreachLogEntity created = outreachLogService.createOutreachLog(outreachLog);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

**Controller Implementation Patterns:**
- **@PreAuthorize** - Role-based access control
- **X-Tenant-ID header** - Multi-tenant enforcement
- **@Valid** - Input validation
- **ResponseEntity** - Proper HTTP status codes
- **OpenAPI annotations** - Auto-generated documentation

---

## Template: Apply Pattern to MedicationReconciliationService

### File Structure
```
For each new service, create 4 files:
├── src/test/java/.../MedicationReconciliationServiceTest.java
├── src/main/java/.../application/MedicationReconciliationService.java
├── src/test/java/.../api/v1/MedicationReconciliationControllerTest.java
└── src/main/java/.../api/v1/MedicationReconciliationController.java
```

### Test Case Template

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationReconciliationService")
class MedicationReconciliationServiceTest {

    @Mock
    private MedicationReconciliationRepository medRecRepository;

    @InjectMocks
    private MedicationReconciliationService medRecService;

    @Test
    @DisplayName("should start medication reconciliation workflow")
    void testStartReconciliation_Success() {
        // Given
        when(medRecRepository.save(any(MedicationReconciliationEntity.class)))
            .thenReturn(testMedRec);

        // When
        MedicationReconciliationEntity result =
            medRecService.startReconciliation(testMedRec);

        // Then
        assertThat(result)
            .isNotNull()
            .hasFieldOrPropertyWithValue("status",
                MedicationReconciliationEntity.ReconciliationStatus.REQUESTED);

        verify(medRecRepository, times(1)).save(any());
    }

    // ... more tests for each public method
}
```

### Service Implementation Template

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationReconciliationService {

    private final MedicationReconciliationRepository medRecRepository;

    @Transactional
    public MedicationReconciliationEntity startReconciliation(
            MedicationReconciliationEntity medRec) {

        log.debug("Starting medication reconciliation for patient {} by reconciler {}",
            medRec.getPatientId(), medRec.getReconcilerId());

        if (medRec.getId() == null) {
            medRec.setId(UUID.randomUUID());
        }

        MedicationReconciliationEntity saved = medRecRepository.save(medRec);

        log.info("Medication reconciliation started: {} for patient {}",
            saved.getId(), saved.getPatientId());

        return saved;
    }

    // ... implement remaining methods following same pattern
}
```

### Controller Template

```java
@Slf4j
@RestController
@RequestMapping("/api/v1/medication-reconciliations")
@RequiredArgsConstructor
@Validated
public class MedicationReconciliationController {

    private final MedicationReconciliationService medRecService;

    @PostMapping
    @PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
    @Operation(summary = "Start medication reconciliation")
    public ResponseEntity<MedicationReconciliationEntity> startReconciliation(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @Valid @RequestBody MedicationReconciliationEntity medRec) {

        log.info("Starting med rec for patient {} in tenant {}",
            medRec.getPatientId(), tenantId);

        medRec.setTenantId(tenantId);
        MedicationReconciliationEntity created =
            medRecService.startReconciliation(medRec);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ... implement remaining endpoints
}
```

---

## Architectural Insights Gained

### ★ Insight 1: Multi-Tenant Isolation Pattern

**Pattern**: Tenant ID first in every query
```java
// ✅ CORRECT
findByTenantIdAndPatientIdOrderByAttemptedAtDesc(tenantId, patientId)

// ❌ WRONG
findByPatientIdAndTenantId(patientId, tenantId)  // Data leakage risk!
```

**Why**: Composite indexes are most efficient when leading column is first. This prevents accidental queries that skip tenant filtering.

### ★ Insight 2: Enum-Based State Management

**Pattern**: Use Java enums instead of string fields
```java
// ✅ CORRECT - Type-safe, compile-time checked
public enum OutcomeType {
    SUCCESSFUL_CONTACT, NO_ANSWER, LEFT_MESSAGE, ...
}

// ❌ RISKY - Strings allow typos, runtime errors
private String outcomeType = "SUCC ESFUL_CONTACT";  // Typo!
```

**Benefit**: Database enforces valid values via CHECK constraints, prevents invalid states, enables IDE autocomplete.

### ★ Insight 3: FHIR Resource Integration Without ORM Relations

**Pattern**: Store FHIR IDs separately, not as foreign keys
```java
// ✅ CORRECT - Loose coupling
@Column(name = "task_id", length = 128)
private String taskId;  // Reference to FHIR Task UUID

// ❌ PROBLEMATIC - Tight coupling
@OneToOne
@JoinColumn(name = "task_id")
private FhirTask task;  // Creates ORM overhead
```

**Benefit**:
- Enables independent schema evolution
- Avoids N+1 query problems
- Services can be deployed independently
- Easier testing (mocking FHIR service)

### ★ Insight 4: Service Layer Metrics as Inner Class

**Pattern**: Simple DTO for dashboard metrics
```java
public class OutreachMetrics {
    private long totalOutreachAttempts;
    private long successfulContacts;
    private long successRate;

    public static Builder builder() { return new Builder(); }
    // ... fluent API
}
```

**Benefit**: Quick metrics calculation without database round-trips, immutable design, fluent builder pattern.

### ★ Insight 5: Test Structure Clarity with DisplayName

**Pattern**: Use @DisplayName for test readability
```java
@Test
@DisplayName("should create outreach log with valid data")
void testCreateOutreachLog_Success() { ... }
```

**Benefit**: Test names are self-documenting, visible in IDE and test reports, clear requirement specification.

---

## Best Practices Implemented

### **1. Logging Strategy**
```java
// DEBUG: Detailed information for developers
log.debug("Creating outreach log for patient {} by nurse {}",
    patientId, nurseId);

// INFO: Important business events (audit trail)
log.info("Outreach log created: {} for patient {} with outcome: {}",
    id, patientId, outcomeType);

// WARN: Potential issues
log.warn("Outreach attempt failed for patient {}: {}", patientId, reason);

// ERROR: Failures requiring attention
log.error("Failed to create outreach log", exception);
```

### **2. Validation Strategy**
```java
// Entity level: @NotNull, @NotBlank in JPA
@Column(name = "tenant_id", nullable = false)
private String tenantId;

// Controller level: @Valid, @Validated
@PostMapping
public ResponseEntity<OutreachLogEntity> create(
    @Valid @RequestBody OutreachLogEntity outreach)

// Service level: Business rule validation
if (outreach.getId() == null) {
    outreach.setId(UUID.randomUUID());
}
```

### **3. Transaction Management**
```java
// Service: Read-only by default, @Transactional for writes
@Service
@Transactional(readOnly = true)
public class OutreachLogService {

    @Transactional  // Read-write for this method only
    public OutreachLogEntity createOutreachLog(...) { ... }
}
```

### **4. Security at Multiple Layers**
```java
// 1. Controller: JWT via gateway (implicit in headers)
// 2. Filter: TrustedHeaderAuthFilter validates HMAC
// 3. Annotation: @PreAuthorize enforces roles
@PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")

// 4. Query: Tenant ID filtered in every repository call
medRecRepository.findByTenantIdAndPatientId(tenantId, patientId)
```

---

## Testing Checklist for Each Service

### Unit Tests (Service Layer)
- [ ] Success case with valid data
- [ ] Get/retrieve operations
- [ ] Pagination handling
- [ ] Filtering/searching
- [ ] Update operations
- [ ] Delete operations (soft delete)
- [ ] Multi-tenant isolation verification
- [ ] Error cases (not found, validation)

### Integration Tests (Controller Layer)
- [ ] POST - Create (201 status)
- [ ] GET single - Retrieve (200 status)
- [ ] GET list - Pagination
- [ ] GET with filters
- [ ] PUT - Update (200 status)
- [ ] DELETE - Delete (204 status)
- [ ] 404 handling
- [ ] Missing tenant header validation
- [ ] Unauthorized access handling

### Key Test Data
```java
private String tenantId = "TENANT001";
private UUID patientId = UUID.randomUUID();
private UUID operatorId = UUID.randomUUID();  // Nurse ID
private OutreachLogEntity testEntity = ...;   // Valid test data
```

---

## Quality Metrics Target

| Metric | Target | Tool |
|--------|--------|------|
| Test Coverage | 80%+ | JaCoCo |
| Code Style | HDIM patterns | Checkstyle |
| Documentation | 100% Javadoc | Javadoc plugin |
| Multi-Tenant Safety | 100% | Manual + tests |
| HIPAA Audit Logging | 100% | Code review |

---

## Common Pitfalls to Avoid

### ❌ Pitfall 1: Forgetting Tenant ID in Queries
```java
// WRONG - Data leakage!
List<OutreachLog> logs = repository.findByPatientId(patientId);

// CORRECT - Tenant-filtered
List<OutreachLog> logs = repository.findByTenantIdAndPatientId(
    tenantId, patientId);
```

### ❌ Pitfall 2: String State Instead of Enums
```java
// WRONG - Allows invalid values
@Column(name = "outcome")
private String outcome = "SUCCEESSFUL";  // Typo!

// CORRECT - Type-safe
@Enumerated(EnumType.STRING)
private OutcomeType outcome;
```

### ❌ Pitfall 3: Missing @Transactional
```java
// WRONG - Read-only transaction for update
public void updateOutreach(OutreachLog log) {
    repository.save(log);  // May not persist!
}

// CORRECT
@Transactional
public void updateOutreach(OutreachLog log) {
    repository.save(log);  // Guaranteed to persist
}
```

### ❌ Pitfall 4: ORM Relations with FHIR
```java
// WRONG - Tight coupling, N+1 queries
@OneToOne
private Task fhirTask;  // Causes lazy load overhead

// CORRECT - Loose coupling
private String taskId;  // Just store the reference
```

### ❌ Pitfall 5: Missing Role Authorization
```java
// WRONG - Anyone can create outreach
@PostMapping
public ResponseEntity<OutreachLog> create(...) { ... }

// CORRECT - Only nurses and admins
@PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
public ResponseEntity<OutreachLog> create(...) { ... }
```

---

## Estimated Effort per Service

Based on OutreachLog implementation:

| Service | Tests | Code | Effort | Effort Days |
|---------|-------|------|--------|-------------|
| **OutreachLog** | 8 | 11 methods | 4 hours | ✅ Done |
| **MedicationReconciliation** | 10 | 13 methods | 5 hours | 0.75 |
| **PatientEducation** | 9 | 11 methods | 4.5 hours | 0.67 |
| **ReferralCoordination** | 11 | 14 methods | 5.5 hours | 0.82 |
| **Integration Tests** | 20+ | - | 8 hours | 1.0 |
| **Total Phase 1** | 48+ | ~49 methods | 27 hours | 3.5 days |

**Timeline**: ~1 week for an experienced developer following this TDD approach

---

## Resources & Documentation

**Within HDIM Codebase**:
- CLAUDE.md - Project standards and patterns
- backend/HIPAA-CACHE-COMPLIANCE.md - PHI handling
- backend/docs/GATEWAY_TRUST_ARCHITECTURE.md - Authentication
- backend/docs/ENTITY_MIGRATION_GUIDE.md - Database patterns

**External References**:
- Spring Boot 3 Testing: https://spring.io/guides/gs/testing-web/
- Mockito: https://javadoc.io/doc/org.mockito/mockito-core/
- AssertJ: https://assertj.github.io/assertj-core-features-highlight.html
- FHIR R4: https://www.hl7.org/fhir/r4/

---

**Document Version**: 1.0
**Last Updated**: 2026-01-16
**Author**: HDIM Nurse Dashboard Team

