#!/bin/bash

###############################################################################
# HealthData Platform - Production Deployment Script
# Version: 2.0.0
# Description: Build, tag, push Docker image and deploy to production
###############################################################################

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
LOG_FILE="${PROJECT_ROOT}/logs/deployment-$(date +%Y%m%d-%H%M%S).log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration variables
VERSION="${VERSION:-2.0.0}"
ENVIRONMENT="${ENVIRONMENT:-production}"
DOCKER_REGISTRY="${DOCKER_REGISTRY:-docker.io}"
DOCKER_NAMESPACE="${DOCKER_NAMESPACE:-healthdata}"
IMAGE_NAME="${IMAGE_NAME:-platform}"
KUBE_CONTEXT="${KUBE_CONTEXT:-}"
KUBE_NAMESPACE="${KUBE_NAMESPACE:-healthdata}"

# Derived variables
FULL_IMAGE_NAME="${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${IMAGE_NAME}:${VERSION}"
LATEST_IMAGE_NAME="${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${IMAGE_NAME}:latest"

###############################################################################
# Utility Functions
###############################################################################

log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    case $level in
        INFO)
            echo -e "${BLUE}[INFO]${NC} ${message}" | tee -a "${LOG_FILE}"
            ;;
        SUCCESS)
            echo -e "${GREEN}[SUCCESS]${NC} ${message}" | tee -a "${LOG_FILE}"
            ;;
        WARNING)
            echo -e "${YELLOW}[WARNING]${NC} ${message}" | tee -a "${LOG_FILE}"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} ${message}" | tee -a "${LOG_FILE}"
            ;;
    esac
}

error_exit() {
    log ERROR "$1"
    exit 1
}

check_prerequisites() {
    log INFO "Checking prerequisites..."

    # Check Docker
    if ! command -v docker &> /dev/null; then
        error_exit "Docker is not installed"
    fi

    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        error_exit "kubectl is not installed"
    fi

    # Check Docker daemon
    if ! docker info &> /dev/null; then
        error_exit "Docker daemon is not running"
    fi

    # Check Kubernetes context
    if [ -n "${KUBE_CONTEXT}" ]; then
        if ! kubectl config use-context "${KUBE_CONTEXT}" &> /dev/null; then
            error_exit "Failed to switch to Kubernetes context: ${KUBE_CONTEXT}"
        fi
    fi

    # Verify namespace exists
    if ! kubectl get namespace "${KUBE_NAMESPACE}" &> /dev/null; then
        log WARNING "Namespace ${KUBE_NAMESPACE} does not exist. Creating..."
        kubectl create namespace "${KUBE_NAMESPACE}"
    fi

    log SUCCESS "Prerequisites check passed"
}

build_application() {
    log INFO "Building application with Gradle..."

    cd "${PROJECT_ROOT}"

    # Clean and build
    ./gradlew clean build -x test --no-daemon --parallel

    if [ $? -ne 0 ]; then
        error_exit "Application build failed"
    fi

    # Verify JAR exists
    if [ ! -f "${PROJECT_ROOT}/build/libs"/*.jar ]; then
        error_exit "JAR file not found after build"
    fi

    log SUCCESS "Application built successfully"
}

build_docker_image() {
    log INFO "Building Docker image: ${FULL_IMAGE_NAME}"

    cd "${PROJECT_ROOT}"

    # Build with BuildKit for better performance
    DOCKER_BUILDKIT=1 docker build \
        -f Dockerfile.production \
        -t "${FULL_IMAGE_NAME}" \
        -t "${LATEST_IMAGE_NAME}" \
        --build-arg BUILD_DATE="$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
        --build-arg VCS_REF="$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')" \
        --build-arg VERSION="${VERSION}" \
        .

    if [ $? -ne 0 ]; then
        error_exit "Docker image build failed"
    fi

    log SUCCESS "Docker image built successfully"
}

run_security_scan() {
    log INFO "Running security scan on Docker image..."

    # Check if Trivy is installed
    if command -v trivy &> /dev/null; then
        trivy image --severity HIGH,CRITICAL "${FULL_IMAGE_NAME}" || log WARNING "Security vulnerabilities found"
    else
        log WARNING "Trivy not installed, skipping security scan"
    fi
}

push_docker_image() {
    log INFO "Pushing Docker image to registry..."

    # Login to registry if credentials are provided
    if [ -n "${DOCKER_USERNAME:-}" ] && [ -n "${DOCKER_PASSWORD:-}" ]; then
        echo "${DOCKER_PASSWORD}" | docker login "${DOCKER_REGISTRY}" -u "${DOCKER_USERNAME}" --password-stdin
    fi

    # Push versioned image
    docker push "${FULL_IMAGE_NAME}"
    if [ $? -ne 0 ]; then
        error_exit "Failed to push Docker image: ${FULL_IMAGE_NAME}"
    fi

    # Push latest tag
    docker push "${LATEST_IMAGE_NAME}"
    if [ $? -ne 0 ]; then
        log WARNING "Failed to push latest tag"
    fi

    log SUCCESS "Docker image pushed successfully"
}

backup_current_deployment() {
    log INFO "Backing up current deployment..."

    local backup_dir="${PROJECT_ROOT}/backups/deployment-$(date +%Y%m%d-%H%M%S)"
    mkdir -p "${backup_dir}"

    # Backup current deployment
    kubectl get deployment healthdata-platform -n "${KUBE_NAMESPACE}" -o yaml > "${backup_dir}/deployment.yaml" 2>/dev/null || true
    kubectl get configmap -n "${KUBE_NAMESPACE}" -o yaml > "${backup_dir}/configmaps.yaml" 2>/dev/null || true
    kubectl get service -n "${KUBE_NAMESPACE}" -o yaml > "${backup_dir}/services.yaml" 2>/dev/null || true

    log SUCCESS "Backup created at: ${backup_dir}"
}

apply_kubernetes_manifests() {
    log INFO "Applying Kubernetes manifests..."

    cd "${PROJECT_ROOT}/k8s"

    # Apply in order
    kubectl apply -f namespace.yaml
    kubectl apply -f secrets.yaml -n "${KUBE_NAMESPACE}"
    kubectl apply -f configmap.yaml -n "${KUBE_NAMESPACE}"
    kubectl apply -f serviceaccount.yaml -n "${KUBE_NAMESPACE}"
    kubectl apply -f deployment.yaml -n "${KUBE_NAMESPACE}"
    kubectl apply -f service.yaml -n "${KUBE_NAMESPACE}"
    kubectl apply -f ingress.yaml -n "${KUBE_NAMESPACE}"
    kubectl apply -f hpa.yaml -n "${KUBE_NAMESPACE}"

    if [ $? -ne 0 ]; then
        error_exit "Failed to apply Kubernetes manifests"
    fi

    log SUCCESS "Kubernetes manifests applied successfully"
}

wait_for_rollout() {
    log INFO "Waiting for deployment rollout..."

    kubectl rollout status deployment/healthdata-platform -n "${KUBE_NAMESPACE}" --timeout=600s

    if [ $? -ne 0 ]; then
        error_exit "Deployment rollout failed"
    fi

    log SUCCESS "Deployment rollout completed"
}

run_health_checks() {
    log INFO "Running health checks..."

    # Wait for pods to be ready
    sleep 10

    # Get pod names
    local pods=$(kubectl get pods -n "${KUBE_NAMESPACE}" -l app=healthdata-platform -o jsonpath='{.items[*].metadata.name}')

    for pod in $pods; do
        log INFO "Checking health of pod: ${pod}"

        # Check pod status
        local status=$(kubectl get pod "${pod}" -n "${KUBE_NAMESPACE}" -o jsonpath='{.status.phase}')
        if [ "${status}" != "Running" ]; then
            log WARNING "Pod ${pod} is not running (status: ${status})"
            continue
        fi

        # Check liveness probe
        kubectl exec "${pod}" -n "${KUBE_NAMESPACE}" -- curl -f http://localhost:8080/actuator/health/liveness || log WARNING "Liveness probe failed for ${pod}"

        # Check readiness probe
        kubectl exec "${pod}" -n "${KUBE_NAMESPACE}" -- curl -f http://localhost:8080/actuator/health/readiness || log WARNING "Readiness probe failed for ${pod}"
    done

    log SUCCESS "Health checks completed"
}

run_smoke_tests() {
    log INFO "Running smoke tests..."

    # Get service endpoint
    local service_url=$(kubectl get service healthdata-platform -n "${KUBE_NAMESPACE}" -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

    if [ -z "${service_url}" ]; then
        service_url=$(kubectl get service healthdata-platform -n "${KUBE_NAMESPACE}" -o jsonpath='{.spec.clusterIP}')
    fi

    if [ -z "${service_url}" ]; then
        log WARNING "Could not determine service URL, skipping smoke tests"
        return
    fi

    # Test health endpoint
    curl -f "http://${service_url}:8080/actuator/health" || log WARNING "Health endpoint smoke test failed"

    log SUCCESS "Smoke tests completed"
}

display_deployment_info() {
    log INFO "Deployment Information:"
    echo ""
    echo "  Version: ${VERSION}"
    echo "  Image: ${FULL_IMAGE_NAME}"
    echo "  Namespace: ${KUBE_NAMESPACE}"
    echo "  Context: $(kubectl config current-context)"
    echo ""

    log INFO "Deployment Status:"
    kubectl get deployment healthdata-platform -n "${KUBE_NAMESPACE}"
    echo ""

    log INFO "Pod Status:"
    kubectl get pods -n "${KUBE_NAMESPACE}" -l app=healthdata-platform
    echo ""

    log INFO "Service Status:"
    kubectl get service healthdata-platform -n "${KUBE_NAMESPACE}"
    echo ""
}

cleanup() {
    log INFO "Cleaning up..."

    # Remove old images (keep last 3 versions)
    docker images "${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${IMAGE_NAME}" --format "{{.Tag}}" | \
        grep -v "^${VERSION}$" | grep -v "^latest$" | tail -n +4 | \
        xargs -r -I {} docker rmi "${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${IMAGE_NAME}:{}" 2>/dev/null || true

    log SUCCESS "Cleanup completed"
}

###############################################################################
# Main Execution
###############################################################################

main() {
    log INFO "Starting HealthData Platform Production Deployment"
    log INFO "Version: ${VERSION}"
    log INFO "Environment: ${ENVIRONMENT}"

    # Create logs directory
    mkdir -p "${PROJECT_ROOT}/logs"
    mkdir -p "${PROJECT_ROOT}/backups"

    # Execute deployment steps
    check_prerequisites
    build_application
    build_docker_image
    run_security_scan
    push_docker_image
    backup_current_deployment
    apply_kubernetes_manifests
    wait_for_rollout
    run_health_checks
    run_smoke_tests
    display_deployment_info
    cleanup

    log SUCCESS "Production deployment completed successfully!"
    log INFO "Log file: ${LOG_FILE}"
}

# Run main function
main "$@"
