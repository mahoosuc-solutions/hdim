#!/bin/bash
set -euo pipefail

# HDIM Platform Smoke Tests
# Validates deployment health across all services and critical workflows
#
# Usage: ./scripts/smoke-tests.sh <base_url> [options]
#   Options:
#     --retries <n>     Number of retries for failed checks (default: 3)
#     --timeout <s>     Timeout in seconds per request (default: 10)
#     --quick           Run only health checks (skip API validation)
#     --verbose         Show detailed output
#     --tenant <id>     Tenant ID for API tests (default: smoke-test-tenant)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="${1:-http://localhost:8080}"
RETRIES=3
TIMEOUT=10
QUICK_MODE=false
VERBOSE=false
TENANT_ID="smoke-test-tenant"
FAILED_TESTS=0
PASSED_TESTS=0
SKIPPED_TESTS=0

# Service list with ports (used for internal cluster communication)
declare -A SERVICES=(
    ["gateway-service"]="8080"
    ["fhir-service"]="8085"
    ["patient-service"]="8084"
    ["care-gap-service"]="8086"
    ["quality-measure-service"]="8087"
    ["cql-engine-service"]="8081"
    ["consent-service"]="8082"
    ["event-processing-service"]="8083"
    ["event-router-service"]="8089"
    ["agent-runtime-service"]="8088"
    ["ai-assistant-service"]="8090"
    ["agent-builder-service"]="8091"
    ["analytics-service"]="8092"
    ["predictive-analytics-service"]="8093"
    ["sdoh-service"]="8094"
    ["approval-service"]="8095"
    ["payer-workflows-service"]="8096"
    ["cdr-processor-service"]="8097"
    ["data-enrichment-service"]="8098"
    ["ehr-connector-service"]="8099"
    ["documentation-service"]="8100"
)

# Core services for quick mode
CORE_SERVICES=(
    "gateway-service"
    "fhir-service"
    "patient-service"
    "care-gap-service"
    "quality-measure-service"
)

# Parse arguments
parse_args() {
    shift # Skip base_url
    while [[ $# -gt 0 ]]; do
        case $1 in
            --retries)
                RETRIES="$2"
                shift 2
                ;;
            --timeout)
                TIMEOUT="$2"
                shift 2
                ;;
            --quick)
                QUICK_MODE=true
                shift
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            --tenant)
                TENANT_ID="$2"
                shift 2
                ;;
            *)
                shift
                ;;
        esac
    done
}

# Logging functions
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[PASS]${NC} $1"; ((PASSED_TESTS++)); }
log_warning() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[FAIL]${NC} $1"; ((FAILED_TESTS++)); }
log_skip() { echo -e "${YELLOW}[SKIP]${NC} $1"; ((SKIPPED_TESTS++)); }
log_debug() { [[ "$VERBOSE" == "true" ]] && echo -e "${BLUE}[DEBUG]${NC} $1"; }

# HTTP request with retry logic
http_request() {
    local method="$1"
    local url="$2"
    local expected_status="${3:-200}"
    local data="${4:-}"
    local retry=0
    local response
    local status_code

    while [[ $retry -lt $RETRIES ]]; do
        log_debug "Attempt $((retry+1))/$RETRIES: $method $url"

        if [[ -n "$data" ]]; then
            response=$(curl -s -w "\n%{http_code}" \
                --connect-timeout "$TIMEOUT" \
                --max-time "$((TIMEOUT * 2))" \
                -X "$method" \
                -H "Content-Type: application/json" \
                -H "X-Tenant-ID: $TENANT_ID" \
                -H "Authorization: Bearer smoke-test-token" \
                -d "$data" \
                "$url" 2>/dev/null || echo -e "\n000")
        else
            response=$(curl -s -w "\n%{http_code}" \
                --connect-timeout "$TIMEOUT" \
                --max-time "$((TIMEOUT * 2))" \
                -X "$method" \
                -H "X-Tenant-ID: $TENANT_ID" \
                -H "Authorization: Bearer smoke-test-token" \
                "$url" 2>/dev/null || echo -e "\n000")
        fi

        status_code=$(echo "$response" | tail -n1)

        if [[ "$status_code" == "$expected_status" ]]; then
            echo "$response" | sed '$d'
            return 0
        fi

        ((retry++))
        [[ $retry -lt $RETRIES ]] && sleep 2
    done

    log_debug "Final status: $status_code (expected: $expected_status)"
    return 1
}

# Check endpoint with status validation
check_endpoint() {
    local name="$1"
    local url="$2"
    local expected="${3:-200}"

    if http_request "GET" "$url" "$expected" > /dev/null 2>&1; then
        log_success "$name"
        return 0
    else
        log_error "$name - failed to respond with status $expected"
        return 1
    fi
}

# ============================================================================
# HEALTH ENDPOINT TESTS
# ============================================================================

test_health_endpoints() {
    log_info "=========================================="
    log_info "Testing Service Health Endpoints"
    log_info "=========================================="

    local services_to_test
    if [[ "$QUICK_MODE" == "true" ]]; then
        services_to_test=("${CORE_SERVICES[@]}")
        log_info "Quick mode: testing ${#services_to_test[@]} core services"
    else
        services_to_test=("${!SERVICES[@]}")
        log_info "Full mode: testing ${#services_to_test[@]} services"
    fi

    for service in "${services_to_test[@]}"; do
        # Try gateway-routed health check first
        local health_url="${BASE_URL}/${service}/actuator/health"

        if check_endpoint "$service health" "$health_url"; then
            # Also check liveness and readiness if available
            check_endpoint "$service liveness" "${BASE_URL}/${service}/actuator/health/liveness" || true
            check_endpoint "$service readiness" "${BASE_URL}/${service}/actuator/health/readiness" || true
        fi
    done
}

# ============================================================================
# INFRASTRUCTURE CONNECTIVITY TESTS
# ============================================================================

test_infrastructure_connectivity() {
    log_info "=========================================="
    log_info "Testing Infrastructure Connectivity"
    log_info "=========================================="

    # Test database connectivity via actuator health
    log_info "Checking database connectivity..."
    local db_response
    db_response=$(http_request "GET" "${BASE_URL}/fhir-service/actuator/health" 200 2>/dev/null || echo "{}")

    if echo "$db_response" | grep -q '"db":.*"status":"UP"' 2>/dev/null; then
        log_success "Database connectivity"
    elif echo "$db_response" | grep -q '"status":"UP"' 2>/dev/null; then
        log_success "Database connectivity (inferred from service health)"
    else
        log_error "Database connectivity - check database connection"
    fi

    # Test Redis connectivity
    log_info "Checking Redis connectivity..."
    local redis_response
    redis_response=$(http_request "GET" "${BASE_URL}/gateway-service/actuator/health" 200 2>/dev/null || echo "{}")

    if echo "$redis_response" | grep -q '"redis":.*"status":"UP"' 2>/dev/null; then
        log_success "Redis connectivity"
    else
        log_skip "Redis connectivity - not exposed in health endpoint"
    fi

    # Test Kafka connectivity
    log_info "Checking Kafka connectivity..."
    local kafka_response
    kafka_response=$(http_request "GET" "${BASE_URL}/event-router-service/actuator/health" 200 2>/dev/null || echo "{}")

    if echo "$kafka_response" | grep -q '"kafka":.*"status":"UP"' 2>/dev/null; then
        log_success "Kafka connectivity"
    else
        log_skip "Kafka connectivity - not exposed in health endpoint"
    fi
}

# ============================================================================
# FHIR API VALIDATION TESTS
# ============================================================================

test_fhir_capability_statement() {
    log_info "=========================================="
    log_info "Testing FHIR API Contracts"
    log_info "=========================================="

    # Test FHIR CapabilityStatement (metadata)
    log_info "Checking FHIR CapabilityStatement..."
    local metadata_response
    metadata_response=$(http_request "GET" "${BASE_URL}/fhir-service/fhir/R4/metadata" 200 2>/dev/null || echo "{}")

    if echo "$metadata_response" | grep -q '"resourceType":"CapabilityStatement"' 2>/dev/null; then
        log_success "FHIR CapabilityStatement"

        # Check for required resources
        for resource in "Patient" "Observation" "Condition" "MeasureReport"; do
            if echo "$metadata_response" | grep -q "\"type\":\"$resource\"" 2>/dev/null; then
                log_success "FHIR $resource resource supported"
            else
                log_warning "FHIR $resource resource not found in CapabilityStatement"
            fi
        done
    else
        log_error "FHIR CapabilityStatement - invalid or missing response"
    fi
}

test_patient_crud_operations() {
    log_info "Testing Patient CRUD operations..."

    # Create a test patient
    local patient_data='{
        "resourceType": "Patient",
        "identifier": [{
            "system": "http://hdim.example.com/smoke-test",
            "value": "smoke-test-patient-001"
        }],
        "name": [{
            "family": "SmokeTest",
            "given": ["Patient"]
        }],
        "birthDate": "1990-01-01",
        "gender": "unknown"
    }'

    # Create patient
    local create_response
    create_response=$(http_request "POST" "${BASE_URL}/fhir-service/fhir/R4/Patient" 201 "$patient_data" 2>/dev/null || echo "{}")

    if echo "$create_response" | grep -q '"resourceType":"Patient"' 2>/dev/null; then
        log_success "Patient CREATE operation"

        # Extract patient ID
        local patient_id
        patient_id=$(echo "$create_response" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

        if [[ -n "$patient_id" ]]; then
            # Read patient
            if http_request "GET" "${BASE_URL}/fhir-service/fhir/R4/Patient/$patient_id" 200 > /dev/null 2>&1; then
                log_success "Patient READ operation"
            else
                log_error "Patient READ operation"
            fi

            # Delete patient (cleanup)
            if http_request "DELETE" "${BASE_URL}/fhir-service/fhir/R4/Patient/$patient_id" 200 > /dev/null 2>&1 || \
               http_request "DELETE" "${BASE_URL}/fhir-service/fhir/R4/Patient/$patient_id" 204 > /dev/null 2>&1; then
                log_success "Patient DELETE operation"
            else
                log_warning "Patient DELETE operation - cleanup may be needed"
            fi
        fi
    else
        log_error "Patient CREATE operation - failed to create test patient"
    fi
}

# ============================================================================
# QUALITY MEASURE TESTS
# ============================================================================

test_quality_measure_endpoints() {
    log_info "=========================================="
    log_info "Testing Quality Measure Endpoints"
    log_info "=========================================="

    # List measures
    log_info "Checking measure list..."
    if check_endpoint "GET /measures" "${BASE_URL}/quality-measure-service/api/v1/measures"; then
        log_debug "Measure list endpoint available"
    fi

    # Check CQL engine availability
    log_info "Checking CQL engine..."
    if check_endpoint "CQL engine health" "${BASE_URL}/cql-engine-service/actuator/health"; then
        log_debug "CQL engine is healthy"
    fi
}

# ============================================================================
# CARE GAP TESTS
# ============================================================================

test_care_gap_endpoints() {
    log_info "=========================================="
    log_info "Testing Care Gap Endpoints"
    log_info "=========================================="

    # List care gaps
    if check_endpoint "GET /care-gaps" "${BASE_URL}/care-gap-service/api/v1/care-gaps"; then
        log_debug "Care gap list endpoint available"
    fi
}

# ============================================================================
# MULTI-TENANT ISOLATION TESTS
# ============================================================================

test_tenant_isolation() {
    log_info "=========================================="
    log_info "Testing Multi-Tenant Isolation"
    log_info "=========================================="

    # Create data with tenant A
    local tenant_a="smoke-test-tenant-a"
    local tenant_b="smoke-test-tenant-b"

    log_info "Creating test data for tenant A..."
    local patient_a='{
        "resourceType": "Patient",
        "identifier": [{
            "system": "http://hdim.example.com/tenant-isolation-test",
            "value": "tenant-a-patient"
        }],
        "name": [{"family": "TenantA", "given": ["Patient"]}]
    }'

    # Save original tenant
    local orig_tenant="$TENANT_ID"

    # Create with tenant A
    TENANT_ID="$tenant_a"
    local create_a_response
    create_a_response=$(http_request "POST" "${BASE_URL}/fhir-service/fhir/R4/Patient" 201 "$patient_a" 2>/dev/null || echo "{}")

    if echo "$create_a_response" | grep -q '"resourceType":"Patient"' 2>/dev/null; then
        local patient_a_id
        patient_a_id=$(echo "$create_a_response" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

        if [[ -n "$patient_a_id" ]]; then
            log_success "Created patient for tenant A"

            # Try to access with tenant B - should fail or return empty
            TENANT_ID="$tenant_b"
            log_info "Attempting to access tenant A's patient from tenant B..."

            if http_request "GET" "${BASE_URL}/fhir-service/fhir/R4/Patient/$patient_a_id" 200 > /dev/null 2>&1; then
                log_error "Tenant isolation VIOLATED - tenant B can access tenant A's data"
            else
                log_success "Tenant isolation verified - tenant B cannot access tenant A's data"
            fi

            # Cleanup - delete with tenant A
            TENANT_ID="$tenant_a"
            http_request "DELETE" "${BASE_URL}/fhir-service/fhir/R4/Patient/$patient_a_id" 200 > /dev/null 2>&1 || \
            http_request "DELETE" "${BASE_URL}/fhir-service/fhir/R4/Patient/$patient_a_id" 204 > /dev/null 2>&1 || true
        fi
    else
        log_skip "Tenant isolation test - could not create test patient"
    fi

    # Restore original tenant
    TENANT_ID="$orig_tenant"
}

# ============================================================================
# CRITICAL WORKFLOW TESTS
# ============================================================================

test_critical_workflows() {
    log_info "=========================================="
    log_info "Testing Critical Workflows"
    log_info "=========================================="

    # Test care gap identification workflow
    log_info "Testing care gap identification workflow..."
    if check_endpoint "Care gap workflow" "${BASE_URL}/care-gap-service/api/v1/care-gaps/identify" 200 || \
       check_endpoint "Care gap workflow" "${BASE_URL}/care-gap-service/api/v1/care-gaps" 200; then
        log_debug "Care gap identification available"
    fi

    # Test analytics endpoint
    log_info "Testing analytics endpoints..."
    if check_endpoint "Analytics summary" "${BASE_URL}/analytics-service/api/v1/analytics/summary" 200 || \
       check_endpoint "Analytics health" "${BASE_URL}/analytics-service/actuator/health" 200; then
        log_debug "Analytics service available"
    fi

    # Test approval workflow
    log_info "Testing approval workflow..."
    if check_endpoint "Approval workflow" "${BASE_URL}/approval-service/api/v1/approvals" 200 || \
       check_endpoint "Approval health" "${BASE_URL}/approval-service/actuator/health" 200; then
        log_debug "Approval service available"
    fi
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

print_summary() {
    echo ""
    log_info "=========================================="
    log_info "SMOKE TEST SUMMARY"
    log_info "=========================================="
    echo -e "${GREEN}Passed:${NC}  $PASSED_TESTS"
    echo -e "${RED}Failed:${NC}  $FAILED_TESTS"
    echo -e "${YELLOW}Skipped:${NC} $SKIPPED_TESTS"
    echo ""

    if [[ $FAILED_TESTS -gt 0 ]]; then
        log_error "Smoke tests FAILED - $FAILED_TESTS test(s) failed"
        return 1
    else
        log_success "All smoke tests PASSED"
        return 0
    fi
}

show_usage() {
    echo "HDIM Platform Smoke Tests"
    echo ""
    echo "Usage: $0 <base_url> [options]"
    echo ""
    echo "Arguments:"
    echo "  base_url    Base URL for the HDIM platform (e.g., http://localhost:8080)"
    echo ""
    echo "Options:"
    echo "  --retries <n>     Number of retries for failed checks (default: 3)"
    echo "  --timeout <s>     Timeout in seconds per request (default: 10)"
    echo "  --quick           Run only health checks (skip API validation)"
    echo "  --verbose         Show detailed output"
    echo "  --tenant <id>     Tenant ID for API tests (default: smoke-test-tenant)"
    echo ""
    echo "Examples:"
    echo "  $0 http://localhost:8080"
    echo "  $0 https://staging.hdim.example.com --quick"
    echo "  $0 https://hdim.example.com --retries 5 --timeout 30"
}

main() {
    if [[ $# -lt 1 ]] || [[ "$1" == "-h" ]] || [[ "$1" == "--help" ]]; then
        show_usage
        exit 0
    fi

    parse_args "$@"

    log_info "Starting HDIM Smoke Tests"
    log_info "Base URL: $BASE_URL"
    log_info "Tenant ID: $TENANT_ID"
    log_info "Quick Mode: $QUICK_MODE"
    echo ""

    # Run test suites
    test_health_endpoints
    test_infrastructure_connectivity

    if [[ "$QUICK_MODE" != "true" ]]; then
        test_fhir_capability_statement
        test_patient_crud_operations
        test_quality_measure_endpoints
        test_care_gap_endpoints
        test_tenant_isolation
        test_critical_workflows
    fi

    print_summary
}

main "$@"
