#!/bin/bash

# Test Batch Evaluation Script
# Triggers a batch evaluation to test real-time WebSocket updates

echo "🚀 Triggering Test Batch Evaluation..."
echo ""

# CQL Engine API endpoint
API_URL="http://localhost:8081/cql-engine/api/v1/evaluate/batch"

# Test payload
PAYLOAD='{
  "measureId": "TEST_MEASURE_001",
  "patientIds": [
    "patient-001",
    "patient-002",
    "patient-003",
    "patient-004",
    "patient-005"
  ],
  "tenantId": "TENANT001",
  "batchId": "test-batch-'$(date +%s)'"
}'

echo "📤 Sending batch evaluation request..."
echo "API: $API_URL"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: TENANT001" \
  -d "$PAYLOAD")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

echo "📥 Response:"
echo "HTTP Status: $HTTP_CODE"
echo ""

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 202 ]; then
  echo "✅ Batch evaluation triggered successfully!"
  echo ""
  echo "Response Body:"
  echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
  echo ""
  echo "🔌 Check the dashboard at http://localhost:3002 for real-time updates!"
  echo ""
  echo "📊 You should see:"
  echo "  - ConnectionStatus change to 'Connected'"
  echo "  - Batch progress updates in PerformanceMetricsPanel"
  echo "  - Events appearing in the event list"
  echo "  - Real-time metrics updating"
else
  echo "❌ Request failed with status $HTTP_CODE"
  echo ""
  echo "Response:"
  echo "$BODY"
  echo ""
  echo "🔍 Troubleshooting:"
  echo "  1. Check if CQL Engine is running: curl http://localhost:8081/cql-engine/actuator/health"
  echo "  2. Check Docker containers: docker ps"
  echo "  3. Check service logs: docker logs healthdata-cql-engine"
fi

echo ""
echo "📝 Additional test commands:"
echo ""
echo "# Check WebSocket endpoint"
echo "curl -i -N \\"
echo "  -H 'Connection: Upgrade' \\"
echo "  -H 'Upgrade: websocket' \\"
echo "  -H 'Sec-WebSocket-Key: test' \\"
echo "  -H 'Sec-WebSocket-Version: 13' \\"
echo "  http://localhost:8081/cql-engine/ws"
echo ""
echo "# Trigger another batch"
echo "./test-batch-evaluation.sh"
