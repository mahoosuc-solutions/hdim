#!/usr/bin/env bash

set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://127.0.0.1:18080}"
TENANT_ID="${TENANT_ID:-acme-health}"
AUTH_TOKEN="${AUTH_TOKEN:-wave1-smoke-token}"
TEST_USERNAME="${TEST_USERNAME:-test_admin}"
TEST_PASSWORD="${TEST_PASSWORD:-password123}"
OUTPUT_DIR="${OUTPUT_DIR:-test-results}"

mkdir -p "$OUTPUT_DIR"

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
output_file="${OUTPUT_DIR}/wave1-edge-gateway-smoke-${timestamp}.json"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

declare -a results=()

pass() {
  local name="$1"
  local details="$2"
  results+=("{\"name\":\"${name}\",\"status\":\"PASS\",\"details\":\"${details}\"}")
  echo "PASS: ${name} - ${details}"
}

fail() {
  local name="$1"
  local details="$2"
  results+=("{\"name\":\"${name}\",\"status\":\"FAIL\",\"details\":\"${details}\"}")
  echo "FAIL: ${name} - ${details}"
}

request_json() {
  local method="$1"
  local path="$2"
  local payload="${3:-}"
  local out_file="$4"

  if [[ -n "$payload" ]]; then
    curl -sS --retry 4 --retry-delay 1 --retry-connrefused -X "$method" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer ${AUTH_TOKEN}" \
      -H "X-Tenant-ID: ${TENANT_ID}" \
      -d "$payload" \
      -o "$out_file" \
      -w "%{http_code}" \
      "${GATEWAY_URL}${path}" || echo "000"
  else
    curl -sS --retry 4 --retry-delay 1 --retry-connrefused -X "$method" \
      -H "Authorization: Bearer ${AUTH_TOKEN}" \
      -H "X-Tenant-ID: ${TENANT_ID}" \
      -o "$out_file" \
      -w "%{http_code}" \
      "${GATEWAY_URL}${path}" || echo "000"
  fi
}

login_payload=$(cat <<JSON
{"username":"${TEST_USERNAME}","password":"${TEST_PASSWORD}"}
JSON
)
login_response="$tmp_dir/login.json"
login_code=$(curl -sS --retry 4 --retry-delay 1 --retry-connrefused -X POST \
  -H "Content-Type: application/json" \
  -d "$login_payload" \
  -o "$login_response" \
  -w "%{http_code}" \
  "${GATEWAY_URL}/api/v1/auth/login" || echo "000")
if [[ "$login_code" == "200" ]]; then
  token_from_login="$(jq -r '.accessToken // empty' "$login_response" 2>/dev/null || true)"
  if [[ -n "$token_from_login" ]]; then
    AUTH_TOKEN="$token_from_login"
    pass "gateway.auth_login" "200 token acquired for ${TEST_USERNAME}"
  else
    fail "gateway.auth_login" "200 but accessToken missing"
  fi
else
  fail "gateway.auth_login" "status=${login_code}"
fi

claim_id="WAVE1-CLAIM-${timestamp}"
claim_corr="WAVE1-CORR-CLAIM-${timestamp}"
adt_source_message="WAVE1-SRC-${timestamp}"
adt_corr="WAVE1-CORR-ADT-${timestamp}"
event_id=""

claim_submit_payload=$(cat <<JSON
{"tenantId":"${TENANT_ID}","claimId":"${claim_id}","patientId":"PATIENT-001","payerId":"PAYER-001","totalAmount":125.00,"idempotencyKey":"IDEMP-${timestamp}","correlationId":"${claim_corr}","actor":"wave1-smoke"}
JSON
)
status_code=$(request_json "POST" "/api/v1/revenue/claims/submissions" "$claim_submit_payload" "$tmp_dir/claim_submit.json")
if [[ "$status_code" == "200" ]] && jq -e ".claimId == \"${claim_id}\"" "$tmp_dir/claim_submit.json" >/dev/null 2>&1; then
  pass "revenue.claim_submission" "200 with claimId=${claim_id}"
else
  fail "revenue.claim_submission" "status=${status_code}"
fi

claim_status_payload=$(cat <<JSON
{"tenantId":"${TENANT_ID}","claimId":"${claim_id}","correlationId":"${claim_corr}","actor":"wave1-smoke"}
JSON
)
status_code=$(request_json "POST" "/api/v1/revenue/claim-status/checks" "$claim_status_payload" "$tmp_dir/claim_status.json")
if [[ "$status_code" == "200" ]] && jq -e '.status != null' "$tmp_dir/claim_status.json" >/dev/null 2>&1; then
  pass "revenue.claim_status" "200 with status=$(jq -r '.status' "$tmp_dir/claim_status.json" 2>/dev/null || echo unknown)"
else
  fail "revenue.claim_status" "status=${status_code}"
fi

remit_payload=$(cat <<JSON
{"tenantId":"${TENANT_ID}","claimId":"${claim_id}","remittanceId":"REMIT-${timestamp}","paymentAmount":100.00,"adjustmentAmount":25.00,"correlationId":"${claim_corr}","actor":"wave1-smoke"}
JSON
)
status_code=$(request_json "POST" "/api/v1/revenue/remittance/advice" "$remit_payload" "$tmp_dir/remit.json")
if [[ "$status_code" == "200" ]] && jq -e '.newStatus != null' "$tmp_dir/remit.json" >/dev/null 2>&1; then
  pass "revenue.remittance_advice" "200 with newStatus=$(jq -r '.newStatus' "$tmp_dir/remit.json" 2>/dev/null || echo unknown)"
else
  fail "revenue.remittance_advice" "status=${status_code}"
fi

adt_ingest_payload=$(cat <<JSON
{"tenantId":"${TENANT_ID}","sourceSystem":"hie-main","sourceMessageId":"${adt_source_message}","eventType":"A01","patientExternalId":"PAT-EXT-001","encounterExternalId":"ENC-EXT-001","payloadHash":"sha256:wave1-smoke","correlationId":"${adt_corr}"}
JSON
)
status_code=$(request_json "POST" "/api/v1/interoperability/adt/messages" "$adt_ingest_payload" "$tmp_dir/adt_ingest.json")
if [[ "$status_code" == "200" ]] && jq -e '.eventId != null' "$tmp_dir/adt_ingest.json" >/dev/null 2>&1; then
  event_id="$(jq -r '.eventId' "$tmp_dir/adt_ingest.json")"
  pass "adt.ingest_message" "200 with eventId=${event_id}"
else
  fail "adt.ingest_message" "status=${status_code}"
fi

if [[ -n "$event_id" ]]; then
  adt_ack_payload=$(cat <<JSON
{"tenantId":"${TENANT_ID}","eventId":"${event_id}","sourceSystem":"hie-main","correlationId":"${adt_corr}"}
JSON
)
  status_code=$(request_json "POST" "/api/v1/interoperability/adt/acks" "$adt_ack_payload" "$tmp_dir/adt_ack.json")
  if [[ "$status_code" == "200" ]] && jq -e '.state != null' "$tmp_dir/adt_ack.json" >/dev/null 2>&1; then
    pass "adt.acknowledge" "200 with state=$(jq -r '.state' "$tmp_dir/adt_ack.json" 2>/dev/null || echo unknown)"
  else
    fail "adt.acknowledge" "status=${status_code}"
  fi

  status_code=$(request_json "GET" "/api/v1/interoperability/adt/events/${event_id}" "" "$tmp_dir/adt_event.json")
  if [[ "$status_code" == "200" ]] && jq -e ".eventId == \"${event_id}\"" "$tmp_dir/adt_event.json" >/dev/null 2>&1; then
    pass "adt.get_event" "200 with eventId=${event_id}"
  else
    fail "adt.get_event" "status=${status_code}"
  fi
else
  fail "adt.acknowledge" "skipped because ingest_message failed"
  fail "adt.get_event" "skipped because ingest_message failed"
fi

passed=0
failed=0
for item in "${results[@]}"; do
  if [[ "$item" == *"\"status\":\"PASS\""* ]]; then
    passed=$((passed + 1))
  else
    failed=$((failed + 1))
  fi
done

jq -n \
  --arg timestamp "$timestamp" \
  --arg gatewayUrl "$GATEWAY_URL" \
  --arg tenantId "$TENANT_ID" \
  --argjson passed "$passed" \
  --argjson failed "$failed" \
  --argjson checks "$(printf '%s\n' "${results[@]}" | jq -s '.')" \
  '{
    timestamp: $timestamp,
    gatewayUrl: $gatewayUrl,
    tenantId: $tenantId,
    summary: {
      totalChecks: ($passed + $failed),
      passed: $passed,
      failed: $failed,
      passRate: (if ($passed + $failed) == 0 then 0 else (($passed / ($passed + $failed)) * 100) end)
    },
    checks: $checks
  }' > "$output_file"

echo "Wave-1 edge gateway smoke report: $output_file"

if [[ "$failed" -gt 0 ]]; then
  exit 1
fi
