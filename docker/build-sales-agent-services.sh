#!/bin/bash

# HDIM Build Automation Script - Sales Agent Services
# Builds Docker images for three services following HDIM gold standard pattern
# CRITICAL: Builds ONE service at a time to avoid system overload
#
# Usage:
#   ./docker/build-sales-agent-services.sh          # Build all 3 services
#   ./docker/build-sales-agent-services.sh ai-sales-agent   # Build one service
#   ./docker/build-sales-agent-services.sh live-call-sales-agent
#   ./docker/build-sales-agent-services.sh coaching-ui

set -euo pipefail

# Color output for readability
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BUILD_LOG="${SCRIPT_DIR}/build.log"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Service configurations
declare -A SERVICES=(
    [ai-sales-agent]="backend/modules/services/ai-sales-agent"
    [live-call-sales-agent]="backend/modules/services/live-call-sales-agent"
    [coaching-ui]="apps/coaching-ui"
)

declare -A SERVICE_NAMES=(
    [ai-sales-agent]="AI Sales Agent (Python FastAPI)"
    [live-call-sales-agent]="Live Call Sales Agent (Python + Chrome)"
    [coaching-ui]="Coaching UI (Angular + Nginx)"
)

declare -A SERVICE_TARGETS=(
    [ai-sales-agent]="350-400MB"
    [live-call-sales-agent]="950-1050MB"
    [coaching-ui]="75-85MB"
)

# Initialize variables
SERVICES_TO_BUILD=()
BUILD_SUCCESS=true
FAILED_SERVICES=()
SKIPPED_SERVICES=()

# ============================================================================
# Functions
# ============================================================================

log() {
    echo -e "${BLUE}[$(date '+%H:%M:%S')]${NC} $*" | tee -a "$BUILD_LOG"
}

log_success() {
    echo -e "${GREEN}✓ $*${NC}" | tee -a "$BUILD_LOG"
}

log_error() {
    echo -e "${RED}✗ $*${NC}" | tee -a "$BUILD_LOG"
}

log_warning() {
    echo -e "${YELLOW}⚠ $*${NC}" | tee -a "$BUILD_LOG"
}

# Print header
print_header() {
    echo -e "\n${BLUE}════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  HDIM Build Automation - Sales Agent Services${NC}"
    echo -e "${BLUE}  Gold Standard: Build ONE service at a time${NC}"
    echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}\n"
}

# Validate Docker is installed
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed or not in PATH"
        exit 1
    fi
    log_success "Docker found: $(docker --version)"
}

# Validate project structure
check_project_structure() {
    log "Validating project structure..."

    for service in "${!SERVICES[@]}"; do
        local path="${SERVICES[$service]}"
        if [ ! -d "$PROJECT_ROOT/$path" ]; then
            log_error "Service directory not found: $PROJECT_ROOT/$path"
            return 1
        fi
    done

    log_success "Project structure validated"
}

# Pre-cache dependencies locally (CRITICAL - prevents Docker TLS issues)
precache_dependencies() {
    local service=$1
    local service_path="${SERVICES[$service]}"

    log "Pre-caching dependencies for ${SERVICE_NAMES[$service]}..."

    if [ "$service" == "coaching-ui" ]; then
        # For Angular: npm ci
        if [ -f "$PROJECT_ROOT/$service_path/package.json" ]; then
            log "  → Running npm ci (deterministic install)..."
            cd "$PROJECT_ROOT" && npm ci --legacy-peer-deps --omit=optional 2>&1 | tail -5
            log_success "npm dependencies cached"
        fi
    else
        # For Python: pip download
        if [ -f "$PROJECT_ROOT/$service_path/requirements.txt" ]; then
            log "  → Downloading Python packages..."
            pip download -r "$PROJECT_ROOT/$service_path/requirements.txt" \
                -d /tmp/pip-cache --no-binary :all: 2>&1 | tail -3
            log_success "Python dependencies cached"
        elif [ -f "$PROJECT_ROOT/$service_path/pyproject.toml" ]; then
            log "  → Using pyproject.toml (no pre-cache needed for setuptools)"
        fi
    fi
}

# Build single service Docker image
build_service() {
    local service=$1
    local service_path="${SERVICES[$service]}"
    local service_name="${SERVICE_NAMES[$service]}"
    local target_size="${SERVICE_TARGETS[$service]}"

    log ""
    log "${BLUE}─────────────────────────────────────────────────────────────${NC}"
    log "Building: ${service_name}"
    log "Path: $service_path"
    log "Target size: $target_size"
    log "${BLUE}─────────────────────────────────────────────────────────────${NC}"

    # Check Dockerfile exists
    local dockerfile_path="$PROJECT_ROOT/$service_path/Dockerfile.optimized"
    if [ ! -f "$dockerfile_path" ]; then
        log_warning "Dockerfile.optimized not found, using standard Dockerfile"
        dockerfile_path="$PROJECT_ROOT/$service_path/Dockerfile"
    fi

    if [ ! -f "$dockerfile_path" ]; then
        log_error "No Dockerfile found for $service at $dockerfile_path"
        FAILED_SERVICES+=("$service")
        BUILD_SUCCESS=false
        return 1
    fi

    # Build image (using BuildKit for better caching)
    local image_tag="hdim-$service:latest"
    local build_timestamp=$(date +%s)

    log "Building image: ${image_tag}..."

    # Use DOCKER_BUILDKIT for better caching and performance
    DOCKER_BUILDKIT=1 docker build \
        --progress=plain \
        -f "$dockerfile_path" \
        -t "$image_tag" \
        -t "hdim-$service:${TIMESTAMP}" \
        --build-arg BUILD_DATE="$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
        --build-arg VCS_REF="$(cd "$PROJECT_ROOT" && git rev-parse --short HEAD 2>/dev/null || echo 'unknown')" \
        "$PROJECT_ROOT/$service_path" 2>&1 | tee -a "$BUILD_LOG"

    if [ ${PIPESTATUS[0]} -eq 0 ]; then
        log_success "Build completed for $service"

        # Get image size
        local image_size=$(docker images "$image_tag" --format "{{.Size}}")
        log "  → Image size: $image_size (target: $target_size)"

        return 0
    else
        log_error "Build failed for $service"
        FAILED_SERVICES+=("$service")
        BUILD_SUCCESS=false
        return 1
    fi
}

# Validate built image
validate_service() {
    local service=$1
    local image_tag="hdim-$service:latest"

    log "Validating image: $image_tag..."

    # Check image exists
    if ! docker image inspect "$image_tag" &> /dev/null; then
        log_error "Image not found: $image_tag"
        return 1
    fi

    # Check health check is configured
    local health_check=$(docker inspect "$image_tag" --format='{{.Config.Healthcheck}}')
    if [[ "$health_check" == "<nil>" ]]; then
        log_warning "No health check configured for $image_tag"
    else
        log_success "Health check configured"
    fi

    # Get metadata
    local image_info=$(docker inspect "$image_tag" --format='Size: {{.Size | humanize}} Created: {{.Created}}')
    log "  → $image_info"
}

# Print summary
print_summary() {
    echo -e "\n${BLUE}════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  Build Summary${NC}"
    echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}\n"

    # Overall status
    if [ "$BUILD_SUCCESS" = true ]; then
        log_success "All builds completed successfully!"
    else
        log_error "Some builds failed!"
    fi

    # Built services
    if [ ${#SERVICES_TO_BUILD[@]} -gt 0 ]; then
        log "\nServices built:"
        for service in "${SERVICES_TO_BUILD[@]}"; do
            if [[ ! " ${FAILED_SERVICES[@]} " =~ " ${service} " ]]; then
                log_success "  • hdim-$service:latest"
            fi
        done
    fi

    # Failed services
    if [ ${#FAILED_SERVICES[@]} -gt 0 ]; then
        log_error "\nFailed services:"
        for service in "${FAILED_SERVICES[@]}"; do
            echo -e "  ${RED}• $service${NC}" | tee -a "$BUILD_LOG"
        done
    fi

    # Docker compose integration
    log "\n${YELLOW}Next steps:${NC}"
    log "1. Update docker-compose.yml with new image tags (if needed)"
    log "2. Start services: docker compose up -d"
    log "3. View logs: docker compose logs -f"
    log "\n${YELLOW}Build log:${NC} $BUILD_LOG"

    echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}\n"
}

# ============================================================================
# Main Script
# ============================================================================

main() {
    # Initialize build log
    > "$BUILD_LOG"

    print_header

    # Validate environment
    check_docker
    check_project_structure

    # Determine which services to build
    if [ $# -eq 0 ]; then
        # Build all services
        SERVICES_TO_BUILD=("ai-sales-agent" "live-call-sales-agent" "coaching-ui")
        log "No service specified - building all 3 services\n"
    else
        # Build specified services
        for arg in "$@"; do
            if [[ -v SERVICES[$arg] ]]; then
                SERVICES_TO_BUILD+=("$arg")
            else
                log_error "Unknown service: $arg"
                log "Available services: ${!SERVICES[@]}"
                exit 1
            fi
        done
        log "Building specified services: ${SERVICES_TO_BUILD[*]}\n"
    fi

    # Build each service (CRITICAL: Sequential, one at a time)
    log "${YELLOW}HDIM Gold Standard: Building services sequentially (one at a time)${NC}\n"

    for service in "${SERVICES_TO_BUILD[@]}"; do
        # Pre-cache dependencies
        precache_dependencies "$service"

        # Build service
        if ! build_service "$service"; then
            log_warning "Skipping validation for failed service"
            continue
        fi

        # Validate built image
        validate_service "$service"

        # Small pause between builds to allow system to settle
        log "Pausing 5 seconds before next build (system cleanup)...\n"
        sleep 5
    done

    # Print summary
    print_summary

    # Exit with appropriate code
    if [ "$BUILD_SUCCESS" = true ]; then
        exit 0
    else
        exit 1
    fi
}

# Run main function
main "$@"
