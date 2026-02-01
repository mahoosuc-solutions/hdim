#!/bin/bash
# Targeted reset for demo: clear care gap tables and Redis cache.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-${SCRIPT_DIR}/docker-compose.demo.yml}"
POSTGRES_SERVICE="${POSTGRES_SERVICE:-postgres}"
REDIS_SERVICE="${REDIS_SERVICE:-redis}"
DB_NAME="${DB_NAME:-caregap_db}"
DB_USER="${DB_USER:-healthdata}"

docker compose -f "$COMPOSE_FILE" exec -T "$POSTGRES_SERVICE" \
  psql -U "$DB_USER" -d "$DB_NAME" -c \
  "TRUNCATE TABLE care_gap_recommendations, care_gap_closures, care_gaps RESTART IDENTITY CASCADE;"

docker compose -f "$COMPOSE_FILE" exec -T "$REDIS_SERVICE" \
  redis-cli FLUSHALL >/dev/null

echo "Targeted reset complete (care gaps + cache)."
echo "Next: run demo/seed-demo-data.sh then demo/validate-demo-data.sh or demo/validate-external.sh."
