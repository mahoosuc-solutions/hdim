#!/bin/bash

# CMS Connector Service - Simple Load Testing Script (Bash + curl)
# Alternative to JMeter for quick load testing without dependencies

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8081}"
RESULTS_DIR="results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
TEST_RESULTS_DIR="${RESULTS_DIR}/${TIMESTAMP}"

# Ensure curl is available
if ! command -v curl &> /dev/null; then
  echo -e "${RED}curl not found. Please install curl${NC}"
  exit 1
fi

# Create results directory
mkdir -p "$TEST_RESULTS_DIR"

# Health check function
check_service_health() {
  echo -e "\n${BLUE}=== Health Check ===${NC}"
  if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Service is healthy${NC}"
    return 0
  else
    echo -e "${RED}✗ Service is not responding at $BASE_URL${NC}"
    return 1
  fi
}

# Run load test scenario using curl with parallel requests
run_load_test() {
  local scenario_name=$1
  local num_users=$2
  local duration=$3
  local results_file="${TEST_RESULTS_DIR}/${scenario_name}.csv"

  echo -e "\n${BLUE}=== Running Load Test: $scenario_name ===${NC}"
  echo "Users: $num_users | Duration: ${duration}s"
  echo "Results: $results_file"

  # Initialize results file
  echo "timestamp,url,response_time_ms,http_code,status" > "$results_file"

  local start_time=$(date +%s%N)
  local end_time=$((start_time + duration * 1000000000))
  local request_count=0
  local success_count=0
  local error_count=0
  local total_response_time=0
  local response_times=()

  # Function to make a single request and record result
  make_request() {
    local req_start=$(date +%s%N)

    # Make request and capture response
    local response=$(curl -s -w "\n%{http_code}\n%{time_total}" "$BASE_URL/actuator/health" 2>&1)
    local http_code=$(echo "$response" | tail -1)
    local time_total=$(echo "$response" | tail -2 | head -1)
    local response_time_ms=$(echo "$time_total * 1000" | bc 2>/dev/null || echo "0")

    local req_time=$(date +%s%N)
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    # Record result
    if [ "$http_code" = "200" ]; then
      echo "$timestamp,$BASE_URL/actuator/health,$response_time_ms,$http_code,success" >> "$results_file"
      ((success_count++))
    else
      echo "$timestamp,$BASE_URL/actuator/health,$response_time_ms,$http_code,error" >> "$results_file"
      ((error_count++))
    fi

    ((request_count++))
    response_times+=("$response_time_ms")
  }

  # Generate load: simulate concurrent users
  echo "Sending requests..."
  for ((i = 0; i < num_users; i++)); do
    # Spawn requests in background, but limit concurrent processes
    make_request &

    # Limit concurrent processes to avoid overwhelming the system
    if (( (i + 1) % 10 == 0 )); then
      wait -n 2>/dev/null || true
    fi

    # Check if we've exceeded duration
    current_time=$(date +%s%N)
    if [ "$current_time" -ge "$end_time" ]; then
      break
    fi

    # Small delay between request batches
    sleep 0.1
  done

  # Wait for all background jobs to complete
  wait

  # Analyze results
  analyze_results "$scenario_name" "$results_file" "$request_count" "$success_count" "$error_count"
}

# Analyze and report results
analyze_results() {
  local scenario=$1
  local results_file=$2
  local total_requests=$3
  local success=$4
  local errors=$5

  echo -e "\n${YELLOW}=== Results: $scenario ===${NC}"
  echo "Total Requests: $total_requests"
  echo -e "Successful: ${GREEN}$success${NC}"
  echo -e "Errors: ${RED}$errors${NC}"

  if [ "$total_requests" -gt 0 ]; then
    local error_rate=$(echo "scale=2; $errors * 100 / $total_requests" | bc 2>/dev/null || echo "0")
    echo "Error Rate: ${error_rate}%"
  fi

  # Extract response times (skip header)
  local response_times=$(tail -n +2 "$results_file" | awk -F, '{print $3}' | sort -n)

  if [ ! -z "$response_times" ]; then
    local count=$(echo "$response_times" | wc -l)
    local min=$(echo "$response_times" | head -1)
    local max=$(echo "$response_times" | tail -1)
    local avg=$(echo "$response_times" | awk '{sum+=$1} END {print int(sum/NR)}')

    # Calculate percentiles
    local p50_line=$(echo "scale=0; $count / 2" | bc)
    local p95_line=$(echo "scale=0; $count * 95 / 100" | bc)
    local p99_line=$(echo "scale=0; $count * 99 / 100" | bc)

    local p50=$(echo "$response_times" | sed -n "${p50_line}p")
    local p95=$(echo "$response_times" | sed -n "${p95_line}p" || echo "N/A")
    local p99=$(echo "$response_times" | sed -n "${p99_line}p" || echo "N/A")

    echo -e "\n${YELLOW}Response Time Analysis (ms):${NC}"
    echo "Min: $min"
    echo "Avg: $avg"
    echo "Max: $max"
    echo "p50: $p50"
    echo "p95: $p95"
    echo "p99: $p99"

    # Performance validation
    if [ "${p95:-999}" -lt 500 ] 2>/dev/null; then
      echo -e "${GREEN}✓ Performance target met (p95 < 500ms)${NC}"
    else
      echo -e "${YELLOW}⚠ Performance target not met (p95 >= 500ms)${NC}"
    fi
  fi

  echo -e "Results saved to: ${BLUE}$results_file${NC}\n"
}

# Main execution
main() {
  echo -e "${BLUE}╔════════════════════════════════════════════════╗${NC}"
  echo -e "${BLUE}║  CMS Connector Service - Load Test (curl)     ║${NC}"
  echo -e "${BLUE}╚════════════════════════════════════════════════╝${NC}"
  echo "Base URL: $BASE_URL"
  echo "Results Directory: $TEST_RESULTS_DIR"

  # Pre-flight checks
  if ! check_service_health; then
    echo -e "${YELLOW}Note: Service may still be starting up. Retrying...${NC}"
    sleep 5
    if ! check_service_health; then
      echo -e "${RED}Service is not available. Start it with: docker-compose -f docker-compose.dev.yml up -d${NC}"
      exit 1
    fi
  fi

  # Run test scenarios
  echo -e "\n${BLUE}Running load test scenarios...${NC}"

  # Scenario 1: Baseline (10 concurrent requests)
  run_load_test "baseline" 10 10

  # Scenario 2: Normal Load (50 concurrent requests)
  run_load_test "normal_load" 50 15

  # Scenario 3: Peak Load (100 concurrent requests)
  run_load_test "peak_load" 100 20

  # Generate summary report
  echo -e "\n${GREEN}╔════════════════════════════════════════════════╗${NC}"
  echo -e "${GREEN}║  Load Testing Complete                         ║${NC}"
  echo -e "${GREEN}╚════════════════════════════════════════════════╝${NC}"

  # Create summary markdown report
  local report_file="${TEST_RESULTS_DIR}/REPORT.md"
  cat > "$report_file" << 'EOF'
# Load Testing Report

## Overview
Load testing conducted using curl with parallel requests.

## Test Scenarios
1. **Baseline**: 10 concurrent users over 10 seconds
2. **Normal Load**: 50 concurrent users over 15 seconds
3. **Peak Load**: 100 concurrent users over 20 seconds

## Performance Targets
- p95 latency: < 500ms
- Error rate: < 1%
- Minimum throughput: > 100 req/s

## Results Analysis
See individual CSV files in this directory for detailed metrics.

### Key Findings
- Review response time trends across scenarios
- Identify any performance degradation at higher loads
- Check for error spikes under peak conditions

## Recommendations
1. If p95 > 500ms at peak load, consider:
   - Connection pool optimization
   - Query optimization
   - Caching improvements

2. If errors appear, check:
   - Service logs for exceptions
   - Database connection limits
   - Memory and CPU utilization

3. Monitor resource usage:
   - Database connections
   - Memory consumption
   - CPU utilization

## Next Steps
- Review CHAOS test results (run-chaos-tests.sh)
- Complete OWASP security validation (tests/security/OWASP-TOP-10-CHECKLIST.md)
- Implement optimizations based on findings
EOF

  echo -e "${GREEN}Summary report: ${BLUE}$report_file${NC}"
  cat "$report_file"

  echo -e "\n${BLUE}Results directory: ${YELLOW}$TEST_RESULTS_DIR${NC}"
}

# Run main function
main "$@"
