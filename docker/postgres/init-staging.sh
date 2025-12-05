#!/bin/bash
set -e

# HealthData-in-Motion - PostgreSQL Database Initialization for Staging
# Creates all databases required for the event-driven platform
# Version: 2.0.0

echo "=================================================="
echo "HealthData-in-Motion - Database Initialization"
echo "Environment: STAGING"
echo "Timestamp: $(date)"
echo "=================================================="

# Create databases for all services
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    -- CQL Engine Service Database
    CREATE DATABASE healthdata_cql;
    GRANT ALL PRIVILEGES ON DATABASE healthdata_cql TO healthdata;

    -- Quality Measure Service Database
    CREATE DATABASE healthdata_quality_measure;
    GRANT ALL PRIVILEGES ON DATABASE healthdata_quality_measure TO healthdata;

    -- FHIR Service Database
    CREATE DATABASE healthdata_fhir;
    GRANT ALL PRIVILEGES ON DATABASE healthdata_fhir TO healthdata;

    -- Patient Service Database
    CREATE DATABASE healthdata_patient;
    GRANT ALL PRIVILEGES ON DATABASE healthdata_patient TO healthdata;

    -- Care Gap Service Database
    CREATE DATABASE healthdata_care_gap;
    GRANT ALL PRIVILEGES ON DATABASE healthdata_care_gap TO healthdata;

    -- Event Router Service Database
    CREATE DATABASE healthdata_event_router;
    GRANT ALL PRIVILEGES ON DATABASE healthdata_event_router TO healthdata;

    -- Gateway Service Database
    CREATE DATABASE healthdata_gateway;
    GRANT ALL PRIVILEGES ON DATABASE healthdata_gateway TO healthdata;
EOSQL

echo ""
echo "✅ Databases created successfully:"
echo "   - healthdata_cql"
echo "   - healthdata_quality_measure"
echo "   - healthdata_fhir"
echo "   - healthdata_patient"
echo "   - healthdata_care_gap"
echo "   - healthdata_event_router"
echo "   - healthdata_gateway"
echo ""

# Enable required extensions on each database
for db in healthdata_cql healthdata_quality_measure healthdata_patient healthdata_care_gap healthdata_event_router healthdata_gateway; do
    echo "Enabling extensions on $db..."
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname="$db" <<-EOSQL
        CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
        CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
EOSQL
done

echo ""
echo "=================================================="
echo "✅ Database initialization complete!"
echo "=================================================="
