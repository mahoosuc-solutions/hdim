-- Demo Seed Data for HDIM Demo Sandbox
-- Provides realistic sample data for prospect evaluation

-- Sample Patients (50 patients)
INSERT INTO patient_demographics (id, tenant_id, fhir_id, first_name, last_name, date_of_birth, gender, address_line1, city, state, zip_code, phone, email, mrn, created_at, updated_at) VALUES
('550e8400-e29b-41d4-a716-446655440100', '550e8400-e29b-41d4-a716-446655440001', 'patient-001', 'John', 'Smith', '1965-03-15', 'male', '123 Main St', 'Boston', 'MA', '02101', '555-0101', 'john.smith@email.com', 'MRN-001', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440001', 'patient-002', 'Mary', 'Johnson', '1958-07-22', 'female', '456 Oak Ave', 'Cambridge', 'MA', '02139', '555-0102', 'mary.johnson@email.com', 'MRN-002', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440102', '550e8400-e29b-41d4-a716-446655440001', 'patient-003', 'Robert', 'Williams', '1970-11-08', 'male', '789 Elm St', 'Somerville', 'MA', '02143', '555-0103', 'robert.williams@email.com', 'MRN-003', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440103', '550e8400-e29b-41d4-a716-446655440001', 'patient-004', 'Patricia', 'Brown', '1962-05-30', 'female', '321 Pine Rd', 'Newton', 'MA', '02458', '555-0104', 'patricia.brown@email.com', 'MRN-004', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440104', '550e8400-e29b-41d4-a716-446655440001', 'patient-005', 'Michael', 'Davis', '1955-09-12', 'male', '654 Maple Dr', 'Brookline', 'MA', '02445', '555-0105', 'michael.davis@email.com', 'MRN-005', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440105', '550e8400-e29b-41d4-a716-446655440001', 'patient-006', 'Linda', 'Garcia', '1968-01-25', 'female', '987 Cedar Ln', 'Medford', 'MA', '02155', '555-0106', 'linda.garcia@email.com', 'MRN-006', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440106', '550e8400-e29b-41d4-a716-446655440001', 'patient-007', 'William', 'Miller', '1972-04-18', 'male', '147 Birch Way', 'Malden', 'MA', '02148', '555-0107', 'william.miller@email.com', 'MRN-007', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440107', '550e8400-e29b-41d4-a716-446655440001', 'patient-008', 'Barbara', 'Wilson', '1960-08-05', 'female', '258 Spruce Ct', 'Everett', 'MA', '02149', '555-0108', 'barbara.wilson@email.com', 'MRN-008', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440108', '550e8400-e29b-41d4-a716-446655440001', 'patient-009', 'David', 'Moore', '1957-12-20', 'male', '369 Walnut Blvd', 'Chelsea', 'MA', '02150', '555-0109', 'david.moore@email.com', 'MRN-009', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440109', '550e8400-e29b-41d4-a716-446655440001', 'patient-010', 'Elizabeth', 'Taylor', '1964-06-14', 'female', '741 Ash St', 'Revere', 'MA', '02151', '555-0110', 'elizabeth.taylor@email.com', 'MRN-010', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440110', '550e8400-e29b-41d4-a716-446655440001', 'patient-011', 'James', 'Anderson', '1959-02-28', 'male', '852 Oak Pl', 'Winthrop', 'MA', '02152', '555-0111', 'james.anderson@email.com', 'MRN-011', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440111', '550e8400-e29b-41d4-a716-446655440001', 'patient-012', 'Jennifer', 'Thomas', '1966-10-03', 'female', '963 Elm Rd', 'Lynn', 'MA', '01901', '555-0112', 'jennifer.thomas@email.com', 'MRN-012', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440112', '550e8400-e29b-41d4-a716-446655440001', 'patient-013', 'Charles', 'Jackson', '1971-07-17', 'male', '159 Pine Ave', 'Salem', 'MA', '01970', '555-0113', 'charles.jackson@email.com', 'MRN-013', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440113', '550e8400-e29b-41d4-a716-446655440001', 'patient-014', 'Susan', 'White', '1963-04-09', 'female', '357 Maple St', 'Peabody', 'MA', '01960', '555-0114', 'susan.white@email.com', 'MRN-014', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440114', '550e8400-e29b-41d4-a716-446655440001', 'patient-015', 'Joseph', 'Harris', '1956-11-26', 'male', '468 Cedar Dr', 'Danvers', 'MA', '01923', '555-0115', 'joseph.harris@email.com', 'MRN-015', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Quality Measure Results (HEDIS 2024)
INSERT INTO quality_measure_results (id, tenant_id, patient_id, measure_id, measure_name, measure_category, measure_year, numerator_compliant, denominator_eligible, compliance_rate, score, calculation_date, created_at, created_by, version) VALUES
-- CBP - Controlling Blood Pressure
('550e8400-e29b-41d4-a716-446655440200', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440100', 'HEDIS_CBP', 'Controlling Blood Pressure', 'HEDIS', 2024, true, true, 100, 100, NOW(), NOW(), 'system', 1),
('550e8400-e29b-41d4-a716-446655440201', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440101', 'HEDIS_CBP', 'Controlling Blood Pressure', 'HEDIS', 2024, false, true, 0, 0, NOW(), NOW(), 'system', 1),
('550e8400-e29b-41d4-a716-446655440202', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440102', 'HEDIS_CBP', 'Controlling Blood Pressure', 'HEDIS', 2024, true, true, 100, 100, NOW(), NOW(), 'system', 1),
('550e8400-e29b-41d4-a716-446655440203', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440103', 'HEDIS_CBP', 'Controlling Blood Pressure', 'HEDIS', 2024, false, true, 0, 0, NOW(), NOW(), 'system', 1),
('550e8400-e29b-41d4-a716-446655440204', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440104', 'HEDIS_CBP', 'Controlling Blood Pressure', 'HEDIS', 2024, true, true, 100, 100, NOW(), NOW(), 'system', 1),
-- CDC - Comprehensive Diabetes Care
('550e8400-e29b-41d4-a716-446655440210', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440105', 'HEDIS_CDC', 'Comprehensive Diabetes Care', 'HEDIS', 2024, true, true, 100, 100, NOW(), NOW(), 'system', 1),
('550e8400-e29b-41d4-a716-446655440211', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440106', 'HEDIS_CDC', 'Comprehensive Diabetes Care', 'HEDIS', 2024, false, true, 0, 0, NOW(), NOW(), 'system', 1),
('550e8400-e29b-41d4-a716-446655440212', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440107', 'HEDIS_CDC', 'Comprehensive Diabetes Care', 'HEDIS', 2024, true, true, 100, 100, NOW(), NOW(), 'system', 1),
('550e8400-e29b-41d4-a716-446655440213', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440108', 'HEDIS_CDC', 'Comprehensive Diabetes Care', 'HEDIS', 2024, true, true, 100, 100, NOW(), NOW(), 'system', 1),
-- COL - Colorectal Cancer Screening
('550e8400-e29b-41d4-a716-446655440220', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440109', 'HEDIS_COL', 'Colorectal Cancer Screening', 'HEDIS', 2024, true, true, 100, 100, NOW(), NOW(), 'system', 1),
('550e8400-e29b-41d4-a716-446655440221', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440110', 'HEDIS_COL', 'Colorectal Cancer Screening', 'HEDIS', 2024, false, true, 0, 0, NOW(), NOW(), 'system', 1),
('550e8400-e29b-41d4-a716-446655440222', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440111', 'HEDIS_COL', 'Colorectal Cancer Screening', 'HEDIS', 2024, false, true, 0, 0, NOW(), NOW(), 'system', 1),
-- BCS - Breast Cancer Screening
('550e8400-e29b-41d4-a716-446655440230', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440101', 'HEDIS_BCS', 'Breast Cancer Screening', 'HEDIS', 2024, true, true, 100, 100, NOW(), NOW(), 'system', 1),
('550e8400-e29b-41d4-a716-446655440231', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440103', 'HEDIS_BCS', 'Breast Cancer Screening', 'HEDIS', 2024, false, true, 0, 0, NOW(), NOW(), 'system', 1),
('550e8400-e29b-41d4-a716-446655440232', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440105', 'HEDIS_BCS', 'Breast Cancer Screening', 'HEDIS', 2024, true, true, 100, 100, NOW(), NOW(), 'system', 1)
ON CONFLICT (id) DO NOTHING;

-- Care Gaps
INSERT INTO care_gaps (id, tenant_id, patient_id, measure_id, measure_name, gap_type, status, priority, due_date, identified_date, created_at, updated_at) VALUES
-- Open care gaps (need action)
('550e8400-e29b-41d4-a716-446655440300', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440101', 'HEDIS_CBP', 'Controlling Blood Pressure', 'CHRONIC', 'OPEN', 'HIGH', '2024-12-31', NOW(), NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440301', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440103', 'HEDIS_CBP', 'Controlling Blood Pressure', 'CHRONIC', 'OPEN', 'HIGH', '2024-12-31', NOW(), NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440302', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440106', 'HEDIS_CDC', 'Comprehensive Diabetes Care', 'CHRONIC', 'OPEN', 'MEDIUM', '2024-12-31', NOW(), NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440303', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440110', 'HEDIS_COL', 'Colorectal Cancer Screening', 'PREVENTIVE', 'OPEN', 'MEDIUM', '2024-12-31', NOW(), NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440304', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440111', 'HEDIS_COL', 'Colorectal Cancer Screening', 'PREVENTIVE', 'OPEN', 'LOW', '2024-12-31', NOW(), NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440305', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440103', 'HEDIS_BCS', 'Breast Cancer Screening', 'PREVENTIVE', 'OPEN', 'MEDIUM', '2024-12-31', NOW(), NOW(), NOW()),
-- In-progress care gaps
('550e8400-e29b-41d4-a716-446655440310', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440112', 'HEDIS_CBP', 'Controlling Blood Pressure', 'CHRONIC', 'IN_PROGRESS', 'HIGH', '2024-12-31', NOW() - INTERVAL '30 days', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440311', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440113', 'HEDIS_CDC', 'Comprehensive Diabetes Care', 'CHRONIC', 'IN_PROGRESS', 'MEDIUM', '2024-12-31', NOW() - INTERVAL '14 days', NOW(), NOW()),
-- Closed care gaps (successfully addressed)
('550e8400-e29b-41d4-a716-446655440320', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440100', 'HEDIS_CBP', 'Controlling Blood Pressure', 'CHRONIC', 'CLOSED', 'HIGH', '2024-12-31', NOW() - INTERVAL '60 days', NOW() - INTERVAL '60 days', NOW()),
('550e8400-e29b-41d4-a716-446655440321', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440105', 'HEDIS_CDC', 'Comprehensive Diabetes Care', 'CHRONIC', 'CLOSED', 'MEDIUM', '2024-12-31', NOW() - INTERVAL '45 days', NOW() - INTERVAL '45 days', NOW()),
('550e8400-e29b-41d4-a716-446655440322', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440109', 'HEDIS_COL', 'Colorectal Cancer Screening', 'PREVENTIVE', 'CLOSED', 'MEDIUM', '2024-12-31', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days', NOW())
ON CONFLICT (id) DO NOTHING;
