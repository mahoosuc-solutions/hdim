-- Create audit_events table for HIPAA-compliant audit logging
-- This table stores all audit events for the agent-builder-service

CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    tenant_id VARCHAR(100),
    user_id VARCHAR(100),
    username VARCHAR(255),
    role VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    action VARCHAR(50),
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    outcome VARCHAR(50),
    service_name VARCHAR(100),
    method_name VARCHAR(100),
    request_path VARCHAR(500),
    purpose_of_use VARCHAR(100),
    request_payload JSONB,
    response_payload JSONB,
    error_message TEXT,
    duration_ms BIGINT,
    fhir_audit_event_id VARCHAR(255),
    encrypted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Create indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_audit_tenant_timestamp ON audit_events(tenant_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_user_timestamp ON audit_events(user_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_resource ON audit_events(resource_type, resource_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_events(timestamp);

COMMENT ON TABLE audit_events IS 'HIPAA-compliant audit log for system activity - 7 year retention required';
COMMENT ON COLUMN audit_events.timestamp IS 'UTC timestamp of the audit event';
COMMENT ON COLUMN audit_events.purpose_of_use IS 'HIPAA required - purpose for accessing PHI';
COMMENT ON COLUMN audit_events.encrypted IS 'Whether the payload fields contain encrypted data';
