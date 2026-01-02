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

PROFILE="core"
BUILD_IMAGES=false
WITH_FRONTEND=false
FRONTEND_MODE="docker"
CLEAN=false

usage() {
    echo "Usage: $0 [--build] [--with-frontend] [--frontend local|docker] [--profile <name>] [--full] [--clean]"
    echo "  --build            Build backend artifacts and Docker images"
    echo "  --with-frontend    Start Clinical Portal"
    echo "  --frontend         Frontend mode: docker (default) or local (Nx dev server)"
    echo "  --profile          Docker Compose profile (default: core)"
    echo "  --full             Shortcut for --profile full"
    echo "  --clean            Remove containers and volumes before starting"
}

while [[ $# -gt 0 ]]; do
    case $1 in
        --build)
            BUILD_IMAGES=true
            shift
            ;;
        --with-frontend)
            WITH_FRONTEND=true
            shift
            ;;
        --frontend)
            FRONTEND_MODE=${2:-docker}
            WITH_FRONTEND=true
            shift 2
            ;;
        --frontend=*)
            FRONTEND_MODE="${1#*=}"
            WITH_FRONTEND=true
            shift
            ;;
        --profile)
            PROFILE=${2:-core}
            shift 2
            ;;
        --full)
            PROFILE="full"
            shift
            ;;
        --clean)
            CLEAN=true
            shift
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Check if we're in the right directory
if [ ! -f "docker-compose.yml" ]; then
    error "docker-compose.yml not found. Please run this script from the project root."
    exit 1
fi

echo "=========================================="
echo "HealthData in Motion Platform Startup"
echo "=========================================="

# Step 1: Optional cleanup
if [ "$CLEAN" = true ]; then
    info "Cleaning up existing containers..."
    docker compose down -v 2>/dev/null || true
    docker ps -a | grep healthdata | awk '{print $1}' | xargs -r docker rm -f 2>/dev/null || true
fi

# Step 2: Build backend artifacts if needed
BACKEND_SERVICES=(
    "gateway-service"
    "cql-engine-service"
    "consent-service"
    "event-processing-service"
    "patient-service"
    "fhir-service"
    "care-gap-service"
    "quality-measure-service"
)

missing_artifacts=()
for service in "${BACKEND_SERVICES[@]}"; do
    if ! ls "backend/modules/services/${service}/build/libs/"*.jar >/dev/null 2>&1; then
        missing_artifacts+=("$service")
    fi
done

if [ "$BUILD_IMAGES" = true ] || [ ${#missing_artifacts[@]} -gt 0 ]; then
    if [ ${#missing_artifacts[@]} -gt 0 ]; then
        warn "Missing build artifacts for: ${missing_artifacts[*]}"
    fi
    info "Building backend services with Gradle..."
    (cd backend && ./gradlew build -x test --parallel) || {
        error "Backend build failed"
        exit 1
    }
fi

# Step 3: Start infrastructure services
info "Starting infrastructure services..."
docker compose --profile "$PROFILE" up -d postgres redis zookeeper kafka

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

# Step 4: Build Docker images for services (optional)
CORE_SERVICES=(
    "fhir-service"
    "cql-engine-service"
    "consent-service"
    "event-processing-service"
    "patient-service"
    "care-gap-service"
    "quality-measure-service"
    "gateway-service"
)

if [ "$BUILD_IMAGES" = true ]; then
    info "Building service Docker images..."
    docker compose --profile "$PROFILE" build "${CORE_SERVICES[@]}"
fi

# Step 5: Start backend services
info "Starting backend services..."
docker compose --profile "$PROFILE" up -d "${CORE_SERVICES[@]}"

# Step 6: Health checks
info "Running health checks..."
sleep 20

services=(
    "gateway-service:8080"
    "cql-engine-service:8081"
    "consent-service:8082"
    "event-processing-service:8083"
    "patient-service:8084"
    "fhir-service:8085"
    "care-gap-service:8086"
    "quality-measure-service:8087"
)

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
if [ "$WITH_FRONTEND" = true ]; then
    info "Starting Clinical Portal frontend..."

    if [ "$FRONTEND_MODE" = "local" ]; then
        info "Starting Nx dev server for Clinical Portal..."
        npm install
        npm run nx -- serve clinical-portal &
    else
        docker compose --profile "$PROFILE" up -d clinical-portal
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
    echo "  - FHIR API: http://localhost:8085"
    echo "  - CQL Engine API: http://localhost:8081"
    [ "$WITH_FRONTEND" = true ] && echo "  - Clinical Portal: http://localhost:4200"
    echo ""
    echo "View logs: docker compose logs -f [service-name]"
    echo "Stop all: docker compose down"
else
    warn "Some services may not be fully ready. Check logs with:"
    echo "  docker compose logs [service-name]"
fi
echo "=========================================="
