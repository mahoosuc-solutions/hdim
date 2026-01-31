#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_common.sh"

open_portal
set_role "Registered Nurse"

assert_text_visible "Care Gaps Assigned"
click_by_label "Care Gaps"

screenshot "01-rn-care-gaps.png"

ab close
