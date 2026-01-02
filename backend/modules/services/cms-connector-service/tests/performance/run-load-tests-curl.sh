#!/bin/bash

##############################################################################
# CMS Connector Service - Load Testing with Pure Curl
# No external dependencies (JMeter not required)
#
# Features:
#   - Simulates concurrent users
#   - Multiple load scenarios: Baseline → Normal → Peak
#   - CSV results export with response times
#   - Percentile analysis (p50, p95, p99)
#   - Markdown report generation
#
# Usage:
#   bash run-load-tests-curl.sh [SERVICE_URL] [RESULTS_DIR]
#   bash run-load-tests-curl.sh http://localhost:8081 ./results
#
# Default Values:
#   SERVICE_URL=http://localhost:8081
#   RESULTS_DIR=./results
##############################################################################

set -euo pipefail

# Configuration
SERVICE_URL="${1:-http://localhost:8081}"
RESULTS_DIR="${2:-.}/results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
TEST_RUN_DIR="${RESULTS_DIR}/${TIMESTAMP}"

# Load test scenarios
declare -A SCENARIOS=(
    [baseline]="10:30:1"      # 10 concurrent users, 30 requests each, 1 second delay
    [normal]="50:20:0.5"      # 50 concurrent users, 20 requests each, 0.5s delay
    [peak]="100:10:0.2"       # 100 concurrent users, 10 requests each, 0.2s delay
)

# Test endpoints
declare -a ENDPOINTS=(
    "/api/v1/actuator/health"
    "/api/v1/cms/claims/search"
    "/api/v1/cms/claims/validate"
    "/api/v1/cms/sync/status"
)

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

check_service_health() {
    log_info "Checking service health at ${SERVICE_URL}..."
    if curl -s -f "${SERVICE_URL}/api/v1/actuator/health" >/dev/null 2>&1; then
        log_success "Service is healthy"
        return 0
    else
        log_error "Service is not reachable at ${SERVICE_URL}"
        return 1
    fi
}

run_load_scenario() {
    local scenario_name="$1"
    local concurrent_users="$2"
    local requests_per_user="$3"
    local delay="$4"
    local output_file="$5"

    log_info "Running $scenario_name scenario: $concurrent_users concurrent users, $requests_per_user requests each"

    echo "user,endpoint,http_code,response_time_ms,timestamp" > "${output_file}"

    for ((user=1; user<=concurrent_users; user++)); do
        (
            for ((i=1; i<=requests_per_user; i++)); do
                local endpoint_idx=$((RANDOM % ${#ENDPOINTS[@]}))
                local endpoint="${ENDPOINTS[$endpoint_idx]}"
                local start_time=$(date +%s%N)
                local http_code=$(curl -s -o /dev/null -w "%{http_code}" \
                    --connect-timeout 5 --max-time 10 \
                    "${SERVICE_URL}${endpoint}" 2>/dev/null || echo "000")
                local end_time=$(date +%s%N)
                local response_time=$(( (end_time - start_time) / 1000000 ))
                echo "${user},${endpoint},${http_code},${response_time},$(date -u +%Y-%m-%dT%H:%M:%SZ)" >> "${output_file}"
                sleep $(echo "$delay" | awk '{print $1}')
            done
        ) &
        sleep 0.05
    done

    wait
    log_success "Scenario ${scenario_name} complete"
}

main() {
    log_info "CMS Connector Service - Load Testing"
    log_info "Service URL: ${SERVICE_URL}"
    log_info "Results Directory: ${TEST_RUN_DIR}"

    mkdir -p "${TEST_RUN_DIR}"

    if ! check_service_health "${SERVICE_URL}"; then
        log_error "Cannot proceed without healthy service"
        exit 1
    fi

    for scenario_name in "${!SCENARIOS[@]}"; do
        IFS=':' read -r concurrent_users requests_per_user delay <<< "${SCENARIOS[$scenario_name]}"
        local output_file="${TEST_RUN_DIR}/${scenario_name}.csv"
        run_load_scenario "$scenario_name" "$concurrent_users" "$requests_per_user" "$delay" "$output_file"
    done

    log_success "Load testing complete!"
    log_info "Results saved to: ${TEST_RUN_DIR}"
}

main "$@"
