#!/bin/bash
#
# HDIM Local Docker Deployment Test Script
# Tests Docker images built for local Docker Desktop deployment
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔══════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   HDIM Local Deployment Test           ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════╝${NC}"
echo ""

# Test 1: Verify Docker is running
echo -e "${BLUE}📋 Test 1: Checking Docker status...${NC}"
if docker info >/dev/null 2>&1; then
    echo -e "${GREEN}✅ Docker is running${NC}"
else
    echo -e "${RED}❌ Docker is not running. Please start Docker Desktop.${NC}"
    exit 1
fi

# Test 2: List built images
echo ""
echo -e "${BLUE}📋 Test 2: Listing HDIM images...${NC}"
IMAGE_COUNT=$(docker images | grep hdim-master | wc -l)
echo -e "${GREEN}✅ Found $IMAGE_COUNT HDIM images${NC}"

if [ $IMAGE_COUNT -eq 0 ]; then
    echo -e "${YELLOW}⚠️  No images found. Run 'docker compose build' first.${NC}"
    exit 1
fi

docker images | grep hdim-master | head -10

# Test 3: Check image sizes
echo ""
echo -e "${BLUE}📋 Test 3: Checking image sizes...${NC}"
echo -e "${YELLOW}Service Image Sizes:${NC}"
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | \
    grep hdim-master | \
    sed 's/hdim-master-//' | \
    head -15

# Test 4: Start infrastructure
echo ""
echo -e "${BLUE}📋 Test 4: Starting infrastructure services...${NC}"
docker compose up -d postgres redis 2>&1 | grep -v "is up-to-date" || true

echo -e "${YELLOW}Waiting for infrastructure to be ready...${NC}"
sleep 10

# Check PostgreSQL
if docker compose ps postgres | grep -q "Up"; then
    echo -e "${GREEN}✅ PostgreSQL is running${NC}"
else
    echo -e "${RED}❌ PostgreSQL failed to start${NC}"
    docker compose logs postgres | tail -20
fi

# Check Redis
if docker compose ps redis | grep -q "Up"; then
    echo -e "${GREEN}✅ Redis is running${NC}"
else
    echo -e "${RED}❌ Redis failed to start${NC}"
    docker compose logs redis | tail -20
fi

# Test 5: Start core services
echo ""
echo -e "${BLUE}📋 Test 5: Starting core services (gateway, fhir, patient)...${NC}"
docker compose up -d gateway-service fhir-service patient-service 2>&1 | \
    grep -v "is up-to-date" || true

echo -e "${YELLOW}Waiting 30 seconds for services to start...${NC}"
sleep 30

# Test 6: Health checks
echo ""
echo -e "${BLUE}📋 Test 6: Running health checks...${NC}"

SERVICES=(
    "8080:gateway-service"
    "8085:fhir-service"
    "8084:patient-service"
)

PASSED=0
FAILED=0

for service in "${SERVICES[@]}"; do
    port="${service%%:*}"
    name="${service##*:}"

    # Try health check
    status=$(curl -s --max-time 5 "http://localhost:$port/actuator/health" 2>/dev/null | \
             jq -r '.status' 2>/dev/null || echo "UNREACHABLE")

    if [ "$status" = "UP" ]; then
        echo -e "${GREEN}✅ $name (port $port): $status${NC}"
        ((PASSED++))
    else
        echo -e "${RED}❌ $name (port $port): $status${NC}"
        ((FAILED++))

        # Show last 10 log lines for failed service
        echo -e "${YELLOW}   Last 10 log lines:${NC}"
        docker compose logs --tail=10 "$name" | sed 's/^/   /'
    fi
done

# Test 7: Container resource usage
echo ""
echo -e "${BLUE}📋 Test 7: Checking container resources...${NC}"
echo -e "${YELLOW}Top 5 containers by memory usage:${NC}"
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" | \
    grep hdim-master | \
    head -6

# Test 8: Network connectivity
echo ""
echo -e "${BLUE}📋 Test 8: Testing inter-service connectivity...${NC}"

# Test if gateway can reach other services
if docker compose exec gateway-service curl -s --max-time 5 http://fhir-service:8085/actuator/health >/dev/null 2>&1; then
    echo -e "${GREEN}✅ Gateway → FHIR connectivity working${NC}"
else
    echo -e "${RED}❌ Gateway → FHIR connectivity failed${NC}"
fi

if docker compose exec gateway-service curl -s --max-time 5 http://patient-service:8084/actuator/health >/dev/null 2>&1; then
    echo -e "${GREEN}✅ Gateway → Patient connectivity working${NC}"
else
    echo -e "${RED}❌ Gateway → Patient connectivity failed${NC}"
fi

# Test 9: Database connectivity
echo ""
echo -e "${BLUE}📋 Test 9: Testing database connectivity...${NC}"

# Check if services can connect to PostgreSQL
DB_TABLES=$(docker compose exec postgres psql -U healthdata -d healthdata_db -t -c "\dt" 2>/dev/null | wc -l)
if [ $DB_TABLES -gt 0 ]; then
    echo -e "${GREEN}✅ Database has $DB_TABLES tables${NC}"
else
    echo -e "${YELLOW}⚠️  Database appears empty (migrations may not have run)${NC}"
fi

# Test 10: Quick API test
echo ""
echo -e "${BLUE}📋 Test 10: Testing FHIR API endpoint...${NC}"

# Try to get FHIR metadata
METADATA=$(curl -s --max-time 5 http://localhost:8085/fhir/metadata 2>/dev/null)
if echo "$METADATA" | grep -q "CapabilityStatement"; then
    echo -e "${GREEN}✅ FHIR metadata endpoint working${NC}"
else
    echo -e "${YELLOW}⚠️  FHIR metadata endpoint not responding correctly${NC}"
fi

# Summary
echo ""
echo -e "${BLUE}╔══════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   Test Summary                          ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}✅ Passed: $PASSED${NC}"
echo -e "${RED}❌ Failed: $FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}╔══════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║   All Tests Passed!                     ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BLUE}Next Steps:${NC}"
    echo -e "  1. View Grafana dashboards: ${YELLOW}http://localhost:3000${NC}"
    echo -e "  2. Test FHIR API: ${YELLOW}curl http://localhost:8085/fhir/Patient${NC}"
    echo -e "  3. Check logs: ${YELLOW}docker compose logs -f gateway-service${NC}"
    echo -e "  4. Start all services: ${YELLOW}docker compose --profile full up -d${NC}"
    echo ""
    echo -e "${GREEN}🚀 HDIM platform is ready for testing!${NC}"
    exit 0
else
    echo -e "${RED}╔══════════════════════════════════════════╗${NC}"
    echo -e "${RED}║   Some Tests Failed                     ║${NC}"
    echo -e "${RED}╚══════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${YELLOW}Troubleshooting:${NC}"
    echo -e "  1. Check logs: ${YELLOW}docker compose logs${NC}"
    echo -e "  2. Restart services: ${YELLOW}docker compose restart${NC}"
    echo -e "  3. View specific service: ${YELLOW}docker compose logs gateway-service${NC}"
    echo -e "  4. Check database: ${YELLOW}docker compose exec postgres psql -U healthdata${NC}"
    exit 1
fi
