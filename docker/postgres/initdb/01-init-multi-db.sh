#!/bin/bash
# HDIM Healthcare Platform - Database Initialization Script
# Creates databases required by the platform services in the demo compose stack.
#
# Which databases are needed is driven by which services are defined in
# docker-compose.demo.yml.  If you add a service that needs a new database,
# add it here too.

set -e

echo "Creating HDIM databases..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- ── Platform Core ──────────────────────────────────────────────────────────
    CREATE DATABASE gateway_db;       -- gateway-admin / gateway-fhir / gateway-clinical
    CREATE DATABASE fhir_db;          -- fhir-service
    CREATE DATABASE cql_db;           -- cql-engine-service
    CREATE DATABASE quality_db;       -- quality-measure-service
    CREATE DATABASE patient_db;       -- patient-service
    CREATE DATABASE caregap_db;       -- care-gap-service
    CREATE DATABASE event_db;         -- event-processing-service
    CREATE DATABASE audit_db;         -- audit-query-service
    CREATE DATABASE hcc_db;           -- hcc-service
    CREATE DATABASE healthdata_demo;  -- demo-seeding-service

    -- ── Grant Privileges ───────────────────────────────────────────────────────
    GRANT ALL PRIVILEGES ON DATABASE gateway_db       TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE fhir_db          TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE cql_db           TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE quality_db       TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE patient_db       TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE caregap_db       TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE event_db         TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE audit_db         TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE hcc_db           TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE healthdata_demo  TO "$POSTGRES_USER";
EOSQL

# Note: PostgreSQL extensions are managed by service Liquibase migrations
# - fhir-service manages pg_trgm in fhir_db
# - cql-engine-service manages pg_trgm in cql_db
# - quality-measure-service manages pg_trgm in quality_db
# - patient-service manages pg_trgm in patient_db
# See: backend/modules/services/*/src/main/resources/db/changelog/0000-enable-extensions.xml

echo "All HDIM databases created successfully! (10 databases)"
