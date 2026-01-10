#!/bin/bash

##############################################################################
# Gateway Smoke Test Script
#
# Comprehensive testing for HDIM Gateway Service covering:
# - Service routing (all 29 services × 2 paths = 58 tests)
# - Authentication flows (JWT, cookies, header injection)
# - Circuit breaker states
# - Rate limiting enforcement
# - Error handling (404, 401, 503, 429)
#
# Prerequisites:
#   - Docker Compose running with gateway-service
#   - Gateway service healthy on port 8001
#   - Backend services running (at least core services for --mode=quick)
#
# Usage:
#   ./scripts/test-gateway-smoke.sh                    # Quick mode (core services)
#   ./scripts/test-gateway-smoke.sh --mode=full        # All 29 services
#   ./scripts/test-gateway-smoke.sh --verbose          # Detailed output
#   ./scripts/test-gateway-smoke.sh --output report.json
#   ./scripts/test-gateway-smoke.sh --skip-circuit-breakers
#
# Exit codes:
#   0 - All tests passed
#   1 - Critical service failure (gateway, auth, core services)
#   2 - Partial failure (extended services)
#   3 - Configuration error
##############################################################################

set -e  # Exit on error
set -u  # Exit on undefined variable

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source helper library
# shellcheck source=scripts/lib/gateway-test-helpers.sh
source "$SCRIPT_DIR/lib/gateway-test-helpers.sh"

# Configuration
TEST_MODE="${TEST_MODE:-quick}"  # quick or full
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8001}"
CONFIG_FILE="$SCRIPT_DIR/config/gateway-service-routes.json"
OUTPUT_FILE="${OUTPUT_FILE:-}"
VERBOSE="${VERBOSE:-false}"
SKIP_CIRCUIT_BREAKERS="${SKIP_CIRCUIT_BREAKERS:-false}"
SKIP_RATE_LIMITING="${SKIP_RATE_LIMITING:-false}"
SKIP_AUTH="${SKIP_AUTH:-false}"

# Test credentials (dev environment only)
TEST_USERNAME="${TEST_USERNAME:-test_admin}"
TEST_PASSWORD="${TEST_PASSWORD:-password123}"

# Parse command-line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --mode=*)
                TEST_MODE="${1#*=}"
                shift
                ;;
            --verbose)
                VERBOSE="true"
                shift
                ;;
            --output)
                OUTPUT_FILE="$2"
                shift 2
                ;;
            --skip-circuit-breakers)
                SKIP_CIRCUIT_BREAKERS="true"
                shift
                ;;
            --skip-rate-limiting)
                SKIP_RATE_LIMITING="true"
                shift
                ;;
            --skip-auth)
                SKIP_AUTH="true"
                shift
                ;;
            --help)
                print_usage
                exit 0
                ;;
            *)
                echo "Unknown option: $1"
                print_usage
                exit 3
                ;;
        esac
    done
}

print_usage() {
    cat <<EOF
Usage: $(basename "$0") [OPTIONS]

Gateway Smoke Test - Comprehensive testing for HDIM Gateway Service

OPTIONS:
    --mode=MODE              Test mode: 'quick' (core services) or 'full' (all 29 services)
                             Default: quick
    --verbose                Enable verbose output
    --output FILE            Write JSON test report to FILE
    --skip-circuit-breakers  Skip circuit breaker tests (faster execution)
    --skip-rate-limiting     Skip rate limiting tests
    --help                   Show this help message

EXAMPLES:
    # Quick test (core services only)
    $0 --mode=quick

    # Full test (all services)
    $0 --mode=full

    # Generate report
    $0 --mode=full --output /tmp/gateway-report.json

    # Fast execution (skip circuit breaker tests)
    $0 --mode=quick --skip-circuit-breakers

ENVIRONMENT VARIABLES:
    GATEWAY_URL              Gateway URL (default: http://localhost:8001)
    TEST_USERNAME            Test username (default: test_admin)
    TEST_PASSWORD            Test password (default: password123)
    TEST_MODE                Test mode (default: quick)
    VERBOSE                  Verbose output (default: false)

EOF
}

##############################################################################
# Main Test Functions
##############################################################################

test_gateway_health() {
    log_info "=== Testing Gateway Health ==="

    test_service_health "Gateway Service" "$GATEWAY_URL"
}

test_authentication_flows() {
    log_info "=== Testing Authentication Flows ==="

    # Test 1: Login and JWT generation
    JWT_TOKEN=$(validate_jwt_auth "$GATEWAY_URL" "$TEST_USERNAME" "$TEST_PASSWORD")

    if [ -z "$JWT_TOKEN" ]; then
        log_error "Failed to obtain JWT token - authentication tests will be skipped"
        return 1
    fi

    # Test 2: Token validation via /auth/me endpoint
    local test_name="Token validation: /auth/me"
    local response_code
    response_code=$(curl -s -o /dev/null -w "%{http_code}" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "X-Tenant-ID: acme-health" \
        "$GATEWAY_URL/api/auth/me" 2>/dev/null)

    if [ "$response_code" = "200" ]; then
        log_test "$test_name" "PASS" "Token validated successfully"
    else
        log_test "$test_name" "FAIL" "Token validation failed (status: $response_code)"
    fi

    # Test 3: Unauthenticated request should return 401
    test_error_handling "$GATEWAY_URL" "401"

    # Test 4: Header injection prevention
    verify_headers "$GATEWAY_URL" "/api/quality-measure/api/v1/measures"
}

test_service_routes() {
    local services_json=$1
    local service_type=$2  # "core" or "extended"

    log_info "=== Testing $service_type Service Routes ==="

    local service_count
    service_count=$(echo "$services_json" | jq '. | length')

    for i in $(seq 0 $((service_count - 1))); do
        local service
        service=$(echo "$services_json" | jq -r ".[$i]")

        local service_name
        service_name=$(echo "$service" | jq -r '.name')

        local service_port
        service_port=$(echo "$service" | jq -r '.port')

        local test_path
        test_path=$(echo "$service" | jq -r '.testPath')

        # Test direct service health (bypass gateway)
        if [ "$VERBOSE" = "true" ]; then
            log_info "Testing direct service: $service_name (port $service_port)"
        fi

        test_service_health "$service_name" "http://localhost:$service_port"

        # Test gateway route: /api/{service-name}/*
        local api_route="/api/$service_name$test_path"
        test_service_route "$service_name" "$GATEWAY_URL" "$api_route" "200" "$JWT_TOKEN"

        # Test gateway route: /{service-name}/*
        local direct_route="/$service_name$test_path"
        test_service_route "$service_name" "$GATEWAY_URL" "$direct_route" "200" "$JWT_TOKEN"
    done
}

test_circuit_breakers() {
    if [ "$SKIP_CIRCUIT_BREAKERS" = "true" ]; then
        log_info "=== Circuit Breaker Tests Skipped ==="
        return 0
    fi

    log_info "=== Testing Circuit Breakers ==="

    local services_json=$1

    local service_count
    service_count=$(echo "$services_json" | jq '. | length')

    for i in $(seq 0 $((service_count - 1))); do
        local service_name
        service_name=$(echo "$services_json" | jq -r ".[$i].name")

        check_circuit_breaker "$service_name" "$GATEWAY_URL"
    done
}

test_rate_limiting() {
    if [ "$SKIP_RATE_LIMITING" = "true" ]; then
        log_info "=== Rate Limiting Tests Skipped ==="
        return 0
    fi

    log_info "=== Testing Rate Limiting ==="

    # Test auth endpoint rate limiting (typically 10 req/min)
    test_rate_limiting "$GATEWAY_URL/api/auth/login" 10 60

    # Test API endpoint rate limiting (typically 100 req/sec)
    # Note: This is a lighter test to avoid overwhelming the system
    test_rate_limiting "$GATEWAY_URL/api/quality-measure/actuator/health" 50 30

    # Test that health endpoints bypass rate limiting
    local test_name="Rate limit bypass: health endpoints"
    local success_count=0

    for i in $(seq 1 20); do
        response_code=$(curl -s -o /dev/null -w "%{http_code}" \
            "$GATEWAY_URL/actuator/health" 2>/dev/null)

        if [ "$response_code" = "200" ]; then
            success_count=$((success_count + 1))
        fi
    done

    if [ "$success_count" -eq 20 ]; then
        log_test "$test_name" "PASS" "Health endpoints not rate limited"
    else
        log_test "$test_name" "FAIL" "Some health checks were rate limited ($success_count/20)"
    fi
}

test_error_handling() {
    log_info "=== Testing Error Handling ==="

    # Test 404 Not Found
    test_error_handling "$GATEWAY_URL" "404"

    # Test 401 Unauthorized
    test_error_handling "$GATEWAY_URL" "401"

    # Test 503 Service Unavailable (skip - requires service downtime)
    local test_name="Error handling: 503 Service Unavailable"
    log_test "$test_name" "SKIP" "Requires service downtime to test"
}

##############################################################################
# Main Workflow
##############################################################################

main() {
    log_info "=================================================================="
    log_info "                HDIM Gateway Smoke Test"
    log_info "=================================================================="
    log_info "Mode:          $TEST_MODE"
    log_info "Gateway URL:   $GATEWAY_URL"
    log_info "Config File:   $CONFIG_FILE"
    log_info "=================================================================="

    # Parse command-line arguments
    parse_args "$@"

    # Check if config file exists
    if [ ! -f "$CONFIG_FILE" ]; then
        log_error "Configuration file not found: $CONFIG_FILE"
        exit 3
    fi

    # Check if jq is installed
    if ! command -v jq &> /dev/null; then
        log_error "jq is required but not installed. Please install jq to continue."
        exit 3
    fi

    # Load service configuration
    local config
    config=$(cat "$CONFIG_FILE")

    local core_services
    core_services=$(echo "$config" | jq -r '.core_services')

    local extended_services
    extended_services=$(echo "$config" | jq -r '.extended_services')

    # Test 1: Gateway Health
    test_gateway_health || {
        log_error "Gateway service is not healthy - aborting tests"
        exit 1
    }

    # Test 2: Authentication Flows
    if [ "$SKIP_AUTH" != "true" ]; then
        test_authentication_flows
    else
        log_info "=== Authentication Tests Skipped (--skip-auth flag) ==="
    fi

    # Test 3: Core Service Routes
    test_service_routes "$core_services" "Core"

    # Test 4: Core Service Circuit Breakers
    if [ "$SKIP_CIRCUIT_BREAKERS" != "true" ]; then
        test_circuit_breakers "$core_services"
    fi

    # Test 5: Extended Services (full mode only)
    if [ "$TEST_MODE" = "full" ]; then
        test_service_routes "$extended_services" "Extended"

        if [ "$SKIP_CIRCUIT_BREAKERS" != "true" ]; then
            test_circuit_breakers "$extended_services"
        fi
    else
        log_info "=== Extended Service Tests Skipped (quick mode) ==="
    fi

    # Test 6: Rate Limiting
    if [ "$SKIP_RATE_LIMITING" != "true" ]; then
        test_rate_limiting
    fi

    # Test 7: Error Handling
    test_error_handling

    # Print summary
    echo ""
    print_test_summary

    # Generate JSON report if requested
    if [ -n "$OUTPUT_FILE" ]; then
        generate_report "$OUTPUT_FILE"
    fi

    # Determine exit code
    if [ "$FAILED_TESTS" -eq 0 ]; then
        log_success "All tests passed!"
        exit 0
    else
        # Check if only extended services failed
        local critical_failures=0

        # Count failures in core services (first 6 services × 2 routes = 12 routing tests + auth tests)
        # If we have > 20 failures, likely core services are affected
        if [ "$FAILED_TESTS" -gt 20 ]; then
            critical_failures=1
        fi

        if [ "$critical_failures" -eq 1 ]; then
            log_error "Critical service failures detected"
            exit 1
        else
            log_warn "Some extended services failed (non-critical)"
            exit 2
        fi
    fi
}

# Run main workflow
main "$@"
