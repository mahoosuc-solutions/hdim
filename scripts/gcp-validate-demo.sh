#!/bin/bash
set +e

# HealthData Demo Validation Script
# Comprehensive checks for all demo services, auth flow, and data presence

# Configuration
PROJECT_ID=${PROJECT_ID:-"healthcare-data-in-motion"}
ZONE=${ZONE:-"us-central1-a"}
VM_NAME=${VM_NAME:-"healthdata-demo"}

# Allow running locally (no GCP SSH) or remotely
if [ "${LOCAL:-}" = "true" ]; then
    BASE_URL="${BASE_URL:-http://localhost}"
    RUN_CMD="bash -c"
else
    # Get VM IP
    VM_IP=$(gcloud compute instances describe $VM_NAME \
      --project=$PROJECT_ID \
      --zone=$ZONE \
      --format="get(networkInterfaces[0].accessConfigs[0].natIP)" 2>/dev/null)

    if [ -z "$VM_IP" ]; then
        echo "❌ Cannot determine VM IP. Is the VM running?"
        echo "   ./scripts/gcp-demo-status.sh"
        exit 1
    fi
    BASE_URL="http://$VM_IP"
fi

echo "================================================"
echo "  HealthData Demo Validation"
echo "================================================"
echo ""
echo "Target: $BASE_URL"
echo ""

PASS=0
FAIL=0
WARN=0

check() {
    local label="$1"
    local result="$2"
    if [ "$result" = "pass" ]; then
        echo "  ✅ $label"
        PASS=$((PASS + 1))
    elif [ "$result" = "warn" ]; then
        echo "  ⚠️  $label"
        WARN=$((WARN + 1))
    else
        echo "  ❌ $label"
        FAIL=$((FAIL + 1))
    fi
}

# --- Section 1: Container Status ---
echo "1. Container Status"
if [ "${LOCAL:-}" = "true" ]; then
    RUNNING=$(docker compose -f docker-compose.demo.yml ps --status running -q 2>/dev/null | wc -l)
else
    RUNNING=$(gcloud compute ssh $VM_NAME --project=$PROJECT_ID --zone=$ZONE --command='
        cd /opt/healthdata/hdim 2>/dev/null && docker compose -f docker-compose.demo.yml ps --status running -q 2>/dev/null | wc -l
    ' 2>/dev/null)
fi
RUNNING="${RUNNING:-0}"
RUNNING=$(echo "$RUNNING" | tr -d '[:space:]')
if [ "$RUNNING" -ge 19 ] 2>/dev/null; then
    check "All containers running ($RUNNING/19)" "pass"
elif [ "$RUNNING" -ge 15 ] 2>/dev/null; then
    check "Most containers running ($RUNNING/19)" "warn"
else
    check "Containers running: $RUNNING/19" "fail"
fi
echo ""

# --- Section 2: Infrastructure Health ---
echo "2. Infrastructure Health"

# PostgreSQL
if curl -sf "${BASE_URL}:8084/patient/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
    check "PostgreSQL (via patient-service health)" "pass"
else
    check "PostgreSQL (via patient-service health)" "fail"
fi

# Redis
if curl -sf "${BASE_URL}:8088/actuator/health" 2>/dev/null | grep -q '"status"'; then
    check "Redis (via audit-query-service health)" "pass"
else
    check "Redis (via audit-query-service health)" "warn"
fi

# Jaeger
if curl -sf "${BASE_URL}:16686/" > /dev/null 2>&1; then
    check "Jaeger UI accessible" "pass"
else
    check "Jaeger UI accessible" "fail"
fi
echo ""

# --- Section 3: Service Health Endpoints ---
echo "3. Service Health Endpoints"

declare -A SERVICES
SERVICES=(
    ["FHIR Service"]="8085/fhir/actuator/health"
    ["Patient Service"]="8084/patient/actuator/health"
    ["Care Gap Service"]="8086/care-gap/actuator/health"
    ["CQL Engine"]="8081/cql-engine/actuator/health"
    ["Quality Measure"]="8087/quality-measure/actuator/health"
    ["Event Processing"]="8083/events/actuator/health"
    ["Audit Query"]="8088/actuator/health"
    ["HCC Service"]="8105/hcc/actuator/health"
)

for svc in "${!SERVICES[@]}"; do
    endpoint="${SERVICES[$svc]}"
    if curl -sf "${BASE_URL}:${endpoint}" > /dev/null 2>&1; then
        check "$svc" "pass"
    else
        check "$svc" "fail"
    fi
done
echo ""

# --- Section 4: Authentication Flow ---
echo "4. Authentication Flow"

LOGIN_RESPONSE=$(curl -sf -X POST "${BASE_URL}:18080/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"demo_admin","password":"demo123"}' 2>/dev/null)

if echo "$LOGIN_RESPONSE" | grep -q "token"; then
    check "Login with demo_admin credentials" "pass"

    # Extract token
    TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | head -1 | cut -d'"' -f4)

    if [ -n "$TOKEN" ]; then
        check "JWT token received" "pass"

        # Authenticated request
        AUTH_RESPONSE=$(curl -sf "${BASE_URL}:18080/patient/api/v1/patients" \
            -H "Authorization: Bearer $TOKEN" \
            -H "X-Tenant-ID: demo-tenant" 2>/dev/null)

        if [ -n "$AUTH_RESPONSE" ]; then
            check "Authenticated API request succeeds" "pass"
        else
            check "Authenticated API request succeeds" "fail"
        fi
    else
        check "JWT token received" "fail"
        check "Authenticated API request succeeds" "fail"
    fi
else
    check "Login with demo_admin credentials" "fail"
    check "JWT token received" "fail"
    check "Authenticated API request succeeds" "fail"
fi
echo ""

# --- Section 5: FHIR Metadata ---
echo "5. FHIR Conformance"

FHIR_META=$(curl -sf "${BASE_URL}:8085/fhir/metadata" 2>/dev/null)
if echo "$FHIR_META" | grep -q "4.0.1"; then
    check "FHIR R4 metadata (version 4.0.1)" "pass"
elif [ -n "$FHIR_META" ]; then
    check "FHIR metadata returned (version check failed)" "warn"
else
    check "FHIR R4 metadata" "fail"
fi
echo ""

# --- Section 6: Demo Data Presence ---
echo "6. Demo Data Presence"

if [ -n "$TOKEN" ]; then
    # Patients
    PATIENT_DATA=$(curl -sf "${BASE_URL}:18080/patient/api/v1/patients" \
        -H "Authorization: Bearer $TOKEN" \
        -H "X-Tenant-ID: demo-tenant" 2>/dev/null)

    if echo "$PATIENT_DATA" | grep -q "patient\|Patient\|content\|id"; then
        check "Demo patients present" "pass"
    else
        check "Demo patients present" "warn"
    fi

    # Care Gaps
    CAREGAP_DATA=$(curl -sf "${BASE_URL}:18080/care-gap/api/v1/care-gaps" \
        -H "Authorization: Bearer $TOKEN" \
        -H "X-Tenant-ID: demo-tenant" 2>/dev/null)

    if echo "$CAREGAP_DATA" | grep -q "careGap\|CareGap\|content\|id"; then
        check "Demo care gaps present" "pass"
    else
        check "Demo care gaps present" "warn"
    fi
else
    check "Demo patients present (no auth token)" "fail"
    check "Demo care gaps present (no auth token)" "fail"
fi
echo ""

# --- Section 7: Jaeger Traces ---
echo "7. Distributed Tracing"

JAEGER_SERVICES=$(curl -sf "${BASE_URL}:16686/api/services" 2>/dev/null)
if echo "$JAEGER_SERVICES" | grep -q "data"; then
    check "Jaeger service discovery" "pass"
else
    check "Jaeger service discovery" "fail"
fi
echo ""

# --- Section 8: Clinical Portal ---
echo "8. Clinical Portal"

PORTAL_HTML=$(curl -sf "${BASE_URL}:4200/" 2>/dev/null)
if echo "$PORTAL_HTML" | grep -q "<html\|<app-root\|angular"; then
    check "Clinical Portal serves HTML" "pass"
else
    check "Clinical Portal serves HTML" "fail"
fi
echo ""

# --- Summary ---
TOTAL=$((PASS + FAIL + WARN))
echo "================================================"
echo "  Validation Summary"
echo "================================================"
echo ""
echo "  ✅ Passed:   $PASS/$TOTAL"
echo "  ⚠️  Warnings: $WARN/$TOTAL"
echo "  ❌ Failed:   $FAIL/$TOTAL"
echo ""

if [ $FAIL -eq 0 ] && [ $WARN -eq 0 ]; then
    echo "  🎉 All checks passed! Demo is fully operational."
elif [ $FAIL -eq 0 ]; then
    echo "  👍 No failures. Some warnings to investigate."
else
    echo "  ⚠️  $FAIL check(s) failed. Review output above."
fi

echo ""
echo "🔑 Demo Credentials:"
echo "   Username: demo_admin@hdim.ai"
echo "   Password: demo123"
echo ""

exit $FAIL
