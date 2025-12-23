#!/bin/bash
# =============================================================================
# HDIM Local CI Runner
# =============================================================================
# Runs GitHub Actions locally using 'act' with memory-safe configurations
# to prevent WSL crashes.
#
# Usage:
#   ./scripts/run-local-ci.sh              # Run default workflow
#   ./scripts/run-local-ci.sh -j build     # Run specific job
#   ./scripts/run-local-ci.sh --list       # List available workflows
#   ./scripts/run-local-ci.sh --dry-run    # Dry run (no containers)
#   ./scripts/run-local-ci.sh --help       # Show help
#
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_success() { echo -e "${GREEN}[OK]${NC} $1"; }

# Memory thresholds (in KB)
MIN_FREE_MEMORY_KB=$((4 * 1024 * 1024))  # 4GB minimum free
WARN_FREE_MEMORY_KB=$((6 * 1024 * 1024)) # 6GB warning threshold

check_memory() {
    log_info "Checking available memory..."

    local free_mem=$(grep MemAvailable /proc/meminfo | awk '{print $2}')
    local free_mem_gb=$(echo "scale=1; $free_mem / 1024 / 1024" | bc)

    if [ "$free_mem" -lt "$MIN_FREE_MEMORY_KB" ]; then
        log_error "Insufficient memory: ${free_mem_gb}GB available"
        log_error "Need at least 4GB free to run safely"
        echo ""
        echo "Suggestions:"
        echo "  1. Close other applications"
        echo "  2. Stop running Docker containers: docker stop \$(docker ps -q)"
        echo "  3. Clear Docker cache: docker system prune"
        echo "  4. Restart WSL: wsl --shutdown (from Windows)"
        return 1
    elif [ "$free_mem" -lt "$WARN_FREE_MEMORY_KB" ]; then
        log_warn "Low memory warning: ${free_mem_gb}GB available"
        log_warn "Recommended: 6GB+ free for stable operation"
    else
        log_success "Memory check passed: ${free_mem_gb}GB available"
    fi
    return 0
}

check_docker() {
    log_info "Checking Docker..."

    if ! docker info &>/dev/null; then
        log_error "Docker is not running or not accessible"
        echo "Please start Docker Desktop or check Docker daemon"
        return 1
    fi

    log_success "Docker is running"
    return 0
}

check_act() {
    log_info "Checking act installation..."

    if ! command -v act &>/dev/null; then
        log_error "'act' is not installed"
        echo ""
        echo "Install with:"
        echo "  curl -s https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash"
        echo "  # or"
        echo "  brew install act"
        return 1
    fi

    log_success "act is installed: $(act --version)"
    return 0
}

start_infrastructure() {
    log_info "Starting minimal infrastructure (light profile)..."

    cd "$PROJECT_DIR"

    # Check if containers are already running
    if docker compose ps --services --filter "status=running" 2>/dev/null | grep -q postgres; then
        log_success "Infrastructure already running"
        return 0
    fi

    docker compose --profile light up -d

    # Wait for postgres to be ready
    log_info "Waiting for PostgreSQL to be ready..."
    local retries=30
    while [ $retries -gt 0 ]; do
        if docker exec healthdata-postgres pg_isready -U healthdata &>/dev/null; then
            log_success "PostgreSQL is ready"
            return 0
        fi
        sleep 1
        ((retries--))
    done

    log_error "PostgreSQL failed to start"
    return 1
}

cleanup_docker() {
    log_info "Cleaning up Docker resources..."

    # Remove stopped containers
    docker container prune -f &>/dev/null || true

    # Remove dangling images
    docker image prune -f &>/dev/null || true

    log_success "Docker cleanup complete"
}

show_help() {
    echo "HDIM Local CI Runner"
    echo ""
    echo "Usage: $0 [OPTIONS] [ACT_ARGS...]"
    echo ""
    echo "Options:"
    echo "  --help          Show this help message"
    echo "  --dry-run       Run act in dry-run mode (no containers)"
    echo "  --list          List available workflows and jobs"
    echo "  --skip-infra    Skip starting infrastructure"
    echo "  --cleanup       Clean up Docker resources before running"
    echo "  --gradle-local  Use reduced-memory Gradle settings"
    echo ""
    echo "Examples:"
    echo "  $0                      # Run default workflow"
    echo "  $0 -j build             # Run 'build' job only"
    echo "  $0 -W .github/workflows/docker-build.yml"
    echo "  $0 --list               # List all workflows"
    echo ""
    echo "Memory-safe defaults are applied via .actrc:"
    echo "  - Container memory limit: 4GB"
    echo "  - Max parallel jobs: 1"
    echo "  - Gradle daemon disabled"
}

# Parse arguments
SKIP_INFRA=false
DO_CLEANUP=false
USE_GRADLE_LOCAL=false
ACT_ARGS=()

while [[ $# -gt 0 ]]; do
    case $1 in
        --help)
            show_help
            exit 0
            ;;
        --skip-infra)
            SKIP_INFRA=true
            shift
            ;;
        --cleanup)
            DO_CLEANUP=true
            shift
            ;;
        --gradle-local)
            USE_GRADLE_LOCAL=true
            shift
            ;;
        --list)
            ACT_ARGS+=("--list")
            SKIP_INFRA=true
            shift
            ;;
        --dry-run|-n)
            ACT_ARGS+=("-n")
            SKIP_INFRA=true
            shift
            ;;
        *)
            ACT_ARGS+=("$1")
            shift
            ;;
    esac
done

# Main execution
echo "============================================"
echo "HDIM Local CI Runner"
echo "============================================"
echo ""

# Pre-flight checks
check_memory || exit 1
check_docker || exit 1
check_act || exit 1

# Optional cleanup
if [ "$DO_CLEANUP" = true ]; then
    cleanup_docker
fi

# Start infrastructure if needed
if [ "$SKIP_INFRA" = false ]; then
    start_infrastructure || exit 1
fi

# Set up Gradle local properties if requested
if [ "$USE_GRADLE_LOCAL" = true ]; then
    log_info "Using reduced-memory Gradle settings"
    export GRADLE_OPTS="-Xmx2g -Dorg.gradle.daemon=false -Dorg.gradle.parallel=false"
fi

# Change to project directory
cd "$PROJECT_DIR"

# Run act
log_info "Running act with args: ${ACT_ARGS[*]:-default}"
echo ""

act "${ACT_ARGS[@]}"

log_success "Local CI run complete"
