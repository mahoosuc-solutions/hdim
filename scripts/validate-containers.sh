#!/bin/bash
# Comprehensive Container and Service Validation Script
# Validates all containers, services, ports, databases, and readiness for testing
# Usage: ./scripts/validate-containers.sh [--verbose] [--fix]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Flags
VERBOSE=false
FIX_ISSUES=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --verbose|-v)
            VERBOSE=true
            shift
            ;;
        --fix|-f)
            FIX_ISSUES=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--verbose] [--fix]"
            exit 1
            ;;
    esac
done

# Counters
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0
WARNING_CHECKS=0

# Function to print colored output
print_status() {
    local status=$1
    local service=$2
    local message=$3

    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))

    if [ "$status" == "success" ]; then
        echo -e "${GREEN}✓${NC} ${service}: ${message}"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 0
    elif [ "$status" == "warning" ]; then
        echo -e "${YELLOW}⚠${NC} ${service}: ${message}"
        WARNING_CHECKS=$((WARNING_CHECKS + 1))
        return 0
    else
        echo -e "${RED}✗${NC} ${service}: ${message}"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        return 1
    fi
}

# Function to check if Docker is running
check_docker() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Docker Environment Check${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""

    if ! command -v docker &> /dev/null; then
        print_status "error" "Docker" "Not installed"
        return 1
    fi

    if ! docker ps &> /dev/null; then
        print_status "error" "Docker Daemon" "Not running"
        if [ "$FIX_ISSUES" == true ]; then
            echo -e "${CYAN}Attempting to start Docker...${NC}"
            if command -v systemctl &> /dev/null; then
                sudo systemctl start docker 2>/dev/null || true
            elif command -v service &> /dev/null; then
                sudo service docker start 2>/dev/null || true
            fi
            sleep 2
            if docker ps &> /dev/null; then
                print_status "success" "Docker Daemon" "Started successfully"
            else
                print_status "error" "Docker Daemon" "Could not start automatically"
                return 1
            fi
        else
            return 1
        fi
    else
        DOCKER_VERSION=$(docker --version | cut -d' ' -f3 | cut -d',' -f1)
        print_status "success" "Docker" "Running (v${DOCKER_VERSION})"
    fi

    if docker compose version &> /dev/null || docker-compose version &> /dev/null; then
        print_status "success" "Docker Compose" "Available"
    else
        print_status "error" "Docker Compose" "Not available"
        return 1
    fi

    echo ""
    return 0
}

# Function to find container by pattern (supports both hdim-demo-* and healthdata-*)
find_container() {
    local pattern=$1
    docker ps --format '{{.Names}}' | grep -E "^${pattern}" | head -1
}

# Function to check container status
check_container() {
    local container=$1
    local name=$2
    local required=${3:-true}
    
    # Try exact match first, then pattern match
    local found_container=$(find_container "${container}")
    if [ -z "$found_container" ]; then
        # Try alternative naming (hdim-demo-* vs healthdata-*)
        if [[ "$container" == healthdata-* ]]; then
            found_container=$(find_container "hdim-demo-${container#healthdata-}")
        elif [[ "$container" == hdim-demo-* ]]; then
            found_container=$(find_container "healthdata-${container#hdim-demo-}")
        fi
    fi
    
    if [ -z "$found_container" ]; then
        found_container="$container"  # Use original for error message
    fi

    if docker ps --format '{{.Names}}' | grep -q "^${found_container}$"; then
        local status=$(docker inspect --format='{{.State.Health.Status}}' "$found_container" 2>/dev/null || echo "none")
        local running=$(docker inspect --format='{{.State.Running}}' "$found_container" 2>/dev/null)

        if [ "$status" == "healthy" ]; then
            print_status "success" "$name" "Healthy"
            return 0
        elif [ "$status" == "none" ] && [ "$running" == "true" ]; then
            print_status "warning" "$name" "Running (no healthcheck)"
            return 0
        elif [ "$running" == "true" ]; then
            print_status "warning" "$name" "Running but unhealthy (status: $status)"
            return 0
        else
            if [ "$required" == "true" ]; then
                print_status "error" "$name" "Not running"
                return 1
            else
                print_status "warning" "$name" "Not running (optional)"
                return 0
            fi
        fi
    else
        if [ "$required" == "true" ]; then
            print_status "error" "$name" "Container not found"
            return 1
        else
            print_status "warning" "$name" "Container not found (optional)"
            return 0
        fi
    fi
}

# Function to check HTTP health endpoint
check_http_health() {
    local name=$1
    local url=$2
    local expected_code=${3:-200}
    local timeout=${4:-5}

    if [ "$VERBOSE" == true ]; then
        echo -e "${CYAN}  Checking: $url${NC}"
    fi

    response=$(curl -s -o /dev/null -w "%{http_code}" --max-time $timeout "$url" 2>/dev/null || echo "000")

    if [ "$response" == "$expected_code" ]; then
        print_status "success" "$name" "Healthy (HTTP $response)"
        return 0
    elif [ "$response" == "000" ]; then
        print_status "error" "$name" "Unreachable (timeout/connection error)"
        return 1
    else
        print_status "error" "$name" "Unhealthy (HTTP $response)"
        return 1
    fi
}

# Function to check port availability
check_port() {
    local port=$1
    local service=$2
    local required=${3:-true}

    if command -v netstat &> /dev/null; then
        if netstat -tuln 2>/dev/null | grep -q ":$port "; then
            print_status "success" "$service (Port $port)" "In use"
            return 0
        fi
    elif command -v ss &> /dev/null; then
        if ss -tuln 2>/dev/null | grep -q ":$port "; then
            print_status "success" "$service (Port $port)" "In use"
            return 0
        fi
    elif command -v lsof &> /dev/null; then
        if lsof -i :$port &> /dev/null; then
            print_status "success" "$service (Port $port)" "In use"
            return 0
        fi
    fi

    if [ "$required" == "true" ]; then
        print_status "error" "$service (Port $port)" "Not in use"
        return 1
    else
        print_status "warning" "$service (Port $port)" "Not in use (optional)"
        return 0
    fi
}

# Function to check database connectivity
check_database() {
    local container=$1
    local db_name=$2
    local user=$3
    local password=$4

    local found_container=$(find_container "${container}")
    if [ -z "$found_container" ] && [[ "$container" == healthdata-* ]]; then
        found_container=$(find_container "hdim-demo-${container#healthdata-}")
    fi
    
    if [ -n "$found_container" ] && docker ps --format '{{.Names}}' | grep -q "^${found_container}$"; then
        if docker exec "$found_container" pg_isready -U "$user" -d "$db_name" &> /dev/null; then
            print_status "success" "PostgreSQL ($db_name)" "Accepting connections"
            return 0
        else
            print_status "error" "PostgreSQL ($db_name)" "Not accepting connections"
            return 1
        fi
    else
        print_status "error" "PostgreSQL ($db_name)" "Container not running"
        return 1
    fi
}

# Function to check Redis connectivity
check_redis() {
    local container=$1

    local found_container=$(find_container "${container}")
    if [ -z "$found_container" ] && [[ "$container" == healthdata-* ]]; then
        found_container=$(find_container "hdim-demo-${container#healthdata-}")
    fi
    
    if [ -n "$found_container" ] && docker ps --format '{{.Names}}' | grep -q "^${found_container}$"; then
        if docker exec "$found_container" redis-cli ping 2>/dev/null | grep -q "PONG"; then
            print_status "success" "Redis" "Responding to PING"
            return 0
        else
            print_status "error" "Redis" "Not responding"
            return 1
        fi
    else
        print_status "error" "Redis" "Container not running"
        return 1
    fi
}

# Function to check Kafka connectivity
check_kafka() {
    local container=$1

    local found_container=$(find_container "${container}")
    if [ -z "$found_container" ] && [[ "$container" == healthdata-* ]]; then
        found_container=$(find_container "hdim-demo-${container#healthdata-}")
    fi
    
    if [ -n "$found_container" ] && docker ps --format '{{.Names}}' | grep -q "^${found_container}$"; then
        if docker exec "$found_container" kafka-broker-api-versions --bootstrap-server localhost:29092 &> /dev/null; then
            print_status "success" "Kafka" "Broker accessible"
            return 0
        else
            print_status "error" "Kafka" "Broker not accessible"
            return 1
        fi
    else
        print_status "error" "Kafka" "Container not running"
        return 1
    fi
}

# Main validation
main() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}HDIM Container & Service Validation${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo -e "${CYAN}Validating all containers and services...${NC}"
    echo ""

    # Step 1: Check Docker environment
    if ! check_docker; then
        echo ""
        echo -e "${RED}Docker environment check failed. Please fix Docker issues first.${NC}"
        exit 1
    fi

    # Step 2: Check Infrastructure Containers
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Infrastructure Services${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""

    check_container "healthdata-postgres" "PostgreSQL" true
    check_container "healthdata-redis" "Redis" true
    check_container "healthdata-kafka" "Kafka" true
    check_container "healthdata-zookeeper" "Zookeeper" true
    check_container "healthdata-jaeger" "Jaeger" false

    # Step 3: Check Database Connectivity
    echo ""
    echo -e "${BLUE}Database Connectivity${NC}"
    echo ""

    if docker ps --format '{{.Names}}' | grep -q "^healthdata-postgres$"; then
        check_database "healthdata-postgres" "healthdata_db" "healthdata" "healthdata_password"
    fi

    # Step 4: Check Redis Connectivity
    echo ""
    echo -e "${BLUE}Redis Connectivity${NC}"
    echo ""

    if docker ps --format '{{.Names}}' | grep -q "^healthdata-redis$"; then
        check_redis "healthdata-redis"
    fi

    # Step 5: Check Kafka Connectivity
    echo ""
    echo -e "${BLUE}Kafka Connectivity${NC}"
    echo ""

    if docker ps --format '{{.Names}}' | grep -q "^healthdata-kafka$"; then
        check_kafka "healthdata-kafka"
    fi

    # Step 6: Check Backend Services
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Backend Services${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""

    # Check gateway on both ports (demo uses 18080, standard uses 8080)
    if curl -s --max-time 2 "http://localhost:18080/actuator/health" &> /dev/null; then
        check_http_health "Gateway Service (Edge)" "http://localhost:18080/actuator/health" 200
    else
        check_http_health "Gateway Service" "http://localhost:8080/actuator/health" 200
    fi
    check_http_health "CQL Engine" "http://localhost:8081/cql-engine/actuator/health" 200
    # Consent Service (optional - not in demo environment)
    if curl -s --max-time 2 "http://localhost:8082/consent/actuator/health" &> /dev/null; then
        check_http_health "Consent Service" "http://localhost:8082/consent/actuator/health" 200
    else
        print_status "warning" "Consent Service (8082)" "Not running (optional for demo)"
    fi
    check_http_health "Event Processing" "http://localhost:8083/events/actuator/health" 200
    check_http_health "Patient Service" "http://localhost:8084/patient/actuator/health" 200
    check_http_health "FHIR Service" "http://localhost:8085/fhir/actuator/health" 200
    check_http_health "Care Gap Service" "http://localhost:8086/care-gap/actuator/health" 200
    check_http_health "Quality Measure" "http://localhost:8087/quality-measure/actuator/health" 200

    # Optional services
    if curl -s --max-time 2 "http://localhost:8088/hcc/actuator/health" &> /dev/null; then
        check_http_health "HCC Service" "http://localhost:8088/hcc/actuator/health" 200 2
    else
        print_status "warning" "HCC Service" "Not running (optional for demo)"
    fi
    if curl -s --max-time 2 "http://localhost:8090/sdoh/actuator/health" &> /dev/null; then
        check_http_health "SDOH Service" "http://localhost:8090/sdoh/actuator/health" 200 2
    else
        print_status "warning" "SDOH Service" "Not running (optional for demo)"
    fi
    # Consent Service on 8091 (optional)
    if curl -s --max-time 2 "http://localhost:8091/consent/actuator/health" &> /dev/null; then
        check_http_health "Consent Service (8091)" "http://localhost:8091/consent/actuator/health" 200 2
    else
        print_status "warning" "Consent Service (8091)" "Not running (optional)"
    fi

    # Step 7: Check Frontend Services
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Frontend Services${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""

    check_http_health "Clinical Portal" "http://localhost:4200" 200 3

    # Step 8: Check Port Availability
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Port Availability${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""

    check_port 5435 "PostgreSQL" true
    check_port 6380 "Redis" true
    check_port 9094 "Kafka" true
    check_port 2182 "Zookeeper" true
    # Check gateway on correct port (demo uses 18080, standard uses 8080)
    if netstat -tuln 2>/dev/null | grep -q ":18080 " || ss -tuln 2>/dev/null | grep -q ":18080 " || lsof -i :18080 &> /dev/null; then
        check_port 18080 "Gateway Edge" true
    else
        check_port 8080 "Gateway" true
    fi
    check_port 8081 "CQL Engine" true
    check_port 8085 "FHIR Service" true
    check_port 4200 "Clinical Portal" true
    check_port 16686 "Jaeger" false

    # Step 9: Check Service Dependencies
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Service Dependencies${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""

    # Check if services can connect to databases
    local postgres_container=$(find_container "healthdata-postgres")
    if [ -z "$postgres_container" ]; then
        postgres_container=$(find_container "hdim-demo-postgres")
    fi
    
    if [ -n "$postgres_container" ] && docker ps --format '{{.Names}}' | grep -q "^${postgres_container}$"; then
        if docker exec "$postgres_container" psql -U healthdata -d healthdata_db -c "SELECT 1;" &> /dev/null; then
            print_status "success" "Database Queries" "PostgreSQL accepting queries"
        else
            print_status "error" "Database Queries" "PostgreSQL not accepting queries"
        fi
    fi

    # Summary
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Validation Summary${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo -e "Total Checks: ${CYAN}$TOTAL_CHECKS${NC}"
    echo -e "Passed: ${GREEN}$PASSED_CHECKS${NC}"
    echo -e "Warnings: ${YELLOW}$WARNING_CHECKS${NC}"
    echo -e "Failed: ${RED}$FAILED_CHECKS${NC}"
    echo ""

    # Determine exit code
    if [ $FAILED_CHECKS -eq 0 ]; then
        if [ $WARNING_CHECKS -eq 0 ]; then
            echo -e "${GREEN}✅ All checks passed! System is ready for testing.${NC}"
            exit 0
        else
            echo -e "${YELLOW}⚠️  All critical checks passed, but some warnings exist.${NC}"
            echo -e "${YELLOW}System is ready for testing, but optional services may be unavailable.${NC}"
            exit 0
        fi
    else
        echo -e "${RED}❌ Some critical checks failed.${NC}"
        echo -e "${RED}Please review the errors above and fix them before testing.${NC}"
        echo ""
        echo -e "${CYAN}To view service logs:${NC}"
        echo -e "  docker logs <container-name>"
        echo -e "  docker compose logs <service-name>"
        echo ""
        if [ "$FIX_ISSUES" != true ]; then
            echo -e "${CYAN}To attempt automatic fixes, run:${NC}"
            echo -e "  $0 --fix"
        fi
        exit 1
    fi
}

# Run main validation
main
