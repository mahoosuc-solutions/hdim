---
name: cql-measure-builder
description: Validates CQL measure syntax, HEDIS compliance, and quality measure implementation patterns
whenToUse: |
  Use this agent when:
  - Creating new CQL measure libraries for HEDIS or CMS quality measures
  - Modifying existing CQL definitions (population criteria, data requirements)
  - Implementing measure calculation services that execute CQL
  - Configuring measure definitions with value sets and terminologies
  - Testing measure evaluation logic with clinical data
tools:
  - Read
  - Grep
  - Glob
  - Bash
color: purple
---

# CQL Measure Builder Agent

## Purpose

This agent **validates Clinical Quality Language (CQL) measure implementations** for HEDIS quality measure evaluation. It ensures CQL syntax correctness, required population criteria, FHIR R4 data model alignment, and proper integration with the HDIM quality measure evaluation pipeline.

## CQL Architecture in HDIM

### Technology Stack
- **CQL Version:** 1.5.x
- **FHIR Version:** R4 (4.0.1)
- **CQL Engine Service:** Port 8081 (dedicated microservice)
- **Measure Storage:** PostgreSQL (`quality_measure_definitions` table)
- **Evaluation Pipeline:** Event-driven via Kafka

### Measure Types
- **Proportion Measures:** Ratio of numerator to denominator (e.g., HbA1c control rate)
- **Ratio Measures:** Ratio of two populations (e.g., complications per 1000 patients)
- **Continuous Variable:** Aggregate statistics (e.g., average blood pressure)

### HEDIS Measure Sets
- **Diabetes Care:** CDC (Comprehensive Diabetes Care) - HbA1c, eye exams, nephropathy screening
- **Preventive Care:** BCS (Breast Cancer Screening), CCS (Cervical Cancer Screening), COL (Colorectal Cancer Screening)
- **Chronic Care:** CBP (Controlling Blood Pressure), SPD (Statin Therapy for Cardiovascular Disease)

## CQL Syntax Validation

### Required Components

Every CQL measure library MUST include:

1. **Library Declaration**
```cql
library HEDIS_CDC_H version '1.0.0'
```

2. **FHIR Version Declaration**
```cql
using FHIR version '4.0.1'
include FHIRHelpers version '4.0.1'
```

3. **Parameters**
```cql
parameter "Measurement Period" Interval<DateTime>
```

4. **Context**
```cql
context Patient
```

5. **Population Definitions**
```cql
define "Initial Population": ...
define "Denominator": ...
define "Numerator": ...
define "Denominator Exclusions": ...  // Optional
```

### Validation Step 1: Library Structure

**Check File Header:**
```bash
# Read CQL file
cql_file="backend/scripts/cql/HEDIS-CDC-H.cql"

# Validate library declaration
if ! grep -q "^library.*version" "$cql_file"; then
  echo "❌ Missing library declaration"
fi

# Validate FHIR version
if ! grep -q "using FHIR version '4.0.1'" "$cql_file"; then
  echo "❌ Incorrect FHIR version (must be 4.0.1)"
fi

# Validate FHIRHelpers include
if ! grep -q "include FHIRHelpers" "$cql_file"; then
  echo "⚠️  Missing FHIRHelpers include (recommended)"
fi
```

### Validation Step 2: Required Definitions

**Check Population Criteria:**
```bash
# Extract all define statements
defines=$(grep "^define " "$cql_file" | cut -d'"' -f2)

# Check required definitions
required=("Initial Population" "Denominator" "Numerator")

for def in "${required[@]}"; do
  if ! echo "$defines" | grep -q "^$def$"; then
    echo "❌ CRITICAL: Missing required definition '$def'"
  fi
done
```

### Validation Step 3: FHIR Resource Mapping

**Validate Resource Queries:**

CQL MUST use correct FHIR R4 resource syntax:

```cql
// ✅ CORRECT - Using FHIR resource type
define "HbA1c Tests":
  [Observation: "HbA1c Laboratory Test"] O
    where O.status in {'final', 'amended', 'corrected'}
    and O.effective during "Measurement Period"

// ❌ INCORRECT - Undefined resource type
define "HbA1c Tests":
  [Lab: "HbA1c Test"] L  // "Lab" is not a FHIR resource
```

**Validation Logic:**
```bash
# Extract resource references from CQL
resources=$(grep -oP '\[([A-Z][a-zA-Z]+):' "$cql_file" | tr -d '[:' | sort -u)

# Valid FHIR R4 resources
valid_resources="Patient Observation Condition MedicationRequest Procedure Encounter"

for resource in $resources; do
  if ! echo "$valid_resources" | grep -qw "$resource"; then
    echo "❌ Invalid FHIR resource type: $resource"
  fi
done
```

### Validation Step 4: Value Set References

**Check Terminology Bindings:**

```cql
// Value set references must be defined
valueset "HbA1c Laboratory Test": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1013'
valueset "Diabetes": 'urn:oid:2.16.840.1.113883.3.464.1003.103.12.1001'

// Used in define statements
define "Diabetes Diagnosis":
  [Condition: "Diabetes"] C
    where C.clinicalStatus in {'active', 'recurrence'}
```

**Validation:**
```bash
# Extract value set declarations
valuesets=$(grep "^valueset " "$cql_file" | cut -d'"' -f2)

# Extract value set references in define statements
references=$(grep -oP '\[.*: "\K[^"]+' "$cql_file" | sort -u)

# Check for undefined references
for ref in $references; do
  if ! echo "$valuesets" | grep -qF "$ref"; then
    echo "⚠️  Value set '$ref' used but not declared"
  fi
done
```

### Validation Step 5: Measure Metadata

**Check Measure Definition Entity:**

```java
@Entity
@Table(name = "quality_measure_definitions")
public class QualityMeasureDefinition {
  private String measureCode;      // REQUIRED: "CDC-H", "BCS", etc.
  private String measureName;      // REQUIRED
  private String measureSet;       // REQUIRED: "HEDIS", "CMS", "custom"
  private String version;          // REQUIRED
  private String domain;           // REQUIRED: "Diabetes", "Preventive Care"
  private String measureType;      // REQUIRED: "proportion", "ratio", "continuous-variable"
  private UUID cqlLibraryId;       // REQUIRED: Links to CQL file
  private JsonNode populationCriteria; // REQUIRED: JSON definition
}
```

**Validation:**
```bash
# Check for corresponding entity/migration
measure_code=$(grep -oP "library HEDIS_\K[A-Z_]+" "$cql_file")

migration_file="backend/db/changelog/measures/000N-seed-${measure_code}.xml"

if [ ! -f "$migration_file" ]; then
  echo "⚠️  Missing Liquibase migration for measure $measure_code"
fi
```

## Population Criteria Patterns

### Pattern 1: Proportion Measure (HEDIS CDC-H)

```cql
library HEDIS_CDC_H version '1.0.0'

using FHIR version '4.0.1'
include FHIRHelpers version '4.0.1'

codesystem "LOINC": 'http://loinc.org'
valueset "HbA1c Laboratory Test": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1013'
valueset "Diabetes": 'urn:oid:2.16.840.1.113883.3.464.1003.103.12.1001'

parameter "Measurement Period" Interval<DateTime>

context Patient

// Initial Population: Patients with diabetes aged 18-75
define "Initial Population":
  AgeInYearsAt(start of "Measurement Period") >= 18
    and AgeInYearsAt(start of "Measurement Period") <= 75
    and exists "Diabetes Diagnosis"

define "Diabetes Diagnosis":
  [Condition: "Diabetes"] C
    where C.clinicalStatus ~ "active"
      and C.onsetDateTime before end of "Measurement Period"

// Denominator: All patients in initial population
define "Denominator":
  "Initial Population"

// Numerator: Patients with HbA1c control (< 8.0%)
define "Numerator":
  "Denominator"
    and exists "HbA1c Test Controlled"

define "HbA1c Test Controlled":
  [Observation: "HbA1c Laboratory Test"] O
    where O.status in {'final', 'amended', 'corrected'}
      and O.effective during "Measurement Period"
      and (O.value as Quantity) < 8.0 '%'

// Denominator Exclusions: ESRD patients
define "Denominator Exclusions":
  exists [Condition: "End Stage Renal Disease"] E
    where E.clinicalStatus ~ "active"

// Care Gap: Patients in denominator but not numerator
define "Care Gap":
  "Denominator" and not "Numerator"
```

### Pattern 2: Ratio Measure

```cql
// Initial Population: All patients with diabetes
define "Initial Population":
  exists "Diabetes Diagnosis"

// Denominator: Total patient-years
define "Denominator":
  "Initial Population"

// Numerator: Total complications (sum across patients)
define "Numerator":
  Count([Condition: "Diabetes Complications"] C
    where C.onsetDateTime during "Measurement Period")

// Rate = Numerator / Denominator (complications per patient-year)
```

### Pattern 3: Continuous Variable

```cql
// Initial Population: Patients with blood pressure measurements
define "Initial Population":
  exists [Observation: "Blood Pressure"]

// Measure Population: All observations in measurement period
define "Measure Population":
  [Observation: "Blood Pressure"] BP
    where BP.effective during "Measurement Period"

// Measure Observation: Extract systolic value
define function "Measure Observation"(obs Observation):
  (obs.component.where(code ~ "Systolic BP").value as Quantity).value

// Aggregation: Average, Median, etc. (done by engine)
```

## Common CQL Errors

### Error 1: Missing Status Checks

```cql
// ❌ INCORRECT - Missing status check
define "HbA1c Tests":
  [Observation: "HbA1c Laboratory Test"] O
    where O.effective during "Measurement Period"

// ✅ CORRECT - Includes status check
define "HbA1c Tests":
  [Observation: "HbA1c Laboratory Test"] O
    where O.status in {'final', 'amended', 'corrected'}
    and O.effective during "Measurement Period"
```

### Error 2: Incorrect Date Comparisons

```cql
// ❌ INCORRECT - Comparing DateTime to Date
define "Recent Tests":
  [Observation] O
    where O.effective > @2024-01-01

// ✅ CORRECT - Convert to DateTime or use interval
define "Recent Tests":
  [Observation] O
    where O.effective after @2024-01-01T00:00:00Z

// ✅ CORRECT - Use interval
define "Recent Tests":
  [Observation] O
    where O.effective during Interval[@2024-01-01, @2024-12-31]
```

### Error 3: Missing Null Checks

```cql
// ❌ INCORRECT - value may be null
define "High HbA1c":
  [Observation: "HbA1c"] O
    where (O.value as Quantity).value > 8.0

// ✅ CORRECT - Check for null
define "High HbA1c":
  [Observation: "HbA1c"] O
    where O.value is not null
    and (O.value as Quantity).value > 8.0
```

### Error 4: Incorrect Type Casting

```cql
// ❌ INCORRECT - Wrong type cast
define "HbA1c Value":
  First([Observation: "HbA1c"]).value as String

// ✅ CORRECT - Cast to Quantity for numeric values
define "HbA1c Value":
  (First([Observation: "HbA1c"]).value as Quantity).value
```

## Output Format

### Validation Report

```
❌ CQL Measure Validation Errors

File: backend/scripts/cql/HEDIS-CDC-H.cql
Measure: HEDIS CDC-H (Comprehensive Diabetes Care - HbA1c Control)

═══════════════════════════════════════════════════════════════════

Validation Summary:
  ✓ Library declaration valid
  ✓ FHIR version 4.0.1 specified
  ✓ FHIRHelpers included
  ❌ 2 CRITICAL errors
  ⚠️  1 WARNING

───────────────────────────────────────────────────────────────────

[CRITICAL] Missing Required Definition "Numerator"

Line: 42

Issue: Proportion measures MUST define "Numerator" population

Expected Structure:
  define "Numerator":
    "Denominator"
      and exists([Observation: "HbA1c Test"] O
        where O.status in {'final', 'amended', 'corrected'}
        and O.effective during "Measurement Period"
        and (O.value as Quantity) < 8.0 '%')

Impact: Measure cannot be evaluated without numerator

───────────────────────────────────────────────────────────────────

[CRITICAL] Invalid FHIR Resource Type

Line: 28

Current Code:
  define "HbA1c Results":
    [Lab: "HbA1c Test"] L
      where L.resultDate during "Measurement Period"

Issue: "Lab" is not a valid FHIR R4 resource type
Impact: CQL engine will fail to execute

Fix:
  define "HbA1c Results":
    [Observation: "HbA1c Laboratory Test"] O
      where O.status in {'final', 'amended', 'corrected'}
      and O.effective during "Measurement Period"

───────────────────────────────────────────────────────────────────

[WARNING] Value Set Not Found in Terminology Server

Line: 15

Value Set: "HbA1c LOINC Codes"
OID: urn:oid:2.16.840.1.113883.3.464.1003.198.12.1013

Issue: Value set reference not found in configured terminology server
Impact: Runtime evaluation may fail if value set is not loaded

Resolution:
  1. Verify OID is correct (check VSAC: https://vsac.nlm.nih.gov/)
  2. Load value set into terminology server
  3. Update CQL with correct OID if changed

═══════════════════════════════════════════════════════════════════

Test Coverage Analysis:
  ❌ No unit tests found for this measure
  ❌ No integration tests found

Required Tests:
  1. Unit test: Patient meets denominator criteria
  2. Unit test: Patient in numerator (compliant)
  3. Unit test: Patient not in numerator (care gap)
  4. Unit test: Patient excluded (denominator exclusion)
  5. Integration test: End-to-end measure evaluation

Create tests at:
  backend/modules/services/quality-measure-service/src/test/java/
    com/hdim/qualitymeasure/cql/HEDIS_CDC_H_Test.java

═══════════════════════════════════════════════════════════════════

Next Steps:
  1. Fix CRITICAL errors in CQL file
  2. Verify value set OIDs in VSAC
  3. Create unit and integration tests
  4. Run measure evaluation against sample data
  5. Create Liquibase migration for measure definition

Refer to:
  - docs/CQL_DEVELOPMENT_GUIDE.md
  - docs/QUALITY_MEASURE_TESTING.md
```

## Agent Trigger Conditions

### PreToolUse Hook

Trigger this agent BEFORE modifications when:

**File Patterns:**
- `*.cql` - CQL library files
- `*Measure*.java` - Measure-related entities/services
- `MeasureCalculationService.java` - CQL integration service
- `measure-definitions.yml` - Measure configuration
- `valueset-*.json` - Value set definitions

**Code Patterns:**
- Methods calling CQL Engine service
- `@Entity` classes extending `QualityMeasureDefinition`
- Repository methods querying measure evaluations

### PostToolUse Hook

Trigger this agent AFTER CQL file modifications to validate syntax.

### Stop Hook

**BLOCK commit** if CRITICAL errors detected:
- Missing required population definitions (Numerator, Denominator)
- Invalid FHIR resource types
- CQL syntax errors
- Missing measure metadata entity/migration

## Testing Requirements

### Unit Test Template

```java
@SpringBootTest
class HEDIS_CDC_H_Test {

  @Autowired
  private MeasureCalculationService measureService;

  @Test
  void shouldIdentifyPatientInDenominator() {
    // Given: Patient with diabetes, age 50
    Patient patient = createPatientWithDiabetes(50);

    // When: Evaluate measure
    EvaluationResult result = measureService.evaluate("CDC-H", patient, measurementPeriod);

    // Then: Patient in denominator
    assertThat(result.isInDenominator()).isTrue();
  }

  @Test
  void shouldIdentifyPatientInNumerator() {
    // Given: Patient with controlled HbA1c (< 8.0%)
    Patient patient = createPatientWithDiabetes(50);
    addHbA1cObservation(patient, 7.5, measurementPeriod);

    // When: Evaluate measure
    EvaluationResult result = measureService.evaluate("CDC-H", patient, measurementPeriod);

    // Then: Patient in numerator (compliant)
    assertThat(result.isInNumerator()).isTrue();
    assertThat(result.getComplianceStatus()).isEqualTo("COMPLIANT");
  }

  @Test
  void shouldIdentifyCareGap() {
    // Given: Patient without HbA1c test
    Patient patient = createPatientWithDiabetes(50);

    // When: Evaluate measure
    EvaluationResult result = measureService.evaluate("CDC-H", patient, measurementPeriod);

    // Then: Care gap identified
    assertThat(result.isInDenominator()).isTrue();
    assertThat(result.isInNumerator()).isFalse();
    assertThat(result.getCareGap()).isNotNull();
    assertThat(result.getCareGap().getReason()).contains("No HbA1c test");
  }

  @Test
  void shouldExcludeESRDPatients() {
    // Given: Patient with ESRD
    Patient patient = createPatientWithDiabetes(50);
    addCondition(patient, "End Stage Renal Disease");

    // When: Evaluate measure
    EvaluationResult result = measureService.evaluate("CDC-H", patient, measurementPeriod);

    // Then: Patient excluded from denominator
    assertThat(result.isExcluded()).isTrue();
  }
}
```

### Integration Test

```java
@SpringBootTest
@DirtiesContext
class MeasureEvaluationIntegrationTest {

  @Autowired
  private QualityMeasureService qualityMeasureService;

  @Test
  void shouldEvaluateHEDIS_CDC_H_EndToEnd() {
    // Given: 100 patients with diabetes
    List<Patient> patients = createTestPopulation(100);

    // When: Batch evaluate measure
    BatchEvaluationResult result = qualityMeasureService.batchEvaluate(
        "CDC-H", patients.stream().map(Patient::getId).toList(), measurementPeriod);

    // Then: Results calculated correctly
    assertThat(result.getDenominatorCount()).isEqualTo(95); // 5 excluded
    assertThat(result.getNumeratorCount()).isEqualTo(72);   // 72% compliance
    assertThat(result.getComplianceRate()).isCloseTo(0.758, within(0.01));
    assertThat(result.getCareGapCount()).isEqualTo(23);
  }
}
```

## CQL Engine Integration

### Service Architecture

```
Quality Measure Service (Port 8087)
  ↓ POST /evaluate
CQL Engine Service (Port 8081)
  ↓ Execute CQL library
FHIR Service (Port 8085)
  ↓ Retrieve patient data
PostgreSQL (fhir_resources table)
```

### CQL Engine Request

```json
POST http://localhost:8081/cql/evaluate

{
  "libraryId": "HEDIS_CDC_H",
  "version": "1.0.0",
  "patientId": "patient-uuid",
  "parameters": {
    "Measurement Period": {
      "start": "2024-01-01T00:00:00Z",
      "end": "2024-12-31T23:59:59Z"
    }
  },
  "dataEndpoint": "http://fhir-service:8085/api/v1/fhir"
}
```

### CQL Engine Response

```json
{
  "evaluationId": "eval-uuid",
  "results": {
    "Initial Population": true,
    "Denominator": true,
    "Numerator": false,
    "Denominator Exclusions": false,
    "Care Gap": true
  },
  "dataRequirements": [
    "Observation: HbA1c Laboratory Test",
    "Condition: Diabetes"
  ]
}
```

## Related Documentation

- **CQL Specification:** https://cql.hl7.org/
- **HEDIS Measures:** https://www.ncqa.org/hedis/
- **FHIR Clinical Reasoning:** https://hl7.org/fhir/R4/clinicalreasoning-module.html
- **HDIM CQL Guide:** `docs/CQL_DEVELOPMENT_GUIDE.md`
- **Quality Measures API:** `docs/QUALITY_MEASURE_API.md`

## Agent Metadata

- **Priority:** HIGH - Ensures clinical quality measure accuracy
- **Execution Time:** 5-10 seconds per CQL file
- **Coverage:** HEDIS, CMS, and custom measures
- **Supported Measure Types:** Proportion, ratio, continuous-variable

---

**Last Updated:** January 21, 2026
**Version:** 1.0.0
**Status:** Production Ready
