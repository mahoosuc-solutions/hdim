#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_common.sh"

open_portal
set_role "Administrator"

assert_text_visible "Clinical Portal Dashboard"
assert_text_visible "Patients Needing Attention"

screenshot "01-admin-dashboard.png"

ab close
