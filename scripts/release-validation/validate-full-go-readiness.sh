#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-v0.0.0-test}"
NOW_UTC="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
REPORT="docs/releases/${VERSION}/validation/full-go-readiness-report.md"
mkdir -p "$(dirname "$REPORT")"

SCORECARD="docs/releases/${VERSION}/validation/pilot-scorecard.md"
ROI="$(ls -1 docs/investor/ROI_DEFENSIBILITY_PACK_*.md 2>/dev/null | grep -v 'TEMPLATE' | sort | tail -n 1 || true)"
GO_LIVE="docs/releases/${VERSION}/GO_LIVE_PACKET.md"
RECON="$(ls -1 docs/compliance/SECURITY_COMPLIANCE_RECONCILIATION_*.md 2>/dev/null | sort | tail -n 1 || true)"
SAFETY_FILES=(
  "docs/releases/${VERSION}/safety/SAFETY_CASE_CARE_GAP_PRIORITIZATION.md"
  "docs/releases/${VERSION}/safety/SAFETY_CASE_REALTIME_INTERVENTION_ALERTING.md"
  "docs/releases/${VERSION}/safety/SAFETY_CASE_REVENUE_CYCLE_DENIAL_PREVENTION.md"
)

failures=0
record(){ printf '| %s | %s | %s |\n' "$1" "$2" "$3" >> "$REPORT"; }

check_file(){
  local p="$1"; local l="$2"
  if [[ -n "$p" && -f "$p" ]]; then record "$l" "PASS" "$p"; else record "$l" "FAIL" "Missing $p"; failures=$((failures+1)); fi
}

check_no_pending(){
  local p="$1"; local l="$2"
  if [[ -z "$p" || ! -f "$p" ]]; then
    record "$l" "FAIL" "Missing file for pending-check: $p"
    failures=$((failures+1))
    return
  fi
  if rg -qi 'Pending|IN PROGRESS|TBD' "$p"; then
    record "$l" "FAIL" "Found Pending/IN PROGRESS/TBD in $p"
    failures=$((failures+1))
  else
    record "$l" "PASS" "No Pending/IN PROGRESS/TBD markers"
  fi
}

cat > "$REPORT" <<EOF_MD
# Full GO Readiness Report

**Version:** ${VERSION}
**Generated:** ${NOW_UTC}

| Check | Status | Details |
|---|---|---|
EOF_MD

check_file "$SCORECARD" "Pilot scorecard exists"
check_file "$ROI" "ROI defensibility pack exists"
check_file "$GO_LIVE" "Go-live packet exists"
check_file "$RECON" "Security/compliance reconciliation note exists"
for sf in "${SAFETY_FILES[@]}"; do
  check_file "$sf" "Safety case exists: $(basename "$sf")"
done

check_no_pending "$SCORECARD" "Pilot scorecard sign-offs complete"
check_no_pending "$ROI" "ROI pack sign-offs complete"
check_no_pending "$GO_LIVE" "Go-live packet approvals complete"
check_no_pending "$RECON" "Reconciliation note sign-offs complete"
for sf in "${SAFETY_FILES[@]}"; do
  check_no_pending "$sf" "Safety approvals complete: $(basename "$sf")"
done

observed_scorecard_count="$(rg -c '\| Observed \|' "$SCORECARD" || true)"
if (( observed_scorecard_count >= 5 )); then
  record "Observed KPI coverage" "PASS" "${observed_scorecard_count} observed KPI rows"
else
  record "Observed KPI coverage" "FAIL" "Need >=5 observed KPI rows, found ${observed_scorecard_count}"
  failures=$((failures+1))
fi

observed_roi_count="$(rg -c '\| Observed \|' "$ROI" || true)"
if (( observed_roi_count >= 2 )); then
  record "Observed ROI input coverage" "PASS" "${observed_roi_count} observed ROI rows"
else
  record "Observed ROI input coverage" "FAIL" "Need >=2 observed ROI rows, found ${observed_roi_count}"
  failures=$((failures+1))
fi

if rg -q 'Closed for `v0.0.0-test` release lane' "$RECON"; then
  record "Reconciliation closure statement" "PASS" "Release-lane closure statement present"
else
  record "Reconciliation closure statement" "FAIL" "Missing release-lane closure statement"
  failures=$((failures+1))
fi

if (( failures > 0 )); then
  cat >> "$REPORT" <<EOF_MD

## Decision

**Decision:** NO-GO

Blocking findings: ${failures}
EOF_MD
  echo "Full GO readiness: NO-GO (${failures} failures)"
  echo "Report: $REPORT"
  exit 1
fi

cat >> "$REPORT" <<EOF_MD

## Decision

**Decision:** GO

All strict full-go closure checks passed.
EOF_MD

echo "Full GO readiness: GO"
echo "Report: $REPORT"
