#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:18080}"
TENANT_ID="${TENANT_ID:-TENANT001}"

smoke() {
  local name="$1"
  local path="$2"
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "X-Tenant-ID: ${TENANT_ID}" "${BASE_URL}${path}")
  if [[ "$code" =~ ^2 ]]; then
    echo "PASS ${name}: ${path} (${code})"
  else
    echo "FAIL ${name}: ${path} (${code})"
    exit 1
  fi
}

smoke "gateway-health" "/actuator/health"
smoke "fhir-capability" "/fhir/metadata"
smoke "patient-search" "/api/v1/patients/search?q=john"
smoke "care-gaps" "/api/v1/care-gaps?page=0&size=5"

echo "Smoke tests complete"
