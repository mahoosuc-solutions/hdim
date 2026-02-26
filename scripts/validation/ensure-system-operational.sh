#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

REPORT_DIR="reports/measure-builder"
mkdir -p "$REPORT_DIR"

TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
READINESS_LOG="$REPORT_DIR/operational-readiness-$TIMESTAMP.log"

PORT="${PORT:-4210}"
BASE_URL="${BASE_URL:-http://localhost:${PORT}}"
CHECK_BASE_URL="${BASE_URL/localhost/127.0.0.1}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
TENANT_ID="${TENANT_ID:-acme-health}"
AUTO_START_CLINICAL_PORTAL="${AUTO_START_CLINICAL_PORTAL:-1}"
READINESS_TIMEOUT_SECONDS="${READINESS_TIMEOUT_SECONDS:-120}"
CLINICAL_PORTAL_START_CMD="${CLINICAL_PORTAL_START_CMD:-npx nx run clinical-portal:serve --port=${PORT}}"
CLINICAL_PORTAL_START_FALLBACK_CMD="${CLINICAL_PORTAL_START_FALLBACK_CMD:-npm run nx -- run clinical-portal:serve --port=${PORT}}"
CLINICAL_PORTAL_STATIC_DIR="${CLINICAL_PORTAL_STATIC_DIR:-dist/apps/clinical-portal/browser}"
CLINICAL_PORTAL_STATIC_FALLBACK_CMD="${CLINICAL_PORTAL_STATIC_FALLBACK_CMD:-node scripts/validation/spa-static-server.mjs --port ${PORT} --dir ${CLINICAL_PORTAL_STATIC_DIR}}"

is_true() {
  case "${1,,}" in
    1|true|yes) return 0 ;;
    *) return 1 ;;
  esac
}

log() {
  echo "[$(date -u +%H:%M:%S)] $*" | tee -a "$READINESS_LOG"
}

http_status() {
  local url="$1"
  shift || true
  curl -sS -o /dev/null -w '%{http_code}' --max-time 8 --connect-timeout 3 "$url" "$@" 2>/dev/null || echo "000"
}

wait_for_http_ok() {
  local url="$1"
  local timeout_seconds="$2"
  local elapsed=0

  while [[ $elapsed -lt $timeout_seconds ]]; do
    local status
    status="$(http_status "$url")"
    if [[ "$status" == "200" ]]; then
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done

  return 1
}

ui_urls() {
  if [[ "$CHECK_BASE_URL" == "$BASE_URL" ]]; then
    printf '%s\n' "$BASE_URL"
  else
    printf '%s\n' "$BASE_URL" "$CHECK_BASE_URL"
  fi
}

wait_for_ui_ok() {
  local timeout_seconds="$1"
  local elapsed=0

  while [[ $elapsed -lt $timeout_seconds ]]; do
    while IFS= read -r url; do
      local status
      status="$(http_status "$url")"
      if [[ "$status" == "200" ]]; then
        return 0
      fi
    done < <(ui_urls)
    sleep 2
    elapsed=$((elapsed + 2))
  done

  return 1
}

wait_for_http_ok_or_process() {
  local url="$1"
  local timeout_seconds="$2"
  local pid="$3"
  local startup_log="$4"
  local elapsed=0

  while [[ $elapsed -lt $timeout_seconds ]]; do
    local status
    status="$(http_status "$url")"
    if [[ "$status" == "200" ]]; then
      return 0
    fi

    if [[ -n "$pid" ]] && ! kill -0 "$pid" 2>/dev/null; then
      log "Startup process ${pid} exited before readiness."
      log "Startup log tail:"
      tail -n 40 "$startup_log" | sed 's/^/[startup] /' | tee -a "$READINESS_LOG"
      return 2
    fi

    sleep 2
    elapsed=$((elapsed + 2))
  done

  return 1
}

wait_for_ui_ok_or_process() {
  local timeout_seconds="$1"
  local pid="$2"
  local startup_log="$3"
  local elapsed=0

  while [[ $elapsed -lt $timeout_seconds ]]; do
    while IFS= read -r url; do
      local status
      status="$(http_status "$url")"
      if [[ "$status" == "200" ]]; then
        return 0
      fi
    done < <(ui_urls)

    if [[ -n "$pid" ]] && ! kill -0 "$pid" 2>/dev/null; then
      log "Startup process ${pid} exited before readiness."
      log "Startup log tail:"
      tail -n 40 "$startup_log" | sed 's/^/[startup] /' | tee -a "$READINESS_LOG"
      return 2
    fi

    sleep 2
    elapsed=$((elapsed + 2))
  done

  return 1
}

port_in_use() {
  if command -v ss >/dev/null 2>&1; then
    ss -ltn "sport = :${PORT}" 2>/dev/null | tail -n +2 | grep -q .
    return
  fi

  if command -v lsof >/dev/null 2>&1; then
    lsof -iTCP:"${PORT}" -sTCP:LISTEN -n -P >/dev/null 2>&1
    return
  fi

  return 1
}

find_port_owner() {
  if command -v lsof >/dev/null 2>&1; then
    lsof -iTCP:"${PORT}" -sTCP:LISTEN -n -P 2>/dev/null | awk 'NR>1 {print $1" "$2}' | head -n 1
    return
  fi

  if command -v ss >/dev/null 2>&1; then
    ss -ltnp "sport = :${PORT}" 2>/dev/null | awk 'NR>1 {print $NF}' | head -n 1
    return
  fi

  echo "unknown"
}

is_clinical_portal_owner() {
  if ! command -v lsof >/dev/null 2>&1; then
    return 1
  fi

  local pid
  pid="$(lsof -iTCP:"${PORT}" -sTCP:LISTEN -n -P 2>/dev/null | awk 'NR>1 {print $2}' | head -n 1 || true)"
  if [[ -z "$pid" ]]; then
    return 1
  fi

  local cmdline
  cmdline="$(ps -p "$pid" -o args= 2>/dev/null || true)"
  [[ "$cmdline" == *"clinical-portal:serve"* ]] || [[ "$cmdline" == *"apps/clinical-portal"* ]]
}

STARTUP_PID=""
STARTUP_LOG=""
STARTUP_MODE=""

start_fallback_only() {
  local startup_log_fallback="$REPORT_DIR/clinical-portal-startup-$TIMESTAMP-fallback-retry.log"
  log "Retrying startup with fallback command: ${CLINICAL_PORTAL_START_FALLBACK_CMD}"
  STARTUP_MODE="fallback"
  STARTUP_LOG="$startup_log_fallback"
  nohup bash -lc "$CLINICAL_PORTAL_START_FALLBACK_CMD" >"$STARTUP_LOG" 2>&1 &
  STARTUP_PID="$!"
  log "fallback startup pid: ${STARTUP_PID}"
  log "fallback startup log: ${STARTUP_LOG}"
}

start_static_fallback_only() {
  local startup_log_static="$REPORT_DIR/clinical-portal-startup-$TIMESTAMP-static-retry.log"
  if [[ ! -f "${CLINICAL_PORTAL_STATIC_DIR}/index.html" ]]; then
    log "Static fallback unavailable: ${CLINICAL_PORTAL_STATIC_DIR}/index.html not found"
    return 1
  fi
  log "Retrying startup with static fallback command: ${CLINICAL_PORTAL_STATIC_FALLBACK_CMD}"
  STARTUP_MODE="static-fallback"
  STARTUP_LOG="$startup_log_static"
  nohup bash -lc "$CLINICAL_PORTAL_STATIC_FALLBACK_CMD" >"$STARTUP_LOG" 2>&1 &
  STARTUP_PID="$!"
  log "static fallback startup pid: ${STARTUP_PID}"
  log "static fallback startup log: ${STARTUP_LOG}"
  return 0
}

start_clinical_portal() {
  local startup_log_primary="$REPORT_DIR/clinical-portal-startup-$TIMESTAMP-primary.log"
  local startup_log_fallback="$REPORT_DIR/clinical-portal-startup-$TIMESTAMP-fallback.log"
  local startup_log_static="$REPORT_DIR/clinical-portal-startup-$TIMESTAMP-static.log"
  local cmd pid

  log "Starting clinical-portal on port ${PORT}"

  for cmd in "$CLINICAL_PORTAL_START_CMD" "$CLINICAL_PORTAL_START_FALLBACK_CMD" "$CLINICAL_PORTAL_STATIC_FALLBACK_CMD"; do
    if [[ "$cmd" == "$CLINICAL_PORTAL_START_CMD" ]]; then
      STARTUP_LOG="$startup_log_primary"
      STARTUP_MODE="primary"
      log "Start command (primary): ${cmd}"
    elif [[ "$cmd" == "$CLINICAL_PORTAL_START_FALLBACK_CMD" ]]; then
      STARTUP_LOG="$startup_log_fallback"
      STARTUP_MODE="fallback"
      log "Primary startup exited quickly; trying fallback command: ${cmd}"
    else
      STARTUP_LOG="$startup_log_static"
      STARTUP_MODE="static-fallback"
      if [[ ! -f "${CLINICAL_PORTAL_STATIC_DIR}/index.html" ]]; then
        log "Skipping static fallback: ${CLINICAL_PORTAL_STATIC_DIR}/index.html not found"
        continue
      fi
      log "Primary/fallback startup exited quickly; trying static fallback command: ${cmd}"
    fi

    nohup bash -lc "$cmd" >"$STARTUP_LOG" 2>&1 &
    pid="$!"
    sleep 4

    if kill -0 "$pid" 2>/dev/null; then
      STARTUP_PID="$pid"
      log "clinical-portal startup pid: ${STARTUP_PID}"
      log "clinical-portal start log: ${STARTUP_LOG}"
      return 0
    fi

    log "Startup command exited immediately: ${cmd}"
    tail -n 20 "$STARTUP_LOG" | sed 's/^/[startup] /' | tee -a "$READINESS_LOG"
  done

  STARTUP_PID=""
  STARTUP_LOG="$startup_log_static"
  STARTUP_MODE="none"
  return 1
}

check_gateway_ready() {
  local health_status
  health_status="$(http_status "${GATEWAY_URL}/actuator/health")"
  [[ "$health_status" == "200" ]]
}

check_ui_route_ready() {
  local route_status
  route_status="$(http_status "${BASE_URL}/measure-builder")"
  [[ "$route_status" == "200" ]]
}

status_allowed() {
  local status="$1"
  shift
  for allowed in "$@"; do
    if [[ "$status" == "$allowed" ]]; then
      return 0
    fi
  done
  return 1
}

check_core_api_ready() {
  local quality_status care_gap_status
  quality_status="$(http_status "${GATEWAY_URL}/quality-measure/results?page=0&size=1" -H "X-Tenant-ID: ${TENANT_ID}")"
  care_gap_status="$(http_status "${GATEWAY_URL}/care-gap/api/v1/care-gaps?page=0&size=1" -H "X-Tenant-ID: ${TENANT_ID}")"

  status_allowed "$quality_status" 200 400 401 403 && status_allowed "$care_gap_status" 200 400 401 403
}

fail_readiness() {
  local code="$1"
  local message="$2"
  log "FAIL ${code}: ${message}"
  log "Remediation:"
  log "- Ensure clinical-portal serves on http://localhost:${PORT}"
  log "- Ensure gateway is available on ${GATEWAY_URL}"
  log "- Re-run: PORT=${PORT} BASE_URL=${BASE_URL} npm run test:measure-builder:group-b"
  exit 1
}

log "Readiness start"
log "BASE_URL=${BASE_URL}"
log "CHECK_BASE_URL=${CHECK_BASE_URL}"
log "PORT=${PORT}"
log "GATEWAY_URL=${GATEWAY_URL}"
log "TENANT_ID=${TENANT_ID}"
log "AUTO_START_CLINICAL_PORTAL=${AUTO_START_CLINICAL_PORTAL}"
log "CLINICAL_PORTAL_START_CMD=${CLINICAL_PORTAL_START_CMD}"
log "CLINICAL_PORTAL_START_FALLBACK_CMD=${CLINICAL_PORTAL_START_FALLBACK_CMD}"
log "CLINICAL_PORTAL_STATIC_FALLBACK_CMD=${CLINICAL_PORTAL_STATIC_FALLBACK_CMD}"

NON_CLINICAL_PORT_OWNER=""

if wait_for_ui_ok 4; then
  log "UI already reachable at ${BASE_URL} or ${CHECK_BASE_URL}"
else
  if port_in_use; then
    local_owner="$(find_port_owner)"
    if is_clinical_portal_owner; then
      log "Port ${PORT} is in use by clinical-portal-like process; waiting for readiness"
    else
      NON_CLINICAL_PORT_OWNER="${local_owner:-unknown}"
      log "Port ${PORT} in use by non-clinical-portal process (${NON_CLINICAL_PORT_OWNER}); waiting for HTTP readiness before declaring conflict"
    fi
  elif is_true "$AUTO_START_CLINICAL_PORTAL"; then
    if ! start_clinical_portal; then
      fail_readiness "STARTUP_FAILED" "Clinical Portal startup commands failed immediately"
    fi
  else
    fail_readiness "UI_NOT_RUNNING" "Clinical Portal is unreachable at ${BASE_URL} and ${CHECK_BASE_URL} and auto-start is disabled"
  fi

  if wait_for_ui_ok_or_process "$READINESS_TIMEOUT_SECONDS" "$STARTUP_PID" "$STARTUP_LOG"; then
    :
  else
    readiness_wait_status=$?
    if [[ $readiness_wait_status -eq 2 ]]; then
      if [[ "$STARTUP_MODE" == "primary" ]] && [[ "$CLINICAL_PORTAL_START_FALLBACK_CMD" != "$CLINICAL_PORTAL_START_CMD" ]]; then
        start_fallback_only
        if ! wait_for_ui_ok_or_process "$READINESS_TIMEOUT_SECONDS" "$STARTUP_PID" "$STARTUP_LOG"; then
          if start_static_fallback_only; then
            if ! wait_for_ui_ok_or_process "$READINESS_TIMEOUT_SECONDS" "$STARTUP_PID" "$STARTUP_LOG"; then
              fail_readiness "STARTUP_FAILED" "Clinical Portal static fallback startup process exited before becoming reachable (see ${STARTUP_LOG})"
            fi
          else
            fail_readiness "STARTUP_FAILED" "Clinical Portal fallback startup process exited before becoming reachable (see ${STARTUP_LOG})"
          fi
        fi
      else
        fail_readiness "STARTUP_FAILED" "Clinical Portal startup process exited before becoming reachable (see ${STARTUP_LOG})"
      fi
    else
      if [[ -n "$NON_CLINICAL_PORT_OWNER" ]]; then
        fail_readiness "PORT_CONFLICT_${PORT}" "Port ${PORT} remained owned by non-clinical process (${NON_CLINICAL_PORT_OWNER}) and UI did not become reachable within ${READINESS_TIMEOUT_SECONDS}s"
      fi
      fail_readiness "UI_NOT_RUNNING" "Clinical Portal did not become reachable at ${BASE_URL} or ${CHECK_BASE_URL} within ${READINESS_TIMEOUT_SECONDS}s"
    fi
  fi
  log "UI reachable at ${BASE_URL} or ${CHECK_BASE_URL}"
fi

if ! check_ui_route_ready; then
  fail_readiness "UI_ROUTE_NOT_FOUND" "Route ${CHECK_BASE_URL}/measure-builder is not reachable (likely non-SPA static server)"
fi
log "UI route ready at ${CHECK_BASE_URL}/measure-builder"

if ! check_gateway_ready; then
  fail_readiness "GATEWAY_DOWN" "Gateway health check failed at ${GATEWAY_URL}/actuator/health"
fi
log "Gateway ready at ${GATEWAY_URL}"

if ! check_core_api_ready; then
  fail_readiness "CORE_API_UNREACHABLE" "Core API readiness check failed for quality-measure and/or care-gap"
fi
log "Core APIs ready (quality-measure + care-gap)"

log "Readiness PASS"
log "Readiness log: ${READINESS_LOG}"
