INSERT INTO patients (id, mrn, first_name, last_name, date_of_birth, sex, race, ethnicity, address_line1, city, state, zip, phone, email)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'MRN-10001', 'Maria', 'Sanchez', '1978-04-12', 'F', 'White', 'Hispanic', '104 W 14th St', 'New York', 'NY', '10011', '212-555-0101', 'maria.sanchez@example.com'),
  ('22222222-2222-2222-2222-222222222222', 'MRN-10002', 'Thomas', 'Reed', '1965-09-03', 'M', 'Black', 'Not Hispanic', '88 Broadway', 'New York', 'NY', '10007', '212-555-0134', 'thomas.reed@example.com'),
  ('33333333-3333-3333-3333-333333333333', 'MRN-10003', 'Aisha', 'Khan', '1990-01-22', 'F', 'Asian', 'Not Hispanic', '22 Essex St', 'New York', 'NY', '10002', '212-555-0188', 'aisha.khan@example.com');

INSERT INTO encounters (id, patient_id, encounter_date, encounter_type, provider_npi, facility, reason, discharge_date)
VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', '2022-03-10 09:30:00-05', 'Outpatient', '1234567890', 'Downtown Clinic', 'Annual physical', '2022-03-10 10:15:00-05'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', '2023-11-04 08:00:00-05', 'Outpatient', '1234567890', 'Downtown Clinic', 'Diabetes follow-up', '2023-11-04 09:05:00-05'),
  ('cccccccc-cccc-cccc-cccc-cccccccccccc', '22222222-2222-2222-2222-222222222222', '2021-06-18 14:15:00-05', 'Inpatient', '9988776655', 'Metro Hospital', 'Chest pain', '2021-06-21 11:00:00-05'),
  ('dddddddd-dddd-dddd-dddd-dddddddddddd', '33333333-3333-3333-3333-333333333333', '2024-01-09 15:45:00-05', 'Emergency', '5544332211', 'Metro Hospital', 'Asthma exacerbation', '2024-01-09 20:30:00-05');

INSERT INTO conditions (id, patient_id, code, description, onset_date, status)
VALUES
  ('e1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'E11.9', 'Type 2 diabetes mellitus without complications', '2020-02-01', 'active'),
  ('e2222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'I10', 'Essential (primary) hypertension', '2018-05-10', 'active'),
  ('e3333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'I25.10', 'Atherosclerotic heart disease', '2021-06-18', 'active'),
  ('e4444444-4444-4444-4444-444444444444', '33333333-3333-3333-3333-333333333333', 'J45.901', 'Asthma with (acute) exacerbation', '2012-08-19', 'active');

INSERT INTO observations (id, patient_id, encounter_id, code, description, value, unit, observed_at)
VALUES
  ('01111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '4548-4', 'Hemoglobin A1c', '7.2', '%', '2023-11-04 08:20:00-05'),
  ('02222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '8480-6', 'Systolic blood pressure', '142', 'mmHg', '2023-11-04 08:10:00-05'),
  ('03333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'cccccccc-cccc-cccc-cccc-cccccccccccc', '8867-4', 'Heart rate', '104', 'bpm', '2021-06-18 14:30:00-05'),
  ('04444444-4444-4444-4444-444444444444', '33333333-3333-3333-3333-333333333333', 'dddddddd-dddd-dddd-dddd-dddddddddddd', '9279-1', 'Respiratory rate', '24', 'breaths/min', '2024-01-09 16:05:00-05');

INSERT INTO medications (id, patient_id, encounter_id, drug_code, drug_name, start_date, end_date, status, route)
VALUES
  ('11111111-aaaa-4aaa-8aaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '860975', 'Metformin 500 MG Oral Tablet', '2020-02-01', NULL, 'active', 'oral'),
  ('22222222-bbbb-4bbb-8bbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '316049', 'Lisinopril 20 MG Oral Tablet', '2018-05-10', NULL, 'active', 'oral'),
  ('33333333-cccc-4ccc-8ccc-cccccccccccc', '22222222-2222-2222-2222-222222222222', 'cccccccc-cccc-cccc-cccc-cccccccccccc', '617314', 'Atorvastatin 40 MG Oral Tablet', '2021-06-19', NULL, 'active', 'oral'),
  ('44444444-dddd-4ddd-8ddd-dddddddddddd', '33333333-3333-3333-3333-333333333333', 'dddddddd-dddd-dddd-dddd-dddddddddddd', '1049502', 'Albuterol 90 MCG/ACT Inhaler', '2012-08-19', NULL, 'active', 'inhalation');

INSERT INTO procedures (id, patient_id, encounter_id, code, description, performed_at)
VALUES
  ('11111111-2222-4333-8444-555555555555', '22222222-2222-2222-2222-222222222222', 'cccccccc-cccc-cccc-cccc-cccccccccccc', '93000', 'Electrocardiogram', '2021-06-18 15:05:00-05'),
  ('66666666-7777-4888-8999-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '36415', 'Routine venipuncture', '2022-03-10 09:45:00-05');

INSERT INTO claims (id, patient_id, encounter_id, payer, claim_number, amount, status, service_date)
VALUES
  ('11111111-2222-3333-4444-555555555556', '22222222-2222-2222-2222-222222222222', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'Medicare', 'CLM-2021-0001', 12850.25, 'paid', '2021-06-18'),
  ('11111111-2222-3333-4444-555555555557', '11111111-1111-1111-1111-111111111111', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Aetna', 'CLM-2023-0142', 245.75, 'paid', '2023-11-04'),
  ('11111111-2222-3333-4444-555555555558', '33333333-3333-3333-3333-333333333333', 'dddddddd-dddd-dddd-dddd-dddddddddddd', 'BCBS', 'CLM-2024-0098', 1325.00, 'pending', '2024-01-09');
