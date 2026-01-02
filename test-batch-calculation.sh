#!/bin/bash
# Test Batch Calculation Endpoints

echo "Testing batch calculation endpoints..."
echo ""

echo "1. Test GET /api/v1/population/jobs (should be empty initially):"
curl -s -H "X-Tenant-ID: default-tenant" \
  http://localhost:8087/quality-measure/api/v1/population/jobs | python3 -m json.tool

echo ""
echo "2. Test POST /api/v1/population/calculate (start batch calculation):"
RESPONSE=$(curl -s -X POST -H "X-Tenant-ID: default-tenant" \
  "http://localhost:8087/quality-measure/api/v1/population/calculate?fhirServerUrl=http://fhir-service-mock:8080/fhir&createdBy=test-user" \
  -H "Content-Type: application/json")
echo "$RESPONSE" | python3 -m json.tool

# Extract jobId from response
JOB_ID=$(echo "$RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('jobId', ''))" 2>/dev/null)

if [ -n "$JOB_ID" ]; then
  echo ""
  echo "Job ID: $JOB_ID"
  echo ""
  echo "3. Test GET /api/v1/population/jobs/{jobId} (check job status):"
  sleep 2  # Give it a moment to process
  curl -s -H "X-Tenant-ID: default-tenant" \
    "http://localhost:8087/quality-measure/api/v1/population/jobs/$JOB_ID" | python3 -m json.tool

  echo ""
  echo "4. Test GET /api/v1/population/jobs (list all jobs):"
  curl -s -H "X-Tenant-ID: default-tenant" \
    http://localhost:8087/quality-measure/api/v1/population/jobs | python3 -m json.tool
else
  echo "Failed to get job ID from response"
fi
