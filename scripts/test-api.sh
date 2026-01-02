#!/bin/bash
# API Testing Script
# Tests CQL Engine API endpoints
# Usage: ./scripts/test-api.sh

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# API Base URL
BASE_URL="http://localhost:8081"
TENANT_ID="tenant-1"

# Test counter
total_tests=0
passed_tests=0

# Test function
test_endpoint() {
    local name=$1
    local method=$2
    local endpoint=$3
    local expected_code=$4
    local extra_args=$5

    total_tests=$((total_tests + 1))

    echo -ne "${BLUE}Testing:${NC} $name ... "

    if [ "$method" == "GET" ]; then
        response=$(curl -s -o /dev/null -w "%{http_code}" \
            -H "X-Tenant-ID: $TENANT_ID" \
            $extra_args \
            "${BASE_URL}${endpoint}" 2>/dev/null || echo "000")
    else
        response=$(curl -s -o /dev/null -w "%{http_code}" \
            -X "$method" \
            -H "Content-Type: application/json" \
            -H "X-Tenant-ID: $TENANT_ID" \
            $extra_args \
            "${BASE_URL}${endpoint}" 2>/dev/null || echo "000")
    fi

    if [ "$response" == "$expected_code" ]; then
        echo -e "${GREEN}✓ PASS${NC} (HTTP $response)"
        passed_tests=$((passed_tests + 1))
        return 0
    else
        echo -e "${RED}✗ FAIL${NC} (Expected HTTP $expected_code, got $response)"
        return 1
    fi
}

# Test with JSON output
test_endpoint_json() {
    local name=$1
    local endpoint=$2
    local jq_filter=$3

    total_tests=$((total_tests + 1))

    echo -ne "${BLUE}Testing:${NC} $name ... "

    response=$(curl -s \
        -H "X-Tenant-ID: $TENANT_ID" \
        "${BASE_URL}${endpoint}" 2>/dev/null)

    if echo "$response" | jq -e "$jq_filter" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PASS${NC}"
        passed_tests=$((passed_tests + 1))
        return 0
    else
        echo -e "${RED}✗ FAIL${NC} (JSON validation failed)"
        echo "Response: $response"
        return 1
    fi
}

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}CQL Engine API Tests${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Wait for service to be ready
echo -e "${BLUE}Checking if CQL Engine is ready...${NC}"
max_wait=60
wait=0
while [ $wait -lt $max_wait ]; do
    if curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/health" | grep -q "200"; then
        echo -e "${GREEN}✓ CQL Engine is ready${NC}"
        echo ""
        break
    fi
    echo -ne "${YELLOW}⏳ Waiting for CQL Engine... (${wait}s/${max_wait}s)\r${NC}"
    sleep 2
    wait=$((wait + 2))
done

if [ $wait -ge $max_wait ]; then
    echo -e "${RED}✗ Timeout waiting for CQL Engine${NC}"
    exit 1
fi

echo -e "${BLUE}Actuator Endpoints:${NC}"
echo ""
test_endpoint "Health Check" "GET" "/actuator/health" "200"
test_endpoint "Health Liveness" "GET" "/actuator/health/liveness" "200"
test_endpoint "Health Readiness" "GET" "/actuator/health/readiness" "200"
test_endpoint "Metrics" "GET" "/actuator/metrics" "200"
test_endpoint "Prometheus Metrics" "GET" "/actuator/prometheus" "200"

echo ""
echo -e "${BLUE}API Endpoints:${NC}"
echo ""
test_endpoint "List All Measures" "GET" "/api/v1/measures" "200"
test_endpoint_json "Measures Response Valid" "/api/v1/measures" "type == \"array\""
test_endpoint_json "52 HEDIS Measures" "/api/v1/measures" "length == 52"

test_endpoint "Get BCS Measure" "GET" "/api/v1/measures/BCS" "200"
test_endpoint_json "BCS Measure Valid" "/api/v1/measures/BCS" ".measureId == \"BCS\""

test_endpoint "Get CDC Measure" "GET" "/api/v1/measures/CDC" "200"
test_endpoint "Get AMM Measure" "GET" "/api/v1/measures/AMM" "200"

# Test invalid measure
test_endpoint "Invalid Measure (404)" "GET" "/api/v1/measures/INVALID" "404"

echo ""
echo -e "${BLUE}Measure Evaluation Endpoints:${NC}"
echo ""

# Note: These will return 404 or error until we have patient data
test_endpoint "Evaluate BCS (No Data)" "GET" "/api/v1/measures/BCS/evaluate/patient-123" "404"
test_endpoint "Evaluate Multiple (No Data)" "GET" "/api/v1/measures/evaluate/patient-123?measureIds=BCS,CDC" "404"
test_endpoint "Dashboard (No Data)" "GET" "/api/v1/measures/dashboard/patient-123" "200"
test_endpoint "Care Gaps (No Data)" "GET" "/api/v1/measures/care-gaps/patient-123" "200"

echo ""
echo -e "${BLUE}Measure Categories:${NC}"
echo ""
test_endpoint "Diabetes Measures" "GET" "/api/v1/measures?category=DIABETES" "200"
test_endpoint "Cardiovascular Measures" "GET" "/api/v1/measures?category=CARDIOVASCULAR" "200"
test_endpoint "Preventive Measures" "GET" "/api/v1/measures?category=PREVENTIVE_CARE" "200"

echo ""
echo -e "${BLUE}Error Handling:${NC}"
echo ""
test_endpoint "Missing Tenant Header" "GET" "/api/v1/measures" "400" "-H 'X-Tenant-ID:'"
test_endpoint "Invalid Endpoint" "GET" "/api/v1/invalid" "404"

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Summary:${NC}"
echo -e "${BLUE}========================================${NC}"

pass_rate=$((passed_tests * 100 / total_tests))

if [ $passed_tests -eq $total_tests ]; then
    echo -e "${GREEN}✓ All tests passed! ($passed_tests/$total_tests - 100%)${NC}"
    exit 0
elif [ $pass_rate -ge 80 ]; then
    echo -e "${YELLOW}⚠ Most tests passed ($passed_tests/$total_tests - $pass_rate%)${NC}"
    echo -e "${YELLOW}Some tests failed - review output above${NC}"
    exit 0
else
    echo -e "${RED}✗ Many tests failed ($passed_tests/$total_tests - $pass_rate%)${NC}"
    echo -e "${RED}Please check service logs: make logs-cql${NC}"
    exit 1
fi
