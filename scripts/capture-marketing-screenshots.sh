#!/usr/bin/env bash
set -euo pipefail

##############################################################################
# capture-marketing-screenshots.sh
#
# Captures 70 marketing screenshots (7 roles × 10 screenshots) using
# Playwright against the running Clinical Portal.
#
# Prerequisites:
#   - Clinical Portal running at http://localhost:4200
#   - Backend services (patient, care-gap, quality-measure) running
#   - Playwright browsers installed: npx playwright install chromium
#
# Usage:
#   ./scripts/capture-marketing-screenshots.sh [role]
#   ./scripts/capture-marketing-screenshots.sh              # all 7 roles
#   ./scripts/capture-marketing-screenshots.sh care-manager  # single role
##############################################################################

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
E2E_DIR="$PROJECT_ROOT/apps/clinical-portal-e2e"
SCREENSHOTS_DIR="$PROJECT_ROOT/landing-page-v0/remotion/public/screenshots"

ROLES=(care-manager cmo quality-analyst provider data-analyst admin ai-user)

# Color output helpers
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# Verify portal is running
check_portal() {
  if ! curl -sf http://localhost:4200 > /dev/null 2>&1; then
    log_error "Clinical Portal not reachable at http://localhost:4200"
    log_error "Start with: cd apps/clinical-portal && npm start"
    exit 1
  fi
  log_info "Clinical Portal is running at http://localhost:4200"
}

# Ensure screenshot directories exist
ensure_dirs() {
  for role in "${ROLES[@]}"; do
    mkdir -p "$SCREENSHOTS_DIR/$role"
  done
  log_info "Screenshot directories ready at $SCREENSHOTS_DIR"
}

# Capture screenshots for a single role
capture_role() {
  local role="$1"
  local spec="$E2E_DIR/src/marketing-screenshots/${role}-screenshots.spec.ts"

  if [[ ! -f "$spec" ]]; then
    log_warn "No spec found for role: $role (expected $spec)"
    return 1
  fi

  log_info "Capturing screenshots for: $role"
  cd "$E2E_DIR"
  npx playwright test "$spec" --project=chromium --reporter=list 2>&1 || {
    log_warn "Some screenshots for $role may have failed (UI elements not present in demo mode)"
  }

  # Count captured screenshots
  local count
  count=$(find "$SCREENSHOTS_DIR/$role" -name "*.png" 2>/dev/null | wc -l)
  log_info "$role: $count screenshots captured"
}

# Main
main() {
  check_portal
  ensure_dirs

  if [[ $# -gt 0 ]]; then
    # Single role mode
    capture_role "$1"
  else
    # All roles mode
    log_info "Capturing screenshots for all 7 roles..."
    echo ""

    local total=0
    for role in "${ROLES[@]}"; do
      capture_role "$role"
      local count
      count=$(find "$SCREENSHOTS_DIR/$role" -name "*.png" 2>/dev/null | wc -l)
      total=$((total + count))
      echo ""
    done

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log_info "Total: $total screenshots captured across 7 roles"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  fi
}

main "$@"
