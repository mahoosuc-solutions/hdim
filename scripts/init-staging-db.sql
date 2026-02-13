-- Staging Database Initialization Script
-- Initializes customer_deployments_db with realistic test data
-- Run automatically by docker-compose.staging.sales-agents.yml
--
-- Tables:
-- - lc_deployments: Customer deployment records
-- - lc_call_transcripts: Call metadata and analytics
-- - lc_coaching_sessions: Coaching effectiveness tracking

-- ============================================================================
-- Staging Tenants (for testing multi-tenant isolation)
-- ============================================================================

-- Data for lc_deployments table (5 realistic staging customers)
INSERT INTO lc_deployments (
    id,
    tenant_id,
    customer_name,
    deployment_status,
    contract_value,
    pilot_start_date,
    pilot_end_date,
    success_metrics,
    created_at
) VALUES
(
    '550e8400-e29b-41d4-a716-446655440001',
    'staging-tenant-001',
    'Blue Cross Insurance Co',
    'active',
    75000.00,
    '2026-02-01'::timestamp,
    '2026-05-01'::timestamp,
    '{"target_discovery_calls": 100, "target_win_rate": 0.3, "target_roi": "5x"}'::jsonb,
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440002',
    'staging-tenant-002',
    'Aetna Health Plans',
    'active',
    100000.00,
    '2026-02-15'::timestamp,
    '2026-05-15'::timestamp,
    '{"target_discovery_calls": 150, "target_win_rate": 0.35, "target_roi": "6x"}'::jsonb,
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440003',
    'staging-tenant-003',
    'United Healthcare Systems',
    'pilot',
    50000.00,
    '2026-02-20'::timestamp,
    '2026-04-20'::timestamp,
    '{"target_discovery_calls": 50, "target_win_rate": 0.25, "target_roi": "4x"}'::jsonb,
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440004',
    'staging-tenant-004',
    'Humana Insurance Group',
    'onboarding',
    60000.00,
    '2026-03-01'::timestamp,
    '2026-06-01'::timestamp,
    '{"target_discovery_calls": 75, "target_win_rate": 0.28, "target_roi": "5x"}'::jsonb,
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440005',
    'staging-tenant-005',
    'Cigna Health Services',
    'evaluation',
    40000.00,
    '2026-03-10'::timestamp,
    '2026-05-10'::timestamp,
    '{"target_discovery_calls": 40, "target_win_rate": 0.22, "target_roi": "3x"}'::jsonb,
    NOW()
);

-- Data for lc_call_transcripts table (8 realistic staging calls)
INSERT INTO lc_call_transcripts (
    id,
    tenant_id,
    deployment_id,
    call_date,
    duration_minutes,
    persona_type,
    qualification_status,
    call_score,
    sentiment_score,
    transcript_file_path,
    pain_points_discovered,
    created_at
) VALUES
-- Blue Cross Insurance Co calls
(
    '660e8400-e29b-41d4-a716-446655440001',
    'staging-tenant-001',
    '550e8400-e29b-41d4-a716-446655440001',
    NOW() - INTERVAL '3 days',
    28,
    'cmo',
    'qualified',
    8.5,
    0.82,
    '/transcripts/staging-tenant-001/call_660e8400.json',
    '{"primary": "care_gap_identification", "secondary": ["quality_measure_coverage", "population_health"]}'::jsonb,
    NOW()
),
(
    '660e8400-e29b-41d4-a716-446655440002',
    'staging-tenant-001',
    '550e8400-e29b-41d4-a716-446655440001',
    NOW() - INTERVAL '2 days',
    35,
    'coordinator',
    'qualified',
    7.8,
    0.75,
    '/transcripts/staging-tenant-001/call_660e8400-002.json',
    '{"primary": "implementation_timeline", "secondary": ["integration_complexity", "team_capacity"]}'::jsonb,
    NOW()
),
-- Aetna Health Plans calls
(
    '660e8400-e29b-41d4-a716-446655440003',
    'staging-tenant-002',
    '550e8400-e29b-41d4-a716-446655440002',
    NOW() - INTERVAL '5 days',
    42,
    'cfo',
    'qualified',
    9.1,
    0.88,
    '/transcripts/staging-tenant-002/call_660e8400-003.json',
    '{"primary": "roi_measurement", "secondary": ["cost_savings", "revenue_impact"]}'::jsonb,
    NOW()
),
(
    '660e8400-e29b-41d4-a716-446655440004',
    'staging-tenant-002',
    '550e8400-e29b-41d4-a716-446655440002',
    NOW() - INTERVAL '4 days',
    31,
    'it_leader',
    'needs_discovery',
    6.5,
    0.68,
    '/transcripts/staging-tenant-002/call_660e8400-004.json',
    '{"primary": "technical_integration", "secondary": ["api_compatibility", "security_concerns"]}'::jsonb,
    NOW()
),
-- United Healthcare Systems calls
(
    '660e8400-e29b-41d4-a716-446655440005',
    'staging-tenant-003',
    '550e8400-e29b-41d4-a716-446655440003',
    NOW() - INTERVAL '7 days',
    26,
    'provider',
    'unqualified',
    5.2,
    0.45,
    '/transcripts/staging-tenant-003/call_660e8400-005.json',
    '{"primary": "not_interested", "secondary": ["timing_wrong", "budget_constraints"]}'::jsonb,
    NOW()
),
-- Humana Insurance Group calls
(
    '660e8400-e29b-41d4-a716-446655440006',
    'staging-tenant-004',
    '550e8400-e29b-41d4-a716-446655440004',
    NOW() - INTERVAL '1 day',
    39,
    'cmo',
    'qualified',
    8.7,
    0.80,
    '/transcripts/staging-tenant-004/call_660e8400-006.json',
    '{"primary": "hedis_measures", "secondary": ["gap_closure_rate", "member_engagement"]}'::jsonb,
    NOW()
),
-- Cigna Health Services calls
(
    '660e8400-e29b-41d4-a716-446655440007',
    'staging-tenant-005',
    '550e8400-e29b-41d4-a716-446655440005',
    NOW() - INTERVAL '6 days',
    24,
    'coordinator',
    'needs_discovery',
    7.1,
    0.71,
    '/transcripts/staging-tenant-005/call_660e8400-007.json',
    '{"primary": "workflow_integration", "secondary": ["staff_training", "change_management"]}'::jsonb,
    NOW()
);

-- Data for lc_coaching_sessions table (coaching effectiveness tracking)
INSERT INTO lc_coaching_sessions (
    id,
    tenant_id,
    call_transcript_id,
    session_type,
    coaching_count,
    objections_detected,
    phase_transitions,
    avg_response_score,
    effectiveness_rating,
    created_at
) VALUES
(
    '770e8400-e29b-41d4-a716-446655440001',
    'staging-tenant-001',
    '660e8400-e29b-41d4-a716-446655440001',
    'discovery_call',
    12,
    3,
    2,
    8.3,
    4.2,
    NOW()
),
(
    '770e8400-e29b-41d4-a716-446655440002',
    'staging-tenant-001',
    '660e8400-e29b-41d4-a716-446655440002',
    'discovery_call',
    15,
    4,
    3,
    7.9,
    4.0,
    NOW()
),
(
    '770e8400-e29b-41d4-a716-446655440003',
    'staging-tenant-002',
    '660e8400-e29b-41d4-a716-446655440003',
    'discovery_call',
    18,
    5,
    3,
    8.8,
    4.5,
    NOW()
),
(
    '770e8400-e29b-41d4-a716-446655440004',
    'staging-tenant-002',
    '660e8400-e29b-41d4-a716-446655440004',
    'discovery_call',
    10,
    2,
    1,
    6.7,
    3.5,
    NOW()
),
(
    '770e8400-e29b-41d4-a716-446655440005',
    'staging-tenant-003',
    '660e8400-e29b-41d4-a716-446655440005',
    'discovery_call',
    8,
    6,
    0,
    5.1,
    2.8,
    NOW()
),
(
    '770e8400-e29b-41d4-a716-446655440006',
    'staging-tenant-004',
    '660e8400-e29b-41d4-a716-446655440006',
    'discovery_call',
    14,
    3,
    2,
    8.5,
    4.1,
    NOW()
),
(
    '770e8400-e29b-41d4-a716-446655440007',
    'staging-tenant-005',
    '660e8400-e29b-41d4-a716-446655440007',
    'discovery_call',
    11,
    2,
    2,
    7.2,
    3.8,
    NOW()
);

-- ============================================================================
-- Create Indexes for Performance
-- ============================================================================

-- Multi-tenant indexes
CREATE INDEX idx_lc_deployments_tenant ON lc_deployments(tenant_id);
CREATE INDEX idx_lc_call_transcripts_tenant ON lc_call_transcripts(tenant_id);
CREATE INDEX idx_lc_coaching_sessions_tenant ON lc_coaching_sessions(tenant_id);

-- Query optimization indexes
CREATE INDEX idx_lc_call_transcripts_deployment ON lc_call_transcripts(deployment_id);
CREATE INDEX idx_lc_call_transcripts_date ON lc_call_transcripts(call_date DESC);
CREATE INDEX idx_lc_coaching_sessions_call ON lc_coaching_sessions(call_transcript_id);

-- ============================================================================
-- Verify Data Integrity
-- ============================================================================

-- Verify deployments created
SELECT COUNT(*) as deployment_count FROM lc_deployments;

-- Verify transcripts created
SELECT COUNT(*) as transcript_count FROM lc_call_transcripts;

-- Verify coaching sessions created
SELECT COUNT(*) as coaching_count FROM lc_coaching_sessions;

-- Sample staging data for verification
SELECT
    d.customer_name,
    COUNT(DISTINCT c.id) as call_count,
    COUNT(DISTINCT cs.id) as coaching_sessions,
    ROUND(AVG(c.call_score)::numeric, 2) as avg_call_score,
    ROUND(AVG(c.sentiment_score)::numeric, 2) as avg_sentiment
FROM lc_deployments d
LEFT JOIN lc_call_transcripts c ON d.id = c.deployment_id
LEFT JOIN lc_coaching_sessions cs ON c.id = cs.call_transcript_id
GROUP BY d.id, d.customer_name
ORDER BY d.created_at DESC;
