# Quality Measure Implementation - Next Steps

## ✅ Completed (Phase 0-1)

### Database Schema Design
- [x] **Comprehensive database plan** created (`QUALITY_MEASURE_DATABASE_PLAN.md`)
- [x] **7 new Liquibase migrations** created (0034-0040)
- [x] **Master changelog** updated with all new migrations
- [x] **Architecture diagrams** and data flow examples documented

### Migrations Created

| Migration | Table | Purpose | Status |
|-----------|-------|---------|--------|
| 0034 | `patient_measure_assignments` | Track which measures apply to which patients | ✅ Created |
| 0035 | `patient_measure_overrides` | Patient-specific parameter customization | ✅ Created |
| 0036 | `measure_config_profiles` | Reusable configuration templates | ✅ Created |
| 0037 | `patient_profile_assignments` | Link patients to profiles | ✅ Created |
| 0038 | `measure_execution_history` | Complete execution audit trail | ✅ Created |
| 0039 | `measure_modification_audit` | Track all measure changes | ✅ Created |
| 0040 | `patient_measure_eligibility_cache` | Performance optimization cache | ✅ Created |

---

## 📋 Immediate Next Steps (Phase 2)

### 1. Test and Deploy Migrations

**Priority**: HIGH | **Effort**: 2-3 hours

```bash
# 1. Test migrations on local database
cd backend
./gradlew :modules:services:quality-measure-service:clean
./gradlew :modules:services:quality-measure-service:build

# 2. Start service and verify Liquibase execution
docker compose up quality-measure-service -d

# 3. Verify all tables were created
docker exec healthdata-postgres psql -U healthdata -d quality_db -c "\dt patient_measure*"
docker exec healthdata-postgres psql -U healthdata -d quality_db -c "\dt measure_*"

# 4. Check for any schema validation errors
docker logs healthdata-quality-measure-service --tail 100 | grep -i "schema"
```

**Acceptance Criteria**:
- [ ] All 7 new tables exist in quality_db
- [ ] All indexes created successfully
- [ ] All CHECK constraints applied
- [ ] All triggers created
- [ ] No schema validation errors in logs

---

### 2. Create JPA Entities

**Priority**: HIGH | **Effort**: 4-6 hours

**Location**: `backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/domain/`

#### 2.1 PatientMeasureAssignment Entity

```java
package com.healthdata.quality.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "patient_measure_assignments", indexes = {
    @Index(name = "idx_pma_patient_active", columnList = "patient_id,active"),
    @Index(name = "idx_pma_measure_active", columnList = "measure_id,active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientMeasureAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "measure_id", nullable = false)
    private UUID measureId;

    @Column(name = "measure_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MeasureType measureType;

    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "assignment_reason")
    private String assignmentReason;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_until")
    private LocalDate effectiveUntil;

    @Type(JsonBinaryType.class)
    @Column(name = "eligibility_criteria", columnDefinition = "jsonb")
    private Map<String, Object> eligibilityCriteria;

    @Column(name = "auto_assigned", nullable = false)
    @Builder.Default
    private Boolean autoAssigned = false;

    @Column(name = "deactivated_by")
    private UUID deactivatedBy;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "deactivation_reason")
    private String deactivationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (assignedAt == null) {
            assignedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum MeasureType {
        STANDARD,
        CUSTOM
    }
}
```

#### 2.2 PatientMeasureOverride Entity

```java
@Entity
@Table(name = "patient_measure_overrides")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientMeasureOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "measure_id", nullable = false)
    private UUID measureId;

    @Column(name = "override_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private OverrideType overrideType;

    @Column(name = "override_field", nullable = false, length = 100)
    private String overrideField;

    @Column(name = "original_value")
    private String originalValue;

    @Column(name = "override_value", nullable = false)
    private String overrideValue;

    @Column(name = "value_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ValueType valueType;

    @Column(name = "clinical_reason", nullable = false)
    private String clinicalReason;

    @Type(JsonBinaryType.class)
    @Column(name = "supporting_evidence", columnDefinition = "jsonb")
    private Map<String, Object> supportingEvidence;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_until")
    private LocalDate effectiveUntil;

    @Column(name = "requires_periodic_review", nullable = false)
    @Builder.Default
    private Boolean requiresPeriodicReview = true;

    @Column(name = "review_frequency_days", nullable = false)
    @Builder.Default
    private Integer reviewFrequencyDays = 90;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "last_reviewed_by")
    private UUID lastReviewedBy;

    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum OverrideType {
        THRESHOLD,
        PARAMETER,
        EXCLUSION,
        INCLUSION_CRITERIA,
        TARGET_VALUE,
        FREQUENCY
    }

    public enum ValueType {
        NUMERIC,
        DATE,
        BOOLEAN,
        TEXT,
        JSON
    }
}
```

**Repeat for remaining 5 entities**: MeasureConfigProfile, PatientProfileAssignment, MeasureExecutionHistory, MeasureModificationAudit, PatientMeasureEligibilityCache

---

### 3. Create Repositories

**Priority**: HIGH | **Effort**: 2-3 hours

**Location**: `backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/repository/`

```java
@Repository
public interface PatientMeasureAssignmentRepository extends JpaRepository<PatientMeasureAssignment, UUID> {

    @Query("SELECT p FROM PatientMeasureAssignment p WHERE p.patientId = :patientId AND p.active = true")
    List<PatientMeasureAssignment> findActiveByPatientId(@Param("patientId") UUID patientId);

    @Query("SELECT p FROM PatientMeasureAssignment p WHERE p.measureId = :measureId AND p.active = true")
    List<PatientMeasureAssignment> findActiveByMeasureId(@Param("measureId") UUID measureId);

    @Query("SELECT p FROM PatientMeasureAssignment p WHERE p.tenantId = :tenantId AND p.patientId = :patientId AND p.active = true")
    List<PatientMeasureAssignment> findActiveByTenantAndPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );
}

@Repository
public interface PatientMeasureOverrideRepository extends JpaRepository<PatientMeasureOverride, UUID> {

    @Query("SELECT p FROM PatientMeasureOverride p WHERE p.patientId = :patientId AND p.measureId = :measureId AND p.active = true")
    List<PatientMeasureOverride> findActiveByPatientAndMeasure(
        @Param("patientId") UUID patientId,
        @Param("measureId") UUID measureId
    );

    @Query("SELECT p FROM PatientMeasureOverride p WHERE p.nextReviewDate <= :date AND p.active = true")
    List<PatientMeasureOverride> findDueForReview(@Param("date") LocalDate date);
}

// ... repositories for other entities
```

---

### 4. Implement Service Layer

**Priority**: HIGH | **Effort**: 1-2 days

#### 4.1 MeasureAssignmentService

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MeasureAssignmentService {

    private final PatientMeasureAssignmentRepository assignmentRepository;
    private final QualityMeasureRepository measureRepository;
    private final MeasureModificationAuditService auditService;

    @Transactional
    public PatientMeasureAssignment assignMeasureToPatient(
        String tenantId,
        UUID patientId,
        UUID measureId,
        UUID assignedBy,
        String reason,
        LocalDate effectiveFrom
    ) {
        log.info("Assigning measure {} to patient {} by user {}", measureId, patientId, assignedBy);

        PatientMeasureAssignment assignment = PatientMeasureAssignment.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .measureId(measureId)
            .assignedBy(assignedBy)
            .assignmentReason(reason)
            .effectiveFrom(effectiveFrom)
            .autoAssigned(false)
            .build();

        assignment = assignmentRepository.save(assignment);

        // Audit the assignment
        auditService.logModification(
            "PATIENT_ASSIGNMENT",
            assignment.getId(),
            "CREATE",
            "Measure assigned to patient",
            assignedBy
        );

        return assignment;
    }

    public List<UUID> getEligibleMeasuresForPatient(String tenantId, UUID patientId) {
        return assignmentRepository.findActiveByTenantAndPatient(tenantId, patientId)
            .stream()
            .map(PatientMeasureAssignment::getMeasureId)
            .collect(Collectors.toList());
    }
}
```

#### 4.2 MeasureOverrideService

```java
@Service
@RequiredArgsConstructor
public class MeasureOverrideService {

    private final PatientMeasureOverrideRepository overrideRepository;
    private final MeasureConfigProfileRepository profileRepository;
    private final PatientProfileAssignmentRepository profileAssignmentRepository;

    /**
     * Resolve final measure parameters for a patient by applying overrides in priority order:
     * 1. Patient-specific overrides (highest priority)
     * 2. Profile-based overrides
     * 3. Base measure definition (lowest priority)
     */
    public Map<String, Object> resolveMeasureParameters(
        UUID patientId,
        UUID measureId,
        Map<String, Object> baseMeasureParams
    ) {
        Map<String, Object> resolvedParams = new HashMap<>(baseMeasureParams);

        // Apply profile-based overrides
        List<MeasureConfigProfile> profiles = getApplicableProfiles(patientId);
        for (MeasureConfigProfile profile : profiles) {
            resolvedParams.putAll(profile.getConfigOverrides());
        }

        // Apply patient-specific overrides (highest priority)
        List<PatientMeasureOverride> overrides = overrideRepository
            .findActiveByPatientAndMeasure(patientId, measureId);
        for (PatientMeasureOverride override : overrides) {
            resolvedParams.put(
                override.getOverrideField(),
                parseOverrideValue(override)
            );
        }

        return resolvedParams;
    }

    private Object parseOverrideValue(PatientMeasureOverride override) {
        return switch (override.getValueType()) {
            case NUMERIC -> Double.parseDouble(override.getOverrideValue());
            case DATE -> LocalDate.parse(override.getOverrideValue());
            case BOOLEAN -> Boolean.parseBoolean(override.getOverrideValue());
            case JSON -> parseJson(override.getOverrideValue());
            default -> override.getOverrideValue();
        };
    }
}
```

---

### 5. Create API Endpoints

**Priority**: MEDIUM | **Effort**: 1-2 days

#### 5.1 REST Controllers

```java
@RestController
@RequestMapping("/api/v1/measures")
@RequiredArgsConstructor
@Validated
public class MeasureAssignmentController {

    private final MeasureAssignmentService assignmentService;

    @PostMapping("/{measureId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<PatientMeasureAssignmentResponse> assignMeasure(
        @PathVariable UUID measureId,
        @RequestBody @Valid AssignMeasureRequest request,
        @RequestHeader("X-Tenant-ID") String tenantId,
        @AuthenticationPrincipal User user
    ) {
        PatientMeasureAssignment assignment = assignmentService.assignMeasureToPatient(
            tenantId,
            request.getPatientId(),
            measureId,
            user.getId(),
            request.getReason(),
            request.getEffectiveFrom()
        );

        return ResponseEntity.ok(toResponse(assignment));
    }

    @GetMapping("/patients/{patientId}/applicable")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'VIEWER')")
    public ResponseEntity<List<ApplicableMeasureResponse>> getApplicableMeasures(
        @PathVariable UUID patientId,
        @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        List<UUID> measureIds = assignmentService.getEligibleMeasuresForPatient(tenantId, patientId);
        // ... fetch full measure details and return
        return ResponseEntity.ok(responses);
    }
}
```

---

### 6. Testing Strategy

**Priority**: HIGH | **Effort**: 2-3 days

#### 6.1 Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class MeasureOverrideServiceTest {

    @Mock
    private PatientMeasureOverrideRepository overrideRepository;

    @InjectMocks
    private MeasureOverrideService overrideService;

    @Test
    void shouldApplyPatientOverrideOverProfileOverride() {
        // Given
        Map<String, Object> baseParams = Map.of("hba1c_target", 7.0);

        MeasureConfigProfile profile = createProfile("hba1c_target", 8.0);
        PatientMeasureOverride patientOverride = createOverride("hba1c_target", "8.5");

        when(overrideRepository.findActiveByPatientAndMeasure(any(), any()))
            .thenReturn(List.of(patientOverride));

        // When
        Map<String, Object> resolved = overrideService.resolveMeasureParameters(
            UUID.randomUUID(),
            UUID.randomUUID(),
            baseParams
        );

        // Then
        assertThat(resolved.get("hba1c_target")).isEqualTo(8.5);
    }
}
```

#### 6.2 Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MeasureAssignmentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAssignMeasureToPatient() throws Exception {
        mockMvc.perform(post("/api/v1/measures/{measureId}/assign", measureId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-ID", "tenant1")
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));
    }
}
```

---

## 📅 Phased Rollout Plan

### Week 1-2: Foundation (Phase 2 Complete)
- [x] Database schema design ✅
- [x] Liquibase migrations ✅
- [ ] JPA entities
- [ ] Repositories
- [ ] Basic service layer

### Week 3-4: Core Features (Phase 3)
- [ ] Patient measure assignments API
- [ ] Patient overrides API
- [ ] Override resolution logic
- [ ] Unit tests

### Week 5-6: Advanced Features (Phase 4)
- [ ] Configuration profiles
- [ ] Profile assignment engine
- [ ] Profile matching rules
- [ ] Integration tests

### Week 7-8: Audit & Performance (Phase 5)
- [ ] Execution history logging
- [ ] Modification audit tracking
- [ ] Eligibility cache implementation
- [ ] Performance optimization

### Week 9-10: UI & Documentation
- [ ] Management UI for assignments
- [ ] Override approval workflow UI
- [ ] API documentation
- [ ] User guide

---

## 🎯 Success Metrics

### Technical Metrics
- [ ] All migrations execute without errors
- [ ] 100% test coverage on service layer
- [ ] < 100ms measure parameter resolution time
- [ ] < 500ms measure eligibility check time
- [ ] Zero HIPAA compliance violations

### Business Metrics
- [ ] Clinical staff can assign measures in < 30 seconds
- [ ] Patient overrides require clinical justification
- [ ] All measure changes have audit trail
- [ ] Measure calculations respect patient-specific parameters

---

## 📚 Documentation Artifacts

1. **QUALITY_MEASURE_DATABASE_PLAN.md** ✅ - Comprehensive schema documentation
2. **IMPLEMENTATION_NEXT_STEPS.md** ✅ - This document
3. **API_DOCUMENTATION.md** (TODO) - REST API reference
4. **USER_GUIDE.md** (TODO) - Clinical user guide
5. **DEVELOPER_GUIDE.md** (TODO) - Integration guide

---

## 🆘 Support & Questions

### Architecture Questions
- Review `QUALITY_MEASURE_DATABASE_PLAN.md` for data model details
- Check existing `quality_measures` and `custom_measures` implementations for patterns

### Implementation Questions
- Follow existing service patterns in quality-measure-service
- Use authentication module patterns for audit logging
- Reference patient-service for patient data access patterns

---

**Last Updated**: 2026-01-11
**Author**: Claude Code
**Status**: Ready for Implementation
