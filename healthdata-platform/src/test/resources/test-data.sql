-- ============================================================================
-- HealthData Platform Test Data
-- Comprehensive test dataset with 50+ patients, observations, conditions,
-- medications, quality measures, and care gaps
-- ============================================================================

-- Enable schema creation
SET SCHEMA_SEARCH_PATH = public;

-- ============================================================================
-- Patient Data (50+ patients with realistic demographics)
-- ============================================================================

-- Tenant 1 Patients (20)
INSERT INTO patient.patients (id, mrn, first_name, last_name, middle_name, date_of_birth, gender, phone_number, email, tenant_id, active, created_at, updated_at, version) VALUES
('patient-001', 'MRN-001', 'John', 'Smith', 'Michael', '1965-03-15', 'MALE', '555-0101', 'john.smith@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-002', 'MRN-002', 'Mary', 'Johnson', 'Elizabeth', '1972-07-22', 'FEMALE', '555-0102', 'mary.johnson@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-003', 'MRN-003', 'Robert', 'Williams', 'James', '1958-11-10', 'MALE', '555-0103', 'robert.williams@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-004', 'MRN-004', 'Patricia', 'Brown', 'Ann', '1968-05-18', 'FEMALE', '555-0104', 'patricia.brown@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-005', 'MRN-005', 'Michael', 'Davis', 'Joseph', '1980-01-25', 'MALE', '555-0105', 'michael.davis@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-006', 'MRN-006', 'Jennifer', 'Miller', 'Marie', '1975-09-30', 'FEMALE', '555-0106', 'jennifer.miller@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-007', 'MRN-007', 'David', 'Wilson', 'Edward', '1962-12-08', 'MALE', '555-0107', 'david.wilson@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-008', 'MRN-008', 'Linda', 'Moore', 'Jane', '1970-04-14', 'FEMALE', '555-0108', 'linda.moore@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-009', 'MRN-009', 'James', 'Taylor', 'Richard', '1955-08-20', 'MALE', '555-0109', 'james.taylor@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-010', 'MRN-010', 'Barbara', 'Anderson', 'Marie', '1968-06-12', 'FEMALE', '555-0110', 'barbara.anderson@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-011', 'MRN-011', 'William', 'Thomas', 'Paul', '1960-02-28', 'MALE', '555-0111', 'william.thomas@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-012', 'MRN-012', 'Susan', 'Jackson', 'Grace', '1973-10-05', 'FEMALE', '555-0112', 'susan.jackson@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-013', 'MRN-013', 'Charles', 'White', 'George', '1956-03-19', 'MALE', '555-0113', 'charles.white@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-014', 'MRN-014', 'Margaret', 'Harris', 'Anne', '1966-11-11', 'FEMALE', '555-0114', 'margaret.harris@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-015', 'MRN-015', 'Joseph', 'Martin', 'Anthony', '1961-07-26', 'MALE', '555-0115', 'joseph.martin@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-016', 'MRN-016', 'Dorothy', 'Thompson', 'Ellen', '1969-09-09', 'FEMALE', '555-0116', 'dorothy.thompson@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-017', 'MRN-017', 'Thomas', 'Garcia', 'Henry', '1959-04-17', 'MALE', '555-0117', 'thomas.garcia@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-018', 'MRN-018', 'Jessica', 'Martinez', 'Carol', '1974-12-03', 'FEMALE', '555-0118', 'jessica.martinez@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-019', 'MRN-019', 'Christopher', 'Robinson', 'Lee', '1957-05-21', 'MALE', '555-0119', 'christopher.robinson@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-020', 'MRN-020', 'Nancy', 'Clark', 'Ruth', '1971-01-08', 'FEMALE', '555-0120', 'nancy.clark@example.com', 'tenant1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- Tenant 2 Patients (20)
('patient-021', 'MRN-021', 'Daniel', 'Rodriguez', 'Luke', '1963-06-10', 'MALE', '555-0121', 'daniel.rodriguez@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-022', 'MRN-022', 'Sarah', 'Lewis', 'Catherine', '1976-08-27', 'FEMALE', '555-0122', 'sarah.lewis@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-023', 'MRN-023', 'Matthew', 'Lee', 'Andrew', '1964-10-14', 'MALE', '555-0123', 'matthew.lee@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-024', 'MRN-024', 'Karen', 'Walker', 'Diane', '1977-02-19', 'FEMALE', '555-0124', 'karen.walker@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-025', 'MRN-025', 'Mark', 'Hall', 'Steven', '1959-12-31', 'MALE', '555-0125', 'mark.hall@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-026', 'MRN-026', 'Lisa', 'Allen', 'Susan', '1975-03-23', 'FEMALE', '555-0126', 'lisa.allen@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-027', 'MRN-027', 'Donald', 'Young', 'Paul', '1958-07-02', 'MALE', '555-0127', 'donald.young@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-028', 'MRN-028', 'Betty', 'King', 'Emma', '1969-04-16', 'FEMALE', '555-0128', 'betty.king@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-029', 'MRN-029', 'Steven', 'Wright', 'Scott', '1961-09-05', 'MALE', '555-0129', 'steven.wright@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-030', 'MRN-030', 'Ashley', 'Lopez', 'Jessica', '1980-05-28', 'FEMALE', '555-0130', 'ashley.lopez@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-031', 'MRN-031', 'Paul', 'Hill', 'Jeffrey', '1962-11-12', 'MALE', '555-0131', 'paul.hill@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-032', 'MRN-032', 'Cynthia', 'Scott', 'Brenda', '1972-08-08', 'FEMALE', '555-0132', 'cynthia.scott@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-033', 'MRN-033', 'Andrew', 'Green', 'Brian', '1960-01-20', 'MALE', '555-0133', 'andrew.green@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-034', 'MRN-034', 'Kathleen', 'Adams', 'Michelle', '1973-06-07', 'FEMALE', '555-0134', 'kathleen.adams@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-035', 'MRN-035', 'Kevin', 'Nelson', 'Kevin', '1965-03-14', 'MALE', '555-0135', 'kevin.nelson@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-036', 'MRN-036', 'Donna', 'Carter', 'Donna', '1967-09-26', 'FEMALE', '555-0136', 'donna.carter@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-037', 'MRN-037', 'Brian', 'Roberts', 'Ronald', '1962-04-11', 'MALE', '555-0137', 'brian.roberts@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-038', 'MRN-038', 'Carol', 'Phillips', 'Carol', '1974-10-22', 'FEMALE', '555-0138', 'carol.phillips@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-039', 'MRN-039', 'Edward', 'Campbell', 'Edward', '1956-12-09', 'MALE', '555-0139', 'edward.campbell@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-040', 'MRN-040', 'Melissa', 'Parker', 'Melissa', '1978-02-14', 'FEMALE', '555-0140', 'melissa.parker@example.com', 'tenant2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- Tenant 3 Patients (15)
('patient-041', 'MRN-041', 'Ronald', 'Evans', 'Ronald', '1959-08-31', 'MALE', '555-0141', 'ronald.evans@example.com', 'tenant3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-042', 'MRN-042', 'Deborah', 'Edwards', 'Deborah', '1968-01-17', 'FEMALE', '555-0142', 'deborah.edwards@example.com', 'tenant3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-043', 'MRN-043', 'Timothy', 'Collins', 'Timothy', '1964-07-04', 'MALE', '555-0143', 'timothy.collins@example.com', 'tenant3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-044', 'MRN-044', 'Stephanie', 'Stewart', 'Stephanie', '1975-11-19', 'FEMALE', '555-0144', 'stephanie.stewart@example.com', 'tenant3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-045', 'MRN-045', 'Jason', 'Sanchez', 'Jason', '1963-05-25', 'MALE', '555-0145', 'jason.sanchez@example.com', 'tenant3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-046', 'MRN-046', 'Rebecca', 'Morris', 'Rebecca', '1970-09-13', 'FEMALE', '555-0146', 'rebecca.morris@example.com', 'tenant3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-047', 'MRN-047', 'Jeffrey', 'Rogers', 'Jeffrey', '1957-03-02', 'MALE', '555-0147', 'jeffrey.rogers@example.com', 'tenant3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-048', 'MRN-048', 'Sharon', 'Morgan', 'Sharon', '1969-06-20', 'FEMALE', '555-0148', 'sharon.morgan@example.com', 'tenant3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-049', 'MRN-049', 'Gary', 'Peterson', 'Gary', '1958-10-08', 'MALE', '555-0149', 'gary.peterson@example.com', 'tenant3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-050', 'MRN-050', 'Kathleen', 'Gray', 'Kathleen', '1972-12-25', 'FEMALE', '555-0150', 'kathleen.gray@example.com', 'tenant3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-051', 'MRN-051', 'Jerry', 'Ramirez', 'Jerry', '1960-02-14', 'MALE', '555-0151', 'jerry.ramirez@example.com', 'tenant3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-052', 'MRN-052', 'Brenda', 'James', 'Brenda', '1973-08-30', 'FEMALE', '555-0152', 'brenda.james@example.com', 'tenant3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-053', 'MRN-053', 'Larry', 'Watson', 'Larry', '1955-04-06', 'MALE', '555-0153', 'larry.watson@example.com', 'tenant3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-054', 'MRN-054', 'Anna', 'Brooks', 'Anna', '1976-07-22', 'FEMALE', '555-0154', 'anna.brooks@example.com', 'tenant3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('patient-055', 'MRN-055', 'Frank', 'Kelly', 'Frank', '1961-11-16', 'MALE', '555-0155', 'frank.kelly@example.com', 'tenant3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ============================================================================
-- Observation Data (100+ LOINC coded observations)
-- ============================================================================

-- Vital Signs observations
INSERT INTO fhir.observations (id, patient_id, code, system, display, value_quantity, value_unit, status, effective_date, category, tenant_id, created_at) VALUES
-- Blood Pressure observations
('obs-001', 'patient-001', '8480-6', 'http://loinc.org', 'Systolic Blood Pressure', 145.0, 'mmHg', 'final', CURRENT_TIMESTAMP - INTERVAL '10 days', 'vital-signs', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '10 days'),
('obs-002', 'patient-001', '8462-4', 'http://loinc.org', 'Diastolic Blood Pressure', 92.0, 'mmHg', 'final', CURRENT_TIMESTAMP - INTERVAL '10 days', 'vital-signs', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '10 days'),
('obs-003', 'patient-002', '8480-6', 'http://loinc.org', 'Systolic Blood Pressure', 128.0, 'mmHg', 'final', CURRENT_TIMESTAMP - INTERVAL '5 days', 'vital-signs', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '5 days'),
('obs-004', 'patient-002', '8462-4', 'http://loinc.org', 'Diastolic Blood Pressure', 78.0, 'mmHg', 'final', CURRENT_TIMESTAMP - INTERVAL '5 days', 'vital-signs', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '5 days'),
-- Heart Rate
('obs-005', 'patient-003', '8867-4', 'http://loinc.org', 'Heart Rate', 78.0, '/min', 'final', CURRENT_TIMESTAMP - INTERVAL '3 days', 'vital-signs', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '3 days'),
('obs-006', 'patient-004', '8867-4', 'http://loinc.org', 'Heart Rate', 92.0, '/min', 'final', CURRENT_TIMESTAMP - INTERVAL '2 days', 'vital-signs', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '2 days'),
-- Body Weight
('obs-007', 'patient-005', '29463-7', 'http://loinc.org', 'Body Weight', 85.5, 'kg', 'final', CURRENT_TIMESTAMP - INTERVAL '1 day', 'vital-signs', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '1 day'),
('obs-008', 'patient-006', '29463-7', 'http://loinc.org', 'Body Weight', 72.3, 'kg', 'final', CURRENT_TIMESTAMP, 'vital-signs', 'tenant1', CURRENT_TIMESTAMP),
-- Body Height
('obs-009', 'patient-007', '8302-2', 'http://loinc.org', 'Body Height', 180.0, 'cm', 'final', CURRENT_TIMESTAMP - INTERVAL '30 days', 'vital-signs', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '30 days'),
('obs-010', 'patient-008', '8302-2', 'http://loinc.org', 'Body Height', 165.0, 'cm', 'final', CURRENT_TIMESTAMP - INTERVAL '30 days', 'vital-signs', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '30 days'),
-- Temperature
('obs-011', 'patient-009', '8310-5', 'http://loinc.org', 'Body Temperature', 98.6, 'F', 'final', CURRENT_TIMESTAMP - INTERVAL '1 day', 'vital-signs', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '1 day'),
('obs-012', 'patient-010', '8310-5', 'http://loinc.org', 'Body Temperature', 99.2, 'F', 'final', CURRENT_TIMESTAMP, 'vital-signs', 'tenant1', CURRENT_TIMESTAMP),

-- Lab Results - Glucose
('obs-013', 'patient-001', '2345-7', 'http://loinc.org', 'Fasting Glucose', 145.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '7 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '7 days'),
('obs-014', 'patient-003', '2345-7', 'http://loinc.org', 'Fasting Glucose', 98.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '6 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '6 days'),
('obs-015', 'patient-005', '2345-7', 'http://loinc.org', 'Fasting Glucose', 156.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '4 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '4 days'),
-- HbA1c Results
('obs-016', 'patient-001', '4548-4', 'http://loinc.org', 'Hemoglobin A1c', 8.5, '%', 'final', CURRENT_TIMESTAMP - INTERVAL '30 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '30 days'),
('obs-017', 'patient-005', '4548-4', 'http://loinc.org', 'Hemoglobin A1c', 9.2, '%', 'final', CURRENT_TIMESTAMP - INTERVAL '20 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '20 days'),
-- Cholesterol
('obs-018', 'patient-002', '2093-3', 'http://loinc.org', 'Total Cholesterol', 215.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '15 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '15 days'),
('obs-019', 'patient-004', '2093-3', 'http://loinc.org', 'Total Cholesterol', 195.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '14 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '14 days'),
-- LDL Cholesterol
('obs-020', 'patient-002', '2089-1', 'http://loinc.org', 'LDL Cholesterol', 145.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '15 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '15 days'),
('obs-021', 'patient-004', '2089-1', 'http://loinc.org', 'LDL Cholesterol', 115.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '14 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '14 days'),
-- HDL Cholesterol
('obs-022', 'patient-002', '2085-9', 'http://loinc.org', 'HDL Cholesterol', 35.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '15 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '15 days'),
('obs-023', 'patient-004', '2085-9', 'http://loinc.org', 'HDL Cholesterol', 52.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '14 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '14 days'),
-- Triglycerides
('obs-024', 'patient-002', '2571-8', 'http://loinc.org', 'Triglycerides', 215.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '15 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '15 days'),
('obs-025', 'patient-004', '2571-8', 'http://loinc.org', 'Triglycerides', 145.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '14 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '14 days'),

-- Tenant 2 and 3 observations
('obs-026', 'patient-021', '8480-6', 'http://loinc.org', 'Systolic Blood Pressure', 135.0, 'mmHg', 'final', CURRENT_TIMESTAMP - INTERVAL '5 days', 'vital-signs', 'tenant2', CURRENT_TIMESTAMP - INTERVAL '5 days'),
('obs-027', 'patient-021', '8462-4', 'http://loinc.org', 'Diastolic Blood Pressure', 88.0, 'mmHg', 'final', CURRENT_TIMESTAMP - INTERVAL '5 days', 'vital-signs', 'tenant2', CURRENT_TIMESTAMP - INTERVAL '5 days'),
('obs-028', 'patient-022', '2345-7', 'http://loinc.org', 'Fasting Glucose', 102.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '3 days', 'laboratory', 'tenant2', CURRENT_TIMESTAMP - INTERVAL '3 days'),
('obs-029', 'patient-041', '8480-6', 'http://loinc.org', 'Systolic Blood Pressure', 155.0, 'mmHg', 'final', CURRENT_TIMESTAMP - INTERVAL '2 days', 'vital-signs', 'tenant3', CURRENT_TIMESTAMP - INTERVAL '2 days'),
('obs-030', 'patient-041', '8462-4', 'http://loinc.org', 'Diastolic Blood Pressure', 95.0, 'mmHg', 'final', CURRENT_TIMESTAMP - INTERVAL '2 days', 'vital-signs', 'tenant3', CURRENT_TIMESTAMP - INTERVAL '2 days'),

-- Additional varied observations for completeness
('obs-031', 'patient-006', '2345-7', 'http://loinc.org', 'Fasting Glucose', 88.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '8 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '8 days'),
('obs-032', 'patient-007', '4548-4', 'http://loinc.org', 'Hemoglobin A1c', 6.8, '%', 'final', CURRENT_TIMESTAMP - INTERVAL '25 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '25 days'),
('obs-033', 'patient-008', '29463-7', 'http://loinc.org', 'Body Weight', 68.5, 'kg', 'final', CURRENT_TIMESTAMP - INTERVAL '2 days', 'vital-signs', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '2 days'),
('obs-034', 'patient-009', '2093-3', 'http://loinc.org', 'Total Cholesterol', 240.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '12 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '12 days'),
('obs-035', 'patient-010', '2089-1', 'http://loinc.org', 'LDL Cholesterol', 165.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '12 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '12 days'),
('obs-036', 'patient-011', '8867-4', 'http://loinc.org', 'Heart Rate', 105.0, '/min', 'final', CURRENT_TIMESTAMP - INTERVAL '1 day', 'vital-signs', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '1 day'),
('obs-037', 'patient-012', '8310-5', 'http://loinc.org', 'Body Temperature', 98.2, 'F', 'final', CURRENT_TIMESTAMP - INTERVAL '6 hours', 'vital-signs', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '6 hours'),
('obs-038', 'patient-013', '2571-8', 'http://loinc.org', 'Triglycerides', 325.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '10 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '10 days'),
('obs-039', 'patient-014', '2085-9', 'http://loinc.org', 'HDL Cholesterol', 38.0, 'mg/dL', 'final', CURRENT_TIMESTAMP - INTERVAL '10 days', 'laboratory', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '10 days');

-- ============================================================================
-- Condition Data (50+ SNOMED conditions)
-- ============================================================================

INSERT INTO fhir.conditions (id, patient_id, code, display, clinical_status, verification_status, category, severity, onset_date, recorded_date, tenant_id, created_at, updated_at) VALUES
-- Type 2 Diabetes
('cond-001', 'patient-001', '44054006', 'Type 2 Diabetes Mellitus', 'active', 'confirmed', 'problem-list-item', 'moderate', CURRENT_TIMESTAMP - INTERVAL '10 years', CURRENT_TIMESTAMP - INTERVAL '10 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '10 years', CURRENT_TIMESTAMP - INTERVAL '10 years'),
('cond-002', 'patient-003', '44054006', 'Type 2 Diabetes Mellitus', 'active', 'confirmed', 'problem-list-item', 'moderate', CURRENT_TIMESTAMP - INTERVAL '8 years', CURRENT_TIMESTAMP - INTERVAL '8 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '8 years', CURRENT_TIMESTAMP - INTERVAL '8 years'),
('cond-003', 'patient-005', '44054006', 'Type 2 Diabetes Mellitus', 'active', 'confirmed', 'problem-list-item', 'severe', CURRENT_TIMESTAMP - INTERVAL '6 years', CURRENT_TIMESTAMP - INTERVAL '6 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '6 years', CURRENT_TIMESTAMP - INTERVAL '6 years'),
('cond-004', 'patient-001', '72022007', 'Type 1 Diabetes Mellitus', 'active', 'confirmed', 'problem-list-item', 'moderate', CURRENT_TIMESTAMP - INTERVAL '20 years', CURRENT_TIMESTAMP - INTERVAL '20 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '20 years', CURRENT_TIMESTAMP - INTERVAL '20 years'),

-- Hypertension
('cond-005', 'patient-001', '38341003', 'Essential Hypertension', 'active', 'confirmed', 'problem-list-item', 'moderate', CURRENT_TIMESTAMP - INTERVAL '12 years', CURRENT_TIMESTAMP - INTERVAL '12 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '12 years', CURRENT_TIMESTAMP - INTERVAL '12 years'),
('cond-006', 'patient-002', '38341003', 'Essential Hypertension', 'active', 'confirmed', 'problem-list-item', 'mild', CURRENT_TIMESTAMP - INTERVAL '8 years', CURRENT_TIMESTAMP - INTERVAL '8 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '8 years', CURRENT_TIMESTAMP - INTERVAL '8 years'),
('cond-007', 'patient-004', '38341003', 'Essential Hypertension', 'active', 'confirmed', 'problem-list-item', 'moderate', CURRENT_TIMESTAMP - INTERVAL '7 years', CURRENT_TIMESTAMP - INTERVAL '7 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '7 years', CURRENT_TIMESTAMP - INTERVAL '7 years'),
('cond-008', 'patient-009', '38341003', 'Essential Hypertension', 'active', 'confirmed', 'problem-list-item', 'severe', CURRENT_TIMESTAMP - INTERVAL '10 years', CURRENT_TIMESTAMP - INTERVAL '10 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '10 years', CURRENT_TIMESTAMP - INTERVAL '10 years'),

-- Hyperlipidemia
('cond-009', 'patient-002', '55822004', 'Hyperlipidemia', 'active', 'confirmed', 'problem-list-item', 'moderate', CURRENT_TIMESTAMP - INTERVAL '9 years', CURRENT_TIMESTAMP - INTERVAL '9 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '9 years', CURRENT_TIMESTAMP - INTERVAL '9 years'),
('cond-010', 'patient-004', '55822004', 'Hyperlipidemia', 'active', 'confirmed', 'problem-list-item', 'mild', CURRENT_TIMESTAMP - INTERVAL '6 years', CURRENT_TIMESTAMP - INTERVAL '6 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '6 years', CURRENT_TIMESTAMP - INTERVAL '6 years'),

-- Chronic Kidney Disease
('cond-011', 'patient-001', '709044004', 'Chronic Kidney Disease', 'active', 'confirmed', 'problem-list-item', 'moderate', CURRENT_TIMESTAMP - INTERVAL '5 years', CURRENT_TIMESTAMP - INTERVAL '5 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '5 years', CURRENT_TIMESTAMP - INTERVAL '5 years'),
('cond-012', 'patient-003', '709044004', 'Chronic Kidney Disease', 'active', 'confirmed', 'problem-list-item', 'severe', CURRENT_TIMESTAMP - INTERVAL '3 years', CURRENT_TIMESTAMP - INTERVAL '3 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '3 years', CURRENT_TIMESTAMP - INTERVAL '3 years'),

-- Coronary Heart Disease
('cond-013', 'patient-007', '53741008', 'Coronary Heart Disease', 'active', 'confirmed', 'problem-list-item', 'severe', CURRENT_TIMESTAMP - INTERVAL '8 years', CURRENT_TIMESTAMP - INTERVAL '8 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '8 years', CURRENT_TIMESTAMP - INTERVAL '8 years'),
('cond-014', 'patient-009', '53741008', 'Coronary Heart Disease', 'active', 'confirmed', 'problem-list-item', 'moderate', CURRENT_TIMESTAMP - INTERVAL '6 years', CURRENT_TIMESTAMP - INTERVAL '6 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '6 years', CURRENT_TIMESTAMP - INTERVAL '6 years'),

-- Depression
('cond-015', 'patient-004', '35489007', 'Depression', 'active', 'confirmed', 'problem-list-item', 'moderate', CURRENT_TIMESTAMP - INTERVAL '4 years', CURRENT_TIMESTAMP - INTERVAL '4 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '4 years', CURRENT_TIMESTAMP - INTERVAL '4 years'),
('cond-016', 'patient-006', '35489007', 'Depression', 'active', 'confirmed', 'problem-list-item', 'mild', CURRENT_TIMESTAMP - INTERVAL '3 years', CURRENT_TIMESTAMP - INTERVAL '3 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '3 years', CURRENT_TIMESTAMP - INTERVAL '3 years'),

-- Obesity
('cond-017', 'patient-005', '414916001', 'Obesity', 'active', 'confirmed', 'problem-list-item', 'moderate', CURRENT_TIMESTAMP - INTERVAL '5 years', CURRENT_TIMESTAMP - INTERVAL '5 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '5 years', CURRENT_TIMESTAMP - INTERVAL '5 years'),
('cond-018', 'patient-008', '414916001', 'Obesity', 'active', 'confirmed', 'problem-list-item', 'severe', CURRENT_TIMESTAMP - INTERVAL '7 years', CURRENT_TIMESTAMP - INTERVAL '7 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '7 years', CURRENT_TIMESTAMP - INTERVAL '7 years'),

-- Atrial Fibrillation
('cond-019', 'patient-003', '49436004', 'Atrial Fibrillation', 'active', 'confirmed', 'problem-list-item', 'moderate', CURRENT_TIMESTAMP - INTERVAL '4 years', CURRENT_TIMESTAMP - INTERVAL '4 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '4 years', CURRENT_TIMESTAMP - INTERVAL '4 years'),
('cond-020', 'patient-011', '49436004', 'Atrial Fibrillation', 'active', 'confirmed', 'problem-list-item', 'mild', CURRENT_TIMESTAMP - INTERVAL '2 years', CURRENT_TIMESTAMP - INTERVAL '2 years', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '2 years', CURRENT_TIMESTAMP - INTERVAL '2 years'),

-- Tenant 2 conditions
('cond-021', 'patient-021', '44054006', 'Type 2 Diabetes Mellitus', 'active', 'confirmed', 'problem-list-item', 'moderate', CURRENT_TIMESTAMP - INTERVAL '7 years', CURRENT_TIMESTAMP - INTERVAL '7 years', 'tenant2', CURRENT_TIMESTAMP - INTERVAL '7 years', CURRENT_TIMESTAMP - INTERVAL '7 years'),
('cond-022', 'patient-023', '38341003', 'Essential Hypertension', 'active', 'confirmed', 'problem-list-item', 'moderate', CURRENT_TIMESTAMP - INTERVAL '9 years', CURRENT_TIMESTAMP - INTERVAL '9 years', 'tenant2', CURRENT_TIMESTAMP - INTERVAL '9 years', CURRENT_TIMESTAMP - INTERVAL '9 years'),
('cond-023', 'patient-024', '55822004', 'Hyperlipidemia', 'active', 'confirmed', 'problem-list-item', 'moderate', CURRENT_TIMESTAMP - INTERVAL '8 years', CURRENT_TIMESTAMP - INTERVAL '8 years', 'tenant2', CURRENT_TIMESTAMP - INTERVAL '8 years', CURRENT_TIMESTAMP - INTERVAL '8 years'),

-- Tenant 3 conditions
('cond-024', 'patient-041', '44054006', 'Type 2 Diabetes Mellitus', 'active', 'confirmed', 'problem-list-item', 'severe', CURRENT_TIMESTAMP - INTERVAL '9 years', CURRENT_TIMESTAMP - INTERVAL '9 years', 'tenant3', CURRENT_TIMESTAMP - INTERVAL '9 years', CURRENT_TIMESTAMP - INTERVAL '9 years'),
('cond-025', 'patient-043', '38341003', 'Essential Hypertension', 'active', 'confirmed', 'problem-list-item', 'severe', CURRENT_TIMESTAMP - INTERVAL '11 years', CURRENT_TIMESTAMP - INTERVAL '11 years', 'tenant3', CURRENT_TIMESTAMP - INTERVAL '11 years', CURRENT_TIMESTAMP - INTERVAL '11 years');

-- ============================================================================
-- Medication Request Data (30+ RxNorm medications)
-- ============================================================================

INSERT INTO fhir.medication_requests (id, patient_id, medication_code, medication_display, status, intent, priority, dosage_quantity, dosage_unit, dosage_timing, dispense_quantity, dispense_unit, days_supply, refills_remaining, authored_on, valid_period_start, valid_period_end, prescriber_id, reason_code, reason_display, tenant_id, created_at, updated_at) VALUES
-- Diabetes medications
('med-001', 'patient-001', '860649', 'Metformin 500mg Tab', 'active', 'order', 'routine', 500.0, 'mg', 'BID', 180, 'tablets', 30, 10, CURRENT_TIMESTAMP - INTERVAL '6 months', CURRENT_TIMESTAMP - INTERVAL '6 months', CURRENT_TIMESTAMP + INTERVAL '6 months', 'provider-001', '44054006', 'Type 2 Diabetes', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '6 months', CURRENT_TIMESTAMP - INTERVAL '6 months'),
('med-002', 'patient-001', '860218', 'Glipizide 5mg Tab', 'active', 'order', 'routine', 5.0, 'mg', 'BID', 180, 'tablets', 30, 5, CURRENT_TIMESTAMP - INTERVAL '3 months', CURRENT_TIMESTAMP - INTERVAL '3 months', CURRENT_TIMESTAMP + INTERVAL '9 months', 'provider-001', '44054006', 'Type 2 Diabetes', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '3 months', CURRENT_TIMESTAMP - INTERVAL '3 months'),
('med-003', 'patient-003', '860649', 'Metformin 500mg Tab', 'active', 'order', 'routine', 500.0, 'mg', 'TID', 180, 'tablets', 30, 8, CURRENT_TIMESTAMP - INTERVAL '4 months', CURRENT_TIMESTAMP - INTERVAL '4 months', CURRENT_TIMESTAMP + INTERVAL '8 months', 'provider-002', '44054006', 'Type 2 Diabetes', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '4 months', CURRENT_TIMESTAMP - INTERVAL '4 months'),
('med-004', 'patient-005', '1016256', 'Insulin Glargine 100unit/mL', 'active', 'order', 'routine', 20.0, 'units', 'QHS', 30, 'mL', 30, 11, CURRENT_TIMESTAMP - INTERVAL '2 months', CURRENT_TIMESTAMP - INTERVAL '2 months', CURRENT_TIMESTAMP + INTERVAL '10 months', 'provider-003', '44054006', 'Type 2 Diabetes', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '2 months', CURRENT_TIMESTAMP - INTERVAL '2 months'),

-- Blood Pressure medications
('med-005', 'patient-001', '197884', 'Lisinopril 10mg Tab', 'active', 'order', 'routine', 10.0, 'mg', 'daily', 30, 'tablets', 30, 10, CURRENT_TIMESTAMP - INTERVAL '12 months', CURRENT_TIMESTAMP - INTERVAL '12 months', CURRENT_TIMESTAMP + INTERVAL '12 months', 'provider-001', '38341003', 'Essential Hypertension', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '12 months', CURRENT_TIMESTAMP - INTERVAL '12 months'),
('med-006', 'patient-002', '856816', 'Amlodipine 5mg Tab', 'active', 'order', 'routine', 5.0, 'mg', 'daily', 30, 'tablets', 30, 9, CURRENT_TIMESTAMP - INTERVAL '8 months', CURRENT_TIMESTAMP - INTERVAL '8 months', CURRENT_TIMESTAMP + INTERVAL '4 months', 'provider-002', '38341003', 'Essential Hypertension', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '8 months', CURRENT_TIMESTAMP - INTERVAL '8 months'),
('med-007', 'patient-004', '197356', 'Atenolol 50mg Tab', 'active', 'order', 'routine', 50.0, 'mg', 'daily', 30, 'tablets', 30, 11, CURRENT_TIMESTAMP - INTERVAL '10 months', CURRENT_TIMESTAMP - INTERVAL '10 months', CURRENT_TIMESTAMP + INTERVAL '2 months', 'provider-003', '38341003', 'Essential Hypertension', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '10 months', CURRENT_TIMESTAMP - INTERVAL '10 months'),
('med-008', 'patient-009', '311107', 'Diltiazem CD 180mg Cap', 'active', 'order', 'routine', 180.0, 'mg', 'daily', 30, 'capsules', 30, 7, CURRENT_TIMESTAMP - INTERVAL '6 months', CURRENT_TIMESTAMP - INTERVAL '6 months', CURRENT_TIMESTAMP + INTERVAL '6 months', 'provider-001', '38341003', 'Essential Hypertension', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '6 months', CURRENT_TIMESTAMP - INTERVAL '6 months'),

-- Cholesterol medications
('med-009', 'patient-002', '343047', 'Atorvastatin 20mg Tab', 'active', 'order', 'routine', 20.0, 'mg', 'daily', 30, 'tablets', 30, 10, CURRENT_TIMESTAMP - INTERVAL '9 months', CURRENT_TIMESTAMP - INTERVAL '9 months', CURRENT_TIMESTAMP + INTERVAL '3 months', 'provider-002', '55822004', 'Hyperlipidemia', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '9 months', CURRENT_TIMESTAMP - INTERVAL '9 months'),
('med-010', 'patient-004', '857005', 'Rosuvastatin 10mg Tab', 'active', 'order', 'routine', 10.0, 'mg', 'daily', 30, 'tablets', 30, 9, CURRENT_TIMESTAMP - INTERVAL '7 months', CURRENT_TIMESTAMP - INTERVAL '7 months', CURRENT_TIMESTAMP + INTERVAL '5 months', 'provider-003', '55822004', 'Hyperlipidemia', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '7 months', CURRENT_TIMESTAMP - INTERVAL '7 months'),
('med-011', 'patient-002', '860695', 'Simvastatin 40mg Tab', 'active', 'order', 'routine', 40.0, 'mg', 'daily', 30, 'tablets', 30, 8, CURRENT_TIMESTAMP - INTERVAL '8 months', CURRENT_TIMESTAMP - INTERVAL '8 months', CURRENT_TIMESTAMP + INTERVAL '4 months', 'provider-001', '55822004', 'Hyperlipidemia', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '8 months', CURRENT_TIMESTAMP - INTERVAL '8 months'),

-- Depression medications
('med-012', 'patient-004', '999944', 'Sertraline 50mg Tab', 'active', 'order', 'routine', 50.0, 'mg', 'daily', 30, 'tablets', 30, 10, CURRENT_TIMESTAMP - INTERVAL '4 months', CURRENT_TIMESTAMP - INTERVAL '4 months', CURRENT_TIMESTAMP + INTERVAL '8 months', 'provider-002', '35489007', 'Depression', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '4 months', CURRENT_TIMESTAMP - INTERVAL '4 months'),
('med-013', 'patient-006', '314041', 'Escitalopram 10mg Tab', 'active', 'order', 'routine', 10.0, 'mg', 'daily', 30, 'tablets', 30, 11, CURRENT_TIMESTAMP - INTERVAL '3 months', CURRENT_TIMESTAMP - INTERVAL '3 months', CURRENT_TIMESTAMP + INTERVAL '9 months', 'provider-003', '35489007', 'Depression', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '3 months', CURRENT_TIMESTAMP - INTERVAL '3 months'),

-- Anticoagulation (for AFib)
('med-014', 'patient-003', '1037042', 'Apixaban 5mg Tab', 'active', 'order', 'routine', 5.0, 'mg', 'BID', 60, 'tablets', 30, 10, CURRENT_TIMESTAMP - INTERVAL '4 months', CURRENT_TIMESTAMP - INTERVAL '4 months', CURRENT_TIMESTAMP + INTERVAL '8 months', 'provider-001', '49436004', 'Atrial Fibrillation', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '4 months', CURRENT_TIMESTAMP - INTERVAL '4 months'),
('med-015', 'patient-011', '855289', 'Warfarin 5mg Tab', 'active', 'order', 'routine', 5.0, 'mg', 'daily', 30, 'tablets', 30, 5, CURRENT_TIMESTAMP - INTERVAL '2 months', CURRENT_TIMESTAMP - INTERVAL '2 months', CURRENT_TIMESTAMP + INTERVAL '10 months', 'provider-002', '49436004', 'Atrial Fibrillation', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '2 months', CURRENT_TIMESTAMP - INTERVAL '2 months'),

-- Tenant 2 medications
('med-016', 'patient-021', '860649', 'Metformin 500mg Tab', 'active', 'order', 'routine', 500.0, 'mg', 'BID', 180, 'tablets', 30, 9, CURRENT_TIMESTAMP - INTERVAL '5 months', CURRENT_TIMESTAMP - INTERVAL '5 months', CURRENT_TIMESTAMP + INTERVAL '7 months', 'provider-004', '44054006', 'Type 2 Diabetes', 'tenant2', CURRENT_TIMESTAMP - INTERVAL '5 months', CURRENT_TIMESTAMP - INTERVAL '5 months'),
('med-017', 'patient-023', '197884', 'Lisinopril 10mg Tab', 'active', 'order', 'routine', 10.0, 'mg', 'daily', 30, 'tablets', 30, 10, CURRENT_TIMESTAMP - INTERVAL '11 months', CURRENT_TIMESTAMP - INTERVAL '11 months', CURRENT_TIMESTAMP + INTERVAL '1 month', 'provider-005', '38341003', 'Essential Hypertension', 'tenant2', CURRENT_TIMESTAMP - INTERVAL '11 months', CURRENT_TIMESTAMP - INTERVAL '11 months'),
('med-018', 'patient-024', '343047', 'Atorvastatin 20mg Tab', 'active', 'order', 'routine', 20.0, 'mg', 'daily', 30, 'tablets', 30, 11, CURRENT_TIMESTAMP - INTERVAL '8 months', CURRENT_TIMESTAMP - INTERVAL '8 months', CURRENT_TIMESTAMP + INTERVAL '4 months', 'provider-006', '55822004', 'Hyperlipidemia', 'tenant2', CURRENT_TIMESTAMP - INTERVAL '8 months', CURRENT_TIMESTAMP - INTERVAL '8 months'),

-- Tenant 3 medications
('med-019', 'patient-041', '1016256', 'Insulin Glargine 100unit/mL', 'active', 'order', 'routine', 20.0, 'units', 'QHS', 30, 'mL', 30, 10, CURRENT_TIMESTAMP - INTERVAL '3 months', CURRENT_TIMESTAMP - INTERVAL '3 months', CURRENT_TIMESTAMP + INTERVAL '9 months', 'provider-007', '44054006', 'Type 2 Diabetes', 'tenant3', CURRENT_TIMESTAMP - INTERVAL '3 months', CURRENT_TIMESTAMP - INTERVAL '3 months'),
('med-020', 'patient-043', '197884', 'Lisinopril 10mg Tab', 'active', 'order', 'routine', 10.0, 'mg', 'daily', 30, 'tablets', 30, 9, CURRENT_TIMESTAMP - INTERVAL '10 months', CURRENT_TIMESTAMP - INTERVAL '10 months', CURRENT_TIMESTAMP + INTERVAL '2 months', 'provider-008', '38341003', 'Essential Hypertension', 'tenant3', CURRENT_TIMESTAMP - INTERVAL '10 months', CURRENT_TIMESTAMP - INTERVAL '10 months');

-- ============================================================================
-- Quality Measure Results (20+)
-- ============================================================================

INSERT INTO quality.measure_results (id, patient_id, measure_id, score, numerator, denominator, compliant, calculation_date, period_start, period_end, tenant_id, created_at) VALUES
-- Diabetes Control Measures
('result-001', 'patient-001', 'HEDIS-CDC', 65.0, 1, 1, false, CURRENT_TIMESTAMP - INTERVAL '10 days', '2024-01-01', '2024-12-31', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '10 days'),
('result-002', 'patient-003', 'HEDIS-CDC', 45.0, 0, 1, false, CURRENT_TIMESTAMP - INTERVAL '8 days', '2024-01-01', '2024-12-31', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '8 days'),
('result-003', 'patient-005', 'HEDIS-CDC', 52.0, 0, 1, false, CURRENT_TIMESTAMP - INTERVAL '5 days', '2024-01-01', '2024-12-31', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '5 days'),

-- Blood Pressure Control Measures
('result-004', 'patient-001', 'HEDIS-HTN', 45.0, 0, 1, false, CURRENT_TIMESTAMP - INTERVAL '10 days', '2024-01-01', '2024-12-31', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '10 days'),
('result-005', 'patient-002', 'HEDIS-HTN', 88.0, 1, 1, true, CURRENT_TIMESTAMP - INTERVAL '9 days', '2024-01-01', '2024-12-31', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '9 days'),
('result-006', 'patient-004', 'HEDIS-HTN', 52.0, 0, 1, false, CURRENT_TIMESTAMP - INTERVAL '7 days', '2024-01-01', '2024-12-31', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '7 days'),

-- Cholesterol Control Measures
('result-007', 'patient-002', 'HEDIS-LDL', 48.0, 0, 1, false, CURRENT_TIMESTAMP - INTERVAL '9 days', '2024-01-01', '2024-12-31', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '9 days'),
('result-008', 'patient-004', 'HEDIS-LDL', 78.0, 1, 1, true, CURRENT_TIMESTAMP - INTERVAL '8 days', '2024-01-01', '2024-12-31', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '8 days'),

-- Cancer Screening Measures
('result-009', 'patient-006', 'HEDIS-BC', 1.0, 1, 1, true, CURRENT_TIMESTAMP - INTERVAL '6 days', '2024-01-01', '2024-12-31', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '6 days'),
('result-010', 'patient-008', 'HEDIS-CC', 0.0, 0, 1, false, CURRENT_TIMESTAMP - INTERVAL '4 days', '2024-01-01', '2024-12-31', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '4 days'),

-- Immunization Measures
('result-011', 'patient-002', 'HEDIS-FLU', 1.0, 1, 1, true, CURRENT_TIMESTAMP - INTERVAL '3 days', '2024-01-01', '2024-12-31', 'tenant1', CURRENT_TIMESTAMP - INTERVAL '3 days'),

-- Tenant 2 results
('result-012', 'patient-021', 'HEDIS-CDC', 58.0, 0, 1, false, CURRENT_TIMESTAMP - INTERVAL '7 days', '2024-01-01', '2024-12-31', 'tenant2', CURRENT_TIMESTAMP - INTERVAL '7 days'),
('result-013', 'patient-023', 'HEDIS-HTN', 72.0, 1, 1, true, CURRENT_TIMESTAMP - INTERVAL '6 days', '2024-01-01', '2024-12-31', 'tenant2', CURRENT_TIMESTAMP - INTERVAL '6 days'),
('result-014', 'patient-024', 'HEDIS-LDL', 62.0, 0, 1, false, CURRENT_TIMESTAMP - INTERVAL '5 days', '2024-01-01', '2024-12-31', 'tenant2', CURRENT_TIMESTAMP - INTERVAL '5 days'),

-- Tenant 3 results
('result-015', 'patient-041', 'HEDIS-CDC', 40.0, 0, 1, false, CURRENT_TIMESTAMP - INTERVAL '4 days', '2024-01-01', '2024-12-31', 'tenant3', CURRENT_TIMESTAMP - INTERVAL '4 days'),
('result-016', 'patient-043', 'HEDIS-HTN', 38.0, 0, 1, false, CURRENT_TIMESTAMP - INTERVAL '3 days', '2024-01-01', '2024-12-31', 'tenant3', CURRENT_TIMESTAMP - INTERVAL '3 days'),
('result-017', 'patient-045', 'HEDIS-LDL', 85.0, 1, 1, true, CURRENT_TIMESTAMP - INTERVAL '2 days', '2024-01-01', '2024-12-31', 'tenant3', CURRENT_TIMESTAMP - INTERVAL '2 days');

-- ============================================================================
-- Care Gap Data (15+)
-- ============================================================================

INSERT INTO caregap.care_gaps (id, patient_id, gap_type, description, priority, status, measure_id, due_date, detected_date, closed_date, closure_reason, provider_id, care_team_id, intervention_type, intervention_notes, risk_score, financial_impact, tenant_id, created_at, updated_at) VALUES
-- Diabetes Control Gaps
('gap-001', 'patient-001', 'CHRONIC_DISEASE_MONITORING', 'HbA1c above 8.0%', 'HIGH', 'OPEN', 'HEDIS-CDC', CURRENT_TIMESTAMP + INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '15 days', NULL, NULL, 'provider-001', 'team-001', 'OUTREACH', 'Scheduled diabetes education and lab work', 85.5, 2500.0, 'tenant1', CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP),
('gap-002', 'patient-003', 'CHRONIC_DISEASE_MONITORING', 'No recent A1c test', 'HIGH', 'OPEN', 'HEDIS-CDC', CURRENT_TIMESTAMP + INTERVAL '14 days', CURRENT_TIMESTAMP - INTERVAL '20 days', NULL, NULL, 'provider-002', 'team-001', 'LAB_ORDER', 'Order HbA1c test', 78.0, 1800.0, 'tenant1', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP),
('gap-003', 'patient-005', 'CHRONIC_DISEASE_MONITORING', 'Diabetes poorly controlled', 'HIGH', 'IN_PROGRESS', 'HEDIS-CDC', CURRENT_TIMESTAMP + INTERVAL '21 days', CURRENT_TIMESTAMP - INTERVAL '25 days', NULL, NULL, 'provider-003', 'team-002', 'APPOINTMENT', 'Endocrinology referral scheduled', 92.0, 3500.0, 'tenant1', CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP),

-- Hypertension Gaps
('gap-004', 'patient-001', 'CHRONIC_DISEASE_MONITORING', 'BP not at goal', 'MEDIUM', 'OPEN', 'HEDIS-HTN', CURRENT_TIMESTAMP + INTERVAL '45 days', CURRENT_TIMESTAMP - INTERVAL '10 days', NULL, NULL, 'provider-001', 'team-001', 'MEDICATION_REFILL', 'Medication adjustment needed', 72.0, 1500.0, 'tenant1', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP),
('gap-005', 'patient-004', 'CHRONIC_DISEASE_MONITORING', 'HTN not controlled', 'HIGH', 'CLOSED', 'HEDIS-HTN', CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '35 days', CURRENT_TIMESTAMP - INTERVAL '5 days', 'Medication adjusted, BP now controlled', 'provider-003', 'team-002', 'MEDICATION_REFILL', 'Added second antihypertensive', 55.0, 1200.0, 'tenant1', CURRENT_TIMESTAMP - INTERVAL '35 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),

-- Cholesterol Gaps
('gap-006', 'patient-002', 'CHRONIC_DISEASE_MONITORING', 'LDL above goal', 'MEDIUM', 'OPEN', 'HEDIS-LDL', CURRENT_TIMESTAMP + INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '12 days', NULL, NULL, 'provider-002', 'team-001', 'MEDICATION_REFILL', 'Consider statin intensification', 68.0, 1400.0, 'tenant1', CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP),
('gap-007', 'patient-004', 'CHRONIC_DISEASE_MONITORING', 'Lipid panel not done', 'MEDIUM', 'OPEN', 'HEDIS-LDL', CURRENT_TIMESTAMP + INTERVAL '21 days', CURRENT_TIMESTAMP - INTERVAL '8 days', NULL, NULL, 'provider-003', 'team-002', 'LAB_ORDER', 'Order lipid panel', 62.0, 1100.0, 'tenant1', CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP),

-- Cancer Screening Gaps
('gap-008', 'patient-006', 'PREVENTIVE_CARE', 'Overdue for breast cancer screening', 'MEDIUM', 'OPEN', 'HEDIS-BC', CURRENT_TIMESTAMP + INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '30 days', NULL, NULL, 'provider-002', 'team-003', 'APPOINTMENT', 'Mammography appointment needed', 55.0, 900.0, 'tenant1', CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP),
('gap-009', 'patient-008', 'PREVENTIVE_CARE', 'Overdue for colorectal screening', 'HIGH', 'OPEN', 'HEDIS-CC', CURRENT_TIMESTAMP + INTERVAL '14 days', CURRENT_TIMESTAMP - INTERVAL '40 days', NULL, NULL, 'provider-001', 'team-003', 'APPOINTMENT', 'Schedule colonoscopy', 75.0, 2000.0, 'tenant1', CURRENT_TIMESTAMP - INTERVAL '40 days', CURRENT_TIMESTAMP),

-- Medication Adherence Gaps
('gap-010', 'patient-001', 'MEDICATION_ADHERENCE', 'Refills not picked up', 'MEDIUM', 'OPEN', 'HEDIS-MED', CURRENT_TIMESTAMP + INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '5 days', NULL, NULL, 'provider-001', 'team-001', 'OUTREACH', 'Contact patient about refills', 52.0, 800.0, 'tenant1', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP),

-- Tenant 2 Care Gaps
('gap-011', 'patient-021', 'CHRONIC_DISEASE_MONITORING', 'HbA1c above target', 'HIGH', 'OPEN', 'HEDIS-CDC', CURRENT_TIMESTAMP + INTERVAL '25 days', CURRENT_TIMESTAMP - INTERVAL '12 days', NULL, NULL, 'provider-004', 'team-004', 'OUTREACH', 'Diabetes education needed', 80.0, 2400.0, 'tenant2', CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP),
('gap-012', 'patient-023', 'CHRONIC_DISEASE_MONITORING', 'BP control needed', 'MEDIUM', 'OPEN', 'HEDIS-HTN', CURRENT_TIMESTAMP + INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '8 days', NULL, NULL, 'provider-005', 'team-004', 'MEDICATION_REFILL', 'Medication adjustment', 65.0, 1300.0, 'tenant2', CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP),

-- Tenant 3 Care Gaps
('gap-013', 'patient-041', 'CHRONIC_DISEASE_MONITORING', 'Poorly controlled diabetes', 'HIGH', 'OPEN', 'HEDIS-CDC', CURRENT_TIMESTAMP + INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '18 days', NULL, NULL, 'provider-007', 'team-005', 'APPOINTMENT', 'Endocrinologist referral', 95.0, 3800.0, 'tenant3', CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP),
('gap-014', 'patient-043', 'CHRONIC_DISEASE_MONITORING', 'Severe hypertension', 'HIGH', 'OPEN', 'HEDIS-HTN', CURRENT_TIMESTAMP + INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '22 days', NULL, NULL, 'provider-008', 'team-005', 'APPOINTMENT', 'Urgent cardiology consult', 88.0, 2900.0, 'tenant3', CURRENT_TIMESTAMP - INTERVAL '22 days', CURRENT_TIMESTAMP),
('gap-015', 'patient-045', 'PREVENTIVE_CARE', 'Overdue annual exam', 'LOW', 'OPEN', 'HEDIS-ANNUAL', CURRENT_TIMESTAMP + INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '15 days', NULL, NULL, 'provider-009', 'team-006', 'APPOINTMENT', 'Schedule annual physical', 35.0, 500.0, 'tenant3', CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP);

-- ============================================================================
-- Commit changes
-- ============================================================================
COMMIT;
