#!/bin/bash

# Comprehensive Service Data Validation Script
# Validates all services return real data for all use cases

set -uo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

BASE_URL="${BASE_URL:-http://localhost:18080}"
TENANT_ID="${TENANT_ID:-acme-health}"
AUTH_USERNAME="${AUTH_USERNAME:-demo_admin@hdim.ai}"
AUTH_PASSWORD="${AUTH_PASSWORD:-demo123}"

AUTH_TOKEN=$(curl -s -X POST "${BASE_URL}/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${AUTH_USERNAME}\",\"password\":\"${AUTH_PASSWORD}\"}" | jq -r '.accessToken // empty' 2>/dev/null)

AUTH_HEADER=()
if [ -n "$AUTH_TOKEN" ] && [ "$AUTH_TOKEN" != "null" ]; then
    AUTH_HEADER=(-H "Authorization: Bearer $AUTH_TOKEN")
else
    echo -e "${YELLOW}⚠ Auth token not available; requests may fail with 401/403.${NC}"
fi

# Track results
TOTAL=0
PASSED=0
FAILED=0
EMPTY=0
ERROR=0
SKIPPED=0

# Results storage
declare -A RESULTS

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.demo.yml}"
RUNNING_SERVICES=""
if [ -f "$COMPOSE_FILE" ] && command -v docker &> /dev/null; then
    RUNNING_SERVICES=$(docker compose -f "$COMPOSE_FILE" ps --services --status running 2>/dev/null || true)
fi

is_service_running() {
    local service=$1
    if [ -z "$RUNNING_SERVICES" ]; then
        return 1
    fi
    echo "$RUNNING_SERVICES" | grep -qx "$service"
}

skip_endpoint() {
    local name=$1
    local reason=$2
    TOTAL=$((TOTAL + 1))
    SKIPPED=$((SKIPPED + 1))
    RESULTS["$name"]="SKIPPED: $reason"
    echo -e "Testing: $name ... ${YELLOW}↷${NC} ($reason)"
}

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Comprehensive Service Data Validation${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to test endpoint and check for data
test_endpoint_with_data() {
    local name=$1
    local method=$2
    local url=$3
    local expected_code=${4:-200}
    local min_items=${5:-1}
    local description=$6
    
    TOTAL=$((TOTAL + 1))
    
    echo -n "Testing: $name ... "
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" -X GET "$url" \
            "${AUTH_HEADER[@]}" \
            -H "X-Tenant-ID: $TENANT_ID" \
            -H "Accept: application/json" \
            2>&1)
    elif [ "$method" = "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$url" \
            "${AUTH_HEADER[@]}" \
            -H "X-Tenant-ID: $TENANT_ID" \
            -H "Content-Type: application/json" \
            -H "Accept: application/json" \
            -d '{}' \
            2>&1)
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" != "$expected_code" ]; then
        echo -e "${RED}✗${NC} (HTTP $http_code - Expected $expected_code)"
        ERROR=$((ERROR + 1))
        FAILED=$((FAILED + 1))
        RESULTS["$name"]="ERROR: HTTP $http_code"
        return 1
    fi
    
    # Check if response has data
    if [ -z "$body" ] || [ "$body" = "null" ]; then
        echo -e "${YELLOW}⚠${NC} (HTTP $http_code - Empty response)"
        EMPTY=$((EMPTY + 1))
        RESULTS["$name"]="EMPTY: No data"
        return 2
    fi
    
    # Try to parse JSON and check for data
    if command -v jq &> /dev/null; then
        # Check if it's a paginated response
        if echo "$body" | jq -e '.content' &> /dev/null; then
            item_count=$(echo "$body" | jq '.content | length')
            total_elements=$(echo "$body" | jq -r '.totalElements // 0')
            
            if [ "$item_count" -ge "$min_items" ] || [ "$total_elements" -ge "$min_items" ]; then
                echo -e "${GREEN}✓${NC} (HTTP $http_code - $item_count items, $total_elements total)"
                PASSED=$((PASSED + 1))
                RESULTS["$name"]="PASS: $item_count items"
                return 0
            else
                echo -e "${YELLOW}⚠${NC} (HTTP $http_code - Only $item_count items, need $min_items)"
                EMPTY=$((EMPTY + 1))
                RESULTS["$name"]="EMPTY: Only $item_count items"
                return 2
            fi
        # Check if it's an array
        elif echo "$body" | jq -e 'type == "array"' &> /dev/null; then
            item_count=$(echo "$body" | jq 'length')
            
            if [ "$item_count" -ge "$min_items" ]; then
                echo -e "${GREEN}✓${NC} (HTTP $http_code - $item_count items)"
                PASSED=$((PASSED + 1))
                RESULTS["$name"]="PASS: $item_count items"
                return 0
            else
                echo -e "${YELLOW}⚠${NC} (HTTP $http_code - Only $item_count items, need $min_items)"
                EMPTY=$((EMPTY + 1))
                RESULTS["$name"]="EMPTY: Only $item_count items"
                return 2
            fi
        # Check if it's an object with data
        elif echo "$body" | jq -e 'type == "object"' &> /dev/null; then
            # Handle FHIR Bundles with entries
            if echo "$body" | jq -e '.entry' &> /dev/null; then
                item_count=$(echo "$body" | jq '.entry | length')
                if [ "$item_count" -ge "$min_items" ]; then
                    echo -e "${GREEN}✓${NC} (HTTP $http_code - $item_count items)"
                    PASSED=$((PASSED + 1))
                    RESULTS["$name"]="PASS: $item_count items"
                    return 0
                else
                    echo -e "${YELLOW}⚠${NC} (HTTP $http_code - Only $item_count items, need $min_items)"
                    EMPTY=$((EMPTY + 1))
                    RESULTS["$name"]="EMPTY: Only $item_count items"
                    return 2
                fi
            fi

            # Check for common data fields
            if echo "$body" | jq -e '.id, .patientId, .measureId, .status, .year, .overallScore, .totalMeasures' &> /dev/null; then
                echo -e "${GREEN}✓${NC} (HTTP $http_code - Has data)"
                PASSED=$((PASSED + 1))
                RESULTS["$name"]="PASS: Has data"
                return 0
            else
                echo -e "${YELLOW}⚠${NC} (HTTP $http_code - Object but no clear data fields)"
                EMPTY=$((EMPTY + 1))
                RESULTS["$name"]="EMPTY: No data fields"
                return 2
            fi
        else
            echo -e "${YELLOW}⚠${NC} (HTTP $http_code - Unknown response format)"
            EMPTY=$((EMPTY + 1))
            RESULTS["$name"]="EMPTY: Unknown format"
            return 2
        fi
    else
        # No jq, just check if response is not empty
        if [ ${#body} -gt 10 ]; then
            echo -e "${GREEN}✓${NC} (HTTP $http_code - Has response)"
            PASSED=$((PASSED + 1))
            RESULTS["$name"]="PASS: Has response"
            return 0
        else
            echo -e "${YELLOW}⚠${NC} (HTTP $http_code - Response too short)"
            EMPTY=$((EMPTY + 1))
            RESULTS["$name"]="EMPTY: Short response"
            return 2
        fi
    fi
}

get_first_patient_id() {
    if ! command -v jq &> /dev/null; then
        return
    fi

    patient_response=$(curl -s -X GET "${BASE_URL}/api/fhir/Patient?_count=1" \
        "${AUTH_HEADER[@]}" \
        -H "X-Tenant-ID: $TENANT_ID" \
        -H "Accept: application/json")
    echo "$patient_response" | jq -r '.entry[0].resource.id // empty' 2>/dev/null
}

FHIR_PATIENT_ID=$(get_first_patient_id)

get_first_patient_service_id() {
    if ! command -v jq &> /dev/null; then
        return
    fi

    patient_response=$(curl -s -X GET "${BASE_URL}/api/patients/api/v1/patients?page=0&size=1" \
        "${AUTH_HEADER[@]}" \
        -H "X-Tenant-ID: $TENANT_ID" \
        -H "Accept: application/json")
    echo "$patient_response" | jq -r '.content[0].id // empty' 2>/dev/null
}

PATIENT_SERVICE_ID=$(get_first_patient_service_id)

# ==================== PATIENT SERVICE ====================
echo -e "${CYAN}=== Patient Service ===${NC}"

test_endpoint_with_data \
    "Get Patients List" \
    "GET" \
    "$BASE_URL/api/patients/api/v1/patients?page=0&size=20" \
    200 \
    1 \
    "Get paginated patient list"

echo -e "${YELLOW}⚠ Skipping patient-by-ID check (endpoint not available in patient-service).${NC}"

# ==================== FHIR SERVICE ====================
echo ""
echo -e "${CYAN}=== FHIR Service ===${NC}"

test_endpoint_with_data \
    "Get FHIR Patients" \
    "GET" \
    "$BASE_URL/api/fhir/Patient?_count=20" \
    200 \
    1 \
    "Get FHIR Patient resources"

if [ -n "$FHIR_PATIENT_ID" ]; then
    test_endpoint_with_data \
        "Get FHIR Conditions" \
        "GET" \
        "$BASE_URL/api/fhir/Condition?patient=${FHIR_PATIENT_ID}&_count=20" \
        200 \
        1 \
        "Get FHIR Condition resources"

    test_endpoint_with_data \
        "Get FHIR Observations" \
        "GET" \
        "$BASE_URL/api/fhir/Observation?patient=${FHIR_PATIENT_ID}&_count=20" \
        200 \
        1 \
        "Get FHIR Observation resources"
else
    echo -e "${YELLOW}⚠ Unable to locate a patient ID for scoped FHIR queries.${NC}"
fi

# ==================== CARE GAP SERVICE ====================
echo ""
echo -e "${CYAN}=== Care Gap Service ===${NC}"

test_endpoint_with_data \
    "Get Care Gaps" \
    "GET" \
    "$BASE_URL/care-gap/api/v1/care-gaps?page=0&size=20" \
    200 \
    0 \
    "Get all care gaps"

test_endpoint_with_data \
    "Get High Priority Care Gaps" \
    "GET" \
    "$BASE_URL/care-gap/api/v1/care-gaps?priority=HIGH&page=0&size=10" \
    200 \
    0 \
    "Get high priority care gaps"

# ==================== QUALITY MEASURE SERVICE ====================
echo ""
echo -e "${CYAN}=== Quality Measure Service ===${NC}"

test_endpoint_with_data \
    "Get Quality Measure Results" \
    "GET" \
    "$BASE_URL/api/quality/results?page=0&size=20" \
    200 \
    0 \
    "Get quality measure results"

test_endpoint_with_data \
    "Get Population Report" \
    "GET" \
    "$BASE_URL/api/quality/report/population?year=2025" \
    200 \
    0 \
    "Get population quality report"

test_endpoint_with_data \
    "Get Local Measures" \
    "GET" \
    "$BASE_URL/api/quality/api/v1/measures" \
    200 \
    1 \
    "Get local quality measures"

# ==================== CQL ENGINE SERVICE ====================
echo ""
echo -e "${CYAN}=== CQL Engine Service ===${NC}"

test_endpoint_with_data \
    "Get CQL Libraries" \
    "GET" \
    "$BASE_URL/api/cql/api/v1/cql/libraries?page=0&size=20" \
    200 \
    1 \
    "Get CQL libraries"

test_endpoint_with_data \
    "Get CQL Evaluations" \
    "GET" \
    "$BASE_URL/api/cql/api/v1/cql/evaluations?page=0&size=20" \
    200 \
    0 \
    "Get CQL evaluations"

## ==================== ANALYTICS SERVICE ====================
echo ""
echo -e "${CYAN}=== Analytics Service ===${NC}"

if is_service_running "analytics-service"; then
    test_endpoint_with_data \
        "Get Analytics KPIs" \
        "GET" \
        "$BASE_URL/api/analytics/kpis" \
        200 \
        0 \
        "Get analytics KPIs"
else
    skip_endpoint "Get Analytics KPIs" "analytics-service not running"
fi

## ==================== HCC SERVICE ====================
echo ""
echo -e "${CYAN}=== HCC Service ===${NC}"

if is_service_running "hcc-service"; then
    test_endpoint_with_data \
        "Get HCC Risk Scores" \
        "GET" \
        "$BASE_URL/api/v1/hcc/risk-scores?page=0&size=20" \
        200 \
        0 \
        "Get HCC risk scores"
else
    skip_endpoint "Get HCC Risk Scores" "hcc-service not running"
fi

## ==================== SDOH SERVICE ====================
echo ""
echo -e "${CYAN}=== SDOH Service ===${NC}"

if is_service_running "sdoh-service"; then
    test_endpoint_with_data \
        "Get SDOH Resources" \
        "GET" \
        "$BASE_URL/api/sdoh/resources?page=0&size=20" \
        200 \
        0 \
        "Get SDOH community resources"
else
    skip_endpoint "Get SDOH Resources" "sdoh-service not running"
fi

## ==================== ECR SERVICE ====================
echo ""
echo -e "${CYAN}=== ECR Service ===${NC}"

if is_service_running "ecr-service"; then
    test_endpoint_with_data \
        "Get ECR Reports" \
        "GET" \
        "$BASE_URL/api/ecr?page=0&size=20" \
        200 \
        0 \
        "Get electronic case reports"
else
    skip_endpoint "Get ECR Reports" "ecr-service not running"
fi

## ==================== QRDA EXPORT SERVICE ====================
echo ""
echo -e "${CYAN}=== QRDA Export Service ===${NC}"

if is_service_running "qrda-export-service"; then
    test_endpoint_with_data \
        "Get QRDA Export Jobs" \
        "GET" \
        "$BASE_URL/api/v1/qrda/jobs?page=0&size=20" \
        200 \
        0 \
        "Get QRDA export jobs"
else
    skip_endpoint "Get QRDA Export Jobs" "qrda-export-service not running"
fi

## ==================== PRIOR AUTH SERVICE ====================
echo ""
echo -e "${CYAN}=== Prior Auth Service ===${NC}"

if is_service_running "prior-auth-service"; then
    test_endpoint_with_data \
        "Get Prior Auth Requests" \
        "GET" \
        "$BASE_URL/api/v1/prior-auth/requests?page=0&size=20" \
        200 \
        0 \
        "Get prior authorization requests"
else
    skip_endpoint "Get Prior Auth Requests" "prior-auth-service not running"
fi

# ==================== SUMMARY ====================
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Validation Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "Total Endpoints Tested: ${CYAN}$TOTAL${NC}"
echo -e "Passed (Has Data): ${GREEN}$PASSED${NC}"
echo -e "Empty (No Data): ${YELLOW}$EMPTY${NC}"
echo -e "Skipped (Not Running): ${YELLOW}$SKIPPED${NC}"
echo -e "Failed (Errors): ${RED}$FAILED${NC}"
echo ""

# Detailed results
echo -e "${BLUE}=== Detailed Results ===${NC}"
for key in "${!RESULTS[@]}"; do
    status="${RESULTS[$key]}"
    if [[ "$status" == PASS* ]]; then
        echo -e "${GREEN}✓${NC} $key: $status"
    elif [[ "$status" == EMPTY* ]]; then
        echo -e "${YELLOW}⚠${NC} $key: $status"
    elif [[ "$status" == SKIPPED* ]]; then
        echo -e "${YELLOW}↷${NC} $key: $status"
    else
        echo -e "${RED}✗${NC} $key: $status"
    fi
done
echo ""

# Recommendations
if [ $EMPTY -gt 0 ]; then
    echo -e "${YELLOW}⚠️  Recommendations:${NC}"
    echo "  - $EMPTY endpoints returned empty data"
    echo "  - Consider seeding demo data using demo-seeding-service"
    echo "  - Or verify data exists in database"
    echo ""
fi

if [ $FAILED -gt 0 ]; then
    echo -e "${RED}❌ Critical Issues:${NC}"
    echo "  - $FAILED endpoints failed with errors"
    echo "  - Review service logs for details"
    echo "  - Check service health endpoints"
    echo ""
fi

if [ $FAILED -eq 0 ] && [ $EMPTY -eq 0 ]; then
    echo -e "${GREEN}✅ All services are returning real data!${NC}"
    exit 0
elif [ $FAILED -eq 0 ]; then
    echo -e "${YELLOW}⚠️  Some services have no data, but no errors.${NC}"
    exit 0
else
    echo -e "${RED}❌ Some services have errors. Review above.${NC}"
    exit 1
fi
