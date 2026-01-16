#!/bin/bash

# Quick Compliance Validation Script
# Runs test harness and validates compliance tracking

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:4200}"
COMPLIANCE_URL="${BACKEND_URL}/api/v1/compliance/errors"

echo -e "${BLUE}╔════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║${NC}  Compliance Validation with Test Harness    ${BLUE}║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════╝${NC}"
echo ""

# Step 1: Check services
echo -e "${YELLOW}Step 1: Checking services...${NC}"
if ! curl -s -f "${BACKEND_URL}/actuator/health" > /dev/null 2>&1; then
    echo -e "${RED}✗ Backend not running. Start with: ./gradlew :modules:services:gateway-clinical-service:bootRun${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Backend is running${NC}"

# Step 2: Enable compliance mode (temporary)
echo ""
echo -e "${YELLOW}Step 2: Enabling compliance mode...${NC}"
ENV_FILE="apps/clinical-portal/src/environments/environment.ts"
if [ -f "$ENV_FILE" ]; then
    # Create backup
    cp "$ENV_FILE" "${ENV_FILE}.backup.$(date +%s)"
    
    # Enable compliance
    sed -i 's/disableFallbacks: false/disableFallbacks: true/g' "$ENV_FILE"
    sed -i 's/strictErrorHandling: false/strictErrorHandling: true/g' "$ENV_FILE"
    
    echo -e "${GREEN}✓ Compliance mode enabled (restart frontend to apply)${NC}"
    echo -e "${YELLOW}  Backup saved. Restore with: cp ${ENV_FILE}.backup.* ${ENV_FILE}${NC}"
else
    echo -e "${YELLOW}⚠ Environment file not found, skipping${NC}"
fi

# Step 3: Test compliance endpoint
echo ""
echo -e "${YELLOW}Step 3: Testing compliance endpoint...${NC}"
TEST_ERROR='{
  "errors": [{
    "id": "err-'$(date +%s)'000-validation",
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.000Z)'",
    "context": {
      "service": "Validation Test",
      "operation": "GET /test",
      "errorCode": "ERR-9001",
      "severity": "ERROR",
      "tenantId": "test-tenant"
    },
    "message": "Validation test error"
  }],
  "syncedAt": "'$(date -u +%Y-%m-%dT%H:%M:%S.000Z)'"
}'

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${COMPLIANCE_URL}" \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: test-tenant" \
    -d "${TEST_ERROR}")

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Compliance endpoint working${NC}"
else
    echo -e "${RED}✗ Compliance endpoint failed (HTTP $HTTP_CODE)${NC}"
    echo "Response: $(echo "$RESPONSE" | head -n -1)"
fi

# Step 4: Load test data
echo ""
echo -e "${YELLOW}Step 4: Loading test data...${NC}"
if [ -f "test-harness/load-fhir-data.sh" ]; then
    cd test-harness
    echo "Running test harness data loader..."
    if bash load-fhir-data.sh 2>&1 | head -20; then
        echo -e "${GREEN}✓ Test data loaded${NC}"
    else
        echo -e "${YELLOW}⚠ Test data load had issues (check logs)${NC}"
    fi
    cd ..
else
    echo -e "${YELLOW}⚠ Test harness not found, skipping${NC}"
fi

# Step 5: Get error stats
echo ""
echo -e "${YELLOW}Step 5: Error statistics...${NC}"
sleep 3
STATS=$(curl -s "${COMPLIANCE_URL}/stats?tenantId=test-tenant" 2>/dev/null || echo '{}')
if [ "$STATS" != "{}" ]; then
    echo "Error Stats:"
    echo "$STATS" | python3 -m json.tool 2>/dev/null || echo "$STATS"
else
    echo -e "${YELLOW}⚠ Could not retrieve stats (may need auth)${NC}"
fi

# Step 6: Instructions
echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║${NC}  Next Steps                                  ${BLUE}║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════╝${NC}"
echo ""
echo "1. Restart frontend to apply compliance mode:"
echo "   npx nx serve clinical-portal"
echo ""
echo "2. Navigate to compliance dashboard:"
echo "   ${FRONTEND_URL}/compliance"
echo ""
echo "3. Monitor errors in real-time:"
echo "   - Errors will sync every 30-60 seconds"
echo "   - Alerts trigger at thresholds"
echo "   - Check backend logs for sync confirmations"
echo ""
echo "4. View errors in database:"
echo "   psql -h localhost -p 5435 -U healthdata -d gateway_db"
echo "   SELECT COUNT(*) FROM compliance_errors;"
echo ""
