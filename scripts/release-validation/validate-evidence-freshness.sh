#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-v0.0.0-test}"
NOW_UTC="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
REPORT="docs/releases/${VERSION}/validation/evidence-freshness-report.md"
MATRIX="docs/compliance/REGULATORY_CONTROL_MATRIX_2026-03-07.md"
SCORECARD="docs/compliance/RELEASE_READINESS_SCORECARD_2026-03-07.md"
GAPS="docs/compliance/GAP_REGISTER_2026-03-07.md"
ACCESS="docs/compliance/ACCESS_REVIEW_2026-03-07.md"
TPR="docs/compliance/THIRD_PARTY_RISK_REGISTER_2026-03-07.md"
RECON="docs/compliance/SECURITY_COMPLIANCE_RECONCILIATION_2026-03-08.md"

mkdir -p "$(dirname "$REPORT")"

failures=0
record(){ printf '| %s | %s | %s |\n' "$1" "$2" "$3" >> "$REPORT"; }

parse_date_from_text(){
  local text="$1"
  if [[ "$text" =~ (20[0-9]{2}-[0-9]{2}-[0-9]{2}) ]]; then echo "${BASH_REMATCH[1]}"; return 0; fi
  return 1
}

check_freshness(){
  local label="$1"; local path="$2"; local max_days="$3"
  if [[ ! -f "$path" ]]; then record "$label" "FAIL" "Missing $path"; failures=$((failures+1)); return; fi
  local date_str; date_str="$(parse_date_from_text "$path" || true)"
  [[ -z "$date_str" ]] && date_str="$(date -u -r "$path" +%Y-%m-%d)"
  local now_epoch artifact_epoch age_days
  now_epoch="$(date -u +%s)"; artifact_epoch="$(date -u -d "$date_str" +%s)"; age_days=$(( (now_epoch - artifact_epoch) / 86400 ))
  if (( age_days > max_days )); then record "$label" "FAIL" "Stale: ${age_days}d old (max ${max_days}d), artifact=$path"; failures=$((failures+1));
  else record "$label" "PASS" "Fresh: ${age_days}d old (max ${max_days}d), artifact=$path"; fi
}

cat > "$REPORT" <<EOF_MD
# Evidence Freshness Report

**Version:** ${VERSION}
**Generated:** ${NOW_UTC}

| Check | Status | Details |
|---|---|---|
EOF_MD

check_freshness "Control matrix freshness" "$MATRIX" 7
check_freshness "Release scorecard freshness" "$SCORECARD" 7
check_freshness "Gap register freshness" "$GAPS" 7
check_freshness "Access review freshness (monthly)" "$ACCESS" 31
check_freshness "Third-party risk freshness (quarterly)" "$TPR" 92
check_freshness "Security/compliance reconciliation freshness" "$RECON" 30

critical_expected=6
critical_pass_count="$(rg -c '^\| RC-(SEC|TEN|RBAC|REL|CNT|CI)-[0-9]+ .*\| PASS \|' "$MATRIX" || true)"
if [[ "$critical_pass_count" == "$critical_expected" ]]; then
  record "No-waiver critical controls" "PASS" "${critical_pass_count}/${critical_expected} critical controls are PASS"
else
  record "No-waiver critical controls" "FAIL" "${critical_pass_count}/${critical_expected} critical controls are PASS"
  failures=$((failures+1))
fi

if rg -q '\| (CRITICAL|HIGH) \|.*\| Open \|' "$GAPS"; then
  record "Open critical/high gaps" "FAIL" "Open CRITICAL/HIGH gaps remain in gap register"
  failures=$((failures+1))
else
  record "Open critical/high gaps" "PASS" "No open CRITICAL/HIGH gaps"
fi

if rg -qi 'Pending|IN PROGRESS|TBD' "$ACCESS" "$TPR"; then
  record "Governance evidence finalized" "FAIL" "Found Pending/IN PROGRESS/TBD in access or third-party risk artifacts"
  failures=$((failures+1))
else
  record "Governance evidence finalized" "PASS" "Access and third-party artifacts are finalized"
fi

if (( failures > 0 )); then
  cat >> "$REPORT" <<EOF_MD

## Decision

**Decision:** NO-GO

Blocking findings: ${failures}
EOF_MD
  echo "Evidence freshness validation: NO-GO (${failures} failures)"
  echo "Report: $REPORT"
  exit 1
fi

cat >> "$REPORT" <<EOF_MD

## Decision

**Decision:** GO

Evidence freshness and no-waiver critical control checks passed.
EOF_MD

echo "Evidence freshness validation: GO"
echo "Report: $REPORT"
