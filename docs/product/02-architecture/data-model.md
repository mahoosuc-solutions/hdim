---
id: "product-data-model-spec"
title: "Data Model & Database Design"
portalType: "product"
path: "product/02-architecture/data-model.md"
category: "architecture"
subcategory: "data-design"
tags: ["data-model", "FHIR", "database-schema", "entity-relationships", "PostgreSQL"]
summary: "Complete data model specification including FHIR resource mappings, PostgreSQL database schemas, relationships, indexes, and lifecycle management for clinical data and quality measures."
estimatedReadTime: 22
difficulty: "advanced"
targetAudience: ["architect", "database-admin", "technical-lead"]
owner: "Product Architecture"
reviewCycle: "quarterly"
status: "published"
version: "1.0"
seoKeywords: ["FHIR resources", "data model", "database schema", "clinical data", "relational database", "PostgreSQL"]
relatedDocuments: ["system-architecture", "integration-patterns", "performance-benchmarks"]
lastUpdated: "2025-12-01"
---

# Data Model & Database Design

## Executive Summary

HealthData in Motion uses a **FHIR-native data model** with a relational PostgreSQL backend. The design balances structured relational data (for queries and analytics) with semi-structured JSONB storage (for FHIR flexibility), enabling both strict validation and schema evolution without database migrations.

**Key Design Principles**:
- FHIR R4 compliance for healthcare interoperability
- JSONB storage for hierarchical clinical data
- Normalized relationships for query performance
- Immutable event log for audit trail
- Multi-tenant isolation via row-level security (RLS)

## Core FHIR Resources

### Patient Resource
Represents an individual patient with demographics and identification.

**Key Fields**:
- **id**: UUID (globally unique)
- **mrn**: Medical Record Number (unique per facility)
- **name**: First and last name
- **dateOfBirth**: Date of birth (required)
- **gender**: male | female | other | unknown
- **telecom**: Email and phone contacts
- **address**: Street address, city, state, zip
- **status**: active | inactive | deceased
- **identifiers**: MRN, SSN, national ID numbers

**Database**:
```sql
CREATE TABLE patients (
  id UUID PRIMARY KEY,
  mrn VARCHAR(50) NOT NULL,
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  date_of_birth DATE,
  gender VARCHAR(20),
  status VARCHAR(20) DEFAULT 'active',
  fhir_data JSONB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE INDEX idx_patients_mrn ON patients(mrn);
CREATE INDEX idx_patients_date_of_birth ON patients(date_of_birth);
CREATE INDEX idx_patients_status ON patients(status);
CREATE INDEX idx_patients_fhir_gin ON patients USING GIN(fhir_data);
```

**Typical Record Size**: 2-5 KB (with FHIR JSON)

### Observation Resource
Clinical measurements: lab results, vital signs, test values.

**Key Fields**:
- **id**: UUID
- **patientId**: FK to Patient
- **code**: LOINC or SNOMED code (e.g., glucose, blood pressure)
- **value**: Numeric, string, or boolean result
- **unit**: Unit of measurement (mg/dL, mmHg)
- **effectiveDateTime**: When measurement was taken
- **status**: registered | preliminary | final | amended | cancelled
- **referenceRange**: Normal range (low/high values)
- **interpretation**: normal | high | low | critical

**Database**:
```sql
CREATE TABLE observations (
  id UUID PRIMARY KEY,
  patient_id UUID NOT NULL REFERENCES patients(id),
  code_system VARCHAR(50),
  code_value VARCHAR(50),
  code_display VARCHAR(255),
  value DECIMAL(15,4),
  unit VARCHAR(50),
  effective_date_time TIMESTAMP NOT NULL,
  status VARCHAR(20) DEFAULT 'final',
  interpretation VARCHAR(20),
  fhir_data JSONB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_obs_patient ON observations(patient_id);
CREATE INDEX idx_obs_code ON observations(code_system, code_value);
CREATE INDEX idx_obs_patient_date ON observations(patient_id, effective_date_time DESC);
CREATE INDEX idx_obs_effective_date ON observations(effective_date_time);
```

**Typical Record Size**: 1-3 KB
**Expected Volume**: 5M-20M records per 1M patients

### Condition Resource
Diagnoses and clinical problems.

**Key Fields**:
- **id**: UUID
- **patientId**: FK to Patient
- **code**: ICD-10 or SNOMED code
- **status**: active | recurrence | relapse | inactive | remission | resolved
- **severity**: mild | moderate | severe
- **onsetDateTime**: When condition started
- **abatementDateTime**: When condition resolved
- **recordedDate**: When diagnosis was documented

**Database**:
```sql
CREATE TABLE conditions (
  id UUID PRIMARY KEY,
  patient_id UUID NOT NULL REFERENCES patients(id),
  code_system VARCHAR(20),
  code_value VARCHAR(20),
  code_display VARCHAR(255),
  status VARCHAR(20) DEFAULT 'active',
  severity VARCHAR(20),
  onset_date_time TIMESTAMP,
  recorded_date TIMESTAMP NOT NULL,
  fhir_data JSONB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_condition_patient ON conditions(patient_id);
CREATE INDEX idx_condition_status ON conditions(patient_id, status);
CREATE INDEX idx_condition_code ON conditions(code_system, code_value);
```

**Typical Record Size**: 1-2 KB
**Expected Volume**: 0.5M-5M records per 1M patients

### Encounter Resource
Clinical visits, hospital stays, appointments.

**Key Fields**:
- **id**: UUID
- **patientId**: FK to Patient
- **type**: inpatient | outpatient | emergency | virtual | home-health
- **status**: planned | in-progress | completed | cancelled
- **startDateTime**: When encounter began
- **endDateTime**: When encounter ended
- **location**: Facility/department name
- **provider**: Provider name/ID
- **reasonCode**: Chief complaint or diagnosis code

**Database**:
```sql
CREATE TABLE encounters (
  id UUID PRIMARY KEY,
  patient_id UUID NOT NULL REFERENCES patients(id),
  type VARCHAR(50),
  status VARCHAR(50) DEFAULT 'completed',
  start_date_time TIMESTAMP,
  end_date_time TIMESTAMP,
  location VARCHAR(255),
  provider_name VARCHAR(255),
  reason_code VARCHAR(50),
  fhir_data JSONB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_encounter_patient ON encounters(patient_id);
CREATE INDEX idx_encounter_date ON encounters(start_date_time);
CREATE INDEX idx_encounter_patient_date ON encounters(patient_id, start_date_time DESC);
```

**Typical Record Size**: 1-2 KB
**Expected Volume**: 5M-20M records per 1M patients

### MedicationRequest Resource
Medication orders and prescriptions.

**Key Fields**:
- **id**: UUID
- **patientId**: FK to Patient
- **medicationCode**: RxNorm or NDC code
- **status**: active | completed | cancelled | stopped
- **authoredOn**: When order was created
- **dosageInstruction**: How to take (text and structured)
- **frequency**: How often (e.g., "twice daily")
- **route**: How given (oral, IV, topical, etc.)

**Database**:
```sql
CREATE TABLE medication_requests (
  id UUID PRIMARY KEY,
  patient_id UUID NOT NULL REFERENCES patients(id),
  medication_code VARCHAR(20),
  medication_display VARCHAR(255),
  status VARCHAR(20) DEFAULT 'active',
  authored_on TIMESTAMP NOT NULL,
  dosage_text VARCHAR(500),
  route VARCHAR(100),
  frequency VARCHAR(100),
  fhir_data JSONB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_med_req_patient ON medication_requests(patient_id);
CREATE INDEX idx_med_req_status ON medication_requests(patient_id, status);
```

**Typical Record Size**: 1-2 KB
**Expected Volume**: 2M-10M records per 1M patients

## Quality Measure Results

### MeasureResult Entity
Stores measure evaluation outcome for each patient.

**Key Fields**:
- **id**: UUID
- **patientId**: FK to Patient
- **measureId**: Measure identifier (HEDIS code, etc.)
- **evaluationDate**: Date measure was evaluated
- **numeratorStatus**: in | not-in | null
- **denominatorStatus**: in | not-in | null
- **result**: pass | fail | null
- **calculatedAt**: When CQL was executed

**Database**:
```sql
CREATE TABLE measure_results (
  id UUID PRIMARY KEY,
  patient_id UUID NOT NULL REFERENCES patients(id),
  measure_id VARCHAR(50) NOT NULL,
  evaluation_date DATE NOT NULL,
  numerator_status VARCHAR(20),
  denominator_status VARCHAR(20),
  result VARCHAR(20),
  calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_measure_result_unique
  ON measure_results(patient_id, measure_id, evaluation_date);
CREATE INDEX idx_measure_result_date ON measure_results(evaluation_date DESC);
```

**Typical Record Size**: 0.5 KB
**Expected Volume**: 50M-200M records per 1M patients × 50 measures

### CareGap Entity
Tracks identified gaps in care and closure status.

**Key Fields**:
- **id**: UUID
- **patientId**: FK to Patient
- **measureId**: Related measure
- **status**: open | in-progress | closed
- **priority**: high | medium | low
- **createdAt**: When gap was detected
- **closedAt**: When gap was closed
- **autoClosed**: True if automatically closed

**Database**:
```sql
CREATE TABLE care_gaps (
  id UUID PRIMARY KEY,
  patient_id UUID NOT NULL REFERENCES patients(id),
  measure_id VARCHAR(50) NOT NULL,
  status VARCHAR(50) DEFAULT 'open',
  priority VARCHAR(20),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  closed_at TIMESTAMP,
  auto_closed BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_care_gap_patient ON care_gaps(patient_id);
CREATE INDEX idx_care_gap_status ON care_gaps(patient_id, status);
CREATE INDEX idx_care_gap_measure ON care_gaps(measure_id, status);
```

**Typical Record Size**: 0.5 KB
**Expected Volume**: 2M-10M active gaps per 1M patients

## Data Relationships

```
Patient (1) ────── (Many) Observations
        ────── (Many) Conditions
        ────── (Many) Encounters
        ────── (Many) MedicationRequests
        ────── (Many) MeasureResults
        ────── (Many) CareGaps

Measure (1) ────── (Many) MeasureResults
       ────── (Many) CareGaps
```

## Indexing Strategy

### Primary Access Patterns
```
1. Patient lookup by ID
   Query: SELECT * FROM patients WHERE id = $1
   Index: PRIMARY KEY (id)
   Speed: <1ms

2. All data for patient in date range
   Query: SELECT * FROM observations
          WHERE patient_id = $1 AND effective_date_time BETWEEN $2 AND $3
   Index: idx_obs_patient_date (patient_id, effective_date_time DESC)
   Speed: 10-50ms for 10,000 observations

3. Measure results for patient
   Query: SELECT * FROM measure_results
          WHERE patient_id = $1 AND measure_id = $2
   Index: idx_measure_result_unique
   Speed: <1ms

4. Open gaps by priority
   Query: SELECT * FROM care_gaps
          WHERE status = 'open' ORDER BY priority
   Index: idx_care_gap_status
   Speed: 100-500ms for 1M gaps
```

### JSONB Indexes
```
GIN indexes for full-text search in FHIR data:
  CREATE INDEX idx_obs_fhir ON observations USING GIN(fhir_data);

Enables queries like:
  WHERE fhir_data @> '{"status": "final"}'
```

### Partitioning (for scale)
```
Large tables partitioned by year:
  observations_2025, observations_2026, etc.

Benefits:
  - Faster queries on recent data
  - Faster maintenance (VACUUM)
  - Archive old partitions to S3
```

## Data Lifecycle

### Patient Data Flow
```
1. INGEST
   - EHR sends FHIR Bundle or CSV
   - Data validated against schema
   - Stored in PostgreSQL

2. ENRICH
   - MPI matching (deduplication)
   - Reference data lookup (drug names, etc.)
   - Calculate derived fields

3. ANALYZE
   - Quality measure evaluation
   - Care gap detection
   - Risk assessment

4. REPORT
   - Dashboards updated
   - Analytics queries
   - Notifications sent

5. ARCHIVE
   - After 7 years, move to S3
   - Compress and encrypt
   - Delete from active database
```

### Retention Policy
```
Clinical Data (Observations, Conditions, etc.):
  - Active: 7 years minimum
  - Archived: S3 cold storage
  - Deleted: Per GDPR/patient request

Measure Results:
  - Keep for 10 years (compliance)
  - Monthly aggregations kept longer

Audit Logs:
  - Immutable, 7-year minimum
  - HIPAA requirement
  - Cannot be deleted
```

## Data Validation

### Field Validation
```
Patient:
  - mrn: Required, 5-50 chars, alphanumeric + dash
  - dateOfBirth: Required, must be past, age < 120
  - gender: Required, must be one of 4 enums
  - firstName, lastName: Required, max 255 chars

Observation:
  - patientId: Must exist in patients table
  - code: Must be valid LOINC/SNOMED code
  - value: Type matches code (numeric, boolean, etc.)
  - effectiveDateTime: Required, not future
  - status: Must be one of 5 enums

Condition:
  - patientId: Must exist
  - code: Must be valid ICD-10/SNOMED code
  - recordedDate: Required, not future
  - onset < abatement (if both present)
```

### Business Rules
```
- No observations dated after today
- No conditions with onset > today
- Medication duration > 0 days
- Encounter end >= encounter start
- Patient age >= 0 and <= 120
- Lab values within expected ranges per code
```

## Multi-Tenancy

### Row-Level Security (RLS)
```
All tables have tenant_id column:
  - PostgreSQL enforces RLS policy
  - Users can only see their facility's data
  - Admin queries include tenant filter

Example policy:
  ALTER TABLE patients ENABLE ROW LEVEL SECURITY;
  CREATE POLICY rls_patients ON patients
    USING (tenant_id = current_setting('app.current_tenant'));
```

### Data Isolation
```
Different customers:
  - Patient A in Facility 1 isolated from Facility 2
  - No cross-facility data leakage
  - Multi-tenant safe on shared infrastructure
```

## Storage & Performance

### Typical Storage Per 1M Patients
```
Patients table:          500 MB
Observations (20M):      40 GB
Conditions (2M):         2 GB
Encounters (15M):        15 GB
MedicationRequests (5M): 5 GB
Measure Results (50M):   10 GB
Care Gaps (5M):          1 GB
Audit Logs (100M):       20 GB

TOTAL:                   ~93 GB

With indexes:            ~120 GB
With backups (daily×30): ~4 TB
```

### Query Performance Targets
```
Patient lookup:          <1ms
Observation search:      <50ms (10,000 records)
Care gap dashboard:      <500ms (1M gaps)
Measure report:          <5s (100K patients)
Bulk export (1M):        <30s
```

## Conclusion

The FHIR-native data model combines healthcare standards interoperability with relational database reliability. Strategic indexing and partitioning ensure performance scales to millions of patients and billions of clinical data points.

**Next Steps**:
- See [System Architecture](system-architecture.md) for data flow
- Review [Performance Benchmarks](performance-benchmarks.md) for query tuning
- Check [Security Architecture](security-architecture.md) for encryption details
