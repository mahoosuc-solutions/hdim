#!/bin/bash
# External validation wrapper for demo data (strict mode).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXPECTED_FILE="${EXPECTED_FILE:-$SCRIPT_DIR/validation/expected-demo-data.json}"

FHIR_BASE="${FHIR_BASE:-http://localhost:8085/fhir}"
CARE_GAP_BASE="${CARE_GAP_BASE:-http://localhost:8086/care-gap}"
TENANT_ID="${TENANT_ID:-acme-health}"

AUTH_USER_ID="${AUTH_USER_ID:-550e8400-e29b-41d4-a716-446655440000}"
AUTH_USERNAME="${AUTH_USERNAME:-demo_user}"
AUTH_ROLES="${AUTH_ROLES:-ADMIN}"
AUTH_VALIDATED="${AUTH_VALIDATED:-gateway-dev}"

PYTHON_BIN="${PYTHON_BIN:-python3}"
if ! command -v "$PYTHON_BIN" >/dev/null 2>&1; then
  PYTHON_BIN=python
fi
if ! command -v "$PYTHON_BIN" >/dev/null 2>&1; then
  echo "Python is required but was not found."
  exit 1
fi

"$PYTHON_BIN" "$SCRIPT_DIR/external-validate.py" \
  --expected "$EXPECTED_FILE" \
  --tenant-id "$TENANT_ID" \
  --fhir-base "$FHIR_BASE" \
  --care-gap-base "$CARE_GAP_BASE" \
  --auth-user-id "$AUTH_USER_ID" \
  --auth-username "$AUTH_USERNAME" \
  --auth-roles "$AUTH_ROLES" \
  --auth-validated "$AUTH_VALIDATED" \
  --mode strict
