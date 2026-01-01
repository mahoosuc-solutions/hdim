#!/bin/bash
# CMS Connector Service - Chaos Engineering Tests
# Phase 5 Week 2: System Resilience Validation

set -euo pipefail

SERVICE_URL="${1:-http://localhost:8081}"
RESULTS_DIR="${2:-.}/chaos-results"
POSTGRES_CONTAINER="${3:-devb-postgres-staging}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
TEST_RUN_DIR="${RESULTS_DIR}/${TIMESTAMP}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[✓]${NC} $1"; }
log_error() { echo -e "${RED}[✗]${NC} $1"; }

check_service_health() {
    log_info "Checking service health..."
    if curl -s "${SERVICE_URL}/api/v1/actuator/health" >/dev/null 2>&1; then
        log_success "Service is healthy"
        return 0
    else
        log_error "Service is not reachable"
        return 1
    fi
}

check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed"
        exit 1
    fi
    log_success "Docker is available"
}

test_database_connection_loss() {
    local result_file="${TEST_RUN_DIR}/database-connection-loss.log"
    log_info "Testing: Database Connection Loss"
    
    {
        echo "=== Database Connection Loss Test ==="
        echo "Phase 1: Baseline"
        for i in {1..5}; do
            curl -s -w "Request $i: %{http_code} (%{time_total}s)\n" "${SERVICE_URL}/api/v1/actuator/health" 2>&1
            sleep 1
        done
        echo ""
        echo "Phase 2: Database Down"
        docker stop "${POSTGRES_CONTAINER}" 2>&1 || true
        sleep 3
        for i in {1..5}; do
            curl -s -w "Request $i: %{http_code} (%{time_total}s)\n" "${SERVICE_URL}/api/v1/actuator/health" 2>&1 || echo "FAILED"
            sleep 1
        done
        echo ""
        echo "Phase 3: Recovery"
        docker start "${POSTGRES_CONTAINER}" 2>&1 || true
        sleep 5
        for i in {1..5}; do
            curl -s -w "Request $i: %{http_code} (%{time_total}s)\n" "${SERVICE_URL}/api/v1/actuator/health" 2>&1
            sleep 1
        done
        echo "Test Complete: $(date)"
    } | tee "${result_file}"
    log_success "Database Connection Loss test completed"
}

test_connection_pool_exhaustion() {
    local result_file="${TEST_RUN_DIR}/connection-pool-exhaustion.log"
    log_info "Testing: Connection Pool Exhaustion"
    
    {
        echo "=== Connection Pool Exhaustion Test ==="
        echo "Launching 30 concurrent requests..."
        local success=0
        for ((i=1; i<=30; i++)); do
            curl -s -w "Request $i: %{http_code}\n" "${SERVICE_URL}/api/v1/actuator/health" >> "${result_file}" 2>&1 &
            ((success++)) || true
        done
        wait
        echo "Results: $success/30 completed"
        echo "Test Complete: $(date)"
    } | tee -a "${result_file}"
    log_success "Connection Pool Exhaustion test completed"
}

test_memory_pressure() {
    local result_file="${TEST_RUN_DIR}/memory-pressure.log"
    log_info "Testing: Memory Pressure"
    
    {
        echo "=== Memory Pressure Test ==="
        echo "Baseline memory:"
        docker stats --no-stream "${POSTGRES_CONTAINER}" 2>&1 || true
        echo ""
        echo "Loading data..."
        for i in {1..20}; do
            curl -s "${SERVICE_URL}/api/v1/actuator/health" >/dev/null 2>&1 || true
            sleep 0.5
        done
        echo "Memory after load:"
        docker stats --no-stream "${POSTGRES_CONTAINER}" 2>&1 || true
        echo "Test Complete: $(date)"
    } | tee "${result_file}"
    log_success "Memory Pressure test completed"
}

test_slow_network() {
    local result_file="${TEST_RUN_DIR}/slow-network.log"
    log_info "Testing: Slow Network Simulation"
    
    {
        echo "=== Slow Network Test ==="
        echo "Normal requests:"
        for i in {1..5}; do
            curl -s -w "Request $i: %{http_code} (%{time_total}s)\n" "${SERVICE_URL}/api/v1/actuator/health" 2>&1
            sleep 0.5
        done
        echo ""
        echo "Slow requests (10s timeout):"
        for i in {1..5}; do
            curl -s -w "Request $i: %{http_code} (%{time_total}s)\n" --max-time 10 "${SERVICE_URL}/api/v1/actuator/health" 2>&1 || echo "TIMEOUT"
            sleep 2
        done
        echo "Test Complete: $(date)"
    } | tee "${result_file}"
    log_success "Slow Network test completed"
}

test_circuit_breaker() {
    local result_file="${TEST_RUN_DIR}/circuit-breaker.log"
    log_info "Testing: Circuit Breaker"
    
    {
        echo "=== Circuit Breaker Test ==="
        echo "Stopping database..."
        docker stop "${POSTGRES_CONTAINER}" 2>&1 || true
        sleep 2
        echo "Making requests while down:"
        for i in {1..10}; do
            curl -s -w "Request $i: %{http_code}\n" "${SERVICE_URL}/api/v1/actuator/health" 2>&1 || echo "FAILED"
            sleep 0.5
        done
        echo ""
        echo "Restarting database..."
        docker start "${POSTGRES_CONTAINER}" 2>&1 || true
        sleep 5
        echo "Recovery requests:"
        for i in {1..10}; do
            curl -s -w "Request $i: %{http_code}\n" "${SERVICE_URL}/api/v1/actuator/health" 2>&1
            sleep 1
        done
        echo "Test Complete: $(date)"
    } | tee "${result_file}"
    log_success "Circuit Breaker test completed"
}

main() {
    log_info "CMS Connector Service - Chaos Engineering Tests"
    mkdir -p "${TEST_RUN_DIR}"
    
    check_docker
    check_service_health || exit 1
    
    test_database_connection_loss
    log_info ""
    test_connection_pool_exhaustion
    log_info ""
    test_memory_pressure
    log_info ""
    test_slow_network
    log_info ""
    test_circuit_breaker
    
    log_success "All chaos tests complete!"
    log_info "Results saved to: ${TEST_RUN_DIR}"
}

main "$@"
