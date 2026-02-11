#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "[dev-shell-all] Starting demo stack (docker-compose.demo.yml)..."
docker compose -f docker-compose.demo.yml up -d

echo "[dev-shell-all] Starting core MFEs..."
npx nx serve mfeDeployment --port=4210 &
npx nx serve mfePatients --port=4201 &
npx nx serve mfeMeasureBuilder --port=4202 &

echo "[dev-shell-all] Starting shell-app on :4300 with all remotes..."
npx nx serve shell-app --configuration=development --port=4300 --publicHost=http://localhost:4300 --staticRemotesPort=4400 --devRemotes=mfeDeployment,mfePatients,mfeMeasureBuilder
