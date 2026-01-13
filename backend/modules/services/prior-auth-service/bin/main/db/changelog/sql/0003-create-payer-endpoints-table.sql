-- Create payer_endpoints table
-- Stores payer-specific API endpoints and authentication details
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
    is_active BOOLEAN DEFAULT true,
    supports_real_time BOOLEAN DEFAULT false,
    supports_batch BOOLEAN DEFAULT true,
    last_health_check TIMESTAMP,
    health_status VARCHAR(32) DEFAULT 'UNKNOWN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_payer_id ON prior_auth.payer_endpoints(payer_id);
CREATE INDEX idx_payer_active ON prior_auth.payer_endpoints(is_active);
