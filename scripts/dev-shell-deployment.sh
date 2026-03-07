#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

PID_DIR=".tmp/dev-shell"
mkdir -p "${PID_DIR}"

cleanup_pid_files() {
  rm -f "${PID_DIR}/shell-app.pid"
}

start_target() {
  local name="$1"
  shift
  "$@" &
  local pid=$!
  echo "${pid}" > "${PID_DIR}/${name}.pid"
  echo "[dev-shell-deployment] ${name} started with pid ${pid}"
}

trap cleanup_pid_files EXIT INT TERM

wait_for_stack_health() {
  local timeout_seconds="${DEV_SHELL_HEALTH_TIMEOUT_SECONDS:-300}"
  local start_time
  start_time="$(date +%s)"
  local containers=(
    hdim-demo-postgres
    hdim-demo-redis
    hdim-demo-kafka
    hdim-demo-fhir
    hdim-demo-patient
    hdim-demo-care-gap
    hdim-demo-quality-measure
    hdim-demo-events
    hdim-demo-gateway-fhir
    hdim-demo-gateway-admin
    hdim-demo-gateway-clinical
    hdim-demo-gateway-edge
    hdim-demo-seeding
    hdim-demo-ops
  )

  echo "[dev-shell-deployment] Waiting for critical demo containers to be healthy (timeout ${timeout_seconds}s)..."
  while true; do
    local pending=0
    for container in "${containers[@]}"; do
      local status
      status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container}" 2>/dev/null || echo "missing")"
      if [[ "${status}" != "healthy" && "${status}" != "running" ]]; then
        pending=$((pending + 1))
      fi
    done

    if [[ "${pending}" -eq 0 ]]; then
      echo "[dev-shell-deployment] Critical containers are healthy/running."
      return 0
    fi

    if (( "$(date +%s)" - start_time >= timeout_seconds )); then
      echo "[dev-shell-deployment] Timed out waiting for container health."
      docker compose -f docker-compose.demo.yml ps || true
      return 1
    fi

    sleep 3
  done
}

echo "[dev-shell-deployment] Starting demo stack (docker-compose.demo.yml)..."
if [[ "${DEV_SHELL_BUILD_OPS:-0}" == "1" ]]; then
  docker compose -f docker-compose.demo.yml up -d --build ops-service || true
  docker compose -f docker-compose.demo.yml up -d || true
else
  docker compose -f docker-compose.demo.yml up -d || true
fi

if [[ "${DEV_SHELL_WAIT_HEALTH:-1}" == "1" ]]; then
  wait_for_stack_health
fi

echo "[dev-shell-deployment] Current stack status:"
docker compose -f docker-compose.demo.yml ps || true

echo "[dev-shell-deployment] Starting shell-app on :4300 with mfeDeployment remote..."
start_target shell-app npx nx serve shell-app --configuration=development --port=4300 --publicHost=http://localhost:4300 --staticRemotesPort=4400 --devRemotes=mfeDeployment
wait "$(cat "${PID_DIR}/shell-app.pid")"
