-- Prior Authorization Service Schema
-- CMS Interoperability and Prior Authorization Rule (CMS-0057-F)

CREATE SCHEMA IF NOT EXISTS prior_auth;

-- Prior Authorization Requests Table
CREATE TABLE prior_auth.prior_auth_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    patient_id UUID NOT NULL,
    pa_request_id VARCHAR(255) UNIQUE,
    service_code VARCHAR(128),
    service_description VARCHAR(500),
    urgency VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
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

-- Indexes for Prior Auth Requests
CREATE INDEX idx_pa_tenant_patient ON prior_auth.prior_auth_requests(tenant_id, patient_id);
CREATE INDEX idx_pa_status ON prior_auth.prior_auth_requests(status);
CREATE INDEX idx_pa_payer ON prior_auth.prior_auth_requests(payer_id);
CREATE INDEX idx_pa_request_id ON prior_auth.prior_auth_requests(pa_request_id);
CREATE INDEX idx_pa_sla_deadline ON prior_auth.prior_auth_requests(sla_deadline);
CREATE INDEX idx_pa_created_at ON prior_auth.prior_auth_requests(created_at);

-- Payer Endpoints Configuration Table
CREATE TABLE prior_auth.payer_endpoints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payer_id VARCHAR(255) NOT NULL UNIQUE,
    payer_name VARCHAR(255) NOT NULL,
    pa_endpoint_url VARCHAR(512),
    pa_fhir_base_url VARCHAR(512),
    provider_access_endpoint_url VARCHAR(512),
    provider_directory_url VARCHAR(512),
    auth_type VARCHAR(32),
    client_id VARCHAR(255),
    client_secret VARCHAR(500),
    token_endpoint_url VARCHAR(512),
    scope VARCHAR(500),
    additional_headers JSONB,
    supported_services JSONB,
    connection_timeout_ms INTEGER DEFAULT 30000,
    read_timeout_ms INTEGER DEFAULT 60000,
    max_retries INTEGER DEFAULT 3,
    is_active BOOLEAN DEFAULT TRUE,
    supports_real_time BOOLEAN DEFAULT FALSE,
    supports_batch BOOLEAN DEFAULT TRUE,
    last_health_check TIMESTAMP,
    health_status VARCHAR(32) DEFAULT 'UNKNOWN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Indexes for Payer Endpoints
CREATE INDEX idx_payer_id ON prior_auth.payer_endpoints(payer_id);
CREATE INDEX idx_payer_active ON prior_auth.payer_endpoints(is_active);

-- PA Audit Log Table
CREATE TABLE prior_auth.pa_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    pa_request_id UUID NOT NULL REFERENCES prior_auth.prior_auth_requests(id),
    action VARCHAR(50) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32),
    performed_by VARCHAR(255),
    details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pa_audit_request ON prior_auth.pa_audit_log(pa_request_id);
CREATE INDEX idx_pa_audit_tenant ON prior_auth.pa_audit_log(tenant_id);

-- Insert some sample payer endpoints (for testing)
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, provider_access_endpoint_url,
    auth_type, is_active, supports_real_time
) VALUES
    ('PAYER001', 'Demo Blue Cross', 'https://api.demo-payer.com/fhir/r4',
     'https://api.demo-payer.com/provider-access', 'OAUTH2_CLIENT_CREDENTIALS', TRUE, TRUE),
    ('PAYER002', 'Demo Aetna', 'https://api.demo-aetna.com/fhir/r4',
     'https://api.demo-aetna.com/provider-access', 'SMART_ON_FHIR', TRUE, FALSE),
    ('PAYER003', 'Demo Medicare Advantage', 'https://api.demo-medicare.com/fhir/r4',
     'https://api.demo-medicare.com/provider-access', 'OAUTH2_CLIENT_CREDENTIALS', TRUE, TRUE);

-- Comments
COMMENT ON TABLE prior_auth.prior_auth_requests IS 'Prior authorization requests per Da Vinci PAS specification';
COMMENT ON TABLE prior_auth.payer_endpoints IS 'Payer API endpoint configurations for PA and Provider Access';
COMMENT ON COLUMN prior_auth.prior_auth_requests.urgency IS 'STAT (72hr SLA) or ROUTINE (7 day SLA)';
COMMENT ON COLUMN prior_auth.prior_auth_requests.status IS 'DRAFT, PENDING_SUBMISSION, SUBMITTED, PENDING_REVIEW, INFO_REQUESTED, APPROVED, PARTIALLY_APPROVED, DENIED, CANCELLED, EXPIRED, ERROR';
