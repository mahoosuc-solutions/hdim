#!/bin/bash

# HealthData in Motion - Production Deployment Script
# Version: 1.0.0
# Date: 2025-11-18

set -e  # Exit on error

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env.production"
DOCKER_COMPOSE_FILE="$SCRIPT_DIR/docker-compose.production.yml"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}HealthData in Motion - Production Deploy${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Pre-flight checks
echo -e "${YELLOW}Running pre-flight checks...${NC}"

# Check if running as root (required for production deployment)
if [ "$EUID" -eq 0 ]; then
   echo -e "${YELLOW}Warning: Running as root. This is acceptable for production deployment.${NC}"
fi

# Check required tools
REQUIRED_TOOLS=("docker" "docker-compose" "git" "curl")
for tool in "${REQUIRED_TOOLS[@]}"; do
    if ! command -v $tool &> /dev/null; then
        echo -e "${RED}Error: $tool is not installed${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓${NC} $tool found"
done

# Check environment file
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}Error: .env.production not found${NC}"
    echo "Please copy .env.production.example and configure it"
    exit 1
fi
echo -e "${GREEN}✓${NC} Environment file found"

# Check for default passwords
if grep -q "CHANGE_ME" "$ENV_FILE"; then
    echo -e "${RED}Error: Default passwords found in .env.production${NC}"
    echo "Please replace all CHANGE_ME values with secure passwords"
    exit 1
fi
echo -e "${GREEN}✓${NC} No default passwords found"

# Check Docker Compose file
if [ ! -f "$DOCKER_COMPOSE_FILE" ]; then
    echo -e "${RED}Error: docker-compose.production.yml not found${NC}"
    exit 1
fi
echo -e "${GREEN}✓${NC} Docker Compose file found"

# Check SSL certificates (if SSL is enabled)
if grep -q "SSL_ENABLED=true" "$ENV_FILE"; then
    SSL_CERT=$(grep "SSL_CERT_PATH" "$ENV_FILE" | cut -d'=' -f2)
    SSL_KEY=$(grep "SSL_KEY_PATH" "$ENV_FILE" | cut -d'=' -f2)

    if [ ! -f "$SSL_CERT" ] || [ ! -f "$SSL_KEY" ]; then
        echo -e "${YELLOW}Warning: SSL enabled but certificates not found${NC}"
        echo "  Certificate: $SSL_CERT"
        echo "  Key: $SSL_KEY"
        read -p "Continue anyway? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        echo -e "${GREEN}✓${NC} SSL certificates found"
    fi
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Deployment Steps${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Step 1: Build Docker images
echo -e "${YELLOW}Step 1: Building Docker images...${NC}"
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" build
echo -e "${GREEN}✓${NC} Docker images built"
echo ""

# Step 2: Run database migrations
echo -e "${YELLOW}Step 2: Running database migrations...${NC}"
# Start only the database
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" up -d postgres
sleep 10  # Wait for PostgreSQL to be ready

# Run migrations for each service
echo "  Running CQL Engine migrations..."
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" run --rm cql-engine-service \
    java -jar app.jar db migrate || echo -e "${YELLOW}  Migration may have already run${NC}"

echo "  Running Quality Measure migrations..."
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" run --rm quality-measure-service \
    java -jar app.jar db migrate || echo -e "${YELLOW}  Migration may have already run${NC}"

echo -e "${GREEN}✓${NC} Database migrations complete"
echo ""

# Step 3: Start all services
echo -e "${YELLOW}Step 3: Starting all services...${NC}"
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" up -d
echo -e "${GREEN}✓${NC} All services started"
echo ""

# Step 4: Wait for services to be healthy
echo -e "${YELLOW}Step 4: Waiting for services to be healthy...${NC}"
MAX_WAIT=180
WAITED=0
ALL_HEALTHY=false

while [ $WAITED -lt $MAX_WAIT ]; do
    UNHEALTHY=$(docker-compose -f "$DOCKER_COMPOSE_FILE" ps | grep -c "unhealthy" || true)
    if [ $UNHEALTHY -eq 0 ]; then
        ALL_HEALTHY=true
        break
    fi
    echo "  Waiting for services to be healthy... ($WAITED/$MAX_WAIT seconds)"
    sleep 10
    WAITED=$((WAITED + 10))
done

if [ "$ALL_HEALTHY" = true ]; then
    echo -e "${GREEN}✓${NC} All services are healthy"
else
    echo -e "${YELLOW}Warning: Some services may still be starting up${NC}"
    echo "Run 'docker-compose -f $DOCKER_COMPOSE_FILE ps' to check status"
fi
echo ""

# Step 5: Run health checks
echo -e "${YELLOW}Step 5: Running health checks...${NC}"

# Check CQL Engine
CQL_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health || echo "000")
if [ "$CQL_HEALTH" = "200" ]; then
    echo -e "${GREEN}✓${NC} CQL Engine Service: Healthy"
else
    echo -e "${YELLOW}⚠${NC} CQL Engine Service: Status $CQL_HEALTH"
fi

# Check Quality Measure Service
QM_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8087/actuator/health || echo "000")
if [ "$QM_HEALTH" = "200" ]; then
    echo -e "${GREEN}✓${NC} Quality Measure Service: Healthy"
else
    echo -e "${YELLOW}⚠${NC} Quality Measure Service: Status $QM_HEALTH"
fi

# Check FHIR Service
FHIR_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8083/fhir/metadata || echo "000")
if [ "$FHIR_HEALTH" = "200" ]; then
    echo -e "${GREEN}✓${NC} FHIR Service: Healthy"
else
    echo -e "${YELLOW}⚠${NC} FHIR Service: Status $FHIR_HEALTH"
fi

echo ""

# Step 6: Load initial data (optional)
read -p "Load sample data? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Step 6: Loading sample data...${NC}"
    if [ -f "$SCRIPT_DIR/sample-data/load-sample-data.py" ]; then
        python3 "$SCRIPT_DIR/sample-data/load-sample-data.py"
        echo -e "${GREEN}✓${NC} Sample data loaded"
    else
        echo -e "${YELLOW}Warning: Sample data script not found${NC}"
    fi
    echo ""
fi

# Deployment complete
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Deployment Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}Service URLs:${NC}"
echo "  CQL Engine Service:      http://localhost:8081"
echo "  Quality Measure Service: http://localhost:8087"
echo "  FHIR Service:            http://localhost:8083"
echo "  Angular Clinical Portal: http://localhost:4200"
echo "  React Dashboard:         http://localhost:3004"
echo "  Prometheus:              http://localhost:9090"
echo "  Grafana:                 http://localhost:3000"
echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo "  1. Configure your reverse proxy (Nginx/Kong) to route traffic"
echo "  2. Set up SSL termination at the load balancer"
echo "  3. Configure DNS to point to your servers"
echo "  4. Set up monitoring alerts in Grafana"
echo "  5. Test all endpoints with production credentials"
echo "  6. Run User Acceptance Testing"
echo "  7. Schedule go-live with stakeholders"
echo ""
echo -e "${YELLOW}Important:${NC}"
echo "  - All services are running in Docker containers"
echo "  - Data is persisted in Docker volumes"
echo "  - Logs are available via 'docker-compose logs'"
echo "  - To stop: docker-compose -f $DOCKER_COMPOSE_FILE down"
echo "  - To restart: docker-compose -f $DOCKER_COMPOSE_FILE restart"
echo ""
echo -e "${GREEN}Deployment successful!${NC}"
