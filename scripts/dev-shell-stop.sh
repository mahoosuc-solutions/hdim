#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

PID_DIR=".tmp/dev-shell"

terminate_pid() {
  local pid="$1"
  local expected="$2"
  local pid_file="$3"

  if ! ps -p "${pid}" > /dev/null 2>&1; then
    echo "[dev-shell-stop] PID ${pid} is not running; removing stale ${pid_file}"
    rm -f "${pid_file}"
    return 0
  fi

  local cmd
  cmd="$(ps -p "${pid}" -o args= || true)"
  if [[ -z "${cmd}" || "${cmd}" != *"${expected}"* ]]; then
    echo "[dev-shell-stop] Skipping PID ${pid}; command does not match expected token '${expected}'"
    echo "[dev-shell-stop] Observed command: ${cmd}"
    return 0
  fi

  echo "[dev-shell-stop] Stopping ${expected} (pid ${pid})"
  kill -TERM "${pid}" || true

  for _ in {1..10}; do
    if ! ps -p "${pid}" > /dev/null 2>&1; then
      rm -f "${pid_file}"
      return 0
    fi
    sleep 1
  done

  echo "[dev-shell-stop] PID ${pid} still running; force killing"
  kill -KILL "${pid}" || true
  rm -f "${pid_file}"
}

stop_from_pid_file() {
  local name="$1"
  local expected="$2"
  local pid_file="${PID_DIR}/${name}.pid"

  if [[ ! -f "${pid_file}" ]]; then
    echo "[dev-shell-stop] No pid file for ${name}"
    return 0
  fi

  local pid
  pid="$(cat "${pid_file}" 2>/dev/null || true)"
  if [[ ! "${pid}" =~ ^[0-9]+$ ]]; then
    echo "[dev-shell-stop] Invalid pid value in ${pid_file}; removing file"
    rm -f "${pid_file}"
    return 0
  fi

  terminate_pid "${pid}" "${expected}" "${pid_file}"
}

echo "[dev-shell-stop] Stopping demo stack (docker-compose.demo.yml)..."
docker compose -f docker-compose.demo.yml down -v || true

if [[ -d "${PID_DIR}" ]]; then
  stop_from_pid_file "mfeDeployment" "nx serve mfeDeployment"
  stop_from_pid_file "mfePatients" "nx serve mfePatients"
  stop_from_pid_file "mfeMeasureBuilder" "nx serve mfeMeasureBuilder"
  stop_from_pid_file "shell-app" "nx serve shell-app"
else
  echo "[dev-shell-stop] PID directory ${PID_DIR} not found"
fi

echo "[dev-shell-stop] Completed."
