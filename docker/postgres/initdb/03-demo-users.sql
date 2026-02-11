-- Demo Users for HDIM Demo Sandbox
-- Passwords:
-- demo123 (BCrypt hashed)
-- Generated: BCryptPasswordEncoder.encode("demo123")
-- Hash: $2a$10$ZMMI78ekTFEdm4fZqaKS.OFWaa.kLBFFydORaLLhUq4LLGtTKE96S
-- password123 (BCrypt hashed)
-- Hash: $2b$12$fTxpaiGtPPcYpMhxQwz0J.jIdPMG8Sq77PtB4FSE0c7X2/m/2Ek9q
--
-- NOTE: Docker's postgres entrypoint runs this script against the POSTGRES_DB (healthdata_db)
-- We need to switch to gateway_db first
\c gateway_db

-- Create demo users with BCrypt hashed password (demo123)
INSERT INTO users (id, email, username, password_hash, first_name, last_name, active, email_verified, mfa_enabled, created_at, updated_at) VALUES
('550e8400-e29b-41d4-a716-446655440010', 'demo_admin@hdim.ai', 'demo_admin', '$2a$10$ZMMI78ekTFEdm4fZqaKS.OFWaa.kLBFFydORaLLhUq4LLGtTKE96S', 'Demo', 'Admin', true, false, false, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440011', 'demo_analyst@hdim.ai', 'demo_analyst', '$2a$10$ZMMI78ekTFEdm4fZqaKS.OFWaa.kLBFFydORaLLhUq4LLGtTKE96S', 'Demo', 'Analyst', true, false, false, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440012', 'demo_viewer@hdim.ai', 'demo_viewer', '$2a$10$ZMMI78ekTFEdm4fZqaKS.OFWaa.kLBFFydORaLLhUq4LLGtTKE96S', 'Demo', 'Viewer', true, false, false, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440013', 'test_admin@hdim.ai', 'test_admin', '$2b$12$fTxpaiGtPPcYpMhxQwz0J.jIdPMG8Sq77PtB4FSE0c7X2/m/2Ek9q', 'Test', 'Admin', true, false, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Create user roles
INSERT INTO user_roles (user_id, role) VALUES
('550e8400-e29b-41d4-a716-446655440010', 'ADMIN'),
('550e8400-e29b-41d4-a716-446655440010', 'EVALUATOR'),
('550e8400-e29b-41d4-a716-446655440011', 'ANALYST'),
('550e8400-e29b-41d4-a716-446655440011', 'EVALUATOR'),
('550e8400-e29b-41d4-a716-446655440012', 'VIEWER'),
('550e8400-e29b-41d4-a716-446655440013', 'ADMIN')
ON CONFLICT DO NOTHING;

-- Map demo users to demo tenant(s)
INSERT INTO user_tenants (user_id, tenant_id) VALUES
('550e8400-e29b-41d4-a716-446655440010', 'acme-health'),
('550e8400-e29b-41d4-a716-446655440011', 'acme-health'),
('550e8400-e29b-41d4-a716-446655440012', 'acme-health'),
('550e8400-e29b-41d4-a716-446655440013', 'acme-health')
ON CONFLICT DO NOTHING;
