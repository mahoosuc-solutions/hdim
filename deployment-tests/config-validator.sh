#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

required_files=(
  "docker-compose.demo.yml"
  "docker-compose.yml"
  "apps/clinical-portal/src/app/config"
)

for path in "${required_files[@]}"; do
  if [[ ! -e "${ROOT_DIR}/${path}" ]]; then
    echo "Missing required config artifact: ${path}"
    exit 1
  fi
  echo "Found ${path}"
done

if [[ -f "${ROOT_DIR}/.env" ]]; then
  echo "Found .env"
else
  echo "No .env found (allowed for CI); ensure required env vars are provided"
fi

echo "Config validation complete"
