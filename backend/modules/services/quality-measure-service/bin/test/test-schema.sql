-- Test Schema Initialization for Notification E2E Tests
-- This script creates the minimal schema needed for notification system testing
-- without depending on Liquibase timing issues

-- Drop tables if they exist (for test isolation)
DROP TABLE IF EXISTS clinical_alerts CASCADE;
DROP TABLE IF EXISTS care_gaps CASCADE;
DROP TABLE IF EXISTS mental_health_assessments CASCADE;

-- Create mental_health_assessments table
CREATE TABLE mental_health_assessments (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    patient_id VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    score INTEGER NOT NULL,
    max_score INTEGER NOT NULL,
    severity VARCHAR(50) NOT NULL,
    interpretation TEXT NOT NULL,
    positive_screen BOOLEAN NOT NULL,
    threshold_score INTEGER NOT NULL,
    requires_followup BOOLEAN NOT NULL,
    assessed_by VARCHAR(255) NOT NULL,
    assessment_date TIMESTAMP WITH TIME ZONE NOT NULL,
    responses JSONB NOT NULL,
    clinical_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Indexes for mental_health_assessments
CREATE INDEX idx_mha_patient_date ON mental_health_assessments(patient_id, assessment_date DESC);
CREATE INDEX idx_mha_patient_type ON mental_health_assessments(patient_id, type);
CREATE INDEX idx_mha_positive_screen ON mental_health_assessments(patient_id, positive_screen);
CREATE INDEX idx_mha_tenant ON mental_health_assessments(tenant_id);
CREATE INDEX idx_mha_tenant_patient ON mental_health_assessments(tenant_id, patient_id);

-- Create care_gaps table
CREATE TABLE care_gaps (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    patient_id VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    gap_type VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    quality_measure VARCHAR(50),
    recommendation TEXT,
    evidence TEXT,
    due_date TIMESTAMP WITH TIME ZONE,
    identified_date TIMESTAMP WITH TIME ZONE NOT NULL,
    addressed_date TIMESTAMP WITH TIME ZONE,
    addressed_by VARCHAR(255),
    addressed_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Indexes for care_gaps
CREATE INDEX idx_cg_patient_status ON care_gaps(patient_id, status);
CREATE INDEX idx_cg_patient_priority ON care_gaps(patient_id, priority);
CREATE INDEX idx_cg_due_date ON care_gaps(due_date);
CREATE INDEX idx_cg_quality_measure ON care_gaps(quality_measure);
CREATE INDEX idx_cg_tenant ON care_gaps(tenant_id);
CREATE INDEX idx_cg_tenant_patient ON care_gaps(tenant_id, patient_id);
CREATE INDEX idx_cg_patient_category ON care_gaps(patient_id, category);

-- Create clinical_alerts table
CREATE TABLE clinical_alerts (
    id UUID PRIMARY KEY,
    patient_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    source_event_type VARCHAR(100),
    source_event_id VARCHAR(100),
    triggered_at TIMESTAMP NOT NULL,
    acknowledged_at TIMESTAMP,
    acknowledged_by VARCHAR(100),
    escalated BOOLEAN NOT NULL DEFAULT FALSE,
    escalated_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Check constraints for enum values
    CONSTRAINT chk_alert_type CHECK (alert_type IN ('MENTAL_HEALTH_CRISIS', 'RISK_ESCALATION', 'HEALTH_DECLINE', 'CHRONIC_DETERIORATION')),
    CONSTRAINT chk_severity CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'ACKNOWLEDGED', 'RESOLVED'))
);

-- Indexes for clinical_alerts
CREATE INDEX idx_alerts_patient_status ON clinical_alerts(tenant_id, patient_id, status);
CREATE INDEX idx_alerts_triggered_at ON clinical_alerts(triggered_at DESC);
CREATE INDEX idx_alerts_severity ON clinical_alerts(severity, status);
CREATE INDEX idx_alerts_type ON clinical_alerts(alert_type, triggered_at DESC);
CREATE INDEX idx_alerts_dedup ON clinical_alerts(tenant_id, patient_id, alert_type, triggered_at DESC);

-- Table and column comments for documentation
COMMENT ON TABLE clinical_alerts IS 'Clinical alerts for mental health crises, risk escalations, and health score declines';
COMMENT ON COLUMN clinical_alerts.alert_type IS 'Type of alert: MENTAL_HEALTH_CRISIS, RISK_ESCALATION, HEALTH_DECLINE, CHRONIC_DETERIORATION';
COMMENT ON COLUMN clinical_alerts.severity IS 'Severity level: CRITICAL (immediate), HIGH (urgent), MEDIUM (attention), LOW (monitor)';
COMMENT ON COLUMN clinical_alerts.escalated IS 'Whether alert has been escalated to higher level of care';
COMMENT ON COLUMN clinical_alerts.source_event_type IS 'Type of event that triggered alert (e.g., mental-health-assessment)';
COMMENT ON COLUMN clinical_alerts.source_event_id IS 'ID of source event (assessment ID, score change event ID, etc.)';

-- Drop notification preferences and custom measures tables if they exist
DROP TABLE IF EXISTS notification_preferences CASCADE;
DROP TABLE IF EXISTS custom_measures CASCADE;

-- Create notification_preferences table
CREATE TABLE notification_preferences (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    email_enabled BOOLEAN DEFAULT TRUE,
    sms_enabled BOOLEAN DEFAULT FALSE,
    push_enabled BOOLEAN DEFAULT TRUE,
    in_app_enabled BOOLEAN DEFAULT TRUE,
    email_address VARCHAR(255),
    phone_number VARCHAR(50),
    push_token VARCHAR(500),
    enabled_types TEXT, -- JSONB equivalent in H2
    severity_threshold VARCHAR(20) DEFAULT 'MEDIUM',
    quiet_hours_enabled BOOLEAN DEFAULT FALSE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    quiet_hours_override_critical BOOLEAN DEFAULT TRUE,
    digest_mode_enabled BOOLEAN DEFAULT FALSE,
    digest_frequency VARCHAR(20) DEFAULT 'DAILY',
    custom_settings TEXT,
    consent_given BOOLEAN DEFAULT FALSE,
    consent_date TIMESTAMP
);

-- Indexes for notification_preferences
CREATE INDEX idx_notification_pref_user ON notification_preferences(user_id);
CREATE INDEX idx_notification_pref_tenant ON notification_preferences(tenant_id);
CREATE UNIQUE INDEX uk_notification_pref_user_tenant ON notification_preferences(user_id, tenant_id);

-- Create custom_measures table
CREATE TABLE custom_measures (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    "year" INTEGER,
    cql_text TEXT,
    value_sets TEXT,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    published_date TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);

-- Indexes for custom_measures
CREATE INDEX idx_custom_measures_tenant ON custom_measures(tenant_id);
CREATE INDEX idx_custom_measures_status ON custom_measures(status);
CREATE INDEX idx_custom_measures_name ON custom_measures(name);
