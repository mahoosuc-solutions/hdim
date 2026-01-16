#!/bin/bash

# Provider Dashboard API Validation Script
# Validates all API endpoints called by the provider dashboard

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

BASE_URL="${BASE_URL:-http://localhost:18080}"
TENANT_ID="${TENANT_ID:-acme-health}"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Provider Dashboard API Validation${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Track results
TOTAL=0
PASSED=0
FAILED=0
MISSING=0

# Function to test endpoint
test_endpoint() {
    local name=$1
    local method=$2
    local url=$3
    local expected_code=${4:-200}
    local description=$5
    
    TOTAL=$((TOTAL + 1))
    
    echo -n "Testing: $name ... "
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" -X GET "$url" \
            -H "X-Tenant-ID: $TENANT_ID" \
            -H "Accept: application/json" \
            2>&1)
    elif [ "$method" = "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$url" \
            -H "X-Tenant-ID: $TENANT_ID" \
            -H "Content-Type: application/json" \
            -H "Accept: application/json" \
            -d '{}' \
            2>&1)
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "$expected_code" ]; then
        echo -e "${GREEN}✓${NC} (HTTP $http_code)"
        PASSED=$((PASSED + 1))
        return 0
    elif [ "$http_code" = "404" ]; then
        echo -e "${RED}✗${NC} (HTTP $http_code - Endpoint not found)"
        FAILED=$((FAILED + 1))
        MISSING=$((MISSING + 1))
        return 1
    elif [ "$http_code" = "500" ]; then
        echo -e "${RED}✗${NC} (HTTP $http_code - Server error)"
        echo -e "  ${YELLOW}Error:${NC} $(echo "$body" | jq -r '.message // .error // "Unknown error"' 2>/dev/null || echo "$body" | head -1)"
        FAILED=$((FAILED + 1))
        return 1
    else
        echo -e "${YELLOW}⚠${NC} (HTTP $http_code - Expected $expected_code)"
        FAILED=$((FAILED + 1))
        return 1
    fi
}

echo -e "${CYAN}Testing Provider Dashboard API Endpoints...${NC}"
echo ""

# 1. Care Gap Endpoints
echo -e "${BLUE}--- Care Gap Endpoints ---${NC}"

# High Priority Care Gaps (called by loadHighPriorityCareGaps)
test_endpoint \
    "High Priority Care Gaps (via care-gap-service)" \
    "GET" \
    "$BASE_URL/care-gap/api/v1/care-gaps?priority=HIGH&page=0&size=10" \
    200 \
    "Get high priority care gaps"

test_endpoint \
    "High Priority Care Gaps (alternative endpoint)" \
    "GET" \
    "$BASE_URL/care-gap/high-priority?limit=10" \
    200 \
    "Alternative high priority endpoint"

# Care Gaps by Patient (called when loading patient details)
test_endpoint \
    "Care Gaps by Patient" \
    "GET" \
    "$BASE_URL/care-gap/api/v1/care-gaps?patientId=00000000-0000-0000-0000-000000000001&page=0&size=10" \
    200 \
    "Get care gaps for specific patient"

# 2. Patient Service Endpoints
echo ""
echo -e "${BLUE}--- Patient Service Endpoints ---${NC}"

# Get Patients (called by loadMetrics, loadRiskStratifiedPatients)
test_endpoint \
    "Get Patients List" \
    "GET" \
    "$BASE_URL/patient/api/v1/patients?page=0&size=10" \
    200 \
    "Get paginated patient list"

# Get Single Patient (called when loading patient names)
test_endpoint \
    "Get Patient by ID" \
    "GET" \
    "$BASE_URL/patient/api/v1/patients/00000000-0000-0000-0000-000000000001" \
    200 \
    "Get single patient details"

# 3. Quality Measure Service Endpoints
echo ""
echo -e "${BLUE}--- Quality Measure Service Endpoints ---${NC}"

# Population Report (called by loadQualityMeasures)
test_endpoint \
    "Population Quality Report" \
    "GET" \
    "$BASE_URL/quality-measure/api/v1/report/population?year=2025" \
    200 \
    "Get population quality report"

# Quality Measure Results (called by loadPendingResults)
test_endpoint \
    "Quality Measure Results" \
    "GET" \
    "$BASE_URL/quality-measure/api/v1/results?page=0&size=10" \
    200 \
    "Get quality measure results"

# Quality Score (used in metrics)
test_endpoint \
    "Quality Score" \
    "GET" \
    "$BASE_URL/quality-measure/api/v1/score?patient=00000000-0000-0000-0000-000000000001" \
    200 \
    "Get quality score for patient"

# 4. CQL Engine Endpoints
echo ""
echo -e "${BLUE}--- CQL Engine Endpoints ---${NC}"

# Get All Evaluations (called by getEvaluationStats)
test_endpoint \
    "Get All Evaluations" \
    "GET" \
    "$BASE_URL/cql-engine/api/v1/cql/evaluations?page=0&size=100" \
    200 \
    "Get all CQL evaluations"

# 5. Provider-Specific Endpoints
echo ""
echo -e "${BLUE}--- Provider-Specific Endpoints ---${NC}"

# Provider Performance (if implemented)
test_endpoint \
    "Provider Performance Metrics" \
    "GET" \
    "$BASE_URL/quality-measure/api/v1/providers/me/performance?period=YTD" \
    200 \
    "Get provider performance metrics"

# Provider Panel (if implemented)
test_endpoint \
    "Provider Panel" \
    "GET" \
    "$BASE_URL/patient/api/v1/providers/00000000-0000-0000-0000-000000000001/panel?page=0&size=50" \
    200 \
    "Get provider patient panel"

# Summary
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Validation Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "Total Endpoints Tested: ${CYAN}$TOTAL${NC}"
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"
if [ $MISSING -gt 0 ]; then
    echo -e "Missing Endpoints: ${RED}$MISSING${NC}"
fi
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ All provider dashboard APIs are working!${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠️  Some endpoints are failing. Review errors above.${NC}"
    exit 1
fi
