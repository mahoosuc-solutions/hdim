#!/bin/bash

# FHIR Data Validation Script
# Validates that FHIR server has adequate data for demo

set +e

FHIR_URL="${FHIR_URL:-http://localhost:8085/fhir}"
TENANT_ID="${TENANT_ID:-acme-health}"
AUTH_USERNAME="${AUTH_USERNAME:-demo_admin@hdim.ai}"
AUTH_USER_ID="${AUTH_USER_ID:-550e8400-e29b-41d4-a716-446655440010}"
AUTH_ROLES="${AUTH_ROLES:-ADMIN,EVALUATOR}"
USE_TRUSTED_HEADERS="${USE_TRUSTED_HEADERS:-true}"
MIN_PATIENT_COUNT="${MIN_PATIENT_COUNT:-25}"
MIN_ENCOUNTER_COUNT="${MIN_ENCOUNTER_COUNT:-25}"

FHIR_AUTH_HEADER=()
if [ "$USE_TRUSTED_HEADERS" = "true" ]; then
    VALIDATED_TS=$(date +%s)
    FHIR_AUTH_HEADER=(
        -H "X-Auth-User-Id: $AUTH_USER_ID"
        -H "X-Auth-Username: $AUTH_USERNAME"
        -H "X-Auth-Roles: $AUTH_ROLES"
        -H "X-Auth-Tenant-Ids: $TENANT_ID"
        -H "X-Auth-Validated: gateway-${VALIDATED_TS}-dev"
        -H "X-Tenant-ID: $TENANT_ID"
    )
fi

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "========================================="
echo "FHIR Data Validation Report"
echo "========================================="
echo "FHIR Server: $FHIR_URL"
echo "Timestamp: $(date)"
echo ""

mapfile -t PATIENT_IDS < <(curl -s "${FHIR_AUTH_HEADER[@]}" "$FHIR_URL/Patient?_count=50" | jq -r '.entry[].resource.id')
if [ ${#PATIENT_IDS[@]} -eq 0 ]; then
    echo -e "${RED}✗${NC} Unable to locate patient IDs for scoped queries"
fi

UUID_REGEX='^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
FHIR_PATIENT_REF_REGEX='^Patient/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'

non_uuid_patients=0
for patient_id in "${PATIENT_IDS[@]}"; do
    if ! [[ "$patient_id" =~ $UUID_REGEX ]]; then
        ((non_uuid_patients++))
        echo -e "${RED}✗${NC} Non-UUID patient ID detected: $patient_id"
    fi
done

if [ "$non_uuid_patients" -eq 0 ]; then
    echo -e "${GREEN}✓${NC} All patient IDs are UUIDs"
else
    echo -e "${RED}✗${NC} Found $non_uuid_patients non-UUID patient IDs"
    ((FAIL++))
fi

check_patient_references() {
    local resource=$1
    echo -n "Checking $resource patient references... "
    refs=$(curl -s "${FHIR_AUTH_HEADER[@]}" "$FHIR_URL/$resource?_count=200" | jq -r '.entry[]? | .resource.subject.reference? // empty')
    bad_refs=0
    while IFS= read -r ref; do
        if [ -n "$ref" ] && ! [[ "$ref" =~ $FHIR_PATIENT_REF_REGEX ]]; then
            ((bad_refs++))
        fi
    done <<< "$refs"

    if [ "$bad_refs" -eq 0 ]; then
        echo -e "${GREEN}✓${NC} All references use UUIDs"
        return 0
    else
        echo -e "${RED}✗${NC} Found $bad_refs non-UUID references"
        return 1
    fi
}

# Function to check resource count
check_resource() {
    local resource=$1
    local min_count=$2
    local description=$3
    echo -n "Checking $description... "
    if [ "$resource" = "Patient" ]; then
        count=$(curl -s "${FHIR_AUTH_HEADER[@]}" "$FHIR_URL/$resource?_summary=count" | jq -r '.total // 0')
    else
        count=0
        for patient_id in "${PATIENT_IDS[@]}"; do
            patient_count=$(curl -s "${FHIR_AUTH_HEADER[@]}" "$FHIR_URL/$resource?_summary=count&patient=${patient_id}" | jq -r '.total // 0')
            if ! [[ "$patient_count" =~ ^[0-9]+$ ]]; then
                patient_count=0
            fi
            count=$((count + patient_count))
        done
    fi
    
    if [ "$count" -ge "$min_count" ]; then
        echo -e "${GREEN}✓${NC} Found: $count (minimum: $min_count)"
        return 0
    else
        echo -e "${RED}✗${NC} Found: $count (minimum: $min_count)"
        return 1
    fi
}

# Function to check for specific coded resources
check_coded_resource() {
    local resource=$1
    local code=$2
    local description=$3
    echo -n "Checking $description... "
    count=0
    for patient_id in "${PATIENT_IDS[@]}"; do
        patient_count=$(curl -s "${FHIR_AUTH_HEADER[@]}" "$FHIR_URL/$resource?code=$code&_summary=count&patient=${patient_id}" | jq -r '.total // 0')
        if ! [[ "$patient_count" =~ ^[0-9]+$ ]]; then
            patient_count=0
        fi
        count=$((count + patient_count))
    done
    
    if [ "$count" -gt 0 ]; then
        echo -e "${GREEN}✓${NC} Found: $count"
        return 0
    else
        echo -e "${RED}✗${NC} Found: 0"
        return 1
    fi
}

echo -e "${BLUE}=== Resource Count Validation ===${NC}"
PASS=0
FAIL=0

if check_resource "Patient" "$MIN_PATIENT_COUNT" "Patients (min ${MIN_PATIENT_COUNT})"; then ((PASS++)); else ((FAIL++)); fi
if check_resource "Condition" 20 "Conditions (min 20)"; then ((PASS++)); else ((FAIL++)); fi
if check_resource "Observation" 30 "Observations (min 30)"; then ((PASS++)); else ((FAIL++)); fi
if check_resource "MedicationRequest" 20 "Medication Requests (min 20)"; then ((PASS++)); else ((FAIL++)); fi
if check_resource "Encounter" "$MIN_ENCOUNTER_COUNT" "Encounters (min ${MIN_ENCOUNTER_COUNT})"; then ((PASS++)); else ((FAIL++)); fi
if check_resource "Procedure" 0 "Procedures (min 0)"; then ((PASS++)); else ((FAIL++)); fi

echo ""
echo -e "${BLUE}=== UUID Reference Validation ===${NC}"
if check_patient_references "Condition"; then ((PASS++)); else ((FAIL++)); fi
if check_patient_references "Observation"; then ((PASS++)); else ((FAIL++)); fi
if check_patient_references "MedicationRequest"; then ((PASS++)); else ((FAIL++)); fi
if check_patient_references "Encounter"; then ((PASS++)); else ((FAIL++)); fi

echo ""
echo -e "${BLUE}=== Quality Measure Validation ===${NC}"

# Check for diabetes patients
if check_coded_resource "Condition" "44054006" "Diabetes Mellitus Type 2 (SNOMED)"; then ((PASS++)); else ((FAIL++)); fi

# Check for hypertension patients
if check_coded_resource "Condition" "59621000" "Essential Hypertension (SNOMED)"; then ((PASS++)); else ((FAIL++)); fi

# Check for HbA1c observations
if check_coded_resource "Observation" "4548-4" "HbA1c Observations (LOINC)"; then ((PASS++)); else ((FAIL++)); fi

# Check for blood pressure observations (panel code)
if check_coded_resource "Observation" "85354-9" "Blood Pressure Observations (LOINC)"; then ((PASS++)); else ((FAIL++)); fi

# Check for PHQ-9 depression screenings
if check_coded_resource "Observation" "44249-1" "Depression Screening PHQ-9 (LOINC)"; then ((PASS++)); else ((FAIL++)); fi

echo ""
echo -e "${BLUE}=== FHIR Server Health ===${NC}"

# Check metadata endpoint
echo -n "FHIR Metadata Endpoint... "
if curl -f -s "${FHIR_AUTH_HEADER[@]}" "$FHIR_URL/metadata" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Responding"
    ((PASS++))
else
    echo -e "${RED}✗${NC} Not responding"
    ((FAIL++))
fi

# Check FHIR version
echo -n "FHIR Version... "
version=$(curl -s "${FHIR_AUTH_HEADER[@]}" "$FHIR_URL/metadata" | jq -r '.fhirVersion // "unknown"')
if [ "$version" = "4.0.1" ]; then
    echo -e "${GREEN}✓${NC} R4 ($version)"
    ((PASS++))
else
    echo -e "${YELLOW}⚠${NC} Version: $version (expected 4.0.1)"
    ((PASS++))
fi

echo ""
echo "========================================="
echo -e "Validation Results: ${GREEN}$PASS passed${NC}, ${RED}$FAIL failed${NC}"
echo "========================================="

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}✓ FHIR server is DEMO-READY!${NC}"
    exit 0
elif [ $FAIL -le 3 ]; then
    echo -e "${YELLOW}⚠ FHIR server needs minor data loading${NC}"
    exit 1
else
    echo -e "${RED}✗ FHIR server needs significant data population${NC}"
    exit 2
fi
