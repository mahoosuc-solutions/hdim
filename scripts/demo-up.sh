#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────
# HDIM Demo Environment — Up + Seed + Validate
#
# Single command to start the demo stack, seed star-ratings data,
# and run smoke tests. Designed for pilot demos and sales calls.
#
# Usage:
#   ./scripts/demo-up.sh                    # Default: docker-compose.demo.yml
#   COMPOSE_FILE=docker-compose.yml ./scripts/demo-up.sh  # Full stack
#
# Environment Variables:
#   COMPOSE_FILE    — Docker Compose file (default: docker-compose.demo.yml)
#   TENANT_ID       — Demo tenant ID (default: demo-tenant)
#   SKIP_SEED       — Set to 1 to skip data seeding
#   SKIP_SMOKE      — Set to 1 to skip smoke tests
# ──────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.demo.yml}"
TENANT_ID="${TENANT_ID:-demo-tenant}"
TENANT_ID_2="${TENANT_ID_2:-acme-health}"
SKIP_SEED="${SKIP_SEED:-0}"
SKIP_SMOKE="${SKIP_SMOKE:-0}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-120}"

# ── Helpers ──────────────────────────────────────────────────────

green()  { printf '\033[0;32m%s\033[0m\n' "$*"; }
red()    { printf '\033[0;31m%s\033[0m\n' "$*"; }
yellow() { printf '\033[0;33m%s\033[0m\n' "$*"; }
bold()   { printf '\033[1m%s\033[0m\n' "$*"; }

timer_start() { START_TIME=$(date +%s); }
timer_elapsed() {
  local elapsed=$(( $(date +%s) - START_TIME ))
  printf '%dm %ds' $((elapsed / 60)) $((elapsed % 60))
}

wait_for_health() {
  local name="$1"
  local url="$2"
  local timeout="${3:-$HEALTH_TIMEOUT}"
  local elapsed=0

  printf "  Waiting for %-30s " "$name..."
  while [[ $elapsed -lt $timeout ]]; do
    local status
    status=$(curl -sS --max-time 5 -o /dev/null -w '%{http_code}' "$url" 2>/dev/null) || true
    if [[ "$status" == "200" ]]; then
      green "✓ (${elapsed}s)"
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  red "✗ (timeout after ${timeout}s)"
  return 1
}

# ── Main ─────────────────────────────────────────────────────────

timer_start

bold ""
bold "╔══════════════════════════════════════════════════╗"
bold "║  HDIM Demo Environment Setup                    ║"
bold "║  Compose: $COMPOSE_FILE"
bold "║  Tenants: $TENANT_ID, $TENANT_ID_2"
bold "╚══════════════════════════════════════════════════╝"
bold ""

# ── Step 1: Start Services ───────────────────────────────────────

bold "Step 1: Starting Docker services..."
cd "$PROJECT_ROOT"
docker compose -f "$COMPOSE_FILE" up -d 2>&1 | tail -5
echo ""

# ── Step 2: Wait for Health ──────────────────────────────────────

bold "Step 2: Waiting for services to become healthy..."

CORE_HEALTHY=true

wait_for_health "PostgreSQL" "http://localhost:5435" 60 2>/dev/null || {
  # PostgreSQL doesn't have an HTTP health endpoint; check via docker
  if docker exec hdim-demo-postgres pg_isready -U healthdata 2>/dev/null | grep -q "accepting"; then
    green "  PostgreSQL...                          ✓ (via pg_isready)"
  else
    wait_for_health "PostgreSQL (container)" "" 60 || CORE_HEALTHY=false
  fi
}

# Check database readiness via pg_isready
PG_CONTAINER=$(docker ps --format '{{.Names}}' | grep -E 'postgres' | head -1)
if [[ -n "$PG_CONTAINER" ]]; then
  PG_READY=false
  for i in $(seq 1 30); do
    if docker exec "$PG_CONTAINER" pg_isready -U healthdata 2>/dev/null | grep -q "accepting"; then
      PG_READY=true
      green "  PostgreSQL ($PG_CONTAINER)              ✓"
      break
    fi
    sleep 2
  done
  if [[ "$PG_READY" == "false" ]]; then
    red "  PostgreSQL ($PG_CONTAINER)              ✗"
    CORE_HEALTHY=false
  fi
fi

# Check care-gap-event-service (required for star ratings)
CARE_GAP_EVENT_PORT="${CARE_GAP_EVENT_PORT:-8111}"
wait_for_health "Care Gap Event Service" "http://localhost:$CARE_GAP_EVENT_PORT/care-gap-event/actuator/health" || CORE_HEALTHY=false

echo ""

if [[ "$CORE_HEALTHY" == "false" ]]; then
  red "Some core services failed to start. Check: docker compose -f $COMPOSE_FILE logs"
  yellow "Continuing with available services..."
fi

# ── Step 3: Seed Demo Data ───────────────────────────────────────

if [[ "$SKIP_SEED" == "1" ]]; then
  yellow "Step 3: Skipping data seeding (SKIP_SEED=1)"
else
  bold "Step 3: Seeding demo data..."

  # Determine DB connection from compose file
  DB_PORT="${DB_PORT:-5435}"
  DB_USER="${DB_USER:-healthdata}"
  DB_PASS="${DB_PASS:-healthdata}"

  # Seed tenant 1
  if TENANT_ID="$TENANT_ID" DB_PORT="$DB_PORT" DB_USER="$DB_USER" DB_PASS="$DB_PASS" \
     bash "$SCRIPT_DIR/seed-star-ratings-demo.sh" 2>&1 | tail -5; then
    green "  Tenant '$TENANT_ID' seeded ✓"
  else
    yellow "  Tenant '$TENANT_ID' seed had warnings (may already be seeded)"
  fi

  # Seed tenant 2
  if TENANT_ID="$TENANT_ID_2" DB_PORT="$DB_PORT" DB_USER="$DB_USER" DB_PASS="$DB_PASS" \
     bash "$SCRIPT_DIR/seed-star-ratings-demo.sh" 2>&1 | tail -5; then
    green "  Tenant '$TENANT_ID_2' seeded ✓"
  else
    yellow "  Tenant '$TENANT_ID_2' seed had warnings (may already be seeded)"
  fi
fi

echo ""

# ── Step 4: Smoke Test ───────────────────────────────────────────

if [[ "$SKIP_SMOKE" == "1" ]]; then
  yellow "Step 4: Skipping smoke tests (SKIP_SMOKE=1)"
else
  bold "Step 4: Running smoke tests..."
  echo ""

  # Quick star-ratings validation
  STAR_OK=true

  star_status=$(curl -sS --max-time 10 -o /dev/null -w '%{http_code}' \
    -H "X-Tenant-ID: $TENANT_ID" \
    "http://localhost:$CARE_GAP_EVENT_PORT/care-gap-event/api/v1/star-ratings/current" 2>/dev/null) || true

  if [[ "$star_status" == "200" ]]; then
    rating=$(curl -sS --max-time 10 \
      -H "X-Tenant-ID: $TENANT_ID" \
      "http://localhost:$CARE_GAP_EVENT_PORT/care-gap-event/api/v1/star-ratings/current" 2>/dev/null \
      | grep -o '"overallRating":[0-9.]*' | cut -d: -f2)
    green "  ✓ Star Rating /current: ${rating:-?} ★ (tenant: $TENANT_ID)"
  else
    red "  ✗ Star Rating /current: HTTP $star_status"
    STAR_OK=false
  fi

  trend_status=$(curl -sS --max-time 10 -o /dev/null -w '%{http_code}' \
    -H "X-Tenant-ID: $TENANT_ID" \
    "http://localhost:$CARE_GAP_EVENT_PORT/care-gap-event/api/v1/star-ratings/trend?weeks=12&granularity=WEEKLY" 2>/dev/null) || true

  if [[ "$trend_status" == "200" ]]; then
    green "  ✓ Star Rating /trend: OK"
  else
    red "  ✗ Star Rating /trend: HTTP $trend_status"
    STAR_OK=false
  fi

  sim_status=$(curl -sS --max-time 10 -o /dev/null -w '%{http_code}' \
    -H "X-Tenant-ID: $TENANT_ID" \
    -H "Content-Type: application/json" \
    -d '{"closures":[{"gapCode":"COL","closures":10}]}' \
    "http://localhost:$CARE_GAP_EVENT_PORT/care-gap-event/api/v1/star-ratings/simulate" 2>/dev/null) || true

  if [[ "$sim_status" == "200" ]]; then
    green "  ✓ Star Rating /simulate: OK"
  else
    yellow "  ⚠ Star Rating /simulate: HTTP $sim_status"
  fi
fi

# ── Summary ──────────────────────────────────────────────────────

ELAPSED=$(timer_elapsed)

bold ""
bold "╔══════════════════════════════════════════════════╗"
bold "║  HDIM Demo Environment Ready                    ║"
bold "╠══════════════════════════════════════════════════╣"

if [[ "${COMPOSE_FILE}" == *"demo"* ]]; then
  bold "║  Gateway:      http://localhost:8080             ║"
else
  bold "║  Gateway:      http://localhost:18080            ║"
fi

bold "║  Star Ratings: http://localhost:$CARE_GAP_EVENT_PORT/care-gap-event/api/v1/star-ratings/current"
bold "║  Tenants:      $TENANT_ID / $TENANT_ID_2"
bold "║  Total time:   $ELAPSED"
bold "╚══════════════════════════════════════════════════╝"
bold ""
bold "Quick test commands:"
echo "  curl http://localhost:$CARE_GAP_EVENT_PORT/care-gap-event/api/v1/star-ratings/current -H 'X-Tenant-ID: $TENANT_ID'"
echo "  curl 'http://localhost:$CARE_GAP_EVENT_PORT/care-gap-event/api/v1/star-ratings/trend?weeks=12&granularity=WEEKLY' -H 'X-Tenant-ID: $TENANT_ID'"
echo ""
