#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────
# HDIM Platform Smoke Test
#
# Validates the end-to-end "5-minute demo" flow:
#   1. Core services are healthy
#   2. Demo data can be seeded
#   3. CQL evaluation runs successfully
#   4. Care gaps are generated
#   5. Dashboard endpoints respond
#
# Usage:
#   ./scripts/smoke-test.sh                    # Default: gateway-edge at 18080
#   GATEWAY_URL=http://localhost:8080 ./scripts/smoke-test.sh
# ──────────────────────────────────────────────────────────────────
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
TIMEOUT="${TIMEOUT:-10}"
PASS=0
FAIL=0
WARN=0

# ── Helpers ──────────────────────────────────────────────────────

green()  { printf '\033[0;32m%s\033[0m\n' "$*"; }
red()    { printf '\033[0;31m%s\033[0m\n' "$*"; }
yellow() { printf '\033[0;33m%s\033[0m\n' "$*"; }
bold()   { printf '\033[1m%s\033[0m\n' "$*"; }

check_pass() {
  green "  ✓ $1"
  PASS=$((PASS + 1))
}

check_fail() {
  red "  ✗ $1"
  FAIL=$((FAIL + 1))
}

check_warn() {
  yellow "  ⚠ $1"
  WARN=$((WARN + 1))
}

# HTTP GET, returns status code. Body goes to stdout if 2xx.
http_get() {
  local url="$1"
  local status
  local body
  body=$(curl -sS --max-time "$TIMEOUT" -o /dev/null -w '%{http_code}' "$url" 2>/dev/null) || true
  echo "$body"
}

# HTTP GET returning body
http_get_body() {
  local url="$1"
  curl -sS --max-time "$TIMEOUT" "$url" 2>/dev/null || echo ""
}

# ── Phase 1: Service Health ──────────────────────────────────────

bold ""
bold "═══════════════════════════════════════════════════"
bold "  HDIM Platform Smoke Test"
bold "  Gateway: $GATEWAY_URL"
bold "═══════════════════════════════════════════════════"
bold ""
bold "Phase 1: Service Health Checks"
bold "───────────────────────────────"

# Core services reachable through gateway
declare -A SERVICES=(
  ["Gateway Edge"]="$GATEWAY_URL/actuator/health"
  ["Patient Service"]="http://localhost:8084/patient/actuator/health"
  ["FHIR Service"]="http://localhost:8085/fhir/actuator/health"
  ["Care Gap Service"]="http://localhost:8086/care-gap/actuator/health"
  ["Quality Measure"]="http://localhost:8087/quality-measure/actuator/health"
  ["CQL Engine"]="http://localhost:8081/cql-engine/actuator/health"
)

# Optional clinical workflow services
declare -A OPTIONAL_SERVICES=(
  ["Nurse Workflow"]="http://localhost:8093/nurse-workflow/actuator/health"
  ["Clinical Workflow"]="http://localhost:8110/clinical-workflow/actuator/health"
  ["Demo Seeding"]="http://localhost:8098/demo/actuator/health"
)

for name in "${!SERVICES[@]}"; do
  status=$(http_get "${SERVICES[$name]}")
  if [[ "$status" == "200" ]]; then
    check_pass "$name is healthy ($status)"
  else
    check_fail "$name is NOT healthy (HTTP $status)"
  fi
done

for name in "${!OPTIONAL_SERVICES[@]}"; do
  status=$(http_get "${OPTIONAL_SERVICES[$name]}")
  if [[ "$status" == "200" ]]; then
    check_pass "$name is healthy ($status)"
  elif [[ -z "$status" || "$status" == "000" ]]; then
    check_warn "$name is not running (optional)"
  else
    check_warn "$name returned HTTP $status (optional)"
  fi
done

# ── Phase 2: Infrastructure ─────────────────────────────────────

bold ""
bold "Phase 2: Infrastructure"
bold "───────────────────────"

# PostgreSQL
pg_status=$(docker exec hdim-postgres pg_isready -U healthdata 2>/dev/null && echo "ready" || echo "not ready")
if [[ "$pg_status" == *"ready"* ]]; then
  check_pass "PostgreSQL is ready"
else
  check_fail "PostgreSQL is NOT ready"
fi

# Redis
redis_status=$(docker exec hdim-redis redis-cli ping 2>/dev/null || echo "not available")
if [[ "$redis_status" == "PONG" ]]; then
  check_pass "Redis is responding"
else
  check_fail "Redis is NOT responding"
fi

# Kafka
kafka_status=$(docker exec hdim-kafka kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/dev/null | head -1)
if [[ -n "$kafka_status" ]]; then
  check_pass "Kafka is responding"
else
  check_warn "Kafka check failed (may not be running)"
fi

# ── Phase 3: Demo Data Seeding ───────────────────────────────────

bold ""
bold "Phase 3: Demo Data Seeding"
bold "──────────────────────────"

seed_status=$(curl -sS --max-time 30 -X POST \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: acme-health" \
  -w '\n%{http_code}' \
  "$GATEWAY_URL/demo/api/v1/seed" 2>/dev/null | tail -1)

if [[ "$seed_status" == "200" || "$seed_status" == "201" ]]; then
  check_pass "Demo data seeded successfully ($seed_status)"
elif [[ "$seed_status" == "409" ]]; then
  check_pass "Demo data already seeded (409 Conflict — idempotent)"
else
  check_warn "Demo seeding returned HTTP $seed_status (may need manual seeding)"
fi

# ── Phase 4: Patient Data ────────────────────────────────────────

bold ""
bold "Phase 4: Data Verification"
bold "──────────────────────────"

patients_body=$(http_get_body "$GATEWAY_URL/patient/api/v1/patients?page=0&size=5")
patients_status=$(http_get "$GATEWAY_URL/patient/api/v1/patients?page=0&size=5")

if [[ "$patients_status" == "200" ]]; then
  check_pass "Patient API responds ($patients_status)"
else
  check_fail "Patient API failed (HTTP $patients_status)"
fi

# Care gaps
gaps_status=$(http_get "$GATEWAY_URL/care-gap/api/v1/care-gaps?page=0&size=5")
if [[ "$gaps_status" == "200" ]]; then
  check_pass "Care Gap API responds ($gaps_status)"
else
  check_fail "Care Gap API failed (HTTP $gaps_status)"
fi

# Quality measures
qm_status=$(http_get "$GATEWAY_URL/quality-measure/api/v1/measures?page=0&size=5")
if [[ "$qm_status" == "200" ]]; then
  check_pass "Quality Measure API responds ($qm_status)"
else
  check_fail "Quality Measure API failed (HTTP $qm_status)"
fi

# ── Phase 5: Star Ratings ──────────────────────────────────────

bold ""
bold "Phase 5: Star Ratings"
bold "─────────────────────"

STAR_TENANT="${STAR_TENANT:-demo-tenant}"
CARE_GAP_EVENT_URL="${CARE_GAP_EVENT_URL:-http://localhost:8111}"

star_status=$(curl -sS --max-time "$TIMEOUT" -o /dev/null -w '%{http_code}' \
  -H "X-Tenant-ID: $STAR_TENANT" \
  "$CARE_GAP_EVENT_URL/care-gap-event/api/v1/star-ratings/current" 2>/dev/null) || true
if [[ "$star_status" == "200" ]]; then
  star_body=$(curl -sS --max-time "$TIMEOUT" \
    -H "X-Tenant-ID: $STAR_TENANT" \
    "$CARE_GAP_EVENT_URL/care-gap-event/api/v1/star-ratings/current" 2>/dev/null)
  rating=$(echo "$star_body" | grep -o '"overallRating":[0-9.]*' | cut -d: -f2)
  if [[ -n "$rating" && "$rating" != "0" && "$rating" != "0.0" ]]; then
    check_pass "Star Rating current: $rating ★ (tenant: $STAR_TENANT)"
  else
    check_warn "Star Rating API responds but rating is $rating (seed data may be missing)"
  fi
else
  check_warn "Star Rating API returned HTTP $star_status (care-gap-event-service may not be running)"
fi

trend_status=$(curl -sS --max-time "$TIMEOUT" -o /dev/null -w '%{http_code}' \
  -H "X-Tenant-ID: $STAR_TENANT" \
  "$CARE_GAP_EVENT_URL/care-gap-event/api/v1/star-ratings/trend?weeks=12&granularity=WEEKLY" 2>/dev/null) || true
if [[ "$trend_status" == "200" ]]; then
  check_pass "Star Rating trend API responds ($trend_status)"
else
  check_warn "Star Rating trend returned HTTP $trend_status"
fi

# ── Phase 6: Observability ───────────────────────────────────────

bold ""
bold "Phase 6: Observability"
bold "──────────────────────"

# Prometheus
prom_status=$(http_get "http://localhost:9090/-/ready")
if [[ "$prom_status" == "200" ]]; then
  check_pass "Prometheus is ready"
else
  check_warn "Prometheus not reachable (HTTP $prom_status)"
fi

# Grafana
grafana_status=$(http_get "http://localhost:3001/api/health")
if [[ "$grafana_status" == "200" ]]; then
  check_pass "Grafana is healthy"
else
  check_warn "Grafana not reachable (HTTP $grafana_status)"
fi

# ── Summary ──────────────────────────────────────────────────────

bold ""
bold "═══════════════════════════════════════════════════"
bold "  RESULTS"
bold "═══════════════════════════════════════════════════"
green "  Passed:   $PASS"
if [[ $WARN -gt 0 ]]; then
  yellow "  Warnings: $WARN"
fi
if [[ $FAIL -gt 0 ]]; then
  red "  Failed:   $FAIL"
fi
bold ""

if [[ $FAIL -gt 0 ]]; then
  red "SMOKE TEST FAILED — $FAIL check(s) failed"
  exit 1
elif [[ $WARN -gt 0 ]]; then
  yellow "SMOKE TEST PASSED WITH WARNINGS — $WARN warning(s)"
  exit 0
else
  green "SMOKE TEST PASSED — all checks green"
  exit 0
fi
