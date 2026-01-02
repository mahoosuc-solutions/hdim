#!/bin/bash

# HealthData Platform API Test Script
# Tests all REST API endpoints

API_BASE="http://localhost:8080/api"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}HealthData Platform API Tests${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local description=$3
    local data=$4

    echo -e "${BLUE}Testing:${NC} $description"
    echo "  Method: $method"
    echo "  Endpoint: $endpoint"

    if [ -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X $method "$API_BASE$endpoint" -H "Content-Type: application/json")
    else
        response=$(curl -s -w "\n%{http_code}" -X $method "$API_BASE$endpoint" -H "Content-Type: application/json" -d "$data")
    fi

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [[ $http_code -ge 200 && $http_code -lt 300 ]] || [[ $http_code -eq 401 ]] || [[ $http_code -eq 403 ]]; then
        echo -e "  Status: ${GREEN}$http_code${NC}"
        if [[ $http_code -eq 401 ]]; then
            echo -e "  Note: ${BLUE}Authentication required (expected)${NC}"
        elif [[ $http_code -eq 403 ]]; then
            echo -e "  Note: ${BLUE}Authorization required (expected)${NC}"
        fi
    else
        echo -e "  Status: ${RED}$http_code${NC}"
    fi

    # Show sample of response body
    if [ ! -z "$body" ]; then
        echo "  Response: $(echo $body | head -c 100)..."
    fi
    echo ""
}

echo -e "${BLUE}1. HEALTH CHECK ENDPOINTS${NC}"
echo "========================================="
test_endpoint "GET" "/health" "Platform Health Check"
test_endpoint "GET" "/health/ready" "Readiness Check"
test_endpoint "GET" "/health/live" "Liveness Check"

echo -e "${BLUE}2. PATIENT ENDPOINTS${NC}"
echo "========================================="

# Create test patient
patient_data='{
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1980-01-01",
    "gender": "MALE",
    "tenantId": "tenant-1"
}'
test_endpoint "POST" "/patients" "Create Patient" "$patient_data"

# Get patients
test_endpoint "GET" "/patients?tenantId=tenant-1" "List Patients"
test_endpoint "GET" "/patients/test-patient-1" "Get Patient by ID"

# Update patient
update_data='{
    "firstName": "John",
    "lastName": "Smith",
    "dateOfBirth": "1980-01-01",
    "gender": "MALE",
    "tenantId": "tenant-1"
}'
test_endpoint "PUT" "/patients/test-patient-1" "Update Patient" "$update_data"

echo -e "${BLUE}3. QUALITY MEASURE ENDPOINTS${NC}"
echo "========================================="
test_endpoint "POST" "/measures/calculate?patientId=test-patient-1&measureId=HbA1c-Control" "Calculate Single Measure"
test_endpoint "POST" "/measures/batch?tenantId=tenant-1&measureId=HbA1c-Control" "Batch Calculate Measures"
test_endpoint "GET" "/measures/status?patientId=test-patient-1&measureId=HbA1c-Control" "Get Measure Status"

echo -e "${BLUE}4. CARE GAP ENDPOINTS${NC}"
echo "========================================="
test_endpoint "GET" "/caregaps/test-patient-1" "Get Patient Care Gaps"
test_endpoint "POST" "/caregaps/detect-batch" "Batch Detect Care Gaps" '["patient-1", "patient-2", "patient-3"]'
test_endpoint "POST" "/caregaps/gap-1/close?reason=Completed" "Close Care Gap"

echo -e "${BLUE}5. FHIR RESOURCE ENDPOINTS${NC}"
echo "========================================="
test_endpoint "GET" "/fhir/observations/test-patient-1" "Get Patient Observations"
test_endpoint "GET" "/fhir/conditions/test-patient-1" "Get Patient Conditions"
test_endpoint "GET" "/fhir/medications/test-patient-1" "Get Patient Medications"

echo -e "${BLUE}6. PATIENT HEALTH OVERVIEW${NC}"
echo "========================================="
test_endpoint "GET" "/patient-health/overview?patientId=test-patient-1" "Get Patient Health Overview"

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}API Testing Complete!${NC}"
echo -e "${BLUE}========================================${NC}"

# Summary
echo ""
echo "Note: Some endpoints may require authentication (401) or authorization (403)."
echo "This is expected behavior for secured endpoints."
echo ""
echo "To test authenticated endpoints, you'll need to:"
echo "1. Obtain a JWT token from the auth endpoint"
echo "2. Include it in the Authorization header: -H \"Authorization: Bearer <token>\""