-- Phase 2 Week 2: Database Roles and Permissions
-- CMS Connector Service - RBAC implementation for multi-user access
-- Created: Phase 2 Week 2

-- ============================================================================
-- Create database roles for different user types
-- ============================================================================

-- Application service role (highest privileges for schema operations)
CREATE ROLE cms_service WITH LOGIN;
GRANT CONNECT ON DATABASE cms_production TO cms_service;

-- Read-only role for analytics and reporting
CREATE ROLE cms_analytics WITH NOLOGIN;

-- Audit-only role for compliance officers
CREATE ROLE cms_auditor WITH NOLOGIN;

-- Clinician/Data accessor role
CREATE ROLE cms_clinician WITH NOLOGIN;

-- Admin role for operations
CREATE ROLE cms_admin WITH NOLOGIN;

-- ============================================================================
-- Grant schema permissions
-- ============================================================================

-- Service role: full schema access (for migrations and operations)
GRANT USAGE ON SCHEMA public TO cms_service;
GRANT CREATE ON SCHEMA public TO cms_service;

-- Other roles: usage only
GRANT USAGE ON SCHEMA public TO cms_analytics, cms_auditor, cms_clinician, cms_admin;

-- ============================================================================
-- Grant table permissions - CMS_CLAIMS table
-- ============================================================================

-- Service role: full access
GRANT SELECT, INSERT, UPDATE, DELETE ON cms_claims TO cms_service;
GRANT USAGE, SELECT ON SEQUENCE cms_claims_id_seq TO cms_service;

-- Analytics role: read-only
GRANT SELECT ON cms_claims TO cms_analytics;

-- Auditor role: read-only for audit
GRANT SELECT ON cms_claims TO cms_auditor;

-- Clinician role: restricted select (only own tenant)
-- Row-level security will enforce tenant isolation
GRANT SELECT ON cms_claims TO cms_clinician;

-- Admin role: read and update status
GRANT SELECT, UPDATE ON cms_claims TO cms_admin;

-- ============================================================================
-- Grant table permissions - CLAIM_VALIDATION_ERRORS table
-- ============================================================================

GRANT SELECT, INSERT ON claim_validation_errors TO cms_service;
GRANT USAGE, SELECT ON SEQUENCE claim_validation_errors_id_seq TO cms_service;

GRANT SELECT ON claim_validation_errors TO cms_analytics;
GRANT SELECT ON claim_validation_errors TO cms_auditor;
GRANT SELECT ON claim_validation_errors TO cms_clinician;
GRANT SELECT, UPDATE ON claim_validation_errors TO cms_admin;

-- ============================================================================
-- Grant table permissions - SYNC_AUDIT_LOG table
-- ============================================================================

-- Service role: full access for sync operations
GRANT SELECT, INSERT, UPDATE ON sync_audit_log TO cms_service;
GRANT USAGE, SELECT ON SEQUENCE sync_audit_log_id_seq TO cms_service;

-- Analytics: read all sync operations
GRANT SELECT ON sync_audit_log TO cms_analytics;

-- Auditor: read all sync operations (compliance)
GRANT SELECT ON sync_audit_log TO cms_auditor;

-- Clinician: no direct access to sync logs
-- (will see aggregated results through views)

-- Admin: full access to sync logs
GRANT SELECT, UPDATE ON sync_audit_log TO cms_admin;

-- ============================================================================
-- Grant table permissions - SYNC_STATUS table
-- ============================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON sync_status TO cms_service;
GRANT USAGE, SELECT ON SEQUENCE sync_status_id_seq TO cms_service;

GRANT SELECT ON sync_status TO cms_analytics;
GRANT SELECT ON sync_status TO cms_auditor;
GRANT SELECT ON sync_status TO cms_admin;

-- ============================================================================
-- Grant table permissions - TENANT_CONFIG table
-- ============================================================================

GRANT SELECT, INSERT, UPDATE ON tenant_config TO cms_service;
GRANT USAGE, SELECT ON SEQUENCE tenant_config_id_seq TO cms_service;

GRANT SELECT ON tenant_config TO cms_analytics;
GRANT SELECT ON tenant_config TO cms_auditor;
GRANT SELECT ON tenant_config TO cms_clinician;
GRANT SELECT, UPDATE ON tenant_config TO cms_admin;

-- ============================================================================
-- Grant table permissions - IMPORT_SESSION tables
-- ============================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON import_session TO cms_service;
GRANT USAGE, SELECT ON SEQUENCE import_session_id_seq TO cms_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON import_session_file TO cms_service;
GRANT USAGE, SELECT ON SEQUENCE import_session_file_id_seq TO cms_service;

GRANT SELECT ON import_session TO cms_analytics;
GRANT SELECT ON import_session_file TO cms_analytics;

GRANT SELECT ON import_session TO cms_auditor;
GRANT SELECT ON import_session_file TO cms_auditor;

GRANT SELECT ON import_session TO cms_admin;
GRANT SELECT ON import_session_file TO cms_admin;

-- ============================================================================
-- Grant table permissions - AUDIT_LOG_ACCESS table (HIPAA compliance)
-- ============================================================================

GRANT SELECT, INSERT ON audit_log_access TO cms_service;
GRANT SELECT, INSERT ON audit_log_access TO cms_auditor;
GRANT USAGE, SELECT ON SEQUENCE audit_log_access_id_seq TO cms_service;

-- ============================================================================
-- Grant view permissions
-- ============================================================================

-- claim_validation_summary view
GRANT SELECT ON claim_validation_summary TO cms_analytics;
GRANT SELECT ON claim_validation_summary TO cms_auditor;
GRANT SELECT ON claim_validation_summary TO cms_clinician;
GRANT SELECT ON claim_validation_summary TO cms_admin;

-- sync_operation_metrics view
GRANT SELECT ON sync_operation_metrics TO cms_analytics;
GRANT SELECT ON sync_operation_metrics TO cms_auditor;
GRANT SELECT ON sync_operation_metrics TO cms_admin;

-- tenant_import_activity view
GRANT SELECT ON tenant_import_activity TO cms_analytics;
GRANT SELECT ON tenant_import_activity TO cms_auditor;
GRANT SELECT ON tenant_import_activity TO cms_admin;

-- claim_metrics_by_source_daily materialized view
GRANT SELECT ON claim_metrics_by_source_daily TO cms_analytics;
GRANT SELECT ON claim_metrics_by_source_daily TO cms_auditor;

-- ============================================================================
-- Grant function and procedure permissions
-- ============================================================================

GRANT EXECUTE ON FUNCTION update_updated_at_column TO cms_service;
GRANT EXECUTE ON FUNCTION cleanup_old_claims TO cms_admin;

-- ============================================================================
-- Create Row-Level Security (RLS) policies for tenant isolation
-- ============================================================================

-- Enable RLS on cms_claims
ALTER TABLE cms_claims ENABLE ROW LEVEL SECURITY;

-- Policy: clinicians can only see claims for their tenant
CREATE POLICY cms_claims_tenant_isolation ON cms_claims
  FOR ALL
  TO cms_clinician
  USING (tenant_id = current_setting('app.current_tenant_id')::UUID);

-- Policy: clinicians are read-only
CREATE POLICY cms_claims_clinician_readonly ON cms_claims
  FOR UPDATE
  TO cms_clinician
  USING (false);

-- Policy: analytics can see all claims (for reporting)
CREATE POLICY cms_claims_analytics_all ON cms_claims
  FOR SELECT
  TO cms_analytics
  USING (true);

-- Policy: service role bypasses RLS for operational tasks
ALTER ROLE cms_service BYPASSRLS;

-- Enable RLS on claim_validation_errors
ALTER TABLE claim_validation_errors ENABLE ROW LEVEL SECURITY;

CREATE POLICY claim_validation_errors_tenant_isolation ON claim_validation_errors
  FOR ALL
  TO cms_clinician
  USING (tenant_id = current_setting('app.current_tenant_id')::UUID);

-- ============================================================================
-- Create audit trigger for tracking data access
-- ============================================================================

CREATE TABLE IF NOT EXISTS data_access_audit (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

  accessed_by VARCHAR(255) NOT NULL,
  accessed_table VARCHAR(255) NOT NULL,
  access_type VARCHAR(20) NOT NULL,  -- SELECT, INSERT, UPDATE, DELETE
  row_count INTEGER,
  accessed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE data_access_audit IS 'Tracks all data access for HIPAA compliance and security monitoring';

-- ============================================================================
-- Configuration parameters for application to use
-- ============================================================================

-- These can be set at session level by the application:
-- SET app.current_tenant_id TO 'tenant-uuid';
-- SET app.current_user TO 'username';

-- ============================================================================
-- Version tracking
-- ============================================================================
-- Flyway migration V3__Add_Database_Roles_And_Permissions.sql
-- Applied for: CMS Connector Service Phase 2 Week 2
-- Changes:
--   - Created 5 database roles (cms_service, cms_analytics, cms_auditor, cms_clinician, cms_admin)
--   - Granted granular table permissions per role
--   - Granted view permissions for reporting
--   - Enabled Row-Level Security (RLS) for tenant isolation
--   - Created RLS policies for clinicians (tenant isolation + read-only)
--   - Created data access audit table for HIPAA compliance
--   - Configured application session parameters for tenant context
