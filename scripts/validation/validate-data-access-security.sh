#!/usr/bin/env bash

set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
TENANT_ID="${TENANT_ID:-acme-health}"
WRONG_TENANT_ID="${WRONG_TENANT_ID:-wrong-tenant}"
AUTH_USERNAME="${AUTH_USERNAME:-demo_admin}"
AUTH_PASSWORD="${AUTH_PASSWORD:-demo123}"
TIMEOUT="${TIMEOUT:-20}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

http_code() {
  local url="$1"
  shift
  curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 -o /tmp/validate-access-body.$$ -w '%{http_code}' "$url" "$@"
}

json_count() {
  local payload="$1"
  printf '%s' "$payload" | jq -r '
    if type=="array" then length
    elif type=="object" and (.total? != null) then .total
    elif type=="object" and (.content? | type=="array") then .content | length
    elif type=="object" and (.entry? | type=="array") then .entry | length
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

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Data Access and Security Validation${NC}"
echo -e "${BLUE}============================================${NC}"
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
  log_pass "Authentication token issued"
else
  log_fail "Unable to authenticate demo user (HTTP ${LOGIN_STATUS})"
  echo "Cannot continue strict auth checks without token."
  exit 1
fi

AUTH_HEADER=(-H "Authorization: Bearer ${AUTH_TOKEN}")
TENANT_HEADER=(-H "X-Tenant-ID: ${TENANT_ID}")
WRONG_TENANT_HEADER=(-H "X-Tenant-ID: ${WRONG_TENANT_ID}")

echo
echo "Data readiness checks"

PATIENTS_PAYLOAD="$(curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 \
  "${GATEWAY_URL}/fhir/Patient?_count=20" "${TENANT_HEADER[@]}" "${AUTH_HEADER[@]}")"
PATIENTS_COUNT="$(json_count "${PATIENTS_PAYLOAD}")"
if (( PATIENTS_COUNT > 0 )); then
  log_pass "Patient list is populated (${PATIENTS_COUNT})"
else
  log_fail "Patient list is empty"
fi

RESULTS_PAYLOAD="$(curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 \
  "${GATEWAY_URL}/quality-measure/results?page=0&size=50" "${TENANT_HEADER[@]}" "${AUTH_HEADER[@]}")"
RESULTS_COUNT="$(json_count "${RESULTS_PAYLOAD}")"
if (( RESULTS_COUNT > 0 )); then
  log_pass "Quality results are populated (${RESULTS_COUNT})"
else
  log_fail "Quality results are empty"
fi

BEST_PATIENT_ID=""
BEST_OBS_COUNT=0
for candidate_id in $(printf '%s' "${PATIENTS_PAYLOAD}" | jq -r '.entry[]?.resource?.id // empty' 2>/dev/null || true); do
  OBS_PAYLOAD="$(curl -sS --max-time "${TIMEOUT}" --connect-timeout 5 \
    "${GATEWAY_URL}/fhir/Observation?patient=${candidate_id}&_summary=count&_count=0" \
    "${TENANT_HEADER[@]}" "${AUTH_HEADER[@]}")"
  OBS_COUNT="$(json_count "${OBS_PAYLOAD}")"
  if (( OBS_COUNT > BEST_OBS_COUNT )); then
    BEST_OBS_COUNT="${OBS_COUNT}"
    BEST_PATIENT_ID="${candidate_id}"
  fi
  if (( OBS_COUNT > 0 )); then
    break
  fi
done

if [[ -n "${BEST_PATIENT_ID}" ]]; then
  if (( BEST_OBS_COUNT > 0 )); then
    log_pass "Patient ${BEST_PATIENT_ID} has clinical observations (${BEST_OBS_COUNT})"
  else
    log_fail "Sampled patients had no observations"
  fi
else
  log_fail "Could not identify a patient ID for detail checks"
fi

echo
echo "Access control checks"

check_access_matrix() {
  local name="$1"
  local url="$2"

  local status_ok status_wrong_tenant status_missing_tenant status_missing_auth
  status_ok="$(http_code "${url}" "${TENANT_HEADER[@]}" "${AUTH_HEADER[@]}")"
  status_wrong_tenant="$(http_code "${url}" "${WRONG_TENANT_HEADER[@]}" "${AUTH_HEADER[@]}")"
  status_missing_tenant="$(http_code "${url}" "${AUTH_HEADER[@]}")"
  status_missing_auth="$(http_code "${url}" "${TENANT_HEADER[@]}")"

  if [[ "${status_ok}" == "200" ]]; then
    log_pass "${name}: valid tenant+auth allowed (200)"
  else
    log_fail "${name}: valid tenant+auth expected 200, got ${status_ok}"
  fi

  if [[ "${status_wrong_tenant}" == "403" ]]; then
    log_pass "${name}: wrong tenant denied (403)"
  else
    log_fail "${name}: wrong tenant expected 403, got ${status_wrong_tenant}"
  fi

  if [[ "${status_missing_tenant}" == "400" || "${status_missing_tenant}" == "403" ]]; then
    log_pass "${name}: missing tenant denied (${status_missing_tenant})"
  else
    log_fail "${name}: missing tenant expected 400/403, got ${status_missing_tenant}"
  fi

  if [[ "${status_missing_auth}" == "401" || "${status_missing_auth}" == "403" ]]; then
    log_pass "${name}: missing auth denied (${status_missing_auth})"
  else
    log_fail "${name}: missing auth expected 401/403, got ${status_missing_auth}"
  fi
}

check_access_matrix "FHIR Patients" "${GATEWAY_URL}/fhir/Patient?_count=1"
check_access_matrix "Quality Results" "${GATEWAY_URL}/quality-measure/results?page=0&size=1"
check_access_matrix "Care Gaps" "${GATEWAY_URL}/care-gap/api/v1/care-gaps?page=0&size=1"
check_access_matrix "Patient API" "${GATEWAY_URL}/patient/api/v1/patients?page=0&size=1"

if (( SCORE < 0 )); then
  SCORE=0
fi
GRADE="$(grade_from_score "${SCORE}")"

echo
echo -e "${BLUE}============================================${NC}"
echo "Score: ${SCORE}"
echo "Grade: ${GRADE}"
echo "Pass: ${PASS_COUNT}"
echo "Fail: ${FAIL_COUNT}"
echo -e "${BLUE}============================================${NC}"

if (( FAIL_COUNT > 0 )); then
  exit 1
fi

exit 0
