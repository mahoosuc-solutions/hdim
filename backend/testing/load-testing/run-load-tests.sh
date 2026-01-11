#!/bin/bash

##############################################################################
# HDIM Load Testing Suite
#
# Uses Apache Bench (ab) to perform load testing on authentication endpoints
# and measure system performance under various load conditions.
#
# Prerequisites:
# - Apache Bench installed (apt-get install apache2-utils)
# - HDIM services running (docker compose up)
# - Test users created in database
#
# Usage:
#   ./run-load-tests.sh [test-name]
#
# Available tests:
#   - login         : Test login endpoint performance
#   - token-refresh : Test token refresh performance
#   - rate-limit    : Test rate limiting behavior
#   - concurrent    : Test concurrent user load
#   - all           : Run all tests
##############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
RESULTS_DIR="./results/$(date +%Y%m%d_%H%M%S)"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8001}"

# Test parameters
CONCURRENCY_LIGHT=10
CONCURRENCY_MEDIUM=50
CONCURRENCY_HEAVY=100
REQUESTS_LIGHT=100
REQUESTS_MEDIUM=1000
REQUESTS_HEAVY=10000

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

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

check_prerequisites() {
    print_header "Checking Prerequisites"

    # Check if ab is installed
    if ! command -v ab &> /dev/null; then
        print_error "Apache Bench (ab) not found"
        echo "Install with: sudo apt-get install apache2-utils"
        exit 1
    fi
    print_success "Apache Bench found: $(ab -V | head -1)"

    # Check if services are running
    if ! curl -s "$GATEWAY_URL/actuator/health" > /dev/null 2>&1; then
        print_error "Gateway service not responding at $GATEWAY_URL"
        echo "Start services with: docker compose up -d"
        exit 1
    fi
    print_success "Gateway service responding"

    # Check if jq is installed (for JSON processing)
    if ! command -v jq &> /dev/null; then
        print_info "jq not found (JSON processing will be limited)"
    else
        print_success "jq found for JSON processing"
    fi
}

##############################################################################
# Test 1: Login Endpoint Performance
##############################################################################

test_login_performance() {
    print_header "Test 1: Login Endpoint Performance"

    # Create test JSON payload
    LOGIN_PAYLOAD='{"username":"test_evaluator","password":"password123"}'
    echo "$LOGIN_PAYLOAD" > "$RESULTS_DIR/login_payload.json"

    print_info "Testing with light load ($CONCURRENCY_LIGHT concurrent, $REQUESTS_LIGHT total)"
    ab -n $REQUESTS_LIGHT -c $CONCURRENCY_LIGHT \
        -p "$RESULTS_DIR/login_payload.json" \
        -T "application/json" \
        -g "$RESULTS_DIR/login_light.tsv" \
        "$GATEWAY_URL/api/v1/auth/login" \
        > "$RESULTS_DIR/login_light.txt" 2>&1

    print_info "Testing with medium load ($CONCURRENCY_MEDIUM concurrent, $REQUESTS_MEDIUM total)"
    ab -n $REQUESTS_MEDIUM -c $CONCURRENCY_MEDIUM \
        -p "$RESULTS_DIR/login_payload.json" \
        -T "application/json" \
        -g "$RESULTS_DIR/login_medium.tsv" \
        "$GATEWAY_URL/api/v1/auth/login" \
        > "$RESULTS_DIR/login_medium.txt" 2>&1

    # Parse results
    print_success "Login performance test completed"
    echo ""
    echo "Light Load Results:"
    grep "Requests per second" "$RESULTS_DIR/login_light.txt"
    grep "Time per request" "$RESULTS_DIR/login_light.txt" | head -1
    echo ""
    echo "Medium Load Results:"
    grep "Requests per second" "$RESULTS_DIR/login_medium.txt"
    grep "Time per request" "$RESULTS_DIR/login_medium.txt" | head -1
}

##############################################################################
# Test 2: Token Refresh Performance
##############################################################################

test_token_refresh() {
    print_header "Test 2: Token Refresh Performance"

    # First, get a valid token
    print_info "Obtaining test token..."
    TOKEN_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"test_evaluator","password":"password123"}')

    if command -v jq &> /dev/null; then
        ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.accessToken')
        REFRESH_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.refreshToken')
    else
        print_error "Cannot extract tokens without jq"
        return 1
    fi

    if [ "$ACCESS_TOKEN" == "null" ] || [ -z "$ACCESS_TOKEN" ]; then
        print_error "Failed to obtain access token"
        return 1
    fi

    print_success "Token obtained"

    # Create refresh payload
    REFRESH_PAYLOAD="{\"refreshToken\":\"$REFRESH_TOKEN\"}"
    echo "$REFRESH_PAYLOAD" > "$RESULTS_DIR/refresh_payload.json"

    print_info "Testing token refresh with light load"
    ab -n $REQUESTS_LIGHT -c $CONCURRENCY_LIGHT \
        -p "$RESULTS_DIR/refresh_payload.json" \
        -T "application/json" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -g "$RESULTS_DIR/refresh_light.tsv" \
        "$GATEWAY_URL/api/v1/auth/refresh" \
        > "$RESULTS_DIR/refresh_light.txt" 2>&1

    print_success "Token refresh test completed"
    grep "Requests per second" "$RESULTS_DIR/refresh_light.txt"
    grep "Time per request" "$RESULTS_DIR/refresh_light.txt" | head -1
}

##############################################################################
# Test 3: Rate Limiting Behavior
##############################################################################

test_rate_limiting() {
    print_header "Test 3: Rate Limiting Behavior"

    LOGIN_PAYLOAD='{"username":"test_viewer","password":"password123"}'
    echo "$LOGIN_PAYLOAD" > "$RESULTS_DIR/ratelimit_payload.json"

    print_info "Sending rapid requests to trigger rate limiting..."
    print_info "Testing with 200 requests, 50 concurrent"

    ab -n 200 -c 50 \
        -p "$RESULTS_DIR/ratelimit_payload.json" \
        -T "application/json" \
        -g "$RESULTS_DIR/ratelimit.tsv" \
        "$GATEWAY_URL/api/v1/auth/login" \
        > "$RESULTS_DIR/ratelimit.txt" 2>&1

    print_success "Rate limiting test completed"

    # Check for 429 responses
    HTTP_429_COUNT=$(grep "429:" "$RESULTS_DIR/ratelimit.txt" | awk '{print $2}' || echo "0")
    HTTP_200_COUNT=$(grep "200:" "$RESULTS_DIR/ratelimit.txt" | awk '{print $2}' || echo "0")

    echo "HTTP 200 (Success): $HTTP_200_COUNT"
    echo "HTTP 429 (Rate Limited): $HTTP_429_COUNT"

    if [ "$HTTP_429_COUNT" -gt 0 ]; then
        print_success "Rate limiting is active"
    else
        print_info "No rate limiting observed (may need configuration)"
    fi
}

##############################################################################
# Test 4: Concurrent User Load
##############################################################################

test_concurrent_load() {
    print_header "Test 4: Concurrent User Load Simulation"

    print_info "Simulating 100 concurrent users with sustained load"

    LOGIN_PAYLOAD='{"username":"test_admin","password":"password123"}'
    echo "$LOGIN_PAYLOAD" > "$RESULTS_DIR/concurrent_payload.json"

    # Long-running test with many concurrent connections
    ab -n $REQUESTS_HEAVY -c $CONCURRENCY_HEAVY \
        -p "$RESULTS_DIR/concurrent_payload.json" \
        -T "application/json" \
        -g "$RESULTS_DIR/concurrent.tsv" \
        "$GATEWAY_URL/api/v1/auth/login" \
        > "$RESULTS_DIR/concurrent.txt" 2>&1

    print_success "Concurrent load test completed"
    echo ""
    grep "Requests per second" "$RESULTS_DIR/concurrent.txt"
    grep "Time per request" "$RESULTS_DIR/concurrent.txt"
    grep "Failed requests" "$RESULTS_DIR/concurrent.txt"
    grep "Transfer rate" "$RESULTS_DIR/concurrent.txt"
}

##############################################################################
# Test 5: System Health Under Load
##############################################################################

test_system_health() {
    print_header "Test 5: System Health Monitoring Under Load"

    print_info "Starting background load..."

    # Start continuous background load
    (
        while true; do
            curl -s -X POST "$GATEWAY_URL/api/v1/auth/login" \
                -H "Content-Type: application/json" \
                -d '{"username":"test_evaluator","password":"password123"}' \
                > /dev/null 2>&1
            sleep 0.1
        done
    ) &
    LOAD_PID=$!

    sleep 2
    print_info "Monitoring system health for 10 seconds..."

    for i in {1..10}; do
        HEALTH=$(curl -s "$GATEWAY_URL/actuator/health" || echo '{"status":"DOWN"}')
        STATUS=$(echo "$HEALTH" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
        echo "[$i/10] Health Status: $STATUS"
        sleep 1
    done

    # Stop background load
    kill $LOAD_PID 2>/dev/null || true

    print_success "Health monitoring completed"
}

##############################################################################
# Generate Summary Report
##############################################################################

generate_report() {
    print_header "Generating Performance Report"

    REPORT_FILE="$RESULTS_DIR/PERFORMANCE_REPORT.md"

    cat > "$REPORT_FILE" << EOF
# HDIM Load Testing Report

**Date**: $(date)
**Base URL**: $GATEWAY_URL
**Test Suite**: Authentication & Rate Limiting

## Test Results Summary

### 1. Login Endpoint Performance

#### Light Load ($REQUESTS_LIGHT requests, $CONCURRENCY_LIGHT concurrent)
\`\`\`
$(grep -A 2 "Requests per second" "$RESULTS_DIR/login_light.txt" || echo "No data")
\`\`\`

#### Medium Load ($REQUESTS_MEDIUM requests, $CONCURRENCY_MEDIUM concurrent)
\`\`\`
$(grep -A 2 "Requests per second" "$RESULTS_DIR/login_medium.txt" || echo "No data")
\`\`\`

### 2. Token Refresh Performance

\`\`\`
$(grep -A 2 "Requests per second" "$RESULTS_DIR/refresh_light.txt" || echo "No data")
\`\`\`

### 3. Rate Limiting

\`\`\`
$(grep "Complete requests" "$RESULTS_DIR/ratelimit.txt" || echo "No data")
$(grep "Failed requests" "$RESULTS_DIR/ratelimit.txt" || echo "No data")
$(grep -E "200:|429:" "$RESULTS_DIR/ratelimit.txt" || echo "No HTTP status data")
\`\`\`

### 4. Concurrent Load ($REQUESTS_HEAVY requests, $CONCURRENCY_HEAVY concurrent)

\`\`\`
$(grep -A 5 "Requests per second" "$RESULTS_DIR/concurrent.txt" || echo "No data")
\`\`\`

## Performance Thresholds

- **Target Login RPS**: > 100 requests/second
- **Target P95 Latency**: < 200ms
- **Target Success Rate**: > 99%
- **Rate Limiting**: Should engage at configured threshold

## Recommendations

1. Monitor authentication service CPU/memory during peak load
2. Consider Redis connection pool sizing based on concurrency
3. Implement circuit breakers for dependent services
4. Set up alerting for P95 latency > 500ms
5. Configure rate limiting tiers based on tenant plans

## Raw Data Files

All raw test data available in: \`$RESULTS_DIR/\`

- login_light.txt / login_medium.txt
- refresh_light.txt
- ratelimit.txt
- concurrent.txt
- *.tsv (gnuplot-compatible timing data)

EOF

    print_success "Report generated: $REPORT_FILE"
    cat "$REPORT_FILE"
}

##############################################################################
# Main Execution
##############################################################################

main() {
    TEST_NAME="${1:-all}"

    print_header "HDIM Load Testing Suite"
    print_info "Test: $TEST_NAME"
    print_info "Results directory: $RESULTS_DIR"

    check_prerequisites

    case "$TEST_NAME" in
        login)
            test_login_performance
            ;;
        token-refresh)
            test_token_refresh
            ;;
        rate-limit)
            test_rate_limiting
            ;;
        concurrent)
            test_concurrent_load
            ;;
        health)
            test_system_health
            ;;
        all)
            test_login_performance
            test_token_refresh
            test_rate_limiting
            test_concurrent_load
            test_system_health
            generate_report
            ;;
        *)
            print_error "Unknown test: $TEST_NAME"
            echo "Available tests: login, token-refresh, rate-limit, concurrent, health, all"
            exit 1
            ;;
    esac

    print_header "Test Suite Complete"
    print_success "Results saved to: $RESULTS_DIR"
}

main "$@"
