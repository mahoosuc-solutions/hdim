#!/bin/bash

# Create Test CQL Library and Measure
# This script creates sample data needed for batch evaluations

set -e

API_BASE="http://localhost:8081/cql-engine"
AUTH="cql-service-user:cql-service-dev-password-change-in-prod"
TENANT="TENANT001"

echo "🚀 Creating Test CQL Library and Measure..."
echo ""

# Step 1: Create a simple CQL library
echo "📚 Step 1: Creating CQL Library..."
LIBRARY_RESPONSE=$(curl -s -u "$AUTH" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: $TENANT" \
  -X POST "$API_BASE/api/v1/cql/libraries" \
  -d '{
    "libraryName": "TestMeasure",
    "version": "1.0.0",
    "cqlContent": "library TestMeasure version '\''1.0.0'\''\ndefine \"Result\": true",
    "description": "Simple test measure for dashboard testing"
  }')

LIBRARY_ID=$(echo "$LIBRARY_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', 'ERROR'))" 2>/dev/null || echo "ERROR")

if [ "$LIBRARY_ID" = "ERROR" ]; then
  echo "❌ Failed to create library"
  echo "Response: $LIBRARY_RESPONSE"
  exit 1
fi

echo "✅ Library created with ID: $LIBRARY_ID"
echo ""

# Step 2: Verify library was created
echo "🔍 Step 2: Verifying library..."
curl -s -u "$AUTH" \
  -H "X-Tenant-ID: $TENANT" \
  "$API_BASE/api/v1/cql/libraries/$LIBRARY_ID" | python3 -m json.tool | head -20

echo ""
echo "✅ Test library created successfully!"
echo ""
echo "📊 You can now trigger batch evaluations:"
echo ""
echo "curl -u \"$AUTH\" \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -H \"X-Tenant-ID: $TENANT\" \\"
echo "  -X POST \"$API_BASE/api/v1/cql/evaluations/batch?libraryId=$LIBRARY_ID\" \\"
echo "  -d '[\"patient-001\", \"patient-002\", \"patient-003\"]'"
echo ""
