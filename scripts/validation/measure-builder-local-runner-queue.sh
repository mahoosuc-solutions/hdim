#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

REPORT_DIR="reports/measure-builder"
mkdir -p "$REPORT_DIR"

TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
QUEUE_LOG="$REPORT_DIR/local-runner-queue-$TIMESTAMP.log"
QUEUE_SUMMARY="$REPORT_DIR/local-runner-queue-summary-$TIMESTAMP.md"

PORT="${PORT:-4210}"
PW_CHROMIUM_NO_SANDBOX="${PW_CHROMIUM_NO_SANDBOX:-1}"
QUEUE_MAX_ATTEMPTS="${QUEUE_MAX_ATTEMPTS:-3}"
QUEUE_SLEEP_SECONDS="${QUEUE_SLEEP_SECONDS:-45}"

log() {
  echo "[$(date -u +%H:%M:%S)] $*" | tee -a "$QUEUE_LOG"
}

run_step() {
  local name="$1"
  shift
  log "START $name"
  set +e
  "$@" >>"$QUEUE_LOG" 2>&1
  local status=$?
  set -e
  log "END $name exit=$status"
  return $status
}

GROUP_A_STATUS=1
GROUP_B_STATUS=1
CENTRALIZED_STATUS=1
READINESS_STATUS=1
ATTEMPT=0

while [[ $ATTEMPT -lt $QUEUE_MAX_ATTEMPTS ]]; do
  ATTEMPT=$((ATTEMPT + 1))
  log "QUEUE ATTEMPT $ATTEMPT/$QUEUE_MAX_ATTEMPTS"

  run_step "Operational Readiness" bash -lc "PORT=$PORT PW_CHROMIUM_NO_SANDBOX=$PW_CHROMIUM_NO_SANDBOX npm run test:measure-builder:readiness" \
    && READINESS_STATUS=0 || READINESS_STATUS=$?

  if [[ $READINESS_STATUS -eq 0 ]]; then
    run_step "Group A" bash -lc "npm run test:measure-builder:group-a" && GROUP_A_STATUS=0 || GROUP_A_STATUS=$?

    run_step "Group B local" bash -lc "PORT=$PORT PW_CHROMIUM_NO_SANDBOX=$PW_CHROMIUM_NO_SANDBOX npm run test:measure-builder:group-b" \
      && GROUP_B_STATUS=0 || GROUP_B_STATUS=$?

    run_step "Centralized local" bash -lc "PORT=$PORT PW_CHROMIUM_NO_SANDBOX=$PW_CHROMIUM_NO_SANDBOX npm run test:measure-builder:centralized" \
      && CENTRALIZED_STATUS=0 || CENTRALIZED_STATUS=$?
  else
    GROUP_A_STATUS=1
    GROUP_B_STATUS=1
    CENTRALIZED_STATUS=1
    log "Skipping test groups due to readiness failure"
  fi

  if [[ $READINESS_STATUS -eq 0 && $GROUP_A_STATUS -eq 0 && $GROUP_B_STATUS -eq 0 && $CENTRALIZED_STATUS -eq 0 ]]; then
    log "QUEUE PASS on attempt $ATTEMPT"
    break
  fi

  if [[ $ATTEMPT -lt $QUEUE_MAX_ATTEMPTS ]]; then
    log "QUEUE RETRY after ${QUEUE_SLEEP_SECONDS}s"
    sleep "$QUEUE_SLEEP_SECONDS"
  fi
done

{
  echo "# Measure Builder Local Runner Queue Summary"
  echo
  echo "- Timestamp (UTC): $TIMESTAMP"
  echo "- Attempts: $ATTEMPT/$QUEUE_MAX_ATTEMPTS"
  echo "- Port: $PORT"
  echo "- PW_CHROMIUM_NO_SANDBOX: $PW_CHROMIUM_NO_SANDBOX"
  echo "- Readiness status: $READINESS_STATUS"
  echo "- Group A status: $GROUP_A_STATUS"
  echo "- Group B status: $GROUP_B_STATUS"
  echo "- Centralized status: $CENTRALIZED_STATUS"
  echo "- Queue log: $QUEUE_LOG"
  echo
  echo "## Result"
  if [[ $READINESS_STATUS -eq 0 && $GROUP_A_STATUS -eq 0 && $GROUP_B_STATUS -eq 0 && $CENTRALIZED_STATUS -eq 0 ]]; then
    echo "PASS"
  else
    echo "FAIL"
  fi
} > "$QUEUE_SUMMARY"

log "Queue summary written to: $QUEUE_SUMMARY"

if [[ $READINESS_STATUS -ne 0 || $GROUP_A_STATUS -ne 0 || $GROUP_B_STATUS -ne 0 || $CENTRALIZED_STATUS -ne 0 ]]; then
  exit 1
fi
