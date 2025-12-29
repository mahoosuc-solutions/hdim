# Demo Environment Status

*Last Updated: December 29, 2025*

## Current State

### Infrastructure: HEALTHY

All 18+ Docker services are running and healthy:

| Service | Port | Status | Health |
|---------|------|--------|--------|
| quality-measure-service | 8087 | UP | Healthy |
| cql-engine-service | 8081 | UP | Healthy |
| fhir-service | 8085 | UP | Healthy |
| patient-service | 8084 | UP | Healthy |
| care-gap-service | 8086 | UP | Healthy |
| consent-service | 8082 | UP | Healthy |
| gateway-service | 8080 | UP | Healthy |
| notification-service | 8107 | UP | Healthy |
| hcc-service | 8105 | UP | Healthy |
| qrda-export-service | 8104 | UP | Healthy |
| prior-auth-service | 8102 | UP | Healthy |
| ecr-service | 8101 | UP | Healthy |
| event-router-service | 8095 | UP | Healthy |
| event-processing-service | 8083 | UP | Healthy |
| postgres | 5435 | UP | Healthy |
| redis | 6380 | UP | Healthy |
| kafka | 9094 | UP | Healthy |
| jaeger | 16686 | UP | Healthy |

### Database State

| Database | Tables | Records | Status |
|----------|--------|---------|--------|
| cql_db | 8 tables | 56 CQL libraries, 0 evaluations | Libraries loaded, no evaluations |
| fhir_db | 27 tables | 0 patients, 0 conditions, 0 observations | Schema ready, no data |
| quality_db | 34 tables | 0 care gaps, 0 quality results | Schema ready, no data |
| patient_db | 12 tables | 0 patients | Schema ready, no data |

### CQL Libraries: LOADED

56 CQL libraries are loaded in cql_db, including:
- HEDIS quality measures
- CMS eCQM measures
- Custom quality measures

### Demo Data: LOADED

| Dataset | Count | Status |
|---------|-------|--------|
| Patients (FHIR) | 10 | Loaded |
| Care Gaps | 9 | Loaded (3 HIGH, 4 MEDIUM, 2 LOW) |
| Quality Results | 15 | Loaded (CMS122, CMS165, CMS2, CMS130) |
| Conditions | 0 | Not yet loaded |

**Tenant:** demo-clinic

---

## Demo Data Loading Options

### Option 1: Load FHIR Patients via API

```bash
# Load sample patients from sample-data/sample-patients.json
curl -X POST http://localhost:8085/fhir/api/v1/bundles \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: demo-clinic" \
  -d @sample-data/sample-patients.json
```

### Option 2: Direct Database Insert

```sql
-- Connect to fhir_db
docker exec -it healthdata-postgres psql -U healthdata -d fhir_db

-- Insert sample patient
INSERT INTO patients (
    id, tenant_id, fhir_id, family_name, given_name,
    birth_date, gender, active, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'demo-clinic', 'patient-001',
    'Doe', 'John', '1959-11-14', 'male', true, NOW(), NOW()
);
```

### Option 3: Run Demo Data Script (Needs Fix)

The existing scripts reference incorrect database names. After fixing:

```bash
# Load demo clinical data
./load-demo-clinical-data.sh
```

---

## Required Demo Scenarios

For SMB healthcare prospects, we need to demonstrate:

### 1. Patient Quality Dashboard
- Population-level quality measure performance
- Trend analysis over time
- Drill-down to individual patients

**Data Needed:**
- 50-100 sample patients
- Multiple quality measure results per patient
- Mix of compliant/non-compliant

### 2. Care Gap Identification
- Open care gaps by priority
- Patient outreach lists
- Gap closure tracking

**Data Needed:**
- Care gaps linked to patients
- Multiple urgency levels (HIGH, MEDIUM, LOW)
- Associated recommended actions

### 3. HEDIS/Quality Measure Evaluation
- CQL library selection
- Patient cohort definition
- Measure calculation results

**Data Needed:**
- CQL libraries (already loaded ✓)
- FHIR patient bundles
- Clinical data (conditions, observations, procedures)

### 4. QRDA Export
- QRDA Cat I (individual)
- QRDA Cat III (aggregate)
- CMS submission formatting

**Data Needed:**
- Quality measure results
- Patient demographics
- Clinical encounters

---

## Demo Data Schema Requirements

### Patients (fhir_db.patients)

```sql
CREATE TABLE patients (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    fhir_id VARCHAR(255) NOT NULL,
    family_name VARCHAR(255),
    given_name VARCHAR(255),
    birth_date DATE,
    gender VARCHAR(32),
    active BOOLEAN DEFAULT true,
    mrn VARCHAR(64),
    mrn_authority VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
```

### Quality Measure Results (quality_db.quality_measure_results)

```sql
CREATE TABLE quality_measure_results (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    patient_id VARCHAR(64),
    measure_id VARCHAR(64) NOT NULL,
    measure_name VARCHAR(255) NOT NULL,
    measure_category VARCHAR(64),
    measure_year INTEGER,
    numerator_compliant BOOLEAN,
    denominator_elligible BOOLEAN,
    compliance_rate DECIMAL(5,2),
    score DECIMAL(5,2),
    calculation_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64)
);
```

### Care Gaps (quality_db.care_gaps)

```sql
CREATE TABLE care_gaps (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    patient_id UUID,
    category VARCHAR(64),
    gap_type VARCHAR(64),
    title VARCHAR(255),
    description TEXT,
    priority VARCHAR(32),
    status VARCHAR(32),
    quality_measure VARCHAR(64),
    recommendation TEXT,
    due_date DATE,
    identified_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
```

---

## Next Steps to Complete Demo Setup

### Immediate (1-2 hours)

1. **Create Demo Data SQL Script**
   - Insert 20 patients into fhir_db.patients
   - Create conditions (diabetes, hypertension, depression)
   - Create observations (HbA1c, blood pressure, PHQ-9)

2. **Insert Quality Measure Results**
   - 100 results across 5 measures
   - 60% compliant, 40% non-compliant

3. **Create Care Gaps**
   - 20 care gaps linked to patients
   - 5 HIGH, 10 MEDIUM, 5 LOW priority

### Short-term (Day 2)

4. **Create Demo User Accounts**
   - demo_admin / demo123
   - demo_evaluator / demo123
   - demo_viewer / demo123

5. **Configure Demo Tenant**
   - Tenant: demo-clinic
   - Organization: "Springfield Community Health Center"

6. **Test API Endpoints**
   - Verify patient search works
   - Verify quality measure evaluation
   - Verify care gap queries

### Pre-outreach (Day 3)

7. **Record Demo Video**
   - 5-minute walkthrough
   - Key value propositions
   - Before/after scenarios

8. **Create Demo Script**
   - Talking points
   - Pain point connections
   - ROI discussion points

---

## API Endpoints for Demo

### Quality Measure Service (8087)

```bash
# Get quality measure results
curl http://localhost:8087/quality-measure/api/v1/results \
  -H "X-Tenant-ID: demo-clinic"

# Get population metrics
curl http://localhost:8087/quality-measure/api/v1/population/metrics \
  -H "X-Tenant-ID: demo-clinic"
```

### Care Gap Service (8086)

```bash
# List care gaps
curl http://localhost:8086/care-gap/api/v1/gaps \
  -H "X-Tenant-ID: demo-clinic"

# Get gap summary by priority
curl http://localhost:8086/care-gap/api/v1/gaps/summary \
  -H "X-Tenant-ID: demo-clinic"
```

### FHIR Service (8085)

```bash
# Search patients
curl http://localhost:8085/fhir/api/v1/Patient \
  -H "X-Tenant-ID: demo-clinic"

# Get patient by ID
curl http://localhost:8085/fhir/api/v1/Patient/{id} \
  -H "X-Tenant-ID: demo-clinic"
```

### CQL Engine Service (8081)

```bash
# Evaluate measure for patient
curl -X POST http://localhost:8081/cql-engine/api/v1/evaluate \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: demo-clinic" \
  -d '{
    "libraryId": "CMS2v13",
    "patientId": "patient-001"
  }'
```

---

## Demo Environment Commands

```bash
# Check service health
docker compose ps -a

# View service logs
docker compose logs -f quality-measure-service

# Restart all services
docker compose restart

# Full restart
docker compose down && docker compose up -d

# Connect to database
docker exec -it healthdata-postgres psql -U healthdata -d fhir_db
```

---

## Authentication Setup Required

APIs return 403 without authentication. Need to:

1. **Create demo users** using `create-demo-users.sh` or manual SQL insert
2. **Generate JWT tokens** via gateway login endpoint
3. **Configure test credentials** for demo walkthrough

Demo Users (to be created):
- demo.admin / demo123 - Full admin access
- demo.doctor / demo123 - Clinical user
- demo.analyst / demo123 - Read-only analytics

---

## Success Criteria

Demo environment is ready when:

- [x] 10+ patients in database
- [x] Care gaps loaded with priorities
- [x] Quality measure results populated
- [ ] Authentication configured
- [ ] API endpoints accessible
- [ ] Demo video recorded
