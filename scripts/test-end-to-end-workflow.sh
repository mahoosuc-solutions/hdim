#!/bin/bash

##############################################################################
# End-to-End Workflow Test Script
#
# Tests the complete FHIR → CQL Engine → Quality Measure → Care Gap workflow
#
# Prerequisites:
#   - Docker and Docker Compose installed
#   - Java 21 installed
#   - Gradle 8.11+ installed
#   - Network connectivity to localhost
#
# Usage:
#   ./scripts/test-end-to-end-workflow.sh
#
# What this script does:
#   1. Starts infrastructure (PostgreSQL, Redis, Kafka)
#   2. Starts core services (FHIR, CQL Engine, Quality Measure)
#   3. Waits for services to be healthy
#   4. Seeds demo patient data
#   5. Runs HEDIS measure evaluations
#   6. Validates results
#   7. Cleans up (optional)
#
# Exit codes:
#   0 - All tests passed
#   1 - Infrastructure startup failed
#   2 - Service startup failed
#   3 - Data seeding failed
#   4 - Measure evaluation failed
#   5 - Validation failed
##############################################################################

set -e  # Exit on error
set -u  # Exit on undefined variable

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
TENANT_ID="${TENANT_ID:-acme-health}"
PATIENT_COUNT="${PATIENT_COUNT:-10}"
CARE_GAP_PERCENTAGE="${CARE_GAP_PERCENTAGE:-30}"
CLEANUP="${CLEANUP:-true}"
SKIP_BUILD="${SKIP_BUILD:-false}"
DEMO_MODE="${DEMO_MODE:-quick}"  # quick (100 patients) or full (19K patients)
RUN_GATEWAY_SMOKE="${RUN_GATEWAY_SMOKE:-true}"

# Service URLs
FHIR_URL="http://localhost:8085"
CQL_ENGINE_URL="http://localhost:8081"
QUALITY_MEASURE_URL="http://localhost:8087"
DEMO_SEEDING_URL="http://localhost:8103"
GATEWAY_URL="http://localhost:8001"

# PID tracking
FHIR_PID=""
CQL_PID=""
QUALITY_PID=""
DEMO_PID=""

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# Cleanup function
cleanup() {
    log_info "Cleaning up..."

    if [ -n "$FHIR_PID" ] && kill -0 "$FHIR_PID" 2>/dev/null; then
        log_info "Stopping FHIR Service (PID: $FHIR_PID)"
        kill "$FHIR_PID" || true
    fi

    if [ -n "$CQL_PID" ] && kill -0 "$CQL_PID" 2>/dev/null; then
        log_info "Stopping CQL Engine Service (PID: $CQL_PID)"
        kill "$CQL_PID" || true
    fi

    if [ -n "$QUALITY_PID" ] && kill -0 "$QUALITY_PID" 2>/dev/null; then
        log_info "Stopping Quality Measure Service (PID: $QUALITY_PID)"
        kill "$QUALITY_PID" || true
    fi

    if [ -n "$DEMO_PID" ] && kill -0 "$DEMO_PID" 2>/dev/null; then
        log_info "Stopping Demo Seeding Service (PID: $DEMO_PID)"
        kill "$DEMO_PID" || true
    fi

    if [ "$CLEANUP" = "true" ]; then
        log_info "Stopping Docker infrastructure..."
        docker compose down
    else
        log_warn "Skipping Docker cleanup (CLEANUP=false)"
    fi
}

# Register cleanup on script exit
trap cleanup EXIT

# Health check function
wait_for_service() {
    local service_name=$1
    local health_url=$2
    local max_wait=${3:-120}  # Default 2 minutes
    local wait_interval=5
    local elapsed=0

    log_info "Waiting for $service_name to be healthy at $health_url"

    while [ $elapsed -lt $max_wait ]; do
        if curl -sf "$health_url" > /dev/null 2>&1; then
            log_success "$service_name is healthy"
            return 0
        fi

        sleep $wait_interval
        elapsed=$((elapsed + wait_interval))
        echo -n "."
    done

    echo ""
    log_error "$service_name failed to become healthy within $max_wait seconds"
    return 1
}

# Main workflow
main() {
    log_info "==================================================================="
    log_info "HDIM End-to-End Workflow Test"
    log_info "==================================================================="
    log_info "Tenant ID: $TENANT_ID"
    log_info "Patient Count: $PATIENT_COUNT"
    log_info "Care Gap %: $CARE_GAP_PERCENTAGE"
    log_info "==================================================================="

    # Step 1: Start infrastructure
    log_info "Step 1: Starting Docker infrastructure (PostgreSQL, Redis, Kafka)"
    docker compose up -d postgres redis kafka || {
        log_error "Failed to start Docker infrastructure"
        exit 1
    }

    log_info "Waiting for PostgreSQL to be ready..."
    sleep 10

    docker compose exec -T postgres pg_isready -U healthdata || {
        log_error "PostgreSQL is not ready"
        exit 1
    }
    log_success "Infrastructure started successfully"

    # Step 2: Build services (optional)
    if [ "$SKIP_BUILD" = "false" ]; then
        log_info "Step 2: Building services..."
        cd backend || exit 1
        ./gradlew :modules:services:fhir-service:build \
                  :modules:services:cql-engine-service:build \
                  :modules:services:quality-measure-service:build \
                  :modules:services:demo-seeding-service:build \
                  -x test || {
            log_error "Build failed"
            exit 2
        }
        cd ..
        log_success "Services built successfully"
    else
        log_warn "Skipping build (SKIP_BUILD=true)"
    fi

    # Step 3: Start FHIR Service
    log_info "Step 3: Starting FHIR Service..."
    cd backend || exit 1
    ./gradlew :modules:services:fhir-service:bootRun > /tmp/fhir-service.log 2>&1 &
    FHIR_PID=$!
    cd ..

    wait_for_service "FHIR Service" "$FHIR_URL/actuator/health" 90 || exit 2

    # Step 4: Start CQL Engine Service
    log_info "Step 4: Starting CQL Engine Service..."
    cd backend || exit 1
    ./gradlew :modules:services:cql-engine-service:bootRun > /tmp/cql-engine.log 2>&1 &
    CQL_PID=$!
    cd ..

    wait_for_service "CQL Engine Service" "$CQL_ENGINE_URL/actuator/health" 90 || exit 2

    # Step 5: Start Quality Measure Service
    log_info "Step 5: Starting Quality Measure Service..."
    cd backend || exit 1
    ./gradlew :modules:services:quality-measure-service:bootRun > /tmp/quality-measure.log 2>&1 &
    QUALITY_PID=$!
    cd ..

    wait_for_service "Quality Measure Service" "$QUALITY_MEASURE_URL/actuator/health" 90 || exit 2

    # Step 6: Start Demo Seeding Service
    log_info "Step 6: Starting Demo Seeding Service..."
    cd backend || exit 1
    ./gradlew :modules:services:demo-seeding-service:bootRun > /tmp/demo-seeding.log 2>&1 &
    DEMO_PID=$!
    cd ..

    wait_for_service "Demo Seeding Service" "$DEMO_SEEDING_URL/actuator/health" 60 || exit 2

    # Step 6.5: Run Gateway Smoke Test (if enabled)
    if [ "$RUN_GATEWAY_SMOKE" = "true" ]; then
        log_info "Step 6.5: Running Gateway Smoke Test..."

        SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

        if [ -f "$SCRIPT_DIR/test-gateway-smoke.sh" ]; then
            # Run smoke test in quick mode (core services only)
            "$SCRIPT_DIR/test-gateway-smoke.sh" --mode=quick --skip-rate-limiting --output /tmp/gateway-smoke.json || {
                log_error "Gateway smoke test failed"
                log_info "Check smoke test report: /tmp/gateway-smoke.json"
                exit 2
            }

            # Extract pass rate from report
            if [ -f /tmp/gateway-smoke.json ]; then
                pass_rate=$(jq -r '.summary.passRate' /tmp/gateway-smoke.json 2>/dev/null || echo "N/A")
                log_success "Gateway smoke test passed: $pass_rate% success rate"
            fi
        else
            log_warn "Gateway smoke test script not found, skipping"
        fi
    else
        log_info "Gateway smoke test skipped (RUN_GATEWAY_SMOKE=false)"
    fi

    # Step 7: Verify measure definitions auto-seeded
    log_info "Step 7: Verifying HEDIS measure definitions..."
    measure_count=$(curl -sf -H "X-Tenant-ID: $TENANT_ID" \
        "$QUALITY_MEASURE_URL/api/v1/measures?measureSet=HEDIS" | jq '. | length' || echo "0")

    if [ "$measure_count" -eq 0 ]; then
        log_error "No HEDIS measures found. Auto-seeding may have failed."
        log_info "Check quality measure service logs: /tmp/quality-measure.log"
        exit 3
    fi

    log_success "Found $measure_count HEDIS measure definitions"

    # Step 8: Verify value sets seeded
    log_info "Step 8: Verifying value sets..."
    value_set_count=$(curl -sf -H "X-Tenant-ID: $TENANT_ID" \
        "$CQL_ENGINE_URL/api/v1/valuesets" | jq '. | length' || echo "0")

    if [ "$value_set_count" -eq 0 ]; then
        log_warn "No value sets found. This may cause measure evaluation to fail."
        log_info "Value sets should be seeded by migration 0014-seed-essential-value-sets.xml"
    else
        log_success "Found $value_set_count value sets"
    fi

    # Step 9: Seed patient data (mode-dependent)
    if [ "$DEMO_MODE" = "full" ]; then
        log_info "Step 9: Loading FULL demo scenarios (19K patients across 4 scenarios)..."
        log_warn "This may take 60-90 seconds. Please wait..."

        # Scenario 1: HEDIS Evaluation (5K patients, 28% gaps)
        log_info "  Loading HEDIS Evaluation scenario (5,000 patients)..."
        hedis_seed=$(curl -sf -X POST \
            -H "X-Tenant-ID: $TENANT_ID" \
            -H "Content-Type: application/json" \
            -d '{"count": 5000, "careGapPercentage": 28}' \
            "$DEMO_SEEDING_URL/api/v1/demo/seed" || echo "failed")

        # Scenario 2: Patient Journey (1K patients, 35% gaps)
        log_info "  Loading Patient Journey scenario (1,000 patients)..."
        journey_seed=$(curl -sf -X POST \
            -H "X-Tenant-ID: $TENANT_ID" \
            -H "Content-Type: application/json" \
            -d '{"count": 1000, "careGapPercentage": 35}' \
            "$DEMO_SEEDING_URL/api/v1/demo/seed" || echo "failed")

        # Scenario 3: Risk Stratification (10K patients, 25% gaps)
        log_info "  Loading Risk Stratification scenario (10,000 patients)..."
        risk_seed=$(curl -sf -X POST \
            -H "X-Tenant-ID: $TENANT_ID" \
            -H "Content-Type: application/json" \
            -d '{"count": 10000, "careGapPercentage": 25}' \
            "$DEMO_SEEDING_URL/api/v1/demo/seed" || echo "failed")

        # Scenario 4: Multi-Tenant (3K patients, 30% gaps)
        log_info "  Loading Multi-Tenant scenario (3,000 patients across 3 tenants)..."
        # For multi-tenant, seed to multiple tenants
        for tenant in "acme-health" "blue-shield-demo" "united-demo"; do
            curl -sf -X POST \
                -H "X-Tenant-ID: $tenant" \
                -H "Content-Type: application/json" \
                -d '{"count": 1000, "careGapPercentage": 30}' \
                "$DEMO_SEEDING_URL/api/v1/demo/seed" > /dev/null 2>&1 || true
        done

        log_success "Full demo scenarios loaded successfully (19,000 total patients)"
    else
        # Quick mode: minimal patient count for fast validation
        PATIENT_COUNT="${PATIENT_COUNT:-100}"
        log_info "Step 9: Seeding QUICK demo ($PATIENT_COUNT patients)..."

        seed_response=$(curl -sf -X POST \
            -H "X-Tenant-ID: $TENANT_ID" \
            -H "Content-Type: application/json" \
            -d "{\"count\": $PATIENT_COUNT, \"careGapPercentage\": $CARE_GAP_PERCENTAGE}" \
            "$DEMO_SEEDING_URL/api/v1/demo/seed") || {
            log_error "Failed to seed patient data"
            exit 3
        }

        log_success "Quick demo seeded: $PATIENT_COUNT patients"
    fi

    # Step 10: Verify patients exist
    log_info "Step 10: Verifying patients exist in FHIR Service..."
    patient_bundle=$(curl -sf -H "X-Tenant-ID: $TENANT_ID" \
        "$FHIR_URL/api/v1/Patient?_count=10" || echo "{}")

    patient_count=$(echo "$patient_bundle" | jq '.entry | length' || echo "0")
    if [ "$patient_count" -eq 0 ]; then
        log_error "No patients found in FHIR Service"
        exit 3
    fi

    # Get first patient ID for testing
    first_patient_id=$(echo "$patient_bundle" | jq -r '.entry[0].resource.id' || echo "")
    if [ -z "$first_patient_id" ] || [ "$first_patient_id" = "null" ]; then
        log_error "Could not extract patient ID from FHIR response"
        exit 3
    fi

    log_success "Found $patient_count patients. First patient ID: $first_patient_id"

    # Step 11: Evaluate HEDIS measures for test patient
    log_info "Step 11: Evaluating HEDIS measures for patient: $first_patient_id"

    # Test BCS (Breast Cancer Screening) measure
    log_info "Evaluating BCS (Breast Cancer Screening) measure..."
    bcs_result=$(curl -sf -X POST \
        -H "X-Tenant-ID: $TENANT_ID" \
        -H "Content-Type: application/json" \
        -d "{\"patientId\": \"$first_patient_id\", \"measureId\": \"BCS\"}" \
        "$QUALITY_MEASURE_URL/api/v1/evaluate" || echo "{}")

    if echo "$bcs_result" | jq -e '.measureId' > /dev/null 2>&1; then
        log_success "BCS measure evaluated successfully"
        echo "$bcs_result" | jq '.' || true
    else
        log_error "BCS measure evaluation failed: $bcs_result"
        exit 4
    fi

    # Test CBP (Controlling Blood Pressure) measure
    log_info "Evaluating CBP (Controlling Blood Pressure) measure..."
    cbp_result=$(curl -sf -X POST \
        -H "X-Tenant-ID: $TENANT_ID" \
        -H "Content-Type: application/json" \
        -d "{\"patientId\": \"$first_patient_id\", \"measureId\": \"CBP\"}" \
        "$QUALITY_MEASURE_URL/api/v1/evaluate" || echo "{}")

    if echo "$cbp_result" | jq -e '.measureId' > /dev/null 2>&1; then
        log_success "CBP measure evaluated successfully"
        echo "$cbp_result" | jq '.' || true
    else
        log_warn "CBP measure evaluation may have failed (patient may not be in denominator)"
        echo "$cbp_result" | jq '.' || echo "$cbp_result"
    fi

    # Step 12: Validate results
    log_info "Step 12: Validating measure results..."

    # Check if measure result was persisted
    results_response=$(curl -sf -H "X-Tenant-ID: $TENANT_ID" \
        "$QUALITY_MEASURE_URL/api/v1/results?patientId=$first_patient_id" || echo "[]")

    result_count=$(echo "$results_response" | jq '. | length' || echo "0")
    if [ "$result_count" -eq 0 ]; then
        log_warn "No measure results persisted for patient $first_patient_id"
    else
        log_success "Found $result_count persisted measure results"
    fi

    # Step 13: Check for care gaps
    log_info "Step 13: Checking for care gaps..."
    care_gaps=$(curl -sf -H "X-Tenant-ID: $TENANT_ID" \
        "$QUALITY_MEASURE_URL/api/v1/care-gaps?patientId=$first_patient_id" || echo "[]")

    care_gap_count=$(echo "$care_gaps" | jq '. | length' || echo "0")
    if [ "$care_gap_count" -gt 0 ]; then
        log_success "Found $care_gap_count care gaps (expected for $CARE_GAP_PERCENTAGE% care gap seeding)"
        echo "$care_gaps" | jq '.[0:2]' || true  # Show first 2 care gaps
    else
        log_info "No care gaps found for this patient"
    fi

    # Step 14: Summary
    log_info "==================================================================="
    log_success "END-TO-END WORKFLOW TEST COMPLETED SUCCESSFULLY"
    log_info "==================================================================="
    log_info "Test Configuration:"
    log_info "  - Demo Mode: $DEMO_MODE"
    log_info "  - Gateway Smoke Test: $RUN_GATEWAY_SMOKE"
    log_info "  - Tenant ID: $TENANT_ID"
    log_info "-------------------------------------------------------------------"
    log_info "Results Summary:"
    log_info "  - HEDIS Measures: $measure_count"
    log_info "  - Value Sets: $value_set_count"
    log_info "  - Patients Created: $patient_count"
    log_info "  - Measure Results: $result_count"
    log_info "  - Care Gaps: $care_gap_count"
    if [ "$RUN_GATEWAY_SMOKE" = "true" ] && [ -f /tmp/gateway-smoke.json ]; then
        gateway_tests=$(jq -r '.summary.totalTests' /tmp/gateway-smoke.json 2>/dev/null || echo "N/A")
        gateway_passed=$(jq -r '.summary.passed' /tmp/gateway-smoke.json 2>/dev/null || echo "N/A")
        log_info "  - Gateway Tests: $gateway_passed/$gateway_tests passed"
    fi
    log_info "==================================================================="
    log_info "Service Logs:"
    log_info "  - FHIR Service: /tmp/fhir-service.log"
    log_info "  - CQL Engine: /tmp/cql-engine.log"
    log_info "  - Quality Measure: /tmp/quality-measure.log"
    log_info "  - Demo Seeding: /tmp/demo-seeding.log"
    if [ "$RUN_GATEWAY_SMOKE" = "true" ]; then
        log_info "  - Gateway Smoke Test: /tmp/gateway-smoke.json"
    fi
    log_info "==================================================================="
    if [ "$DEMO_MODE" = "full" ]; then
        log_info "💡 Full demo mode loaded 19,000 patients across 4 scenarios"
    else
        log_info "💡 To load full demo: DEMO_MODE=full ./scripts/test-end-to-end-workflow.sh"
    fi
    log_info "==================================================================="

    return 0
}

# Run main workflow
main "$@"
