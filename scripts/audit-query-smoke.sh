#!/bin/bash
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
AUDIT_DIRECT_URL="${AUDIT_DIRECT_URL:-http://localhost:8088/api/v1/audit/logs/statistics}"
AUDIT_GATEWAY_URL="${AUDIT_GATEWAY_URL:-${GATEWAY_URL}/api/v1/audit/logs/statistics}"
TENANT_ID="${TENANT_ID:-acme-health}"
AUTH_USERNAME="${AUTH_USERNAME:-demo_admin@hdim.ai}"
AUTH_PASSWORD="${AUTH_PASSWORD:-demo123}"
AUTH_USER_ID="${AUTH_USER_ID:-550e8400-e29b-41d4-a716-446655440010}"
AUTH_ROLES="${AUTH_ROLES:-ADMIN,EVALUATOR}"

echo "Audit Query Smoke Test"
echo "Tenant: ${TENANT_ID}"

AUTH_TOKEN=$(curl -s -X POST "${GATEWAY_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${AUTH_USERNAME}\",\"password\":\"${AUTH_PASSWORD}\"}" | \
  python3 -c "import json,sys; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null || true)

AUTH_HEADER=()
if [ -n "$AUTH_TOKEN" ] && [ "$AUTH_TOKEN" != "null" ]; then
  AUTH_HEADER=(-H "Authorization: Bearer $AUTH_TOKEN")
fi

XAUTH_HEADERS=(
  -H "X-Auth-User-Id: ${AUTH_USER_ID}"
  -H "X-Auth-Username: ${AUTH_USERNAME}"
  -H "X-Auth-Roles: ${AUTH_ROLES}"
  -H "X-Auth-Tenant-Ids: ${TENANT_ID}"
  -H "X-Auth-Validated: gateway-dev"
)

DIRECT_STATUS=$(curl -s -o /dev/null -w '%{http_code}' \
  -H "X-Tenant-ID: ${TENANT_ID}" "${XAUTH_HEADERS[@]}" \
  "${AUDIT_DIRECT_URL}")
echo "Direct stats:  ${DIRECT_STATUS}"

GATEWAY_STATUS=$(curl -s -o /dev/null -w '%{http_code}' \
  -H "X-Tenant-ID: ${TENANT_ID}" "${AUTH_HEADER[@]}" \
  "${AUDIT_GATEWAY_URL}")
echo "Gateway stats: ${GATEWAY_STATUS}"

if [ "$DIRECT_STATUS" = "200" ] && [ "$GATEWAY_STATUS" = "200" ]; then
  echo "OK"
  exit 0
fi

echo "FAILED"
exit 1
