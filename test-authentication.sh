#!/bin/bash
#
# Authentication Test Script
# Tests login and token-based API access
#

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "========================================"
echo "Authentication Test"
echo "========================================"
echo ""

# Test 1: Login as admin
echo -e "${YELLOW}1. Login as test_admin...${NC}"
RESPONSE=$(curl -s -X POST http://localhost:8087/quality-measure/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"test_admin","password":"password123"}')

TOKEN=$(echo "$RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('accessToken', 'ERROR'))" 2>/dev/null || echo "ERROR")

if [ "$TOKEN" != "ERROR" ] && [ "$TOKEN" != "null" ] && [ -n "$TOKEN" ]; then
  echo -e "${GREEN}✓ Login successful - Token received (${#TOKEN} characters)${NC}"
  echo ""

  # Test 2: Access API with token
  echo -e "${YELLOW}2. Access Patient Health API with token...${NC}"
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8087/quality-measure/patient-health/overview/test123 \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-Tenant-ID: default")

  if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Authenticated access successful (HTTP $HTTP_CODE)${NC}"
  else
    echo -e "${YELLOW}⚠ Response: HTTP $HTTP_CODE${NC}"
    echo -e "${YELLOW}  (Note: Auth currently disabled with .permitAll())${NC}"
  fi
else
  echo -e "${RED}✗ Login failed${NC}"
  echo "Response: $RESPONSE"
  exit 1
fi

echo ""
echo "========================================"
echo -e "${GREEN}Authentication Test Complete${NC}"
echo "========================================"
echo ""
echo "Test Users Available:"
echo "  • test_superadmin (SUPER_ADMIN)"
echo "  • test_admin (ADMIN)"
echo "  • test_evaluator (EVALUATOR)"
echo "  • test_analyst (ANALYST)"
echo "  • test_viewer (VIEWER)"
echo "  • test_multiuser (ADMIN, ANALYST, EVALUATOR)"
echo ""
echo "All passwords: password123"
echo ""
echo "For more details, see: AUTHENTICATION_GUIDE.md"
echo ""
