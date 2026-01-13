-- Create prior_auth_requests table
-- Implements Da Vinci PAS (Prior Authorization Support) specification
CREATE TABLE prior_auth.prior_auth_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    patient_id UUID NOT NULL,
    pa_request_id VARCHAR(255) UNIQUE,
    service_code VARCHAR(128),
    service_description VARCHAR(500),
    urgency VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    payer_id VARCHAR(255),
    payer_name VARCHAR(255),
    provider_id VARCHAR(255),
    provider_npi VARCHAR(10),
    facility_id VARCHAR(255),
    diagnosis_codes VARCHAR(500),
    procedure_codes VARCHAR(500),
    quantity_requested INTEGER,
    quantity_approved INTEGER,
    claim_bundle_json JSONB,
    claim_response_json JSONB,
    supporting_info_json JSONB,
    submitted_at TIMESTAMP,
    sla_deadline TIMESTAMP,
    decision_at TIMESTAMP,
    decision_reason VARCHAR(1000),
    auth_number VARCHAR(100),
    auth_effective_date TIMESTAMP,
    auth_expiration_date TIMESTAMP,
    payer_tracking_id VARCHAR(255),
    retry_count INTEGER DEFAULT 0,
    last_error VARCHAR(1000),
    requested_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_pa_tenant_patient ON prior_auth.prior_auth_requests(tenant_id, patient_id);
CREATE INDEX idx_pa_status ON prior_auth.prior_auth_requests(status);
CREATE INDEX idx_pa_payer ON prior_auth.prior_auth_requests(payer_id);
CREATE INDEX idx_pa_request_id ON prior_auth.prior_auth_requests(pa_request_id);
CREATE INDEX idx_pa_sla_deadline ON prior_auth.prior_auth_requests(sla_deadline);
