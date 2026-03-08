#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-v0.0.0-test}"
DATE="$(date -u +%Y-%m-%d)"
NOW_UTC="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
REPORT="docs/investor/INVESTOR_READINESS_REVIEW_${DATE}.md"

mkdir -p docs/investor

failures=0

record() {
  printf '| %s | %s | %s |\n' "$1" "$2" "$3" >> "$REPORT"
}

check_file() {
  local path="$1"
  local label="$2"
  if [[ -f "$path" ]]; then
    record "$label" "PASS" "$path"
  else
    record "$label" "FAIL" "Missing $path"
    failures=$((failures + 1))
  fi
}

check_contains() {
  local path="$1"
  local pattern="$2"
  local label="$3"
  if [[ -f "$path" ]] && rg -q "$pattern" "$path"; then
    record "$label" "PASS" "Pattern '$pattern' found in $path"
  else
    record "$label" "FAIL" "Pattern '$pattern' missing in $path"
    failures=$((failures + 1))
  fi
}

cat > "$REPORT" <<EOF_MD
# Investor Readiness Review

**Version:** ${VERSION}
**Generated:** ${NOW_UTC}

| Check | Status | Details |
|---|---|---|
EOF_MD

check_file "docs/TECHNICAL_INVESTOR_VALIDATION_REPORT_2026-02-26.md" "Technical investor validation report exists"
check_file "docs/compliance/REGULATORY_CONTROL_MATRIX_2026-03-07.md" "Regulatory control matrix exists"
check_file "docs/compliance/RELEASE_READINESS_SCORECARD_2026-03-07.md" "Release readiness scorecard exists"
check_file "docs/compliance/EVIDENCE_INDEX_2026-03-07.md" "Evidence index exists"
check_file "docs/compliance/ACCESS_REVIEW_2026-03-07.md" "Access review artifact exists"
check_file "docs/compliance/THIRD_PARTY_RISK_REGISTER_2026-03-07.md" "Third-party risk artifact exists"

check_contains "docs/compliance/RELEASE_READINESS_SCORECARD_2026-03-07.md" "Current Decision: GO" "Scorecard decision is GO"
check_contains "docs/compliance/REGULATORY_CONTROL_MATRIX_2026-03-07.md" "\| RC-SEC-001 .*\| PASS \|" "Security control is PASS"
check_contains "docs/compliance/REGULATORY_CONTROL_MATRIX_2026-03-07.md" "\| RC-CI-001 .*\| PASS \|" "CI freshness control is PASS"
check_contains "docs/compliance/GAP_REGISTER_2026-03-07.md" "\| GAP-003 .*\| Closed \|" "Access-governance gap closed"
check_contains "docs/compliance/GAP_REGISTER_2026-03-07.md" "\| GAP-004 .*\| Closed \|" "Third-party-risk gap closed"

# Reuse validator scripts as hard checks.
if bash scripts/release-validation/validate-access-review-evidence.sh "docs/compliance/ACCESS_REVIEW_2026-03-07.md" >/dev/null; then
  record "Access review validator" "PASS" "validate-access-review-evidence.sh"
else
  record "Access review validator" "FAIL" "validate-access-review-evidence.sh"
  failures=$((failures + 1))
fi

if bash scripts/release-validation/validate-third-party-risk-evidence.sh "docs/compliance/THIRD_PARTY_RISK_REGISTER_2026-03-07.md" >/dev/null; then
  record "Third-party risk validator" "PASS" "validate-third-party-risk-evidence.sh"
else
  record "Third-party risk validator" "FAIL" "validate-third-party-risk-evidence.sh"
  failures=$((failures + 1))
fi

if bash scripts/release-validation/validate-evidence-freshness.sh "$VERSION" >/dev/null; then
  record "Evidence freshness validator" "PASS" "validate-evidence-freshness.sh"
else
  record "Evidence freshness validator" "FAIL" "validate-evidence-freshness.sh"
  failures=$((failures + 1))
fi

if (( failures > 0 )); then
  cat >> "$REPORT" <<EOF_MD

## Decision

**Decision:** NO-GO

Blocking findings: ${failures}
EOF_MD
  echo "Investor readiness validation: NO-GO (${failures} failures)"
  echo "Report: $REPORT"
  exit 1
fi

cat >> "$REPORT" <<EOF_MD

## Decision

**Decision:** GO

Investor readiness controls and evidence validations passed for the current release lane.
EOF_MD

echo "Investor readiness validation: GO"
echo "Report: $REPORT"
