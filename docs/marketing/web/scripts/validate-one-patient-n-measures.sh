#!/usr/bin/env bash
set -euo pipefail

# Validate the claim: one patient event/data context can be evaluated against N measures.
# Required env:
#   QUALITY_MEASURE_BASE_URL (example: http://localhost:8087/quality-measure)
#   PATIENT_ID (example: PAT-12345)
# Optional env:
#   N_MEASURES (default: 5)
#   TENANT_ID (adds X-Tenant-ID header)
#   AUTH_TOKEN (adds Authorization: Bearer ...)
#   OUT_DIR (default: docs/marketing/web/evidence)
#   API_MODE (auto|a|b)

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required" >&2
  exit 2
fi
if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required" >&2
  exit 2
fi

: "${QUALITY_MEASURE_BASE_URL:?QUALITY_MEASURE_BASE_URL is required}"
: "${PATIENT_ID:?PATIENT_ID is required}"

N_MEASURES="${N_MEASURES:-5}"
OUT_DIR="${OUT_DIR:-docs/marketing/web/evidence}"
mkdir -p "$OUT_DIR"

TS_UTC="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
JSON_OUT="$OUT_DIR/one-patient-n-measures-$STAMP.json"
MD_OUT="$OUT_DIR/one-patient-n-measures-$STAMP.md"

BASE_URL="${QUALITY_MEASURE_BASE_URL%/}"
MEASURES_URL="$BASE_URL/api/v1/measures?status=ACTIVE"
EVALUATIONS_URL="$BASE_URL/api/v1/evaluations"
PATIENT_EVALS_URL="$BASE_URL/api/v1/patients/$PATIENT_ID/evaluations"

HEADERS=(-H "Content-Type: application/json")
if [[ -n "${TENANT_ID:-}" ]]; then
  HEADERS+=(-H "X-Tenant-ID: $TENANT_ID")
fi
if [[ -n "${AUTH_TOKEN:-}" ]]; then
  HEADERS+=(-H "Authorization: Bearer $AUTH_TOKEN")
fi

now_ms() {
  if date +%s%3N >/dev/null 2>&1; then
    date +%s%3N
  else
    awk 'BEGIN{srand(); print int(systime()*1000)}'
  fi
}

total_start_ms="$(now_ms)"

measures_raw="$(curl -sS "${HEADERS[@]}" "$MEASURES_URL")"
measure_ids="$(echo "$measures_raw" | jq -r '
  if type=="object" and (.measures | type=="array") then .measures[]? | (.measureId // .id)
  elif type=="object" and (.content | type=="array") then .content[]? | (.measureId // .id)
  elif type=="array" then .[]? | (.measureId // .id)
  else empty end
' | head -n "$N_MEASURES")"

if [[ -z "$measure_ids" ]]; then
  measure_ids="$(echo "${DEFAULT_MEASURE_IDS:-HEDIS-BCS,HEDIS-CBP,HEDIS-CCS,HEDIS-CDC,HEDIS-COL,HEDIS-EED,HEDIS-IET,HEDIS-KED,HEDIS-LBP,HEDIS-IMA,HEDIS-SPD,HEDIS-URI}" | tr ',' '\n' | head -n "$N_MEASURES")"
  if [[ -z "$measure_ids" ]]; then
    echo "No active measure IDs found from $MEASURES_URL and no defaults available" >&2
    exit 3
  fi
fi

mapfile -t ids <<<"$measure_ids"
requested_count="${#ids[@]}"

success_count=0
fail_count=0
results_json='[]'

eval_date="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
api_mode="${API_MODE:-auto}"

mode_a_request() {
  local measure_id="$1"
  local payload response_file http_code

  payload="$(jq -nc --arg pid "$PATIENT_ID" --arg mid "$measure_id" --arg dt "$eval_date" '{patientId:$pid, measureId:$mid, evaluationDate:$dt}')"
  response_file="$(mktemp)"
  http_code="$(curl -sS -o "$response_file" -w "%{http_code}" "${HEADERS[@]}" -X POST "$EVALUATIONS_URL" -d "$payload" || true)"
  cat "$response_file"
  rm -f "$response_file"
  printf "\n__HTTP_CODE__=%s\n" "$http_code"
}

mode_b_request() {
  local measure_id="$1"
  local response_file http_code url

  url="$BASE_URL/calculate?patient=$PATIENT_ID&measure=$measure_id&createdBy=marketing-validation"
  response_file="$(mktemp)"
  http_code="$(curl -sS -o "$response_file" -w "%{http_code}" "${HEADERS[@]}" -X POST "$url" || true)"
  cat "$response_file"
  rm -f "$response_file"
  printf "\n__HTTP_CODE__=%s\n" "$http_code"
}

for measure_id in "${ids[@]}"; do
  requested_measure_id="$measure_id"
  body=""
  http_code="000"
  measure_start_ms="$(now_ms)"

  if [[ "$api_mode" == "a" || "$api_mode" == "auto" ]]; then
    raw="$(mode_a_request "$measure_id")"
    body="$(echo "$raw" | sed '/__HTTP_CODE__=/d')"
    http_code="$(echo "$raw" | sed -n 's/__HTTP_CODE__=//p' | tail -n 1)"
  fi

  if [[ "$api_mode" == "b" || ("$api_mode" == "auto" && "$http_code" == "404") ]]; then
    raw="$(mode_b_request "$requested_measure_id")"
    body="$(echo "$raw" | sed '/__HTTP_CODE__=/d')"
    http_code="$(echo "$raw" | sed -n 's/__HTTP_CODE__=//p' | tail -n 1)"
    if [[ "$http_code" =~ ^5 && "$requested_measure_id" != HEDIS-* ]]; then
      requested_measure_id="HEDIS-$requested_measure_id"
      raw="$(mode_b_request "$requested_measure_id")"
      body="$(echo "$raw" | sed '/__HTTP_CODE__=/d')"
      http_code="$(echo "$raw" | sed -n 's/__HTTP_CODE__=//p' | tail -n 1)"
    fi
    api_mode="b"
  elif [[ "$api_mode" == "auto" && "$http_code" != "404" ]]; then
    api_mode="a"
  fi

  eval_id="$(echo "$body" | jq -r '.evaluationId // .id // empty' 2>/dev/null || true)"

  status="fail"
  if [[ "$http_code" == "200" || "$http_code" == "201" ]]; then
    status="pass"
    success_count=$((success_count + 1))
  else
    fail_count=$((fail_count + 1))
  fi

  measure_end_ms="$(now_ms)"
  runtime_ms=$((measure_end_ms - measure_start_ms))

  result_obj="$(jq -nc \
    --arg measureId "$requested_measure_id" \
    --arg status "$status" \
    --arg httpCode "$http_code" \
    --arg evaluationId "$eval_id" \
    --arg response "$body" \
    --argjson runtimeMs "$runtime_ms" \
    '{measureId:$measureId,status:$status,httpCode:$httpCode,evaluationId:$evaluationId,runtimeMs:$runtimeMs,response:$response}')"
  results_json="$(jq -nc --argjson acc "$results_json" --argjson item "$result_obj" '$acc + [$item]')"
done

patient_evals_raw="$(curl -sS "${HEADERS[@]}" "$PATIENT_EVALS_URL" || true)"
patient_eval_count="$(echo "$patient_evals_raw" | jq -r '
  if .totalCount then .totalCount
  elif .evaluations then (.evaluations|length)
  elif type=="array" then length
  else 0 end
' 2>/dev/null || echo 0)"

if [[ "$api_mode" == "b" ]]; then
  patient_evals_raw="$(curl -sS "${HEADERS[@]}" "$BASE_URL/results?patient=$PATIENT_ID" || true)"
  patient_eval_count="$(echo "$patient_evals_raw" | jq -r '
    if type=="array" then length
    elif .results then (.results|length)
    elif .content then (.content|length)
    else 0 end
  ' 2>/dev/null || echo 0)"
fi

total_end_ms="$(now_ms)"
total_runtime_ms=$((total_end_ms - total_start_ms))

claim_validated=false
if [[ "$success_count" -eq "$requested_count" && "$requested_count" -ge 1 ]]; then
  claim_validated=true
fi

jq -n \
  --arg timestamp "$TS_UTC" \
  --arg baseUrl "$BASE_URL" \
  --arg patientId "$PATIENT_ID" \
  --argjson requestedMeasures "$requested_count" \
  --argjson successCount "$success_count" \
  --argjson failCount "$fail_count" \
  --argjson patientEvalCount "$patient_eval_count" \
  --arg apiMode "$api_mode" \
  --argjson totalRuntimeMs "$total_runtime_ms" \
  --argjson claimValidated "$claim_validated" \
  --argjson results "$results_json" \
  '{
    timestamp: $timestamp,
    baseUrl: $baseUrl,
    patientId: $patientId,
    requestedMeasures: $requestedMeasures,
    successCount: $successCount,
    failCount: $failCount,
    patientEvalCount: $patientEvalCount,
    apiMode: $apiMode,
    totalRuntimeMs: $totalRuntimeMs,
    claimValidated: $claimValidated,
    results: $results
  }' > "$JSON_OUT"

{
  echo "# One Patient -> N Measures Validation"
  echo
  echo "- Timestamp (UTC): $TS_UTC"
  echo "- Base URL: $BASE_URL"
  echo "- Patient ID: $PATIENT_ID"
  echo "- Requested Measures (N): $requested_count"
  echo "- API Mode Used: $api_mode"
  echo "- Successful Evaluations: $success_count"
  echo "- Failed Evaluations: $fail_count"
  echo "- Patient Evaluation Count Endpoint Result: $patient_eval_count"
  echo "- Total Runtime (ms): $total_runtime_ms"
  echo "- Claim Validated: $claim_validated"
  echo
  echo "## Per-Measure Results"
  echo
  echo "| Measure ID | Status | HTTP | Runtime (ms) | Evaluation ID |"
  echo "|---|---|---:|---:|---|"
  echo "$results_json" | jq -r '.[] | "| \(.measureId) | \(.status) | \(.httpCode) | \(.runtimeMs) | \(.evaluationId // "") |"'
  echo
  echo "JSON evidence: $(basename "$JSON_OUT")"
} > "$MD_OUT"

echo "Wrote: $JSON_OUT"
echo "Wrote: $MD_OUT"

if [[ "$claim_validated" != "true" ]]; then
  echo "Validation failed: claim not fully validated" >&2
  exit 1
fi

echo "Validation passed: one patient evaluated against N measures"
