#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/agent-browser/_common.sh"

open_portal
ab get title || true
screenshot "01-dashboard.png"

navigate() {
  local label="$1"
  local shot="$2"

  echo "Navigating to ${label}"
  ab find role link click --name "${label}" \
    || ab find role button click --name "${label}" \
    || ab find text "${label}" click
  sleep "${AB_SLEEP_SECS}"
  ab get url || true
  screenshot "${shot}"
}

navigate "Patients" "02-patients.png"
navigate "Care Gaps" "03-care-gaps.png"
navigate "Evaluations" "04-evaluations.png"
navigate "Reports" "05-reports.png"

ab close
echo "Smoke run complete. Screenshots: ${AB_SCREENSHOT_DIR}"
