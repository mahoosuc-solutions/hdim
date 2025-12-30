-- Demo Users for HDIM Demo Sandbox
-- Password for all users: Demo123! (BCrypt hashed)

-- Create demo tenant
INSERT INTO tenants (id, name, created_at, updated_at) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'DEMO001', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Create demo users with BCrypt hashed password (Demo123!)
-- Hash: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
INSERT INTO users (id, tenant_id, email, username, password_hash, first_name, last_name, role, active, created_at, updated_at) VALUES
('550e8400-e29b-41d4-a716-446655440010', '550e8400-e29b-41d4-a716-446655440001', 'demo_admin@hdim.ai', 'demo_admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Demo', 'Admin', 'ADMIN', true, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440011', '550e8400-e29b-41d4-a716-446655440001', 'demo_analyst@hdim.ai', 'demo_analyst', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Demo', 'Analyst', 'ANALYST', true, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440012', '550e8400-e29b-41d4-a716-446655440001', 'demo_viewer@hdim.ai', 'demo_viewer', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Demo', 'Viewer', 'VIEWER', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Create user roles
INSERT INTO user_roles (user_id, role) VALUES
('550e8400-e29b-41d4-a716-446655440010', 'ADMIN'),
('550e8400-e29b-41d4-a716-446655440010', 'EVALUATOR'),
('550e8400-e29b-41d4-a716-446655440011', 'ANALYST'),
('550e8400-e29b-41d4-a716-446655440011', 'EVALUATOR'),
('550e8400-e29b-41d4-a716-446655440012', 'VIEWER')
ON CONFLICT DO NOTHING;
