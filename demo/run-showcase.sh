#!/usr/bin/env bash
# HDIM Demo Showcase Runner
# - Starts the full demo stack
# - Waits for health
# - Seeds demo data
# - Runs Playwright walkthrough (headed)
# - Records screen via ffmpeg (optional)

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEMO_DIR="${ROOT_DIR}/demo"
SHOWCASE_DIR="${DEMO_DIR}/recordings"
mkdir -p "${SHOWCASE_DIR}"

timestamp="$(date +%Y%m%d_%H%M%S)"
video_file="${SHOWCASE_DIR}/demo-showcase-${timestamp}.mp4"

RUN_BUILD=false
RUN_CLEAN=false
RECORD_VIDEO=false

for arg in "$@"; do
  case "$arg" in
    --build)
      RUN_BUILD=true
      ;;
    --clean)
      RUN_CLEAN=true
      ;;
    --record)
      RECORD_VIDEO=true
      ;;
    *)
      echo "Unknown option: $arg"
      echo "Usage: $0 [--clean] [--build] [--record]"
      exit 1
      ;;
  esac
done

start_args=()
if [ "$RUN_BUILD" = true ]; then
  start_args+=("--build")
fi
if [ "$RUN_CLEAN" = true ]; then
  start_args+=("--clean")
fi

echo "Starting demo stack..."
"${DEMO_DIR}/start-demo.sh" "${start_args[@]}"

echo "Seeding demo data..."
"${DEMO_DIR}/seed-demo-data.sh" --reset --wait

echo "Validating demo data (lenient)..."
"${DEMO_DIR}/validate-demo-data-lenient.sh"

record_pid=""
if [ "$RECORD_VIDEO" = true ]; then
  if command -v ffmpeg >/dev/null 2>&1; then
    display="${DISPLAY:-:0.0}"
    echo "Recording demo to ${video_file} (display ${display})"
    ffmpeg -y -video_size 1920x1080 -framerate 30 -f x11grab -i "${display}" \
      -vcodec libx264 -preset veryfast -pix_fmt yuv420p "${video_file}" >/dev/null 2>&1 &
    record_pid=$!
  else
    echo "ffmpeg not found. Skipping recording."
  fi
fi

echo "Running Playwright demo walkthrough (headed)..."
DEMO_BASE_URL=${DEMO_BASE_URL:-http://localhost:4200}
BASE_URL="${DEMO_BASE_URL}" npx playwright test \
  apps/clinical-portal-e2e/src/demo-walkthrough.spec.ts \
  --config apps/clinical-portal-e2e/playwright.config.ts \
  --project=chromium \
  --headed

if [ -n "$record_pid" ]; then
  echo "Stopping recording..."
  kill "$record_pid" >/dev/null 2>&1 || true
fi

echo "Showcase complete."
echo "Screenshots: apps/clinical-portal-e2e/playwright-report (or test-results)"
if [ "$RECORD_VIDEO" = true ]; then
  echo "Video: ${video_file}"
fi
