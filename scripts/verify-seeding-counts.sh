#!/usr/bin/env bash
set -euo pipefail

FHIR_BASE_URL="${FHIR_BASE_URL:-http://localhost:8085/fhir}"
CARE_GAP_BASE_URL="${CARE_GAP_BASE_URL:-http://localhost:8086/care-gap}"
TENANTS_CSV="${TENANTS:-summit-care-2026,valley-health-2026,acme-health}"
EXPECTED_PATIENTS_PER_TENANT="${EXPECTED_PATIENTS_PER_TENANT:-}"

AUTH_USER_ID="${AUTH_USER_ID:-00000000-0000-0000-0000-000000000001}"
AUTH_USERNAME="${AUTH_USERNAME:-demo-seeder}"
AUTH_ROLES="${AUTH_ROLES:-ADMIN,SYSTEM}"
AUTH_VALIDATED="${AUTH_VALIDATED:-gateway-healthcheck}"

read -r -a TENANTS <<< "${TENANTS_CSV//,/ }"

print_row() {
  printf "%-22s | %-12s | %-14s | %-10s\n" "$1" "$2" "$3" "$4"
}

get_fhir_patient_count() {
  local tenant="$1"
  local tmp
  tmp="$(mktemp)"
  curl -sS \
    -H "X-Tenant-ID: ${tenant}" \
    -H "X-Auth-User-Id: ${AUTH_USER_ID}" \
    -H "X-Auth-Username: ${AUTH_USERNAME}" \
    -H "X-Auth-Tenant-Ids: ${tenant}" \
    -H "X-Auth-Roles: ${AUTH_ROLES}" \
    -H "X-Auth-Validated: ${AUTH_VALIDATED}" \
    "${FHIR_BASE_URL}/Patient?_summary=count&_count=0" \
    -o "${tmp}"
  python3 - <<PY
import json
try:
  with open("${tmp}", "r", encoding="utf-8") as fh:
    data=json.load(fh)
  print(data.get("total", ""))
except Exception:
  print("")
PY
  rm -f "${tmp}"
}

get_care_gap_count() {
  local tenant="$1"
  local tmp
  tmp="$(mktemp)"
  curl -sS \
    -H "X-Tenant-ID: ${tenant}" \
    -H "X-Auth-User-Id: ${AUTH_USER_ID}" \
    -H "X-Auth-Username: ${AUTH_USERNAME}" \
    -H "X-Auth-Tenant-Ids: ${tenant}" \
    -H "X-Auth-Roles: ${AUTH_ROLES}" \
    -H "X-Auth-Validated: ${AUTH_VALIDATED}" \
    "${CARE_GAP_BASE_URL}/api/v1/care-gaps?page=0&size=1" \
    -o "${tmp}"
  python3 - <<PY
import json
try:
  with open("${tmp}", "r", encoding="utf-8") as fh:
    data=json.load(fh)
  print(data.get("totalElements", ""))
except Exception:
  print("")
PY
  rm -f "${tmp}"
}

print_row "Tenant" "Patients" "Care Gaps" "Match"
print_row "------" "--------" "---------" "-----"

exit_code=0
for tenant in "${TENANTS[@]}"; do
  patient_count="$(get_fhir_patient_count "$tenant")"
  care_gap_count="$(get_care_gap_count "$tenant")"

  match="n/a"
  if [[ -n "${EXPECTED_PATIENTS_PER_TENANT}" && -n "${patient_count}" ]]; then
    if [[ "${patient_count}" == "${EXPECTED_PATIENTS_PER_TENANT}" ]]; then
      match="yes"
    else
      match="no"
      exit_code=2
    fi
  fi

  print_row "$tenant" "${patient_count:-?}" "${care_gap_count:-?}" "$match"

done

if [[ $exit_code -ne 0 ]]; then
  echo "\nExpected patient count mismatch. EXPECTED_PATIENTS_PER_TENANT=${EXPECTED_PATIENTS_PER_TENANT}" >&2
fi

exit $exit_code
