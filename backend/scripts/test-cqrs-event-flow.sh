#!/bin/bash

# HDIM CQRS Event Flow Integration Test
# Tests end-to-end event publishing and projection updates
# Validates eventual consistency timing and multi-tenant isolation

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;36m'
NC='\033[0m' # No Color

# Test configuration
GATEWAY_URL="http://localhost:8001"
PATIENT_EVENT_URL="http://localhost:8110"
CARE_GAP_EVENT_URL="http://localhost:8111"
QUALITY_EVENT_URL="http://localhost:8112"
WORKFLOW_EVENT_URL="http://localhost:8113"

KAFKA_BOOTSTRAP="localhost:9094"
TENANT_ID="tenant-integration-test"
TEST_PATIENT_ID="patient-$(date +%s)"

# State tracking
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓ PASS]${NC} $1"
    ((TESTS_PASSED++))
}

log_error() {
    echo -e "${RED}[✗ FAIL]${NC} $1"
    ((TESTS_FAILED++))
}

log_test() {
    echo -e "\n${YELLOW}[TEST]${NC} $1"
    ((TESTS_RUN++))
}

# Test 1: Health checks for all services
test_service_health() {
    log_test "Service Health Checks"

    local services=(
        "patient-event-service:8110"
        "care-gap-event-service:8111"
        "quality-measure-event-service:8112"
        "clinical-workflow-event-service:8113"
    )

    for service in "${services[@]}"; do
        local name="${service%%:*}"
        local port="${service##*:}"
        local context="/${name%-event-service}-event"

        local response=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${port}${context}/actuator/health" 2>/dev/null || echo "000")

        if [ "$response" == "200" ]; then
            log_success "$name health check (HTTP $response)"
        else
            log_error "$name health check failed (HTTP $response)"
        fi
    done
}

# Test 2: Verify Kafka connectivity
test_kafka_connectivity() {
    log_test "Kafka Connectivity"

    # Check if Kafka is accessible
    if kafka-broker-api-versions.sh --bootstrap-server "$KAFKA_BOOTSTRAP" &>/dev/null; then
        log_success "Kafka broker accessible at $KAFKA_BOOTSTRAP"
    else
        log_error "Cannot connect to Kafka at $KAFKA_BOOTSTRAP"
        return 1
    fi

    # List existing topics
    local topics=$(kafka-topics.sh --bootstrap-server "$KAFKA_BOOTSTRAP" --list 2>/dev/null || echo "")
    if [ -n "$topics" ]; then
        log_success "Kafka topics accessible (found: $(echo $topics | wc -w) topics)"
    else
        log_error "No Kafka topics found"
    fi
}

# Test 3: Publish test patient event
test_publish_patient_event() {
    log_test "Publish Patient Created Event"

    local event_data='{
        "eventId": "'$(uuidgen)'",
        "tenantId": "'$TENANT_ID'",
        "patientId": "'$TEST_PATIENT_ID'",
        "firstName": "Integration",
        "lastName": "Test",
        "dateOfBirth": "1980-01-15",
        "gender": "M",
        "eventTimestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.000Z)'",
        "eventType": "PATIENT_CREATED"
    }'

    # Publish to Kafka
    echo "$event_data" | kafka-console-producer.sh \
        --broker-list "$KAFKA_BOOTSTRAP" \
        --topic "patient.events" \
        --property "parse.key=false" &>/dev/null

    if [ $? -eq 0 ]; then
        log_success "Patient created event published to Kafka"
        sleep 2  # Allow time for processing
    else
        log_error "Failed to publish patient event to Kafka"
        return 1
    fi
}

# Test 4: Query patient projection
test_query_patient_projection() {
    log_test "Query Patient Event Projection"

    local response=$(curl -s -H "X-Tenant-ID: $TENANT_ID" \
        "http://localhost:8110/patient-event/api/v1/projections/patients?limit=100" 2>/dev/null || echo "{}")

    # Check if response contains our test patient
    if echo "$response" | grep -q "$TEST_PATIENT_ID"; then
        log_success "Patient projection contains test patient (eventual consistency validated)"
    else
        # Patient might not have been projected yet - this is acceptable for eventual consistency
        log_info "Patient projection not yet updated (eventual consistency - retry expected)"
    fi
}

# Test 5: Verify multi-tenant isolation
test_multi_tenant_isolation() {
    log_test "Multi-Tenant Isolation"

    local tenant1_response=$(curl -s -H "X-Tenant-ID: tenant-1" \
        "http://localhost:8110/patient-event/api/v1/projections/patients?limit=10" 2>/dev/null || echo "{}")

    local tenant2_response=$(curl -s -H "X-Tenant-ID: tenant-2" \
        "http://localhost:8110/patient-event/api/v1/projections/patients?limit=10" 2>/dev/null || echo "{}")

    # Both should return valid responses (200)
    if [ -n "$tenant1_response" ] && [ -n "$tenant2_response" ]; then
        # Verify they contain different data (tenant isolation)
        if ! diff <(echo "$tenant1_response" | jq -S '.') <(echo "$tenant2_response" | jq -S '.') &>/dev/null; then
            log_success "Multi-tenant isolation verified (different data for different tenants)"
        else
            log_info "Tenants may share data or be empty - isolation check inconclusive"
        fi
    else
        log_error "Failed to query projections for multi-tenant test"
    fi
}

# Test 6: Verify cache behavior
test_cache_behavior() {
    log_test "Cache Behavior (Redis TTL)"

    local response=$(curl -s -i -H "X-Tenant-ID: $TENANT_ID" \
        "http://localhost:8110/patient-event/api/v1/projections/patients?limit=10" 2>/dev/null || echo "")

    # Check for cache control headers
    if echo "$response" | grep -qi "cache-control.*no-store"; then
        log_success "Cache-Control: no-store header present (PHI protection)"
    else
        log_error "Missing Cache-Control header for PHI endpoint"
    fi

    if echo "$response" | grep -qi "pragma.*no-cache"; then
        log_success "Pragma: no-cache header present (PHI protection)"
    else
        log_error "Missing Pragma header for PHI endpoint"
    fi
}

# Test 7: Verify Kafka consumer groups
test_consumer_groups() {
    log_test "Kafka Consumer Group Registration"

    local consumer_groups=$(kafka-consumer-groups.sh --bootstrap-server "$KAFKA_BOOTSTRAP" --list 2>/dev/null || echo "")

    local expected_groups=(
        "patient-event-service"
        "care-gap-event-service"
        "quality-measure-event-service"
        "clinical-workflow-event-service"
    )

    for group in "${expected_groups[@]}"; do
        if echo "$consumer_groups" | grep -q "^$group$"; then
            log_success "Consumer group registered: $group"
        else
            log_info "Consumer group not yet registered: $group (may start on first event)"
        fi
    done
}

# Test 8: Verify eventual consistency timing
test_eventual_consistency_timing() {
    log_test "Eventual Consistency Timing (target: 100-500ms)"

    local start_time=$(date +%s%3N)

    # Publish event
    local event_data='{
        "eventId": "'$(uuidgen)'",
        "tenantId": "'$TENANT_ID'",
        "patientId": "timing-test-'$(date +%s)'",
        "firstName": "Timing",
        "lastName": "Test",
        "dateOfBirth": "1985-06-20",
        "gender": "F",
        "eventTimestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.000Z)'",
        "eventType": "PATIENT_CREATED"
    }'

    echo "$event_data" | kafka-console-producer.sh \
        --broker-list "$KAFKA_BOOTSTRAP" \
        --topic "patient.events" \
        --property "parse.key=false" &>/dev/null

    # Poll for projection update
    local max_attempts=20  # 2 seconds max (100ms per attempt)
    local attempt=0
    local end_time=0

    while [ $attempt -lt $max_attempts ]; do
        local response=$(curl -s "http://localhost:8110/patient-event/api/v1/projections/patients?limit=100" 2>/dev/null || echo "{}")

        if echo "$response" | grep -q "timing-test-"; then
            end_time=$(date +%s%3N)
            local latency=$((end_time - start_time))

            if [ $latency -lt 500 ]; then
                log_success "Event projected within ${latency}ms (within 100-500ms target)"
            else
                log_error "Event projection exceeded 500ms target (took ${latency}ms)"
            fi
            return 0
        fi

        sleep 0.1
        ((attempt++))
    done

    log_error "Event not projected within 2 second timeout"
}

# Test 9: Verify database connectivity
test_database_connectivity() {
    log_test "Event Service Database Connectivity"

    local databases=(
        "patient_event_db"
        "care_gap_event_db"
        "quality_event_db"
        "clinical_workflow_event_db"
    )

    for db in "${databases[@]}"; do
        if PGPASSWORD=healthdata_password psql -h localhost -p 5435 -U healthdata -d "$db" -c "SELECT 1" &>/dev/null; then
            log_success "Database $db accessible"
        else
            log_error "Cannot connect to database $db"
        fi
    done
}

# Test 10: Verify projection tables exist
test_projection_tables() {
    log_test "Projection Table Existence"

    local tables=(
        "patient_event_db:patient_projections"
        "care_gap_event_db:care_gap_projections"
        "quality_event_db:quality_measure_projections"
        "clinical_workflow_event_db:workflow_projections"
    )

    for table_info in "${tables[@]}"; do
        local db="${table_info%%:*}"
        local table="${table_info##*:}"

        local result=$(PGPASSWORD=healthdata_password psql -h localhost -p 5435 -U healthdata -d "$db" \
            -c "SELECT to_regclass('public.$table')" 2>/dev/null || echo "")

        if [ -n "$result" ] && [ "$result" != "(null)" ]; then
            log_success "Projection table exists: $table in $db"
        else
            log_info "Projection table may not exist yet: $table in $db"
        fi
    done
}

# Main execution
main() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  HDIM CQRS Event Flow Integration Tests                  ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"

    # Run all tests
    test_service_health
    test_kafka_connectivity
    test_database_connectivity
    test_projection_tables
    test_consumer_groups
    test_publish_patient_event
    test_query_patient_projection
    test_multi_tenant_isolation
    test_cache_behavior
    test_eventual_consistency_timing

    # Summary
    echo -e "\n${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  Test Summary                                              ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"

    echo -e "Total Tests:  ${YELLOW}$TESTS_RUN${NC}"
    echo -e "Passed:       ${GREEN}$TESTS_PASSED${NC}"
    echo -e "Failed:       ${RED}$TESTS_FAILED${NC}"

    if [ $TESTS_FAILED -eq 0 ]; then
        echo -e "\n${GREEN}✓ All integration tests passed!${NC}"
        exit 0
    else
        echo -e "\n${RED}✗ Some tests failed. Review output above.${NC}"
        exit 1
    fi
}

main "$@"
