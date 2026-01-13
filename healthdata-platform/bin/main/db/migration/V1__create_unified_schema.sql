-- HealthData Platform - Unified Database Schema
-- Single database with logical separation via schemas
-- This replaces 6 separate databases with 1 consolidated database

-- Create schemas for logical separation
CREATE SCHEMA IF NOT EXISTS patient;
CREATE SCHEMA IF NOT EXISTS fhir;
CREATE SCHEMA IF NOT EXISTS quality;
CREATE SCHEMA IF NOT EXISTS caregap;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS audit;

-- ==================== PATIENT SCHEMA ====================

-- Core patient table
CREATE TABLE patient.patients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mrn VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,

    -- Address (embedded)
    street VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(50),
    postal_code VARCHAR(20),
    country VARCHAR(100),

    phone_number VARCHAR(50),
    email VARCHAR(255),
    tenant_id VARCHAR(50) NOT NULL,
    active BOOLEAN DEFAULT true,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,

    CONSTRAINT chk_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'UNKNOWN'))
);

-- Patient identifiers
CREATE TABLE patient.patient_identifiers (
    patient_id UUID REFERENCES patient.patients(id) ON DELETE CASCADE,
    system VARCHAR(255) NOT NULL,
    value VARCHAR(255) NOT NULL,
    type VARCHAR(100),
    PRIMARY KEY (patient_id, system, value)
);

-- Indexes for performance
CREATE INDEX idx_patients_mrn ON patient.patients(mrn);
CREATE INDEX idx_patients_tenant ON patient.patients(tenant_id);
CREATE INDEX idx_patients_name ON patient.patients(last_name, first_name);
CREATE INDEX idx_patients_active_tenant ON patient.patients(tenant_id, active) WHERE active = true;

-- ==================== FHIR SCHEMA ====================

-- FHIR Observations
CREATE TABLE fhir.observations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    system VARCHAR(255),
    display VARCHAR(255),
    value_quantity DECIMAL(10, 2),
    value_unit VARCHAR(50),
    value_string TEXT,
    status VARCHAR(50) NOT NULL,
    effective_date TIMESTAMP,
    category VARCHAR(100),
    tenant_id VARCHAR(50) NOT NULL,
    fhir_resource JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_obs_patient FOREIGN KEY (patient_id)
        REFERENCES patient.patients(id) ON DELETE CASCADE
);

-- FHIR Conditions
CREATE TABLE fhir.conditions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    system VARCHAR(255),
    display VARCHAR(255),
    clinical_status VARCHAR(50),
    verification_status VARCHAR(50),
    onset_date DATE,
    abatement_date DATE,
    tenant_id VARCHAR(50) NOT NULL,
    fhir_resource JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cond_patient FOREIGN KEY (patient_id)
        REFERENCES patient.patients(id) ON DELETE CASCADE
);

-- FHIR Medications
CREATE TABLE fhir.medication_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    medication_code VARCHAR(50),
    medication_display VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    intent VARCHAR(50),
    authored_on TIMESTAMP,
    dosage_instruction TEXT,
    tenant_id VARCHAR(50) NOT NULL,
    fhir_resource JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_med_patient FOREIGN KEY (patient_id)
        REFERENCES patient.patients(id) ON DELETE CASCADE
);

-- FHIR indexes
CREATE INDEX idx_obs_patient_code ON fhir.observations(patient_id, code);
CREATE INDEX idx_obs_effective ON fhir.observations(effective_date);
CREATE INDEX idx_obs_tenant ON fhir.observations(tenant_id);
CREATE INDEX idx_obs_fhir_gin ON fhir.observations USING gin(fhir_resource);

CREATE INDEX idx_cond_patient ON fhir.conditions(patient_id);
CREATE INDEX idx_cond_code ON fhir.conditions(code);
CREATE INDEX idx_cond_tenant ON fhir.conditions(tenant_id);

CREATE INDEX idx_med_patient ON fhir.medication_requests(patient_id);
CREATE INDEX idx_med_status ON fhir.medication_requests(status);
CREATE INDEX idx_med_tenant ON fhir.medication_requests(tenant_id);

-- ==================== QUALITY SCHEMA ====================

-- Quality Measures
CREATE TABLE quality.measures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    measure_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    version VARCHAR(20),
    cql_library TEXT,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Measure Results
CREATE TABLE quality.measure_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    measure_id VARCHAR(50) NOT NULL,
    score DECIMAL(5, 2),
    numerator INTEGER,
    denominator INTEGER,
    compliant BOOLEAN,
    calculation_date TIMESTAMP NOT NULL,
    period_start DATE,
    period_end DATE,
    tenant_id VARCHAR(50) NOT NULL,
    details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_result_patient FOREIGN KEY (patient_id)
        REFERENCES patient.patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_result_measure FOREIGN KEY (measure_id)
        REFERENCES quality.measures(measure_id)
);

-- Health Scores
CREATE TABLE quality.health_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    overall_score DECIMAL(5, 2) NOT NULL,
    clinical_score DECIMAL(5, 2),
    preventive_score DECIMAL(5, 2),
    medication_score DECIMAL(5, 2),
    calculated_at TIMESTAMP NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    score_components JSONB,

    CONSTRAINT fk_score_patient FOREIGN KEY (patient_id)
        REFERENCES patient.patients(id) ON DELETE CASCADE
);

-- Quality indexes
CREATE INDEX idx_results_patient_measure ON quality.measure_results(patient_id, measure_id);
CREATE INDEX idx_results_calculation ON quality.measure_results(calculation_date DESC);
CREATE INDEX idx_results_tenant ON quality.measure_results(tenant_id);
CREATE INDEX idx_scores_patient ON quality.health_scores(patient_id, calculated_at DESC);
CREATE INDEX idx_scores_tenant ON quality.health_scores(tenant_id);

-- ==================== CARE GAP SCHEMA ====================

-- Care Gaps
CREATE TABLE caregap.care_gaps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    measure_id VARCHAR(50) NOT NULL,
    gap_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20),
    identified_date TIMESTAMP NOT NULL,
    due_date DATE,
    closed_date TIMESTAMP,
    closure_reason VARCHAR(255),
    tenant_id VARCHAR(50) NOT NULL,
    metadata JSONB,

    CONSTRAINT fk_gap_patient FOREIGN KEY (patient_id)
        REFERENCES patient.patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_gap_measure FOREIGN KEY (measure_id)
        REFERENCES quality.measures(measure_id),
    CONSTRAINT chk_gap_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'CLOSED', 'DEFERRED'))
);

-- Care Gap Interventions
CREATE TABLE caregap.interventions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    care_gap_id UUID NOT NULL,
    intervention_type VARCHAR(100) NOT NULL,
    description TEXT,
    performed_by VARCHAR(255),
    performed_date TIMESTAMP NOT NULL,
    outcome VARCHAR(100),
    notes TEXT,

    CONSTRAINT fk_intervention_gap FOREIGN KEY (care_gap_id)
        REFERENCES caregap.care_gaps(id) ON DELETE CASCADE
);

-- Care Gap indexes
CREATE INDEX idx_gaps_patient_status ON caregap.care_gaps(patient_id, status);
CREATE INDEX idx_gaps_measure ON caregap.care_gaps(measure_id);
CREATE INDEX idx_gaps_due_date ON caregap.care_gaps(due_date) WHERE status = 'OPEN';
CREATE INDEX idx_gaps_tenant ON caregap.care_gaps(tenant_id);

-- ==================== NOTIFICATION SCHEMA ====================

-- Notification Templates
CREATE TABLE notification.templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    subject VARCHAR(255),
    body TEXT NOT NULL,
    variables JSONB,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_channel CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP'))
);

-- Notification History
CREATE TABLE notification.history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id VARCHAR(100) NOT NULL,
    patient_id UUID,
    tenant_id VARCHAR(50) NOT NULL,
    template_id VARCHAR(100),
    channel VARCHAR(50) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    content TEXT,
    status VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failed_at TIMESTAMP,
    error_message TEXT,
    metadata JSONB,

    CONSTRAINT fk_notif_patient FOREIGN KEY (patient_id)
        REFERENCES patient.patients(id) ON DELETE SET NULL,
    CONSTRAINT fk_notif_template FOREIGN KEY (template_id)
        REFERENCES notification.templates(template_id)
);

-- Notification indexes
CREATE INDEX idx_notif_patient ON notification.history(patient_id);
CREATE INDEX idx_notif_status ON notification.history(status);
CREATE INDEX idx_notif_sent ON notification.history(sent_at DESC);
CREATE INDEX idx_notif_tenant ON notification.history(tenant_id);

-- ==================== AUDIT SCHEMA ====================

-- Audit Log for compliance
CREATE TABLE audit.audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    user_id VARCHAR(100),
    user_name VARCHAR(255),
    tenant_id VARCHAR(50),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    old_values JSONB,
    new_values JSONB,
    metadata JSONB
);

-- Audit indexes
CREATE INDEX idx_audit_entity ON audit.audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_user ON audit.audit_log(user_id);
CREATE INDEX idx_audit_timestamp ON audit.audit_log(timestamp DESC);
CREATE INDEX idx_audit_tenant ON audit.audit_log(tenant_id);

-- ==================== INITIAL DATA ====================

-- Insert default quality measures
INSERT INTO quality.measures (measure_id, name, description, category, version) VALUES
('HbA1c-Control', 'Diabetes: HbA1c Control', 'Percentage of patients with diabetes who have HbA1c < 8%', 'Clinical', '2024.1'),
('BP-Control', 'Blood Pressure Control', 'Percentage of patients with hypertension who have BP < 140/90', 'Clinical', '2024.1'),
('Medication-Adherence', 'Medication Adherence', 'Percentage of patients adhering to prescribed medications', 'Process', '2024.1'),
('Preventive-Screening', 'Preventive Care Screening', 'Completion of age-appropriate preventive screenings', 'Preventive', '2024.1'),
('Mental-Health-Screen', 'Depression Screening', 'Annual depression screening for adults', 'Behavioral', '2024.1');

-- Insert default notification templates
INSERT INTO notification.templates (template_id, name, channel, subject, body) VALUES
('care-gap-reminder', 'Care Gap Reminder', 'EMAIL', 'You have an upcoming care requirement',
 'Dear {{patientName}}, you have a care gap for {{measureName}} that needs attention.'),
('appointment-reminder', 'Appointment Reminder', 'SMS', NULL,
 'Reminder: You have an appointment on {{date}} at {{time}}. Reply CONFIRM to confirm.'),
('measure-result', 'Quality Measure Result', 'EMAIL', 'Your Health Score Update',
 'Your recent health assessment score is {{score}}. {{recommendations}}');

-- ==================== MIGRATION NOTES ====================
-- This schema consolidates 6 databases into 1:
-- 1. patient_db → patient schema
-- 2. fhir_db → fhir schema
-- 3. quality_db → quality schema
-- 4. caregap_db → caregap schema
-- 5. event_db → integrated as JSONB in relevant tables
-- 6. Notification → notification schema

-- Benefits:
-- - Single connection pool (30 connections vs 180)
-- - Shared transactions across all modules
-- - Simplified backup and recovery
-- - Better referential integrity
-- - Reduced operational complexity