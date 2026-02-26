#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

PORT="${PORT:-4200}"
BASE_URL="${BASE_URL:-http://localhost:${PORT}}"
SKIP_WEB_SERVER="${SKIP_WEB_SERVER:-0}"
PW_CHROMIUM_NO_SANDBOX="${PW_CHROMIUM_NO_SANDBOX:-0}"

is_true() {
  case "${1,,}" in
    1|true|yes) return 0 ;;
    *) return 1 ;;
  esac
}

echo "[preflight] BASE_URL=${BASE_URL}"
echo "[preflight] SKIP_WEB_SERVER=${SKIP_WEB_SERVER}"
echo "[preflight] PW_CHROMIUM_NO_SANDBOX=${PW_CHROMIUM_NO_SANDBOX}"

if ! is_true "$SKIP_WEB_SERVER"; then
  if ! command -v python3 >/dev/null 2>&1; then
    echo "[preflight] FAIL: python3 is required by Playwright webServer command."
    exit 1
  fi
fi

if [[ "$(uname -s)" == "Linux" ]] && ! is_true "$PW_CHROMIUM_NO_SANDBOX"; then
  if [[ -r /proc/sys/kernel/unprivileged_userns_clone ]] && [[ "$(cat /proc/sys/kernel/unprivileged_userns_clone)" == "0" ]]; then
    echo "[preflight] FAIL: Chromium sandbox likely blocked (unprivileged_userns_clone=0)."
    echo "[preflight] Action: set PW_CHROMIUM_NO_SANDBOX=1 for constrained runners."
    exit 1
  fi
fi

echo "[preflight] PASS"
