#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

REPORT_DIR="validation-reports"
SUMMARY_JSON="$REPORT_DIR/compliance-evidence-gate-summary.json"
SUMMARY_MD="$REPORT_DIR/compliance-evidence-gate-summary.md"
TMP_FILE="$REPORT_DIR/.compliance-evidence.tmp"
STRICT_BACKEND_CVE="${STRICT_BACKEND_CVE:-true}"
BACKEND_CVE_FAIL_ON_CVSS="${BACKEND_CVE_FAIL_ON_CVSS:-9.0}"

mkdir -p "$REPORT_DIR"
trap 'rm -f "$TMP_FILE"' EXIT

failures=0

latest_match() {
  local pattern="$1"
  ls -1t $pattern 2>/dev/null | head -n 1 || true
}

record_status() {
  local key="$1"
  local status="$2"
  local detail="$3"
  printf '%s|%s|%s\n' "$key" "$status" "$detail" >> "$TMP_FILE"
  if [[ "$status" == "fail" ]]; then
    failures=$((failures + 1))
  fi
}

: > "$TMP_FILE"

# Required evidence 1: SOC2 CC control matrix
soc2_matrix="$(latest_match "docs/compliance/SOC2_CC_CONTROL_EVIDENCE_MATRIX_*.md")"
if [[ -n "$soc2_matrix" ]]; then
  compliance_line="$(rg "^- Compliance approver:" "$soc2_matrix" | head -n1 || true)"
  technical_line="$(rg "^- Technical approver:" "$soc2_matrix" | head -n1 || true)"
  final_date_line="$(rg "^- Final approval date \\(UTC\\):" "$soc2_matrix" | head -n1 || true)"
  compliance_value="$(printf '%s' "$compliance_line" | sed -E 's/^- Compliance approver:[[:space:]]*//; s/`//g')"
  technical_value="$(printf '%s' "$technical_line" | sed -E 's/^- Technical approver:[[:space:]]*//; s/`//g')"
  final_date_value="$(printf '%s' "$final_date_line" | sed -E 's/^- Final approval date \(UTC\):[[:space:]]*//; s/`//g')"
  if [[ -z "$compliance_line" || -z "$technical_line" || -z "$final_date_line" ]]; then
    record_status "soc2_control_matrix" "fail" "$soc2_matrix missing required sign-off lines"
  elif [[ "$compliance_value" =~ ^(TBD|Pending|N/A|-)?$ || "$technical_value" =~ ^(TBD|Pending|N/A|-)?$ || "$final_date_value" =~ ^(TBD|Pending|N/A|-)?$ ]]; then
    record_status "soc2_control_matrix" "fail" "$soc2_matrix has unresolved sign-off values"
  elif [[ ! "$final_date_value" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z$ ]]; then
    record_status "soc2_control_matrix" "fail" "$soc2_matrix final approval date is not RFC3339 UTC"
  else
    record_status "soc2_control_matrix" "pass" "$soc2_matrix"
  fi
else
  record_status "soc2_control_matrix" "fail" "Missing docs/compliance/SOC2_CC_CONTROL_EVIDENCE_MATRIX_*.md"
fi

# Required evidence 2: HIPAA controls validation log
hipaa_log="$(latest_match "test-results/hipaa-controls-*.log")"
if [[ -n "$hipaa_log" ]]; then
  record_status "hipaa_controls_log" "pass" "$hipaa_log"
else
  record_status "hipaa_controls_log" "fail" "Missing test-results/hipaa-controls-*.log"
fi

# Required evidence 3: Backend dependency-check report artifact
backend_cve_html="$(latest_match "backend/build/reports/dependency-check-report.html")"
backend_cve_sarif="$(latest_match "backend/build/reports/dependency-check-report.sarif")"
backend_cve_json="$(latest_match "backend/build/reports/dependency-check-report.json")"
backend_manifest="$(latest_match "test-results/backend-cve-artifacts-manifest-*.md")"

if [[ -n "$backend_cve_html" ]]; then
  detail="$backend_cve_html"
  if [[ -n "$backend_cve_sarif" ]]; then
    detail="$detail; $backend_cve_sarif"
  fi
  if [[ -n "$backend_cve_json" ]]; then
    vuln_count="$(jq '[.dependencies[] | (.vulnerabilities // []) | length] | add // 0' "$backend_cve_json" 2>/dev/null || echo 0)"
    max_cvss="$(jq '[.dependencies[] | (.vulnerabilities // [])[] | (.cvssv3.baseScore // .cvssv2.score // 0)] | max // 0' "$backend_cve_json" 2>/dev/null || echo 0)"
    if awk "BEGIN { exit !($max_cvss >= $BACKEND_CVE_FAIL_ON_CVSS) }"; then
      record_status "backend_cve_report" "fail" "$detail; vulnerabilities=$vuln_count; max_cvss=$max_cvss (threshold=$BACKEND_CVE_FAIL_ON_CVSS)"
    else
      record_status "backend_cve_report" "pass" "$detail; vulnerabilities=$vuln_count; max_cvss=$max_cvss"
    fi
  else
    record_status "backend_cve_report" "pass" "$detail"
  fi
else
  if [[ "$STRICT_BACKEND_CVE" == "false" && -n "$backend_manifest" ]]; then
    record_status "backend_cve_report" "pass" "STRICT_BACKEND_CVE=false; using manifest placeholder: $backend_manifest"
  else
    record_status "backend_cve_report" "fail" "Missing backend/build/reports/dependency-check-report.html (run dependencyCheckAggregate with NVD data)"
  fi
fi

# Required evidence 4: ZAP baseline triage artifact
zap_triage="$(latest_match "docs/compliance/OWASP_ZAP_BASELINE_TRIAGE_*.md")"
if [[ -n "$zap_triage" ]]; then
  if rg -qi "pending first successful baseline run|pending run" "$zap_triage"; then
    record_status "zap_triage_evidence" "fail" "$zap_triage still marked as pending run"
  elif ! rg -q "report_json\\.json" "$zap_triage"; then
    record_status "zap_triage_evidence" "fail" "$zap_triage missing report_json.json reference"
  else
    record_status "zap_triage_evidence" "pass" "$zap_triage"
  fi
else
  record_status "zap_triage_evidence" "fail" "Missing docs/compliance/OWASP_ZAP_BASELINE_TRIAGE_*.md"
fi

# Generate markdown summary
{
  echo "# Compliance Evidence Gate Summary"
  echo
  echo "- Date (UTC): $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
  echo "- Strict backend CVE mode: $STRICT_BACKEND_CVE"
  echo
  echo "| Control | Status | Detail |"
  echo "|---|---|---|"
  while IFS='|' read -r key status detail; do
    icon="✅"
    [[ "$status" == "fail" ]] && icon="❌"
    echo "| $key | $icon $status | $detail |"
  done < "$TMP_FILE"
  echo
  if [[ "$failures" -eq 0 ]]; then
    echo "**Gate Result:** PASS"
  else
    echo "**Gate Result:** FAIL ($failures control(s) missing)"
  fi
} > "$SUMMARY_MD"

# Generate JSON summary
{
  echo "{"
  echo "  \"generatedAt\": \"$(date -u +"%Y-%m-%dT%H:%M:%SZ")\"," 
  echo "  \"strictBackendCve\": $([[ "$STRICT_BACKEND_CVE" == "true" ]] && echo true || echo false),"
  echo "  \"checks\": ["
  first=true
  while IFS='|' read -r key status detail; do
    $first || echo ","
    first=false
    escaped_detail=$(printf '%s' "$detail" | sed 's/\\/\\\\/g; s/"/\\"/g')
    echo "    {\"key\":\"$key\",\"status\":\"$status\",\"detail\":\"$escaped_detail\"}"
  done < "$TMP_FILE"
  echo
  echo "  ],"
  echo "  \"failures\": $failures"
  echo "}"
} > "$SUMMARY_JSON"

if [[ "$failures" -gt 0 ]]; then
  cat "$SUMMARY_MD"
  exit 1
fi

cat "$SUMMARY_MD"
