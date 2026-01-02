-- Demo Data for HDIM Platform
-- Run with: docker exec -i healthdata-postgres psql -U healthdata -f /load-demo-data-v2.sql
-- Or copy and paste sections into psql

-- ==============================================================================
-- FHIR Database: Patients
-- ==============================================================================
\c fhir_db

-- Insert 20 demo patients
INSERT INTO patients (id, tenant_id, fhir_id, family_name, given_name, birth_date, gender, active, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'demo-clinic', 'patient-001', 'Doe', 'John', '1959-11-14', 'male', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-002', 'Smith', 'Jane', '1967-03-22', 'female', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-003', 'Johnson', 'Robert', '1953-08-15', 'male', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-004', 'Williams', 'Mary', '1975-12-03', 'female', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-005', 'Brown', 'Michael', '1962-05-28', 'male', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-006', 'Davis', 'Patricia', '1970-07-11', 'female', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-007', 'Miller', 'James', '1955-01-30', 'male', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-008', 'Wilson', 'Linda', '1968-09-17', 'female', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-009', 'Moore', 'William', '1972-04-25', 'male', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-010', 'Taylor', 'Barbara', '1960-06-08', 'female', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-011', 'Anderson', 'David', '1958-11-22', 'male', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-012', 'Thomas', 'Susan', '1965-02-14', 'female', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-013', 'Jackson', 'Richard', '1973-08-03', 'male', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-014', 'White', 'Jessica', '1980-10-19', 'female', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-015', 'Harris', 'Charles', '1956-03-27', 'male', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-016', 'Martin', 'Sarah', '1963-12-05', 'female', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-017', 'Garcia', 'Joseph', '1969-05-16', 'male', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-018', 'Martinez', 'Karen', '1976-09-30', 'female', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-019', 'Robinson', 'Thomas', '1961-07-12', 'male', true, NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', 'patient-020', 'Clark', 'Nancy', '1974-01-08', 'female', true, NOW(), NOW())
ON CONFLICT DO NOTHING;

SELECT 'Inserted patients: ' || COUNT(*) FROM patients WHERE tenant_id = 'demo-clinic';

-- Insert conditions (diabetes, hypertension)
INSERT INTO conditions (id, tenant_id, patient_id, code, code_system, display_name, clinical_status, verification_status, onset_date, created_at, updated_at)
VALUES
    -- Diabetes Type 2 (E11.9)
    (gen_random_uuid(), 'demo-clinic', (SELECT id FROM patients WHERE fhir_id = 'patient-001' LIMIT 1), 'E11.9', 'http://hl7.org/fhir/sid/icd-10-cm', 'Type 2 diabetes mellitus without complications', 'active', 'confirmed', '2020-03-15', NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', (SELECT id FROM patients WHERE fhir_id = 'patient-003' LIMIT 1), 'E11.9', 'http://hl7.org/fhir/sid/icd-10-cm', 'Type 2 diabetes mellitus without complications', 'active', 'confirmed', '2019-06-22', NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', (SELECT id FROM patients WHERE fhir_id = 'patient-005' LIMIT 1), 'E11.9', 'http://hl7.org/fhir/sid/icd-10-cm', 'Type 2 diabetes mellitus without complications', 'active', 'confirmed', '2021-01-10', NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', (SELECT id FROM patients WHERE fhir_id = 'patient-011' LIMIT 1), 'E11.9', 'http://hl7.org/fhir/sid/icd-10-cm', 'Type 2 diabetes mellitus without complications', 'active', 'confirmed', '2018-09-05', NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', (SELECT id FROM patients WHERE fhir_id = 'patient-015' LIMIT 1), 'E11.9', 'http://hl7.org/fhir/sid/icd-10-cm', 'Type 2 diabetes mellitus without complications', 'active', 'confirmed', '2017-11-30', NOW(), NOW()),
    -- Hypertension (I10)
    (gen_random_uuid(), 'demo-clinic', (SELECT id FROM patients WHERE fhir_id = 'patient-002' LIMIT 1), 'I10', 'http://hl7.org/fhir/sid/icd-10-cm', 'Essential (primary) hypertension', 'active', 'confirmed', '2019-02-18', NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', (SELECT id FROM patients WHERE fhir_id = 'patient-004' LIMIT 1), 'I10', 'http://hl7.org/fhir/sid/icd-10-cm', 'Essential (primary) hypertension', 'active', 'confirmed', '2020-07-14', NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', (SELECT id FROM patients WHERE fhir_id = 'patient-006' LIMIT 1), 'I10', 'http://hl7.org/fhir/sid/icd-10-cm', 'Essential (primary) hypertension', 'active', 'confirmed', '2018-12-03', NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', (SELECT id FROM patients WHERE fhir_id = 'patient-009' LIMIT 1), 'I10', 'http://hl7.org/fhir/sid/icd-10-cm', 'Essential (primary) hypertension', 'active', 'confirmed', '2021-04-20', NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', (SELECT id FROM patients WHERE fhir_id = 'patient-017' LIMIT 1), 'I10', 'http://hl7.org/fhir/sid/icd-10-cm', 'Essential (primary) hypertension', 'active', 'confirmed', '2019-08-11', NOW(), NOW()),
    -- Depression (F32.9)
    (gen_random_uuid(), 'demo-clinic', (SELECT id FROM patients WHERE fhir_id = 'patient-007' LIMIT 1), 'F32.9', 'http://hl7.org/fhir/sid/icd-10-cm', 'Major depressive disorder, single episode, unspecified', 'active', 'confirmed', '2022-05-10', NOW(), NOW()),
    (gen_random_uuid(), 'demo-clinic', (SELECT id FROM patients WHERE fhir_id = 'patient-012' LIMIT 1), 'F32.9', 'http://hl7.org/fhir/sid/icd-10-cm', 'Major depressive disorder, single episode, unspecified', 'active', 'confirmed', '2023-01-25', NOW(), NOW())
ON CONFLICT DO NOTHING;

SELECT 'Inserted conditions: ' || COUNT(*) FROM conditions WHERE tenant_id = 'demo-clinic';

-- ==============================================================================
-- Quality Database: Results and Care Gaps
-- ==============================================================================
\c quality_db

-- Insert quality measure results
INSERT INTO quality_measure_results (
    id, tenant_id, patient_id, measure_id, measure_name, measure_category,
    measure_year, numerator_compliant, denominator_elligible, compliance_rate,
    score, calculation_date, created_at, created_by
) VALUES
    -- CMS122: Diabetes HbA1c Control (5 patients with diabetes)
    (gen_random_uuid(), 'demo-clinic', 'patient-001', 'CMS122', 'Diabetes: Hemoglobin A1c (HbA1c) Poor Control (>9%)', 'Chronic Disease', 2025, true, true, 100.0, 100.0, NOW() - INTERVAL '5 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-003', 'CMS122', 'Diabetes: Hemoglobin A1c (HbA1c) Poor Control (>9%)', 'Chronic Disease', 2025, false, true, 0.0, 0.0, NOW() - INTERVAL '5 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-005', 'CMS122', 'Diabetes: Hemoglobin A1c (HbA1c) Poor Control (>9%)', 'Chronic Disease', 2025, true, true, 100.0, 100.0, NOW() - INTERVAL '5 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-011', 'CMS122', 'Diabetes: Hemoglobin A1c (HbA1c) Poor Control (>9%)', 'Chronic Disease', 2025, false, true, 0.0, 0.0, NOW() - INTERVAL '5 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-015', 'CMS122', 'Diabetes: Hemoglobin A1c (HbA1c) Poor Control (>9%)', 'Chronic Disease', 2025, true, true, 100.0, 100.0, NOW() - INTERVAL '5 days', NOW(), 'demo-loader'),

    -- CMS165: Controlling Blood Pressure (5 patients with hypertension)
    (gen_random_uuid(), 'demo-clinic', 'patient-002', 'CMS165', 'Controlling High Blood Pressure', 'Chronic Disease', 2025, true, true, 100.0, 100.0, NOW() - INTERVAL '3 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-004', 'CMS165', 'Controlling High Blood Pressure', 'Chronic Disease', 2025, false, true, 0.0, 0.0, NOW() - INTERVAL '3 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-006', 'CMS165', 'Controlling High Blood Pressure', 'Chronic Disease', 2025, true, true, 100.0, 100.0, NOW() - INTERVAL '3 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-009', 'CMS165', 'Controlling High Blood Pressure', 'Chronic Disease', 2025, false, true, 0.0, 0.0, NOW() - INTERVAL '3 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-017', 'CMS165', 'Controlling High Blood Pressure', 'Chronic Disease', 2025, true, true, 100.0, 100.0, NOW() - INTERVAL '3 days', NOW(), 'demo-loader'),

    -- CMS2: Depression Screening (all adult patients)
    (gen_random_uuid(), 'demo-clinic', 'patient-001', 'CMS2', 'Screening for Depression and Follow-Up Plan', 'Behavioral Health', 2025, true, true, 100.0, 100.0, NOW() - INTERVAL '7 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-002', 'CMS2', 'Screening for Depression and Follow-Up Plan', 'Behavioral Health', 2025, false, true, 0.0, 0.0, NOW() - INTERVAL '7 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-003', 'CMS2', 'Screening for Depression and Follow-Up Plan', 'Behavioral Health', 2025, true, true, 100.0, 100.0, NOW() - INTERVAL '7 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-004', 'CMS2', 'Screening for Depression and Follow-Up Plan', 'Behavioral Health', 2025, true, true, 100.0, 100.0, NOW() - INTERVAL '7 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-005', 'CMS2', 'Screening for Depression and Follow-Up Plan', 'Behavioral Health', 2025, false, true, 0.0, 0.0, NOW() - INTERVAL '7 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-006', 'CMS2', 'Screening for Depression and Follow-Up Plan', 'Behavioral Health', 2025, true, true, 100.0, 100.0, NOW() - INTERVAL '7 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-007', 'CMS2', 'Screening for Depression and Follow-Up Plan', 'Behavioral Health', 2025, true, true, 100.0, 100.0, NOW() - INTERVAL '7 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-008', 'CMS2', 'Screening for Depression and Follow-Up Plan', 'Behavioral Health', 2025, false, true, 0.0, 0.0, NOW() - INTERVAL '7 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-009', 'CMS2', 'Screening for Depression and Follow-Up Plan', 'Behavioral Health', 2025, true, true, 100.0, 100.0, NOW() - INTERVAL '7 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-010', 'CMS2', 'Screening for Depression and Follow-Up Plan', 'Behavioral Health', 2025, true, true, 100.0, 100.0, NOW() - INTERVAL '7 days', NOW(), 'demo-loader'),

    -- CMS130: Colorectal Cancer Screening (age 45-75)
    (gen_random_uuid(), 'demo-clinic', 'patient-003', 'CMS130', 'Colorectal Cancer Screening', 'Preventive Care', 2025, true, true, 100.0, 100.0, NOW() - INTERVAL '10 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-007', 'CMS130', 'Colorectal Cancer Screening', 'Preventive Care', 2025, false, true, 0.0, 0.0, NOW() - INTERVAL '10 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-010', 'CMS130', 'Colorectal Cancer Screening', 'Preventive Care', 2025, true, true, 100.0, 100.0, NOW() - INTERVAL '10 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-015', 'CMS130', 'Colorectal Cancer Screening', 'Preventive Care', 2025, false, true, 0.0, 0.0, NOW() - INTERVAL '10 days', NOW(), 'demo-loader'),
    (gen_random_uuid(), 'demo-clinic', 'patient-019', 'CMS130', 'Colorectal Cancer Screening', 'Preventive Care', 2025, true, true, 100.0, 100.0, NOW() - INTERVAL '10 days', NOW(), 'demo-loader')
ON CONFLICT DO NOTHING;

SELECT 'Inserted quality results: ' || COUNT(*) FROM quality_measure_results WHERE tenant_id = 'demo-clinic';

-- Insert care gaps for non-compliant patients
INSERT INTO care_gaps (
    id, tenant_id, patient_id, category, gap_type, title, description,
    priority, status, quality_measure, recommendation,
    due_date, identified_date, created_at, updated_at
) VALUES
    -- HIGH priority gaps
    (gen_random_uuid(), 'demo-clinic', (SELECT id::varchar FROM (SELECT gen_random_uuid() as id) x), 'Chronic Disease', 'DIABETES_HBA1C',
     'HbA1c Test Overdue - Robert Johnson', 'Patient has not had HbA1c test in over 12 months. Last result was 9.2%.',
     'HIGH', 'OPEN', 'CMS122', 'Schedule HbA1c test and diabetes management appointment',
     CURRENT_DATE + INTERVAL '7 days', CURRENT_DATE - INTERVAL '30 days', NOW(), NOW()),

    (gen_random_uuid(), 'demo-clinic', (SELECT id::varchar FROM (SELECT gen_random_uuid() as id) x), 'Chronic Disease', 'DIABETES_HBA1C',
     'HbA1c Test Overdue - David Anderson', 'Patient has diabetes but no HbA1c on record for measurement year.',
     'HIGH', 'OPEN', 'CMS122', 'Urgent: Schedule HbA1c test',
     CURRENT_DATE + INTERVAL '14 days', CURRENT_DATE - INTERVAL '45 days', NOW(), NOW()),

    (gen_random_uuid(), 'demo-clinic', (SELECT id::varchar FROM (SELECT gen_random_uuid() as id) x), 'Chronic Disease', 'HYPERTENSION_BP',
     'Blood Pressure Not Controlled - Mary Williams', 'Last BP reading: 152/94 mmHg. Target is <140/90.',
     'HIGH', 'OPEN', 'CMS165', 'Schedule follow-up, review medication compliance',
     CURRENT_DATE + INTERVAL '3 days', CURRENT_DATE - INTERVAL '14 days', NOW(), NOW()),

    -- MEDIUM priority gaps
    (gen_random_uuid(), 'demo-clinic', (SELECT id::varchar FROM (SELECT gen_random_uuid() as id) x), 'Chronic Disease', 'HYPERTENSION_BP',
     'Blood Pressure Monitoring Needed - William Moore', 'BP elevated at last visit. Needs follow-up measurement.',
     'MEDIUM', 'OPEN', 'CMS165', 'Schedule BP check, consider home monitoring',
     CURRENT_DATE + INTERVAL '21 days', CURRENT_DATE - INTERVAL '10 days', NOW(), NOW()),

    (gen_random_uuid(), 'demo-clinic', (SELECT id::varchar FROM (SELECT gen_random_uuid() as id) x), 'Behavioral Health', 'DEPRESSION_SCREEN',
     'Depression Screening Overdue - Jane Smith', 'Annual PHQ-9 screening not completed.',
     'MEDIUM', 'OPEN', 'CMS2', 'Administer PHQ-9 at next visit',
     CURRENT_DATE + INTERVAL '30 days', CURRENT_DATE - INTERVAL '60 days', NOW(), NOW()),

    (gen_random_uuid(), 'demo-clinic', (SELECT id::varchar FROM (SELECT gen_random_uuid() as id) x), 'Behavioral Health', 'DEPRESSION_SCREEN',
     'Depression Screening Overdue - Michael Brown', 'Patient declined screening at last visit.',
     'MEDIUM', 'OPEN', 'CMS2', 'Discuss importance of screening, offer alternatives',
     CURRENT_DATE + INTERVAL '14 days', CURRENT_DATE - INTERVAL '45 days', NOW(), NOW()),

    (gen_random_uuid(), 'demo-clinic', (SELECT id::varchar FROM (SELECT gen_random_uuid() as id) x), 'Behavioral Health', 'DEPRESSION_SCREEN',
     'Depression Screening Overdue - Linda Wilson', 'No PHQ-9 on record for measurement year.',
     'MEDIUM', 'OPEN', 'CMS2', 'Schedule screening at next appointment',
     CURRENT_DATE + INTERVAL '21 days', CURRENT_DATE - INTERVAL '30 days', NOW(), NOW()),

    -- LOW priority gaps
    (gen_random_uuid(), 'demo-clinic', (SELECT id::varchar FROM (SELECT gen_random_uuid() as id) x), 'Preventive Care', 'COLORECTAL_SCREEN',
     'Colorectal Screening Due - James Miller', 'Patient is due for colorectal cancer screening.',
     'LOW', 'OPEN', 'CMS130', 'Discuss screening options: colonoscopy, FIT, or Cologuard',
     CURRENT_DATE + INTERVAL '60 days', CURRENT_DATE - INTERVAL '5 days', NOW(), NOW()),

    (gen_random_uuid(), 'demo-clinic', (SELECT id::varchar FROM (SELECT gen_random_uuid() as id) x), 'Preventive Care', 'COLORECTAL_SCREEN',
     'Colorectal Screening Due - Charles Harris', 'Patient declined colonoscopy, consider FIT test.',
     'LOW', 'OPEN', 'CMS130', 'Offer FIT test as alternative',
     CURRENT_DATE + INTERVAL '45 days', CURRENT_DATE - INTERVAL '20 days', NOW(), NOW())
ON CONFLICT DO NOTHING;

SELECT 'Inserted care gaps: ' || COUNT(*) FROM care_gaps WHERE tenant_id = 'demo-clinic';

-- Summary
SELECT
    'Demo Data Summary' as report,
    (SELECT COUNT(*) FROM quality_measure_results WHERE tenant_id = 'demo-clinic') as total_results,
    (SELECT COUNT(*) FROM care_gaps WHERE tenant_id = 'demo-clinic') as total_gaps,
    (SELECT COUNT(*) FROM care_gaps WHERE tenant_id = 'demo-clinic' AND priority = 'HIGH') as high_priority_gaps,
    (SELECT COUNT(*) FROM care_gaps WHERE tenant_id = 'demo-clinic' AND priority = 'MEDIUM') as medium_priority_gaps,
    (SELECT COUNT(*) FROM care_gaps WHERE tenant_id = 'demo-clinic' AND priority = 'LOW') as low_priority_gaps;
