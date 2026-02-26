#!/usr/bin/env bash

set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
TENANT_ID="${TENANT_ID:-acme-health}"
WRONG_TENANT_ID="${WRONG_TENANT_ID:-wrong-tenant}"
AUTH_USERNAME="${AUTH_USERNAME:-demo_admin}"
AUTH_PASSWORD="${AUTH_PASSWORD:-demo123}"
TIMEOUT="${TIMEOUT:-20}"
PATIENT_CANDIDATE_COUNT="${PATIENT_CANDIDATE_COUNT:-25}"

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

PASS_COUNT=0
FAIL_COUNT=0
SCORE=100

log_pass() {
  PASS_COUNT=$((PASS_COUNT + 1))
  echo -e "${GREEN}PASS${NC} - $1"
}

log_fail() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  SCORE=$((SCORE - 10))
  echo -e "${RED}FAIL${NC} - $1"
}

json_count() {
  local payload="$1"
  printf '%s' "$payload" | jq -r '
    if type=="array" then length
    elif type=="object" and (.total? != null) then .total
    elif type=="object" and (.entry? | type=="array") then .entry | length
    elif type=="object" and (.content? | type=="array") then .content | length
    else 0
    end
  ' 2>/dev/null || echo 0
}

grade_from_score() {
  local score="$1"
  if (( score >= 90 )); then echo "A";
  elif (( score >= 80 )); then echo "B";
  elif (( score >= 70 )); then echo "C";
  elif (( score >= 60 )); then echo "D";
  else echo "F"; fi
}

request_json() {
  local url="$1"
  shift
  local body status
  body="$(mktemp)"
  status="$(curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 -o "${body}" -w '%{http_code}' "${url}" "$@" || true)"
  echo "${status}|$(cat "${body}")"
  rm -f "${body}"
}

echo -e "${BLUE}=================================================${NC}"
echo -e "${BLUE}Patient Detail UI API Data + Security Validation${NC}"
echo -e "${BLUE}=================================================${NC}"
echo "Gateway: ${GATEWAY_URL}"
echo "Tenant: ${TENANT_ID}"
echo

LOGIN_RESPONSE="$(curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 \
  -X POST "${GATEWAY_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: ${TENANT_ID}" \
  -d "{\"username\":\"${AUTH_USERNAME}\",\"password\":\"${AUTH_PASSWORD}\"}" \
  -w $'\n%{http_code}')"
LOGIN_STATUS="$(printf '%s' "${LOGIN_RESPONSE}" | tail -n1)"
LOGIN_BODY="$(printf '%s' "${LOGIN_RESPONSE}" | sed '$d')"
AUTH_TOKEN="$(printf '%s' "${LOGIN_BODY}" | jq -r '.accessToken // empty' 2>/dev/null || true)"

if [[ "${LOGIN_STATUS}" == "200" && -n "${AUTH_TOKEN}" ]]; then
  log_pass "Authenticated ${AUTH_USERNAME}"
else
  log_fail "Authentication failed for ${AUTH_USERNAME} (HTTP ${LOGIN_STATUS})"
  exit 1
fi

TENANT_IDS="$(printf '%s' "${LOGIN_BODY}" | jq -c '.tenantIds // []' 2>/dev/null || echo '[]')"
if printf '%s' "${TENANT_IDS}" | jq -e --arg t "${TENANT_ID}" 'index($t) != null' >/dev/null 2>&1; then
  log_pass "User token includes tenant scope ${TENANT_ID}"
else
  log_fail "User token missing tenant scope ${TENANT_ID}"
fi

AUTH_HEADER=(-H "Authorization: Bearer ${AUTH_TOKEN}")
TENANT_HEADER=(-H "X-Tenant-ID: ${TENANT_ID}")
WRONG_TENANT_HEADER=(-H "X-Tenant-ID: ${WRONG_TENANT_ID}")

PATIENTS_PAYLOAD="$(curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 \
  "${GATEWAY_URL}/fhir/Patient?_count=${PATIENT_CANDIDATE_COUNT}" "${TENANT_HEADER[@]}" "${AUTH_HEADER[@]}")"

PATIENT_COUNT="$(json_count "${PATIENTS_PAYLOAD}")"
if (( PATIENT_COUNT > 0 )); then
  log_pass "Patient list is available (${PATIENT_COUNT})"
else
  log_fail "Patient list is empty"
  exit 1
fi

SELECTED_PATIENT_ID=""
BEST_SCORE=-1
for pid in $(printf '%s' "${PATIENTS_PAYLOAD}" | jq -r '.entry[]?.resource?.id // empty' 2>/dev/null || true); do
  OBS="$(curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 "${GATEWAY_URL}/fhir/Observation?patient=${pid}&_summary=count&_count=0" "${TENANT_HEADER[@]}" "${AUTH_HEADER[@]}")"
  RES="$(curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 "${GATEWAY_URL}/quality-measure/results?patient=${pid}&page=0&size=5" "${TENANT_HEADER[@]}" "${AUTH_HEADER[@]}")"
  OBS_COUNT="$(json_count "${OBS}")"
  RES_COUNT="$(json_count "${RES}")"
  SCORE_CANDIDATE=$(( OBS_COUNT * 2 + RES_COUNT * 3 ))
  if (( SCORE_CANDIDATE > BEST_SCORE )); then
    BEST_SCORE="${SCORE_CANDIDATE}"
    SELECTED_PATIENT_ID="${pid}"
  fi
done

if [[ -z "${SELECTED_PATIENT_ID}" ]]; then
  log_fail "Could not resolve a patient ID for patient-detail API checks"
  exit 1
fi
log_pass "Selected patient for UI API contract checks: ${SELECTED_PATIENT_ID}"

declare -a DETAIL_ENDPOINTS=(
  "/fhir/Patient/${SELECTED_PATIENT_ID}"
  "/fhir/Observation?patient=${SELECTED_PATIENT_ID}&_count=50"
  "/fhir/Condition?patient=${SELECTED_PATIENT_ID}&_count=50"
  "/fhir/Procedure?patient=${SELECTED_PATIENT_ID}&_count=50"
  "/quality-measure/results?patient=${SELECTED_PATIENT_ID}&page=0&size=50"
  "/quality-measure/patient-health/overview/${SELECTED_PATIENT_ID}"
)

echo
echo "Validating patient-detail API calls (UI contract)"
for endpoint in "${DETAIL_ENDPOINTS[@]}"; do
  RESPONSE="$(request_json "${GATEWAY_URL}${endpoint}" "${TENANT_HEADER[@]}" "${AUTH_HEADER[@]}")"
  STATUS="${RESPONSE%%|*}"
  BODY="${RESPONSE#*|}"

  if [[ "${STATUS}" == "200" ]]; then
    log_pass "${endpoint} returns 200"
  else
    log_fail "${endpoint} expected 200, got ${STATUS}"
    continue
  fi

  case "${endpoint}" in
    /fhir/Patient/*)
      if printf '%s' "${BODY}" | jq -e --arg id "${SELECTED_PATIENT_ID}" '.resourceType=="Patient" and .id==$id' >/dev/null 2>&1; then
        log_pass "Patient payload contract valid"
      else
        log_fail "Patient payload contract invalid"
      fi
      ;;
    /fhir/Observation*|/fhir/Condition*|/fhir/Procedure*)
      if printf '%s' "${BODY}" | jq -e '.resourceType=="Bundle"' >/dev/null 2>&1; then
        log_pass "FHIR bundle contract valid for ${endpoint}"
      else
        log_fail "FHIR bundle contract invalid for ${endpoint}"
      fi
      ;;
    /quality-measure/results*)
      if printf '%s' "${BODY}" | jq -e 'type=="array"' >/dev/null 2>&1; then
        log_pass "Quality results contract valid (array payload)"
      else
        log_fail "Quality results contract invalid"
      fi
      ;;
    /quality-measure/patient-health/overview/*)
      if printf '%s' "${BODY}" | jq -e '
        .patientId != null and
        (
          .overallHealthScore != null or
          .healthScore.overallScore != null
        )
      ' >/dev/null 2>&1; then
        log_pass "Patient health overview contract valid"
      else
        log_fail "Patient health overview contract invalid"
      fi
      ;;
  esac
done

echo
echo "Validating tenant/auth enforcement on patient-detail APIs"
for endpoint in "${DETAIL_ENDPOINTS[@]}"; do
  OK_CODE="$(curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 -o /dev/null -w '%{http_code}' "${GATEWAY_URL}${endpoint}" "${TENANT_HEADER[@]}" "${AUTH_HEADER[@]}")"
  WRONG_TENANT_CODE="$(curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 -o /dev/null -w '%{http_code}' "${GATEWAY_URL}${endpoint}" "${WRONG_TENANT_HEADER[@]}" "${AUTH_HEADER[@]}")"
  MISSING_TENANT_CODE="$(curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 -o /dev/null -w '%{http_code}' "${GATEWAY_URL}${endpoint}" "${AUTH_HEADER[@]}")"
  MISSING_AUTH_CODE="$(curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 -o /dev/null -w '%{http_code}' "${GATEWAY_URL}${endpoint}" "${TENANT_HEADER[@]}")"

  [[ "${OK_CODE}" == "200" ]] && log_pass "${endpoint}: valid tenant+auth allowed (200)" || log_fail "${endpoint}: expected 200, got ${OK_CODE}"
  [[ "${WRONG_TENANT_CODE}" == "403" ]] && log_pass "${endpoint}: wrong tenant denied (403)" || log_fail "${endpoint}: wrong tenant expected 403, got ${WRONG_TENANT_CODE}"
  [[ "${MISSING_TENANT_CODE}" == "400" || "${MISSING_TENANT_CODE}" == "403" ]] && log_pass "${endpoint}: missing tenant denied (${MISSING_TENANT_CODE})" || log_fail "${endpoint}: missing tenant expected 400/403, got ${MISSING_TENANT_CODE}"
  [[ "${MISSING_AUTH_CODE}" == "401" || "${MISSING_AUTH_CODE}" == "403" ]] && log_pass "${endpoint}: missing auth denied (${MISSING_AUTH_CODE})" || log_fail "${endpoint}: missing auth expected 401/403, got ${MISSING_AUTH_CODE}"
done

if (( SCORE < 0 )); then SCORE=0; fi
GRADE="$(grade_from_score "${SCORE}")"

echo
echo -e "${BLUE}=================================================${NC}"
echo "Score: ${SCORE}"
echo "Grade: ${GRADE}"
echo "Pass: ${PASS_COUNT}"
echo "Fail: ${FAIL_COUNT}"
echo -e "${BLUE}=================================================${NC}"

if (( FAIL_COUNT > 0 )); then
  exit 1
fi
exit 0
