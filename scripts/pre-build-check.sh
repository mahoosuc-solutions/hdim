#!/bin/bash

################################################################################
# pre-build-check.sh
#
# Pre-build validation script
# Ensures sufficient disk space and Docker configuration before building HDIM
#
# Usage: ./scripts/pre-build-check.sh [--required-space GB] [--strict]
# Options:
#   --required-space GB : Required free space (default: 100GB)
#   --strict           : Fail if Docker is not using external drive
#
# This script is called automatically by build pipelines
#
################################################################################

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
REQUIRED_SPACE=100  # GB
STRICT_MODE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --required-space) REQUIRED_SPACE="$2"; shift 2 ;;
        --strict) STRICT_MODE=true; shift ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}Pre-Build System Check${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

STATUS=0

# Check Docker is running
echo -e "${YELLOW}Checking Docker daemon...${NC}"
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}✗ FAIL: Docker daemon is not running${NC}"
    echo "  Start Docker Desktop and try again"
    exit 1
fi
echo -e "${GREEN}✓ Docker daemon is running${NC}"
echo ""

# Check Docker data-root location
echo -e "${YELLOW}Checking Docker configuration...${NC}"
DATA_ROOT=$(docker info --format '{{.DockerRootDir}}')
echo "  Docker data-root: $DATA_ROOT"

if [[ "$DATA_ROOT" == "/mnt/wd-black"* ]]; then
    echo -e "${GREEN}✓ Docker is using external drive${NC}"
elif [[ "$DATA_ROOT" == "/var/lib/docker"* ]]; then
    echo -e "${YELLOW}⚠ Docker is using WSL filesystem (not optimal)${NC}"
    if [ "$STRICT_MODE" = true ]; then
        echo -e "${RED}  FAIL: Strict mode requires external drive${NC}"
        STATUS=1
    fi
else
    echo -e "${YELLOW}⚠ Unexpected Docker data-root: $DATA_ROOT${NC}"
fi
echo ""

# Check available space
echo -e "${YELLOW}Checking disk space...${NC}"

# Check C: drive if on Windows/WSL
if [ -d "/mnt/c" ]; then
    C_AVAILABLE=$(df /mnt/c | tail -1 | awk '{print int($4/1024/1024)}')  # GB
    C_PERCENT=$(df /mnt/c | tail -1 | awk '{print $5}' | sed 's/%//')
    echo "  C: Drive available: ${C_AVAILABLE}GB (${C_PERCENT}% used)"

    if [ "$C_PERCENT" -ge 90 ]; then
        echo -e "${RED}✗ FAIL: C: drive is ${C_PERCENT}% full${NC}"
        echo "  Run: ./scripts/docker-cleanup.sh --aggressive"
        STATUS=1
    elif [ "$C_PERCENT" -ge 85 ]; then
        echo -e "${YELLOW}⚠ WARNING: C: drive is ${C_PERCENT}% full${NC}"
    else
        echo -e "${GREEN}✓ OK: C: drive has space${NC}"
    fi
fi

# Check Docker storage space
DOCKER_AVAILABLE=$(docker system df --format "{{.} }" | grep "Local Volumes" -A 100 | head -1 | awk '{print int($3)}')
if [ -z "$DOCKER_AVAILABLE" ] || [ "$DOCKER_AVAILABLE" -eq 0 ]; then
    DOCKER_AVAILABLE=$(df "$DATA_ROOT" 2>/dev/null | tail -1 | awk '{print int($4/1024/1024)}')  # GB
fi
echo "  Docker storage available: ${DOCKER_AVAILABLE}GB"

if [ "$DOCKER_AVAILABLE" -lt "$REQUIRED_SPACE" ]; then
    echo -e "${RED}✗ FAIL: Insufficient space for build${NC}"
    echo "  Required: ${REQUIRED_SPACE}GB, Available: ${DOCKER_AVAILABLE}GB"
    echo "  Actions:"
    echo "  1. Run: ./scripts/docker-cleanup.sh --aggressive"
    echo "  2. Ensure Docker data-root is on external drive"
    STATUS=1
else
    echo -e "${GREEN}✓ OK: Sufficient space for build${NC}"
fi
echo ""

# Check docker-compose
echo -e "${YELLOW}Checking Docker Compose...${NC}"
if ! docker compose version > /dev/null 2>&1; then
    echo -e "${RED}✗ FAIL: Docker Compose is not available${NC}"
    echo "  Install: docker compose (v2+)"
    STATUS=1
else
    COMPOSE_VERSION=$(docker compose version --short)
    echo -e "${GREEN}✓ Docker Compose $COMPOSE_VERSION available${NC}"
fi
echo ""

# Check if external drive override exists
echo -e "${YELLOW}Checking volume configuration...${NC}"
if [ -f "docker-compose.override.yml" ]; then
    echo -e "${GREEN}✓ Found docker-compose.override.yml${NC}"
    echo "  Volumes configured for external drive"
else
    echo -e "${YELLOW}⚠ No docker-compose.override.yml found${NC}"
    echo "  Run: cp /mnt/wd-black/config/docker-compose.override.yml ."
fi
echo ""

# Check build context
echo -e "${YELLOW}Checking build context...${NC}"
DOCKERFILE_COUNT=$(find . -name "Dockerfile" | wc -l)
echo "  Dockerfiles found: $DOCKERFILE_COUNT"

if [ "$DOCKERFILE_COUNT" -eq 0 ]; then
    echo -e "${YELLOW}⚠ WARNING: No Dockerfiles found${NC}"
fi
echo ""

# Summary
echo -e "${BLUE}================================================${NC}"
if [ $STATUS -eq 0 ]; then
    echo -e "${GREEN}✓ All pre-build checks passed!${NC}"
    echo "  Ready to build HDIM services"
else
    echo -e "${RED}✗ Pre-build checks failed${NC}"
    echo "  Fix issues above before building"
fi
echo -e "${BLUE}================================================${NC}"

exit $STATUS
