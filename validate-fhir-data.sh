#!/bin/bash

# FHIR Data Validation Script
# Validates that FHIR server has adequate data for demo

set -e

FHIR_URL="${FHIR_URL:-http://localhost:8083/fhir}"

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

# Function to check resource count
check_resource() {
    local resource=$1
    local min_count=$2
    local description=$3
    
    echo -n "Checking $description... "
    count=$(curl -s "$FHIR_URL/$resource?_summary=count" | jq -r '.total // 0')
    
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
    count=$(curl -s "$FHIR_URL/$resource?code=$code&_summary=count" | jq -r '.total // 0')
    
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

check_resource "Patient" 50 "Patients (min 50)" && ((PASS++)) || ((FAIL++))
check_resource "Condition" 50 "Conditions (min 50)" && ((PASS++)) || ((FAIL++))
check_resource "Observation" 200 "Observations (min 200)" && ((PASS++)) || ((FAIL++))
check_resource "MedicationRequest" 50 "Medication Requests (min 50)" && ((PASS++)) || ((FAIL++))
check_resource "Encounter" 50 "Encounters (min 50)" && ((PASS++)) || ((FAIL++))
check_resource "Procedure" 20 "Procedures (min 20)" && ((PASS++)) || ((FAIL++))

echo ""
echo -e "${BLUE}=== Quality Measure Validation ===${NC}"

# Check for diabetes patients
check_coded_resource "Condition" "44054006" "Diabetes Mellitus Type 2 (SNOMED)" && ((PASS++)) || ((FAIL++))

# Check for hypertension patients
check_coded_resource "Condition" "59621000" "Essential Hypertension (SNOMED)" && ((PASS++)) || ((FAIL++))

# Check for HbA1c observations
check_coded_resource "Observation" "4548-4" "HbA1c Observations (LOINC)" && ((PASS++)) || ((FAIL++))

# Check for blood pressure observations
check_coded_resource "Observation" "8480-6" "Systolic BP Observations (LOINC)" && ((PASS++)) || ((FAIL++))

# Check for PHQ-9 depression screenings
check_coded_resource "Observation" "44249-1" "Depression Screening PHQ-9 (LOINC)" && ((PASS++)) || ((FAIL++))

echo ""
echo -e "${BLUE}=== FHIR Server Health ===${NC}"

# Check metadata endpoint
echo -n "FHIR Metadata Endpoint... "
if curl -f -s "$FHIR_URL/metadata" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Responding"
    ((PASS++))
else
    echo -e "${RED}✗${NC} Not responding"
    ((FAIL++))
fi

# Check FHIR version
echo -n "FHIR Version... "
version=$(curl -s "$FHIR_URL/metadata" | jq -r '.fhirVersion // "unknown"')
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
