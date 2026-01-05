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
    GRANT ALL PRIVILEGES ON DATABASE sales_automation_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE notification_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE hcc_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE prior_auth_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE qrda_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE ecr_db TO "$POSTGRES_USER";
    GRANT ALL PRIVILEGES ON DATABASE healthdata_demo TO "$POSTGRES_USER";
EOSQL

echo "Enabling PostgreSQL extensions..."

# Enable pg_trgm extension for databases that use GIN trigram indexes
for db in fhir_db cql_db quality_db patient_db; do
    echo "Enabling pg_trgm in $db..."
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$db" <<-EOSQL
        CREATE EXTENSION IF NOT EXISTS pg_trgm;
EOSQL
done

echo "Creating authentication schema in gateway_db..."

# Create authentication tables in gateway_db
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "gateway_db" <<-EOSQL
    -- Users table for authentication
    CREATE TABLE IF NOT EXISTS users (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        username VARCHAR(50) NOT NULL UNIQUE,
        email VARCHAR(100) NOT NULL UNIQUE,
        password_hash VARCHAR(255) NOT NULL,
        first_name VARCHAR(100) NOT NULL,
        last_name VARCHAR(100) NOT NULL,
        active BOOLEAN NOT NULL DEFAULT true,
        email_verified BOOLEAN NOT NULL DEFAULT false,
        mfa_enabled BOOLEAN NOT NULL DEFAULT false,
        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

    CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
    CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
    CREATE INDEX IF NOT EXISTS idx_users_active ON users(active);

    -- User roles table
    CREATE TABLE IF NOT EXISTS user_roles (
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        role VARCHAR(50) NOT NULL,
        PRIMARY KEY (user_id, role)
    );

    CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);

    -- User tenants table (multi-tenant support)
    CREATE TABLE IF NOT EXISTS user_tenants (
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        tenant_id VARCHAR(100) NOT NULL,
        PRIMARY KEY (user_id, tenant_id)
    );

    CREATE INDEX IF NOT EXISTS idx_user_tenants_user_id ON user_tenants(user_id);
    CREATE INDEX IF NOT EXISTS idx_user_tenants_tenant_id ON user_tenants(tenant_id);

    -- Audit logs table
    CREATE TABLE IF NOT EXISTS audit_logs (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID REFERENCES users(id) ON DELETE SET NULL,
        action VARCHAR(100) NOT NULL,
        entity_type VARCHAR(100),
        entity_id UUID,
        timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        ip_address VARCHAR(45),
        user_agent TEXT,
        details JSONB
    );

    CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
    CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp);

    -- Refresh tokens table
    CREATE TABLE IF NOT EXISTS refresh_tokens (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        token_hash VARCHAR(255) NOT NULL,
        expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        revoked BOOLEAN NOT NULL DEFAULT false,
        revoked_at TIMESTAMP WITH TIME ZONE
    );

    CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
    CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
EOSQL

echo "All HDIM databases created successfully!"
