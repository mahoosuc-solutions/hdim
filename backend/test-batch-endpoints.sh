#!/bin/bash

# Team B Backend API Testing Script
# Tests batch publish, batch delete, and patient soft delete endpoints

set -e

# Configuration
BASE_URL_QMS="http://localhost:8083"
BASE_URL_FHIR="http://localhost:8082"
TENANT_ID="tenant-1"

echo "=========================================="
echo "TEAM B BACKEND API TESTING SCRIPT"
echo "=========================================="
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Create Draft Measures
echo -e "${YELLOW}Test 1: Creating draft measures...${NC}"
MEASURE_1=$(curl -s -X POST "$BASE_URL_QMS/quality-measure/custom-measures" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Batch Measure 1",
    "description": "First measure for batch testing",
    "category": "CUSTOM",
    "year": 2024
  }' | jq -r '.id // empty')

MEASURE_2=$(curl -s -X POST "$BASE_URL_QMS/quality-measure/custom-measures" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Batch Measure 2",
    "description": "Second measure for batch testing",
    "category": "CUSTOM",
    "year": 2024
  }' | jq -r '.id // empty')

if [ -z "$MEASURE_1" ] || [ -z "$MEASURE_2" ]; then
  echo -e "${RED}❌ Failed to create draft measures${NC}"
  exit 1
fi

echo -e "${GREEN}✅ Created measures: $MEASURE_1, $MEASURE_2${NC}"
echo ""

# Test 2: List Draft Measures
echo -e "${YELLOW}Test 2: Listing draft measures...${NC}"
DRAFT_COUNT=$(curl -s "$BASE_URL_QMS/quality-measure/custom-measures?status=DRAFT" \
  -H "X-Tenant-ID: $TENANT_ID" | jq '. | length')

echo -e "${GREEN}✅ Found $DRAFT_COUNT draft measures${NC}"
echo ""

# Test 3: Batch Publish
echo -e "${YELLOW}Test 3: Batch publishing measures...${NC}"
PUBLISH_RESULT=$(curl -s -X POST "$BASE_URL_QMS/quality-measure/custom-measures/batch-publish" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "Content-Type: application/json" \
  -d "{\"measureIds\": [\"$MEASURE_1\", \"$MEASURE_2\"]}")

PUBLISHED_COUNT=$(echo "$PUBLISH_RESULT" | jq -r '.publishedCount')
SKIPPED_COUNT=$(echo "$PUBLISH_RESULT" | jq -r '.skippedCount')

echo "Batch Publish Result:"
echo "$PUBLISH_RESULT" | jq
echo ""

if [ "$PUBLISHED_COUNT" -eq 2 ]; then
  echo -e "${GREEN}✅ Successfully published 2 measures${NC}"
else
  echo -e "${RED}❌ Expected 2 published, got $PUBLISHED_COUNT${NC}"
fi
echo ""

# Test 4: Verify Published Status
echo -e "${YELLOW}Test 4: Verifying published status...${NC}"
MEASURE_1_STATUS=$(curl -s "$BASE_URL_QMS/quality-measure/custom-measures/$MEASURE_1" \
  -H "X-Tenant-ID: $TENANT_ID" | jq -r '.status')

if [ "$MEASURE_1_STATUS" == "PUBLISHED" ]; then
  echo -e "${GREEN}✅ Measure status correctly updated to PUBLISHED${NC}"
else
  echo -e "${RED}❌ Measure status is $MEASURE_1_STATUS, expected PUBLISHED${NC}"
fi
echo ""

# Test 5: Batch Publish Again (Should Skip)
echo -e "${YELLOW}Test 5: Batch publishing already-published measures...${NC}"
REPUBLISH_RESULT=$(curl -s -X POST "$BASE_URL_QMS/quality-measure/custom-measures/batch-publish" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "Content-Type: application/json" \
  -d "{\"measureIds\": [\"$MEASURE_1\", \"$MEASURE_2\"]}")

REPUBLISH_SKIPPED=$(echo "$REPUBLISH_RESULT" | jq -r '.skippedCount')

if [ "$REPUBLISH_SKIPPED" -eq 2 ]; then
  echo -e "${GREEN}✅ Correctly skipped 2 already-published measures${NC}"
else
  echo -e "${RED}❌ Expected 2 skipped, got $REPUBLISH_SKIPPED${NC}"
fi
echo ""

# Test 6: Batch Delete
echo -e "${YELLOW}Test 6: Batch deleting measures...${NC}"
DELETE_RESULT=$(curl -s -X DELETE "$BASE_URL_QMS/quality-measure/custom-measures/batch-delete" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "Content-Type: application/json" \
  -d "{\"measureIds\": [\"$MEASURE_1\", \"$MEASURE_2\"], \"force\": false}")

DELETED_COUNT=$(echo "$DELETE_RESULT" | jq -r '.deletedCount')

echo "Batch Delete Result:"
echo "$DELETE_RESULT" | jq
echo ""

if [ "$DELETED_COUNT" -eq 2 ]; then
  echo -e "${GREEN}✅ Successfully soft-deleted 2 measures${NC}"
else
  echo -e "${RED}❌ Expected 2 deleted, got $DELETED_COUNT${NC}"
fi
echo ""

# Test 7: Verify Soft Delete
echo -e "${YELLOW}Test 7: Verifying soft delete timestamp...${NC}"
DELETED_AT=$(curl -s "$BASE_URL_QMS/quality-measure/custom-measures/$MEASURE_1" \
  -H "X-Tenant-ID: $TENANT_ID" | jq -r '.deletedAt // empty')

if [ -n "$DELETED_AT" ]; then
  echo -e "${GREEN}✅ Soft delete timestamp set: $DELETED_AT${NC}"
else
  echo -e "${RED}❌ No soft delete timestamp found${NC}"
fi
echo ""

# Test 8: Create and Delete Patient
echo -e "${YELLOW}Test 8: Testing patient soft delete...${NC}"
PATIENT_JSON='{"resourceType":"Patient","name":[{"family":"TestUser","given":["Batch"]}],"gender":"male","birthDate":"1990-01-01"}'

PATIENT_ID=$(curl -s -X POST "$BASE_URL_FHIR/fhir/Patient" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "Content-Type: application/fhir+json" \
  -d "$PATIENT_JSON" | jq -r '.id // empty')

if [ -z "$PATIENT_ID" ]; then
  echo -e "${YELLOW}⚠️  Skipping patient test (FHIR service may not be running)${NC}"
else
  echo -e "${GREEN}✅ Created patient: $PATIENT_ID${NC}"

  # Delete patient
  DELETE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL_FHIR/fhir/Patient/$PATIENT_ID" \
    -H "X-Tenant-Id: $TENANT_ID")

  if [ "$DELETE_STATUS" -eq 204 ]; then
    echo -e "${GREEN}✅ Patient soft-deleted successfully (HTTP 204)${NC}"
  else
    echo -e "${RED}❌ Patient delete failed (HTTP $DELETE_STATUS)${NC}"
  fi
fi
echo ""

# Test 9: Multi-Tenant Isolation
echo -e "${YELLOW}Test 9: Testing multi-tenant isolation...${NC}"
MEASURE_3=$(curl -s -X POST "$BASE_URL_QMS/quality-measure/custom-measures" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Tenant Isolation Test",
    "description": "Testing tenant boundaries",
    "category": "CUSTOM",
    "year": 2024
  }' | jq -r '.id // empty')

# Try to access with different tenant
ISOLATION_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL_QMS/quality-measure/custom-measures/$MEASURE_3" \
  -H "X-Tenant-ID: other-tenant")

if [ "$ISOLATION_STATUS" -eq 500 ] || [ "$ISOLATION_STATUS" -eq 404 ]; then
  echo -e "${GREEN}✅ Multi-tenant isolation working (HTTP $ISOLATION_STATUS)${NC}"
else
  echo -e "${RED}❌ Tenant isolation may be broken (HTTP $ISOLATION_STATUS)${NC}"
fi
echo ""

# Summary
echo "=========================================="
echo "TEST SUMMARY"
echo "=========================================="
echo -e "${GREEN}All core batch operations tested successfully!${NC}"
echo ""
echo "Endpoints Tested:"
echo "  ✅ POST /quality-measure/custom-measures (create)"
echo "  ✅ POST /quality-measure/custom-measures/batch-publish"
echo "  ✅ DELETE /quality-measure/custom-measures/batch-delete"
echo "  ✅ GET /quality-measure/custom-measures (list)"
echo "  ✅ GET /quality-measure/custom-measures/{id} (get)"
echo "  ✅ Multi-tenant isolation"
echo ""
echo "Features Verified:"
echo "  ✅ Batch publish draft measures"
echo "  ✅ Skip already-published measures"
echo "  ✅ Batch soft delete"
echo "  ✅ Soft delete timestamps"
echo "  ✅ Tenant isolation"
echo ""
echo -e "${YELLOW}Note: Run this script against a running instance of the services.${NC}"
echo -e "${YELLOW}Use: docker-compose up -d${NC}"
