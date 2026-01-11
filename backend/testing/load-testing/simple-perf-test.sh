#!/bin/bash

##############################################################################
# Simple Performance Testing Script (No Apache Bench Required)
#
# Uses curl and bash to perform basic performance testing when ab is not available
#
# Usage:
#   ./simple-perf-test.sh
##############################################################################

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
RESULTS_DIR="./results/simple_$(date +%Y%m%d_%H%M%S)"

mkdir -p "$RESULTS_DIR"

print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Test 1: Gateway Health Check Performance
test_health_endpoint() {
    print_header "Test 1: Health Endpoint Performance"

    local total_time=0
    local success_count=0
    local iterations=100

    print_info "Running $iterations health check requests..."

    for i in $(seq 1 $iterations); do
        local start=$(date +%s%N)
        local http_code=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY_URL/actuator/health")
        local end=$(date +%s%N)

        local duration=$((($end - $start) / 1000000)) # Convert to milliseconds
        total_time=$(($total_time + $duration))

        if [ "$http_code" = "200" ]; then
            success_count=$(($success_count + 1))
        fi

        # Progress indicator
        if [ $(($i % 10)) -eq 0 ]; then
            echo -n "."
        fi
    done
    echo ""

    local avg_time=$(($total_time / $iterations))
    local success_rate=$((($success_count * 100) / $iterations))

    print_success "Average response time: ${avg_time}ms"
    print_success "Success rate: ${success_rate}%"
    print_success "Total requests: $iterations"
    print_success "Successful: $success_count"

    echo "$avg_time" > "$RESULTS_DIR/health_avg_ms.txt"
    echo "$success_rate" > "$RESULTS_DIR/health_success_rate.txt"
}

# Test 2: Login Endpoint Performance
test_login_endpoint() {
    print_header "Test 2: Login Endpoint Performance"

    local iterations=50
    local total_time=0
    local success_count=0

    print_info "Running $iterations login requests..."

    # Create test payload
    local payload='{"username":"test_admin","password":"password123"}'

    for i in $(seq 1 $iterations); do
        local start=$(date +%s%N)
        local http_code=$(curl -s -o /dev/null -w "%{http_code}" \
            -X POST \
            -H "Content-Type: application/json" \
            -d "$payload" \
            "$GATEWAY_URL/api/v1/auth/login")
        local end=$(date +%s%N)

        local duration=$((($end - $start) / 1000000))
        total_time=$(($total_time + $duration))

        if [ "$http_code" = "200" ]; then
            success_count=$(($success_count + 1))
        fi

        if [ $(($i % 5)) -eq 0 ]; then
            echo -n "."
        fi
    done
    echo ""

    local avg_time=$(($total_time / $iterations))
    local success_rate=$((($success_count * 100) / $iterations))

    print_success "Average response time: ${avg_time}ms"
    print_success "Success rate: ${success_rate}%"
    print_success "Total requests: $iterations"

    echo "$avg_time" > "$RESULTS_DIR/login_avg_ms.txt"
    echo "$success_rate" > "$RESULTS_DIR/login_success_rate.txt"
}

# Test 3: Concurrent Request Simulation
test_concurrent_requests() {
    print_header "Test 3: Concurrent Request Simulation"

    print_info "Launching 10 concurrent health check requests..."

    local start=$(date +%s%N)

    for i in $(seq 1 10); do
        curl -s "$GATEWAY_URL/actuator/health" > /dev/null &
    done

    wait

    local end=$(date +%s%N)
    local duration=$((($end - $start) / 1000000))

    print_success "10 concurrent requests completed in ${duration}ms"
    print_success "Average time per request: $((duration / 10))ms (sequential would be slower)"

    echo "$duration" > "$RESULTS_DIR/concurrent_10_total_ms.txt"
}

# Generate Summary Report
generate_report() {
    print_header "Performance Test Summary"

    local report_file="$RESULTS_DIR/SUMMARY.md"

    cat > "$report_file" << EOF
# Simple Performance Test Results

**Test Date:** $(date)
**Gateway URL:** $GATEWAY_URL
**Environment:** Development

## Test Results

### 1. Health Endpoint Performance
- **Average Response Time:** $(cat $RESULTS_DIR/health_avg_ms.txt)ms
- **Success Rate:** $(cat $RESULTS_DIR/health_success_rate.txt)%
- **Requests:** 100

### 2. Login Endpoint Performance
- **Average Response Time:** $(cat $RESULTS_DIR/login_avg_ms.txt)ms
- **Success Rate:** $(cat $RESULTS_DIR/login_success_rate.txt)%
- **Requests:** 50

### 3. Concurrent Performance
- **10 Concurrent Requests:** $(cat $RESULTS_DIR/concurrent_10_total_ms.txt)ms total

## Analysis

**Health Endpoint:**
- Target: < 50ms
- Status: $([ $(cat $RESULTS_DIR/health_avg_ms.txt) -lt 50 ] && echo "✅ PASS" || echo "⚠️  SLOW")

**Login Endpoint:**
- Target: < 200ms
- Status: $([ $(cat $RESULTS_DIR/login_avg_ms.txt) -lt 200 ] && echo "✅ PASS" || echo "⚠️  SLOW")

## Recommendations

For comprehensive load testing, install Apache Bench:
\`\`\`bash
sudo apt-get install apache2-utils
./run-load-tests.sh all
\`\`\`

---
*Generated by simple-perf-test.sh*
EOF

    print_success "Report saved to: $report_file"
    echo ""
    cat "$report_file"
}

# Main execution
main() {
    print_header "HDIM Simple Performance Test Suite"
    print_info "Gateway: $GATEWAY_URL"
    print_info "Results: $RESULTS_DIR"

    # Verify gateway is up
    if ! curl -s "$GATEWAY_URL/actuator/health" | grep -q "UP"; then
        echo "ERROR: Gateway not responding or unhealthy"
        exit 1
    fi

    test_health_endpoint
    test_login_endpoint
    test_concurrent_requests
    generate_report

    print_header "Tests Complete!"
    print_success "Results directory: $RESULTS_DIR"
}

main
