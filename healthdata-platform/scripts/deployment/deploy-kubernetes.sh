#!/bin/bash

###############################################################################
# HealthData Platform - Kubernetes Deployment Script (Helm)
# Version: 2.0.0
# Description: Deploy using Helm chart
###############################################################################

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
LOG_FILE="${PROJECT_ROOT}/logs/helm-deployment-$(date +%Y%m%d-%H%M%S).log"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
RELEASE_NAME="${RELEASE_NAME:-healthdata-platform}"
NAMESPACE="${NAMESPACE:-healthdata}"
CHART_PATH="${PROJECT_ROOT}/helm/healthdata-platform"
VALUES_FILE="${VALUES_FILE:-${CHART_PATH}/values.yaml}"
HELM_TIMEOUT="${HELM_TIMEOUT:-600s}"
DRY_RUN="${DRY_RUN:-false}"

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

    # Check Helm
    if ! command -v helm &> /dev/null; then
        error_exit "Helm is not installed. Install from: https://helm.sh/docs/intro/install/"
    fi

    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        error_exit "kubectl is not installed"
    fi

    # Check Helm version (minimum v3.0.0)
    local helm_version=$(helm version --short | grep -oP 'v\K[0-9]+' | head -1)
    if [ "${helm_version}" -lt 3 ]; then
        error_exit "Helm version 3.x or higher is required"
    fi

    # Check Kubernetes connection
    if ! kubectl cluster-info &> /dev/null; then
        error_exit "Cannot connect to Kubernetes cluster"
    fi

    log SUCCESS "Prerequisites check passed"
}

create_namespace() {
    log INFO "Checking namespace: ${NAMESPACE}"

    if ! kubectl get namespace "${NAMESPACE}" &> /dev/null; then
        log INFO "Creating namespace: ${NAMESPACE}"
        kubectl create namespace "${NAMESPACE}"
        kubectl label namespace "${NAMESPACE}" name="${NAMESPACE}"
    else
        log INFO "Namespace ${NAMESPACE} already exists"
    fi
}

create_secrets() {
    log INFO "Creating secrets..."

    # Check if secrets already exist
    if kubectl get secret healthdata-secrets -n "${NAMESPACE}" &> /dev/null; then
        log WARNING "Secrets already exist. Skipping creation."
        log WARNING "To update secrets, delete them first: kubectl delete secret healthdata-secrets -n ${NAMESPACE}"
        return
    fi

    # Prompt for secrets if not set
    if [ -z "${DB_PASSWORD:-}" ]; then
        read -sp "Enter database password: " DB_PASSWORD
        echo
    fi

    if [ -z "${JWT_SECRET:-}" ]; then
        read -sp "Enter JWT secret: " JWT_SECRET
        echo
    fi

    if [ -z "${REDIS_PASSWORD:-}" ]; then
        read -sp "Enter Redis password: " REDIS_PASSWORD
        echo
    fi

    # Create secrets
    kubectl create secret generic healthdata-secrets \
        -n "${NAMESPACE}" \
        --from-literal=db-password="${DB_PASSWORD}" \
        --from-literal=jwt-secret="${JWT_SECRET}" \
        --from-literal=redis-password="${REDIS_PASSWORD}" \
        --dry-run=client -o yaml | kubectl apply -f -

    log SUCCESS "Secrets created successfully"
}

lint_chart() {
    log INFO "Linting Helm chart..."

    helm lint "${CHART_PATH}" --values "${VALUES_FILE}"

    if [ $? -ne 0 ]; then
        error_exit "Helm chart linting failed"
    fi

    log SUCCESS "Helm chart validation passed"
}

package_chart() {
    log INFO "Packaging Helm chart..."

    cd "${PROJECT_ROOT}/helm"

    helm package healthdata-platform

    if [ $? -ne 0 ]; then
        error_exit "Failed to package Helm chart"
    fi

    log SUCCESS "Helm chart packaged successfully"
}

deploy_with_helm() {
    log INFO "Deploying with Helm..."

    local helm_cmd="helm upgrade --install"
    local helm_args=(
        "${RELEASE_NAME}"
        "${CHART_PATH}"
        "--namespace" "${NAMESPACE}"
        "--create-namespace"
        "--values" "${VALUES_FILE}"
        "--timeout" "${HELM_TIMEOUT}"
        "--wait"
        "--atomic"
    )

    # Add additional values files if specified
    if [ -n "${EXTRA_VALUES_FILE:-}" ]; then
        helm_args+=("--values" "${EXTRA_VALUES_FILE}")
    fi

    # Set specific values via command line
    if [ -n "${IMAGE_TAG:-}" ]; then
        helm_args+=("--set" "image.tag=${IMAGE_TAG}")
    fi

    if [ -n "${REPLICA_COUNT:-}" ]; then
        helm_args+=("--set" "replicaCount=${REPLICA_COUNT}")
    fi

    # Dry run if requested
    if [ "${DRY_RUN}" = "true" ]; then
        helm_args+=("--dry-run" "--debug")
        log INFO "Running in DRY RUN mode"
    fi

    # Execute Helm command
    ${helm_cmd} "${helm_args[@]}"

    if [ $? -ne 0 ]; then
        error_exit "Helm deployment failed"
    fi

    log SUCCESS "Helm deployment completed successfully"
}

wait_for_ready() {
    log INFO "Waiting for pods to be ready..."

    kubectl wait --for=condition=ready pod \
        -l app.kubernetes.io/name=healthdata-platform \
        -n "${NAMESPACE}" \
        --timeout=300s

    if [ $? -ne 0 ]; then
        log WARNING "Some pods may not be ready"
        kubectl get pods -n "${NAMESPACE}" -l app.kubernetes.io/name=healthdata-platform
    else
        log SUCCESS "All pods are ready"
    fi
}

run_tests() {
    log INFO "Running Helm tests..."

    helm test "${RELEASE_NAME}" -n "${NAMESPACE}"

    if [ $? -ne 0 ]; then
        log WARNING "Helm tests failed"
    else
        log SUCCESS "Helm tests passed"
    fi
}

display_status() {
    log INFO "Deployment Status:"
    echo ""

    # Helm release status
    helm status "${RELEASE_NAME}" -n "${NAMESPACE}"
    echo ""

    # Pod status
    log INFO "Pod Status:"
    kubectl get pods -n "${NAMESPACE}" -l app.kubernetes.io/name=healthdata-platform
    echo ""

    # Service status
    log INFO "Service Status:"
    kubectl get svc -n "${NAMESPACE}" -l app.kubernetes.io/name=healthdata-platform
    echo ""

    # Ingress status
    log INFO "Ingress Status:"
    kubectl get ingress -n "${NAMESPACE}"
    echo ""

    # Get application URL
    local ingress_host=$(kubectl get ingress -n "${NAMESPACE}" -o jsonpath='{.items[0].spec.rules[0].host}' 2>/dev/null)
    if [ -n "${ingress_host}" ]; then
        log INFO "Application URL: https://${ingress_host}"
    fi
}

display_logs() {
    log INFO "Recent application logs:"
    kubectl logs -n "${NAMESPACE}" -l app.kubernetes.io/name=healthdata-platform --tail=50
}

###############################################################################
# Main Execution
###############################################################################

main() {
    log INFO "Starting HealthData Platform Helm Deployment"
    log INFO "Release: ${RELEASE_NAME}"
    log INFO "Namespace: ${NAMESPACE}"

    # Create logs directory
    mkdir -p "${PROJECT_ROOT}/logs"

    # Execute deployment steps
    check_prerequisites
    create_namespace
    create_secrets
    lint_chart
    deploy_with_helm

    if [ "${DRY_RUN}" != "true" ]; then
        wait_for_ready
        display_status

        # Optionally run tests
        if [ "${RUN_TESTS:-false}" = "true" ]; then
            run_tests
        fi

        # Optionally display logs
        if [ "${SHOW_LOGS:-false}" = "true" ]; then
            display_logs
        fi
    fi

    log SUCCESS "Deployment process completed!"
    log INFO "Log file: ${LOG_FILE}"

    # Display helpful commands
    echo ""
    log INFO "Useful commands:"
    echo "  View pods:      kubectl get pods -n ${NAMESPACE}"
    echo "  View logs:      kubectl logs -n ${NAMESPACE} -l app.kubernetes.io/name=healthdata-platform -f"
    echo "  View services:  kubectl get svc -n ${NAMESPACE}"
    echo "  Helm status:    helm status ${RELEASE_NAME} -n ${NAMESPACE}"
    echo "  Helm history:   helm history ${RELEASE_NAME} -n ${NAMESPACE}"
    echo "  Upgrade:        helm upgrade ${RELEASE_NAME} ${CHART_PATH} -n ${NAMESPACE}"
    echo "  Rollback:       helm rollback ${RELEASE_NAME} -n ${NAMESPACE}"
    echo "  Uninstall:      helm uninstall ${RELEASE_NAME} -n ${NAMESPACE}"
}

# Run main function
main "$@"
