-- Demo User Seed Data for Development
-- This script creates a demo user for local development and testing
--
-- Credentials:
--   Username: demo-user
--   Password: demo
--   Tenant: acme-health
--   Roles: ADMIN, ANALYST, EVALUATOR
--
-- Usage:
--   docker exec -i healthdata-postgres psql -U healthdata -d gateway_db < backend/demo-user-seed.sql
--
-- Note: This should ONLY be used in development environments

-- Create demo user
INSERT INTO users (
    id,
    username,
    email,
    password_hash,
    first_name,
    last_name,
    active,
    email_verified,
    mfa_enabled,
    failed_login_attempts,
    created_at,
    updated_at
)
VALUES (
    '00000000-0000-0000-0000-000000000001'::uuid,
    'demo-user',
    'demo@healthdata.com',
    '$2b$10$8JKAxgd8DlUIss6D8BObBuB8JXMMcfg0yDvjDl3TJGfcwGKCN9Wwu',  -- BCrypt hash of 'demo'
    'Demo',
    'User',
    true,   -- active
    true,   -- email_verified
    false,  -- mfa_enabled
    0,      -- failed_login_attempts
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    updated_at = CURRENT_TIMESTAMP;

-- Create tenant association
INSERT INTO user_tenants (user_id, tenant_id)
VALUES ('00000000-0000-0000-0000-000000000001'::uuid, 'acme-health')
ON CONFLICT DO NOTHING;

-- Assign roles (valid roles: SUPER_ADMIN, ADMIN, EVALUATOR, ANALYST, VIEWER)
INSERT INTO user_roles (user_id, role)
VALUES
    ('00000000-0000-0000-0000-000000000001'::uuid, 'ADMIN'),
    ('00000000-0000-0000-0000-000000000001'::uuid, 'EVALUATOR'),
    ('00000000-0000-0000-0000-000000000001'::uuid, 'ANALYST')
ON CONFLICT DO NOTHING;

-- Verify creation
SELECT
    u.username,
    u.email,
    u.active,
    u.mfa_enabled,
    array_agg(DISTINCT ur.role ORDER BY ur.role) as roles,
    array_agg(DISTINCT ut.tenant_id ORDER BY ut.tenant_id) as tenants
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN user_tenants ut ON u.id = ut.user_id
WHERE u.username = 'demo-user'
GROUP BY u.id, u.username, u.email, u.active, u.mfa_enabled;
