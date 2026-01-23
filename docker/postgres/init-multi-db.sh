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
    CREATE DATABASE audit_db;

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
    CREATE DATABASE workflow_db;
    CREATE DATABASE approval_db;
    CREATE DATABASE payer_db;
    CREATE DATABASE migration_db;

    -- Sales & CRM Services
    CREATE DATABASE sales_automation_db;

    -- Integration Services
    CREATE DATABASE ehr_connector_db;

    -- Support Services
    CREATE DATABASE docs_db;
    CREATE DATABASE notification_db;

    -- Healthcare Services
    CREATE DATABASE hcc_db;
    CREATE DATABASE prior_auth_db;
    CREATE DATABASE qrda_db;
    CREATE DATABASE ecr_db;

    -- Demo Services
    CREATE DATABASE healthdata_demo;

    -- Load Testing Services (Separate Container)
    CREATE DATABASE data_ingestion_db;

    -- CQRS Event Projection Services (Read Models)
    CREATE DATABASE patient_event_db;
    CREATE DATABASE care_gap_event_db;
    CREATE DATABASE quality_event_db;
    CREATE DATABASE clinical_workflow_event_db;

    -- Event Store Service (Immutable Event Log)
    CREATE DATABASE event_store_db;

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
    GRANT ALL PRIVILEGES ON DATABASE audit_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE agent_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE agent_runtime_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE ai_assistant_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE analytics_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE predictive_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE sdoh_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE enrichment_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE cdr_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE workflow_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE approval_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE payer_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE migration_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE ehr_connector_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE docs_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE sales_automation_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE notification_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE hcc_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE prior_auth_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE qrda_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE ecr_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE healthdata_demo TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE data_ingestion_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE patient_event_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE care_gap_event_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE quality_event_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE clinical_workflow_event_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE event_store_db TO "$POSTGRES_USER";
EOSQL

# Note: PostgreSQL extensions are now managed by service Liquibase migrations
# - fhir-service manages pg_trgm in fhir_db
# - cql-engine-service manages pg_trgm in cql_db
# - quality-measure-service manages pg_trgm in quality_db
# - patient-service manages pg_trgm in patient_db
# See: backend/modules/services/*/src/main/resources/db/changelog/0000-enable-extensions.xml

# Note: Gateway authentication tables are now managed by gateway-service Liquibase migrations
# See: backend/modules/services/gateway-service/src/main/resources/db/changelog/

echo "All HDIM databases created successfully!"
