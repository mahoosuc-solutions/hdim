#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="docker-compose.demo.yml"
PORTAL_URL="${PORTAL_URL:-http://localhost:4400}"
BACKEND_URL="${BACKEND_URL:-http://127.0.0.1:18080}"

function log() {
  printf "[%s] %s\n" "$(date --iso-8601=seconds)" "$*"
}

function inspect_stack() {
  log "Verifying Docker compose stack (${COMPOSE_FILE})..."
  docker compose -f "${COMPOSE_FILE}" ps
}

function check_endpoint() {
  local url="$1"
  local label="$2"
  local attempts=5
  for i in $(seq 1 ${attempts}); do
    http_code=$(curl -s -o /tmp/ensure-endpoint -D /tmp/ensure-headers -w "%{http_code}" --max-time 5 "${url}")
    if [[ "${http_code}" =~ ^(20[01]|401|403|404)$ ]]; then
      log "${label} reachable (${http_code}) at ${url}"
      return 0
    fi
    log "Retrying ${label} (${i}/${attempts})... code=${http_code}"
    sleep 2
  done
  log "ERROR: ${label} unreachable at ${url} (last code=${http_code})" >&2
  return 1
}

function run_checks() {
  check_endpoint "${BACKEND_URL}/actuator/health" "gateway-edge health"
  check_endpoint "${BACKEND_URL}/api/v1/auth/health" "auth health"
  check_endpoint "${PORTAL_URL}/login" "clinical portal UI"
}

log "Starting demo stack readiness checks"
inspect_stack
run_checks
log "Demo stack is operational"
