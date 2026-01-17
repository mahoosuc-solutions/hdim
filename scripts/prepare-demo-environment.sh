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
DEMO_TENANT="${DEMO_TENANT:-demo-tenant}"
DEMO_ENVIRONMENT="${DEMO_ENVIRONMENT:-demo}"
QUALITY_MEASURE_URL="${QUALITY_MEASURE_URL:-http://localhost:8087/quality-measure}"
FHIR_SEED_SCRIPT="${FHIR_SEED_SCRIPT:-$PROJECT_ROOT/load-fhir-demo-data.sh}"
CLINICAL_SEED_SCRIPT="${CLINICAL_SEED_SCRIPT:-$PROJECT_ROOT/load-demo-clinical-data.sh}"
PATIENT_SEED_SCRIPT="${PATIENT_SEED_SCRIPT:-$PROJECT_ROOT/load-demo-patient-data.sh}"
POSTGRES_USER="${POSTGRES_USER:-healthdata}"
POSTGRES_CQL_DB="${POSTGRES_CQL_DB:-cql_db}"
AUTH_USER_ID="${AUTH_USER_ID:-550e8400-e29b-41d4-a716-446655440010}"
AUTH_USERNAME="${AUTH_USERNAME:-demo_admin@hdim.ai}"
AUTH_ROLES="${AUTH_ROLES:-ADMIN,EVALUATOR}"
USE_TRUSTED_HEADERS="${USE_TRUSTED_HEADERS:-true}"

AUTH_HEADERS=()
if [ "$USE_TRUSTED_HEADERS" = "true" ]; then
    VALIDATED_TS=$(date +%s)
    AUTH_HEADERS=(
        -H "X-Auth-User-Id: $AUTH_USER_ID"
        -H "X-Auth-Username: $AUTH_USERNAME"
        -H "X-Auth-Roles: $AUTH_ROLES"
        -H "X-Auth-Tenant-Ids: $DEMO_TENANT"
        -H "X-Auth-Validated: gateway-${VALIDATED_TS}-dev"
    )
fi

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
    docker-compose exec -T postgres pg_isready -U "$POSTGRES_USER" || log_error "PostgreSQL not ready"
    
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

    seed_quality_measures_and_operational_data
    
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
# Phase 11a: Seed Quality Measures + Operational Data
################################################################################

seed_quality_measure_definitions() {
    log_info "Seeding quality measure definitions..."

    response=$(curl -s -X POST "${QUALITY_MEASURE_URL}/api/v1/measures/seed" \
        -H "X-Tenant-ID: $DEMO_TENANT" \
        "${AUTH_HEADERS[@]}" 2>/dev/null || true)

    if echo "$response" | grep -q "seeded"; then
        log_success "Quality measures seeded"
    else
        log_warning "Quality measure seeding may have failed or already exists"
    fi
}

seed_cql_libraries_for_tenant() {
    if [ "$DEMO_TENANT" = "demo-tenant" ]; then
        return 0
    fi

    log_info "Seeding CQL libraries for tenant $DEMO_TENANT..."

    docker-compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_CQL_DB" -c \
        "INSERT INTO cql_libraries (
            id, tenant_id, name, version, status, cql_content, elm_json, description, publisher,
            created_at, updated_at, created_by, library_name, elm_xml, fhir_library_id, active,
            measure_class, category
        )
        SELECT
            gen_random_uuid(), '${DEMO_TENANT}', name, version, status, cql_content, elm_json, description, publisher,
            created_at, updated_at, created_by, library_name, elm_xml, fhir_library_id, active,
            measure_class, category
        FROM cql_libraries source
        WHERE source.tenant_id = 'demo-tenant'
        AND NOT EXISTS (
            SELECT 1 FROM cql_libraries target
            WHERE target.tenant_id = '${DEMO_TENANT}'
              AND target.name = source.name
              AND target.version = source.version
        );" > /dev/null 2>&1 || log_warning "CQL library seeding failed"
}

seed_operational_measure_data() {
    if [ -f "$FHIR_SEED_SCRIPT" ]; then
        log_info "Loading FHIR data for quality measures..."
        TENANT_ID="$DEMO_TENANT" USE_TRUSTED_HEADERS="$USE_TRUSTED_HEADERS" \
            AUTH_USER_ID="$AUTH_USER_ID" AUTH_USERNAME="$AUTH_USERNAME" AUTH_ROLES="$AUTH_ROLES" \
            bash "$FHIR_SEED_SCRIPT"
    else
        log_warning "FHIR seed script not found: $FHIR_SEED_SCRIPT"
    fi

    if [ -f "$PATIENT_SEED_SCRIPT" ]; then
        log_info "Loading patient demographics for patient service..."
        DB_CONTAINER_ID=$(docker-compose ps -q postgres || true)
        if [ -n "$DB_CONTAINER_ID" ]; then
            DB_CONTAINER="$DB_CONTAINER_ID" TENANT_ID="$DEMO_TENANT" \
                USE_TRUSTED_HEADERS="$USE_TRUSTED_HEADERS" \
                AUTH_USER_ID="$AUTH_USER_ID" AUTH_USERNAME="$AUTH_USERNAME" AUTH_ROLES="$AUTH_ROLES" \
                bash "$PATIENT_SEED_SCRIPT"
        else
            TENANT_ID="$DEMO_TENANT" USE_TRUSTED_HEADERS="$USE_TRUSTED_HEADERS" \
                AUTH_USER_ID="$AUTH_USER_ID" AUTH_USERNAME="$AUTH_USERNAME" AUTH_ROLES="$AUTH_ROLES" \
                bash "$PATIENT_SEED_SCRIPT"
        fi
    else
        log_warning "Patient seed script not found: $PATIENT_SEED_SCRIPT"
    fi

    if [ -f "$CLINICAL_SEED_SCRIPT" ]; then
        log_info "Loading clinical quality measure data..."
        DB_CONTAINER_ID=$(docker-compose ps -q postgres || true)
        if [ -n "$DB_CONTAINER_ID" ]; then
            DB_CONTAINER="$DB_CONTAINER_ID" TENANT_ID="$DEMO_TENANT" bash "$CLINICAL_SEED_SCRIPT"
        else
            TENANT_ID="$DEMO_TENANT" bash "$CLINICAL_SEED_SCRIPT"
        fi
    else
        log_warning "Clinical seed script not found: $CLINICAL_SEED_SCRIPT"
    fi
}

seed_quality_measures_and_operational_data() {
    seed_quality_measure_definitions
    seed_cql_libraries_for_tenant
    seed_operational_measure_data
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
