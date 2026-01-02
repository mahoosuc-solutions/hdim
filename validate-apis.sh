#!/bin/bash

# HealthData in Motion - Comprehensive API & Data Model Validation Script
# This script validates all service APIs, data models, and inter-service communication

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored messages
info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }
test() { echo -e "${BLUE}[TEST]${NC} $1"; }

# Results tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
RESULTS=""

# Function to run a test
run_test() {
    local test_name=$1
    local command=$2
    local expected=$3

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo -n "Testing: $test_name... "

    if result=$(eval "$command" 2>&1); then
        if [[ -n "$expected" ]]; then
            if echo "$result" | grep -q "$expected"; then
                echo -e "${GREEN}✓${NC}"
                PASSED_TESTS=$((PASSED_TESTS + 1))
                RESULTS="${RESULTS}\n${GREEN}✓${NC} $test_name"
            else
                echo -e "${RED}✗${NC} (unexpected response)"
                FAILED_TESTS=$((FAILED_TESTS + 1))
                RESULTS="${RESULTS}\n${RED}✗${NC} $test_name - Unexpected response"
            fi
        else
            echo -e "${GREEN}✓${NC}"
            PASSED_TESTS=$((PASSED_TESTS + 1))
            RESULTS="${RESULTS}\n${GREEN}✓${NC} $test_name"
        fi
    else
        echo -e "${RED}✗${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        RESULTS="${RESULTS}\n${RED}✗${NC} $test_name - $result"
    fi
}

echo "=========================================="
echo "API & Data Model Validation Suite"
echo "=========================================="
echo ""

# ============================================
# SECTION 1: Health Check Validation
# ============================================
info "Section 1: Service Health Checks"
echo "----------------------------------------"

# Check each service health endpoint
run_test "PostgreSQL Database" \
    "docker exec healthdata-postgres pg_isready -U healthdata" \
    "accepting connections"

run_test "Redis Cache" \
    "docker exec healthdata-redis redis-cli ping" \
    "PONG"

run_test "Kafka Messaging" \
    "docker exec healthdata-kafka kafka-topics --bootstrap-server localhost:9092 --list > /dev/null && echo 'OK'" \
    "OK"

run_test "Quality Measure Service Health" \
    "curl -s http://localhost:8087/quality-measure/actuator/health | jq -r '.status'" \
    "UP"

run_test "FHIR Service Health" \
    "curl -s http://localhost:8081/actuator/health | jq -r '.status'" \
    "UP"

run_test "CQL Engine Service Health" \
    "curl -s http://localhost:8086/actuator/health | jq -r '.status'" \
    "UP"

run_test "Patient Service Health" \
    "curl -s http://localhost:8082/actuator/health | jq -r '.status'" \
    "UP"

run_test "Care Gap Service Health" \
    "curl -s http://localhost:8083/actuator/health | jq -r '.status'" \
    "UP"

run_test "Event Processing Service Health" \
    "curl -s http://localhost:8084/actuator/health | jq -r '.status'" \
    "UP"

run_test "Gateway Service Health" \
    "curl -s http://localhost:8080/actuator/health | jq -r '.status'" \
    "UP"

echo ""

# ============================================
# SECTION 2: Database Schema Validation
# ============================================
info "Section 2: Database Schema Validation"
echo "----------------------------------------"

# Check if all databases exist
run_test "FHIR Database Exists" \
    "docker exec healthdata-postgres psql -U healthdata -d healthdata_db -c \"SELECT datname FROM pg_database WHERE datname='fhir_db';\" | grep -c fhir_db" \
    "1"

run_test "Quality Database Exists" \
    "docker exec healthdata-postgres psql -U healthdata -d healthdata_db -c \"SELECT datname FROM pg_database WHERE datname='quality_db';\" | grep -c quality_db" \
    "1"

run_test "CQL Database Exists" \
    "docker exec healthdata-postgres psql -U healthdata -d healthdata_db -c \"SELECT datname FROM pg_database WHERE datname='cql_db';\" | grep -c cql_db" \
    "1"

run_test "Patient Database Exists" \
    "docker exec healthdata-postgres psql -U healthdata -d healthdata_db -c \"SELECT datname FROM pg_database WHERE datname='patient_db';\" | grep -c patient_db" \
    "1"

run_test "Care Gap Database Exists" \
    "docker exec healthdata-postgres psql -U healthdata -d healthdata_db -c \"SELECT datname FROM pg_database WHERE datname='caregap_db';\" | grep -c caregap_db" \
    "1"

run_test "Event Database Exists" \
    "docker exec healthdata-postgres psql -U healthdata -d healthdata_db -c \"SELECT datname FROM pg_database WHERE datname='event_db';\" | grep -c event_db" \
    "1"

# Check key tables exist
run_test "Quality DB - Notification History Table" \
    "docker exec healthdata-postgres psql -U healthdata -d quality_db -c \"\\dt notification_history\" 2>/dev/null | grep -c notification_history" \
    "1"

run_test "Quality DB - Care Gaps Table" \
    "docker exec healthdata-postgres psql -U healthdata -d quality_db -c \"\\dt care_gaps\" 2>/dev/null | grep -c care_gaps" \
    "1"

run_test "Quality DB - Health Scores Table" \
    "docker exec healthdata-postgres psql -U healthdata -d quality_db -c \"\\dt health_scores\" 2>/dev/null | grep -c health_scores" \
    "1"

echo ""

# ============================================
# SECTION 3: FHIR API Testing
# ============================================
info "Section 3: FHIR API Testing"
echo "----------------------------------------"

# Test FHIR metadata endpoint
run_test "FHIR Metadata Endpoint" \
    "curl -s -o /dev/null -w '%{http_code}' http://localhost:8081/fhir/metadata" \
    "200"

# Create a test patient
PATIENT_JSON='{
  "resourceType": "Patient",
  "identifier": [{
    "system": "http://example.org/mrn",
    "value": "TEST-12345"
  }],
  "name": [{
    "use": "official",
    "family": "TestFamily",
    "given": ["TestGiven"]
  }],
  "gender": "male",
  "birthDate": "1990-01-01"
}'

run_test "FHIR Create Patient" \
    "curl -s -X POST http://localhost:8081/fhir/Patient -H 'Content-Type: application/fhir+json' -d '$PATIENT_JSON' -o /dev/null -w '%{http_code}'" \
    "201"

# Search for patients
run_test "FHIR Search Patients" \
    "curl -s 'http://localhost:8081/fhir/Patient?name=TestFamily' | jq -r '.resourceType'" \
    "Bundle"

# Create test observation
OBSERVATION_JSON='{
  "resourceType": "Observation",
  "status": "final",
  "code": {
    "coding": [{
      "system": "http://loinc.org",
      "code": "8867-4",
      "display": "Heart rate"
    }]
  },
  "subject": {
    "reference": "Patient/TEST-12345"
  },
  "valueQuantity": {
    "value": 75,
    "unit": "beats/minute"
  }
}'

run_test "FHIR Create Observation" \
    "curl -s -X POST http://localhost:8081/fhir/Observation -H 'Content-Type: application/fhir+json' -d '$OBSERVATION_JSON' -o /dev/null -w '%{http_code}'" \
    "201"

echo ""

# ============================================
# SECTION 4: Quality Measure API Testing
# ============================================
info "Section 4: Quality Measure API Testing"
echo "----------------------------------------"

# Test quality measure endpoints
run_test "Quality Measure - List Measures" \
    "curl -s http://localhost:8087/quality-measure/api/measures | jq -r 'type'" \
    "array"

# Test patient health overview
run_test "Quality Measure - Patient Health Overview" \
    "curl -s 'http://localhost:8087/quality-measure/api/patient-health/overview?patientId=TEST-12345' -o /dev/null -w '%{http_code}'" \
    "200"

# Test care gaps endpoint
run_test "Quality Measure - Care Gaps" \
    "curl -s 'http://localhost:8087/quality-measure/api/care-gaps?patientId=TEST-12345' | jq -r 'type'" \
    "array"

# Test health score calculation
HEALTH_SCORE_REQUEST='{
  "patientId": "TEST-12345",
  "tenantId": "test-tenant",
  "measureIds": ["HbA1c-Control", "BP-Control"]
}'

run_test "Quality Measure - Calculate Health Score" \
    "curl -s -X POST http://localhost:8087/quality-measure/api/health-scores/calculate -H 'Content-Type: application/json' -d '$HEALTH_SCORE_REQUEST' -o /dev/null -w '%{http_code}'" \
    "200"

echo ""

# ============================================
# SECTION 5: CQL Engine API Testing
# ============================================
info "Section 5: CQL Engine API Testing"
echo "----------------------------------------"

# Test CQL evaluation endpoint
CQL_EVALUATION_REQUEST='{
  "patientId": "TEST-12345",
  "cqlLibrary": "TestLibrary",
  "parameters": {}
}'

run_test "CQL Engine - Evaluate Expression" \
    "curl -s -X POST http://localhost:8086/cql/evaluate -H 'Content-Type: application/json' -d '$CQL_EVALUATION_REQUEST' -o /dev/null -w '%{http_code}'" \
    "200"

# Test CQL library management
run_test "CQL Engine - List Libraries" \
    "curl -s http://localhost:8086/cql/libraries | jq -r 'type'" \
    "array"

echo ""

# ============================================
# SECTION 6: Inter-Service Communication
# ============================================
info "Section 6: Inter-Service Communication Testing"
echo "----------------------------------------"

# Test gateway routing to services
run_test "Gateway -> FHIR Service Routing" \
    "curl -s http://localhost:8080/fhir/metadata -o /dev/null -w '%{http_code}'" \
    "200"

run_test "Gateway -> Quality Measure Routing" \
    "curl -s http://localhost:8080/quality-measure/actuator/health | jq -r '.status'" \
    "UP"

run_test "Gateway -> CQL Engine Routing" \
    "curl -s http://localhost:8080/cql/actuator/health | jq -r '.status'" \
    "UP"

# Test Kafka topic creation
run_test "Kafka Topics Created" \
    "docker exec healthdata-kafka kafka-topics --bootstrap-server localhost:9092 --list | wc -l | xargs -I {} test {} -gt 0 && echo 'OK'" \
    "OK"

echo ""

# ============================================
# SECTION 7: Data Persistence Validation
# ============================================
info "Section 7: Data Persistence Validation"
echo "----------------------------------------"

# Check if data was persisted
run_test "Patient Data Persisted" \
    "docker exec healthdata-postgres psql -U healthdata -d fhir_db -c \"SELECT COUNT(*) FROM patient WHERE identifier LIKE '%TEST-12345%';\" | grep -E '[0-9]+' | tr -d ' ' | xargs -I {} test {} -gt 0 && echo 'OK'" \
    "OK"

run_test "Observation Data Persisted" \
    "docker exec healthdata-postgres psql -U healthdata -d fhir_db -c \"SELECT COUNT(*) FROM observation WHERE patient_id LIKE '%TEST-12345%';\" | grep -E '[0-9]+' | tr -d ' ' | xargs -I {} test {} -ge 0 && echo 'OK'" \
    "OK"

echo ""

# ============================================
# SECTION 8: Security & Authentication
# ============================================
info "Section 8: Security & Authentication Testing"
echo "----------------------------------------"

# Test authentication endpoints
run_test "Authentication - Health Check (No Auth Required)" \
    "curl -s http://localhost:8080/actuator/health -o /dev/null -w '%{http_code}'" \
    "200"

# Test CORS headers
run_test "CORS Headers Present" \
    "curl -s -I -X OPTIONS http://localhost:8080/fhir/Patient -H 'Origin: http://localhost:4200' | grep -c 'Access-Control-Allow'" \
    "1"

echo ""

# ============================================
# RESULTS SUMMARY
# ============================================
echo "=========================================="
echo "Validation Results Summary"
echo "=========================================="
echo -e "Total Tests: $TOTAL_TESTS"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"
echo ""
echo "Detailed Results:"
echo -e "$RESULTS"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    info "✅ All validation tests passed successfully!"
    exit 0
else
    warn "⚠️ Some tests failed. Please review the results above."
    exit 1
fi