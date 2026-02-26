-- HDIM Authentication Schema Initialization
-- Creates the users table and related tables required by the authentication system
-- This script runs BEFORE demo-users.sql to ensure tables exist
--
-- Note: This script needs to be run against the gateway_db database
-- The initialization system connects to each database and runs this script
\connect gateway_db

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

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(active);

-- Create user_roles table (for storing user roles)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);

-- Create user_tenants table (for multi-tenant support)
CREATE TABLE IF NOT EXISTS user_tenants (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_user_tenants_user_id ON user_tenants(user_id);
CREATE INDEX IF NOT EXISTS idx_user_tenants_tenant_id ON user_tenants(tenant_id);

-- Create audit_logs table (for authentication audit trail)
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

-- Create refresh_tokens table (for JWT refresh token tracking)
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

-- Create tenant_service_config_versions table (for per-tenant configuration versioning)
CREATE TABLE IF NOT EXISTS tenant_service_config_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(120) NOT NULL,
    version_number INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    config_json JSONB NOT NULL,
    config_hash VARCHAR(64) NOT NULL,
    change_summary TEXT,
    source_version_id UUID,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tenant_service_config_versions UNIQUE (tenant_id, service_name, version_number)
);

CREATE INDEX IF NOT EXISTS idx_config_versions_tenant_service
    ON tenant_service_config_versions(tenant_id, service_name);

-- Current active config per tenant/service
CREATE TABLE IF NOT EXISTS tenant_service_config_current (
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(120) NOT NULL,
    active_version_id UUID NOT NULL REFERENCES tenant_service_config_versions(id),
    updated_by VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (tenant_id, service_name)
);

-- Configuration audit log
CREATE TABLE IF NOT EXISTS tenant_service_config_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(120) NOT NULL,
    version_id UUID NOT NULL REFERENCES tenant_service_config_versions(id),
    action VARCHAR(30) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    details JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_config_audit_tenant_service
    ON tenant_service_config_audit(tenant_id, service_name);

CREATE INDEX IF NOT EXISTS idx_config_audit_version
    ON tenant_service_config_audit(version_id);

-- Configuration approvals (two-person gate)
CREATE TABLE IF NOT EXISTS tenant_service_config_approvals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(120) NOT NULL,
    version_id UUID NOT NULL REFERENCES tenant_service_config_versions(id),
    action VARCHAR(30) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_config_approvals_tenant_service
    ON tenant_service_config_approvals(tenant_id, service_name);

CREATE INDEX IF NOT EXISTS idx_config_approvals_version
    ON tenant_service_config_approvals(version_id);


-- Operational run tracking
CREATE TABLE IF NOT EXISTS operation_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operation_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    parameters_json JSONB,
    requested_by VARCHAR(255) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    summary TEXT,
    exit_code INT,
    log_output TEXT,
    idempotency_key VARCHAR(128),
    cancel_requested BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_operation_runs_requested_at
    ON operation_runs(requested_at DESC);

CREATE INDEX IF NOT EXISTS idx_operation_runs_status
    ON operation_runs(status);

CREATE UNIQUE INDEX IF NOT EXISTS uq_operation_runs_type_idempotency
    ON operation_runs(operation_type, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS operation_run_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL REFERENCES operation_runs(id) ON DELETE CASCADE,
    step_order INT NOT NULL,
    step_name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    command_text TEXT,
    message TEXT,
    output TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_operation_run_steps_run
    ON operation_run_steps(run_id, step_order);

-- Schema creation complete
