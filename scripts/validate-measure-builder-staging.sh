#!/bin/bash

################################################################################
# Measure Builder Staging Deployment Validation Script
#
# Purpose: Comprehensive validation of measure builder system before production
# Author: HDIM Deployment Team
# Date: January 18, 2026
# Version: 1.0
#
# Usage:
#   ./scripts/validate-measure-builder-staging.sh [--verbose] [--report-dir PATH]
#
# Features:
#   - Service health checks
#   - API endpoint validation
#   - Database connectivity verification
#   - Measure builder workflow testing
#   - Performance baseline measurement
#   - Multi-tenant isolation validation
#   - Security header verification
#   - Generate comprehensive validation report
################################################################################

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
VERBOSE="${VERBOSE:-false}"
REPORT_DIR="${REPORT_DIR:-.}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE="$REPORT_DIR/MEASURE_BUILDER_STAGING_VALIDATION_${TIMESTAMP}.md"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_SKIPPED=0

# Timeouts and delays (seconds)
HEALTH_CHECK_TIMEOUT=5
API_TIMEOUT=10
LOAD_TEST_DURATION=30
LOAD_TEST_CONCURRENT_USERS=10

# Service URLs (from environment or defaults)
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8001}"
CLINICAL_PORTAL_URL="${CLINICAL_PORTAL_URL:-http://localhost:4200}"
QUALITY_MEASURE_URL="${QUALITY_MEASURE_URL:-http://localhost:8087}"
FHIR_URL="${FHIR_URL:-http://localhost:8085}"
CQL_URL="${CQL_URL:-http://localhost:8081}"

# Test credentials
TEST_USERNAME="${TEST_USERNAME:-test_evaluator}"
TEST_PASSWORD="${TEST_PASSWORD:-password123}"
TEST_TENANT="${TEST_TENANT:-TENANT001}"

################################################################################
# Utility Functions
################################################################################

log_header() {
    echo -e "\n${BLUE}▶ $1${NC}"
}

log_pass() {
    echo -e "${GREEN}✓ $1${NC}"
    ((TESTS_PASSED++))
}

log_fail() {
    echo -e "${RED}✗ $1${NC}"
    ((TESTS_FAILED++))
}

log_skip() {
    echo -e "${YELLOW}⊘ $1${NC}"
    ((TESTS_SKIPPED++))
}

log_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

verbose_output() {
    if [ "$VERBOSE" = "true" ]; then
        echo "$@"
    fi
}

# Report functions
report_header() {
    echo "# Measure Builder Staging Validation Report" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "**Generated:** $(date '+%Y-%m-%d %H:%M:%S')" >> "$REPORT_FILE"
    echo "**Environment:** Staging" >> "$REPORT_FILE"
    echo "**Version:** 1.0" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
}

report_section() {
    echo "" >> "$REPORT_FILE"
    echo "## $1" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
}

report_test() {
    local status="$1"
    local name="$2"
    local details="${3:-}"

    if [ "$status" = "PASS" ]; then
        echo "- ✅ **$name**" >> "$REPORT_FILE"
    elif [ "$status" = "FAIL" ]; then
        echo "- ❌ **$name**" >> "$REPORT_FILE"
    else
        echo "- ⊘ **$name** (Skipped)" >> "$REPORT_FILE"
    fi

    if [ -n "$details" ]; then
        echo "  - $details" >> "$REPORT_FILE"
    fi
}

report_metrics() {
    local metric="$1"
    local value="$2"
    local target="$3"

    if [ -n "$target" ]; then
        echo "- **$metric:** $value (target: $target)" >> "$REPORT_FILE"
    else
        echo "- **$metric:** $value" >> "$REPORT_FILE"
    fi
}

################################################################################
# Health Check Tests
################################################################################

test_service_health() {
    log_header "Testing Service Health Checks"

    local services=(
        "Gateway:$GATEWAY_URL/health"
        "Clinical Portal:$CLINICAL_PORTAL_URL"
        "Quality Measure:$QUALITY_MEASURE_URL/health"
        "FHIR Service:$FHIR_URL/health"
        "CQL Engine:$CQL_URL/health"
    )

    for service in "${services[@]}"; do
        IFS=: read -r name url <<< "$service"

        if timeout $HEALTH_CHECK_TIMEOUT curl -s -f "$url" > /dev/null 2>&1; then
            log_pass "Service health: $name"
            report_test "PASS" "$name Health Check"
        else
            log_fail "Service health: $name (URL: $url)"
            report_test "FAIL" "$name Health Check" "Service did not respond to health check"
        fi
    done
}

################################################################################
# Database Connectivity Tests
################################################################################

test_database_connectivity() {
    log_header "Testing Database Connectivity"

    if command -v psql &> /dev/null; then
        if PGPASSWORD="${POSTGRES_PASSWORD:-password}" psql -h "${POSTGRES_HOST:-localhost}" \
            -U "${POSTGRES_USER:-healthdata}" -d "quality_db" -c "SELECT 1;" &> /dev/null; then
            log_pass "Database connectivity: PostgreSQL"
            report_test "PASS" "PostgreSQL Database Connection"
        else
            log_fail "Database connectivity: PostgreSQL"
            report_test "FAIL" "PostgreSQL Database Connection"
        fi
    else
        log_skip "Database connectivity: PostgreSQL (psql not installed)"
        report_test "SKIP" "PostgreSQL Database Connection"
    fi
}

################################################################################
# Authentication Tests
################################################################################

test_authentication() {
    log_header "Testing Authentication"

    # Test JWT token generation
    local auth_response
    auth_response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$TEST_USERNAME\",\"password\":\"$TEST_PASSWORD\"}" \
        "$GATEWAY_URL/api/v1/auth/login" 2>/dev/null || echo "")

    if echo "$auth_response" | grep -q "access_token\|token"; then
        log_pass "Authentication: JWT token generation"
        report_test "PASS" "JWT Token Generation"
    else
        log_fail "Authentication: JWT token generation"
        report_test "FAIL" "JWT Token Generation" "Could not obtain authentication token"
    fi
}

################################################################################
# Measure Builder API Tests
################################################################################

test_measure_builder_apis() {
    log_header "Testing Measure Builder APIs"

    # Test measure builder endpoints exist
    local endpoints=(
        "/api/v1/measures"
        "/api/v1/measures/builder"
        "/api/v1/measures/templates"
    )

    for endpoint in "${endpoints[@]}"; do
        local response_code
        response_code=$(curl -s -o /dev/null -w "%{http_code}" \
            -H "X-Tenant-ID: $TEST_TENANT" \
            "$QUALITY_MEASURE_URL$endpoint" 2>/dev/null || echo "000")

        if [ "$response_code" -eq 200 ] || [ "$response_code" -eq 401 ]; then
            log_pass "Measure Builder API: $endpoint"
            report_test "PASS" "Endpoint: $endpoint (HTTP $response_code)"
        else
            log_fail "Measure Builder API: $endpoint (HTTP $response_code)"
            report_test "FAIL" "Endpoint: $endpoint" "HTTP $response_code returned"
        fi
    done
}

################################################################################
# Multi-Tenant Isolation Tests
################################################################################

test_multi_tenant_isolation() {
    log_header "Testing Multi-Tenant Isolation"

    # Test that X-Tenant-ID header is required
    local response_code
    response_code=$(curl -s -o /dev/null -w "%{http_code}" \
        "$QUALITY_MEASURE_URL/api/v1/measures" 2>/dev/null || echo "000")

    if [ "$response_code" -eq 400 ] || [ "$response_code" -eq 401 ] || [ "$response_code" -eq 403 ]; then
        log_pass "Multi-tenant isolation: X-Tenant-ID required"
        report_test "PASS" "X-Tenant-ID Header Enforcement"
    else
        log_fail "Multi-tenant isolation: X-Tenant-ID not enforced (HTTP $response_code)"
        report_test "FAIL" "X-Tenant-ID Header Enforcement" "Header validation not enforced"
    fi
}

################################################################################
# Security Header Tests
################################################################################

test_security_headers() {
    log_header "Testing Security Headers"

    local response_headers
    response_headers=$(curl -s -i "$CLINICAL_PORTAL_URL" 2>/dev/null || echo "")

    local required_headers=(
        "Content-Security-Policy"
        "X-Content-Type-Options"
        "X-Frame-Options"
        "Strict-Transport-Security"
    )

    for header in "${required_headers[@]}"; do
        if echo "$response_headers" | grep -qi "$header"; then
            log_pass "Security header present: $header"
            report_test "PASS" "Security Header: $header"
        else
            log_fail "Security header missing: $header"
            report_test "FAIL" "Security Header: $header"
        fi
    done
}

################################################################################
# Cache Control Tests (HIPAA Compliance)
################################################################################

test_cache_control() {
    log_header "Testing Cache Control (HIPAA Compliance)"

    # Test PHI endpoints have no-cache headers
    local response_headers
    response_headers=$(curl -s -i \
        -H "X-Tenant-ID: $TEST_TENANT" \
        "$QUALITY_MEASURE_URL/api/v1/measures" 2>/dev/null || echo "")

    if echo "$response_headers" | grep -qi "Cache-Control.*no-store\|Cache-Control.*no-cache"; then
        log_pass "Cache control: no-store/no-cache headers present"
        report_test "PASS" "Cache-Control Headers (PHI Endpoints)"
    else
        log_fail "Cache control: Missing no-store/no-cache headers"
        report_test "FAIL" "Cache-Control Headers (PHI Endpoints)"
    fi
}

################################################################################
# Performance Baseline Tests
################################################################################

test_performance_baselines() {
    log_header "Testing Performance Baselines"

    # Test API response time for measure list
    local start_time
    local end_time
    local response_time

    start_time=$(date +%s%N)
    curl -s -o /dev/null \
        -H "X-Tenant-ID: $TEST_TENANT" \
        "$QUALITY_MEASURE_URL/api/v1/measures?limit=10" 2>/dev/null || true
    end_time=$(date +%s%N)

    response_time=$(( (end_time - start_time) / 1000000 ))

    if [ "$response_time" -lt 1000 ]; then
        log_pass "Performance: API response time ${response_time}ms (target: <1000ms)"
        report_metrics "API Response Time" "${response_time}ms" "<1000ms"
    else
        log_fail "Performance: API response time ${response_time}ms exceeds target"
        report_metrics "API Response Time" "${response_time}ms" "<1000ms"
    fi
}

################################################################################
# Load Test
################################################################################

test_load_performance() {
    log_header "Testing Load Performance (Light Load Test)"

    if ! command -v ab &> /dev/null; then
        log_skip "Load testing: Apache Bench (ab) not installed"
        report_test "SKIP" "Load Testing" "Apache Bench not available"
        return
    fi

    log_info "Running load test: 10 requests with 2 concurrent users..."

    local load_result
    load_result=$(ab -n 10 -c 2 -q "$QUALITY_MEASURE_URL/api/v1/measures" 2>/dev/null || echo "")

    if [ -n "$load_result" ]; then
        log_pass "Load test completed successfully"
        report_test "PASS" "Load Testing (10 requests, 2 concurrent)"

        # Extract metrics if possible
        local avg_time
        avg_time=$(echo "$load_result" | grep "Time per request:" | head -1 | awk '{print $NF}')
        if [ -n "$avg_time" ]; then
            report_metrics "Average Response Time" "$avg_time" "<500ms"
        fi
    else
        log_fail "Load test failed"
        report_test "FAIL" "Load Testing" "Apache Bench failed to complete"
    fi
}

################################################################################
# Logging and Monitoring Tests
################################################################################

test_logging_configuration() {
    log_header "Testing Logging Configuration"

    # Check if Docker containers are running and check logs for errors
    if command -v docker &> /dev/null; then
        local critical_errors=0
        local error_containers=""

        for container in "quality-measure-service" "clinical-portal" "gateway-service"; do
            if docker ps --filter "name=$container" --quiet | grep -q .; then
                # Check for critical errors in logs (last 100 lines)
                local error_count
                error_count=$(docker logs "$container" 2>/dev/null | tail -100 | grep -c "ERROR\|FATAL" || echo "0")

                if [ "$error_count" -eq 0 ]; then
                    log_pass "Logging: No critical errors in $container"
                    report_test "PASS" "Container Logs: $container"
                else
                    log_fail "Logging: Found $error_count critical errors in $container"
                    report_test "FAIL" "Container Logs: $container" "Found $error_count errors"
                    ((critical_errors++))
                    error_containers="$error_containers $container"
                fi
            fi
        done
    else
        log_skip "Logging configuration: Docker not available"
        report_test "SKIP" "Container Logging Analysis"
    fi
}

################################################################################
# Summary and Report Generation
################################################################################

generate_report() {
    # Initialize report
    echo "# Measure Builder Staging Validation Report" > "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "**Generated:** $(date '+%Y-%m-%d %H:%M:%S')" >> "$REPORT_FILE"
    echo "**Environment:** Staging" >> "$REPORT_FILE"
    echo "**Validation Script Version:** 1.0" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"

    # Add service configuration section
    report_section "Service Configuration"
    echo "| Service | URL |" >> "$REPORT_FILE"
    echo "|---------|-----|" >> "$REPORT_FILE"
    echo "| Gateway | $GATEWAY_URL |" >> "$REPORT_FILE"
    echo "| Clinical Portal | $CLINICAL_PORTAL_URL |" >> "$REPORT_FILE"
    echo "| Quality Measure | $QUALITY_MEASURE_URL |" >> "$REPORT_FILE"
    echo "| FHIR Service | $FHIR_URL |" >> "$REPORT_FILE"
    echo "| CQL Engine | $CQL_URL |" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"

    # Add test results summary
    report_section "Test Results Summary"
    echo "| Status | Count |" >> "$REPORT_FILE"
    echo "|--------|-------|" >> "$REPORT_FILE"
    echo "| ✅ Passed | $TESTS_PASSED |" >> "$REPORT_FILE"
    echo "| ❌ Failed | $TESTS_FAILED |" >> "$REPORT_FILE"
    echo "| ⊘ Skipped | $TESTS_SKIPPED |" >> "$REPORT_FILE"
    echo "| **Total** | $((TESTS_PASSED + TESTS_FAILED + TESTS_SKIPPED)) |" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
}

print_summary() {
    local total=$((TESTS_PASSED + TESTS_FAILED + TESTS_SKIPPED))
    local pass_rate=0

    if [ $total -gt 0 ]; then
        pass_rate=$(( (TESTS_PASSED * 100) / total ))
    fi

    echo ""
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║              Validation Results Summary                     ║${NC}"
    echo -e "${BLUE}╠════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${BLUE}║${NC} Tests Passed:  ${GREEN}$TESTS_PASSED${NC}"
    echo -e "${BLUE}║${NC} Tests Failed:  ${RED}$TESTS_FAILED${NC}"
    echo -e "${BLUE}║${NC} Tests Skipped: ${YELLOW}$TESTS_SKIPPED${NC}"
    echo -e "${BLUE}║${NC} Pass Rate:     ${GREEN}${pass_rate}%${NC}"
    echo -e "${BLUE}╠════════════════════════════════════════════════════════════╣${NC}"

    if [ $TESTS_FAILED -eq 0 ]; then
        echo -e "${BLUE}║${NC} ${GREEN}✓ VALIDATION PASSED - Ready for Production${NC}"
    else
        echo -e "${BLUE}║${NC} ${RED}✗ VALIDATION FAILED - Issues found${NC}"
    fi

    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "📋 Detailed report: ${GREEN}$REPORT_FILE${NC}"
}

################################################################################
# Main Execution
################################################################################

main() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║   Measure Builder Staging Deployment Validation             ║${NC}"
    echo -e "${BLUE}║   Version 1.0 - $(date '+%Y-%m-%d %H:%M:%S')         ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --verbose)
                VERBOSE="true"
                shift
                ;;
            --report-dir)
                REPORT_DIR="$2"
                shift 2
                ;;
            *)
                echo "Unknown option: $1"
                exit 1
                ;;
        esac
    done

    # Create report directory if needed
    mkdir -p "$REPORT_DIR"

    # Run all validation tests
    test_service_health
    test_database_connectivity
    test_authentication
    test_measure_builder_apis
    test_multi_tenant_isolation
    test_security_headers
    test_cache_control
    test_performance_baselines
    test_load_performance
    test_logging_configuration

    # Generate report
    generate_report

    # Print summary
    print_summary

    # Exit with appropriate code
    if [ $TESTS_FAILED -eq 0 ]; then
        exit 0
    else
        exit 1
    fi
}

# Execute main function
main "$@"
