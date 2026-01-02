#!/bin/bash
#
# Build All Services Script
# Builds Docker images for all HealthData-in-Motion services
# Usage: ./scripts/build-all.sh [--no-cache]
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse arguments
NO_CACHE=""
if [ "$1" == "--no-cache" ]; then
    NO_CACHE="--no-cache"
    echo -e "${YELLOW}Building with --no-cache flag${NC}"
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Building All HealthData-in-Motion Services${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Track build results
total=0
success=0
failed=0

# Function to build and track
build_service() {
    local name=$1
    local build_cmd=$2

    total=$((total + 1))
    echo -e "${BLUE}Building ${name}...${NC}"

    if eval "$build_cmd"; then
        echo -e "${GREEN}✓ ${name} built successfully${NC}"
        success=$((success + 1))
    else
        echo -e "${RED}✗ ${name} build failed${NC}"
        failed=$((failed + 1))
        return 1
    fi
    echo ""
}

# 1. Build Frontend (Clinical Portal)
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Frontend Services${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

build_service "Clinical Portal Frontend" \
    "docker build $NO_CACHE -f apps/clinical-portal/Dockerfile -t healthdata/clinical-portal:latest ."

# 2. Build Backend Services
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Backend Services${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Build all backend services with Gradle
echo -e "${BLUE}Building backend services with Gradle...${NC}"
if cd backend && ./gradlew clean build -x test && cd ..; then
    echo -e "${GREEN}✓ Backend services compiled successfully${NC}"
    echo ""
else
    echo -e "${RED}✗ Backend compilation failed${NC}"
    exit 1
fi

# Build Gateway Service
build_service "Gateway Service" \
    "docker build $NO_CACHE -f backend/modules/services/gateway-service/Dockerfile -t healthdata/gateway-service:latest backend/modules/services/gateway-service"

# Build CQL Engine Service
build_service "CQL Engine Service" \
    "docker build $NO_CACHE -f backend/modules/services/cql-engine-service/Dockerfile -t healthdata/cql-engine-service:latest backend/modules/services/cql-engine-service"

# Build Quality Measure Service
build_service "Quality Measure Service" \
    "docker build $NO_CACHE -f backend/modules/services/quality-measure-service/Dockerfile -t healthdata/quality-measure-service:latest backend/modules/services/quality-measure-service"

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Build Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "Total:   $total"
echo -e "${GREEN}Success: $success${NC}"
if [ $failed -gt 0 ]; then
    echo -e "${RED}Failed:  $failed${NC}"
fi
echo ""

if [ $failed -eq 0 ]; then
    echo -e "${GREEN}✅ All services built successfully!${NC}"
    echo ""
    echo -e "${BLUE}Next steps:${NC}"
    echo "  1. Deploy stack:  ./scripts/deploy.sh"
    echo "  2. Check health:  ./scripts/health-check.sh"
    echo ""
    exit 0
else
    echo -e "${RED}❌ Some builds failed${NC}"
    echo "Please check the error messages above"
    exit 1
fi
