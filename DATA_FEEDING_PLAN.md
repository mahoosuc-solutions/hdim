# HealthData In Motion - Data Feeding Plan

**Date:** 2025-11-04
**Status:** Planning Phase
**Purpose:** Comprehensive strategy for feeding data through the quality measure evaluation platform

---

## Executive Summary

The HealthData platform requires three types of data to function:

1. **FHIR Patient Data** (External) - Clinical records from healthcare systems
2. **CQL Measure Libraries** (Internal) - Quality measure definitions and logic
3. **Value Sets** (Internal) - Clinical terminology code sets

This document outlines the complete strategy for populating and managing each data type.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     DATA SOURCES                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │ FHIR Server  │  │ CQL Library  │  │  Value Sets  │        │
│  │  (External)  │  │  (Database)  │  │  (Database)  │        │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘        │
│         │                 │                  │                 │
└─────────┼─────────────────┼──────────────────┼─────────────────┘
          │                 │                  │
          ▼                 ▼                  ▼
    ┌─────────────────────────────────────────────┐
    │      CQL Engine Service (Evaluator)         │
    │  - Fetches FHIR resources                   │
    │  - Loads CQL templates                      │
    │  - Applies measure logic                    │
    │  - Publishes results & events               │
    └─────────────────┬───────────────────────────┘
                      │
                      ▼
              ┌───────────────┐
              │   PostgreSQL  │
              │  (Results DB) │
              └───────────────┘
                      │
                      ▼
              ┌───────────────┐
              │  Kafka Events │
              │  → WebSocket  │
              │  → Dashboard  │
              └───────────────┘
```

---

## Phase 1: FHIR Patient Data Strategy

### Current State

- **FHIR Server URL:** `http://fhir-service-mock:8080/fhir` (configured, not running)
- **Required Resources:** Patient, Observation, Condition, Procedure, MedicationRequest, Encounter, Immunization, AllergyIntolerance
- **Access Pattern:** REST API calls via `FhirServiceClient`
- **Caching:** ThreadLocal per-evaluation caching in `FHIRDataProvider`

### Option 1: HAPI FHIR Test Server (Recommended for Development)

**Deploy HAPI FHIR Server:**

```yaml
# Add to docker-compose.yml
fhir-server:
  image: hapiproject/hapi:latest
  container_name: healthdata-fhir-server
  restart: unless-stopped
  ports:
    - "8082:8080"
  environment:
    SPRING_CONFIG_LOCATION: file:///app/application.yaml
    HAPI_FHIR_FHIR_VERSION: R4
    HAPI_FHIR_SUBSCRIPTION_RESTHOOK_ENABLED: "true"
  volumes:
    - fhir_data:/data/hapi
  networks:
    - healthdata-network
```

**Update CQL Engine Configuration:**
```yaml
# application-docker.yml
fhir:
  server:
    url: http://fhir-server:8080/fhir
    timeout: 30000
```

**Populate with Synthea Data:**

1. Install Synthea (Synthetic Patient Generator):
   ```bash
   git clone https://github.com/synthetichealth/synthea.git
   cd synthea
   ./gradlew build check test
   ```

2. Generate test population:
   ```bash
   # Generate 100 patients in Massachusetts
   ./run_synthea -p 100 Massachusetts Boston
   ```

3. Upload to HAPI FHIR:
   ```bash
   # Upload generated FHIR bundles
   for file in output/fhir/*.json; do
     curl -X POST \
       -H "Content-Type: application/fhir+json" \
       -d @"$file" \
       http://localhost:8082/fhir
   done
   ```

**Benefits:**
- Realistic synthetic patient data
- Complete FHIR R4 resources
- Includes conditions, medications, procedures, observations
- Free and open-source

### Option 2: Mock FHIR Service (Quick Testing)

**Create simple mock service:**

```javascript
// fhir-mock-server.js
const express = require('express');
const app = express();

app.get('/fhir/Patient/:id', (req, res) => {
  res.json({
    resourceType: 'Patient',
    id: req.params.id,
    birthDate: '1978-03-15',
    gender: 'male'
  });
});

app.get('/fhir/Observation', (req, res) => {
  const patientId = req.query.patient;
  res.json({
    resourceType: 'Bundle',
    entry: [
      {
        resource: {
          resourceType: 'Observation',
          id: 'obs-1',
          subject: { reference: `Patient/${patientId}` },
          code: { coding: [{ system: 'http://loinc.org', code: '4548-4' }] },
          valueQuantity: { value: 7.2, unit: '%' },
          effectiveDateTime: '2025-10-01'
        }
      }
    ]
  });
});

app.listen(8082);
```

**Deploy as Docker service:**
```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY fhir-mock-server.js .
RUN npm install express
CMD ["node", "fhir-mock-server.js"]
```

### Option 3: Epic/Cerner Sandbox (Production Testing)

**Connect to Epic Sandbox:**
- Register at https://fhir.epic.com/
- Obtain OAuth credentials
- Configure FhirServiceClient with OAuth2

**Connect to Cerner Sandbox:**
- Register at https://code-console.cerner.com/
- Use Cerner's test patients
- Configure OAuth2 authentication

---

## Phase 2: CQL Library Creation

### Current State

- **Database Table:** `cql_libraries`
- **Existing Test Library:** TestMeasure v1.0.0 (simple "define Result: true")
- **API Endpoint:** `POST /api/v1/cql/libraries`

### CQL Measure Development Workflow

#### Step 1: Define Measure Requirements

**Example: Diabetes HbA1c Control (CDC-H)**

```yaml
Measure: CDC-H
Name: Comprehensive Diabetes Care - HbA1c Control
Population:
  Denominator: Patients 18-75 with diabetes diagnosis
  Exclusions: Patients with end-stage renal disease
  Numerator: HbA1c < 8.0% in measurement period
Data Requirements:
  - Patient demographics (age)
  - Active diabetes conditions (ICD-10)
  - HbA1c observations (LOINC 4548-4)
  - Exclusion conditions (ESRD)
```

#### Step 2: Write CQL Logic

**File:** `measures/CDC-H.cql`

```cql
library CDC_H version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

codesystem "LOINC": 'http://loinc.org'
codesystem "ICD10": 'http://hl7.org/fhir/sid/icd-10-cm'

valueset "Diabetes": 'urn:oid:2.16.840.1.113883.3.464.1003.103.12.1001'
valueset "ESRD": 'urn:oid:2.16.840.1.113883.3.526.3.353'

code "HbA1c": '4548-4' from "LOINC"

parameter "Measurement Period" Interval<DateTime>

context Patient

// Calculate patient age
define "Patient Age":
  AgeInYearsAt(start of "Measurement Period")

// Denominator: Patients 18-75 with diabetes
define "Denominator":
  "Patient Age" >= 18 and "Patient Age" <= 75
    and exists([Condition: "Diabetes"])

// Exclusions: Patients with ESRD
define "Exclusions":
  exists([Condition: "ESRD"])

// Get most recent HbA1c
define "Most Recent HbA1c":
  Last(
    [Observation: "HbA1c"] O
      where O.effective during "Measurement Period"
      sort by effective
  )

// Numerator: HbA1c < 8.0%
define "Numerator":
  "Denominator"
    and not "Exclusions"
    and "Most Recent HbA1c".value < 8.0 '%'

// Final measure result
define "Is Compliant":
  "Numerator"
```

#### Step 3: Compile CQL to ELM

```bash
# Install CQL-to-ELM compiler
npm install -g cql-to-elm

# Compile CQL to JSON ELM
cql-to-elm --input measures/CDC-H.cql --output measures/CDC-H.json --format json
```

#### Step 4: Load into Database

```bash
curl -u "cql-service-user:cql-service-dev-password-change-in-prod" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: TENANT001" \
  -X POST "http://localhost:8081/cql-engine/api/v1/cql/libraries" \
  -d @- <<'EOF'
{
  "libraryName": "CDC-H",
  "version": "1.0.0",
  "status": "ACTIVE",
  "description": "Comprehensive Diabetes Care - HbA1c Control",
  "publisher": "HealthData In Motion",
  "cqlContent": "$(cat measures/CDC-H.cql | jq -Rs .)",
  "elmJson": "$(cat measures/CDC-H.json | jq -c .)"
}
EOF
```

### Pre-Built Measure Library Sources

**Option 1: NCQA HEDIS Measures**
- Download from: https://www.ncqa.org/hedis/
- Requires license for production use
- Includes 80+ quality measures
- Format: Technical specifications (convert to CQL)

**Option 2: CMS eCQMs**
- Download from: https://ecqi.healthit.gov/
- Free for use
- CQL source included
- Annual updates

**Option 3: FHIR Clinical Guidelines**
- Repository: https://github.com/cqframework/clinical_quality_language
- Example measures with CQL source
- Community-maintained

---

## Phase 3: Value Set Management

### Current State

- **Database Table:** `value_sets`
- **API Endpoint:** `POST /api/v1/cql/value-sets`
- **Supported Code Systems:** SNOMED, LOINC, RxNorm, ICD-10, CPT, HCPCS

### Value Set Sources

#### VSAC (Value Set Authority Center)

**Download value sets:**

1. Register at https://vsac.nlm.nih.gov/
2. Download OID-based value sets
3. Convert to JSON format

**Example value set structure:**

```json
{
  "oid": "2.16.840.1.113883.3.464.1003.103.12.1001",
  "name": "Diabetes",
  "version": "1.0",
  "codeSystem": "ICD10CM",
  "description": "ICD-10-CM codes for diabetes mellitus",
  "codes": [
    {"code": "E10", "display": "Type 1 diabetes mellitus"},
    {"code": "E11", "display": "Type 2 diabetes mellitus"},
    {"code": "E11.9", "display": "Type 2 diabetes mellitus without complications"},
    {"code": "E11.65", "display": "Type 2 diabetes mellitus with hyperglycemia"}
  ]
}
```

**Load via API:**

```bash
curl -u "cql-service-user:cql-service-dev-password-change-in-prod" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: TENANT001" \
  -X POST "http://localhost:8081/cql-engine/api/v1/cql/value-sets" \
  -d @diabetes-valueset.json
```

#### LOINC Codes

**Common LOINC codes for quality measures:**

```json
{
  "oid": "2.16.840.1.113883.3.464.1003.198.12.1013",
  "name": "HbA1c Laboratory Test",
  "codeSystem": "LOINC",
  "codes": [
    {"code": "4548-4", "display": "Hemoglobin A1c/Hemoglobin.total in Blood"},
    {"code": "17856-6", "display": "Hemoglobin A1c/Hemoglobin.total in Blood by HPLC"},
    {"code": "41995-2", "display": "Hemoglobin A1c [Mass/volume] in Blood"}
  ]
}
```

#### Bulk Value Set Loading

**Create bulk loading script:**

```bash
#!/bin/bash
# load-value-sets.sh

API_BASE="http://localhost:8081/cql-engine"
AUTH="cql-service-user:cql-service-dev-password-change-in-prod"
TENANT="TENANT001"

for file in value-sets/*.json; do
  echo "Loading $(basename $file)..."
  curl -s -u "$AUTH" \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: $TENANT" \
    -X POST "$API_BASE/api/v1/cql/value-sets" \
    -d @"$file"
  echo ""
done
```

---

## Phase 4: Integration Testing Data Flow

### End-to-End Test Scenario

**Objective:** Evaluate diabetes care quality for synthetic patients

#### Step 1: Prepare FHIR Data

```bash
# Generate 10 diabetes patients with Synthea
./run_synthea -p 10 -m diabetes

# Upload to HAPI FHIR
for bundle in output/fhir/*.json; do
  curl -X POST \
    -H "Content-Type: application/fhir+json" \
    -d @"$bundle" \
    http://localhost:8082/fhir
done

# Extract patient IDs
PATIENT_IDS=$(curl -s http://localhost:8082/fhir/Patient \
  | jq -r '.entry[].resource.id')
```

#### Step 2: Load CQL Measure

```bash
# Create and load CDC-H measure (HbA1c control)
./scripts/create-cql-library.sh CDC-H

# Verify library loaded
LIBRARY_ID=$(curl -s -u "$AUTH" \
  -H "X-Tenant-ID: TENANT001" \
  "http://localhost:8081/cql-engine/api/v1/cql/libraries?name=CDC-H" \
  | jq -r '.[0].id')

echo "Library ID: $LIBRARY_ID"
```

#### Step 3: Trigger Batch Evaluation

```bash
# Convert patient IDs to JSON array
PATIENT_ARRAY=$(echo "$PATIENT_IDS" | jq -R . | jq -s .)

# Trigger batch evaluation
curl -u "cql-service-user:cql-service-dev-password-change-in-prod" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: TENANT001" \
  -X POST \
  "http://localhost:8081/cql-engine/api/v1/cql/evaluations/batch?libraryId=$LIBRARY_ID" \
  -d "$PATIENT_ARRAY"
```

#### Step 4: Monitor Dashboard

1. Open http://localhost:3002
2. Watch real-time evaluation events
3. View completion metrics
4. Analyze compliance rates

#### Step 5: Query Results

```bash
# Get evaluation results
curl -s -u "$AUTH" \
  -H "X-Tenant-ID: TENANT001" \
  "http://localhost:8081/cql-engine/api/v1/cql/evaluations?libraryId=$LIBRARY_ID" \
  | jq '.[] | {patientId, status, complianceRate: .evaluationResult.complianceRate}'
```

---

## Phase 5: Production Data Pipeline

### Continuous Data Flow Architecture

```
┌──────────────┐
│  EHR System  │
│  (Epic/      │
│   Cerner)    │
└──────┬───────┘
       │ HL7/FHIR Export
       ▼
┌──────────────┐
│ FHIR Server  │
│  (HAPI)      │
└──────┬───────┘
       │ REST API
       ▼
┌──────────────────────┐
│  CQL Engine Service  │
│  - Scheduled batch   │
│  - Real-time eval    │
│  - Event streaming   │
└──────┬───────────────┘
       │
       ├─► PostgreSQL (Results)
       ├─► Kafka (Events)
       └─► Quality Measure Service
```

### Scheduled Evaluation Jobs

**Create Spring Scheduler:**

```java
@Service
public class MeasureEvaluationScheduler {

    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    public void evaluateAllPatients() {
        List<String> patientIds = fhirClient.getAllPatientIds();
        List<CqlLibrary> activeLibraries = libraryRepository
            .findByStatusAndTenantId("ACTIVE", tenantId);

        for (CqlLibrary library : activeLibraries) {
            evaluationService.batchEvaluate(
                tenantId,
                library.getId(),
                patientIds
            );
        }
    }

    @Scheduled(fixedDelay = 3600000) // Hourly
    public void evaluateNewPatients() {
        // Evaluate patients added in last hour
    }
}
```

### Data Refresh Strategy

**Option 1: Full Refresh (Nightly)**
- Query all patients from FHIR
- Re-evaluate all active measures
- Update compliance metrics
- Generate reports

**Option 2: Incremental Updates (Real-time)**
- Subscribe to FHIR server subscriptions
- Evaluate on patient data change
- Update affected measures only

**Option 3: Hybrid Approach (Recommended)**
- Real-time for critical measures (diabetes, hypertension)
- Nightly batch for comprehensive reporting
- Weekly reconciliation for accuracy

---

## Phase 6: Data Quality & Monitoring

### Data Quality Checks

```sql
-- Check FHIR data completeness
SELECT
    COUNT(DISTINCT patient_id) as total_patients,
    COUNT(DISTINCT CASE WHEN has_demographics THEN patient_id END) as with_demographics,
    COUNT(DISTINCT CASE WHEN has_conditions THEN patient_id END) as with_conditions,
    COUNT(DISTINCT CASE WHEN has_observations THEN patient_id END) as with_observations
FROM evaluation_summary;

-- Check evaluation success rate
SELECT
    library.library_name,
    COUNT(*) as total_evaluations,
    SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as successful,
    ROUND(100.0 * SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) / COUNT(*), 2) as success_rate
FROM cql_evaluations eval
JOIN cql_libraries library ON eval.library_id = library.id
GROUP BY library.library_name;

-- Check evaluation performance
SELECT
    library_name,
    AVG(duration_ms) as avg_duration_ms,
    MAX(duration_ms) as max_duration_ms,
    MIN(duration_ms) as min_duration_ms
FROM cql_evaluations eval
JOIN cql_libraries library ON eval.library_id = library.id
WHERE status = 'SUCCESS'
GROUP BY library_name;
```

### Monitoring Metrics

**Prometheus Metrics (Available):**
- `cql_evaluations_total` - Total evaluations
- `cql_evaluations_duration_seconds` - Evaluation duration
- `cql_evaluations_errors_total` - Error count
- `fhir_client_requests_total` - FHIR API calls
- `kafka_producer_record_send_total` - Kafka events

**Grafana Dashboard Panels:**
1. Evaluation throughput (evals/minute)
2. Success rate over time
3. Average evaluation duration
4. FHIR API latency
5. Kafka event lag

---

## Implementation Priority

### Phase 1 (Week 1): Foundation
- [ ] Deploy HAPI FHIR server
- [ ] Generate Synthea test data (100 patients)
- [ ] Verify FHIR connectivity from CQL Engine
- [ ] Test single patient evaluation

### Phase 2 (Week 2): Measure Development
- [ ] Create 5 core HEDIS measures (CDC, CBP, COL, BCS, CIS)
- [ ] Load value sets (diabetes, hypertension, cancer screening)
- [ ] Test measure logic with synthetic data
- [ ] Validate results against NCQA specifications

### Phase 3 (Week 3): Batch Processing
- [ ] Configure batch evaluation endpoints
- [ ] Test concurrent evaluation (10, 50, 100 patients)
- [ ] Monitor Kafka event flow
- [ ] Verify dashboard real-time updates

### Phase 4 (Week 4): Production Readiness
- [ ] Implement scheduled jobs
- [ ] Set up monitoring and alerts
- [ ] Create data quality reports
- [ ] Document measure maintenance procedures

---

## Success Criteria

### Technical Validation
- ✅ FHIR server responds to Patient, Observation, Condition queries
- ✅ CQL libraries load and compile successfully
- ✅ Batch evaluations complete with >95% success rate
- ✅ Kafka events published to all topics
- ✅ Dashboard displays real-time updates
- ✅ PostgreSQL stores evaluation results

### Business Validation
- ✅ Measure results match manual calculation
- ✅ Compliance rates align with HEDIS benchmarks
- ✅ Care gaps identified correctly
- ✅ Performance meets SLA (<500ms per evaluation)

---

## Next Steps

1. **Immediate (Today):**
   - Add HAPI FHIR service to docker-compose.yml
   - Update CQL Engine FHIR URL configuration
   - Test FHIR connectivity

2. **Short-term (This Week):**
   - Generate Synthea patient data
   - Create first production CQL measure (CDC-H)
   - Run end-to-end test

3. **Medium-term (Next Week):**
   - Develop remaining core measures
   - Implement scheduled evaluation jobs
   - Set up monitoring dashboards

4. **Long-term (Next Month):**
   - Connect to Epic/Cerner sandbox
   - Implement incremental evaluation
   - Production deployment planning

---

## Resources

### Documentation
- CQL Specification: https://cql.hl7.org/
- FHIR R4: https://hl7.org/fhir/R4/
- HEDIS Measures: https://www.ncqa.org/hedis/
- HAPI FHIR: https://hapifhir.io/

### Tools
- Synthea: https://github.com/synthetichealth/synthea
- CQL-to-ELM Compiler: https://github.com/cqframework/clinical_quality_language
- FHIR Validator: https://confluence.hl7.org/display/FHIR/Using+the+FHIR+Validator

### APIs
- Epic Sandbox: https://fhir.epic.com/
- Cerner Sandbox: https://code-console.cerner.com/
- VSAC: https://vsac.nlm.nih.gov/

---

**Document Version:** 1.0
**Last Updated:** 2025-11-04
**Maintained By:** Development Team
