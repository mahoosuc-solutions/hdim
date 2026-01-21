#!/bin/bash

# End-to-End Test Script for Batch Calculation Feature
# Tests complete functionality from backend API to frontend integration

set -e  # Exit on error

echo "=========================================="
echo "Batch Calculation Feature - E2E Test"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
TENANT_ID="${TENANT_ID:-acme-health}"
AUTH_USERNAME="${AUTH_USERNAME:-demo.admin}"
AUTH_PASSWORD="${AUTH_PASSWORD:-demo123}"
QUALITY_API_BASE="${QUALITY_API_BASE:-${GATEWAY_URL}/api/quality}"
FHIR_URL="${FHIR_URL:-http://localhost:8085/fhir}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:4200}"
ROOT_DIR="$(pwd)"

AUTH_TOKEN=$(curl -s -X POST "${GATEWAY_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${AUTH_USERNAME}\",\"password\":\"${AUTH_PASSWORD}\"}" | jq -r '.accessToken' 2>/dev/null)

AUTH_HEADER=()
if [ -n "$AUTH_TOKEN" ] && [ "$AUTH_TOKEN" != "null" ]; then
  AUTH_HEADER=(-H "Authorization: Bearer $AUTH_TOKEN")
fi

# Test counters
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function to run a test
run_test() {
  local test_name=$1
  local test_command=$2

  TESTS_RUN=$((TESTS_RUN + 1))
  echo -n "Test $TESTS_RUN: $test_name... "

  if eval "$test_command" > /dev/null 2>&1; then
    echo -e "${GREEN}PASSED${NC}"
    TESTS_PASSED=$((TESTS_PASSED + 1))
    return 0
  else
    echo -e "${RED}FAILED${NC}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
    return 1
  fi
}

# Helper function to make HTTP request and check status
http_check() {
  local url=$1
  local expected_status=$2
  local method=${3:-GET}
  local headers=$4

  if [ "$method" = "POST" ]; then
    response=$(eval "curl -s -o /dev/null -w \"%{http_code}\" -X POST $headers \"$url\"")
  else
    response=$(eval "curl -s -o /dev/null -w \"%{http_code}\" $headers \"$url\"")
  fi

  [ "$response" = "$expected_status" ]
}

echo "=========================================="
echo "PHASE 1: Backend API Tests"
echo "=========================================="
echo ""

# Test 1: Quality Measure service health check
run_test "Quality Measure service is running" \
  "http_check 'http://localhost:8087/quality-measure/actuator/health' '200'"

# Test 2: Get all jobs endpoint (should return empty array initially)
run_test "GET /population/jobs returns 200" \
  "http_check '${QUALITY_API_BASE}/population/jobs' '200' 'GET' '-H \"X-Tenant-ID: ${TENANT_ID}\" -H \"Authorization: Bearer ${AUTH_TOKEN}\"'"

# Test 3: Start batch calculation
echo ""
echo -e "${YELLOW}Starting batch calculation job...${NC}"
START_RESPONSE=$(curl -s -X POST \
  -H "X-Tenant-ID: ${TENANT_ID}" "${AUTH_HEADER[@]}" \
  "${QUALITY_API_BASE}/population/calculate?fhirServerUrl=${FHIR_URL}&createdBy=e2e-test")

JOB_ID=$(echo "$START_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('jobId', ''))" 2>/dev/null)

if [ -n "$JOB_ID" ]; then
  TESTS_RUN=$((TESTS_RUN + 1))
  echo -e "Test $TESTS_RUN: POST /population/calculate starts job... ${GREEN}PASSED${NC}"
  echo "   Job ID: $JOB_ID"
  TESTS_PASSED=$((TESTS_PASSED + 1))
else
  TESTS_RUN=$((TESTS_RUN + 1))
  echo -e "Test $TESTS_RUN: POST /population/calculate starts job... ${RED}FAILED${NC}"
  TESTS_FAILED=$((TESTS_FAILED + 1))
  echo "   Response: $START_RESPONSE"
fi

# Test 4: Get job status
if [ -n "$JOB_ID" ]; then
  sleep 2  # Give job time to process
  run_test "GET /population/jobs/{jobId} returns 200" \
    "http_check '${QUALITY_API_BASE}/population/jobs/$JOB_ID' '200' 'GET' '-H \"X-Tenant-ID: ${TENANT_ID}\" -H \"Authorization: Bearer ${AUTH_TOKEN}\"'"

  # Test 5: Verify job status contains expected fields
  JOB_STATUS=$(curl -s -H "X-Tenant-ID: ${TENANT_ID}" "${AUTH_HEADER[@]}" \
    "${QUALITY_API_BASE}/population/jobs/$JOB_ID")

  echo -n "Test $((TESTS_RUN + 1)): Job status contains required fields... "
  TESTS_RUN=$((TESTS_RUN + 1))

  if echo "$JOB_STATUS" | grep -q "jobId" && \
     echo "$JOB_STATUS" | grep -q "status" && \
     echo "$JOB_STATUS" | grep -q "totalPatients" && \
     echo "$JOB_STATUS" | grep -q "progressPercent"; then
    echo -e "${GREEN}PASSED${NC}"
    TESTS_PASSED=$((TESTS_PASSED + 1))

    # Extract and display job details
    echo "   Status: $(echo "$JOB_STATUS" | python3 -c "import sys, json; print(json.load(sys.stdin).get('status', 'unknown'))" 2>/dev/null)"
    echo "   Progress: $(echo "$JOB_STATUS" | python3 -c "import sys, json; print(json.load(sys.stdin).get('progressPercent', 0))" 2>/dev/null)%"
    echo "   Total Patients: $(echo "$JOB_STATUS" | python3 -c "import sys, json; print(json.load(sys.stdin).get('totalPatients', 0))" 2>/dev/null)"
  else
    echo -e "${RED}FAILED${NC}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
  fi

  # Test 6: Verify job appears in all jobs list
  run_test "Job appears in GET /population/jobs list" \
    "curl -s -H 'X-Tenant-ID: ${TENANT_ID}' -H 'Authorization: Bearer ${AUTH_TOKEN}' '${QUALITY_API_BASE}/population/jobs' | grep -q '$JOB_ID'"
fi

echo ""
echo "=========================================="
echo "PHASE 2: Frontend Build Tests"
echo "=========================================="
echo ""

# Test 7: Frontend build exists
run_test "Frontend build directory exists" \
  "test -d '${ROOT_DIR}/dist/apps/clinical-portal'"

# Test 8: Main JS bundle exists
run_test "Main JavaScript bundle exists" \
  "test -f ${ROOT_DIR}/dist/apps/clinical-portal/browser/main*.js"

# Test 9: Dashboard component compiled
run_test "Dashboard component chunk exists" \
  "ls ${ROOT_DIR}/dist/apps/clinical-portal/browser/*.js | xargs grep -l 'dashboard-component' | head -1"

echo ""
echo "=========================================="
echo "PHASE 3: Frontend Integration Tests"
echo "=========================================="
echo ""

# Test 10: Angular dev server is accessible
run_test "Angular dev server responds on port 4200" \
  "http_check '${FRONTEND_URL}' '200'"

# Test 11: API proxy configuration works
run_test "Proxied FHIR endpoint accessible via Angular" \
  "http_check '${FRONTEND_URL}/fhir/Patient?_count=1' '200'"

# Test 12: Proxied Quality Measure endpoint accessible
run_test "Proxied Quality Measure endpoint accessible" \
  "http_check '${FRONTEND_URL}/quality-measure/actuator/health' '200'"

# Test 13: Batch calculation endpoints proxied correctly
run_test "Proxied batch calculation endpoint accessible" \
  "http_check '${FRONTEND_URL}/quality-measure/population/jobs' '200' 'GET' '-H \"X-Tenant-ID: ${TENANT_ID}\" -H \"Authorization: Bearer ${AUTH_TOKEN}\"'"

echo ""
echo "=========================================="
echo "PHASE 4: Component Integration Tests"
echo "=========================================="
echo ""

# Test 14: BatchCalculationService exists
run_test "BatchCalculationService file exists" \
  "test -f '${ROOT_DIR}/apps/clinical-portal/src/app/services/batch-calculation.service.ts'"

# Test 15: BatchCalculationComponent exists
run_test "BatchCalculationComponent file exists" \
  "test -f '${ROOT_DIR}/apps/clinical-portal/src/app/shared/components/batch-calculation/batch-calculation.component.ts'"

# Test 16: Component integrated into dashboard
run_test "Dashboard imports BatchCalculationComponent" \
  "grep -q 'BatchCalculationComponent' '${ROOT_DIR}/apps/clinical-portal/src/app/pages/dashboard/dashboard.component.ts'"

# Test 17: Component rendered in dashboard HTML
run_test "Dashboard HTML includes batch calculation component" \
  "grep -q 'app-batch-calculation' '${ROOT_DIR}/apps/clinical-portal/src/app/pages/dashboard/dashboard.component.html'"

echo ""
echo "=========================================="
echo "PHASE 5: Configuration Tests"
echo "=========================================="
echo ""

# Test 18: API config has batch endpoints
run_test "API config includes POPULATION_CALCULATE endpoint" \
  "grep -q 'POPULATION_CALCULATE' '${ROOT_DIR}/apps/clinical-portal/src/app/config/api.config.ts'"

# Test 19: Proxy configuration exists
run_test "Proxy configuration file exists" \
  "test -f '${ROOT_DIR}/apps/clinical-portal/proxy.conf.json'"

# Test 20: Proxy configured for quality-measure
run_test "Proxy config includes quality-measure route" \
  "grep -q 'quality-measure' '${ROOT_DIR}/apps/clinical-portal/proxy.conf.json'"

echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo ""
echo "Total Tests Run: $TESTS_RUN"
echo -e "${GREEN}Tests Passed: $TESTS_PASSED${NC}"
if [ $TESTS_FAILED -gt 0 ]; then
  echo -e "${RED}Tests Failed: $TESTS_FAILED${NC}"
else
  echo "Tests Failed: 0"
fi
echo ""

# Calculate success rate
SUCCESS_RATE=$((TESTS_PASSED * 100 / TESTS_RUN))
echo "Success Rate: $SUCCESS_RATE%"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
  echo -e "${GREEN}=========================================="
  echo "ALL TESTS PASSED! ✅"
  echo "==========================================${NC}"
  echo ""
  echo "Batch Calculation Feature Status:"
  echo "  ✅ Backend API - Fully functional"
  echo "  ✅ Frontend Build - Successful"
  echo "  ✅ Frontend Integration - Complete"
  echo "  ✅ Component Integration - Verified"
  echo "  ✅ Configuration - Correct"
  echo ""
  echo "The batch calculation feature is ready for use!"
  echo ""
  echo "Access the feature at:"
  echo "  http://localhost:4200 (Dashboard)"
  echo ""
  exit 0
else
  echo -e "${RED}=========================================="
  echo "SOME TESTS FAILED"
  echo "==========================================${NC}"
  echo ""
  echo "Please review the failed tests above."
  exit 1
fi
