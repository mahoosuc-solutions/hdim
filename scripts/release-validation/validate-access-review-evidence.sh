#!/usr/bin/env bash
set -euo pipefail

REPORT_PATH="${1:-docs/compliance/ACCESS_REVIEW_2026-03-07.md}"
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

check '^## Scope' 'scope section present'
check '^## Inventory Summary' 'inventory summary section present'
check '^## Findings' 'findings section present'
check '^## Remediation Actions' 'remediation actions section present'
check '^## Sign-Off' 'sign-off section present'

if rg -q 'TBD|Pending|Status:\s*Open' "$REPORT_PATH"; then
  fail 'inventory, findings, remediation, and sign-off are finalized'
else
  pass 'inventory, findings, remediation, and sign-off are finalized'
fi

if [ "$failures" -gt 0 ]; then
  echo "Access review evidence validation: FAIL ($failures issues)"
  exit 1
fi

echo 'Access review evidence validation: PASS'
