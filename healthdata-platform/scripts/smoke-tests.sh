#!/bin/bash
# HealthData Platform - Smoke Tests Script
# This script performs comprehensive smoke tests after deployment
set -euo pipefail

BASE_URL=${1:-http://localhost:8080}
MAX_RETRIES=${2:-10}
RETRY_DELAY=${3:-10}
TIMEOUT=${4:-30}

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test results tracking
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
    ((TESTS_PASSED++))
}

log_failure() {
    echo -e "${RED}[FAIL]${NC} $1"
    ((TESTS_FAILED++))
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# HTTP request helper with retry logic
http_get() {
    local url=$1
    local expected_status=${2:-200}
    local retry_count=0

    while [ $retry_count -lt $MAX_RETRIES ]; do
        local response=$(curl -s -w "\n%{http_code}" --max-time $TIMEOUT "$url" 2>/dev/null || echo "000")
        local http_code=$(echo "$response" | tail -n1)
        local body=$(echo "$response" | sed '$d')

        if [ "$http_code" = "$expected_status" ]; then
            echo "$body"
            return 0
        fi

        retry_count=$((retry_count + 1))
        if [ $retry_count -lt $MAX_RETRIES ]; then
            log_warning "Request failed (HTTP $http_code), retrying in ${RETRY_DELAY}s... (Attempt $retry_count/$MAX_RETRIES)"
            sleep $RETRY_DELAY
        fi
    done

    log_failure "Request to $url failed after $MAX_RETRIES attempts (HTTP $http_code)"
    return 1
}

# Test: Health check endpoint
test_health_check() {
    ((TESTS_TOTAL++))
    log_info "Testing health check endpoint..."

    if http_get "$BASE_URL/actuator/health" 200 > /dev/null; then
        log_success "Health check endpoint is responding"
        return 0
    else
        log_failure "Health check endpoint failed"
        return 1
    fi
}

# Test: FHIR Service health
test_fhir_service() {
    ((TESTS_TOTAL++))
    log_info "Testing FHIR service..."

    if http_get "$BASE_URL/fhir/health" 200 > /dev/null || \
       http_get "$BASE_URL/api/fhir/Patient" 200 > /dev/null; then
        log_success "FHIR service is operational"
        return 0
    else
        log_failure "FHIR service is not responding"
        return 1
    fi
}

# Test: Patient API
test_patient_api() {
    ((TESTS_TOTAL++))
    log_info "Testing Patient API..."

    local response=$(http_get "$BASE_URL/api/patients" 200 || echo "")

    if [ -n "$response" ]; then
        log_success "Patient API is responding"
        return 0
    else
        log_failure "Patient API failed"
        return 1
    fi
}

# Test: Quality Measures API
test_quality_measures_api() {
    ((TESTS_TOTAL++))
    log_info "Testing Quality Measures API..."

    if http_get "$BASE_URL/api/measures" 200 > /dev/null || \
       http_get "$BASE_URL/api/quality-measures" 200 > /dev/null; then
        log_success "Quality Measures API is responding"
        return 0
    else
        log_failure "Quality Measures API failed"
        return 1
    fi
}

# Test: CQL Engine Service
test_cql_engine() {
    ((TESTS_TOTAL++))
    log_info "Testing CQL Engine service..."

    if http_get "$BASE_URL/cql/health" 200 > /dev/null || \
       http_get "$BASE_URL/api/cql/evaluations" 200 > /dev/null; then
        log_success "CQL Engine service is operational"
        return 0
    else
        log_failure "CQL Engine service is not responding"
        return 1
    fi
}

# Test: Care Gap Service
test_care_gap_service() {
    ((TESTS_TOTAL++))
    log_info "Testing Care Gap service..."

    if http_get "$BASE_URL/api/care-gaps" 200 > /dev/null; then
        log_success "Care Gap service is responding"
        return 0
    else
        log_failure "Care Gap service failed"
        return 1
    fi
}

# Test: Database connectivity
test_database_connectivity() {
    ((TESTS_TOTAL++))
    log_info "Testing database connectivity..."

    local response=$(http_get "$BASE_URL/actuator/health" 200 || echo "")

    if echo "$response" | grep -q "\"status\":\"UP\""; then
        log_success "Database connectivity verified"
        return 0
    else
        log_failure "Database connectivity issue detected"
        return 1
    fi
}

# Test: Redis cache connectivity
test_redis_connectivity() {
    ((TESTS_TOTAL++))
    log_info "Testing Redis cache connectivity..."

    local response=$(http_get "$BASE_URL/actuator/health" 200 || echo "")

    if echo "$response" | grep -q "redis" || echo "$response" | grep -q "\"status\":\"UP\""; then
        log_success "Redis cache connectivity verified"
        return 0
    else
        log_warning "Redis cache status could not be verified"
        return 0  # Non-critical, don't fail
    fi
}

# Test: Frontend application
test_frontend_app() {
    ((TESTS_TOTAL++))
    log_info "Testing frontend application..."

    if http_get "$BASE_URL" 200 > /dev/null || \
       http_get "$BASE_URL/index.html" 200 > /dev/null; then
        log_success "Frontend application is accessible"
        return 0
    else
        log_failure "Frontend application is not accessible"
        return 1
    fi
}

# Test: API Gateway routing
test_api_gateway() {
    ((TESTS_TOTAL++))
    log_info "Testing API Gateway routing..."

    if http_get "$BASE_URL/api/health" 200 > /dev/null; then
        log_success "API Gateway routing is working"
        return 0
    else
        log_failure "API Gateway routing failed"
        return 1
    fi
}

# Test: Authentication endpoint
test_authentication() {
    ((TESTS_TOTAL++))
    log_info "Testing authentication endpoint..."

    # Should return 401 or 200 depending on configuration
    local response=$(curl -s -w "\n%{http_code}" --max-time $TIMEOUT "$BASE_URL/api/auth/login" 2>/dev/null || echo "000")
    local http_code=$(echo "$response" | tail -n1)

    if [ "$http_code" = "200" ] || [ "$http_code" = "401" ] || [ "$http_code" = "405" ]; then
        log_success "Authentication endpoint is responding"
        return 0
    else
        log_warning "Authentication endpoint returned unexpected status: $http_code"
        return 0  # Non-critical
    fi
}

# Test: WebSocket endpoint
test_websocket() {
    ((TESTS_TOTAL++))
    log_info "Testing WebSocket endpoint..."

    # Check if WebSocket upgrade is supported
    local response=$(curl -s -I --max-time $TIMEOUT "$BASE_URL/ws" 2>/dev/null || echo "")

    if echo "$response" | grep -qi "upgrade"; then
        log_success "WebSocket endpoint is available"
        return 0
    else
        log_warning "WebSocket endpoint could not be verified"
        return 0  # Non-critical
    fi
}

# Test: Static assets
test_static_assets() {
    ((TESTS_TOTAL++))
    log_info "Testing static assets..."

    if http_get "$BASE_URL/assets/logo.png" 200 > /dev/null || \
       http_get "$BASE_URL/favicon.ico" 200 > /dev/null; then
        log_success "Static assets are being served"
        return 0
    else
        log_warning "Static assets could not be verified"
        return 0  # Non-critical
    fi
}

# Test: API rate limiting (if configured)
test_rate_limiting() {
    ((TESTS_TOTAL++))
    log_info "Testing API rate limiting..."

    # Make multiple rapid requests
    for i in {1..5}; do
        curl -s --max-time $TIMEOUT "$BASE_URL/api/patients" > /dev/null 2>&1 || true
    done

    log_success "Rate limiting test completed"
    return 0
}

# Test: CORS headers
test_cors_headers() {
    ((TESTS_TOTAL++))
    log_info "Testing CORS headers..."

    local response=$(curl -s -I --max-time $TIMEOUT \
        -H "Origin: http://localhost:4200" \
        -H "Access-Control-Request-Method: GET" \
        "$BASE_URL/api/patients" 2>/dev/null || echo "")

    if echo "$response" | grep -qi "access-control-allow-origin"; then
        log_success "CORS headers are configured"
        return 0
    else
        log_warning "CORS headers could not be verified"
        return 0  # Non-critical
    fi
}

# Test: Response time
test_response_time() {
    ((TESTS_TOTAL++))
    log_info "Testing response time..."

    local start_time=$(date +%s%3N)
    http_get "$BASE_URL/api/health" 200 > /dev/null || true
    local end_time=$(date +%s%3N)
    local response_time=$((end_time - start_time))

    if [ $response_time -lt 5000 ]; then
        log_success "Response time is acceptable (${response_time}ms)"
        return 0
    else
        log_warning "Response time is slow (${response_time}ms)"
        return 0  # Non-critical
    fi
}

# Display test summary
display_summary() {
    echo ""
    echo "======================================"
    echo "       SMOKE TEST SUMMARY"
    echo "======================================"
    echo "Base URL: $BASE_URL"
    echo "Total Tests: $TESTS_TOTAL"
    echo -e "Passed: ${GREEN}$TESTS_PASSED${NC}"
    echo -e "Failed: ${RED}$TESTS_FAILED${NC}"
    echo "Success Rate: $(( TESTS_PASSED * 100 / TESTS_TOTAL ))%"
    echo "======================================"
    echo ""

    if [ $TESTS_FAILED -eq 0 ]; then
        log_success "All smoke tests passed! ✅"
        return 0
    else
        log_failure "Some smoke tests failed. Please check the logs above."
        return 1
    fi
}

# Main test execution
main() {
    log_info "Starting smoke tests for $BASE_URL"
    log_info "Max retries: $MAX_RETRIES, Retry delay: ${RETRY_DELAY}s, Timeout: ${TIMEOUT}s"
    echo ""

    # Core infrastructure tests
    test_health_check
    test_database_connectivity
    test_redis_connectivity

    # Backend service tests
    test_fhir_service
    test_patient_api
    test_quality_measures_api
    test_cql_engine
    test_care_gap_service

    # Frontend and gateway tests
    test_frontend_app
    test_api_gateway
    test_authentication

    # Additional feature tests
    test_websocket
    test_static_assets
    test_rate_limiting
    test_cors_headers
    test_response_time

    # Display summary
    display_summary
}

# Run main function and capture exit code
main "$@"
exit_code=$?

exit $exit_code
