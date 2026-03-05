# Synthetic Patient Phenotype Profiles

## Overview

Six clinically valid synthetic patient phenotypes validate the HDIM platform's clinical decision support, quality measurement, and care gap detection capabilities. Each phenotype represents a distinct clinical scenario with known expected outcomes.

## Phenotype 1: Type 2 Diabetes — Managed (t2dm-managed)

**Demographics:** 58-year-old female
**Conditions:** Type 2 diabetes mellitus (SNOMED 44054006)
**Key Labs:** HbA1c 6.8% (within target <7%)
**Medications:** Metformin 500 MG
**Screenings:** All current (retinopathy, nephropathy, foot exam)

**Clinical Rationale:** Represents the well-controlled T2DM patient who should have zero open care gaps and full compliance with HEDIS diabetes measures. ADA Standards of Medical Care in Diabetes (2024) defines <7% HbA1c as the general adult target.

**Expected Platform Behavior:**
- `care_gap_list` → 0 gaps
- `measure_evaluate(HbA1c-Control)` → compliant
- `cql_evaluate(Diabetic-Eye-Exam)` → compliant
- `patient_risk` → low risk score

## Phenotype 2: Type 2 Diabetes — Unmanaged (t2dm-unmanaged)

**Demographics:** 67-year-old male
**Conditions:** Type 2 diabetes mellitus
**Key Labs:** HbA1c 9.2% (above target)
**Medications:** None active
**Missing Screenings:** Retinopathy exam, nephropathy screening

**Clinical Rationale:** Represents poorly controlled T2DM with missing preventive screenings. Per ADA guidelines, annual retinopathy screening and biannual nephropathy monitoring are required for all T2DM patients.

**Expected Platform Behavior:**
- `care_gap_list` → 2 gaps (retinopathy, nephropathy)
- `measure_evaluate(HbA1c-Control)` → non-compliant
- `cds_patient_view` → CDS alert cards
- `patient_risk` → high risk score

## Phenotype 3: CHF with Polypharmacy (chf-polypharmacy)

**Demographics:** 74-year-old female
**Conditions:** Congestive heart failure (EF 35%)
**Medications:** 8 active prescriptions
**Encounters:** Recent ED visit within 30 days

**Clinical Rationale:** Tests complex medication management and encounter timeline reconstruction. ACC/AHA heart failure guidelines recommend close monitoring of polypharmacy patients with reduced ejection fraction.

**Expected Platform Behavior:**
- `patient_summary` → 8+ medications
- `patient_risk` → high risk score
- `patient_timeline` → recent ED encounter visible

## Phenotype 4: Preventive Care Gaps (preventive-gaps)

**Demographics:** 45-year-old male
**Conditions:** None chronic
**Missing:** Colonoscopy (due at 45), flu vaccine (annual), lipid panel (5-year)

**Clinical Rationale:** USPSTF recommends colorectal cancer screening starting at age 45 (2021 update). CDC recommends annual influenza vaccination. USPSTF recommends lipid screening every 4-6 years for adults 40-75.

**Expected Platform Behavior:**
- `care_gap_list` → 3+ open gaps
- `patient_risk` → moderate (age-appropriate screening gaps)

## Phenotype 5: Healthy Pediatric (healthy-pediatric)

**Demographics:** 12-year-old female
**Conditions:** None
**Encounters:** Routine well-child visits
**Immunizations:** Current per CDC schedule

**Clinical Rationale:** Tests non-chronic patient handling. ACIP immunization schedule compliance verification. Proves the platform correctly identifies patients with no care gaps or active conditions.

**Expected Platform Behavior:**
- `patient_summary` → no active conditions
- `care_gap_list` → 0 gaps
- Immunization tracking → current

## Phenotype 6: Multi-Chronic Elderly (multi-chronic-elderly)

**Demographics:** 82-year-old male
**Conditions:** COPD + CKD Stage 3 + Essential hypertension
**Encounters:** Multiple specialist visits

**Clinical Rationale:** Tests risk stratification for complex multi-morbidity. GOLD COPD guidelines, KDIGO CKD guidelines, and JNC8 hypertension guidelines each contribute to care planning complexity.

**Expected Platform Behavior:**
- `patient_summary` → 3+ active conditions
- `health_score` → low score
- `patient_risk` → high risk score
- Multiple quality measures applicable
