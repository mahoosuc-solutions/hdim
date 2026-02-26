#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REPORT_DIR="$ROOT/docs/review/evidence"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
REPORT_FILE="$REPORT_DIR/technical-review-checks-$STAMP.md"

WITH_RUNTIME=false
if [[ "${1:-}" == "--with-runtime" ]]; then
  WITH_RUNTIME=true
fi

mkdir -p "$REPORT_DIR"

status_line() {
  local label="$1"
  local state="$2"
  local detail="$3"
  printf '| %s | %s | %s |\n' "$label" "$state" "$detail" >> "$REPORT_FILE"
}

write_header() {
  {
    echo "# Technical Review Checks"
    echo
    echo "- Generated (UTC): $(date -u '+%Y-%m-%d %H:%M:%S')"
    echo "- Commit: $(git -C "$ROOT" rev-parse HEAD)"
    echo "- Branch: $(git -C "$ROOT" branch --show-current)"
    echo "- Runtime checks enabled: $WITH_RUNTIME"
    echo
    echo "| Check | Result | Detail |"
    echo "|---|---|---|"
  } > "$REPORT_FILE"
}

run_check() {
  local label="$1"
  shift
  if "$@" >/tmp/hdim-review-check.out 2>/tmp/hdim-review-check.err; then
    local detail
    detail="$(head -n 1 /tmp/hdim-review-check.out | tr '|' '/' || true)"
    status_line "$label" "PASS" "${detail:-ok}"
  else
    local detail
    detail="$(head -n 1 /tmp/hdim-review-check.err | tr '|' '/' || true)"
    status_line "$label" "FAIL" "${detail:-command failed}"
  fi
}

write_header

run_check "Git available" git -C "$ROOT" rev-parse --is-inside-work-tree
run_check "Tag v2.7.0 exists" git -C "$ROOT" rev-parse -q --verify "refs/tags/v2.7.0"
run_check "Tag v2.7.1-rc2 exists" git -C "$ROOT" rev-parse -q --verify "refs/tags/v2.7.1-rc2"
run_check "Release v2.7.0 exists (GitHub)" gh api repos/webemo-aaron/hdim/releases/tags/v2.7.0 --jq .tag_name
run_check "Release v2.7.1-rc2 exists (GitHub)" gh api repos/webemo-aaron/hdim/releases/tags/v2.7.1-rc2 --jq .tag_name
run_check "Architecture doc present" test -f "$ROOT/docs/architecture/SYSTEM_ARCHITECTURE.md"
run_check "YC architecture doc present" test -f "$ROOT/yc-application-v2/TECHNICAL_ARCHITECTURE.md"
run_check "RC2 go/no-go checklist present" test -f "$ROOT/docs/releases/v2.7.1-rc2/RC2_GO_NO_GO_CHECKLIST.md"
run_check "Evidence generator script executable" test -x "$ROOT/scripts/review/generate-technical-review-pack.sh"

"$ROOT/scripts/review/generate-technical-review-pack.sh" >/tmp/hdim-review-evidence.out 2>/tmp/hdim-review-evidence.err \
  && status_line "Generate evidence pack" "PASS" "$(head -n 1 /tmp/hdim-review-evidence.out)" \
  || status_line "Generate evidence pack" "FAIL" "$(head -n 1 /tmp/hdim-review-evidence.err)"

if [[ "$WITH_RUNTIME" == "true" ]]; then
  run_check "Demo validator" "$ROOT/validate-system.sh"
fi

{
  echo
  echo "## Notes"
  echo
  echo "- This report is machine-generated."
  echo "- For runtime proof, rerun with \`--with-runtime\` in an environment with the demo stack up."
} >> "$REPORT_FILE"

echo "Review report written: $REPORT_FILE"
