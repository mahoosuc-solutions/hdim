-- Agent Builder Service Database Schema
-- Version: 1.0.0
-- Description: Tables for no-code agent configuration and management

-- Agent configurations (tenant-isolated)
CREATE TABLE IF NOT EXISTS agent_configurations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    version VARCHAR(32) NOT NULL DEFAULT '1.0.0',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',

    -- Persona configuration
    persona_name VARCHAR(255),
    persona_role VARCHAR(255),
    persona_avatar_url VARCHAR(512),

    -- Model configuration
    model_provider VARCHAR(64) NOT NULL DEFAULT 'claude',
    model_id VARCHAR(128),
    max_tokens INTEGER DEFAULT 4096,
    temperature DECIMAL(3,2) DEFAULT 0.3,

    -- Prompts
    system_prompt TEXT NOT NULL,
    welcome_message TEXT,

    -- Tool configuration (JSON array of tool names and configs)
    tool_configuration JSONB DEFAULT '[]',

    -- Guardrail configuration
    guardrail_configuration JSONB DEFAULT '{}',

    -- UI configuration (colors, position, behavior)
    ui_configuration JSONB DEFAULT '{}',

    -- Access control
    allowed_roles TEXT[] DEFAULT '{}',
    requires_patient_context BOOLEAN DEFAULT FALSE,

    -- Metadata
    tags TEXT[] DEFAULT '{}',
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE,
    archived_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uk_agent_config_tenant_slug UNIQUE (tenant_id, slug),
    CONSTRAINT chk_agent_status CHECK (status IN ('DRAFT', 'TESTING', 'ACTIVE', 'DEPRECATED', 'ARCHIVED'))
);

CREATE INDEX idx_agent_config_tenant ON agent_configurations(tenant_id);
CREATE INDEX idx_agent_config_status ON agent_configurations(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_agent_config_tags ON agent_configurations USING GIN(tags);

-- Agent versions (immutable snapshots for rollback)
CREATE TABLE IF NOT EXISTS agent_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_configuration_id UUID NOT NULL REFERENCES agent_configurations(id) ON DELETE CASCADE,
    version_number VARCHAR(32) NOT NULL,

    -- Complete configuration snapshot
    configuration_snapshot JSONB NOT NULL,

    -- Version metadata
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    change_summary TEXT,
    change_type VARCHAR(32) DEFAULT 'MINOR',

    -- Publishing info
    published_at TIMESTAMP WITH TIME ZONE,
    published_by VARCHAR(100),

    -- Rollback tracking
    rolled_back_at TIMESTAMP WITH TIME ZONE,
    rolled_back_by VARCHAR(100),
    rollback_reason TEXT,

    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_agent_version UNIQUE (agent_configuration_id, version_number),
    CONSTRAINT chk_version_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ROLLED_BACK', 'SUPERSEDED')),
    CONSTRAINT chk_change_type CHECK (change_type IN ('MAJOR', 'MINOR', 'PATCH'))
);

CREATE INDEX idx_agent_versions_config ON agent_versions(agent_configuration_id);
CREATE INDEX idx_agent_versions_status ON agent_versions(status);

-- Prompt templates (reusable prompt components)
CREATE TABLE IF NOT EXISTS prompt_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(64) NOT NULL,

    -- Template content with variable placeholders
    content TEXT NOT NULL,

    -- Variables that can be substituted
    variables JSONB DEFAULT '[]',

    -- Usage tracking
    usage_count INTEGER DEFAULT 0,

    is_system BOOLEAN DEFAULT FALSE,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_prompt_template_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT chk_prompt_category CHECK (category IN (
        'SYSTEM_PROMPT', 'CAPABILITIES', 'CONSTRAINTS', 'RESPONSE_FORMAT',
        'CLINICAL_SAFETY', 'TOOL_USAGE', 'PERSONA', 'CUSTOM'
    ))
);

CREATE INDEX idx_prompt_templates_tenant ON prompt_templates(tenant_id);
CREATE INDEX idx_prompt_templates_category ON prompt_templates(category);

-- Agent test sessions
CREATE TABLE IF NOT EXISTS agent_test_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_configuration_id UUID NOT NULL REFERENCES agent_configurations(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,

    -- Test configuration
    test_type VARCHAR(32) NOT NULL DEFAULT 'INTERACTIVE',
    test_scenario TEXT,

    -- Test execution
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',

    -- Results
    messages JSONB DEFAULT '[]',
    tool_invocations JSONB DEFAULT '[]',
    metrics JSONB DEFAULT '{}',

    -- Feedback
    tester_feedback TEXT,
    tester_rating INTEGER,

    created_by VARCHAR(100) NOT NULL,

    CONSTRAINT chk_test_type CHECK (test_type IN ('INTERACTIVE', 'AUTOMATED', 'SCENARIO')),
    CONSTRAINT chk_test_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_rating CHECK (tester_rating IS NULL OR (tester_rating >= 1 AND tester_rating <= 5))
);

CREATE INDEX idx_agent_tests_config ON agent_test_sessions(agent_configuration_id);
CREATE INDEX idx_agent_tests_tenant ON agent_test_sessions(tenant_id);

-- Agent usage analytics (daily aggregates)
CREATE TABLE IF NOT EXISTS agent_usage_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_configuration_id UUID NOT NULL REFERENCES agent_configurations(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    analytics_date DATE NOT NULL,

    -- Usage metrics
    total_sessions INTEGER DEFAULT 0,
    total_messages INTEGER DEFAULT 0,
    unique_users INTEGER DEFAULT 0,

    -- Performance metrics
    total_tokens_used BIGINT DEFAULT 0,
    avg_response_time_ms INTEGER DEFAULT 0,
    p95_response_time_ms INTEGER DEFAULT 0,

    -- Quality metrics
    tool_invocation_count INTEGER DEFAULT 0,
    successful_tool_invocations INTEGER DEFAULT 0,
    guardrail_triggers INTEGER DEFAULT 0,

    -- User feedback
    positive_feedback_count INTEGER DEFAULT 0,
    negative_feedback_count INTEGER DEFAULT 0,
    avg_rating DECIMAL(3,2),

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_agent_analytics UNIQUE (agent_configuration_id, analytics_date)
);

CREATE INDEX idx_agent_analytics_tenant_date ON agent_usage_analytics(tenant_id, analytics_date DESC);

-- Tool definitions (available tools for agents)
CREATE TABLE IF NOT EXISTS tool_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(64) NOT NULL,

    -- JSON Schema for input parameters
    input_schema JSONB NOT NULL,

    -- Tool metadata
    requires_patient_context BOOLEAN DEFAULT FALSE,
    requires_approval BOOLEAN DEFAULT FALSE,
    risk_level VARCHAR(32) DEFAULT 'LOW',

    -- Access control
    allowed_roles TEXT[] DEFAULT '{}',

    is_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_tool_category CHECK (category IN (
        'FHIR_QUERY', 'CQL_EXECUTION', 'DATA_RETRIEVAL', 'DATA_MUTATION',
        'NOTIFICATION', 'REPORTING', 'EXTERNAL_API'
    )),
    CONSTRAINT chk_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- Insert default tool definitions
INSERT INTO tool_definitions (name, display_name, description, category, input_schema, requires_patient_context, risk_level)
VALUES
    ('fhir_query', 'FHIR Query', 'Query FHIR resources from the healthcare data repository', 'FHIR_QUERY',
     '{"type":"object","properties":{"resourceType":{"type":"string"},"patientId":{"type":"string"},"searchParams":{"type":"object"}},"required":["resourceType"]}',
     false, 'LOW'),
    ('cql_execute', 'CQL Execution', 'Execute CQL quality measures and clinical logic', 'CQL_EXECUTION',
     '{"type":"object","properties":{"measureId":{"type":"string"},"patientId":{"type":"string"},"expression":{"type":"string"}},"required":[]}',
     false, 'LOW'),
    ('publish_event', 'Publish Event', 'Publish events to trigger workflows and notifications', 'NOTIFICATION',
     '{"type":"object","properties":{"eventType":{"type":"string"},"patientId":{"type":"string"},"priority":{"type":"string"},"payload":{"type":"object"}},"required":["eventType"]}',
     false, 'MEDIUM'),
    ('send_notification', 'Send Notification', 'Send notifications via multiple channels', 'NOTIFICATION',
     '{"type":"object","properties":{"channel":{"type":"string"},"recipientType":{"type":"string"},"recipientId":{"type":"string"},"message":{"type":"string"}},"required":["channel","recipientType","recipientId"]}',
     false, 'MEDIUM')
ON CONFLICT (name) DO NOTHING;

-- Comments
COMMENT ON TABLE agent_configurations IS 'Custom agent configurations created by tenants via no-code builder';
COMMENT ON TABLE agent_versions IS 'Immutable version snapshots for agent configurations';
COMMENT ON TABLE prompt_templates IS 'Reusable prompt components for building agents';
COMMENT ON TABLE agent_test_sessions IS 'Test sessions for validating agent behavior';
COMMENT ON TABLE agent_usage_analytics IS 'Daily aggregated usage analytics for agents';
COMMENT ON TABLE tool_definitions IS 'Available tools that can be enabled for custom agents';
