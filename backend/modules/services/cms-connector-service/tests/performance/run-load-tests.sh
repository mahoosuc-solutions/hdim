#!/bin/bash

# CMS Connector Service - Load Testing Script
# Runs all load test scenarios and generates reports

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8081}"
JMETER_HOME="${JMETER_HOME:-$(which jmeter)}"
RESULTS_DIR="results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
TEST_RESULTS_DIR="${RESULTS_DIR}/${TIMESTAMP}"

# Ensure JMeter is available
if [ ! -x "$JMETER_HOME" ] && ! command -v jmeter &> /dev/null; then
  echo -e "${RED}JMeter not found. Please install JMeter or set JMETER_HOME${NC}"
  echo "Install: brew install jmeter"
  exit 1
fi

# Create results directory
mkdir -p "$TEST_RESULTS_DIR"

# Function to run a test scenario
run_test_scenario() {
  local scenario_name=$1
  local threads=$2
  local ramp_time=$3
  local duration=$4
  
  echo -e "\n${BLUE}=== Running: $scenario_name ===${NC}"
  echo "Users: $threads | Ramp Time: ${ramp_time}s | Duration: ${duration}s"
  
  local jmx_file="tests/performance/cms-connector-load-test.jmx"
  local results_file="${TEST_RESULTS_DIR}/${scenario_name}.csv"
  local log_file="${TEST_RESULTS_DIR}/${scenario_name}.log"
  
  # Run JMeter test
  jmeter -n \
    -t "$jmx_file" \
    -l "$results_file" \
    -j "$log_file" \
    -J THREADS="$threads" \
    -J RAMP_TIME="$ramp_time" \
    -J DURATION="$duration" \
    -J BASE_URL="$BASE_URL" \
    -q
  
  if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Test completed: $scenario_name${NC}"
  else
    echo -e "${RED}✗ Test failed: $scenario_name${NC}"
    exit 1
  fi
}

# Function to analyze results
analyze_results() {
  local results_file=$1
  local scenario_name=$2
  
  echo -e "\n${BLUE}=== Analysis: $scenario_name ===${NC}"
  
  # Count samples
  local total_samples=$(tail -n +2 "$results_file" 2>/dev/null | wc -l)
  echo "Total Samples: $total_samples"
  
  # Count errors
  local errors=$(tail -n +2 "$results_file" 2>/dev/null | awk -F',' '$7 ~ /false/ {print}' | wc -l)
  local error_rate=$( [ $total_samples -gt 0 ] && echo "scale=2; $errors * 100 / $total_samples" | bc || echo "0" )
  echo "Errors: $errors (${error_rate}%)"
  
  # Calculate response times (column 2 is elapsed time in milliseconds)
  if command -v awk &> /dev/null; then
    awk -F',' '
      NR > 1 {
        times[NR] = $2
        sum += $2
        if ($2 > max) max = $2
        if ($2 < min || NR == 2) min = $2
      }
      END {
        if (NR > 1) {
          avg = sum / (NR - 1)
          printf "Response Times (ms):\n"
          printf "  Min: %.0f\n", min
          printf "  Avg: %.0f\n", avg
          printf "  Max: %.0f\n", max
          
          # Calculate percentiles (simplified)
          n = NR - 1
          p95_idx = int(n * 0.95)
          p99_idx = int(n * 0.99)
          
          asort(times)
          if (p95_idx > 0 && p95_idx <= n) {
            printf "  P95: %.0f\n", times[p95_idx]
          }
          if (p99_idx > 0 && p99_idx <= n) {
            printf "  P99: %.0f\n", times[p99_idx]
          }
        }
      }
    ' "$results_file"
  fi
  
  # Calculate throughput
  local duration_seconds=$(tail -n +2 "$results_file" 2>/dev/null | awk -F',' '{s+=$1; if(NR==1)first=$1; last=$1} END {if(NR>1) print int((last-first)/1000); else print 0}')
  if [ "$duration_seconds" -gt 0 ]; then
    local throughput=$(echo "scale=2; $total_samples / $duration_seconds" | bc 2>/dev/null || echo "N/A")
    echo "Throughput: ${throughput} req/s"
  fi
}

# Pre-flight checks
echo -e "${BLUE}=== Pre-Test Checks ===${NC}"

echo -n "Checking application health... "
if curl -s "$BASE_URL/api/v1/actuator/health" | grep -q "UP\|\"status\":\"UP\""; then
  echo -e "${GREEN}✓ OK${NC}"
else
  echo -e "${RED}✗ Application not responding${NC}"
  echo "Start the application with: ./docker-run.sh dev up"
  exit 1
fi

echo -n "Checking database... "
if curl -s "$BASE_URL/api/v1/actuator/health" | grep -q "database"; then
  echo -e "${GREEN}✓ OK${NC}"
else
  echo -e "${YELLOW}⚠ Database info not available${NC}"
fi

# Create results directory
echo "Creating results directory: $TEST_RESULTS_DIR"

# Run test scenarios
echo -e "\n${BLUE}=== LOAD TEST SCENARIOS ===${NC}"

# Scenario 1: Baseline
run_test_scenario "baseline-load" 10 10 300

# Scenario 2: Normal Load
run_test_scenario "normal-load" 100 60 600

# Scenario 3: Peak Load
run_test_scenario "peak-load" 500 120 900

# Generate analysis report
echo -e "\n${BLUE}=== ANALYSIS REPORT ===${NC}"

analyze_results "${TEST_RESULTS_DIR}/baseline-load.csv" "Baseline Load (10 users)"
analyze_results "${TEST_RESULTS_DIR}/normal-load.csv" "Normal Load (100 users)"
analyze_results "${TEST_RESULTS_DIR}/peak-load.csv" "Peak Load (500 users)"

# Generate summary
cat > "${TEST_RESULTS_DIR}/REPORT.md" <<EOF
# Load Test Report
**Date**: $(date)
**Baseline URL**: $BASE_URL
**Test Results Directory**: $TEST_RESULTS_DIR

## Scenarios Tested

### Scenario 1: Baseline Load
- Users: 10
- Ramp Time: 10s
- Duration: 300s (5 minutes)
- Results: [See baseline-load.csv]

### Scenario 2: Normal Load
- Users: 100
- Ramp Time: 60s
- Duration: 600s (10 minutes)
- Results: [See normal-load.csv]

### Scenario 3: Peak Load
- Users: 500
- Ramp Time: 120s
- Duration: 900s (15 minutes)
- Results: [See peak-load.csv]

## Key Metrics

See analysis output above.

## Recommendations

1. Check bottlenecks identified
2. Review error logs for any issues
3. Compare metrics to targets in load-test-plan.md
4. Document optimization opportunities
5. Re-run tests after optimizations
EOF

echo -e "\n${GREEN}=== TESTS COMPLETED ===${NC}"
echo "Results saved to: $TEST_RESULTS_DIR"
echo "Report: ${TEST_RESULTS_DIR}/REPORT.md"
echo ""
echo "Next steps:"
echo "1. Review results: cat ${TEST_RESULTS_DIR}/REPORT.md"
echo "2. Analyze slow endpoints"
echo "3. Check resource utilization (CPU, memory, DB connections)"
echo "4. Implement optimizations"
echo "5. Re-run tests to validate improvements"
