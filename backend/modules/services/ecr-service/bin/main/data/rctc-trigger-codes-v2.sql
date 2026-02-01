-- RCTC (Reportable Condition Trigger Codes) V2 - Additional Lab Result Codes
-- Supplements initial seed data with more laboratory test codes
-- Based on CDC RCTC Value Sets for Electronic Case Reporting

-- =============================================================================
-- COVID-19 Additional Lab Tests
-- =============================================================================
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('94759-8', '2.16.840.1.113883.6.1', 'SARS-CoV-2 (COVID-19) RNA [Presence] in Nasopharynx by NAA with probe detection', 'LAB_RESULT', 'COVID-19', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('94531-1', '2.16.840.1.113883.6.1', 'SARS-CoV-2 (COVID-19) RNA panel - Respiratory specimen by NAA with probe detection', 'LAB_RESULT', 'COVID-19', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('95209-3', '2.16.840.1.113883.6.1', 'SARS-CoV-2 (COVID-19) IgG Ab [Presence] in Serum or Plasma by Immunoassay', 'LAB_RESULT', 'COVID-19', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('95416-4', '2.16.840.1.113883.6.1', 'SARS-CoV-2 (COVID-19) IgM Ab [Presence] in DBS by Immunoassay', 'LAB_RESULT', 'COVID-19', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('96896-6', '2.16.840.1.113883.6.1', 'SARS-CoV-2 (COVID-19) S gene mutation detected [Identifier] in Specimen by Molecular method', 'LAB_RESULT', 'COVID-19', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code, code_system) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- Mpox (Monkeypox) Lab Tests
-- =============================================================================
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('100383-9', '2.16.840.1.113883.6.1', 'Monkeypox virus DNA [Presence] in Specimen by NAA with probe detection', 'LAB_RESULT', 'Mpox', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('100384-7', '2.16.840.1.113883.6.1', 'Non-variola Orthopoxvirus DNA [Presence] in Specimen by NAA with probe detection', 'LAB_RESULT', 'Mpox', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code, code_system) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- Mpox Diagnosis Codes
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('B04', '2.16.840.1.113883.6.90', 'Monkeypox', 'DIAGNOSIS', 'Mpox', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code, code_system) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- Hepatitis Lab Tests
-- =============================================================================
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('13954-3', '2.16.840.1.113883.6.1', 'Hepatitis A virus IgM Ab [Presence] in Serum', 'LAB_RESULT', 'Hepatitis A', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('22314-9', '2.16.840.1.113883.6.1', 'Hepatitis A virus IgM Ab [Presence] in Serum by Immunoassay', 'LAB_RESULT', 'Hepatitis A', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('31204-1', '2.16.840.1.113883.6.1', 'Hepatitis B virus surface Ag [Presence] in Serum', 'LAB_RESULT', 'Hepatitis B (Acute)', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('5196-1', '2.16.840.1.113883.6.1', 'Hepatitis B virus surface Ag [Presence] in Serum by Immunoassay', 'LAB_RESULT', 'Hepatitis B (Acute)', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('13955-0', '2.16.840.1.113883.6.1', 'Hepatitis B virus core IgM Ab [Presence] in Serum', 'LAB_RESULT', 'Hepatitis B (Acute)', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('16128-1', '2.16.840.1.113883.6.1', 'Hepatitis C virus Ab [Presence] in Serum', 'LAB_RESULT', 'Hepatitis C (Acute)', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('11259-9', '2.16.840.1.113883.6.1', 'Hepatitis C virus RNA [Presence] in Serum or Plasma by NAA with probe detection', 'LAB_RESULT', 'Hepatitis C (Acute)', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code, code_system) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- STI Lab Tests
-- =============================================================================
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
-- Chlamydia
('21613-5', '2.16.840.1.113883.6.1', 'Chlamydia trachomatis DNA [Presence] in Specimen by NAA with probe detection', 'LAB_RESULT', 'Chlamydia', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('43304-5', '2.16.840.1.113883.6.1', 'Chlamydia trachomatis rRNA [Presence] in Specimen by NAA with probe detection', 'LAB_RESULT', 'Chlamydia', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Gonorrhea
('21415-5', '2.16.840.1.113883.6.1', 'Neisseria gonorrhoeae DNA [Presence] in Urethra by NAA with probe detection', 'LAB_RESULT', 'Gonorrhea', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('43305-2', '2.16.840.1.113883.6.1', 'Neisseria gonorrhoeae rRNA [Presence] in Specimen by NAA with probe detection', 'LAB_RESULT', 'Gonorrhea', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('688-2', '2.16.840.1.113883.6.1', 'Neisseria gonorrhoeae [Presence] in Cervix by Organism specific culture', 'LAB_RESULT', 'Gonorrhea', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Syphilis
('17723-1', '2.16.840.1.113883.6.1', 'Treponema pallidum Ab [Presence] in Serum by Immunoassay', 'LAB_RESULT', 'Syphilis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('22587-0', '2.16.840.1.113883.6.1', 'Treponema pallidum Ab [Presence] in Serum', 'LAB_RESULT', 'Syphilis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('31147-2', '2.16.840.1.113883.6.1', 'Reagin Ab [Titer] in Serum by RPR', 'LAB_RESULT', 'Syphilis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('20507-0', '2.16.840.1.113883.6.1', 'Reagin Ab [Presence] in Serum by VDRL', 'LAB_RESULT', 'Syphilis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- HIV
('7918-6', '2.16.840.1.113883.6.1', 'HIV 1 RNA [#/volume] (viral load) in Serum or Plasma by NAA with probe detection', 'LAB_RESULT', 'HIV/AIDS', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('29893-5', '2.16.840.1.113883.6.1', 'HIV 1 Ab [Presence] in Serum or Plasma by Immunoassay', 'LAB_RESULT', 'HIV/AIDS', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('68961-2', '2.16.840.1.113883.6.1', 'HIV 1+2 Ab+HIV1 p24 Ag [Presence] in Serum or Plasma by Immunoassay', 'LAB_RESULT', 'HIV/AIDS', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code, code_system) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- Enteric Diseases Lab Tests
-- =============================================================================
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
-- Salmonella
('625-4', '2.16.840.1.113883.6.1', 'Bacteria identified in Stool by Culture', 'LAB_RESULT', 'Salmonellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('17576-3', '2.16.840.1.113883.6.1', 'Salmonella sp identified in Specimen by Organism specific culture', 'LAB_RESULT', 'Salmonellosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- E. coli O157
('16832-8', '2.16.840.1.113883.6.1', 'Escherichia coli O157 Ag [Presence] in Stool', 'LAB_RESULT', 'E. coli O157', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('44087-5', '2.16.840.1.113883.6.1', 'Escherichia coli O157:H7 DNA [Presence] in Stool by NAA with probe detection', 'LAB_RESULT', 'E. coli O157', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Campylobacter
('17579-7', '2.16.840.1.113883.6.1', 'Campylobacter sp identified in Specimen by Organism specific culture', 'LAB_RESULT', 'Campylobacteriosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Cryptosporidium
('6375-4', '2.16.840.1.113883.6.1', 'Cryptosporidium sp [Presence] in Stool by Organism specific culture', 'LAB_RESULT', 'Cryptosporidiosis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Giardia
('17581-3', '2.16.840.1.113883.6.1', 'Giardia lamblia [Presence] in Stool by Organism specific culture', 'LAB_RESULT', 'Giardiasis', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code, code_system) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- E. coli O157 Diagnosis Codes
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A04.3', '2.16.840.1.113883.6.90', 'Enterohemorrhagic Escherichia coli infection', 'DIAGNOSIS', 'E. coli O157', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code, code_system) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- Arboviral Diseases Lab Tests (West Nile, Zika, Dengue)
-- =============================================================================
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
-- West Nile Virus
('29534-5', '2.16.840.1.113883.6.1', 'West Nile virus IgM Ab [Presence] in Serum by Immunoassay', 'LAB_RESULT', 'West Nile Virus', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('31705-7', '2.16.840.1.113883.6.1', 'West Nile virus IgM Ab [Presence] in Cerebral spinal fluid', 'LAB_RESULT', 'West Nile Virus', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Zika Virus
('80618-2', '2.16.840.1.113883.6.1', 'Zika virus IgM Ab [Presence] in Serum by Immunoassay', 'LAB_RESULT', 'Zika Virus', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('80823-8', '2.16.840.1.113883.6.1', 'Zika virus RNA [Presence] in Serum by NAA with probe detection', 'LAB_RESULT', 'Zika Virus', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Dengue
('6384-6', '2.16.840.1.113883.6.1', 'Dengue virus IgM Ab [Presence] in Serum by Immunoassay', 'LAB_RESULT', 'Dengue', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('60262-3', '2.16.840.1.113883.6.1', 'Dengue virus 1+2+3+4 RNA [Presence] in Serum by NAA with probe detection', 'LAB_RESULT', 'Dengue', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code, code_system) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- Arboviral Diagnosis Codes
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A92.31', '2.16.840.1.113883.6.90', 'West Nile virus infection with encephalitis', 'DIAGNOSIS', 'West Nile Virus', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A92.32', '2.16.840.1.113883.6.90', 'West Nile virus infection with other neurologic manifestation', 'DIAGNOSIS', 'West Nile Virus', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A92.39', '2.16.840.1.113883.6.90', 'West Nile virus infection with other complications', 'DIAGNOSIS', 'West Nile Virus', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A92.5', '2.16.840.1.113883.6.90', 'Zika virus disease', 'DIAGNOSIS', 'Zika Virus', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A90', '2.16.840.1.113883.6.90', 'Dengue fever [classical dengue]', 'DIAGNOSIS', 'Dengue', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A91', '2.16.840.1.113883.6.90', 'Dengue hemorrhagic fever', 'DIAGNOSIS', 'Dengue', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code, code_system) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- Carbapenem-Resistant Organisms (CRO/CRE)
-- =============================================================================
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('85770-7', '2.16.840.1.113883.6.1', 'Carbapenemase genes [Presence] in Isolate by NAA with probe detection', 'LAB_RESULT', 'Carbapenem-resistant Enterobacteriaceae', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('88462-7', '2.16.840.1.113883.6.1', 'Carbapenem-Resistant Enterobacteriaceae [Presence] by Organism specific culture', 'LAB_RESULT', 'Carbapenem-resistant Enterobacteriaceae', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code, code_system) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- Vancomycin-Resistant Enterococcus (VRE)
-- =============================================================================
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('20954-4', '2.16.840.1.113883.6.1', 'Vancomycin resistant Enterococcus [Presence] in Specimen by Organism specific culture', 'LAB_RESULT', 'Vancomycin-Resistant Enterococcus', 'WITHIN_72_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code, code_system) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- Candida auris
-- =============================================================================
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('90030-4', '2.16.840.1.113883.6.1', 'Candida auris [Presence] in Specimen by Organism specific culture', 'LAB_RESULT', 'Candida auris', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B37.7', '2.16.840.1.113883.6.90', 'Candidal sepsis', 'DIAGNOSIS', 'Candida auris', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code, code_system) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- Rabies
-- =============================================================================
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A82.0', '2.16.840.1.113883.6.90', 'Sylvatic rabies', 'DIAGNOSIS', 'Rabies', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A82.1', '2.16.840.1.113883.6.90', 'Urban rabies', 'DIAGNOSIS', 'Rabies', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A82.9', '2.16.840.1.113883.6.90', 'Rabies, unspecified', 'DIAGNOSIS', 'Rabies', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('6525-3', '2.16.840.1.113883.6.1', 'Rabies virus Ab [Presence] in Serum by Immunofluorescence', 'LAB_RESULT', 'Rabies', 'IMMEDIATE', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code, code_system) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- Tetanus
-- =============================================================================
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A34', '2.16.840.1.113883.6.90', 'Obstetrical tetanus', 'DIAGNOSIS', 'Tetanus', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A35', '2.16.840.1.113883.6.90', 'Other tetanus', 'DIAGNOSIS', 'Tetanus', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code, code_system) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- Diphtheria
-- =============================================================================
INSERT INTO ecr.rctc_trigger_codes (code, code_system, display, trigger_type, condition_name, urgency, is_active, rctc_version, created_at, updated_at) VALUES
('A36.0', '2.16.840.1.113883.6.90', 'Pharyngeal diphtheria', 'DIAGNOSIS', 'Diphtheria', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A36.1', '2.16.840.1.113883.6.90', 'Nasopharyngeal diphtheria', 'DIAGNOSIS', 'Diphtheria', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A36.2', '2.16.840.1.113883.6.90', 'Laryngeal diphtheria', 'DIAGNOSIS', 'Diphtheria', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A36.3', '2.16.840.1.113883.6.90', 'Cutaneous diphtheria', 'DIAGNOSIS', 'Diphtheria', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A36.9', '2.16.840.1.113883.6.90', 'Diphtheria, unspecified', 'DIAGNOSIS', 'Diphtheria', 'WITHIN_24_HOURS', true, '2024.1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code, code_system) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;
