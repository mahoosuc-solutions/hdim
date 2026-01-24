#!/bin/bash
# Frontend Production Rollback Script
# Rolls back to previous stable version

set -e  # Exit on error

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}🔄 Initiating production rollback...${NC}"

# ============================================================================
# Configuration
# ============================================================================
APP_NAME="clinical-portal"
NAMESPACE="hdim-production"
PRODUCTION_URL="${PRODUCTION_URL:-https://app.hdim.example.com}"

# Determine which color to roll back to
if [ -n "$1" ]; then
    ROLLBACK_TO="$1"
else
    # Auto-detect: find which deployment has more replicas (likely the previous stable)
    if command -v kubectl &> /dev/null; then
        BLUE_REPLICAS=$(kubectl get deployment "${APP_NAME}-blue" -n "$NAMESPACE" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "0")
        GREEN_REPLICAS=$(kubectl get deployment "${APP_NAME}-green" -n "$NAMESPACE" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "0")

        if [ "$BLUE_REPLICAS" -gt "$GREEN_REPLICAS" ]; then
            ROLLBACK_TO="blue"
        else
            ROLLBACK_TO="green"
        fi
    else
        echo -e "${RED}❌ kubectl not found and no rollback target specified${NC}"
        echo "Usage: $0 <blue|green>"
        exit 1
    fi
fi

echo "Rollback target: $ROLLBACK_TO"

# ============================================================================
# Step 1: Switch Traffic Back
# ============================================================================
echo -e "\n${YELLOW}Step 1: Switching traffic to ${ROLLBACK_TO}...${NC}"

if command -v kubectl &> /dev/null; then
    # Scale up the rollback target if needed
    kubectl scale deployment "${APP_NAME}-${ROLLBACK_TO}" \
        --replicas=3 \
        -n "$NAMESPACE"

    # Wait for pods to be ready
    kubectl rollout status "deployment/${APP_NAME}-${ROLLBACK_TO}" \
        -n "$NAMESPACE" \
        --timeout=3m

    # Update service to point to rollback target
    kubectl patch service "${APP_NAME}" \
        -n "$NAMESPACE" \
        -p "{\"spec\":{\"selector\":{\"color\":\"${ROLLBACK_TO}\"}}}"

    echo -e "${GREEN}✅ Traffic switched to ${ROLLBACK_TO}${NC}"
else
    echo -e "${YELLOW}⚠️  Manual traffic switch required to ${ROLLBACK_TO}${NC}"
fi

# ============================================================================
# Step 2: Health Check
# ============================================================================
echo -e "\n${YELLOW}Step 2: Verifying health after rollback...${NC}"

MAX_ATTEMPTS=20
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${PRODUCTION_URL}/health" || echo "000")

    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✅ Health check passed after rollback${NC}"
        break
    else
        ATTEMPT=$((ATTEMPT + 1))
        echo "Health check attempt $ATTEMPT/$MAX_ATTEMPTS (HTTP $HTTP_CODE)..."
        sleep 5
    fi
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo -e "${RED}❌ Health check failed after rollback - manual intervention required${NC}"
    exit 1
fi

# ============================================================================
# Step 3: Monitor for 2 Minutes
# ============================================================================
echo -e "\n${YELLOW}Step 3: Monitoring rollback stability...${NC}"

for ((i=1; i<=4; i++)); do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${PRODUCTION_URL}/health" || echo "000")
    if [ "$HTTP_CODE" != "200" ]; then
        echo -e "${RED}❌ Rollback unstable (HTTP $HTTP_CODE)${NC}"
        exit 1
    fi
    echo "Stability check $i/4: HTTP $HTTP_CODE"
    sleep 30
done

# ============================================================================
# Success
# ============================================================================
echo -e "\n${GREEN}✅ Rollback completed successfully${NC}"
echo "Production is now running: $ROLLBACK_TO"
echo "URL: $PRODUCTION_URL"
echo ""
echo "Post-rollback actions:"
echo "  1. Investigate what caused the deployment failure"
echo "  2. Review logs: kubectl logs -l app=${APP_NAME},color=${ROLLBACK_TO} -n $NAMESPACE"
echo "  3. Check Grafana dashboards for anomalies"
echo "  4. Fix the issue before attempting redeployment"
