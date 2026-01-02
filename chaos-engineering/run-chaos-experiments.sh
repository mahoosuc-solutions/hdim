#!/bin/bash
# Chaos Engineering Experiment Runner
# Executes chaos experiments and validates system resilience

set -e

TOXIPROXY_API="http://localhost:8474"
GATEWAY_URL="http://localhost:9000"
RESULTS_DIR="./results/$(date +%Y%m%d_%H%M%S)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Parse arguments
DRY_RUN=false
CATEGORY="all"

while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --category)
            CATEGORY="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

mkdir -p "$RESULTS_DIR"

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if service is healthy
check_health() {
    local url=$1
    local response=$(curl -s -o /dev/null -w "%{http_code}" "$url/actuator/health" 2>/dev/null || echo "000")
    [[ "$response" == "200" ]]
}

# Wait for service to become healthy
wait_for_health() {
    local url=$1
    local max_attempts=30
    local attempt=0

    while [[ $attempt -lt $max_attempts ]]; do
        if check_health "$url"; then
            return 0
        fi
        ((attempt++))
        sleep 2
    done
    return 1
}

# Add toxic to proxy
add_toxic() {
    local proxy=$1
    local toxic_name=$2
    local toxic_type=$3
    local attributes=$4

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would add toxic: $toxic_name to $proxy"
        return 0
    fi

    curl -s -X POST "$TOXIPROXY_API/proxies/$proxy/toxics" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"$toxic_name\",\"type\":\"$toxic_type\",\"attributes\":$attributes}" \
        > /dev/null

    log_info "Added toxic: $toxic_name to $proxy"
}

# Remove toxic from proxy
remove_toxic() {
    local proxy=$1
    local toxic_name=$2

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would remove toxic: $toxic_name from $proxy"
        return 0
    fi

    curl -s -X DELETE "$TOXIPROXY_API/proxies/$proxy/toxics/$toxic_name" > /dev/null
    log_info "Removed toxic: $toxic_name from $proxy"
}

# Measure baseline metrics
measure_baseline() {
    log_info "Measuring baseline metrics..."

    local success_count=0
    local total_count=100
    local total_latency=0

    for i in $(seq 1 $total_count); do
        local start_time=$(date +%s%N)
        local response=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY_URL/actuator/health")
        local end_time=$(date +%s%N)

        if [[ "$response" == "200" ]]; then
            ((success_count++))
        fi

        local latency=$(( (end_time - start_time) / 1000000 ))
        total_latency=$((total_latency + latency))
    done

    local success_rate=$((success_count * 100 / total_count))
    local avg_latency=$((total_latency / total_count))

    echo "baseline_success_rate=$success_rate" >> "$RESULTS_DIR/metrics.txt"
    echo "baseline_avg_latency_ms=$avg_latency" >> "$RESULTS_DIR/metrics.txt"

    log_info "Baseline: Success Rate=$success_rate%, Avg Latency=${avg_latency}ms"
}

# Experiment 1: Database Latency
experiment_db_latency() {
    log_info "=== Experiment: Database Latency Injection ==="

    local experiment_name="db_latency"
    local start_time=$(date +%s)

    # Add latency
    add_toxic "postgres" "latency" "latency" '{"latency":1000}'

    # Measure during chaos
    sleep 5
    local chaos_metrics=$(measure_during_chaos)

    # Remove latency
    remove_toxic "postgres" "latency"

    # Wait for recovery
    sleep 10
    local recovery_time=$(($(date +%s) - start_time))

    # Record results
    echo "experiment=$experiment_name" >> "$RESULTS_DIR/$experiment_name.txt"
    echo "recovery_time_seconds=$recovery_time" >> "$RESULTS_DIR/$experiment_name.txt"
    echo "$chaos_metrics" >> "$RESULTS_DIR/$experiment_name.txt"

    log_info "Experiment completed: $experiment_name"
}

# Experiment 2: Redis Failure
experiment_redis_failure() {
    log_info "=== Experiment: Redis Connection Failure ==="

    local experiment_name="redis_failure"

    # Disable Redis proxy
    add_toxic "redis" "down" "timeout" '{"timeout":0}'

    # Wait and measure
    sleep 30

    # Re-enable Redis
    remove_toxic "redis" "down"

    # Wait for recovery
    wait_for_health "$GATEWAY_URL"

    log_info "Experiment completed: $experiment_name"
}

# Experiment 3: Service Timeout
experiment_service_timeout() {
    log_info "=== Experiment: FHIR Service Timeout ==="

    local experiment_name="fhir_timeout"

    # Add timeout toxic
    add_toxic "fhir-service" "timeout" "timeout" '{"timeout":30000}'

    # Measure circuit breaker behavior
    sleep 60

    # Remove timeout
    remove_toxic "fhir-service" "timeout"

    log_info "Experiment completed: $experiment_name"
}

# Experiment 4: Network Partition
experiment_network_partition() {
    log_info "=== Experiment: Kafka Network Partition ==="

    local experiment_name="kafka_partition"

    # Simulate packet loss
    add_toxic "kafka" "packet_loss" "reset_peer" '{"timeout":5000}'

    # Observe message processing
    sleep 120

    # Remove partition
    remove_toxic "kafka" "packet_loss"

    log_info "Experiment completed: $experiment_name"
}

# Main execution
main() {
    log_info "Starting Chaos Engineering Experiments"
    log_info "Results directory: $RESULTS_DIR"
    log_info "Category: $CATEGORY"
    log_info "Dry run: $DRY_RUN"

    # Check prerequisites
    if ! curl -s "$TOXIPROXY_API" > /dev/null 2>&1; then
        log_error "Toxiproxy is not running. Start with: docker-compose -f docker-compose.chaos.yml up -d"
        exit 1
    fi

    # Measure baseline
    measure_baseline

    # Run experiments based on category
    case $CATEGORY in
        "all")
            experiment_db_latency
            experiment_redis_failure
            experiment_service_timeout
            experiment_network_partition
            ;;
        "network")
            experiment_db_latency
            experiment_network_partition
            ;;
        "service")
            experiment_service_timeout
            ;;
        "cache")
            experiment_redis_failure
            ;;
        *)
            log_error "Unknown category: $CATEGORY"
            exit 1
            ;;
    esac

    log_info "All experiments completed. Results saved to: $RESULTS_DIR"
}

main "$@"
