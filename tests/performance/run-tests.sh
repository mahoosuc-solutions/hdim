#!/bin/bash

#
# Performance Test Runner
# Executes all k6 performance and load tests for HDIM platform
#
# Usage:
#   ./run-tests.sh              # Run all tests
#   ./run-tests.sh api          # Run API performance tests only
#   ./run-tests.sh load         # Run load tests only
#   ./run-tests.sh stress       # Run stress test only
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
REPORTS_DIR="$(pwd)/reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Create reports directory
mkdir -p "$REPORTS_DIR"

echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║           HDIM Performance Testing Suite                    ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Gateway URL:${NC} $GATEWAY_URL"
echo -e "${YELLOW}Reports Dir:${NC} $REPORTS_DIR"
echo -e "${YELLOW}Timestamp:${NC} $TIMESTAMP"
echo ""

# Function to check if gateway is healthy
check_gateway() {
    echo -e "${BLUE}🔍 Checking gateway health...${NC}"

    if ! curl -s -f "$GATEWAY_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${RED}❌ Gateway is not responding at $GATEWAY_URL${NC}"
        echo -e "${YELLOW}💡 Start services with: docker compose up -d${NC}"
        exit 1
    fi

    echo -e "${GREEN}✅ Gateway is healthy${NC}"
    echo ""
}

# Function to run a k6 test
run_test() {
    local test_file=$1
    local test_name=$2
    local report_file="${REPORTS_DIR}/${test_name}_${TIMESTAMP}.json"

    echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  Running: ${test_name}${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""

    if docker run --rm -i --network=host \
        -e GATEWAY_URL="$GATEWAY_URL" \
        -v "$REPORTS_DIR:/reports" \
        grafana/k6 run --out json="/reports/$(basename $report_file)" - < "$test_file"; then
        echo ""
        echo -e "${GREEN}✅ $test_name completed successfully${NC}"
        echo -e "${YELLOW}📊 Report: $report_file${NC}"
        echo ""
        return 0
    else
        echo ""
        echo -e "${RED}❌ $test_name failed${NC}"
        echo ""
        return 1
    fi
}

# Function to run API performance tests
run_api_tests() {
    echo -e "${BLUE}🚀 Running API Performance Tests${NC}"
    echo ""

    run_test "tests/performance/api-gateway-performance.js" "api-gateway-performance"
}

# Function to run load tests
run_load_tests() {
    echo -e "${BLUE}🚀 Running Load Tests${NC}"
    echo ""

    run_test "tests/performance/load-test-normal.js" "load-test-normal-100users"
    run_test "tests/performance/load-test-stress.js" "load-test-stress-1500users"
}

# Function to run stress test
run_stress_test() {
    echo -e "${BLUE}🚀 Running Stress Test${NC}"
    echo ""

    run_test "tests/performance/load-test-stress.js" "load-test-stress-1500users"
}

# Function to generate summary report
generate_summary() {
    echo ""
    echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  Test Execution Summary                                     ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""

    echo -e "${YELLOW}Reports generated in:${NC} $REPORTS_DIR"
    echo ""

    ls -lh "$REPORTS_DIR"/*.json 2>/dev/null || echo "No reports found"

    echo ""
    echo -e "${GREEN}✅ Performance testing complete!${NC}"
    echo ""
    echo -e "${YELLOW}📖 View detailed results in JSON reports${NC}"
    echo -e "${YELLOW}💡 Tip: Use 'jq' to parse JSON reports${NC}"
    echo -e "${YELLOW}   Example: jq '.test_name, .response_times' $REPORTS_DIR/api-gateway-performance_*.json${NC}"
    echo ""
}

# Main execution
main() {
    local test_type="${1:-all}"

    # Check gateway health first
    check_gateway

    # Run tests based on argument
    case "$test_type" in
        api)
            run_api_tests
            ;;
        load)
            run_load_tests
            ;;
        stress)
            run_stress_test
            ;;
        all)
            run_api_tests
            run_load_tests
            ;;
        *)
            echo -e "${RED}❌ Unknown test type: $test_type${NC}"
            echo ""
            echo "Usage: $0 [api|load|stress|all]"
            echo ""
            echo "Options:"
            echo "  api      - Run API performance tests only"
            echo "  load     - Run load tests (100 users + stress test)"
            echo "  stress   - Run stress test only (1500 users)"
            echo "  all      - Run all tests (default)"
            echo ""
            exit 1
            ;;
    esac

    # Generate summary
    generate_summary
}

# Execute main function
main "$@"
