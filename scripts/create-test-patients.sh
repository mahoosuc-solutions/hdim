#!/bin/bash

# Script to create test FHIR patients with clinical data for quality measure testing
# This creates patients with conditions and observations for HEDIS measures

FHIR_URL="http://localhost:8080/fhir"

echo "=========================================="
echo "Creating Test Patients with Clinical Data"
echo "=========================================="

# Patient 1: Diabetes patient with HbA1c tests (for CDC measure)
echo "Creating Patient 1: Maria Garcia (Diabetes with HbA1c)"
PATIENT1_ID=$(curl -s -X POST $FHIR_URL/Patient \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Patient",
    "identifier": [{
      "system": "http://healthdata-in-motion.com/mrn",
      "value": "TEST-001"
    }],
    "name": [{
      "use": "official",
      "family": "Garcia",
      "given": ["Maria"]
    }],
    "gender": "female",
    "birthDate": "1975-06-15",
    "active": true
  }' | grep -o '"id":"[0-9]*"' | cut -d'"' -f4)

echo "Patient 1 ID: $PATIENT1_ID"

# Add Diabetes diagnosis
curl -s -X POST $FHIR_URL/Condition \
  -H "Content-Type: application/fhir+json" \
  -d "{
    \"resourceType\": \"Condition\",
    \"subject\": {
      \"reference\": \"Patient/$PATIENT1_ID\"
    },
    \"code\": {
      \"coding\": [{
        \"system\": \"http://snomed.info/sct\",
        \"code\": \"44054006\",
        \"display\": \"Type 2 diabetes mellitus\"
      }]
    },
    \"onsetDateTime\": \"2020-01-15\",
    \"clinicalStatus\": {
      \"coding\": [{
        \"system\": \"http://terminology.hl7.org/CodeSystem/condition-clinical\",
        \"code\": \"active\"
      }]
    }
  }" > /dev/null

# Add HbA1c observation (good control)
curl -s -X POST $FHIR_URL/Observation \
  -H "Content-Type: application/fhir+json" \
  -d "{
    \"resourceType\": \"Observation\",
    \"status\": \"final\",
    \"category\": [{
      \"coding\": [{
        \"system\": \"http://terminology.hl7.org/CodeSystem/observation-category\",
        \"code\": \"laboratory\"
      }]
    }],
    \"code\": {
      \"coding\": [{
        \"system\": \"http://loinc.org\",
        \"code\": \"4548-4\",
        \"display\": \"Hemoglobin A1c\"
      }]
    },
    \"subject\": {
      \"reference\": \"Patient/$PATIENT1_ID\"
    },
    \"effectiveDateTime\": \"$(date -d '6 months ago' +%Y-%m-%d)\",
    \"valueQuantity\": {
      \"value\": 7.2,
      \"unit\": \"%\",
      \"system\": \"http://unitsofmeasure.org\",
      \"code\": \"%\"
    }
  }" > /dev/null

echo "✓ Created diabetes condition and HbA1c observation"

# Patient 2: Hypertension patient with BP readings (for CBP measure)
echo ""
echo "Creating Patient 2: Robert Chen (Hypertension with BP)"
PATIENT2_ID=$(curl -s -X POST $FHIR_URL/Patient \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Patient",
    "identifier": [{
      "system": "http://healthdata-in-motion.com/mrn",
      "value": "TEST-002"
    }],
    "name": [{
      "use": "official",
      "family": "Chen",
      "given": ["Robert"]
    }],
    "gender": "male",
    "birthDate": "1968-03-22",
    "active": true
  }' | grep -o '"id":"[0-9]*"' | cut -d'"' -f4)

echo "Patient 2 ID: $PATIENT2_ID"

# Add Hypertension diagnosis
curl -s -X POST $FHIR_URL/Condition \
  -H "Content-Type: application/fhir+json" \
  -d "{
    \"resourceType\": \"Condition\",
    \"subject\": {
      \"reference\": \"Patient/$PATIENT2_ID\"
    },
    \"code\": {
      \"coding\": [{
        \"system\": \"http://snomed.info/sct\",
        \"code\": \"38341003\",
        \"display\": \"Essential hypertension\"
      }]
    },
    \"onsetDateTime\": \"2019-05-10\",
    \"clinicalStatus\": {
      \"coding\": [{
        \"system\": \"http://terminology.hl7.org/CodeSystem/condition-clinical\",
        \"code\": \"active\"
      }]
    }
  }" > /dev/null

# Add BP observation (controlled)
curl -s -X POST $FHIR_URL/Observation \
  -H "Content-Type: application/fhir+json" \
  -d "{
    \"resourceType\": \"Observation\",
    \"status\": \"final\",
    \"category\": [{
      \"coding\": [{
        \"system\": \"http://terminology.hl7.org/CodeSystem/observation-category\",
        \"code\": \"vital-signs\"
      }]
    }],
    \"code\": {
      \"coding\": [{
        \"system\": \"http://loinc.org\",
        \"code\": \"85354-9\",
        \"display\": \"Blood pressure panel\"
      }]
    },
    \"subject\": {
      \"reference\": \"Patient/$PATIENT2_ID\"
    },
    \"effectiveDateTime\": \"$(date -d '3 months ago' +%Y-%m-%d)\",
    \"component\": [
      {
        \"code\": {
          \"coding\": [{
            \"system\": \"http://loinc.org\",
            \"code\": \"8480-6\",
            \"display\": \"Systolic blood pressure\"
          }]
        },
        \"valueQuantity\": {
          \"value\": 128,
          \"unit\": \"mmHg\",
          \"system\": \"http://unitsofmeasure.org\",
          \"code\": \"mm[Hg]\"
        }
      },
      {
        \"code\": {
          \"coding\": [{
            \"system\": \"http://loinc.org\",
            \"code\": \"8462-4\",
            \"display\": \"Diastolic blood pressure\"
          }]
        },
        \"valueQuantity\": {
          \"value\": 82,
          \"unit\": \"mmHg\",
          \"system\": \"http://unitsofmeasure.org\",
          \"code\": \"mm[Hg]\"
        }
      }
    ]
  }" > /dev/null

echo "✓ Created hypertension condition and BP observation"

# Patient 3: Healthy patient (control group)
echo ""
echo "Creating Patient 3: Sarah Johnson (Healthy control)"
PATIENT3_ID=$(curl -s -X POST $FHIR_URL/Patient \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Patient",
    "identifier": [{
      "system": "http://healthdata-in-motion.com/mrn",
      "value": "TEST-003"
    }],
    "name": [{
      "use": "official",
      "family": "Johnson",
      "given": ["Sarah"]
    }],
    "gender": "female",
    "birthDate": "1990-09-05",
    "active": true
  }' | grep -o '"id":"[0-9]*"' | cut -d'"' -f4)

echo "Patient 3 ID: $PATIENT3_ID"
echo "✓ Created healthy patient (no conditions)"

echo ""
echo "=========================================="
echo "Test Patient Creation Complete!"
echo "=========================================="
echo ""
echo "Summary:"
echo "  Patient 1 (Maria Garcia): Diabetes with HbA1c 7.2%"
echo "  Patient 2 (Robert Chen): Hypertension with BP 128/82"
echo "  Patient 3 (Sarah Johnson): Healthy (no conditions)"
echo ""
echo "You can now run CQL evaluations against these patients."
