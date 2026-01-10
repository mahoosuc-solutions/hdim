-- HDIM Healthcare Platform - External PostgreSQL Setup
-- Run this script as a superuser (postgres) on your external PostgreSQL instance
--
-- Usage:
--   psql -U postgres -f external-db-setup.sql
--
-- After running, set environment variables:
--   export POSTGRES_HOST=localhost
--   export POSTGRES_PORT=5432
--   export POSTGRES_USER=healthdata
--   export POSTGRES_PASSWORD=healthdata_password

-- Create the healthdata user
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'healthdata') THEN
        CREATE ROLE healthdata WITH LOGIN PASSWORD 'healthdata_password';
    END IF;
END
$$;

-- Grant necessary permissions
ALTER ROLE healthdata CREATEDB;

-- Create all required databases
CREATE DATABASE healthdata_db OWNER healthdata;
CREATE DATABASE fhir_db OWNER healthdata;
CREATE DATABASE cql_db OWNER healthdata;
CREATE DATABASE quality_db OWNER healthdata;
CREATE DATABASE patient_db OWNER healthdata;
CREATE DATABASE caregap_db OWNER healthdata;
CREATE DATABASE consent_db OWNER healthdata;
CREATE DATABASE event_db OWNER healthdata;
CREATE DATABASE event_router_db OWNER healthdata;
CREATE DATABASE gateway_db OWNER healthdata;
CREATE DATABASE agent_db OWNER healthdata;
CREATE DATABASE agent_runtime_db OWNER healthdata;
CREATE DATABASE ai_assistant_db OWNER healthdata;
CREATE DATABASE analytics_db OWNER healthdata;
CREATE DATABASE predictive_db OWNER healthdata;
CREATE DATABASE sdoh_db OWNER healthdata;
CREATE DATABASE enrichment_db OWNER healthdata;
CREATE DATABASE cdr_db OWNER healthdata;
CREATE DATABASE approval_db OWNER healthdata;
CREATE DATABASE payer_db OWNER healthdata;
CREATE DATABASE migration_db OWNER healthdata;
CREATE DATABASE ehr_connector_db OWNER healthdata;
CREATE DATABASE docs_db OWNER healthdata;
CREATE DATABASE sales_automation_db OWNER healthdata;
CREATE DATABASE notification_db OWNER healthdata;
CREATE DATABASE hcc_db OWNER healthdata;
CREATE DATABASE prior_auth_db OWNER healthdata;
CREATE DATABASE qrda_db OWNER healthdata;
CREATE DATABASE ecr_db OWNER healthdata;
CREATE DATABASE healthdata_demo OWNER healthdata;

-- Enable pg_trgm extension for databases that use GIN trigram indexes
\c fhir_db
CREATE EXTENSION IF NOT EXISTS pg_trgm;

\c cql_db
CREATE EXTENSION IF NOT EXISTS pg_trgm;

\c quality_db
CREATE EXTENSION IF NOT EXISTS pg_trgm;

\c patient_db
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Create authentication schema in gateway_db
\c gateway_db

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

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);

CREATE TABLE IF NOT EXISTS user_tenants (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_user_tenants_user_id ON user_tenants(user_id);
CREATE INDEX IF NOT EXISTS idx_user_tenants_tenant_id ON user_tenants(tenant_id);

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

\echo 'HDIM external database setup complete!'
\echo 'Databases created: 30'
\echo 'User: healthdata / healthdata_password'
