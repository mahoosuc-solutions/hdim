#!/bin/bash

# Quick FHIR Population Script
# Populates essential clinical data for quality measure demonstration

set -e

GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
FHIR_URL="${FHIR_URL:-http://localhost:8085/fhir}"
TENANT_ID="${TENANT_ID:-acme-health}"
AUTH_USERNAME="${AUTH_USERNAME:-demo_admin@hdim.ai}"
AUTH_PASSWORD="${AUTH_PASSWORD:-demo123}"
AUTH_USER_ID="${AUTH_USER_ID:-550e8400-e29b-41d4-a716-446655440010}"
AUTH_ROLES="${AUTH_ROLES:-ADMIN,EVALUATOR}"
USE_TRUSTED_HEADERS="${USE_TRUSTED_HEADERS:-true}"
API_TOKEN="${API_TOKEN:-}"

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

TARGET_PATIENT_COUNT="${TARGET_PATIENT_COUNT:-25}"

AUTH_HEADER=()
if [ "$USE_TRUSTED_HEADERS" = "true" ]; then
    VALIDATED_TS=$(date +%s)
    AUTH_HEADER=(
        -H "X-Auth-User-Id: $AUTH_USER_ID"
        -H "X-Auth-Username: $AUTH_USERNAME"
        -H "X-Auth-Roles: $AUTH_ROLES"
        -H "X-Auth-Tenant-Ids: $TENANT_ID"
        -H "X-Auth-Validated: gateway-${VALIDATED_TS}-dev"
    )
else
    # Optional auth via gateway login (uses demo credentials by default)
    if [ -z "$API_TOKEN" ]; then
        LOGIN_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/login" \
            -H "Content-Type: application/json" \
            -d "{\"username\":\"${AUTH_USERNAME}\",\"password\":\"${AUTH_PASSWORD}\"}")
        API_TOKEN=$(echo "$LOGIN_RESPONSE" | python3 -c "import json,sys; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
    fi

    if [ -n "$API_TOKEN" ]; then
        AUTH_HEADER=(-H "Authorization: Bearer $API_TOKEN")
    fi
fi

# Function to create a patient and return the ID
create_patient() {
    local index=$1
    local response
    response=$(curl -s -X POST "$FHIR_URL/Patient" \
        -H "Content-Type: application/fhir+json" \
        -H "X-Tenant-ID: $TENANT_ID" \
        "${AUTH_HEADER[@]}" \
        -d "{
  \"resourceType\": \"Patient\",
  \"name\": [{
    \"use\": \"official\",
    \"family\": \"Demo\",
    \"given\": [\"Patient\", \"$index\"]
  }],
  \"gender\": \"unknown\",
  \"birthDate\": \"1980-01-01\"
}")
    echo "$response" | python3 -c "import json,sys; print(json.load(sys.stdin).get('id',''))" 2>/dev/null
}

# Function to post FHIR resource
post_fhir() {
    local resource_type=$1
    local data=$2
    local description=$3

    echo -n "Creating $description... "
    response=$(curl -s -X POST "$FHIR_URL/$resource_type" \
        -H "Content-Type: application/fhir+json" \
        -H "X-Tenant-ID: $TENANT_ID" \
        "${AUTH_HEADER[@]}" \
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

echo -e "${BLUE}Step 0: Ensuring Patients Exist${NC}"
echo "-------------------------------------------"
mapfile -t patient_ids < <(curl -s -X GET "$FHIR_URL/Patient?_count=50" \
    -H "X-Tenant-ID: $TENANT_ID" \
    "${AUTH_HEADER[@]}" | python3 -c "import json,sys; data=json.load(sys.stdin); entries=data.get('entry') or []; \
print('\\n'.join([e.get('resource',{}).get('id','') for e in entries if e.get('resource',{}).get('id')]))"
)

UUID_REGEX='^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
valid_patient_ids=()
for patient_id in "${patient_ids[@]}"; do
    if [[ "$patient_id" =~ $UUID_REGEX ]]; then
        valid_patient_ids+=("$patient_id")
    else
        echo -e "${YELLOW}Skipping non-UUID patient ID: $patient_id${NC}"
    fi
done
patient_ids=("${valid_patient_ids[@]}")

if [ "${#patient_ids[@]}" -lt "$TARGET_PATIENT_COUNT" ]; then
    for i in $(seq $(("${#patient_ids[@]}" + 1)) "$TARGET_PATIENT_COUNT"); do
        new_id=$(create_patient "$i")
        if [[ -n "$new_id" && "$new_id" =~ $UUID_REGEX ]]; then
            patient_ids+=("$new_id")
            echo -e "Created Patient $i... ${GREEN}✓${NC} ID: $new_id"
        else
            echo -e "Created Patient $i... ${RED}✗${NC}"
            exit 1
        fi
    done
else
    echo -e "${GREEN}✓${NC} Found ${#patient_ids[@]} existing patients"
fi

diabetes_ids=("${patient_ids[@]:0:13}")
hypertension_ids=("${patient_ids[@]:13:12}")
bp_noncompliant_ids=("${hypertension_ids[@]:8}")
depression_ids=("${patient_ids[@]:15:10}")
metformin_ids=("${diabetes_ids[@]:0:10}")
lisinopril_ids=("${hypertension_ids[@]:0:10}")
encounter_ids=("${patient_ids[@]:0:40}")

echo ""
echo -e "${BLUE}Step 1: Adding Diabetes Conditions to Existing Patients${NC}"
echo "-------------------------------------------"

# Add diabetes to patients 1, 3, 4, 6-15 (15 patients total)
for patient_id in "${diabetes_ids[@]}"; do
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
for patient_id in "${hypertension_ids[@]}"; do
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
for patient_id in "${diabetes_ids[@]:0:8}"; do
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
for patient_id in "${diabetes_ids[@]:8:5}"; do
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
for patient_id in "${hypertension_ids[@]:0:8}"; do
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
for patient_id in "${bp_noncompliant_ids[@]}"; do
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
for patient_id in "${depression_ids[@]}"; do
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
for patient_id in "${metformin_ids[@]}"; do
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
  \"authoredOn\": \"2024-0$((RANDOM % 9 + 1))-15\"
}" "Metformin for Patient $patient_id"
done

# Lisinopril for hypertensive patients
for patient_id in "${lisinopril_ids[@]}"; do
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
  \"authoredOn\": \"2024-0$((RANDOM % 9 + 1))-15\"
}" "Lisinopril for Patient $patient_id"
done

echo ""
echo -e "${BLUE}Step 7: Adding Encounters${NC}"
echo "-------------------------------------------"

# Office visits for patients 1-40
for patient_id in "${encounter_ids[@]}"; do
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
echo "- ${#diabetes_ids[@]} patients with Diabetes mellitus type 2"
echo "- ${#hypertension_ids[@]} patients with Essential hypertension"
echo "- 13 HbA1c observations (8 compliant, 5 non-compliant)"
echo "- $(( ${#hypertension_ids[@]} )) Blood pressure observations (8 compliant, ${#bp_noncompliant_ids[@]} non-compliant)"
echo "- ${#depression_ids[@]} Depression screening (PHQ-9) observations"
echo "- $(( ${#metformin_ids[@]} + ${#lisinopril_ids[@]} )) Medication requests (Metformin + Lisinopril)"
echo "- ${#encounter_ids[@]} Office visit encounters"
echo ""
echo "Next steps:"
echo "1. Run ./validate-fhir-data.sh to verify data"
echo "2. Test quality measure calculations"
echo "3. Verify care gap generation"
