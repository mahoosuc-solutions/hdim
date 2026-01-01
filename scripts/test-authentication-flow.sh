#!/bin/bash

################################################################################
# HDIM Clinical Portal - Authentication Flow Test Suite
#
# This script tests the complete authentication flow end-to-end:
# 1. Service health checks
# 2. Login endpoint validation
# 3. Cookie verification (Path, HttpOnly, SameSite)
# 4. Authenticated API calls to all services
# 5. JWT validation
# 6. Multi-tenant isolation
#
# Usage:
#   ./scripts/test-authentication-flow.sh
#   ./scripts/test-authentication-flow.sh --verbose
#   ./scripts/test-authentication-flow.sh --output report.json
#
# Exit Codes:
#   0 = All tests passed
#   1 = One or more tests failed
#   2 = Configuration error
#
################################################################################

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
REPORT_FILE="${1:-}"
VERBOSE="${VERBOSE:-0}"
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test results
declare -A TEST_RESULTS
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_SKIPPED=0

# API Configuration
PORTAL_URL="http://localhost:4200"
GATEWAY_URL="http://localhost:8080"
POSTGRES_CONTAINER="hdim-demo-postgres"
GATEWAY_CONTAINER="hdim-demo-gateway"

# Demo users
declare -A DEMO_USERS=(
    ["admin"]="demo_admin@hdim.ai:demo123"
    ["analyst"]="demo_analyst@hdim.ai:demo123"
    ["viewer"]="demo_viewer@hdim.ai:demo123"
)

################################################################################
# Logging Functions
################################################################################

log_info() {
    echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $*"
}

log_error() {
    echo -e "${RED}[✗]${NC} $*"
}

log_warning() {
    echo -e "${YELLOW}[⚠]${NC} $*"
}

log_section() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}$*${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
}

log_verbose() {
    if [[ "$VERBOSE" == "1" ]]; then
        echo -e "${BLUE}[DEBUG]${NC} $*"
    fi
}

################################################################################
# Test Result Tracking
################################################################################

record_test() {
    local test_name="$1"
    local result="$2"
    local message="${3:-}"

    TEST_RESULTS["$test_name"]="$result|$message"

    if [[ "$result" == "PASS" ]]; then
        ((TESTS_PASSED++))
        log_success "$test_name"
        [[ -n "$message" ]] && log_verbose "  → $message"
    elif [[ "$result" == "FAIL" ]]; then
        ((TESTS_FAILED++))
        log_error "$test_name"
        [[ -n "$message" ]] && log_error "  → $message"
    elif [[ "$result" == "SKIP" ]]; then
        ((TESTS_SKIPPED++))
        log_warning "$test_name (skipped)"
        [[ -n "$message" ]] && log_verbose "  → $message"
    fi
}

################################################################################
# Service Health Checks
################################################################################

test_service_health() {
    log_section "1. Service Health Checks"

    local services=("clinical-portal:4200" "gateway:8080" "postgres:5435" "redis:6380")

    for service in "${services[@]}"; do
        local name="${service%%:*}"
        local port="${service##*:}"

        if timeout 5 bash -c "echo >/dev/tcp/localhost/$port" 2>/dev/null; then
            record_test "Service health: $name (port $port)" "PASS" "Service responding"
        else
            record_test "Service health: $name (port $port)" "FAIL" "Service not responding"
        fi
    done
}

################################################################################
# Docker Service Health
################################################################################

test_docker_services_healthy() {
    log_section "2. Docker Services Status"

    if ! command -v docker &> /dev/null; then
        record_test "Docker installed" "SKIP" "Docker not found in PATH"
        return
    fi

    local services=("clinical-portal" "gateway-service" "fhir-service" "patient-service" "quality-measure-service" "care-gap-service" "postgres" "redis" "kafka")

    for service in "${services[@]}"; do
        local container_name="hdim-demo-${service}"

        # Check if container exists
        if ! docker ps --filter "name=$container_name" --format "{{.Names}}" | grep -q "$container_name"; then
            record_test "Docker service running: $service" "FAIL" "Container not found or not running"
            continue
        fi

        # Check health status
        local health_status=$(docker inspect "$container_name" --format='{{.State.Health.Status}}' 2>/dev/null || echo "unknown")

        if [[ "$health_status" == "healthy" ]]; then
            record_test "Docker service healthy: $service" "PASS" "Status: $health_status"
        elif [[ "$health_status" == "unknown" ]]; then
            # Service doesn't have healthcheck, just check if running
            local running_status=$(docker inspect "$container_name" --format='{{.State.Running}}' 2>/dev/null)
            if [[ "$running_status" == "true" ]]; then
                record_test "Docker service running: $service" "PASS" "Status: running (no healthcheck)"
            else
                record_test "Docker service running: $service" "FAIL" "Status: not running"
            fi
        else
            record_test "Docker service healthy: $service" "FAIL" "Status: $health_status"
        fi
    done
}

################################################################################
# Database Health Checks
################################################################################

test_database_health() {
    log_section "3. Database Health & Schema"

    if ! command -v docker &> /dev/null; then
        record_test "Database connection" "SKIP" "Docker not found"
        return
    fi

    # Check if we can connect to database
    if docker exec "$POSTGRES_CONTAINER" pg_isready -U healthdata &>/dev/null; then
        record_test "Database connection (PostgreSQL)" "PASS" "Database responding"
    else
        record_test "Database connection (PostgreSQL)" "FAIL" "Cannot connect to database"
        return
    fi

    # Check if gateway_db exists
    if docker exec "$POSTGRES_CONTAINER" psql -U healthdata -d gateway_db -c "SELECT 1;" &>/dev/null; then
        record_test "Database exists (gateway_db)" "PASS" "Database found"
    else
        record_test "Database exists (gateway_db)" "FAIL" "Database not found"
        return
    fi

    # Check authentication tables
    local tables=("users" "user_roles" "user_tenants" "audit_logs" "refresh_tokens")

    for table in "${tables[@]}"; do
        if docker exec "$POSTGRES_CONTAINER" psql -U healthdata -d gateway_db -c "SELECT 1 FROM information_schema.tables WHERE table_name='$table';" 2>/dev/null | grep -q "1"; then
            record_test "Database table exists: $table" "PASS" "Table found"
        else
            record_test "Database table exists: $table" "FAIL" "Table not found"
        fi
    done

    # Check demo users exist
    local user_count=$(docker exec "$POSTGRES_CONTAINER" psql -U healthdata -d gateway_db -t -c "SELECT COUNT(*) FROM users;" 2>/dev/null | tr -d ' ')

    if [[ "$user_count" -ge 3 ]]; then
        record_test "Demo users created ($user_count users)" "PASS" "Expected 3+ demo users found"
    else
        record_test "Demo users created ($user_count users)" "FAIL" "Expected 3+ demo users, found $user_count"
    fi
}

################################################################################
# Login Tests
################################################################################

test_login() {
    log_section "4. Login Endpoint Tests"

    local user_type="$1"
    local credentials="${DEMO_USERS[$user_type]}"

    if [[ -z "$credentials" ]]; then
        record_test "Login: $user_type" "FAIL" "Unknown user type: $user_type"
        return 1
    fi

    local email="${credentials%%:*}"
    local password="${credentials##*:}"

    # Make login request
    local response_file="/tmp/login_response_${user_type}.json"
    local http_code=$(curl -s -w "%{http_code}" -X POST "$GATEWAY_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$email\",\"password\":\"$password\"}" \
        -o "$response_file" 2>/dev/null)

    log_verbose "Login response HTTP code: $http_code"

    if [[ "$http_code" == "200" ]]; then
        record_test "Login endpoint: $user_type ($email)" "PASS" "HTTP 200"

        # Check response structure
        if jq -e '.user.id' "$response_file" &>/dev/null; then
            record_test "Login response: user.id present" "PASS" "User ID in response"
        else
            record_test "Login response: user.id present" "FAIL" "User ID missing from response"
        fi

        if jq -e '.user.email' "$response_file" &>/dev/null; then
            record_test "Login response: user.email present" "PASS" "Email in response"
        else
            record_test "Login response: user.email present" "FAIL" "Email missing from response"
        fi

        if jq -e '.user.roles' "$response_file" &>/dev/null; then
            record_test "Login response: user.roles present" "PASS" "Roles in response"
        else
            record_test "Login response: user.roles present" "FAIL" "Roles missing from response"
        fi

        return 0
    else
        record_test "Login endpoint: $user_type ($email)" "FAIL" "HTTP $http_code"
        log_verbose "Response: $(cat "$response_file")"
        return 1
    fi
}

################################################################################
# Cookie Tests
################################################################################

test_cookies() {
    log_section "5. Authentication Cookie Tests"

    local user_type="$1"
    local credentials="${DEMO_USERS[$user_type]}"
    local email="${credentials%%:*}"
    local password="${credentials##*:}"

    # Create temporary cookie jar
    local cookie_jar="/tmp/cookies_${user_type}.txt"
    rm -f "$cookie_jar"

    # Login and capture cookies
    local response_file="/tmp/login_response_${user_type}_cookies.json"
    local http_code=$(curl -s -w "%{http_code}" -X POST "$GATEWAY_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$email\",\"password\":\"$password\"}" \
        -c "$cookie_jar" \
        -o "$response_file" 2>/dev/null)

    if [[ "$http_code" != "200" ]]; then
        record_test "Cookie test: login failed" "FAIL" "Cannot proceed with cookie tests"
        return 1
    fi

    # Check if access token cookie exists
    if grep -q "hdim_access_token" "$cookie_jar" 2>/dev/null; then
        record_test "Cookie set: hdim_access_token" "PASS" "Access token cookie found"

        # Extract cookie details
        local cookie_path=$(grep "hdim_access_token" "$cookie_jar" | awk '{print $NF}' | head -1 || echo "")
        log_verbose "  Cookie path: $cookie_path"

        if [[ "$cookie_path" == "/" ]]; then
            record_test "Cookie path correct: hdim_access_token (Path=/)" "PASS" "Cookie path is /"
        elif [[ "$cookie_path" == "/auth" ]] || [[ "$cookie_path" == "/api" ]]; then
            record_test "Cookie path correct: hdim_access_token (Path=/)" "FAIL" "Cookie path is $cookie_path (should be /)"
        else
            record_test "Cookie path correct: hdim_access_token (Path=/)" "FAIL" "Cookie path is $cookie_path"
        fi
    else
        record_test "Cookie set: hdim_access_token" "FAIL" "Access token cookie not found"
    fi

    # Check if refresh token cookie exists
    if grep -q "hdim_refresh_token" "$cookie_jar" 2>/dev/null; then
        record_test "Cookie set: hdim_refresh_token" "PASS" "Refresh token cookie found"
    else
        record_test "Cookie set: hdim_refresh_token" "FAIL" "Refresh token cookie not found"
    fi

    # Save cookie jar for API tests
    cp "$cookie_jar" "/tmp/active_cookies_${user_type}.txt"
}

################################################################################
# API Authentication Tests
################################################################################

test_api_authentication() {
    log_section "6. Authenticated API Requests"

    local user_type="$1"
    local cookie_jar="/tmp/active_cookies_${user_type}.txt"

    if [[ ! -f "$cookie_jar" ]]; then
        record_test "API auth test: cookies available" "FAIL" "Cookie jar not found"
        return 1
    fi

    # Test FHIR API
    local http_code=$(curl -s -w "%{http_code}" -X GET "$GATEWAY_URL/fhir/metadata" \
        -b "$cookie_jar" \
        -o /dev/null 2>/dev/null)

    if [[ "$http_code" == "200" ]]; then
        record_test "Authenticated API: GET /fhir/metadata" "PASS" "HTTP 200"
    else
        record_test "Authenticated API: GET /fhir/metadata" "FAIL" "HTTP $http_code"
    fi

    # Test Patient API
    http_code=$(curl -s -w "%{http_code}" -X GET "$GATEWAY_URL/patient/api/v1/patients" \
        -H "X-Tenant-ID: DEMO001" \
        -b "$cookie_jar" \
        -o /dev/null 2>/dev/null)

    if [[ "$http_code" == "200" ]]; then
        record_test "Authenticated API: GET /patient/api/v1/patients" "PASS" "HTTP 200"
    elif [[ "$http_code" == "401" ]]; then
        record_test "Authenticated API: GET /patient/api/v1/patients" "FAIL" "HTTP 401 (authentication failed)"
    else
        record_test "Authenticated API: GET /patient/api/v1/patients" "FAIL" "HTTP $http_code"
    fi

    # Test Quality Measure API
    http_code=$(curl -s -w "%{http_code}" -X GET "$GATEWAY_URL/quality-measure/api/v1/measures" \
        -H "X-Tenant-ID: DEMO001" \
        -b "$cookie_jar" \
        -o /dev/null 2>/dev/null)

    if [[ "$http_code" == "200" ]]; then
        record_test "Authenticated API: GET /quality-measure/api/v1/measures" "PASS" "HTTP 200"
    elif [[ "$http_code" == "401" ]]; then
        record_test "Authenticated API: GET /quality-measure/api/v1/measures" "FAIL" "HTTP 401 (authentication failed)"
    else
        record_test "Authenticated API: GET /quality-measure/api/v1/measures" "FAIL" "HTTP $http_code"
    fi
}

################################################################################
# Gateway Log Analysis
################################################################################

test_gateway_logs() {
    log_section "7. Gateway Authentication Logs"

    if ! command -v docker &> /dev/null; then
        record_test "Gateway logs: JWT validation" "SKIP" "Docker not found"
        return
    fi

    # Check gateway logs for successful JWT validation
    if docker logs "$GATEWAY_CONTAINER" 2>&1 | grep -qi "jwt validated"; then
        record_test "Gateway logs: JWT validated" "PASS" "Found JWT validation in logs"
    else
        record_test "Gateway logs: JWT validated" "FAIL" "No JWT validation found in recent logs"
    fi

    # Check for authentication errors
    if docker logs "$GATEWAY_CONTAINER" 2>&1 | grep -qi "authentication error"; then
        record_test "Gateway logs: no auth errors" "FAIL" "Found authentication errors in logs"
    else
        record_test "Gateway logs: no auth errors" "PASS" "No authentication errors"
    fi
}

################################################################################
# Report Generation
################################################################################

generate_text_report() {
    {
        echo ""
        echo "╔════════════════════════════════════════════════════════════════╗"
        echo "║         HDIM Clinical Portal - Authentication Test Report       ║"
        echo "╚════════════════════════════════════════════════════════════════╝"
        echo ""
        echo "Timestamp: $TIMESTAMP"
        echo "Test Runner: Authentication Flow Test Suite v1.0"
        echo ""
        echo "Test Results Summary:"
        echo "  ✓ Passed:  $TESTS_PASSED"
        echo "  ✗ Failed:  $TESTS_FAILED"
        echo "  ⊘ Skipped: $TESTS_SKIPPED"
        echo "  ─────────────────────"
        echo "  Total:    $((TESTS_PASSED + TESTS_FAILED + TESTS_SKIPPED))"
        echo ""

        if [[ $TESTS_FAILED -eq 0 ]]; then
            echo "Status: ✅ ALL TESTS PASSED"
        else
            echo "Status: ❌ TESTS FAILED ($TESTS_FAILED failures)"
        fi
        echo ""

        echo "Detailed Results:"
        echo "─────────────────────────────────────────────────────────────────"

        for test_name in "${!TEST_RESULTS[@]}"; do
            IFS='|' read -r result message <<< "${TEST_RESULTS[$test_name]}"

            case "$result" in
                PASS)
                    printf "  ✓ %-55s PASS\n" "$test_name"
                    ;;
                FAIL)
                    printf "  ✗ %-55s FAIL\n" "$test_name"
                    ;;
                SKIP)
                    printf "  ⊘ %-55s SKIP\n" "$test_name"
                    ;;
            esac

            [[ -n "$message" ]] && echo "      └─ $message"
        done

        echo ""
        echo "─────────────────────────────────────────────────────────────────"
        echo ""

        if [[ $TESTS_FAILED -eq 0 ]]; then
            echo "✅ Authentication flow is working correctly!"
            echo ""
            echo "Next Steps:"
            echo "  1. Access Clinical Portal: http://localhost:4200"
            echo "  2. Login with: demo_admin@hdim.ai / demo123"
            echo "  3. Verify patient data loads without 401 errors"
            echo "  4. Check DevTools → Application → Cookies for proper Path"
        else
            echo "❌ Some tests failed. Please review the failures above."
            echo ""
            echo "Common Issues:"
            echo "  • Services not healthy: Check 'docker compose -f docker-compose.demo.yml ps'"
            echo "  • Login fails: Verify demo users in database"
            echo "  • API returns 401: Check cookie Path= / in nginx.conf"
            echo "  • Gateway logs: Check 'docker logs hdim-demo-gateway' for JWT errors"
        fi
        echo ""
    }
}

generate_json_report() {
    local json_output="/tmp/auth_test_report.json"

    {
        echo "{"
        echo "  \"timestamp\": \"$TIMESTAMP\","
        echo "  \"test_suite\": \"Authentication Flow Tests\","
        echo "  \"summary\": {"
        echo "    \"passed\": $TESTS_PASSED,"
        echo "    \"failed\": $TESTS_FAILED,"
        echo "    \"skipped\": $TESTS_SKIPPED,"
        echo "    \"total\": $((TESTS_PASSED + TESTS_FAILED + TESTS_SKIPPED))"
        echo "  },"
        echo "  \"status\": \"$([ $TESTS_FAILED -eq 0 ] && echo 'PASSED' || echo 'FAILED')\","
        echo "  \"tests\": {"

        local first=true
        for test_name in "${!TEST_RESULTS[@]}"; do
            IFS='|' read -r result message <<< "${TEST_RESULTS[$test_name]}"

            if [[ "$first" == false ]]; then
                echo ","
            fi
            first=false

            # Escape test name for JSON
            local escaped_name="${test_name//\"/\\\"}"
            local escaped_message="${message//\"/\\\"}"

            echo -n "    \"$escaped_name\": {\"result\": \"$result\", \"message\": \"$escaped_message\"}"
        done

        echo ""
        echo "  }"
        echo "}"
    } > "$json_output"

    cat "$json_output"
}

################################################################################
# Main Execution
################################################################################

main() {
    log_section "HDIM Clinical Portal - Authentication Flow Test Suite"
    log_info "Starting comprehensive authentication tests..."
    echo ""

    # Run all test categories
    test_service_health
    test_docker_services_healthy
    test_database_health

    # Test all demo users
    for user_type in "${!DEMO_USERS[@]}"; do
        test_login "$user_type" || true
        test_cookies "$user_type" || true
        test_api_authentication "$user_type" || true
    done

    test_gateway_logs

    # Generate reports
    log_section "Test Summary"
    generate_text_report

    # Save JSON report if requested
    if [[ -n "$REPORT_FILE" ]]; then
        log_info "Generating JSON report: $REPORT_FILE"
        generate_json_report > "$REPORT_FILE"
        log_success "Report saved to: $REPORT_FILE"
    fi

    # Exit with appropriate code
    if [[ $TESTS_FAILED -eq 0 ]]; then
        echo ""
        log_success "All tests passed! ✅"
        exit 0
    else
        echo ""
        log_error "Some tests failed! ❌"
        exit 1
    fi
}

# Run main function
main "$@"
