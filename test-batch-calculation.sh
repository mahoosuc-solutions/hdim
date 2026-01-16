#!/bin/bash
# Test Batch Calculation Endpoints

GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
TENANT_ID="${TENANT_ID:-acme-health}"
AUTH_USERNAME="${AUTH_USERNAME:-demo.admin}"
AUTH_PASSWORD="${AUTH_PASSWORD:-demo123}"
QUALITY_API_BASE="${QUALITY_API_BASE:-${GATEWAY_URL}/api/quality/quality-measure}"
FHIR_URL="${FHIR_URL:-http://localhost:8085/fhir}"

AUTH_TOKEN=$(curl -s -X POST "${GATEWAY_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${AUTH_USERNAME}\",\"password\":\"${AUTH_PASSWORD}\"}" | jq -r '.accessToken' 2>/dev/null)

AUTH_HEADER=()
if [ -n "$AUTH_TOKEN" ] && [ "$AUTH_TOKEN" != "null" ]; then
  AUTH_HEADER=(-H "Authorization: Bearer $AUTH_TOKEN")
fi

echo "Testing batch calculation endpoints..."
echo ""

echo "1. Test GET /population/jobs (should be empty initially):"
curl -s -H "X-Tenant-ID: ${TENANT_ID}" "${AUTH_HEADER[@]}" \
  "${QUALITY_API_BASE}/population/jobs" | python3 -m json.tool

echo ""
echo "2. Test POST /population/calculate (start batch calculation):"
RESPONSE=$(curl -s -X POST -H "X-Tenant-ID: ${TENANT_ID}" "${AUTH_HEADER[@]}" \
  "${QUALITY_API_BASE}/population/calculate?fhirServerUrl=${FHIR_URL}&createdBy=test-user" \
  -H "Content-Type: application/json")
echo "$RESPONSE" | python3 -m json.tool

# Extract jobId from response
JOB_ID=$(echo "$RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('jobId', ''))" 2>/dev/null)

if [ -n "$JOB_ID" ]; then
  echo ""
  echo "Job ID: $JOB_ID"
  echo ""
  echo "3. Test GET /population/jobs/{jobId} (check job status):"
  sleep 2  # Give it a moment to process
  curl -s -H "X-Tenant-ID: ${TENANT_ID}" "${AUTH_HEADER[@]}" \
    "${QUALITY_API_BASE}/population/jobs/$JOB_ID" | python3 -m json.tool

  echo ""
  echo "4. Test GET /population/jobs (list all jobs):"
  curl -s -H "X-Tenant-ID: ${TENANT_ID}" "${AUTH_HEADER[@]}" \
    "${QUALITY_API_BASE}/population/jobs" | python3 -m json.tool
else
  echo "Failed to get job ID from response"
fi
