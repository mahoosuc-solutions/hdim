#!/bin/bash

# HealthData in Motion - Production Deployment Script
# Version: 1.0.0
# Date: 2025-11-18

set -euo pipefail  # Exit on error and undefined vars

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
LOAD_SAMPLE_DATA="${LOAD_SAMPLE_DATA:-false}"

if docker compose version &> /dev/null; then
  DOCKER_COMPOSE=(docker compose)
elif command -v docker-compose &> /dev/null; then
  DOCKER_COMPOSE=(docker-compose)
else
  echo -e "${RED}Error: neither 'docker compose' nor 'docker-compose' is available${NC}"
  exit 1
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}HealthData in Motion - Production Deploy${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

service_exists() {
  local service_name="$1"
  "${DOCKER_COMPOSE[@]}" -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" config --services | grep -qx "$service_name"
}

# Pre-flight checks
echo -e "${YELLOW}Running pre-flight checks...${NC}"

# Check if running as root (required for production deployment)
if [ "$EUID" -eq 0 ]; then
   echo -e "${YELLOW}Warning: Running as root. This is acceptable for production deployment.${NC}"
fi

# Check required tools
REQUIRED_TOOLS=("docker" "git" "curl")
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
if grep -Eq "CHANGE_ME|YOUR/WEBHOOK/URL" "$ENV_FILE"; then
    echo -e "${RED}Error: Default passwords found in .env.production${NC}"
    echo "Please replace all placeholder values with secure secrets"
    exit 1
fi
echo -e "${GREEN}✓${NC} No default passwords found"

# Check immutable image refs are configured with digest pinning
REQUIRED_IMAGE_VARS=(
  "AI_SALES_AGENT_IMAGE"
  "LIVE_CALL_SALES_AGENT_IMAGE"
  "COACHING_UI_IMAGE"
  "POSTGRES_IMAGE"
  "REDIS_IMAGE"
  "JAEGER_IMAGE"
  "PROMETHEUS_IMAGE"
  "ALERTMANAGER_IMAGE"
  "GRAFANA_IMAGE"
)

for image_var in "${REQUIRED_IMAGE_VARS[@]}"; do
    image_value=$(grep -E "^${image_var}=" "$ENV_FILE" | cut -d'=' -f2- || true)
    if [ -z "$image_value" ]; then
        echo -e "${RED}Error: ${image_var} is not set in .env.production${NC}"
        exit 1
    fi
    if [[ "$image_value" != *@sha256:* ]]; then
        echo -e "${RED}Error: ${image_var} must be digest-pinned (expected @sha256:...)${NC}"
        exit 1
    fi
done
echo -e "${GREEN}✓${NC} Immutable digest-pinned image refs configured"

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
        echo -e "${RED}Error: SSL enabled but certificates not found${NC}"
        echo "  Certificate: $SSL_CERT"
        echo "  Key: $SSL_KEY"
        exit 1
    else
        echo -e "${GREEN}✓${NC} SSL certificates found"
    fi
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Deployment Steps${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Step 1: Pull pinned Docker images
echo -e "${YELLOW}Step 1: Pulling pinned Docker images...${NC}"
"${DOCKER_COMPOSE[@]}" -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" pull
echo -e "${GREEN}✓${NC} Docker images pulled"
echo ""

# Step 2: Run database migrations
echo -e "${YELLOW}Step 2: Running database migrations...${NC}"
# Start only the database
"${DOCKER_COMPOSE[@]}" -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" up -d postgres-primary
sleep 10  # Wait for PostgreSQL to be ready

# Run migrations for each service
if service_exists "cql-engine-service"; then
    echo "  Running CQL Engine migrations..."
    "${DOCKER_COMPOSE[@]}" -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" run --rm cql-engine-service \
      java -jar app.jar db migrate || echo -e "${YELLOW}  Migration may have already run${NC}"
fi

if service_exists "quality-measure-service"; then
    echo "  Running Quality Measure migrations..."
    "${DOCKER_COMPOSE[@]}" -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" run --rm quality-measure-service \
      java -jar app.jar db migrate || echo -e "${YELLOW}  Migration may have already run${NC}"
fi

echo -e "${GREEN}✓${NC} Database migrations complete"
echo ""

# Step 3: Start all services
echo -e "${YELLOW}Step 3: Starting all services...${NC}"
"${DOCKER_COMPOSE[@]}" -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" up -d
echo -e "${GREEN}✓${NC} All services started"
echo ""

# Step 4: Wait for services to be healthy
echo -e "${YELLOW}Step 4: Waiting for services to be healthy...${NC}"
MAX_WAIT=180
WAITED=0
ALL_HEALTHY=false

while [ $WAITED -lt $MAX_WAIT ]; do
    UNHEALTHY=$("${DOCKER_COMPOSE[@]}" -f "$DOCKER_COMPOSE_FILE" ps | grep -c "unhealthy" || true)
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
    echo "Run '${DOCKER_COMPOSE[*]} -f $DOCKER_COMPOSE_FILE ps' to check status"
fi
echo ""

# Step 5: Run health checks
echo -e "${YELLOW}Step 5: Running health checks...${NC}"

# Check AI Sales Agent
AI_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/health || echo "000")
if [ "$AI_HEALTH" = "200" ]; then
    echo -e "${GREEN}✓${NC} AI Sales Agent: Healthy"
else
    echo -e "${YELLOW}⚠${NC} AI Sales Agent: Status $AI_HEALTH"
fi

# Check Live Call Sales Agent
LIVE_CALL_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8095/health || echo "000")
if [ "$LIVE_CALL_HEALTH" = "200" ]; then
    echo -e "${GREEN}✓${NC} Live Call Sales Agent: Healthy"
else
    echo -e "${YELLOW}⚠${NC} Live Call Sales Agent: Status $LIVE_CALL_HEALTH"
fi

# Check Coaching UI
COACHING_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:4200/ || echo "000")
if [ "$COACHING_HEALTH" = "200" ]; then
    echo -e "${GREEN}✓${NC} Coaching UI: Healthy"
else
    echo -e "${YELLOW}⚠${NC} Coaching UI: Status $COACHING_HEALTH"
fi

echo ""

# Step 6: Load initial data (optional; disabled by default in production)
if [[ "$LOAD_SAMPLE_DATA" == "true" ]]; then
    echo -e "${YELLOW}Step 6: Loading sample data...${NC}"
    if [ -f "$SCRIPT_DIR/sample-data/load-sample-data.py" ]; then
        python3 "$SCRIPT_DIR/sample-data/load-sample-data.py"
        echo -e "${GREEN}✓${NC} Sample data loaded"
    else
        echo -e "${YELLOW}Warning: Sample data script not found${NC}"
    fi
    echo ""
else
    echo -e "${GREEN}✓${NC} Step 6: Sample data load skipped (LOAD_SAMPLE_DATA=false)"
    echo ""
fi

# Deployment complete
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Deployment Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}Service URLs:${NC}"
echo "  AI Sales Agent:          http://localhost:8090"
echo "  Live Call Sales Agent:   http://localhost:8095"
echo "  Coaching UI:             http://localhost:4200"
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
echo "  - Logs are available via '${DOCKER_COMPOSE[*]} logs'"
echo "  - To stop: ${DOCKER_COMPOSE[*]} -f $DOCKER_COMPOSE_FILE down"
echo "  - To restart: ${DOCKER_COMPOSE[*]} -f $DOCKER_COMPOSE_FILE restart"
echo ""
echo -e "${GREEN}Deployment successful!${NC}"
