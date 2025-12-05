#!/bin/bash
#
# Deploy Script for HealthData-in-Motion Distributed Architecture
# Deploys the complete stack using Docker Compose
# Usage: ./scripts/deploy.sh [--build] [--monitoring]
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse arguments
BUILD_FLAG=""
MONITORING_FLAG=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --build)
            BUILD_FLAG="--build"
            shift
            ;;
        --monitoring)
            MONITORING_FLAG="--profile monitoring"
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Usage: $0 [--build] [--monitoring]"
            echo "  --build       Build images before deploying"
            echo "  --monitoring  Include Prometheus and Grafana"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}HealthData-in-Motion Deployment${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}ERROR: Docker is not running${NC}"
    echo "Please start Docker and try again"
    exit 1
fi

# Check if docker-compose.yml exists
if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}ERROR: docker-compose.yml not found${NC}"
    echo "Please run this script from the project root directory"
    exit 1
fi

# Step 1: Build images if requested
if [ -n "$BUILD_FLAG" ]; then
    echo -e "${BLUE}Step 1: Building Docker images...${NC}"
    if [ -f "scripts/build-all.sh" ]; then
        ./scripts/build-all.sh
        if [ $? -ne 0 ]; then
            echo -e "${RED}Build failed. Aborting deployment.${NC}"
            exit 1
        fi
    else
        echo -e "${YELLOW}Warning: scripts/build-all.sh not found. Skipping build.${NC}"
    fi
    echo ""
else
    echo -e "${YELLOW}Skipping build step (use --build to rebuild images)${NC}"
    echo ""
fi

# Step 2: Stop existing containers
echo -e "${BLUE}Step 2: Stopping existing containers...${NC}"
docker compose down
echo -e "${GREEN}✓ Containers stopped${NC}"
echo ""

# Step 3: Pull latest images (for services using pre-built images)
echo -e "${BLUE}Step 3: Pulling latest images...${NC}"
docker compose pull || echo -e "${YELLOW}Warning: Some images could not be pulled${NC}"
echo ""

# Step 4: Start containers
echo -e "${BLUE}Step 4: Starting containers...${NC}"
if [ -n "$MONITORING_FLAG" ]; then
    echo -e "${BLUE}Including monitoring services (Prometheus + Grafana)${NC}"
    docker compose $MONITORING_FLAG up -d
else
    docker compose up -d
fi
echo -e "${GREEN}✓ Containers started${NC}"
echo ""

# Step 5: Wait for services to be healthy
echo -e "${BLUE}Step 5: Waiting for services to initialize...${NC}"
echo "This may take up to 2 minutes for all services to become healthy..."
sleep 30

# Show container status
echo ""
echo -e "${BLUE}Container Status:${NC}"
docker compose ps
echo ""

# Step 6: Run health checks
echo -e "${BLUE}Step 6: Running health checks...${NC}"
sleep 10  # Give services a bit more time

if [ -f "scripts/health-check.sh" ]; then
    ./scripts/health-check.sh
    HEALTH_STATUS=$?
else
    echo -e "${YELLOW}Warning: scripts/health-check.sh not found${NC}"
    HEALTH_STATUS=0
fi

echo ""

# Step 7: Display deployment summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Deployment Complete!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

if [ $HEALTH_STATUS -eq 0 ]; then
    echo -e "${GREEN}✅ All services are healthy${NC}"
else
    echo -e "${YELLOW}⚠️  Some services may need attention${NC}"
fi

echo ""
echo -e "${BLUE}Access Points:${NC}"
echo ""
echo -e "  ${GREEN}Frontend:${NC}"
echo "    Clinical Portal:    http://localhost:4200"
echo ""
echo -e "  ${GREEN}API Gateway (Kong):${NC}"
echo "    Gateway:            http://localhost:8000"
echo "    Admin API:          http://localhost:8001"
echo "    Admin UI:           http://localhost:8002"
echo ""
echo -e "  ${GREEN}Backend Services (Direct):${NC}"
echo "    CQL Engine:         http://localhost:8081/cql-engine/actuator/health"
echo "    Quality Measure:    http://localhost:8087/quality-measure/actuator/health"
echo "    Gateway Service:    http://localhost:9000/actuator/health"
echo "    FHIR Server:        http://localhost:8083/fhir/metadata"
echo ""
echo -e "  ${GREEN}Infrastructure:${NC}"
echo "    PostgreSQL:         localhost:5435"
echo "    Redis:              localhost:6380"
echo "    Kafka:              localhost:9094"
echo ""

if [ -n "$MONITORING_FLAG" ]; then
    echo -e "  ${GREEN}Monitoring:${NC}"
    echo "    Prometheus:         http://localhost:9090"
    echo "    Grafana:            http://localhost:3001"
    echo "      (Default credentials: admin/admin)"
    echo ""
fi

echo -e "${BLUE}Useful Commands:${NC}"
echo "  View logs:          docker compose logs -f [service-name]"
echo "  Restart service:    docker compose restart [service-name]"
echo "  Stop all:           docker compose down"
echo "  Health check:       ./scripts/health-check.sh"
echo ""

if [ $HEALTH_STATUS -ne 0 ]; then
    echo -e "${YELLOW}Troubleshooting:${NC}"
    echo "  Check service logs: docker compose logs -f"
    echo "  Verify containers:  docker compose ps"
    echo "  Run health check:   ./scripts/health-check.sh"
    echo ""
fi
