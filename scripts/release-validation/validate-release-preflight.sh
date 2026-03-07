#!/bin/bash
# Release preflight stability gate
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

VERSION="${1:-${VERSION:-}}"
if [ -z "$VERSION" ]; then
  echo -e "${RED}ERROR: VERSION not specified${NC}"
  echo "Usage: $0 vX.Y.Z[-suffix]"
  exit 1
fi

cd "$(dirname "$0")/../.." || exit 1

REPORT_DIR="docs/releases/${VERSION}/validation"
mkdir -p "$REPORT_DIR"
REPORT_FILE="${REPORT_DIR}/preflight-stability-report.md"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.demo.yml}"

IFS=',' read -r -a REQUIRED_CONTAINERS <<< "${RELEASE_PREFLIGHT_CONTAINERS:-hdim-demo-postgres,hdim-demo-redis,hdim-demo-kafka,hdim-demo-fhir,hdim-demo-patient,hdim-demo-care-gap,hdim-demo-quality-measure,hdim-demo-events,hdim-demo-gateway-fhir,hdim-demo-gateway-admin,hdim-demo-gateway-clinical,hdim-demo-gateway-edge,hdim-demo-seeding,hdim-demo-ops}"

status_for_container() {
  local container="$1"
  docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || echo "missing"
}

OVERALL_STATUS=0
{
  echo "# Release Preflight Stability Report"
  echo
  echo "- Version: \`${VERSION}\`"
  echo "- Compose file: \`${COMPOSE_FILE}\`"
  echo "- Generated at: \`$(date -u +"%Y-%m-%dT%H:%M:%SZ")\`"
  echo
  echo "## Required Containers"
  echo
  echo "| Container | Status | Gate |"
  echo "|---|---|---|"
} > "$REPORT_FILE"

if ! docker info >/dev/null 2>&1; then
  {
    echo
    echo "## ❌ Result: FAIL"
    echo
    echo "Docker daemon is not accessible from this execution context."
    echo "Run this script in an environment with Docker socket access."
  } >> "$REPORT_FILE"
  echo -e "${RED}✗ Release preflight stability gate failed (docker access unavailable)${NC}"
  echo "Report: $REPORT_FILE"
  exit 1
fi

for container in "${REQUIRED_CONTAINERS[@]}"; do
  status="$(status_for_container "$container")"
  status="${status//$'\n'/}"
  status="${status//$'\r'/}"
  gate="PASS"
  if [[ "$status" != "healthy" && "$status" != "running" ]]; then
    gate="FAIL"
    OVERALL_STATUS=1
  fi
  echo "| \`${container}\` | \`${status}\` | ${gate} |" >> "$REPORT_FILE"
done

{
  echo
  echo "## docker compose ps Snapshot"
  echo
  echo '```'
  docker compose -f "$COMPOSE_FILE" ps 2>&1 || true
  echo '```'
  echo
  if [ "$OVERALL_STATUS" -eq 0 ]; then
    echo "## ✅ Result: PASS"
  else
    echo "## ❌ Result: FAIL"
    echo
    echo "One or more required containers are not healthy/running."
  fi
} >> "$REPORT_FILE"

if [ "$OVERALL_STATUS" -eq 0 ]; then
  echo -e "${GREEN}✓ Release preflight stability gate passed${NC}"
else
  echo -e "${RED}✗ Release preflight stability gate failed${NC}"
fi
echo "Report: $REPORT_FILE"

exit "$OVERALL_STATUS"
