#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_common.sh"

open_portal
set_role "Medical Assistant"

assert_text_visible "Medical Assistant Dashboard"
assert_text_visible "Patient Check-in"
click_by_label "View Patient"

screenshot "01-ma-prep.png"

ab close
