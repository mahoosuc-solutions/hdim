#!/bin/bash

# HealthData in Motion Platform Startup Script
# This script handles all startup, troubleshooting, and validation

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored messages
info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Check if we're in the right directory
if [ ! -f "docker-compose.yml" ]; then
    error "docker-compose.yml not found. Please run this script from the project root."
    exit 1
fi

echo "=========================================="
echo "HealthData in Motion Platform Startup"
echo "=========================================="

# Step 1: Clean up any existing containers
info "Cleaning up existing containers..."
docker compose down -v 2>/dev/null || true
docker ps -a | grep healthdata | awk '{print $1}' | xargs -r docker rm -f 2>/dev/null || true

# Step 2: Build backend if needed
if [ "$1" == "--build" ] || [ ! -d "backend/modules/services/quality-measure-service/build" ]; then
    info "Building backend services..."
    cd backend
    ./gradlew build -x test --parallel || {
        error "Backend build failed"
        exit 1
    }
    cd ..
fi

# Step 3: Start infrastructure services
info "Starting infrastructure services..."
docker compose up -d postgres redis zookeeper kafka

# Wait for PostgreSQL
info "Waiting for PostgreSQL..."
for i in {1..30}; do
    if docker exec healthdata-postgres pg_isready -U healthdata &>/dev/null; then
        info "PostgreSQL is ready!"
        break
    fi
    echo -n "."
    sleep 2
done

# Wait for Kafka
info "Waiting for Kafka..."
for i in {1..30}; do
    if docker exec healthdata-kafka kafka-topics --bootstrap-server localhost:9092 --list &>/dev/null; then
        info "Kafka is ready!"
        break
    fi
    echo -n "."
    sleep 2
done

# Step 4: Build Docker images for services
info "Building service Docker images..."
docker compose build fhir-service quality-measure-service cql-engine-service gateway-service patient-service care-gap-service event-processing-service

# Step 5: Start backend services
info "Starting backend services..."
docker compose up -d fhir-service cql-engine-service patient-service care-gap-service event-processing-service

# Give services time to initialize
sleep 10

# Start quality-measure-service (depends on others)
docker compose up -d quality-measure-service

# Start gateway last
sleep 5
docker compose up -d gateway-service

# Step 6: Health checks
info "Running health checks..."
sleep 20

services=("fhir-service:8081" "quality-measure-service:8087" "cql-engine-service:8086" "gateway-service:8080")

all_healthy=true
for service in "${services[@]}"; do
    IFS=':' read -r name port <<< "$service"
    if curl -f -s "http://localhost:$port/actuator/health" &>/dev/null; then
        info "$name is healthy on port $port"
    else
        warn "$name is not responding on port $port"
        all_healthy=false
    fi
done

# Step 7: Start frontend (if requested)
if [ "$2" == "--with-frontend" ]; then
    info "Starting Clinical Portal frontend..."

    # Check if running in Angular dev mode or Docker
    if [ -d "apps/clinical-portal" ]; then
        cd apps/clinical-portal
        npm install
        npx nx serve clinical-portal &
        cd ../..
    else
        docker compose up -d clinical-portal
    fi
fi

# Step 8: Final status
echo ""
echo "=========================================="
if [ "$all_healthy" = true ]; then
    info "Platform startup completed successfully!"
    echo ""
    echo "Services available at:"
    echo "  - Gateway API: http://localhost:8080"
    echo "  - Quality Measure API: http://localhost:8087"
    echo "  - FHIR API: http://localhost:8081"
    echo "  - CQL Engine API: http://localhost:8086"
    [ "$2" == "--with-frontend" ] && echo "  - Clinical Portal: http://localhost:4200"
    echo ""
    echo "View logs: docker compose logs -f [service-name]"
    echo "Stop all: docker compose down"
else
    warn "Some services may not be fully ready. Check logs with:"
    echo "  docker compose logs [service-name]"
fi
echo "=========================================="