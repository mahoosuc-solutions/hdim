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

check_contains() {
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
check_file "docs/compliance/SECURITY_COMPLIANCE_RECONCILIATION_2026-03-08.md" "Security/compliance reconciliation published"

check_contains "docs/compliance/RELEASE_READINESS_SCORECARD_2026-03-07.md" "Current Decision: GO" "Scorecard decision is GO"

if bash scripts/release-validation/validate-access-review-evidence.sh "docs/compliance/ACCESS_REVIEW_2026-03-07.md" >/dev/null; then
  echo "| Access review evidence quality | PASS | validate-access-review-evidence.sh passed |" >> "$REPORT"
else
  echo "| Access review evidence quality | FAIL | validate-access-review-evidence.sh failed |" >> "$REPORT"
  failures=$((failures + 1))
fi

if bash scripts/release-validation/validate-third-party-risk-evidence.sh "docs/compliance/THIRD_PARTY_RISK_REGISTER_2026-03-07.md" >/dev/null; then
  echo "| Third-party risk evidence quality | PASS | validate-third-party-risk-evidence.sh passed |" >> "$REPORT"
else
  echo "| Third-party risk evidence quality | FAIL | validate-third-party-risk-evidence.sh failed |" >> "$REPORT"
  failures=$((failures + 1))
fi

if bash scripts/release-validation/validate-evidence-freshness.sh "$VERSION" >/dev/null; then
  echo "| Evidence freshness and no-waiver policy | PASS | validate-evidence-freshness.sh passed |" >> "$REPORT"
else
  echo "| Evidence freshness and no-waiver policy | FAIL | validate-evidence-freshness.sh failed |" >> "$REPORT"
  failures=$((failures + 1))
fi


if bash scripts/release-validation/validate-full-go-readiness.sh "$VERSION" >/dev/null; then
  echo "| Strict full-go validator | PASS | validate-full-go-readiness.sh passed |" >> "$REPORT"
else
  echo "| Strict full-go validator | FAIL | validate-full-go-readiness.sh failed |" >> "$REPORT"
  failures=$((failures + 1))
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
