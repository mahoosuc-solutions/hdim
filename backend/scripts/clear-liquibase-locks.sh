#!/bin/bash

###############################################################################
# clear-liquibase-locks.sh
# Emergency Recovery Script for Orphaned Liquibase Locks
#
# Purpose: Clear stuck Liquibase migration locks that cause services to hang
# Use Case: When a service crashes during migration and leaves databasechangeloglock
#           in locked state, preventing other migrations from running
#
# WARNING: Only use when you are CERTAIN the migration process has completely died
#          Using while a migration is in progress will cause database corruption
#
# Usage: ./clear-liquibase-locks.sh [service_name] [database_name]
# Examples:
#   ./clear-liquibase-locks.sh  # Clear all service locks
#   ./clear-liquibase-locks.sh consent-service consent_db
###############################################################################

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5435}"
POSTGRES_USER="${POSTGRES_USER:-healthdata}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-healthdata_password}"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# List of all service databases (from docker-compose.yml and init-multi-db.sh)
declare -A SERVICE_DATABASES=(
    ["consent-service"]="consent_db"
    ["event-processing-service"]="event_processing_db"
    ["care-gap-service"]="care_gap_db"
    ["patient-service"]="patient_db"
    ["quality-measure-service"]="quality_db"
    ["cql-engine-service"]="cql_db"
    ["fhir-service"]="fhir_db"
    ["gateway-admin-service"]="gateway_db"
    ["gateway-fhir-service"]="gateway_db"
    ["gateway-clinical-service"]="gateway_db"
    ["agent-runtime-service"]="agent_runtime_db"
    ["ai-assistant-service"]="ai_assistant_db"
    ["analytics-service"]="analytics_db"
    ["predictive-analytics-service"]="predictive_analytics_db"
    ["sdoh-service"]="sdoh_db"
    ["data-enrichment-service"]="data_enrichment_db"
    ["event-router-service"]="event_router_db"
    ["cdr-processor-service"]="cdr_processor_db"
    ["approval-service"]="approval_db"
    ["payer-workflows-service"]="payer_workflows_db"
    ["migration-workflow-service"]="migration_workflow_db"
    ["ehr-connector-service"]="ehr_connector_db"
    ["ecr-service"]="ecr_db"
    ["qrda-export-service"]="qrda_export_db"
    ["hcc-service"]="hcc_db"
    ["prior-auth-service"]="prior_auth_db"
    ["documentation-service"]="documentation_db"
    ["patient-event-service"]="patient_db"
    ["quality-measure-event-service"]="quality_db"
    ["care-gap-event-service"]="care_gap_db"
    ["clinical-workflow-event-service"]="clinical_workflow_db"
)

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

# Function to check if PostgreSQL is running
check_postgres() {
    print_status "Checking PostgreSQL connection..."

    if ! PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "postgres" -c "SELECT 1" >/dev/null 2>&1; then
        print_error "Cannot connect to PostgreSQL at $POSTGRES_HOST:$POSTGRES_PORT"
        print_status "Make sure PostgreSQL is running: docker compose up -d postgres"
        exit 1
    fi

    print_success "PostgreSQL is accessible"
}

# Function to clear lock for a single database
clear_lock_for_database() {
    local db_name=$1

    print_status "Checking lock status for database: $db_name"

    # Check if database exists
    if ! PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "postgres" -tc "SELECT 1 FROM pg_database WHERE datname='$db_name'" | grep -q 1; then
        print_warning "Database $db_name does not exist, skipping..."
        return 0
    fi

    # Check if databasechangeloglock table exists
    if ! PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$db_name" -tc "SELECT 1 FROM information_schema.tables WHERE table_name='databasechangeloglock'" | grep -q 1; then
        print_warning "Liquibase lock table does not exist in $db_name (schema not yet initialized)"
        return 0
    fi

    # Check current lock status
    local lock_status
    lock_status=$(PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$db_name" -tc "SELECT LOCKED FROM databasechangeloglock LIMIT 1" | tr -d ' ')

    if [ -z "$lock_status" ]; then
        print_warning "databasechangeloglock table exists but is empty in $db_name"
        return 0
    fi

    if [ "$lock_status" = "f" ]; then
        print_success "Database $db_name is NOT locked (status: unlocked)"
        return 0
    fi

    # Lock is set to TRUE, clear it
    print_warning "Database $db_name IS LOCKED! Clearing the lock..."
    print_warning "⚠ IMPORTANT: Only proceed if you are CERTAIN no migration is currently running"

    # Ask for confirmation
    read -p "Clear the lock for $db_name? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        print_status "Skipped clearing lock for $db_name"
        return 0
    fi

    # Execute the clear operation
    PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$db_name" << EOF
        UPDATE databasechangeloglock SET LOCKED=false WHERE ID=1;
        SELECT 'Lock cleared for ' || '$db_name' AS result;
EOF

    print_success "Lock cleared for database $db_name"
}

# Function to display lock status for a single database
show_lock_status_for_database() {
    local db_name=$1

    # Check if database exists
    if ! PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "postgres" -tc "SELECT 1 FROM pg_database WHERE datname='$db_name'" | grep -q 1; then
        return 0
    fi

    # Check if databasechangeloglock table exists
    if ! PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$db_name" -tc "SELECT 1 FROM information_schema.tables WHERE table_name='databasechangeloglock'" | grep -q 1; then
        return 0
    fi

    # Show lock status
    local lock_status
    lock_status=$(PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$db_name" -tc "SELECT LOCKED FROM databasechangeloglock LIMIT 1" | tr -d ' ')

    if [ "$lock_status" = "t" ]; then
        print_warning "$db_name: LOCKED"
    elif [ "$lock_status" = "f" ]; then
        print_success "$db_name: unlocked"
    fi
}

# Main execution
main() {
    echo -e "\n${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}Liquibase Emergency Lock Recovery Script${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}\n"

    # Check PostgreSQL connection
    check_postgres

    if [ $# -eq 2 ]; then
        # Clear lock for specific service/database
        local service_name=$1
        local db_name=$2

        print_status "Target: Service=$service_name, Database=$db_name"
        clear_lock_for_database "$db_name"

    elif [ $# -eq 0 ]; then
        # Show status for all databases
        print_status "Scanning all service databases for locked migrations...\n"

        local locked_count=0
        for service in "${!SERVICE_DATABASES[@]}"; do
            db="${SERVICE_DATABASES[$service]}"
            lock_status=$(PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "postgres" -tc "SELECT CASE WHEN EXISTS(SELECT 1 FROM pg_database WHERE datname='$db') THEN (SELECT COALESCE(LOCKED, false) FROM ${db}.databasechangeloglock LIMIT 1) ELSE false END" 2>/dev/null | tr -d ' ')

            if [ "$lock_status" = "t" ]; then
                ((locked_count++))
                echo -e "${RED}✗${NC} $service ($db): LOCKED"
            else
                echo -e "${GREEN}✓${NC} $service ($db): unlocked"
            fi
        done

        echo ""
        if [ $locked_count -gt 0 ]; then
            print_warning "Found $locked_count locked databases"
            echo -e "\n${YELLOW}To clear a specific lock, run:${NC}"
            echo "  ./clear-liquibase-locks.sh <service-name> <database-name>"
            echo -e "\n${YELLOW}Example:${NC}"
            echo "  ./clear-liquibase-locks.sh consent-service consent_db"
        else
            print_success "All databases are unlocked!"
        fi

    else
        print_error "Invalid arguments"
        echo ""
        echo "Usage:"
        echo "  $0                              # Show status for all services"
        echo "  $0 <service-name> <db-name>    # Clear lock for specific service"
        echo ""
        echo "Examples:"
        echo "  $0"
        echo "  $0 consent-service consent_db"
        exit 1
    fi

    echo -e "\n${BLUE}═══════════════════════════════════════════════════════════${NC}\n"
}

main "$@"
