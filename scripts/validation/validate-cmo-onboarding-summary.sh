#!/usr/bin/env bash

set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
TENANT_ID="${TENANT_ID:-acme-health}"
AUTH_USERNAME="${AUTH_USERNAME:-test_admin}"
AUTH_PASSWORD="${AUTH_PASSWORD:-password123}"
TIMEOUT="${TIMEOUT:-20}"
ENDPOINT="${ENDPOINT:-/api/executive/cmo-onboarding/summary}"

pass_count=0
fail_count=0

pass() {
  pass_count=$((pass_count + 1))
  echo "PASS: $1"
}

fail() {
  fail_count=$((fail_count + 1))
  echo "FAIL: $1"
}

http_code() {
  local url="$1"
  shift
  curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 -o /tmp/cmo-onboarding-validation-body.$$ -w '%{http_code}' "$url" "$@"
}

login_response="$(curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 \
  -X POST "${GATEWAY_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: ${TENANT_ID}" \
  -d "{\"username\":\"${AUTH_USERNAME}\",\"password\":\"${AUTH_PASSWORD}\"}" \
  -w $'\n%{http_code}')"
login_status="$(printf '%s' "${login_response}" | tail -n1)"
login_body="$(printf '%s' "${login_response}" | sed '$d')"
auth_token="$(printf '%s' "${login_body}" | jq -r '.accessToken // empty' 2>/dev/null || true)"

if [[ "${login_status}" == "200" && -n "${auth_token}" ]]; then
  pass "Auth token acquired"
else
  fail "Failed auth login for validation (status=${login_status})"
  echo "Summary: pass=${pass_count} fail=${fail_count}"
  exit 1
fi

payload_file="/tmp/cmo-onboarding-summary.$$.json"
status_ok="$(curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 \
  -o "${payload_file}" -w '%{http_code}' \
  -H "Authorization: Bearer ${auth_token}" \
  -H "X-Tenant-ID: ${TENANT_ID}" \
  "${GATEWAY_URL}${ENDPOINT}")"

if [[ "${status_ok}" == "200" ]]; then
  pass "Summary endpoint returns 200 with auth and tenant"
else
  fail "Summary endpoint expected 200, got ${status_ok}"
fi

tenant_value="$(jq -r '.tenantId // empty' "${payload_file}" 2>/dev/null || true)"
kpi_count="$(jq -r '.kpis | length' "${payload_file}" 2>/dev/null || echo 0)"
actions_count="$(jq -r '.topActions | length' "${payload_file}" 2>/dev/null || echo 0)"
signals_count="$(jq -r '.governanceSignals | length' "${payload_file}" 2>/dev/null || echo 0)"

if [[ "${tenant_value}" == "${TENANT_ID}" ]]; then
  pass "tenantId is propagated (${tenant_value})"
else
  fail "tenantId mismatch expected=${TENANT_ID} actual=${tenant_value:-missing}"
fi

if (( kpi_count >= 1 )); then
  pass "kpis array present with ${kpi_count} item(s)"
else
  fail "kpis array missing or empty"
fi

if (( actions_count >= 1 )); then
  pass "topActions array present with ${actions_count} item(s)"
else
  fail "topActions array missing or empty"
fi

if (( signals_count >= 1 )); then
  pass "governanceSignals array present with ${signals_count} item(s)"
else
  fail "governanceSignals array missing or empty"
fi

first_kpi_has_fields="$(jq -r '
  if (.kpis[0].label? and .kpis[0].value? and .kpis[0].trend? and .kpis[0].status?) then "yes" else "no" end
' "${payload_file}" 2>/dev/null || echo "no")"
if [[ "${first_kpi_has_fields}" == "yes" ]]; then
  pass "First KPI includes label/value/trend/status"
else
  fail "First KPI missing one or more required fields"
fi

status_missing_auth="$(http_code "${GATEWAY_URL}${ENDPOINT}" -H "X-Tenant-ID: ${TENANT_ID}")"
if [[ "${status_missing_auth}" == "401" || "${status_missing_auth}" == "403" ]]; then
  pass "Missing auth rejected (${status_missing_auth})"
else
  fail "Missing auth expected 401/403, got ${status_missing_auth}"
fi

status_no_tenant="$(curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 \
  -o /tmp/cmo-onboarding-no-tenant.$$.json -w '%{http_code}' \
  -H "Authorization: Bearer ${auth_token}" \
  "${GATEWAY_URL}${ENDPOINT}")"
if [[ "${status_no_tenant}" == "200" ]]; then
  fallback_tenant="$(jq -r '.tenantId // empty' /tmp/cmo-onboarding-no-tenant.$$.json 2>/dev/null || true)"
  if [[ "${fallback_tenant}" == "acme-health" ]]; then
    pass "No-tenant request resolves to default tenant (acme-health)"
  else
    fail "No-tenant request returned 200 but default tenant missing (${fallback_tenant:-missing})"
  fi
else
  fail "No-tenant request expected 200 with defaulting, got ${status_no_tenant}"
fi

echo "Summary: pass=${pass_count} fail=${fail_count}"
if (( fail_count > 0 )); then
  exit 1
fi

exit 0
