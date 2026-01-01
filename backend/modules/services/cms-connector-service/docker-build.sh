#!/bin/bash

# CMS Connector Service - Docker Build Script
# Builds optimized Docker image for the application

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
REGISTRY="${DOCKER_REGISTRY:-docker.io}"
NAMESPACE="${DOCKER_NAMESPACE:-webemo}"
IMAGE_NAME="${APP_NAME:-cms-connector-service}"
IMAGE_VERSION="${APP_VERSION:-1.0.0}"
IMAGE_TAG="${REGISTRY}/${NAMESPACE}/${IMAGE_NAME}:${IMAGE_VERSION}"
LATEST_TAG="${REGISTRY}/${NAMESPACE}/${IMAGE_NAME}:latest"

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}CMS Connector Service - Docker Build${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# Check Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker is available${NC}"
echo ""

# Check if in correct directory
if [ ! -f "Dockerfile" ]; then
    echo -e "${RED}Error: Dockerfile not found in current directory${NC}"
    echo "Please run this script from the cms-connector-service directory"
    exit 1
fi

echo -e "${GREEN}✓ Dockerfile found${NC}"
echo ""

# Build Maven application (if needed)
if [ -f "pom.xml" ] && [ ! -f "target/cms-connector-service-*.jar" ]; then
    echo -e "${YELLOW}Building Maven application...${NC}"
    ./mvn/bin/mvn clean package -DskipTests -q
    echo -e "${GREEN}✓ Maven build completed${NC}"
    echo ""
fi

# Build Docker image
echo -e "${YELLOW}Building Docker image: ${IMAGE_TAG}${NC}"
docker build \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg VCS_REF=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown") \
    --tag "${IMAGE_TAG}" \
    --tag "${LATEST_TAG}" \
    --file Dockerfile \
    .

echo ""
echo -e "${GREEN}✓ Docker image built successfully${NC}"
echo ""

# Show image info
echo -e "${YELLOW}Image Information:${NC}"
docker images | grep "${IMAGE_NAME}" | head -2
echo ""

# Optional: Push to registry
if [ "${1}" == "push" ]; then
    echo -e "${YELLOW}Pushing image to registry...${NC}"
    docker push "${IMAGE_TAG}"
    docker push "${LATEST_TAG}"
    echo -e "${GREEN}✓ Image pushed successfully${NC}"
    echo ""
fi

# Optional: Run tests
if [ "${1}" == "test" ] || [ "${2}" == "test" ]; then
    echo -e "${YELLOW}Running container health check...${NC}"

    # Start container temporarily
    CONTAINER_ID=$(docker run -d \
        -e SPRING_PROFILES_ACTIVE=test \
        -e SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb \
        "${IMAGE_TAG}")

    # Wait for health check
    sleep 10

    # Check health
    if docker exec "${CONTAINER_ID}" curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Container health check passed${NC}"
    else
        echo -e "${RED}✗ Container health check failed${NC}"
    fi

    # Cleanup
    docker stop "${CONTAINER_ID}" > /dev/null
    docker rm "${CONTAINER_ID}" > /dev/null
    echo ""
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Build completed successfully!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Image: ${IMAGE_TAG}"
echo ""
echo "Next steps:"
echo "  1. Run locally: docker-compose -f docker-compose.dev.yml up"
echo "  2. Deploy to prod: docker-compose -f docker-compose.prod.yml up"
echo "  3. Push to registry: ./docker-build.sh push"
echo ""
