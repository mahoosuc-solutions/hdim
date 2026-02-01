#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_common.sh"

open_portal
set_role "Medical Assistant"

click_by_label "Care Gaps"
wait_for_heading "Care Gaps"
assert_text_visible "Send Reminder"
click_by_label "Send Reminder"

screenshot "01-ma-reminders.png"

ab close
