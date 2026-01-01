#!/bin/bash

################################################################################
# HDIM Authentication Flow Testing with Comprehensive Logging
#
# This script runs authentication tests while capturing:
# - Shell test output
# - Docker service logs
# - Gateway authentication logs
# - API request/response logs
# - Database query logs
#
# Usage:
#   ./scripts/test-with-logs.sh
#   ./scripts/test-with-logs.sh --output-dir ./test-results
#   ./scripts/test-with-logs.sh --follow-logs
#
################################################################################

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
OUTPUT_DIR="${1:-.}/test-results-$(date +%Y%m%d-%H%M%S)"
FOLLOW_LOGS="${FOLLOW_LOGS:-0}"
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Log files
TEST_LOG="$OUTPUT_DIR/test-results.log"
GATEWAY_LOG="$OUTPUT_DIR/gateway-service.log"
FHIR_LOG="$OUTPUT_DIR/fhir-service.log"
PATIENT_LOG="$OUTPUT_DIR/patient-service.log"
QUALITY_LOG="$OUTPUT_DIR/quality-measure-service.log"
CARE_GAP_LOG="$OUTPUT_DIR/care-gap-service.log"
POSTGRES_LOG="$OUTPUT_DIR/postgres.log"
API_LOG="$OUTPUT_DIR/api-requests.log"
SUMMARY_LOG="$OUTPUT_DIR/summary.log"

mkdir -p "$OUTPUT_DIR"

################################################################################
# Logging Functions
################################################################################

log_to_file() {
    local file="$1"
    shift
    echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] $*" >> "$file"
}

log_section_to_file() {
    local file="$1"
    local title="$2"
    {
        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") - $title"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    } >> "$file"
}

log_info() {
    echo -e "${BLUE}[INFO]${NC} $*"
    log_to_file "$SUMMARY_LOG" "[INFO] $*"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $*"
    log_to_file "$SUMMARY_LOG" "[SUCCESS] $*"
}

log_error() {
    echo -e "${RED}[✗]${NC} $*"
    log_to_file "$SUMMARY_LOG" "[ERROR] $*"
}

log_warning() {
    echo -e "${YELLOW}[⚠]${NC} $*"
    log_to_file "$SUMMARY_LOG" "[WARNING] $*"
}

################################################################################
# Service Log Capture
################################################################################

capture_docker_logs() {
    log_info "Capturing Docker service logs..."

    local containers=(
        "hdim-demo-gateway:$GATEWAY_LOG"
        "hdim-demo-fhir:$FHIR_LOG"
        "hdim-demo-patient:$PATIENT_LOG"
        "hdim-demo-quality-measure:$QUALITY_LOG"
        "hdim-demo-care-gap:$CARE_GAP_LOG"
        "hdim-demo-postgres:$POSTGRES_LOG"
    )

    for container_config in "${containers[@]}"; do
        local container_name="${container_config%%:*}"
        local log_file="${container_config##*:}"

        if docker ps --filter "name=$container_name" --format "{{.Names}}" | grep -q "$container_name"; then
            log_section_to_file "$log_file" "Docker Logs: $container_name"
            docker logs "$container_name" 2>&1 >> "$log_file" || true
            log_success "Captured logs from $container_name"
        else
            log_warning "Container $container_name not running, skipping log capture"
        fi
    done
}

capture_gateway_auth_logs() {
    log_info "Extracting authentication-specific logs from gateway..."

    if [[ -f "$GATEWAY_LOG" ]]; then
        {
            echo "═════════════════════════════════════════════════════════════════════"
            echo "Gateway Authentication Events"
            echo "═════════════════════════════════════════════════════════════════════"
            echo ""
            echo "JWT Validation Events:"
            grep -i "jwt\|authentication\|token" "$GATEWAY_LOG" || echo "No JWT events found"
            echo ""
            echo "Authentication Errors:"
            grep -i "error\|exception" "$GATEWAY_LOG" || echo "No errors found"
            echo ""
            echo "User Authentication:"
            grep -i "user\|login\|auth.*success" "$GATEWAY_LOG" || echo "No user auth events found"
        } >> "$GATEWAY_LOG.analysis"

        log_success "Generated gateway authentication analysis"
    fi
}

################################################################################
# API Request Logging
################################################################################

log_api_request() {
    local method="$1"
    local path="$2"
    local http_code="$3"
    local response_time="$4"

    log_to_file "$API_LOG" "$method $path - HTTP $http_code (${response_time}ms)"
}

################################################################################
# Test Execution with Logging
################################################################################

run_tests_with_logging() {
    log_section_to_file "$TEST_LOG" "Authentication Test Suite Execution"
    log_info "Running authentication tests..."

    local test_script="$PROJECT_ROOT/scripts/test-authentication-flow.sh"

    if [[ ! -f "$test_script" ]]; then
        log_error "Test script not found: $test_script"
        return 1
    fi

    # Run tests and capture output
    {
        VERBOSE=1 bash "$test_script" 2>&1
    } | tee -a "$TEST_LOG" || true

    log_success "Tests completed"
}

################################################################################
# Connectivity Tests with Logging
################################################################################

test_services_with_logging() {
    log_section_to_file "$API_LOG" "Service Connectivity Tests"

    local services=(
        "http://localhost:4200:Clinical Portal"
        "http://localhost:8080:Gateway Service"
        "http://localhost:8085:FHIR Service"
        "http://localhost:8084:Patient Service"
        "http://localhost:8087:Quality Measure Service"
        "http://localhost:8086:Care Gap Service"
    )

    for service_config in "${services[@]}"; do
        local url="${service_config%%:*}"
        local name="${service_config##*:}"

        {
            echo "Testing: $name"
            echo "URL: $url"
            echo "Timestamp: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
        } >> "$API_LOG"

        local http_code=$(curl -s -w "%{http_code}" -o /dev/null "$url" 2>/dev/null || echo "000")

        if [[ "$http_code" == "200" ]] || [[ "$http_code" == "302" ]]; then
            log_api_request "GET" "$url" "$http_code" "0"
            log_success "Service $name responding (HTTP $http_code)"
        else
            log_api_request "GET" "$url" "$http_code" "0"
            log_warning "Service $name not responding (HTTP $http_code)"
        fi

        echo "" >> "$API_LOG"
    done
}

################################################################################
# Authentication Flow with Detailed Logging
################################################################################

test_login_flow_with_logging() {
    log_section_to_file "$API_LOG" "Login Flow Test"

    local email="demo_admin@hdim.ai"
    local password="demo123"
    local url="http://localhost:8080/api/v1/auth/login"

    log_info "Testing login flow for $email..."

    {
        echo "Login Request:"
        echo "  URL: $url"
        echo "  Method: POST"
        echo "  Content-Type: application/json"
        echo "  Timestamp: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
        echo ""
        echo "Request Body:"
        echo "  {\"email\":\"$email\",\"password\":\"***\"}"
        echo ""
    } >> "$API_LOG"

    local response_file="/tmp/login_response_$(date +%s).json"
    local start_time=$(date +%s%N | cut -b1-13)

    local http_code=$(curl -s -w "%{http_code}" -X POST "$url" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$email\",\"password\":\"$password\"}" \
        -o "$response_file" 2>/dev/null)

    local end_time=$(date +%s%N | cut -b1-13)
    local response_time=$((end_time - start_time))

    {
        echo "Response:"
        echo "  HTTP Code: $http_code"
        echo "  Response Time: ${response_time}ms"
        echo ""
        echo "Response Body:"
        if [[ -f "$response_file" ]]; then
            cat "$response_file" | jq '.' 2>/dev/null || cat "$response_file"
        fi
        echo ""
    } >> "$API_LOG"

    log_api_request "POST" "$url" "$http_code" "$response_time"

    if [[ "$http_code" == "200" ]]; then
        log_success "Login successful (HTTP $http_code, ${response_time}ms)"
    else
        log_error "Login failed (HTTP $http_code)"
    fi

    rm -f "$response_file"
}

################################################################################
# Report Generation
################################################################################

generate_log_summary() {
    log_section_to_file "$SUMMARY_LOG" "Log Summary"

    {
        echo ""
        echo "Log Files Generated:"
        echo "  • Test Results: $(du -h "$TEST_LOG" 2>/dev/null | awk '{print $1}' || echo 'N/A')"
        echo "  • Gateway Logs: $(du -h "$GATEWAY_LOG" 2>/dev/null | awk '{print $1}' || echo 'N/A')"
        echo "  • FHIR Logs: $(du -h "$FHIR_LOG" 2>/dev/null | awk '{print $1}' || echo 'N/A')"
        echo "  • Patient Logs: $(du -h "$PATIENT_LOG" 2>/dev/null | awk '{print $1}' || echo 'N/A')"
        echo "  • Quality Measure Logs: $(du -h "$QUALITY_LOG" 2>/dev/null | awk '{print $1}' || echo 'N/A')"
        echo "  • Care Gap Logs: $(du -h "$CARE_GAP_LOG" 2>/dev/null | awk '{print $1}' || echo 'N/A')"
        echo "  • API Requests: $(du -h "$API_LOG" 2>/dev/null | awk '{print $1}' || echo 'N/A')"
        echo ""
        echo "Output Directory: $OUTPUT_DIR"
        echo ""
    } >> "$SUMMARY_LOG"

    # Count test results
    local passed=$(grep -c "✓.*PASS" "$TEST_LOG" 2>/dev/null || echo "0")
    local failed=$(grep -c "✗.*FAIL" "$TEST_LOG" 2>/dev/null || echo "0")
    local skipped=$(grep -c "⊘.*SKIP" "$TEST_LOG" 2>/dev/null || echo "0")

    {
        echo "Test Summary:"
        echo "  Passed:  $passed"
        echo "  Failed:  $failed"
        echo "  Skipped: $skipped"
        echo ""
    } >> "$SUMMARY_LOG"

    # Count authentication events
    local auth_events=$(grep -c "jwt\|authentication\|token" "$GATEWAY_LOG" 2>/dev/null || echo "0")

    {
        echo "Gateway Authentication Events: $auth_events"
        echo ""
    } >> "$SUMMARY_LOG"

    # Count errors
    local gateway_errors=$(grep -c "ERROR\|Exception" "$GATEWAY_LOG" 2>/dev/null || echo "0")
    local fhir_errors=$(grep -c "ERROR\|Exception" "$FHIR_LOG" 2>/dev/null || echo "0")
    local patient_errors=$(grep -c "ERROR\|Exception" "$PATIENT_LOG" 2>/dev/null || echo "0")

    {
        echo "Service Errors:"
        echo "  Gateway: $gateway_errors"
        echo "  FHIR: $fhir_errors"
        echo "  Patient: $patient_errors"
        echo ""
    } >> "$SUMMARY_LOG"
}

################################################################################
# Main Execution
################################################################################

main() {
    echo ""
    echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║  HDIM Authentication Testing with Comprehensive Logging        ║${NC}"
    echo -e "${CYAN}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""

    log_info "Output directory: $OUTPUT_DIR"
    log_info "Starting comprehensive authentication tests with logging..."
    echo ""

    # Run all tests and capture logs
    test_services_with_logging
    run_tests_with_logging
    test_login_flow_with_logging
    capture_docker_logs
    capture_gateway_auth_logs
    generate_log_summary

    # Display summary
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}Test Execution Complete${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo ""

    cat "$SUMMARY_LOG"

    echo ""
    echo -e "${BLUE}Generated Log Files:${NC}"
    ls -lh "$OUTPUT_DIR"/
    echo ""
    echo -e "${GREEN}View logs with:${NC}"
    echo "  • Full summary: cat $SUMMARY_LOG"
    echo "  • Test results: cat $TEST_LOG"
    echo "  • Gateway logs: cat $GATEWAY_LOG"
    echo "  • API requests: cat $API_LOG"
    echo ""
}

# Run main function
main "$@"
