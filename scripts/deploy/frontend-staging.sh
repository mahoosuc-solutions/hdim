#!/bin/bash
# Frontend Staging Deployment Script
# Deploys Clinical Portal to staging environment

set -e  # Exit on error
set -o pipefail  # Exit on pipe failure

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}🚀 Starting Clinical Portal staging deployment...${NC}"

# ============================================================================
# Configuration
# ============================================================================
APP_NAME="clinical-portal"
ENVIRONMENT="staging"
IMAGE_TAG="${GITHUB_SHA:-latest}"
REGISTRY="${DOCKER_REGISTRY:-registry.hdim.example.com}"
IMAGE_NAME="${REGISTRY}/${APP_NAME}:${IMAGE_TAG}"
DEPLOYMENT_NAME="${APP_NAME}-staging"
NAMESPACE="hdim-staging"

echo "Configuration:"
echo "  App: $APP_NAME"
echo "  Environment: $ENVIRONMENT"
echo "  Image: $IMAGE_NAME"
echo "  Tag: $IMAGE_TAG"

# ============================================================================
# Step 1: Build Docker Image
# ============================================================================
echo -e "\n${YELLOW}Step 1: Building Docker image...${NC}"

if [ ! -f "apps/clinical-portal/Dockerfile" ]; then
    echo -e "${RED}❌ Dockerfile not found at apps/clinical-portal/Dockerfile${NC}"
    exit 1
fi

docker build \
    -t "$IMAGE_NAME" \
    -f apps/clinical-portal/Dockerfile \
    --build-arg BUILD_DATE="$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
    --build-arg VERSION="$IMAGE_TAG" \
    .

echo -e "${GREEN}✅ Docker image built successfully${NC}"

# ============================================================================
# Step 2: Push to Container Registry
# ============================================================================
echo -e "\n${YELLOW}Step 2: Pushing image to registry...${NC}"

# Login to registry (assumes credentials are set via environment)
if [ -n "$DOCKER_USERNAME" ] && [ -n "$DOCKER_PASSWORD" ]; then
    echo "$DOCKER_PASSWORD" | docker login "$REGISTRY" -u "$DOCKER_USERNAME" --password-stdin
else
    echo -e "${YELLOW}⚠️  Docker credentials not set - skipping registry push${NC}"
    echo "Set DOCKER_USERNAME and DOCKER_PASSWORD environment variables"
fi

docker push "$IMAGE_NAME"
echo -e "${GREEN}✅ Image pushed to registry${NC}"

# ============================================================================
# Step 3: Deploy to Kubernetes/Docker Swarm
# ============================================================================
echo -e "\n${YELLOW}Step 3: Deploying to staging environment...${NC}"

# Check if kubectl is available
if command -v kubectl &> /dev/null; then
    echo "Using Kubernetes deployment..."

    # Create namespace if it doesn't exist
    kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

    # Update deployment image
    kubectl set image "deployment/${DEPLOYMENT_NAME}" \
        "${APP_NAME}=${IMAGE_NAME}" \
        -n "$NAMESPACE" \
        --record

    # Wait for rollout to complete
    kubectl rollout status "deployment/${DEPLOYMENT_NAME}" \
        -n "$NAMESPACE" \
        --timeout=5m

    echo -e "${GREEN}✅ Kubernetes deployment successful${NC}"

elif command -v docker &> /dev/null; then
    echo "Using Docker Compose deployment..."

    # Deploy using Docker Compose
    COMPOSE_FILE="docker-compose.staging.yml"

    if [ -f "$COMPOSE_FILE" ]; then
        docker compose -f "$COMPOSE_FILE" up -d clinical-portal
        echo -e "${GREEN}✅ Docker Compose deployment successful${NC}"
    else
        echo -e "${YELLOW}⚠️  $COMPOSE_FILE not found - skipping Docker deployment${NC}"
    fi
else
    echo -e "${RED}❌ Neither kubectl nor docker found - cannot deploy${NC}"
    exit 1
fi

# ============================================================================
# Step 4: Health Check
# ============================================================================
echo -e "\n${YELLOW}Step 4: Performing health checks...${NC}"

STAGING_URL="${STAGING_URL:-https://staging.hdim.example.com}"
MAX_ATTEMPTS=30
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${STAGING_URL}/health" || echo "000")

    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✅ Health check passed (HTTP $HTTP_CODE)${NC}"
        break
    else
        ATTEMPT=$((ATTEMPT + 1))
        echo "Health check attempt $ATTEMPT/$MAX_ATTEMPTS (HTTP $HTTP_CODE) - retrying in 10s..."
        sleep 10
    fi
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo -e "${RED}❌ Health check failed after $MAX_ATTEMPTS attempts${NC}"
    echo "Rolling back deployment..."
    kubectl rollout undo "deployment/${DEPLOYMENT_NAME}" -n "$NAMESPACE" || true
    exit 1
fi

# ============================================================================
# Step 5: Smoke Tests
# ============================================================================
echo -e "\n${YELLOW}Step 5: Running smoke tests...${NC}"

# Test critical endpoints
ENDPOINTS=(
    "/"
    "/assets/config.json"
)

for ENDPOINT in "${ENDPOINTS[@]}"; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${STAGING_URL}${ENDPOINT}" || echo "000")
    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✅ $ENDPOINT returned HTTP $HTTP_CODE${NC}"
    else
        echo -e "${RED}❌ $ENDPOINT returned HTTP $HTTP_CODE${NC}"
        exit 1
    fi
done

# ============================================================================
# Success
# ============================================================================
echo -e "\n${GREEN}🎉 Deployment to staging successful!${NC}"
echo "URL: $STAGING_URL"
echo "Version: $IMAGE_TAG"
echo "Image: $IMAGE_NAME"
