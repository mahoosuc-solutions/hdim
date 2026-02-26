#!/bin/bash

# Comprehensive Demo Data Seeding Script
# Seeds all required data for service validation

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

: "${LOG_DIR:=logs/seed-runs}"
: "${RUN_ID:=$(date +%Y%m%d-%H%M%S)}"
LOG_FILE="${LOG_DIR}/seed-all-demo-data-${RUN_ID}.log"
mkdir -p "$LOG_DIR"
START_TS="$(date +%s)"
exec > >(tee -a "$LOG_FILE") 2>&1

on_exit() {
    local exit_code=$?
    local end_ts
    end_ts="$(date +%s)"
    local duration=$((end_ts - START_TS))
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Seeding Run Summary${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo "Run ID: ${RUN_ID}"
    echo "Log file: ${LOG_FILE}"
    echo "Exit code: ${exit_code}"
    echo "Duration: ${duration}s"
}
trap on_exit EXIT

DEMO_SEEDING_URL="${DEMO_SEEDING_URL:-http://localhost:8098}"
TENANT_ID="${TENANT_ID:-acme-health}"
NON_INTERACTIVE="${NON_INTERACTIVE:-0}"
WAIT_TIMEOUT_SECS="${WAIT_TIMEOUT_SECS:-240}"
WAIT_SLEEP_SECS="${WAIT_SLEEP_SECS:-3}"
CURL_CONNECT_TIMEOUT="${CURL_CONNECT_TIMEOUT:-5}"
SEED_PROFILE="${SEED_PROFILE:-}"
SMOKE_PATIENT_COUNT="${SMOKE_PATIENT_COUNT:-50}"
FHIR_BASE_URL="${FHIR_BASE_URL:-http://localhost:8085/fhir}"
AUTH_USER_ID="${AUTH_USER_ID:-00000000-0000-0000-0000-000000000001}"
AUTH_USERNAME="${AUTH_USERNAME:-demo-seeder}"
AUTH_ROLES="${AUTH_ROLES:-ADMIN,SYSTEM}"
AUTH_VALIDATED="${AUTH_VALIDATED:-gateway-healthcheck}"
DEMO_TENANT_SOURCE="${DEMO_TENANT_SOURCE:-demo-tenant}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-hdim-demo-postgres}"
POSTGRES_USER="${POSTGRES_USER:-healthdata}"
POSTGRES_CQL_DB="${POSTGRES_CQL_DB:-cql_db}"

# If running under a non-tty (e.g., local CI), force non-interactive mode.
if [[ ! -t 0 ]]; then
    NON_INTERACTIVE="1"
fi

if [[ -z "${SEED_PROFILE}" ]]; then
    if [[ "${NON_INTERACTIVE}" == "1" ]]; then
        SEED_PROFILE="smoke"
    else
        SEED_PROFILE="full"
    fi
fi

if [[ -z "${CURL_MAX_TIME:-}" ]]; then
    if [[ "${NON_INTERACTIVE}" == "1" && "${SEED_PROFILE}" == "smoke" ]]; then
        # Keep local CI deterministic: bound long-running smoke seed requests.
        CURL_MAX_TIME="300"
    else
        CURL_MAX_TIME="1800"
    fi
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Demo Data Seeding Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Wait for demo-seeding-service availability
echo -e "${CYAN}Checking demo-seeding-service availability (timeout: ${WAIT_TIMEOUT_SECS}s)...${NC}"
start_wait="$(date +%s)"
while true; do
    if curl -sf "$DEMO_SEEDING_URL/demo/actuator/health" > /dev/null 2>&1; then
        break
    fi
    now="$(date +%s)"
    elapsed=$((now - start_wait))
    if (( elapsed >= WAIT_TIMEOUT_SECS )); then
        echo -e "${RED}✗ Demo seeding service is not available at $DEMO_SEEDING_URL${NC}"
        echo -e "${YELLOW}Troubleshooting:${NC}"
        echo "  - Ensure demo stack is up: docker compose -f docker-compose.demo.yml up -d"
        echo "  - Check container: docker compose -f docker-compose.demo.yml ps demo-seeding-service"
        echo "  - Logs: docker logs hdim-demo-seeding --tail=200"
        exit 1
    fi
    sleep "${WAIT_SLEEP_SECS}"
done
echo -e "${GREEN}✓ Demo seeding service is available${NC}"
echo ""

# Function to seed data
get_seeded_patient_count() {
    local tmp
    tmp="$(mktemp)"
    if ! curl -sS \
        -H "X-Tenant-ID: ${TENANT_ID}" \
        -H "X-Auth-User-Id: ${AUTH_USER_ID}" \
        -H "X-Auth-Username: ${AUTH_USERNAME}" \
        -H "X-Auth-Tenant-Ids: ${TENANT_ID}" \
        -H "X-Auth-Roles: ${AUTH_ROLES}" \
        -H "X-Auth-Validated: ${AUTH_VALIDATED}" \
        "${FHIR_BASE_URL}/Patient?_summary=count&_count=0" \
        -o "${tmp}" > /dev/null 2>&1; then
        rm -f "${tmp}"
        echo ""
        return
    fi
    python3 - <<PY
import json
try:
  with open("${tmp}", "r", encoding="utf-8") as fh:
    data=json.load(fh)
  print(data.get("total", ""))
except Exception:
  print("")
PY
    rm -f "${tmp}"
}

get_cql_library_count() {
    local tenant="$1"
    docker exec "${POSTGRES_CONTAINER}" psql -U "${POSTGRES_USER}" -d "${POSTGRES_CQL_DB}" -tA -c \
        "SELECT COUNT(*) FROM cql_libraries WHERE tenant_id = '${tenant}';" 2>/dev/null | tr -d '[:space:]' || true
}

sync_cql_libraries_for_tenant() {
    if [[ "${TENANT_ID}" == "${DEMO_TENANT_SOURCE}" ]]; then
        echo -e "${GREEN}✓ CQL library sync not required for tenant ${TENANT_ID}${NC}"
        return 0
    fi

    if ! command -v docker >/dev/null 2>&1; then
        echo -e "${YELLOW}! Docker is unavailable; skipping CQL library sync${NC}"
        return 0
    fi

    if ! docker ps --format '{{.Names}}' | grep -q "^${POSTGRES_CONTAINER}\$"; then
        echo -e "${YELLOW}! Postgres container ${POSTGRES_CONTAINER} not found; skipping CQL library sync${NC}"
        return 0
    fi

    local source_count target_count
    source_count="$(get_cql_library_count "${DEMO_TENANT_SOURCE}")"
    target_count="$(get_cql_library_count "${TENANT_ID}")"

    if [[ ! "${source_count}" =~ ^[0-9]+$ ]]; then
        echo -e "${YELLOW}! Unable to read source CQL library count; skipping sync${NC}"
        return 0
    fi

    if [[ "${target_count}" =~ ^[0-9]+$ ]] && (( target_count > 0 )); then
        echo -e "${GREEN}✓ CQL libraries already present for ${TENANT_ID} (${target_count})${NC}"
        return 0
    fi

    echo -e "${CYAN}Syncing CQL libraries from ${DEMO_TENANT_SOURCE} to ${TENANT_ID}...${NC}"
    docker exec "${POSTGRES_CONTAINER}" psql -U "${POSTGRES_USER}" -d "${POSTGRES_CQL_DB}" -c \
        "INSERT INTO cql_libraries (
            id, tenant_id, name, version, status, cql_content, elm_json, description, publisher,
            created_at, updated_at, created_by, library_name, elm_xml, fhir_library_id, active,
            measure_class, category
        )
        SELECT
            gen_random_uuid(), '${TENANT_ID}', name, version, status, cql_content, elm_json, description, publisher,
            created_at, updated_at, created_by, library_name, elm_xml, fhir_library_id, active,
            measure_class, category
        FROM cql_libraries source
        WHERE source.tenant_id = '${DEMO_TENANT_SOURCE}'
        AND NOT EXISTS (
            SELECT 1 FROM cql_libraries target
            WHERE target.tenant_id = '${TENANT_ID}'
              AND target.name = source.name
              AND target.version = source.version
        );" >/dev/null

    target_count="$(get_cql_library_count "${TENANT_ID}")"
    if [[ "${target_count}" =~ ^[0-9]+$ ]] && (( target_count > 0 )); then
        echo -e "${GREEN}✓ CQL library sync complete for ${TENANT_ID} (${target_count})${NC}"
    else
        echo -e "${YELLOW}! CQL library sync completed but target count is ${target_count:-unknown}${NC}"
    fi
}

seed_data() {
    local name=$1
    local endpoint=$2
    local data=$3
    
    echo -n "Seeding: $name ... "
    
    response=$(curl -s --connect-timeout "${CURL_CONNECT_TIMEOUT}" --max-time "${CURL_MAX_TIME}" -w "\n%{http_code}" -X POST "$endpoint" \
        -H "X-Tenant-ID: $TENANT_ID" \
        -H "Content-Type: application/json" \
        -d "$data" \
        2>&1 || true)
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        echo -e "${GREEN}✓${NC} (HTTP $http_code)"
        
        # Try to extract useful info from response
        if command -v jq &> /dev/null; then
            if echo "$body" | jq -e '.patientsCreated' &> /dev/null; then
                count=$(echo "$body" | jq -r '.patientsCreated // 0')
                echo "  → Created $count patients"
            elif echo "$body" | jq -e '.patientCount' &> /dev/null; then
                count=$(echo "$body" | jq -r '.patientCount // 0')
                echo "  → Loaded $count patients"
            fi
        fi
        return 0
    else
        # If smoke seeding timed out/failed but expected counts are already present,
        # treat it as success to avoid blocking local CI on a late HTTP response.
        if [[ "${NON_INTERACTIVE}" == "1" && "${SEED_PROFILE}" == "smoke" && "$endpoint" == */demo/api/v1/demo/patients/generate ]]; then
            local current_count
            current_count="$(get_seeded_patient_count)"
            if [[ "${current_count}" =~ ^[0-9]+$ ]] && (( current_count >= SMOKE_PATIENT_COUNT )); then
                echo -e "${YELLOW}!${NC} HTTP $http_code from seed endpoint, but tenant ${TENANT_ID} already has ${current_count} patients (>= ${SMOKE_PATIENT_COUNT}); continuing."
                return 0
            fi
        fi
        echo -e "${RED}✗${NC} (HTTP $http_code)"
        echo "  Error: $body"
        return 1
    fi
}

# Seed data using scenarios
sync_cql_libraries_for_tenant

echo -e "${CYAN}=== Seeding Data Using Scenarios ===${NC}"
echo ""

if [[ "${SEED_PROFILE}" == "smoke" ]]; then
    echo -e "${YELLOW}Loading SMOKE seed (fast)...${NC}"
    echo "  This seed includes:"
    echo "  - Small patient set"
    echo "  - Some care gaps for workflow validation"
    echo ""

    seed_data \
        "Generate Patients (smoke)" \
        "$DEMO_SEEDING_URL/demo/api/v1/demo/patients/generate" \
        "{\"count\": ${SMOKE_PATIENT_COUNT}, \"tenantId\": \"${TENANT_ID}\", \"careGapPercentage\": 30}"
else
    # Full demo seed (slower, higher fidelity)
    echo -e "${YELLOW}Loading HEDIS Evaluation Scenario...${NC}"
    echo "  This scenario includes:"
    echo "  - Synthetic patient population"
    echo "  - HEDIS quality measure evaluations"
    echo "  - Care gaps for a portion of patients"
    echo ""

    seed_data \
        "HEDIS Evaluation Scenario" \
        "$DEMO_SEEDING_URL/demo/api/v1/demo/scenarios/hedis-evaluation" \
        "{}"
fi

echo ""

# In CI/non-interactive mode, stop here for determinism.
if [[ "${NON_INTERACTIVE}" == "1" ]]; then
    sync_cql_libraries_for_tenant
    echo -e "${GREEN}✓ Non-interactive mode (${SEED_PROFILE}): skipping optional scenarios${NC}"
    exit 0
fi

# Option 2: Patient Journey Scenario (if needed)
read -p "Load Patient Journey Scenario (1,000 patients)? [y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    seed_data \
        "Patient Journey Scenario" \
        "$DEMO_SEEDING_URL/demo/api/v1/demo/scenarios/patient-journey" \
        "{}"
    echo ""
fi

# Option 3: Risk Stratification Scenario (if needed)
read -p "Load Risk Stratification Scenario (10,000 patients)? [y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    seed_data \
        "Risk Stratification Scenario" \
        "$DEMO_SEEDING_URL/demo/api/v1/demo/scenarios/risk-stratification" \
        "{}"
    echo ""
fi

# Alternative: Direct seeding
echo -e "${CYAN}=== Alternative: Direct Seeding ===${NC}"
echo ""
read -p "Seed additional data directly (100 patients, 30% care gaps)? [y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    seed_data \
        "Direct Patient Seeding" \
        "$DEMO_SEEDING_URL/demo/api/v1/demo/seed" \
        '{"count": 100, "careGapPercentage": 30}'
    echo ""
fi

# Summary
sync_cql_libraries_for_tenant

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Seeding Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${GREEN}✓ Data seeding completed${NC}"
echo ""
echo -e "${CYAN}Next Steps:${NC}"
echo "  1. Wait 30-60 seconds for data to propagate"
echo "  2. Run validation: ./scripts/validate-all-services-data.sh"
echo "  3. Test services in the clinical portal"
echo ""
