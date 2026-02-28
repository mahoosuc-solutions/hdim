#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

BUILD_WAVE1_IMAGES=true ./scripts/validation/run-wave1-local-assurance.sh "$@"
