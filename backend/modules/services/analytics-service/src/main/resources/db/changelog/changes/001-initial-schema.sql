--liquibase formatted sql

--changeset analytics:001-create-dashboards-table
CREATE TABLE IF NOT EXISTS dashboards (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    layout JSONB,
    is_default BOOLEAN NOT NULL DEFAULT false,
    is_shared BOOLEAN NOT NULL DEFAULT false,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_dashboard_tenant ON dashboards(tenant_id);
CREATE INDEX IF NOT EXISTS idx_dashboard_created_by ON dashboards(created_by);
CREATE INDEX IF NOT EXISTS idx_dashboard_default ON dashboards(tenant_id, is_default);

--changeset analytics:002-create-dashboard-widgets-table
CREATE TABLE IF NOT EXISTS dashboard_widgets (
    id UUID PRIMARY KEY,
    dashboard_id UUID NOT NULL REFERENCES dashboards(id) ON DELETE CASCADE,
    tenant_id VARCHAR(100) NOT NULL,
    widget_type VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    config JSONB NOT NULL,
    data_source VARCHAR(100),
    refresh_interval_seconds INTEGER,
    position_x INTEGER,
    position_y INTEGER,
    width INTEGER,
    height INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_widget_dashboard ON dashboard_widgets(dashboard_id);
CREATE INDEX IF NOT EXISTS idx_widget_tenant ON dashboard_widgets(tenant_id);
CREATE INDEX IF NOT EXISTS idx_widget_type ON dashboard_widgets(widget_type);

--changeset analytics:003-create-reports-table
CREATE TABLE IF NOT EXISTS reports (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    report_type VARCHAR(50) NOT NULL,
    parameters JSONB,
    schedule_cron VARCHAR(100),
    schedule_enabled BOOLEAN NOT NULL DEFAULT false,
    output_format VARCHAR(20) DEFAULT 'PDF',
    recipients JSONB,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_report_tenant ON reports(tenant_id);
CREATE INDEX IF NOT EXISTS idx_report_type ON reports(report_type);
CREATE INDEX IF NOT EXISTS idx_report_created_by ON reports(created_by);

--changeset analytics:004-create-report-executions-table
CREATE TABLE IF NOT EXISTS report_executions (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    tenant_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    parameters JSONB,
    result_data JSONB,
    result_file_path VARCHAR(500),
    result_file_size BIGINT,
    row_count INTEGER,
    triggered_by VARCHAR(100),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    error_stack_trace TEXT
);

CREATE INDEX IF NOT EXISTS idx_execution_report ON report_executions(report_id);
CREATE INDEX IF NOT EXISTS idx_execution_tenant ON report_executions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_execution_status ON report_executions(status);
CREATE INDEX IF NOT EXISTS idx_execution_started ON report_executions(started_at);

--changeset analytics:005-create-metric-snapshots-table
CREATE TABLE IF NOT EXISTS metric_snapshots (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(15, 4),
    dimensions JSONB,
    breakdown JSONB,
    snapshot_date DATE NOT NULL,
    period_start DATE,
    period_end DATE,
    sample_size INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_snapshot_tenant ON metric_snapshots(tenant_id);
CREATE INDEX IF NOT EXISTS idx_snapshot_type ON metric_snapshots(metric_type);
CREATE INDEX IF NOT EXISTS idx_snapshot_date ON metric_snapshots(snapshot_date);
CREATE INDEX IF NOT EXISTS idx_snapshot_tenant_type_date ON metric_snapshots(tenant_id, metric_type, snapshot_date);

--changeset analytics:006-create-alert-rules-table
CREATE TABLE IF NOT EXISTS alert_rules (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    metric_type VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100),
    condition_operator VARCHAR(20) NOT NULL,
    threshold_value DECIMAL(15, 4) NOT NULL,
    secondary_threshold DECIMAL(15, 4),
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    notification_channels JSONB,
    filters JSONB,
    cooldown_minutes INTEGER DEFAULT 60,
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_triggered_at TIMESTAMP,
    trigger_count INTEGER DEFAULT 0,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_alert_tenant ON alert_rules(tenant_id);
CREATE INDEX IF NOT EXISTS idx_alert_metric_type ON alert_rules(metric_type);
CREATE INDEX IF NOT EXISTS idx_alert_active ON alert_rules(is_active);
