#!/usr/bin/env bash
set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

REPORT_DIR="reports/measure-builder"
mkdir -p "$REPORT_DIR"

TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
SUMMARY_FILE="$REPORT_DIR/centralized-test-summary-$TIMESTAMP.md"
READINESS_LOG="$REPORT_DIR/readiness-$TIMESTAMP.log"
GROUP_A_LOG="$REPORT_DIR/group-a-$TIMESTAMP.log"
GROUP_B_LOG="$REPORT_DIR/group-b-$TIMESTAMP.log"

run_group() {
  local name="$1"
  local log_file="$2"
  shift 2

  echo "[$(date -u +%H:%M:%S)] Running $name" | tee -a "$log_file"
  "$@" >>"$log_file" 2>&1
  local status=$?
  echo "[$(date -u +%H:%M:%S)] $name exit=$status" | tee -a "$log_file"
  return $status
}

READINESS_STATUS=0
GROUP_A_STATUS=0
GROUP_B_STATUS=0

run_group "Operational Readiness" "$READINESS_LOG" bash -lc "npm run test:measure-builder:readiness" || READINESS_STATUS=$?
if [[ $READINESS_STATUS -eq 0 ]]; then
  run_group "Group A: Typecheck + Focused Jest" "$GROUP_A_LOG" bash -lc "npx tsc -p apps/clinical-portal/tsconfig.app.json --noEmit && npm run test:measure-builder:focused" || GROUP_A_STATUS=$?
  run_group "Group B: Focused Playwright" "$GROUP_B_LOG" bash -lc "npm run test:measure-builder:group-b" || GROUP_B_STATUS=$?
else
  GROUP_A_STATUS=1
  GROUP_B_STATUS=1
  echo "[$(date -u +%H:%M:%S)] Skipping Group A due to readiness failure" | tee -a "$GROUP_A_LOG"
  echo "[$(date -u +%H:%M:%S)] Skipping Group B due to readiness failure" | tee -a "$GROUP_B_LOG"
fi

{
  echo "# Measure Builder Centralized Test Summary"
  echo
  echo "- Timestamp (UTC): $TIMESTAMP"
  echo "- Readiness status: $READINESS_STATUS"
  echo "- Readiness log: $READINESS_LOG"
  echo "- Group A status: $GROUP_A_STATUS"
  echo "- Group B status: $GROUP_B_STATUS"
  echo "- Group A log: $GROUP_A_LOG"
  echo "- Group B log: $GROUP_B_LOG"
  echo
  echo "## Release Gate"
  if [[ $READINESS_STATUS -eq 0 && $GROUP_A_STATUS -eq 0 && $GROUP_B_STATUS -eq 0 ]]; then
    echo "PASS"
  else
    echo "FAIL"
  fi
  echo
  echo "## Notes"
  echo "- Group A validates compile + focused unit regression coverage."
  echo "- Group B validates metadata retry and field-error e2e behavior."
  echo "- Review logs for root-cause details when Group B fails in constrained environments."
} > "$SUMMARY_FILE"

echo "Centralized summary written to: $SUMMARY_FILE"

if [[ $READINESS_STATUS -ne 0 || $GROUP_A_STATUS -ne 0 || $GROUP_B_STATUS -ne 0 ]]; then
  exit 1
fi
