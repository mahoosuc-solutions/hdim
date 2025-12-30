-- ============================================
-- HDIM E2E Test Data Seed Script
-- ============================================
-- This script populates test data for E2E testing
-- Run with: docker exec -i healthdata-postgres psql -U healthdata -f -

-- ============================================
-- 1. PATIENT DATA (patient_db)
-- ============================================
\c patient_db;

-- Insert test patients
INSERT INTO patient_demographics (
    id, tenant_id, fhir_patient_id, mrn, first_name, last_name, middle_name,
    date_of_birth, gender, race, ethnicity, preferred_language,
    address_line1, city, state, zip_code, country, phone, email,
    pcp_id, pcp_name, active, deceased, created_at, updated_at
) VALUES
    -- Patient 1: Sarah Johnson - Diabetes patient with care gaps
    ('11111111-1111-1111-1111-111111111111', 'TENANT001', 'fhir-patient-001', 'MRN-001',
     'Sarah', 'Johnson', 'Marie', '1965-03-15', 'female', 'White', 'Non-Hispanic', 'English',
     '123 Main Street', 'Boston', 'MA', '02101', 'USA', '617-555-0101', 'sarah.johnson@email.com',
     'pcp-001', 'Dr. Michael Chen', true, false, NOW(), NOW()),

    -- Patient 2: Robert Williams - Hypertension patient
    ('22222222-2222-2222-2222-222222222222', 'TENANT001', 'fhir-patient-002', 'MRN-002',
     'Robert', 'Williams', 'James', '1958-07-22', 'male', 'African American', 'Non-Hispanic', 'English',
     '456 Oak Avenue', 'Cambridge', 'MA', '02139', 'USA', '617-555-0102', 'robert.williams@email.com',
     'pcp-002', 'Dr. Lisa Park', true, false, NOW(), NOW()),

    -- Patient 3: Maria Garcia - Depression screening needed
    ('33333333-3333-3333-3333-333333333333', 'TENANT001', 'fhir-patient-003', 'MRN-003',
     'Maria', 'Garcia', 'Elena', '1972-11-08', 'female', 'Hispanic', 'Hispanic/Latino', 'Spanish',
     '789 Elm Street', 'Somerville', 'MA', '02143', 'USA', '617-555-0103', 'maria.garcia@email.com',
     'pcp-001', 'Dr. Michael Chen', true, false, NOW(), NOW()),

    -- Patient 4: Emily Chen - Breast cancer screening
    ('44444444-4444-4444-4444-444444444444', 'TENANT001', 'fhir-patient-004', 'MRN-004',
     'Emily', 'Chen', 'Wei', '1968-04-30', 'female', 'Asian', 'Non-Hispanic', 'English',
     '321 Pine Road', 'Brookline', 'MA', '02445', 'USA', '617-555-0104', 'emily.chen@email.com',
     'pcp-003', 'Dr. Sarah Kim', true, false, NOW(), NOW()),

    -- Patient 5: David Brown - Multiple conditions
    ('55555555-5555-5555-5555-555555555555', 'TENANT001', 'fhir-patient-005', 'MRN-005',
     'David', 'Brown', 'Andrew', '1955-09-12', 'male', 'White', 'Non-Hispanic', 'English',
     '654 Maple Lane', 'Newton', 'MA', '02458', 'USA', '617-555-0105', 'david.brown@email.com',
     'pcp-002', 'Dr. Lisa Park', true, false, NOW(), NOW()),

    -- Additional patients for population testing
    ('66666666-6666-6666-6666-666666666666', 'TENANT001', 'fhir-patient-006', 'MRN-006',
     'Jennifer', 'Martinez', 'Rose', '1980-01-25', 'female', 'Hispanic', 'Hispanic/Latino', 'English',
     '987 Cedar Court', 'Quincy', 'MA', '02169', 'USA', '617-555-0106', 'jennifer.martinez@email.com',
     'pcp-001', 'Dr. Michael Chen', true, false, NOW(), NOW()),

    ('77777777-7777-7777-7777-777777777777', 'TENANT001', 'fhir-patient-007', 'MRN-007',
     'Michael', 'Taylor', 'Joseph', '1962-06-18', 'male', 'African American', 'Non-Hispanic', 'English',
     '147 Birch Street', 'Medford', 'MA', '02155', 'USA', '617-555-0107', 'michael.taylor@email.com',
     'pcp-003', 'Dr. Sarah Kim', true, false, NOW(), NOW()),

    ('88888888-8888-8888-8888-888888888888', 'TENANT001', 'fhir-patient-008', 'MRN-008',
     'Lisa', 'Anderson', 'Ann', '1975-12-03', 'female', 'White', 'Non-Hispanic', 'English',
     '258 Walnut Drive', 'Arlington', 'MA', '02474', 'USA', '617-555-0108', 'lisa.anderson@email.com',
     'pcp-002', 'Dr. Lisa Park', true, false, NOW(), NOW()),

    ('99999999-9999-9999-9999-999999999999', 'TENANT001', 'fhir-patient-009', 'MRN-009',
     'William', 'Thomas', 'Robert', '1950-08-20', 'male', 'White', 'Non-Hispanic', 'English',
     '369 Spruce Avenue', 'Waltham', 'MA', '02451', 'USA', '617-555-0109', 'william.thomas@email.com',
     'pcp-001', 'Dr. Michael Chen', true, false, NOW(), NOW()),

    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'TENANT001', 'fhir-patient-010', 'MRN-010',
     'Patricia', 'Jackson', 'Lynn', '1970-02-14', 'female', 'African American', 'Non-Hispanic', 'English',
     '741 Oak Lane', 'Lexington', 'MA', '02420', 'USA', '617-555-0110', 'patricia.jackson@email.com',
     'pcp-003', 'Dr. Sarah Kim', true, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

SELECT 'Inserted ' || COUNT(*) || ' patients' AS status FROM patient_demographics WHERE tenant_id = 'TENANT001';

-- ============================================
-- 2. CARE GAPS (caregap_db)
-- ============================================
\c caregap_db;

-- Insert care gaps for test patients (using valid UUIDs)
INSERT INTO care_gaps (
    id, tenant_id, patient_id, measure_id, measure_name, gap_type, status,
    priority, severity, due_date, identified_date, measure_year,
    description, recommendation, gap_reason, gap_category,
    risk_score, star_impact, created_by, created_at, version
) VALUES
    -- Diabetes HbA1c gaps
    ('c1111111-1111-1111-1111-111111111111', 'TENANT001', '11111111-1111-1111-1111-111111111111',
     'NQF-0059', 'Diabetes: Hemoglobin A1c Control', 'SCREENING', 'OPEN',
     'HIGH', 'MODERATE', CURRENT_DATE + INTERVAL '30 days', NOW() - INTERVAL '15 days', 2024,
     'Patient is due for HbA1c test. Last result was 8.2% (3 months ago).',
     'Schedule HbA1c lab test. Consider medication adjustment if result > 8%.',
     'No HbA1c result in past 3 months', 'DIABETES',
     7.5, 0.25, 'system', NOW(), 1),

    -- Hypertension control gaps
    ('c2222222-2222-2222-2222-222222222222', 'TENANT001', '22222222-2222-2222-2222-222222222222',
     'NQF-0018', 'Controlling High Blood Pressure', 'TREATMENT', 'OPEN',
     'CRITICAL', 'HIGH', CURRENT_DATE + INTERVAL '7 days', NOW() - INTERVAL '10 days', 2024,
     'Blood pressure consistently above 140/90. Current reading: 158/96.',
     'Consider medication adjustment. Schedule follow-up BP check in 2 weeks.',
     'Uncontrolled hypertension despite current medication', 'CARDIOVASCULAR',
     9.0, 0.50, 'system', NOW(), 1),

    -- Depression screening gaps
    ('c3333333-3333-3333-3333-333333333333', 'TENANT001', '33333333-3333-3333-3333-333333333333',
     'NQF-0418', 'Depression Screening and Follow-Up', 'SCREENING', 'OPEN',
     'MEDIUM', 'MODERATE', CURRENT_DATE + INTERVAL '45 days', NOW() - INTERVAL '5 days', 2024,
     'Annual depression screening is overdue. Last PHQ-9 was 14 months ago.',
     'Administer PHQ-9 screening at next visit.',
     'No depression screening in past 12 months', 'BEHAVIORAL_HEALTH',
     5.0, 0.15, 'system', NOW(), 1),

    -- Breast cancer screening gaps
    ('c4444444-4444-4444-4444-444444444444', 'TENANT001', '44444444-4444-4444-4444-444444444444',
     'NQF-2372', 'Breast Cancer Screening', 'SCREENING', 'OPEN',
     'HIGH', 'MODERATE', CURRENT_DATE + INTERVAL '60 days', NOW() - INTERVAL '20 days', 2024,
     'Patient is due for mammography. Last screening was 26 months ago.',
     'Schedule mammography screening. Discuss benefits and risks with patient.',
     'No mammography in past 24 months', 'CANCER_SCREENING',
     6.5, 0.20, 'system', NOW(), 1),

    -- Statin therapy gap
    ('c5555555-5555-5555-5555-555555555555', 'TENANT001', '55555555-5555-5555-5555-555555555555',
     'NQF-0438', 'Statin Therapy for CVD Patients', 'TREATMENT', 'OPEN',
     'HIGH', 'HIGH', CURRENT_DATE + INTERVAL '14 days', NOW() - INTERVAL '30 days', 2024,
     'Patient with ASCVD not on statin therapy. Last LDL: 145 mg/dL.',
     'Initiate high-intensity statin therapy. Discuss with patient.',
     'Eligible for statin but not currently prescribed', 'CARDIOVASCULAR',
     8.0, 0.35, 'system', NOW(), 1),

    -- Closed care gap example
    ('c6666666-6666-6666-6666-666666666666', 'TENANT001', '66666666-6666-6666-6666-666666666666',
     'NQF-0034', 'Colorectal Cancer Screening', 'SCREENING', 'CLOSED',
     'LOW', 'LOW', CURRENT_DATE - INTERVAL '10 days', NOW() - INTERVAL '60 days', 2024,
     'Patient completed colonoscopy on schedule.',
     'No action needed. Next screening in 10 years.',
     'Colonoscopy completed', 'CANCER_SCREENING',
     2.0, 0.10, 'system', NOW() - INTERVAL '60 days', 1),

    -- Additional open gaps
    ('c7777777-7777-7777-7777-777777777777', 'TENANT001', '77777777-7777-7777-7777-777777777777',
     'NQF-0059', 'Diabetes: Hemoglobin A1c Control', 'SCREENING', 'OPEN',
     'MEDIUM', 'MODERATE', CURRENT_DATE + INTERVAL '21 days', NOW() - INTERVAL '8 days', 2024,
     'HbA1c test overdue. Patient has Type 2 diabetes.',
     'Schedule HbA1c lab test.',
     'No HbA1c in past 6 months', 'DIABETES',
     6.0, 0.20, 'system', NOW(), 1),

    ('c8888888-8888-8888-8888-888888888888', 'TENANT001', '88888888-8888-8888-8888-888888888888',
     'NQF-0418', 'Depression Screening and Follow-Up', 'SCREENING', 'OPEN',
     'LOW', 'LOW', CURRENT_DATE + INTERVAL '90 days', NOW() - INTERVAL '3 days', 2024,
     'Annual depression screening coming due.',
     'Schedule PHQ-9 at next wellness visit.',
     'Depression screening due within 90 days', 'BEHAVIORAL_HEALTH',
     3.0, 0.10, 'system', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

SELECT 'Inserted ' || COUNT(*) || ' care gaps' AS status FROM care_gaps WHERE tenant_id = 'TENANT001';

-- ============================================
-- 3. QUALITY MEASURE RESULTS (quality_db)
-- ============================================
\c quality_db;

-- Insert quality measure results (matching actual schema)
INSERT INTO quality_measure_results (
    id, tenant_id, patient_id, measure_id, measure_name, measure_category, measure_year,
    numerator_compliant, denominator_eligible, denominator_elligible, compliance_rate, score,
    calculation_date, created_by, created_at, updated_at, version
) VALUES
    -- Sarah Johnson - Diabetes measure
    ('a1111111-1111-1111-1111-111111111111', 'TENANT001', '11111111-1111-1111-1111-111111111111',
     'NQF-0059', 'Diabetes: Hemoglobin A1c Control', 'DIABETES', 2024,
     false, true, true, 0.0, 7.5, CURRENT_DATE, 'system', NOW(), NOW(), 1),

    -- Robert Williams - Blood Pressure
    ('a2222222-2222-2222-2222-222222222222', 'TENANT001', '22222222-2222-2222-2222-222222222222',
     'NQF-0018', 'Controlling High Blood Pressure', 'CARDIOVASCULAR', 2024,
     false, true, true, 0.0, 8.5, CURRENT_DATE, 'system', NOW(), NOW(), 1),

    -- Maria Garcia - Depression
    ('a3333333-3333-3333-3333-333333333333', 'TENANT001', '33333333-3333-3333-3333-333333333333',
     'NQF-0418', 'Depression Screening and Follow-Up', 'BEHAVIORAL_HEALTH', 2024,
     true, true, true, 100.0, 6.0, CURRENT_DATE, 'system', NOW(), NOW(), 1),

    -- Emily Chen - Breast Cancer
    ('a4444444-4444-4444-4444-444444444444', 'TENANT001', '44444444-4444-4444-4444-444444444444',
     'NQF-2372', 'Breast Cancer Screening', 'CANCER_SCREENING', 2024,
     false, true, true, 0.0, 5.5, CURRENT_DATE, 'system', NOW(), NOW(), 1),

    -- David Brown - Statin Therapy
    ('a5555555-5555-5555-5555-555555555555', 'TENANT001', '55555555-5555-5555-5555-555555555555',
     'NQF-0438', 'Statin Therapy for CVD Patients', 'CARDIOVASCULAR', 2024,
     false, true, true, 0.0, 8.0, CURRENT_DATE, 'system', NOW(), NOW(), 1),

    -- Jennifer Martinez - Colorectal (compliant)
    ('a6666666-6666-6666-6666-666666666666', 'TENANT001', '66666666-6666-6666-6666-666666666666',
     'NQF-0034', 'Colorectal Cancer Screening', 'CANCER_SCREENING', 2024,
     true, true, true, 100.0, 2.0, CURRENT_DATE, 'system', NOW(), NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- Insert saved reports (matching actual schema)
INSERT INTO saved_reports (
    id, tenant_id, report_type, report_name, description, year, start_date, end_date,
    report_data, created_by, created_at, generated_at, status, version
) VALUES
    ('b1111111-1111-1111-1111-111111111111', 'TENANT001', 'POPULATION',
     'Q4 2024 Quality Summary', 'Quarterly quality measure performance summary',
     2024, '2024-10-01', '2024-12-31',
     '{"totalPatients": 250, "avgComplianceRate": 76.5, "measuresEvaluated": 12}',
     'test_admin', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days', 'COMPLETED', 1),

    ('b2222222-2222-2222-2222-222222222222', 'TENANT001', 'PATIENT',
     'Diabetes Care Report - Sarah Johnson', 'Individual patient diabetes care assessment',
     2024, '2024-01-01', '2024-12-31',
     '{"patientName": "Sarah Johnson", "hba1c": 7.8, "lastTest": "2024-11-15", "compliant": false}',
     'test_evaluator', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', 'COMPLETED', 1),

    ('b3333333-3333-3333-3333-333333333333', 'TENANT001', 'POPULATION',
     'Care Gap Analysis - December 2024', 'Monthly care gap closure analysis',
     2024, '2024-12-01', '2024-12-31',
     '{"openGaps": 45, "closedGaps": 112, "closureRate": 71.3, "criticalGaps": 8}',
     'test_analyst', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', 'COMPLETED', 1)
ON CONFLICT (id) DO NOTHING;

SELECT 'Inserted ' || COUNT(*) || ' quality measure results' AS status FROM quality_measure_results WHERE tenant_id = 'TENANT001';
SELECT 'Inserted ' || COUNT(*) || ' saved reports' AS status FROM saved_reports WHERE tenant_id = 'TENANT001';

-- ============================================
-- 4. RISK SCORES (patient_db)
-- ============================================
\c patient_db;

-- Insert risk scores (matching actual schema)
INSERT INTO patient_risk_scores (
    id, tenant_id, patient_id, score_type, risk_category, score_value,
    factors, calculation_date, created_at
) VALUES
    ('d1111111-1111-1111-1111-111111111111', 'TENANT001', '11111111-1111-1111-1111-111111111111',
     'HCC', 'HIGH', 7.5000, '{"hba1c": 8.2, "bmi": 32.1, "age": 59}', NOW(), NOW()),

    ('d2222222-2222-2222-2222-222222222222', 'TENANT001', '22222222-2222-2222-2222-222222222222',
     'HCC', 'CRITICAL', 8.5000, '{"bp_systolic": 158, "cholesterol": 245, "smoker": false}', NOW(), NOW()),

    ('d3333333-3333-3333-3333-333333333333', 'TENANT001', '33333333-3333-3333-3333-333333333333',
     'BEHAVIORAL', 'MODERATE', 6.0000, '{"phq9_score": 12, "anxiety_gad7": 8}', NOW(), NOW()),

    ('d4444444-4444-4444-4444-444444444444', 'TENANT001', '44444444-4444-4444-4444-444444444444',
     'PREVENTIVE', 'MODERATE', 5.5000, '{"age": 56, "family_history": true}', NOW(), NOW()),

    ('d5555555-5555-5555-5555-555555555555', 'TENANT001', '55555555-5555-5555-5555-555555555555',
     'HCC', 'HIGH', 8.0000, '{"chronic_conditions": 3, "er_visits": 2, "age": 69}', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

SELECT 'Inserted ' || COUNT(*) || ' risk scores' AS status FROM patient_risk_scores WHERE tenant_id = 'TENANT001';

-- ============================================
-- SUMMARY
-- ============================================
\echo ''
\echo '=========================================='
\echo 'E2E Test Data Seeding Complete!'
\echo '=========================================='
\echo ''
