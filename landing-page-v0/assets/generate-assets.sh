#!/bin/bash
#
# HDIM Landing Page Visual Asset Generator
# Wrapper script for easy asset generation
#
# Usage:
#   ./generate-assets.sh              # Show help
#   ./generate-assets.sh all          # Generate all assets
#   ./generate-assets.sh hero         # Generate hero images
#   ./generate-assets.sh portraits    # Generate patient portraits
#   ./generate-assets.sh list         # List all available assets
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_SCRIPT="$SCRIPT_DIR/generate_landing_page_assets.py"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# API Key - MUST be set via environment variable
if [[ -z "$GOOGLE_API_KEY" ]]; then
    echo -e "${RED}ERROR: GOOGLE_API_KEY environment variable not set${NC}"
    echo "Set it with: export GOOGLE_API_KEY='your-api-key'"
    exit 1
fi
export GOOGLE_API_KEY

echo -e "${BLUE}"
echo "=================================================="
echo "  HDIM Landing Page Visual Asset Generator"
echo "=================================================="
echo -e "${NC}"

# Check Python and curl dependencies
check_dependencies() {
    echo -e "${YELLOW}Checking dependencies...${NC}"

    if ! command -v python3 &> /dev/null; then
        echo -e "${RED}ERROR: Python 3 not found${NC}"
        exit 1
    fi

    if ! command -v curl &> /dev/null; then
        echo -e "${RED}ERROR: curl not found${NC}"
        exit 1
    fi

    echo -e "${GREEN}Dependencies OK (python3, curl)${NC}"
}

show_help() {
    echo "Usage: $0 [command] [options]"
    echo ""
    echo "Commands:"
    echo "  all         Generate all visual assets (26 total)"
    echo "  list        List all available assets"
    echo ""
    echo "Categories:"
    echo "  hero        Hero images (3 assets)"
    echo "  portraits   Patient portraits - Maria, James, Sarah (3 assets)"
    echo "  story       The 5-minute story visuals (2 assets)"
    echo "  technical   Architecture diagrams (4 assets)"
    echo "  icons       Trust badge icons (6 assets)"
    echo "  dashboard   Dashboard mockups (2 assets)"
    echo "  comparison  Before/after comparison (1 asset)"
    echo "  social      Social media templates (2 assets)"
    echo "  video       Video thumbnails (2 assets)"
    echo "  email       Email graphics (1 asset)"
    echo ""
    echo "Single asset:"
    echo "  asset ID    Generate specific asset (e.g., HERO-01, PORTRAIT-MARIA)"
    echo ""
    echo "Options:"
    echo "  --dry-run   Show what would be generated without calling API"
    echo "  --output    Specify output directory (default: ./generated)"
    echo ""
    echo "Examples:"
    echo "  $0 all                  # Generate all 26 assets"
    echo "  $0 hero                 # Generate hero images only"
    echo "  $0 PORTRAIT-MARIA       # Generate Maria's portrait"
    echo "  $0 list                 # Show all available assets"
    echo ""
    echo "API Key: ${GOOGLE_API_KEY:0:10}...${GOOGLE_API_KEY: -4}"
}

# Main
case "${1:-help}" in
    all)
        check_dependencies
        echo -e "${BLUE}Generating ALL assets...${NC}"
        python3 "$PYTHON_SCRIPT" --all --output "$SCRIPT_DIR/generated"
        ;;
    list)
        python3 "$PYTHON_SCRIPT" --list
        ;;
    hero|portraits|story|technical|icons|dashboard|comparison|social|video|email)
        check_dependencies
        echo -e "${BLUE}Generating $1 category...${NC}"
        python3 "$PYTHON_SCRIPT" --category "$1" --output "$SCRIPT_DIR/generated"
        ;;
    HERO-*|PORTRAIT-*|STORY-*|TECH-*|ICON-*|DASHBOARD-*|COMPARISON|SOCIAL-*|VIDEO-*|EMAIL-*)
        check_dependencies
        echo -e "${BLUE}Generating asset: $1${NC}"
        python3 "$PYTHON_SCRIPT" --asset "$1" --output "$SCRIPT_DIR/generated"
        ;;
    --dry-run)
        python3 "$PYTHON_SCRIPT" --all --dry-run
        ;;
    help|--help|-h|"")
        show_help
        ;;
    *)
        echo -e "${RED}Unknown command: $1${NC}"
        echo ""
        show_help
        exit 1
        ;;
esac
