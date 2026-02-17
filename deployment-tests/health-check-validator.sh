#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:18080}"
TENANT_ID="${TENANT_ID:-TENANT001}"

check() {
  local path="$1"
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "X-Tenant-ID: ${TENANT_ID}" "${BASE_URL}${path}")
  if [[ "$code" != "200" ]]; then
    echo "FAILED ${path} -> ${code}"
    exit 1
  fi
  echo "OK ${path}"
}

check "/actuator/health"
check "/fhir/metadata"
check "/api/v1/admin/system-health"

echo "Health check validation complete"
