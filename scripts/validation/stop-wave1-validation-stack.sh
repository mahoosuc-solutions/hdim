#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.demo.yml}"
KEEP_VOLUMES="${KEEP_VOLUMES:-false}"

PAYER_CONTAINER="${PAYER_CONTAINER:-hdim-demo-payer-workflows}"
INGEST_CONTAINER="${INGEST_CONTAINER:-hdim-demo-data-ingestion}"

echo "Stopping Wave-1 standalone containers"
docker rm -f "$PAYER_CONTAINER" "$INGEST_CONTAINER" >/dev/null 2>&1 || true

echo "Stopping demo stack"
if [[ "$KEEP_VOLUMES" == "true" ]]; then
  docker compose -f "$COMPOSE_FILE" down --remove-orphans
else
  docker compose -f "$COMPOSE_FILE" down -v --remove-orphans
fi

echo "Wave-1 validation stack stopped."
