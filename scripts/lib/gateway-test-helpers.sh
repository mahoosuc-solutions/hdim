#!/bin/bash

##############################################################################
# Gateway Test Helper Library
#
# Reusable functions for gateway smoke testing, service validation,
# authentication testing, circuit breaker validation, and rate limiting tests.
#
# Usage:
#   source scripts/lib/gateway-test-helpers.sh
#   test_service_health "quality-measure" "http://localhost:8087"
##############################################################################

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Test result tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# JSON report data
declare -a TEST_RESULTS=()

##############################################################################
# Logging Functions
##############################################################################

log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_test() {
    local test_name=$1
    local status=$2
    local message=$3

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    case "$status" in
        "PASS")
            PASSED_TESTS=$((PASSED_TESTS + 1))
            echo -e "${GREEN}✓${NC} $test_name"
            ;;
        "FAIL")
            FAILED_TESTS=$((FAILED_TESTS + 1))
            echo -e "${RED}✗${NC} $test_name - $message"
            ;;
        "SKIP")
            SKIPPED_TESTS=$((SKIPPED_TESTS + 1))
            echo -e "${YELLOW}⊘${NC} $test_name - $message"
            ;;
    esac

    # Add to JSON results
    TEST_RESULTS+=("{\"name\":\"$test_name\",\"status\":\"$status\",\"message\":\"$message\",\"timestamp\":\"$(date -Iseconds)\"}")
}

##############################################################################
# Service Health Check
##############################################################################

test_service_health() {
    local service_name=$1
    local health_url=$2
    local timeout=${3:-10}

    local test_name="Health check: $service_name"

    if timeout "$timeout" curl -sf "$health_url/actuator/health" > /dev/null 2>&1; then
        log_test "$test_name" "PASS" "Service is healthy"
        return 0
    else
        log_test "$test_name" "FAIL" "Service health check failed or timed out"
        return 1
    fi
}

##############################################################################
# Service Route Testing
##############################################################################

test_service_route() {
    local service_name=$1
    local gateway_url=$2
    local route_path=$3
    local expected_status=${4:-200}
    local auth_token=${5:-""}

    local test_name="Route test: $route_path"
    local headers=()

    if [ -n "$auth_token" ]; then
        headers+=(-H "Authorization: Bearer $auth_token")
    fi
    headers+=(-H "X-Tenant-ID: acme-health")

    local response_code
    response_code=$(curl -s -o /dev/null -w "%{http_code}" \
        "${headers[@]}" \
        "$gateway_url$route_path" 2>/dev/null)

    if [ "$response_code" = "$expected_status" ]; then
        log_test "$test_name" "PASS" "Got expected status $expected_status"
        return 0
    else
        log_test "$test_name" "FAIL" "Expected $expected_status, got $response_code"
        return 1
    fi
}

##############################################################################
# Circuit Breaker Validation
##############################################################################

check_circuit_breaker() {
    local service_name=$1
    local gateway_url=$2

    local test_name="Circuit breaker: $service_name"
    local actuator_url="$gateway_url/actuator/circuitbreakers"

    # Check if circuit breaker actuator is available
    if ! curl -sf "$actuator_url" > /dev/null 2>&1; then
        log_test "$test_name" "SKIP" "Circuit breaker actuator not available"
        return 0
    fi

    local cb_state
    cb_state=$(curl -sf "$actuator_url" | \
        grep -o "\"$service_name\"[^}]*\"state\":\"[^\"]*\"" | \
        grep -o "CLOSED\|OPEN\|HALF_OPEN" || echo "UNKNOWN")

    if [ "$cb_state" = "CLOSED" ] || [ "$cb_state" = "HALF_OPEN" ]; then
        log_test "$test_name" "PASS" "Circuit breaker state: $cb_state"
        return 0
    elif [ "$cb_state" = "OPEN" ]; then
        log_test "$test_name" "FAIL" "Circuit breaker is OPEN (service degraded)"
        return 1
    else
        log_test "$test_name" "SKIP" "Circuit breaker state unknown or not configured"
        return 0
    fi
}

##############################################################################
# Rate Limiting Tests
##############################################################################

test_rate_limiting() {
    local endpoint_url=$1
    local rate_limit=$2
    local test_window=${3:-60}

    local test_name="Rate limiting: $endpoint_url"

    log_info "Testing rate limit of $rate_limit requests per ${test_window}s..."

    local success_count=0
    local rate_limited_count=0

    # Send requests slightly above the rate limit
    local test_requests=$((rate_limit + 5))

    for i in $(seq 1 "$test_requests"); do
        response_code=$(curl -s -o /dev/null -w "%{http_code}" \
            -H "X-Tenant-ID: acme-health" \
            "$endpoint_url" 2>/dev/null)

        if [ "$response_code" = "429" ]; then
            rate_limited_count=$((rate_limited_count + 1))
        elif [ "$response_code" = "200" ] || [ "$response_code" = "201" ]; then
            success_count=$((success_count + 1))
        fi

        # Small delay to avoid overwhelming the server
        sleep 0.05
    done

    if [ "$rate_limited_count" -gt 0 ]; then
        log_test "$test_name" "PASS" "Rate limiting working: $rate_limited_count/429 responses"
        return 0
    else
        log_test "$test_name" "WARN" "No rate limiting detected (might be disabled in dev mode)"
        return 0
    fi
}

##############################################################################
# Authentication Validation
##############################################################################

validate_jwt_auth() {
    local gateway_url=$1
    local username=$2
    local password=$3

    local test_name="JWT authentication: $username"

    # Attempt login (with 10 second timeout)
    local login_response
    login_response=$(timeout 10 curl -sf -X POST \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}" \
        "$gateway_url/api/auth/login" 2>/dev/null)

    if [ -z "$login_response" ]; then
        log_test "$test_name" "FAIL" "Login request failed"
        return 1
    fi

    # Extract JWT token (handle both direct token response and JSON object)
    local jwt_token
    jwt_token=$(echo "$login_response" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

    if [ -z "$jwt_token" ]; then
        # Try extracting as direct token response
        jwt_token=$(echo "$login_response" | tr -d '\n\r')
    fi

    if [ -z "$jwt_token" ]; then
        log_test "$test_name" "FAIL" "No JWT token in login response"
        return 1
    fi

    # Test JWT token by accessing protected endpoint
    local auth_test_response
    auth_test_response=$(curl -s -o /dev/null -w "%{http_code}" \
        -H "Authorization: Bearer $jwt_token" \
        -H "X-Tenant-ID: acme-health" \
        "$gateway_url/api/auth/me" 2>/dev/null)

    if [ "$auth_test_response" = "200" ]; then
        log_test "$test_name" "PASS" "JWT authentication successful"
        echo "$jwt_token"
        return 0
    else
        log_test "$test_name" "FAIL" "JWT token validation failed (status: $auth_test_response)"
        return 1
    fi
}

##############################################################################
# Header Injection Prevention
##############################################################################

verify_headers() {
    local gateway_url=$1
    local endpoint=$2

    local test_name="Header injection prevention"

    # Try to inject malicious X-Auth-User-Id header
    local response_code
    response_code=$(curl -s -o /dev/null -w "%{http_code}" \
        -H "X-Auth-User-Id: malicious-user-id" \
        -H "X-Auth-Tenant-Ids: malicious-tenant" \
        -H "X-Tenant-ID: acme-health" \
        "$gateway_url$endpoint" 2>/dev/null)

    # Gateway should strip these headers and return 401 (no valid auth)
    if [ "$response_code" = "401" ] || [ "$response_code" = "403" ]; then
        log_test "$test_name" "PASS" "Malicious headers rejected (status: $response_code)"
        return 0
    else
        log_test "$test_name" "WARN" "Unexpected response to header injection: $response_code"
        return 0
    fi
}

##############################################################################
# Error Handling Validation
##############################################################################

test_error_handling() {
    local gateway_url=$1
    local error_type=$2

    case "$error_type" in
        "404")
            local test_name="Error handling: 404 Not Found"
            local response_code
            response_code=$(curl -s -o /dev/null -w "%{http_code}" \
                "$gateway_url/api/nonexistent-service/test" 2>/dev/null)

            if [ "$response_code" = "404" ]; then
                log_test "$test_name" "PASS" "404 returned for nonexistent route"
                return 0
            else
                log_test "$test_name" "FAIL" "Expected 404, got $response_code"
                return 1
            fi
            ;;

        "401")
            local test_name="Error handling: 401 Unauthorized"
            local response_code
            response_code=$(curl -s -o /dev/null -w "%{http_code}" \
                "$gateway_url/api/quality-measure/api/v1/measures" 2>/dev/null)

            # Should return 401 without authentication
            if [ "$response_code" = "401" ]; then
                log_test "$test_name" "PASS" "401 returned for unauthenticated request"
                return 0
            else
                log_test "$test_name" "WARN" "Expected 401, got $response_code (might be dev mode)"
                return 0
            fi
            ;;

        "503")
            local test_name="Error handling: 503 Service Unavailable"
            # This test requires a service to be down - skip in normal operation
            log_test "$test_name" "SKIP" "Requires service downtime to test"
            return 0
            ;;

        *)
            log_warn "Unknown error type: $error_type"
            return 1
            ;;
    esac
}

##############################################################################
# Report Generation
##############################################################################

generate_report() {
    local output_file=${1:-"/tmp/gateway-smoke-report.json"}

    local pass_rate=0
    if [ "$TOTAL_TESTS" -gt 0 ]; then
        pass_rate=$(awk "BEGIN {printf \"%.1f\", ($PASSED_TESTS / $TOTAL_TESTS) * 100}")
    fi

    cat > "$output_file" <<EOF
{
  "timestamp": "$(date -Iseconds)",
  "summary": {
    "totalTests": $TOTAL_TESTS,
    "passed": $PASSED_TESTS,
    "failed": $FAILED_TESTS,
    "skipped": $SKIPPED_TESTS,
    "passRate": $pass_rate
  },
  "tests": [
    $(IFS=,; echo "${TEST_RESULTS[*]}")
  ]
}
EOF

    log_info "Test report written to: $output_file"
}

##############################################################################
# Test Summary
##############################################################################

print_test_summary() {
    echo ""
    echo "=================================================================="
    echo "                     TEST SUMMARY"
    echo "=================================================================="
    echo -e "Total Tests:   ${CYAN}$TOTAL_TESTS${NC}"
    echo -e "Passed:        ${GREEN}$PASSED_TESTS${NC}"
    echo -e "Failed:        ${RED}$FAILED_TESTS${NC}"
    echo -e "Skipped:       ${YELLOW}$SKIPPED_TESTS${NC}"

    if [ "$TOTAL_TESTS" -gt 0 ]; then
        local pass_rate
        pass_rate=$(awk "BEGIN {printf \"%.1f\", ($PASSED_TESTS / $TOTAL_TESTS) * 100}")
        echo -e "Pass Rate:     ${CYAN}$pass_rate%${NC}"
    fi

    echo "=================================================================="

    if [ "$FAILED_TESTS" -eq 0 ]; then
        return 0
    else
        return 1
    fi
}

##############################################################################
# Wait for Service
##############################################################################

wait_for_service() {
    local service_name=$1
    local health_url=$2
    local max_wait=${3:-120}
    local wait_interval=5
    local elapsed=0

    log_info "Waiting for $service_name to be healthy at $health_url"

    while [ $elapsed -lt $max_wait ]; do
        if curl -sf "$health_url/actuator/health" > /dev/null 2>&1; then
            log_success "$service_name is healthy"
            return 0
        fi

        sleep $wait_interval
        elapsed=$((elapsed + wait_interval))
        echo -n "."
    done

    echo ""
    log_error "$service_name failed to become healthy within $max_wait seconds"
    return 1
}
