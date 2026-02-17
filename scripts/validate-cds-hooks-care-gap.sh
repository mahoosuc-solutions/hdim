#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "Running CDS Hooks care-gap integration validation..."
cd "$ROOT_DIR/backend"
./gradlew \
  :modules:services:quality-measure-service:test \
  --tests '*CdsHooksControllerTest' \
  --tests '*CdsHooksCareGapIntegrationTest' \
  --no-daemon

echo "CDS Hooks care-gap validation complete."
