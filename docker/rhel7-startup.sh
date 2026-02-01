#!/bin/bash
#
# RHEL 7 Startup Script for HealthData In Motion Platform
#
# This script starts all services in the correct order within the RHEL 7 container
#

set -e

echo "=========================================="
echo "HealthData In Motion Platform"
echo "RHEL 7 Deployment Environment"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

function log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

function log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

function log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Change to project directory
cd /home/healthdata/healthdata-in-motion

#
# Step 1: Start Docker daemon
#
log_info "Starting Docker daemon..."
sudo dockerd > /var/log/dockerd.log 2>&1 &
sleep 5

# Wait for Docker to be ready
DOCKER_READY=0
for i in {1..30}; do
    if docker info > /dev/null 2>&1; then
        DOCKER_READY=1
        break
    fi
    log_info "Waiting for Docker daemon to start... ($i/30)"
    sleep 2
done

if [ $DOCKER_READY -eq 0 ]; then
    log_error "Docker daemon failed to start"
    exit 1
fi

log_info "Docker daemon is running"

#
# Step 2: Pull required Docker images
#
log_info "Pulling Docker images (this may take a while)..."
docker pull postgres:16-alpine
docker pull redis:7-alpine
docker pull confluentinc/cp-zookeeper:7.5.0
docker pull confluentinc/cp-kafka:7.5.0
docker pull kong:3.4
docker pull pantsel/konga:latest

#
# Step 3: Start backend services
#
log_info "Starting backend services..."
docker-compose up -d postgres redis zookeeper kafka

# Wait for database to be ready
log_info "Waiting for PostgreSQL to be ready..."
sleep 10

#
# Step 4: Build and start Java services
#
log_info "Building Java services..."
cd backend

# Set JAVA_HOME for Gradle
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk

# Build all services
./gradlew clean build -x test || log_warn "Some tests failed, continuing..."

# Start services with Docker
cd ..
docker-compose up -d cql-engine quality-measure fhir-mock

log_info "Waiting for backend services to start..."
sleep 20

#
# Step 5: Start Kong API Gateway
#
log_info "Starting Kong API Gateway..."
docker-compose -f kong/docker-compose-kong.yml up -d

# Wait for Kong to be ready
log_info "Waiting for Kong to be healthy..."
sleep 30

KONG_READY=0
for i in {1..20}; do
    if curl -sf http://localhost:8001/ > /dev/null 2>&1; then
        KONG_READY=1
        break
    fi
    log_info "Waiting for Kong Admin API... ($i/20)"
    sleep 3
done

if [ $KONG_READY -eq 0 ]; then
    log_warn "Kong may not be fully ready yet"
else
    log_info "Kong is ready"

    # Configure Kong routes
    log_info "Configuring Kong routes and plugins..."
    ./kong/kong-setup.sh || log_warn "Kong setup had issues, check logs"
fi

#
# Step 6: Start Angular frontend
#
log_info "Starting Angular Clinical Portal..."
cd apps/clinical-portal

# Serve the production build if it exists, otherwise development mode
if [ -d "../../dist/apps/clinical-portal" ]; then
    log_info "Serving production build..."
    npx http-server ../../dist/apps/clinical-portal -p 4200 --proxy http://localhost:8000 > /var/log/angular.log 2>&1 &
else
    log_info "Serving development build..."
    npx nx serve clinical-portal --host 0.0.0.0 --port 4200 > /var/log/angular.log 2>&1 &
fi

cd ../..

#
# Step 7: Display status and access information
#
sleep 10

echo ""
echo "=========================================="
log_info "Platform Startup Complete!"
echo "=========================================="
echo ""
echo "Service Status:"
echo "---------------"
docker-compose ps

echo ""
echo "Access Points:"
echo "--------------"
echo "  Clinical Portal:     http://localhost:4200"
echo "  Kong API Gateway:    http://localhost:8000"
echo "  Kong Admin API:      http://localhost:8001"
echo "  Konga Admin UI:      http://localhost:1337"
echo ""

echo "API Endpoints (via Kong):"
echo "-------------------------"
echo "  CQL Engine:          http://localhost:8000/api/cql"
echo "  Quality Measure:     http://localhost:8000/api/quality"
echo "  FHIR Server:         http://localhost:8000/api/fhir"
echo ""

echo "Direct Service Access (Development):"
echo "------------------------------------"
echo "  CQL Engine:          http://localhost:8081/cql-engine"
echo "  Quality Measure:     http://localhost:8087/quality-measure"
echo "  FHIR Server:         http://localhost:8083/fhir"
echo ""

log_info "Logs available at:"
echo "  Docker:   /var/log/dockerd.log"
echo "  Angular:  /var/log/angular.log"
echo ""

#
# Step 8: Keep container running and tail logs
#
log_info "Tailing logs (Ctrl+C to exit)..."
tail -f /var/log/angular.log /var/log/dockerd.log
