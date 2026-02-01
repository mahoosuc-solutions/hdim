#!/bin/bash
# HDIM Demo Data Validation
# Verifies seeding, loading, processing, and analytics counts against known expectations.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXPECTED_FILE="${EXPECTED_FILE:-$SCRIPT_DIR/validation/expected-demo-data.json}"
export EXPECTED_FILE

TENANT_ID="${TENANT_ID:-acme-health}"
FHIR_BASE="${FHIR_BASE:-http://localhost:8085/fhir}"
CARE_GAP_BASE="${CARE_GAP_BASE:-http://localhost:8086/care-gap}"
AUTH_USER_ID="${AUTH_USER_ID:-550e8400-e29b-41d4-a716-446655440000}"
AUTH_USERNAME="${AUTH_USERNAME:-demo_user}"
AUTH_ROLES="${AUTH_ROLES:-ADMIN}"
AUTH_VALIDATED="${AUTH_VALIDATED:-gateway-dev}"
TENANT_HEADER="X-Tenant-ID: $TENANT_ID"
AUTH_HEADER_ARGS=(
    -H "X-Auth-User-Id: $AUTH_USER_ID"
    -H "X-Auth-Username: $AUTH_USERNAME"
    -H "X-Auth-Tenant-Ids: $TENANT_ID"
    -H "X-Auth-Roles: $AUTH_ROLES"
    -H "X-Auth-Validated: $AUTH_VALIDATED"
)

PYTHON_BIN="${PYTHON_BIN:-python3}"
if ! command -v "$PYTHON_BIN" >/dev/null 2>&1; then
    PYTHON_BIN=python
fi
if ! command -v "$PYTHON_BIN" >/dev/null 2>&1; then
    echo "Python is required for validation but was not found."
    exit 1
fi

failures=0

echo "========================================"
echo "HDIM Demo - Data Validation"
echo "========================================"

if [ ! -f "$EXPECTED_FILE" ]; then
    echo "Expected data file not found: $EXPECTED_FILE"
    exit 1
fi

expect_patients="$($PYTHON_BIN - <<'PY'
import json, os
data = json.load(open(os.environ["EXPECTED_FILE"]))
print(data["expectedTotals"]["patients"])
PY
)"
expect_gaps="$($PYTHON_BIN - <<'PY'
import json, os
data = json.load(open(os.environ["EXPECTED_FILE"]))
print(data["expectedTotals"]["careGaps"])
PY
)"
expect_high="$($PYTHON_BIN - <<'PY'
import json, os
data = json.load(open(os.environ["EXPECTED_FILE"]))
print(data["expectedTotals"]["priority"]["HIGH"])
PY
)"
expect_medium="$($PYTHON_BIN - <<'PY'
import json, os
data = json.load(open(os.environ["EXPECTED_FILE"]))
print(data["expectedTotals"]["priority"]["MEDIUM"])
PY
)"

echo "Checking service health..."
for port in 8080 8084 8086 8087; do
    if ! curl -s "http://localhost:${port}/actuator/health" >/dev/null 2>&1; then
        echo "  FAIL: service on port ${port} not healthy"
        failures=$((failures + 1))
    else
        echo "  OK: port ${port}"
    fi
done

if ! curl -s "$FHIR_BASE/metadata" >/dev/null 2>&1; then
    echo "  FAIL: FHIR metadata not reachable at $FHIR_BASE"
    failures=$((failures + 1))
else
    echo "  OK: FHIR metadata"
fi

echo ""
echo "Validating seeded patients..."
patient_total="$(
    curl -s "$FHIR_BASE/Patient?_count=200" "${AUTH_HEADER_ARGS[@]}" -H "$TENANT_HEADER" | \
    "$PYTHON_BIN" -c 'import sys, json; data=json.loads(sys.stdin.read() or "{}"); print(data.get("total", 0))'
)"

if [ "$patient_total" -ne "$expect_patients" ]; then
    echo "  FAIL: patient total $patient_total (expected $expect_patients)"
    failures=$((failures + 1))
else
    echo "  OK: patient total $patient_total"
fi

while IFS=$'\t' read -r patient_id patient_name expected_gaps expected_gap_count; do
    status_code="$(
        curl -s -o /dev/null -w "%{http_code}" \
            "${AUTH_HEADER_ARGS[@]}" -H "$TENANT_HEADER" \
            "$FHIR_BASE/Patient/$patient_id"
    )"
    if [ "$status_code" != "200" ]; then
        echo "  FAIL: patient $patient_name ($patient_id) not found"
        failures=$((failures + 1))
    else
        echo "  OK: patient $patient_name"
    fi
done < <("$PYTHON_BIN" - <<'PY'
import json, os
data = json.load(open(os.environ["EXPECTED_FILE"]))
for p in data["patients"]:
    gaps = ",".join(p["expectedGaps"])
    print(f"{p['id']}\t{p['name']}\t{gaps}\t{len(p['expectedGaps'])}")
PY
)

echo ""
echo "Validating care gaps (seeding + processing)..."
gap_total="$(
    curl -s "$CARE_GAP_BASE/api/v1/care-gaps?size=200" "${AUTH_HEADER_ARGS[@]}" -H "$TENANT_HEADER" | \
    "$PYTHON_BIN" -c 'import sys, json; data=json.loads(sys.stdin.read() or "{}"); print(data.get("totalElements", 0))'
)"

if [ "$gap_total" -ne "$expect_gaps" ]; then
    echo "  FAIL: care gap total $gap_total (expected $expect_gaps)"
    failures=$((failures + 1))
else
    echo "  OK: care gap total $gap_total"
fi

gap_high="$(
    curl -s "$CARE_GAP_BASE/api/v1/care-gaps?size=200&priority=HIGH" "${AUTH_HEADER_ARGS[@]}" -H "$TENANT_HEADER" | \
    "$PYTHON_BIN" -c 'import sys, json; data=json.loads(sys.stdin.read() or "{}"); print(data.get("totalElements", 0))'
)"
gap_medium="$(
    curl -s "$CARE_GAP_BASE/api/v1/care-gaps?size=200&priority=MEDIUM" "${AUTH_HEADER_ARGS[@]}" -H "$TENANT_HEADER" | \
    "$PYTHON_BIN" -c 'import sys, json; data=json.loads(sys.stdin.read() or "{}"); print(data.get("totalElements", 0))'
)"

if [ "$gap_high" -ne "$expect_high" ]; then
    echo "  FAIL: high priority gaps $gap_high (expected $expect_high)"
    failures=$((failures + 1))
else
    echo "  OK: high priority gaps $gap_high"
fi
if [ "$gap_medium" -ne "$expect_medium" ]; then
    echo "  FAIL: medium priority gaps $gap_medium (expected $expect_medium)"
    failures=$((failures + 1))
else
    echo "  OK: medium priority gaps $gap_medium"
fi

while IFS=$'\t' read -r patient_id patient_name expected_gaps expected_gap_count; do
    patient_response="$(
        curl -s "$CARE_GAP_BASE/api/v1/care-gaps?size=50&patientId=$patient_id" \
            "${AUTH_HEADER_ARGS[@]}" -H "$TENANT_HEADER"
    )"
    patient_gap_count="$(
        printf "%s" "$patient_response" | "$PYTHON_BIN" -c 'import sys, json; data=json.loads(sys.stdin.read() or "{}"); print(len(data.get("content", [])))'
    )"
    patient_gap_measures="$(
        printf "%s" "$patient_response" | "$PYTHON_BIN" -c 'import sys, json; data=json.loads(sys.stdin.read() or "{}"); measures = sorted({item.get("measureId") for item in data.get("content", []) if item.get("measureId")}); print(",".join(measures))'
    )"

    if [ "$patient_gap_count" -ne "$expected_gap_count" ]; then
        echo "  FAIL: $patient_name gap count $patient_gap_count (expected $expected_gap_count)"
        failures=$((failures + 1))
    else
        echo "  OK: $patient_name gap count $patient_gap_count"
    fi

    if [ "$patient_gap_measures" != "$(echo "$expected_gaps" | tr ',' '\n' | sort | tr '\n' ',' | sed 's/,$//')" ]; then
        echo "  FAIL: $patient_name measures [$patient_gap_measures] (expected $expected_gaps)"
        failures=$((failures + 1))
    else
        echo "  OK: $patient_name measures $patient_gap_measures"
    fi

    stats_response="$(
        curl -s "$CARE_GAP_BASE/stats?patient=$patient_id" "${AUTH_HEADER_ARGS[@]}" -H "$TENANT_HEADER"
    )"
    if printf "%s" "$stats_response" | "$PYTHON_BIN" -c 'import sys, json; data=json.loads(sys.stdin.read() or "{}"); sys.exit(0 if "openGapsCount" in data else 1)' 2>/dev/null; then
        open_count="$(
            printf "%s" "$stats_response" | "$PYTHON_BIN" -c 'import sys, json; data=json.loads(sys.stdin.read() or "{}"); print(data.get("openGapsCount", 0))'
        )"
    else
        stats_response="$(
            curl -s "$CARE_GAP_BASE/care-gap/stats?patient=$patient_id" "${AUTH_HEADER_ARGS[@]}" -H "$TENANT_HEADER"
        )"
        open_count="$(
            printf "%s" "$stats_response" | "$PYTHON_BIN" -c 'import sys, json; data=json.loads(sys.stdin.read() or "{}"); print(data.get("openGapsCount", 0))'
        )"
    fi
    if [ "$open_count" -ne "$expected_gap_count" ]; then
        echo "  FAIL: $patient_name open gaps $open_count (expected $expected_gap_count)"
        failures=$((failures + 1))
    else
        echo "  OK: $patient_name open gaps $open_count"
    fi
done < <("$PYTHON_BIN" - <<'PY'
import json, os
data = json.load(open(os.environ["EXPECTED_FILE"]))
for p in data["patients"]:
    gaps = ",".join(p["expectedGaps"])
    print(f"{p['id']}\t{p['name']}\t{gaps}\t{len(p['expectedGaps'])}")
PY
)

echo ""
if [ "$failures" -eq 0 ]; then
    echo "✅ Validation passed."
else
    echo "❌ Validation failed with $failures issue(s)."
    exit 1
fi
