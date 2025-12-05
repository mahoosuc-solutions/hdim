#!/bin/bash
#
# HealthData In Motion - Deployment Verification Script
#
# Quickly verify all services are running and accessible
#

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

function check_service() {
    local name=$1
    local url=$2
    local expected_status=${3:-200}

    status_code=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)

    if [ "$status_code" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓${NC} $name - OK (HTTP $status_code)"
        return 0
    else
        echo -e "${RED}✗${NC} $name - FAILED (HTTP $status_code, expected $expected_status)"
        return 1
    fi
}

function check_docker_container() {
    local name=$1

    status=$(docker ps --filter "name=$name" --format "{{.Status}}" 2>/dev/null)

    if [[ $status == *"Up"* ]] && [[ $status == *"healthy"* ]]; then
        echo -e "${GREEN}✓${NC} $name - Running (healthy)"
        return 0
    elif [[ $status == *"Up"* ]]; then
        echo -e "${YELLOW}⚠${NC} $name - Running (not healthy yet)"
        return 0
    else
        echo -e "${RED}✗${NC} $name - Not running"
        return 1
    fi
}

echo "=========================================="
echo "HealthData In Motion - Deployment Verification"
echo "=========================================="
echo ""

#
# 1. Check Docker Containers
#
echo -e "${BLUE}1. Docker Containers${NC}"
echo "-------------------"
check_docker_container "healthdata-postgres"
check_docker_container "healthdata-redis"
check_docker_container "healthdata-kafka"
check_docker_container "healthdata-cql-engine"
check_docker_container "healthdata-quality-measure"
check_docker_container "healthdata-fhir-mock"
check_docker_container "healthdata-kong"
check_docker_container "healthdata-kong-db"
echo ""

#
# 2. Check Direct Service Health
#
echo -e "${BLUE}2. Backend Services (Direct)${NC}"
echo "----------------------------"
check_service "CQL Engine Health" "http://localhost:8081/cql-engine/actuator/health"
check_service "Quality Measure Health" "http://localhost:8087/quality-measure/actuator/health"
check_service "FHIR Server Metadata" "http://localhost:8083/fhir/metadata"
echo ""

#
# 3. Check Kong API Gateway
#
echo -e "${BLUE}3. Kong API Gateway${NC}"
echo "-------------------"
check_service "Kong Admin API" "http://localhost:8001/"
check_service "Kong Proxy" "http://localhost:8000/"
echo ""

#
# 4. Check Services via Kong
#
echo -e "${BLUE}4. Services via Kong${NC}"
echo "--------------------"
check_service "CQL Engine (Kong)" "http://localhost:8000/api/cql/api/v1/cql/evaluations?page=0&size=1" "200"
check_service "Quality Measure (Kong)" "http://localhost:8000/api/quality/quality-measure/results?page=0&size=1" "200"
check_service "FHIR Server (Kong)" "http://localhost:8000/api/fhir/Patient?_count=1" "200"
echo ""

#
# 5. Check Angular Frontend
#
echo -e "${BLUE}5. Angular Frontend${NC}"
echo "-------------------"
if curl -sf http://localhost:4200 > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Clinical Portal - Running (http://localhost:4200)"
else
    echo -e "${RED}✗${NC} Clinical Portal - Not accessible"
fi
echo ""

#
# 6. Quick Data Test
#
echo -e "${BLUE}6. Data Verification${NC}"
echo "-------------------"
cql_count=$(curl -s -H "X-Tenant-ID: default" "http://localhost:8000/api/cql/api/v1/cql/evaluations?page=0&size=1" | grep -o '"totalElements":[0-9]*' | grep -o '[0-9]*' || echo "0")
quality_count=$(curl -s -H "X-Tenant-ID: default" "http://localhost:8000/api/quality/quality-measure/results" | python3 -c "import sys, json; data=json.load(sys.stdin); print(len(data))" 2>/dev/null || echo "0")
fhir_count=$(curl -s "http://localhost:8000/api/fhir/Patient?_count=100" | grep -o '"total":[0-9]*' | grep -o '[0-9]*' | head -1 || echo "0")

echo "CQL Evaluations:     $cql_count records"
echo "Quality Measures:    $quality_count records"
echo "FHIR Patients:       $fhir_count patients"
echo ""

#
# Summary
#
echo "=========================================="
echo -e "${GREEN}Verification Complete${NC}"
echo "=========================================="
echo ""
echo "Access Points:"
echo "--------------"
echo "Clinical Portal:     http://localhost:4200"
echo "Kong API Gateway:    http://localhost:8000"
echo "Kong Admin:          http://localhost:8001"
echo ""
echo "Next Steps:"
echo "-----------"
echo "1. Open browser: http://localhost:4200"
echo "2. View logs: docker-compose logs -f"
echo "3. Test APIs: curl -H 'X-Tenant-ID: default' http://localhost:8000/api/cql/evaluations"
echo ""
