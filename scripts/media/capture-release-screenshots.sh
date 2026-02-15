#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

VERSION="${1:-}"
SUITE="${2:-smoke}"

show_help() {
  cat <<'EOF'
Capture release screenshots into docs/releases/<version>/media/screenshots/agent-browser/

Usage:
  ./scripts/media/capture-release-screenshots.sh <version> [suite]

Examples:
  ./scripts/media/capture-release-screenshots.sh v2.7.1-rc1 smoke
  ./scripts/media/capture-release-screenshots.sh v2.7.1-rc1 all

Env:
  PORTAL_URL      Defaults to http://localhost:18080
  AB_SLEEP_SECS   agent-browser sleep between navigation steps (default: scripts/agent-browser/_common.sh default)
EOF
}

if [[ -z "$VERSION" || "$VERSION" == "-h" || "$VERSION" == "--help" ]]; then
  show_help
  exit 1
fi

PORTAL_URL="${PORTAL_URL:-http://localhost:18080}"
OUT_DIR="docs/releases/${VERSION}/media/screenshots/agent-browser"
mkdir -p "$OUT_DIR"

write_manifest() {
  local f="${OUT_DIR}/MANIFEST.txt"
  {
    echo "version=${VERSION}"
    echo "captured_at_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
    echo "git_rev=$(git rev-parse HEAD)"
    echo "portal_url=${PORTAL_URL}"
    echo "suite=${SUITE}"
  } >"$f"
}

run_smoke() {
  PORTAL_URL="$PORTAL_URL" SCREENSHOT_DIR="$OUT_DIR" ./scripts/agent-browser-smoke.sh
}

run_all() {
  # Order matters a bit for narrative continuity.
  local scripts=(
    "scripts/agent-browser/nav-global.sh"
    "scripts/agent-browser/provider-care-gaps.sh"
    "scripts/agent-browser/provider-measures.sh"
    "scripts/agent-browser/provider-results.sh"
    "scripts/agent-browser/rn-care-gaps.sh"
    "scripts/agent-browser/rn-followups.sh"
    "scripts/agent-browser/quality-trends.sh"
    "scripts/agent-browser/quality-reports.sh"
    "scripts/agent-browser/reports-compliance.sh"
    "scripts/agent-browser/dashboard-admin.sh"
    "scripts/agent-browser/ma-prep.sh"
    "scripts/agent-browser/ma-reminders.sh"
  )

  for s in "${scripts[@]}"; do
    if [[ ! -x "$s" ]]; then
      echo "Missing or not executable: $s"
      exit 1
    fi

    local base
    base="$(basename "$s" .sh)"
    local run_dir="${OUT_DIR}/${base}"
    mkdir -p "$run_dir"

    echo "[capture] $s -> $run_dir"
    PORTAL_URL="$PORTAL_URL" SCREENSHOT_DIR="$run_dir" "$s"
  done
}

write_manifest

case "$SUITE" in
  smoke) run_smoke ;;
  all) run_all ;;
  *) show_help; echo ""; echo "Unknown suite: $SUITE"; exit 1 ;;
esac

echo "Screenshots captured under: ${OUT_DIR}"

