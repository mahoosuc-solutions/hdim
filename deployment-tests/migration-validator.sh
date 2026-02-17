#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-healthdata}"
DB_USER="${DB_USER:-healthdata}"

if ! command -v psql >/dev/null 2>&1; then
  echo "psql is required for migration validation"
  exit 1
fi

echo "Checking Flyway history table on ${DB_HOST}:${DB_PORT}/${DB_NAME}"
PGPASSWORD="${DB_PASSWORD:-}" psql \
  -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -c "select installed_rank, version, description, success from flyway_schema_history order by installed_rank desc limit 20;"

echo "Migration validation complete"
