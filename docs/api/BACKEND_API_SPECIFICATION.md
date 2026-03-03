# Backend API Specification for Patient Health Overview

**Date**: November 20, 2025
**Purpose**: Define REST API endpoints for Patient Health Overview system

---

## API Overview

Base URL: `/api/v1`

Authentication: JWT Bearer Token (HIPAA-compliant)

All endpoints require `PROVIDER` or `CLINICIAN` role unless specified.

---

## 1. Mental Health Assessment APIs

### 1.1 Submit Mental Health Assessment

Submit a completed mental health screening questionnaire (PHQ-9, GAD-7, PHQ-2, etc.)

**Endpoint**: `POST /mental-health/assessments`

**Request Headers**:
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Request Body**:
```json
{
  "patientId": "patient-123",
  "assessmentType": "PHQ-9",
  "assessedBy": "provider-456",
  "assessmentDate": "2025-11-20T14:30:00Z",
  "responses": {
    "q1": 2,
    "q2": 2,
    "q3": 1,
    "q4": 1,
    "q5": 1,
    "q6": 1,
    "q7": 1,
    "q8": 1,
    "q9": 2
  },
  "clinicalNotes": "Patient reports moderate symptoms, engaged in discussion about treatment options"
}
```

**Response**: `201 Created`
```json
{
  "id": "assessment-789",
  "patientId": "patient-123",
  "assessmentType": "PHQ-9",
  "name": "Patient Health Questionnaire-9",
  "score": 12,
  "maxScore": 27,
  "severity": "moderate",
  "interpretation": "Moderate depression",
  "positiveScreen": true,
  "thresholdScore": 10,
  "requiresFollowup": true,
  "assessedBy": "provider-456",
  "assessmentDate": "2025-11-20T14:30:00Z",
  "createdAt": "2025-11-20T14:30:05Z"
}
```

**Java Service Implementation**:

```java
@Service
public class MentalHealthAssessmentService {

    @Autowired
    private MentalHealthAssessmentRepository repository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private CareGapService careGapService;

    @Transactional
    public MentalHealthAssessmentResponse submitAssessment(MentalHealthAssessmentRequest request) {
        // 1. Validate request
        validateAssessmentRequest(request);

        // 2. Calculate score based on assessment type
        MentalHealthAssessment assessment = calculateScore(request);

        // 3. Determine severity and interpretation
        assessment.setSeverity(determineSeverity(assessment.getType(), assessment.getScore()));
        assessment.setInterpretation(getInterpretation(assessment.getType(), assessment.getSeverity()));

        // 4. Check if positive screen
        assessment.setPositiveScreen(assessment.getScore() >= getThreshold(assessment.getType()));

        // 5. Save to database
        assessment = repository.save(assessment);

        // 6. Trigger care gap if positive screen
        if (assessment.isPositiveScreen()) {
            careGapService.createMentalHealthFollowupGap(request.getPatientId(), assessment);
        }

        // 7. Audit log
        auditService.logMentalHealthAssessment(request.getPatientId(), assessment.getId(), request.getAssessedBy());

        // 8. Return response
        return mapToResponse(assessment);
    }

    private int calculatePHQ9Score(Map<String, Integer> responses) {
        return responses.values().stream()
            .mapToInt(Integer::intValue)
            .sum();
    }

    private String determinePHQ9Severity(int score) {
        if (score <= 4) return "minimal";
        if (score <= 9) return "mild";
        if (score <= 14) return "moderate";
        if (score <= 19) return "moderately-severe";
        return "severe";
    }

    private int getPHQ9Threshold() {
        return 10; // Positive screen at ≥10
    }
}
```

**Entity**:

```java
@Entity
@Table(name = "mental_health_assessments")
public class MentalHealthAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AssessmentType type; // PHQ_9, GAD_7, PHQ_2, etc.

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private Integer maxScore;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false, length = 1000)
    private String interpretation;

    @Column(nullable = false)
    private Boolean positiveScreen;

    @Column(nullable = false)
    private Integer thresholdScore;

    @Column(nullable = false)
    private Boolean requiresFollowup;

    @Column(name = "assessed_by", nullable = false)
    private String assessedBy;

    @Column(name = "assessment_date", nullable = false)
    private Instant assessmentDate;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Integer> responses;

    @Column(length = 2000)
    private String clinicalNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Getters and setters
}
```

**Repository**:

```java
@Repository
public interface MentalHealthAssessmentRepository extends JpaRepository<MentalHealthAssessment, UUID> {

    List<MentalHealthAssessment> findByPatientIdOrderByAssessmentDateDesc(String patientId);

    List<MentalHealthAssessment> findByPatientIdAndTypeOrderByAssessmentDateDesc(
        String patientId,
        AssessmentType type
    );

    Optional<MentalHealthAssessment> findFirstByPatientIdAndTypeOrderByAssessmentDateDesc(
        String patientId,
        AssessmentType type
    );

    @Query("SELECT a FROM MentalHealthAssessment a WHERE a.patientId = :patientId " +
           "AND a.positiveScreen = true AND a.requiresFollowup = true " +
           "ORDER BY a.assessmentDate DESC")
    List<MentalHealthAssessment> findPositiveScreensRequiringFollowup(@Param("patientId") String patientId);
}
```

### 1.2 Get Patient Mental Health Assessments

**Endpoint**: `GET /mental-health/assessments/{patientId}`

**Query Parameters**:
- `type` (optional): Filter by assessment type (PHQ-9, GAD-7, etc.)
- `limit` (optional, default: 10): Number of results to return
- `offset` (optional, default: 0): Pagination offset

**Response**: `200 OK`
```json
{
  "patientId": "patient-123",
  "total": 15,
  "assessments": [
    {
      "id": "assessment-789",
      "type": "PHQ-9",
      "score": 12,
      "severity": "moderate",
      "positiveScreen": true,
      "assessmentDate": "2025-11-20T14:30:00Z"
    },
    {
      "id": "assessment-788",
      "type": "GAD-7",
      "score": 8,
      "severity": "mild",
      "positiveScreen": false,
      "assessmentDate": "2025-11-15T10:00:00Z"
    }
  ]
}
```

### 1.3 Get Assessment Trend

**Endpoint**: `GET /mental-health/assessments/{patientId}/trend`

**Query Parameters**:
- `type`: Assessment type (required)
- `startDate`: Start date for trend (ISO 8601)
- `endDate`: End date for trend (ISO 8601)

**Response**: `200 OK`
```json
{
  "patientId": "patient-123",
  "assessmentType": "PHQ-9",
  "trend": "declining",  // improving | stable | declining
  "dataPoints": [
    { "date": "2025-09-01", "score": 8 },
    { "date": "2025-10-01", "score": 10 },
    { "date": "2025-11-01", "score": 12 }
  ],
  "averageScore": 10.0,
  "currentSeverity": "moderate",
  "previousSeverity": "mild"
}
```

---

## 2. Care Gap APIs

### 2.1 Get Patient Care Gaps

**Endpoint**: `GET /care-gaps/{patientId}`

**Query Parameters**:
- `category` (optional): Filter by category (preventive, chronic-disease, mental-health, etc.)
- `priority` (optional): Filter by priority (urgent, high, medium, low)
- `includeAddressed` (optional, default: false): Include addressed gaps

**Response**: `200 OK`
```json
{
  "patientId": "patient-123",
  "total": 5,
  "urgent": 1,
  "high": 2,
  "medium": 2,
  "careGaps": [
    {
      "id": "gap-001",
      "category": "mental-health",
      "title": "PHQ-9 Follow-up Required",
      "description": "Patient screened positive on PHQ-9 (score: 12). Requires clinical follow-up within 30 days.",
      "priority": "high",
      "status": "open",
      "dueDate": "2025-12-20",
      "overdueDays": 0,
      "measureId": "CMS2",
      "measureName": "Preventive Care and Screening: Screening for Depression and Follow-Up Plan",
      "recommendedActions": [
        "Schedule follow-up appointment with mental health provider",
        "Review treatment options with patient",
        "Consider referral to psychiatry"
      ],
      "barriers": ["Patient transportation issues"],
      "createdDate": "2025-11-20",
      "lastUpdated": "2025-11-20"
    }
  ]
}
```

**Java Service**:

```java
@Service
public class CareGapService {

    @Autowired
    private CareGapRepository repository;

    @Autowired
    private QualityMeasureService qualityMeasureService;

    public void createMentalHealthFollowupGap(String patientId, MentalHealthAssessment assessment) {
        CareGap gap = new CareGap();
        gap.setPatientId(patientId);
        gap.setCategory(CareGapCategory.MENTAL_HEALTH);
        gap.setTitle(String.format("%s Follow-up Required", assessment.getType()));
        gap.setDescription(String.format(
            "Patient screened positive on %s (score: %d). Requires clinical follow-up within 30 days.",
            assessment.getType(), assessment.getScore()
        ));
        gap.setPriority(determinePriority(assessment.getSeverity()));
        gap.setStatus(CareGapStatus.OPEN);
        gap.setDueDate(Instant.now().plus(30, ChronoUnit.DAYS));
        gap.setMeasureId("CMS2");
        gap.setMeasureName("Preventive Care and Screening: Screening for Depression and Follow-Up Plan");
        gap.setRecommendedActions(getRecommendedActions(assessment));

        repository.save(gap);
    }

    private CareGapPriority determinePriority(String severity) {
        switch (severity) {
            case "severe":
            case "moderately-severe":
                return CareGapPriority.URGENT;
            case "moderate":
                return CareGapPriority.HIGH;
            case "mild":
                return CareGapPriority.MEDIUM;
            default:
                return CareGapPriority.LOW;
        }
    }
}
```

### 2.2 Mark Care Gap as Addressed

**Endpoint**: `PUT /care-gaps/{gapId}/address`

**Request Body**:
```json
{
  "addressedBy": "provider-456",
  "addressedDate": "2025-11-21T10:00:00Z",
  "interventions": [
    "Scheduled follow-up appointment for 2025-11-28",
    "Referred to psychiatry",
    "Provided patient education materials"
  ],
  "notes": "Patient engaged and receptive to treatment plan"
}
```

**Response**: `200 OK`
```json
{
  "id": "gap-001",
  "status": "addressed",
  "addressedBy": "provider-456",
  "addressedDate": "2025-11-21T10:00:00Z",
  "interventions": [...]
}
```

---

## 3. Risk Stratification APIs

### 3.1 Calculate Patient Risk Score

**Endpoint**: `POST /risk-stratification/{patientId}/calculate`

**Request Body**: (Optional - if not provided, uses all available data)
```json
{
  "includeFactors": [
    "clinical-complexity",
    "social-complexity",
    "mental-health",
    "utilization",
    "cost"
  ],
  "timeframe": "90-days"  // 30-days | 90-days | 1-year
}
```

**Response**: `200 OK`
```json
{
  "patientId": "patient-123",
  "calculationDate": "2025-11-20T15:00:00Z",
  "overallRisk": "moderate",
  "scores": {
    "clinicalComplexity": 65,
    "socialComplexity": 45,
    "mentalHealthRisk": 72,
    "utilizationRisk": 38,
    "costRisk": 42
  },
  "predictions": {
    "hospitalizationRisk30Day": 18,
    "hospitalizationRisk90Day": 35,
    "edVisitRisk30Day": 22,
    "readmissionRisk": 18
  },
  "categories": {
    "diabetes": "moderate",
    "cardiovascular": "low",
    "respiratory": "low",
    "mentalHealth": "high",
    "fallRisk": "low"
  },
  "contributingFactors": [
    { "factor": "Positive PHQ-9 screen", "weight": 0.25 },
    { "factor": "Multiple chronic conditions", "weight": 0.20 },
    { "factor": "Recent hospitalization", "weight": 0.15 }
  ]
}
```

**Java Service**:

```java
@Service
public class RiskStratificationService {

    @Autowired
    private MentalHealthAssessmentRepository mentalHealthRepo;

    @Autowired
    private ConditionRepository conditionRepo;

    @Autowired
    private ObservationRepository observationRepo;

    @Autowired
    private EncounterRepository encounterRepo;

    public RiskStratificationResponse calculateRisk(String patientId, RiskCalculationRequest request) {
        // 1. Gather all relevant patient data
        List<Condition> conditions = conditionRepo.findActiveConditions(patientId);
        List<MentalHealthAssessment> mhAssessments = mentalHealthRepo.findByPatientIdOrderByAssessmentDateDesc(patientId);
        List<Observation> recentLabs = observationRepo.findRecentLabs(patientId, 90);
        List<Encounter> recentEncounters = encounterRepo.findByPatientIdAndDateAfter(patientId, Instant.now().minus(90, ChronoUnit.DAYS));

        // 2. Calculate component scores
        int clinicalComplexity = calculateClinicalComplexity(conditions, recentLabs);
        int mentalHealthRisk = calculateMentalHealthRisk(mhAssessments);
        int utilizationRisk = calculateUtilizationRisk(recentEncounters);

        // 3. Calculate overall risk level
        RiskLevel overallRisk = determineOverallRisk(clinicalComplexity, mentalHealthRisk, utilizationRisk);

        // 4. Run predictive models
        PredictiveScores predictions = runPredictiveModels(patientId, conditions, mhAssessments, recentEncounters);

        // 5. Build response
        return RiskStratificationResponse.builder()
            .patientId(patientId)
            .calculationDate(Instant.now())
            .overallRisk(overallRisk)
            .scores(buildScores(clinicalComplexity, mentalHealthRisk, utilizationRisk))
            .predictions(predictions)
            .categories(calculateCategoryRisks(conditions, mhAssessments))
            .build();
    }

    private int calculateMentalHealthRisk(List<MentalHealthAssessment> assessments) {
        if (assessments.isEmpty()) return 0;

        // Get most recent assessment
        MentalHealthAssessment latest = assessments.get(0);

        int baseRisk = switch (latest.getSeverity()) {
            case "severe" -> 90;
            case "moderately-severe" -> 75;
            case "moderate" -> 60;
            case "mild" -> 40;
            default -> 20;
        };

        // Adjust for trend
        if (assessments.size() >= 2) {
            MentalHealthAssessment previous = assessments.get(1);
            if (latest.getScore() > previous.getScore()) {
                baseRisk += 10; // Worsening trend
            } else if (latest.getScore() < previous.getScore()) {
                baseRisk -= 10; // Improving trend
            }
        }

        return Math.min(100, Math.max(0, baseRisk));
    }
}
```

---

## 4. Health Score APIs

### 4.1 Get Patient Health Score

**Endpoint**: `GET /health-score/{patientId}`

**Response**: `200 OK`
```json
{
  "patientId": "patient-123",
  "score": 68,
  "status": "fair",
  "trend": "declining",
  "components": {
    "physical": 72,
    "mental": 58,
    "social": 65,
    "preventive": 75
  },
  "lastCalculated": "2025-11-20T15:00:00Z"
}
```

---

## 5. Database Schema

### Mental Health Assessments Table

```sql
CREATE TABLE mental_health_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,  -- PHQ_9, GAD_7, PHQ_2, etc.
    score INTEGER NOT NULL,
    max_score INTEGER NOT NULL,
    severity VARCHAR(50) NOT NULL,
    interpretation VARCHAR(1000) NOT NULL,
    positive_screen BOOLEAN NOT NULL,
    threshold_score INTEGER NOT NULL,
    requires_followup BOOLEAN NOT NULL,
    assessed_by VARCHAR(100) NOT NULL,
    assessment_date TIMESTAMP WITH TIME ZONE NOT NULL,
    responses JSONB NOT NULL,
    clinical_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,

    INDEX idx_patient_date (patient_id, assessment_date DESC),
    INDEX idx_patient_type (patient_id, type),
    INDEX idx_positive_screen (patient_id, positive_screen) WHERE positive_screen = true
);
```

### Care Gaps Table

```sql
CREATE TABLE care_gaps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,  -- preventive, chronic-disease, mental-health, etc.
    title VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL,  -- urgent, high, medium, low
    status VARCHAR(20) NOT NULL,  -- open, addressed, dismissed
    due_date TIMESTAMP WITH TIME ZONE,
    overdue_days INTEGER,
    measure_id VARCHAR(100),
    measure_name VARCHAR(500),
    recommended_actions JSONB,
    barriers JSONB,
    addressed_by VARCHAR(100),
    addressed_date TIMESTAMP WITH TIME ZONE,
    interventions JSONB,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,

    INDEX idx_patient_status (patient_id, status),
    INDEX idx_patient_priority (patient_id, priority),
    INDEX idx_due_date (due_date) WHERE status = 'open'
);
```

### Risk Assessments Table

```sql
CREATE TABLE risk_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id VARCHAR(100) NOT NULL,
    calculation_date TIMESTAMP WITH TIME ZONE NOT NULL,
    overall_risk VARCHAR(20) NOT NULL,  -- low, moderate, high, critical
    clinical_complexity_score INTEGER NOT NULL,
    social_complexity_score INTEGER NOT NULL,
    mental_health_risk_score INTEGER NOT NULL,
    utilization_risk_score INTEGER NOT NULL,
    cost_risk_score INTEGER NOT NULL,
    hospitalization_risk_30day DECIMAL(5,2),
    hospitalization_risk_90day DECIMAL(5,2),
    ed_visit_risk_30day DECIMAL(5,2),
    readmission_risk DECIMAL(5,2),
    category_risks JSONB,
    contributing_factors JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    INDEX idx_patient_date (patient_id, calculation_date DESC),
    INDEX idx_high_risk (patient_id, overall_risk) WHERE overall_risk IN ('high', 'critical')
);
```

---

## 6. Security & Audit

### Audit Logging

All API calls must be logged for HIPAA compliance:

```java
@Aspect
@Component
public class MentalHealthAuditAspect {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Around("@annotation(com.healthdata.security.Audited)")
    public Object auditMentalHealthAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        // Extract patient ID from method arguments
        String patientId = extractPatientId(joinPoint.getArgs());
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        // Log access
        AuditLog log = AuditLog.builder()
            .resourceType("MentalHealthAssessment")
            .resourceId(patientId)
            .action(joinPoint.getSignature().getName())
            .userId(userId)
            .timestamp(Instant.now())
            .ipAddress(getClientIpAddress())
            .build();

        auditLogRepository.save(log);

        // Proceed with method execution
        return joinPoint.proceed();
    }
}
```

---

## 7. Testing

### Integration Test Example

```java
@SpringBootTest
@AutoConfigureMockMvc
class MentalHealthAssessmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "PROVIDER")
    void shouldSubmitPHQ9Assessment() throws Exception {
        MentalHealthAssessmentRequest request = MentalHealthAssessmentRequest.builder()
            .patientId("patient-123")
            .assessmentType("PHQ-9")
            .assessedBy("provider-456")
            .responses(Map.of(
                "q1", 2, "q2", 2, "q3", 1, "q4", 1,
                "q5", 1, "q6", 1, "q7", 1, "q8", 1, "q9", 2
            ))
            .build();

        mockMvc.perform(post("/api/v1/mental-health/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.score").value(12))
            .andExpect(jsonPath("$.severity").value("moderate"))
            .andExpect(jsonPath("$.positiveScreen").value(true))
            .andExpect(jsonPath("$.requiresFollowup").value(true));
    }

    @Test
    @WithMockUser(roles = "PROVIDER")
    void shouldCalculateCorrectPHQ9Severity() {
        // Test all severity levels
        assertSeverity(3, "minimal");
        assertSeverity(7, "mild");
        assertSeverity(12, "moderate");
        assertSeverity(17, "moderately-severe");
        assertSeverity(23, "severe");
    }
}
```

---

## Summary

This specification provides:

1. ✅ Complete REST API design for mental health assessments
2. ✅ Care gap tracking and management endpoints
3. ✅ Risk stratification calculation APIs
4. ✅ Java service implementation examples
5. ✅ Database schema definitions
6. ✅ Security and audit logging
7. ✅ Testing examples

**Next Steps**: Implement these endpoints in your backend service layer, following the patterns shown in the examples.
