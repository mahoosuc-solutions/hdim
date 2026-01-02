-- ================================================
-- HealthData In Motion - Test Users
-- Password for all users: password123
-- ================================================

-- Clear existing test users
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'test_%');
DELETE FROM user_tenants WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'test_%');
DELETE FROM users WHERE username LIKE 'test_%';

-- test_superadmin: Full system access
INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, email_verified, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'test_superadmin',
    'superadmin@test.com',
    '$2b$10$h8pkIMdAPk.cFG7SCXheJewO/gq4VpnvcDFYXiRXdJ8sAZqFnoPOi',
    'Test',
    'SuperAdmin',
    true,  -- active
    true,  -- email_verified
    NOW(),
    NOW()
) ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'SUPER_ADMIN' FROM users WHERE username = 'test_superadmin'
ON CONFLICT DO NOTHING;

INSERT INTO user_tenants (user_id, tenant_id)
SELECT id, 'default' FROM users WHERE username = 'test_superadmin'
ON CONFLICT DO NOTHING;

-- test_admin: Administrative access
INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, email_verified, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'test_admin',
    'admin@test.com',
    '$2b$10$h8pkIMdAPk.cFG7SCXheJewO/gq4VpnvcDFYXiRXdJ8sAZqFnoPOi',
    'Test',
    'Admin',
    true,  -- active
    true,  -- email_verified
    NOW(),
    NOW()
) ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE username = 'test_admin'
ON CONFLICT DO NOTHING;

INSERT INTO user_tenants (user_id, tenant_id)
SELECT id, 'default' FROM users WHERE username = 'test_admin'
ON CONFLICT DO NOTHING;

-- test_evaluator: CQL evaluation and measure calculation
INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, email_verified, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'test_evaluator',
    'evaluator@test.com',
    '$2b$10$h8pkIMdAPk.cFG7SCXheJewO/gq4VpnvcDFYXiRXdJ8sAZqFnoPOi',
    'Test',
    'Evaluator',
    true,  -- active
    true,  -- email_verified
    NOW(),
    NOW()
) ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'EVALUATOR' FROM users WHERE username = 'test_evaluator'
ON CONFLICT DO NOTHING;

INSERT INTO user_tenants (user_id, tenant_id)
SELECT id, 'default' FROM users WHERE username = 'test_evaluator'
ON CONFLICT DO NOTHING;

-- test_analyst: Quality analyst - reporting and metrics
INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, email_verified, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'test_analyst',
    'analyst@test.com',
    '$2b$10$h8pkIMdAPk.cFG7SCXheJewO/gq4VpnvcDFYXiRXdJ8sAZqFnoPOi',
    'Test',
    'Analyst',
    true,  -- active
    true,  -- email_verified
    NOW(),
    NOW()
) ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'ANALYST' FROM users WHERE username = 'test_analyst'
ON CONFLICT DO NOTHING;

INSERT INTO user_tenants (user_id, tenant_id)
SELECT id, 'default' FROM users WHERE username = 'test_analyst'
ON CONFLICT DO NOTHING;

-- test_viewer: Read-only access to reports and data
INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, email_verified, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'test_viewer',
    'viewer@test.com',
    '$2b$10$h8pkIMdAPk.cFG7SCXheJewO/gq4VpnvcDFYXiRXdJ8sAZqFnoPOi',
    'Test',
    'Viewer',
    true,  -- active
    true,  -- email_verified
    NOW(),
    NOW()
) ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'VIEWER' FROM users WHERE username = 'test_viewer'
ON CONFLICT DO NOTHING;

INSERT INTO user_tenants (user_id, tenant_id)
SELECT id, 'default' FROM users WHERE username = 'test_viewer'
ON CONFLICT DO NOTHING;

-- test_multiuser: User with multiple roles for testing
INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, email_verified, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'test_multiuser',
    'multi@test.com',
    '$2b$10$h8pkIMdAPk.cFG7SCXheJewO/gq4VpnvcDFYXiRXdJ8sAZqFnoPOi',
    'Test',
    'MultiRole',
    true,  -- active
    true,  -- email_verified
    NOW(),
    NOW()
) ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE username = 'test_multiuser'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'ANALYST' FROM users WHERE username = 'test_multiuser'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'EVALUATOR' FROM users WHERE username = 'test_multiuser'
ON CONFLICT DO NOTHING;

INSERT INTO user_tenants (user_id, tenant_id)
SELECT id, 'default' FROM users WHERE username = 'test_multiuser'
ON CONFLICT DO NOTHING;


-- Verify users created
SELECT
    u.username,
    u.email,
    u.first_name || ' ' || u.last_name as full_name,
    string_agg(DISTINCT ur.role, ', ') as roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
WHERE u.username LIKE 'test_%'
GROUP BY u.username, u.email, u.first_name, u.last_name
ORDER BY u.username;
