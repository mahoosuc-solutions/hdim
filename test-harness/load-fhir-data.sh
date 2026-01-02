#!/bin/bash
# Load FHIR data into HDIM platform

set -e

# Get token
echo "Getting authentication token..."
AUTH_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username": "test_admin", "password": "password123"}')

TOKEN=$(echo "$AUTH_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))")

if [ -z "$TOKEN" ]; then
  echo "Failed to get token"
  exit 1
fi

echo "Token obtained successfully"

# Test FHIR endpoint
echo "Testing FHIR endpoint..."
RESPONSE=$(curl -s -w "\n%{http_code}" "http://localhost:8080/fhir/Patient" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default")

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
echo "FHIR Patient endpoint response code: $HTTP_CODE"

# Load the FHIR bundle
DATA_FILE="${1:-/home/mahoosuc-solutions/projects/hdim-master/test-harness/gcp-data/academic-medical-center-1000-fhir.json}"

if [ ! -f "$DATA_FILE" ]; then
  echo "Data file not found: $DATA_FILE"
  exit 1
fi

echo "Loading FHIR bundle from: $DATA_FILE"
echo "Bundle size: $(wc -c < "$DATA_FILE") bytes"

# Post the bundle to FHIR service
echo "Posting bundle to FHIR service..."
LOAD_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "http://localhost:8080/fhir" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default" \
  -H "Content-Type: application/fhir+json" \
  -d @"$DATA_FILE" --max-time 300)

LOAD_HTTP_CODE=$(echo "$LOAD_RESPONSE" | tail -1)
LOAD_BODY=$(echo "$LOAD_RESPONSE" | head -n -1)

echo "Load response code: $LOAD_HTTP_CODE"
echo "Response (first 500 chars): ${LOAD_BODY:0:500}"

if [ "$LOAD_HTTP_CODE" = "200" ] || [ "$LOAD_HTTP_CODE" = "201" ]; then
  echo "SUCCESS: FHIR bundle loaded successfully!"
else
  echo "WARNING: Unexpected response code $LOAD_HTTP_CODE"
fi
