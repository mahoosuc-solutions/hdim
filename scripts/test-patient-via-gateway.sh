#!/bin/bash
# Test patient service via gateway

curl -s -X POST http://localhost:8001/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test_admin","password":"password123"}' > /tmp/login.json

TOKEN=$(grep -o '"accessToken":"[^"]*"' /tmp/login.json | cut -d'"' -f4)

echo "Testing patient endpoint via gateway..."
# Gateway forwards /patient/** to patient-service with path prefix /patient stripped
# So /patient/health-record becomes /health-record on patient-service
# Patient service context path is /patient, so full path is /patient/health-record
curl -s -w "\nHTTP Status: %{http_code}\n" \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-Tenant-ID: demo-tenant" \
    "http://localhost:8001/patient/health-record?patient=test-patient-1" | tail -10
