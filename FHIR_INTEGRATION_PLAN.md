# FHIR Server Integration Plan
## Comprehensive Strategy for Fully Populated FHIR Server Demo

**Status:** Planning Phase  
**Priority:** HIGH - Critical for System Demonstration  
**Last Updated:** November 24, 2025

---

## Executive Summary

This plan outlines the strategy to integrate and fully populate the HAPI FHIR R4 server with realistic clinical data that demonstrates all system capabilities including quality measure calculation, care gap identification, CQL engine processing, and multi-tenant patient management.

**Current State:**
- ✅ HAPI FHIR R4 server running and responsive (version 4.0.1)
- ✅ 73 patients currently loaded
- ✅ 29 observations loaded
- ✅ 10 medication requests loaded
- ✅ 12 procedures loaded
- ⚠️ 0 conditions loaded (critical gap)
- ⚠️ Container health check showing "unhealthy" (functional but needs investigation)
- ✅ FHIR service clients configured in CQL Engine and Patient Service
- ✅ Existing test data script available: `sample-data/comprehensive-fhir-test-data.sh`

**Target State:**
- Fully populated FHIR server with 50-100 patients
- Complete clinical scenarios covering all quality measures (CMS2, CMS134, CMS165, CMS122)
- Realistic data for demos: chronic disease, preventive care, behavioral health
- Multi-tenant data segregation properly configured
- Health checks passing consistently
- Integration validated end-to-end with CQL Engine

---

## Phase 1: Current State Assessment and Gap Analysis

### 1.1 Infrastructure Status ✅

**FHIR Server Configuration:**
```yaml
Service: fhir-service-mock
Image: hapiproject/hapi:latest
Port: 8083 (host) → 8080 (container)
Database: PostgreSQL healthdata_fhir
FHIR Version: R4 (4.0.1)
Status: Running 4 days, responding to API calls
Health Check: Unhealthy (needs investigation)
```

**Environment Variables:**
```bash
FHIR_SERVICE_URL: http://fhir-service-mock:8080/fhir  # CQL Engine
FHIR_SERVER_URL: http://fhir-service-mock:8080/fhir   # Quality Measure
External Access: http://localhost:8083/fhir
```

### 1.2 Current Data Inventory

**Resource Counts (as of Nov 24, 2025):**
| Resource Type | Count | Status | Demo Adequacy |
|--------------|-------|--------|---------------|
| Patient | 73 | ✅ Good | Sufficient for demo |
| Observation | 29 | ⚠️ Low | Need vitals, labs, screenings |
| MedicationRequest | 10 | ⚠️ Low | Need chronic disease meds |
| Procedure | 12 | ⚠️ Low | Need preventive care procedures |
| Condition | 0 | ❌ Critical | **BLOCKING** - Required for quality measures |
| Encounter | ? | Unknown | Need to verify |
| Immunization | ? | Unknown | Need for CIS/FVA measures |
| AllergyIntolerance | ? | Unknown | Nice to have |

**Sample Existing Patients:**
- Patient 1: John Doe (DOB: 1959-11-14) - Age 65
- Patient 2: Jane Smith (DOB: 1979-11-14) - Age 45
- Patient 3: Robert Johnson (DOB: 1952-11-14) - Age 72
- Patient 4: Maria Garcia (DOB: 1974-11-14) - Age 50
- Patient 5: Michael Brown (DOB: 2022-11-14) - Age 3

### 1.3 Critical Gaps Identified

**HIGH PRIORITY:**
1. **No Condition Resources** - Blocking quality measure calculations requiring diabetes, hypertension, depression diagnoses
2. **Insufficient Observations** - Need HbA1c, BP, BMI, depression screenings for CMS measures
3. **Low MedicationRequest count** - Need diabetes, hypertension, depression medications
4. **Container Health Check Failing** - Container functional but healthcheck endpoint may be timing out

**MEDIUM PRIORITY:**
5. Encounter data completeness unknown
6. Immunization coverage unknown (needed for pediatric measures)
7. Multi-tenant segregation not validated with current data

**LOW PRIORITY:**
8. AllergyIntolerance resources (nice-to-have for comprehensive patient view)
9. CarePlan resources (future enhancement)

---

## Phase 2: Data Population Strategy

### 2.1 Clinical Scenario Design

**Scenario 1: Diabetes Care Management (CMS122 - HbA1c Testing)**
- 15 patients with Type 2 Diabetes diagnosis
- HbA1c observations (compliant: <8%, non-compliant: >9%)
- Metformin/insulin medication requests
- Annual wellness encounters
- Age range: 45-75 years

**Scenario 2: Hypertension Control (CMS165 - BP Control)**
- 15 patients with essential hypertension diagnosis
- Blood pressure observations (compliant: <140/90, non-compliant: >150/95)
- ACE inhibitor/ARB medication requests
- Office visit encounters every 3-6 months
- Age range: 40-70 years

**Scenario 3: Depression Screening (CMS2 - PHQ-9)**
- 10 patients requiring depression screening
- PHQ-9 observations with standardized coding
- Annual wellness encounters
- Follow-up assessments for positive screens
- Age range: 18-80 years (diverse)

**Scenario 4: Diabetic Nephropathy Screening (CMS134)**
- 10 patients with diabetes diagnosis
- Urine albumin/creatinine observations
- eGFR observations
- Nephrology encounters for advanced cases
- Age range: 50-80 years

**Scenario 5: Pediatric Immunization (CIS/FVA)**
- 10 pediatric patients (ages 1-3)
- Comprehensive immunization records (DTaP, MMR, Varicella, etc.)
- Well-child visit encounters
- Immunization compliance tracking

**Scenario 6: Preventive Screening**
- 10 patients for colorectal cancer screening
- 10 patients for breast cancer screening (mammography)
- Age-appropriate screenings
- Procedure records for completed screenings

### 2.2 Data Generation Approach

**Option A: Enhance Existing Script (RECOMMENDED)**
- Modify `sample-data/comprehensive-fhir-test-data.sh`
- Current script already creates patients with comprehensive demographics
- Extend to include all resource types with proper clinical relationships
- Add quality measure-specific coding (LOINC, SNOMED, RxNorm)
- **Pros:** Existing foundation, known working patterns
- **Cons:** Bash script complexity, harder to maintain
- **Timeline:** 1-2 days

**Option B: Use Synthea Realistic Patient Generator**
- Download Synthea: https://github.com/synthetichealth/synthea
- Generate 50-100 synthetic patients with complete medical histories
- Import FHIR bundles directly to HAPI server
- **Pros:** Highly realistic, comprehensive, well-tested
- **Cons:** Large data volume, harder to control specific scenarios
- **Timeline:** 1 day (plus Synthea setup)

**Option C: Hybrid Approach (BEST)**
- Use Synthea for base population (30-40 patients with natural variety)
- Supplement with scripted patients for specific quality measure scenarios
- Ensures both realism and demo control
- **Pros:** Best of both worlds, realistic + targeted
- **Cons:** Two data sources to manage
- **Timeline:** 2-3 days

**RECOMMENDATION: Option C - Hybrid Approach**

### 2.3 Technical Implementation Details

**Resource Coding Standards:**
```javascript
// LOINC Codes for Observations
HbA1c: "4548-4" (Hemoglobin A1c/Hemoglobin.total in Blood)
BP Systolic: "8480-6" (Systolic blood pressure)
BP Diastolic: "8462-4" (Diastolic blood pressure)
PHQ-9: "44249-1" (PHQ-9 quick depression assessment panel)
eGFR: "48643-1" (Glomerular filtration rate)
BMI: "39156-5" (Body mass index)

// SNOMED-CT Codes for Conditions
Type 2 Diabetes: "44054006" (Diabetes mellitus type 2)
Essential Hypertension: "59621000" (Essential hypertension)
Major Depression: "370143000" (Major depressive disorder)
Chronic Kidney Disease: "709044004" (Chronic kidney disease)

// RxNorm Codes for Medications
Metformin: "860975" (metformin 500 MG Oral Tablet)
Lisinopril: "314076" (lisinopril 10 MG Oral Tablet)
Sertraline: "312940" (sertraline 50 MG Oral Tablet)
```

**Multi-Tenant Configuration:**
```bash
# Data segregation by tenant
TENANT_ID="demo-clinic"  # Primary demo tenant
TENANT_ID="clinic-001"   # Secondary tenant for multi-tenant demo
TENANT_ID="clinic-002"   # Tertiary tenant

# Each patient tagged with tenant identifier
# FHIR requests include X-Tenant-ID header
```

**Data Volume Targets:**
| Resource | Target Count | Per Patient Avg |
|----------|-------------|-----------------|
| Patient | 50-100 | 1 |
| Condition | 100-200 | 1-3 chronic conditions |
| Observation | 500-1000 | 5-20 observations |
| MedicationRequest | 200-400 | 2-5 active meds |
| Encounter | 300-500 | 3-10 encounters |
| Procedure | 100-200 | 1-3 procedures |
| Immunization | 50-100 | 0-8 (pediatric) |

---

## Phase 3: Implementation Steps

### 3.1 Prerequisite Tasks

**Task 1: Investigate FHIR Container Health Check**
```bash
# Check healthcheck configuration
docker inspect healthdata-fhir-mock --format='{{json .State.Health}}' | jq

# Test health endpoint directly
curl -v http://localhost:8083/fhir/metadata

# Review container logs for errors
docker compose logs fhir-service-mock --tail=100

# Potential Fix: Increase healthcheck timeout or interval
```

**Task 2: Validate Current Data Quality**
```bash
# Audit existing 73 patients
curl 'http://localhost:8083/fhir/Patient?_summary=count'

# Check for orphaned resources (resources without patient reference)
curl 'http://localhost:8083/fhir/Observation?_has:Patient:_id:missing=true'

# Verify tenant segregation
curl 'http://localhost:8083/fhir/Patient?identifier=demo-clinic'
```

**Task 3: Backup Current Data**
```bash
# Export current FHIR database
docker exec healthdata-postgres pg_dump -U healthdata healthdata_fhir > fhir_backup_$(date +%Y%m%d).sql

# Alternative: FHIR Bulk Export
curl 'http://localhost:8083/fhir/$export'
```

### 3.2 Data Generation and Loading

**Step 1: Install Synthea (if using hybrid approach)**
```bash
# Download Synthea
cd /tmp
wget https://github.com/synthetichealth/synthea/releases/download/master-branch-latest/synthea-with-dependencies.jar

# Generate 40 patients with specific conditions
java -jar synthea-with-dependencies.jar \
  -p 40 \
  --exporter.fhir.export=true \
  --exporter.fhir.bulk_data=true \
  Illinois Springfield

# Output location: ./output/fhir/*.json
```

**Step 2: Create Enhanced FHIR Population Script**
```bash
# Location: /home/webemo-aaron/projects/healthdata-in-motion/load-fhir-demo-data.sh
# Based on existing comprehensive-fhir-test-data.sh but targeted for quality measures

#!/bin/bash
set -e

FHIR_URL="${FHIR_URL:-http://localhost:8083/fhir}"
TENANT_ID="${TENANT_ID:-demo-clinic}"

# Function to create patient with conditions, observations, medications
# Scenario-specific patient generation
# Quality measure validation data
```

**Step 3: Import Synthea Data**
```bash
# Import Synthea FHIR bundles
for bundle in /tmp/output/fhir/*.json; do
  curl -X POST http://localhost:8083/fhir \
    -H "Content-Type: application/fhir+json" \
    -H "X-Tenant-ID: demo-clinic" \
    -d @"$bundle"
done

# Monitor import progress
watch "curl -s http://localhost:8083/fhir/Patient?_summary=count | jq '.total'"
```

**Step 4: Run Enhanced Population Script**
```bash
# Execute targeted quality measure patient creation
cd /home/webemo-aaron/projects/healthdata-in-motion
chmod +x load-fhir-demo-data.sh
./load-fhir-demo-data.sh
```

**Step 5: Validate Data Population**
```bash
# Execute comprehensive validation
./validate-fhir-data.sh

# Expected output:
# - Patient count: 50-100
# - Condition count: 100-200
# - Observation count: 500-1000
# - All quality measures have eligible populations
```

### 3.3 Quality Measure Integration Testing

**Step 1: Test CQL Engine FHIR Access**
```bash
# Verify CQL Engine can access FHIR server
docker compose logs cql-engine-service | grep -i "fhir\|patient"

# Test FHIR client connectivity from CQL Engine
curl -X POST http://localhost:8081/cql/evaluate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: demo-clinic" \
  -d '{
    "patientId": "1",
    "libraryName": "CMS2DepressionScreening",
    "version": "1.0.0"
  }'
```

**Step 2: Execute Quality Measure Calculations**
```bash
# Run quality measure against populated FHIR data
curl -X GET "http://localhost:8087/quality-measure/results?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: demo-clinic"

# Expected: Results showing compliant/non-compliant patients
```

**Step 3: Validate Care Gap Generation**
```bash
# Check care gaps are generated from FHIR data
curl -X GET "http://localhost:9000/api/care-gaps" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: demo-clinic"

# Expected: High/medium/low priority gaps for patients with conditions
```

### 3.4 Multi-Tenant Validation

**Step 1: Load Data for Secondary Tenant**
```bash
# Create clinic-001 tenant patients
TENANT_ID=clinic-001 ./load-fhir-demo-data.sh

# Verify segregation
curl 'http://localhost:8083/fhir/Patient?_tag=http://healthdata.example.org/tenant|clinic-001'
```

**Step 2: Test Cross-Tenant Isolation**
```bash
# Attempt to access demo-clinic data with clinic-001 token
# Should return empty or forbidden
```

---

## Phase 4: Demo Scenarios and Validation

### 4.1 Demo Scenario 1: End-to-End Quality Measure Flow

**Setup:**
- Patient: Thomas Anderson (ID: 1001), Age 60, Type 2 Diabetes
- Condition: Diabetes mellitus type 2 (SNOMED: 44054006)
- Observation: HbA1c = 9.5% (non-compliant, taken 3 months ago)
- Medication: Metformin 500mg BID

**Demo Steps:**
1. Show patient in FHIR server: `GET /fhir/Patient/1001`
2. Show diabetes condition: `GET /fhir/Condition?patient=1001`
3. Show HbA1c observation: `GET /fhir/Observation?patient=1001&code=4548-4`
4. Trigger quality measure calculation: `POST /cql/evaluate`
5. Show care gap generated: `GET /api/care-gaps?patientId=1001`
6. Show in Clinical Portal patient list with care gap indicator

**Talking Points:**
- "FHIR-native architecture means every data point is standards-based"
- "CQL engine retrieves clinical data directly from FHIR repository"
- "No custom ETL - this is how data flows in real healthcare systems"

### 4.2 Demo Scenario 2: Multi-Patient Quality Measure Dashboard

**Setup:**
- 15 patients with diabetes
- 10 compliant (HbA1c <8%)
- 5 non-compliant (HbA1c >9%)

**Demo Steps:**
1. Show quality measure dashboard: Overall 67% compliance
2. Drill into non-compliant patients
3. Show each patient's clinical data in FHIR
4. Generate outreach list from care gaps
5. Show personalized email generation

**Talking Points:**
- "Population health at scale - 100, 1000, 10000 patients"
- "Automated care gap identification saves clinical staff hours"
- "Real-time data from EHR through FHIR interface"

### 4.3 Demo Scenario 3: Clinical Portal Patient Detail

**Setup:**
- Patient: Sofia Martinez, Age 32, Depression screening due
- Multiple encounters, observations, medications

**Demo Steps:**
1. Navigate to patient detail page
2. Show demographics from FHIR Patient resource
3. Show vital signs from FHIR Observations
4. Show conditions from FHIR Condition resources
5. Show medications from FHIR MedicationRequest
6. Show care gap highlighting depression screening needed

**Talking Points:**
- "Single source of truth - FHIR server"
- "Real-time clinical context for care coordinators"
- "Actionable intelligence embedded in workflow"

### 4.4 Validation Checklist

**Technical Validation:**
- [ ] All 8 Docker services running
- [ ] FHIR server health check passing
- [ ] 50+ patients loaded with complete demographics
- [ ] 100+ conditions with proper SNOMED coding
- [ ] 500+ observations with proper LOINC coding
- [ ] 200+ medication requests with RxNorm coding
- [ ] Patient-resource relationships validated
- [ ] Multi-tenant segregation working
- [ ] CQL Engine can retrieve FHIR data
- [ ] Quality measures calculate successfully
- [ ] Care gaps generated from FHIR data
- [ ] Clinical Portal displays FHIR resources

**Functional Validation:**
- [ ] CMS2 (Depression Screening) - eligible population exists
- [ ] CMS122 (HbA1c Testing) - diabetic patients with labs
- [ ] CMS134 (Diabetic Nephropathy) - diabetes + kidney function labs
- [ ] CMS165 (BP Control) - hypertensive patients with BP readings
- [ ] Care gap priorities calculated correctly (high/medium/low)
- [ ] Patient detail shows complete clinical picture
- [ ] Quality measure results match expected compliance rates
- [ ] Demo runs smoothly without errors

**Demo Readiness:**
- [ ] Demo script updated with FHIR-specific talking points
- [ ] Sample patients memorized (names, IDs, conditions)
- [ ] FHIR URLs accessible and responsive
- [ ] Backup plan if FHIR server becomes unresponsive
- [ ] Screen recordings made for backup
- [ ] Cheat sheet includes FHIR resource queries

---

## Phase 5: Ongoing Maintenance and Enhancement

### 5.1 Data Refresh Strategy

**Weekly Refresh:**
- Clear stale data (>30 days old)
- Regenerate with updated scenarios
- Validate quality measure calculations

**Pre-Demo Refresh:**
- Run validation script
- Confirm all services healthy
- Reset any modified data

**Automated Refresh:**
```bash
# Cron job to refresh FHIR data weekly
0 2 * * 0 cd /home/webemo-aaron/projects/healthdata-in-motion && ./refresh-fhir-data.sh
```

### 5.2 Health Monitoring

**FHIR Server Monitoring:**
```bash
# Health check script
#!/bin/bash
# File: monitor-fhir-health.sh

# Check metadata endpoint
if curl -f -s http://localhost:8083/fhir/metadata > /dev/null; then
  echo "✅ FHIR server responding"
else
  echo "❌ FHIR server not responding"
  docker compose restart fhir-service-mock
fi

# Check resource counts
PATIENT_COUNT=$(curl -s 'http://localhost:8083/fhir/Patient?_summary=count' | jq -r '.total // 0')
if [ "$PATIENT_COUNT" -lt 50 ]; then
  echo "⚠️ Low patient count: $PATIENT_COUNT (expected 50+)"
fi
```

### 5.3 Enhancement Roadmap

**Short-term (Next 2 weeks):**
1. Implement real-time FHIR event notifications (FHIR Subscriptions)
2. Add DocumentReference resources for clinical documents
3. Implement FHIR search parameter optimization
4. Add SMART-on-FHIR authentication (if needed)

**Medium-term (Next 1-2 months):**
5. Integrate with real EHR FHIR endpoint (test environment)
6. Implement FHIR Bulk Data Export ($export operation)
7. Add FHIR validation profiles (US Core)
8. Performance testing with 10,000+ patients

**Long-term (Next 3-6 months):**
9. CDS Hooks integration for clinical decision support
10. FHIR questionnaires for patient-reported outcomes
11. HL7 FHIR Genomics support
12. International patient summary (IPS) implementation

---

## Phase 6: Risk Assessment and Mitigation

### 6.1 Technical Risks

**Risk 1: FHIR Server Performance Degradation**
- **Probability:** Medium
- **Impact:** High (demo failure)
- **Mitigation:** 
  - Load test before demo (ApacheBench, JMeter)
  - Implement database connection pooling
  - Add Redis caching layer for frequent queries
  - Prepared backup: pre-recorded demo videos

**Risk 2: Container Health Check Continues Failing**
- **Probability:** Low
- **Impact:** Low (cosmetic, service functional)
- **Mitigation:**
  - Adjust healthcheck interval and timeout
  - Change healthcheck endpoint to simpler path
  - Disable healthcheck if necessary (service proven stable)

**Risk 3: Data Quality Issues in Generated Resources**
- **Probability:** Medium
- **Impact:** Medium (confusing demo, invalid calculations)
- **Mitigation:**
  - Implement FHIR validator in data generation script
  - Manual review of sample patients before demo
  - Validation script checks for required fields
  - Use proven Synthea data as baseline

**Risk 4: Multi-Tenant Data Leakage**
- **Probability:** Low
- **Impact:** High (security concern, demo credibility)
- **Mitigation:**
  - Thorough testing of tenant isolation
  - Code review of FHIR service client tenant headers
  - Integration tests for cross-tenant access attempts
  - Demo uses single tenant to avoid complexity

### 6.2 Demo-Specific Risks

**Risk 5: Live Demo Connectivity Issues**
- **Probability:** Low
- **Impact:** High
- **Mitigation:**
  - Run all services locally (no cloud dependencies)
  - Test network connectivity before demo
  - Have offline backup (screenshots, videos)
  - Practice demo multiple times

**Risk 6: Complex Medical Terminology Confuses Audience**
- **Probability:** Medium
- **Impact:** Medium (audience disengagement)
- **Mitigation:**
  - Simplify medical examples for non-clinical audience
  - Use familiar conditions (diabetes, hypertension)
  - Provide glossary in demo materials
  - Focus on business value, not clinical minutiae

---

## Phase 7: Timeline and Resource Allocation

### 7.1 Implementation Timeline

**Day 1 (4-6 hours):**
- [ ] Investigate and fix FHIR container health check
- [ ] Backup current FHIR database
- [ ] Install and configure Synthea
- [ ] Generate 40 base patients with Synthea
- [ ] Import Synthea data to FHIR server

**Day 2 (4-6 hours):**
- [ ] Create enhanced FHIR population script
- [ ] Generate 20-30 targeted quality measure patients
- [ ] Populate Condition resources (CRITICAL)
- [ ] Populate additional Observations (HbA1c, BP, PHQ-9)
- [ ] Populate MedicationRequests for chronic conditions

**Day 3 (4-6 hours):**
- [ ] Validate data population (run validation script)
- [ ] Test CQL Engine integration with FHIR
- [ ] Execute quality measure calculations
- [ ] Verify care gap generation
- [ ] Test multi-tenant segregation

**Day 4 (2-4 hours):**
- [ ] Practice demo scenarios
- [ ] Update demo script with FHIR talking points
- [ ] Create FHIR query cheat sheet
- [ ] Record backup demo videos
- [ ] Final validation run

**Total Estimated Effort:** 14-22 hours (2-3 days)

### 7.2 Success Metrics

**Quantitative:**
- 50+ patients loaded ✅/❌
- 100+ conditions loaded ✅/❌
- 500+ observations loaded ✅/❌
- All quality measures have eligible populations ✅/❌
- CQL engine successfully retrieves FHIR data ✅/❌
- Care gaps generated for all non-compliant patients ✅/❌
- Demo runs in <15 minutes ✅/❌

**Qualitative:**
- FHIR integration demonstrates standards-based architecture
- Clinical scenarios are realistic and relatable
- Demo flows smoothly without technical issues
- Audience understands FHIR value proposition
- System demonstrates scalability and robustness

---

## Quick Start Commands

### Immediate Next Steps

```bash
# 1. Check current FHIR server status
docker compose ps fhir-service-mock
curl http://localhost:8083/fhir/metadata | jq '.fhirVersion'

# 2. Backup current data
docker exec healthdata-postgres pg_dump -U healthdata healthdata_fhir > fhir_backup_$(date +%Y%m%d).sql

# 3. Validate current data completeness
curl -s 'http://localhost:8083/fhir/Patient?_summary=count' | jq
curl -s 'http://localhost:8083/fhir/Condition?_summary=count' | jq
curl -s 'http://localhost:8083/fhir/Observation?_summary=count' | jq

# 4. Install Synthea
cd /tmp
wget https://github.com/synthetichealth/synthea/releases/download/master-branch-latest/synthea-with-dependencies.jar

# 5. Generate test patients
java -jar synthea-with-dependencies.jar -p 40 Illinois Springfield

# 6. Create targeted quality measure patients
cd /home/webemo-aaron/projects/healthdata-in-motion
# (Script creation in next phase)

# 7. Validate integration
./test-fhir-integration.sh
```

---

## Appendix

### A. FHIR Resource Reference Examples

**Patient Resource:**
```json
{
  "resourceType": "Patient",
  "id": "1001",
  "identifier": [{"system": "http://demo-clinic.example.org/mrn", "value": "MRN-1001"}],
  "name": [{"family": "Anderson", "given": ["Thomas"]}],
  "gender": "male",
  "birthDate": "1965-03-15"
}
```

**Condition Resource (Diabetes):**
```json
{
  "resourceType": "Condition",
  "subject": {"reference": "Patient/1001"},
  "code": {
    "coding": [{
      "system": "http://snomed.info/sct",
      "code": "44054006",
      "display": "Diabetes mellitus type 2"
    }]
  },
  "clinicalStatus": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/condition-clinical",
      "code": "active"
    }]
  }
}
```

**Observation Resource (HbA1c):**
```json
{
  "resourceType": "Observation",
  "status": "final",
  "subject": {"reference": "Patient/1001"},
  "code": {
    "coding": [{
      "system": "http://loinc.org",
      "code": "4548-4",
      "display": "Hemoglobin A1c/Hemoglobin.total in Blood"
    }]
  },
  "valueQuantity": {
    "value": 9.5,
    "unit": "%",
    "system": "http://unitsofmeasure.org",
    "code": "%"
  },
  "effectiveDateTime": "2025-08-15"
}
```

### B. Quality Measure FHIR Requirements

**CMS2 (Depression Screening):**
- Patient: Age 12+
- Encounter: Office visit or telehealth
- Observation: PHQ-9 (LOINC: 44249-1)
- Timeframe: Within 12 months

**CMS122 (Diabetes HbA1c Testing):**
- Condition: Diabetes mellitus (SNOMED: 44054006, 46635009)
- Observation: HbA1c (LOINC: 4548-4)
- Timeframe: Within 12 months
- Target: At least one HbA1c test per year

**CMS134 (Diabetic Nephropathy Screening):**
- Condition: Diabetes mellitus
- Observation: Urine albumin (LOINC: 14958-3) OR eGFR (LOINC: 48643-1)
- Timeframe: Within 12 months

**CMS165 (Hypertension Blood Pressure Control):**
- Condition: Hypertension (SNOMED: 59621000)
- Observation: BP (LOINC: 8480-6 systolic, 8462-4 diastolic)
- Target: Most recent BP <140/90 mmHg

### C. Useful FHIR Queries

```bash
# Get all patients
curl 'http://localhost:8083/fhir/Patient'

# Get patient by ID
curl 'http://localhost:8083/fhir/Patient/1001'

# Search patients by name
curl 'http://localhost:8083/fhir/Patient?name=Anderson'

# Get all conditions for a patient
curl 'http://localhost:8083/fhir/Condition?patient=1001'

# Get specific observations (HbA1c) for a patient
curl 'http://localhost:8083/fhir/Observation?patient=1001&code=4548-4'

# Get observations within date range
curl 'http://localhost:8083/fhir/Observation?patient=1001&date=ge2025-01-01&date=le2025-12-31'

# Get active medications for a patient
curl 'http://localhost:8083/fhir/MedicationRequest?patient=1001&status=active'

# Count resources
curl 'http://localhost:8083/fhir/Patient?_summary=count'

# Get recent encounters
curl 'http://localhost:8083/fhir/Encounter?patient=1001&_sort=-date&_count=5'
```

### D. References and Resources

**FHIR Specifications:**
- FHIR R4 Specification: http://hl7.org/fhir/R4/
- US Core Implementation Guide: http://hl7.org/fhir/us/core/
- HAPI FHIR Documentation: https://hapifhir.io/

**Quality Measures:**
- CMS HEDIS Measures: https://www.cms.gov/medicare/quality/measures/hedis
- CQL Specification: https://cql.hl7.org/
- FHIR Clinical Reasoning Module: http://hl7.org/fhir/clinicalreasoning-module.html

**Data Generation Tools:**
- Synthea Patient Generator: https://github.com/synthetichealth/synthea
- FHIR Bulk Data Generator: https://github.com/smart-on-fhir/bulk-data-server

**Testing and Validation:**
- FHIR Validator: https://github.com/hapifhir/org.hl7.fhir.core
- Inferno Testing Framework: https://inferno.healthit.gov/

---

## Document Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-11-24 | 1.0 | Initial plan creation | GitHub Copilot |

---

**Next Actions:**
1. ✅ Review and approve this plan
2. 🔲 Investigate FHIR container health check issue
3. 🔲 Install Synthea and generate base patient population
4. 🔲 Create enhanced FHIR population script for targeted scenarios
5. 🔲 Execute data loading and validation
6. 🔲 Update demo script with FHIR integration points
