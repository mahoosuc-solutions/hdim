#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "[smart-conformance] Running deterministic SMART conformance lane..."
cd "${ROOT_DIR}/backend"

./gradlew :modules:services:fhir-service:test \
  --tests '*SmartConformanceLaneTest' \
  --tests '*SmartAuthorizationServiceTest' \
  --tests '*SmartLaunchContextStoreTest' \
  --no-daemon

echo "[smart-conformance] Completed successfully."
