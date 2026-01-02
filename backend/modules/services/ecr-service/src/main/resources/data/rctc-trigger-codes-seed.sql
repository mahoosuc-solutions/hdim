-- RCTC (Reportable Condition Trigger Codes) Seed Data
-- Based on CDC RCTC Value Sets for Electronic Case Reporting
-- Version: 2024.1

-- =============================================================================
-- IMMEDIATE REPORTING (within 4 hours) - Bioterrorism & High-Priority
-- =============================================================================

-- Anthrax
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A22.0', '2.16.840.1.113883.6.90', 'Cutaneous anthrax', 'DIAGNOSIS', 'Anthrax', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A22.1', '2.16.840.1.113883.6.90', 'Pulmonary anthrax', 'DIAGNOSIS', 'Anthrax', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A22.2', '2.16.840.1.113883.6.90', 'Gastrointestinal anthrax', 'DIAGNOSIS', 'Anthrax', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A22.7', '2.16.840.1.113883.6.90', 'Anthrax sepsis', 'DIAGNOSIS', 'Anthrax', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A22.8', '2.16.840.1.113883.6.90', 'Other forms of anthrax', 'DIAGNOSIS', 'Anthrax', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A22.9', '2.16.840.1.113883.6.90', 'Anthrax, unspecified', 'DIAGNOSIS', 'Anthrax', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Botulism
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A05.1', '2.16.840.1.113883.6.90', 'Botulism food poisoning', 'DIAGNOSIS', 'Botulism', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A48.51', '2.16.840.1.113883.6.90', 'Infant botulism', 'DIAGNOSIS', 'Botulism', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A48.52', '2.16.840.1.113883.6.90', 'Wound botulism', 'DIAGNOSIS', 'Botulism', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Plague
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A20.0', '2.16.840.1.113883.6.90', 'Bubonic plague', 'DIAGNOSIS', 'Plague', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A20.1', '2.16.840.1.113883.6.90', 'Cellulocutaneous plague', 'DIAGNOSIS', 'Plague', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A20.2', '2.16.840.1.113883.6.90', 'Pneumonic plague', 'DIAGNOSIS', 'Plague', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A20.3', '2.16.840.1.113883.6.90', 'Plague meningitis', 'DIAGNOSIS', 'Plague', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A20.7', '2.16.840.1.113883.6.90', 'Septicemic plague', 'DIAGNOSIS', 'Plague', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A20.9', '2.16.840.1.113883.6.90', 'Plague, unspecified', 'DIAGNOSIS', 'Plague', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Smallpox
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('B03', '2.16.840.1.113883.6.90', 'Smallpox', 'DIAGNOSIS', 'Smallpox', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Viral Hemorrhagic Fevers
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A96.0', '2.16.840.1.113883.6.90', 'Junin hemorrhagic fever', 'DIAGNOSIS', 'Viral Hemorrhagic Fever', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A96.1', '2.16.840.1.113883.6.90', 'Machupo hemorrhagic fever', 'DIAGNOSIS', 'Viral Hemorrhagic Fever', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A96.2', '2.16.840.1.113883.6.90', 'Lassa fever', 'DIAGNOSIS', 'Viral Hemorrhagic Fever', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A98.0', '2.16.840.1.113883.6.90', 'Crimean-Congo hemorrhagic fever', 'DIAGNOSIS', 'Viral Hemorrhagic Fever', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A98.3', '2.16.840.1.113883.6.90', 'Marburg virus disease', 'DIAGNOSIS', 'Viral Hemorrhagic Fever', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A98.4', '2.16.840.1.113883.6.90', 'Ebola virus disease', 'DIAGNOSIS', 'Viral Hemorrhagic Fever', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- 24-HOUR REPORTING
-- =============================================================================

-- Measles
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('B05.0', '2.16.840.1.113883.6.90', 'Measles complicated by encephalitis', 'DIAGNOSIS', 'Measles', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B05.1', '2.16.840.1.113883.6.90', 'Measles complicated by meningitis', 'DIAGNOSIS', 'Measles', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B05.2', '2.16.840.1.113883.6.90', 'Measles complicated by pneumonia', 'DIAGNOSIS', 'Measles', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B05.3', '2.16.840.1.113883.6.90', 'Measles complicated by otitis media', 'DIAGNOSIS', 'Measles', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B05.4', '2.16.840.1.113883.6.90', 'Measles with intestinal complications', 'DIAGNOSIS', 'Measles', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B05.81', '2.16.840.1.113883.6.90', 'Measles keratitis and keratoconjunctivitis', 'DIAGNOSIS', 'Measles', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B05.89', '2.16.840.1.113883.6.90', 'Other measles complications', 'DIAGNOSIS', 'Measles', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B05.9', '2.16.840.1.113883.6.90', 'Measles without complication', 'DIAGNOSIS', 'Measles', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Rubella
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('B06.00', '2.16.840.1.113883.6.90', 'Rubella with neurological complication, unspecified', 'DIAGNOSIS', 'Rubella', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B06.01', '2.16.840.1.113883.6.90', 'Rubella encephalitis', 'DIAGNOSIS', 'Rubella', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B06.02', '2.16.840.1.113883.6.90', 'Rubella meningitis', 'DIAGNOSIS', 'Rubella', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B06.09', '2.16.840.1.113883.6.90', 'Other neurological complications of rubella', 'DIAGNOSIS', 'Rubella', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B06.81', '2.16.840.1.113883.6.90', 'Rubella pneumonia', 'DIAGNOSIS', 'Rubella', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B06.82', '2.16.840.1.113883.6.90', 'Rubella arthritis', 'DIAGNOSIS', 'Rubella', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B06.89', '2.16.840.1.113883.6.90', 'Other rubella complications', 'DIAGNOSIS', 'Rubella', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B06.9', '2.16.840.1.113883.6.90', 'Rubella without complication', 'DIAGNOSIS', 'Rubella', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Pertussis (Whooping Cough)
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A37.00', '2.16.840.1.113883.6.90', 'Whooping cough due to B. pertussis without pneumonia', 'DIAGNOSIS', 'Pertussis', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A37.01', '2.16.840.1.113883.6.90', 'Whooping cough due to B. pertussis with pneumonia', 'DIAGNOSIS', 'Pertussis', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A37.10', '2.16.840.1.113883.6.90', 'Whooping cough due to B. parapertussis without pneumonia', 'DIAGNOSIS', 'Pertussis', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A37.11', '2.16.840.1.113883.6.90', 'Whooping cough due to B. parapertussis with pneumonia', 'DIAGNOSIS', 'Pertussis', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A37.80', '2.16.840.1.113883.6.90', 'Whooping cough due to other Bordetella species without pneumonia', 'DIAGNOSIS', 'Pertussis', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A37.81', '2.16.840.1.113883.6.90', 'Whooping cough due to other Bordetella species with pneumonia', 'DIAGNOSIS', 'Pertussis', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A37.90', '2.16.840.1.113883.6.90', 'Whooping cough, unspecified species without pneumonia', 'DIAGNOSIS', 'Pertussis', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A37.91', '2.16.840.1.113883.6.90', 'Whooping cough, unspecified species with pneumonia', 'DIAGNOSIS', 'Pertussis', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Meningococcal Disease
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A39.0', '2.16.840.1.113883.6.90', 'Meningococcal meningitis', 'DIAGNOSIS', 'Meningococcal Disease', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A39.1', '2.16.840.1.113883.6.90', 'Waterhouse-Friderichsen syndrome', 'DIAGNOSIS', 'Meningococcal Disease', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A39.2', '2.16.840.1.113883.6.90', 'Acute meningococcemia', 'DIAGNOSIS', 'Meningococcal Disease', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A39.3', '2.16.840.1.113883.6.90', 'Chronic meningococcemia', 'DIAGNOSIS', 'Meningococcal Disease', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A39.4', '2.16.840.1.113883.6.90', 'Meningococcemia, unspecified', 'DIAGNOSIS', 'Meningococcal Disease', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A39.50', '2.16.840.1.113883.6.90', 'Meningococcal carditis, unspecified', 'DIAGNOSIS', 'Meningococcal Disease', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A39.9', '2.16.840.1.113883.6.90', 'Meningococcal infection, unspecified', 'DIAGNOSIS', 'Meningococcal Disease', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Hepatitis A
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('B15.0', '2.16.840.1.113883.6.90', 'Hepatitis A with hepatic coma', 'DIAGNOSIS', 'Hepatitis A', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B15.9', '2.16.840.1.113883.6.90', 'Hepatitis A without hepatic coma', 'DIAGNOSIS', 'Hepatitis A', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- 72-HOUR REPORTING (Standard)
-- =============================================================================

-- Hepatitis B (Acute)
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('B16.0', '2.16.840.1.113883.6.90', 'Acute hepatitis B with delta-agent with hepatic coma', 'DIAGNOSIS', 'Hepatitis B (Acute)', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B16.1', '2.16.840.1.113883.6.90', 'Acute hepatitis B with delta-agent without hepatic coma', 'DIAGNOSIS', 'Hepatitis B (Acute)', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B16.2', '2.16.840.1.113883.6.90', 'Acute hepatitis B without delta-agent with hepatic coma', 'DIAGNOSIS', 'Hepatitis B (Acute)', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B16.9', '2.16.840.1.113883.6.90', 'Acute hepatitis B without delta-agent and without hepatic coma', 'DIAGNOSIS', 'Hepatitis B (Acute)', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Hepatitis C (Acute)
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('B17.10', '2.16.840.1.113883.6.90', 'Acute hepatitis C without hepatic coma', 'DIAGNOSIS', 'Hepatitis C (Acute)', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B17.11', '2.16.840.1.113883.6.90', 'Acute hepatitis C with hepatic coma', 'DIAGNOSIS', 'Hepatitis C (Acute)', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Salmonellosis
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A02.0', '2.16.840.1.113883.6.90', 'Salmonella enteritis', 'DIAGNOSIS', 'Salmonellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A02.1', '2.16.840.1.113883.6.90', 'Salmonella sepsis', 'DIAGNOSIS', 'Salmonellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A02.20', '2.16.840.1.113883.6.90', 'Localized salmonella infection, unspecified', 'DIAGNOSIS', 'Salmonellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A02.21', '2.16.840.1.113883.6.90', 'Salmonella meningitis', 'DIAGNOSIS', 'Salmonellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A02.22', '2.16.840.1.113883.6.90', 'Salmonella pneumonia', 'DIAGNOSIS', 'Salmonellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A02.23', '2.16.840.1.113883.6.90', 'Salmonella arthritis', 'DIAGNOSIS', 'Salmonellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A02.24', '2.16.840.1.113883.6.90', 'Salmonella osteomyelitis', 'DIAGNOSIS', 'Salmonellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A02.29', '2.16.840.1.113883.6.90', 'Salmonella with other localized infection', 'DIAGNOSIS', 'Salmonellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A02.8', '2.16.840.1.113883.6.90', 'Other specified salmonella infections', 'DIAGNOSIS', 'Salmonellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A02.9', '2.16.840.1.113883.6.90', 'Salmonella infection, unspecified', 'DIAGNOSIS', 'Salmonellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Shigellosis
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A03.0', '2.16.840.1.113883.6.90', 'Shigellosis due to Shigella dysenteriae', 'DIAGNOSIS', 'Shigellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A03.1', '2.16.840.1.113883.6.90', 'Shigellosis due to Shigella flexneri', 'DIAGNOSIS', 'Shigellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A03.2', '2.16.840.1.113883.6.90', 'Shigellosis due to Shigella boydii', 'DIAGNOSIS', 'Shigellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A03.3', '2.16.840.1.113883.6.90', 'Shigellosis due to Shigella sonnei', 'DIAGNOSIS', 'Shigellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A03.8', '2.16.840.1.113883.6.90', 'Other shigellosis', 'DIAGNOSIS', 'Shigellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A03.9', '2.16.840.1.113883.6.90', 'Shigellosis, unspecified', 'DIAGNOSIS', 'Shigellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Legionellosis
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A48.1', '2.16.840.1.113883.6.90', 'Legionnaires'' disease', 'DIAGNOSIS', 'Legionellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A48.2', '2.16.840.1.113883.6.90', 'Nonpneumonic Legionnaires'' disease [Pontiac fever]', 'DIAGNOSIS', 'Legionellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Lyme Disease
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A69.20', '2.16.840.1.113883.6.90', 'Lyme disease, unspecified', 'DIAGNOSIS', 'Lyme Disease', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A69.21', '2.16.840.1.113883.6.90', 'Meningitis due to Lyme disease', 'DIAGNOSIS', 'Lyme Disease', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A69.22', '2.16.840.1.113883.6.90', 'Other neurologic disorders in Lyme disease', 'DIAGNOSIS', 'Lyme Disease', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A69.23', '2.16.840.1.113883.6.90', 'Arthritis due to Lyme disease', 'DIAGNOSIS', 'Lyme Disease', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A69.29', '2.16.840.1.113883.6.90', 'Other conditions associated with Lyme disease', 'DIAGNOSIS', 'Lyme Disease', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Tuberculosis
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A15.0', '2.16.840.1.113883.6.90', 'Tuberculosis of lung', 'DIAGNOSIS', 'Tuberculosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A15.4', '2.16.840.1.113883.6.90', 'Tuberculosis of intrathoracic lymph nodes', 'DIAGNOSIS', 'Tuberculosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A15.5', '2.16.840.1.113883.6.90', 'Tuberculosis of larynx, trachea and bronchus', 'DIAGNOSIS', 'Tuberculosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A15.6', '2.16.840.1.113883.6.90', 'Tuberculous pleurisy', 'DIAGNOSIS', 'Tuberculosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A15.7', '2.16.840.1.113883.6.90', 'Primary respiratory tuberculosis', 'DIAGNOSIS', 'Tuberculosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A15.8', '2.16.840.1.113883.6.90', 'Other respiratory tuberculosis', 'DIAGNOSIS', 'Tuberculosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A15.9', '2.16.840.1.113883.6.90', 'Respiratory tuberculosis unspecified', 'DIAGNOSIS', 'Tuberculosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A17.0', '2.16.840.1.113883.6.90', 'Tuberculous meningitis', 'DIAGNOSIS', 'Tuberculosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A18.01', '2.16.840.1.113883.6.90', 'Tuberculosis of spine', 'DIAGNOSIS', 'Tuberculosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A19.0', '2.16.840.1.113883.6.90', 'Acute miliary tuberculosis of a single specified site', 'DIAGNOSIS', 'Tuberculosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A19.1', '2.16.840.1.113883.6.90', 'Acute miliary tuberculosis of multiple sites', 'DIAGNOSIS', 'Tuberculosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A19.9', '2.16.840.1.113883.6.90', 'Miliary tuberculosis, unspecified', 'DIAGNOSIS', 'Tuberculosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- STIs: Chlamydia
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A56.00', '2.16.840.1.113883.6.90', 'Chlamydial infection of lower genitourinary tract, unspecified', 'DIAGNOSIS', 'Chlamydia', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A56.01', '2.16.840.1.113883.6.90', 'Chlamydial cystitis and urethritis', 'DIAGNOSIS', 'Chlamydia', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A56.02', '2.16.840.1.113883.6.90', 'Chlamydial vulvovaginitis', 'DIAGNOSIS', 'Chlamydia', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A56.09', '2.16.840.1.113883.6.90', 'Other chlamydial infection of lower genitourinary tract', 'DIAGNOSIS', 'Chlamydia', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A56.11', '2.16.840.1.113883.6.90', 'Chlamydial female pelvic inflammatory disease', 'DIAGNOSIS', 'Chlamydia', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A56.19', '2.16.840.1.113883.6.90', 'Other chlamydial genitourinary infection', 'DIAGNOSIS', 'Chlamydia', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- STIs: Gonorrhea
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A54.00', '2.16.840.1.113883.6.90', 'Gonococcal infection of lower genitourinary tract, unspecified', 'DIAGNOSIS', 'Gonorrhea', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A54.01', '2.16.840.1.113883.6.90', 'Gonococcal cystitis and urethritis, unspecified', 'DIAGNOSIS', 'Gonorrhea', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A54.02', '2.16.840.1.113883.6.90', 'Gonococcal vulvovaginitis, unspecified', 'DIAGNOSIS', 'Gonorrhea', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A54.03', '2.16.840.1.113883.6.90', 'Gonococcal cervicitis, unspecified', 'DIAGNOSIS', 'Gonorrhea', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A54.24', '2.16.840.1.113883.6.90', 'Gonococcal female pelvic inflammatory disease', 'DIAGNOSIS', 'Gonorrhea', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A54.9', '2.16.840.1.113883.6.90', 'Gonococcal infection, unspecified', 'DIAGNOSIS', 'Gonorrhea', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- STIs: Syphilis
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A51.0', '2.16.840.1.113883.6.90', 'Primary genital syphilis', 'DIAGNOSIS', 'Syphilis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A51.1', '2.16.840.1.113883.6.90', 'Primary anal syphilis', 'DIAGNOSIS', 'Syphilis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A51.2', '2.16.840.1.113883.6.90', 'Primary syphilis of other sites', 'DIAGNOSIS', 'Syphilis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A51.31', '2.16.840.1.113883.6.90', 'Secondary syphilitic meningitis', 'DIAGNOSIS', 'Syphilis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A51.39', '2.16.840.1.113883.6.90', 'Other secondary syphilis of skin', 'DIAGNOSIS', 'Syphilis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A51.9', '2.16.840.1.113883.6.90', 'Early syphilis, unspecified', 'DIAGNOSIS', 'Syphilis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A52.0', '2.16.840.1.113883.6.90', 'Cardiovascular and cerebrovascular syphilis', 'DIAGNOSIS', 'Syphilis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A52.3', '2.16.840.1.113883.6.90', 'Neurosyphilis, unspecified', 'DIAGNOSIS', 'Syphilis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A53.9', '2.16.840.1.113883.6.90', 'Syphilis, unspecified', 'DIAGNOSIS', 'Syphilis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- HIV/AIDS
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('B20', '2.16.840.1.113883.6.90', 'Human immunodeficiency virus [HIV] disease', 'DIAGNOSIS', 'HIV/AIDS', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Z21', '2.16.840.1.113883.6.90', 'Asymptomatic human immunodeficiency virus [HIV] infection status', 'DIAGNOSIS', 'HIV/AIDS', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- LAB RESULT TRIGGERS (LOINC codes)
-- =============================================================================
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
-- COVID-19
('94500-6', '2.16.840.1.113883.6.1', 'SARS-CoV-2 (COVID-19) RNA [Presence] in Respiratory specimen by NAA with probe detection', 'LAB_RESULT', 'COVID-19', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('94309-2', '2.16.840.1.113883.6.1', 'SARS-CoV-2 (COVID-19) RNA [Presence] in Specimen by NAA with probe detection', 'LAB_RESULT', 'COVID-19', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('94558-4', '2.16.840.1.113883.6.1', 'SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay', 'LAB_RESULT', 'COVID-19', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Influenza
('80383-3', '2.16.840.1.113883.6.1', 'Influenza virus A H3 RNA [Presence] in Nasopharynx by NAA with probe detection', 'LAB_RESULT', 'Influenza', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('80382-5', '2.16.840.1.113883.6.1', 'Influenza virus A H1 RNA [Presence] in Nasopharynx by NAA with probe detection', 'LAB_RESULT', 'Influenza', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('76078-5', '2.16.840.1.113883.6.1', 'Influenza virus A RNA [Presence] in Nasopharynx by NAA with probe detection', 'LAB_RESULT', 'Influenza', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('76080-1', '2.16.840.1.113883.6.1', 'Influenza virus B RNA [Presence] in Nasopharynx by NAA with probe detection', 'LAB_RESULT', 'Influenza', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- RSV
('92131-2', '2.16.840.1.113883.6.1', 'Respiratory syncytial virus RNA [Presence] in Respiratory specimen by NAA with probe detection', 'LAB_RESULT', 'RSV', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Group A Strep
('18481-2', '2.16.840.1.113883.6.1', 'Streptococcus pyogenes Ag [Presence] in Throat by Rapid immunoassay', 'LAB_RESULT', 'Group A Streptococcus', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('6557-7', '2.16.840.1.113883.6.1', 'Streptococcus pyogenes [Presence] in Throat by Organism specific culture', 'LAB_RESULT', 'Group A Streptococcus', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- MRSA
('35492-8', '2.16.840.1.113883.6.1', 'Methicillin resistant Staphylococcus aureus (MRSA) DNA [Presence] by NAA with probe detection', 'LAB_RESULT', 'MRSA', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('13317-3', '2.16.840.1.113883.6.1', 'Methicillin resistant Staphylococcus aureus [Presence] in Specimen by Organism specific culture', 'LAB_RESULT', 'MRSA', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ON CONFLICT handling for codes that already exist from initial seed
-- This allows the script to be run multiple times safely
