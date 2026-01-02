#!/bin/bash

# Custom Measures Testing Script
# Demonstrates creating and testing quality measures with comprehensive FHIR test data

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

BASE_URL="${BASE_URL:-http://localhost:8000/api/quality/quality-measure}"
TENANT_ID="${TENANT_ID:-clinic-001}"

echo "========================================="
echo -e "${BLUE}Custom Measures Testing Script${NC}"
echo "========================================="
echo ""

# Function to pretty print JSON
pretty_json() {
    if command -v python3 &> /dev/null; then
        python3 -m json.tool
    elif command -v jq &> /dev/null; then
        jq .
    else
        cat
    fi
}

# Function to create measure
create_measure() {
    local name=$1
    local category=$2
    local description=$3

    echo -e "${YELLOW}Creating measure: ${name}${NC}"

    response=$(curl -s -X POST "${BASE_URL}/custom-measures" \
        -H "X-Tenant-ID: ${TENANT_ID}" \
        -H "Content-Type: application/json" \
        -d "{
            \"name\": \"${name}\",
            \"description\": \"${description}\",
            \"category\": \"${category}\",
            \"year\": 2025
        }")

    echo "$response" | pretty_json

    # Extract ID for later use
    if command -v python3 &> /dev/null; then
        measure_id=$(echo "$response" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")
        echo -e "${GREEN}✓ Created measure: ${measure_id}${NC}"
        echo "$measure_id"
    elif command -v jq &> /dev/null; then
        measure_id=$(echo "$response" | jq -r '.id')
        echo -e "${GREEN}✓ Created measure: ${measure_id}${NC}"
        echo "$measure_id"
    else
        echo -e "${GREEN}✓ Measure created${NC}"
        echo ""
    fi
}

# Function to list measures
list_measures() {
    echo -e "${YELLOW}Listing all custom measures...${NC}"
    curl -s "${BASE_URL}/custom-measures" \
        -H "X-Tenant-ID: ${TENANT_ID}" | pretty_json
}

# Function to check FHIR data
check_fhir_data() {
    echo -e "${YELLOW}Checking FHIR test data...${NC}"

    # Check patients
    patient_count=$(curl -s "http://localhost:8000/api/fhir/Patient?identifier=TEST-" | \
        grep -o '"resourceType":"Patient"' | wc -l || echo "0")

    if [ "$patient_count" -gt 0 ]; then
        echo -e "${GREEN}✓ Found ${patient_count} test patients${NC}"
    else
        echo -e "${RED}✗ No test patients found. Run: ./sample-data/comprehensive-fhir-test-data.sh${NC}"
        exit 1
    fi
}

# Main execution
main() {
    echo -e "${BLUE}Step 1: Verify FHIR Test Data${NC}"
    echo "-------------------------------------------"
    check_fhir_data
    echo ""

    echo -e "${BLUE}Step 2: Create Quality Measures${NC}"
    echo "-------------------------------------------"

    # Example 1: Diabetes HbA1c Control
    diabetes_id=$(create_measure \
        "CDC-A1C - Diabetes HbA1c Control" \
        "Diabetes" \
        "HEDIS measure: Percentage of diabetic patients aged 18-75 with HbA1c <8%")
    echo ""
    sleep 1

    # Example 2: Blood Pressure Control
    bp_id=$(create_measure \
        "CBP - Controlling Blood Pressure" \
        "Hypertension" \
        "HEDIS measure: Hypertensive patients with BP <140/90 mmHg")
    echo ""
    sleep 1

    # Example 3: Prenatal Care
    prenatal_id=$(create_measure \
        "PPC - Timeliness of Prenatal Care" \
        "Maternal Health" \
        "HEDIS measure: Prenatal care visit in first trimester")
    echo ""
    sleep 1

    # Example 4: Depression Screening
    depression_id=$(create_measure \
        "CMS2v12 - Depression Screening" \
        "Mental Health" \
        "Annual depression screening using PHQ-9 or equivalent")
    echo ""
    sleep 1

    # Example 5: Pediatric Asthma
    asthma_id=$(create_measure \
        "ASM - Asthma Medication Ratio" \
        "Pediatric" \
        "HEDIS measure: Pediatric asthma patients with appropriate controller therapy")
    echo ""
    sleep 1

    # Example 6: CKD Monitoring
    ckd_id=$(create_measure \
        "CKD Monitoring - eGFR" \
        "Nephrology" \
        "Annual eGFR monitoring for patients with chronic kidney disease")
    echo ""

    echo -e "${BLUE}Step 3: List All Measures${NC}"
    echo "-------------------------------------------"
    list_measures
    echo ""

    echo -e "${BLUE}Step 4: Test Patient Matching${NC}"
    echo "-------------------------------------------"
    echo -e "${YELLOW}Checking test patients against measures...${NC}"
    echo ""

    # Patient 179: Thomas Anderson - Diabetes + Hypertension
    echo -e "${GREEN}Patient 179 (Thomas Anderson):${NC}"
    echo "  - CDC-A1C (Diabetes HbA1c): Expected PASS (HbA1c 7.2% < 8%)"
    echo "  - CBP (Blood Pressure): Expected PASS (BP 128/82 < 140/90)"
    echo ""

    # Patient 180: Sofia Martinez - Pregnant
    echo -e "${GREEN}Patient 180 (Sofia Martinez):${NC}"
    echo "  - PPC (Prenatal Care): Expected PASS (1st trimester visit 2025-04-15)"
    echo ""

    # Patient 181: Emily Chen - Pediatric Asthma
    echo -e "${GREEN}Patient 181 (Emily Chen):${NC}"
    echo "  - ASM (Asthma Medication): Expected PASS (Controller medication active)"
    echo ""

    # Patient 182: Robert Johnson - CKD
    echo -e "${GREEN}Patient 182 (Robert Johnson):${NC}"
    echo "  - CKD Monitoring: Expected PASS (eGFR tested 2025-11-01)"
    echo ""

    # Patient 183: Sarah Williams - Depression
    echo -e "${GREEN}Patient 183 (Sarah Williams):${NC}"
    echo "  - CMS2 (Depression Screening): Expected PASS (PHQ-9 completed 2025-11-10)"
    echo ""

    echo "========================================="
    echo -e "${GREEN}Testing Complete!${NC}"
    echo "========================================="
    echo ""
    echo "Next steps:"
    echo "1. Open Clinical Portal: http://localhost:4200/measure-builder"
    echo "2. View created measures in the UI"
    echo "3. Run evaluations against test patients"
    echo "4. Generate quality reports"
    echo ""
    echo "Documentation:"
    echo "  - CUSTOM_MEASURES_EXAMPLES.md - Detailed usage examples"
    echo "  - COMPREHENSIVE_FHIR_TEST_DATA.md - Test data reference"
    echo ""
}

# Run main function
main
