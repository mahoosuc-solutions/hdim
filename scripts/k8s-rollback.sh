#!/bin/bash
set -euo pipefail

# HDIM Platform Kubernetes Rollback Script
# Provides automated rollback capabilities for K8s deployments
#
# Usage: ./scripts/k8s-rollback.sh <environment> [options]
#   Environments: staging, production
#   Options:
#     --version <tag>     Rollback to specific version
#     --service <name>    Rollback specific service only
#     --revision <n>      Rollback to specific revision number
#     --dry-run           Show what would be rolled back without executing
#     --force             Skip confirmation prompts
#     --verify            Run smoke tests after rollback

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT="${1:-}"
TARGET_VERSION=""
TARGET_SERVICE=""
TARGET_REVISION=""
DRY_RUN=false
FORCE=false
VERIFY=false
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Namespace mapping
declare -A NAMESPACES=(
    ["staging"]="hdim-staging"
    ["production"]="hdim-production"
    ["local"]="hdim"
)

# All services
SERVICES=(
    "gateway-service"
    "fhir-service"
    "patient-service"
    "care-gap-service"
    "quality-measure-service"
    "cql-engine-service"
    "consent-service"
    "event-processing-service"
    "event-router-service"
    "agent-runtime-service"
    "ai-assistant-service"
    "agent-builder-service"
    "analytics-service"
    "predictive-analytics-service"
    "sdoh-service"
    "approval-service"
    "payer-workflows-service"
    "cdr-processor-service"
    "data-enrichment-service"
    "ehr-connector-service"
    "documentation-service"
)

# Logging functions
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Parse arguments
parse_args() {
    shift # Skip environment
    while [[ $# -gt 0 ]]; do
        case $1 in
            --version)
                TARGET_VERSION="$2"
                shift 2
                ;;
            --service)
                TARGET_SERVICE="$2"
                shift 2
                ;;
            --revision)
                TARGET_REVISION="$2"
                shift 2
                ;;
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            --force)
                FORCE=true
                shift
                ;;
            --verify)
                VERIFY=true
                shift
                ;;
            *)
                shift
                ;;
        esac
    done
}

# Validate environment
validate_environment() {
    if [[ -z "$ENVIRONMENT" ]]; then
        log_error "Environment is required"
        show_usage
        exit 1
    fi

    if [[ ! -v "NAMESPACES[$ENVIRONMENT]" ]]; then
        log_error "Invalid environment: $ENVIRONMENT"
        log_info "Valid environments: staging, production, local"
        exit 1
    fi

    NAMESPACE="${NAMESPACES[$ENVIRONMENT]}"

    # Check if namespace exists
    if ! kubectl get namespace "$NAMESPACE" &>/dev/null; then
        log_error "Namespace '$NAMESPACE' does not exist"
        exit 1
    fi
}

# Get current deployment status
get_deployment_status() {
    local service="$1"

    log_info "Current status of $service:"

    kubectl get deployment "$service" -n "$NAMESPACE" \
        -o custom-columns="NAME:.metadata.name,READY:.status.readyReplicas,DESIRED:.spec.replicas,IMAGE:.spec.template.spec.containers[0].image,AGE:.metadata.creationTimestamp" \
        2>/dev/null || echo "  Deployment not found"
}

# Get deployment history
get_deployment_history() {
    local service="$1"

    log_info "Deployment history for $service:"
    kubectl rollout history deployment/"$service" -n "$NAMESPACE" 2>/dev/null || echo "  No history available"
}

# Rollback single service
rollback_service() {
    local service="$1"
    local revision="${2:-}"

    log_info "Rolling back $service in $NAMESPACE..."

    if [[ "$DRY_RUN" == "true" ]]; then
        log_warning "[DRY-RUN] Would rollback $service"
        if [[ -n "$revision" ]]; then
            log_info "[DRY-RUN] Target revision: $revision"
        fi
        return 0
    fi

    # Perform rollback
    if [[ -n "$revision" ]]; then
        if kubectl rollout undo deployment/"$service" -n "$NAMESPACE" --to-revision="$revision" 2>/dev/null; then
            log_success "Initiated rollback of $service to revision $revision"
        else
            log_error "Failed to rollback $service"
            return 1
        fi
    else
        if kubectl rollout undo deployment/"$service" -n "$NAMESPACE" 2>/dev/null; then
            log_success "Initiated rollback of $service to previous revision"
        else
            log_error "Failed to rollback $service"
            return 1
        fi
    fi

    # Wait for rollback to complete
    log_info "Waiting for $service rollback to complete..."
    if kubectl rollout status deployment/"$service" -n "$NAMESPACE" --timeout=300s 2>/dev/null; then
        log_success "$service rollback completed successfully"
        return 0
    else
        log_error "$service rollback failed or timed out"
        return 1
    fi
}

# Rollback to specific image version
rollback_to_version() {
    local service="$1"
    local version="$2"

    log_info "Rolling back $service to version $version..."

    if [[ "$DRY_RUN" == "true" ]]; then
        log_warning "[DRY-RUN] Would set $service image to hdim/$service:$version"
        return 0
    fi

    local image="hdim/$service:$version"

    if kubectl set image deployment/"$service" "$service=$image" -n "$NAMESPACE" 2>/dev/null; then
        log_success "Set $service image to $image"

        # Wait for rollout
        log_info "Waiting for $service rollout to complete..."
        if kubectl rollout status deployment/"$service" -n "$NAMESPACE" --timeout=300s 2>/dev/null; then
            log_success "$service version update completed successfully"
            return 0
        else
            log_error "$service version update failed or timed out"
            return 1
        fi
    else
        log_error "Failed to set image for $service"
        return 1
    fi
}

# Rollback all services
rollback_all_services() {
    local failed=0
    local success=0
    local total=${#SERVICES[@]}

    log_info "Rolling back all $total services in $NAMESPACE..."

    for service in "${SERVICES[@]}"; do
        # Check if deployment exists
        if ! kubectl get deployment "$service" -n "$NAMESPACE" &>/dev/null; then
            log_warning "Skipping $service - deployment not found"
            continue
        fi

        if [[ -n "$TARGET_VERSION" ]]; then
            if rollback_to_version "$service" "$TARGET_VERSION"; then
                ((success++))
            else
                ((failed++))
            fi
        elif [[ -n "$TARGET_REVISION" ]]; then
            if rollback_service "$service" "$TARGET_REVISION"; then
                ((success++))
            else
                ((failed++))
            fi
        else
            if rollback_service "$service"; then
                ((success++))
            else
                ((failed++))
            fi
        fi
    done

    echo ""
    log_info "Rollback Summary:"
    log_info "  Successful: $success"
    log_info "  Failed: $failed"

    [[ $failed -eq 0 ]]
}

# Check deployment health after rollback
check_deployment_health() {
    log_info "Checking deployment health..."

    local healthy=0
    local unhealthy=0

    for service in "${SERVICES[@]}"; do
        if ! kubectl get deployment "$service" -n "$NAMESPACE" &>/dev/null; then
            continue
        fi

        local ready
        ready=$(kubectl get deployment "$service" -n "$NAMESPACE" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
        local desired
        desired=$(kubectl get deployment "$service" -n "$NAMESPACE" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "1")

        if [[ "$ready" == "$desired" ]] && [[ "$ready" != "0" ]]; then
            ((healthy++))
        else
            ((unhealthy++))
            log_warning "$service: $ready/$desired pods ready"
        fi
    done

    log_info "Health Check: $healthy healthy, $unhealthy unhealthy"

    [[ $unhealthy -eq 0 ]]
}

# Store deployment metadata for tracking
store_deployment_metadata() {
    local action="$1"
    local details="$2"

    if [[ "$DRY_RUN" == "true" ]]; then
        return 0
    fi

    local timestamp
    timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

    # Store metadata in a ConfigMap
    local configmap_name="hdim-deployment-history"

    # Check if ConfigMap exists
    if ! kubectl get configmap "$configmap_name" -n "$NAMESPACE" &>/dev/null; then
        kubectl create configmap "$configmap_name" -n "$NAMESPACE" \
            --from-literal="created=$timestamp" \
            2>/dev/null || true
    fi

    # Add deployment record
    local record="{\"timestamp\":\"$timestamp\",\"action\":\"$action\",\"details\":\"$details\",\"environment\":\"$ENVIRONMENT\"}"

    kubectl annotate configmap "$configmap_name" -n "$NAMESPACE" \
        "hdim.io/last-action=$action" \
        "hdim.io/last-action-time=$timestamp" \
        --overwrite 2>/dev/null || true
}

# Run smoke tests
run_smoke_tests() {
    log_info "Running smoke tests..."

    local base_url
    case "$ENVIRONMENT" in
        staging)
            base_url="https://staging.hdim.example.com"
            ;;
        production)
            base_url="https://hdim.example.com"
            ;;
        local)
            base_url="http://localhost:8080"
            ;;
    esac

    if [[ -x "$SCRIPT_DIR/smoke-tests.sh" ]]; then
        if "$SCRIPT_DIR/smoke-tests.sh" "$base_url" --quick; then
            log_success "Smoke tests passed"
            return 0
        else
            log_error "Smoke tests failed"
            return 1
        fi
    else
        log_warning "Smoke tests script not found or not executable"
        return 0
    fi
}

# Confirm action
confirm_action() {
    local message="$1"

    if [[ "$FORCE" == "true" ]]; then
        return 0
    fi

    echo -e "${YELLOW}$message${NC}"
    read -p "Continue? [y/N] " -n 1 -r
    echo

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Operation cancelled"
        exit 0
    fi
}

# Show usage
show_usage() {
    echo "HDIM Kubernetes Rollback Script"
    echo ""
    echo "Usage: $0 <environment> [options]"
    echo ""
    echo "Environments:"
    echo "  staging      Rollback staging environment"
    echo "  production   Rollback production environment"
    echo "  local        Rollback local development environment"
    echo ""
    echo "Options:"
    echo "  --version <tag>     Rollback to specific image version"
    echo "  --service <name>    Rollback specific service only"
    echo "  --revision <n>      Rollback to specific revision number"
    echo "  --dry-run           Show what would be rolled back"
    echo "  --force             Skip confirmation prompts"
    echo "  --verify            Run smoke tests after rollback"
    echo ""
    echo "Examples:"
    echo "  $0 staging                          # Rollback all staging services"
    echo "  $0 production --service gateway     # Rollback gateway in production"
    echo "  $0 staging --version v1.2.3         # Set all staging to v1.2.3"
    echo "  $0 production --dry-run             # Preview production rollback"
    echo "  $0 staging --revision 3             # Rollback to revision 3"
}

# Show status
show_status() {
    log_info "Current deployment status for $NAMESPACE:"
    echo ""

    kubectl get deployments -n "$NAMESPACE" \
        -o custom-columns="SERVICE:.metadata.name,READY:.status.readyReplicas,DESIRED:.spec.replicas,UPDATED:.status.updatedReplicas,IMAGE:.spec.template.spec.containers[0].image" \
        2>/dev/null || echo "No deployments found"

    echo ""
    log_info "Recent events:"
    kubectl get events -n "$NAMESPACE" --sort-by='.lastTimestamp' 2>/dev/null | tail -10 || echo "No events found"
}

# Main execution
main() {
    if [[ $# -lt 1 ]] || [[ "$1" == "-h" ]] || [[ "$1" == "--help" ]]; then
        show_usage
        exit 0
    fi

    if [[ "$1" == "status" ]]; then
        ENVIRONMENT="${2:-staging}"
        validate_environment
        show_status
        exit 0
    fi

    parse_args "$@"
    validate_environment

    log_info "HDIM Kubernetes Rollback"
    log_info "Environment: $ENVIRONMENT"
    log_info "Namespace: $NAMESPACE"
    [[ -n "$TARGET_VERSION" ]] && log_info "Target Version: $TARGET_VERSION"
    [[ -n "$TARGET_SERVICE" ]] && log_info "Target Service: $TARGET_SERVICE"
    [[ -n "$TARGET_REVISION" ]] && log_info "Target Revision: $TARGET_REVISION"
    [[ "$DRY_RUN" == "true" ]] && log_warning "DRY RUN MODE"
    echo ""

    # Show current status
    show_status
    echo ""

    # Confirm action
    if [[ -n "$TARGET_SERVICE" ]]; then
        confirm_action "This will rollback $TARGET_SERVICE in $ENVIRONMENT"
    else
        confirm_action "This will rollback ALL services in $ENVIRONMENT"
    fi

    # Store metadata before rollback
    store_deployment_metadata "rollback-start" "Initiating rollback for $ENVIRONMENT"

    # Perform rollback
    local rollback_success=true

    if [[ -n "$TARGET_SERVICE" ]]; then
        # Single service rollback
        if ! kubectl get deployment "$TARGET_SERVICE" -n "$NAMESPACE" &>/dev/null; then
            log_error "Deployment '$TARGET_SERVICE' not found in $NAMESPACE"
            exit 1
        fi

        get_deployment_history "$TARGET_SERVICE"
        echo ""

        if [[ -n "$TARGET_VERSION" ]]; then
            rollback_to_version "$TARGET_SERVICE" "$TARGET_VERSION" || rollback_success=false
        elif [[ -n "$TARGET_REVISION" ]]; then
            rollback_service "$TARGET_SERVICE" "$TARGET_REVISION" || rollback_success=false
        else
            rollback_service "$TARGET_SERVICE" || rollback_success=false
        fi
    else
        # All services rollback
        rollback_all_services || rollback_success=false
    fi

    echo ""

    # Check health after rollback
    if [[ "$DRY_RUN" != "true" ]]; then
        check_deployment_health
    fi

    # Run smoke tests if requested
    if [[ "$VERIFY" == "true" ]] && [[ "$DRY_RUN" != "true" ]]; then
        echo ""
        run_smoke_tests || rollback_success=false
    fi

    # Store completion metadata
    if [[ "$rollback_success" == "true" ]]; then
        store_deployment_metadata "rollback-complete" "Rollback completed successfully"
        log_success "Rollback completed successfully"
        exit 0
    else
        store_deployment_metadata "rollback-failed" "Rollback completed with errors"
        log_error "Rollback completed with errors"
        exit 1
    fi
}

main "$@"
