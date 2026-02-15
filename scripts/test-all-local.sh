#!/usr/bin/env bash
set -euo pipefail

: "${LOG_DIR:=logs/test-runs}"
: "${RUN_ID:=$(date +%Y%m%d-%H%M%S)}"
LOG_FILE="${LOG_DIR}/test-all-local-${RUN_ID}.log"
mkdir -p "$LOG_DIR"
START_TS="$(date +%s)"
exec > >(tee -a "$LOG_FILE") 2>&1

on_exit() {
  local exit_code=$?
  local end_ts
  end_ts="$(date +%s)"
  local duration=$((end_ts - START_TS))
  echo ""
  echo "═══════════════════════════════════════════════════════════════"
  echo "Test Run Summary"
  echo "═══════════════════════════════════════════════════════════════"
  echo "Run ID: ${RUN_ID}"
  echo "Log file: ${LOG_FILE}"
  echo "Exit code: ${exit_code}"
  echo "Duration: ${duration}s"
}
trap on_exit EXIT

stage() {
  echo ""
  echo "═══════════════════════════════════════════════════════════════"
  echo "${1}"
  echo "═══════════════════════════════════════════════════════════════"
}

: "${AUTH_USERNAME:=demo-evaluator}"
: "${AUTH_PASSWORD:=demo}"
: "${KEEP_STACK:=0}"
: "${SKIP_BACKEND_TESTS:=0}"
STACK_STARTED=0
TENANT_ID="${TENANT_ID:-acme-health}"

has_npm_script() {
  node -e "const s=require('./package.json').scripts||{}; process.exit(s['$1']?0:1)"
}

run_npm_script() {
  local script="$1"
  if has_npm_script "$script"; then
    npm run "$script" "${@:2}"
  else
    echo "Skipping npm run ${script} (script not found in package.json)"
  fi
}

run_gradle() {
  (cd backend && ./gradlew "$@")
}

load_gradle_tasks() {
  (cd backend && ./gradlew tasks --all --quiet | sed -n 's/ - .*//p' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | rg -v '^$')
}

has_gradle_task() {
  echo "$GRADLE_TASKS" | rg -qx "$1"
}

run_gradle_tasks_if_available() {
  local tasks=()
  for t in "$@"; do
    if has_gradle_task "$t"; then
      tasks+=("$t")
    else
      echo "Skipping gradle task ${t} (task not found)"
    fi
  done
  if ((${#tasks[@]})); then
    run_gradle "${tasks[@]}"
  fi
}

teardown() {
  if [[ "${STACK_STARTED}" != "1" ]]; then
    return
  fi
  if [[ "${KEEP_STACK}" == "1" ]]; then
    echo ""
    echo "KEEP_STACK=1 set; leaving demo stack running."
    return
  fi
  echo ""
  echo "Tearing down demo stack..."
  docker compose -f docker-compose.demo.yml down -v
}
trap teardown EXIT

stage "1) Lint"
run_npm_script lint
if [[ "${RUN_SLOW_LINT:-0}" == "1" ]]; then
  run_npm_script lint:slow
else
  echo "Skipping npm run lint:slow (set RUN_SLOW_LINT=1 to enable)"
fi
if [[ "${SKIP_BACKEND_TESTS}" == "1" ]]; then
  echo "Skipping backend lint tasks (SKIP_BACKEND_TESTS=1)"
else
  GRADLE_TASKS="$(load_gradle_tasks)"
  run_gradle_tasks_if_available checkstyleMain checkstyleTest
  run_gradle_tasks_if_available spotbugsMain spotbugsTest
fi

stage "2) Unit Tests"
if [[ "${SKIP_BACKEND_TESTS}" == "1" ]]; then
  echo "Skipping backend unit tests (SKIP_BACKEND_TESTS=1)"
else
  run_gradle_tasks_if_available test
fi
run_npm_script test

stage "3) Integration Tests (service-level)"
if [[ "${SKIP_BACKEND_TESTS}" == "1" ]]; then
  echo "Skipping backend integration tests (SKIP_BACKEND_TESTS=1)"
else
  run_gradle_tasks_if_available :modules:services:fhir-service:test
  run_gradle_tasks_if_available :modules:services:patient-service:test
  run_gradle_tasks_if_available :modules:services:quality-measure-service:test
  run_gradle_tasks_if_available :modules:services:care-gap-service:test
fi

stage "4) Full Stack E2E API (demo stack)"
if [ -x ./backend/gradlew ]; then
  echo ""
  echo "Building backend bootJars required by demo stack..."
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
  echo "Missing ./backend/gradlew; cannot build demo bootJars."
  exit 1
fi

echo ""
echo "Ensuring demo stack is clean (down -v) before bringing it up..."
docker compose -f docker-compose.demo.yml down -v --remove-orphans || true

if ! docker compose -f docker-compose.demo.yml up -d; then
  echo ""
  echo "docker compose up failed. Recent status/logs:"
  docker compose -f docker-compose.demo.yml ps || true
  docker compose -f docker-compose.demo.yml logs --tail=200 postgres kafka || true
  exit 1
fi
STACK_STARTED=1
NON_INTERACTIVE=1 SEED_PROFILE="${SEED_PROFILE:-smoke}" WAIT_TIMEOUT_SECS="${WAIT_TIMEOUT_SECS:-900}" ./scripts/seed-all-demo-data.sh

# Seeded data validation gate (ensures E2E/UI tests have real data to operate on).
if [[ -x ./scripts/verify-seeding-counts.sh ]]; then
  echo ""
  echo "Validating seeded data counts..."
  expected_patients=""
  if [[ "${SEED_PROFILE:-smoke}" == "smoke" ]]; then
    expected_patients="50"
  fi
  TENANTS="${TENANT_ID}" \
    EXPECTED_PATIENTS_PER_TENANT="${expected_patients}" \
    ./scripts/verify-seeding-counts.sh
else
  echo "Missing ./scripts/verify-seeding-counts.sh; skipping seeded data validation."
fi

AUTH_USERNAME="${AUTH_USERNAME}" AUTH_PASSWORD="${AUTH_PASSWORD}" ./validate-system.sh

stage "5) Full Stack E2E UI (Playwright)"
if [[ "${RUN_FULL_E2E:-0}" == "1" ]]; then
  run_npm_script e2e:clinical-portal:demo
else
  run_npm_script e2e:clinical-portal:smoke
fi

stage "DONE"
echo "All local tests completed."
