-- Agent Runtime Service Database Schema
-- Version: 1.0.0
-- Description: Create tables for agent task execution tracking

-- Agent task executions table
CREATE TABLE IF NOT EXISTS agent_task_executions (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    agent_id VARCHAR(64),
    agent_type VARCHAR(64),
    task_type VARCHAR(64) NOT NULL,
    input_summary TEXT,
    output_summary TEXT,
    status VARCHAR(32) NOT NULL,
    duration_ms BIGINT DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_agent_tasks_tenant_session
    ON agent_task_executions(tenant_id, session_id);

CREATE INDEX IF NOT EXISTS idx_agent_tasks_tenant_user
    ON agent_task_executions(tenant_id, user_id);

CREATE INDEX IF NOT EXISTS idx_agent_tasks_started_at
    ON agent_task_executions(started_at DESC);

CREATE INDEX IF NOT EXISTS idx_agent_tasks_status
    ON agent_task_executions(status) WHERE status = 'IN_PROGRESS';

CREATE INDEX IF NOT EXISTS idx_agent_tasks_agent_type
    ON agent_task_executions(agent_type);

-- Agent configurations table (for custom agents)
CREATE TABLE IF NOT EXISTS agent_configurations (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    version VARCHAR(32) NOT NULL DEFAULT '1.0.0',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    description TEXT,
    persona_name VARCHAR(255),
    persona_role VARCHAR(255),
    model_provider VARCHAR(64),
    model_id VARCHAR(128),
    system_prompt TEXT,
    prompt_templates JSONB DEFAULT '{}',
    tool_configuration JSONB DEFAULT '[]',
    guardrail_configuration JSONB DEFAULT '{}',
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uk_agent_config_tenant_name_version UNIQUE (tenant_id, name, version),
    CONSTRAINT chk_agent_status CHECK (status IN ('DRAFT', 'TESTING', 'ACTIVE', 'DEPRECATED'))
);

CREATE INDEX IF NOT EXISTS idx_agent_config_tenant
    ON agent_configurations(tenant_id);

CREATE INDEX IF NOT EXISTS idx_agent_config_status
    ON agent_configurations(status) WHERE status = 'ACTIVE';

-- Agent versions table (for rollback support)
CREATE TABLE IF NOT EXISTS agent_versions (
    id UUID PRIMARY KEY,
    agent_configuration_id UUID NOT NULL REFERENCES agent_configurations(id) ON DELETE CASCADE,
    version_number VARCHAR(32) NOT NULL,
    configuration_snapshot JSONB NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    change_summary TEXT,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_agent_version UNIQUE (agent_configuration_id, version_number),
    CONSTRAINT chk_version_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ROLLED_BACK'))
);

CREATE INDEX IF NOT EXISTS idx_agent_versions_config
    ON agent_versions(agent_configuration_id);

-- Agent execution metrics (for analytics)
CREATE TABLE IF NOT EXISTS agent_execution_metrics (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    agent_id VARCHAR(64),
    agent_type VARCHAR(64) NOT NULL,
    metric_date DATE NOT NULL,
    total_executions INTEGER DEFAULT 0,
    successful_executions INTEGER DEFAULT 0,
    failed_executions INTEGER DEFAULT 0,
    total_tokens_used BIGINT DEFAULT 0,
    total_duration_ms BIGINT DEFAULT 0,
    avg_duration_ms BIGINT DEFAULT 0,
    p95_duration_ms BIGINT DEFAULT 0,
    p99_duration_ms BIGINT DEFAULT 0,
    tool_usage JSONB DEFAULT '{}',
    error_breakdown JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_agent_metrics UNIQUE (tenant_id, agent_id, agent_type, metric_date)
);

CREATE INDEX IF NOT EXISTS idx_agent_metrics_tenant_date
    ON agent_execution_metrics(tenant_id, metric_date DESC);

-- AI consent tracking
CREATE TABLE IF NOT EXISTS ai_consent (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    feature_type VARCHAR(64) NOT NULL,
    consent_level VARCHAR(32) NOT NULL,
    llm_data_sharing_consent BOOLEAN DEFAULT FALSE,
    cross_border_consent BOOLEAN DEFAULT FALSE,
    jurisdictions_consented TEXT[],
    consented_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    consent_source VARCHAR(64),
    consent_document_id VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_consent_level CHECK (consent_level IN ('EXPLICIT_OPT_IN', 'OPT_OUT_AVAILABLE', 'OPT_OUT')),
    CONSTRAINT chk_feature_type CHECK (feature_type IN (
        'AI_CLINICAL_SUMMARY',
        'AI_RISK_PREDICTION',
        'AI_CARE_RECOMMENDATIONS',
        'AI_DOCUMENTATION',
        'AI_OUTREACH'
    ))
);

CREATE INDEX IF NOT EXISTS idx_ai_consent_patient
    ON ai_consent(patient_id, tenant_id);

CREATE INDEX IF NOT EXISTS idx_ai_consent_feature
    ON ai_consent(tenant_id, feature_type) WHERE revoked_at IS NULL;

-- Comments
COMMENT ON TABLE agent_task_executions IS 'Stores all agent task executions for auditing and analytics';
COMMENT ON TABLE agent_configurations IS 'Stores custom agent configurations created by tenants';
COMMENT ON TABLE agent_versions IS 'Stores version history for agent configurations';
COMMENT ON TABLE agent_execution_metrics IS 'Daily aggregated metrics for agent performance';
COMMENT ON TABLE ai_consent IS 'Tracks patient consent for AI features';
