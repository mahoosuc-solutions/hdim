#!/bin/bash
# HealthData Platform - Deployment Rollback Script
# This script performs automated rollback to previous deployment version
set -euo pipefail

ENVIRONMENT=${1:-production}
REVISION=${2:-}  # Optional: specific revision to rollback to
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Validate environment
validate_environment() {
    log_info "Validating rollback environment: $ENVIRONMENT"

    if [[ ! "$ENVIRONMENT" =~ ^(staging|production|development)$ ]]; then
        log_error "Invalid environment: $ENVIRONMENT. Must be staging, production, or development"
        exit 1
    fi

    # Extra confirmation for production rollback
    if [ "$ENVIRONMENT" = "production" ]; then
        log_warning "⚠️  PRODUCTION ROLLBACK REQUESTED ⚠️"
        echo "This will rollback the production environment to a previous version."
        read -p "Are you sure you want to continue? (yes/no): " confirm

        if [ "$confirm" != "yes" ]; then
            log_info "Rollback cancelled by user"
            exit 0
        fi
    fi

    log_success "Environment validation passed"
}

# Check kubectl connectivity
check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl not found. Please install kubectl first."
        exit 1
    fi

    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster. Please check your kubeconfig."
        exit 1
    fi

    log_success "Prerequisites check passed"
}

# Get deployment list
get_deployments() {
    log_info "Fetching deployments in namespace: $ENVIRONMENT"

    local deployments=$(kubectl get deployments -n "$ENVIRONMENT" -o jsonpath='{.items[*].metadata.name}')

    if [ -z "$deployments" ]; then
        log_error "No deployments found in namespace $ENVIRONMENT"
        exit 1
    fi

    echo "$deployments"
}

# Show rollout history
show_rollout_history() {
    local deployment=$1

    log_info "Rollout history for $deployment:"
    kubectl rollout history deployment/"$deployment" -n "$ENVIRONMENT"
}

# Get previous revision
get_previous_revision() {
    local deployment=$1

    if [ -n "$REVISION" ]; then
        echo "$REVISION"
        return
    fi

    # Get the previous revision (current - 1)
    local current_revision=$(kubectl get deployment "$deployment" -n "$ENVIRONMENT" -o jsonpath='{.metadata.annotations.deployment\.kubernetes\.io/revision}')
    local previous_revision=$((current_revision - 1))

    if [ $previous_revision -lt 1 ]; then
        log_error "No previous revision found for $deployment"
        return 1
    fi

    echo "$previous_revision"
}

# Perform rollback for a single deployment
rollback_deployment() {
    local deployment=$1

    log_info "Rolling back deployment: $deployment"

    # Show history
    show_rollout_history "$deployment"

    # Get previous revision
    local target_revision=$(get_previous_revision "$deployment")

    if [ -z "$target_revision" ]; then
        log_error "Failed to determine target revision for $deployment"
        return 1
    fi

    log_info "Rolling back to revision: $target_revision"

    # Perform rollback
    if [ -n "$REVISION" ]; then
        kubectl rollout undo deployment/"$deployment" -n "$ENVIRONMENT" --to-revision="$target_revision"
    else
        kubectl rollout undo deployment/"$deployment" -n "$ENVIRONMENT"
    fi

    log_success "Rollback initiated for $deployment"
}

# Wait for rollback to complete
wait_for_rollback() {
    local deployment=$1

    log_info "Waiting for rollback to complete: $deployment"

    if kubectl rollout status deployment/"$deployment" -n "$ENVIRONMENT" --timeout=300s; then
        log_success "Rollback completed successfully for $deployment"
        return 0
    else
        log_error "Rollback failed for $deployment"
        return 1
    fi
}

# Verify rollback health
verify_rollback_health() {
    local deployment=$1

    log_info "Verifying health of $deployment after rollback..."

    # Check pod status
    local pods=$(kubectl get pods -n "$ENVIRONMENT" -l app="$deployment" -o jsonpath='{.items[*].metadata.name}')

    for pod in $pods; do
        local pod_status=$(kubectl get pod "$pod" -n "$ENVIRONMENT" -o jsonpath='{.status.phase}')

        if [ "$pod_status" != "Running" ]; then
            log_error "Pod $pod is not running (status: $pod_status)"
            kubectl logs "$pod" -n "$ENVIRONMENT" --tail=50 || true
            return 1
        fi
    done

    # Check deployment status
    local ready_replicas=$(kubectl get deployment "$deployment" -n "$ENVIRONMENT" -o jsonpath='{.status.readyReplicas}')
    local desired_replicas=$(kubectl get deployment "$deployment" -n "$ENVIRONMENT" -o jsonpath='{.spec.replicas}')

    if [ "$ready_replicas" != "$desired_replicas" ]; then
        log_error "Deployment $deployment is not fully ready (ready: $ready_replicas, desired: $desired_replicas)"
        return 1
    fi

    log_success "Health check passed for $deployment"
    return 0
}

# Rollback all deployments
rollback_all_deployments() {
    local deployments=$(get_deployments)
    local failed_deployments=()

    log_info "Starting rollback for all deployments in $ENVIRONMENT"

    for deployment in $deployments; do
        log_info "Processing: $deployment"

        if rollback_deployment "$deployment"; then
            if wait_for_rollback "$deployment"; then
                if verify_rollback_health "$deployment"; then
                    log_success "✅ $deployment rolled back successfully"
                else
                    log_error "❌ $deployment health check failed after rollback"
                    failed_deployments+=("$deployment")
                fi
            else
                log_error "❌ $deployment rollback timeout"
                failed_deployments+=("$deployment")
            fi
        else
            log_error "❌ $deployment rollback failed"
            failed_deployments+=("$deployment")
        fi

        echo ""
    done

    if [ ${#failed_deployments[@]} -gt 0 ]; then
        log_error "Rollback failed for: ${failed_deployments[*]}"
        return 1
    fi

    log_success "All deployments rolled back successfully"
    return 0
}

# Create rollback snapshot
create_rollback_snapshot() {
    log_info "Creating rollback snapshot..."

    local snapshot_dir="${SCRIPT_DIR}/../rollback-snapshots"
    mkdir -p "$snapshot_dir"

    local timestamp=$(date +%Y%m%d-%H%M%S)
    local snapshot_file="${snapshot_dir}/${ENVIRONMENT}-rollback-${timestamp}.yaml"

    kubectl get all -n "$ENVIRONMENT" -o yaml > "$snapshot_file"

    log_success "Rollback snapshot saved to: $snapshot_file"
}

# Send notification about rollback
send_notification() {
    local status=$1
    local message=$2

    log_info "Sending rollback notification..."

    # Slack notification (if webhook configured)
    if [ -n "${SLACK_WEBHOOK:-}" ]; then
        curl -X POST "$SLACK_WEBHOOK" \
            -H 'Content-Type: application/json' \
            -d "{
                \"text\": \"🔄 Deployment Rollback - $status\",
                \"blocks\": [
                    {
                        \"type\": \"section\",
                        \"text\": {
                            \"type\": \"mrkdwn\",
                            \"text\": \"*Rollback Status:* $status\\n*Environment:* $ENVIRONMENT\\n*Message:* $message\\n*Timestamp:* $(date)\"
                        }
                    }
                ]
            }" || log_warning "Failed to send Slack notification"
    fi

    # Email notification (if configured)
    if [ -n "${NOTIFICATION_EMAIL:-}" ]; then
        echo "$message" | mail -s "Rollback $status - $ENVIRONMENT" "$NOTIFICATION_EMAIL" || log_warning "Failed to send email notification"
    fi
}

# Display rollback summary
display_summary() {
    local status=$1

    echo ""
    echo "======================================"
    echo "      ROLLBACK SUMMARY"
    echo "======================================"
    echo "Environment: $ENVIRONMENT"
    echo "Status: $status"
    echo "Timestamp: $(date)"
    echo ""

    log_info "Current deployment status:"
    kubectl get deployments -n "$ENVIRONMENT"
    echo ""

    log_info "Pod status:"
    kubectl get pods -n "$ENVIRONMENT"
    echo ""

    echo "======================================"
}

# Main rollback workflow
main() {
    log_info "🔄 Starting rollback for $ENVIRONMENT environment"

    # Validate and prepare
    validate_environment
    check_prerequisites

    # Create snapshot before rollback
    create_rollback_snapshot

    # Perform rollback
    if rollback_all_deployments; then
        display_summary "SUCCESS"
        send_notification "SUCCESS ✅" "Rollback completed successfully for $ENVIRONMENT"
        log_success "Rollback completed successfully! 🎉"
        return 0
    else
        display_summary "FAILED"
        send_notification "FAILED ❌" "Rollback failed for $ENVIRONMENT. Manual intervention required."
        log_error "Rollback failed. Please check the logs and investigate."
        return 1
    fi
}

# Error handler
trap 'log_error "Rollback failed at line $LINENO. Exit code: $?"' ERR

# Run main function
main "$@"
exit_code=$?

exit $exit_code
