#!/usr/bin/env bash
set -euo pipefail

: "${COMPOSE_FILE:=docker-compose.demo.yml}"
: "${SINCE:=30m}"
: "${LOG_DIR:=logs/compose}"
: "${RUN_ID:=$(date +%Y%m%d-%H%M%S)}"

mkdir -p "$LOG_DIR"
LOG_FILE="${LOG_DIR}/compose-${RUN_ID}.log"

{
  echo "============================================"
  echo "Compose Log Capture"
  echo "============================================"
  echo "Run ID: ${RUN_ID}"
  echo "Compose file: ${COMPOSE_FILE}"
  echo "Since: ${SINCE}"
  echo "Timestamp: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
  echo ""
  echo "docker compose ps"
  docker compose -f "${COMPOSE_FILE}" ps || true
  echo ""
  echo "docker compose logs --since ${SINCE}"
  docker compose -f "${COMPOSE_FILE}" logs --since "${SINCE}" || true
} | tee "${LOG_FILE}"

echo ""
echo "Log captured: ${LOG_FILE}"
