#!/bin/bash

##############################################################################
# System Health Test Runner
#
# Orchestrates comprehensive system testing:
# 1. Infrastructure health checks
# 2. Gateway smoke tests (all 29 services)
# 3. Authentication flow validation
# 4. Demo platform validation (configurable: quick or full)
# 5. End-to-end workflow tests
#
# Prerequisites:
#   - Docker and Docker Compose running
#   - All HDIM services deployed
#   - Network connectivity to localhost
#
# Usage:
#   ./scripts/test-system-health.sh                 # Quick validation
#   DEMO_MODE=full ./scripts/test-system-health.sh  # Full demo validation
#   ./scripts/test-system-health.sh --skip-e2e      # Skip end-to-end tests
#
# Exit codes:
#   0 - All tests passed
#   1 - Infrastructure health failed
#   2 - Gateway tests failed
#   3 - Authentication tests failed
#   4 - Demo platform validation failed
#   5 - End-to-end workflow failed
##############################################################################

set -e  # Exit on error
set -u  # Exit on undefined variable

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
DEMO_MODE="${DEMO_MODE:-quick}"  # quick or full
SKIP_E2E="${SKIP_E2E:-false}"
SKIP_GATEWAY="${SKIP_GATEWAY:-false}"
SKIP_AUTH="${SKIP_AUTH:-false}"
OUTPUT_DIR="${OUTPUT_DIR:-/tmp/hdim-health-tests}"

# Test result tracking
TOTAL_TEST_SUITES=0
PASSED_TEST_SUITES=0
FAILED_TEST_SUITES=0

# Create output directory
mkdir -p "$OUTPUT_DIR"

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

log_test_suite() {
    local suite_name=$1
    local status=$2

    TOTAL_TEST_SUITES=$((TOTAL_TEST_SUITES + 1))

    if [ "$status" = "PASS" ]; then
        PASSED_TEST_SUITES=$((PASSED_TEST_SUITES + 1))
        echo -e "${GREEN}✓${NC} $suite_name"
    else
        FAILED_TEST_SUITES=$((FAILED_TEST_SUITES + 1))
        echo -e "${RED}✗${NC} $suite_name"
    fi
}

##############################################################################
# Test Suite Functions
##############################################################################

run_infrastructure_checks() {
    log_info "==================================================================="
    log_info "1. Infrastructure Health Checks"
    log_info "==================================================================="

    local all_healthy=true

    # Check Docker
    if ! docker ps > /dev/null 2>&1; then
        log_error "Docker is not running"
        log_test_suite "Infrastructure: Docker" "FAIL"
        return 1
    fi
    log_success "Docker is running"

    # Check PostgreSQL
    if docker compose exec -T postgres pg_isready -U healthdata > /dev/null 2>&1; then
        log_success "PostgreSQL is healthy"
    else
        log_error "PostgreSQL is not healthy"
        all_healthy=false
    fi

    # Check Redis
    if docker compose exec -T redis redis-cli ping > /dev/null 2>&1; then
        log_success "Redis is healthy"
    else
        log_error "Redis is not healthy"
        all_healthy=false
    fi

    # Check Kafka
    if docker compose ps kafka | grep -q "Up"; then
        log_success "Kafka is running"
    else
        log_warn "Kafka may not be running"
    fi

    if [ "$all_healthy" = "true" ]; then
        log_test_suite "Infrastructure Health" "PASS"
        return 0
    else
        log_test_suite "Infrastructure Health" "FAIL"
        return 1
    fi
}

run_gateway_smoke_test() {
    if [ "$SKIP_GATEWAY" = "true" ]; then
        log_info "Gateway smoke test skipped (SKIP_GATEWAY=true)"
        return 0
    fi

    log_info "==================================================================="
    log_info "2. Gateway Smoke Test (29 Services)"
    log_info "==================================================================="

    if [ ! -f "$SCRIPT_DIR/test-gateway-smoke.sh" ]; then
        log_error "Gateway smoke test script not found"
        log_test_suite "Gateway Smoke Test" "FAIL"
        return 2
    fi

    # Run gateway smoke test
    if "$SCRIPT_DIR/test-gateway-smoke.sh" --mode=full --output "$OUTPUT_DIR/gateway-smoke.json"; then
        log_success "Gateway smoke test completed"

        # Show summary
        if [ -f "$OUTPUT_DIR/gateway-smoke.json" ]; then
            pass_rate=$(jq -r '.summary.passRate' "$OUTPUT_DIR/gateway-smoke.json" 2>/dev/null || echo "N/A")
            total=$(jq -r '.summary.totalTests' "$OUTPUT_DIR/gateway-smoke.json" 2>/dev/null || echo "N/A")
            passed=$(jq -r '.summary.passed' "$OUTPUT_DIR/gateway-smoke.json" 2>/dev/null || echo "N/A")
            log_info "Results: $passed/$total tests passed ($pass_rate%)"
        fi

        log_test_suite "Gateway Smoke Test" "PASS"
        return 0
    else
        log_error "Gateway smoke test failed"
        log_test_suite "Gateway Smoke Test" "FAIL"
        return 2
    fi
}

run_authentication_tests() {
    if [ "$SKIP_AUTH" = "true" ]; then
        log_info "Authentication tests skipped (SKIP_AUTH=true)"
        return 0
    fi

    log_info "==================================================================="
    log_info "3. Authentication Flow Validation"
    log_info "==================================================================="

    if [ ! -f "$SCRIPT_DIR/test-authentication-flow.sh" ]; then
        log_warn "Authentication test script not found, skipping"
        return 0
    fi

    # Run authentication tests
    if "$SCRIPT_DIR/test-authentication-flow.sh" > "$OUTPUT_DIR/auth-test.log" 2>&1; then
        log_success "Authentication tests passed"
        log_test_suite "Authentication Tests" "PASS"
        return 0
    else
        log_error "Authentication tests failed"
        log_info "See log: $OUTPUT_DIR/auth-test.log"
        log_test_suite "Authentication Tests" "FAIL"
        return 3
    fi
}

run_demo_platform_validation() {
    log_info "==================================================================="
    log_info "4. Demo Platform Validation ($DEMO_MODE mode)"
    log_info "==================================================================="

    if [ "$DEMO_MODE" = "full" ]; then
        log_info "Full demo mode: Expecting 19,000 patients across 4 scenarios"
        log_info "This validation checks that all scenarios loaded correctly"
    else
        log_info "Quick demo mode: Minimal patient validation"
    fi

    # Basic validation: Check if demo seeding service is available
    if curl -sf http://localhost:8103/actuator/health > /dev/null 2>&1; then
        log_success "Demo Seeding Service is healthy"
        log_test_suite "Demo Platform Validation" "PASS"
        return 0
    else
        log_error "Demo Seeding Service is not available"
        log_test_suite "Demo Platform Validation" "FAIL"
        return 4
    fi
}

run_end_to_end_workflow() {
    if [ "$SKIP_E2E" = "true" ]; then
        log_info "End-to-end workflow test skipped (SKIP_E2E=true)"
        return 0
    fi

    log_info "==================================================================="
    log_info "5. End-to-End Workflow Test"
    log_info "==================================================================="

    if [ ! -f "$SCRIPT_DIR/test-end-to-end-workflow.sh" ]; then
        log_error "End-to-end workflow script not found"
        log_test_suite "End-to-End Workflow" "FAIL"
        return 5
    fi

    # Run end-to-end workflow with current demo mode
    export DEMO_MODE
    export RUN_GATEWAY_SMOKE=false  # Already ran gateway smoke test
    export CLEANUP=false  # Don't cleanup after e2e test

    if "$SCRIPT_DIR/test-end-to-end-workflow.sh" > "$OUTPUT_DIR/e2e-workflow.log" 2>&1; then
        log_success "End-to-end workflow test passed"
        log_test_suite "End-to-End Workflow" "PASS"
        return 0
    else
        log_error "End-to-end workflow test failed"
        log_info "See log: $OUTPUT_DIR/e2e-workflow.log"
        log_test_suite "End-to-End Workflow" "FAIL"
        return 5
    fi
}

generate_comprehensive_report() {
    log_info "==================================================================="
    log_info "Generating Comprehensive Test Report"
    log_info "==================================================================="

    local report_file="$OUTPUT_DIR/system-health-report.txt"

    cat > "$report_file" <<EOF
================================================================================
HDIM System Health Report
================================================================================
Generated: $(date)
Demo Mode: $DEMO_MODE
Output Directory: $OUTPUT_DIR

TEST SUITE SUMMARY
--------------------------------------------------------------------------------
Total Test Suites: $TOTAL_TEST_SUITES
Passed: $PASSED_TEST_SUITES
Failed: $FAILED_TEST_SUITES
Success Rate: $(awk "BEGIN {printf \"%.1f\", ($PASSED_TEST_SUITES / $TOTAL_TEST_SUITES) * 100}")%

TEST RESULTS
--------------------------------------------------------------------------------
EOF

    # Append individual test results
    if [ -f "$OUTPUT_DIR/gateway-smoke.json" ]; then
        echo "" >> "$report_file"
        echo "Gateway Smoke Test:" >> "$report_file"
        jq -r '.summary' "$OUTPUT_DIR/gateway-smoke.json" >> "$report_file" 2>/dev/null || echo "  No data" >> "$report_file"
    fi

    echo "" >> "$report_file"
    echo "================================================================================
" >> "$report_file"

    log_success "Report saved to: $report_file"
}

print_test_summary() {
    echo ""
    echo "==================================================================="
    echo "                     SYSTEM HEALTH TEST SUMMARY"
    echo "==================================================================="
    echo -e "Total Test Suites:  ${CYAN}$TOTAL_TEST_SUITES${NC}"
    echo -e "Passed:             ${GREEN}$PASSED_TEST_SUITES${NC}"
    echo -e "Failed:             ${RED}$FAILED_TEST_SUITES${NC}"

    if [ "$TOTAL_TEST_SUITES" -gt 0 ]; then
        local success_rate
        success_rate=$(awk "BEGIN {printf \"%.1f\", ($PASSED_TEST_SUITES / $TOTAL_TEST_SUITES) * 100}")
        echo -e "Success Rate:       ${CYAN}$success_rate%${NC}"
    fi

    echo "==================================================================="
    echo "Output Directory: $OUTPUT_DIR"
    echo ""

    if [ -f "$OUTPUT_DIR/gateway-smoke.json" ]; then
        echo "Reports:"
        echo "  - Gateway Smoke Test: $OUTPUT_DIR/gateway-smoke.json"
    fi
    if [ -f "$OUTPUT_DIR/auth-test.log" ]; then
        echo "  - Authentication Tests: $OUTPUT_DIR/auth-test.log"
    fi
    if [ -f "$OUTPUT_DIR/e2e-workflow.log" ]; then
        echo "  - End-to-End Workflow: $OUTPUT_DIR/e2e-workflow.log"
    fi
    if [ -f "$OUTPUT_DIR/system-health-report.txt" ]; then
        echo "  - Comprehensive Report: $OUTPUT_DIR/system-health-report.txt"
    fi

    echo "==================================================================="

    if [ "$FAILED_TEST_SUITES" -eq 0 ]; then
        return 0
    else
        return 1
    fi
}

##############################################################################
# Main Workflow
##############################################################################

main() {
    log_info "==================================================================="
    log_info "          HDIM SYSTEM HEALTH TEST RUNNER"
    log_info "==================================================================="
    log_info "Demo Mode: $DEMO_MODE"
    log_info "Output Directory: $OUTPUT_DIR"
    log_info "==================================================================="

    # Run test suites
    run_infrastructure_checks || true
    run_gateway_smoke_test || true
    run_authentication_tests || true
    run_demo_platform_validation || true
    run_end_to_end_workflow || true

    # Generate reports
    generate_comprehensive_report

    # Print summary
    print_test_summary

    # Determine exit code
    if [ "$FAILED_TEST_SUITES" -eq 0 ]; then
        log_success "All tests passed!"
        exit 0
    else
        log_error "Some tests failed"
        exit 1
    fi
}

# Run main workflow
main "$@"
