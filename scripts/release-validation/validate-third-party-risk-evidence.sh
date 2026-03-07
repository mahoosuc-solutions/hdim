#!/usr/bin/env bash
set -euo pipefail

REPORT_PATH="${1:-docs/compliance/THIRD_PARTY_RISK_REGISTER_2026-03-07.md}"
failures=0

fail() { echo "FAIL: $1"; failures=$((failures + 1)); }
pass() { echo "PASS: $1"; }

[ -f "$REPORT_PATH" ] || { echo "FAIL: missing report $REPORT_PATH"; exit 1; }

check() {
  local pattern="$1"
  local label="$2"
  if rg -q "$pattern" "$REPORT_PATH"; then
    pass "$label"
  else
    fail "$label"
  fi
}

check '^\| Vendor \| Service Area \| Data Class \| BAA \| DPA \| SOC2/ISO \| Risk Rating \| Owner \| Next Review \| Notes \|' 'required vendor risk columns present'
check '^## Open High-Risk Items' 'open high-risk items section present'
check '^## Exceptions' 'exceptions section present'
check '^## Sign-Off' 'sign-off section present'

if rg -q '\| TBD \| TBD \| TBD \| TBD \| TBD \| TBD \| TBD \| TBD \| TBD \| TBD \|' "$REPORT_PATH"; then
  fail 'vendor entries are populated'
else
  pass 'vendor entries are populated'
fi

if rg -q 'Status:\s*Open|Pending|TBD' "$REPORT_PATH"; then
  fail 'risk register status and approvals are finalized'
else
  pass 'risk register status and approvals are finalized'
fi

if [ "$failures" -gt 0 ]; then
  echo "Third-party risk evidence validation: FAIL ($failures issues)"
  exit 1
fi

echo 'Third-party risk evidence validation: PASS'
