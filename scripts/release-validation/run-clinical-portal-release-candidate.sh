#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:-baseline}"
TIMESTAMP="${RELEASE_TIMESTAMP:-$(date -u +"%Y%m%dT%H%M%SZ")}"
LOG_DIR="${RELEASE_LOG_DIR:-logs/release-candidate/$TIMESTAMP}"

mkdir -p "$LOG_DIR"

run_step() {
  local name="$1"
  shift
  local log_file="$LOG_DIR/${name}.log"
  echo "[release-candidate] Running $name"
  echo "[release-candidate] Command: $*"
  "$@" 2>&1 | tee "$log_file"
}

show_help() {
  cat <<'EOF'
Clinical Portal Release Candidate Runner

Usage:
  bash scripts/release-validation/run-clinical-portal-release-candidate.sh [mode]

Modes:
  baseline  Run hygiene, lint, tests, build, and MCP validation
  smoke     Run the clinical portal smoke suite
  evidence  Build the MCP evidence pack
  go-no-go  Run the operator go/no-go gate
  full      Run baseline, smoke, evidence, and go-no-go in order

Environment:
  GO_NO_GO_MODE          permissive (default) or strict
  RELEASE_LOG_DIR        override output log directory
  RELEASE_TIMESTAMP      override UTC timestamp component

Artifacts:
  Command logs are written under logs/release-candidate/<timestamp>/
EOF
}

run_baseline() {
  run_step hygiene-audit npm run hygiene:audit
  run_step lint-all npm run lint:all
  run_step test-all npm run test:all
  run_step build-clinical-portal npm run build:clinical-portal
  run_step test-mcp npm run test:mcp
}

run_smoke() {
  run_step e2e-clinical-portal-smoke npm run e2e:clinical-portal:smoke
}

run_evidence() {
  run_step mcp-evidence-pack npm run mcp:evidence-pack -- --report-dir logs/mcp-reports --output-dir logs/mcp-reports
}

run_go_no_go() {
  run_step operator-go-no-go npm run mcp:operator:go-no-go -- --mode "${GO_NO_GO_MODE:-permissive}"
}

case "$MODE" in
  -h|--help|help)
    show_help
    ;;
  baseline)
    run_baseline
    ;;
  smoke)
    run_smoke
    ;;
  evidence)
    run_evidence
    ;;
  go-no-go)
    run_go_no_go
    ;;
  full)
    run_baseline
    run_smoke
    run_evidence
    run_go_no_go
    ;;
  *)
    echo "Unknown mode: $MODE" >&2
    show_help >&2
    exit 1
    ;;
esac
