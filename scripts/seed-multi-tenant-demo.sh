#!/bin/bash

set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
DEMO_SERVICE_URL="${DEMO_SERVICE_URL:-http://localhost:8091}"
TENANT_ID="${TENANT_ID:-demo-admin}"
USE_TRUSTED_HEADERS="${USE_TRUSTED_HEADERS:-true}"
AUTH_USER_ID="${AUTH_USER_ID:-550e8400-e29b-41d4-a716-446655440010}"
AUTH_USERNAME="${AUTH_USERNAME:-demo_admin@hdim.ai}"
AUTH_ROLES="${AUTH_ROLES:-ADMIN,EVALUATOR}"

AUTH_HEADER=()
if [ "$USE_TRUSTED_HEADERS" = "true" ]; then
  VALIDATED_TS=$(date +%s)
  AUTH_HEADER=(
    -H "X-Auth-User-Id: $AUTH_USER_ID"
    -H "X-Auth-Username: $AUTH_USERNAME"
    -H "X-Auth-Roles: $AUTH_ROLES"
    -H "X-Auth-Tenant-Ids: $TENANT_ID"
    -H "X-Auth-Validated: gateway-${VALIDATED_TS}-dev"
  )
else
  API_TOKEN="${API_TOKEN:-}"
  if [ -z "$API_TOKEN" ]; then
    LOGIN_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"username\":\"${AUTH_USERNAME}\",\"password\":\"${AUTH_PASSWORD:-demo123}\"}")
    API_TOKEN=$(echo "$LOGIN_RESPONSE" | python3 -c "import json,sys; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
  fi
  if [ -n "$API_TOKEN" ]; then
    AUTH_HEADER=(-H "Authorization: Bearer $API_TOKEN")
  fi
fi

echo "Seeding multi-tenant demo via $DEMO_SERVICE_URL"
echo "Scenario: multi-tenant"
echo "Patients per tenant (config): ${DEMO_MULTI_TENANT_PATIENTS_PER_TENANT:-200}"
echo "Care gap percentage (config): ${DEMO_MULTI_TENANT_CARE_GAP_PERCENTAGE:-30}"

PAYLOAD="{}"
if [ -n "${DEMO_MULTI_TENANT_PATIENTS_PER_TENANT:-}" ] || [ -n "${DEMO_MULTI_TENANT_CARE_GAP_PERCENTAGE:-}" ]; then
  PAYLOAD="{"
  COMMA=""
  if [ -n "${DEMO_MULTI_TENANT_PATIENTS_PER_TENANT:-}" ]; then
    PAYLOAD="${PAYLOAD}\"patientsPerTenant\": ${DEMO_MULTI_TENANT_PATIENTS_PER_TENANT}"
    COMMA=", "
  fi
  if [ -n "${DEMO_MULTI_TENANT_CARE_GAP_PERCENTAGE:-}" ]; then
    PAYLOAD="${PAYLOAD}${COMMA}\"careGapPercentage\": ${DEMO_MULTI_TENANT_CARE_GAP_PERCENTAGE}"
  fi
  PAYLOAD="${PAYLOAD}}"
fi

curl -s -X POST "$DEMO_SERVICE_URL/api/v1/demo/scenarios/multi-tenant" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: $TENANT_ID" \
  "${AUTH_HEADER[@]}" \
  -d "$PAYLOAD" \
  | tee /tmp/seed-multi-tenant-demo.json

echo ""
echo "Response saved to /tmp/seed-multi-tenant-demo.json"
