#!/bin/bash
set +e

# HDIM K3s Demo Validation
# Validates all pods, ingress routing, auth flow, FHIR, demo data

NAMESPACE="${NAMESPACE:-hdim-demo}"
BASE_URL="${BASE_URL:-http://demo.healthdatainmotion.com}"
API_URL="${API_URL:-http://api.healthdatainmotion.com}"
FHIR_URL="${FHIR_URL:-http://fhir.healthdatainmotion.com}"

echo "================================================"
echo "  HDIM K3s Demo Validation"
echo "================================================"
echo ""

PASS=0
FAIL=0
WARN=0

check() {
    local label="$1" result="$2"
    if [ "$result" = "pass" ]; then echo "  [PASS] $label"; PASS=$((PASS+1))
    elif [ "$result" = "warn" ]; then echo "  [WARN] $label"; WARN=$((WARN+1))
    else echo "  [FAIL] $label"; FAIL=$((FAIL+1)); fi
}

# 1. Pod status
echo "1. Pod Status"
READY=$(kubectl -n "$NAMESPACE" get pods --no-headers 2>/dev/null | grep -c "Running")
TOTAL=$(kubectl -n "$NAMESPACE" get pods --no-headers 2>/dev/null | wc -l)
if [ "$READY" -ge 19 ] 2>/dev/null; then check "All pods running ($READY/$TOTAL)" "pass"
elif [ "$READY" -ge 15 ] 2>/dev/null; then check "Most pods running ($READY/$TOTAL)" "warn"
else check "Pods running: $READY/$TOTAL" "fail"; fi

# Show non-ready pods
kubectl -n "$NAMESPACE" get pods --no-headers 2>/dev/null | grep -v Running | while read -r line; do
    echo "    -> $line"
done
echo ""

# 2. Infrastructure
echo "2. Infrastructure"
for svc in postgres redis kafka jaeger; do
    STATUS=$(kubectl -n "$NAMESPACE" get pod -l app=$svc -o jsonpath='{.items[0].status.phase}' 2>/dev/null)
    if [ "$STATUS" = "Running" ]; then check "$svc" "pass"; else check "$svc" "fail"; fi
done
echo ""

# 3. Service health endpoints
echo "3. Service Health (via ingress)"
for pair in \
    "Clinical Portal|${BASE_URL}/" \
    "Gateway Edge|${API_URL}/actuator/health" \
    "FHIR Metadata|${FHIR_URL}/metadata"; do
    label="${pair%%|*}"
    url="${pair##*|}"
    if curl -sf "$url" > /dev/null 2>&1; then check "$label" "pass"; else check "$label" "fail"; fi
done
echo ""

# 4. Auth flow
echo "4. Authentication"
LOGIN_RESPONSE=$(curl -sf -X POST "${API_URL}/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"demo_admin","password":"demo123"}' 2>/dev/null)

if echo "$LOGIN_RESPONSE" | grep -q "token"; then
    check "Login with demo_admin" "pass"
    TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | head -1 | cut -d'"' -f4)
    if [ -n "$TOKEN" ]; then
        check "JWT token received" "pass"
        AUTH_RESP=$(curl -sf "${API_URL}/patient/api/v1/patients" \
            -H "Authorization: Bearer $TOKEN" \
            -H "X-Tenant-ID: demo-tenant" 2>/dev/null)
        if [ -n "$AUTH_RESP" ]; then check "Authenticated request" "pass"
        else check "Authenticated request" "fail"; fi
    else check "JWT token received" "fail"; fi
else check "Login with demo_admin" "fail"; fi
echo ""

# 5. FHIR
echo "5. FHIR Conformance"
FHIR_META=$(curl -sf "${FHIR_URL}/metadata" 2>/dev/null)
if echo "$FHIR_META" | grep -q "4.0.1"; then check "FHIR R4 version 4.0.1" "pass"
elif [ -n "$FHIR_META" ]; then check "FHIR metadata (version mismatch)" "warn"
else check "FHIR R4 metadata" "fail"; fi
echo ""

# 6. Basic auth on internal routes
echo "6. Internal Route Protection"
TRACES_UNAUTH=$(curl -sf -o /dev/null -w "%{http_code}" "http://traces.healthdatainmotion.com/" 2>/dev/null)
if [ "$TRACES_UNAUTH" = "401" ]; then check "Jaeger requires basic auth" "pass"
else check "Jaeger requires basic auth (got $TRACES_UNAUTH)" "warn"; fi
echo ""

# Summary
TOTAL=$((PASS + FAIL + WARN))
echo "================================================"
echo "  Validation Summary"
echo "================================================"
echo ""
echo "  PASS:     $PASS/$TOTAL"
echo "  WARNINGS: $WARN/$TOTAL"
echo "  FAILED:   $FAIL/$TOTAL"
echo ""
if [ $FAIL -eq 0 ]; then echo "  All checks passed!"; else echo "  $FAIL check(s) failed."; fi
echo ""

exit $FAIL
