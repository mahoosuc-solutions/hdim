---
name: add-cql-measure
description: Scaffolds a new HEDIS or CMS quality measure with CQL library, entity, service, tests, and database migration
arguments:
  - name: measure_code
    description: HEDIS/CMS measure code (e.g., "CDC-H", "BCS", "COL")
    required: true
  - name: measure_name
    description: Full measure name (e.g., "Comprehensive Diabetes Care - HbA1c Control")
    required: true
  - name: measure_set
    description: Measure set (HEDIS, CMS, custom)
    required: false
    default: "HEDIS"
---

# Add CQL Measure Command

## Purpose

This command **scaffolds a complete HEDIS or CMS quality measure implementation** including:
- CQL library file with population criteria
- Java entity for measure definition
- Service integration with CQL Engine
- Unit and integration tests
- Liquibase migration for database seeding

**Time Savings:** ~2 hours of manual scaffolding → 10 minutes with automated generation

## Usage

```bash
# Basic usage (HEDIS measure)
/add-cql-measure CDC-H "Comprehensive Diabetes Care - HbA1c Control"

# CMS measure
/add-cql-measure CMS-122 "Diabetes HbA1c Poor Control" CMS

# Custom measure
/add-cql-measure CUSTOM-BP "Blood Pressure Control" custom
```

## Generated Files

### 1. CQL Library

**Location:** `backend/scripts/cql/{{MEASURE_SET}}-{{MEASURE_CODE}}.cql`

**Template:**
```cql
library {{MEASURE_SET}}_{{MEASURE_CODE}} version '1.0.0'

using FHIR version '4.0.1'
include FHIRHelpers version '4.0.1'

// ====================
// Terminology
// ====================

codesystem "LOINC": 'http://loinc.org'
codesystem "SNOMEDCT": 'http://snomed.info/sct'

// TODO: Add value set declarations
// valueset "Example Value Set": 'urn:oid:2.16.840.1.113883.3.464.XXXXX'

parameter "Measurement Period" Interval<DateTime>

context Patient

// ====================
// Population Criteria
// ====================

// Initial Population
// TODO: Define eligibility criteria (age, diagnosis, etc.)
define "Initial Population":
  AgeInYearsAt(start of "Measurement Period") >= 18
    and AgeInYearsAt(start of "Measurement Period") <= 75
    // TODO: Add additional eligibility criteria

// Denominator
define "Denominator":
  "Initial Population"
    // TODO: Add denominator-specific criteria

// Numerator
// TODO: Define compliance criteria
define "Numerator":
  "Denominator"
    // TODO: Add numerator criteria (what makes a patient compliant?)

// Denominator Exclusions (Optional)
define "Denominator Exclusions":
  false
  // TODO: Add exclusion criteria (conditions that exclude patients from measure)

// Numerator Exclusions (Optional)
define "Numerator Exclusions":
  false
  // TODO: Add numerator exclusion criteria if applicable

// ====================
// Care Gap Logic
// ====================

define "Care Gap":
  "Denominator"
    and not "Numerator"
    and not "Denominator Exclusions"

define "Care Gap Reason":
  if "Care Gap" then
    // TODO: Provide specific reason for care gap
    'TODO: Implement care gap reason logic'
  else null

define "Care Gap Priority":
  if "Care Gap" then
    // TODO: Assign priority (HIGH, MEDIUM, LOW) based on clinical severity
    'MEDIUM'
  else null

define "Next Action":
  if "Care Gap" then
    // TODO: Provide actionable recommendation
    'TODO: Implement next action recommendation'
  else null

define "Due Date":
  if "Care Gap" then
    // TODO: Calculate due date for action
    Today() + 30 days
  else null

// ====================
// Helper Definitions
// ====================

// TODO: Add helper definitions for:
// - Condition/diagnosis checks
// - Observation/lab result retrieval
// - Medication checks
// - Procedure checks
// - Date range calculations

// Example helper:
// define "Active Diabetes":
//   [Condition: "Diabetes"] C
//     where C.clinicalStatus ~ "active"
//       and C.verificationStatus ~ "confirmed"
```

### 2. Measure Definition Migration

**Location:** `backend/db/changelog/quality-measures/{{SEQUENCE}}-seed-{{MEASURE_CODE}}-definition.xml`

**Template:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

  <changeSet id="seed-{{MEASURE_CODE}}-definition" author="hdim-accelerator">
    <comment>Seed {{MEASURE_SET}} {{MEASURE_CODE}} measure definition</comment>

    <insert tableName="quality_measure_definitions">
      <column name="id" valueComputed="gen_random_uuid()"/>
      <column name="tenant_id" value="default"/>
      <column name="measure_code" value="{{MEASURE_CODE}}"/>
      <column name="measure_name" value="{{MEASURE_NAME}}"/>
      <column name="measure_set" value="{{MEASURE_SET}}"/>
      <column name="version" value="1.0.0"/>
      <column name="domain" value="TODO: Set domain (e.g., Diabetes, Preventive Care)"/>
      <column name="measure_type" value="proportion"/>
      <column name="population_criteria" value='{
        "initialPopulation": "Initial Population",
        "denominator": "Denominator",
        "numerator": "Numerator",
        "denominatorExclusions": "Denominator Exclusions",
        "numeratorExclusions": "Numerator Exclusions"
      }'/>
      <column name="active" valueBoolean="true"/>
      <column name="effective_period_start" valueDate="2024-01-01"/>
      <column name="effective_period_end" valueDate="2025-12-31"/>
      <column name="created_at" valueComputed="NOW()"/>
      <column name="updated_at" valueComputed="NOW()"/>
    </insert>

    <rollback>
      <delete tableName="quality_measure_definitions">
        <where>measure_code = '{{MEASURE_CODE}}' AND tenant_id = 'default'</where>
      </delete>
    </rollback>
  </changeSet>
</databaseChangeLog>
```

### 3. Unit Test

**Location:** `backend/modules/services/quality-measure-service/src/test/java/com/hdim/qualitymeasure/cql/{{MEASURE_SET}}_{{MEASURE_CODE}}_Test.java`

**Template:**
```java
package com.hdim.qualitymeasure.cql;

import com.hdim.qualitymeasure.domain.EvaluationPeriod;
import com.hdim.qualitymeasure.domain.EvaluationResult;
import com.hdim.qualitymeasure.service.QualityMeasureService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class {{MEASURE_SET}}_{{MEASURE_CODE}}_Test {

  @Autowired
  private QualityMeasureService measureService;

  private static final EvaluationPeriod MEASUREMENT_PERIOD = EvaluationPeriod.builder()
      .start(Instant.parse("2024-01-01T00:00:00Z"))
      .end(Instant.parse("2024-12-31T23:59:59Z"))
      .build();

  @Test
  void shouldIdentifyPatientInDenominator() {
    // Given: Patient meeting denominator criteria
    // TODO: Create test patient data
    String patientId = "test-patient-001";

    // When: Evaluate measure
    EvaluationResult result = measureService.evaluate(
        "{{MEASURE_CODE}}", patientId, MEASUREMENT_PERIOD, "tenant-001");

    // Then: Patient in denominator
    assertThat(result.isInDenominator()).isTrue();
  }

  @Test
  void shouldIdentifyCompliantPatient() {
    // Given: Patient meeting numerator criteria
    // TODO: Create compliant test patient data
    String patientId = "test-patient-compliant";

    // When: Evaluate measure
    EvaluationResult result = measureService.evaluate(
        "{{MEASURE_CODE}}", patientId, MEASUREMENT_PERIOD, "tenant-001");

    // Then: Patient in numerator (compliant)
    assertThat(result.isInDenominator()).isTrue();
    assertThat(result.isInNumerator()).isTrue();
    assertThat(result.getComplianceStatus()).isEqualTo("COMPLIANT");
    assertThat(result.getCareGap()).isNull();
  }

  @Test
  void shouldIdentifyCareGap() {
    // Given: Patient in denominator but not numerator
    // TODO: Create non-compliant test patient data
    String patientId = "test-patient-gap";

    // When: Evaluate measure
    EvaluationResult result = measureService.evaluate(
        "{{MEASURE_CODE}}", patientId, MEASUREMENT_PERIOD, "tenant-001");

    // Then: Care gap identified
    assertThat(result.isInDenominator()).isTrue();
    assertThat(result.isInNumerator()).isFalse();
    assertThat(result.getComplianceStatus()).isEqualTo("NON_COMPLIANT");
    assertThat(result.getCareGap()).isNotNull();
    assertThat(result.getCareGap().getReason()).isNotEmpty();
    assertThat(result.getCareGap().getNextAction()).isNotEmpty();
  }

  @Test
  void shouldExcludePatient() {
    // Given: Patient with exclusion criteria
    // TODO: Create excluded test patient data
    String patientId = "test-patient-excluded";

    // When: Evaluate measure
    EvaluationResult result = measureService.evaluate(
        "{{MEASURE_CODE}}", patientId, MEASUREMENT_PERIOD, "tenant-001");

    // Then: Patient excluded from denominator
    assertThat(result.isExcluded()).isTrue();
    assertThat(result.getComplianceStatus()).isEqualTo("EXCLUDED");
  }

  // TODO: Add edge case tests:
  // - Patient outside age range
  // - Missing required data elements
  // - Multiple observations in period
  // - Boundary date conditions
}
```

### 4. Integration Test

**Location:** `backend/modules/services/quality-measure-service/src/test/java/com/hdim/qualitymeasure/integration/{{MEASURE_SET}}_{{MEASURE_CODE}}_IntegrationTest.java`

**Template:**
```java
package com.hdim.qualitymeasure.integration;

import com.hdim.qualitymeasure.domain.EvaluationPeriod;
import com.hdim.qualitymeasure.domain.PopulationSummary;
import com.hdim.qualitymeasure.service.QualityMeasureService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@DirtiesContext
class {{MEASURE_SET}}_{{MEASURE_CODE}}_IntegrationTest {

  @Autowired
  private QualityMeasureService qualityMeasureService;

  private static final EvaluationPeriod MEASUREMENT_PERIOD = EvaluationPeriod.builder()
      .start(Instant.parse("2024-01-01T00:00:00Z"))
      .end(Instant.parse("2024-12-31T23:59:59Z"))
      .build();

  @Test
  void shouldEvaluatePopulationMeasure() {
    // Given: Population of test patients
    // TODO: Create test population (100 patients with varying compliance)
    int populationSize = 100;

    // When: Batch evaluate
    CompletableFuture<String> jobId = qualityMeasureService.batchEvaluate(
        "{{MEASURE_CODE}}",
        createTestPatientIds(populationSize),
        MEASUREMENT_PERIOD,
        "tenant-001"
    );

    // Wait for completion (max 30 seconds)
    await().atMost(30, TimeUnit.SECONDS).until(() -> isJobComplete(jobId.join()));

    // Then: Population summary calculated
    PopulationSummary summary = qualityMeasureService.getSummary(
        "{{MEASURE_CODE}}",
        MEASUREMENT_PERIOD.getStart(),
        MEASUREMENT_PERIOD.getEnd(),
        "tenant-001"
    );

    // TODO: Adjust expected values based on test data
    assertThat(summary.getDenominatorCount()).isGreaterThan(0);
    assertThat(summary.getNumeratorCount()).isGreaterThan(0);
    assertThat(summary.getComplianceRate()).isBetween(0.0, 1.0);
    assertThat(summary.getTotalCareGaps()).isGreaterThanOrEqualTo(0);
  }

  // TODO: Add integration tests for:
  // - CQL Engine communication
  // - FHIR data retrieval
  // - Event publishing
  // - Projection updates
}
```

### 5. Changelog Master Update

**Location:** `backend/db/changelog/db.changelog-master.xml`

**Append:**
```xml
<include file="quality-measures/{{SEQUENCE}}-seed-{{MEASURE_CODE}}-definition.xml"
         relativeToChangelogFile="true"/>
```

## Post-Generation Steps

After running this command, the developer should:

### 1. Implement CQL Logic

Open `backend/scripts/cql/{{MEASURE_SET}}-{{MEASURE_CODE}}.cql` and:

- [ ] Replace `TODO` comments with actual CQL definitions
- [ ] Add correct value set OIDs from VSAC (https://vsac.nlm.nih.gov/)
- [ ] Implement population criteria (Initial Population, Denominator, Numerator)
- [ ] Add exclusion criteria if applicable
- [ ] Implement care gap logic (reason, priority, next action, due date)
- [ ] Add helper definitions for common queries

**Example Value Set Lookup:**
```bash
# Search VSAC for HbA1c test codes
# https://vsac.nlm.nih.gov/
# Search: "HbA1c"
# Result: urn:oid:2.16.840.1.113883.3.464.1003.198.12.1013
```

### 2. Update Migration

Edit `backend/db/changelog/quality-measures/{{SEQUENCE}}-seed-{{MEASURE_CODE}}-definition.xml`:

- [ ] Set correct `domain` value (e.g., "Diabetes", "Cardiovascular", "Preventive Care")
- [ ] Verify `measure_type` (proportion, ratio, continuous-variable)
- [ ] Update `effective_period_start` and `effective_period_end` dates
- [ ] Add stratification criteria to `population_criteria` JSON if needed

### 3. Implement Tests

Edit test files to create realistic test data:

**Unit Test:**
- [ ] Create test patients with specific conditions/observations
- [ ] Use FHIR test data builders
- [ ] Test all population criteria paths
- [ ] Test edge cases (boundary dates, missing data)

**Integration Test:**
- [ ] Create diverse test population (compliant, non-compliant, excluded)
- [ ] Verify CQL Engine integration
- [ ] Test event publishing
- [ ] Validate projection updates

### 4. Run Validation

```bash
# Run CQL Measure Builder Agent to validate syntax
# (Automatically triggered by hooks on file save)

# Run unit tests
cd backend
./gradlew :modules:services:quality-measure-service:test \
  --tests "{{MEASURE_SET}}_{{MEASURE_CODE}}_Test"

# Run entity-migration validation
./gradlew test --tests "*EntityMigrationValidationTest"

# Apply migration
./gradlew :modules:services:quality-measure-service:update

# Run integration test
./gradlew :modules:services:quality-measure-service:test \
  --tests "{{MEASURE_SET}}_{{MEASURE_CODE}}_IntegrationTest"
```

### 5. Deploy and Test

```bash
# Build service
./gradlew :modules:services:quality-measure-service:bootJar

# Build Docker image
docker compose build quality-measure-service

# Start service
docker compose up -d quality-measure-service

# Test evaluation endpoint
curl -X POST http://localhost:8087/api/v1/quality-measures/evaluate \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -d '{
    "measureCode": "{{MEASURE_CODE}}",
    "patientId": "test-patient-001",
    "evaluationPeriod": {
      "start": "2024-01-01T00:00:00Z",
      "end": "2024-12-31T23:59:59Z"
    }
  }'
```

## Command Implementation

This command generates files using template substitution:

**Variables:**
- `{{MEASURE_CODE}}` - User-provided measure code
- `{{MEASURE_NAME}}` - User-provided measure name
- `{{MEASURE_SET}}` - User-provided or default "HEDIS"
- `{{SEQUENCE}}` - Auto-incremented migration sequence number

**Logic:**
1. Validate measure code format (alphanumeric, hyphens allowed)
2. Check for existing measure (avoid duplicates)
3. Generate sequence number for migration
4. Create CQL file from template
5. Create migration file with seed data
6. Create test files with boilerplate
7. Update changelog master
8. Trigger CQL Measure Builder Agent for validation

## Example Output

```
✅ Successfully scaffolded HEDIS CDC-H measure

Generated files:
  1. backend/scripts/cql/HEDIS-CDC-H.cql
  2. backend/db/changelog/quality-measures/0042-seed-CDC-H-definition.xml
  3. backend/modules/services/quality-measure-service/src/test/java/
     com/hdim/qualitymeasure/cql/HEDIS_CDC_H_Test.java
  4. backend/modules/services/quality-measure-service/src/test/java/
     com/hdim/qualitymeasure/integration/HEDIS_CDC_H_IntegrationTest.java
  5. backend/db/changelog/db.changelog-master.xml (updated)

Next steps:
  1. Edit HEDIS-CDC-H.cql to implement measure logic
  2. Add value set OIDs from VSAC
  3. Implement unit and integration tests
  4. Run validation: ./gradlew test --tests "*HEDIS_CDC_H*"
  5. Apply migration: ./gradlew update
  6. Deploy and test service

Refer to:
  - CQL Development Guide: docs/CQL_DEVELOPMENT_GUIDE.md
  - Quality Measures Skill: .claude/plugins/hdim-accelerator/skills/quality-measures.md
  - CQL Measure Builder Agent: .claude/plugins/hdim-accelerator/agents/cql-measure-builder.md
```

## Related Documentation

- **Quality Measures Skill:** `.claude/plugins/hdim-accelerator/skills/quality-measures.md`
- **CQL Measure Builder Agent:** `.claude/plugins/hdim-accelerator/agents/cql-measure-builder.md`
- **CQL Development Guide:** `docs/CQL_DEVELOPMENT_GUIDE.md`
- **HEDIS Specifications:** https://www.ncqa.org/hedis/

---

**Last Updated:** January 21, 2026
**Version:** 1.0.0
**Time Savings:** ~2 hours → 10 minutes (92% faster)
