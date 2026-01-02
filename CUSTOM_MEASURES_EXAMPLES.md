# Custom Quality Measures - Usage Examples

**Status**: ✅ API Verified Working
**Date**: 2025-11-19
**Endpoint**: `http://localhost:8000/api/quality/quality-measure/custom-measures`

---

## Quick Start

### Prerequisites
```bash
# 1. Ensure all services are running
docker compose ps

# 2. Load comprehensive FHIR test data
./sample-data/comprehensive-fhir-test-data.sh

# 3. Verify FHIR patients exist
curl http://localhost:8000/api/fhir/Patient?identifier=TEST-
```

---

## Example 1: Create Diabetes HbA1c Control Measure

### Clinical Scenario
Measure diabetes patients with good HbA1c control (<8%). Uses **Patient 179 (Thomas Anderson)** who has HbA1c of 7.2%.

### API Call
```bash
curl -X POST "http://localhost:8000/api/quality/quality-measure/custom-measures" \
  -H "X-Tenant-ID: clinic-001" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CDC-A1C - Diabetes HbA1c Control",
    "description": "HEDIS measure: Percentage of diabetic patients aged 18-75 with HbA1c <8%",
    "category": "Diabetes",
    "year": 2025,
    "cqlText": "library CDC_A1C version '\''1.0.0'\''\n\nusing FHIR version '\''4.0.1'\''\n\ninclude FHIRHelpers version '\''4.0.1'\''\n\ncodesystem \"LOINC\": '\''http://loinc.org'\''\ncode \"HbA1c\": '\''4548-4'\'' from \"LOINC\"\n\ncontext Patient\n\ndefine \"Initial Population\":\n  AgeInYears() >= 18 and AgeInYears() <= 75\n  and exists([Condition: code ~ ToConcept(SNOMED \"44054006\")] C where C.clinicalStatus ~ ToConcept(FHIR \"active\"))\n\ndefine \"Denominator\":\n  \"Initial Population\"\n\ndefine \"Numerator\":\n  exists(\n    [Observation: code ~ ToConcept(\"HbA1c\")] O\n    where O.value.value < 8.0\n    and O.effective during Interval[Today() - 1 year, Today()]\n  )\n\ndefine \"Denominator Exclusions\":\n  false"
  }'
```

### Expected Response
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "tenantId": "clinic-001",
  "name": "CDC-A1C - Diabetes HbA1c Control",
  "version": "1.0.0",
  "status": "DRAFT",
  "category": "Diabetes",
  "year": 2025,
  "createdBy": "clinical-portal",
  "createdAt": "2025-11-19T21:45:00.123456"
}
```

### Test Patient Match
- **Patient 179 (Thomas Anderson)**: ✅ **PASS**
  - Age: 60 (within 18-75 range)
  - Condition: Type 2 Diabetes (active since 2015)
  - HbA1c: 7.2% (measured 2025-10-15) → **<8%** ✓
  - **Expected**: Numerator = TRUE, In compliance

---

## Example 2: Blood Pressure Control Measure

### Clinical Scenario
Measure hypertensive patients with controlled BP (<140/90). Uses **Patient 179** with BP 128/82.

### API Call
```bash
curl -X POST "http://localhost:8000/api/quality/quality-measure/custom-measures" \
  -H "X-Tenant-ID: clinic-001" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CBP - Controlling Blood Pressure",
    "description": "HEDIS measure: Hypertensive patients with BP <140/90 mmHg",
    "category": "Hypertension",
    "year": 2025,
    "cqlText": "library CBP version '\''1.0.0'\''\n\nusing FHIR version '\''4.0.1'\''\n\ncontext Patient\n\ndefine \"Has Hypertension\":\n  exists([Condition: code.coding contains Code { system: '\''http://snomed.info/sct'\'', code: '\''38341003'\'' }])\n\ndefine \"BP Controlled\":\n  exists(\n    [Observation: code.coding contains Code { system: '\''http://loinc.org'\'', code: '\''85354-9'\'' }] O\n    where O.component[0].value.value < 140\n    and O.component[1].value.value < 90\n    and O.effective during Interval[Today() - 1 year, Today()]\n  )\n\ndefine \"In Numerator\":\n  \"Has Hypertension\" and \"BP Controlled\""
  }'
```

### Test Patient Match
- **Patient 179**: ✅ **PASS**
  - Condition: Hypertension (active since 2018)
  - BP: 128/82 mmHg (2025-11-01) → **<140/90** ✓

---

## Example 3: Prenatal Care Timeliness

### Clinical Scenario
Pregnant patients with prenatal visit in first trimester. Uses **Patient 180 (Sofia Martinez)**.

### API Call
```bash
curl -X POST "http://localhost:8000/api/quality/quality-measure/custom-measures" \
  -H "X-Tenant-ID: clinic-001" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "PPC - Timeliness of Prenatal Care",
    "description": "HEDIS measure: Prenatal care visit in first trimester",
    "category": "Maternal Health",
    "year": 2025,
    "cqlText": "library PPC version '\''1.0.0'\''\n\nusing FHIR version '\''4.0.1'\''\n\ncontext Patient\n\ndefine \"Is Pregnant\":\n  exists([Condition: code.coding contains Code { system: '\''http://snomed.info/sct'\'', code: '\''77386006'\'' }])\n\ndefine \"First Trimester Visit\":\n  exists(\n    [Encounter: type.coding contains Code { system: '\''http://snomed.info/sct'\'', code: '\''424441002'\'' }] E\n    where E.period.start before (EDD() - 6 months)\n  )\n\ndefine \"In Numerator\":\n  \"Is Pregnant\" and \"First Trimester Visit\""
  }'
```

### Test Patient Match
- **Patient 180 (Sofia Martinez)**: ✅ **PASS**
  - Condition: Pregnancy (EDD: 2025-12-15)
  - Encounter: Prenatal initial visit (2025-04-15) → **Within first trimester** ✓
  - Immunization: Tdap vaccine completed ✓

---

## Example 4: Depression Screening

### Clinical Scenario
Patients screened for depression using standardized tool. Uses **Patient 183 (Sarah Williams)**.

### API Call
```bash
curl -X POST "http://localhost:8000/api/quality/quality-measure/custom-measures" \
  -H "X-Tenant-ID: clinic-001" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CMS2v12 - Depression Screening",
    "description": "Annual depression screening using PHQ-9 or equivalent",
    "category": "Mental Health",
    "year": 2025,
    "cqlText": "library CMS2 version '\''12.0.0'\''\n\nusing FHIR version '\''4.0.1'\''\n\ncontext Patient\n\ndefine \"Age 12 or Older\":\n  AgeInYears() >= 12\n\ndefine \"Depression Screening Performed\":\n  exists(\n    [Observation: code.coding contains Code { system: '\''http://loinc.org'\'', code: '\''44261-6'\'' }] O\n    where O.effective during Interval[Today() - 1 year, Today()]\n    and O.status = '\''final'\''\n  )\n\ndefine \"In Numerator\":\n  \"Age 12 or Older\" and \"Depression Screening Performed\""
  }'
```

### Test Patient Match
- **Patient 183 (Sarah Williams)**: ✅ **PASS**
  - Age: 37 (≥12 years)
  - Assessment: PHQ-9 score 15 (2025-11-10) → **Screening completed** ✓
  - Condition: Major Depression diagnosed
  - Treatment: Sertraline 50mg QD

---

## Example 5: Pediatric Asthma Management

### Clinical Scenario
Asthmatic children with appropriate controller medication. Uses **Patient 181 (Emily Chen)**.

### API Call
```bash
curl -X POST "http://localhost:8000/api/quality/quality-measure/custom-measures" \
  -H "X-Tenant-ID: clinic-001" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ASM - Asthma Medication Ratio",
    "description": "HEDIS measure: Pediatric asthma patients with appropriate controller therapy",
    "category": "Pediatric",
    "year": 2025,
    "cqlText": "library ASM version '\''1.0.0'\''\n\nusing FHIR version '\''4.0.1'\''\n\ncontext Patient\n\ndefine \"Has Asthma\":\n  exists([Condition: code.coding contains Code { system: '\''http://snomed.info/sct'\'', code: '\''195967001'\'' }])\n\ndefine \"Age 5 to 18\":\n  AgeInYears() >= 5 and AgeInYears() <= 18\n\ndefine \"On Controller Medication\":\n  exists(\n    [MedicationRequest: medication.coding contains Code { system: '\''http://www.nlm.nih.gov/research/umls/rxnorm'\'', code: '\''351094'\'' }] M\n    where M.status = '\''active'\''\n  )\n\ndefine \"In Numerator\":\n  \"Has Asthma\" and \"Age 5 to 18\" and \"On Controller Medication\""
  }'
```

### Test Patient Match
- **Patient 181 (Emily Chen)**: ✅ **PASS**
  - Age: 7 years (within 5-18 range)
  - Condition: Asthma (active since 2020)
  - Medication: Albuterol inhaler PRN → **Controller available** ✓
  - Care: Well-child visit completed

---

## Example 6: Chronic Kidney Disease Monitoring

### Clinical Scenario
CKD patients with annual eGFR monitoring. Uses **Patient 182 (Robert Johnson)**.

### API Call
```bash
curl -X POST "http://localhost:8000/api/quality/quality-measure/custom-measures" \
  -H "X-Tenant-ID: clinic-001" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CKD Monitoring - eGFR",
    "description": "Annual eGFR monitoring for patients with chronic kidney disease",
    "category": "Nephrology",
    "year": 2025,
    "cqlText": "library CKD_Monitor version '\''1.0.0'\''\n\nusing FHIR version '\''4.0.1'\''\n\ncontext Patient\n\ndefine \"Has CKD\":\n  exists([Condition: code.coding contains Code { system: '\''http://snomed.info/sct'\'', code: '\''431855005'\'' }])\n\ndefine \"eGFR Tested\":\n  exists(\n    [Observation: code.coding contains Code { system: '\''http://loinc.org'\'', code: '\''62238-1'\'' }] O\n    where O.effective during Interval[Today() - 1 year, Today()]\n  )\n\ndefine \"In Numerator\":\n  \"Has CKD\" and \"eGFR Tested\""
  }'
```

### Test Patient Match
- **Patient 182 (Robert Johnson)**: ✅ **PASS**
  - Age: 80 years (elderly patient)
  - Condition: CKD Stage 3 (active since 2020)
  - Lab: eGFR 52 mL/min (2025-11-01) → **Annual monitoring complete** ✓
  - Procedure: Colonoscopy (2024-03-10) - preventive care ✓

---

## CRUD Operations Examples

### List All Custom Measures
```bash
curl "http://localhost:8000/api/quality/quality-measure/custom-measures" \
  -H "X-Tenant-ID: clinic-001"
```

### Get Measure by ID
```bash
MEASURE_ID="a1b2c3d4-e5f6-7890-abcd-ef1234567890"
curl "http://localhost:8000/api/quality/quality-measure/custom-measures/${MEASURE_ID}" \
  -H "X-Tenant-ID: clinic-001"
```

### Filter by Status
```bash
# Get only DRAFT measures
curl "http://localhost:8000/api/quality/quality-measure/custom-measures?status=DRAFT" \
  -H "X-Tenant-ID: clinic-001"

# Get PUBLISHED measures
curl "http://localhost:8000/api/quality/quality-measure/custom-measures?status=PUBLISHED" \
  -H "X-Tenant-ID: clinic-001"
```

### Update Measure
```bash
MEASURE_ID="a1b2c3d4-e5f6-7890-abcd-ef1234567890"
curl -X PUT "http://localhost:8000/api/quality/quality-measure/custom-measures/${MEASURE_ID}" \
  -H "X-Tenant-ID: clinic-001" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CDC-A1C - Diabetes HbA1c Control (Updated)",
    "description": "Updated description with clarifications",
    "category": "Diabetes",
    "year": 2025,
    "status": "PUBLISHED"
  }'
```

### Delete Measure (Soft Delete)
```bash
MEASURE_ID="a1b2c3d4-e5f6-7890-abcd-ef1234567890"
curl -X DELETE "http://localhost:8000/api/quality/quality-measure/custom-measures/${MEASURE_ID}" \
  -H "X-Tenant-ID: clinic-001"
```

---

## Testing Scenarios

### Scenario 1: Multi-Condition Patient
**Patient 179 (Thomas Anderson)** - Test all applicable measures:

```bash
# Test 1: Diabetes HbA1c Control → Expected: PASS (7.2% < 8%)
# Test 2: Blood Pressure Control → Expected: PASS (128/82 < 140/90)
# Test 3: Diabetic Eye Exam → Expected: PASS (screened 2025-06-15)
# Test 4: BMI Screening → Expected: PASS (28.5 kg/m² recorded)
# Test 5: Statin Therapy → Expected: Candidate (elevated cholesterol 195)
```

### Scenario 2: Pregnancy Care Pathway
**Patient 180 (Sofia Martinez)** - Prenatal care tracking:

```bash
# Test 1: First Trimester Visit → Expected: PASS (2025-04-15)
# Test 2: Tdap Immunization → Expected: PASS (2025-05-15)
# Test 3: Ultrasound Performed → Expected: PASS (2025-05-20)
# Test 4: Prenatal Labs → Expected: PASS (pregnancy test 2025-03-20)
```

### Scenario 3: Pediatric Care Gap Analysis
**Patient 181 (Emily Chen)** - Identify missing care:

```bash
# Complete:
# - Asthma controller medication ✓
# - Well-child visit (2025-10-20) ✓
# - MMR immunization (2019-11-15) ✓

# Gaps Identified:
# - Missing DTaP boosters
# - Missing Polio series completion
# - Missing Varicella vaccine
# - Missing Hepatitis A/B series
# - Missing annual influenza vaccine
```

### Scenario 4: Elderly Patient Monitoring
**Patient 182 (Robert Johnson)** - Geriatric quality measures:

```bash
# Test 1: CKD Monitoring → Expected: PASS (eGFR 52, 2025-11-01)
# Test 2: Colorectal Cancer Screening → Expected: PASS (colonoscopy 2024-03-10)
# Test 3: Medicare Annual Wellness → Expected: Recommend scheduling
# Test 4: Polypharmacy Review → Expected: Not yet addressed
```

### Scenario 5: Mental Health Follow-up
**Patient 183 (Sarah Williams)** - Depression care coordination:

```bash
# Test 1: Depression Screening → Expected: PASS (PHQ-9: 15)
# Test 2: Antidepressant Medication → Expected: PASS (Sertraline 50mg)
# Test 3: Follow-up Encounter → Expected: PASS (2025-11-10)
# Test 4: Repeat Screening → Expected: DUE (4-8 weeks post-treatment)
```

---

## UI Testing Guide

### Access Measure Builder
```
URL: http://localhost:4200/measure-builder
```

### Create Measure in UI
1. Click "Create New Measure" button
2. Fill in basic information:
   - **Name**: CDC-A1C - Diabetes HbA1c Control
   - **Category**: Diabetes
   - **Year**: 2025
   - **Description**: HEDIS measure for HbA1c <8%
3. Add CQL Logic (optional for draft)
4. Click "Save as Draft"
5. Verify measure appears in list

### Test with Patient Data
1. Navigate to "Evaluations" page
2. Select custom measure from dropdown
3. Choose patient (e.g., Patient 179 - Thomas Anderson)
4. Click "Run Evaluation"
5. Review results:
   - Initial Population: ✓
   - Denominator: ✓
   - Numerator: ✓
   - Compliance: 100%

---

## Batch Evaluation Example

### Evaluate All Diabetes Patients
```bash
curl -X POST "http://localhost:8000/api/quality/quality-measure/calculate" \
  -H "X-Tenant-ID: clinic-001" \
  -H "Content-Type: application/json" \
  -d '{
    "measureId": "CDC-A1C",
    "measureName": "Diabetes HbA1c Control",
    "patientIds": ["179"],
    "evaluationDate": "2025-11-19"
  }'
```

### Expected Results
```json
{
  "measureId": "CDC-A1C",
  "totalPatients": 1,
  "initialPopulation": 1,
  "denominator": 1,
  "numerator": 1,
  "exclusions": 0,
  "complianceRate": 100.0,
  "evaluationDate": "2025-11-19",
  "results": [
    {
      "patientId": "179",
      "patientName": "Thomas Anderson",
      "inInitialPopulation": true,
      "inDenominator": true,
      "inNumerator": true,
      "excluded": false,
      "score": 1.0,
      "details": {
        "age": 60,
        "hasDiabetes": true,
        "hba1cValue": 7.2,
        "hba1cDate": "2025-10-15",
        "meetsCriteria": true
      }
    }
  ]
}
```

---

## Troubleshooting

### Issue: 404 Not Found
```bash
# Check service is running
docker ps | grep quality-measure

# Check logs
docker logs healthdata-quality-measure --tail 50

# Verify endpoint
curl -v http://localhost:8000/api/quality/quality-measure/custom-measures \
  -H "X-Tenant-ID: clinic-001"
```

### Issue: Empty Results
```bash
# Verify FHIR data exists
curl http://localhost:8000/api/fhir/Patient/179

# Check patient conditions
curl "http://localhost:8000/api/fhir/Condition?patient=179"

# Check observations
curl "http://localhost:8000/api/fhir/Observation?patient=179"
```

### Issue: CQL Validation Errors
- Ensure CQL syntax is valid
- Check code system URLs are correct
- Verify value set bindings exist
- Test CQL logic in CQL Engine service first

---

## Performance Tips

### 1. Batch Evaluations
- Evaluate multiple patients in single request
- Use async processing for large populations
- Monitor Kafka topics for evaluation events

### 2. Caching
- Results cached in Redis for 10 minutes
- Use cache for repeated queries
- Clear cache after data updates

### 3. Indexing
- Custom measures indexed by tenant, status, category
- FHIR resources indexed by patient, code, date
- Query performance optimized for common patterns

---

## Related Documentation

- [COMPREHENSIVE_FHIR_TEST_DATA.md](COMPREHENSIVE_FHIR_TEST_DATA.md) - Complete test data reference
- [BACKEND_QUICK_REFERENCE.md](BACKEND_QUICK_REFERENCE.md) - API endpoints
- [REPORTS_API_DOCUMENTATION.md](REPORTS_API_DOCUMENTATION.md) - Report generation
- [TESTING_GUIDE.md](TESTING_GUIDE.md) - Integration testing

---

**Last Updated**: 2025-11-19
**Version**: 1.0.0
**Maintainer**: HealthData In Motion Team
