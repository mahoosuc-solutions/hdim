#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCREENSHOT_DIR="${SCREENSHOT_DIR:-screenshots/demo-portal}"
PORTAL_URL="${PORTAL_URL:-http://localhost:4400}"
BACKEND_URL="${BACKEND_URL:-http://127.0.0.1:18080}"
DEMO_USER="${DEMO_USER:-demo_admin}"
DEMO_PASS="${DEMO_PASS:-demo123}"
DEMO_TENANT="${DEMO_TENANT:-acme-health}"
AUTH_ENDPOINT="${BACKEND_URL}/api/v1/auth/login"
DETAIL_WAIT_RETRIES="${DETAIL_WAIT_RETRIES:-8}"
DETAIL_WAIT_DELAY="${DETAIL_WAIT_DELAY:-2}"
DETAIL_SUMMARY_SELECTOR="${DETAIL_SUMMARY_SELECTOR:-.patient-detail-container .patient-summary}"
HANDSHAKE_FILE="${HANDSHAKE_FILE:-${SCREENSHOT_DIR}/patient-detail-handshake.json}"

mkdir -p "${SCREENSHOT_DIR}"

source "${SCRIPT_DIR}/_common.sh"

log() {
  printf "[%s] %s\n" "$(date --iso-8601=seconds)" "$*"
}
ensure_portal_reachable
require_agent_browser

export AGENT_BROWSER_SESSION="${AGENT_BROWSER_SESSION:-patient-detail-${USER}-${RANDOM}}"
log "Using agent-browser session ${AGENT_BROWSER_SESSION}"

run_agent() {
  log "agent-browser $*"
  agent-browser "$@"
}

screenshot_with_retry() {
  local name="$1"
  local path="${SCREENSHOT_DIR}/${name}"
  local attempts=0
  while ((attempts < 3)); do
    attempts=$((attempts + 1))
    log "Capturing ${name} (attempt ${attempts})"
    if agent-browser screenshot "${path}"; then
      if [[ -s "${path}" ]]; then
        log "Saved ${path}"
        return 0
      fi
      log "Screenshot ${path} is empty"
    else
      log "Screenshot command failed for ${name}"
    fi
    sleep 2
  done
  log "Failed to capture ${name} after ${attempts} attempts"
  return 1
}

wait_for_patient_list() {
  run_agent wait "table.patients-table tbody tr"
  run_agent wait "button[aria-label*=\"View full details\"]"
}

click_patient_detail() {
  local attempts=0
  while ((attempts < 3)); do
    attempts=$((attempts + 1))
    log "Attempt ${attempts}: clicking first patient detail button"
    run_agent eval "(function(){const btn=document.querySelector('button[aria-label*=\"View full details\"]'); if(btn){btn.scrollIntoView(); btn.click(); return true;} return false;})()"
    if run_agent wait "div.patient-detail-container .content"; then
      sleep 1
      run_agent wait "div.patient-detail-container .content"
      return 0
    fi
    log "Patient detail panel did not stabilize on attempt ${attempts}"
  done
  log "ERROR: Unable to open patient detail panel after ${attempts} attempts"
  return 1
}

capture_patient_list() {
  wait_for_patient_list
  screenshot_with_retry "patient-list-agent.png"
}

capture_patient_detail() {
  log "Opening first patient detail"
  if ! click_patient_detail; then
    log "ERROR: Patient detail capture workflow failed"
    return 1
  fi
  screenshot_with_retry "patient-detail-agent.png"
}

grade_run() {
  local grade="PASS"
  local missing=()
  for file in patient-list-agent.png patient-detail-agent.png; do
    local path="${SCREENSHOT_DIR}/${file}"
    if [[ -s "${path}" ]]; then
      log "Screenshot ${file} size $(stat -c%s "${path}") bytes"
    else
      grade="FAIL"
      missing+=("${file}")
      log "Screenshot ${file} is missing or empty"
    fi
  done
  log "Grade: ${grade}"
  if [[ "${grade}" == "FAIL" ]]; then
    log "Missing assets: ${missing[*]}"
    exit 1
  fi
}

log "Validating demo stack before capture"
"${SCRIPT_DIR}/../monitor/ensure-demo-stack.sh"

log "Requesting demo auth token"
AUTH_RESPONSE=$(curl -fsS -X POST "${AUTH_ENDPOINT}" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: ${DEMO_TENANT}" \
  -d "{\"username\":\"${DEMO_USER}\",\"password\":\"${DEMO_PASS}\"}")

ACCESS_TOKEN=$(printf '%s' "${AUTH_RESPONSE}" | jq -r '.accessToken')
REFRESH_TOKEN=$(printf '%s' "${AUTH_RESPONSE}" | jq -r '.refreshToken')

if [[ -z "${ACCESS_TOKEN}" || -z "${REFRESH_TOKEN}" ]]; then
  log "ERROR: Auth response missing tokens: ${AUTH_RESPONSE}"
  exit 1
fi

AGENT_HOST=$(python - <<'PY'
from urllib.parse import urlparse
import os
url = os.environ.get('PORTAL_URL', 'http://localhost:4400')
print(urlparse(url).hostname)
PY
)

log "Seeding agent-browser session with auth cookies and user profile"
agent-browser cookies set hdim_access_token "${ACCESS_TOKEN}" --domain "${AGENT_HOST}" --path /api --sameSite Strict --httpOnly
agent-browser cookies set hdim_refresh_token "${REFRESH_TOKEN}" --domain "${AGENT_HOST}" --path /api/v1/auth --sameSite Strict --httpOnly

run_agent open "${PORTAL_URL}"
run_agent wait "body"

USER_PROFILE=$(python - <<'PY'
import json, os
portal_user = {
  'id': '',
  'username': os.environ.get('DEMO_USER', 'demo_admin'),
  'email': os.environ.get('DEMO_USER', 'demo_admin') + '@hdim.ai',
  'firstName': os.environ.get('DEMO_USER', 'demo_admin'),
  'lastName': '',
  'fullName': os.environ.get('DEMO_USER', 'demo_admin'),
  'roles': [{'id': '', 'name': 'ADMIN'}, {'id': '', 'name': 'EVALUATOR'}],
  'tenantId': os.environ.get('DEMO_TENANT', 'acme-health'),
  'tenantIds': [os.environ.get('DEMO_TENANT', 'acme-health')],
  'active': True,
}
print(json.dumps(portal_user))
PY
)

agent-browser eval "localStorage.setItem('healthdata_user', '${USER_PROFILE}'); localStorage.setItem('healthdata_tenant', '${DEMO_TENANT}');"

log "Navigating to patient list"
run_agent open "${PORTAL_URL}/patients"
capture_patient_list
capture_patient_detail
grade_run

log "Agent-browser patient detail capture complete"
