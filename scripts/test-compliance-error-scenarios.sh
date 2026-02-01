#!/bin/bash

# Compliance Error Scenario Testing Script
# =========================================
# Triggers various error scenarios to test compliance tracking

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
FRONTEND_URL="${FRONTEND_URL:-http://localhost:4200}"
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"

echo "=========================================="
echo -e "${BLUE}Compliance Error Scenario Testing${NC}"
echo "=========================================="
echo ""
echo "This script will trigger various error scenarios"
echo "to test the compliance tracking system."
echo ""
echo "Configuration:"
echo "  Frontend: ${FRONTEND_URL}"
echo "  Backend: ${BACKEND_URL}"
echo "  Gateway: ${GATEWAY_URL}"
echo ""

# Function to trigger FHIR error
trigger_fhir_error() {
    echo -e "${CYAN}Scenario 1: FHIR Service Error${NC}"
    echo "  Triggering: GET invalid patient ID"
    
    # Try to fetch a non-existent patient
    curl -s -X GET "${BACKEND_URL}/fhir/Patient/invalid-patient-id-12345" \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: demo-tenant" \
        > /dev/null 2>&1 || true
    
    echo -e "  ${GREEN}✓${NC} Error triggered"
    echo ""
}

# Function to trigger Care Gap error
trigger_care_gap_error() {
    echo -e "${CYAN}Scenario 2: Care Gap Service Error${NC}"
    echo "  Triggering: GET care gaps for non-existent patient"
    
    # Try to fetch care gaps for invalid patient
    curl -s -X GET "${BACKEND_URL}/care-gap/patient/00000000-0000-0000-0000-999999999999/gaps" \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: demo-tenant" \
        > /dev/null 2>&1 || true
    
    echo -e "  ${GREEN}✓${NC} Error triggered"
    echo ""
}

# Function to trigger Quality Measure error
trigger_quality_measure_error() {
    echo -e "${CYAN}Scenario 3: Quality Measure Service Error${NC}"
    echo "  Triggering: POST invalid measure evaluation"
    
    # Try to evaluate a measure with invalid data
    curl -s -X POST "${BACKEND_URL}/quality-measure/evaluate" \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: demo-tenant" \
        -d '{"measureId": "invalid-measure", "patientId": "invalid-patient"}' \
        > /dev/null 2>&1 || true
    
    echo -e "  ${GREEN}✓${NC} Error triggered"
    echo ""
}

# Function to trigger network error (simulate backend down)
trigger_network_error() {
    echo -e "${CYAN}Scenario 4: Network Error${NC}"
    echo "  Triggering: Request to unavailable service"
    
    # Try to connect to a non-existent service
    curl -s -X GET "http://localhost:9999/nonexistent" \
        --max-time 2 \
        > /dev/null 2>&1 || true
    
    echo -e "  ${GREEN}✓${NC} Error triggered"
    echo ""
}

# Function to trigger API 500 error
trigger_api_500_error() {
    echo -e "${CYAN}Scenario 5: API 500 Error${NC}"
    echo "  Triggering: Request that causes server error"
    
    # Try to trigger a 500 error (invalid endpoint or malformed request)
    curl -s -X POST "${BACKEND_URL}/api/invalid-endpoint" \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: demo-tenant" \
        -d '{"invalid": "data"}' \
        > /dev/null 2>&1 || true
    
    echo -e "  ${GREEN}✓${NC} Error triggered"
    echo ""
}

# Function to trigger multiple errors for threshold testing
trigger_multiple_errors() {
    echo -e "${CYAN}Scenario 6: Multiple Errors (Threshold Testing)${NC}"
    echo "  Triggering: 10 errors in quick succession"
    
    for i in {1..10}; do
        curl -s -X GET "${BACKEND_URL}/fhir/Patient/invalid-id-${i}" \
            -H "Content-Type: application/json" \
            -H "X-Tenant-ID: demo-tenant" \
            > /dev/null 2>&1 || true
        sleep 0.1
    done
    
    echo -e "  ${GREEN}✓${NC} 10 errors triggered"
    echo ""
}

# Main execution
echo "Starting error scenario tests..."
echo ""

# Run all scenarios
trigger_fhir_error
sleep 1

trigger_care_gap_error
sleep 1

trigger_quality_measure_error
sleep 1

trigger_network_error
sleep 1

trigger_api_500_error
sleep 1

trigger_multiple_errors

echo "=========================================="
echo -e "${GREEN}All Error Scenarios Completed${NC}"
echo "=========================================="
echo ""
echo "Next Steps:"
echo "  1. Check frontend compliance dashboard: ${FRONTEND_URL}/compliance"
echo "  2. Monitor database: ./scripts/monitor-compliance-database.sh"
echo "  3. Check backend logs: docker logs hdim-demo-gateway-clinical | grep compliance"
echo "  4. Wait 30 seconds for sync, then check database for errors"
echo ""
