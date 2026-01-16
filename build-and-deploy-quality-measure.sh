#!/bin/bash

# Build and Deploy Quality Measure Service with Patient Health Overview
# This script builds the new code and deploys it to Docker

set -e  # Exit on error

echo "=========================================="
echo "Building Quality Measure Service"
echo "=========================================="

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Step 1: Build the JAR
echo -e "${YELLOW}Step 1: Building JAR with Gradle...${NC}"
cd backend
./gradlew :modules:services:quality-measure-service:build -x test

if [ -f "modules/services/quality-measure-service/build/libs/quality-measure-service.jar" ]; then
    echo -e "${GREEN}✓ JAR built successfully${NC}"
    ls -lh modules/services/quality-measure-service/build/libs/quality-measure-service.jar
else
    echo "Error: JAR file not found"
    exit 1
fi

# Step 2: Build Docker image
echo ""
echo -e "${YELLOW}Step 2: Building Docker image...${NC}"
cd modules/services/quality-measure-service
docker build -t healthdata/quality-measure-service:1.0.12 .
docker tag healthdata/quality-measure-service:1.0.12 healthdata/quality-measure-service:latest

echo -e "${GREEN}✓ Docker image built successfully${NC}"
docker images | grep quality-measure-service

# Step 3: Update docker-compose.yml
cd ../../..
echo ""
echo -e "${YELLOW}Step 3: Update docker-compose.yml to use new image...${NC}"
echo "Please update docker-compose.yml:"
echo "  Change: image: healthdata/quality-measure-service:1.0.11"
echo "  To:     image: healthdata/quality-measure-service:1.0.12"
echo ""
echo "Or run: sed -i 's/quality-measure-service:1.0.11/quality-measure-service:1.0.12/' docker-compose.yml"

# Step 4: Restart service
echo ""
echo -e "${YELLOW}Step 4: Restarting service with new image...${NC}"
docker compose stop quality-measure-service
docker compose up -d quality-measure-service

# Step 5: Wait for health check
echo ""
echo -e "${YELLOW}Step 5: Waiting for service to become healthy...${NC}"
for i in {1..30}; do
    if docker compose ps | grep quality-measure | grep -q "healthy"; then
        echo -e "${GREEN}✓ Service is healthy!${NC}"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

# Step 6: Check logs
echo ""
echo -e "${YELLOW}Step 6: Checking startup logs...${NC}"
docker logs healthdata-quality-measure --tail 50 | grep -i "started\|error\|0005\|0006\|0007" || true

# Step 7: Verify database migrations
echo ""
echo -e "${YELLOW}Step 7: Verifying database tables...${NC}"
docker exec healthdata-postgres psql -U healthdata -d healthdata_cql -c "\dt" | grep -E "mental_health|care_gap|risk_assess" || echo "Tables may need to be checked manually"

echo ""
echo "=========================================="
echo "Deployment Complete!"
echo "=========================================="
echo ""
echo "Next Steps:"
echo "1. Run API tests: ./test-patient-health-api.sh"
echo "2. Check service logs: docker logs healthdata-quality-measure"
echo "3. Verify endpoints: curl http://localhost:8087/actuator/health"
echo ""
echo "Documentation:"
echo "- DEPLOYMENT_STATUS_FINAL.md"
echo "- BACKEND_IMPLEMENTATION_COMPLETE.md"
echo "=========================================="
