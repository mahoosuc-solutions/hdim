#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

fail=0

check_exists() {
  local path="$1"
  if [[ -e "$path" ]]; then
    echo "OK    exists: $path"
  else
    echo "FAIL  missing: $path"
    fail=1
  fi
}

check_absent() {
  local path="$1"
  if [[ -e "$path" ]]; then
    echo "FAIL  duplicate/legacy still present: $path"
    fail=1
  else
    echo "OK    absent: $path"
  fi
}

echo "=== Required migration-doc-pack files ==="
required=(
  "LICENSE"
  "SECURITY.md"
  "NOTICE"
  "README.md"
  "CONTRIBUTING.md"
  "CODE_OF_CONDUCT.md"
  ".github/pull_request_template.md"
  ".github/ISSUE_TEMPLATE/bug-report.md"
  ".github/ISSUE_TEMPLATE/feature-request.md"
  ".github/ISSUE_TEMPLATE/config.yml"
  "docs/commercial/BSL_AND_COMMERCIAL_SUMMARY.md"
  "docs/commercial/COMMERCIAL_LICENSE_TERMS_OUTLINE.md"
  "docs/commercial/PRICING_PAGE_CONTENT.md"
  "docs/legal/IP_ASSIGNMENT_TEMPLATE.md"
  "docs/legal/ROBS_BUSINESS_RATIONALE.md"
  "docs/finance/CURRENT_STRUCTURE_FINANCIAL_FORECAST_2026-2028.md"
  "docs/compliance/ENV_SECRET_AUDIT_2026-03-07.md"
  "docs/compliance/GITLEAKS_HISTORY_SCAN_2026-03-07.md"
  "docs/compliance/COPYRIGHT_HEADER_COVERAGE_2026-03-07.md"
  "docs/plans/2026-03-07-himss-open-source-launch-punchlist.md"
  "docs/plans/2026-03-07-CODE_COMPLETENESS_AND_MIGRATION_READINESS.md"
  "docs/plans/2026-03-07-DUPLICATE_CONTENT_ALIGNMENT_REPORT.md"
  "docs/plans/2026-03-07-MIGRATION_PUSH_PLAN_MAHOOSUC_SOLUTIONS.md"
)
for p in "${required[@]}"; do check_exists "$p"; done

echo
echo "=== Known duplicate/legacy files that must be absent ==="
forbidden=(
  ".github/ISSUE_TEMPLATE/bug_report.md"
  ".github/ISSUE_TEMPLATE/feature_request.md"
  "docs/finance/FINANCIAL_FORECAST_2026-2028_CURRENT_STRUCTURE.md"
)
for p in "${forbidden[@]}"; do check_absent "$p"; done

echo
echo "=== Link target check: BSL/commercial summary absolute links ==="
if [[ -f "docs/commercial/BSL_AND_COMMERCIAL_SUMMARY.md" ]]; then
  mapfile -t links < <(rg -o '\]\(/mnt/wdblack/dev/projects/hdim-master[^)]+' docs/commercial/BSL_AND_COMMERCIAL_SUMMARY.md | sed 's/^\](//')
  if [[ ${#links[@]} -eq 0 ]]; then
    echo "WARN  no absolute links found in summary"
  else
    for l in "${links[@]}"; do
      if [[ -e "$l" ]]; then
        echo "OK    link target exists: $l"
      else
        echo "FAIL  missing link target: $l"
        fail=1
      fi
    done
  fi
fi

echo
if [[ $fail -eq 0 ]]; then
  echo "PASS  migration doc-pack validation succeeded"
else
  echo "FAIL  migration doc-pack validation failed"
fi

exit $fail
