#!/bin/bash
# Test FHIR Patient endpoint access

echo "Getting fresh token..."
curl -s -X POST http://localhost:8001/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test_admin","password":"password123"}' > /tmp/fresh_login.json

TOKEN=$(grep -o '"accessToken":"[^"]*"' /tmp/fresh_login.json | cut -d'"' -f4)
echo "Token length: ${#TOKEN}"

echo ""
echo "Testing direct FHIR service access (port 8085):"
curl -s -w "\nHTTP Status: %{http_code}\n" \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-Tenant-ID: demo-tenant" \
    http://localhost:8085/fhir/Patient 2>&1 | tail -5

echo ""
echo "Testing FHIR via Gateway (port 8001):"
curl -s -w "\nHTTP Status: %{http_code}\n" \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-Tenant-ID: demo-tenant" \
    http://localhost:8001/fhir/Patient 2>&1 | tail -5
