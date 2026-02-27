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
  record_status "soc2_control_matrix" "pass" "$soc2_matrix"
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
