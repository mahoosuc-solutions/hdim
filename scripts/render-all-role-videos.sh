#!/usr/bin/env bash
set -euo pipefail

##############################################################################
# render-all-role-videos.sh
#
# Renders all 14 role story videos (7 roles × 2 variants) using Remotion CLI.
#
# Usage:
#   ./scripts/render-all-role-videos.sh              # all 14 videos
#   ./scripts/render-all-role-videos.sh care-manager  # single role (both variants)
##############################################################################

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
REMOTION_DIR="$PROJECT_ROOT/landing-page-v0/remotion"

ROLES=(care-manager cmo quality-analyst provider data-analyst admin ai-user)

# Color output helpers
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }

# Verify screenshots exist for a role
check_screenshots() {
  local role="$1"
  local dir="$REMOTION_DIR/public/screenshots/$role"
  local count
  count=$(find "$dir" -name "*.png" 2>/dev/null | wc -l)

  if [[ "$count" -eq 0 ]]; then
    log_warn "$role: No screenshots found in $dir"
    log_warn "Run ./scripts/capture-marketing-screenshots.sh $role first"
    return 1
  fi

  log_info "$role: $count screenshots found"
  return 0
}

# Render both variants for a role
render_role() {
  local role="$1"
  cd "$REMOTION_DIR"

  log_info "Rendering: $role (default variant — ~90s)"
  npm run "render:$role" 2>&1

  log_info "Rendering: $role (short variant — ~60s)"
  npm run "render:$role:short" 2>&1

  log_info "$role: Both variants rendered"
}

# Main
main() {
  cd "$REMOTION_DIR"

  if [[ $# -gt 0 ]]; then
    # Single role mode
    check_screenshots "$1" || true
    render_role "$1"
  else
    # All roles mode
    log_info "Rendering all 14 role story videos..."
    echo ""

    local rendered=0
    for role in "${ROLES[@]}"; do
      check_screenshots "$role" || true
      render_role "$role"
      rendered=$((rendered + 2))
      echo ""
    done

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log_info "Total: $rendered videos rendered"
    log_info "Output: $REMOTION_DIR/out/"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    # Copy to public videos directory
    log_info "Copying to landing page public/videos/..."
    mkdir -p "$PROJECT_ROOT/landing-page-v0/public/videos"
    cp "$REMOTION_DIR/out/role-"*.mp4 "$PROJECT_ROOT/landing-page-v0/public/videos/" 2>/dev/null || true
    log_info "Done."
  fi
}

main "$@"
