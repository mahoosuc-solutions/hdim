#!/bin/bash
# HDIM Healthcare Platform - Multi-Database Initialization Script
# This script creates all databases required by the 26 microservices

set -e

echo "Creating HDIM databases..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Core Clinical Services
    CREATE DATABASE fhir_db;
    CREATE DATABASE cql_db;
    CREATE DATABASE quality_db;
    CREATE DATABASE patient_db;
    CREATE DATABASE caregap_db;
    CREATE DATABASE consent_db;
    CREATE DATABASE event_db;
    CREATE DATABASE event_router_db;
    CREATE DATABASE gateway_db;

    -- AI Services
    CREATE DATABASE agent_db;
    CREATE DATABASE agent_runtime_db;
    CREATE DATABASE ai_assistant_db;

    -- Analytics Services
    CREATE DATABASE analytics_db;
    CREATE DATABASE predictive_db;
    CREATE DATABASE sdoh_db;

    -- Data Processing Services
    CREATE DATABASE enrichment_db;
    CREATE DATABASE cdr_db;

    -- Workflow Services
    CREATE DATABASE approval_db;
    CREATE DATABASE payer_db;
    CREATE DATABASE migration_db;

    -- Integration Services
    CREATE DATABASE ehr_connector_db;

    -- Support Services
    CREATE DATABASE docs_db;

    -- Grant privileges to postgres user
    GRANT ALL PRIVILEGES ON DATABASE fhir_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE cql_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE quality_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE patient_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE caregap_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE consent_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE event_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE event_router_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE gateway_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE agent_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE agent_runtime_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE ai_assistant_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE analytics_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE predictive_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE sdoh_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE enrichment_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE cdr_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE approval_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE payer_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE migration_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE ehr_connector_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE docs_db TO "$POSTGRES_USER";
EOSQL

echo "All HDIM databases created successfully!"
