#!/bin/bash

# Comprehensive FHIR Test Data Generator
# Creates exhaustive medically relevant FHIR resources for testing

set -e

FHIR_URL="${FHIR_URL:-http://localhost:8000/api/fhir}"
TENANT_ID="${TENANT_ID:-clinic-001}"

echo "========================================="
echo "Comprehensive FHIR Test Data Generator"
echo "========================================="
echo "FHIR Server: $FHIR_URL"
echo "Tenant: $TENANT_ID"
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to post FHIR resource
post_fhir() {
    local resource_type=$1
    local data=$2
    local description=$3

    echo -n "Creating $description... "
    response=$(curl -s -X POST "$FHIR_URL/$resource_type" \
        -H "Content-Type: application/fhir+json" \
        -H "X-Tenant-ID: $TENANT_ID" \
        -d "$data")

    if echo "$response" | grep -q '"resourceType"'; then
        echo -e "${GREEN}✓${NC}"
        echo "$response" | python3 -c "import json, sys; d=json.load(sys.stdin); print(f'  ID: {d.get(\"id\", \"unknown\")}')" 2>/dev/null || true
    else
        echo -e "${RED}✗${NC}"
        echo "$response" | head -3
    fi
}

# =============================================================================
# STEP 1: CREATE PATIENTS with comprehensive demographics
# =============================================================================

echo -e "${BLUE}Step 1: Creating Test Patients${NC}"
echo "-------------------------------------------"

# Patient 1: Diabetic adult male with hypertension
post_fhir "Patient" '{
  "resourceType": "Patient",
  "identifier": [
    {"system": "http://hospital.example.org/mrn", "value": "TEST-1001"},
    {"system": "http://hl7.org/fhir/sid/us-ssn", "value": "123-45-6789"}
  ],
  "active": true,
  "name": [{
    "use": "official",
    "family": "Anderson",
    "given": ["Thomas", "James"],
    "prefix": ["Mr."]
  }],
  "telecom": [
    {"system": "phone", "value": "555-0101", "use": "home"},
    {"system": "email", "value": "thomas.anderson@example.com"}
  ],
  "gender": "male",
  "birthDate": "1965-03-15",
  "address": [{
    "use": "home",
    "line": ["123 Main St", "Apt 4B"],
    "city": "Springfield",
    "state": "IL",
    "postalCode": "62701",
    "country": "USA"
  }],
  "maritalStatus": {
    "coding": [{"system": "http://terminology.hl7.org/CodeSystem/v3-MaritalStatus", "code": "M", "display": "Married"}]
  },
  "communication": [{
    "language": {"coding": [{"system": "urn:ietf:bcp:47", "code": "en-US"}]},
    "preferred": true
  }]
}' "Patient 1: Thomas Anderson (Diabetic + Hypertension)"

# Patient 2: Pregnant female
post_fhir "Patient" '{
  "resourceType": "Patient",
  "identifier": [{"system": "http://hospital.example.org/mrn", "value": "TEST-1002"}],
  "active": true,
  "name": [{"use": "official", "family": "Martinez", "given": ["Sofia", "Elena"]}],
  "gender": "female",
  "birthDate": "1992-07-22",
  "address": [{
    "use": "home",
    "line": ["456 Oak Avenue"],
    "city": "Springfield",
    "state": "IL",
    "postalCode": "62702"
  }]
}' "Patient 2: Sofia Martinez (Pregnant)"

# Patient 3: Pediatric patient with asthma
post_fhir "Patient" '{
  "resourceType": "Patient",
  "identifier": [{"system": "http://hospital.example.org/mrn", "value": "TEST-1003"}],
  "active": true,
  "name": [{"use": "official", "family": "Chen", "given": ["Emily"]}],
  "gender": "female",
  "birthDate": "2018-11-10",
  "contact": [{
    "relationship": [{"coding": [{"system": "http://terminology.hl7.org/CodeSystem/v2-0131", "code": "N"}]}],
    "name": {"family": "Chen", "given": ["David"]},
    "telecom": [{"system": "phone", "value": "555-0103"}]
  }]
}' "Patient 3: Emily Chen (Pediatric, Asthma)"

# Patient 4: Elderly patient with multiple chronic conditions
post_fhir "Patient" '{
  "resourceType": "Patient",
  "identifier": [{"system": "http://hospital.example.org/mrn", "value": "TEST-1004"}],
  "active": true,
  "name": [{"use": "official", "family": "Johnson", "given": ["Robert", "William"], "suffix": ["Sr."]}],
  "gender": "male",
  "birthDate": "1945-01-05",
  "deceasedBoolean": false
}' "Patient 4: Robert Johnson (Elderly, Multiple Conditions)"

# Patient 5: Patient with mental health conditions
post_fhir "Patient" '{
  "resourceType": "Patient",
  "identifier": [{"system": "http://hospital.example.org/mrn", "value": "TEST-1005"}],
  "active": true,
  "name": [{"use": "official", "family": "Williams", "given": ["Sarah", "Marie"]}],
  "gender": "female",
  "birthDate": "1988-05-30"
}' "Patient 5: Sarah Williams (Mental Health)"

echo ""

# =============================================================================
# STEP 2: CREATE CONDITIONS (Problems/Diagnoses)
# =============================================================================

echo -e "${BLUE}Step 2: Creating Conditions${NC}"
echo "-------------------------------------------"

# We'll store patient IDs - in real scenario, get them from API
# For now, using placeholder IDs that will be filled

# Diabetes Type 2
post_fhir "Condition" '{
  "resourceType": "Condition",
  "clinicalStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active"}]},
  "verificationStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-ver-status", "code": "confirmed"}]},
  "category": [{
    "coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-category", "code": "encounter-diagnosis"}]
  }],
  "code": {
    "coding": [
      {"system": "http://snomed.info/sct", "code": "44054006", "display": "Diabetes mellitus type 2"},
      {"system": "http://hl7.org/fhir/sid/icd-10-cm", "code": "E11.9", "display": "Type 2 diabetes mellitus without complications"}
    ],
    "text": "Type 2 Diabetes Mellitus"
  },
  "subject": {"reference": "Patient/1"},
  "onsetDateTime": "2015-06-10",
  "recordedDate": "2015-06-10"
}' "Diabetes Type 2"

# Hypertension
post_fhir "Condition" '{
  "resourceType": "Condition",
  "clinicalStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active"}]},
  "verificationStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-ver-status", "code": "confirmed"}]},
  "code": {
    "coding": [
      {"system": "http://snomed.info/sct", "code": "38341003", "display": "Hypertension"},
      {"system": "http://hl7.org/fhir/sid/icd-10-cm", "code": "I10", "display": "Essential (primary) hypertension"}
    ]
  },
  "subject": {"reference": "Patient/1"},
  "onsetDateTime": "2018-03-22"
}' "Hypertension"

# Pregnancy
post_fhir "Condition" '{
  "resourceType": "Condition",
  "clinicalStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active"}]},
  "verificationStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-ver-status", "code": "confirmed"}]},
  "code": {
    "coding": [
      {"system": "http://snomed.info/sct", "code": "77386006", "display": "Pregnancy"},
      {"system": "http://hl7.org/fhir/sid/icd-10-cm", "code": "Z34.90", "display": "Encounter for supervision of normal pregnancy"}
    ]
  },
  "subject": {"reference": "Patient/2"},
  "onsetDateTime": "2025-03-15"
}' "Pregnancy"

# Asthma
post_fhir "Condition" '{
  "resourceType": "Condition",
  "clinicalStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active"}]},
  "verificationStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-ver-status", "code": "confirmed"}]},
  "code": {
    "coding": [
      {"system": "http://snomed.info/sct", "code": "195967001", "display": "Asthma"},
      {"system": "http://hl7.org/fhir/sid/icd-10-cm", "code": "J45.909", "display": "Unspecified asthma, uncomplicated"}
    ]
  },
  "subject": {"reference": "Patient/3"},
  "onsetDateTime": "2020-05-12"
}' "Asthma"

# Chronic Kidney Disease
post_fhir "Condition" '{
  "resourceType": "Condition",
  "clinicalStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active"}]},
  "code": {
    "coding": [
      {"system": "http://snomed.info/sct", "code": "431855005", "display": "Chronic kidney disease stage 3"},
      {"system": "http://hl7.org/fhir/sid/icd-10-cm", "code": "N18.3", "display": "Chronic kidney disease, stage 3"}
    ]
  },
  "subject": {"reference": "Patient/4"},
  "onsetDateTime": "2020-01-15"
}' "Chronic Kidney Disease Stage 3"

# Depression
post_fhir "Condition" '{
  "resourceType": "Condition",
  "clinicalStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active"}]},
  "code": {
    "coding": [
      {"system": "http://snomed.info/sct", "code": "370143000", "display": "Major depressive disorder"},
      {"system": "http://hl7.org/fhir/sid/icd-10-cm", "code": "F33.1", "display": "Major depressive disorder, recurrent, moderate"}
    ]
  },
  "subject": {"reference": "Patient/5"},
  "onsetDateTime": "2022-08-10"
}' "Major Depression"

echo ""

# =============================================================================
# STEP 3: CREATE OBSERVATIONS (Vital Signs, Lab Results)
# =============================================================================

echo -e "${BLUE}Step 3: Creating Observations${NC}"
echo "-------------------------------------------"

# Blood Pressure - Controlled
post_fhir "Observation" '{
  "resourceType": "Observation",
  "status": "final",
  "category": [{
    "coding": [{"system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "vital-signs"}]
  }],
  "code": {
    "coding": [
      {"system": "http://loinc.org", "code": "85354-9", "display": "Blood pressure panel"}
    ]
  },
  "subject": {"reference": "Patient/1"},
  "effectiveDateTime": "2025-11-01T10:30:00Z",
  "component": [
    {
      "code": {"coding": [{"system": "http://loinc.org", "code": "8480-6", "display": "Systolic blood pressure"}]},
      "valueQuantity": {"value": 128, "unit": "mmHg", "system": "http://unitsofmeasure.org", "code": "mm[Hg]"}
    },
    {
      "code": {"coding": [{"system": "http://loinc.org", "code": "8462-4", "display": "Diastolic blood pressure"}]},
      "valueQuantity": {"value": 82, "unit": "mmHg", "system": "http://unitsofmeasure.org", "code": "mm[Hg]"}
    }
  ]
}' "Blood Pressure (Controlled)"

# HbA1c - Good control
post_fhir "Observation" '{
  "resourceType": "Observation",
  "status": "final",
  "category": [{
    "coding": [{"system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "laboratory"}]
  }],
  "code": {
    "coding": [
      {"system": "http://loinc.org", "code": "4548-4", "display": "Hemoglobin A1c/Hemoglobin.total in Blood"}
    ],
    "text": "HbA1c"
  },
  "subject": {"reference": "Patient/1"},
  "effectiveDateTime": "2025-10-15T09:00:00Z",
  "valueQuantity": {"value": 7.2, "unit": "%", "system": "http://unitsofmeasure.org", "code": "%"}
}' "HbA1c (Good Control at 7.2%)"

# BMI
post_fhir "Observation" '{
  "resourceType": "Observation",
  "status": "final",
  "category": [{
    "coding": [{"system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "vital-signs"}]
  }],
  "code": {
    "coding": [
      {"system": "http://loinc.org", "code": "39156-5", "display": "Body mass index (BMI) [Ratio]"}
    ]
  },
  "subject": {"reference": "Patient/1"},
  "effectiveDateTime": "2025-11-01T10:30:00Z",
  "valueQuantity": {"value": 28.5, "unit": "kg/m2", "system": "http://unitsofmeasure.org", "code": "kg/m2"}
}' "BMI"

# Cholesterol Panel
post_fhir "Observation" '{
  "resourceType": "Observation",
  "status": "final",
  "category": [{
    "coding": [{"system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "laboratory"}]
  }],
  "code": {
    "coding": [{"system": "http://loinc.org", "code": "2093-3", "display": "Total Cholesterol"}]
  },
  "subject": {"reference": "Patient/1"},
  "effectiveDateTime": "2025-10-15T09:00:00Z",
  "valueQuantity": {"value": 195, "unit": "mg/dL", "system": "http://unitsofmeasure.org", "code": "mg/dL"}
}' "Total Cholesterol"

# eGFR (Kidney Function)
post_fhir "Observation" '{
  "resourceType": "Observation",
  "status": "final",
  "category": [{
    "coding": [{"system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "laboratory"}]
  }],
  "code": {
    "coding": [{"system": "http://loinc.org", "code": "48643-1", "display": "Glomerular filtration rate"}]
  },
  "subject": {"reference": "Patient/4"},
  "effectiveDateTime": "2025-11-01T09:00:00Z",
  "valueQuantity": {"value": 52, "unit": "mL/min", "system": "http://unitsofmeasure.org", "code": "mL/min"}
}' "eGFR (Kidney Function)"

# Pregnancy Test - Positive
post_fhir "Observation" '{
  "resourceType": "Observation",
  "status": "final",
  "category": [{
    "coding": [{"system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "laboratory"}]
  }],
  "code": {
    "coding": [{"system": "http://loinc.org", "code": "2106-3", "display": "Choriogonadotropin (Pregnancy test)"}]
  },
  "subject": {"reference": "Patient/2"},
  "effectiveDateTime": "2025-03-20T10:00:00Z",
  "valueCodeableConcept": {
    "coding": [{"system": "http://snomed.info/sct", "code": "10828004", "display": "Positive"}]
  }
}' "Pregnancy Test (Positive)"

# Depression Screening (PHQ-9)
post_fhir "Observation" '{
  "resourceType": "Observation",
  "status": "final",
  "category": [{
    "coding": [{"system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "survey"}]
  }],
  "code": {
    "coding": [{"system": "http://loinc.org", "code": "44261-6", "display": "PHQ-9 total score"}]
  },
  "subject": {"reference": "Patient/5"},
  "effectiveDateTime": "2025-11-10T14:00:00Z",
  "valueInteger": 15,
  "interpretation": [{
    "coding": [{"system": "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation", "code": "H", "display": "High"}]
  }]
}' "PHQ-9 Depression Score"

echo ""

# =============================================================================
# STEP 4: CREATE MEDICATIONS
# =============================================================================

echo -e "${BLUE}Step 4: Creating Medications${NC}"
echo "-------------------------------------------"

# Metformin for Diabetes
post_fhir "MedicationRequest" '{
  "resourceType": "MedicationRequest",
  "status": "active",
  "intent": "order",
  "medicationCodeableConcept": {
    "coding": [
      {"system": "http://www.nlm.nih.gov/research/umls/rxnorm", "code": "860975", "display": "Metformin 500 MG Oral Tablet"}
    ]
  },
  "subject": {"reference": "Patient/1"},
  "authoredOn": "2025-01-15",
  "dosageInstruction": [{
    "text": "Take 1 tablet twice daily with meals",
    "timing": {"repeat": {"frequency": 2, "period": 1, "periodUnit": "d"}},
    "route": {"coding": [{"system": "http://snomed.info/sct", "code": "26643006", "display": "Oral route"}]},
    "doseAndRate": [{
      "doseQuantity": {"value": 500, "unit": "mg", "system": "http://unitsofmeasure.org", "code": "mg"}
    }]
  }]
}' "Metformin 500mg (Diabetes)"

# Lisinopril for Hypertension
post_fhir "MedicationRequest" '{
  "resourceType": "MedicationRequest",
  "status": "active",
  "intent": "order",
  "medicationCodeableConcept": {
    "coding": [
      {"system": "http://www.nlm.nih.gov/research/umls/rxnorm", "code": "314076", "display": "Lisinopril 10 MG Oral Tablet"}
    ]
  },
  "subject": {"reference": "Patient/1"},
  "authoredOn": "2025-01-15",
  "dosageInstruction": [{
    "text": "Take 1 tablet once daily",
    "timing": {"repeat": {"frequency": 1, "period": 1, "periodUnit": "d"}},
    "route": {"coding": [{"system": "http://snomed.info/sct", "code": "26643006", "display": "Oral route"}]}
  }]
}' "Lisinopril 10mg (Hypertension)"

# Prenatal Vitamins
post_fhir "MedicationRequest" '{
  "resourceType": "MedicationRequest",
  "status": "active",
  "intent": "order",
  "medicationCodeableConcept": {
    "coding": [
      {"system": "http://www.nlm.nih.gov/research/umls/rxnorm", "code": "1000126", "display": "Prenatal Multivitamin"}
    ]
  },
  "subject": {"reference": "Patient/2"},
  "authoredOn": "2025-03-20",
  "dosageInstruction": [{
    "text": "Take 1 tablet daily",
    "timing": {"repeat": {"frequency": 1, "period": 1, "periodUnit": "d"}}
  }]
}' "Prenatal Vitamins"

# Albuterol Inhaler for Asthma
post_fhir "MedicationRequest" '{
  "resourceType": "MedicationRequest",
  "status": "active",
  "intent": "order",
  "medicationCodeableConcept": {
    "coding": [
      {"system": "http://www.nlm.nih.gov/research/umls/rxnorm", "code": "745752", "display": "Albuterol 90 MCG/ACTUAT Metered Dose Inhaler"}
    ]
  },
  "subject": {"reference": "Patient/3"},
  "authoredOn": "2025-01-10",
  "dosageInstruction": [{
    "text": "Inhale 2 puffs as needed for wheezing or shortness of breath",
    "asNeededBoolean": true,
    "route": {"coding": [{"system": "http://snomed.info/sct", "code": "447694001", "display": "Inhalation route"}]}
  }]
}' "Albuterol Inhaler (Asthma)"

# Sertraline for Depression
post_fhir "MedicationRequest" '{
  "resourceType": "MedicationRequest",
  "status": "active",
  "intent": "order",
  "medicationCodeableConcept": {
    "coding": [
      {"system": "http://www.nlm.nih.gov/research/umls/rxnorm", "code": "312940", "display": "Sertraline 50 MG Oral Tablet"}
    ]
  },
  "subject": {"reference": "Patient/5"},
  "authoredOn": "2025-09-01",
  "dosageInstruction": [{
    "text": "Take 1 tablet once daily",
    "timing": {"repeat": {"frequency": 1, "period": 1, "periodUnit": "d"}}
  }]
}' "Sertraline 50mg (Depression)"

echo ""

# =============================================================================
# STEP 5: CREATE ENCOUNTERS
# =============================================================================

echo -e "${BLUE}Step 5: Creating Encounters${NC}"
echo "-------------------------------------------"

# Annual Wellness Visit
post_fhir "Encounter" '{
  "resourceType": "Encounter",
  "status": "finished",
  "class": {"system": "http://terminology.hl7.org/CodeSystem/v3-ActCode", "code": "AMB", "display": "ambulatory"},
  "type": [{
    "coding": [
      {"system": "http://snomed.info/sct", "code": "439740005", "display": "Annual wellness visit"},
      {"system": "http://www.ama-assn.org/go/cpt", "code": "99385", "display": "Initial comprehensive preventive medicine evaluation"}
    ]
  }],
  "subject": {"reference": "Patient/1"},
  "period": {"start": "2025-11-01T10:00:00Z", "end": "2025-11-01T11:00:00Z"}
}' "Annual Wellness Visit"

# Prenatal Visit
post_fhir "Encounter" '{
  "resourceType": "Encounter",
  "status": "finished",
  "class": {"system": "http://terminology.hl7.org/CodeSystem/v3-ActCode", "code": "AMB"},
  "type": [{
    "coding": [{"system": "http://snomed.info/sct", "code": "424441002", "display": "Prenatal initial visit"}]
  }],
  "subject": {"reference": "Patient/2"},
  "period": {"start": "2025-04-15T14:00:00Z", "end": "2025-04-15T14:45:00Z"}
}' "Prenatal Visit"

# Pediatric Well-Child Visit
post_fhir "Encounter" '{
  "resourceType": "Encounter",
  "status": "finished",
  "class": {"system": "http://terminology.hl7.org/CodeSystem/v3-ActCode", "code": "AMB"},
  "type": [{
    "coding": [{"system": "http://www.ama-assn.org/go/cpt", "code": "99392", "display": "Well-child visit"}]
  }],
  "subject": {"reference": "Patient/3"},
  "period": {"start": "2025-10-20T09:00:00Z", "end": "2025-10-20T09:30:00Z"}
}' "Pediatric Well-Child Visit"

# Mental Health Follow-up
post_fhir "Encounter" '{
  "resourceType": "Encounter",
  "status": "finished",
  "class": {"system": "http://terminology.hl7.org/CodeSystem/v3-ActCode", "code": "AMB"},
  "type": [{
    "coding": [{"system": "http://snomed.info/sct", "code": "185349003", "display": "Mental health follow-up"}]
  }],
  "subject": {"reference": "Patient/5"},
  "period": {"start": "2025-11-10T14:00:00Z", "end": "2025-11-10T14:45:00Z"}
}' "Mental Health Follow-up"

echo ""

# =============================================================================
# STEP 6: CREATE PROCEDURES
# =============================================================================

echo -e "${BLUE}Step 6: Creating Procedures${NC}"
echo "-------------------------------------------"

# Diabetic Retinopathy Screening
post_fhir "Procedure" '{
  "resourceType": "Procedure",
  "status": "completed",
  "code": {
    "coding": [
      {"system": "http://snomed.info/sct", "code": "252779009", "display": "Diabetic retinopathy screening"},
      {"system": "http://www.ama-assn.org/go/cpt", "code": "92250", "display": "Fundus photography"}
    ]
  },
  "subject": {"reference": "Patient/1"},
  "performedDateTime": "2025-06-15"
}' "Diabetic Retinopathy Screening"

# Obstetric Ultrasound
post_fhir "Procedure" '{
  "resourceType": "Procedure",
  "status": "completed",
  "code": {
    "coding": [
      {"system": "http://snomed.info/sct", "code": "268445003", "display": "Antenatal ultrasound scan"},
      {"system": "http://www.ama-assn.org/go/cpt", "code": "76805", "display": "Ultrasound, pregnant uterus"}
    ]
  },
  "subject": {"reference": "Patient/2"},
  "performedDateTime": "2025-05-20"
}' "Obstetric Ultrasound"

# Colonoscopy
post_fhir "Procedure" '{
  "resourceType": "Procedure",
  "status": "completed",
  "code": {
    "coding": [
      {"system": "http://snomed.info/sct", "code": "73761001", "display": "Colonoscopy"},
      {"system": "http://www.ama-assn.org/go/cpt", "code": "45378", "display": "Colonoscopy, flexible"}
    ]
  },
  "subject": {"reference": "Patient/4"},
  "performedDateTime": "2024-03-10"
}' "Colonoscopy (Colorectal Cancer Screening)"

echo ""

# =============================================================================
# STEP 7: CREATE IMMUNIZATIONS
# =============================================================================

echo -e "${BLUE}Step 7: Creating Immunizations${NC}"
echo "-------------------------------------------"

# Influenza Vaccine
post_fhir "Immunization" '{
  "resourceType": "Immunization",
  "status": "completed",
  "vaccineCode": {
    "coding": [
      {"system": "http://hl7.org/fhir/sid/cvx", "code": "141", "display": "Influenza, seasonal, injectable"}
    ]
  },
  "patient": {"reference": "Patient/1"},
  "occurrenceDateTime": "2025-10-01",
  "primarySource": true
}' "Influenza Vaccine"

# Tdap Vaccine (Pregnancy)
post_fhir "Immunization" '{
  "resourceType": "Immunization",
  "status": "completed",
  "vaccineCode": {
    "coding": [
      {"system": "http://hl7.org/fhir/sid/cvx", "code": "115", "display": "Tdap"}
    ]
  },
  "patient": {"reference": "Patient/2"},
  "occurrenceDateTime": "2025-05-15",
  "primarySource": true
}' "Tdap Vaccine (Pregnancy)"

# Pediatric MMR
post_fhir "Immunization" '{
  "resourceType": "Immunization",
  "status": "completed",
  "vaccineCode": {
    "coding": [
      {"system": "http://hl7.org/fhir/sid/cvx", "code": "03", "display": "MMR"}
    ]
  },
  "patient": {"reference": "Patient/3"},
  "occurrenceDateTime": "2019-11-15",
  "primarySource": true
}' "MMR Vaccine (Pediatric)"

echo ""

# =============================================================================
# STEP 8: CREATE ALLERGY INTOLERANCES
# =============================================================================

echo -e "${BLUE}Step 8: Creating Allergies${NC}"
echo "-------------------------------------------"

# Penicillin Allergy
post_fhir "AllergyIntolerance" '{
  "resourceType": "AllergyIntolerance",
  "clinicalStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical", "code": "active"}]},
  "verificationStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-verification", "code": "confirmed"}]},
  "type": "allergy",
  "category": ["medication"],
  "criticality": "high",
  "code": {
    "coding": [{"system": "http://www.nlm.nih.gov/research/umls/rxnorm", "code": "7980", "display": "Penicillin"}]
  },
  "patient": {"reference": "Patient/1"},
  "onsetDateTime": "1985-03-15",
  "reaction": [{
    "manifestation": [{
      "coding": [{"system": "http://snomed.info/sct", "code": "39579001", "display": "Anaphylaxis"}]
    }],
    "severity": "severe"
  }]
}' "Penicillin Allergy (Severe)"

# Peanut Allergy (Pediatric)
post_fhir "AllergyIntolerance" '{
  "resourceType": "AllergyIntolerance",
  "clinicalStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical", "code": "active"}]},
  "verificationStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-verification", "code": "confirmed"}]},
  "type": "allergy",
  "category": ["food"],
  "criticality": "high",
  "code": {
    "coding": [{"system": "http://snomed.info/sct", "code": "762952008", "display": "Peanut allergy"}]
  },
  "patient": {"reference": "Patient/3"},
  "onsetDateTime": "2020-06-10",
  "reaction": [{
    "manifestation": [{
      "coding": [{"system": "http://snomed.info/sct", "code": "271807003", "display": "Rash"}]
    }],
    "severity": "moderate"
  }]
}' "Peanut Allergy (Pediatric)"

echo ""

# =============================================================================
# STEP 9: CREATE CARE PLANS
# =============================================================================

echo -e "${BLUE}Step 9: Creating Care Plans${NC}"
echo "-------------------------------------------"

# Diabetes Management Plan
post_fhir "CarePlan" '{
  "resourceType": "CarePlan",
  "status": "active",
  "intent": "plan",
  "title": "Diabetes Management Plan",
  "description": "Comprehensive diabetes management including medication, diet, and monitoring",
  "subject": {"reference": "Patient/1"},
  "period": {"start": "2025-01-01"},
  "activity": [
    {
      "detail": {
        "code": {"coding": [{"system": "http://snomed.info/sct", "code": "698360004", "display": "Diabetes self-management plan"}]},
        "status": "in-progress",
        "scheduledTiming": {"repeat": {"frequency": 1, "period": 1, "periodUnit": "d"}},
        "description": "Check blood glucose twice daily"
      }
    }
  ]
}' "Diabetes Management Care Plan"

echo ""

# =============================================================================
# STEP 10: CREATE DIAGNOSTIC REPORTS
# =============================================================================

echo -e "${BLUE}Step 10: Creating Diagnostic Reports${NC}"
echo "-------------------------------------------"

# Lipid Panel Report
post_fhir "DiagnosticReport" '{
  "resourceType": "DiagnosticReport",
  "status": "final",
  "category": [{
    "coding": [{"system": "http://terminology.hl7.org/CodeSystem/v2-0074", "code": "LAB", "display": "Laboratory"}]
  }],
  "code": {
    "coding": [{"system": "http://loinc.org", "code": "57698-3", "display": "Lipid panel"}]
  },
  "subject": {"reference": "Patient/1"},
  "effectiveDateTime": "2025-10-15T09:00:00Z",
  "issued": "2025-10-15T14:00:00Z",
  "conclusion": "Lipid levels within acceptable range for patient with diabetes"
}' "Lipid Panel Report"

echo ""

# =============================================================================
# SUMMARY
# =============================================================================

echo "========================================="
echo -e "${GREEN}Test Data Creation Complete!${NC}"
echo "========================================="
echo ""
echo "Created Resources:"
echo "  ✓ 5 Patients (diverse demographics)"
echo "  ✓ 6 Conditions (chronic diseases)"
echo "  ✓ 7 Observations (vitals & labs)"
echo "  ✓ 5 Medications"
echo "  ✓ 4 Encounters"
echo "  ✓ 3 Procedures"
echo "  ✓ 3 Immunizations"
echo "  ✓ 2 Allergies"
echo "  ✓ 1 Care Plan"
echo "  ✓ 1 Diagnostic Report"
echo ""
echo "Coverage includes:"
echo "  • Chronic Disease Management (Diabetes, Hypertension, CKD)"
echo "  • Maternal Health (Pregnancy)"
echo "  • Pediatric Care (Asthma, Well-child)"
echo "  • Mental Health (Depression)"
echo "  • Preventive Care (Screenings, Immunizations)"
echo "  • Safety (Allergies)"
echo ""
echo "These resources support quality measure testing for:"
echo "  - HEDIS measures (Diabetes HbA1c, Blood Pressure Control)"
echo "  - CMS measures (Preventive care, chronic disease management)"
echo "  - Custom measures"
echo ""
echo "View resources at:"
echo "  http://localhost:8000/api/fhir/Patient"
echo "  http://localhost:8000/api/fhir/Observation"
echo "  http://localhost:8000/api/fhir/Condition"
echo ""
