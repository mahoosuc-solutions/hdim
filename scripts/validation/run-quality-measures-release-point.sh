#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

PORT="${PORT:-4210}"
PW_WEB_SERVER_TARGET="${PW_WEB_SERVER_TARGET:-clinical-portal:serve-static}"
PW_CONFIG="apps/clinical-portal-e2e/playwright.config.ts"
PW_SPEC="apps/clinical-portal-e2e/src/quality-measures-patient-evaluation.e2e.spec.ts"
RUN_ALL_BROWSERS="${RUN_ALL_BROWSERS:-true}"
NX_STABLE_ENV=(NX_DAEMON=false NX_ISOLATE_PLUGINS=false)

run_cmd() {
  echo "\n==> $*"
  "$@"
}

echo "Starting quality-measures release-point validation"
echo "PORT=$PORT"
echo "PW_WEB_SERVER_TARGET=$PW_WEB_SERVER_TARGET"

run_cmd npx nx reset

run_cmd npx tsc -p apps/clinical-portal/tsconfig.app.json --noEmit
run_cmd npx tsc -p apps/mfe-quality/tsconfig.app.json --noEmit

JEST_CONFIG="apps/clinical-portal/jest.config.ts"
run_cmd env "${NX_STABLE_ENV[@]}" npx jest --config "$JEST_CONFIG" apps/clinical-portal/src/app/pages/quality-measures/quality-measures.component.spec.ts
run_cmd env "${NX_STABLE_ENV[@]}" npx jest --config "$JEST_CONFIG" apps/clinical-portal/src/app/pages/quality-measure-detail/quality-measure-detail.component.spec.ts
run_cmd env "${NX_STABLE_ENV[@]}" npx jest --config "$JEST_CONFIG" apps/clinical-portal/src/app/services/evaluation.service.spec.ts

run_pw() {
  local project="$1"
  run_cmd env "${NX_STABLE_ENV[@]}" PORT="$PORT" PW_REUSE_SERVER=false PW_WEB_SERVER_TARGET="$PW_WEB_SERVER_TARGET" npx playwright test \
    --config "$PW_CONFIG" \
    "$PW_SPEC" \
    --project="$project"
}

run_pw chromium

if [[ "$RUN_ALL_BROWSERS" == "true" ]]; then
  run_pw firefox
  run_pw webkit
fi

echo "\nRelease-point validation completed successfully."
