#!/usr/bin/env bash
set -euo pipefail

##############################################################################
# marketing-video-pipeline.sh
#
# Full end-to-end pipeline: capture screenshots → render videos.
# One command to regenerate all 70 screenshots and 14 videos.
#
# Prerequisites:
#   - Clinical Portal running at http://localhost:4200
#   - Backend services running (docker compose up -d)
#   - Playwright browsers installed: npx playwright install chromium
#   - Remotion dependencies installed: cd landing-page-v0/remotion && npm install
#
# Usage:
#   ./scripts/marketing-video-pipeline.sh              # full pipeline
#   ./scripts/marketing-video-pipeline.sh --skip-capture # render only (screenshots exist)
#   ./scripts/marketing-video-pipeline.sh --skip-render  # capture only (no rendering)
##############################################################################

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Color output helpers
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_step()  { echo -e "${CYAN}[STEP]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }

SKIP_CAPTURE=false
SKIP_RENDER=false

for arg in "$@"; do
  case $arg in
    --skip-capture) SKIP_CAPTURE=true ;;
    --skip-render)  SKIP_RENDER=true ;;
    *)              log_warn "Unknown argument: $arg" ;;
  esac
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  HDIM Marketing Video Pipeline"
echo "  7 roles × 10 screenshots × 2 video variants"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

START_TIME=$(date +%s)

# Step 1: Capture screenshots
if [[ "$SKIP_CAPTURE" == false ]]; then
  log_step "Step 1/2: Capturing 70 marketing screenshots..."
  echo ""
  bash "$SCRIPT_DIR/capture-marketing-screenshots.sh"
  echo ""
else
  log_step "Step 1/2: Skipped screenshot capture (--skip-capture)"
fi

# Step 2: Render videos
if [[ "$SKIP_RENDER" == false ]]; then
  log_step "Step 2/2: Rendering 14 role story videos..."
  echo ""
  bash "$SCRIPT_DIR/render-all-role-videos.sh"
  echo ""
else
  log_step "Step 2/2: Skipped video rendering (--skip-render)"
fi

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log_info "Pipeline complete in ${ELAPSED}s"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "  Screenshots: landing-page-v0/remotion/public/screenshots/"
echo "  Videos:      landing-page-v0/remotion/out/"
echo "  Preview:     cd landing-page-v0/remotion && npm run dev"
echo ""
