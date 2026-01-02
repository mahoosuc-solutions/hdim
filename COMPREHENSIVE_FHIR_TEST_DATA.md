# Comprehensive FHIR Test Data

**Status**: ✅ Complete
**Date**: 2025-11-19
**Resources Created**: 37
**FHIR Server**: http://localhost:8000/api/fhir

---

## Overview

This document describes the exhaustive FHIR test data created to support quality measure evaluation, clinical decision support, and comprehensive system testing.

## Test Patients

### Patient 1: Thomas Anderson (ID: 179)
**Demographics**:
- MRN: TEST-1001
- SSN: 123-45-6789
- DOB: 1965-03-15 (Age: 60)
- Gender: Male
- Address: 123 Main St, Apt 4B, Springfield, IL 62701
- Contact: 555-0101, thomas.anderson@example.com
- Marital Status: Married

**Clinical Profile**:
- **Conditions**: Type 2 Diabetes (since 2015), Hypertension (since 2018)
- **Medications**: Metformin 500mg BID, Lisinopril 10mg QD
- **Allergies**: Penicillin (anaphylaxis - severe)
- **Recent Labs**:
  - HbA1c: 7.2% (good control) - 2025-10-15
  - BP: 128/82 mmHg (controlled) - 2025-11-01
  - BMI: 28.5 kg/m² (overweight)
  - Total Cholesterol: 195 mg/dL
- **Procedures**: Diabetic retinopathy screening (2025-06-15)
- **Immunizations**: Influenza vaccine (2025-10-01)
- **Care Plan**: Active diabetes management plan

**Quality Measures Applicable**:
- ✅ CDC-A1C (Diabetes HbA1c Control) - **MEETS**
- ✅ CBP (Controlling Blood Pressure) - **MEETS**
- ✅ Diabetic Eye Exam - **MEETS**
- ✅ Statin Therapy - Candidate
- ✅ Aspirin Therapy - Candidate

---

### Patient 2: Sofia Martinez (ID: 180)
**Demographics**:
- MRN: TEST-1002
- DOB: 1992-07-22 (Age: 33)
- Gender: Female
- Address: 456 Oak Avenue, Springfield, IL 62702

**Clinical Profile**:
- **Conditions**: Pregnancy (EDD: 2025-12-15)
- **Medications**: Prenatal vitamins QD
- **Labs**: Pregnancy test positive (2025-03-20)
- **Procedures**: Obstetric ultrasound (2025-05-20)
- **Immunizations**: Tdap vaccine (2025-05-15)
- **Encounters**: Prenatal initial visit (2025-04-15)

**Quality Measures Applicable**:
- ✅ Prenatal Care - **MEETS**
- ✅ Tdap Immunization in Pregnancy - **MEETS**
- ✅ Prenatal Screening - Active

---

### Patient 3: Emily Chen (ID: 181)
**Demographics**:
- MRN: TEST-1003
- DOB: 2018-11-10 (Age: 7)
- Gender: Female
- Guardian: David Chen (555-0103)

**Clinical Profile**:
- **Conditions**: Asthma (since 2020)
- **Medications**: Albuterol inhaler PRN
- **Allergies**: Peanut allergy (moderate - rash)
- **Immunizations**: MMR (2019-11-15)
- **Encounters**: Well-child visit (2025-10-20)

**Quality Measures Applicable**:
- ✅ Childhood Immunization Status - Partial
- ✅ Asthma Medication Ratio - Active
- ✅ Well-Child Visits - **MEETS**

---

### Patient 4: Robert Johnson (ID: 182)
**Demographics**:
- MRN: TEST-1004
- DOB: 1945-01-05 (Age: 80)
- Gender: Male
- Suffix: Sr.

**Clinical Profile**:
- **Conditions**: Chronic Kidney Disease Stage 3 (since 2020)
- **Labs**: eGFR 52 mL/min (2025-11-01)
- **Procedures**: Colonoscopy (2024-03-10)

**Quality Measures Applicable**:
- ✅ CKD Monitoring - **MEETS**
- ✅ Colorectal Cancer Screening - **MEETS**
- ✅ Medicare Annual Wellness Visit - Candidate

---

### Patient 5: Sarah Williams (ID: 183)
**Demographics**:
- MRN: TEST-1005
- DOB: 1988-05-30 (Age: 37)
- Gender: Female

**Clinical Profile**:
- **Conditions**: Major Depressive Disorder (since 2022)
- **Medications**: Sertraline 50mg QD
- **Assessments**: PHQ-9 score: 15 (moderately severe) - 2025-11-10
- **Encounters**: Mental health follow-up (2025-11-10)

**Quality Measures Applicable**:
- ✅ Depression Screening - **MEETS**
- ✅ Depression Remission - Active monitoring
- ✅ Follow-up After ED Visit for Mental Illness - If applicable

---

## Resource Summary by Type

### Patients (5)
| ID  | Name              | Age | Conditions                  | Test Focus                    |
|-----|-------------------|-----|-----------------------------|-------------------------------|
| 179 | Thomas Anderson   | 60  | Diabetes, Hypertension      | Chronic disease management    |
| 180 | Sofia Martinez    | 33  | Pregnancy                   | Maternal health               |
| 181 | Emily Chen        | 7   | Asthma                      | Pediatric care                |
| 182 | Robert Johnson    | 80  | CKD Stage 3                 | Elderly care, CKD monitoring  |
| 183 | Sarah Williams    | 37  | Major Depression            | Mental health                 |

### Conditions (6)
| ID  | Condition                    | Patient | ICD-10   | SNOMED     |
|-----|------------------------------|---------|----------|------------|
| 184 | Diabetes Type 2              | 179     | E11.9    | 44054006   |
| 185 | Hypertension                 | 179     | I10      | 38341003   |
| 186 | Pregnancy                    | 180     | Z34.90   | 77386006   |
| 187 | Asthma                       | 181     | J45.909  | 195967001  |
| 188 | CKD Stage 3                  | 182     | N18.3    | 431855005  |
| 189 | Major Depression             | 183     | F33.1    | 370143000  |

### Observations (7)
| ID  | Type                  | Value         | Patient | Date       |
|-----|-----------------------|---------------|---------|------------|
| 190 | Blood Pressure        | 128/82 mmHg   | 179     | 2025-11-01 |
| 191 | HbA1c                 | 7.2%          | 179     | 2025-10-15 |
| 192 | BMI                   | 28.5 kg/m²    | 179     | 2025-11-01 |
| 193 | Total Cholesterol     | 195 mg/dL     | 179     | 2025-10-15 |
| 194 | eGFR                  | 52 mL/min     | 182     | 2025-11-01 |
| 195 | Pregnancy Test        | Positive      | 180     | 2025-03-20 |
| 196 | PHQ-9                 | 15 (High)     | 183     | 2025-11-10 |

### Medications (5)
| ID  | Medication            | Dose      | Frequency | Patient | Indication      |
|-----|-----------------------|-----------|-----------|---------|-----------------|
| 197 | Metformin             | 500mg     | BID       | 179     | Diabetes        |
| 198 | Lisinopril            | 10mg      | QD        | 179     | Hypertension    |
| 199 | Prenatal Vitamins     | -         | QD        | 180     | Pregnancy       |
| 200 | Albuterol Inhaler     | 90 MCG    | PRN       | 181     | Asthma          |
| 201 | Sertraline            | 50mg      | QD        | 183     | Depression      |

### Encounters (4)
| ID  | Type                      | Patient | Date       |
|-----|---------------------------|---------|------------|
| 202 | Annual Wellness Visit     | 179     | 2025-11-01 |
| 203 | Prenatal Visit            | 180     | 2025-04-15 |
| 204 | Well-Child Visit          | 181     | 2025-10-20 |
| 205 | Mental Health Follow-up   | 183     | 2025-11-10 |

### Procedures (3)
| ID  | Procedure                       | Patient | Date       | CPT   |
|-----|---------------------------------|---------|------------|-------|
| 206 | Diabetic Retinopathy Screening  | 179     | 2025-06-15 | 92250 |
| 207 | Obstetric Ultrasound            | 180     | 2025-05-20 | 76805 |
| 208 | Colonoscopy                     | 182     | 2024-03-10 | 45378 |

### Immunizations (3)
| ID  | Vaccine         | Patient | Date       | CVX |
|-----|-----------------|---------|------------|-----|
| 209 | Influenza       | 179     | 2025-10-01 | 141 |
| 210 | Tdap            | 180     | 2025-05-15 | 115 |
| 211 | MMR             | 181     | 2019-11-15 | 03  |

### Allergies (2)
| ID  | Allergen  | Type       | Severity | Reaction      | Patient |
|-----|-----------|------------|----------|---------------|---------|
| 212 | Penicillin| Medication | Severe   | Anaphylaxis   | 179     |
| 213 | Peanut    | Food       | Moderate | Rash          | 181     |

### Care Plans (1)
| ID  | Title                        | Patient | Status  |
|-----|------------------------------|---------|---------|
| 214 | Diabetes Management Plan     | 179     | Active  |

### Diagnostic Reports (1)
| ID  | Report      | Patient | Date       | Status |
|-----|-------------|---------|------------|--------|
| 215 | Lipid Panel | 179     | 2025-10-15 | Final  |

---

## Quality Measure Coverage

### HEDIS Measures Supported

#### CDC - Comprehensive Diabetes Care
- ✅ **HbA1c Testing** - Patient 179 has HbA1c result
- ✅ **HbA1c Control (<8%)** - Patient 179 at 7.2% (**MEETS**)
- ✅ **Blood Pressure Control** - Patient 179 BP 128/82 (**MEETS**)
- ✅ **Eye Exam** - Patient 179 had retinopathy screening (**MEETS**)

#### CBP - Controlling High Blood Pressure
- ✅ **BP Control (<140/90)** - Patient 179 at 128/82 (**MEETS**)

#### CHL - Chlamydia Screening in Women
- ⚠️ Partial - Would need additional test data for applicable patients

#### PPC - Prenatal and Postpartum Care
- ✅ **Timeliness of Prenatal Care** - Patient 180 (**MEETS**)
- ✅ **Tdap Immunization** - Patient 180 (**MEETS**)

#### CIS - Childhood Immunization Status
- ⚠️ Partial - Patient 181 has MMR, would need additional vaccines

#### ASM - Asthma Medication Ratio
- ✅ Patient 181 on controller medication (**MEETS**)

### CMS Measures Supported

#### CMS122v11 - Diabetes: Hemoglobin A1c (HbA1c) Poor Control (>9%)
- ✅ Patient 179 data available - **PASS** (7.2%)

#### CMS165v11 - Controlling High Blood Pressure
- ✅ Patient 179 data available - **PASS** (128/82)

#### CMS130v11 - Colorectal Cancer Screening
- ✅ Patient 182 had colonoscopy - **MEETS**

#### CMS2v12 - Preventive Care and Screening: Depression Screening
- ✅ Patient 183 has PHQ-9 screening - **MEETS**

---

## Data Quality Features

### Comprehensive Coding
- **SNOMED CT** - Clinical terminology
- **LOINC** - Lab observations
- **RxNorm** - Medications
- **CPT** - Procedures
- **ICD-10-CM** - Diagnoses
- **CVX** - Vaccines

### Temporal Coverage
- Historical data (2015-2024)
- Current data (2025)
- Future dates (pregnancy EDD)

### Demographic Diversity
- Age range: 7 to 80 years
- Both genders represented
- Pregnant female
- Pediatric patient
- Elderly patient

### Clinical Complexity
- Single condition patients
- Multiple chronic conditions (comorbidities)
- Acute conditions (pregnancy)
- Chronic conditions (diabetes, CKD)
- Mental health conditions

### Care Continuum
- Preventive care (immunizations, screenings)
- Acute care (procedures)
- Chronic care management (care plans)
- Specialty care (mental health, obstetrics)

---

## Testing Scenarios

### Scenario 1: Diabetes Quality Reporting
**Test**: Generate quality report for diabetes patients

**Expected Results**:
- 1 patient in denominator (Patient 179)
- HbA1c < 8%: **PASS**
- BP < 140/90: **PASS**
- Eye exam: **PASS**
- Overall compliance: **100%**

### Scenario 2: Maternal Health Tracking
**Test**: Track prenatal care compliance

**Expected Results**:
- 1 pregnant patient (Patient 180)
- Prenatal visit in 1st trimester: **PASS**
- Tdap vaccination: **PASS**
- Ultrasound performed: **PASS**

### Scenario 3: Pediatric Care Gaps
**Test**: Identify missing immunizations

**Expected Results**:
- Patient 181 (age 7) needs:
  - DTaP boosters
  - Polio series
  - Varicella
  - Hepatitis A/B
  - Annual flu vaccine

### Scenario 4: Mental Health Screening
**Test**: Depression screening and follow-up

**Expected Results**:
- Patient 183 screened (PHQ-9: 15)
- On treatment (Sertraline)
- Follow-up encounter documented
- Needs repeat PHQ-9 in 4-8 weeks

### Scenario 5: Chronic Disease Management
**Test**: Multi-condition patient tracking

**Expected Results**:
- Patient 179 has 2 chronic conditions
- Both conditions being treated
- Regular monitoring documented
- Active care plan in place

---

## API Query Examples

### Get All Test Patients
```bash
curl http://localhost:8000/api/fhir/Patient?identifier=TEST-
```

### Get Diabetes Patients
```bash
curl "http://localhost:8000/api/fhir/Condition?code=44054006"
```

### Get Recent Lab Results
```bash
curl "http://localhost:8000/api/fhir/Observation?category=laboratory&date=ge2025-10-01"
```

### Get Active Medications
```bash
curl "http://localhost:8000/api/fhir/MedicationRequest?status=active"
```

### Get Preventive Procedures
```bash
curl "http://localhost:8000/api/fhir/Procedure?code=92250,45378"
```

---

## Future Enhancements

### Additional Resource Types Needed
- [ ] **DiagnosticReport** - Lab reports with full panels
- [ ] **DocumentReference** - Clinical notes, discharge summaries
- [ ] **Goal** - Patient-specific health goals
- [ ] **ServiceRequest** - Orders for labs, imaging
- [ ] **Claim** - Billing and insurance data
- [ ] **Coverage** - Insurance coverage details
- [ ] **Provenance** - Data source tracking
- [ ] **AuditEvent** - Access audit trails

### Additional Clinical Scenarios
- [ ] Emergency department visit
- [ ] Hospital admission/discharge
- [ ] Surgical procedures
- [ ] Cancer diagnosis and treatment
- [ ] Behavioral health crisis
- [ ] Substance use disorder
- [ ] Chronic pain management
- [ ] Palliative/end-of-life care

### Additional Quality Measures
- [ ] CMS measures (all applicable)
- [ ] PQRS measures
- [ ] MIPS measures
- [ ] State-specific measures
- [ ] ACO measures
- [ ] Patient satisfaction (CAHPS)

---

## Maintenance

### Regenerating Test Data
```bash
./sample-data/comprehensive-fhir-test-data.sh
```

### Clearing Test Data
```bash
# Delete patients with TEST- identifier
for id in 179 180 181 182 183; do
  curl -X DELETE http://localhost:8000/api/fhir/Patient/$id
done
```

### Validating Data Quality
```bash
# Check resource counts
curl http://localhost:8000/api/fhir/Patient?_summary=count
curl http://localhost:8000/api/fhir/Observation?_summary=count
curl http://localhost:8000/api/fhir/Condition?_summary=count
```

---

## Related Documentation

- [FHIR R4 Specification](http://hl7.org/fhir/R4/)
- [HEDIS Measures](https://www.ncqa.org/hedis/)
- [CMS Quality Measures](https://www.cms.gov/medicare/quality/measures)
- [LOINC Codes](https://loinc.org/)
- [SNOMED CT](https://www.snomed.org/)

---

**Last Updated**: 2025-11-19
**Maintainer**: HealthData In Motion Team
**Version**: 1.0.0
