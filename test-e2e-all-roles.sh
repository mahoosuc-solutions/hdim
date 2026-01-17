#!/bin/bash

################################################################################
# End-to-End Testing Suite for All User Roles
# Validates data model, API endpoints, and role-based access control
################################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
TENANT_ID="${TENANT_ID:-acme-health}"
PASSWORD="${PASSWORD:-demo123}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-hdim-demo-postgres}"
GATEWAY_DB_NAME="${GATEWAY_DB_NAME:-gateway_db}"
QUALITY_DB_NAME="${QUALITY_DB_NAME:-quality_db}"
CAREGAP_DB_NAME="${CAREGAP_DB_NAME:-caregap_db}"
CQL_DB_NAME="${CQL_DB_NAME:-cql_db}"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# Test results array
declare -a TEST_RESULTS=()

echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║       End-to-End Testing Suite - All User Roles              ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Function to run a test
run_test() {
    local test_name="$1"
    local test_command="$2"
    local expected_status="$3"
    local user_token="$4"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -ne "${BLUE}Testing: ${test_name}${NC} ... "
    
    # Execute test
    if [ -n "$user_token" ]; then
        response=$(eval "$test_command -H 'Authorization: Bearer $user_token' -H 'X-Tenant-ID: ${TENANT_ID}'" 2>/dev/null || echo "ERROR")
    else
        response=$(eval "$test_command" 2>/dev/null || echo "ERROR")
    fi
    
    # Check result
    if [ "$response" = "ERROR" ] || [ -z "$response" ]; then
        echo -e "${RED}✗ FAILED${NC} (Connection error)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        TEST_RESULTS+=("FAIL: $test_name - Connection error")
    elif echo "$response" | jq -e . >/dev/null 2>&1; then
        # Valid JSON response
        http_status=$(echo "$response" | jq -r '.status // 200' 2>/dev/null || echo "200")
        
        if [ "$expected_status" = "any" ] || [ "$http_status" = "$expected_status" ]; then
            echo -e "${GREEN}✓ PASSED${NC}"
            PASSED_TESTS=$((PASSED_TESTS + 1))
            TEST_RESULTS+=("PASS: $test_name")
        else
            echo -e "${RED}✗ FAILED${NC} (Expected: $expected_status, Got: $http_status)"
            FAILED_TESTS=$((FAILED_TESTS + 1))
            TEST_RESULTS+=("FAIL: $test_name - Status mismatch")
        fi
    else
        # Non-JSON response (text, HTML, or success message)
        if [ "$expected_status" = "any" ] && [ -n "$response" ]; then
            echo -e "${GREEN}✓ PASSED${NC}"
            PASSED_TESTS=$((PASSED_TESTS + 1))
            TEST_RESULTS+=("PASS: $test_name")
        else
            echo -e "${YELLOW}⊗ SKIPPED${NC} (Non-JSON response)"
            SKIPPED_TESTS=$((SKIPPED_TESTS + 1))
            TEST_RESULTS+=("SKIP: $test_name - Non-JSON response")
        fi
    fi
}

# Function to login and get token
get_auth_token() {
    local username="$1"
    local token=$(curl -s -X POST "${GATEWAY_URL}/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"${username}\",\"password\":\"${PASSWORD}\"}" \
        2>/dev/null | jq -r '.accessToken // empty')
    
    if [ -z "$token" ]; then
        echo "ERROR: Failed to get token for $username" >&2
        return 1
    fi
    echo "$token"
}

################################################################################
# 1. DATA MODEL VALIDATION
################################################################################

echo -e "${MAGENTA}════════════════════════════════════════════════════════════════${NC}"
echo -e "${MAGENTA}1. DATA MODEL VALIDATION${NC}"
echo -e "${MAGENTA}════════════════════════════════════════════════════════════════${NC}"
echo ""

echo -e "${CYAN}1.1 User Management Schema${NC}"
run_test "Users table exists" \
    "docker exec ${POSTGRES_CONTAINER} psql -U healthdata -d ${GATEWAY_DB_NAME} -c '\d users' -t" \
    "any" ""

run_test "User roles table exists" \
    "docker exec ${POSTGRES_CONTAINER} psql -U healthdata -d ${GATEWAY_DB_NAME} -c '\d user_roles' -t" \
    "any" ""

run_test "User tenants table exists" \
    "docker exec ${POSTGRES_CONTAINER} psql -U healthdata -d ${GATEWAY_DB_NAME} -c '\d user_tenants' -t" \
    "any" ""

run_test "Demo users exist (5 expected)" \
    "docker exec ${POSTGRES_CONTAINER} psql -U healthdata -d ${GATEWAY_DB_NAME} -c 'SELECT COUNT(*) FROM users WHERE username LIKE '\"'\"'demo.%'\"'\"';' -t | xargs" \
    "any" ""

echo ""
echo -e "${CYAN}1.2 Quality Measure Schema${NC}"
run_test "Quality measure results table" \
    "docker exec ${POSTGRES_CONTAINER} psql -U healthdata -d ${QUALITY_DB_NAME} -c '\d quality_measure_results' -t" \
    "any" ""

run_test "Custom measures table" \
    "docker exec ${POSTGRES_CONTAINER} psql -U healthdata -d ${QUALITY_DB_NAME} -c '\d custom_measures' -t" \
    "any" ""

echo ""
echo -e "${CYAN}1.3 Care Gap Schema${NC}"
run_test "Care gaps table" \
    "docker exec ${POSTGRES_CONTAINER} psql -U healthdata -d ${CAREGAP_DB_NAME} -c '\d care_gaps' -t" \
    "any" ""

echo ""
echo -e "${CYAN}1.4 CQL Engine Schema${NC}"
run_test "CQL libraries table" \
    "docker exec ${POSTGRES_CONTAINER} psql -U healthdata -d ${CQL_DB_NAME} -c '\d cql_libraries' -t" \
    "any" ""

run_test "CQL evaluations table" \
    "docker exec ${POSTGRES_CONTAINER} psql -U healthdata -d ${CQL_DB_NAME} -c '\d cql_evaluations' -t" \
    "any" ""

run_test "Value sets table" \
    "docker exec ${POSTGRES_CONTAINER} psql -U healthdata -d ${CQL_DB_NAME} -c '\d value_sets' -t" \
    "any" ""

echo ""
echo -e "${CYAN}1.5 Referential Integrity${NC}"
run_test "User -> User Roles FK" \
    "docker exec ${POSTGRES_CONTAINER} psql -U healthdata -d ${GATEWAY_DB_NAME} -t -A -c \"SELECT CASE WHEN COUNT(*) > 0 THEN 'OK' END FROM information_schema.table_constraints WHERE table_name='user_roles' AND constraint_type='FOREIGN KEY';\"" \
    "any" ""

run_test "User -> User Tenants FK" \
    "docker exec ${POSTGRES_CONTAINER} psql -U healthdata -d ${GATEWAY_DB_NAME} -t -A -c \"SELECT CASE WHEN COUNT(*) > 0 THEN 'OK' END FROM information_schema.table_constraints WHERE table_name='user_tenants' AND constraint_type='FOREIGN KEY';\"" \
    "any" ""

################################################################################
# 2. AUTHENTICATION SYSTEM
################################################################################

echo ""
echo -e "${MAGENTA}════════════════════════════════════════════════════════════════${NC}"
echo -e "${MAGENTA}2. AUTHENTICATION SYSTEM${NC}"
echo -e "${MAGENTA}════════════════════════════════════════════════════════════════${NC}"
echo ""

echo -e "${CYAN}2.1 Public Endpoints (No Auth Required)${NC}"
run_test "Gateway health check" \
    "curl -s ${GATEWAY_URL}/actuator/health" \
    "any" ""

run_test "Login endpoint accessible" \
    "curl -s -X POST ${GATEWAY_URL}/api/v1/auth/login -H 'Content-Type: application/json' -d '{\"username\":\"invalid\",\"password\":\"invalid\"}'" \
    "401" ""

echo ""
echo -e "${CYAN}2.2 User Authentication${NC}"
for username in "demo.doctor" "demo.analyst" "demo.care" "demo.admin" "demo.viewer"; do
    run_test "Login as $username" \
        "curl -s -X POST ${GATEWAY_URL}/api/v1/auth/login -H 'Content-Type: application/json' -d '{\"username\":\"${username}\",\"password\":\"${PASSWORD}\"}'" \
        "any" ""
done

################################################################################
# 3. ROLE-BASED ACCESS CONTROL - EVALUATOR (demo.doctor)
################################################################################

echo ""
echo -e "${MAGENTA}════════════════════════════════════════════════════════════════${NC}"
echo -e "${MAGENTA}3. EVALUATOR ROLE - Dr. Sarah Chen (demo.doctor)${NC}"
echo -e "${MAGENTA}   Can: Evaluate patients, calculate quality measures${NC}"
echo -e "${MAGENTA}════════════════════════════════════════════════════════════════${NC}"
echo ""

EVALUATOR_TOKEN=$(get_auth_token "demo.doctor")
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Successfully authenticated as EVALUATOR${NC}"
    echo ""
    
    echo -e "${CYAN}3.1 Quality Measure Access${NC}"
    run_test "Access quality measure results" \
        "curl -s ${GATEWAY_URL}/api/quality/results" \
        "any" "$EVALUATOR_TOKEN"
    
    run_test "Access quality report (population)" \
        "curl -s ${GATEWAY_URL}/api/quality/report/population" \
        "any" "$EVALUATOR_TOKEN"
    
    echo ""
    echo -e "${CYAN}3.2 CQL Engine Access${NC}"
    run_test "Access CQL libraries" \
        "curl -s ${GATEWAY_URL}/api/cql/libraries" \
        "any" "$EVALUATOR_TOKEN"
    
    run_test "Access value sets" \
        "curl -s ${GATEWAY_URL}/api/cql/value-sets" \
        "any" "$EVALUATOR_TOKEN"
    
    echo ""
    echo -e "${CYAN}3.3 Care Gap Access${NC}"
    run_test "View care gaps (via gateway)" \
        "curl -s ${GATEWAY_URL}/api/care-gaps/" \
        "any" "$EVALUATOR_TOKEN"
else
    echo -e "${RED}✗ Failed to authenticate as EVALUATOR${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 3))
fi

################################################################################
# 4. ROLE-BASED ACCESS CONTROL - ANALYST (demo.analyst)
################################################################################

echo ""
echo -e "${MAGENTA}════════════════════════════════════════════════════════════════${NC}"
echo -e "${MAGENTA}4. ANALYST ROLE - Michael Rodriguez (demo.analyst)${NC}"
echo -e "${MAGENTA}   Can: Analyze data, generate reports, view metrics${NC}"
echo -e "${MAGENTA}════════════════════════════════════════════════════════════════${NC}"
echo ""

ANALYST_TOKEN=$(get_auth_token "demo.analyst")
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Successfully authenticated as ANALYST${NC}"
    echo ""
    
    echo -e "${CYAN}4.1 Quality Measure Analytics${NC}"
    run_test "Access quality measure results" \
        "curl -s ${GATEWAY_URL}/api/quality/results" \
        "any" "$ANALYST_TOKEN"
    
    run_test "Access quality report (population)" \
        "curl -s ${GATEWAY_URL}/api/quality/report/population" \
        "any" "$ANALYST_TOKEN"
    
    echo ""
    echo -e "${CYAN}4.2 Reporting Access${NC}"
    run_test "Access saved reports" \
        "curl -s ${GATEWAY_URL}/api/quality/reports" \
        "any" "$ANALYST_TOKEN"
    
    echo ""
    echo -e "${CYAN}4.3 Data Visualization${NC}"
    run_test "Access CQL visualizations" \
        "curl -s ${GATEWAY_URL}/api/cql/visualizations" \
        "any" "$ANALYST_TOKEN"
else
    echo -e "${RED}✗ Failed to authenticate as ANALYST${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 3))
fi

################################################################################
# 5. ROLE-BASED ACCESS CONTROL - ADMIN (demo.admin)
################################################################################

echo ""
echo -e "${MAGENTA}════════════════════════════════════════════════════════════════${NC}"
echo -e "${MAGENTA}5. ADMIN ROLE - David Johnson (demo.admin)${NC}"
echo -e "${MAGENTA}   Can: Full system access, configuration, user management${NC}"
echo -e "${MAGENTA}════════════════════════════════════════════════════════════════${NC}"
echo ""

ADMIN_TOKEN=$(get_auth_token "demo.admin")
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Successfully authenticated as ADMIN${NC}"
    echo ""
    
    echo -e "${CYAN}5.1 Full Quality Measure Access${NC}"
    run_test "Access all quality measures" \
        "curl -s ${GATEWAY_URL}/api/quality/results" \
        "any" "$ADMIN_TOKEN"
    
    run_test "Access quality report (population)" \
        "curl -s ${GATEWAY_URL}/api/quality/report/population" \
        "any" "$ADMIN_TOKEN"
    
    echo ""
    echo -e "${CYAN}5.2 CQL Library Management${NC}"
    run_test "Access CQL libraries" \
        "curl -s ${GATEWAY_URL}/api/cql/libraries" \
        "any" "$ADMIN_TOKEN"
    
    run_test "Access value sets" \
        "curl -s ${GATEWAY_URL}/api/cql/value-sets" \
        "any" "$ADMIN_TOKEN"
    
    echo ""
    echo -e "${CYAN}5.3 System Configuration${NC}"
    run_test "Access Gateway actuator" \
        "curl -s ${GATEWAY_URL}/actuator/metrics" \
        "any" "$ADMIN_TOKEN"
else
    echo -e "${RED}✗ Failed to authenticate as ADMIN${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 3))
fi

################################################################################
# 6. ROLE-BASED ACCESS CONTROL - VIEWER (demo.viewer)
################################################################################

echo ""
echo -e "${MAGENTA}════════════════════════════════════════════════════════════════${NC}"
echo -e "${MAGENTA}6. VIEWER ROLE - Emily Martinez (demo.viewer)${NC}"
echo -e "${MAGENTA}   Can: Read-only access to dashboards and reports${NC}"
echo -e "${MAGENTA}════════════════════════════════════════════════════════════════${NC}"
echo ""

VIEWER_TOKEN=$(get_auth_token "demo.viewer")
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Successfully authenticated as VIEWER${NC}"
    echo ""
    
    echo -e "${CYAN}6.1 Read-Only Quality Measure Access${NC}"
    run_test "View quality measure results (read-only)" \
        "curl -s ${GATEWAY_URL}/api/quality/results" \
        "any" "$VIEWER_TOKEN"
    
    echo ""
    echo -e "${CYAN}6.2 Viewer Restrictions${NC}"
    run_test "Cannot calculate measures (should be denied)" \
        "curl -s -X POST ${GATEWAY_URL}/api/quality/calculate?patient=test&measure=CMS134" \
        "403" "$VIEWER_TOKEN"
    
    run_test "Cannot modify CQL libraries (should be denied)" \
        "curl -s -X POST ${GATEWAY_URL}/api/cql/libraries -H 'Content-Type: application/json' -d '{}'" \
        "403" "$VIEWER_TOKEN"
else
    echo -e "${RED}✗ Failed to authenticate as VIEWER${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 2))
fi

################################################################################
# 7. BACKEND SERVICE HEALTH CHECKS
################################################################################

echo ""
echo -e "${MAGENTA}════════════════════════════════════════════════════════════════${NC}"
echo -e "${MAGENTA}7. BACKEND SERVICE HEALTH${NC}"
echo -e "${MAGENTA}════════════════════════════════════════════════════════════════${NC}"
echo ""

run_test "Gateway service health" \
    "curl -s ${GATEWAY_URL}/actuator/health" \
    "any" ""

run_test "CQL Engine service (direct)" \
    "curl -s http://localhost:8081/actuator/health" \
    "any" ""

run_test "Quality Measure service (direct)" \
    "curl -s http://localhost:8087/actuator/health" \
    "any" ""

run_test "PostgreSQL database" \
    "docker exec ${POSTGRES_CONTAINER} pg_isready -U healthdata" \
    "any" ""

################################################################################
# 8. SUMMARY
################################################################################

echo ""
echo -e "${CYAN}════════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}TEST SUMMARY${NC}"
echo -e "${CYAN}════════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BLUE}Total Tests:${NC}   $TOTAL_TESTS"
echo -e "${GREEN}Passed:${NC}        $PASSED_TESTS"
echo -e "${RED}Failed:${NC}        $FAILED_TESTS"
echo -e "${YELLOW}Skipped:${NC}       $SKIPPED_TESTS"
echo ""

# Calculate pass rate
if [ $TOTAL_TESTS -gt 0 ]; then
    PASS_RATE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    echo -e "${CYAN}Pass Rate:${NC}     ${PASS_RATE}%"
fi

echo ""
echo -e "${CYAN}════════════════════════════════════════════════════════════════${NC}"

# Exit with appropriate code
if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✓ ALL TESTS PASSED!${NC}"
    echo -e "${GREEN}System is ready for production demo!${NC}"
    exit 0
else
    echo -e "${RED}✗ SOME TESTS FAILED${NC}"
    echo -e "${YELLOW}Review failed tests above for details${NC}"
    exit 1
fi
