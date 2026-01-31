#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_common.sh"

open_portal
set_role "Provider"

assert_text_visible "Results Awaiting Review"
click_by_label "Review"

screenshot "01-provider-results.png"

ab close
