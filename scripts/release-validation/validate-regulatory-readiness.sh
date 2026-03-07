#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-v0.0.0-test}"
DATE_UTC="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
REPORT="docs/releases/${VERSION}/validation/regulatory-readiness-report.md"
mkdir -p "$(dirname "$REPORT")"

failures=0

check_file() {
  local path="$1"
  local label="$2"
  if [ -f "$path" ]; then
    printf '| %s | PASS | %s |\n' "$label" "$path" >> "$REPORT"
  else
    printf '| %s | FAIL | Missing %s |\n' "$label" "$path" >> "$REPORT"
    failures=$((failures + 1))
  fi
}

check_log_contains() {
  local path="$1"
  local pattern="$2"
  local label="$3"
  if [ -f "$path" ] && rg -q "$pattern" "$path"; then
    printf '| %s | PASS | Pattern "%s" found in %s |\n' "$label" "$pattern" "$path" >> "$REPORT"
  else
    printf '| %s | FAIL | Pattern "%s" missing in %s |\n' "$label" "$pattern" "$path" >> "$REPORT"
    failures=$((failures + 1))
  fi
}

cat > "$REPORT" <<EOF_MD
# Regulatory Readiness Report

**Version:** ${VERSION}
**Generated:** ${DATE_UTC}

| Check | Status | Details |
|---|---|---|
EOF_MD

check_file "docs/compliance/REGULATORY_CONTROL_MATRIX_2026-03-07.md" "Control matrix published"
check_file "docs/compliance/RELEASE_READINESS_SCORECARD_2026-03-07.md" "Readiness scorecard published"
check_file "docs/compliance/EVIDENCE_INDEX_2026-03-07.md" "Evidence index published"
check_file "docs/compliance/GAP_REGISTER_2026-03-07.md" "Gap register published"
check_file "docs/compliance/OVERSIGHT_RESPONSE_PACK_v0.0.0-test_2026-03-07.md" "Oversight response pack published"

check_file "test-results/security-auth-tenant-rerun-2026-03-07.log" "Security regression run log present"
check_log_contains "test-results/security-auth-tenant-rerun-2026-03-07.log" "BUILD SUCCESSFUL" "Security regression build success"

check_file "test-results/release-preflight-2026-03-07.log" "Release preflight log present"
check_log_contains "test-results/release-preflight-2026-03-07.log" "preflight stability gate passed" "Release preflight passed"

check_file "test-results/contract-tests-2026-03-07.log" "Contract test log present"
check_log_contains "test-results/contract-tests-2026-03-07.log" "Successfully ran target test-contracts" "Contract tests passed"

check_file "test-results/mcp-orchestration-tests-2026-03-07.log" "MCP orchestration log present"
check_log_contains "test-results/mcp-orchestration-tests-2026-03-07.log" "# pass 2" "MCP orchestration tests passed"

check_file "test-results/upstream-ci-gates-2026-03-07.log" "Upstream CI gate log present"
if [ -f "test-results/upstream-ci-gates-2026-03-07.log" ] && rg -q "Decision: NO-GO" "test-results/upstream-ci-gates-2026-03-07.log"; then
  echo "| Upstream CI gate execution context | FAIL | Upstream CI gate validator returned NO-GO (failed/missing/stale required workflows) |" >> "$REPORT"
  failures=$((failures + 1))
elif [ -f "test-results/upstream-ci-gates-2026-03-07.log" ] && rg -q "Decision: GO" "test-results/upstream-ci-gates-2026-03-07.log"; then
  echo "| Upstream CI gate execution context | PASS | Upstream CI gate validator returned GO |" >> "$REPORT"
else
  echo "| Upstream CI gate execution context | FAIL | Upstream CI gate result indeterminate; rerun validation |" >> "$REPORT"
  failures=$((failures + 1))
fi

if [ -f "docs/compliance/GAP_REGISTER_2026-03-07.md" ] && rg -q "\| CRITICAL \|.*\| Open \|" "docs/compliance/GAP_REGISTER_2026-03-07.md"; then
  echo "| Critical gaps | FAIL | Open critical gap(s) found in gap register |" >> "$REPORT"
  failures=$((failures + 1))
else
  echo "| Critical gaps | PASS | No open critical gaps in gap register |" >> "$REPORT"
fi

if [ "$failures" -gt 0 ]; then
  cat >> "$REPORT" <<EOF_MD

## Go/No-Go

**Decision:** NO-GO

Blocking findings: ${failures}
EOF_MD
  echo "Regulatory readiness: NO-GO (${failures} failures)"
  echo "Report: $REPORT"
  exit 1
fi

cat >> "$REPORT" <<EOF_MD

## Go/No-Go

**Decision:** GO

All required checks passed.
EOF_MD

echo "Regulatory readiness: GO"
echo "Report: $REPORT"
