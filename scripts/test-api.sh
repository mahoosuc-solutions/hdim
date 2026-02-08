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
BASE_URL="http://localhost:8081/cql-engine"
TENANT_ID="tenant-1"

# Trusted gateway headers (dev mode)
AUTH_USERNAME="${AUTH_USERNAME:-api_test_user}"
AUTH_USER_ID="${AUTH_USER_ID:-6f52b4c2-7f41-4c58-a61e-b9e671ba63ce}"
AUTH_ROLES="${AUTH_ROLES:-ADMIN}"
AUTH_TENANT_IDS="${AUTH_TENANT_IDS:-$TENANT_ID}"
AUTH_VALIDATED="${AUTH_VALIDATED:-gateway-dev}"

# Test counter
total_tests=0
passed_tests=0

# Test function
test_endpoint() {
    local name=$1
    local method=$2
    local endpoint=$3
    local expected_code=$4
    shift 4
    local extra_args=("$@")

    total_tests=$((total_tests + 1))

    echo -ne "${BLUE}Testing:${NC} $name ... "

    local auth_headers=(
        -H "X-Tenant-ID: $TENANT_ID"
        -H "X-Auth-Validated: $AUTH_VALIDATED"
        -H "X-Auth-Username: $AUTH_USERNAME"
        -H "X-Auth-User-Id: $AUTH_USER_ID"
        -H "X-Auth-Tenant-Ids: $AUTH_TENANT_IDS"
        -H "X-Auth-Roles: $AUTH_ROLES"
    )

    if [ "$method" == "GET" ]; then
        response=$(curl -s -o /dev/null -w "%{http_code}" \
            "${auth_headers[@]}" \
            "${extra_args[@]}" \
            "${BASE_URL}${endpoint}" 2>/dev/null || echo "000")
    else
        response=$(curl -s -o /dev/null -w "%{http_code}" \
            -X "$method" \
            -H "Content-Type: application/json" \
            "${auth_headers[@]}" \
            "${extra_args[@]}" \
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
        -H "X-Auth-Validated: $AUTH_VALIDATED" \
        -H "X-Auth-Username: $AUTH_USERNAME" \
        -H "X-Auth-User-Id: $AUTH_USER_ID" \
        -H "X-Auth-Tenant-Ids: $AUTH_TENANT_IDS" \
        -H "X-Auth-Roles: $AUTH_ROLES" \
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
test_endpoint "List All Measures" "GET" "/evaluate/measures" "200"
test_endpoint_json "Measures Response Valid" "/evaluate/measures" ".measures | type == \"array\""
test_endpoint_json "56 HEDIS Measures" "/evaluate/measures" ".totalMeasures == 56"

test_endpoint "Get BCS Measure" "GET" "/evaluate/measures/BCS" "200"
test_endpoint_json "BCS Measure Valid" "/evaluate/measures/BCS" ".measureId == \"BCS\""

test_endpoint "Get CDC Measure" "GET" "/evaluate/measures/CDC" "200"
test_endpoint "Get AMM Measure" "GET" "/evaluate/measures/AMM" "200"

test_endpoint "Measure Exists" "GET" "/evaluate/measures/BCS/exists" "200"

# Test invalid measure
test_endpoint "Invalid Measure (404)" "GET" "/evaluate/measures/INVALID" "404"

echo ""
echo -e "${BLUE}Error Handling:${NC}"
echo ""
test_endpoint "Missing Tenant Header" "GET" "/evaluate/measures" "200" -H "X-Tenant-ID:"
test_endpoint "Invalid Endpoint" "GET" "/invalid" "403"

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
