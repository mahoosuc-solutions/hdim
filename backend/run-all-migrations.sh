#!/bin/bash
set -e

cd /home/webemo-aaron/projects/healthdata-in-motion/backend

echo "=========================================="
echo "Running Database Migrations for All Services"
echo "=========================================="

# Services that need migrations (FHIR already done)
services=(
    "cql-engine-service"
    "consent-service"
    "event-processing-service"
    "patient-service"
    "care-gap-service"
    "analytics-service"
    "quality-measure-service"
)

for service in "${services[@]}"; do
    echo ""
    echo "==========================================  "
    echo "Migrating: $service"
    echo "=========================================="

    # Run service in background
    timeout 90 ./gradlew :modules:services:$service:bootRun 2>&1 | tee "/tmp/${service}-migration.log" | grep -iE "(liquibase|Running changeset|successfully applied)" &

    PID=$!

    # Wait for it to complete or timeout
    wait $PID || true

    # Check if migration was successful
    if grep -q "Liquibase: Update has been successful" "/tmp/${service}-migration.log"; then
        echo "✓ Migration successful for $service"
    elif grep -q "Running Changeset" "/tmp/${service}-migration.log"; then
        echo "✓ Migration executed for $service"
    else
        echo "⚠ Check log for $service: /tmp/${service}-migration.log"
    fi
done

echo ""
echo "=========================================="
echo "Migration process complete!"
echo "=========================================="
