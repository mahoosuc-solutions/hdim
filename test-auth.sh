#!/bin/bash
# Quick Test Script for Gateway Authentication

echo "🧪 Testing Gateway Authentication..."

# Test 1: Health check (no auth required)
echo ""
echo "1️⃣ Testing health endpoint (public)..."
curl -s http://localhost:8000/actuator/health | jq .

# Test 2: Login to get JWT
echo ""
echo "2️⃣ Testing login endpoint..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

echo "$LOGIN_RESPONSE" | jq .

# Extract access token
ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r .accessToken)

if [ "$ACCESS_TOKEN" != "null" ] && [ -n "$ACCESS_TOKEN" ]; then
  echo "✅ Login successful! Got JWT token"
  
  # Test 3: Call protected endpoint with JWT
  echo ""
  echo "3️⃣ Testing protected endpoint with JWT..."
  curl -s http://localhost:8000/api/quality/patient-health/overview/Patient-123 \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "X-Tenant-ID: tenant-1" | jq .
  
  echo ""
  echo "✅ All tests passed!"
else
  echo "❌ Login failed - check if Gateway is running and database is initialized"
fi
