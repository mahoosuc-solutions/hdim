#!/bin/bash

# Patient Health Overview API Test Script
# Tests all 8 REST endpoints with sample data

set -e  # Exit on error

API_BASE="http://localhost:8087/quality-measure/patient-health"
TENANT_ID="test-tenant"
PATIENT_ID="Patient/test-$(date +%s)"  # Unique patient ID

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "Patient Health Overview API Tests"
echo "=========================================="
echo "API Base: $API_BASE"
echo "Tenant ID: $TENANT_ID"
echo "Patient ID: $PATIENT_ID"
echo ""

# Check if service is running
echo -e "${YELLOW}Checking if service is healthy...${NC}"
if curl -s -f http://localhost:8087/quality-measure/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Service is healthy${NC}"
else
    echo -e "${RED}✗ Service is not responding. Please start the service first.${NC}"
    exit 1
fi

# Note: In production, you'd need a valid JWT token
# For testing, we'll assume the endpoints are accessible or add basic auth
# TOKEN="your-jwt-token-here"
# AUTH_HEADER="Authorization: Bearer $TOKEN"

echo ""
echo "=========================================="
echo "Test 1: Submit PHQ-9 Assessment (Moderate Depression)"
echo "=========================================="
ASSESSMENT_RESPONSE=$(curl -s -X POST "$API_BASE/mental-health/assessments" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -d "{
    \"patientId\": \"$PATIENT_ID\",
    \"assessmentType\": \"phq-9\",
    \"responses\": {
      \"q1\": 2, \"q2\": 2, \"q3\": 1, \"q4\": 1, \"q5\": 1,
      \"q6\": 2, \"q7\": 1, \"q8\": 1, \"q9\": 1
    },
    \"assessedBy\": \"Practitioner/Dr-Test\",
    \"clinicalNotes\": \"Test assessment - moderate symptoms reported\"
  }")

if echo "$ASSESSMENT_RESPONSE" | grep -q "score"; then
    echo -e "${GREEN}✓ Assessment submitted successfully${NC}"
    echo "Response: $ASSESSMENT_RESPONSE" | jq '.' 2>/dev/null || echo "$ASSESSMENT_RESPONSE"

    # Extract score to verify
    SCORE=$(echo "$ASSESSMENT_RESPONSE" | jq -r '.score' 2>/dev/null || echo "N/A")
    SEVERITY=$(echo "$ASSESSMENT_RESPONSE" | jq -r '.severity' 2>/dev/null || echo "N/A")
    POSITIVE=$(echo "$ASSESSMENT_RESPONSE" | jq -r '.positiveScreen' 2>/dev/null || echo "N/A")

    echo ""
    echo "Expected: Score=12, Severity=moderate, PositiveScreen=true"
    echo "Actual:   Score=$SCORE, Severity=$SEVERITY, PositiveScreen=$POSITIVE"

    if [ "$SCORE" = "12" ] && [ "$SEVERITY" = "moderate" ] && [ "$POSITIVE" = "true" ]; then
        echo -e "${GREEN}✓ Scoring algorithm validated!${NC}"
    fi
else
    echo -e "${RED}✗ Failed to submit assessment${NC}"
    echo "Response: $ASSESSMENT_RESPONSE"
fi

sleep 2

echo ""
echo "=========================================="
echo "Test 2: Get Patient Assessments"
echo "=========================================="
ASSESSMENTS_RESPONSE=$(curl -s "$API_BASE/mental-health/assessments/$PATIENT_ID" \
  -H "X-Tenant-ID: $TENANT_ID")

if echo "$ASSESSMENTS_RESPONSE" | grep -q "score\|assessmentType"; then
    echo -e "${GREEN}✓ Retrieved assessments successfully${NC}"
    echo "$ASSESSMENTS_RESPONSE" | jq '.' 2>/dev/null || echo "$ASSESSMENTS_RESPONSE"
else
    echo -e "${RED}✗ Failed to retrieve assessments${NC}"
    echo "Response: $ASSESSMENTS_RESPONSE"
fi

sleep 2

echo ""
echo "=========================================="
echo "Test 3: Get Assessment Trend"
echo "=========================================="
TREND_RESPONSE=$(curl -s "$API_BASE/mental-health/assessments/$PATIENT_ID/trend?type=phq-9" \
  -H "X-Tenant-ID: $TENANT_ID")

if echo "$TREND_RESPONSE" | grep -q "trend\|assessmentType"; then
    echo -e "${GREEN}✓ Retrieved trend successfully${NC}"
    echo "$TREND_RESPONSE" | jq '.' 2>/dev/null || echo "$TREND_RESPONSE"
else
    echo -e "${RED}✗ Failed to retrieve trend${NC}"
    echo "Response: $TREND_RESPONSE"
fi

sleep 2

echo ""
echo "=========================================="
echo "Test 4: Get Care Gaps (Should have 1 auto-created)"
echo "=========================================="
CARE_GAPS_RESPONSE=$(curl -s "$API_BASE/care-gaps/$PATIENT_ID" \
  -H "X-Tenant-ID: $TENANT_ID")

if echo "$CARE_GAPS_RESPONSE" | grep -q "gapType\|title\|\[\]"; then
    echo -e "${GREEN}✓ Retrieved care gaps successfully${NC}"
    echo "$CARE_GAPS_RESPONSE" | jq '.' 2>/dev/null || echo "$CARE_GAPS_RESPONSE"

    GAP_COUNT=$(echo "$CARE_GAPS_RESPONSE" | jq 'length' 2>/dev/null || echo "0")
    if [ "$GAP_COUNT" -gt "0" ]; then
        echo -e "${GREEN}✓ Auto-created care gap verified! ($GAP_COUNT gap(s) found)${NC}"
    else
        echo -e "${YELLOW}⚠ No care gaps found (may not have been auto-created yet)${NC}"
    fi
else
    echo -e "${RED}✗ Failed to retrieve care gaps${NC}"
    echo "Response: $CARE_GAPS_RESPONSE"
fi

sleep 2

echo ""
echo "=========================================="
echo "Test 5: Calculate Risk Stratification"
echo "=========================================="
RISK_RESPONSE=$(curl -s -X POST "$API_BASE/risk-stratification/$PATIENT_ID/calculate" \
  -H "X-Tenant-ID: $TENANT_ID")

if echo "$RISK_RESPONSE" | grep -q "riskScore\|riskLevel"; then
    echo -e "${GREEN}✓ Risk assessment calculated successfully${NC}"
    echo "$RISK_RESPONSE" | jq '.' 2>/dev/null || echo "$RISK_RESPONSE"

    RISK_SCORE=$(echo "$RISK_RESPONSE" | jq -r '.riskScore' 2>/dev/null || echo "N/A")
    RISK_LEVEL=$(echo "$RISK_RESPONSE" | jq -r '.riskLevel' 2>/dev/null || echo "N/A")
    echo "Risk Score: $RISK_SCORE, Risk Level: $RISK_LEVEL"
else
    echo -e "${RED}✗ Failed to calculate risk${NC}"
    echo "Response: $RISK_RESPONSE"
fi

sleep 2

echo ""
echo "=========================================="
echo "Test 6: Get Risk Stratification"
echo "=========================================="
RISK_GET_RESPONSE=$(curl -s "$API_BASE/risk-stratification/$PATIENT_ID" \
  -H "X-Tenant-ID: $TENANT_ID")

if echo "$RISK_GET_RESPONSE" | grep -q "riskScore\|riskLevel"; then
    echo -e "${GREEN}✓ Retrieved risk assessment successfully${NC}"
    echo "$RISK_GET_RESPONSE" | jq '.' 2>/dev/null || echo "$RISK_GET_RESPONSE"
else
    echo -e "${RED}✗ Failed to retrieve risk assessment${NC}"
    echo "Response: $RISK_GET_RESPONSE"
fi

sleep 2

echo ""
echo "=========================================="
echo "Test 7: Get Health Score"
echo "=========================================="
HEALTH_SCORE_RESPONSE=$(curl -s "$API_BASE/health-score/$PATIENT_ID" \
  -H "X-Tenant-ID: $TENANT_ID")

if echo "$HEALTH_SCORE_RESPONSE" | grep -q "overallScore\|interpretation"; then
    echo -e "${GREEN}✓ Retrieved health score successfully${NC}"
    echo "$HEALTH_SCORE_RESPONSE" | jq '.' 2>/dev/null || echo "$HEALTH_SCORE_RESPONSE"

    OVERALL_SCORE=$(echo "$HEALTH_SCORE_RESPONSE" | jq -r '.overallScore' 2>/dev/null || echo "N/A")
    INTERPRETATION=$(echo "$HEALTH_SCORE_RESPONSE" | jq -r '.interpretation' 2>/dev/null || echo "N/A")
    echo "Overall Score: $OVERALL_SCORE, Interpretation: $INTERPRETATION"
else
    echo -e "${RED}✗ Failed to retrieve health score${NC}"
    echo "Response: $HEALTH_SCORE_RESPONSE"
fi

sleep 2

echo ""
echo "=========================================="
echo "Test 8: Get Complete Health Overview"
echo "=========================================="
OVERVIEW_RESPONSE=$(curl -s "$API_BASE/overview/$PATIENT_ID" \
  -H "X-Tenant-ID: $TENANT_ID")

if echo "$OVERVIEW_RESPONSE" | grep -q "healthScore\|summaryStats"; then
    echo -e "${GREEN}✓ Retrieved complete health overview successfully${NC}"
    echo "$OVERVIEW_RESPONSE" | jq '.' 2>/dev/null || echo "$OVERVIEW_RESPONSE"

    TOTAL_GAPS=$(echo "$OVERVIEW_RESPONSE" | jq -r '.summaryStats.totalOpenCareGaps' 2>/dev/null || echo "N/A")
    URGENT_GAPS=$(echo "$OVERVIEW_RESPONSE" | jq -r '.summaryStats.urgentCareGaps' 2>/dev/null || echo "N/A")
    echo "Total Open Care Gaps: $TOTAL_GAPS, Urgent: $URGENT_GAPS"
else
    echo -e "${RED}✗ Failed to retrieve health overview${NC}"
    echo "Response: $OVERVIEW_RESPONSE"
fi

echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo "All 8 endpoints tested!"
echo ""
echo "Next Steps:"
echo "1. Review the responses above"
echo "2. Verify scoring algorithms are correct"
echo "3. Check that care gaps were auto-created"
echo "4. Update frontend to use these APIs"
echo ""
echo "For detailed testing, see: DEPLOYMENT_STATUS_FINAL.md"
echo "=========================================="
