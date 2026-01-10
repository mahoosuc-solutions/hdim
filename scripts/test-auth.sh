#!/bin/bash
# Test authentication flow

echo "=== HDIM Authentication Validation ==="
echo ""

# Test 1: Login all users
echo "1. Testing user logins..."
for user in test_admin test_evaluator demo_admin; do
    RESPONSE=$(curl -s -X POST http://localhost:8001/api/v1/auth/login \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$user\",\"password\":\"password123\"}")

    if echo "$RESPONSE" | grep -q "Login successful"; then
        echo "   ✓ $user: Login successful"
    else
        echo "   ✗ $user: Login failed"
    fi
done

echo ""
echo "2. Testing /auth/me endpoint..."
TOKEN=$(curl -s -X POST http://localhost:8001/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test_admin","password":"password123"}' | \
    grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

ME_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8001/api/v1/auth/me)
if echo "$ME_RESPONSE" | grep -q "test_admin"; then
    echo "   ✓ /auth/me returned user info correctly"
else
    echo "   ✗ /auth/me failed"
fi

echo ""
echo "3. Testing service health endpoints..."
for svc in "Gateway:8001:/actuator/health" "FHIR:8085:/fhir/actuator/health" "Patient:8084:/patient/actuator/health" "QualityMeasure:8087:/quality-measure/actuator/health" "CareGap:8086:/care-gap/actuator/health"; do
    NAME=$(echo $svc | cut -d: -f1)
    PORT=$(echo $svc | cut -d: -f2)
    HEALTH_PATH=$(echo $svc | cut -d: -f3-)
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$PORT$HEALTH_PATH")
    if [ "$STATUS" = "200" ]; then
        echo "   ✓ $NAME service: healthy"
    else
        echo "   ⚠ $NAME service: status $STATUS"
    fi
done

echo ""
echo "4. Testing authenticated API access..."
echo "   Note: Backend services use gateway-trust auth (X-Auth-* headers)"
# In dev mode, we can simulate gateway-trust headers
# Test Patient service which has proper FHIR-style endpoints
PATIENT_RESULT=$(curl -s \
    -H "X-Auth-User-Id: 11111111-1111-1111-1111-111111111111" \
    -H "X-Auth-Username: test_admin" \
    -H "X-Auth-Tenant-Ids: demo-tenant" \
    -H "X-Auth-Roles: ADMIN,EVALUATOR" \
    -H "X-Auth-Validated: gateway-dev-mode-bypass" \
    -H "X-Tenant-ID: demo-tenant" \
    http://localhost:8084/patient/api/v1/patients 2>/dev/null)
PATIENT_STATUS=$?
if echo "$PATIENT_RESULT" | grep -q "content\|patients\|Patient\|\[\]"; then
    echo "   ✓ Patient service: authenticated access works"
else
    echo "   ⚠ Patient service: check response"
fi

echo ""
echo "5. Testing External FHIR Server..."
EXT_FHIR=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/fhir/metadata)
if [ "$EXT_FHIR" = "200" ]; then
    echo "   ✓ External HAPI FHIR: accessible"
else
    echo "   ⚠ External HAPI FHIR: status $EXT_FHIR"
fi

echo ""
echo "=== Validation Complete ==="
