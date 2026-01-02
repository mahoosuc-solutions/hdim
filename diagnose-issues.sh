#!/bin/bash

# HealthData Platform Diagnostic Script
# Identifies and provides solutions for common startup issues

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
debug() { echo -e "${BLUE}[DEBUG]${NC} $1"; }

echo "=========================================="
echo "HealthData Platform Diagnostic Tool"
echo "=========================================="

# Check Docker
echo ""
info "Checking Docker..."
if ! docker --version &>/dev/null; then
    error "Docker is not installed or not running"
    echo "Solution: Install Docker Desktop or start Docker service"
    exit 1
fi
info "Docker version: $(docker --version)"

# Check Docker Compose
if ! docker compose version &>/dev/null; then
    error "Docker Compose v2 is not available"
    echo "Solution: Update Docker Desktop to latest version"
    exit 1
fi
info "Docker Compose version: $(docker compose version)"

# Check Java version
echo ""
info "Checking Java..."
if command -v java &>/dev/null; then
    java_version=$(java -version 2>&1 | head -n 1)
    info "Java: $java_version"

    # Check if Java 21
    if [[ ! "$java_version" =~ "21" ]]; then
        warn "Java 21 is recommended for building the project"
    fi
else
    warn "Java not found in PATH (required for local development)"
fi

# Check running containers
echo ""
info "Checking running containers..."
running_containers=$(docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep healthdata || true)

if [ -z "$running_containers" ]; then
    warn "No HealthData containers are running"
else
    echo "$running_containers"
fi

# Check for port conflicts
echo ""
info "Checking for port conflicts..."
ports=(5435 6380 9094 8080 8081 8082 8083 8084 8086 8087 4200)

for port in "${ports[@]}"; do
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        process=$(lsof -Pi :$port -sTCP:LISTEN 2>/dev/null | tail -n 1)
        warn "Port $port is in use: $process"
    else
        debug "Port $port is available"
    fi
done

# Check container logs for errors
echo ""
info "Checking container logs for errors..."
services=("postgres" "kafka" "fhir-service" "quality-measure-service" "cql-engine-service")

for service in "${services[@]}"; do
    container="healthdata-$service"
    if docker ps -a | grep -q "$container"; then
        errors=$(docker logs "$container" 2>&1 | grep -i "error\|exception\|fail" | tail -3 || true)
        if [ -n "$errors" ]; then
            warn "Errors found in $service:"
            echo "$errors" | head -3
            echo ""
        fi
    fi
done

# Check database connectivity
echo ""
info "Checking database connectivity..."
if docker ps | grep -q healthdata-postgres; then
    if docker exec healthdata-postgres psql -U healthdata -d healthdata_db -c '\l' &>/dev/null; then
        info "PostgreSQL is accessible"

        # Check if databases exist
        dbs=$(docker exec healthdata-postgres psql -U healthdata -d healthdata_db -c '\l' 2>/dev/null | grep "_db" | wc -l)
        info "Found $dbs application databases"
    else
        error "Cannot connect to PostgreSQL"
        echo "Solution: Check PostgreSQL logs: docker logs healthdata-postgres"
    fi
else
    warn "PostgreSQL container is not running"
fi

# Check Kafka
echo ""
info "Checking Kafka..."
if docker ps | grep -q healthdata-kafka; then
    if docker exec healthdata-kafka kafka-topics --bootstrap-server localhost:9092 --list &>/dev/null; then
        info "Kafka is accessible"
    else
        warn "Kafka is running but not fully initialized"
    fi
else
    warn "Kafka container is not running"
fi

# Common issues and solutions
echo ""
echo "=========================================="
echo "Common Issues and Solutions:"
echo "=========================================="

# Check for HQL/JPA errors
if docker logs healthdata-quality-measure-service 2>&1 | grep -q "HqlParser\|QuerySyntaxException"; then
    error "JPA/HQL Query Syntax Error detected"
    echo "Solution:"
    echo "  1. Check repository classes for malformed @Query annotations"
    echo "  2. Ensure proper JSONB query syntax for PostgreSQL"
    echo "  3. Review recent changes to entity classes"
    echo ""
fi

# Check for class version errors
if docker logs healthdata-quality-measure-service 2>&1 | grep -q "UnsupportedClassVersionError"; then
    error "Java version mismatch detected"
    echo "Solution:"
    echo "  1. Rebuild with: cd backend && ./gradlew clean build -x test"
    echo "  2. Ensure Dockerfiles use: FROM eclipse-temurin:21-jdk-alpine"
    echo "  3. Rebuild Docker images: docker compose build --no-cache"
    echo ""
fi

# Check for connection errors
if docker logs healthdata-quality-measure-service 2>&1 | grep -q "Connection refused\|Cannot connect"; then
    error "Service connectivity issues detected"
    echo "Solution:"
    echo "  1. Ensure all dependent services are running"
    echo "  2. Check service URLs in docker-compose.yml environment variables"
    echo "  3. Verify network configuration"
    echo ""
fi

echo "=========================================="
info "Diagnostic complete"

# Provide recommendations
echo ""
echo "Recommended next steps:"
if [ -z "$running_containers" ]; then
    echo "  1. Start the platform: ./start-platform.sh"
else
    echo "  1. View logs: docker compose logs -f [service-name]"
    echo "  2. Restart services: docker compose restart"
fi
echo "  3. Clean restart: docker compose down -v && ./start-platform.sh --build"
echo "=========================================="