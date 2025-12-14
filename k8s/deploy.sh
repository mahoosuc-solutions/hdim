#!/bin/bash
set -e

# HDIM Kubernetes Deployment Script
# Deploys HDIM platform to local Kubernetes cluster (Docker Desktop)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Parse arguments
ACTION="${1:-deploy}"
PROFILE="${2:-core}"

show_help() {
    echo "HDIM Kubernetes Deployment Script"
    echo ""
    echo "Usage: $0 [action] [profile]"
    echo ""
    echo "Actions:"
    echo "  deploy     Deploy HDIM to Kubernetes (default)"
    echo "  delete     Remove HDIM from Kubernetes"
    echo "  status     Show deployment status"
    echo "  build      Build Docker images only"
    echo "  logs       Show logs for services"
    echo ""
    echo "Profiles:"
    echo "  infra      Infrastructure only (PostgreSQL, Redis, Kafka)"
    echo "  core       Core services (infra + 10 clinical services)"
    echo "  ai         AI services (infra + 3 AI services)"
    echo "  analytics  Analytics services (infra + 3 analytics)"
    echo "  full       All 22 services"
    echo ""
    echo "Examples:"
    echo "  $0 deploy core      # Deploy infrastructure + core services"
    echo "  $0 deploy full      # Deploy all services"
    echo "  $0 status           # Check deployment status"
    echo "  $0 delete           # Remove all HDIM resources"
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl not found. Please install kubectl."
        exit 1
    fi

    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster. Is Docker Desktop running with Kubernetes enabled?"
        exit 1
    fi

    log_success "Prerequisites check passed"
}

build_images() {
    log_info "Building Docker images..."

    cd "$PROJECT_ROOT"

    # Define services based on profile
    declare -a SERVICES

    case "$PROFILE" in
        infra)
            SERVICES=()
            ;;
        core)
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
            )
            ;;
        ai)
            SERVICES=(
                "agent-runtime-service"
                "ai-assistant-service"
                "agent-builder-service"
            )
            ;;
        analytics)
            SERVICES=(
                "analytics-service"
                "predictive-analytics-service"
                "sdoh-service"
            )
            ;;
        full)
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
            ;;
    esac

    for service in "${SERVICES[@]}"; do
        log_info "Building $service..."
        docker build -t "hdim/$service:latest" \
            -f "backend/modules/services/$service/Dockerfile" \
            backend/ || {
            log_warning "Failed to build $service, continuing..."
        }
    done

    log_success "Docker images built"
}

deploy_infra() {
    log_info "Deploying infrastructure..."

    kubectl apply -f "$SCRIPT_DIR/base/namespace.yaml"
    kubectl apply -f "$SCRIPT_DIR/base/secrets.yaml"
    kubectl apply -f "$SCRIPT_DIR/base/configmap.yaml"
    kubectl apply -f "$SCRIPT_DIR/infrastructure/"

    log_info "Waiting for infrastructure to be ready..."
    kubectl wait --for=condition=ready pod -l app=postgres -n hdim --timeout=120s || true
    kubectl wait --for=condition=ready pod -l app=redis -n hdim --timeout=60s || true
    kubectl wait --for=condition=ready pod -l app=zookeeper -n hdim --timeout=90s || true
    kubectl wait --for=condition=ready pod -l app=kafka -n hdim --timeout=120s || true

    log_success "Infrastructure deployed"
}

deploy_services() {
    local tier="$1"
    log_info "Deploying $tier services..."

    case "$tier" in
        core)
            kubectl apply -f "$SCRIPT_DIR/services/gateway-service.yaml"
            kubectl apply -f "$SCRIPT_DIR/services/fhir-service.yaml"
            kubectl apply -f "$SCRIPT_DIR/services/patient-service.yaml"
            kubectl apply -f "$SCRIPT_DIR/services/care-gap-service.yaml"
            kubectl apply -f "$SCRIPT_DIR/services/quality-measure-service.yaml"
            kubectl apply -f "$SCRIPT_DIR/services/cql-engine-service.yaml"
            kubectl apply -f "$SCRIPT_DIR/services/consent-service.yaml"
            kubectl apply -f "$SCRIPT_DIR/services/event-processing-service.yaml"
            kubectl apply -f "$SCRIPT_DIR/services/event-router-service.yaml"
            ;;
        ai)
            kubectl apply -f "$SCRIPT_DIR/services/agent-runtime-service.yaml"
            kubectl apply -f "$SCRIPT_DIR/services/ai-assistant-service.yaml"
            kubectl apply -f "$SCRIPT_DIR/services/agent-builder-service.yaml"
            ;;
        analytics)
            kubectl apply -f "$SCRIPT_DIR/services/analytics-service.yaml"
            kubectl apply -f "$SCRIPT_DIR/services/predictive-analytics-service.yaml"
            kubectl apply -f "$SCRIPT_DIR/services/sdoh-service.yaml"
            ;;
        workflow)
            kubectl apply -f "$SCRIPT_DIR/services/approval-service.yaml"
            kubectl apply -f "$SCRIPT_DIR/services/payer-workflows-service.yaml"
            ;;
        data)
            kubectl apply -f "$SCRIPT_DIR/services/cdr-processor-service.yaml"
            kubectl apply -f "$SCRIPT_DIR/services/data-enrichment-service.yaml"
            ;;
        integration)
            kubectl apply -f "$SCRIPT_DIR/services/ehr-connector-service.yaml"
            ;;
        support)
            kubectl apply -f "$SCRIPT_DIR/services/documentation-service.yaml"
            ;;
    esac

    log_success "$tier services deployed"
}

deploy() {
    check_prerequisites

    log_info "Deploying HDIM with profile: $PROFILE"

    # Always deploy infrastructure first
    deploy_infra

    case "$PROFILE" in
        infra)
            log_success "Infrastructure deployment complete"
            ;;
        core)
            build_images
            deploy_services "core"
            ;;
        ai)
            build_images
            deploy_services "ai"
            ;;
        analytics)
            build_images
            deploy_services "analytics"
            ;;
        full)
            build_images
            deploy_services "core"
            deploy_services "ai"
            deploy_services "analytics"
            deploy_services "workflow"
            deploy_services "data"
            deploy_services "integration"
            deploy_services "support"
            ;;
        *)
            log_error "Unknown profile: $PROFILE"
            show_help
            exit 1
            ;;
    esac

    log_success "HDIM deployment complete!"
    echo ""
    show_status
}

show_status() {
    log_info "HDIM Deployment Status"
    echo ""

    echo "=== Namespace ==="
    kubectl get namespace hdim 2>/dev/null || echo "Namespace not found"
    echo ""

    echo "=== Pods ==="
    kubectl get pods -n hdim 2>/dev/null || echo "No pods found"
    echo ""

    echo "=== Services ==="
    kubectl get services -n hdim 2>/dev/null || echo "No services found"
    echo ""

    echo "=== Resource Usage ==="
    kubectl top pods -n hdim 2>/dev/null || echo "Metrics not available"
}

delete_deployment() {
    log_warning "Removing HDIM from Kubernetes..."

    kubectl delete namespace hdim --ignore-not-found=true

    log_success "HDIM removed from Kubernetes"
}

show_logs() {
    local service="${3:-gateway-service}"
    log_info "Showing logs for $service..."
    kubectl logs -f -l app="$service" -n hdim --tail=100
}

# Main
case "$ACTION" in
    deploy)
        deploy
        ;;
    delete)
        delete_deployment
        ;;
    status)
        check_prerequisites
        show_status
        ;;
    build)
        build_images
        ;;
    logs)
        show_logs
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        log_error "Unknown action: $ACTION"
        show_help
        exit 1
        ;;
esac
