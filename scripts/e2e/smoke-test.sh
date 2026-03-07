#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# HDIM Sprint 4 — E2E Smoke Test
# ---------------------------------------------------------------------------

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

SKIP_STARTUP=false
SCENARIO=0  # 0 = run all

# ---- Parse flags ----------------------------------------------------------
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-startup) SKIP_STARTUP=true; shift ;;
    --scenario)     SCENARIO="${2:?--scenario requires a value}"; shift 2 ;;
    *)              echo -e "${RED}Unknown flag: $1${NC}"; exit 1 ;;
  esac
done

# ---- Service definitions --------------------------------------------------
declare -A SERVICES=(
  [corehive]="http://localhost:8120/corehive-adapter/actuator/health"
  [healthix]="http://localhost:8121/healthix-adapter/actuator/health"
  [hedis]="http://localhost:8122/hedis-adapter/actuator/health"
  [ihe]="http://localhost:8125/ihe-gateway/health"
)

PASS=0
FAIL=0

pass() { echo -e "  ${GREEN}PASS${NC} $1"; ((PASS++)); }
fail() { echo -e "  ${RED}FAIL${NC} $1"; ((FAIL++)); }

# ---- Docker Compose startup (optional) -----------------------------------
if [[ "$SKIP_STARTUP" == false ]]; then
  echo -e "${YELLOW}Starting Docker Compose (external profile)...${NC}"
  docker compose --profile external up -d
fi

# ---- Wait for services to become healthy ----------------------------------
echo -e "${YELLOW}Waiting for services to become healthy...${NC}"
MAX_WAIT=120
INTERVAL=5

for svc in "${!SERVICES[@]}"; do
  url="${SERVICES[$svc]}"
  elapsed=0
  healthy=false
  while [[ $elapsed -lt $MAX_WAIT ]]; do
    if curl -sf --max-time 3 "$url" > /dev/null 2>&1; then
      healthy=true
      break
    fi
    sleep "$INTERVAL"
    elapsed=$((elapsed + INTERVAL))
  done
  if $healthy; then
    echo -e "  ${GREEN}${svc}${NC} is healthy"
  else
    echo -e "  ${RED}${svc}${NC} did not become healthy within ${MAX_WAIT}s"
    FAIL=$((FAIL + 1))
  fi
done

if [[ $FAIL -gt 0 ]]; then
  echo -e "${RED}One or more services failed health checks. Aborting.${NC}"
  exit 2
fi

# ---- Scenario helpers -----------------------------------------------------
should_run() { [[ "$SCENARIO" -eq 0 || "$SCENARIO" -eq "$1" ]]; }

assert_status() {
  local label="$1" expected="$2" actual="$3"
  if [[ "$actual" -eq "$expected" ]]; then
    pass "$label (HTTP $actual)"
  else
    fail "$label (expected $expected, got $actual)"
  fi
}

# ---- Scenario 1: Healthix C-CDA ------------------------------------------
if should_run 1; then
  echo ""
  echo -e "${YELLOW}=== Scenario 1: Healthix C-CDA ===${NC}"

  # Health check
  status=$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8121/healthix-adapter/actuator/health)
  assert_status "Healthix health check" 200 "$status"

  # POST webhook (C-CDA document)
  status=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    -H "Content-Type: application/xml" \
    -d '<ClinicalDocument xmlns="urn:hl7-org:v3"><id root="test"/></ClinicalDocument>' \
    http://localhost:8121/healthix-adapter/api/v1/webhook/ccda)
  assert_status "Healthix C-CDA webhook POST" 200 "$status"

  # Verify metrics endpoint is reachable
  status=$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8121/healthix-adapter/actuator/prometheus)
  assert_status "Healthix metrics endpoint" 200 "$status"
fi

# ---- Scenario 2: hedis measure sync --------------------------------------
if should_run 2; then
  echo ""
  echo -e "${YELLOW}=== Scenario 2: hedis Measure Sync ===${NC}"

  # POST measure sync
  status=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -d '{"measureId":"BCS","reportingYear":2026}' \
    http://localhost:8122/hedis-adapter/api/v1/measures/sync)
  assert_status "hedis measure sync POST" 200 "$status"

  # POST CQL calculate
  status=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -d '{"measureId":"BCS","patientId":"test-001"}' \
    http://localhost:8122/hedis-adapter/api/v1/cql/calculate)
  assert_status "hedis CQL calculate POST" 200 "$status"

  # Verify metrics
  status=$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8122/hedis-adapter/actuator/prometheus)
  assert_status "hedis metrics endpoint" 200 "$status"
fi

# ---- Scenario 3: CoreHive AI scoring -------------------------------------
if should_run 3; then
  echo ""
  echo -e "${YELLOW}=== Scenario 3: CoreHive AI Scoring ===${NC}"

  # POST score
  status=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -d '{"patientId":"synth-001","modelVersion":"v2"}' \
    http://localhost:8120/corehive-adapter/api/v1/ai/score)
  assert_status "CoreHive AI score POST" 200 "$status"

  # POST ROI
  status=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -d '{"programId":"CHF-001","quarter":"Q1-2026"}' \
    http://localhost:8120/corehive-adapter/api/v1/ai/roi)
  assert_status "CoreHive ROI POST" 200 "$status"

  # Verify metrics
  status=$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8120/corehive-adapter/actuator/prometheus)
  assert_status "CoreHive metrics endpoint" 200 "$status"
fi

# ---- Summary --------------------------------------------------------------
echo ""
TOTAL=$((PASS + FAIL))
echo -e "${YELLOW}=== Smoke Test Summary ===${NC}"
echo -e "  Total : $TOTAL"
echo -e "  ${GREEN}Passed: $PASS${NC}"
echo -e "  ${RED}Failed: $FAIL${NC}"

if [[ $FAIL -gt 0 ]]; then
  echo -e "${RED}SMOKE TEST FAILED${NC}"
  exit 2
fi

echo -e "${GREEN}ALL SMOKE TESTS PASSED${NC}"
exit 0
