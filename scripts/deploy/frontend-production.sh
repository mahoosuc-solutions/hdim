#!/bin/bash
# Frontend Production Deployment Script
# Deploys Clinical Portal to production using blue-green strategy

set -e  # Exit on error
set -o pipefail  # Exit on pipe failure

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}🚀 Starting Clinical Portal production deployment (blue-green)...${NC}"

# ============================================================================
# Configuration
# ============================================================================
APP_NAME="clinical-portal"
ENVIRONMENT="production"
IMAGE_TAG="${GITHUB_SHA:-latest}"
REGISTRY="${DOCKER_REGISTRY:-registry.hdim.example.com}"
IMAGE_NAME="${REGISTRY}/${APP_NAME}:${IMAGE_TAG}"
NAMESPACE="hdim-production"
PRODUCTION_URL="${PRODUCTION_URL:-https://app.hdim.example.com}"

# Blue-green configuration
CURRENT_COLOR="${CURRENT_COLOR:-blue}"  # Current live environment
NEW_COLOR=$([ "$CURRENT_COLOR" = "blue" ] && echo "green" || echo "blue")

echo "Configuration:"
echo "  App: $APP_NAME"
echo "  Environment: $ENVIRONMENT"
echo "  Image: $IMAGE_NAME"
echo "  Current (live): $CURRENT_COLOR"
echo "  New (deploying): $NEW_COLOR"

# ============================================================================
# Step 1: Build and Push Docker Image
# ============================================================================
echo -e "\n${YELLOW}Step 1: Building and pushing Docker image...${NC}"

if [ ! -f "apps/clinical-portal/Dockerfile" ]; then
    echo -e "${RED}❌ Dockerfile not found${NC}"
    exit 1
fi

docker build \
    -t "$IMAGE_NAME" \
    -f apps/clinical-portal/Dockerfile \
    --build-arg BUILD_DATE="$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
    --build-arg VERSION="$IMAGE_TAG" \
    .

if [ -n "$DOCKER_USERNAME" ] && [ -n "$DOCKER_PASSWORD" ]; then
    echo "$DOCKER_PASSWORD" | docker login "$REGISTRY" -u "$DOCKER_USERNAME" --password-stdin
    docker push "$IMAGE_NAME"
    echo -e "${GREEN}✅ Image pushed to registry${NC}"
else
    echo -e "${YELLOW}⚠️  Docker credentials not set${NC}"
fi

# ============================================================================
# Step 2: Deploy to GREEN Environment (inactive)
# ============================================================================
echo -e "\n${YELLOW}Step 2: Deploying to ${NEW_COLOR} environment...${NC}"

DEPLOYMENT_NAME="${APP_NAME}-${NEW_COLOR}"

if command -v kubectl &> /dev/null; then
    # Create namespace if it doesn't exist
    kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

    # Update green deployment
    kubectl set image "deployment/${DEPLOYMENT_NAME}" \
        "${APP_NAME}=${IMAGE_NAME}" \
        -n "$NAMESPACE" \
        --record

    # Wait for rollout
    kubectl rollout status "deployment/${DEPLOYMENT_NAME}" \
        -n "$NAMESPACE" \
        --timeout=5m

    echo -e "${GREEN}✅ ${NEW_COLOR} deployment successful${NC}"
else
    echo -e "${YELLOW}⚠️  kubectl not found - using Docker Compose${NC}"

    COMPOSE_FILE="docker-compose.production.yml"
    if [ -f "$COMPOSE_FILE" ]; then
        docker compose -f "$COMPOSE_FILE" up -d "clinical-portal-${NEW_COLOR}"
    fi
fi

# ============================================================================
# Step 3: Health Check on GREEN
# ============================================================================
echo -e "\n${YELLOW}Step 3: Health check on ${NEW_COLOR} environment...${NC}"

GREEN_URL="${PRODUCTION_URL}-${NEW_COLOR}"  # e.g., https://app-green.hdim.example.com
MAX_ATTEMPTS=30
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${GREEN_URL}/health" || echo "000")

    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✅ ${NEW_COLOR} health check passed${NC}"
        break
    else
        ATTEMPT=$((ATTEMPT + 1))
        echo "${NEW_COLOR} health check attempt $ATTEMPT/$MAX_ATTEMPTS (HTTP $HTTP_CODE)..."
        sleep 10
    fi
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo -e "${RED}❌ ${NEW_COLOR} health check failed - aborting deployment${NC}"
    exit 1
fi

# ============================================================================
# Step 4: Smoke Tests on GREEN
# ============================================================================
echo -e "\n${YELLOW}Step 4: Running smoke tests on ${NEW_COLOR}...${NC}"

ENDPOINTS=(
    "/"
    "/assets/config.json"
)

for ENDPOINT in "${ENDPOINTS[@]}"; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${GREEN_URL}${ENDPOINT}" || echo "000")
    if [ "$HTTP_CODE" != "200" ]; then
        echo -e "${RED}❌ Smoke test failed: $ENDPOINT returned HTTP $HTTP_CODE${NC}"
        exit 1
    fi
done

echo -e "${GREEN}✅ All smoke tests passed${NC}"

# ============================================================================
# Step 5: Switch Traffic to GREEN
# ============================================================================
echo -e "\n${YELLOW}Step 5: Switching traffic from ${CURRENT_COLOR} to ${NEW_COLOR}...${NC}"

if command -v kubectl &> /dev/null; then
    # Update service selector to point to green pods
    kubectl patch service "${APP_NAME}" \
        -n "$NAMESPACE" \
        -p "{\"spec\":{\"selector\":{\"color\":\"${NEW_COLOR}\"}}}"

    echo -e "${GREEN}✅ Traffic switched to ${NEW_COLOR}${NC}"
else
    echo -e "${YELLOW}⚠️  Manual traffic switch required${NC}"
    echo "Update your load balancer to point to ${NEW_COLOR} deployment"
fi

# ============================================================================
# Step 6: Monitor Production for 5 Minutes
# ============================================================================
echo -e "\n${YELLOW}Step 6: Monitoring production metrics...${NC}"

MONITOR_DURATION=300  # 5 minutes
MONITOR_INTERVAL=30   # Check every 30 seconds
CHECKS=$((MONITOR_DURATION / MONITOR_INTERVAL))

for ((i=1; i<=CHECKS; i++)); do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${PRODUCTION_URL}/health" || echo "000")
    RESPONSE_TIME=$(curl -s -o /dev/null -w "%{time_total}" "$PRODUCTION_URL" || echo "0")

    if [ "$HTTP_CODE" != "200" ]; then
        echo -e "${RED}❌ Production health check failed (HTTP $HTTP_CODE) - initiating rollback${NC}"
        bash "$(dirname "$0")/frontend-rollback.sh" "$CURRENT_COLOR"
        exit 1
    fi

    # Check response time (should be < 2 seconds)
    if (( $(echo "$RESPONSE_TIME > 2" | bc -l) )); then
        echo -e "${YELLOW}⚠️  Slow response time: ${RESPONSE_TIME}s${NC}"
    fi

    echo "Monitor check $i/$CHECKS: HTTP $HTTP_CODE, Response time: ${RESPONSE_TIME}s"
    sleep $MONITOR_INTERVAL
done

echo -e "${GREEN}✅ Production monitoring complete - deployment stable${NC}"

# ============================================================================
# Step 7: Scale Down BLUE (old version)
# ============================================================================
echo -e "\n${YELLOW}Step 7: Scaling down ${CURRENT_COLOR} deployment...${NC}"

if command -v kubectl &> /dev/null; then
    # Keep blue running but reduce replicas (for quick rollback if needed)
    kubectl scale deployment "${APP_NAME}-${CURRENT_COLOR}" \
        --replicas=1 \
        -n "$NAMESPACE"

    echo -e "${GREEN}✅ ${CURRENT_COLOR} scaled down to 1 replica (kept for rollback)${NC}"
fi

# ============================================================================
# Success
# ============================================================================
echo -e "\n${GREEN}🎉 Production deployment successful!${NC}"
echo "URL: $PRODUCTION_URL"
echo "Version: $IMAGE_TAG"
echo "Active Color: $NEW_COLOR"
echo ""
echo "Next steps:"
echo "  - Monitor metrics in Grafana: http://localhost:3001"
echo "  - Check Prometheus alerts: http://localhost:9090"
echo "  - After 24 hours of stability, you can remove ${CURRENT_COLOR}:"
echo "    kubectl delete deployment ${APP_NAME}-${CURRENT_COLOR} -n $NAMESPACE"
