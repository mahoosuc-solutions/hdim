#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_common.sh"

open_portal
click_by_label "Reports"
wait_for_heading "Reports"

assert_text_visible "Compliance"
click_by_label "Compliance"
sleep "${AB_SLEEP_SECS}"

assert_text_visible "Export"

screenshot "01-compliance-report.png"

ab close
