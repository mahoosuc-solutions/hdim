--liquibase formatted sql

--changeset migration:create-migration-jobs-table
-- Migration jobs table for tracking data migration state

CREATE TABLE IF NOT EXISTS migration_jobs (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    job_name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    source_type VARCHAR(20) NOT NULL,
    source_config JSONB NOT NULL,
    data_type VARCHAR(20) NOT NULL,
    convert_to_fhir BOOLEAN NOT NULL DEFAULT TRUE,
    continue_on_error BOOLEAN NOT NULL DEFAULT TRUE,
    batch_size INT NOT NULL DEFAULT 100,
    resumable BOOLEAN NOT NULL DEFAULT TRUE,

    -- Progress tracking
    total_records BIGINT DEFAULT 0,
    processed_count BIGINT DEFAULT 0,
    success_count BIGINT DEFAULT 0,
    failure_count BIGINT DEFAULT 0,
    skipped_count BIGINT DEFAULT 0,

    -- Timing
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    last_checkpoint_at TIMESTAMPTZ,

    -- Retry tracking
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    next_retry_at TIMESTAMPTZ,

    -- Checkpoint data
    checkpoint JSONB,

    -- FHIR resources created
    fhir_resources_created JSONB,

    -- External references
    target_fhir_url VARCHAR(500),
    callback_url VARCHAR(500)
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_migration_jobs_tenant ON migration_jobs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_migration_jobs_status ON migration_jobs(status);
CREATE INDEX IF NOT EXISTS idx_migration_jobs_tenant_status ON migration_jobs(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_migration_jobs_created ON migration_jobs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_migration_jobs_retry ON migration_jobs(status, next_retry_at)
    WHERE status = 'RETRYING';

--changeset migration:create-migration-errors-table
-- Migration errors table for tracking individual record failures

CREATE TABLE IF NOT EXISTS migration_errors (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES migration_jobs(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    record_identifier VARCHAR(255),
    source_file VARCHAR(500),
    record_offset BIGINT,
    error_category VARCHAR(50) NOT NULL,
    error_message TEXT NOT NULL,
    error_details JSONB,
    source_data TEXT,
    stack_trace TEXT,
    patient_id VARCHAR(64),
    message_type VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for error analysis
CREATE INDEX IF NOT EXISTS idx_migration_errors_job ON migration_errors(job_id);
CREATE INDEX IF NOT EXISTS idx_migration_errors_category ON migration_errors(error_category);
CREATE INDEX IF NOT EXISTS idx_migration_errors_job_category ON migration_errors(job_id, error_category);
CREATE INDEX IF NOT EXISTS idx_migration_errors_patient ON migration_errors(patient_id) WHERE patient_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_migration_errors_created ON migration_errors(created_at DESC);

--changeset migration:create-migration-checkpoints-table
-- Migration checkpoints table for job resumability

CREATE TABLE IF NOT EXISTS migration_checkpoints (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES migration_jobs(id) ON DELETE CASCADE,
    checkpoint_number INT NOT NULL,
    checkpoint_data JSONB NOT NULL,
    records_processed BIGINT NOT NULL,
    records_success BIGINT NOT NULL,
    records_failure BIGINT NOT NULL,
    current_file VARCHAR(500),
    current_offset BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_migration_checkpoints_job_number UNIQUE (job_id, checkpoint_number)
);

-- Indexes for checkpoint retrieval
CREATE INDEX IF NOT EXISTS idx_migration_checkpoints_job ON migration_checkpoints(job_id);
CREATE INDEX IF NOT EXISTS idx_migration_checkpoints_job_num ON migration_checkpoints(job_id, checkpoint_number DESC);

--changeset migration:add-migration-jobs-comments
COMMENT ON TABLE migration_jobs IS 'Migration job tracking with progress and checkpoint support';
COMMENT ON COLUMN migration_jobs.source_config IS 'JSON configuration for source connector (FILE, SFTP, MLLP)';
COMMENT ON COLUMN migration_jobs.checkpoint IS 'Latest checkpoint data for job resumability';
COMMENT ON COLUMN migration_jobs.fhir_resources_created IS 'Count of FHIR resources created by type';

COMMENT ON TABLE migration_errors IS 'Individual record failures during migration';
COMMENT ON COLUMN migration_errors.source_data IS 'Original source data (truncated) for debugging';

COMMENT ON TABLE migration_checkpoints IS 'Checkpoints for resumable migrations';
COMMENT ON COLUMN migration_checkpoints.checkpoint_data IS 'Source-specific state needed for resume';
