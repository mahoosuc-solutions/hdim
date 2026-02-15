#!/usr/bin/env bash
set -euo pipefail

# Local CI entrypoint intended to be run before pushing to GitHub.
# This is not a replacement for GitHub Actions; it's a deterministic gate you can run offline.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() { echo -e "${BLUE}[local-ci]${NC} $*"; }
warn() { echo -e "${YELLOW}[local-ci]${NC} $*"; }
die() { echo -e "${RED}[local-ci]${NC} $*"; exit 1; }

MODE="${1:-pr}"

show_help() {
  cat <<'EOF'
Local CI Runner

Usage:
  ./scripts/ci/local-ci.sh [mode]

Modes:
  quick   Fast checks (no docker stack): MCP config + lint/test if available
  pr      PR gate (recommended): quick + dockerfile checks + full local test runner
  demo    Demo gate: bring up demo stack, seed, validate, then UI screenshot smoke
  release Release prep: generate release docs + run release-validation launcher (interactive)

Env:
  VERSION             Required for release mode (e.g., v2.7.1-rc1)
  KEEP_STACK=1        Keep demo stack up after demo/pr mode runs (via scripts/test-all-local.sh)
  SKIP_BACKEND_TESTS=1 Skip backend gradle tasks in scripts/test-all-local.sh
  RUN_SLOW_LINT=1     Run slower TS lint targets in scripts/test-all-local.sh (optional)
EOF
}

has_npm_script() {
  node -e "const s=require('./package.json').scripts||{}; process.exit(s['$1']?0:1)"
}

run_npm_script() {
  local script="$1"
  if has_npm_script "$script"; then
    log "npm run ${script} ${*:2}"
    npm run "$script" "${@:2}"
  else
    warn "Skipping npm run ${script} (script not found in package.json)"
  fi
}

run_quick() {
  log "Mode=quick"
  run_npm_script test:mcp
  run_npm_script lint
  run_npm_script test
}

run_pr() {
  log "Mode=pr"
  run_quick
  if [ -x ./scripts/validate-dockerfiles.sh ]; then
    log "./scripts/validate-dockerfiles.sh"
    ./scripts/validate-dockerfiles.sh
  fi
  if [ -x ./scripts/test-all-local.sh ]; then
    log "./scripts/test-all-local.sh"
    ./scripts/test-all-local.sh
  else
    die "Missing ./scripts/test-all-local.sh"
  fi
}

run_demo() {
  log "Mode=demo"
  if [ -x ./scripts/seed-all-demo-data.sh ]; then
    :
  else
    die "Missing ./scripts/seed-all-demo-data.sh"
  fi
  if [ -x ./validate-system.sh ]; then
    :
  else
    die "Missing ./validate-system.sh"
  fi

  # Most backend service Dockerfiles expect pre-built JARs.
  # We build only what the demo compose file needs, then build the stack.
  if [ -x ./backend/gradlew ]; then
    log "Building backend bootJars required by demo stack"
    (cd backend && ./gradlew \
      :modules:services:gateway-admin-service:bootJar \
      :modules:services:gateway-fhir-service:bootJar \
      :modules:services:gateway-clinical-service:bootJar \
      :modules:services:fhir-service:bootJar \
      :modules:services:cql-engine-service:bootJar \
      :modules:services:patient-service:bootJar \
      :modules:services:quality-measure-service:bootJar \
      :modules:services:care-gap-service:bootJar \
      :modules:services:event-processing-service:bootJar \
      :modules:services:hcc-service:bootJar \
      :modules:services:audit-query-service:bootJar \
      :modules:services:demo-seeding-service:bootJar \
      -x test --no-daemon)
  else
    die "Missing ./backend/gradlew"
  fi

  log "docker compose -f docker-compose.demo.yml up -d --build"
  docker compose -f docker-compose.demo.yml up -d --build
  log "./scripts/seed-all-demo-data.sh (non-interactive)"
  NON_INTERACTIVE=1 SEED_PROFILE="${SEED_PROFILE:-smoke}" WAIT_TIMEOUT_SECS="${WAIT_TIMEOUT_SECS:-900}" ./scripts/seed-all-demo-data.sh
  log "./validate-system.sh"
  ./validate-system.sh

  # Screenshot smoke suite (if agent-browser is installed)
  if [ -x ./scripts/agent-browser-smoke.sh ]; then
    log "Agent-browser smoke screenshots"
    SCREENSHOT_DIR="${SCREENSHOT_DIR:-/tmp/agent-browser-smoke}" ./scripts/agent-browser-smoke.sh || true
    log "Screenshots: ${SCREENSHOT_DIR:-/tmp/agent-browser-smoke}"
  fi
}

run_release() {
  log "Mode=release"
  : "${VERSION:?VERSION is required for release mode (e.g., v2.7.1-rc1)}"
  log "Generating release docs for ${VERSION}"
  ./scripts/release-validation/generate-release-docs.sh "${VERSION}"
  warn "Release validation launcher is semi-automated and interactive (ralph-loop)."
  warn "Run: ./scripts/release-validation/run-release-validation.sh ${VERSION}"
}

case "${MODE}" in
  -h|--help|help) show_help ;;
  quick) run_quick ;;
  pr) run_pr ;;
  demo) run_demo ;;
  release) run_release ;;
  *) die "Unknown mode: ${MODE}. Use --help." ;;
esac
