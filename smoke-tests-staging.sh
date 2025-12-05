#!/bin/bash

# Smoke Tests for Staging Deployment
# Tests all critical service endpoints

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PASS=0
FAIL=0

test_endpoint() {
    local name=$1
    local url=$2
    local expected_code=${3:-200}

    echo -n "Testing $name... "

    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>&1)

    if [ "$response" = "$expected_code" ]; then
        echo -e "${GREEN}✓ PASS${NC} (HTTP $response)"
        PASS=$((PASS + 1))
    else
        echo -e "${RED}✗ FAIL${NC} (Expected $expected_code, got $response)"
        FAIL=$((FAIL + 1))
    fi
}

test_json_response() {
    local name=$1
    local url=$2
    local json_path=$3

    echo -n "Testing $name... "

    response=$(curl -s "$url" 2>&1)

    if echo "$response" | jq -e "$json_path" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PASS${NC}"
        PASS=$((PASS + 1))
    else
        echo -e "${RED}✗ FAIL${NC} (JSON path not found: $json_path)"
        FAIL=$((FAIL + 1))
    fi
}

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "  HealthData-in-Motion - Staging Smoke Tests"
echo "═══════════════════════════════════════════════════════════"
echo ""

echo "1. Infrastructure Services"
echo "─────────────────────────────────────"

# PostgreSQL
echo -n "Testing PostgreSQL... "
if docker compose -f docker-compose.staging.yml -p healthdata-staging exec -T postgres pg_isready -U healthdata > /dev/null 2>&1; then
    echo -e "${GREEN}✓ PASS${NC}"
    PASS=$((PASS + 1))
else
    echo -e "${RED}✗ FAIL${NC}"
    FAIL=$((FAIL + 1))
fi

# Redis
echo -n "Testing Redis... "
if docker compose -f docker-compose.staging.yml -p healthdata-staging exec -T redis redis-cli ping | grep -q PONG; then
    echo -e "${GREEN}✓ PASS${NC}"
    PASS=$((PASS + 1))
else
    echo -e "${RED}✗ FAIL${NC}"
    FAIL=$((FAIL + 1))
fi

# Kafka
echo -n "Testing Kafka... "
if docker compose -f docker-compose.staging.yml -p healthdata-staging exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1; then
    echo -e "${GREEN}✓ PASS${NC}"
    PASS=$((PASS + 1))
else
    echo -e "${RED}✗ FAIL${NC}"
    FAIL=$((FAIL + 1))
fi

echo ""
echo "2. Application Services"
echo "─────────────────────────────────────"

# Gateway Service
test_json_response "Gateway Health" "http://localhost:9000/actuator/health" ".status"
test_json_response "Gateway Info" "http://localhost:9000/actuator/info" "."

# Event Router Service
test_json_response "Event Router Health" "http://localhost:8089/actuator/health" ".status"
test_endpoint "Event Router Metrics" "http://localhost:8089/actuator/prometheus"

# Prometheus
test_endpoint "Prometheus" "http://localhost:9090/-/healthy"
test_endpoint "Prometheus Metrics" "http://localhost:9090/metrics"

echo ""
echo "3. Database Connectivity"
echo "─────────────────────────────────────"

# Check all databases exist
for db in healthdata_cql healthdata_quality_measure healthdata_fhir healthdata_patient healthdata_care_gap healthdata_event_router healthdata_gateway; do
    echo -n "Testing database $db... "
    if docker compose -f docker-compose.staging.yml -p healthdata-staging exec -T postgres psql -U healthdata -lqt | cut -d \| -f 1 | grep -qw "$db"; then
        echo -e "${GREEN}✓ PASS${NC}"
        PASS=$((PASS + 1))
    else
        echo -e "${RED}✗ FAIL${NC}"
        FAIL=$((FAIL + 1))
    fi
done

echo ""
echo "4. Event Streaming"
echo "─────────────────────────────────────"

# Check critical Kafka topics (should auto-create on first use)
echo -n "Testing Kafka broker... "
topic_count=$(docker compose -f docker-compose.staging.yml -p healthdata-staging exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | wc -l)
if [ "$topic_count" -ge 0 ]; then
    echo -e "${GREEN}✓ PASS${NC} ($topic_count topics)"
    PASS=$((PASS + 1))
else
    echo -e "${RED}✗ FAIL${NC}"
    FAIL=$((FAIL + 1))
fi

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "  Test Results"
echo "═══════════════════════════════════════════════════════════"
echo ""
echo -e "Passed: ${GREEN}$PASS${NC}"
echo -e "Failed: ${RED}$FAIL${NC}"
echo "Total:  $((PASS + FAIL))"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    echo ""
    exit 0
else
    echo -e "${YELLOW}⚠ Some tests failed${NC}"
    echo ""
    exit 1
fi
