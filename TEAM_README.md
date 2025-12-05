# Team 4: Care Gap Service
**Priority**: MEDIUM (Quality Measures)
**Estimated Time**: 6-8 days
**Worktree**: `/home/webemo-aaron/projects/healthdata-feature-care-gap-service`
**Branch**: `feature/care-gap-service`
**Dependencies**: Patient Service, CQL Engine Service

---

## 🎯 Mission

Build the **Care Gap Service** to identify and track quality measure gaps (HEDIS), prioritize gap closure opportunities, and generate patient outreach lists for care teams.

---

## 📋 Core Requirements

### Features
- [ ] Identify care gaps across HEDIS measures
- [ ] Calculate gap closure priority/impact
- [ ] Track gap status (open, closed, pending)
- [ ] Generate patient outreach lists
- [ ] Provide gap closure recommendations
- [ ] Track closure methods and dates
- [ ] Analytics dashboard data

---

## 🏗️ Data Model

```java
@Entity
@Table(name = "care_gaps")
public class CareGapEntity {
    @Id
    private UUID id;
    private String tenantId;
    private UUID patientId;

    // Measure details
    private String measureId;           // e.g., "CDC-HbA1c"
    private String measureName;
    private String measureYear;

    // Gap details
    private String gapType;             // missing-screening, overdue-visit, etc
    private String status;              // open, closed, pending
    private LocalDate identifiedDate;
    private LocalDate dueDate;

    // Priority
    private String priority;            // low, medium, high, critical
    private Double impactScore;         // 0.0-1.0

    // Recommendation
    private String recommendation;
    private String actionType;          // screening, medication, visit

    // Closure
    private LocalDate closedDate;
    private String closureMethod;       // completed-screening, medication-started, etc
    private String closedBy;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
}
```

---

## 📡 API Endpoints

```java
// Care Gap Management
POST   /api/v1/care-gaps/identify                    // Run gap identification
GET    /api/v1/care-gaps/patient/{id}                // Get patient's gaps
GET    /api/v1/care-gaps/tenant?measure={id}         // Get gaps by measure
GET    /api/v1/care-gaps/open?priority={priority}    // Get open gaps
POST   /api/v1/care-gaps/{id}/close                  // Close a gap
PUT    /api/v1/care-gaps/{id}                        // Update gap

// Outreach Lists
GET    /api/v1/care-gaps/outreach-list?measure={id}&priority={priority}
GET    /api/v1/care-gaps/outreach-list/export?format=csv

// Analytics
GET    /api/v1/care-gaps/analytics/tenant
GET    /api/v1/care-gaps/analytics/measure/{id}
GET    /api/v1/care-gaps/analytics/trends?period=30d
```

---

## 🧪 TDD Implementation Plan

### Phase 1: Gap Identification Engine (Day 1-3)

#### GapIdentificationService Tests (25+ tests)
```java
@Test
void testIdentifyGaps_detectsMissingScreening()
@Test
void testIdentifyGaps_detectsOverdueVisit()
@Test
void testIdentifyGaps_ignoresRecentlyCompleted()
@Test
void testCalculateImpactScore_considersMultipleFactors()
@Test
void testPrioritizeGaps_ordersByImpact()
@Test
void testCQLIntegration_executesMeasureLogic()
```

#### Implementation
```java
@Service
public class GapIdentificationService {
    private final PatientServiceClient patientClient;
    private final CqlEvaluationServiceClient cqlClient;
    private final CareGapRepository gapRepository;

    @Transactional
    public List<CareGapDTO> identifyGapsForPatient(String tenantId, UUID patientId, List<String> measureIds) {
        List<CareGapDTO> gaps = new ArrayList<>();

        for (String measureId : measureIds) {
            // 1. Execute CQL measure logic
            CqlEvaluationResult result = cqlClient.evaluateMeasure(tenantId, patientId, measureId);

            // 2. Check if in denominator but not numerator (gap exists)
            if (result.isInDenominator() && !result.isInNumerator()) {
                CareGapDTO gap = buildGap(measureId, result);
                gap.setImpactScore(calculateImpactScore(gap));
                gap.setPriority(determinePriority(gap.getImpactScore()));
                gaps.add(gap);
            }
        }

        // 3. Save gaps
        List<CareGapEntity> entities = gaps.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
        gapRepository.saveAll(entities);

        return gaps;
    }

    private double calculateImpactScore(CareGapDTO gap) {
        // Factors: days overdue, patient risk, measure weight
        double overdueScore = calculateOverdueScore(gap.getDueDate());
        double riskScore = getPatientRiskScore(gap.getPatientId());
        double measureWeight = getMeasureWeight(gap.getMeasureId());

        return (overdueScore * 0.4) + (riskScore * 0.3) + (measureWeight * 0.3);
    }
}
```

---

### Phase 2: Gap Closure Workflow (Day 3-4)

#### GapClosureService Tests (15+ tests)
```java
@Test
void testCloseGap_updatesStatus()
@Test
void testCloseGap_recordsMethod()
@Test
void testCloseGap_publishes Event()
@Test
void testValidateGapClosure_checksEvidence()
```

---

### Phase 3: Outreach List Generation (Day 4-5)

#### OutreachListService Tests (15+ tests)
```java
@Test
void testGenerateOutreachList_filtersByPriority()
@Test
void testGenerateOutreachList_groupsByMeasure()
@Test
void testGenerateOutreachList_includesContactInfo()
@Test
void testExportToCSV_formatsCorrectly()
```

---

### Phase 4: Analytics Engine (Day 5-6)

#### GapAnalyticsService Tests (20+ tests)
```java
@Test
void testCalculateTenantMetrics_aggregatesCorrectly()
@Test
void testCalculateGapClosureRate_computesPercentage()
@Test
void testIdentifyTrends_detectsPatterns()
@Test
void testMeasurePerformance_ranksProviders()
```

---

### Phase 5: Integration & Performance (Day 7-8)

#### Integration Tests (15+ tests)
```java
@Test
void testEndToEndGapIdentification_completesWorkflow()
@Test
void testBatchProcessing_handles1000Patients()
@Test
void testConcurrentGapClosure_maintainsConsistency()
@Test
void testAnalyticsQuery_returnsWithin2Seconds()
```

---

## 🔗 Dependencies

### CQL Engine Client
```java
@FeignClient(name = "cql-engine-service")
public interface CqlEvaluationServiceClient {
    @PostMapping("/api/v1/cql/evaluations")
    CqlEvaluationResult evaluateMeasure(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestBody CqlEvaluationRequest request);
}
```

### Patient Service Client
```java
@FeignClient(name = "patient-service")
public interface PatientServiceClient {
    @GetMapping("/api/v1/patients/{id}/health-status")
    HealthStatusDTO getHealthStatus(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable UUID id);
}
```

---

## ✅ Definition of Done

- [ ] All tests passing (90+ tests)
- [ ] Code coverage ≥80%
- [ ] Gap identification working for ≥5 HEDIS measures
- [ ] Outreach lists generating correctly
- [ ] Analytics queries < 2 seconds
- [ ] Batch processing tested with 1000+ patients
- [ ] Integration with CQL Engine verified
- [ ] PR reviewed

---

## 🚀 Getting Started

```bash
cd /home/webemo-aaron/projects/healthdata-feature-care-gap-service/backend

# Ensure dependencies are running
curl http://localhost:8082/api/v1/cql/health      # CQL Engine
curl http://localhost:8083/api/v1/patients/health  # Patient Service

# Create service structure
mkdir -p modules/services/care-gap-service/src/{main,test}/java/com/healthdata/caregap

# Start with entity tests
./gradlew :modules:services:care-gap-service:test
```

---

**TDD Focus**: CQL integration + Batch processing + Analytics

**Slack**: `#tdd-swarm-team4-care-gap`
