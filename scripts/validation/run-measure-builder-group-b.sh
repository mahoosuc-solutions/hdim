#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

PORT="${PORT:-4210}"
BASE_URL="${BASE_URL:-http://localhost:${PORT}}"
PW_CHROMIUM_NO_SANDBOX="${PW_CHROMIUM_NO_SANDBOX:-1}"
GROUP_B_SERVER_MODE="${GROUP_B_SERVER_MODE:-auto}"

case "${GROUP_B_SERVER_MODE}" in
  auto)
    if curl -fsS --max-time 3 "$BASE_URL" >/dev/null 2>&1; then
      SKIP_WEB_SERVER=1
      echo "[group-b] Detected existing server at $BASE_URL; using SKIP_WEB_SERVER=1"
    else
      SKIP_WEB_SERVER=0
      echo "[group-b] No existing server detected at $BASE_URL; using managed Playwright webServer"
    fi
    ;;
  existing)
    SKIP_WEB_SERVER=1
    echo "[group-b] Forced existing server mode (SKIP_WEB_SERVER=1)"
    ;;
  managed)
    SKIP_WEB_SERVER=0
    echo "[group-b] Forced managed webServer mode (SKIP_WEB_SERVER=0)"
    ;;
  *)
    echo "[group-b] Invalid GROUP_B_SERVER_MODE=$GROUP_B_SERVER_MODE (expected auto|existing|managed)"
    exit 1
    ;;
esac

AUTO_START_CLINICAL_PORTAL=1
if [[ "${GROUP_B_SERVER_MODE}" == "existing" ]]; then
  AUTO_START_CLINICAL_PORTAL=0
fi

export PORT BASE_URL SKIP_WEB_SERVER PW_CHROMIUM_NO_SANDBOX
export AUTO_START_CLINICAL_PORTAL

bash scripts/validation/ensure-system-operational.sh
bash scripts/validation/playwright-preflight.sh
node node_modules/@playwright/test/cli.js test \
  --config apps/clinical-portal-e2e/playwright.config.ts \
  --project=chromium \
  apps/clinical-portal-e2e/src/measure-builder-metadata-retry.e2e.spec.ts
