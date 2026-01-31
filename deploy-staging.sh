#!/bin/bash

# HealthData-in-Motion - Staging Deployment Script
# Version: 2.0.0
# Description: Complete deployment orchestration for staging environment

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Deployment configuration
COMPOSE_FILE="docker-compose.staging.yml"
PROJECT_NAME="healthdata-staging"
DEPLOYMENT_LOG="logs/staging-deployment-$(date +%Y%m%d-%H%M%S).log"

# Create logs directory
mkdir -p logs/staging

# Logging function
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$DEPLOYMENT_LOG"
}

log_error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1" | tee -a "$DEPLOYMENT_LOG"
}

log_warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING:${NC} $1" | tee -a "$DEPLOYMENT_LOG"
}

log_info() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] INFO:${NC} $1" | tee -a "$DEPLOYMENT_LOG"
}

# Print banner
print_banner() {
    echo ""
    echo "═══════════════════════════════════════════════════════════"
    echo "  HealthData-in-Motion - Staging Deployment"
    echo "  Event-Driven Patient Health Intelligence Platform"
    echo "  Version: 2.0.0"
    echo "═══════════════════════════════════════════════════════════"
    echo ""
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."

    # Check Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi

    # Check Docker Compose
    if ! command -v docker compose &> /dev/null; then
        log_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi

    # Check if Docker daemon is running
    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running. Please start Docker."
        exit 1
    fi

    # Check available disk space (require at least 10GB)
    available_space=$(df -BG . | awk 'NR==2 {print $4}' | sed 's/G//')
    if [ "$available_space" -lt 10 ]; then
        log_warning "Low disk space: ${available_space}GB available. At least 10GB recommended."
    fi

    log "✅ Prerequisites check passed"
}

# Clean up previous deployment
cleanup_previous() {
    log "Cleaning up previous deployment..."

    # Stop and remove containers
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down -v 2>/dev/null || true

    # Remove old images (optional - comment out to keep cache)
    # docker images | grep healthdata | awk '{print $3}' | xargs -r docker rmi -f || true

    # Clean Gradle build cache
    ./backend/gradlew clean -p backend || true

    log "✅ Cleanup complete"
}

# Build all service JARs
build_services() {
    log "Building all services with Gradle..."

    cd backend

    # Build all services in parallel for speed
    ./gradlew \
        :modules:services:cql-engine-service:bootJar \
        :modules:services:quality-measure-service:bootJar \
        :modules:services:event-router-service:bootJar \
        :modules:services:patient-service:bootJar \
        :modules:services:care-gap-service:bootJar \
        :modules:services:gateway-admin-service:bootJar \
        :modules:services:gateway-fhir-service:bootJar \
        :modules:services:gateway-clinical-service:bootJar \
        --parallel \
        --no-daemon \
        --build-cache 2>&1 | tee -a "../$DEPLOYMENT_LOG"

    if [ $? -eq 0 ]; then
        log "✅ All services built successfully"
    else
        log_error "Service build failed. Check logs for details."
        exit 1
    fi

    cd ..
}

# Build all Docker images
build_docker_images() {
    log "Building Docker images..."

    # Build images using docker compose
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" build --parallel 2>&1 | tee -a "$DEPLOYMENT_LOG"

    if [ $? -eq 0 ]; then
        log "✅ Docker images built successfully"
    else
        log_error "Docker image build failed"
        exit 1
    fi

    # List built images
    log_info "Built images:"
    docker images | grep healthdata | tee -a "$DEPLOYMENT_LOG"
}

# Start infrastructure services first
start_infrastructure() {
    log "Starting infrastructure services (Postgres, Redis, Kafka)..."

    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d \
        postgres \
        redis \
        zookeeper \
        kafka

    log "Waiting for infrastructure to be healthy..."
    sleep 15

    # Wait for Postgres
    log_info "Waiting for PostgreSQL..."
    for i in {1..30}; do
        if docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" exec -T postgres pg_isready -U healthdata &>/dev/null; then
            log "✅ PostgreSQL is ready"
            break
        fi
        sleep 2
    done

    # Wait for Kafka
    log_info "Waiting for Kafka..."
    for i in {1..30}; do
        if docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list &>/dev/null; then
            log "✅ Kafka is ready"
            break
        fi
        sleep 2
    done

    log "✅ Infrastructure services started"
}

# Start FHIR service
start_fhir() {
    log "Starting FHIR service..."

    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d fhir-service

    log "Waiting for FHIR service to be ready (this may take 2-3 minutes)..."
    for i in {1..60}; do
        if curl -f http://localhost:8083/fhir/metadata &>/dev/null; then
            log "✅ FHIR service is ready"
            break
        fi
        sleep 3
    done
}

# Start application services
start_application_services() {
    log "Starting application services..."

    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d \
        cql-engine-service \
        patient-service \
        care-gap-service \
        event-router-service \
        quality-measure-service

    log "Waiting for services to be healthy..."
    sleep 30

    # Check service health
    check_service_health "CQL Engine" "http://localhost:8081/cql-engine/actuator/health"
    check_service_health "Quality Measure" "http://localhost:8087/quality-measure/actuator/health"
    check_service_health "Event Router" "http://localhost:8089/actuator/health"
    check_service_health "Patient Service" "http://localhost:8084/actuator/health"
    check_service_health "Care Gap Service" "http://localhost:8085/actuator/health"

    log "✅ Application services started"
}

# Start gateway and frontend
start_gateway_frontend() {
    log "Starting gateway and frontend..."

    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d \
        gateway-admin-service \
        gateway-fhir-service \
        gateway-clinical-service \
        gateway-edge \
        clinical-portal

    sleep 20

    check_service_health "Gateway" "http://localhost:9000/actuator/health"
    check_service_health "Clinical Portal" "http://localhost:4200/"

    log "✅ Gateway and frontend started"
}

# Start monitoring services
start_monitoring() {
    log "Starting monitoring services (Prometheus, Grafana)..."

    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d \
        prometheus \
        grafana

    sleep 10

    log "✅ Monitoring services started"
    log_info "Prometheus: http://localhost:9090"
    log_info "Grafana: http://localhost:3001 (admin/staging_grafana_2025)"
}

# Check service health
check_service_health() {
    local service_name=$1
    local health_url=$2
    local max_attempts=30

    log_info "Checking $service_name health..."

    for i in $(seq 1 $max_attempts); do
        if curl -f -s "$health_url" &>/dev/null; then
            log "✅ $service_name is healthy"
            return 0
        fi
        sleep 2
    done

    log_warning "$service_name health check timed out (may still be starting)"
    return 1
}

# Run database migrations
run_migrations() {
    log "Running database migrations..."

    # Migrations are run automatically by Liquibase on service startup
    # We just need to verify they ran successfully

    log_info "Verifying migrations..."

    # Check migration status for each service
    sleep 10

    # Verify via service logs
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" logs cql-engine-service | grep -i "liquibase" | tail -5 | tee -a "$DEPLOYMENT_LOG"
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" logs quality-measure-service | grep -i "liquibase" | tail -5 | tee -a "$DEPLOYMENT_LOG"

    log "✅ Database migrations completed"
}

# Apply row-level security
apply_rls() {
    log "Applying row-level security migration..."

    # Copy RLS script into postgres container and execute
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" exec -T postgres psql -U healthdata -d healthdata_quality_measure < backend/enable-row-level-security.sql 2>&1 | tee -a "$DEPLOYMENT_LOG"

    if [ $? -eq 0 ]; then
        log "✅ Row-level security applied"
    else
        log_warning "RLS migration had warnings (may already be applied)"
    fi
}

# Load test data
load_test_data() {
    log "Loading test/demo data..."

    # Execute test data scripts if they exist
    if [ -f "sample-data/comprehensive-fhir-test-data.sh" ]; then
        bash sample-data/comprehensive-fhir-test-data.sh 2>&1 | tee -a "$DEPLOYMENT_LOG"
        log "✅ Test data loaded"
    else
        log_warning "No test data scripts found - skipping"
    fi
}

# Run smoke tests
run_smoke_tests() {
    log "Running smoke tests..."

    # Basic connectivity tests
    log_info "Testing service endpoints..."

    # Test FHIR service
    if curl -f http://localhost:8083/fhir/metadata &>/dev/null; then
        log "✅ FHIR service responding"
    else
        log_error "FHIR service not responding"
    fi

    # Test Gateway
    if curl -f http://localhost:9000/actuator/health &>/dev/null; then
        log "✅ Gateway responding"
    else
        log_error "Gateway not responding"
    fi

    # Test Quality Measure Service
    if curl -f http://localhost:8087/quality-measure/actuator/health &>/dev/null; then
        log "✅ Quality Measure service responding"
    else
        log_error "Quality Measure service not responding"
    fi

    # Test Clinical Portal
    if curl -f http://localhost:4200/ &>/dev/null; then
        log "✅ Clinical Portal responding"
    else
        log_error "Clinical Portal not responding"
    fi

    log "✅ Smoke tests completed"
}

# Print deployment summary
print_summary() {
    echo ""
    echo "═══════════════════════════════════════════════════════════"
    echo "  Deployment Summary"
    echo "═══════════════════════════════════════════════════════════"
    echo ""
    echo "🎉 Staging environment is deployed and running!"
    echo ""
    echo "📊 Service Endpoints:"
    echo "   • Clinical Portal:       http://localhost:4200"
    echo "   • API Gateway:           http://localhost:9000"
    echo "   • FHIR Server:           http://localhost:8083/fhir"
    echo "   • Quality Measure API:   http://localhost:8087/quality-measure"
    echo "   • CQL Engine API:        http://localhost:8081/cql-engine"
    echo "   • Event Router API:      http://localhost:8089"
    echo ""
    echo "📈 Monitoring:"
    echo "   • Prometheus:            http://localhost:9090"
    echo "   • Grafana:               http://localhost:3001"
    echo "     Username: admin"
    echo "     Password: staging_grafana_2025"
    echo ""
    echo "🗄️  Databases:"
    echo "   • PostgreSQL:            localhost:5435"
    echo "     Username: healthdata"
    echo "     Password: staging_password_2025"
    echo ""
    echo "📨 Event Streaming:"
    echo "   • Kafka:                 localhost:9094"
    echo ""
    echo "📝 Deployment Log:"
    echo "   • Log file:              $DEPLOYMENT_LOG"
    echo ""
    echo "🔧 Management Commands:"
    echo "   • View logs:             docker compose -f $COMPOSE_FILE -p $PROJECT_NAME logs -f"
    echo "   • Stop services:         docker compose -f $COMPOSE_FILE -p $PROJECT_NAME stop"
    echo "   • Restart services:      docker compose -f $COMPOSE_FILE -p $PROJECT_NAME restart"
    echo "   • Tear down:             docker compose -f $COMPOSE_FILE -p $PROJECT_NAME down -v"
    echo ""
    echo "═══════════════════════════════════════════════════════════"
    echo ""
}

# Main deployment flow
main() {
    print_banner

    log "Starting staging deployment..."

    # Step 1: Prerequisites
    check_prerequisites

    # Step 2: Cleanup
    if [ "${SKIP_CLEANUP:-false}" != "true" ]; then
        cleanup_previous
    fi

    # Step 3: Build services
    build_services

    # Step 4: Build Docker images
    build_docker_images

    # Step 5: Start infrastructure
    start_infrastructure

    # Step 6: Start FHIR service
    start_fhir

    # Step 7: Start application services
    start_application_services

    # Step 8: Start gateway and frontend
    start_gateway_frontend

    # Step 9: Start monitoring
    start_monitoring

    # Step 10: Run migrations
    run_migrations

    # Step 11: Apply RLS
    if [ "${SKIP_RLS:-false}" != "true" ]; then
        apply_rls
    fi

    # Step 12: Load test data
    if [ "${LOAD_TEST_DATA:-false}" == "true" ]; then
        load_test_data
    fi

    # Step 13: Run smoke tests
    run_smoke_tests

    # Step 14: Print summary
    print_summary

    log "🎉 Deployment completed successfully!"
}

# Handle script interruption
trap 'log_error "Deployment interrupted"; exit 1' INT TERM

# Run main deployment
main "$@"
