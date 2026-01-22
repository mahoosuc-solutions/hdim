---
name: quality-measures
description: Comprehensive guide to implementing HEDIS quality measures using CQL (Clinical Quality Language), FHIR R4 data retrieval, and event-driven evaluation pipelines
---

# Quality Measures Skill

## Overview

This skill provides comprehensive guidance on implementing **clinical quality measures** in HDIM using **Clinical Quality Language (CQL)** for HEDIS and CMS quality reporting. HDIM evaluates quality measures to identify care gaps, calculate compliance rates, and support value-based care contracts.

**Key Technologies:**
- CQL 1.5.x - HL7 standard for clinical quality logic
- FHIR R4 - Data model for clinical resources
- CQL Engine Service (Port 8081) - Executes CQL libraries
- Event-driven architecture - Kafka-based asynchronous evaluation

## Quality Measure Architecture

### Measure Evaluation Pipeline

```
Quality Measure Service (Port 8087)
  ↓ POST /api/v1/quality-measures/evaluate
CQL Engine Service (Port 8081)
  ↓ Fetch patient data
FHIR Service (Port 8085)
  ↓ Retrieve FHIR resources
  ↓ Execute CQL logic
  ↓ Return evaluation result
Quality Measure Event Service
  ↓ Update projections
Care Gap Service (if NON_COMPLIANT)
  ↓ Generate care gap recommendations
```

### Measure Types

**1. Proportion Measures** (Most common in HEDIS)
- **Structure:** Numerator / Denominator = Compliance Rate
- **Example:** Diabetes HbA1c Control (CDC-H)
  - Denominator: Patients with diabetes aged 18-75
  - Numerator: Patients with HbA1c < 8.0% in measurement period
  - Rate: 72% compliance

**2. Ratio Measures**
- **Structure:** Numerator / Denominator (different populations)
- **Example:** Complications per 1000 patient-years
  - Denominator: Total patient-years
  - Numerator: Total complications
  - Rate: 15.3 complications per 1000 patient-years

**3. Continuous Variable Measures**
- **Structure:** Aggregate function over measure population
- **Example:** Average blood pressure
  - Population: All BP measurements
  - Aggregation: Mean systolic BP = 128.5 mmHg

## CQL Measure Structure

### Required Components

Every CQL measure library MUST include these components:

```cql
library HEDIS_CDC_H version '1.0.0'

// 1. FHIR version declaration
using FHIR version '4.0.1'
include FHIRHelpers version '4.0.1'

// 2. Value set declarations (terminology bindings)
valueset "HbA1c Laboratory Test": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1013'
valueset "Diabetes": 'urn:oid:2.16.840.1.113883.3.464.1003.103.12.1001'

// 3. Parameters (measurement period)
parameter "Measurement Period" Interval<DateTime>

// 4. Context (Patient-level evaluation)
context Patient

// 5. Population definitions (REQUIRED for proportion measures)
define "Initial Population": ...
define "Denominator": ...
define "Numerator": ...
define "Denominator Exclusions": ...  // Optional
define "Numerator Exclusions": ...     // Optional

// 6. Care gap identification
define "Care Gap": "Denominator" and not "Numerator"
```

## HEDIS Measure Example: CDC-H (HbA1c Control)

### Complete CQL Library

```cql
library HEDIS_CDC_H version '1.0.0'

using FHIR version '4.0.1'
include FHIRHelpers version '4.0.1'

// Terminology
codesystem "LOINC": 'http://loinc.org'
codesystem "SNOMEDCT": 'http://snomed.info/sct'

valueset "HbA1c Laboratory Test": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1013'
valueset "Diabetes": 'urn:oid:2.16.840.1.113883.3.464.1003.103.12.1001'
valueset "End Stage Renal Disease": 'urn:oid:2.16.840.1.113883.3.464.1003.109.12.1028'

parameter "Measurement Period" Interval<DateTime>

context Patient

// ====================
// Population Criteria
// ====================

// Initial Population: Adults with diabetes
define "Initial Population":
  AgeInYearsAt(start of "Measurement Period") >= 18
    and AgeInYearsAt(start of "Measurement Period") <= 75
    and exists "Diabetes Diagnosis"

define "Diabetes Diagnosis":
  [Condition: "Diabetes"] C
    where C.clinicalStatus ~ "active"
      and C.verificationStatus ~ "confirmed"
      and C.onsetDateTime before end of "Measurement Period"

// Denominator: All patients in initial population
define "Denominator":
  "Initial Population"

// Numerator: Patients with controlled HbA1c (< 8.0%)
define "Numerator":
  "Denominator"
    and exists "HbA1c Test Controlled"

define "HbA1c Test Controlled":
  [Observation: "HbA1c Laboratory Test"] O
    where O.status in {'final', 'amended', 'corrected'}
      and O.effective during "Measurement Period"
      and (O.value as Quantity) < 8.0 '%'

// Denominator Exclusions: Patients with ESRD
define "Denominator Exclusions":
  exists [Condition: "End Stage Renal Disease"] E
    where E.clinicalStatus ~ "active"
      and E.verificationStatus ~ "confirmed"

// ====================
// Care Gap Logic
// ====================

define "Care Gap":
  "Denominator"
    and not "Numerator"
    and not "Denominator Exclusions"

define "Care Gap Reason":
  if not exists "HbA1c Tests" then 'No HbA1c test in measurement period'
  else if not exists "HbA1c Test Controlled" then 'HbA1c not controlled (≥ 8.0%)'
  else null

define "HbA1c Tests":
  [Observation: "HbA1c Laboratory Test"] O
    where O.status in {'final', 'amended', 'corrected'}
      and O.effective during "Measurement Period"

define "Most Recent HbA1c Value":
  Last(
    "HbA1c Tests" O
      sort by (O.effective as FHIR.dateTime)
  ).value as Quantity

define "Care Gap Priority":
  if "Most Recent HbA1c Value" >= 9.0 '%' then 'HIGH'
  else if "Most Recent HbA1c Value" >= 8.5 '%' then 'MEDIUM'
  else if "Most Recent HbA1c Value" is null then 'MEDIUM'
  else 'LOW'

define "Next Action":
  if "Care Gap Reason" = 'No HbA1c test in measurement period'
    then 'Order HbA1c laboratory test'
  else if "Care Gap Reason" = 'HbA1c not controlled (≥ 8.0%)'
    then 'Review diabetes management plan, consider medication adjustment'
  else null

define "Due Date":
  if "Care Gap Reason" = 'No HbA1c test in measurement period'
    then Today() + 30 days
  else if "Care Gap Reason" = 'HbA1c not controlled (≥ 8.0%)'
    then Today() + 14 days
  else null
```

### Key CQL Patterns

#### Pattern 1: Age Calculation

```cql
// Calculate age at start of measurement period
define "Is Adult":
  AgeInYearsAt(start of "Measurement Period") >= 18
```

#### Pattern 2: Condition Check with Status

```cql
// Always check clinicalStatus and verificationStatus
define "Active Diabetes":
  [Condition: "Diabetes"] C
    where C.clinicalStatus ~ "active"         // Not resolved/inactive
      and C.verificationStatus ~ "confirmed"   // Not entered-in-error
      and C.onsetDateTime before end of "Measurement Period"
```

#### Pattern 3: Observation Retrieval

```cql
// Always filter by status (final, amended, corrected)
define "Valid Lab Results":
  [Observation: "HbA1c Laboratory Test"] O
    where O.status in {'final', 'amended', 'corrected'}
      and O.effective during "Measurement Period"
      and O.value is not null
```

#### Pattern 4: Value Comparison with Type Casting

```cql
// Cast value to Quantity for numeric comparison
define "Elevated HbA1c":
  [Observation: "HbA1c Laboratory Test"] O
    where O.value is not null
      and (O.value as Quantity).value >= 8.0
```

#### Pattern 5: Most Recent Value

```cql
// Use Last() with sort by effective date
define "Most Recent BP":
  Last(
    [Observation: "Blood Pressure"] BP
      where BP.status in {'final', 'amended'}
      sort by (effective as FHIR.dateTime)
  )
```

#### Pattern 6: Date Interval Checks

```cql
// Use 'during' for interval containment
define "Recent Tests":
  [Observation: "Lab Test"] O
    where O.effective during "Measurement Period"

// Use 'before' for date comparison
define "Historical Diagnoses":
  [Condition: "Diabetes"] C
    where C.onsetDateTime before start of "Measurement Period"
```

## Quality Measure Data Model

### Measure Definition Entity

```java
@Entity
@Table(name = "quality_measure_definitions", indexes = {
  @Index(name = "idx_qmd_tenant_code", columnList = "tenant_id,measure_code", unique = true)
})
public class QualityMeasureDefinition {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(name = "measure_code", nullable = false)
  private String measureCode;  // "CDC-H", "BCS", "COL"

  @Column(name = "measure_name", nullable = false)
  private String measureName;  // "Comprehensive Diabetes Care - HbA1c Control"

  @Column(name = "measure_set")
  private String measureSet;   // "HEDIS", "CMS", "custom"

  @Column(name = "version", nullable = false)
  private String version;      // "1.0.0"

  @Column(name = "domain")
  private String domain;       // "Diabetes", "Preventive Care"

  @Column(name = "measure_type", nullable = false)
  private String measureType;  // "proportion", "ratio", "continuous-variable"

  @Column(name = "cql_library_id")
  private UUID cqlLibraryId;   // Reference to CQL file storage

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "population_criteria", columnDefinition = "jsonb")
  private PopulationCriteria populationCriteria;

  @Column(name = "active")
  private Boolean active = true;

  @Column(name = "effective_period_start")
  private LocalDate effectivePeriodStart;

  @Column(name = "effective_period_end")
  private LocalDate effectivePeriodEnd;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;
}
```

### Population Criteria (JSONB)

```json
{
  "initialPopulation": "Initial Population",
  "denominator": "Denominator",
  "numerator": "Numerator",
  "denominatorExclusions": "Denominator Exclusions",
  "numeratorExclusions": null,
  "stratifications": [
    {
      "name": "Age 18-44",
      "expression": "AgeInYearsAt(start of 'Measurement Period') between 18 and 44"
    },
    {
      "name": "Age 45-64",
      "expression": "AgeInYearsAt(start of 'Measurement Period') between 45 and 64"
    },
    {
      "name": "Age 65-75",
      "expression": "AgeInYearsAt(start of 'Measurement Period') between 65 and 75"
    }
  ]
}
```

### Measure Evaluation Projection

```java
@Entity
@Table(name = "measure_evaluation_projections", indexes = {
  @Index(name = "idx_mep_tenant_measure_patient",
         columnList = "tenant_id,measure_id,patient_id", unique = true),
  @Index(name = "idx_mep_care_gap",
         columnList = "tenant_id,measure_id,compliance_status")
})
public class MeasureEvaluationProjection {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(name = "measure_id", nullable = false)
  private UUID measureId;

  @Column(name = "patient_id", nullable = false)
  private String patientId;

  @Column(name = "evaluation_date", nullable = false)
  private Instant evaluationDate;

  @Column(name = "measurement_period_start", nullable = false)
  private Instant measurementPeriodStart;

  @Column(name = "measurement_period_end", nullable = false)
  private Instant measurementPeriodEnd;

  // Population status
  @Column(name = "in_initial_population")
  private Boolean inInitialPopulation;

  @Column(name = "in_denominator")
  private Boolean inDenominator;

  @Column(name = "in_numerator")
  private Boolean inNumerator;

  @Column(name = "in_denominator_exclusions")
  private Boolean inDenominatorExclusions;

  @Column(name = "in_numerator_exclusions")
  private Boolean inNumeratorExclusions;

  // Compliance
  @Column(name = "compliance_status")
  private String complianceStatus;  // "COMPLIANT", "NON_COMPLIANT", "EXCLUDED", "PENDING"

  // Care gap details
  @Column(name = "care_gap_reason")
  private String careGapReason;  // "No HbA1c test in measurement period"

  @Column(name = "care_gap_priority")
  private String careGapPriority;  // "HIGH", "MEDIUM", "LOW"

  @Column(name = "next_action")
  private String nextAction;  // "Order HbA1c laboratory test"

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;
}
```

### Population Summary (Aggregated)

```java
@Entity
@Table(name = "measure_population_summaries", indexes = {
  @Index(name = "idx_mps_tenant_measure_period",
         columnList = "tenant_id,measure_code,evaluation_period_start", unique = true)
})
public class MeasurePopulationSummary {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(name = "measure_code", nullable = false)
  private String measureCode;

  @Column(name = "evaluation_period_start", nullable = false)
  private Instant evaluationPeriodStart;

  @Column(name = "evaluation_period_end", nullable = false)
  private Instant evaluationPeriodEnd;

  @Column(name = "denominator_count")
  private Integer denominatorCount;

  @Column(name = "numerator_count")
  private Integer numeratorCount;

  @Column(name = "exclusion_count")
  private Integer exclusionCount;

  @Column(name = "compliance_rate")
  private BigDecimal complianceRate;  // numerator / denominator

  @Column(name = "total_care_gaps")
  private Integer totalCareGaps;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "stratifications", columnDefinition = "jsonb")
  private Map<String, StratificationResult> stratifications;

  @Column(name = "calculated_at", nullable = false)
  private Instant calculatedAt;
}
```

## Quality Measure Service Implementation

### Single Patient Evaluation (Synchronous)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class QualityMeasureService {

  private final QualityMeasureRepository measureRepository;
  private final CqlEngineClient cqlEngineClient;
  private final MeasureEvaluationProjectionRepository projectionRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public EvaluationResult evaluate(
      String measureCode, String patientId, EvaluationPeriod period, String tenantId) {

    // Lookup measure definition
    QualityMeasureDefinition measure = measureRepository
        .findByCodeAndTenant(measureCode, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Measure", measureCode));

    // Call CQL Engine
    CqlEvaluationRequest request = CqlEvaluationRequest.builder()
        .libraryId("HEDIS_" + measureCode)
        .version(measure.getVersion())
        .patientId(patientId)
        .parameters(Map.of(
            "Measurement Period", Map.of(
                "start", period.getStart(),
                "end", period.getEnd()
            )
        ))
        .dataEndpoint("http://fhir-service:8085/api/v1/fhir")
        .build();

    CqlEvaluationResponse response = cqlEngineClient.evaluate(request);

    // Map to evaluation result
    EvaluationResult result = mapToEvaluationResult(response, measure, patientId);

    // Update projection
    updateProjection(result, tenantId);

    // Publish event
    if ("NON_COMPLIANT".equals(result.getComplianceStatus())) {
      eventPublisher.publishEvent(new CareGapIdentifiedEvent(result));
    }

    return result;
  }

  private EvaluationResult mapToEvaluationResult(
      CqlEvaluationResponse response, QualityMeasureDefinition measure, String patientId) {

    Map<String, Boolean> results = response.getResults();

    boolean inDenominator = results.getOrDefault("Denominator", false);
    boolean inNumerator = results.getOrDefault("Numerator", false);
    boolean excluded = results.getOrDefault("Denominator Exclusions", false);

    String complianceStatus;
    if (excluded) {
      complianceStatus = "EXCLUDED";
    } else if (inNumerator) {
      complianceStatus = "COMPLIANT";
    } else if (inDenominator) {
      complianceStatus = "NON_COMPLIANT";
    } else {
      complianceStatus = "NOT_APPLICABLE";
    }

    CareGap careGap = null;
    if ("NON_COMPLIANT".equals(complianceStatus)) {
      String reason = (String) results.get("Care Gap Reason");
      String priority = (String) results.get("Care Gap Priority");
      String nextAction = (String) results.get("Next Action");
      LocalDate dueDate = parseLocalDate(results.get("Due Date"));

      careGap = CareGap.builder()
          .identified(true)
          .reason(reason)
          .priority(priority)
          .nextAction(nextAction)
          .dueDate(dueDate)
          .build();
    }

    return EvaluationResult.builder()
        .measureCode(measure.getMeasureCode())
        .measureName(measure.getMeasureName())
        .patientId(patientId)
        .evaluationDate(Instant.now())
        .inDenominator(inDenominator)
        .inNumerator(inNumerator)
        .excluded(excluded)
        .complianceStatus(complianceStatus)
        .careGap(careGap)
        .build();
  }
}
```

### Batch Evaluation (Asynchronous)

```java
@Service
public class BatchMeasureEvaluationService {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Async
  public CompletableFuture<String> batchEvaluate(
      String measureCode, List<String> patientIds, EvaluationPeriod period, String tenantId) {

    String jobId = UUID.randomUUID().toString();

    // Publish evaluation requests to Kafka
    for (String patientId : patientIds) {
      MeasureEvaluationRequested event = MeasureEvaluationRequested.builder()
          .measureCode(measureCode)
          .patientId(patientId)
          .evaluationPeriod(period)
          .priority("NORMAL")
          .correlationId(jobId)
          .tenantId(tenantId)
          .build();

      String topic = "measure.evaluation.requested";
      String key = tenantId + ":" + measureCode + ":" + patientId;

      try {
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(topic, key, payload);
      } catch (JsonProcessingException e) {
        log.error("Failed to publish measure evaluation request", e);
      }
    }

    return CompletableFuture.completedFuture(jobId);
  }
}
```

### Event Consumer (Kafka)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class MeasureEvaluationConsumer {

  private final QualityMeasureService qualityMeasureService;

  @KafkaListener(topics = "measure.evaluation.requested", groupId = "quality-measure-service")
  public void handleEvaluationRequest(String message) {
    try {
      MeasureEvaluationRequested event = objectMapper.readValue(
          message, MeasureEvaluationRequested.class);

      // Evaluate measure
      EvaluationResult result = qualityMeasureService.evaluate(
          event.getMeasureCode(),
          event.getPatientId(),
          event.getEvaluationPeriod(),
          event.getTenantId()
      );

      // Publish result
      publishEvaluationResult(result, event.getCorrelationId());

    } catch (Exception e) {
      log.error("Failed to process measure evaluation request", e);
    }
  }
}
```

## Quality Measure REST API

### Endpoints

```java
@RestController
@RequestMapping("/api/v1/quality-measures")
@RequiredArgsConstructor
public class QualityMeasureController {

  private final QualityMeasureService qualityMeasureService;
  private final BatchMeasureEvaluationService batchService;
  private final MeasureReportingService reportingService;

  // Evaluate single measure for patient (synchronous)
  @PostMapping("/evaluate")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
  @Audited(action = AuditAction.READ, resourceType = "QualityMeasure", encryptPayload = false)
  public ResponseEntity<EvaluationResult> evaluate(
      @RequestBody EvaluateRequest request,
      @RequestHeader("X-Tenant-ID") String tenantId) {

    EvaluationResult result = qualityMeasureService.evaluate(
        request.getMeasureCode(),
        request.getPatientId(),
        request.getEvaluationPeriod(),
        tenantId
    );

    return ResponseEntity.ok(result);
  }

  // Batch evaluate for population (asynchronous)
  @PostMapping("/batch-evaluate")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
  @Audited(action = AuditAction.BATCH, resourceType = "QualityMeasure", encryptPayload = false)
  public ResponseEntity<BatchEvaluationResponse> batchEvaluate(
      @RequestBody BatchEvaluateRequest request,
      @RequestHeader("X-Tenant-ID") String tenantId) {

    CompletableFuture<String> jobId = batchService.batchEvaluate(
        request.getMeasureCode(),
        request.getPatientIds(),
        request.getEvaluationPeriod(),
        tenantId
    );

    return ResponseEntity.accepted()
        .body(new BatchEvaluationResponse(jobId.join(), "PROCESSING"));
  }

  // Get evaluation results
  @GetMapping("/{measureCode}/results")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST')")
  public ResponseEntity<Page<EvaluationResult>> getResults(
      @PathVariable String measureCode,
      @RequestParam(required = false) String patientId,
      @RequestParam(required = false) String status,
      @RequestHeader("X-Tenant-ID") String tenantId,
      Pageable pageable) {

    Page<EvaluationResult> results = qualityMeasureService.getResults(
        measureCode, patientId, status, tenantId, pageable);

    return ResponseEntity.ok(results);
  }

  // Get population summary
  @GetMapping("/{measureCode}/summary")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST')")
  public ResponseEntity<PopulationSummary> getSummary(
      @PathVariable String measureCode,
      @RequestParam Instant evaluationPeriodStart,
      @RequestParam Instant evaluationPeriodEnd,
      @RequestHeader("X-Tenant-ID") String tenantId) {

    PopulationSummary summary = qualityMeasureService.getSummary(
        measureCode, evaluationPeriodStart, evaluationPeriodEnd, tenantId);

    return ResponseEntity.ok(summary);
  }

  // Get all measures for patient
  @GetMapping("/patient/{patientId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
  @Audited(action = AuditAction.READ, resourceType = "QualityMeasure", encryptPayload = true)
  public ResponseEntity<Map<String, List<EvaluationResult>>> getPatientMeasures(
      @PathVariable String patientId,
      @RequestHeader("X-Tenant-ID") String tenantId) {

    Map<String, List<EvaluationResult>> results = qualityMeasureService
        .getPatientMeasures(patientId, tenantId);

    return ResponseEntity.ok(results);
  }

  // Compliance report
  @GetMapping("/reports/compliance")
  @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
  public ResponseEntity<byte[]> getComplianceReport(
      @RequestParam(defaultValue = "JSON") String format,
      @RequestHeader("X-Tenant-ID") String tenantId) {

    byte[] report = reportingService.generateComplianceReport(format, tenantId);

    return ResponseEntity.ok()
        .header("Content-Type", getContentType(format))
        .header("Content-Disposition", "attachment; filename=compliance-report." + format.toLowerCase())
        .body(report);
  }

  // Care gap report
  @GetMapping("/reports/care-gaps")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
  public ResponseEntity<CareGapReport> getCareGapReport(
      @RequestParam(required = false) String priority,
      @RequestHeader("X-Tenant-ID") String tenantId) {

    CareGapReport report = reportingService.getCareGapReport(priority, tenantId);

    return ResponseEntity.ok(report);
  }
}
```

## Testing Quality Measures

### Unit Test: CQL Logic

```java
@SpringBootTest
class HEDIS_CDC_H_Test {

  @Autowired
  private QualityMeasureService measureService;

  private static final EvaluationPeriod MEASUREMENT_PERIOD = EvaluationPeriod.builder()
      .start(Instant.parse("2024-01-01T00:00:00Z"))
      .end(Instant.parse("2024-12-31T23:59:59Z"))
      .build();

  @Test
  void shouldIdentifyPatientInDenominator() {
    // Given: Patient with diabetes, age 50
    String patientId = createPatientWithDiabetes(50);

    // When: Evaluate measure
    EvaluationResult result = measureService.evaluate(
        "CDC-H", patientId, MEASUREMENT_PERIOD, "tenant-001");

    // Then: Patient in denominator
    assertThat(result.isInDenominator()).isTrue();
    assertThat(result.getComplianceStatus()).isIn("COMPLIANT", "NON_COMPLIANT");
  }

  @Test
  void shouldIdentifyCompliantPatient() {
    // Given: Patient with controlled HbA1c (7.5%)
    String patientId = createPatientWithDiabetes(50);
    addHbA1cObservation(patientId, 7.5, MEASUREMENT_PERIOD.getStart().plus(30, ChronoUnit.DAYS));

    // When: Evaluate measure
    EvaluationResult result = measureService.evaluate(
        "CDC-H", patientId, MEASUREMENT_PERIOD, "tenant-001");

    // Then: Patient in numerator (compliant)
    assertThat(result.isInDenominator()).isTrue();
    assertThat(result.isInNumerator()).isTrue();
    assertThat(result.getComplianceStatus()).isEqualTo("COMPLIANT");
    assertThat(result.getCareGap()).isNull();
  }

  @Test
  void shouldIdentifyCareGap_NoTest() {
    // Given: Patient without HbA1c test
    String patientId = createPatientWithDiabetes(50);

    // When: Evaluate measure
    EvaluationResult result = measureService.evaluate(
        "CDC-H", patientId, MEASUREMENT_PERIOD, "tenant-001");

    // Then: Care gap identified
    assertThat(result.isInDenominator()).isTrue();
    assertThat(result.isInNumerator()).isFalse();
    assertThat(result.getComplianceStatus()).isEqualTo("NON_COMPLIANT");
    assertThat(result.getCareGap()).isNotNull();
    assertThat(result.getCareGap().getReason()).contains("No HbA1c test");
    assertThat(result.getCareGap().getNextAction()).contains("Order HbA1c");
  }

  @Test
  void shouldIdentifyCareGap_UncontrolledHbA1c() {
    // Given: Patient with uncontrolled HbA1c (9.2%)
    String patientId = createPatientWithDiabetes(50);
    addHbA1cObservation(patientId, 9.2, MEASUREMENT_PERIOD.getStart().plus(30, ChronoUnit.DAYS));

    // When: Evaluate measure
    EvaluationResult result = measureService.evaluate(
        "CDC-H", patientId, MEASUREMENT_PERIOD, "tenant-001");

    // Then: Care gap with HIGH priority
    assertThat(result.isInDenominator()).isTrue();
    assertThat(result.isInNumerator()).isFalse();
    assertThat(result.getCareGap().getPriority()).isEqualTo("HIGH");
    assertThat(result.getCareGap().getNextAction()).contains("medication adjustment");
  }

  @Test
  void shouldExcludeESRDPatient() {
    // Given: Patient with ESRD
    String patientId = createPatientWithDiabetes(50);
    addCondition(patientId, "End Stage Renal Disease");

    // When: Evaluate measure
    EvaluationResult result = measureService.evaluate(
        "CDC-H", patientId, MEASUREMENT_PERIOD, "tenant-001");

    // Then: Patient excluded
    assertThat(result.isExcluded()).isTrue();
    assertThat(result.getComplianceStatus()).isEqualTo("EXCLUDED");
  }
}
```

### Integration Test: End-to-End

```java
@SpringBootTest
@DirtiesContext
class QualityMeasureIntegrationTest {

  @Autowired
  private QualityMeasureService qualityMeasureService;

  @Test
  void shouldEvaluatePopulationMeasure() {
    // Given: Population of 100 patients
    List<String> patientIds = createTestPopulation(100);

    // When: Batch evaluate
    CompletableFuture<String> jobId = qualityMeasureService.batchEvaluate(
        "CDC-H", patientIds, MEASUREMENT_PERIOD, "tenant-001");

    // Wait for completion
    await().atMost(30, TimeUnit.SECONDS).until(() -> isJobComplete(jobId.join()));

    // Then: Population summary calculated
    PopulationSummary summary = qualityMeasureService.getSummary(
        "CDC-H", MEASUREMENT_PERIOD.getStart(), MEASUREMENT_PERIOD.getEnd(), "tenant-001");

    assertThat(summary.getDenominatorCount()).isEqualTo(95);  // 5 excluded
    assertThat(summary.getNumeratorCount()).isEqualTo(72);
    assertThat(summary.getComplianceRate()).isCloseTo(0.758, within(0.01));
    assertThat(summary.getTotalCareGaps()).isEqualTo(23);
  }
}
```

## Common Quality Measure Patterns

### Pattern: Multiple Observation Check

```cql
// Check if patient has at least 2 HbA1c tests
define "Has Multiple HbA1c Tests":
  Count([Observation: "HbA1c Laboratory Test"] O
    where O.status in {'final', 'amended'}
    and O.effective during "Measurement Period") >= 2
```

### Pattern: Value Range Check

```cql
// Check if value is within normal range
define "Normal Blood Pressure":
  [Observation: "Blood Pressure"] BP
    where (BP.component.where(code ~ "Systolic BP").value as Quantity).value < 140
    and (BP.component.where(code ~ "Diastolic BP").value as Quantity).value < 90
```

### Pattern: Medication Check

```cql
// Check for active statin prescription
define "On Statin Therapy":
  exists [MedicationRequest: "Statin Medications"] M
    where M.status = 'active'
    and M.authoredOn during "Measurement Period"
```

## Related Documentation

- **CQL Specification:** https://cql.hl7.org/
- **HEDIS Measures:** https://www.ncqa.org/hedis/
- **CQL Measure Builder Agent:** `.claude/plugins/hdim-accelerator/agents/cql-measure-builder.md`
- **Add-CQL-Measure Command:** `.claude/plugins/hdim-accelerator/commands/add-cql-measure.md`

---

**Last Updated:** January 21, 2026
**Version:** 1.0.0
**Supported Measures:** HEDIS, CMS, Custom
