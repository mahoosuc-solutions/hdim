--liquibase formatted sql

--changeset cost-analysis:001-create-cost-tracking
CREATE TABLE IF NOT EXISTS cost_tracking (
    id UUID PRIMARY KEY,
    metric_type VARCHAR(64) NOT NULL,
    metric_value DECIMAL(16,4) NOT NULL,
    cost_amount DECIMAL(16,4) NOT NULL,
    timestamp_utc TIMESTAMP NOT NULL,
    service_id VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    feature_key VARCHAR(128),
    request_id VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_cost_tracking_service_time ON cost_tracking(service_id, timestamp_utc);
CREATE INDEX IF NOT EXISTS idx_cost_tracking_tenant_time ON cost_tracking(tenant_id, timestamp_utc);
CREATE INDEX IF NOT EXISTS idx_cost_tracking_metric_type ON cost_tracking(metric_type);

--changeset cost-analysis:002-create-cost-daily-summary
CREATE TABLE IF NOT EXISTS cost_daily_summary (
    id UUID PRIMARY KEY,
    summary_date DATE NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    service_id VARCHAR(128) NOT NULL,
    feature_key VARCHAR(128),
    total_cost DECIMAL(16,4) NOT NULL,
    sample_count BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cost_daily_summary_tenant_date ON cost_daily_summary(tenant_id, summary_date);
CREATE INDEX IF NOT EXISTS idx_cost_daily_summary_service_date ON cost_daily_summary(service_id, summary_date);

--changeset cost-analysis:003-create-analysis-cache
CREATE TABLE IF NOT EXISTS cost_analysis_cache (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    analysis_type VARCHAR(64) NOT NULL,
    analysis_period VARCHAR(32) NOT NULL,
    service_name VARCHAR(128),
    result_data TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    cache_hits INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_cache_tenant_type_period
    ON cost_analysis_cache(tenant_id, analysis_type, analysis_period);
CREATE INDEX IF NOT EXISTS idx_cache_expires_at ON cost_analysis_cache(expires_at);

--changeset cost-analysis:004-create-optimization-recommendation
CREATE TABLE IF NOT EXISTS optimization_recommendation (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(128) NOT NULL,
    recommendation_type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    estimated_savings DECIMAL(14,2) NOT NULL,
    actual_savings DECIMAL(14,2),
    savings_currency VARCHAR(8),
    savings_timeframe VARCHAR(32),
    confidence_score DECIMAL(5,2),
    implementation_effort VARCHAR(16) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    implementation_date TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_opt_tenant_status ON optimization_recommendation(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_opt_tenant_service ON optimization_recommendation(tenant_id, service_name);
