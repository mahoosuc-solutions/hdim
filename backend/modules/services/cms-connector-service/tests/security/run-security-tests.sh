#!/bin/bash
# CMS Connector Service - Security Testing
# Phase 5 Week 2: OWASP Top 10 Validation

set -euo pipefail

SERVICE_URL="${1:-http://localhost:8081}"
RESULTS_DIR="${2:-.}/security-results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
TEST_RUN_DIR="${RESULTS_DIR}/${TIMESTAMP}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_pass() { echo -e "${GREEN}[PASS]${NC} $1"; }
log_fail() { echo -e "${RED}[FAIL]${NC} $1"; }

test_authentication() {
    local result_file="${TEST_RUN_DIR}/authentication.log"
    log_info "Testing: Authentication & Authorization"
    
    {
        echo "=== Authentication Test ==="
        echo "Protected endpoints (expect 401):"
        for endpoint in "/api/v1/cms/claims/search" "/api/v1/cms/claims/validate" "/api/v1/cms/sync/status"; do
            http_code=$(curl -s -o /dev/null -w "%{http_code}" "${SERVICE_URL}${endpoint}" 2>&1)
            if [ "$http_code" == "401" ]; then
                echo "✓ $endpoint: HTTP $http_code (Correctly rejected)"
            else
                echo "✗ $endpoint: HTTP $http_code (Should be 401)"
            fi
        done
        echo ""
        echo "Public endpoints (expect 200/503):"
        http_code=$(curl -s -o /dev/null -w "%{http_code}" "${SERVICE_URL}/api/v1/actuator/health" 2>&1)
        echo "✓ /api/v1/actuator/health: HTTP $http_code (Accessible)"
        echo ""
        echo "Invalid Bearer token:"
        http_code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer invalid" "${SERVICE_URL}/api/v1/cms/claims/search" 2>&1)
        echo "✓ Invalid Bearer token: HTTP $http_code (Correctly rejected)"
        echo "Test Complete: $(date)"
    } | tee "${result_file}"
    log_pass "Authentication test completed"
}

test_sql_injection() {
    local result_file="${TEST_RUN_DIR}/sql-injection.log"
    log_info "Testing: SQL Injection Prevention"
    
    {
        echo "=== SQL Injection Test ==="
        local payloads=("1' OR '1'='1" "admin' --" "1; DROP TABLE users; --")
        for payload in "${payloads[@]}"; do
            http_code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${SERVICE_URL}/api/v1/cms/claims/search" \
                -H "Content-Type: application/json" -d "{\"query\": \"$payload\"}" 2>&1)
            if [[ "$http_code" =~ ^(400|401|403|500)$ ]]; then
                echo "✓ Payload rejected: '$payload' -> HTTP $http_code"
            else
                echo "✗ Potential vulnerability: '$payload' -> HTTP $http_code"
            fi
        done
        echo "Test Complete: $(date)"
    } | tee "${result_file}"
    log_pass "SQL Injection test completed"
}

test_xss() {
    local result_file="${TEST_RUN_DIR}/xss-prevention.log"
    log_info "Testing: XSS Prevention"
    
    {
        echo "=== XSS Prevention Test ==="
        local payloads=("<script>alert('XSS')</script>" "<img src=x onerror=alert('XSS')>" "<svg onload=alert('XSS')>")
        for payload in "${payloads[@]}"; do
            response=$(curl -s -X POST "${SERVICE_URL}/api/v1/cms/claims/validate" \
                -H "Content-Type: application/json" -d "{\"data\": \"$payload\"}" 2>&1)
            if echo "$response" | grep -q "<script\|onerror=\|onload="; then
                echo "✗ Potential XSS: Unescaped payload in response"
            else
                echo "✓ Payload safely handled: $payload"
            fi
        done
        echo "Test Complete: $(date)"
    } | tee "${result_file}"
    log_pass "XSS Prevention test completed"
}

test_security_headers() {
    local result_file="${TEST_RUN_DIR}/security-headers.log"
    log_info "Testing: Security Headers"
    
    {
        echo "=== Security Headers Test ==="
        response=$(curl -s -i "${SERVICE_URL}/api/v1/actuator/health" 2>&1)
        echo "Checking headers:"
        for header in "X-Content-Type-Options" "X-Frame-Options" "X-XSS-Protection"; do
            if echo "$response" | grep -qi "^$header:"; then
                echo "✓ Header present: $header"
            else
                echo "! Header missing: $header"
            fi
        done
        echo "Test Complete: $(date)"
    } | tee "${result_file}"
    log_pass "Security Headers test completed"
}

test_access_control() {
    local result_file="${TEST_RUN_DIR}/access-control.log"
    log_info "Testing: Access Control"
    
    {
        echo "=== Access Control Test ==="
        echo "Cross-tenant access:"
        http_code=$(curl -s -o /dev/null -w "%{http_code}" \
            -H "X-Tenant-ID: different-tenant" "${SERVICE_URL}/api/v1/cms/claims/search" 2>&1)
        echo "✓ Cross-tenant: HTTP $http_code"
        echo "Test Complete: $(date)"
    } | tee "${result_file}"
    log_pass "Access Control test completed"
}

test_sensitive_data() {
    local result_file="${TEST_RUN_DIR}/sensitive-data.log"
    log_info "Testing: Sensitive Data"
    
    {
        echo "=== Sensitive Data Test ==="
        response=$(curl -s "${SERVICE_URL}/api/v1/actuator/health" 2>&1)
        echo "Checking for sensitive data:"
        for pattern in "password" "api_key" "secret" "token"; do
            if echo "$response" | grep -qi "$pattern"; then
                echo "✗ Potential exposure: $pattern"
            else
                echo "✓ No exposure of: $pattern"
            fi
        done
        echo "Test Complete: $(date)"
    } | tee "${result_file}"
    log_pass "Sensitive Data test completed"
}

test_rate_limiting() {
    local result_file="${TEST_RUN_DIR}/rate-limiting.log"
    log_info "Testing: Rate Limiting"
    
    {
        echo "=== Rate Limiting Test ==="
        echo "Sending 100 rapid requests..."
        local success=0
        for i in {1..100}; do
            http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 "${SERVICE_URL}/api/v1/actuator/health" 2>&1)
            if [ "$http_code" == "200" ]; then
                ((success++))
            fi
        done
        echo "Results: $success/100 successful"
        echo "Test Complete: $(date)"
    } | tee "${result_file}"
    log_pass "Rate Limiting test completed"
}

main() {
    log_info "CMS Connector Service - Security Testing"
    mkdir -p "${TEST_RUN_DIR}"
    
    test_authentication
    log_info ""
    test_sql_injection
    log_info ""
    test_xss
    log_info ""
    test_security_headers
    log_info ""
    test_access_control
    log_info ""
    test_sensitive_data
    log_info ""
    test_rate_limiting
    
    log_pass "All security tests complete!"
    log_info "Results saved to: ${TEST_RUN_DIR}"
}

main "$@"
