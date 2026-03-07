#!/usr/bin/env bash
# Validates that upstream performance and security workflows passed recently.
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
  echo "$1"
}

err() {
  echo -e "${RED}ERROR:${NC} $1" >&2
}

warn() {
  echo -e "${YELLOW}WARN:${NC} $1"
}

ok() {
  echo -e "${GREEN}OK:${NC} $1"
}

require_env() {
  local name="$1"
  if [ -z "${!name:-}" ]; then
    err "Missing required environment variable: $name"
    exit 1
  fi
}

require_env GITHUB_TOKEN
require_env GITHUB_REPOSITORY

BRANCH_NAME="${BRANCH_NAME:-master}"
PERFORMANCE_WORKFLOW_FILE="${PERFORMANCE_WORKFLOW_FILE:-release-performance-gate.yml}"
SECURITY_WORKFLOW_FILE="${SECURITY_WORKFLOW_FILE:-release-security-gate.yml}"
MAX_GATE_AGE_HOURS="${MAX_GATE_AGE_HOURS:-168}"
REQUIRE_PERFORMANCE_GATE="${REQUIRE_PERFORMANCE_GATE:-true}"
REQUIRE_SECURITY_GATE="${REQUIRE_SECURITY_GATE:-true}"
VERSION="${VERSION:-latest}"

RELEASE_DIR="docs/releases/${VERSION}"
VALIDATION_DIR="${RELEASE_DIR}/validation"
REPORT_FILE="${VALIDATION_DIR}/ci-upstream-gates-report.md"
mkdir -p "${VALIDATION_DIR}"

api_get() {
  local url="$1"
  curl -fsSL \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    "${url}"
}

hours_since() {
  local iso_ts="$1"
  python3 - <<'PY' "$iso_ts"
import datetime
import sys

iso = sys.argv[1]
iso = iso.replace('Z', '+00:00')
ts = datetime.datetime.fromisoformat(iso)
now = datetime.datetime.now(datetime.timezone.utc)
print(int((now - ts).total_seconds() // 3600))
PY
}

check_workflow_gate() {
  local gate_name="$1"
  local workflow_file="$2"
  local required="$3"

  if [ "$required" != "true" ]; then
    warn "$gate_name gate is disabled"
    printf '| %s | SKIPPED | Disabled by config |\n' "$gate_name" >> "$REPORT_FILE"
    return 0
  fi

  local url="https://api.github.com/repos/${GITHUB_REPOSITORY}/actions/workflows/${workflow_file}/runs?branch=${BRANCH_NAME}&status=completed&per_page=1"
  local json
  json="$(api_get "$url")"

  local run_count
  run_count="$(echo "$json" | jq -r '.total_count // 0')"

  if [ "$run_count" -lt 1 ]; then
    err "$gate_name gate has no completed workflow runs for branch ${BRANCH_NAME}"
    printf '| %s | FAIL | No completed runs found on `%s` |\n' "$gate_name" "$BRANCH_NAME" >> "$REPORT_FILE"
    return 1
  fi

  local conclusion updated_at html_url run_name run_id
  conclusion="$(echo "$json" | jq -r '.workflow_runs[0].conclusion // "unknown"')"
  updated_at="$(echo "$json" | jq -r '.workflow_runs[0].updated_at // empty')"
  html_url="$(echo "$json" | jq -r '.workflow_runs[0].html_url // empty')"
  run_name="$(echo "$json" | jq -r '.workflow_runs[0].name // "unknown"')"
  run_id="$(echo "$json" | jq -r '.workflow_runs[0].id // "unknown"')"

  if [ -z "$updated_at" ]; then
    err "$gate_name gate has malformed run metadata (missing updated_at)"
    printf '| %s | FAIL | Malformed run metadata |\n' "$gate_name" >> "$REPORT_FILE"
    return 1
  fi

  local age_hours
  age_hours="$(hours_since "$updated_at")"

  if [ "$conclusion" != "success" ]; then
    err "$gate_name gate latest run failed (conclusion=$conclusion, run_id=$run_id)"
    printf '| %s | FAIL | Conclusion `%s` ([run](%s)) |\n' "$gate_name" "$conclusion" "$html_url" >> "$REPORT_FILE"
    return 1
  fi

  if [ "$age_hours" -gt "$MAX_GATE_AGE_HOURS" ]; then
    err "$gate_name gate is stale (${age_hours}h old, max ${MAX_GATE_AGE_HOURS}h)"
    printf '| %s | FAIL | Stale `%sh` > `%sh` ([run](%s)) |\n' "$gate_name" "$age_hours" "$MAX_GATE_AGE_HOURS" "$html_url" >> "$REPORT_FILE"
    return 1
  fi

  ok "$gate_name gate passed via ${run_name} (run_id=${run_id}, age=${age_hours}h)"
  printf '| %s | PASS | `%s`, age `%sh` ([run](%s)) |\n' "$gate_name" "$conclusion" "$age_hours" "$html_url" >> "$REPORT_FILE"
  return 0
}

cat > "$REPORT_FILE" <<EOF_MD
# CI Upstream Gate Validation Report

**Version:** ${VERSION}
**Repository:** ${GITHUB_REPOSITORY}
**Branch:** ${BRANCH_NAME}
**Generated:** $(date -u +"%Y-%m-%dT%H:%M:%SZ")
**Max Gate Age (hours):** ${MAX_GATE_AGE_HOURS}

| Gate | Status | Details |
|------|--------|---------|
EOF_MD

log "Checking upstream CI gates for ${GITHUB_REPOSITORY}@${BRANCH_NAME}..."

status=0
check_workflow_gate "Performance SLO" "$PERFORMANCE_WORKFLOW_FILE" "$REQUIRE_PERFORMANCE_GATE" || status=1
check_workflow_gate "Security Scan" "$SECURITY_WORKFLOW_FILE" "$REQUIRE_SECURITY_GATE" || status=1

if [ "$status" -ne 0 ]; then
  cat >> "$REPORT_FILE" <<EOF_MD

## Go/No-Go

**Decision:** NO-GO

At least one required upstream CI gate failed, is missing, or is stale.
EOF_MD
  err "Upstream CI gates validation failed. Decision: NO-GO"
  exit 1
fi

cat >> "$REPORT_FILE" <<EOF_MD

## Go/No-Go

**Decision:** GO

All required upstream CI gates passed and are within freshness policy.
EOF_MD

ok "Upstream CI gates validation passed. Decision: GO"
log "Report: $REPORT_FILE"
