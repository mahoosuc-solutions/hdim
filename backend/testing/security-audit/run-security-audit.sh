#!/bin/bash

##############################################################################
# HDIM Security Audit Script
#
# Performs comprehensive security testing on HDIM authentication and API
# endpoints following OWASP Top 10 and HIPAA security requirements.
#
# Prerequisites:
# - curl, jq installed
# - HDIM services running
# - Test environment (NOT production!)
#
# Usage:
#   ./run-security-audit.sh [category]
#
# Categories:
#   - auth          : Authentication security tests
#   - injection     : SQL/NoSQL/Command injection tests
#   - xss           : Cross-Site Scripting tests
#   - csrf          : Cross-Site Request Forgery tests
#   - headers       : Security headers validation
#   - tls           : TLS/SSL configuration tests
#   - hipaa         : HIPAA-specific compliance checks
#   - all           : Run all tests
##############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8001}"
RESULTS_DIR="./audit-results/$(date +%Y%m%d_%H%M%S)"
PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0

# Create results directory
mkdir -p "$RESULTS_DIR"

##############################################################################
# Helper Functions
##############################################################################

print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_test() {
    echo -e "${YELLOW}TEST:${NC} $1"
}

print_pass() {
    echo -e "${GREEN}✓ PASS:${NC} $1"
    ((PASS_COUNT++))
    echo "PASS: $1" >> "$RESULTS_DIR/summary.txt"
}

print_fail() {
    echo -e "${RED}✗ FAIL:${NC} $1"
    ((FAIL_COUNT++))
    echo "FAIL: $1" >> "$RESULTS_DIR/summary.txt"
}

print_warn() {
    echo -e "${YELLOW}⚠ WARN:${NC} $1"
    ((WARN_COUNT++))
    echo "WARN: $1" >> "$RESULTS_DIR/summary.txt"
}

check_prerequisites() {
    print_header "Checking Prerequisites"

    if ! command -v curl &> /dev/null; then
        echo "ERROR: curl not found"
        exit 1
    fi

    if ! curl -s "$GATEWAY_URL/actuator/health" > /dev/null 2>&1; then
        echo "ERROR: Gateway not responding at $GATEWAY_URL"
        exit 1
    fi

    print_pass "Prerequisites check passed"
}

##############################################################################
# Category 1: Authentication Security
##############################################################################

test_authentication_security() {
    print_header "Authentication Security Tests"

    # Test 1.1: Password strength requirements
    print_test "1.1 - Weak password rejection"
    RESPONSE=$(curl -s -w "%{http_code}" -X POST "$GATEWAY_URL/api/v1/auth/register" \
        -H "Content-Type: application/json" \
        -d '{"username":"testuser","password":"123","email":"test@test.com"}' \
        -o /dev/null)

    if [ "$RESPONSE" -eq 400 ] || [ "$RESPONSE" -eq 422 ]; then
        print_pass "Weak passwords are rejected"
    else
        print_fail "Weak password accepted (HTTP $RESPONSE)"
    fi

    # Test 1.2: SQL injection in username
    print_test "1.2 - SQL injection in login username"
    RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin'\'' OR '\''1'\''='\''1","password":"password"}')

    if echo "$RESPONSE" | grep -q "error\|invalid\|unauthorized"; then
        print_pass "SQL injection attempt blocked"
    else
        print_fail "Possible SQL injection vulnerability"
    fi

    # Test 1.3: Account lockout after failed attempts
    print_test "1.3 - Account lockout after multiple failed logins"
    for i in {1..6}; do
        curl -s -X POST "$GATEWAY_URL/api/v1/auth/login" \
            -H "Content-Type: application/json" \
            -d '{"username":"test_evaluator","password":"wrongpassword"}' \
            > /dev/null 2>&1
    done

    RESPONSE=$(curl -s -w "%{http_code}" -X POST "$GATEWAY_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"test_evaluator","password":"wrongpassword"}' \
        -o /dev/null)

    if [ "$RESPONSE" -eq 423 ] || [ "$RESPONSE" -eq 429 ]; then
        print_pass "Account lockout engaged after failed attempts"
    else
        print_warn "Account lockout not detected (HTTP $RESPONSE)"
    fi

    # Test 1.4: JWT token tampering
    print_test "1.4 - JWT token tampering detection"
    TAMPERED_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTUxNjIzOTAyMn0.TAMPERED"

    RESPONSE=$(curl -s -w "%{http_code}" -X GET "$GATEWAY_URL/api/v1/auth/me" \
        -H "Authorization: Bearer $TAMPERED_TOKEN" \
        -o /dev/null)

    if [ "$RESPONSE" -eq 401 ] || [ "$RESPONSE" -eq 403 ]; then
        print_pass "Tampered JWT tokens rejected"
    else
        print_fail "Tampered JWT accepted (HTTP $RESPONSE)"
    fi

    # Test 1.5: Session fixation
    print_test "1.5 - Session fixation protection"
    # Get a token
    TOKEN_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"test_viewer","password":"password123"}')

    if echo "$TOKEN_RESPONSE" | grep -q "accessToken"; then
        # Try to reuse the same token after logout
        if command -v jq &> /dev/null; then
            ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.accessToken')

            # Logout
            curl -s -X POST "$GATEWAY_URL/api/v1/auth/logout" \
                -H "Authorization: Bearer $ACCESS_TOKEN" > /dev/null 2>&1

            # Try to use token after logout
            REUSE_RESPONSE=$(curl -s -w "%{http_code}" -X GET "$GATEWAY_URL/api/v1/auth/me" \
                -H "Authorization: Bearer $ACCESS_TOKEN" \
                -o /dev/null)

            if [ "$REUSE_RESPONSE" -eq 401 ]; then
                print_pass "Tokens invalidated after logout"
            else
                print_fail "Token still valid after logout (HTTP $REUSE_RESPONSE)"
            fi
        else
            print_warn "jq not available - skipping session fixation test"
        fi
    else
        print_warn "Could not obtain token for session fixation test"
    fi
}

##############################################################################
# Category 2: Injection Vulnerabilities
##############################################################################

test_injection_vulnerabilities() {
    print_header "Injection Vulnerability Tests"

    # Test 2.1: SQL injection in search parameters
    print_test "2.1 - SQL injection in search query"
    RESPONSE=$(curl -s "$GATEWAY_URL/api/v1/patients?search=1%27%20OR%20%271%27%3D%271")

    if echo "$RESPONSE" | grep -qi "error\|invalid"; then
        print_pass "SQL injection in search blocked"
    else
        print_warn "Potential SQL injection - verify response"
    fi

    # Test 2.2: NoSQL injection
    print_test "2.2 - NoSQL injection attempt"
    RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":{"$ne":""},"password":{"$ne":""}}')

    if echo "$RESPONSE" | grep -qi "error\|invalid"; then
        print_pass "NoSQL injection attempt blocked"
    else
        print_fail "Possible NoSQL injection vulnerability"
    fi

    # Test 2.3: Command injection
    print_test "2.3 - Command injection in parameters"
    RESPONSE=$(curl -s "$GATEWAY_URL/api/v1/patients?id=1;%20cat%20/etc/passwd")

    if echo "$RESPONSE" | grep -qi "root:x:0:0"; then
        print_fail "CRITICAL: Command injection successful!"
    else
        print_pass "Command injection blocked"
    fi

    # Test 2.4: LDAP injection
    print_test "2.4 - LDAP injection attempt"
    RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"*)(uid=*))(|(uid=*","password":"password"}')

    if echo "$RESPONSE" | grep -qi "error\|invalid"; then
        print_pass "LDAP injection blocked"
    else
        print_warn "Potential LDAP injection - verify response"
    fi
}

##############################################################################
# Category 3: Cross-Site Scripting (XSS)
##############################################################################

test_xss_vulnerabilities() {
    print_header "Cross-Site Scripting (XSS) Tests"

    # Test 3.1: Reflected XSS in error messages
    print_test "3.1 - Reflected XSS in error responses"
    RESPONSE=$(curl -s "$GATEWAY_URL/api/v1/patients?name=%3Cscript%3Ealert%281%29%3C%2Fscript%3E")

    if echo "$RESPONSE" | grep -q "<script>"; then
        print_fail "XSS vulnerability - script tag in response"
    else
        print_pass "XSS payload properly escaped"
    fi

    # Test 3.2: Stored XSS in user fields
    print_test "3.2 - Stored XSS in registration"
    RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/register" \
        -H "Content-Type: application/json" \
        -d '{"username":"<img src=x onerror=alert(1)>","password":"Password123!","email":"test@test.com"}')

    if echo "$RESPONSE" | grep -q "error\|invalid"; then
        print_pass "XSS in username rejected"
    else
        print_warn "Verify XSS payload handling in database"
    fi
}

##############################################################################
# Category 4: CSRF Protection
##############################################################################

test_csrf_protection() {
    print_header "CSRF Protection Tests"

    # Test 4.1: State-changing operations without CSRF token
    print_test "4.1 - POST request without CSRF token"
    RESPONSE=$(curl -s -w "%{http_code}" -X POST "$GATEWAY_URL/api/v1/patients" \
        -H "Content-Type: application/json" \
        -d '{"name":"Test Patient"}' \
        -o /dev/null)

    if [ "$RESPONSE" -eq 401 ] || [ "$RESPONSE" -eq 403 ]; then
        print_pass "CSRF protection active (requires authentication)"
    else
        print_warn "Verify CSRF protection (HTTP $RESPONSE)"
    fi
}

##############################################################################
# Category 5: Security Headers
##############################################################################

test_security_headers() {
    print_header "Security Headers Validation"

    HEADERS=$(curl -s -I "$GATEWAY_URL/api/v1/auth/login")

    # Test 5.1: X-Content-Type-Options
    print_test "5.1 - X-Content-Type-Options header"
    if echo "$HEADERS" | grep -qi "X-Content-Type-Options.*nosniff"; then
        print_pass "X-Content-Type-Options header present"
    else
        print_fail "Missing X-Content-Type-Options: nosniff"
    fi

    # Test 5.2: X-Frame-Options
    print_test "5.2 - X-Frame-Options header"
    if echo "$HEADERS" | grep -qi "X-Frame-Options"; then
        print_pass "X-Frame-Options header present"
    else
        print_fail "Missing X-Frame-Options header (clickjacking risk)"
    fi

    # Test 5.3: Strict-Transport-Security
    print_test "5.3 - Strict-Transport-Security header (HSTS)"
    if echo "$HEADERS" | grep -qi "Strict-Transport-Security"; then
        print_pass "HSTS header present"
    else
        print_warn "Missing HSTS header (recommended for production)"
    fi

    # Test 5.4: Content-Security-Policy
    print_test "5.4 - Content-Security-Policy header"
    if echo "$HEADERS" | grep -qi "Content-Security-Policy"; then
        print_pass "CSP header present"
    else
        print_warn "Missing Content-Security-Policy header"
    fi

    # Test 5.5: Cache-Control for sensitive data
    print_test "5.5 - Cache-Control for authentication endpoints"
    if echo "$HEADERS" | grep -qi "Cache-Control.*no-store"; then
        print_pass "Cache-Control properly configured"
    else
        print_fail "Missing Cache-Control: no-store (PHI caching risk)"
    fi
}

##############################################################################
# Category 6: TLS/SSL Configuration
##############################################################################

test_tls_configuration() {
    print_header "TLS/SSL Configuration Tests"

    # Test 6.1: TLS version support
    print_test "6.1 - TLS 1.2+ enforced"
    TLS_TEST=$(curl -v --tlsv1.0 --tls-max 1.0 "$GATEWAY_URL" 2>&1 || true)

    if echo "$TLS_TEST" | grep -qi "refused\|error"; then
        print_pass "TLS 1.0/1.1 disabled"
    else
        print_warn "TLS version enforcement - verify manually"
    fi

    # Test 6.2: Certificate validation (if HTTPS)
    if [[ "$GATEWAY_URL" == https://* ]]; then
        print_test "6.2 - Certificate validation"
        if curl -s -I "$GATEWAY_URL" > /dev/null 2>&1; then
            print_pass "Valid TLS certificate"
        else
            print_fail "Certificate validation failed"
        fi
    else
        print_warn "Not using HTTPS - skipping certificate tests"
    fi
}

##############################################################################
# Category 7: HIPAA Compliance
##############################################################################

test_hipaa_compliance() {
    print_header "HIPAA Compliance Tests"

    # Test 7.1: PHI in logs
    print_test "7.1 - PHI not exposed in error messages"
    RESPONSE=$(curl -s "$GATEWAY_URL/api/v1/patients/00000000-0000-0000-0000-000000000000")

    if echo "$RESPONSE" | grep -qiE "ssn|social.security|date.of.birth"; then
        print_fail "PHI potentially exposed in error response"
    else
        print_pass "No PHI in error responses"
    fi

    # Test 7.2: Audit logging
    print_test "7.2 - Audit logging for PHI access"
    # This would require checking audit logs - simplified test
    print_warn "Verify audit logs manually - check for LOGIN_SUCCESS events"

    # Test 7.3: Data encryption at rest
    print_test "7.3 - Database encryption configuration"
    print_warn "Verify database encryption manually (check PostgreSQL config)"

    # Test 7.4: Access control
    print_test "7.4 - Role-based access control (RBAC)"
    # Try accessing admin endpoint as viewer
    TOKEN_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"test_viewer","password":"password123"}')

    if command -v jq &> /dev/null && echo "$TOKEN_RESPONSE" | jq -e '.accessToken' > /dev/null 2>&1; then
        TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.accessToken')

        ADMIN_RESPONSE=$(curl -s -w "%{http_code}" -X POST "$GATEWAY_URL/api/v1/auth/register" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d '{"username":"newadmin","password":"Password123!"}' \
            -o /dev/null)

        if [ "$ADMIN_RESPONSE" -eq 403 ]; then
            print_pass "RBAC enforced - viewers cannot register users"
        else
            print_fail "RBAC violation - viewer has admin privileges (HTTP $ADMIN_RESPONSE)"
        fi
    else
        print_warn "Could not test RBAC - token acquisition failed"
    fi
}

##############################################################################
# Generate Security Report
##############################################################################

generate_report() {
    print_header "Generating Security Audit Report"

    REPORT_FILE="$RESULTS_DIR/SECURITY_AUDIT_REPORT.md"

    cat > "$REPORT_FILE" << EOF
# HDIM Security Audit Report

**Date**: $(date)
**Target**: $GATEWAY_URL
**Auditor**: Automated Security Scanner

## Executive Summary

- **Passed**: $PASS_COUNT tests
- **Failed**: $FAIL_COUNT tests
- **Warnings**: $WARN_COUNT tests

## Test Results

### Authentication Security
$(grep "1\." "$RESULTS_DIR/summary.txt" || echo "No data")

### Injection Vulnerabilities
$(grep "2\." "$RESULTS_DIR/summary.txt" || echo "No data")

### Cross-Site Scripting (XSS)
$(grep "3\." "$RESULTS_DIR/summary.txt" || echo "No data")

### CSRF Protection
$(grep "4\." "$RESULTS_DIR/summary.txt" || echo "No data")

### Security Headers
$(grep "5\." "$RESULTS_DIR/summary.txt" || echo "No data")

### TLS/SSL Configuration
$(grep "6\." "$RESULTS_DIR/summary.txt" || echo "No data")

### HIPAA Compliance
$(grep "7\." "$RESULTS_DIR/summary.txt" || echo "No data")

## Critical Findings

$(grep "FAIL:" "$RESULTS_DIR/summary.txt" | sed 's/^/- /' || echo "None")

## Warnings Requiring Review

$(grep "WARN:" "$RESULTS_DIR/summary.txt" | sed 's/^/- /' || echo "None")

## Recommendations

1. **Authentication**
   - Implement account lockout after 5 failed attempts
   - Enforce strong password policy (min 12 chars, complexity)
   - Enable MFA for admin accounts

2. **Input Validation**
   - Validate and sanitize all user inputs
   - Use parameterized queries for database access
   - Implement input length limits

3. **Security Headers**
   - Add all recommended security headers
   - Enable HSTS with long max-age
   - Implement strict CSP policy

4. **HIPAA Compliance**
   - Enable audit logging for all PHI access
   - Implement database encryption at rest
   - Regular security training for developers

5. **TLS/SSL**
   - Enforce TLS 1.2+ only
   - Use strong cipher suites
   - Regular certificate rotation

## Next Steps

1. Address all FAILED tests immediately
2. Review and remediate WARN items
3. Perform manual penetration testing
4. Schedule regular security audits (quarterly)
5. Implement continuous security scanning in CI/CD

---

**Compliance Status**: $([ $FAIL_COUNT -eq 0 ] && echo "COMPLIANT ✓" || echo "NON-COMPLIANT ✗ - $FAIL_COUNT issues")

**Last Audit**: $(date)
EOF

    cat "$REPORT_FILE"
}

##############################################################################
# Main Execution
##############################################################################

main() {
    CATEGORY="${1:-all}"

    print_header "HDIM Security Audit"
    echo "Target: $GATEWAY_URL"
    echo "Results: $RESULTS_DIR"
    echo ""

    check_prerequisites

    case "$CATEGORY" in
        auth)
            test_authentication_security
            ;;
        injection)
            test_injection_vulnerabilities
            ;;
        xss)
            test_xss_vulnerabilities
            ;;
        csrf)
            test_csrf_protection
            ;;
        headers)
            test_security_headers
            ;;
        tls)
            test_tls_configuration
            ;;
        hipaa)
            test_hipaa_compliance
            ;;
        all)
            test_authentication_security
            test_injection_vulnerabilities
            test_xss_vulnerabilities
            test_csrf_protection
            test_security_headers
            test_tls_configuration
            test_hipaa_compliance
            generate_report
            ;;
        *)
            echo "Unknown category: $CATEGORY"
            echo "Available: auth, injection, xss, csrf, headers, tls, hipaa, all"
            exit 1
            ;;
    esac

    print_header "Security Audit Complete"
    echo -e "${GREEN}Passed:${NC} $PASS_COUNT"
    echo -e "${RED}Failed:${NC} $FAIL_COUNT"
    echo -e "${YELLOW}Warnings:${NC} $WARN_COUNT"
    echo ""
    echo "Full report: $RESULTS_DIR/SECURITY_AUDIT_REPORT.md"
}

main "$@"
