#!/bin/bash
# Load FHIR Test Data Script
# Loads sample patients, conditions, procedures, and observations into FHIR server
# Usage: ./docker/fhir-test-data/load-test-data.sh [fhir-base-url]

set -e

# FHIR server base URL
FHIR_URL="${1:-http://localhost:8080/fhir}"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Loading FHIR Test Data${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}FHIR Server: ${FHIR_URL}${NC}"
echo ""

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Check if FHIR server is available
echo -ne "${BLUE}Checking FHIR server...${NC} "
if curl -s -o /dev/null -w "%{http_code}" "${FHIR_URL}/metadata" | grep -q "200"; then
    echo -e "${GREEN}✓ Online${NC}"
else
    echo -e "${RED}✗ Offline${NC}"
    echo -e "${RED}Please start the FHIR server first: make up${NC}"
    exit 1
fi

# Function to load a bundle
load_bundle() {
    local file=$1
    local description=$2

    echo -ne "${BLUE}Loading ${description}...${NC} "

    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST \
        -H "Content-Type: application/json" \
        -d @"${SCRIPT_DIR}/${file}" \
        "${FHIR_URL}")

    if [ "$response" == "200" ] || [ "$response" == "201" ]; then
        echo -e "${GREEN}✓ Success (HTTP $response)${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠ Warning (HTTP $response)${NC}"
        return 1
    fi
}

# Load test data in order
total=0
loaded=0

# Patients (must be first)
total=$((total + 1))
if load_bundle "sample-patients.json" "Patients"; then
    loaded=$((loaded + 1))
fi

sleep 1

# Conditions
total=$((total + 1))
if load_bundle "sample-conditions.json" "Conditions"; then
    loaded=$((loaded + 1))
fi

sleep 1

# Observations
total=$((total + 1))
if load_bundle "sample-observations.json" "Observations"; then
    loaded=$((loaded + 1))
fi

sleep 1

# Procedures
total=$((total + 1))
if load_bundle "sample-procedures.json" "Procedures"; then
    loaded=$((loaded + 1))
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Summary${NC}"
echo -e "${BLUE}========================================${NC}"

if [ $loaded -eq $total ]; then
    echo -e "${GREEN}✓ All test data loaded successfully! ($loaded/$total)${NC}"
    echo ""
    echo -e "${BLUE}Test Data Summary:${NC}"
    echo "  - 3 Patients: patient-123, patient-456, patient-789"
    echo "  - 3 Conditions: Hypertension, Diabetes, Depression"
    echo "  - 3 Observations: Blood Pressure, HbA1c, Weight"
    echo "  - 3 Procedures: Mammography, Diabetes Visit, Lipid Panel"
    echo ""
    echo -e "${BLUE}Test Commands:${NC}"
    echo "  curl ${FHIR_URL}/Patient/patient-123"
    echo "  curl ${FHIR_URL}/Condition?patient=Patient/patient-456"
    echo "  curl ${FHIR_URL}/Observation?patient=Patient/patient-789"
    echo ""
    exit 0
else
    echo -e "${YELLOW}⚠ Partially loaded ($loaded/$total)${NC}"
    echo -e "${YELLOW}Some resources may already exist or there were errors${NC}"
    exit 0
fi
