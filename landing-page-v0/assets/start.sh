#!/bin/bash
# HDIM AI Image Generation with Quality Control - Quick Start Script

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "\n${BLUE}╔══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}  $1"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════╝${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Check dependencies
check_dependencies() {
    print_header "Checking Dependencies"

    # Check Python
    if command -v python3 &> /dev/null; then
        PYTHON_VERSION=$(python3 --version)
        print_success "Python found: $PYTHON_VERSION"
    else
        print_error "Python 3 not found. Please install Python 3.9+"
        exit 1
    fi

    # Check GOOGLE_API_KEY
    if [ -n "$GOOGLE_API_KEY" ]; then
        print_success "GOOGLE_API_KEY is set"
    else
        print_warning "GOOGLE_API_KEY not set"
        echo "  Set it with: export GOOGLE_API_KEY='your-api-key'"
        echo "  Get a key from: https://aistudio.google.com/app/apikey"
    fi
}

# Install Python dependencies
install_deps() {
    print_header "Installing Python Dependencies"

    if [ -f requirements.txt ]; then
        pip3 install -r requirements.txt --quiet
        print_success "Dependencies installed"
    else
        print_error "requirements.txt not found"
        exit 1
    fi
}

# Start review portal
start_portal() {
    print_header "Starting Review Portal"

    echo "Starting server at http://127.0.0.1:5555"
    echo "Press Ctrl+C to stop"
    echo ""

    python3 review_portal.py
}

# Generate a single asset
generate_asset() {
    local asset_id="$1"
    print_header "Generating Asset: $asset_id"

    if [ -z "$GOOGLE_API_KEY" ]; then
        print_error "GOOGLE_API_KEY not set"
        echo "  Run: export GOOGLE_API_KEY='your-api-key'"
        exit 1
    fi

    python3 generate_with_qc.py --asset "$asset_id"
}

# Generate all priority assets
generate_priority() {
    print_header "Generating Priority Assets"

    if [ -z "$GOOGLE_API_KEY" ]; then
        print_error "GOOGLE_API_KEY not set"
        echo "  Run: export GOOGLE_API_KEY='your-api-key'"
        exit 1
    fi

    python3 generate_with_qc.py --priority
}

# List available assets
list_assets() {
    print_header "Available Assets"
    python3 generate_with_qc.py --list
}

# Show help
show_help() {
    echo "HDIM AI Image Generation with Quality Control"
    echo ""
    echo "Usage: ./start.sh [command] [options]"
    echo ""
    echo "Commands:"
    echo "  portal              Start the web review portal"
    echo "  generate <asset>    Generate a specific asset (e.g., HERO-01)"
    echo "  priority            Generate all high-priority assets"
    echo "  list                List all available assets"
    echo "  install             Install Python dependencies"
    echo "  check               Check system dependencies"
    echo "  help                Show this help message"
    echo ""
    echo "Examples:"
    echo "  ./start.sh portal                  # Start review portal"
    echo "  ./start.sh generate HERO-01        # Generate hero image"
    echo "  ./start.sh priority                # Generate all priority assets"
    echo ""
    echo "Environment Variables:"
    echo "  GOOGLE_API_KEY      Your Google AI API key (required for generation)"
    echo ""
}

# Main command handler
main() {
    case "${1:-help}" in
        portal)
            check_dependencies
            start_portal
            ;;
        generate)
            if [ -z "$2" ]; then
                print_error "Asset ID required"
                echo "  Example: ./start.sh generate HERO-01"
                exit 1
            fi
            check_dependencies
            generate_asset "$2"
            ;;
        priority)
            check_dependencies
            generate_priority
            ;;
        list)
            list_assets
            ;;
        install)
            install_deps
            ;;
        check)
            check_dependencies
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "Unknown command: $1"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
