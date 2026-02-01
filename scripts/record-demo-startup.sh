#!/usr/bin/env bash
# Record demo startup with logs, stats, and automated walkthrough.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEMO_DIR="${ROOT_DIR}/demo"
COMPOSE_FILE="${DEMO_DIR}/docker-compose.demo.yml"

START_ARGS=()
SKIP_STORYBOARD=false

for arg in "$@"; do
  case "$arg" in
    --clean|--build)
      START_ARGS+=("$arg")
      ;;
    --skip-storyboard)
      SKIP_STORYBOARD=true
      ;;
    *)
      echo "Unknown option: $arg"
      echo "Usage: $0 [--clean] [--build] [--skip-storyboard]"
      exit 1
      ;;
  esac
done

timestamp="$(date +%Y%m%d_%H%M%S)"
record_dir="${DEMO_DIR}/recordings/${timestamp}"
mkdir -p "${record_dir}"

log_file="${record_dir}/docker-compose.log"
stats_file="${record_dir}/docker-stats.log"
summary_file="${record_dir}/startup-summary.txt"

echo "Recording to ${record_dir}"

cleanup() {
  if [[ -n "${LOG_PID:-}" ]]; then
    kill "${LOG_PID}" >/dev/null 2>&1 || true
  fi
  if [[ -n "${STATS_PID:-}" ]]; then
    kill "${STATS_PID}" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT

docker compose -f "${COMPOSE_FILE}" logs -f --since=0 > "${log_file}" 2>&1 &
LOG_PID=$!

docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}" \
  > "${stats_file}" 2>&1 &
STATS_PID=$!

"${DEMO_DIR}/start-demo.sh" "${START_ARGS[@]}" | tee "${summary_file}"

if [ "${SKIP_STORYBOARD}" = false ]; then
  node "${ROOT_DIR}/scripts/run-demo-storyboard.js"
fi

echo "Startup recording complete: ${record_dir}"
