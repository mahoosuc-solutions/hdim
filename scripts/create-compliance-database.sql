-- Compliance Database Setup Script
-- ==================================
-- Creates the gateway_db database and compliance_errors table if they don't exist
-- Run this script to initialize the database before starting the service

-- Connect to PostgreSQL (default database)
\c postgres;

-- Create database if it doesn't exist
SELECT 'CREATE DATABASE gateway_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'gateway_db')\gexec

-- Connect to gateway_db
\c gateway_db;

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- For text search if needed

-- Create compliance_errors table (if not exists)
-- Note: This will be managed by Liquibase, but this script can be used for manual setup
CREATE TABLE IF NOT EXISTS compliance_errors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp TIMESTAMP NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    service VARCHAR(100) NOT NULL,
    endpoint VARCHAR(500),
    operation VARCHAR(500) NOT NULL,
    error_code VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    stack TEXT,
    additional_data JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_compliance_tenant_timestamp ON compliance_errors(tenant_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_compliance_severity ON compliance_errors(severity);
CREATE INDEX IF NOT EXISTS idx_compliance_service ON compliance_errors(service);
CREATE INDEX IF NOT EXISTS idx_compliance_timestamp ON compliance_errors(timestamp DESC);

-- Create GIN index for JSONB column (for efficient JSON queries)
CREATE INDEX IF NOT EXISTS idx_compliance_additional_data ON compliance_errors USING GIN (additional_data);

-- Grant permissions (adjust as needed for your setup)
-- GRANT ALL PRIVILEGES ON TABLE compliance_errors TO healthdata;
-- GRANT USAGE, SELECT ON SEQUENCE compliance_errors_id_seq TO healthdata;

-- Display table info
\d compliance_errors

-- Show indexes
\di compliance_errors*

-- Summary
SELECT 
    'Database setup complete' as status,
    COUNT(*) as existing_records
FROM compliance_errors;
