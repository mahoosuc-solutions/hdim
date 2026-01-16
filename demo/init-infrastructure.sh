#!/bin/bash
# HDIM Demo - Infrastructure Initialization
# This script initializes all infrastructure components after Docker build
# Runs as a one-time service after all containers are built

set -e

# Configuration
POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_USER="${POSTGRES_USER:-healthdata}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-demo_password_123}"
REDIS_HOST="${REDIS_HOST:-redis}"
REDIS_PORT="${REDIS_PORT:-6379}"
KAFKA_HOST="${KAFKA_HOST:-kafka}"
KAFKA_PORT="${KAFKA_PORT:-9092}"
ELASTICSEARCH_HOST="${ELASTICSEARCH_HOST:-elasticsearch}"
ELASTICSEARCH_PORT="${ELASTICSEARCH_PORT:-9200}"

# Service URLs
GATEWAY_URL="${GATEWAY_URL:-http://gateway-service:8080}"
FHIR_URL="${FHIR_URL:-http://fhir-service:8085}"
PATIENT_URL="${PATIENT_URL:-http://patient-service:8084}"
CARE_GAP_URL="${CARE_GAP_URL:-http://care-gap-service:8086}"
QUALITY_MEASURE_URL="${QUALITY_MEASURE_URL:-http://quality-measure-service:8087}"
CQL_ENGINE_URL="${CQL_ENGINE_URL:-http://cql-engine-service:8081}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
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

log_section() {
    echo ""
    echo -e "${CYAN}========================================${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}========================================${NC}"
    echo ""
}

# Wait for service to be ready
wait_for_service() {
    local service_name=$1
    local check_command=$2
    local max_attempts=${3:-30}
    local attempt=1

    log_info "Waiting for $service_name to be ready..."
    
    while [ $attempt -le $max_attempts ]; do
        if eval "$check_command" > /dev/null 2>&1; then
            log_success "$service_name is ready!"
            return 0
        fi
        
        if [ $attempt -eq $max_attempts ]; then
            log_error "$service_name failed to become ready after $max_attempts attempts"
            return 1
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
}

# Wait for PostgreSQL
wait_for_postgres() {
    wait_for_service "PostgreSQL" \
        "PGPASSWORD=$POSTGRES_PASSWORD psql -h $POSTGRES_HOST -p $POSTGRES_PORT -U $POSTGRES_USER -d postgres -c 'SELECT 1'"
}

# Wait for Redis
wait_for_redis() {
    wait_for_service "Redis" \
        "redis-cli -h $REDIS_HOST -p $REDIS_PORT ping"
}

# Wait for Kafka
wait_for_kafka() {
    wait_for_service "Kafka" \
        "nc -z $KAFKA_HOST $KAFKA_PORT"
}

# Wait for Elasticsearch
wait_for_elasticsearch() {
    wait_for_service "Elasticsearch" \
        "curl -s http://$ELASTICSEARCH_HOST:$ELASTICSEARCH_PORT/_cluster/health | grep -q 'green\\|yellow'"
}

# Wait for service health endpoint
wait_for_service_health() {
    local service_name=$1
    local health_url=$2
    local max_attempts=${3:-60}
    local attempt=1

    log_info "Waiting for $service_name service to be healthy..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -sf "$health_url/actuator/health" > /dev/null 2>&1; then
            log_success "$service_name service is healthy!"
            return 0
        fi
        
        if [ $attempt -eq $max_attempts ]; then
            log_error "$service_name service failed to become healthy after $max_attempts attempts"
            return 1
        fi
        
        echo -n "."
        sleep 3
        attempt=$((attempt + 1))
    done
}

# Initialize databases
init_databases() {
    log_section "Initializing Databases"
    
    log_info "Creating service databases..."
    
    PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d postgres <<-EOSQL
        -- Create databases if they don't exist
        SELECT 'CREATE DATABASE gateway_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'gateway_db')\gexec
        SELECT 'CREATE DATABASE cql_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cql_db')\gexec
        SELECT 'CREATE DATABASE patient_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'patient_db')\gexec
        SELECT 'CREATE DATABASE fhir_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'fhir_db')\gexec
        SELECT 'CREATE DATABASE caregap_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'caregap_db')\gexec
        SELECT 'CREATE DATABASE quality_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'quality_db')\gexec
        SELECT 'CREATE DATABASE healthdata_demo' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'healthdata_demo')\gexec

        -- Grant privileges
        GRANT ALL PRIVILEGES ON DATABASE gateway_db TO "$POSTGRES_USER";
        GRANT ALL PRIVILEGES ON DATABASE cql_db TO "$POSTGRES_USER";
        GRANT ALL PRIVILEGES ON DATABASE patient_db TO "$POSTGRES_USER";
        GRANT ALL PRIVILEGES ON DATABASE fhir_db TO "$POSTGRES_USER";
        GRANT ALL PRIVILEGES ON DATABASE caregap_db TO "$POSTGRES_USER";
        GRANT ALL PRIVILEGES ON DATABASE quality_db TO "$POSTGRES_USER";
        GRANT ALL PRIVILEGES ON DATABASE healthdata_demo TO "$POSTGRES_USER";
EOSQL

    log_success "Databases created"

    # Enable extensions on each database
    log_info "Enabling PostgreSQL extensions..."
    
    for db in gateway_db cql_db patient_db fhir_db caregap_db quality_db healthdata_demo; do
        log_info "  - Configuring $db..."
        PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$db" <<-EOSQL
            CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
            CREATE EXTENSION IF NOT EXISTS pg_trgm;
EOSQL
    done

    log_success "Extensions enabled on all databases"
}

# Wait for service schemas to be created
wait_for_service_schemas() {
    log_section "Waiting for Service Schemas"
    
    # Wait for gateway service to create users table
    log_info "Waiting for gateway service schema..."
    local attempt=1
    while [ $attempt -le 60 ]; do
        if PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d gateway_db -c "\d users" > /dev/null 2>&1; then
            log_success "Gateway service schema ready"
            break
        fi
        if [ $attempt -eq 60 ]; then
            log_error "Gateway service schema not ready after 60 attempts"
            return 1
        fi
        sleep 3
        attempt=$((attempt + 1))
    done
}

# Initialize demo users
init_demo_users() {
    log_section "Initializing Demo Users"
    
    # BCrypt hash for password "demo123"
    DEMO_PASSWORD_HASH='$2a$10$ZMMI78ekTFEdm4fZqaKS.OFWaa.kLBFFydORaLLhUq4LLGtTKE96S'
    
    log_info "Creating demo users..."
    
    PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d gateway_db <<-EOSQL
        -- Insert demo users
        ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS user_roles_role_check;
        ALTER TABLE user_roles ADD CONSTRAINT user_roles_role_check
            CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'MEASURE_DEVELOPER', 'EVALUATOR', 'ANALYST', 'VIEWER'));

        INSERT INTO users (
            id, username, email, password_hash, first_name, last_name,
            active, email_verified, mfa_enabled, failed_login_attempts,
            created_at, updated_at
        ) VALUES
        (
            '550e8400-e29b-41d4-a716-446655440010'::uuid,
            'demo_admin', 'demo_admin@hdim.ai', '$DEMO_PASSWORD_HASH',
            'Demo', 'Admin', true, true, false, 0, NOW(), NOW()
        ),
        (
            '550e8400-e29b-41d4-a716-446655440011'::uuid,
            'demo_analyst', 'demo_analyst@hdim.ai', '$DEMO_PASSWORD_HASH',
            'Demo', 'Analyst', true, true, false, 0, NOW(), NOW()
        ),
        (
            '550e8400-e29b-41d4-a716-446655440012'::uuid,
            'demo_viewer', 'demo_viewer@hdim.ai', '$DEMO_PASSWORD_HASH',
            'Demo', 'Viewer', true, true, false, 0, NOW(), NOW()
        ),
        (
            '550e8400-e29b-41d4-a716-446655440013'::uuid,
            'demo_user', 'demo_user@hdim.ai', '$DEMO_PASSWORD_HASH',
            'Demo', 'User', true, true, false, 0, NOW(), NOW()
        ),
        (
            '550e8400-e29b-41d4-a716-446655440014'::uuid,
            'demo.developer', 'demo.developer@hdim.ai', '$DEMO_PASSWORD_HASH',
            'Demo', 'Developer', true, true, false, 0, NOW(), NOW()
        )
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            password_hash = EXCLUDED.password_hash,
            first_name = EXCLUDED.first_name,
            last_name = EXCLUDED.last_name,
            active = EXCLUDED.active,
            email_verified = EXCLUDED.email_verified,
            updated_at = NOW();

        -- Create user roles
        INSERT INTO user_roles (user_id, role) VALUES
        ('550e8400-e29b-41d4-a716-446655440010'::uuid, 'ADMIN'),
        ('550e8400-e29b-41d4-a716-446655440010'::uuid, 'EVALUATOR'),
        ('550e8400-e29b-41d4-a716-446655440011'::uuid, 'ANALYST'),
        ('550e8400-e29b-41d4-a716-446655440011'::uuid, 'EVALUATOR'),
        ('550e8400-e29b-41d4-a716-446655440012'::uuid, 'VIEWER'),
        ('550e8400-e29b-41d4-a716-446655440013'::uuid, 'VIEWER'),
        ('550e8400-e29b-41d4-a716-446655440014'::uuid, 'MEASURE_DEVELOPER'),
        ('550e8400-e29b-41d4-a716-446655440014'::uuid, 'EVALUATOR')
        ON CONFLICT DO NOTHING;

        -- Create user tenants
        INSERT INTO user_tenants (user_id, tenant_id) VALUES
        ('550e8400-e29b-41d4-a716-446655440010'::uuid, 'acme-health'),
        ('550e8400-e29b-41d4-a716-446655440011'::uuid, 'acme-health'),
        ('550e8400-e29b-41d4-a716-446655440012'::uuid, 'acme-health'),
        ('550e8400-e29b-41d4-a716-446655440013'::uuid, 'acme-health'),
        ('550e8400-e29b-41d4-a716-446655440014'::uuid, 'acme-health')
        ON CONFLICT DO NOTHING;
EOSQL

    log_success "Demo users created"
    log_info "  - demo_admin / demo123 (ADMIN, EVALUATOR)"
    log_info "  - demo_analyst / demo123 (ANALYST, EVALUATOR)"
    log_info "  - demo_viewer / demo123 (VIEWER)"
    log_info "  - demo_user / demo123 (VIEWER)"
    log_info "  - demo.developer / demo123 (MEASURE_DEVELOPER, EVALUATOR)"
}

# Verify infrastructure
verify_infrastructure() {
    log_section "Verifying Infrastructure"
    
    log_info "Checking database connectivity..."
    PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d gateway_db -c "SELECT COUNT(*) FROM users;" > /dev/null
    log_success "Database connectivity verified"
    
    log_info "Checking Redis connectivity..."
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping > /dev/null
    log_success "Redis connectivity verified"
    
    log_info "Checking Kafka connectivity..."
    nc -z "$KAFKA_HOST" "$KAFKA_PORT" > /dev/null
    log_success "Kafka connectivity verified"
    
    log_info "Checking Elasticsearch connectivity..."
    curl -s "http://$ELASTICSEARCH_HOST:$ELASTICSEARCH_PORT/_cluster/health" > /dev/null
    log_success "Elasticsearch connectivity verified"
}

# Main execution
main() {
    log_section "HDIM Demo Infrastructure Initialization"
    echo "Starting infrastructure initialization..."
    echo ""
    
    # Step 1: Wait for infrastructure services
    log_section "Step 1: Waiting for Infrastructure Services"
    wait_for_postgres || exit 1
    wait_for_redis || exit 1
    wait_for_kafka || exit 1
    wait_for_elasticsearch || exit 1
    
    # Step 2: Initialize databases
    log_section "Step 2: Initializing Databases"
    init_databases || exit 1
    
    # Step 3: Wait for application services
    log_section "Step 3: Waiting for Application Services"
    wait_for_service_health "Gateway" "$GATEWAY_URL" || exit 1
    wait_for_service_health "FHIR" "$FHIR_URL" || exit 1
    wait_for_service_health "Patient" "$PATIENT_URL" || exit 1
    wait_for_service_health "Care Gap" "$CARE_GAP_URL" || exit 1
    wait_for_service_health "Quality Measure" "$QUALITY_MEASURE_URL" || exit 1
    wait_for_service_health "CQL Engine" "$CQL_ENGINE_URL" || exit 1
    
    # Step 4: Wait for schemas
    log_section "Step 4: Waiting for Service Schemas"
    wait_for_service_schemas || exit 1
    
    # Step 5: Initialize users
    log_section "Step 5: Initializing Demo Users"
    init_demo_users || exit 1
    
    # Step 6: Verify infrastructure
    log_section "Step 6: Verifying Infrastructure"
    verify_infrastructure || exit 1
    
    # Final summary
    log_section "Initialization Complete"
    log_success "All infrastructure components initialized successfully!"
    echo ""
    echo "Demo Environment Ready:"
    echo "  - Portal: http://localhost:4200"
    echo "  - Gateway: http://localhost:8080"
    echo "  - Login: demo_admin / demo123"
    echo ""
    echo "Next step: Run ./seed-demo-data.sh to seed patient and care gap data"
    echo ""
}

# Run main function
main "$@"
