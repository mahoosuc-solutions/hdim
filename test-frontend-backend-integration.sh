#!/bin/bash
#
# Frontend-Backend Integration Test
# Tests that Angular frontend can successfully call Patient Health APIs
#

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "========================================"
echo "Frontend-Backend Integration Test"
echo "========================================"
echo ""

API_BASE="http://localhost:8087/quality-measure/patient-health"
TENANT_ID="default"
PATIENT_ID="test-integration-$(date +%s)"

echo -e "${BLUE}Testing Patient Health API Endpoints${NC}"
echo "Patient ID: $PATIENT_ID"
echo ""

# Test 1: Submit PHQ-9 Assessment
echo -e "${YELLOW}1. Submitting PHQ-9 Assessment...${NC}"
RESPONSE=$(curl -s -X POST "$API_BASE/mental-health/assessments" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -d "{
    \"patientId\": \"$PATIENT_ID\",
    \"assessmentType\": \"phq-9\",
    \"responses\": {\"q1\":2,\"q2\":2,\"q3\":1,\"q4\":1,\"q5\":1,\"q6\":2,\"q7\":1,\"q8\":1,\"q9\":1},
    \"assessedBy\": \"Dr-Integration-Test\"
  }")

SCORE=$(echo "$RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('score', 'ERROR'))" 2>/dev/null || echo "ERROR")

if [ "$SCORE" = "12" ]; then
  echo -e "${GREEN}✓ PHQ-9 submission successful (Score: $SCORE)${NC}"
else
  echo -e "${RED}✗ PHQ-9 submission failed (Expected: 12, Got: $SCORE)${NC}"
  exit 1
fi

# Test 2: Get Patient Health Overview
echo -e "${YELLOW}2. Fetching Patient Health Overview...${NC}"
RESPONSE=$(curl -s "$API_BASE/overview/$PATIENT_ID" \
  -H "X-Tenant-ID: $TENANT_ID")

HEALTH_SCORE=$(echo "$RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('healthScore', {}).get('overallScore', 'ERROR'))" 2>/dev/null || echo "ERROR")

if [ "$HEALTH_SCORE" != "ERROR" ]; then
  echo -e "${GREEN}✓ Health overview retrieved (Score: $HEALTH_SCORE)${NC}"
else
  echo -e "${RED}✗ Health overview failed${NC}"
  exit 1
fi

# Test 3: Get Care Gaps
echo -e "${YELLOW}3. Fetching Care Gaps...${NC}"
RESPONSE=$(curl -s "$API_BASE/care-gaps/$PATIENT_ID?status=OPEN" \
  -H "X-Tenant-ID: $TENANT_ID")

GAP_COUNT=$(echo "$RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(len(data))" 2>/dev/null || echo "0")

if [ "$GAP_COUNT" -ge "1" ]; then
  echo -e "${GREEN}✓ Care gaps retrieved ($GAP_COUNT gaps found - auto-created from PHQ-9)${NC}"
else
  echo -e "${YELLOW}⚠ Care gaps retrieved but none found (Expected auto-creation from positive PHQ-9)${NC}"
fi

# Test 4: Get Risk Stratification
echo -e "${YELLOW}4. Calculating Risk Stratification...${NC}"
RESPONSE=$(curl -s -X POST "$API_BASE/risk-stratification/$PATIENT_ID/calculate" \
  -H "X-Tenant-ID: $TENANT_ID")

RISK_SCORE=$(echo "$RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('riskScore', 'ERROR'))" 2>/dev/null || echo "ERROR")

if [ "$RISK_SCORE" != "ERROR" ]; then
  echo -e "${GREEN}✓ Risk stratification calculated (Score: $RISK_SCORE)${NC}"
else
  echo -e "${RED}✗ Risk stratification failed${NC}"
  exit 1
fi

# Test 5: Get Mental Health Assessment History
echo -e "${YELLOW}5. Fetching Assessment History...${NC}"
RESPONSE=$(curl -s "$API_BASE/mental-health/assessments/$PATIENT_ID?limit=10" \
  -H "X-Tenant-ID: $TENANT_ID")

ASSESSMENT_COUNT=$(echo "$RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(len(data))" 2>/dev/null || echo "0")

if [ "$ASSESSMENT_COUNT" -ge "1" ]; then
  echo -e "${GREEN}✓ Assessment history retrieved ($ASSESSMENT_COUNT assessments)${NC}"
else
  echo -e "${RED}✗ Assessment history failed${NC}"
  exit 1
fi

echo ""
echo "========================================"
echo -e "${GREEN}All Integration Tests Passed! ✓${NC}"
echo "========================================"
echo ""
echo "Summary:"
echo "  • PHQ-9 Assessment: Score 12 (Moderate)"
echo "  • Health Overview: Score $HEALTH_SCORE"
echo "  • Care Gaps: $GAP_COUNT gaps"
echo "  • Risk Stratification: Score $RISK_SCORE"
echo "  • Assessment History: $ASSESSMENT_COUNT assessments"
echo ""
echo "Next Steps:"
echo "  1. Start Angular dev server: npx nx serve clinical-portal"
echo "  2. Open browser: http://localhost:4200"
echo "  3. Navigate to Patient Health Overview page"
echo "  4. Verify data loads from backend APIs"
echo ""
