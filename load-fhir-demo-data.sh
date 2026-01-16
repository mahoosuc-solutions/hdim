#!/bin/bash

# Quick FHIR Population Script
# Populates essential clinical data for quality measure demonstration

set -e

FHIR_URL="${FHIR_URL:-http://localhost:8085/fhir}"
TENANT_ID="${TENANT_ID:-acme-health}"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "========================================="
echo "FHIR Quick Population Script"
echo "========================================="
echo "FHIR Server: $FHIR_URL"
echo "Tenant: $TENANT_ID"
echo ""

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
        id=$(echo "$response" | jq -r '.id // "unknown"')
        echo -e "${GREEN}✓${NC} ID: $id"
        return 0
    else
        echo -e "${RED}✗${NC}"
        echo "$response" | head -3
        return 1
    fi
}

echo -e "${BLUE}Step 1: Adding Diabetes Conditions to Existing Patients${NC}"
echo "-------------------------------------------"

# Add diabetes to patients 1, 3, 4, 6-15 (15 patients total)
for patient_id in 1 3 4 6 7 8 9 10 11 12 13 14 15; do
    post_fhir "Condition" "{
  \"resourceType\": \"Condition\",
  \"subject\": {\"reference\": \"Patient/$patient_id\"},
  \"code\": {
    \"coding\": [{
      \"system\": \"http://snomed.info/sct\",
      \"code\": \"44054006\",
      \"display\": \"Diabetes mellitus type 2\"
    }]
  },
  \"clinicalStatus\": {
    \"coding\": [{
      \"system\": \"http://terminology.hl7.org/CodeSystem/condition-clinical\",
      \"code\": \"active\"
    }]
  },
  \"onsetDateTime\": \"2020-0$((RANDOM % 9 + 1))-15\"
}" "Diabetes for Patient $patient_id"
done

echo ""
echo -e "${BLUE}Step 2: Adding Hypertension Conditions${NC}"
echo "-------------------------------------------"

# Add hypertension to patients 2, 3, 16-25 (15 patients total)
for patient_id in 2 3 16 17 18 19 20 21 22 23 24 25; do
    post_fhir "Condition" "{
  \"resourceType\": \"Condition\",
  \"subject\": {\"reference\": \"Patient/$patient_id\"},
  \"code\": {
    \"coding\": [{
      \"system\": \"http://snomed.info/sct\",
      \"code\": \"59621000\",
      \"display\": \"Essential hypertension\"
    }]
  },
  \"clinicalStatus\": {
    \"coding\": [{
      \"system\": \"http://terminology.hl7.org/CodeSystem/condition-clinical\",
      \"code\": \"active\"
    }]
  },
  \"onsetDateTime\": \"2019-0$((RANDOM % 9 + 1))-15\"
}" "Hypertension for Patient $patient_id"
done

echo ""
echo -e "${BLUE}Step 3: Adding HbA1c Observations for Diabetic Patients${NC}"
echo "-------------------------------------------"

# Compliant patients (HbA1c < 8%)
for patient_id in 1 3 4 6 7 8 9 10; do
    hba1c=$(echo "scale=1; 6.5 + ($RANDOM % 15) / 10" | bc)
    post_fhir "Observation" "{
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
      \"display\": \"Hemoglobin A1c/Hemoglobin.total in Blood\"
    }]
  },
  \"subject\": {\"reference\": \"Patient/$patient_id\"},
  \"effectiveDateTime\": \"2025-0$((RANDOM % 9 + 1))-15\",
  \"valueQuantity\": {
    \"value\": $hba1c,
    \"unit\": \"%\",
    \"system\": \"http://unitsofmeasure.org\",
    \"code\": \"%\"
  }
}" "HbA1c $hba1c% for Patient $patient_id (Compliant)"
done

# Non-compliant patients (HbA1c > 9%)
for patient_id in 11 12 13 14 15; do
    hba1c=$(echo "scale=1; 9.0 + ($RANDOM % 20) / 10" | bc)
    post_fhir "Observation" "{
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
      \"display\": \"Hemoglobin A1c/Hemoglobin.total in Blood\"
    }]
  },
  \"subject\": {\"reference\": \"Patient/$patient_id\"},
  \"effectiveDateTime\": \"2025-0$((RANDOM % 9 + 1))-15\",
  \"valueQuantity\": {
    \"value\": $hba1c,
    \"unit\": \"%\",
    \"system\": \"http://unitsofmeasure.org\",
    \"code\": \"%\"
  }
}" "HbA1c $hba1c% for Patient $patient_id (Non-Compliant)"
done

echo ""
echo -e "${BLUE}Step 4: Adding Blood Pressure Observations${NC}"
echo "-------------------------------------------"

# Compliant BP readings (< 140/90)
for patient_id in 2 3 16 17 18 19 20 21; do
    systolic=$((120 + RANDOM % 15))
    diastolic=$((75 + RANDOM % 10))
    
    post_fhir "Observation" "{
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
  \"subject\": {\"reference\": \"Patient/$patient_id\"},
  \"effectiveDateTime\": \"2025-0$((RANDOM % 9 + 1))-15\",
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
        \"value\": $systolic,
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
        \"value\": $diastolic,
        \"unit\": \"mmHg\",
        \"system\": \"http://unitsofmeasure.org\",
        \"code\": \"mm[Hg]\"
      }
    }
  ]
}" "BP $systolic/$diastolic for Patient $patient_id (Compliant)"
done

# Non-compliant BP readings (> 150/95)
for patient_id in 22 23 24 25; do
    systolic=$((150 + RANDOM % 20))
    diastolic=$((95 + RANDOM % 10))
    
    post_fhir "Observation" "{
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
  \"subject\": {\"reference\": \"Patient/$patient_id\"},
  \"effectiveDateTime\": \"2025-0$((RANDOM % 9 + 1))-15\",
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
        \"value\": $systolic,
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
        \"value\": $diastolic,
        \"unit\": \"mmHg\",
        \"system\": \"http://unitsofmeasure.org\",
        \"code\": \"mm[Hg]\"
      }
    }
  ]
}" "BP $systolic/$diastolic for Patient $patient_id (Non-Compliant)"
done

echo ""
echo -e "${BLUE}Step 5: Adding Depression Screening Observations${NC}"
echo "-------------------------------------------"

# PHQ-9 screenings for patients 26-35
for patient_id in 26 27 28 29 30 31 32 33 34 35; do
    phq9_score=$((RANDOM % 20))
    
    post_fhir "Observation" "{
  \"resourceType\": \"Observation\",
  \"status\": \"final\",
  \"category\": [{
    \"coding\": [{
      \"system\": \"http://terminology.hl7.org/CodeSystem/observation-category\",
      \"code\": \"survey\"
    }]
  }],
  \"code\": {
    \"coding\": [{
      \"system\": \"http://loinc.org\",
      \"code\": \"44249-1\",
      \"display\": \"PHQ-9 quick depression assessment panel\"
    }]
  },
  \"subject\": {\"reference\": \"Patient/$patient_id\"},
  \"effectiveDateTime\": \"2025-0$((RANDOM % 9 + 1))-15\",
  \"valueInteger\": $phq9_score
}" "PHQ-9 Score: $phq9_score for Patient $patient_id"
done

echo ""
echo -e "${BLUE}Step 6: Adding Medication Requests${NC}"
echo "-------------------------------------------"

# Metformin for diabetic patients
for patient_id in 1 3 4 6 7 8 9 10 11 12; do
    post_fhir "MedicationRequest" "{
  \"resourceType\": \"MedicationRequest\",
  \"status\": \"active\",
  \"intent\": \"order\",
  \"medicationCodeableConcept\": {
    \"coding\": [{
      \"system\": \"http://www.nlm.nih.gov/research/umls/rxnorm\",
      \"code\": \"860975\",
      \"display\": \"metformin 500 MG Oral Tablet\"
    }]
  },
  \"subject\": {\"reference\": \"Patient/$patient_id\"},
  \"authoredOn\": \"2024-0$((RANDOM % 9 + 1))-15\",
  \"dosageInstruction\": [{
    \"text\": \"Take 500mg by mouth twice daily with meals\",
    \"timing\": {
      \"repeat\": {
        \"frequency\": 2,
        \"period\": 1,
        \"periodUnit\": \"d\"
      }
    }
  }]
}" "Metformin for Patient $patient_id"
done

# Lisinopril for hypertensive patients
for patient_id in 2 3 16 17 18 19 20 21 22 23; do
    post_fhir "MedicationRequest" "{
  \"resourceType\": \"MedicationRequest\",
  \"status\": \"active\",
  \"intent\": \"order\",
  \"medicationCodeableConcept\": {
    \"coding\": [{
      \"system\": \"http://www.nlm.nih.gov/research/umls/rxnorm\",
      \"code\": \"314076\",
      \"display\": \"lisinopril 10 MG Oral Tablet\"
    }]
  },
  \"subject\": {\"reference\": \"Patient/$patient_id\"},
  \"authoredOn\": \"2024-0$((RANDOM % 9 + 1))-15\",
  \"dosageInstruction\": [{
    \"text\": \"Take 10mg by mouth once daily\",
    \"timing\": {
      \"repeat\": {
        \"frequency\": 1,
        \"period\": 1,
        \"periodUnit\": \"d\"
      }
    }
  }]
}" "Lisinopril for Patient $patient_id"
done

echo ""
echo -e "${BLUE}Step 7: Adding Encounters${NC}"
echo "-------------------------------------------"

# Office visits for patients 1-40
for patient_id in {1..40}; do
    post_fhir "Encounter" "{
  \"resourceType\": \"Encounter\",
  \"status\": \"finished\",
  \"class\": {
    \"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActCode\",
    \"code\": \"AMB\",
    \"display\": \"ambulatory\"
  },
  \"type\": [{
    \"coding\": [{
      \"system\": \"http://snomed.info/sct\",
      \"code\": \"185349003\",
      \"display\": \"Encounter for check up\"
    }]
  }],
  \"subject\": {\"reference\": \"Patient/$patient_id\"},
  \"period\": {
    \"start\": \"2025-0$((RANDOM % 9 + 1))-15T09:00:00Z\",
    \"end\": \"2025-0$((RANDOM % 9 + 1))-15T09:30:00Z\"
  }
}" "Office Visit for Patient $patient_id" 2>&1 | head -1
done

echo ""
echo "========================================="
echo -e "${GREEN}FHIR Population Complete!${NC}"
echo "========================================="
echo ""
echo "Summary of data created:"
echo "- 15 patients with Diabetes mellitus type 2"
echo "- 12 patients with Essential hypertension"
echo "- 13 HbA1c observations (8 compliant, 5 non-compliant)"
echo "- 12 Blood pressure observations (8 compliant, 4 non-compliant)"
echo "- 10 Depression screening (PHQ-9) observations"
echo "- 20 Medication requests (Metformin + Lisinopril)"
echo "- 40 Office visit encounters"
echo ""
echo "Next steps:"
echo "1. Run ./validate-fhir-data.sh to verify data"
echo "2. Test quality measure calculations"
echo "3. Verify care gap generation"
