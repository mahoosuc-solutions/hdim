-- Performance Optimization: GIN Indexes for JSONB Columns
-- Version: 2.0.0
-- Description: Add GIN indexes for JSONB queries and additional B-tree indexes for common queries

-- ============================================================================
-- GIN INDEXES FOR JSONB COLUMNS
-- ============================================================================

-- Agent configurations: tool_configuration JSONB
-- Enables fast containment queries like: tool_configuration @> '{"name": "fhir_query"}'
CREATE INDEX IF NOT EXISTS idx_agent_config_tools_gin
    ON agent_configurations USING GIN (tool_configuration jsonb_path_ops);

-- Agent configurations: guardrail_configuration JSONB
-- Enables fast queries on guardrail settings
CREATE INDEX IF NOT EXISTS idx_agent_config_guardrails_gin
    ON agent_configurations USING GIN (guardrail_configuration jsonb_path_ops);

-- Agent configurations: ui_configuration JSONB
CREATE INDEX IF NOT EXISTS idx_agent_config_ui_gin
    ON agent_configurations USING GIN (ui_configuration jsonb_path_ops);

-- Agent versions: configuration_snapshot JSONB
-- Critical for version comparison and restoration queries
CREATE INDEX IF NOT EXISTS idx_agent_version_snapshot_gin
    ON agent_versions USING GIN (configuration_snapshot jsonb_path_ops);

-- Prompt templates: variables JSONB
CREATE INDEX IF NOT EXISTS idx_prompt_template_variables_gin
    ON prompt_templates USING GIN (variables jsonb_path_ops);

-- Agent test sessions: messages, tool_invocations, metrics JSONB
CREATE INDEX IF NOT EXISTS idx_test_session_messages_gin
    ON agent_test_sessions USING GIN (messages jsonb_path_ops);

CREATE INDEX IF NOT EXISTS idx_test_session_metrics_gin
    ON agent_test_sessions USING GIN (metrics jsonb_path_ops);

-- Tool definitions: input_schema JSONB
CREATE INDEX IF NOT EXISTS idx_tool_def_schema_gin
    ON tool_definitions USING GIN (input_schema jsonb_path_ops);

-- ============================================================================
-- B-TREE INDEXES FOR COMMON QUERY PATTERNS
-- ============================================================================

-- Composite index for tenant + status queries (most common pattern)
CREATE INDEX IF NOT EXISTS idx_agent_config_tenant_status
    ON agent_configurations (tenant_id, status);

-- Composite index for tenant + name searches
CREATE INDEX IF NOT EXISTS idx_agent_config_tenant_name
    ON agent_configurations (tenant_id, LOWER(name));

-- Index for version lookups with status
CREATE INDEX IF NOT EXISTS idx_agent_version_status_created
    ON agent_versions (agent_configuration_id, status, created_at DESC);

-- Index for latest version queries (covering index)
CREATE INDEX IF NOT EXISTS idx_agent_version_latest
    ON agent_versions (agent_configuration_id, created_at DESC)
    INCLUDE (version_number, status);

-- Prompt templates: tenant + category for filtered listings
CREATE INDEX IF NOT EXISTS idx_prompt_template_tenant_category
    ON prompt_templates (tenant_id, category);

-- Prompt templates: system templates (frequently accessed)
CREATE INDEX IF NOT EXISTS idx_prompt_template_system
    ON prompt_templates (is_system) WHERE is_system = TRUE;

-- Test sessions: agent + status for active tests
CREATE INDEX IF NOT EXISTS idx_test_session_agent_status
    ON agent_test_sessions (agent_configuration_id, status);

-- Test sessions: recent tests per tenant
CREATE INDEX IF NOT EXISTS idx_test_session_tenant_time
    ON agent_test_sessions (tenant_id, started_at DESC);

-- Analytics: common time-range queries
CREATE INDEX IF NOT EXISTS idx_analytics_tenant_date_range
    ON agent_usage_analytics (tenant_id, analytics_date DESC);

-- ============================================================================
-- PARTIAL INDEXES FOR FILTERED QUERIES
-- ============================================================================

-- Only active agents (frequently queried, small subset)
CREATE INDEX IF NOT EXISTS idx_agent_config_active_only
    ON agent_configurations (tenant_id, name)
    WHERE status = 'ACTIVE';

-- Only draft agents (for builder UI)
CREATE INDEX IF NOT EXISTS idx_agent_config_draft_only
    ON agent_configurations (tenant_id, updated_at DESC)
    WHERE status = 'DRAFT';

-- Only published versions (for production deployment)
CREATE INDEX IF NOT EXISTS idx_agent_version_published_only
    ON agent_versions (agent_configuration_id, published_at DESC)
    WHERE status = 'PUBLISHED';

-- ============================================================================
-- EXPRESSION INDEXES FOR CASE-INSENSITIVE SEARCHES
-- ============================================================================

-- Case-insensitive name search
CREATE INDEX IF NOT EXISTS idx_agent_config_name_lower
    ON agent_configurations (tenant_id, LOWER(name) varchar_pattern_ops);

-- Case-insensitive description search
CREATE INDEX IF NOT EXISTS idx_agent_config_desc_lower
    ON agent_configurations (tenant_id, LOWER(description) varchar_pattern_ops);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON INDEX idx_agent_config_tools_gin IS 'GIN index for tool_configuration JSONB queries';
COMMENT ON INDEX idx_agent_version_snapshot_gin IS 'GIN index for configuration_snapshot JSONB - critical for version restore';
COMMENT ON INDEX idx_agent_config_tenant_status IS 'Composite index for tenant+status filtering - most common query pattern';
COMMENT ON INDEX idx_agent_version_latest IS 'Covering index for latest version lookups - avoids table access';
