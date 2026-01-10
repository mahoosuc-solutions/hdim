#!/bin/bash
# =============================================================================
# Load Sample FHIR Data into External HAPI FHIR Server
# =============================================================================
# This script loads sample clinical data for testing quality measures
#
# Prerequisites:
# - HAPI FHIR server running on localhost:8080
# - curl installed
#
# Usage:
#   ./scripts/load-sample-fhir-data.sh
# =============================================================================

FHIR_URL="${FHIR_URL:-http://localhost:8080/fhir}"

echo "========================================"
echo "Loading Sample FHIR Data"
echo "========================================"
echo "FHIR Server: $FHIR_URL"
echo ""

# Check if FHIR server is ready
echo "Checking FHIR server availability..."
for i in {1..30}; do
    if curl -s "$FHIR_URL/metadata" > /dev/null 2>&1; then
        echo "FHIR server is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "ERROR: FHIR server not available after 30 attempts"
        exit 1
    fi
    echo "  Waiting for FHIR server... ($i/30)"
    sleep 5
done

echo ""
echo "[1/4] Creating Patients..."

# Patient 1: Michael Chen - Diabetes patient
curl -s -X POST "$FHIR_URL/Patient" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Patient",
    "id": "patient-001",
    "identifier": [{"system": "http://demo-tenant.com/mrn", "value": "MRN001"}],
    "name": [{"family": "Chen", "given": ["Michael"]}],
    "gender": "male",
    "birthDate": "1965-03-15",
    "address": [{"city": "Boston", "state": "MA", "postalCode": "02101"}]
  }' > /dev/null

# Patient 2: Sarah Martinez - Hypertension patient
curl -s -X POST "$FHIR_URL/Patient" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Patient",
    "id": "patient-002",
    "identifier": [{"system": "http://demo-tenant.com/mrn", "value": "MRN002"}],
    "name": [{"family": "Martinez", "given": ["Sarah"]}],
    "gender": "female",
    "birthDate": "1972-07-22",
    "address": [{"city": "Cambridge", "state": "MA", "postalCode": "02139"}]
  }' > /dev/null

# Patient 3: Emma Johnson - CHF patient
curl -s -X POST "$FHIR_URL/Patient" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Patient",
    "id": "patient-003",
    "identifier": [{"system": "http://demo-tenant.com/mrn", "value": "MRN003"}],
    "name": [{"family": "Johnson", "given": ["Emma"]}],
    "gender": "female",
    "birthDate": "1958-11-30",
    "address": [{"city": "Somerville", "state": "MA", "postalCode": "02143"}]
  }' > /dev/null

# Patient 4: Carlos Rodriguez - Multiple conditions
curl -s -X POST "$FHIR_URL/Patient" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Patient",
    "id": "patient-004",
    "identifier": [{"system": "http://demo-tenant.com/mrn", "value": "MRN004"}],
    "name": [{"family": "Rodriguez", "given": ["Carlos"]}],
    "gender": "male",
    "birthDate": "1970-05-10",
    "address": [{"city": "Brookline", "state": "MA", "postalCode": "02445"}]
  }' > /dev/null

echo "  Created 4 patients"

echo ""
echo "[2/4] Creating Conditions..."

# Diabetes for Michael Chen
curl -s -X POST "$FHIR_URL/Condition" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Condition",
    "subject": {"reference": "Patient/patient-001"},
    "code": {"coding": [{"system": "http://snomed.info/sct", "code": "73211009", "display": "Diabetes mellitus"}]},
    "clinicalStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active"}]},
    "onsetDateTime": "2020-01-15"
  }' > /dev/null

# Hypertension for Sarah Martinez
curl -s -X POST "$FHIR_URL/Condition" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Condition",
    "subject": {"reference": "Patient/patient-002"},
    "code": {"coding": [{"system": "http://snomed.info/sct", "code": "38341003", "display": "Hypertension"}]},
    "clinicalStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active"}]},
    "onsetDateTime": "2019-06-20"
  }' > /dev/null

# CHF for Emma Johnson
curl -s -X POST "$FHIR_URL/Condition" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Condition",
    "subject": {"reference": "Patient/patient-003"},
    "code": {"coding": [{"system": "http://snomed.info/sct", "code": "84114007", "display": "Heart failure"}]},
    "clinicalStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active"}]},
    "onsetDateTime": "2021-03-10"
  }' > /dev/null

# Multiple conditions for Carlos Rodriguez
curl -s -X POST "$FHIR_URL/Condition" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Condition",
    "subject": {"reference": "Patient/patient-004"},
    "code": {"coding": [{"system": "http://snomed.info/sct", "code": "73211009", "display": "Diabetes mellitus"}]},
    "clinicalStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active"}]},
    "onsetDateTime": "2018-09-05"
  }' > /dev/null

curl -s -X POST "$FHIR_URL/Condition" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Condition",
    "subject": {"reference": "Patient/patient-004"},
    "code": {"coding": [{"system": "http://snomed.info/sct", "code": "38341003", "display": "Hypertension"}]},
    "clinicalStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active"}]},
    "onsetDateTime": "2019-02-14"
  }' > /dev/null

echo "  Created 5 conditions"

echo ""
echo "[3/4] Creating Observations..."

# HbA1c for Michael Chen (diabetes control)
curl -s -X POST "$FHIR_URL/Observation" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Observation",
    "status": "final",
    "subject": {"reference": "Patient/patient-001"},
    "code": {"coding": [{"system": "http://loinc.org", "code": "4548-4", "display": "Hemoglobin A1c"}]},
    "valueQuantity": {"value": 7.2, "unit": "%", "system": "http://unitsofmeasure.org", "code": "%"},
    "effectiveDateTime": "2025-12-15"
  }' > /dev/null

# Blood Pressure for Sarah Martinez
curl -s -X POST "$FHIR_URL/Observation" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Observation",
    "status": "final",
    "subject": {"reference": "Patient/patient-002"},
    "code": {"coding": [{"system": "http://loinc.org", "code": "85354-9", "display": "Blood pressure panel"}]},
    "component": [
      {"code": {"coding": [{"system": "http://loinc.org", "code": "8480-6", "display": "Systolic BP"}]}, "valueQuantity": {"value": 145, "unit": "mmHg"}},
      {"code": {"coding": [{"system": "http://loinc.org", "code": "8462-4", "display": "Diastolic BP"}]}, "valueQuantity": {"value": 92, "unit": "mmHg"}}
    ],
    "effectiveDateTime": "2025-12-10"
  }' > /dev/null

# BNP for Emma Johnson (heart failure marker)
curl -s -X POST "$FHIR_URL/Observation" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Observation",
    "status": "final",
    "subject": {"reference": "Patient/patient-003"},
    "code": {"coding": [{"system": "http://loinc.org", "code": "30934-4", "display": "BNP"}]},
    "valueQuantity": {"value": 450, "unit": "pg/mL", "system": "http://unitsofmeasure.org", "code": "pg/mL"},
    "effectiveDateTime": "2025-12-01"
  }' > /dev/null

# HbA1c for Carlos Rodriguez
curl -s -X POST "$FHIR_URL/Observation" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Observation",
    "status": "final",
    "subject": {"reference": "Patient/patient-004"},
    "code": {"coding": [{"system": "http://loinc.org", "code": "4548-4", "display": "Hemoglobin A1c"}]},
    "valueQuantity": {"value": 8.5, "unit": "%", "system": "http://unitsofmeasure.org", "code": "%"},
    "effectiveDateTime": "2025-11-20"
  }' > /dev/null

echo "  Created 4 observations"

echo ""
echo "[4/4] Creating Medications..."

# Metformin for Michael Chen
curl -s -X POST "$FHIR_URL/MedicationRequest" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "MedicationRequest",
    "status": "active",
    "intent": "order",
    "subject": {"reference": "Patient/patient-001"},
    "medicationCodeableConcept": {"coding": [{"system": "http://www.nlm.nih.gov/research/umls/rxnorm", "code": "860975", "display": "Metformin 500mg"}]},
    "dosageInstruction": [{"text": "Take 500mg twice daily with meals"}],
    "authoredOn": "2025-01-15"
  }' > /dev/null

# Lisinopril for Sarah Martinez
curl -s -X POST "$FHIR_URL/MedicationRequest" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "MedicationRequest",
    "status": "active",
    "intent": "order",
    "subject": {"reference": "Patient/patient-002"},
    "medicationCodeableConcept": {"coding": [{"system": "http://www.nlm.nih.gov/research/umls/rxnorm", "code": "314076", "display": "Lisinopril 10mg"}]},
    "dosageInstruction": [{"text": "Take 10mg once daily"}],
    "authoredOn": "2025-02-10"
  }' > /dev/null

# Furosemide for Emma Johnson
curl -s -X POST "$FHIR_URL/MedicationRequest" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "MedicationRequest",
    "status": "active",
    "intent": "order",
    "subject": {"reference": "Patient/patient-003"},
    "medicationCodeableConcept": {"coding": [{"system": "http://www.nlm.nih.gov/research/umls/rxnorm", "code": "310429", "display": "Furosemide 40mg"}]},
    "dosageInstruction": [{"text": "Take 40mg once daily in the morning"}],
    "authoredOn": "2025-03-20"
  }' > /dev/null

echo "  Created 3 medication requests"

echo ""
echo "========================================"
echo "Sample FHIR Data Loaded Successfully!"
echo "========================================"
echo ""
echo "Summary:"
echo "  - 4 Patients"
echo "  - 5 Conditions (Diabetes, Hypertension, CHF)"
echo "  - 4 Observations (HbA1c, BP, BNP)"
echo "  - 3 Medication Requests"
echo ""
echo "Query examples:"
echo "  curl '$FHIR_URL/Patient'"
echo "  curl '$FHIR_URL/Condition?code=73211009'"
echo "  curl '$FHIR_URL/Observation?code=4548-4'"
echo ""
