#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

OUTPUT_DIR="${OUTPUT_DIR:-test-results}"
TENANT_ID="${TENANT_ID:-acme-health}"
TEST_USERNAME="${TEST_USERNAME:-test_admin}"
TEST_PASSWORD="${TEST_PASSWORD:-password123}"
IN_NETWORK_GATEWAY_URL="${IN_NETWORK_GATEWAY_URL:-http://gateway-edge:8080}"
PERF_ENABLED="${PERF_ENABLED:-true}"
PERF_SAMPLES="${PERF_SAMPLES:-20}"
PERF_WARMUP="${PERF_WARMUP:-5}"
PERF_BUDGET_REVENUE_CLAIM_STATUS_P95_MS="${PERF_BUDGET_REVENUE_CLAIM_STATUS_P95_MS:-120}"
PERF_BUDGET_ADT_GET_EVENT_P95_MS="${PERF_BUDGET_ADT_GET_EVENT_P95_MS:-120}"
PERF_BUDGET_PRICE_ESTIMATE_P95_MS="${PERF_BUDGET_PRICE_ESTIMATE_P95_MS:-150}"
PERF_BUDGET_PRICE_ESTIMATE_LOAD_P95_MS="${PERF_BUDGET_PRICE_ESTIMATE_LOAD_P95_MS:-250}"
PERF_TREND_WARN_THRESHOLD_PERCENT="${PERF_TREND_WARN_THRESHOLD_PERCENT:-15}"
PERF_PRICE_ESTIMATE_LOAD_ENABLED="${PERF_PRICE_ESTIMATE_LOAD_ENABLED:-true}"
PERF_PRICE_ESTIMATE_LOAD_SAMPLES="${PERF_PRICE_ESTIMATE_LOAD_SAMPLES:-120}"

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
declare -a perf_results=()
declare -a trend_warnings=()

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

in_expected_codes() {
  local code="$1"
  local expected_csv="$2"
  [[ ",${expected_csv}," == *",${code},"* ]]
}

measure_latency() {
  local name="$1"
  local method="$2"
  local path="$3"
  local payload="${4:-}"
  local expected_csv="${5:-200}"
  local p95_budget_ms="${6:-}"
  local samples="${7:-$PERF_SAMPLES}"

  local -a latencies=()
  local success=0
  local failure=0
  local curl_cmd out code seconds ms
  local i

  for i in $(seq 1 "$PERF_WARMUP"); do
    if [[ -n "$payload" ]]; then
      run_in_ops "curl -sS -o /tmp/perf_warmup_${name}.json -w '%{http_code}' -X ${method} -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' -d '${payload}' ${IN_NETWORK_GATEWAY_URL}${path}" >/dev/null 2>&1 || true
    else
      run_in_ops "curl -sS -o /tmp/perf_warmup_${name}.json -w '%{http_code}' -X ${method} -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' ${IN_NETWORK_GATEWAY_URL}${path}" >/dev/null 2>&1 || true
    fi
  done

  for i in $(seq 1 "$samples"); do
    if [[ -n "$payload" ]]; then
      curl_cmd="curl -sS -o /tmp/perf_${name}.json -w '%{http_code} %{time_total}' -X ${method} -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' -d '${payload}' ${IN_NETWORK_GATEWAY_URL}${path}"
    else
      curl_cmd="curl -sS -o /tmp/perf_${name}.json -w '%{http_code} %{time_total}' -X ${method} -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' ${IN_NETWORK_GATEWAY_URL}${path}"
    fi
    out="$(run_in_ops "$curl_cmd")"
    code="$(printf '%s' "$out" | awk '{print $1}')"
    seconds="$(printf '%s' "$out" | awk '{print $2}')"
    ms="$(awk -v s="${seconds:-0}" 'BEGIN { printf "%.2f", s * 1000 }')"
    latencies+=("$ms")

    if in_expected_codes "$code" "$expected_csv"; then
      success=$((success + 1))
    else
      failure=$((failure + 1))
    fi
  done

  local min_ms max_ms avg_ms p95_ms n rank
  n="${#latencies[@]}"
  if [[ "$n" -eq 0 ]]; then
    fail "performance.${name}" "no samples collected"
    perf_results+=("{\"name\":\"${name}\",\"status\":\"FAIL\",\"samples\":0,\"success\":0,\"failure\":0,\"expectedCodes\":\"${expected_csv}\"}")
    return
  fi

  min_ms="$(printf '%s\n' "${latencies[@]}" | sort -n | head -n 1)"
  max_ms="$(printf '%s\n' "${latencies[@]}" | sort -n | tail -n 1)"
  avg_ms="$(printf '%s\n' "${latencies[@]}" | awk '{sum+=$1} END {if (NR==0) print "0.00"; else printf "%.2f", sum/NR}')"
  rank=$(( (95 * n + 99) / 100 ))
  if [[ "$rank" -lt 1 ]]; then
    rank=1
  fi
  p95_ms="$(printf '%s\n' "${latencies[@]}" | sort -n | sed -n "${rank}p")"

  local budget_ok=1
  if [[ -n "$p95_budget_ms" ]]; then
    if ! awk -v p95="$p95_ms" -v budget="$p95_budget_ms" 'BEGIN { exit !(p95 <= budget) }'; then
      budget_ok=0
    fi
  fi

  if [[ "$failure" -eq 0 && "$budget_ok" -eq 1 ]]; then
    pass "performance.${name}" "samples=${n} p95Ms=${p95_ms} avgMs=${avg_ms} minMs=${min_ms} maxMs=${max_ms} budgetP95Ms=${p95_budget_ms:-none}"
    perf_results+=("{\"name\":\"${name}\",\"status\":\"PASS\",\"samples\":${n},\"success\":${success},\"failure\":${failure},\"expectedCodes\":\"${expected_csv}\",\"p95Ms\":${p95_ms},\"avgMs\":${avg_ms},\"minMs\":${min_ms},\"maxMs\":${max_ms},\"budgetP95Ms\":${p95_budget_ms:-null}}")
  else
    fail "performance.${name}" "samples=${n} failures=${failure} expected=${expected_csv} p95Ms=${p95_ms} budgetP95Ms=${p95_budget_ms:-none}"
    perf_results+=("{\"name\":\"${name}\",\"status\":\"FAIL\",\"samples\":${n},\"success\":${success},\"failure\":${failure},\"expectedCodes\":\"${expected_csv}\",\"p95Ms\":${p95_ms},\"avgMs\":${avg_ms},\"minMs\":${min_ms},\"maxMs\":${max_ms},\"budgetP95Ms\":${p95_budget_ms:-null}}")
  fi
}

preflight_non_404() {
  local name="$1"
  local method="$2"
  local path="$3"
  local payload="${4:-}"

  local code
  if [[ -n "$payload" ]]; then
    code="$(run_in_ops "curl -sS -o /tmp/preflight_${name}.json -w '%{http_code}' -X ${method} -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' -d '${payload}' ${IN_NETWORK_GATEWAY_URL}${path}")"
  else
    code="$(run_in_ops "curl -sS -o /tmp/preflight_${name}.json -w '%{http_code}' -X ${method} -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' ${IN_NETWORK_GATEWAY_URL}${path}")"
  fi

  if [[ "$code" == "404" ]]; then
    fail "preflight.${name}" "status=404 route unavailable path=${path}"
  else
    pass "preflight.${name}" "status=${code} route available path=${path}"
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

# Detect missing gateway/backend route wiring before full assurance run.
preflight_non_404 "revenue_claim_submission_route" "POST" "/api/v1/revenue/claims/submissions" "{"
preflight_non_404 "adt_ingest_route" "POST" "/api/v1/interoperability/adt/messages" "{"
preflight_non_404 "price_publish_route" "POST" "/api/v1/revenue/price-transparency/rates/publish" "{"
preflight_non_404 "price_estimate_route" "POST" "/api/v1/revenue/price-transparency/estimates" "{"
preflight_non_404 "cmo_onboarding_summary_route" "GET" "/api/executive/cmo-onboarding/summary"

echo "Running CMO onboarding summary contract validation"
set +e
run_in_ops "GATEWAY_URL=${IN_NETWORK_GATEWAY_URL} TENANT_ID=${TENANT_ID} AUTH_USERNAME=${TEST_USERNAME} AUTH_PASSWORD=${TEST_PASSWORD} bash ./scripts/validation/validate-cmo-onboarding-summary.sh"
cmo_validation_rc=$?
set -e
if [[ "$cmo_validation_rc" -eq 0 ]]; then
  pass "cmo_onboarding.summary_contract" "validation script passed"
else
  fail "cmo_onboarding.summary_contract" "validation script rc=${cmo_validation_rc}"
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

baseline_claim_id="$(jq -r '.checks[] | select(.name=="revenue.claim_submission") | .details' "${baseline_artifact:-/dev/null}" 2>/dev/null | sed -n 's/.*claimId=\([^ ]*\).*/\1/p')"
baseline_event_id="$(jq -r '.checks[] | select(.name=="adt.ingest_message") | .details' "${baseline_artifact:-/dev/null}" 2>/dev/null | sed -n 's/.*eventId=\([^ ]*\).*/\1/p')"

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

price_publish_payload="{\"tenantId\":\"${TENANT_ID}\",\"sourceReference\":\"cms-rate-file-${timestamp}\",\"correlationId\":\"ASSURE-PRICE-PUBLISH-${timestamp}\",\"actor\":\"assurance\",\"rates\":[{\"serviceCode\":\"SVC-99213\",\"negotiatedRate\":75.00,\"cashPrice\":95.00},{\"serviceCode\":\"SVC-93000\",\"negotiatedRate\":40.00,\"cashPrice\":60.00}]}"
price_publish_code="$(run_in_ops "curl -sS -o /tmp/price_publish.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' -d '${price_publish_payload}' ${IN_NETWORK_GATEWAY_URL}/api/v1/revenue/price-transparency/rates/publish")"
if [[ "$price_publish_code" == "200" ]]; then
  price_version_id="$(run_in_ops "jq -r '.versionId // empty' /tmp/price_publish.json")"
  if [[ -n "$price_version_id" ]]; then
    pass "price.publish_rates" "status=200 versionId=${price_version_id}"
  else
    fail "price.publish_rates" "status=200 but versionId missing"
  fi
else
  fail "price.publish_rates" "status=${price_publish_code}"
fi

price_current_code="$(run_in_ops "curl -sS -o /tmp/price_current.json -w '%{http_code}' -X GET -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' '${IN_NETWORK_GATEWAY_URL}/api/v1/revenue/price-transparency/rates/current?tenantId=${TENANT_ID}&correlationId=ASSURE-PRICE-CURRENT-${timestamp}&actor=assurance'")"
if [[ "$price_current_code" == "200" ]]; then
  current_version_id="$(run_in_ops "jq -r '.versionId // empty' /tmp/price_current.json")"
  if [[ -n "${price_version_id:-}" && "$current_version_id" == "$price_version_id" ]]; then
    pass "price.read_current_rates" "status=200 versionId=${current_version_id}"
  else
    fail "price.read_current_rates" "status=200 versionId=${current_version_id:-missing} expected=${price_version_id:-missing}"
  fi
else
  fail "price.read_current_rates" "status=${price_current_code}"
fi

if [[ -n "${price_version_id:-}" ]]; then
  price_version_code="$(run_in_ops "curl -sS -o /tmp/price_version.json -w '%{http_code}' -X GET -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' '${IN_NETWORK_GATEWAY_URL}/api/v1/revenue/price-transparency/rates/${price_version_id}?tenantId=${TENANT_ID}&correlationId=ASSURE-PRICE-VERSION-${timestamp}&actor=assurance'")"
  expect_code "price.read_version_rates" "$price_version_code" "200"
else
  fail "price.read_version_rates" "price_version_id unavailable"
fi

price_estimate_payload="{\"tenantId\":\"${TENANT_ID}\",\"versionId\":\"${price_version_id:-}\",\"serviceCode\":\"SVC-99213\",\"units\":2,\"correlationId\":\"ASSURE-PRICE-EST-${timestamp}\",\"actor\":\"assurance\"}"
price_estimate_code="$(run_in_ops "curl -sS -o /tmp/price_estimate.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' -d '${price_estimate_payload}' ${IN_NETWORK_GATEWAY_URL}/api/v1/revenue/price-transparency/estimates")"
if [[ "$price_estimate_code" == "200" ]]; then
  estimate_allowed="$(run_in_ops "jq -r '.estimatedAllowedAmount // empty' /tmp/price_estimate.json")"
  if [[ "$estimate_allowed" == "150.00" || "$estimate_allowed" == "150" ]]; then
    pass "price.estimate_deterministic" "status=200 estimatedAllowedAmount=${estimate_allowed}"
  else
    fail "price.estimate_deterministic" "status=200 estimatedAllowedAmount=${estimate_allowed:-missing} expected=150.00"
  fi
else
  fail "price.estimate_deterministic" "status=${price_estimate_code}"
fi

price_no_auth_code="$(run_in_ops "curl -sS -o /tmp/price_no_auth.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'X-Tenant-ID: ${TENANT_ID}' -d '${price_publish_payload}' ${IN_NETWORK_GATEWAY_URL}/api/v1/revenue/price-transparency/rates/publish")"
expect_code "price.reject_missing_auth" "$price_no_auth_code" "401"

price_no_tenant_code="$(run_in_ops "curl -sS -o /tmp/price_no_tenant.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -d '${price_publish_payload}' ${IN_NETWORK_GATEWAY_URL}/api/v1/revenue/price-transparency/rates/publish")"
expect_code "price.reject_missing_tenant" "$price_no_tenant_code" "400"

price_bad_json_code="$(run_in_ops "curl -sS -o /tmp/price_bad_json.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' -d '{' ${IN_NETWORK_GATEWAY_URL}/api/v1/revenue/price-transparency/estimates")"
expect_code "price.reject_malformed_payload" "$price_bad_json_code" "400,422"

price_duplicate_payload="{\"tenantId\":\"${TENANT_ID}\",\"sourceReference\":\"cms-rate-file-dupe-${timestamp}\",\"correlationId\":\"ASSURE-PRICE-DUPE-${timestamp}\",\"actor\":\"assurance\",\"rates\":[{\"serviceCode\":\"SVC-99213\",\"negotiatedRate\":75.00,\"cashPrice\":95.00},{\"serviceCode\":\"SVC-99213\",\"negotiatedRate\":80.00,\"cashPrice\":99.00}]}"
price_duplicate_code="$(run_in_ops "curl -sS -o /tmp/price_duplicate.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' -d '${price_duplicate_payload}' ${IN_NETWORK_GATEWAY_URL}/api/v1/revenue/price-transparency/rates/publish")"
expect_code "price.reject_duplicate_service_codes" "$price_duplicate_code" "400,422"

price_missing_version_payload="{\"tenantId\":\"${TENANT_ID}\",\"versionId\":\"PTR-NOT-FOUND\",\"serviceCode\":\"SVC-99213\",\"units\":2,\"correlationId\":\"ASSURE-PRICE-VERSION-MISS-${timestamp}\",\"actor\":\"assurance\"}"
price_missing_version_code="$(run_in_ops "curl -sS -o /tmp/price_missing_version.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' -d '${price_missing_version_payload}' ${IN_NETWORK_GATEWAY_URL}/api/v1/revenue/price-transparency/estimates")"
expect_code "price.reject_missing_version" "$price_missing_version_code" "404"

price_unsupported_service_payload="{\"tenantId\":\"${TENANT_ID}\",\"versionId\":\"${price_version_id:-}\",\"serviceCode\":\"SVC-UNKNOWN\",\"units\":1,\"correlationId\":\"ASSURE-PRICE-UNKNOWN-SVC-${timestamp}\",\"actor\":\"assurance\"}"
price_unsupported_service_code="$(run_in_ops "curl -sS -o /tmp/price_unsupported_service.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H 'Authorization: Bearer ${auth_token}' -H 'X-Tenant-ID: ${TENANT_ID}' -d '${price_unsupported_service_payload}' ${IN_NETWORK_GATEWAY_URL}/api/v1/revenue/price-transparency/estimates")"
if [[ "$price_unsupported_service_code" == "200" ]]; then
  unsupported_error_code="$(run_in_ops "jq -r '.errorCode // empty' /tmp/price_unsupported_service.json")"
  if [[ "$unsupported_error_code" == "VALIDATION_ERROR" ]]; then
    pass "price.reject_unsupported_service_code" "status=200 errorCode=${unsupported_error_code}"
  else
    fail "price.reject_unsupported_service_code" "status=200 errorCode=${unsupported_error_code:-missing} expected=VALIDATION_ERROR"
  fi
else
  fail "price.reject_unsupported_service_code" "status=${price_unsupported_service_code} expected=200"
fi

if [[ "$PERF_ENABLED" == "true" ]]; then
  if [[ -n "${baseline_claim_id:-}" ]]; then
    perf_claim_payload="{\"tenantId\":\"${TENANT_ID}\",\"claimId\":\"${baseline_claim_id}\",\"correlationId\":\"PERF-CLAIM-${timestamp}\",\"actor\":\"assurance-perf\"}"
    measure_latency "revenue_claim_status" "POST" "/api/v1/revenue/claim-status/checks" "$perf_claim_payload" "200" "$PERF_BUDGET_REVENUE_CLAIM_STATUS_P95_MS"
  else
    fail "performance.revenue_claim_status" "baseline claim id unavailable"
  fi

  if [[ -n "${baseline_event_id:-}" ]]; then
    measure_latency "adt_get_event" "GET" "/api/v1/interoperability/adt/events/${baseline_event_id}" "" "200" "$PERF_BUDGET_ADT_GET_EVENT_P95_MS"
  else
    fail "performance.adt_get_event" "baseline event id unavailable"
  fi

  if [[ -n "${price_version_id:-}" ]]; then
    perf_price_estimate_payload="{\"tenantId\":\"${TENANT_ID}\",\"versionId\":\"${price_version_id}\",\"serviceCode\":\"SVC-99213\",\"units\":2,\"correlationId\":\"PERF-PRICE-${timestamp}\",\"actor\":\"assurance-perf\"}"
    measure_latency "price_estimate" "POST" "/api/v1/revenue/price-transparency/estimates" "$perf_price_estimate_payload" "200" "$PERF_BUDGET_PRICE_ESTIMATE_P95_MS"

    if [[ "$PERF_PRICE_ESTIMATE_LOAD_ENABLED" == "true" ]]; then
      measure_latency "price_estimate_load" "POST" "/api/v1/revenue/price-transparency/estimates" "$perf_price_estimate_payload" "200" "$PERF_BUDGET_PRICE_ESTIMATE_LOAD_P95_MS" "$PERF_PRICE_ESTIMATE_LOAD_SAMPLES"
    fi
  else
    fail "performance.price_estimate" "price_version_id unavailable"
  fi
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

perf_json="[]"
if [[ "${#perf_results[@]}" -gt 0 ]]; then
  perf_json="$(printf '%s\n' "${perf_results[@]}" | jq -s '.')"
fi

previous_assurance_artifact="$(ls -1t ${OUTPUT_DIR}/wave1-local-assurance-*.json 2>/dev/null | head -n 1 || true)"
if [[ -n "$previous_assurance_artifact" && -f "$previous_assurance_artifact" && "${#perf_results[@]}" -gt 0 ]]; then
  for metric in revenue_claim_status adt_get_event price_estimate price_estimate_load; do
    current_p95="$(jq -r --arg metric "$metric" '.[] | select(.name == $metric) | .p95Ms // empty' <<< "$perf_json")"
    previous_p95="$(jq -r --arg metric "$metric" '.performance[]? | select(.name == $metric) | .p95Ms // empty' "$previous_assurance_artifact")"
    if [[ -z "$current_p95" || -z "$previous_p95" ]]; then
      continue
    fi

    delta_percent="$(awk -v cur="$current_p95" -v prev="$previous_p95" 'BEGIN { if (prev <= 0) { print "0.00"; } else { printf "%.2f", ((cur-prev)/prev)*100; } }')"
    if awk -v delta="$delta_percent" -v threshold="$PERF_TREND_WARN_THRESHOLD_PERCENT" 'BEGIN { exit !(delta > threshold) }'; then
      warning="Metric ${metric} p95 regression ${delta_percent}% (previous=${previous_p95}ms current=${current_p95}ms threshold=${PERF_TREND_WARN_THRESHOLD_PERCENT}%)"
      trend_warnings+=("{\"metric\":\"${metric}\",\"warning\":\"${warning}\"}")
      echo "WARN: ${warning}"
    fi
  done
fi

trend_json="[]"
if [[ "${#trend_warnings[@]}" -gt 0 ]]; then
  trend_json="$(printf '%s\n' "${trend_warnings[@]}" | jq -s '.')"
fi

jq -n \
  --arg timestamp "$timestamp" \
  --arg tenantId "$TENANT_ID" \
  --arg gatewayUrl "$IN_NETWORK_GATEWAY_URL" \
  --arg baselineArtifact "$baseline_artifact" \
  --arg previousAssuranceArtifact "$previous_assurance_artifact" \
  --argjson passed "$passed" \
  --argjson failed "$failed" \
  --argjson checks "$(printf '%s\n' "${results[@]}" | jq -s '.')" \
  --argjson performance "$perf_json" \
  --argjson trendWarnings "$trend_json" \
  '{
    timestamp: $timestamp,
    tenantId: $tenantId,
    gatewayUrl: $gatewayUrl,
    baselineArtifact: $baselineArtifact,
    previousAssuranceArtifact: $previousAssuranceArtifact,
    summary: {
      totalChecks: ($passed + $failed),
      passed: $passed,
      failed: $failed,
      passRate: (if ($passed + $failed) == 0 then 0 else (($passed / ($passed + $failed)) * 100) end)
    },
    performance: $performance,
    trendWarnings: $trendWarnings,
    checks: $checks
  }' > "$report_file"

echo "Wave-1 local assurance report: $report_file"

if [[ "$failed" -gt 0 ]]; then
  exit 1
fi
