-- Phase 2 Week 2: Initial Database Schema
-- CMS Connector Service - PostgreSQL Production Database
-- Created: Phase 2 Week 2
-- Purpose: Foundation tables for claims management, validation, and sync operations

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "btree_gin";

-- ============================================================================
-- 1. CMS_CLAIMS - Main claims table
-- ============================================================================
CREATE TABLE IF NOT EXISTS cms_claims (
  -- Primary key
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

  -- Claim identification
  claim_id VARCHAR(255) NOT NULL,
  beneficiary_id VARCHAR(255) NOT NULL,
  data_source VARCHAR(50) NOT NULL CHECK (data_source IN ('BCDA', 'DPC', 'AB2D')),

  -- Multi-tenancy
  tenant_id UUID NOT NULL,

  -- Claim details
  claim_type VARCHAR(100),
  claim_date DATE,
  processing_date DATE,
  total_charge_amount NUMERIC(15, 2),
  total_allowed_amount NUMERIC(15, 2),

  -- FHIR data (stored as JSONB for flexibility)
  fhir_resource JSONB NOT NULL,

  -- Processing status
  is_processed BOOLEAN DEFAULT false,
  has_validation_errors BOOLEAN DEFAULT false,
  content_hash VARCHAR(64) NOT NULL,  -- SHA-256 for deduplication

  -- Import tracking
  imported_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  imported_by VARCHAR(255),

  -- Deduplication tracking
  deduplication_status VARCHAR(50) DEFAULT 'NEW' CHECK (
    deduplication_status IN ('NEW', 'EXACT_MATCH', 'CONTENT_MATCH', 'BENEFICIARY_DATE_MATCH', 'UNIQUE')
  ),
  deduplication_confidence NUMERIC(3, 2),  -- 0.00 to 1.00
  matched_claim_id UUID,  -- Reference to duplicate if found

  -- Audit
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

  -- Data quality
  validation_run_count INTEGER DEFAULT 0,
  last_validation_at TIMESTAMP WITH TIME ZONE
);

-- Table comments
COMMENT ON TABLE cms_claims IS 'Primary claims data from CMS APIs (BCDA, DPC, AB2D). HIPAA-protected.';
COMMENT ON COLUMN cms_claims.content_hash IS 'SHA-256 hash of FHIR resource for deduplication';
COMMENT ON COLUMN cms_claims.deduplication_status IS 'Status of deduplication check: NEW, EXACT_MATCH (100%), CONTENT_MATCH (100%), BENEFICIARY_DATE_MATCH (70%), UNIQUE';
COMMENT ON COLUMN cms_claims.fhir_resource IS 'HL7 FHIR R4 ExplanationOfBenefit resource as JSONB';

-- ============================================================================
-- 2. CLAIM_VALIDATION_ERRORS - Validation failure details
-- ============================================================================
CREATE TABLE IF NOT EXISTS claim_validation_errors (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

  claim_id UUID NOT NULL REFERENCES cms_claims(id) ON DELETE CASCADE,
  tenant_id UUID NOT NULL,

  -- Error details
  rule_id VARCHAR(50) NOT NULL,  -- e.g., RULE_001, RULE_002, etc.
  rule_name VARCHAR(255) NOT NULL,
  error_message TEXT NOT NULL,
  error_severity VARCHAR(20) CHECK (error_severity IN ('ERROR', 'WARNING')),

  -- Context
  field_name VARCHAR(255),
  field_value TEXT,

  -- Audit
  detected_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  resolved BOOLEAN DEFAULT false,
  resolved_at TIMESTAMP WITH TIME ZONE,

  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE claim_validation_errors IS 'Detailed validation error logs for claims that fail validation rules';
COMMENT ON COLUMN claim_validation_errors.rule_id IS 'Rule identifier: RULE_001 through RULE_020 as per Phase 1 Week 3 design';

-- ============================================================================
-- 3. SYNC_AUDIT_LOG - Sync operation tracking
-- ============================================================================
CREATE TABLE IF NOT EXISTS sync_audit_log (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

  -- Operation details
  sync_source VARCHAR(50) NOT NULL CHECK (sync_source IN ('BCDA', 'DPC', 'AB2D')),
  sync_type VARCHAR(50) NOT NULL CHECK (sync_type IN ('BULK_EXPORT', 'POINT_OF_CARE', 'MANUAL')),
  sync_status VARCHAR(50) NOT NULL CHECK (sync_status IN ('INITIATED', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),

  -- Multi-tenancy
  tenant_id UUID,

  -- Statistics
  total_claims INTEGER DEFAULT 0,
  successful_claims INTEGER DEFAULT 0,
  failed_claims INTEGER DEFAULT 0,
  duplicate_claims INTEGER DEFAULT 0,
  error_message TEXT,

  -- Timing
  started_at TIMESTAMP WITH TIME ZONE NOT NULL,
  completed_at TIMESTAMP WITH TIME ZONE,
  duration_seconds INTEGER,

  -- Export references
  export_id VARCHAR(255),  -- BCDA/AB2D export ID

  -- Audit
  created_by VARCHAR(255),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sync_audit_log IS 'Complete audit trail of all CMS data sync operations for compliance';
COMMENT ON COLUMN sync_audit_log.sync_type IS 'BULK_EXPORT: BCDA/AB2D scheduled, POINT_OF_CARE: DPC real-time, MANUAL: User-triggered';

-- ============================================================================
-- 4. SYNC_STATUS - Track in-progress sync operations
-- ============================================================================
CREATE TABLE IF NOT EXISTS sync_status (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

  -- Operation reference
  sync_audit_log_id UUID NOT NULL REFERENCES sync_audit_log(id) ON DELETE CASCADE,

  -- Current state
  current_phase VARCHAR(50) NOT NULL,  -- EXPORT_REQUEST, POLLING, DOWNLOADING, IMPORTING
  phase_start_time TIMESTAMP WITH TIME ZONE,

  -- Progress tracking
  total_files INTEGER DEFAULT 0,
  processed_files INTEGER DEFAULT 0,
  current_file_name VARCHAR(255),

  -- Latest status update
  last_message TEXT,
  last_updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sync_status IS 'Real-time status tracking for long-running sync operations';

-- ============================================================================
-- 5. TENANT_CONFIG - Multi-tenant configuration
-- ============================================================================
CREATE TABLE IF NOT EXISTS tenant_config (
  id UUID PRIMARY KEY,  -- Same as tenant_id

  -- Tenant info
  tenant_name VARCHAR(255) NOT NULL UNIQUE,
  tenant_type VARCHAR(50),  -- HOSPITAL, HEALTH_PLAN, RESEARCH, SYSTEM

  -- CMS source assignment
  primary_data_sources VARCHAR(500),  -- JSON array: ["BCDA", "DPC", "AB2D"]

  -- Configuration
  claim_retention_days INTEGER DEFAULT 2555,  -- 7 years
  sync_enabled BOOLEAN DEFAULT true,

  -- Audit
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE tenant_config IS 'Multi-tenant configuration for data isolation and retention';

-- ============================================================================
-- 6. IMPORT_SESSION - Track multi-file imports
-- ============================================================================
CREATE TABLE IF NOT EXISTS import_session (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

  tenant_id UUID NOT NULL,
  data_source VARCHAR(50) NOT NULL,
  session_type VARCHAR(50) NOT NULL,  -- BULK_EXPORT, MANUAL_UPLOAD

  -- File tracking
  total_files INTEGER DEFAULT 0,
  processed_files INTEGER DEFAULT 0,

  -- Status
  status VARCHAR(50) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'PAUSED')),
  error_message TEXT,

  -- Timestamps
  started_at TIMESTAMP WITH TIME ZONE NOT NULL,
  completed_at TIMESTAMP WITH TIME ZONE,

  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE import_session IS 'Parent record for tracking multi-file import operations';

-- ============================================================================
-- 7. IMPORT_SESSION_FILE - Track individual files in import session
-- ============================================================================
CREATE TABLE IF NOT EXISTS import_session_file (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

  import_session_id UUID NOT NULL REFERENCES import_session(id) ON DELETE CASCADE,

  -- File details
  file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500),
  file_size_bytes INTEGER,

  -- Processing
  status VARCHAR(50) NOT NULL,  -- PENDING, PROCESSING, COMPLETED, FAILED
  claims_in_file INTEGER DEFAULT 0,
  claims_processed INTEGER DEFAULT 0,
  claims_failed INTEGER DEFAULT 0,

  error_message TEXT,

  -- Timestamps
  started_at TIMESTAMP WITH TIME ZONE,
  completed_at TIMESTAMP WITH TIME ZONE,

  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE import_session_file IS 'Granular tracking of individual files within import sessions';

-- ============================================================================
-- Create indexes for performance
-- ============================================================================

-- cms_claims indexes
CREATE INDEX idx_cms_claims_tenant_id ON cms_claims(tenant_id);
CREATE INDEX idx_cms_claims_claim_id ON cms_claims(claim_id);
CREATE INDEX idx_cms_claims_beneficiary_id ON cms_claims(beneficiary_id);
CREATE INDEX idx_cms_claims_data_source ON cms_claims(data_source);
CREATE INDEX idx_cms_claims_imported_at ON cms_claims(imported_at DESC);
CREATE INDEX idx_cms_claims_content_hash ON cms_claims(content_hash);
CREATE INDEX idx_cms_claims_dedup_status ON cms_claims(deduplication_status);

-- Multi-column indexes for common queries
CREATE INDEX idx_cms_claims_tenant_source_date ON cms_claims(tenant_id, data_source, claim_date DESC);
CREATE INDEX idx_cms_claims_tenant_beneficiary ON cms_claims(tenant_id, beneficiary_id);

-- JSONB index for FHIR resource queries
CREATE INDEX idx_cms_claims_fhir_gin ON cms_claims USING GIN (fhir_resource);

-- claim_validation_errors indexes
CREATE INDEX idx_validation_errors_claim_id ON claim_validation_errors(claim_id);
CREATE INDEX idx_validation_errors_tenant_id ON claim_validation_errors(tenant_id);
CREATE INDEX idx_validation_errors_rule_id ON claim_validation_errors(rule_id);
CREATE INDEX idx_validation_errors_resolved ON claim_validation_errors(resolved);

-- sync_audit_log indexes
CREATE INDEX idx_sync_audit_source ON sync_audit_log(sync_source);
CREATE INDEX idx_sync_audit_status ON sync_audit_log(sync_status);
CREATE INDEX idx_sync_audit_created_at ON sync_audit_log(created_at DESC);
CREATE INDEX idx_sync_audit_tenant_id ON sync_audit_log(tenant_id);

-- import_session indexes
CREATE INDEX idx_import_session_tenant_id ON import_session(tenant_id);
CREATE INDEX idx_import_session_status ON import_session(status);

-- ============================================================================
-- Create update trigger for updated_at column
-- ============================================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER cms_claims_update_trigger
BEFORE UPDATE ON cms_claims
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER tenant_config_update_trigger
BEFORE UPDATE ON tenant_config
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- Version tracking
-- ============================================================================
-- This schema represents Flyway migration V1__Initial_Schema.sql
-- Applied for: CMS Connector Service Phase 2 Week 2
-- Tables created: 7 (cms_claims, claim_validation_errors, sync_audit_log, sync_status, tenant_config, import_session, import_session_file)
-- Indexes created: 17 (optimized for common queries)
-- Extensions required: uuid-ossp, pg_trgm, btree_gin
