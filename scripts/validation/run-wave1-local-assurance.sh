#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

OUTPUT_DIR="${OUTPUT_DIR:-test-results}"
TENANT_ID="${TENANT_ID:-acme-health}"
TEST_USERNAME="${TEST_USERNAME:-test_admin}"
TEST_PASSWORD="${TEST_PASSWORD:-password123}"
IN_NETWORK_GATEWAY_URL="${IN_NETWORK_GATEWAY_URL:-http://gateway-edge:8080}"

START_STACK="${START_STACK:-true}"
STOP_STACK="${STOP_STACK:-true}"
BUILD_WAVE1_IMAGES="${BUILD_WAVE1_IMAGES:-false}"
INGEST_IMAGE="${INGEST_IMAGE:-hdim-data-ingestion-service:wave1-local}"

mkdir -p "$OUTPUT_DIR"

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
report_file="${OUTPUT_DIR}/wave1-local-assurance-${timestamp}.json"
baseline_artifact=""

cleanup() {
  if [[ "$STOP_STACK" == "true" ]]; then
    ./scripts/validation/stop-wave1-validation-stack.sh >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

declare -a results=()

pass() {
  local name="$1"
  local details="$2"
  details="${details//\"/\'}"
  results+=("{\"name\":\"${name}\",\"status\":\"PASS\",\"details\":\"${details}\"}")
  echo "PASS: ${name} - ${details}"
}

fail() {
  local name="$1"
  local details="$2"
  details="${details//\"/\'}"
  results+=("{\"name\":\"${name}\",\"status\":\"FAIL\",\"details\":\"${details}\"}")
  echo "FAIL: ${name} - ${details}"
}

run_in_ops() {
  local cmd="$1"
  docker exec hdim-demo-ops sh -lc "cd /workspace && ${cmd}"
}

expect_code() {
  local name="$1"
  local code="$2"
  local expected_csv="$3"
  if [[ ",${expected_csv}," == *",${code},"* ]]; then
    pass "$name" "status=${code} expected=${expected_csv}"
  else
    fail "$name" "status=${code} expected=${expected_csv}"
  fi
}

if [[ "$START_STACK" == "true" ]]; then
  BUILD_WAVE1_IMAGES="$BUILD_WAVE1_IMAGES" INGEST_IMAGE="$INGEST_IMAGE" ./scripts/validation/start-wave1-validation-stack.sh
fi

login_json="$(run_in_ops "curl -sS -X POST -H 'Content-Type: application/json' -d '{\"username\":\"${TEST_USERNAME}\",\"password\":\"${TEST_PASSWORD}\"}' ${IN_NETWORK_GATEWAY_URL}/api/v1/auth/login")"
auth_token="$(printf '%s' "$login_json" | jq -r '.accessToken // empty')"
if [[ -n "$auth_token" ]]; then
  pass "auth.token_acquired" "token for ${TEST_USERNAME}"
else
  fail "auth.token_acquired" "accessToken missing"
fi

echo "Running baseline Wave-1 smoke in demo network"
set +e
run_in_ops "GATEWAY_URL=${IN_NETWORK_GATEWAY_URL} TENANT_ID=${TENANT_ID} OUTPUT_DIR=${OUTPUT_DIR} ./scripts/validation/validate-wave1-edge-gateway-flow.sh"
baseline_rc=$?
set -e
baseline_artifact="$(ls -1t ${OUTPUT_DIR}/wave1-edge-gateway-smoke-*.json 2>/dev/null | head -n 1 || true)"
if [[ "$baseline_rc" -eq 0 ]]; then
  pass "baseline.wave1_smoke" "pass artifact=${baseline_artifact:-missing}"
else
  fail "baseline.wave1_smoke" "rc=${baseline_rc} artifact=${baseline_artifact:-missing}"
fi

claim_id="ASSURE-CLAIM-${timestamp}"
claim_payload="{\"tenantId\":\"${TENANT_ID}\",\"claimId\":\"${claim_id}\",\"patientId\":\"PATIENT-001\",\"payerId\":\"PAYER-001\",\"totalAmount\":125.00,\"idempotencyKey\":\"ASSURE-IDEMP-${timestamp}\",\"correlationId\":\"ASSURE-CORR-${timestamp}\",\"actor\":\"assurance\"}"

code_no_auth="$(run_in_ops "curl -sS -o /tmp/rev_no_auth.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'X-Tenant-ID: ${TENANT_ID}' -d '${claim_payload}' ${IN_NETWORK_GATEWAY_URL}/api/v1/revenue/claims/submissions")"
expect_code "revenue.reject_missing_auth" "$code_no_auth" "401"

code_no_tenant="$(run_in_ops "curl -sS -o /tmp/rev_no_tenant.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -d '${claim_payload}' ${IN_NETWORK_GATEWAY_URL}/api/v1/revenue/claims/submissions")"
expect_code "revenue.reject_missing_tenant" "$code_no_tenant" "400"

code_bad_json="$(run_in_ops "curl -sS -o /tmp/rev_bad_json.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' -d '{' ${IN_NETWORK_GATEWAY_URL}/api/v1/revenue/claims/submissions")"
expect_code "revenue.reject_malformed_payload" "$code_bad_json" "400,422"

idem_claim_id="ASSURE-IDEM-CLAIM-${timestamp}"
idem_payload="{\"tenantId\":\"${TENANT_ID}\",\"claimId\":\"${idem_claim_id}\",\"patientId\":\"PATIENT-001\",\"payerId\":\"PAYER-001\",\"totalAmount\":125.00,\"idempotencyKey\":\"ASSURE-IDEMP-REPLAY-${timestamp}\",\"correlationId\":\"ASSURE-IDEMP-CORR-${timestamp}\",\"actor\":\"assurance\"}"

code_idem_1="$(run_in_ops "curl -sS -o /tmp/rev_idem_1.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' -d '${idem_payload}' ${IN_NETWORK_GATEWAY_URL}/api/v1/revenue/claims/submissions")"
code_idem_2="$(run_in_ops "curl -sS -o /tmp/rev_idem_2.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' -d '${idem_payload}' ${IN_NETWORK_GATEWAY_URL}/api/v1/revenue/claims/submissions")"
claim_1="$(run_in_ops "jq -r '.claimId // empty' /tmp/rev_idem_1.json")"
claim_2="$(run_in_ops "jq -r '.claimId // empty' /tmp/rev_idem_2.json")"
if [[ "$code_idem_1" == "200" && "$code_idem_2" == "200" && -n "$claim_1" && "$claim_1" == "$claim_2" ]]; then
  pass "revenue.idempotency_replay" "codes=200,200 claimId=${claim_1}"
else
  fail "revenue.idempotency_replay" "codes=${code_idem_1},${code_idem_2} claim1=${claim_1:-none} claim2=${claim_2:-none}"
fi

adt_payload="{\"tenantId\":\"${TENANT_ID}\",\"sourceSystem\":\"hie-main\",\"sourceMessageId\":\"ASSURE-SRC-${timestamp}\",\"eventType\":\"A01\",\"patientExternalId\":\"PAT-EXT-001\",\"encounterExternalId\":\"ENC-EXT-001\",\"payloadHash\":\"sha256:assurance\",\"correlationId\":\"ASSURE-ADT-CORR-${timestamp}\"}"

code_adt_no_auth="$(run_in_ops "curl -sS -o /tmp/adt_no_auth.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'X-Tenant-ID: ${TENANT_ID}' -d '${adt_payload}' ${IN_NETWORK_GATEWAY_URL}/api/v1/interoperability/adt/messages")"
expect_code "adt.reject_missing_auth" "$code_adt_no_auth" "401"

code_adt_no_tenant="$(run_in_ops "curl -sS -o /tmp/adt_no_tenant.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -d '${adt_payload}' ${IN_NETWORK_GATEWAY_URL}/api/v1/interoperability/adt/messages")"
expect_code "adt.reject_missing_tenant" "$code_adt_no_tenant" "400"

code_adt_bad_json="$(run_in_ops "curl -sS -o /tmp/adt_bad_json.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' -d '{' ${IN_NETWORK_GATEWAY_URL}/api/v1/interoperability/adt/messages")"
expect_code "adt.reject_malformed_payload" "$code_adt_bad_json" "400,422"

dupe_src="ASSURE-DUPE-SRC-${timestamp}"
dupe_payload="{\"tenantId\":\"${TENANT_ID}\",\"sourceSystem\":\"hie-main\",\"sourceMessageId\":\"${dupe_src}\",\"eventType\":\"A01\",\"patientExternalId\":\"PAT-EXT-001\",\"encounterExternalId\":\"ENC-EXT-001\",\"payloadHash\":\"sha256:assurance-dupe\",\"correlationId\":\"ASSURE-DUPE-CORR-${timestamp}\"}"
code_adt_1="$(run_in_ops "curl -sS -o /tmp/adt_dupe_1.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' -d '${dupe_payload}' ${IN_NETWORK_GATEWAY_URL}/api/v1/interoperability/adt/messages")"
code_adt_2="$(run_in_ops "curl -sS -o /tmp/adt_dupe_2.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' -d '${dupe_payload}' ${IN_NETWORK_GATEWAY_URL}/api/v1/interoperability/adt/messages")"
if [[ "$code_adt_1" == "200" && ( "$code_adt_2" == "200" || "$code_adt_2" == "409" ) ]]; then
  pass "adt.duplicate_source_message_handled" "codes=${code_adt_1},${code_adt_2}"
else
  fail "adt.duplicate_source_message_handled" "codes=${code_adt_1},${code_adt_2}"
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
  --arg tenantId "$TENANT_ID" \
  --arg gatewayUrl "$IN_NETWORK_GATEWAY_URL" \
  --arg baselineArtifact "$baseline_artifact" \
  --argjson passed "$passed" \
  --argjson failed "$failed" \
  --argjson checks "$(printf '%s\n' "${results[@]}" | jq -s '.')" \
  '{
    timestamp: $timestamp,
    tenantId: $tenantId,
    gatewayUrl: $gatewayUrl,
    baselineArtifact: $baselineArtifact,
    summary: {
      totalChecks: ($passed + $failed),
      passed: $passed,
      failed: $failed,
      passRate: (if ($passed + $failed) == 0 then 0 else (($passed / ($passed + $failed)) * 100) end)
    },
    checks: $checks
  }' > "$report_file"

echo "Wave-1 local assurance report: $report_file"

if [[ "$failed" -gt 0 ]]; then
  exit 1
fi
