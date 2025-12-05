#!/bin/bash

###############################################################################
# HealthData Platform - Rollback Script
# Version: 2.0.0
# Description: Rollback deployment to previous version
###############################################################################

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
LOG_FILE="${PROJECT_ROOT}/logs/rollback-$(date +%Y%m%d-%H%M%S).log"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
NAMESPACE="${NAMESPACE:-healthdata}"
RELEASE_NAME="${RELEASE_NAME:-healthdata-platform}"
DEPLOYMENT_NAME="${DEPLOYMENT_NAME:-healthdata-platform}"
REVISION="${REVISION:-0}"  # 0 means rollback to previous revision

###############################################################################
# Utility Functions
###############################################################################

log() {
    local level=$1
    shift
    local message="$*"

    case $level in
        INFO)    echo -e "${BLUE}[INFO]${NC} ${message}" | tee -a "${LOG_FILE}" ;;
        SUCCESS) echo -e "${GREEN}[SUCCESS]${NC} ${message}" | tee -a "${LOG_FILE}" ;;
        WARNING) echo -e "${YELLOW}[WARNING]${NC} ${message}" | tee -a "${LOG_FILE}" ;;
        ERROR)   echo -e "${RED}[ERROR]${NC} ${message}" | tee -a "${LOG_FILE}" ;;
    esac
}

error_exit() {
    log ERROR "$1"
    exit 1
}

check_prerequisites() {
    log INFO "Checking prerequisites..."

    if ! command -v kubectl &> /dev/null; then
        error_exit "kubectl is not installed"
    fi

    if ! kubectl cluster-info &> /dev/null; then
        error_exit "Cannot connect to Kubernetes cluster"
    fi

    if ! kubectl get namespace "${NAMESPACE}" &> /dev/null; then
        error_exit "Namespace ${NAMESPACE} does not exist"
    fi

    log SUCCESS "Prerequisites check passed"
}

show_deployment_history() {
    log INFO "Current deployment history:"
    echo ""
    kubectl rollout history deployment/"${DEPLOYMENT_NAME}" -n "${NAMESPACE}"
    echo ""
}

show_helm_history() {
    log INFO "Helm release history:"
    echo ""

    if command -v helm &> /dev/null; then
        if helm list -n "${NAMESPACE}" | grep -q "${RELEASE_NAME}"; then
            helm history "${RELEASE_NAME}" -n "${NAMESPACE}"
        else
            log WARNING "Helm release not found: ${RELEASE_NAME}"
        fi
    else
        log WARNING "Helm not installed, skipping Helm history"
    fi
    echo ""
}

get_current_state() {
    log INFO "Current deployment state:"
    echo ""

    # Get current deployment
    kubectl get deployment "${DEPLOYMENT_NAME}" -n "${NAMESPACE}"
    echo ""

    # Get current pods
    log INFO "Current pods:"
    kubectl get pods -n "${NAMESPACE}" -l app=healthdata-platform
    echo ""

    # Get current image
    local current_image=$(kubectl get deployment "${DEPLOYMENT_NAME}" -n "${NAMESPACE}" -o jsonpath='{.spec.template.spec.containers[0].image}')
    log INFO "Current image: ${current_image}"
    echo ""
}

confirm_rollback() {
    local revision_msg="previous revision"
    if [ "${REVISION}" != "0" ]; then
        revision_msg="revision ${REVISION}"
    fi

    echo ""
    log WARNING "You are about to rollback deployment '${DEPLOYMENT_NAME}' to ${revision_msg}"
    echo ""

    read -p "Are you sure you want to proceed? (yes/no): " confirmation

    if [ "${confirmation}" != "yes" ]; then
        log INFO "Rollback cancelled by user"
        exit 0
    fi
}

backup_current_state() {
    log INFO "Backing up current deployment state..."

    local backup_dir="${PROJECT_ROOT}/backups/rollback-backup-$(date +%Y%m%d-%H%M%S)"
    mkdir -p "${backup_dir}"

    # Backup deployment
    kubectl get deployment "${DEPLOYMENT_NAME}" -n "${NAMESPACE}" -o yaml > "${backup_dir}/deployment.yaml"

    # Backup configmaps
    kubectl get configmap -n "${NAMESPACE}" -o yaml > "${backup_dir}/configmaps.yaml"

    # Backup services
    kubectl get service -n "${NAMESPACE}" -o yaml > "${backup_dir}/services.yaml"

    # Backup current pod logs
    local pods=$(kubectl get pods -n "${NAMESPACE}" -l app=healthdata-platform -o jsonpath='{.items[*].metadata.name}')
    for pod in $pods; do
        kubectl logs "${pod}" -n "${NAMESPACE}" > "${backup_dir}/${pod}.log" 2>&1 || true
    done

    log SUCCESS "Backup created at: ${backup_dir}"
    echo "${backup_dir}" > "${PROJECT_ROOT}/logs/last-rollback-backup.txt"
}

rollback_deployment() {
    log INFO "Rolling back deployment..."

    local rollback_cmd="kubectl rollout undo deployment/${DEPLOYMENT_NAME} -n ${NAMESPACE}"

    if [ "${REVISION}" != "0" ]; then
        rollback_cmd="${rollback_cmd} --to-revision=${REVISION}"
    fi

    eval "${rollback_cmd}"

    if [ $? -ne 0 ]; then
        error_exit "Rollback command failed"
    fi

    log SUCCESS "Rollback initiated"
}

rollback_helm() {
    log INFO "Rolling back Helm release..."

    if ! command -v helm &> /dev/null; then
        log WARNING "Helm not installed, skipping Helm rollback"
        return
    fi

    if ! helm list -n "${NAMESPACE}" | grep -q "${RELEASE_NAME}"; then
        log WARNING "Helm release not found, skipping Helm rollback"
        return
    fi

    local helm_cmd="helm rollback ${RELEASE_NAME} -n ${NAMESPACE}"

    if [ "${REVISION}" != "0" ]; then
        helm_cmd="${helm_cmd} ${REVISION}"
    fi

    eval "${helm_cmd}"

    if [ $? -ne 0 ]; then
        error_exit "Helm rollback failed"
    fi

    log SUCCESS "Helm rollback completed"
}

wait_for_rollout() {
    log INFO "Waiting for rollout to complete..."

    kubectl rollout status deployment/"${DEPLOYMENT_NAME}" -n "${NAMESPACE}" --timeout=300s

    if [ $? -ne 0 ]; then
        error_exit "Rollout did not complete successfully"
    fi

    log SUCCESS "Rollout completed"
}

verify_rollback() {
    log INFO "Verifying rollback..."

    # Wait for pods to stabilize
    sleep 10

    # Check pod status
    local running_pods=$(kubectl get pods -n "${NAMESPACE}" -l app=healthdata-platform --field-selector=status.phase=Running --no-headers | wc -l)
    log INFO "Running pods: ${running_pods}"

    # Check health endpoints
    local pods=$(kubectl get pods -n "${NAMESPACE}" -l app=healthdata-platform -o jsonpath='{.items[*].metadata.name}')

    local healthy_count=0
    for pod in $pods; do
        log INFO "Checking health of pod: ${pod}"

        if kubectl exec "${pod}" -n "${NAMESPACE}" -- curl -f -s http://localhost:8080/actuator/health/liveness > /dev/null 2>&1; then
            log SUCCESS "Pod ${pod} is healthy"
            ((healthy_count++))
        else
            log WARNING "Pod ${pod} health check failed"
        fi
    done

    if [ ${healthy_count} -eq 0 ]; then
        error_exit "No healthy pods found after rollback"
    fi

    log SUCCESS "Rollback verification completed (${healthy_count} healthy pods)"
}

show_rollback_status() {
    log INFO "Rollback Status:"
    echo ""

    # Show deployment status
    kubectl get deployment "${DEPLOYMENT_NAME}" -n "${NAMESPACE}"
    echo ""

    # Show pod status
    log INFO "Pod Status:"
    kubectl get pods -n "${NAMESPACE}" -l app=healthdata-platform
    echo ""

    # Show current image
    local current_image=$(kubectl get deployment "${DEPLOYMENT_NAME}" -n "${NAMESPACE}" -o jsonpath='{.spec.template.spec.containers[0].image}')
    log INFO "Current image after rollback: ${current_image}"
    echo ""

    # Show recent events
    log INFO "Recent events:"
    kubectl get events -n "${NAMESPACE}" --sort-by='.lastTimestamp' | tail -20
}

show_recent_logs() {
    log INFO "Recent application logs:"
    kubectl logs -n "${NAMESPACE}" -l app=healthdata-platform --tail=50
}

###############################################################################
# Main Execution
###############################################################################

main() {
    log INFO "HealthData Platform - Deployment Rollback"
    log INFO "Namespace: ${NAMESPACE}"
    log INFO "Deployment: ${DEPLOYMENT_NAME}"

    # Create logs directory
    mkdir -p "${PROJECT_ROOT}/logs"
    mkdir -p "${PROJECT_ROOT}/backups"

    # Execute rollback steps
    check_prerequisites
    show_deployment_history

    # Show Helm history if available
    if command -v helm &> /dev/null; then
        show_helm_history
    fi

    get_current_state
    confirm_rollback
    backup_current_state

    # Perform rollback (use Helm if available, otherwise kubectl)
    if command -v helm &> /dev/null && helm list -n "${NAMESPACE}" | grep -q "${RELEASE_NAME}"; then
        rollback_helm
    else
        rollback_deployment
    fi

    wait_for_rollout
    verify_rollback
    show_rollback_status

    # Optionally show logs
    if [ "${SHOW_LOGS:-false}" = "true" ]; then
        show_recent_logs
    fi

    log SUCCESS "Rollback completed successfully!"
    log INFO "Log file: ${LOG_FILE}"

    echo ""
    log INFO "Useful commands:"
    echo "  View pods:      kubectl get pods -n ${NAMESPACE}"
    echo "  View logs:      kubectl logs -n ${NAMESPACE} -l app=healthdata-platform -f"
    echo "  View events:    kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp'"
    echo "  Rollback again: REVISION=<number> ${0}"
}

# Run main function
main "$@"
