-- ================================================
-- NYC Demo Tenants for HealthData In Motion
-- ================================================

-- Ensure base test users exist (run backend/test-users.sql first).

-- Major NYC hospital tenants for demo
WITH tenants AS (
    SELECT UNNEST(ARRAY[
        'nyc-mount-sinai',
        'nyc-nyu-langone',
        'nyc-nyc-health-hospitals',
        'nyc-newyork-presbyterian',
        'nyc-montefiore'
    ]) AS tenant_id
),
demo_users AS (
    SELECT id FROM users WHERE username IN (
        'test_superadmin',
        'test_admin',
        'test_evaluator',
        'test_analyst',
        'test_viewer'
    )
)
INSERT INTO user_tenants (user_id, tenant_id)
SELECT u.id, t.tenant_id
FROM demo_users u
CROSS JOIN tenants t
ON CONFLICT DO NOTHING;

-- Quick verification
SELECT tenant_id, COUNT(*) AS users
FROM user_tenants
WHERE tenant_id LIKE 'nyc-%'
GROUP BY tenant_id
ORDER BY tenant_id;
