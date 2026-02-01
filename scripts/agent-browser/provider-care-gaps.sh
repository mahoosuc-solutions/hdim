#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_common.sh"

open_portal
set_role "Provider"

assert_text_visible "High Priority Care Gaps"
click_by_label "Address"

screenshot "01-provider-care-gaps.png"

ab close
