#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

REPORT_DIR="validation-reports"
SUMMARY_MD="$REPORT_DIR/bsl-launch-ops-summary.md"
SUMMARY_JSON="$REPORT_DIR/bsl-launch-ops-summary.json"
TMP_FILE="$REPORT_DIR/.bsl-launch-ops.tmp"

mkdir -p "$REPORT_DIR"
trap 'rm -f "$TMP_FILE"' EXIT

failures=0
: > "$TMP_FILE"

record_status() {
  local key="$1"
  local status="$2"
  local detail="$3"
  printf '%s|%s|%s\n' "$key" "$status" "$detail" >> "$TMP_FILE"
  if [[ "$status" == "fail" ]]; then
    failures=$((failures + 1))
  fi
}

check_file_exists() {
  local key="$1"
  local path="$2"
  if [[ -f "$path" ]]; then
    record_status "$key" "pass" "$path"
  else
    record_status "$key" "fail" "Missing $path"
  fi
}

check_url_200() {
  local key="$1"
  local url="$2"
  local code
  code="$(curl -sS -o /dev/null -w '%{http_code}' "$url" || echo "000")"
  if [[ "$code" == "200" ]]; then
    record_status "$key" "pass" "$url ($code)"
  else
    record_status "$key" "fail" "$url ($code)"
  fi
}

check_file_exists "bsl_release_plan" "docs/compliance/BSL_RELEASE_PLAN.md"
check_file_exists "licensing_boundary" "docs/compliance/LICENSING-BOUNDARY.md"
check_file_exists "third_party_notices" "docs/compliance/THIRD_PARTY_NOTICES.md"
check_file_exists "licensing_transparency_page" "landing-page/src/app/resources/licensing/LicensingTransparencyHub.tsx"

check_url_200 "himss_brief_url" "https://hdim-himss.vercel.app/resources/himss-brief"
check_url_200 "trust_center_url" "https://hdim-himss.vercel.app/resources/trust-center"
check_url_200 "evidence_room_url" "https://hdim-himss.vercel.app/resources/evidence-room"
check_url_200 "procurement_url" "https://hdim-himss.vercel.app/resources/procurement"
check_url_200 "licensing_url" "https://hdim-himss.vercel.app/resources/licensing"
check_url_200 "terms_url" "https://hdim-himss.vercel.app/terms"

PUBLIC_SCAN_DIRS=(
  "landing-page/public"
  "docs/marketing/web"
  "docs/marketing/himss/print-deck-2026-03-10"
)

CONTROLLED_NAME_PATTERN='(hedis|ncqa|vsd|mld|cpt|cdt|loinc|snomed|rxnorm|umls|radlex|ub[-_]?codes?)'
offending_files="$(rg --files "${PUBLIC_SCAN_DIRS[@]}" | rg -i "$CONTROLLED_NAME_PATTERN" || true)"
if [[ -n "$offending_files" ]]; then
  record_status "controlled_content_scan" "fail" "Potentially controlled asset names found: $(echo "$offending_files" | tr '\n' ';' | sed 's/;$/ /')"
else
  record_status "controlled_content_scan" "pass" "No controlled-content filename patterns found in public artifacts"
fi

{
  echo "# BSL Launch Ops Validation Summary"
  echo
  echo "- Date (UTC): $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
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
    echo "**Gate Result:** FAIL ($failures control(s) failed)"
  fi
} > "$SUMMARY_MD"

{
  echo "{"
  echo "  \"generatedAt\": \"$(date -u +"%Y-%m-%dT%H:%M:%SZ")\","
  echo "  \"checks\": ["
  first=true
  while IFS='|' read -r key status detail; do
    $first || echo ","
    first=false
    escaped_detail="$(printf '%s' "$detail" | sed 's/\\/\\\\/g; s/"/\\"/g')"
    echo "    {\"key\":\"$key\",\"status\":\"$status\",\"detail\":\"$escaped_detail\"}"
  done < "$TMP_FILE"
  echo
  echo "  ],"
  echo "  \"failures\": $failures"
  echo "}"
} > "$SUMMARY_JSON"

cat "$SUMMARY_MD"

if [[ "$failures" -gt 0 ]]; then
  exit 1
fi
