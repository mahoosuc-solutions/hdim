#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "[dev-shell-deployment] Starting demo stack (docker-compose.demo.yml)..."
docker compose -f docker-compose.demo.yml up -d || true

echo "[dev-shell-deployment] Current stack status:"
docker compose -f docker-compose.demo.yml ps || true

echo "[dev-shell-deployment] Starting MFE deployment remote on :4210..."
npx nx serve mfeDeployment --port=4210 &

echo "[dev-shell-deployment] Starting shell-app on :4300 with mfeDeployment remote..."
npx nx serve shell-app --configuration=development --port=4300 --publicHost=http://localhost:4300 --staticRemotesPort=4400 --devRemotes=mfeDeployment
