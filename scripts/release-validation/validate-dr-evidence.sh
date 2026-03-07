#!/usr/bin/env bash
set -euo pipefail

REPORT_PATH="${1:-docs/compliance/DR_TEST_RESULTS_2026-03-07.md}"
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
check '^## Targets' 'targets section present'
check '^## Execution Timeline' 'execution timeline section present'
check '^## Measured Results' 'measured results section present'
check '^## Post-Recovery Validation' 'post-recovery validation section present'
check '^## Approvals' 'approvals section present'

if rg -q 'Measured RTO: TBD|Measured RPO: TBD|Threshold met: TBD' "$REPORT_PATH"; then
  fail 'measured RTO/RPO and threshold are populated'
else
  pass 'measured RTO/RPO and threshold are populated'
fi

if rg -q 'Status:\s*Open|Pending|TBD' "$REPORT_PATH"; then
  fail 'report status and sign-off fields are finalized'
else
  pass 'report status and sign-off fields are finalized'
fi

if [ "$failures" -gt 0 ]; then
  echo "DR evidence validation: FAIL ($failures issues)"
  exit 1
fi

echo 'DR evidence validation: PASS'
