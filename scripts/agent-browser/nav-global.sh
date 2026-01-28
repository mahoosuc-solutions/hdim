#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/_common.sh"

open_portal
screenshot "01-dashboard.png"

click_by_label "Patients"
sleep "${AB_SLEEP_SECS}"
wait_for_heading "Patients"
screenshot "02-patients.png"

click_by_label "Care Gaps"
sleep "${AB_SLEEP_SECS}"
wait_for_heading "Care Gaps"
screenshot "03-care-gaps.png"

click_by_label "Evaluations"
sleep "${AB_SLEEP_SECS}"
wait_for_heading "Evaluations"
screenshot "04-evaluations.png"

click_by_label "Reports"
sleep "${AB_SLEEP_SECS}"
wait_for_heading "Reports"
screenshot "05-reports.png"

ab close
