-- Human-in-the-Loop Approval Tables
-- Version 1.0 - Initial schema

-- Approval requests table
CREATE TABLE approval_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL,

    -- What needs approval
    request_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(255),

    -- Request details
    action_requested VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}',
    confidence_score DECIMAL(5,4),
    risk_level VARCHAR(20) NOT NULL,

    -- Requester info
    requested_by VARCHAR(100) NOT NULL,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_service VARCHAR(100),
    correlation_id VARCHAR(255),

    -- Assignment
    assigned_to VARCHAR(100),
    assigned_at TIMESTAMP,
    assigned_role VARCHAR(100),

    -- Status tracking
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    -- Decision
    decision_by VARCHAR(100),
    decision_at TIMESTAMP,
    decision_reason TEXT,

    -- Escalation
    escalation_count INT DEFAULT 0,
    escalated_to VARCHAR(100),
    escalated_at TIMESTAMP,

    -- Expiration
    expires_at TIMESTAMP,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for approval_requests
CREATE INDEX idx_approval_tenant_status ON approval_requests(tenant_id, status, created_at);
CREATE INDEX idx_approval_assigned ON approval_requests(assigned_to, status);
CREATE INDEX idx_approval_type ON approval_requests(request_type, status);
CREATE INDEX idx_approval_expires ON approval_requests(expires_at) WHERE status IN ('PENDING', 'ASSIGNED');
CREATE INDEX idx_approval_correlation ON approval_requests(tenant_id, correlation_id);

-- Approval history table for audit trail
CREATE TABLE approval_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    approval_request_id UUID NOT NULL REFERENCES approval_requests(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for approval_history
CREATE INDEX idx_history_request ON approval_history(approval_request_id, created_at);
CREATE INDEX idx_history_action ON approval_history(action, created_at);

-- Comments
COMMENT ON TABLE approval_requests IS 'Human-in-the-Loop approval requests for HDIM platform';
COMMENT ON TABLE approval_history IS 'Audit trail for all approval request state changes';

COMMENT ON COLUMN approval_requests.request_type IS 'Type of request: AGENT_ACTION, GUARDRAIL_REVIEW, DATA_MUTATION, EXPORT, WORKFLOW_DEPLOY, DLQ_REPROCESS, CONSENT_CHANGE, EMERGENCY_ACCESS';
COMMENT ON COLUMN approval_requests.risk_level IS 'Risk classification: LOW, MEDIUM, HIGH, CRITICAL';
COMMENT ON COLUMN approval_requests.status IS 'Workflow status: PENDING, ASSIGNED, APPROVED, REJECTED, EXPIRED, ESCALATED';
COMMENT ON COLUMN approval_requests.payload IS 'Full context for reviewer in JSON format';
COMMENT ON COLUMN approval_requests.confidence_score IS 'AI confidence score (0.0000 to 1.0000) if applicable';
COMMENT ON COLUMN approval_history.action IS 'History action: CREATED, ASSIGNED, REASSIGNED, APPROVED, REJECTED, ESCALATED, EXPIRED, VIEWED, COMMENTED, REMINDER_SENT, EXECUTED';
