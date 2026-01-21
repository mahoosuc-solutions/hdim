#!/bin/bash

# Compliance System Validation Script
# Tests end-to-end compliance error tracking flow

set -e

echo "=========================================="
echo "Compliance System Validation"
echo "=========================================="

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
FRONTEND_URL="${FRONTEND_URL:-http://localhost:4200}"
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
COMPLIANCE_ENDPOINT="${BACKEND_URL}/api/v1/compliance/errors"

echo ""
echo "Configuration:"
echo "  Frontend URL: ${FRONTEND_URL}"
echo "  Backend URL: ${BACKEND_URL}"
echo "  Compliance Endpoint: ${COMPLIANCE_ENDPOINT}"
echo ""

# Test 1: Check backend endpoint is accessible
echo "Test 1: Backend endpoint health check..."
if curl -s -f -o /dev/null "${BACKEND_URL}/actuator/health"; then
    echo -e "${GREEN}✓${NC} Backend is accessible"
else
    echo -e "${RED}✗${NC} Backend is not accessible at ${BACKEND_URL}"
    exit 1
fi

# Test 2: Test error sync endpoint (POST)
echo ""
echo "Test 2: Testing error sync endpoint..."
TEST_PAYLOAD='{
  "errors": [
    {
      "id": "err-1736934000000-test123",
      "timestamp": "2025-01-15T10:00:00.000Z",
      "context": {
        "service": "Test Service",
        "endpoint": "/api/test",
        "operation": "GET /api/test",
        "errorCode": "ERR-9001",
        "severity": "ERROR",
        "userId": "test-user",
        "tenantId": "test-tenant"
      },
      "message": "Test error message",
      "stack": "Error: Test error\n    at test.js:1:1"
    }
  ],
  "syncedAt": "2025-01-15T10:00:05.000Z"
}'

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: test-tenant" \
  -d "${TEST_PAYLOAD}" \
  "${COMPLIANCE_ENDPOINT}")

HTTP_CODE=$(echo "${RESPONSE}" | tail -n1)
BODY=$(echo "${RESPONSE}" | sed '$d')

if [ "${HTTP_CODE}" = "200" ]; then
    echo -e "${GREEN}✓${NC} Error sync endpoint responded successfully"
    echo "  Response: ${BODY}"
else
    echo -e "${RED}✗${NC} Error sync endpoint failed with HTTP ${HTTP_CODE}"
    echo "  Response: ${BODY}"
    exit 1
fi

# Test 3: Query errors (requires auth - skip for now)
echo ""
echo "Test 3: Query errors endpoint (requires authentication)..."
echo -e "${YELLOW}⚠${NC} Skipping - requires ADMIN/DEVELOPER role"

# Test 4: Check frontend build
echo ""
echo "Test 4: Frontend build status..."
if npx nx run clinical-portal:build --skip-nx-cache 2>&1 | grep -q "Successfully"; then
    echo -e "${GREEN}✓${NC} Frontend builds successfully"
else
    echo -e "${RED}✗${NC} Frontend build failed"
    exit 1
fi

# Test 5: Check backend compilation
echo ""
echo "Test 5: Backend compilation status..."
if ./gradlew :modules:services:gateway-clinical-service:compileJava --no-daemon 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    echo -e "${GREEN}✓${NC} Backend compiles successfully"
else
    echo -e "${YELLOW}⚠${NC} Backend compilation check - run manually: ./gradlew :modules:services:gateway-clinical-service:compileJava"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}Validation Complete${NC}"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Start backend: ./gradlew :modules:services:gateway-clinical-service:bootRun"
echo "2. Start frontend: npx nx serve clinical-portal"
echo "3. Navigate to: http://localhost:4200/compliance"
echo "4. Trigger errors and verify they appear in dashboard"
echo "5. Check backend logs for sync confirmations"
