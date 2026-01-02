# CSV Import Templates

> Standard CSV templates for manual data upload to HDIM. These templates support customers without FHIR API access.

## Table of Contents

1. [Patient Roster](#patient-roster)
2. [Conditions/Diagnoses](#conditionsdiagnoses)
3. [Medications](#medications)
4. [Lab Results](#lab-results)
5. [Vitals](#vitals)
6. [Immunizations](#immunizations)
7. [Encounters/Visits](#encountersvisits)
8. [Combined Export](#combined-export)
9. [EHR-Specific Export Guides](#ehr-specific-export-guides)

---

## Patient Roster

### Template: `patients.csv`

```csv
mrn,first_name,last_name,middle_name,dob,gender,race,ethnicity,language,phone,email,address_line1,address_line2,city,state,zip,pcp_npi,pcp_name,insurance_type,insurance_id
12345,Maria,Garcia,Elena,1968-03-15,F,White,Not Hispanic or Latino,English,555-123-4567,maria.garcia@email.com,123 Main Street,Apt 4B,Springfield,MA,01101,1234567890,Dr. Sarah Martinez,Medicare,MED123456789
67890,Robert,Johnson,James,1955-07-22,M,Black or African American,Not Hispanic or Latino,English,555-234-5678,rjohnson@email.com,456 Oak Avenue,,Boston,MA,02101,1234567890,Dr. Sarah Martinez,Commercial,BCBS987654321
11111,Jennifer,Smith,,1972-11-08,F,Asian,Not Hispanic or Latino,English,555-345-6789,jen.smith@email.com,789 Elm Street,,Cambridge,MA,02139,1234567890,Dr. Sarah Martinez,Medicaid,MCD456789012
```

### Field Specifications

| Field | Required | Format | Notes |
|-------|----------|--------|-------|
| `mrn` | Yes | String | Medical Record Number - unique identifier |
| `first_name` | Yes | String | Patient first name |
| `last_name` | Yes | String | Patient last name |
| `middle_name` | No | String | Patient middle name |
| `dob` | Yes | YYYY-MM-DD | Date of birth |
| `gender` | Yes | M/F/O/U | Male, Female, Other, Unknown |
| `race` | No | String | See race codes below |
| `ethnicity` | No | String | Hispanic or Latino / Not Hispanic or Latino |
| `language` | No | String | Preferred language |
| `phone` | No | String | Primary phone number |
| `email` | No | String | Email address |
| `address_line1` | No | String | Street address |
| `address_line2` | No | String | Apt/Suite/Unit |
| `city` | No | String | City |
| `state` | No | String | 2-letter state code |
| `zip` | No | String | 5 or 9 digit ZIP |
| `pcp_npi` | No | String | 10-digit NPI of PCP |
| `pcp_name` | No | String | PCP display name |
| `insurance_type` | No | String | Medicare/Medicaid/Commercial/Self-Pay |
| `insurance_id` | No | String | Insurance member ID |

### Race Codes

- American Indian or Alaska Native
- Asian
- Black or African American
- Native Hawaiian or Other Pacific Islander
- White
- Two or More Races
- Unknown

---

## Conditions/Diagnoses

### Template: `conditions.csv`

```csv
mrn,icd10_code,icd10_description,snomed_code,onset_date,status,diagnosed_by_npi,diagnosed_date
12345,E11.9,Type 2 diabetes mellitus without complications,44054006,2018-06-15,active,1234567890,2018-06-15
12345,I10,Essential (primary) hypertension,38341003,2019-02-10,active,1234567890,2019-02-10
12345,E78.5,Hyperlipidemia unspecified,55822004,2019-02-10,active,1234567890,2019-02-10
67890,J45.909,Unspecified asthma uncomplicated,195967001,2010-01-15,active,1234567890,2010-01-15
67890,F32.9,Major depressive disorder single episode unspecified,35489007,2023-01-15,active,1234567890,2023-01-15
11111,C50.919,Malignant neoplasm of unspecified site of unspecified female breast,254837009,2022-03-01,resolved,1234567890,2022-03-01
```

### Field Specifications

| Field | Required | Format | Notes |
|-------|----------|--------|-------|
| `mrn` | Yes | String | Must match patient roster |
| `icd10_code` | Yes | String | ICD-10-CM code (with period) |
| `icd10_description` | No | String | ICD-10 description |
| `snomed_code` | No | String | SNOMED CT code (if available) |
| `onset_date` | No | YYYY-MM-DD | When condition began |
| `status` | Yes | String | active, resolved, inactive |
| `diagnosed_by_npi` | No | String | Diagnosing provider NPI |
| `diagnosed_date` | No | YYYY-MM-DD | When diagnosis was made |

### Common ICD-10 Codes for Quality Measures

| Category | Code | Description |
|----------|------|-------------|
| Diabetes | E11.9 | Type 2 diabetes without complications |
| Diabetes | E11.65 | Type 2 diabetes with hyperglycemia |
| Hypertension | I10 | Essential hypertension |
| Heart Disease | I25.10 | Atherosclerotic heart disease |
| Depression | F32.9 | Major depressive disorder |
| Asthma | J45.909 | Unspecified asthma |
| COPD | J44.9 | COPD unspecified |
| CKD | N18.3 | Chronic kidney disease stage 3 |

---

## Medications

### Template: `medications.csv`

```csv
mrn,rxnorm_code,medication_name,dose,unit,frequency,route,start_date,end_date,status,prescriber_npi
12345,860974,Metformin Hydrochloride 500 MG Oral Tablet,500,mg,twice daily,oral,2018-06-15,,active,1234567890
12345,314076,Lisinopril 10 MG Oral Tablet,10,mg,once daily,oral,2019-02-10,,active,1234567890
12345,617311,Atorvastatin 20 MG Oral Tablet,20,mg,once daily at bedtime,oral,2019-02-10,,active,1234567890
67890,746574,Albuterol 90 MCG/ACTUAT Metered Dose Inhaler,90,mcg,as needed,inhalation,2010-01-15,,active,1234567890
67890,311725,Escitalopram 10 MG Oral Tablet,10,mg,once daily,oral,2023-01-20,,active,1234567890
```

### Field Specifications

| Field | Required | Format | Notes |
|-------|----------|--------|-------|
| `mrn` | Yes | String | Must match patient roster |
| `rxnorm_code` | Yes* | String | RxNorm code (*or NDC required) |
| `medication_name` | Yes | String | Full medication name with strength |
| `dose` | No | Number | Dose amount |
| `unit` | No | String | mg, mcg, mL, etc. |
| `frequency` | No | String | once daily, twice daily, etc. |
| `route` | No | String | oral, inhalation, injection, etc. |
| `start_date` | No | YYYY-MM-DD | Prescription start date |
| `end_date` | No | YYYY-MM-DD | Prescription end date (blank if active) |
| `status` | Yes | String | active, completed, stopped |
| `prescriber_npi` | No | String | Prescriber NPI |

### Common Medication Categories for Quality Measures

| Category | RxNorm | Example |
|----------|--------|---------|
| Diabetes | 860974 | Metformin 500 MG |
| Diabetes | 847910 | Glipizide 5 MG |
| ACE Inhibitor | 314076 | Lisinopril 10 MG |
| ARB | 979480 | Losartan 50 MG |
| Statin | 617311 | Atorvastatin 20 MG |
| Antidepressant | 311725 | Escitalopram 10 MG |
| Asthma | 746574 | Albuterol MDI |

---

## Lab Results

### Template: `labs.csv`

```csv
mrn,loinc_code,test_name,result_value,result_unit,reference_low,reference_high,result_date,status,performing_lab
12345,4548-4,Hemoglobin A1c,7.2,%,4.0,5.6,2024-09-15,final,Quest Diagnostics
12345,2089-1,LDL Cholesterol,118,mg/dL,0,100,2024-09-15,final,Quest Diagnostics
12345,2160-0,Creatinine,1.1,mg/dL,0.7,1.3,2024-09-15,final,Quest Diagnostics
12345,33914-3,eGFR,68,mL/min/1.73m2,60,120,2024-09-15,final,Quest Diagnostics
67890,4548-4,Hemoglobin A1c,5.4,%,4.0,5.6,2024-08-20,final,LabCorp
11111,4548-4,Hemoglobin A1c,6.8,%,4.0,5.6,2024-07-10,final,Quest Diagnostics
12345,44261-6,PHQ-9 Total Score,12,{score},0,4,2024-10-01,final,In-Office
```

### Field Specifications

| Field | Required | Format | Notes |
|-------|----------|--------|-------|
| `mrn` | Yes | String | Must match patient roster |
| `loinc_code` | Yes | String | LOINC code for the test |
| `test_name` | Yes | String | Display name of test |
| `result_value` | Yes | Number/String | Test result value |
| `result_unit` | Yes | String | Unit of measure |
| `reference_low` | No | Number | Normal range lower bound |
| `reference_high` | No | Number | Normal range upper bound |
| `result_date` | Yes | YYYY-MM-DD | Date of result |
| `status` | No | String | final, preliminary, corrected |
| `performing_lab` | No | String | Laboratory name |

### Common LOINC Codes for Quality Measures

| Measure | LOINC | Test Name |
|---------|-------|-----------|
| Diabetes Control | 4548-4 | Hemoglobin A1c |
| Lipid Panel | 2089-1 | LDL Cholesterol |
| Lipid Panel | 2093-3 | Total Cholesterol |
| Lipid Panel | 2085-9 | HDL Cholesterol |
| Kidney | 2160-0 | Creatinine |
| Kidney | 33914-3 | eGFR |
| Depression | 44261-6 | PHQ-9 Total Score |
| Colon Cancer | 57803-7 | FIT Result |

---

## Vitals

### Template: `vitals.csv`

```csv
mrn,vital_type,systolic,diastolic,value,unit,measurement_date,measured_by
12345,blood_pressure,128,82,,mmHg,2024-10-15,MA-Johnson
12345,bmi,,,28.5,kg/m2,2024-10-15,MA-Johnson
12345,weight,,,175,lbs,2024-10-15,MA-Johnson
12345,height,,,67,in,2024-10-15,MA-Johnson
67890,blood_pressure,135,88,,mmHg,2024-10-10,MA-Smith
67890,bmi,,,32.1,kg/m2,2024-10-10,MA-Smith
```

### Field Specifications

| Field | Required | Format | Notes |
|-------|----------|--------|-------|
| `mrn` | Yes | String | Must match patient roster |
| `vital_type` | Yes | String | blood_pressure, bmi, weight, height, etc. |
| `systolic` | Cond. | Number | Required for blood_pressure |
| `diastolic` | Cond. | Number | Required for blood_pressure |
| `value` | Cond. | Number | Required for non-BP vitals |
| `unit` | Yes | String | mmHg, kg/m2, lbs, in, etc. |
| `measurement_date` | Yes | YYYY-MM-DD | Date of measurement |
| `measured_by` | No | String | Person who took measurement |

### Vital Types

- `blood_pressure` - systolic/diastolic in mmHg
- `bmi` - kg/m2
- `weight` - lbs or kg
- `height` - in or cm
- `temperature` - F or C
- `pulse` - bpm
- `respiratory_rate` - breaths/min
- `oxygen_saturation` - %

---

## Immunizations

### Template: `immunizations.csv`

```csv
mrn,cvx_code,vaccine_name,administration_date,lot_number,expiration_date,site,administered_by_npi,status
12345,141,Influenza seasonal injectable,2024-10-01,FLU2024-A123,2025-03-31,Left arm,1234567890,completed
12345,133,Pneumococcal conjugate PCV 13,2023-11-15,PCV13-B456,2024-11-15,Right arm,1234567890,completed
12345,208,COVID-19 mRNA Pfizer,2024-09-15,COVID-C789,2025-06-30,Left arm,1234567890,completed
67890,141,Influenza seasonal injectable,2024-09-20,FLU2024-D012,2025-03-31,Left arm,1234567890,completed
11111,21,Varicella,2024-01-15,VZV-E345,2025-01-15,Right arm,1234567890,completed
```

### Field Specifications

| Field | Required | Format | Notes |
|-------|----------|--------|-------|
| `mrn` | Yes | String | Must match patient roster |
| `cvx_code` | Yes | String | CVX vaccine code |
| `vaccine_name` | Yes | String | Vaccine display name |
| `administration_date` | Yes | YYYY-MM-DD | Date given |
| `lot_number` | No | String | Vaccine lot number |
| `expiration_date` | No | YYYY-MM-DD | Lot expiration |
| `site` | No | String | Injection site |
| `administered_by_npi` | No | String | Administering provider |
| `status` | Yes | String | completed, not-done |

### Common CVX Codes

| Code | Vaccine |
|------|---------|
| 141 | Influenza, seasonal, injectable |
| 140 | Influenza, seasonal, intranasal |
| 133 | Pneumococcal conjugate PCV 13 |
| 33 | Pneumococcal polysaccharide PPSV23 |
| 208 | COVID-19 mRNA Pfizer |
| 207 | COVID-19 mRNA Moderna |
| 113 | Td adult |
| 115 | Tdap |
| 21 | Varicella |
| 03 | MMR |

---

## Encounters/Visits

### Template: `encounters.csv`

```csv
mrn,encounter_id,encounter_type,cpt_code,encounter_date,provider_npi,provider_name,location,primary_dx,status
12345,ENC001,office_visit,99214,2024-10-15,1234567890,Dr. Sarah Martinez,Main Clinic,E11.9,completed
12345,ENC002,annual_wellness,G0438,2024-01-10,1234567890,Dr. Sarah Martinez,Main Clinic,Z00.00,completed
67890,ENC003,office_visit,99213,2024-10-10,1234567890,Dr. Sarah Martinez,Main Clinic,J45.909,completed
11111,ENC004,telehealth,99213,2024-09-20,1234567890,Dr. Sarah Martinez,Telehealth,F32.9,completed
12345,ENC005,preventive,99396,2024-06-01,1234567890,Dr. Sarah Martinez,Main Clinic,Z00.00,completed
```

### Field Specifications

| Field | Required | Format | Notes |
|-------|----------|--------|-------|
| `mrn` | Yes | String | Must match patient roster |
| `encounter_id` | Yes | String | Unique encounter identifier |
| `encounter_type` | Yes | String | office_visit, annual_wellness, telehealth, etc. |
| `cpt_code` | No | String | CPT code for encounter |
| `encounter_date` | Yes | YYYY-MM-DD | Date of visit |
| `provider_npi` | No | String | Provider NPI |
| `provider_name` | No | String | Provider display name |
| `location` | No | String | Site of service |
| `primary_dx` | No | String | Primary diagnosis code |
| `status` | Yes | String | completed, cancelled, no-show |

### Common Encounter CPT Codes

| Code | Description |
|------|-------------|
| 99213 | Office visit, established, low complexity |
| 99214 | Office visit, established, moderate complexity |
| 99215 | Office visit, established, high complexity |
| 99396 | Preventive visit, 40-64 years |
| 99397 | Preventive visit, 65+ years |
| G0438 | Annual wellness visit, initial |
| G0439 | Annual wellness visit, subsequent |

---

## Combined Export

For simple implementations, a single combined file can be used:

### Template: `patient_data_combined.csv`

```csv
record_type,mrn,field1,field2,field3,field4,field5,field6,field7,field8
PATIENT,12345,Maria,Garcia,1968-03-15,F,555-123-4567,123 Main St,Springfield,MA
CONDITION,12345,E11.9,Type 2 diabetes,active,2018-06-15,,,
CONDITION,12345,I10,Essential hypertension,active,2019-02-10,,,
MEDICATION,12345,860974,Metformin 500 MG,active,2018-06-15,twice daily,,
LAB,12345,4548-4,HbA1c,7.2,%,2024-09-15,,
VITAL,12345,blood_pressure,128,82,mmHg,2024-10-15,,
IMMUNIZATION,12345,141,Influenza vaccine,2024-10-01,completed,,,
```

---

## EHR-Specific Export Guides

### DrChrono Export

1. Navigate to **Reports > Patient Export**
2. Select date range and patient population
3. Choose fields: Demographics, Problems, Medications, Labs
4. Export as CSV
5. Map columns to HDIM template

### athenahealth Export

1. Go to **Reports > Quality Reports**
2. Select "Custom Report Builder"
3. Add required fields for each domain
4. Export to Excel, save as CSV
5. Map columns to HDIM template

### eClinicalWorks Export

1. Access **Reports > Meaningful Use > Data Export**
2. Select patient population
3. Choose export type: Patient Summary
4. Export to CSV format
5. Map columns to HDIM template

### Practice Fusion Export

1. Navigate to **Reports > Custom Reports**
2. Build report with demographic and clinical fields
3. Export as spreadsheet
4. Convert to CSV with proper encoding
5. Map columns to HDIM template

### Allscripts Export

1. Go to **Analytics > Report Writer**
2. Create new report with required fields
3. Select patient population filter
4. Export to CSV
5. Map columns to HDIM template

---

## Upload Instructions

### File Requirements

- Format: UTF-8 encoded CSV
- Line endings: Unix (LF) or Windows (CRLF)
- Maximum file size: 50 MB per file
- Date format: YYYY-MM-DD
- Null values: Leave blank, don't use "NULL" or "N/A"

### Upload Process

1. Log into HDIM at https://app.healthdatainmotion.com
2. Navigate to **Data > Import**
3. Select file type (Patients, Conditions, etc.)
4. Upload CSV file
5. Review mapping preview
6. Confirm import
7. Monitor import status

### Validation

HDIM validates:
- Required fields present
- Date formats correct
- Code values valid (ICD-10, LOINC, RxNorm)
- MRN references exist
- No duplicate records

### Error Handling

- Rows with errors are rejected
- Error report downloadable after import
- Correct errors and re-upload failed rows

---

*CSV Templates Version: 1.0*
*Last Updated: December 2025*
