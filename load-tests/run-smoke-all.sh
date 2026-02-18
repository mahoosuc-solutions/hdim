#!/usr/bin/env bash
# =============================================================================
# run-smoke-all.sh — HDIM Service Validation Smoke Runner
# =============================================================================
#
# Runs k6 contract smoke tests against all demo-stack services.
# Each test: 1 VU × 3 iterations (~10-15s per service).
#
# Output:
#   - Colored terminal table (✅/❌ per service)
#   - validation/results/YYYY-MM-DD-HHmmss-smoke.md (persistent record)
#
# Usage:
#   ./load-tests/run-smoke-all.sh                          # All services
#   ./load-tests/run-smoke-all.sh --service fhir-service   # One service
#
# Prerequisites:
#   - Demo containers running (hdim-demo-patient etc.)
#   - k6 at /tmp/k6-v0.54.0-linux-amd64/k6 or on PATH
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RESULTS_DIR="${REPO_ROOT}/validation/results"
TIMESTAMP="$(date +%Y-%m-%d-%H%M%S)"
RESULTS_FILE="${RESULTS_DIR}/${TIMESTAMP}-smoke.md"
SERVICE_FILTER=""
EXIT_CODE=0

# ── Parse args ────────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --service) SERVICE_FILTER="${2:-}"; shift 2 ;;
    --help|-h) sed -n '2,20p' "$0" | sed 's/^# //'; exit 0 ;;
    *) echo "Unknown argument: $1. Use --help." >&2; exit 1 ;;
  esac
done

# ── Colours ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; BOLD='\033[1m'; RESET='\033[0m'

log_info()    { echo -e "${BLUE}[INFO]${RESET}  $*"; }
log_ok()      { echo -e "${GREEN}[PASS]${RESET}  $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${RESET}  $*"; }
log_error()   { echo -e "${RED}[FAIL]${RESET}  $*"; }
log_section() { echo -e "\n${BOLD}${BLUE}━━━ $* ━━━${RESET}\n"; }

# ── Find k6 ───────────────────────────────────────────────────────────────────
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

# ── Extract mTLS certs from hdim-demo-patient ─────────────────────────────────
extract_certs() {
  local container="hdim-demo-patient"
  if ! docker ps --format "{{.Names}}" | grep -q "^${container}$"; then
    log_error "Container ${container} is not running. Start demo stack first."
    exit 1
  fi

  TLS_CLIENT_CERT="$(docker exec "${container}" cat /etc/ssl/mtls/gateway-fhir-service/certificate.pem 2>/dev/null)"
  TLS_CLIENT_KEY="$(docker exec "${container}" cat /etc/ssl/mtls/gateway-fhir-service/private-key.pem 2>/dev/null)"

  if [[ -z "${TLS_CLIENT_CERT}" || -z "${TLS_CLIENT_KEY}" ]]; then
    log_error "Failed to extract mTLS certificates from ${container}."
    exit 1
  fi

  export TLS_CLIENT_CERT TLS_CLIENT_KEY
  log_ok "mTLS certificates extracted"
}

# ── Result tracking ───────────────────────────────────────────────────────────
declare -a RESULTS=()
declare -a FAILURES=()

# ── Run one smoke scenario ────────────────────────────────────────────────────
# Args: service-name  BASE_URL_VAR  base_url_value
run_smoke() {
  local service="$1"
  local base_url_var="$2"
  local base_url_val="$3"

  # Apply service filter if set
  if [[ -n "${SERVICE_FILTER}" && "${service}" != "${SERVICE_FILTER}" ]]; then
    return 0
  fi

  local smoke_file="${SCRIPT_DIR}/scenarios/smoke/${service}.js"
  if [[ ! -f "${smoke_file}" ]]; then
    log_warn "No smoke test for ${service} — skipping"
    RESULTS+=("⏭  ${service} | SKIPPED | no smoke test file")
    return 0
  fi

  local tmp_out
  tmp_out="$(mktemp /tmp/hdim-smoke-XXXXXX.txt)"

  local start_ms duration_ms
  start_ms="$(date +%s%3N)"

  local k6_exit=0
  "${K6}" run \
    --quiet \
    -e "TLS_CLIENT_CERT=${TLS_CLIENT_CERT}" \
    -e "TLS_CLIENT_KEY=${TLS_CLIENT_KEY}" \
    -e "TENANT_ID=${TENANT_ID:-acme-health}" \
    -e "PATIENT_ID=${PATIENT_ID:-f47ac10b-58cc-4372-a567-0e02b2c3d479}" \
    -e "${base_url_var}=${base_url_val}" \
    "${smoke_file}" > "${tmp_out}" 2>&1 || k6_exit=$?

  duration_ms=$(( $(date +%s%3N) - start_ms ))

  if [[ "${k6_exit}" -eq 0 ]]; then
    log_ok "${service} — ${duration_ms}ms"
    RESULTS+=("✅ | ${service} | ${duration_ms}ms | PASS")
    rm -f "${tmp_out}"
    return 0
  else
    log_error "${service} — FAILED (${duration_ms}ms)"
    local detail
    detail="$(tail -15 "${tmp_out}" | sed 's/^/    /')"
    RESULTS+=("❌ | ${service} | ${duration_ms}ms | FAIL")
    FAILURES+=("### ${service}\n\`\`\`\n${detail}\n\`\`\`\n")
    rm -f "${tmp_out}"
    EXIT_CODE=1
    return 1
  fi
}

# ── Write markdown results file ───────────────────────────────────────────────
write_results() {
  mkdir -p "${RESULTS_DIR}"

  local git_sha
  git_sha="$(git -C "${REPO_ROOT}" rev-parse --short HEAD 2>/dev/null || echo 'unknown')"

  local pass_count=0 fail_count=0 skip_count=0
  for r in "${RESULTS[@]}"; do
    case "${r}" in
      ✅*) pass_count=$(( pass_count + 1 )) ;;
      ❌*) fail_count=$(( fail_count + 1 )) ;;
      ⏭*) skip_count=$(( skip_count + 1 )) ;;
    esac
  done
  local total_count="${#RESULTS[@]}"

  {
    echo "# Service Validation Smoke Results"
    echo ""
    echo "**Date:** $(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "**Git:**  ${git_sha}"
    echo "**Mode:** Contract smoke (1 VU × 3 iterations)"
    echo ""
    echo "## Summary"
    echo ""
    echo "| Result | Count |"
    echo "|--------|-------|"
    echo "| ✅ PASS | ${pass_count} |"
    echo "| ❌ FAIL | ${fail_count} |"
    echo "| ⏭ SKIP | ${skip_count} |"
    echo "| Total  | ${total_count} |"
    echo ""
    echo "## Per-Service Results"
    echo ""
    echo "| Status | Service | Duration |"
    echo "|--------|---------|----------|"
    for r in "${RESULTS[@]}"; do
      IFS='|' read -r status svc dur _rest <<< "${r}"
      printf "| %s | %s | %s |\n" "${status}" "${svc}" "${dur}"
    done
    echo ""
    if [[ "${fail_count}" -gt 0 ]]; then
      echo "## Failure Details"
      echo ""
      for f in "${FAILURES[@]}"; do
        echo -e "${f}"
      done
    fi
  } > "${RESULTS_FILE}"

  log_info "Results written: ${RESULTS_FILE}"
}

# ── Main ──────────────────────────────────────────────────────────────────────
main() {
  log_section "HDIM Service Validation — Contract Smoke Tests"
  log_info "k6:        ${K6}"
  log_info "Timestamp: ${TIMESTAMP}"
  [[ -n "${SERVICE_FILTER}" ]] && log_info "Filter:    ${SERVICE_FILTER}"

  extract_certs

  log_section "Running Smoke Tests"

  run_smoke "patient-service"          "BASE_URL_PATIENT"  "https://localhost:8084" || true
  run_smoke "fhir-service"             "BASE_URL_FHIR"     "https://localhost:8085" || true
  run_smoke "care-gap-service"         "BASE_URL_CARE_GAP" "https://localhost:8086" || true
  run_smoke "quality-measure-service"  "BASE_URL_QUALITY"  "https://localhost:8087" || true
  run_smoke "audit-query-service"      "BASE_URL_AUDIT"    "https://localhost:8088" || true
  run_smoke "hcc-service"              "BASE_URL_HCC"      "https://localhost:8105" || true
  run_smoke "cql-engine-service"       "BASE_URL_CQL"      "https://localhost:8081" || true
  run_smoke "events-service"           "BASE_URL_EVENTS"   "https://localhost:8083" || true
  run_smoke "demo-seeding-service"     "BASE_URL_SEEDING"  "http://localhost:8098"  || true

  write_results

  log_section "Summary"
  printf "\n"
  for r in "${RESULTS[@]}"; do
    echo "  ${r}"
  done
  printf "\n"

  if [[ "${EXIT_CODE}" -eq 0 ]]; then
    log_ok "All smoke tests passed."
  else
    log_error "One or more smoke tests failed. See: ${RESULTS_FILE}"
  fi

  exit "${EXIT_CODE}"
}

main "$@"
