-- Phase 2 Week 2: Add Foreign Keys and Constraints
-- CMS Connector Service - Database Schema Enhancement
-- Created: Phase 2 Week 2

-- ============================================================================
-- Add unique constraints for deduplication tracking
-- ============================================================================
ALTER TABLE cms_claims
ADD CONSTRAINT uq_claims_content_hash_tenant UNIQUE (content_hash, tenant_id);

-- Index for tenant-specific deduplication lookups
CREATE UNIQUE INDEX idx_claims_exact_dedup ON cms_claims(claim_id, tenant_id, data_source)
WHERE deduplication_status = 'EXACT_MATCH';

-- ============================================================================
-- Add foreign key constraint from matched claims
-- ============================================================================
ALTER TABLE cms_claims
ADD CONSTRAINT fk_cms_claims_matched_claim
FOREIGN KEY (matched_claim_id)
REFERENCES cms_claims(id)
ON DELETE SET NULL;

-- ============================================================================
-- Add constraint for claim validation errors
-- ============================================================================
ALTER TABLE claim_validation_errors
ADD CONSTRAINT fk_validation_errors_tenant
FOREIGN KEY (tenant_id)
REFERENCES tenant_config(id)
ON DELETE RESTRICT;

-- ============================================================================
-- Add constraint for sync audit log
-- ============================================================================
ALTER TABLE sync_audit_log
ADD CONSTRAINT fk_sync_audit_tenant
FOREIGN KEY (tenant_id)
REFERENCES tenant_config(id)
ON DELETE SET NULL;

-- ============================================================================
-- Add constraint for sync status
-- ============================================================================
ALTER TABLE sync_status
ADD CONSTRAINT fk_sync_status_audit
FOREIGN KEY (sync_audit_log_id)
REFERENCES sync_audit_log(id)
ON DELETE CASCADE;

-- ============================================================================
-- Add constraints for import session and files
-- ============================================================================
ALTER TABLE import_session
ADD CONSTRAINT fk_import_session_tenant
FOREIGN KEY (tenant_id)
REFERENCES tenant_config(id)
ON DELETE RESTRICT;

ALTER TABLE import_session_file
ADD CONSTRAINT fk_import_session_file_tenant
FOREIGN KEY (import_session_id)
REFERENCES import_session(id)
ON DELETE CASCADE;

-- ============================================================================
-- Add check constraints for data quality
-- ============================================================================
ALTER TABLE cms_claims
ADD CONSTRAINT check_claim_amounts_positive
CHECK (total_charge_amount >= 0 AND total_allowed_amount >= 0);

ALTER TABLE sync_audit_log
ADD CONSTRAINT check_sync_stats_non_negative
CHECK (total_claims >= 0 AND successful_claims >= 0 AND failed_claims >= 0);

ALTER TABLE sync_audit_log
ADD CONSTRAINT check_sync_timing
CHECK (completed_at IS NULL OR completed_at >= started_at);

-- ============================================================================
-- Create view for easy access to validation summary
-- ============================================================================
CREATE OR REPLACE VIEW claim_validation_summary AS
SELECT
  c.id,
  c.tenant_id,
  c.claim_id,
  c.beneficiary_id,
  c.data_source,
  c.has_validation_errors,
  COUNT(ve.id) as error_count,
  COUNT(CASE WHEN ve.error_severity = 'ERROR' THEN 1 END) as critical_error_count,
  COUNT(CASE WHEN ve.error_severity = 'WARNING' THEN 1 END) as warning_count,
  MAX(ve.detected_at) as last_error_at,
  c.last_validation_at
FROM cms_claims c
LEFT JOIN claim_validation_errors ve ON c.id = ve.claim_id
GROUP BY c.id, c.tenant_id, c.claim_id, c.beneficiary_id, c.data_source, c.has_validation_errors, c.last_validation_at;

COMMENT ON VIEW claim_validation_summary IS 'Summary view of validation errors per claim for reporting and troubleshooting';

-- ============================================================================
-- Create view for sync operation metrics
-- ============================================================================
CREATE OR REPLACE VIEW sync_operation_metrics AS
SELECT
  DATE(sal.started_at) as sync_date,
  sal.sync_source,
  sal.sync_type,
  sal.sync_status,
  COUNT(*) as operation_count,
  SUM(sal.total_claims) as total_claims_processed,
  SUM(sal.successful_claims) as total_claims_successful,
  SUM(sal.failed_claims) as total_claims_failed,
  SUM(sal.duplicate_claims) as total_duplicates,
  AVG(sal.duration_seconds) as avg_duration_seconds,
  MAX(sal.duration_seconds) as max_duration_seconds,
  MIN(sal.duration_seconds) as min_duration_seconds
FROM sync_audit_log sal
GROUP BY DATE(sal.started_at), sal.sync_source, sal.sync_type, sal.sync_status;

COMMENT ON VIEW sync_operation_metrics IS 'Daily metrics for sync operations by source and type for monitoring';

-- ============================================================================
-- Create view for tenant import activity
-- ============================================================================
CREATE OR REPLACE VIEW tenant_import_activity AS
SELECT
  sal.tenant_id,
  tc.tenant_name,
  COUNT(DISTINCT sal.id) as total_syncs,
  SUM(sal.total_claims) as total_claims,
  SUM(CASE WHEN sal.sync_status = 'COMPLETED' THEN 1 ELSE 0 END) as successful_syncs,
  SUM(CASE WHEN sal.sync_status = 'FAILED' THEN 1 ELSE 0 END) as failed_syncs,
  MAX(sal.completed_at) as last_sync_completed,
  AVG(sal.duration_seconds) FILTER (WHERE sal.sync_status = 'COMPLETED') as avg_sync_duration_seconds
FROM sync_audit_log sal
LEFT JOIN tenant_config tc ON sal.tenant_id = tc.id
GROUP BY sal.tenant_id, tc.tenant_name;

COMMENT ON VIEW tenant_import_activity IS 'Per-tenant import activity and success metrics for SLA monitoring';

-- ============================================================================
-- Create role-based access control foundation
-- ============================================================================
CREATE TABLE IF NOT EXISTS audit_log_access (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

  audit_log_id UUID NOT NULL REFERENCES sync_audit_log(id) ON DELETE CASCADE,
  accessed_by VARCHAR(255) NOT NULL,
  accessed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  action VARCHAR(50),  -- VIEW, EXPORT, etc.

  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE audit_log_access IS 'Track who accesses sync audit logs for HIPAA compliance';

-- ============================================================================
-- Create performance optimization materialized view
-- ============================================================================
CREATE MATERIALIZED VIEW IF NOT EXISTS claim_metrics_by_source_daily AS
SELECT
  DATE(c.imported_at) as import_date,
  c.data_source,
  c.tenant_id,
  COUNT(*) as claim_count,
  COUNT(CASE WHEN c.has_validation_errors THEN 1 END) as claims_with_errors,
  COUNT(CASE WHEN c.deduplication_status != 'NEW' THEN 1 END) as duplicate_claims,
  COUNT(CASE WHEN c.is_processed THEN 1 END) as processed_claims
FROM cms_claims c
GROUP BY DATE(c.imported_at), c.data_source, c.tenant_id;

CREATE UNIQUE INDEX idx_claim_metrics_unique ON claim_metrics_by_source_daily (import_date, data_source, tenant_id);

COMMENT ON MATERIALIZED VIEW claim_metrics_by_source_daily IS 'Daily metrics materialized view for fast dashboard queries (refresh hourly)';

-- ============================================================================
-- Create stored procedure for claim retention cleanup
-- ============================================================================
CREATE OR REPLACE FUNCTION cleanup_old_claims()
RETURNS TABLE(deleted_count INTEGER, deleted_errors_count INTEGER) AS $$
DECLARE
  v_deleted_count INTEGER := 0;
  v_deleted_errors_count INTEGER := 0;
BEGIN
  -- Get retention settings per tenant and delete old claims
  WITH retention_settings AS (
    SELECT
      tc.id,
      CURRENT_TIMESTAMP - (tc.claim_retention_days || ' days')::INTERVAL as cutoff_date
    FROM tenant_config tc
    WHERE tc.claim_retention_days > 0
  )
  DELETE FROM cms_claims c
  USING retention_settings rs
  WHERE c.tenant_id = rs.id
    AND c.imported_at < rs.cutoff_date
    AND c.is_processed = true  -- Only delete processed claims
  RETURNING 1;

  GET DIAGNOSTICS v_deleted_count = ROW_COUNT;

  -- Delete associated validation errors
  WITH old_errors AS (
    SELECT cve.id
    FROM claim_validation_errors cve
    WHERE NOT EXISTS (
      SELECT 1 FROM cms_claims c WHERE c.id = cve.claim_id
    )
  )
  DELETE FROM claim_validation_errors
  WHERE id IN (SELECT id FROM old_errors);

  GET DIAGNOSTICS v_deleted_errors_count = ROW_COUNT;

  RETURN QUERY SELECT v_deleted_count, v_deleted_errors_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_old_claims IS 'Maintenance procedure to delete old claims based on tenant retention policy';

-- ============================================================================
-- Version tracking
-- ============================================================================
-- Flyway migration V2__Add_Constraints_And_Partitioning.sql
-- Applied for: CMS Connector Service Phase 2 Week 2
-- Changes:
--   - Added UNIQUE constraints for deduplication tracking
--   - Added foreign key constraints with proper referential integrity
--   - Added check constraints for data validation
--   - Created 3 views for reporting and monitoring
--   - Created stored procedure for data retention cleanup
--   - Created materialized view for dashboard performance
--   - Added RBAC foundation table for audit access tracking
