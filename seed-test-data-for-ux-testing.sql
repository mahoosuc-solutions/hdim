-- Seed Test Data for UX Testing
-- This script creates sample data to enable full Playwright testing of Phase 1 UX improvements
-- Run this against the PostgreSQL database used by the clinical portal

-- ============================================================================
-- 1. Create Test Patients
-- ============================================================================

-- Insert test patients if they don't exist
INSERT INTO patient (id, family_name, given_name, birth_date, gender, active, created_at, updated_at)
VALUES
  ('test-patient-001', 'Doe', 'John', '1975-05-15', 'male', true, NOW(), NOW()),
  ('test-patient-002', 'Smith', 'Jane', '1980-03-22', 'female', true, NOW(), NOW()),
  ('test-patient-003', 'Johnson', 'Robert', '1965-11-08', 'male', true, NOW(), NOW()),
  ('test-patient-004', 'Williams', 'Mary', '1990-07-19', 'female', true, NOW(), NOW()),
  ('test-patient-005', 'Brown', 'Michael', '1972-09-30', 'male', true, NOW(), NOW()),
  ('test-patient-006', 'Jones', 'Patricia', '1985-12-14', 'female', true, NOW(), NOW()),
  ('test-patient-007', 'Garcia', 'James', '1978-04-25', 'male', true, NOW(), NOW()),
  ('test-patient-008', 'Martinez', 'Linda', '1992-06-17', 'female', true, NOW(), NOW()),
  ('test-patient-009', 'Davis', 'William', '1968-02-09', 'male', true, NOW(), NOW()),
  ('test-patient-010', 'Rodriguez', 'Barbara', '1988-10-03', 'female', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Add MRNs for fuzzy search testing
UPDATE patient SET mrn = 'MRN001', mrn_authority = 'TEST_HOSPITAL' WHERE id = 'test-patient-001';
UPDATE patient SET mrn = 'MRN002', mrn_authority = 'TEST_HOSPITAL' WHERE id = 'test-patient-002';
UPDATE patient SET mrn = 'MRN003', mrn_authority = 'TEST_HOSPITAL' WHERE id = 'test-patient-003';
UPDATE patient SET mrn = 'MRN004', mrn_authority = 'TEST_HOSPITAL' WHERE id = 'test-patient-004';
UPDATE patient SET mrn = 'MRN005', mrn_authority = 'TEST_HOSPITAL' WHERE id = 'test-patient-005';
UPDATE patient SET mrn = 'MRN006', mrn_authority = 'TEST_HOSPITAL' WHERE id = 'test-patient-006';
UPDATE patient SET mrn = 'MRN007', mrn_authority = 'TEST_HOSPITAL' WHERE id = 'test-patient-007';
UPDATE patient SET mrn = 'MRN008', mrn_authority = 'TEST_HOSPITAL' WHERE id = 'test-patient-008';
UPDATE patient SET mrn = 'MRN009', mrn_authority = 'TEST_HOSPITAL' WHERE id = 'test-patient-009';
UPDATE patient SET mrn = 'MRN010', mrn_authority = 'TEST_HOSPITAL' WHERE id = 'test-patient-010';

-- ============================================================================
-- 2. Create Test Quality Measures
-- ============================================================================

-- Insert sample quality measures
INSERT INTO quality_measure (id, name, description, version, status, created_at, updated_at)
VALUES
  ('measure-diabetes-hba1c', 'Diabetes HbA1c Control', 'HEDIS - Comprehensive Diabetes Care: HbA1c Control', '1.0', 'active', NOW(), NOW()),
  ('measure-colorectal-screening', 'Colorectal Cancer Screening', 'HEDIS - Colorectal Cancer Screening', '1.0', 'active', NOW(), NOW()),
  ('measure-breast-cancer-screening', 'Breast Cancer Screening', 'HEDIS - Breast Cancer Screening', '1.0', 'active', NOW(), NOW()),
  ('measure-depression-screening', 'Depression Screening', 'Adult Depression Screening and Follow-up', '1.0', 'active', NOW(), NOW()),
  ('measure-medication-adherence', 'Medication Adherence', 'Diabetes Medication Adherence', '1.0', 'active', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 3. Create CQL Evaluations (Mix of Compliant and Non-Compliant)
-- ============================================================================

-- Non-compliant evaluations (for care gaps testing)
INSERT INTO cql_evaluation (
  id, patient_id, library_name, library_version, evaluation_date,
  in_initial_population, in_denominator, in_numerator, in_denominator_exclusion,
  evaluation_result, created_at, updated_at, tenant_id
)
VALUES
  -- Patient 001: Non-compliant diabetes (HIGH URGENCY - 120 days overdue)
  (
    'eval-001-diabetes', 'test-patient-001', 'DiabetesHbA1cControl', '1.0',
    NOW() - INTERVAL '120 days',
    true, true, false, false,
    '{"compliant": false, "reason": "HbA1c not tested", "daysOverdue": 120}',
    NOW(), NOW(), 'test-tenant'
  ),

  -- Patient 003: Non-compliant colorectal screening (HIGH URGENCY - 95 days overdue)
  (
    'eval-003-colorectal', 'test-patient-003', 'ColorectalCancerScreening', '1.0',
    NOW() - INTERVAL '95 days',
    true, true, false, false,
    '{"compliant": false, "reason": "Screening not completed", "daysOverdue": 95}',
    NOW(), NOW(), 'test-tenant'
  ),

  -- Patient 005: Non-compliant breast cancer screening (MEDIUM URGENCY - 45 days overdue)
  (
    'eval-005-breast', 'test-patient-005', 'BreastCancerScreening', '1.0',
    NOW() - INTERVAL '45 days',
    true, true, false, false,
    '{"compliant": false, "reason": "Mammogram overdue", "daysOverdue": 45}',
    NOW(), NOW(), 'test-tenant'
  ),

  -- Patient 007: Non-compliant depression screening (MEDIUM URGENCY - 35 days overdue)
  (
    'eval-007-depression', 'test-patient-007', 'DepressionScreening', '1.0',
    NOW() - INTERVAL '35 days',
    true, true, false, false,
    '{"compliant": false, "reason": "PHQ-9 not administered", "daysOverdue": 35}',
    NOW(), NOW(), 'test-tenant'
  ),

  -- Patient 009: Non-compliant medication adherence (LOW URGENCY - 15 days overdue)
  (
    'eval-009-medication', 'test-patient-009', 'MedicationAdherence', '1.0',
    NOW() - INTERVAL '15 days',
    true, true, false, false,
    '{"compliant": false, "reason": "Refill not picked up", "daysOverdue": 15}',
    NOW(), NOW(), 'test-tenant'
  ),

  -- Compliant evaluations (for dashboard statistics)
  (
    'eval-002-diabetes', 'test-patient-002', 'DiabetesHbA1cControl', '1.0',
    NOW() - INTERVAL '10 days',
    true, true, true, false,
    '{"compliant": true, "hba1c": 6.8}',
    NOW(), NOW(), 'test-tenant'
  ),
  (
    'eval-004-colorectal', 'test-patient-004', 'ColorectalCancerScreening', '1.0',
    NOW() - INTERVAL '5 days',
    true, true, true, false,
    '{"compliant": true, "screeningDate": "2024-11-20"}',
    NOW(), NOW(), 'test-tenant'
  ),
  (
    'eval-006-breast', 'test-patient-006', 'BreastCancerScreening', '1.0',
    NOW() - INTERVAL '3 days',
    true, true, true, false,
    '{"compliant": true, "mammogramDate": "2024-11-22"}',
    NOW(), NOW(), 'test-tenant'
  ),
  (
    'eval-008-depression', 'test-patient-008', 'DepressionScreening', '1.0',
    NOW() - INTERVAL '7 days',
    true, true, true, false,
    '{"compliant": true, "phq9Score": 2}',
    NOW(), NOW(), 'test-tenant'
  ),
  (
    'eval-010-medication', 'test-patient-010', 'MedicationAdherence', '1.0',
    NOW() - INTERVAL '2 days',
    true, true, true, false,
    '{"compliant": true, "adherenceRate": 95}',
    NOW(), NOW(), 'test-tenant'
  )
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 4. Create Quality Measure Results (for dashboard statistics)
-- ============================================================================

INSERT INTO quality_measure_result (
  id, measure_id, patient_id, evaluation_date,
  compliance_status, numerator, denominator, result_details,
  created_at, updated_at, tenant_id
)
VALUES
  -- Non-compliant results (for care gaps)
  (
    'result-001', 'measure-diabetes-hba1c', 'test-patient-001', NOW() - INTERVAL '120 days',
    'NON_COMPLIANT', 0, 1, '{"hba1c": null, "lastTestDate": null}',
    NOW(), NOW(), 'test-tenant'
  ),
  (
    'result-003', 'measure-colorectal-screening', 'test-patient-003', NOW() - INTERVAL '95 days',
    'NON_COMPLIANT', 0, 1, '{"lastScreening": null, "ageEligible": true}',
    NOW(), NOW(), 'test-tenant'
  ),
  (
    'result-005', 'measure-breast-cancer-screening', 'test-patient-005', NOW() - INTERVAL '45 days',
    'NON_COMPLIANT', 0, 1, '{"lastMammogram": null, "ageEligible": true}',
    NOW(), NOW(), 'test-tenant'
  ),
  (
    'result-007', 'measure-depression-screening', 'test-patient-007', NOW() - INTERVAL '35 days',
    'NON_COMPLIANT', 0, 1, '{"lastPHQ9": null, "eligible": true}',
    NOW(), NOW(), 'test-tenant'
  ),
  (
    'result-009', 'measure-medication-adherence', 'test-patient-009', NOW() - INTERVAL '15 days',
    'NON_COMPLIANT', 0, 1, '{"adherence": 65, "threshold": 80}',
    NOW(), NOW(), 'test-tenant'
  ),

  -- Compliant results (for dashboard stats)
  (
    'result-002', 'measure-diabetes-hba1c', 'test-patient-002', NOW() - INTERVAL '10 days',
    'COMPLIANT', 1, 1, '{"hba1c": 6.8, "lastTestDate": "2024-11-15"}',
    NOW(), NOW(), 'test-tenant'
  ),
  (
    'result-004', 'measure-colorectal-screening', 'test-patient-004', NOW() - INTERVAL '5 days',
    'COMPLIANT', 1, 1, '{"lastScreening": "2024-11-20", "method": "colonoscopy"}',
    NOW(), NOW(), 'test-tenant'
  ),
  (
    'result-006', 'measure-breast-cancer-screening', 'test-patient-006', NOW() - INTERVAL '3 days',
    'COMPLIANT', 1, 1, '{"lastMammogram": "2024-11-22", "result": "negative"}',
    NOW(), NOW(), 'test-tenant'
  ),
  (
    'result-008', 'measure-depression-screening', 'test-patient-008', NOW() - INTERVAL '7 days',
    'COMPLIANT', 1, 1, '{"lastPHQ9": "2024-11-18", "score": 2}',
    NOW(), NOW(), 'test-tenant'
  ),
  (
    'result-010', 'measure-medication-adherence', 'test-patient-010', NOW() - INTERVAL '2 days',
    'COMPLIANT', 1, 1, '{"adherence": 95, "threshold": 80}',
    NOW(), NOW(), 'test-tenant'
  )
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 5. Verification Queries
-- ============================================================================

-- Verify patients
SELECT COUNT(*) as patient_count FROM patient WHERE id LIKE 'test-patient-%';

-- Verify evaluations
SELECT COUNT(*) as evaluation_count FROM cql_evaluation WHERE patient_id LIKE 'test-patient-%';

-- Verify non-compliant evaluations (should be 5 for care gaps)
SELECT COUNT(*) as non_compliant_count
FROM cql_evaluation
WHERE patient_id LIKE 'test-patient-%'
  AND in_denominator = true
  AND in_numerator = false;

-- Verify compliant evaluations (should be 5 for dashboard stats)
SELECT COUNT(*) as compliant_count
FROM cql_evaluation
WHERE patient_id LIKE 'test-patient-%'
  AND in_denominator = true
  AND in_numerator = true;

-- Display care gap summary
SELECT
  ce.patient_id,
  p.family_name,
  p.given_name,
  ce.library_name,
  ce.evaluation_date,
  EXTRACT(DAY FROM (NOW() - ce.evaluation_date)) as days_overdue,
  CASE
    WHEN EXTRACT(DAY FROM (NOW() - ce.evaluation_date)) > 90 THEN 'HIGH'
    WHEN EXTRACT(DAY FROM (NOW() - ce.evaluation_date)) > 30 THEN 'MEDIUM'
    ELSE 'LOW'
  END as urgency
FROM cql_evaluation ce
JOIN patient p ON ce.patient_id = p.id
WHERE ce.patient_id LIKE 'test-patient-%'
  AND ce.in_denominator = true
  AND ce.in_numerator = false
ORDER BY days_overdue DESC;

-- Display compliance summary for dashboard
SELECT
  COUNT(*) as total_evaluations,
  SUM(CASE WHEN in_numerator = true THEN 1 ELSE 0 END) as compliant,
  SUM(CASE WHEN in_numerator = false THEN 1 ELSE 0 END) as non_compliant,
  ROUND(100.0 * SUM(CASE WHEN in_numerator = true THEN 1 ELSE 0 END) / COUNT(*), 1) as compliance_rate
FROM cql_evaluation
WHERE patient_id LIKE 'test-patient-%'
  AND in_denominator = true;

COMMIT;

-- ============================================================================
-- Test Data Summary
-- ============================================================================
--
-- PATIENTS: 10 test patients (IDs: test-patient-001 through test-patient-010)
--
-- CARE GAPS (5 non-compliant evaluations):
--   1. test-patient-001: Diabetes HbA1c (HIGH - 120 days overdue)
--   2. test-patient-003: Colorectal Screening (HIGH - 95 days overdue)
--   3. test-patient-005: Breast Cancer Screening (MEDIUM - 45 days overdue)
--   4. test-patient-007: Depression Screening (MEDIUM - 35 days overdue)
--   5. test-patient-009: Medication Adherence (LOW - 15 days overdue)
--
-- COMPLIANT EVALUATIONS (5 for dashboard statistics):
--   1. test-patient-002: Diabetes HbA1c (compliant)
--   2. test-patient-004: Colorectal Screening (compliant)
--   3. test-patient-006: Breast Cancer Screening (compliant)
--   4. test-patient-008: Depression Screening (compliant)
--   5. test-patient-010: Medication Adherence (compliant)
--
-- DASHBOARD STATISTICS:
--   - Total Evaluations: 10
--   - Total Patients: 10
--   - Compliance Rate: 50% (5 compliant / 10 total)
--   - Recent Evaluations (last 30 days): 10
--
-- EXPECTED TEST RESULTS:
--   ✅ Care Gaps Card: Should display 5 gaps (2 high, 2 medium, 1 low urgency)
--   ✅ Patient Search: Should find "John Doe" when searching "Jon" (fuzzy match)
--   ✅ Quick Actions: Should show buttons on all 4 stat cards
--
-- ============================================================================
