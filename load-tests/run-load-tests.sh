#!/usr/bin/env bash
# =============================================================================
# run-load-tests.sh — HDIM k6 Load Test Runner
# =============================================================================
#
# Runs all k6 load test scenarios against the 4 core pilot services and writes
# JSON results to load-tests/results/.
#
# Usage:
#   ./load-tests/run-load-tests.sh                # Full load test (100 VUs)
#   ./load-tests/run-load-tests.sh --smoke        # Smoke test (1 VU, 1 iteration)
#   ./load-tests/run-load-tests.sh --scenario patient   # Run 1 scenario only
#   ./load-tests/run-load-tests.sh --scenario pipeline  # Run pipeline only
#
# Environment overrides:
#   AUTH_TOKEN          Real JWT token (required for authenticated environments)
#   TENANT_ID           Tenant ID (default: test-tenant-perf)
#   PATIENT_ID          Patient UUID for test data
#   BASE_URL_PATIENT    Override patient-service base URL
#   BASE_URL_CARE_GAP   Override care-gap-service base URL
#   BASE_URL_QUALITY_MEASURE  Override quality-measure-service base URL
#   ENVIRONMENT         Tag label for results (default: local)
#
# Prerequisites:
#   brew install k6      (macOS)
#   sudo snap install k6 (Ubuntu/WSL2)
#   https://k6.io/docs/getting-started/installation/
# =============================================================================

set -euo pipefail

# ── Script location ───────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/results"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

# ── Defaults ──────────────────────────────────────────────────────────────────
SMOKE_MODE=false
SCENARIO_FILTER=""
ENVIRONMENT="${ENVIRONMENT:-local}"
EXIT_CODE=0

# ── Parse arguments ───────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --smoke)
      SMOKE_MODE=true
      shift
      ;;
    --scenario)
      SCENARIO_FILTER="${2:-}"
      shift 2
      ;;
    --help|-h)
      sed -n '2,40p' "$0" | sed 's/^# //'
      exit 0
      ;;
    *)
      echo "Unknown argument: $1. Use --help for usage." >&2
      exit 1
      ;;
  esac
done

# ── Colour output helpers ─────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
RESET='\033[0m'

log_info()    { echo -e "${BLUE}[INFO]${RESET}  $*"; }
log_ok()      { echo -e "${GREEN}[PASS]${RESET}  $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${RESET}  $*"; }
log_error()   { echo -e "${RED}[FAIL]${RESET}  $*"; }
log_section() { echo -e "\n${BOLD}${BLUE}━━━ $* ━━━${RESET}\n"; }

# ── Check k6 is installed ─────────────────────────────────────────────────────
check_k6() {
  if ! command -v k6 &>/dev/null; then
    log_error "k6 is not installed."
    echo ""
    echo "Install k6:"
    echo "  macOS:       brew install k6"
    echo "  Ubuntu/WSL2: sudo snap install k6"
    echo "  Docker:      docker run --rm -i grafana/k6 run - < script.js"
    echo "  More:        https://k6.io/docs/getting-started/installation/"
    echo ""
    exit 1
  fi

  local k6_version
  k6_version="$(k6 version 2>&1 | head -1)"
  log_info "k6 found: ${k6_version}"
}

# ── Ensure results directory exists ──────────────────────────────────────────
ensure_results_dir() {
  mkdir -p "${RESULTS_DIR}"
  log_info "Results directory: ${RESULTS_DIR}"
}

# ── Build k6 command ──────────────────────────────────────────────────────────
build_k6_cmd() {
  local scenario_name="$1"
  local script_path="$2"
  local result_file="${RESULTS_DIR}/${scenario_name}_${TIMESTAMP}.json"

  local cmd=(
    k6 run
    --out "json=${result_file}"
    --summary-trend-stats "min,avg,med,p(90),p(95),p(99),max"
    -e "ENVIRONMENT=${ENVIRONMENT}"
  )

  if [[ "${SMOKE_MODE}" == "true" ]]; then
    cmd+=(-e "TEST_TYPE=smoke")
    log_info "Smoke mode: 1 VU, 1 iteration"
  else
    cmd+=(-e "TEST_TYPE=load")
  fi

  # Forward optional environment overrides
  [[ -n "${AUTH_TOKEN:-}"              ]] && cmd+=(-e "AUTH_TOKEN=${AUTH_TOKEN}")
  [[ -n "${TENANT_ID:-}"              ]] && cmd+=(-e "TENANT_ID=${TENANT_ID}")
  [[ -n "${PATIENT_ID:-}"             ]] && cmd+=(-e "PATIENT_ID=${PATIENT_ID}")
  [[ -n "${BASE_URL_PATIENT:-}"       ]] && cmd+=(-e "BASE_URL_PATIENT=${BASE_URL_PATIENT}")
  [[ -n "${BASE_URL_CARE_GAP:-}"      ]] && cmd+=(-e "BASE_URL_CARE_GAP=${BASE_URL_CARE_GAP}")
  [[ -n "${BASE_URL_QUALITY_MEASURE:-}" ]] && cmd+=(-e "BASE_URL_QUALITY_MEASURE=${BASE_URL_QUALITY_MEASURE}")

  cmd+=("${script_path}")
  echo "${cmd[@]}"
}

# ── Run a single scenario ─────────────────────────────────────────────────────
run_scenario() {
  local name="$1"
  local script="$2"

  # Skip if scenario filter is set and doesn't match
  if [[ -n "${SCENARIO_FILTER}" && "${name}" != *"${SCENARIO_FILTER}"* ]]; then
    log_warn "Skipping ${name} (filter: ${SCENARIO_FILTER})"
    return 0
  fi

  log_section "Running: ${name}"
  log_info "Script: ${script}"

  local k6_cmd
  k6_cmd="$(build_k6_cmd "${name}" "${script}")"

  if eval "${k6_cmd}"; then
    log_ok "${name} PASSED"
    return 0
  else
    log_error "${name} FAILED (k6 thresholds not met or script error)"
    EXIT_CODE=1
    return 1
  fi
}

# ── Print summary ─────────────────────────────────────────────────────────────
print_summary() {
  log_section "Load Test Summary"

  echo -e "  Timestamp:    ${TIMESTAMP}"
  echo -e "  Environment:  ${ENVIRONMENT}"
  echo -e "  Mode:         $([ "${SMOKE_MODE}" == "true" ] && echo "Smoke (1 VU)" || echo "Load (100 VUs)")"
  echo -e "  Results:      ${RESULTS_DIR}/"
  echo ""

  if [[ "${EXIT_CODE}" -eq 0 ]]; then
    log_ok "All scenarios passed. SLO targets met."
  else
    log_error "One or more scenarios failed. Review threshold output above."
    echo ""
    echo "Common causes:"
    echo "  - Services not running: docker compose -f docker-compose.minimal-clinical.yml up -d"
    echo "  - Expired AUTH_TOKEN: regenerate and pass via -e AUTH_TOKEN=..."
    echo "  - Resources under-provisioned: reduce VU count or increase service memory"
  fi

  echo ""
  echo "Result files:"
  ls -lh "${RESULTS_DIR}"/*.json 2>/dev/null | tail -4 || echo "  (no JSON files yet)"
}

# ── Main ──────────────────────────────────────────────────────────────────────
main() {
  log_section "HDIM k6 Load Test Suite"
  check_k6
  ensure_results_dir

  # Scenario registry: name -> relative script path
  declare -A SCENARIOS=(
    [patient-service]="${SCRIPT_DIR}/scenarios/patient-service.js"
    [care-gap-service]="${SCRIPT_DIR}/scenarios/care-gap-service.js"
    [quality-measure-service]="${SCRIPT_DIR}/scenarios/quality-measure-service.js"
    [full-pipeline]="${SCRIPT_DIR}/scenarios/full-pipeline.js"
  )

  # Run in a deterministic order
  local ordered_scenarios=(
    patient-service
    care-gap-service
    quality-measure-service
    full-pipeline
  )

  for scenario_name in "${ordered_scenarios[@]}"; do
    run_scenario "${scenario_name}" "${SCENARIOS[${scenario_name}]}" || true
  done

  print_summary
  exit "${EXIT_CODE}"
}

main "$@"
