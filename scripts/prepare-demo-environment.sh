#!/bin/bash

################################################################################
# Demo Environment Preparation Script
# Prepares complete environment for documentation screenshots
################################################################################

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Configuration
DEMO_TENANT="demo-tenant"
DEMO_ENVIRONMENT="demo"

################################################################################
# Phase 1: Environment Cleanup
################################################################################

cleanup_environment() {
    log_info "Cleaning up existing environment..."
    
    # Stop all running containers
    cd "$PROJECT_ROOT"
    if [ -f "docker-compose.yml" ]; then
        docker-compose down -v || log_warning "Failed to stop containers"
    fi
    
    # Clean Docker system (optional - commented out for safety)
    # docker system prune -a --volumes -f
    
    log_success "Environment cleaned"
}

################################################################################
# Phase 2: Build Backend Services
################################################################################

build_backend() {
    log_info "Building backend services..."
    
    cd "$PROJECT_ROOT/backend"
    
    # Clean build
    ./gradlew clean
    
    # Build all services
    ./gradlew build -x test
    
    # Build Docker images
    ./gradlew dockerBuild
    
    log_success "Backend services built"
}

################################################################################
# Phase 3: Build Frontend Applications
################################################################################

build_frontend() {
    log_info "Building frontend applications..."
    
    cd "$PROJECT_ROOT/frontend"
    
    # Build each frontend application
    for app in clinical-dashboard admin-portal ai-assistant patient-portal analytics; do
        if [ -d "$app" ]; then
            log_info "Building $app..."
            cd "$app"
            npm install --legacy-peer-deps
            npm run build
            cd ..
        else
            log_warning "Frontend app $app not found, skipping"
        fi
    done
    
    log_success "Frontend applications built"
}

################################################################################
# Phase 4: Start Infrastructure
################################################################################

start_infrastructure() {
    log_info "Starting infrastructure services..."
    
    cd "$PROJECT_ROOT"
    
    # Start infrastructure only
    docker-compose up -d postgres redis kafka zookeeper
    
    # Wait for services to be healthy
    log_info "Waiting for infrastructure to be ready..."
    sleep 30
    
    # Verify PostgreSQL
    docker-compose exec -T postgres pg_isready -U hdim || log_error "PostgreSQL not ready"
    
    # Verify Redis
    docker-compose exec -T redis redis-cli ping || log_error "Redis not ready"
    
    log_success "Infrastructure services started"
}

################################################################################
# Phase 5: Initialize Database
################################################################################

init_database() {
    log_info "Initializing database..."
    
    cd "$PROJECT_ROOT/backend"
    
    # Run Flyway migrations
    ./gradlew flywayMigrate -Penvironment=demo
    
    log_success "Database initialized"
}

################################################################################
# Phase 6: Start Backend Services
################################################################################

start_backend_services() {
    log_info "Starting backend services..."
    
    cd "$PROJECT_ROOT"
    
    # Start all backend services
    docker-compose up -d \
        gateway-service \
        gateway-admin-service \
        gateway-clinical-service \
        gateway-fhir-service \
        cql-engine-service \
        care-gap-service \
        agent-runtime-service \
        predictive-analytics-service \
        quality-measure-service \
        hcc-service \
        patient-service \
        fhir-service \
        analytics-service \
        notification-service
    
    log_info "Waiting for services to start..."
    sleep 60
    
    log_success "Backend services started"
}

################################################################################
# Phase 7: Health Check
################################################################################

health_check() {
    log_info "Performing health checks..."
    
    services=(
        "http://localhost:8080/actuator/health:Gateway"
        "http://localhost:8100/actuator/health:CQL Engine"
        "http://localhost:8101/actuator/health:Care Gap"
        "http://localhost:8088/actuator/health:Agent Runtime"
        "http://localhost:8105/actuator/health:Predictive Analytics"
    )
    
    failed=0
    for service_url in "${services[@]}"; do
        url="${service_url%%:*}"
        name="${service_url##*:}"
        
        if curl -s -f "$url" > /dev/null; then
            log_success "$name service is healthy"
        else
            log_error "$name service is not responding"
            failed=$((failed + 1))
        fi
    done
    
    if [ $failed -eq 0 ]; then
        log_success "All services are healthy"
        return 0
    else
        log_error "$failed service(s) failed health check"
        return 1
    fi
}

################################################################################
# Phase 8: Seed Demo Data
################################################################################

seed_demo_data() {
    log_info "Seeding demo data..."
    
    # Create demo tenant
    curl -X POST http://localhost:8081/api/v1/admin/tenants \
        -H "Content-Type: application/json" \
        -d "{
            \"id\": \"$DEMO_TENANT\",
            \"name\": \"Demo Healthcare Organization\",
            \"enabled\": true
        }" || log_warning "Tenant may already exist"
    
    # Create demo patients
    log_info "Creating demo patients..."
    
    patients=(
        '{"id":"pat-001","tenantId":"demo-tenant","mrn":"MRN001","firstName":"John","lastName":"Diabetes","dob":"1965-03-15","gender":"M"}'
        '{"id":"pat-002","tenantId":"demo-tenant","mrn":"MRN002","firstName":"Sarah","lastName":"Heart","dob":"1958-07-22","gender":"F"}'
        '{"id":"pat-003","tenantId":"demo-tenant","mrn":"MRN003","firstName":"Michael","lastName":"CKD","dob":"1972-11-30","gender":"M"}'
        '{"id":"pat-004","tenantId":"demo-tenant","mrn":"MRN004","firstName":"Emma","lastName":"Healthy","dob":"1990-05-18","gender":"F"}'
        '{"id":"pat-005","tenantId":"demo-tenant","mrn":"MRN005","firstName":"Robert","lastName":"Complex","dob":"1945-12-08","gender":"M"}'
    )
    
    for patient in "${patients[@]}"; do
        curl -X POST http://localhost:8104/api/v1/patients \
            -H "Content-Type: application/json" \
            -H "X-Tenant-ID: $DEMO_TENANT" \
            -d "$patient" || log_warning "Patient may already exist"
    done
    
    log_success "Demo data seeded"
}

################################################################################
# Phase 9: Create Demo Users
################################################################################

create_demo_users() {
    log_info "Creating demo users..."
    
    users=(
        '{"username":"care.manager@demo.com","password":"Demo2026!","role":"CARE_MANAGER","firstName":"Alice","lastName":"Manager"}'
        '{"username":"dr.smith@demo.com","password":"Demo2026!","role":"PHYSICIAN","firstName":"Dr. James","lastName":"Smith"}'
        '{"username":"admin@demo.com","password":"Demo2026!","role":"SYSTEM_ADMIN","firstName":"System","lastName":"Admin"}'
        '{"username":"ai.user@demo.com","password":"Demo2026!","role":"AI_USER","firstName":"AI","lastName":"User"}'
        '{"username":"patient@demo.com","password":"Demo2026!","role":"PATIENT","firstName":"John","lastName":"Patient","patientId":"pat-001"}'
        '{"username":"quality.manager@demo.com","password":"Demo2026!","role":"QUALITY_MANAGER","firstName":"Quality","lastName":"Manager"}'
        '{"username":"billing.specialist@demo.com","password":"Demo2026!","role":"BILLING_SPECIALIST","firstName":"Billing","lastName":"Specialist"}'
    )
    
    for user in "${users[@]}"; do
        curl -X POST http://localhost:8081/api/v1/admin/users \
            -H "Content-Type: application/json" \
            -d "$user" || log_warning "User may already exist"
    done
    
    log_success "Demo users created"
}

################################################################################
# Phase 10: Generate Care Gaps
################################################################################

generate_care_gaps() {
    log_info "Generating care gaps..."
    
    curl -X POST http://localhost:8101/api/v1/care-gaps/identify-batch \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: $DEMO_TENANT" \
        -d '{
            "patientIds": ["pat-001", "pat-002", "pat-003", "pat-004", "pat-005"]
        }' || log_warning "Care gap generation may have failed"
    
    log_success "Care gaps generated"
}

################################################################################
# Phase 11: Run CQL Evaluations
################################################################################

run_cql_evaluations() {
    log_info "Running CQL evaluations..."
    
    curl -X POST http://localhost:8100/api/v1/cql/evaluate-batch \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: $DEMO_TENANT" \
        -d '{
            "patientIds": ["pat-001", "pat-002", "pat-003"],
            "measureIds": ["CMS122", "CMS134", "CMS165"]
        }' || log_warning "CQL evaluation may have failed"
    
    log_success "CQL evaluations complete"
}

################################################################################
# Phase 12: Start Frontend Services
################################################################################

start_frontend_services() {
    log_info "Starting frontend services..."
    
    cd "$PROJECT_ROOT/frontend"
    
    # Start each frontend in background
    for app in clinical-dashboard admin-portal ai-assistant patient-portal analytics; do
        if [ -d "$app" ]; then
            log_info "Starting $app on port..."
            cd "$app"
            npm run start:prod > /tmp/$app.log 2>&1 &
            echo $! > /tmp/$app.pid
            cd ..
        fi
    done
    
    log_success "Frontend services started"
}

################################################################################
# Main Execution
################################################################################

main() {
    log_info "=========================================="
    log_info "Demo Environment Preparation"
    log_info "=========================================="
    
    # Check if running in project root
    if [ ! -f "$PROJECT_ROOT/docker-compose.yml" ]; then
        log_error "docker-compose.yml not found. Please run from project root."
        exit 1
    fi
    
    # Execute phases
    cleanup_environment
    build_backend
    build_frontend
    start_infrastructure
    init_database
    start_backend_services
    
    if health_check; then
        seed_demo_data
        create_demo_users
        generate_care_gaps
        run_cql_evaluations
        start_frontend_services
        
        log_success "=========================================="
        log_success "Demo environment ready!"
        log_success "=========================================="
        log_info ""
        log_info "Access URLs:"
        log_info "  Clinical Dashboard: http://localhost:3000"
        log_info "  Admin Portal:       http://localhost:3001"
        log_info "  AI Assistant:       http://localhost:3002"
        log_info "  Patient Portal:     http://localhost:3003"
        log_info "  Analytics:          http://localhost:3004"
        log_info ""
        log_info "Demo Users:"
        log_info "  Care Manager:       care.manager@demo.com / Demo2026!"
        log_info "  Physician:          dr.smith@demo.com / Demo2026!"
        log_info "  Admin:              admin@demo.com / Demo2026!"
        log_info "  AI User:            ai.user@demo.com / Demo2026!"
        log_info "  Patient:            patient@demo.com / Demo2026!"
        log_info ""
        log_info "Ready for screenshots!"
    else
        log_error "Environment setup failed. Check logs for details."
        exit 1
    fi
}

# Run main function
main "$@"
