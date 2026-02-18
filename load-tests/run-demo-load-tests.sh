#!/usr/bin/env bash
# =============================================================================
# run-demo-load-tests.sh — HDIM k6 Load Tests Against Demo Environment (mTLS)
# =============================================================================
#
# Runs k6 load tests against the running demo containers, which use mutual
# TLS (mTLS). Extracts client certificates from the demo containers, sets
# gateway-trust headers for authentication, and runs all scenarios.
#
# Usage:
#   ./load-tests/run-demo-load-tests.sh              # Full load test (100 VUs)
#   ./load-tests/run-demo-load-tests.sh --smoke      # Smoke test (1 VU, 1 iter)
#   ./load-tests/run-demo-load-tests.sh --scenario patient  # One scenario
#
# Prerequisites:
#   - Docker running with hdim-demo-patient, hdim-demo-care-gap,
#     hdim-demo-quality-measure containers healthy
#   - k6 binary at /tmp/k6-v0.54.0-linux-amd64/k6 or on PATH
#
# Environment overrides:
#   TENANT_ID          Tenant (default: acme-health)
#   PATIENT_ID         Patient UUID (default: f47ac10b-58cc-4372-a567-0e02b2c3d479)
#   ENVIRONMENT        Label for results (default: demo)
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/results"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

# ── Defaults ──────────────────────────────────────────────────────────────────
SMOKE_MODE=false
SCENARIO_FILTER=""
ENVIRONMENT="${ENVIRONMENT:-demo}"
EXIT_CODE=0

# ── Parse arguments ───────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --smoke)     SMOKE_MODE=true; shift ;;
    --scenario)  SCENARIO_FILTER="${2:-}"; shift 2 ;;
    --help|-h)   sed -n '2,35p' "$0" | sed 's/^# //'; exit 0 ;;
    *)           echo "Unknown argument: $1. Use --help for usage." >&2; exit 1 ;;
  esac
done

# ── Colour helpers ────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; BOLD='\033[1m'; RESET='\033[0m'

log_info()    { echo -e "${BLUE}[INFO]${RESET}  $*"; }
log_ok()      { echo -e "${GREEN}[PASS]${RESET}  $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${RESET}  $*"; }
log_error()   { echo -e "${RED}[FAIL]${RESET}  $*"; }
log_section() { echo -e "\n${BOLD}${BLUE}━━━ $* ━━━${RESET}\n"; }

# ── Locate k6 ─────────────────────────────────────────────────────────────────
find_k6() {
  if command -v k6 &>/dev/null; then
    echo "k6"
  elif [[ -x /tmp/k6-v0.54.0-linux-amd64/k6 ]]; then
    echo "/tmp/k6-v0.54.0-linux-amd64/k6"
  else
    log_error "k6 not found. Install from https://k6.io/docs/getting-started/installation/"
    exit 1
  fi
}

K6="$(find_k6)"
log_info "k6: ${K6} ($(${K6} version 2>&1 | head -1))"

# ── Extract mTLS certs from running demo containers ───────────────────────────
extract_certs() {
  log_section "Extracting mTLS certificates from demo containers"

  local container="hdim-demo-patient"

  if ! docker ps --format "{{.Names}}" | grep -q "^${container}$"; then
    log_error "Container ${container} is not running."
    log_error "Start demo: docker compose -f docker-compose.demo.yml up -d"
    exit 1
  fi

  TLS_CA_CERT="$(docker exec "${container}" cat /etc/ssl/mtls/ca-cert.pem 2>/dev/null)"
  TLS_CLIENT_CERT="$(docker exec "${container}" cat /etc/ssl/mtls/gateway-fhir-service/certificate.pem 2>/dev/null)"
  TLS_CLIENT_KEY="$(docker exec "${container}" cat /etc/ssl/mtls/gateway-fhir-service/private-key.pem 2>/dev/null)"

  if [[ -z "${TLS_CA_CERT}" || -z "${TLS_CLIENT_CERT}" || -z "${TLS_CLIENT_KEY}" ]]; then
    log_error "Failed to extract mTLS certificates from ${container}."
    exit 1
  fi

  export TLS_CA_CERT TLS_CLIENT_CERT TLS_CLIENT_KEY
  log_ok "mTLS certificates extracted (CA + gateway-fhir client cert)"
}

# ── Verify service health via mTLS ─────────────────────────────────────────────
verify_services() {
  log_section "Verifying demo service health (mTLS)"

  # Save certs to temp files for curl
  local ca_file client_cert client_key
  ca_file=$(mktemp /tmp/hdim-ca-XXXXXX.pem)
  client_cert=$(mktemp /tmp/hdim-cert-XXXXXX.pem)
  client_key=$(mktemp /tmp/hdim-key-XXXXXX.pem)
  echo "${TLS_CA_CERT}"     > "${ca_file}"
  echo "${TLS_CLIENT_CERT}" > "${client_cert}"
  echo "${TLS_CLIENT_KEY}"  > "${client_key}"
  trap "rm -f ${ca_file} ${client_cert} ${client_key}" EXIT

  local services=(
    "patient-service|https://localhost:8084/patient/actuator/health"
    "care-gap-service|https://localhost:8086/care-gap/actuator/health"
    "quality-measure-service|https://localhost:8087/quality-measure/actuator/health"
  )

  for svc_url in "${services[@]}"; do
    local name="${svc_url%%|*}"
    local url="${svc_url##*|}"
    local status
    status=$(curl -s -o /dev/null -w "%{http_code}" \
      --cacert "${ca_file}" \
      --cert "${client_cert}" \
      --key "${client_key}" \
      "${url}" 2>/dev/null || echo "000")

    if [[ "${status}" == "200" ]]; then
      log_ok "${name}: UP (HTTP ${status})"
    else
      log_warn "${name}: HTTP ${status} at ${url}"
    fi
  done

  rm -f "${ca_file}" "${client_cert}" "${client_key}"
  trap - EXIT
}

# ── Seed a test patient if the DB is empty ────────────────────────────────────
ensure_test_patient() {
  local patient_id="${PATIENT_ID:-f47ac10b-58cc-4372-a567-0e02b2c3d479}"
  local tenant="${TENANT_ID:-acme-health}"

  log_info "Ensuring test patient ${patient_id} exists in tenant ${tenant}..."

  docker exec hdim-demo-postgres psql -U healthdata -d patient_db -q -c "
    INSERT INTO patient_demographics (
      id, tenant_id, fhir_patient_id, mrn, first_name, last_name,
      date_of_birth, gender, active, deceased
    ) VALUES (
      '${patient_id}',
      '${tenant}',
      'fhir-${patient_id}',
      'LOAD-TEST-001',
      'Load',
      'Test',
      '1970-01-15',
      'male',
      true,
      false
    ) ON CONFLICT (id) DO NOTHING;
  " 2>/dev/null && log_ok "Test patient ready" || log_warn "Could not seed patient (may already exist or DB unreachable)"
}

# ── Run one scenario ──────────────────────────────────────────────────────────
run_scenario() {
  local name="$1"
  local script="$2"

  if [[ -n "${SCENARIO_FILTER}" && "${name}" != *"${SCENARIO_FILTER}"* ]]; then
    log_warn "Skipping ${name} (filter: ${SCENARIO_FILTER})"
    return 0
  fi

  log_section "Running: ${name}"
  local result_file="${RESULTS_DIR}/${name}_${TIMESTAMP}.json"
  mkdir -p "${RESULTS_DIR}"

  local cmd=(
    "${K6}" run
    --out "json=${result_file}"
    --summary-trend-stats "min,avg,med,p(90),p(95),p(99),max"
    -e "ENVIRONMENT=${ENVIRONMENT}"
    -e "TENANT_ID=${TENANT_ID:-acme-health}"
    -e "PATIENT_ID=${PATIENT_ID:-f47ac10b-58cc-4372-a567-0e02b2c3d479}"
    -e "BASE_URL_PATIENT=https://localhost:8084"
    -e "BASE_URL_CARE_GAP=https://localhost:8086"
    -e "BASE_URL_QUALITY_MEASURE=https://localhost:8087"
    -e "TLS_CA_CERT=${TLS_CA_CERT}"
    -e "TLS_CLIENT_CERT=${TLS_CLIENT_CERT}"
    -e "TLS_CLIENT_KEY=${TLS_CLIENT_KEY}"
  )

  if [[ "${SMOKE_MODE}" == "true" ]]; then
    cmd+=(-e "TEST_TYPE=smoke")
    log_info "Smoke mode: 1 VU, 1 iteration"
  else
    cmd+=(-e "TEST_TYPE=load")
  fi

  cmd+=("${script}")

  if "${cmd[@]}"; then
    log_ok "${name} PASSED"
    return 0
  else
    log_error "${name} FAILED (thresholds not met or script error)"
    EXIT_CODE=1
    return 1
  fi
}

# ── Main ──────────────────────────────────────────────────────────────────────
main() {
  log_section "HDIM k6 Load Tests — Demo Environment (mTLS)"

  extract_certs
  verify_services
  ensure_test_patient

  # Run scenarios in order
  local scenarios=(
    "patient-service|${SCRIPT_DIR}/scenarios/patient-service.js"
    "care-gap-service|${SCRIPT_DIR}/scenarios/care-gap-service.js"
    "quality-measure-service|${SCRIPT_DIR}/scenarios/quality-measure-service.js"
    "full-pipeline|${SCRIPT_DIR}/scenarios/full-pipeline.js"
  )

  for entry in "${scenarios[@]}"; do
    local name="${entry%%|*}"
    local script="${entry##*|}"
    run_scenario "${name}" "${script}" || true
  done

  log_section "Summary"
  echo "  Timestamp:   ${TIMESTAMP}"
  echo "  Environment: ${ENVIRONMENT}"
  echo "  Mode:        $([ "${SMOKE_MODE}" == "true" ] && echo "Smoke (1 VU)" || echo "Load (100 VUs)")"
  echo "  Results:     ${RESULTS_DIR}/"
  echo ""

  if [[ "${EXIT_CODE}" -eq 0 ]]; then
    log_ok "All scenarios passed."
  else
    log_error "One or more scenarios failed. See threshold output above."
  fi

  exit "${EXIT_CODE}"
}

main "$@"
