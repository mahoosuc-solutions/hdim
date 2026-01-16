#!/bin/bash
# Full reset for demo: stop services, remove volumes, then restart.
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-demo/docker-compose.demo.yml}"

docker compose -f "$COMPOSE_FILE" down -v
docker compose -f "$COMPOSE_FILE" up -d

echo "Full reset complete."
echo "Next: run demo/seed-demo-data.sh then demo/validate-demo-data.sh or demo/validate-external.sh."
