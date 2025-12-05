#!/bin/bash

# Run all Liquibase migrations for Health Data In Motion services
# This script runs each service briefly to execute Liquibase migrations on startup

set -e

echo "======================================"
echo "  Running Database Migrations"
echo "======================================"
echo ""

# Database connection details
export DB_HOST=localhost
export DB_PORT=5435
export DB_USER=healthdata
export DB_PASSWORD=dev_password

# Services to migrate (in order)
services=(
    "fhir-service"
    "cql-engine-service"
    "consent-service"
    "event-processing-service"
    "patient-service"
    "care-gap-service"
    "analytics-service"
    "quality-measure-service"
)

echo "Starting migration process for ${#services[@]} services..."
echo ""

for service in "${services[@]}"; do
    echo "----------------------------------------"
    echo "Running migrations for: $service"
    echo "----------------------------------------"

    # Run service in background, wait for migrations, then stop
    ./gradlew :modules:services:$service:bootRun \
        -Dspring.liquibase.enabled=true \
        -Dserver.port=0 \
        --args='--spring.main.web-environment=false' \
        > "/tmp/${service}-migration.log" 2>&1 &

    SERVICE_PID=$!

    # Wait for Liquibase to complete (check logs)
    echo "Waiting for Liquibase migrations to complete..."
    timeout=60
    elapsed=0

    while [ $elapsed -lt $timeout ]; do
        if grep -q "Liquibase: Update has been successful" "/tmp/${service}-migration.log" 2>/dev/null; then
            echo "✓ Migrations completed successfully for $service"
            kill $SERVICE_PID 2>/dev/null || true
            wait $SERVICE_PID 2>/dev/null || true
            break
        fi

        if ! ps -p $SERVICE_PID > /dev/null 2>&1; then
            if grep -q "Error" "/tmp/${service}-migration.log" 2>/dev/null; then
                echo "✗ Migration failed for $service. Check /tmp/${service}-migration.log"
                exit 1
            else
                echo "✓ Service completed for $service"
                break
            fi
        fi

        sleep 2
        elapsed=$((elapsed + 2))
    done

    if [ $elapsed -ge $timeout ]; then
        echo "⚠ Timeout waiting for $service migrations"
        kill $SERVICE_PID 2>/dev/null || true
        wait $SERVICE_PID 2>/dev/null || true
    fi

    echo ""
done

echo "======================================"
echo "  Migration Summary"
echo "======================================"
echo ""
echo "Migrations executed for all services."
echo "Check individual logs in /tmp/*-migration.log if needed."
echo ""
echo "To verify tables were created, run:"
echo "  docker exec -it healthdata-postgres psql -U healthdata -d healthdata_[service_name] -c '\\dt'"
echo ""
