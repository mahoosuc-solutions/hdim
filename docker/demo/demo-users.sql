-- Demo Users for HDIM Demo Sandbox
-- Password for all users: demo123 (BCrypt hashed)
-- Generated: BCryptPasswordEncoder.encode("demo123")
-- Hash: $2a$10$ZMMI78ekTFEdm4fZqaKS.OFWaa.kLBFFydORaLLhUq4LLGtTKE96S
--
-- NOTE: Docker's postgres entrypoint runs this script against the POSTGRES_DB (healthdata_db)
-- We need to switch to gateway_db first
\c gateway_db

-- Create demo users with BCrypt hashed password (demo123)
INSERT INTO users (id, email, username, password_hash, first_name, last_name, active, email_verified, mfa_enabled, created_at, updated_at) VALUES
('550e8400-e29b-41d4-a716-446655440010', 'demo_admin@hdim.ai', 'demo_admin', '$2a$10$ZMMI78ekTFEdm4fZqaKS.OFWaa.kLBFFydORaLLhUq4LLGtTKE96S', 'Demo', 'Admin', true, false, false, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440011', 'demo_analyst@hdim.ai', 'demo_analyst', '$2a$10$ZMMI78ekTFEdm4fZqaKS.OFWaa.kLBFFydORaLLhUq4LLGtTKE96S', 'Demo', 'Analyst', true, false, false, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440012', 'demo_viewer@hdim.ai', 'demo_viewer', '$2a$10$ZMMI78ekTFEdm4fZqaKS.OFWaa.kLBFFydORaLLhUq4LLGtTKE96S', 'Demo', 'Viewer', true, false, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Create user roles
INSERT INTO user_roles (user_id, role) VALUES
('550e8400-e29b-41d4-a716-446655440010', 'ADMIN'),
('550e8400-e29b-41d4-a716-446655440010', 'EVALUATOR'),
('550e8400-e29b-41d4-a716-446655440011', 'ANALYST'),
('550e8400-e29b-41d4-a716-446655440011', 'EVALUATOR'),
('550e8400-e29b-41d4-a716-446655440012', 'VIEWER')
ON CONFLICT DO NOTHING;
