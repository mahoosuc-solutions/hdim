#!/bin/bash

# Simple Liquibase migration runner using Docker Liquibase CLI
set -e

echo "======================================"
echo "  Running Liquibase Migrations"
echo "======================================"

# Database connection details
DB_HOST=localhost
DB_PORT=5435
DB_USER=healthdata
DB_PASSWORD=dev_password

# Function to run liquibase for a service
run_migration() {
    local service_name=$1
    local db_name=$2
    local changelog_path=$3

    echo ""
    echo "----------------------------------------"
    echo "Migrating: $service_name -> $db_name"
    echo "----------------------------------------"

    docker run --rm \
        --network host \
        -v "$(pwd)/$changelog_path:/liquibase/changelog" \
        liquibase/liquibase:4.29 \
        --url="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${db_name}" \
        --username="${DB_USER}" \
        --password="${DB_PASSWORD}" \
        --changelog-file="/liquibase/changelog/db.changelog-master.xml" \
        --log-level=INFO \
        update

    if [ $? -eq 0 ]; then
        echo "✓ Migration successful for $service_name"
    else
        echo "✗ Migration failed for $service_name"
        return 1
    fi
}

# Run migrations for each service
echo ""
echo "Starting migrations..."

run_migration "FHIR Service" "healthdata_fhir" \
    "modules/services/fhir-service/src/main/resources/db/changelog"

run_migration "CQL Engine Service" "healthdata_cql" \
    "modules/services/cql-engine-service/src/main/resources/db/changelog"

run_migration "Consent Service" "healthdata_consent" \
    "modules/services/consent-service/src/main/resources/db/changelog"

run_migration "Event Processing Service" "healthdata_events" \
    "modules/services/event-processing-service/src/main/resources/db/changelog"

run_migration "Patient Service" "healthdata_patient" \
    "modules/services/patient-service/src/main/resources/db/changelog"

run_migration "Care Gap Service" "healthdata_care_gap" \
    "modules/services/care-gap-service/src/main/resources/db/changelog"

run_migration "Analytics Service" "healthdata_analytics" \
    "modules/services/analytics-service/src/main/resources/db/changelog"

run_migration "Quality Measure Service" "healthdata_quality_measure" \
    "modules/services/quality-measure-service/src/main/resources/db/changelog"

run_migration "Audit Module" "healthdata_audit" \
    "modules/shared/infrastructure/audit/src/main/resources/db/changelog"

echo ""
echo "======================================"
echo "  ✓ All Migrations Complete"
echo "======================================"
echo ""
echo "To verify tables, run:"
echo "  docker exec healthdata-postgres psql -U healthdata -d [database_name] -c '\\dt'"
