-- HDIM E2E Test Seed Data
-- This script creates test users, tenants, and sample data for E2E testing
--
-- Usage: Automatically loaded by docker-compose.test.yml
--
-- Test Users (password: password123 - bcrypt hash):
--   test_superadmin - SUPER_ADMIN role
--   test_admin - ADMIN role
--   test_evaluator - EVALUATOR role
--   test_analyst - ANALYST role
--   test_viewer - VIEWER role

-- ============================================
-- TENANTS
-- ============================================

INSERT INTO tenants (id, name, code, status, created_at, updated_at)
VALUES
  ('550e8400-e29b-41d4-a716-446655440001', 'Acme Healthcare', 'TENANT001', 'ACTIVE', NOW(), NOW()),
  ('550e8400-e29b-41d4-a716-446655440002', 'Beta Medical Group', 'TENANT002', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- USERS
-- Password hash is bcrypt of 'password123'
-- ============================================

INSERT INTO users (id, username, email, password_hash, first_name, last_name, tenant_id, status, created_at, updated_at)
VALUES
  -- Super Admin (no tenant restriction)
  ('660e8400-e29b-41d4-a716-446655440001', 'test_superadmin', 'superadmin@test.hdim.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMye3kJPbv6z5E.3ZoM/HqL8.VJPbJrKqLu',
   'Super', 'Admin', NULL, 'ACTIVE', NOW(), NOW()),

  -- Tenant 1 Users
  ('660e8400-e29b-41d4-a716-446655440002', 'test_admin', 'admin@acme.hdim.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMye3kJPbv6z5E.3ZoM/HqL8.VJPbJrKqLu',
   'Test', 'Admin', '550e8400-e29b-41d4-a716-446655440001', 'ACTIVE', NOW(), NOW()),

  ('660e8400-e29b-41d4-a716-446655440003', 'test_evaluator', 'evaluator@acme.hdim.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMye3kJPbv6z5E.3ZoM/HqL8.VJPbJrKqLu',
   'Test', 'Evaluator', '550e8400-e29b-41d4-a716-446655440001', 'ACTIVE', NOW(), NOW()),

  ('660e8400-e29b-41d4-a716-446655440004', 'test_analyst', 'analyst@acme.hdim.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMye3kJPbv6z5E.3ZoM/HqL8.VJPbJrKqLu',
   'Test', 'Analyst', '550e8400-e29b-41d4-a716-446655440001', 'ACTIVE', NOW(), NOW()),

  ('660e8400-e29b-41d4-a716-446655440005', 'test_viewer', 'viewer@acme.hdim.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMye3kJPbv6z5E.3ZoM/HqL8.VJPbJrKqLu',
   'Test', 'Viewer', '550e8400-e29b-41d4-a716-446655440001', 'ACTIVE', NOW(), NOW()),

  -- Tenant 2 User (for multi-tenant testing)
  ('660e8400-e29b-41d4-a716-446655440006', 'test_admin_t2', 'admin@beta.hdim.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMye3kJPbv6z5E.3ZoM/HqL8.VJPbJrKqLu',
   'Beta', 'Admin', '550e8400-e29b-41d4-a716-446655440002', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- ROLES
-- ============================================

INSERT INTO roles (id, name, description, created_at)
VALUES
  ('770e8400-e29b-41d4-a716-446655440001', 'SUPER_ADMIN', 'Full system access', NOW()),
  ('770e8400-e29b-41d4-a716-446655440002', 'ADMIN', 'Tenant administrator', NOW()),
  ('770e8400-e29b-41d4-a716-446655440003', 'EVALUATOR', 'Can run evaluations', NOW()),
  ('770e8400-e29b-41d4-a716-446655440004', 'ANALYST', 'Can view reports', NOW()),
  ('770e8400-e29b-41d4-a716-446655440005', 'VIEWER', 'Read-only access', NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- USER ROLES
-- ============================================

INSERT INTO user_roles (user_id, role_id)
VALUES
  ('660e8400-e29b-41d4-a716-446655440001', '770e8400-e29b-41d4-a716-446655440001'), -- superadmin
  ('660e8400-e29b-41d4-a716-446655440002', '770e8400-e29b-41d4-a716-446655440002'), -- admin
  ('660e8400-e29b-41d4-a716-446655440003', '770e8400-e29b-41d4-a716-446655440003'), -- evaluator
  ('660e8400-e29b-41d4-a716-446655440004', '770e8400-e29b-41d4-a716-446655440004'), -- analyst
  ('660e8400-e29b-41d4-a716-446655440005', '770e8400-e29b-41d4-a716-446655440005'), -- viewer
  ('660e8400-e29b-41d4-a716-446655440006', '770e8400-e29b-41d4-a716-446655440002')  -- admin t2
ON CONFLICT DO NOTHING;

-- ============================================
-- SAMPLE PATIENTS (Tenant 1)
-- ============================================

INSERT INTO patients (id, tenant_id, fhir_id, first_name, last_name, date_of_birth, gender, mrn, status, created_at, updated_at)
VALUES
  ('880e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001',
   'patient-001', 'John', 'Smith', '1965-03-15', 'male', 'MRN001', 'ACTIVE', NOW(), NOW()),
  ('880e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001',
   'patient-002', 'Jane', 'Doe', '1978-07-22', 'female', 'MRN002', 'ACTIVE', NOW(), NOW()),
  ('880e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001',
   'patient-003', 'Robert', 'Johnson', '1955-11-08', 'male', 'MRN003', 'ACTIVE', NOW(), NOW()),
  ('880e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440001',
   'patient-004', 'Maria', 'Garcia', '1982-04-30', 'female', 'MRN004', 'ACTIVE', NOW(), NOW()),
  ('880e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440001',
   'patient-005', 'David', 'Williams', '1970-09-12', 'male', 'MRN005', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- SAMPLE PATIENTS (Tenant 2 - for isolation testing)
-- ============================================

INSERT INTO patients (id, tenant_id, fhir_id, first_name, last_name, date_of_birth, gender, mrn, status, created_at, updated_at)
VALUES
  ('880e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440002',
   'patient-t2-001', 'Alice', 'Brown', '1990-01-15', 'female', 'T2-MRN001', 'ACTIVE', NOW(), NOW()),
  ('880e8400-e29b-41d4-a716-446655440102', '550e8400-e29b-41d4-a716-446655440002',
   'patient-t2-002', 'Bob', 'Wilson', '1985-06-20', 'male', 'T2-MRN002', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- QUALITY MEASURES
-- ============================================

INSERT INTO quality_measures (id, tenant_id, measure_id, name, description, category, status, version, created_at, updated_at)
VALUES
  ('990e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001',
   'CMS122v12', 'Diabetes: Hemoglobin A1c (HbA1c) Poor Control (>9%)',
   'Percentage of patients with diabetes whose HbA1c is >9%', 'HEDIS', 'ACTIVE', '2024', NOW(), NOW()),
  ('990e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001',
   'CMS165v12', 'Controlling High Blood Pressure',
   'Percentage of patients with hypertension with adequately controlled BP', 'HEDIS', 'ACTIVE', '2024', NOW(), NOW()),
  ('990e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001',
   'CMS125v12', 'Breast Cancer Screening',
   'Percentage of women who had a mammogram in past 27 months', 'HEDIS', 'ACTIVE', '2024', NOW(), NOW()),
  ('990e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440001',
   'CMS130v12', 'Colorectal Cancer Screening',
   'Percentage of patients appropriately screened for colorectal cancer', 'HEDIS', 'ACTIVE', '2024', NOW(), NOW()),
  ('990e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440001',
   'CMS347v7', 'Statin Therapy for Cardiovascular Disease',
   'Percentage of patients with clinical ASCVD prescribed statin therapy', 'HEDIS', 'ACTIVE', '2024', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- CARE GAPS
-- ============================================

INSERT INTO care_gaps (id, tenant_id, patient_id, measure_id, gap_type, status, urgency, identified_date, due_date, created_at, updated_at)
VALUES
  ('aa0e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001',
   '880e8400-e29b-41d4-a716-446655440001', '990e8400-e29b-41d4-a716-446655440001',
   'MISSING_TEST', 'OPEN', 'HIGH', NOW() - INTERVAL '30 days', NOW() + INTERVAL '30 days', NOW(), NOW()),
  ('aa0e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001',
   '880e8400-e29b-41d4-a716-446655440002', '990e8400-e29b-41d4-a716-446655440003',
   'SCREENING_DUE', 'OPEN', 'MEDIUM', NOW() - INTERVAL '15 days', NOW() + INTERVAL '60 days', NOW(), NOW()),
  ('aa0e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001',
   '880e8400-e29b-41d4-a716-446655440003', '990e8400-e29b-41d4-a716-446655440002',
   'UNCONTROLLED', 'OPEN', 'HIGH', NOW() - INTERVAL '45 days', NOW() + INTERVAL '14 days', NOW(), NOW()),
  ('aa0e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440001',
   '880e8400-e29b-41d4-a716-446655440004', '990e8400-e29b-41d4-a716-446655440004',
   'SCREENING_DUE', 'IN_PROGRESS', 'LOW', NOW() - INTERVAL '7 days', NOW() + INTERVAL '90 days', NOW(), NOW()),
  ('aa0e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440001',
   '880e8400-e29b-41d4-a716-446655440001', '990e8400-e29b-41d4-a716-446655440005',
   'MEDICATION_GAP', 'CLOSED', 'MEDIUM', NOW() - INTERVAL '60 days', NOW() - INTERVAL '10 days', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- EVALUATIONS
-- ============================================

INSERT INTO evaluations (id, tenant_id, name, status, measure_ids, patient_count, start_date, end_date, created_by, created_at, updated_at)
VALUES
  ('bb0e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001',
   'Q4 2024 HEDIS Evaluation', 'COMPLETED',
   ARRAY['990e8400-e29b-41d4-a716-446655440001', '990e8400-e29b-41d4-a716-446655440002']::uuid[],
   5, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', '660e8400-e29b-41d4-a716-446655440003', NOW(), NOW()),
  ('bb0e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001',
   'Diabetes Care Review', 'IN_PROGRESS',
   ARRAY['990e8400-e29b-41d4-a716-446655440001']::uuid[],
   3, NOW(), NULL, '660e8400-e29b-41d4-a716-446655440003', NOW(), NOW()),
  ('bb0e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001',
   'Cancer Screening Audit', 'PENDING',
   ARRAY['990e8400-e29b-41d4-a716-446655440003', '990e8400-e29b-41d4-a716-446655440004']::uuid[],
   0, NULL, NULL, '660e8400-e29b-41d4-a716-446655440002', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- RISK SCORES
-- ============================================

INSERT INTO patient_risk_scores (id, tenant_id, patient_id, risk_score, risk_level, hcc_raf_score, model_version, calculated_at, created_at)
VALUES
  ('cc0e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001',
   '880e8400-e29b-41d4-a716-446655440001', 0.85, 'HIGH', 1.45, 'V28', NOW(), NOW()),
  ('cc0e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001',
   '880e8400-e29b-41d4-a716-446655440002', 0.35, 'LOW', 0.92, 'V28', NOW(), NOW()),
  ('cc0e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001',
   '880e8400-e29b-41d4-a716-446655440003', 0.72, 'HIGH', 1.78, 'V28', NOW(), NOW()),
  ('cc0e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440001',
   '880e8400-e29b-41d4-a716-446655440004', 0.45, 'MEDIUM', 1.12, 'V28', NOW(), NOW()),
  ('cc0e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440001',
   '880e8400-e29b-41d4-a716-446655440005', 0.58, 'MEDIUM', 1.25, 'V28', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- AUDIT LOG ENTRIES (Sample)
-- ============================================

INSERT INTO audit_logs (id, tenant_id, user_id, action, resource_type, resource_id, ip_address, created_at)
VALUES
  ('dd0e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001',
   '660e8400-e29b-41d4-a716-446655440003', 'LOGIN', 'USER', '660e8400-e29b-41d4-a716-446655440003', '192.168.1.100', NOW() - INTERVAL '1 hour'),
  ('dd0e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001',
   '660e8400-e29b-41d4-a716-446655440003', 'VIEW', 'PATIENT', '880e8400-e29b-41d4-a716-446655440001', '192.168.1.100', NOW() - INTERVAL '30 minutes'),
  ('dd0e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001',
   '660e8400-e29b-41d4-a716-446655440003', 'RUN_EVALUATION', 'EVALUATION', 'bb0e8400-e29b-41d4-a716-446655440001', '192.168.1.100', NOW() - INTERVAL '15 minutes')
ON CONFLICT (id) DO NOTHING;

-- Success message
DO $$
BEGIN
  RAISE NOTICE 'HDIM E2E test data seeded successfully';
  RAISE NOTICE 'Test users: test_superadmin, test_admin, test_evaluator, test_analyst, test_viewer (password: password123)';
END $$;
