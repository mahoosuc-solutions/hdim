#!/bin/bash
set -euo pipefail

EDGE_URL=${EDGE_URL:-http://localhost:18080}
CUSTOMER_FHIR_URL=${CUSTOMER_FHIR_URL:-http://localhost:8090/fhir}
CDR_CONTAINER=${CDR_CONTAINER:-hdim-demo-customer-cdr}
CDR_DB=${CDR_DB:-customer_cdr}
CDR_USER=${CDR_USER:-healthdata}

tmpdir=$(mktemp -d)
cleanup() { rm -rf "$tmpdir"; }
trap cleanup EXIT

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_cmd curl
require_cmd jq

edge_out="$tmpdir/edge.json"
fhir_out="$tmpdir/fhir.json"

echo "Checking gateway edge health..."
curl --silent --output "$edge_out" "${EDGE_URL}/actuator/health"
jq -e '.status == "UP"' "$edge_out" >/dev/null

echo "Checking customer FHIR metadata..."
curl --silent --output "$fhir_out" "${CUSTOMER_FHIR_URL}/metadata"
jq -e '.resourceType == "CapabilityStatement"' "$fhir_out" >/dev/null

if ! docker ps --format '{{.Names}}' | grep -q "^${CDR_CONTAINER}$"; then
  echo "CDR container not running: ${CDR_CONTAINER}" >&2
  exit 1
fi

echo "Checking CDR seed counts..."
docker exec -i "${CDR_CONTAINER}" psql -U "${CDR_USER}" -d "${CDR_DB}" -c "SELECT count(*) AS encounters FROM encounters;" | grep -q " 4"
docker exec -i "${CDR_CONTAINER}" psql -U "${CDR_USER}" -d "${CDR_DB}" -c "SELECT count(*) AS conditions FROM conditions;" | grep -q " 4"

echo "Demo integration checks passed."
