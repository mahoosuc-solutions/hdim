#!/bin/bash
# Health Check Script for HealthData-in-Motion Services
# Checks health of all Docker services
# Usage: ./scripts/health-check.sh [service-name]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local status=$1
    local service=$2
    local message=$3

    if [ "$status" == "success" ]; then
        echo -e "${GREEN}✓${NC} ${service}: ${message}"
    elif [ "$status" == "warning" ]; then
        echo -e "${YELLOW}!${NC} ${service}: ${message}"
    else
        echo -e "${RED}✗${NC} ${service}: ${message}"
    fi
}

# Function to check service health via HTTP
check_http_health() {
    local name=$1
    local url=$2
    local expected_code=${3:-200}

    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")

    if [ "$response" == "$expected_code" ]; then
        print_status "success" "$name" "Healthy (HTTP $response)"
        return 0
    else
        print_status "error" "$name" "Unhealthy (HTTP $response)"
        return 1
    fi
}

# Function to check container status
check_container() {
    local container=$1
    local name=$2

    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        local status=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "none")

        if [ "$status" == "healthy" ]; then
            print_status "success" "$name" "Container healthy"
            return 0
        elif [ "$status" == "none" ]; then
            local running=$(docker inspect --format='{{.State.Running}}' "$container" 2>/dev/null)
            if [ "$running" == "true" ]; then
                print_status "warning" "$name" "Container running (no healthcheck)"
                return 0
            else
                print_status "error" "$name" "Container not running"
                return 1
            fi
        else
            print_status "error" "$name" "Container status: $status"
            return 1
        fi
    else
        print_status "error" "$name" "Container not found"
        return 1
    fi
}

# Main health check
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}HealthData-in-Motion Health Check${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

total=0
passed=0

# Check specific service if provided
if [ -n "$1" ]; then
    SERVICE=$1
    echo -e "${BLUE}Checking service: $SERVICE${NC}"
    echo ""

    case $SERVICE in
        clinical-portal|frontend)
            check_http_health "Clinical Portal" "http://localhost:4200/health"
            exit $?
            ;;
        gateway)
            check_http_health "Gateway Service" "http://localhost:8080/actuator/health"
            exit $?
            ;;
        cql-engine|cql)
            check_http_health "CQL Engine" "http://localhost:8081/actuator/health"
            exit $?
            ;;
        quality-measure|quality)
            check_http_health "Quality Measure" "http://localhost:8087/actuator/health"
            exit $?
            ;;
        postgres|db)
            check_container "healthdata-postgres" "PostgreSQL"
            exit $?
            ;;
        redis)
            check_container "healthdata-redis" "Redis"
            exit $?
            ;;
        kafka)
            check_container "healthdata-kafka" "Kafka"
            exit $?
            ;;
        fhir)
            check_http_health "FHIR Service" "http://localhost:8085/actuator/health"
            exit $?
            ;;
        consent)
            check_http_health "Consent Service" "http://localhost:8082/actuator/health"
            exit $?
            ;;
        event-processing|events)
            check_http_health "Event Processing Service" "http://localhost:8083/actuator/health"
            exit $?
            ;;
        patient)
            check_http_health "Patient Service" "http://localhost:8084/actuator/health"
            exit $?
            ;;
        care-gap)
            check_http_health "Care Gap Service" "http://localhost:8086/actuator/health"
            exit $?
            ;;
        prometheus)
            check_http_health "Prometheus" "http://localhost:9090/-/healthy"
            exit $?
            ;;
        grafana)
            check_http_health "Grafana" "http://localhost:3001/api/health"
            exit $?
            ;;
        *)
            echo -e "${RED}Unknown service: $SERVICE${NC}"
            echo "Available services: clinical-portal, gateway, cql-engine, quality-measure, postgres, redis, kafka, fhir, consent, event-processing, patient, care-gap, prometheus, grafana"
            exit 1
            ;;
    esac
fi

# Check all services
echo -e "${BLUE}Infrastructure Services:${NC}"
echo ""

# PostgreSQL
total=$((total + 1))
if check_container "healthdata-postgres" "PostgreSQL"; then
    passed=$((passed + 1))
fi

# Redis
total=$((total + 1))
if check_container "healthdata-redis" "Redis"; then
    passed=$((passed + 1))
fi

# Kafka
total=$((total + 1))
if check_container "healthdata-kafka" "Kafka"; then
    passed=$((passed + 1))
fi

# Zookeeper
total=$((total + 1))
if check_container "healthdata-zookeeper" "Zookeeper"; then
    passed=$((passed + 1))
fi

echo ""
echo -e "${BLUE}Frontend Services:${NC}"
echo ""

# Clinical Portal
total=$((total + 1))
if check_http_health "Clinical Portal" "http://localhost:4200/health"; then
    passed=$((passed + 1))
fi

echo ""
echo -e "${BLUE}Application Services:${NC}"
echo ""

# Gateway Service
total=$((total + 1))
if check_http_health "Gateway Service" "http://localhost:8080/actuator/health"; then
    passed=$((passed + 1))
fi

# CQL Engine
total=$((total + 1))
if check_http_health "CQL Engine" "http://localhost:8081/actuator/health"; then
    passed=$((passed + 1))
fi

# Quality Measure Service
total=$((total + 1))
if check_http_health "Quality Measure" "http://localhost:8087/actuator/health"; then
    passed=$((passed + 1))
fi

# Consent Service
total=$((total + 1))
if check_http_health "Consent Service" "http://localhost:8082/actuator/health"; then
    passed=$((passed + 1))
fi

# Event Processing Service
total=$((total + 1))
if check_http_health "Event Processing Service" "http://localhost:8083/actuator/health"; then
    passed=$((passed + 1))
fi

# Patient Service
total=$((total + 1))
if check_http_health "Patient Service" "http://localhost:8084/actuator/health"; then
    passed=$((passed + 1))
fi

# FHIR Service
total=$((total + 1))
if check_http_health "FHIR Service" "http://localhost:8085/actuator/health"; then
    passed=$((passed + 1))
fi

# Care Gap Service
total=$((total + 1))
if check_http_health "Care Gap Service" "http://localhost:8086/actuator/health"; then
    passed=$((passed + 1))
fi

echo ""
echo -e "${BLUE}Monitoring Services (Optional):${NC}"
echo ""

# Prometheus (optional)
if docker ps --format '{{.Names}}' | grep -q "healthdata-prometheus"; then
    total=$((total + 1))
    if check_http_health "Prometheus" "http://localhost:9090/-/healthy"; then
        passed=$((passed + 1))
    fi
else
    print_status "warning" "Prometheus" "Not running (optional)"
fi

# Grafana (optional)
if docker ps --format '{{.Names}}' | grep -q "healthdata-grafana"; then
    total=$((total + 1))
    if check_http_health "Grafana" "http://localhost:3001/api/health"; then
        passed=$((passed + 1))
    fi
else
    print_status "warning" "Grafana" "Not running (optional)"
fi

# Summary
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Summary:${NC}"
echo -e "${BLUE}========================================${NC}"

if [ $passed -eq $total ]; then
    echo -e "${GREEN}All services healthy! ($passed/$total)${NC}"
    exit 0
elif [ $passed -ge $((total * 75 / 100)) ]; then
    echo -e "${YELLOW}Most services healthy ($passed/$total)${NC}"
    echo -e "${YELLOW}Some services may need attention${NC}"
    exit 0
else
    echo -e "${RED}Multiple services unhealthy ($passed/$total)${NC}"
    echo -e "${RED}Please check service logs: make logs${NC}"
    exit 1
fi
