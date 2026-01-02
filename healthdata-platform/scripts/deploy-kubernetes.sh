#!/bin/bash
# HealthData Platform - Kubernetes Deployment Script
# This script deploys services to Kubernetes clusters with zero-downtime rolling updates
set -euo pipefail

# Script configuration
ENVIRONMENT=${1:-staging}
IMAGE_TAG=${2:-latest}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
K8S_DIR="${PROJECT_ROOT}/k8s"

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
    log_info "Validating deployment environment: $ENVIRONMENT"

    if [[ ! "$ENVIRONMENT" =~ ^(staging|production|development)$ ]]; then
        log_error "Invalid environment: $ENVIRONMENT. Must be staging, production, or development"
        exit 1
    fi

    log_success "Environment validation passed"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl not found. Please install kubectl first."
        exit 1
    fi

    # Check kubectl connectivity
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster. Please check your kubeconfig."
        exit 1
    fi

    # Check helm (optional)
    if command -v helm &> /dev/null; then
        log_info "Helm is installed: $(helm version --short)"
    else
        log_warning "Helm not found. Helm charts will not be used."
    fi

    log_success "Prerequisites check passed"
}

# Create namespace if it doesn't exist
create_namespace() {
    log_info "Creating namespace: $ENVIRONMENT"

    if kubectl get namespace "$ENVIRONMENT" &> /dev/null; then
        log_info "Namespace $ENVIRONMENT already exists"
    else
        kubectl create namespace "$ENVIRONMENT"
        log_success "Namespace $ENVIRONMENT created"
    fi

    # Label namespace
    kubectl label namespace "$ENVIRONMENT" \
        environment="$ENVIRONMENT" \
        managed-by=kubectl \
        --overwrite
}

# Apply secrets
apply_secrets() {
    log_info "Applying secrets for $ENVIRONMENT..."

    # Check if secrets file exists
    if [ -f "${K8S_DIR}/${ENVIRONMENT}/secrets.yaml" ]; then
        kubectl apply -f "${K8S_DIR}/${ENVIRONMENT}/secrets.yaml" -n "$ENVIRONMENT"
        log_success "Secrets applied"
    elif [ -f "${K8S_DIR}/secrets.yaml" ]; then
        kubectl apply -f "${K8S_DIR}/secrets.yaml" -n "$ENVIRONMENT"
        log_success "Secrets applied"
    else
        log_warning "No secrets file found. Make sure secrets are configured manually."
    fi
}

# Apply ConfigMaps
apply_configmaps() {
    log_info "Applying ConfigMaps for $ENVIRONMENT..."

    # Check if configmap file exists
    if [ -f "${K8S_DIR}/${ENVIRONMENT}/configmap.yaml" ]; then
        kubectl apply -f "${K8S_DIR}/${ENVIRONMENT}/configmap.yaml" -n "$ENVIRONMENT"
        log_success "ConfigMaps applied"
    elif [ -f "${K8S_DIR}/configmap.yaml" ]; then
        kubectl apply -f "${K8S_DIR}/configmap.yaml" -n "$ENVIRONMENT"
        log_success "ConfigMaps applied"
    else
        log_warning "No ConfigMap file found."
    fi
}

# Deploy database (PostgreSQL)
deploy_database() {
    log_info "Deploying PostgreSQL database..."

    if [ -f "${K8S_DIR}/database/postgres-statefulset.yaml" ]; then
        kubectl apply -f "${K8S_DIR}/database/postgres-statefulset.yaml" -n "$ENVIRONMENT"
        kubectl apply -f "${K8S_DIR}/database/postgres-service.yaml" -n "$ENVIRONMENT"
        log_success "PostgreSQL deployed"
    else
        log_warning "PostgreSQL manifests not found. Skipping database deployment."
    fi
}

# Deploy Redis
deploy_redis() {
    log_info "Deploying Redis cache..."

    if [ -f "${K8S_DIR}/cache/redis-deployment.yaml" ]; then
        kubectl apply -f "${K8S_DIR}/cache/redis-deployment.yaml" -n "$ENVIRONMENT"
        kubectl apply -f "${K8S_DIR}/cache/redis-service.yaml" -n "$ENVIRONMENT"
        log_success "Redis deployed"
    else
        log_warning "Redis manifests not found. Skipping cache deployment."
    fi
}

# Deploy backend services
deploy_backend_services() {
    log_info "Deploying backend services..."

    local services=(
        "fhir-service"
        "quality-measure-service"
        "cql-engine-service"
        "patient-service"
        "care-gap-service"
        "event-processing-service"
    )

    for service in "${services[@]}"; do
        log_info "Deploying $service..."

        # Update image tag in deployment
        if [ -f "${K8S_DIR}/services/${service}-deployment.yaml" ]; then
            # Create a temporary file with updated image tag
            sed "s|image:.*$service.*|image: ghcr.io/healthdata-platform/$service:$IMAGE_TAG|g" \
                "${K8S_DIR}/services/${service}-deployment.yaml" > /tmp/${service}-deployment.yaml

            kubectl apply -f /tmp/${service}-deployment.yaml -n "$ENVIRONMENT"
            kubectl apply -f "${K8S_DIR}/services/${service}-service.yaml" -n "$ENVIRONMENT" || true

            rm /tmp/${service}-deployment.yaml
            log_success "$service deployed"
        else
            log_warning "$service deployment manifest not found. Skipping."
        fi
    done
}

# Deploy frontend services
deploy_frontend_services() {
    log_info "Deploying frontend services..."

    if [ -f "${K8S_DIR}/frontend/clinical-portal-deployment.yaml" ]; then
        # Update image tag
        sed "s|image:.*clinical-portal.*|image: ghcr.io/healthdata-platform/clinical-portal:$IMAGE_TAG|g" \
            "${K8S_DIR}/frontend/clinical-portal-deployment.yaml" > /tmp/clinical-portal-deployment.yaml

        kubectl apply -f /tmp/clinical-portal-deployment.yaml -n "$ENVIRONMENT"
        kubectl apply -f "${K8S_DIR}/frontend/clinical-portal-service.yaml" -n "$ENVIRONMENT"

        rm /tmp/clinical-portal-deployment.yaml
        log_success "Clinical portal deployed"
    else
        log_warning "Clinical portal manifests not found. Skipping frontend deployment."
    fi
}

# Deploy ingress
deploy_ingress() {
    log_info "Deploying ingress controller..."

    if [ -f "${K8S_DIR}/ingress/${ENVIRONMENT}-ingress.yaml" ]; then
        kubectl apply -f "${K8S_DIR}/ingress/${ENVIRONMENT}-ingress.yaml" -n "$ENVIRONMENT"
        log_success "Ingress deployed"
    elif [ -f "${K8S_DIR}/ingress/ingress.yaml" ]; then
        kubectl apply -f "${K8S_DIR}/ingress/ingress.yaml" -n "$ENVIRONMENT"
        log_success "Ingress deployed"
    else
        log_warning "Ingress manifests not found. Skipping ingress deployment."
    fi
}

# Deploy horizontal pod autoscalers
deploy_hpa() {
    log_info "Deploying Horizontal Pod Autoscalers..."

    if [ -f "${K8S_DIR}/autoscaling/hpa.yaml" ]; then
        kubectl apply -f "${K8S_DIR}/autoscaling/hpa.yaml" -n "$ENVIRONMENT"
        log_success "HPA deployed"
    else
        log_warning "HPA manifests not found. Skipping autoscaling deployment."
    fi
}

# Wait for deployments to be ready
wait_for_deployments() {
    log_info "Waiting for deployments to be ready..."

    local deployments=$(kubectl get deployments -n "$ENVIRONMENT" -o jsonpath='{.items[*].metadata.name}')

    for deployment in $deployments; do
        log_info "Waiting for $deployment..."

        if kubectl rollout status deployment/"$deployment" -n "$ENVIRONMENT" --timeout=300s; then
            log_success "$deployment is ready"
        else
            log_error "$deployment failed to become ready"
            return 1
        fi
    done

    log_success "All deployments are ready"
}

# Verify deployment health
verify_deployment() {
    log_info "Verifying deployment health..."

    # Check pod status
    log_info "Pod status:"
    kubectl get pods -n "$ENVIRONMENT" -o wide

    # Check for failed pods
    local failed_pods=$(kubectl get pods -n "$ENVIRONMENT" --field-selector=status.phase!=Running,status.phase!=Succeeded -o jsonpath='{.items[*].metadata.name}')

    if [ -n "$failed_pods" ]; then
        log_error "Failed pods detected: $failed_pods"

        # Show logs for failed pods
        for pod in $failed_pods; do
            log_error "Logs for failed pod $pod:"
            kubectl logs "$pod" -n "$ENVIRONMENT" --tail=50 || true
        done

        return 1
    fi

    # Check service endpoints
    log_info "Service endpoints:"
    kubectl get endpoints -n "$ENVIRONMENT"

    log_success "Deployment health check passed"
}

# Run database migrations
run_migrations() {
    log_info "Running database migrations..."

    # Create a migration job
    if [ -f "${K8S_DIR}/jobs/migration-job.yaml" ]; then
        kubectl apply -f "${K8S_DIR}/jobs/migration-job.yaml" -n "$ENVIRONMENT"

        # Wait for job to complete
        kubectl wait --for=condition=complete --timeout=300s job/migration-job -n "$ENVIRONMENT" || {
            log_error "Migration job failed"
            kubectl logs job/migration-job -n "$ENVIRONMENT"
            return 1
        }

        log_success "Database migrations completed"
    else
        log_warning "Migration job manifest not found. Skipping migrations."
    fi
}

# Cleanup old resources
cleanup_old_resources() {
    log_info "Cleaning up old resources..."

    # Remove completed jobs older than 7 days
    kubectl delete jobs -n "$ENVIRONMENT" --field-selector status.successful=1 || true

    # Remove old replica sets
    kubectl delete replicasets -n "$ENVIRONMENT" --field-selector 'status.replicas=0' || true

    log_success "Cleanup completed"
}

# Display deployment summary
display_summary() {
    log_info "Deployment Summary"
    echo "===================="
    echo "Environment: $ENVIRONMENT"
    echo "Image Tag: $IMAGE_TAG"
    echo "Timestamp: $(date)"
    echo ""

    log_info "Deployments:"
    kubectl get deployments -n "$ENVIRONMENT"
    echo ""

    log_info "Services:"
    kubectl get services -n "$ENVIRONMENT"
    echo ""

    log_info "Ingress:"
    kubectl get ingress -n "$ENVIRONMENT" || echo "No ingress configured"
    echo ""

    log_success "Deployment completed successfully!"
}

# Main deployment workflow
main() {
    log_info "Starting deployment to $ENVIRONMENT environment with image tag $IMAGE_TAG"

    # Validate and prepare
    validate_environment
    check_prerequisites
    create_namespace

    # Deploy infrastructure
    apply_secrets
    apply_configmaps
    deploy_database
    deploy_redis

    # Deploy application services
    deploy_backend_services
    deploy_frontend_services
    deploy_ingress
    deploy_hpa

    # Post-deployment tasks
    wait_for_deployments
    verify_deployment
    run_migrations
    cleanup_old_resources

    # Summary
    display_summary

    log_success "Deployment to $ENVIRONMENT completed successfully! 🚀"
}

# Error handler
trap 'log_error "Deployment failed at line $LINENO. Exit code: $?"' ERR

# Run main function
main "$@"
