#!/bin/bash
# Intelligence Engine Readiness Gate
# Validates DB migrations, Kafka topics, and endpoint liveness for event-processing-service intelligence APIs.
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

VERSION="${1:-${VERSION:-}}"
if [ -z "$VERSION" ]; then
  echo -e "${RED}ERROR: VERSION not specified${NC}"
  echo "Usage: $0 vX.Y.Z"
  exit 1
fi

cd "$(dirname "$0")/../.." || exit 1

REPORT_DIR="docs/releases/${VERSION}/validation"
mkdir -p "$REPORT_DIR"
REPORT_FILE="$REPORT_DIR/intelligence-readiness-validation-report.md"

# Connection and target settings
EVENT_SERVICE_BASE_URL="${EVENT_SERVICE_BASE_URL:-http://localhost:8083/events}"
EVENT_TENANT_ID="${EVENT_TENANT_ID:-tenant-a}"
KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-localhost:9094}"
KAFKA_CONTAINER="${KAFKA_CONTAINER:-}"
DB_CONTAINER="${DB_CONTAINER:-}"
DB_NAME="${DB_NAME:-event_db}"
DB_USER="${DB_USER:-healthdata}"
DATABASE_URL="${DATABASE_URL:-}"
CHECK_MIGRATIONS="${CHECK_MIGRATIONS:-true}"
CHECK_KAFKA_TOPICS="${CHECK_KAFKA_TOPICS:-true}"
CHECK_ENDPOINTS="${CHECK_ENDPOINTS:-true}"

REQUIRED_TOPICS=(
  "ingest.raw"
  "ingest.normalized"
  "validation.findings"
  "intelligence.signals"
  "recommendations.generated"
  "recommendations.reviewed"
)

REQUIRED_CHANGESETS=(
  "0005-create-intelligence-recommendations-table"
  "0006-create-intelligence-validation-findings-table"
  "0007-create-intelligence-tenant-trust-projection-table"
)

cat > "$REPORT_FILE" <<EOF_REPORT
# Intelligence Readiness Validation Report

## Overview
Validates intelligence deployment gates for event-processing-service:
- Liquibase migrations applied
- Kafka intelligence topics reachable
- Service and intelligence endpoint liveness

## Runtime Inputs
- VERSION: ${VERSION}
- EVENT_SERVICE_BASE_URL: ${EVENT_SERVICE_BASE_URL}
- EVENT_TENANT_ID: ${EVENT_TENANT_ID}
- KAFKA_BOOTSTRAP_SERVERS: ${KAFKA_BOOTSTRAP_SERVERS}
- DB_CONTAINER: ${DB_CONTAINER:-<none>}
- DATABASE_URL: ${DATABASE_URL:-<none>}
- CHECK_MIGRATIONS: ${CHECK_MIGRATIONS}
- CHECK_KAFKA_TOPICS: ${CHECK_KAFKA_TOPICS}
- CHECK_ENDPOINTS: ${CHECK_ENDPOINTS}

---
EOF_REPORT

OVERALL_STATUS=0

log_ok() {
  echo -e "${GREEN}✓${NC} $1"
  echo "- ✅ $1" >> "$REPORT_FILE"
}

log_warn() {
  echo -e "${YELLOW}⚠${NC} $1"
  echo "- ⚠️ $1" >> "$REPORT_FILE"
}

log_fail() {
  echo -e "${RED}✗${NC} $1"
  echo "- ❌ $1" >> "$REPORT_FILE"
  OVERALL_STATUS=1
}

run_db_scalar() {
  local sql="$1"
  if [ -n "$DB_CONTAINER" ]; then
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -tAc "$sql" 2>/dev/null
    return
  fi

  if [ -n "$DATABASE_URL" ]; then
    psql "$DATABASE_URL" -tAc "$sql" 2>/dev/null
    return
  fi

  if [ -n "${PGHOST:-}" ]; then
    psql -d "${PGDATABASE:-$DB_NAME}" -U "${PGUSER:-$DB_USER}" -tAc "$sql" 2>/dev/null
    return
  fi

  return 1
}

check_migrations() {
  echo "" >> "$REPORT_FILE"
  echo "## Migration Validation" >> "$REPORT_FILE"

  for changeset in "${REQUIRED_CHANGESETS[@]}"; do
    local count
    count="$(run_db_scalar "SELECT COUNT(*) FROM databasechangelog WHERE id='${changeset}'" || true)"
    count="$(echo "$count" | tr -d '[:space:]')"
    if [ "$count" = "1" ] || [ "$count" = "2" ] || [ "$count" = "3" ]; then
      log_ok "Liquibase changeset applied: ${changeset}"
    else
      log_fail "Liquibase changeset missing: ${changeset}"
    fi
  done

  # Confirm required tables exist
  for table_name in intelligence_recommendations intelligence_validation_findings intelligence_tenant_trust_projection; do
    local exists
    exists="$(run_db_scalar "SELECT to_regclass('public.${table_name}')" || true)"
    exists="$(echo "$exists" | tr -d '[:space:]')"
    if [ "$exists" = "public.${table_name}" ]; then
      log_ok "Table exists: public.${table_name}"
    else
      log_fail "Table missing: public.${table_name}"
    fi
  done
}

list_topics() {
  if command -v kafka-topics >/dev/null 2>&1; then
    kafka-topics --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" --list 2>/dev/null || return 1
    return 0
  fi

  if [ -n "$KAFKA_CONTAINER" ]; then
    docker exec "$KAFKA_CONTAINER" kafka-topics --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" --list 2>/dev/null || return 1
    return 0
  fi

  return 1
}

check_kafka_topics() {
  echo "" >> "$REPORT_FILE"
  echo "## Kafka Topic Validation" >> "$REPORT_FILE"

  local topics
  topics="$(list_topics || true)"

  if [ -z "$topics" ]; then
    log_fail "Unable to query Kafka topics (set kafka-topics CLI or KAFKA_CONTAINER)"
    return
  fi

  for topic in "${REQUIRED_TOPICS[@]}"; do
    if echo "$topics" | grep -Fxq "$topic"; then
      log_ok "Kafka topic reachable: ${topic}"
    else
      log_fail "Kafka topic missing: ${topic}"
    fi
  done
}

check_endpoint_code() {
  local url="$1"
  local method="${2:-GET}"
  local code

  if [ "$method" = "GET" ]; then
    code="$(curl -sS -o /dev/null -w "%{http_code}" "$url" \
      -H "X-Tenant-ID: ${EVENT_TENANT_ID}" || true)"
  else
    code="$(curl -sS -o /dev/null -w "%{http_code}" -X "$method" "$url" \
      -H "X-Tenant-ID: ${EVENT_TENANT_ID}" || true)"
  fi

  echo "$code"
}

check_endpoint_liveness() {
  echo "" >> "$REPORT_FILE"
  echo "## Endpoint Liveness Validation" >> "$REPORT_FILE"

  local health_code
  health_code="$(curl -sS -o /dev/null -w "%{http_code}" "${EVENT_SERVICE_BASE_URL}/actuator/health" || true)"
  if [ "$health_code" = "200" ]; then
    log_ok "Actuator health endpoint responded 200"
  else
    log_fail "Actuator health endpoint failed (HTTP ${health_code})"
  fi

  local dashboard_code
  dashboard_code="$(check_endpoint_code "${EVENT_SERVICE_BASE_URL}/api/v1/intelligence/tenants/${EVENT_TENANT_ID}/trust-dashboard")"

  case "$dashboard_code" in
    200|401|403|404)
      log_ok "Intelligence trust dashboard endpoint reachable (HTTP ${dashboard_code})"
      ;;
    *)
      log_fail "Intelligence trust dashboard endpoint unhealthy (HTTP ${dashboard_code})"
      ;;
  esac
}

echo "Intelligence Readiness Validation - Version: $VERSION"

if [ "$CHECK_MIGRATIONS" = "true" ]; then
  check_migrations
else
  log_warn "Skipping migration validation (CHECK_MIGRATIONS=false)"
fi

if [ "$CHECK_KAFKA_TOPICS" = "true" ]; then
  check_kafka_topics
else
  log_warn "Skipping Kafka topic validation (CHECK_KAFKA_TOPICS=false)"
fi

if [ "$CHECK_ENDPOINTS" = "true" ]; then
  check_endpoint_liveness
else
  log_warn "Skipping endpoint liveness validation (CHECK_ENDPOINTS=false)"
fi

echo "" >> "$REPORT_FILE"
if [ "$OVERALL_STATUS" -eq 0 ]; then
  echo "### ✅ Overall Status: PASSED" >> "$REPORT_FILE"
  echo -e "${GREEN}Intelligence readiness validation PASSED${NC}"
else
  echo "### ❌ Overall Status: FAILED" >> "$REPORT_FILE"
  echo -e "${RED}Intelligence readiness validation FAILED${NC}"
fi

echo "Report: $REPORT_FILE"
exit "$OVERALL_STATUS"
