-- HCC V24/V28 Crosswalk Additional High-Impact Codes
-- Supplements V1 seed data with additional common diagnostic codes
-- Focused on high-RAF-impact conditions frequently seen in Medicare populations

-- =============================================================================
-- ATRIAL FIBRILLATION (HCC 96 V24 -> HCC 223 V28)
-- =============================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('I4891', 'Unspecified atrial fibrillation', '96', 'Specified Heart Arrhythmias', '223', 'Atrial Fibrillation and Other Arrhythmias', 0.259, 0.271, true, true, 'Document type (persistent, paroxysmal, permanent)'),
('I48.0', 'Paroxysmal atrial fibrillation', '96', 'Specified Heart Arrhythmias', '223', 'Atrial Fibrillation and Other Arrhythmias', 0.259, 0.271, false, true, 'New V28 category'),
('I48.1', 'Persistent atrial fibrillation', '96', 'Specified Heart Arrhythmias', '223', 'Atrial Fibrillation and Other Arrhythmias', 0.259, 0.271, false, true, 'New V28 category'),
('I48.2', 'Chronic atrial fibrillation', '96', 'Specified Heart Arrhythmias', '223', 'Atrial Fibrillation and Other Arrhythmias', 0.259, 0.271, false, true, 'New V28 category'),
('I4820', 'Chronic atrial fibrillation, unspecified', '96', 'Specified Heart Arrhythmias', '223', 'Atrial Fibrillation and Other Arrhythmias', 0.259, 0.271, true, true, 'Document permanent vs long-standing persistent'),
('I4821', 'Permanent atrial fibrillation', '96', 'Specified Heart Arrhythmias', '223', 'Atrial Fibrillation and Other Arrhythmias', 0.259, 0.271, false, true, 'New V28 category'),
('I48.92', 'Unspecified atrial flutter', '96', 'Specified Heart Arrhythmias', '223', 'Atrial Fibrillation and Other Arrhythmias', 0.259, 0.271, true, true, 'Document type if known')
ON CONFLICT (icd10_code) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- DEMENTIA (HCC 51-52 V24 -> HCC 149-150 V28)
-- =============================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('F0150', 'Vascular dementia without behavioral disturbance', '52', 'Dementia without Complication', '150', 'Dementia without Behavioral Disturbance', 0.235, 0.248, false, true, 'New V28 category'),
('F0151', 'Vascular dementia with behavioral disturbance', '51', 'Dementia with Complication', '149', 'Dementia with Behavioral Disturbance', 0.435, 0.469, false, true, 'New V28 category'),
('F0280', 'Dementia in other diseases classified elsewhere without behavioral disturbance', '52', 'Dementia without Complication', '150', 'Dementia without Behavioral Disturbance', 0.235, 0.248, false, true, 'New V28 category'),
('F0281', 'Dementia in other diseases classified elsewhere with behavioral disturbance', '51', 'Dementia with Complication', '149', 'Dementia with Behavioral Disturbance', 0.435, 0.469, false, true, 'New V28 category'),
('F03.90', 'Unspecified dementia without behavioral disturbance', '52', 'Dementia without Complication', '150', 'Dementia without Behavioral Disturbance', 0.235, 0.248, true, true, 'Document etiology if known'),
('F03.91', 'Unspecified dementia with behavioral disturbance', '51', 'Dementia with Complication', '149', 'Dementia with Behavioral Disturbance', 0.435, 0.469, false, true, 'New V28 category'),
('G3084', 'Mild cognitive impairment, so stated', NULL, NULL, NULL, NULL, NULL, NULL, true, false, 'MCI does not map to HCC; document progression to dementia'),
('G309', 'Alzheimer disease, unspecified', '52', 'Dementia without Complication', '150', 'Dementia without Behavioral Disturbance', 0.235, 0.248, true, true, 'Document stage and behavioral symptoms'),
('G300', 'Alzheimer disease with early onset', '52', 'Dementia without Complication', '150', 'Dementia without Behavioral Disturbance', 0.235, 0.248, false, true, 'New V28 category'),
('G301', 'Alzheimer disease with late onset', '52', 'Dementia without Complication', '150', 'Dementia without Behavioral Disturbance', 0.235, 0.248, false, true, 'New V28 category')
ON CONFLICT (icd10_code) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- OBESITY (HCC 22 V24 -> HCC 48 V28)
-- =============================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('E66.01', 'Morbid (severe) obesity due to excess calories', '22', 'Morbid Obesity', '48', 'Morbid Obesity', 0.250, 0.265, false, true, 'Coefficient increased in V28'),
('E66.2', 'Morbid (severe) obesity with alveolar hypoventilation', '22', 'Morbid Obesity', '48', 'Morbid Obesity', 0.250, 0.265, false, true, 'Higher severity with pulmonary involvement'),
('E66.09', 'Other obesity due to excess calories', NULL, NULL, NULL, NULL, NULL, NULL, true, false, 'Only morbid obesity maps to HCC'),
('E66.9', 'Obesity, unspecified', NULL, NULL, NULL, NULL, NULL, NULL, true, false, 'Document BMI >= 40 for morbid obesity'),
('Z68.41', 'Body mass index (BMI) 40.0-44.9, adult', NULL, NULL, NULL, NULL, NULL, NULL, true, false, 'Use with E66 codes; supports morbid obesity'),
('Z68.42', 'Body mass index (BMI) 45.0-49.9, adult', NULL, NULL, NULL, NULL, NULL, NULL, true, false, 'Use with E66 codes; supports morbid obesity'),
('Z68.43', 'Body mass index (BMI) 50.0-59.9, adult', NULL, NULL, NULL, NULL, NULL, NULL, true, false, 'Use with E66 codes; supports morbid obesity'),
('Z68.44', 'Body mass index (BMI) 60.0-69.9, adult', NULL, NULL, NULL, NULL, NULL, NULL, true, false, 'Use with E66 codes; supports morbid obesity'),
('Z68.45', 'Body mass index (BMI) 70 or greater, adult', NULL, NULL, NULL, NULL, NULL, NULL, true, false, 'Use with E66 codes; supports morbid obesity')
ON CONFLICT (icd10_code) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- DRUG/ALCOHOL USE DISORDERS (HCC 54-56 V24 -> HCC 135-137 V28)
-- =============================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('F10.20', 'Alcohol dependence, uncomplicated', '55', 'Drug/Alcohol Dependence', '136', 'Alcohol Dependence', 0.329, 0.341, false, true, 'Separated from drug dependence in V28'),
('F10.21', 'Alcohol dependence, in remission', '55', 'Drug/Alcohol Dependence', '136', 'Alcohol Dependence', 0.329, 0.341, false, true, 'Separated from drug dependence in V28'),
('F10.239', 'Alcohol dependence with withdrawal, unspecified', '55', 'Drug/Alcohol Dependence', '136', 'Alcohol Dependence', 0.329, 0.341, false, true, 'New V28 category'),
('F10.29', 'Alcohol dependence with unspecified alcohol-induced disorder', '55', 'Drug/Alcohol Dependence', '136', 'Alcohol Dependence', 0.329, 0.341, false, true, 'New V28 category'),
('F11.20', 'Opioid dependence, uncomplicated', '55', 'Drug/Alcohol Dependence', '137', 'Opioid Dependence', 0.329, 0.385, false, true, 'Higher coefficient for opioid in V28'),
('F11.21', 'Opioid dependence, in remission', '55', 'Drug/Alcohol Dependence', '137', 'Opioid Dependence', 0.329, 0.385, false, true, 'Higher coefficient for opioid in V28'),
('F11.23', 'Opioid dependence with withdrawal', '55', 'Drug/Alcohol Dependence', '137', 'Opioid Dependence', 0.329, 0.385, false, true, 'Higher coefficient for opioid in V28'),
('F11.29', 'Opioid dependence with unspecified opioid-induced disorder', '55', 'Drug/Alcohol Dependence', '137', 'Opioid Dependence', 0.329, 0.385, false, true, 'Higher coefficient for opioid in V28'),
('F14.20', 'Cocaine dependence, uncomplicated', '55', 'Drug/Alcohol Dependence', '135', 'Other Drug Dependence', 0.329, 0.341, false, true, 'New V28 category'),
('F15.20', 'Other stimulant dependence, uncomplicated', '55', 'Drug/Alcohol Dependence', '135', 'Other Drug Dependence', 0.329, 0.341, false, true, 'New V28 category')
ON CONFLICT (icd10_code) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- SCHIZOPHRENIA & BIPOLAR (HCC 57-58 V24 -> HCC 151-152 V28)
-- =============================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('F20.0', 'Paranoid schizophrenia', '57', 'Schizophrenia', '151', 'Schizophrenia', 0.475, 0.498, false, true, 'Coefficient increased in V28'),
('F20.1', 'Disorganized schizophrenia', '57', 'Schizophrenia', '151', 'Schizophrenia', 0.475, 0.498, false, true, 'Coefficient increased in V28'),
('F20.2', 'Catatonic schizophrenia', '57', 'Schizophrenia', '151', 'Schizophrenia', 0.475, 0.498, false, true, 'Coefficient increased in V28'),
('F20.3', 'Undifferentiated schizophrenia', '57', 'Schizophrenia', '151', 'Schizophrenia', 0.475, 0.498, false, true, 'Coefficient increased in V28'),
('F20.5', 'Residual schizophrenia', '57', 'Schizophrenia', '151', 'Schizophrenia', 0.475, 0.498, false, true, 'Coefficient increased in V28'),
('F20.81', 'Schizophreniform disorder', '57', 'Schizophrenia', '151', 'Schizophrenia', 0.475, 0.498, false, true, 'Coefficient increased in V28'),
('F20.89', 'Other schizophrenia', '57', 'Schizophrenia', '151', 'Schizophrenia', 0.475, 0.498, false, true, 'Coefficient increased in V28'),
('F20.9', 'Schizophrenia, unspecified', '57', 'Schizophrenia', '151', 'Schizophrenia', 0.475, 0.498, true, true, 'Document specific type'),
('F25.0', 'Schizoaffective disorder, bipolar type', '57', 'Schizophrenia', '151', 'Schizophrenia', 0.475, 0.498, false, true, 'Coefficient increased in V28'),
('F25.1', 'Schizoaffective disorder, depressive type', '57', 'Schizophrenia', '151', 'Schizophrenia', 0.475, 0.498, false, true, 'Coefficient increased in V28'),
('F31.0', 'Bipolar disorder, current episode hypomanic', '58', 'Bipolar Disorders', '152', 'Bipolar Disorder', 0.339, 0.355, false, true, 'Coefficient increased in V28'),
('F31.10', 'Bipolar disorder, current episode manic without psychotic features, unspecified', '58', 'Bipolar Disorders', '152', 'Bipolar Disorder', 0.339, 0.355, false, true, 'Coefficient increased in V28'),
('F31.2', 'Bipolar disorder, current episode manic severe with psychotic features', '58', 'Bipolar Disorders', '152', 'Bipolar Disorder', 0.339, 0.355, false, true, 'Coefficient increased in V28'),
('F31.30', 'Bipolar disorder, current episode depressed, mild or moderate severity, unspecified', '58', 'Bipolar Disorders', '152', 'Bipolar Disorder', 0.339, 0.355, false, true, 'Coefficient increased in V28'),
('F31.4', 'Bipolar disorder, current episode depressed, severe, without psychotic features', '58', 'Bipolar Disorders', '152', 'Bipolar Disorder', 0.339, 0.355, false, true, 'Coefficient increased in V28'),
('F31.5', 'Bipolar disorder, current episode depressed, severe, with psychotic features', '58', 'Bipolar Disorders', '152', 'Bipolar Disorder', 0.339, 0.355, false, true, 'Coefficient increased in V28'),
('F31.9', 'Bipolar disorder, unspecified', '58', 'Bipolar Disorders', '152', 'Bipolar Disorder', 0.339, 0.355, true, true, 'Document current episode type')
ON CONFLICT (icd10_code) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- PRESSURE ULCERS (HCC 157-161 V24 -> HCC 379-383 V28)
-- =============================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('L89.003', 'Pressure ulcer of unspecified elbow, stage 3', '158', 'Pressure Ulcer Stage 3', '380', 'Pressure Ulcer Stage 3', 1.037, 1.125, false, true, 'Coefficient increased in V28'),
('L89.004', 'Pressure ulcer of unspecified elbow, stage 4', '157', 'Pressure Ulcer Stage 4', '379', 'Pressure Ulcer Stage 4', 1.352, 1.489, false, true, 'Coefficient increased in V28'),
('L89.013', 'Pressure ulcer of right elbow, stage 3', '158', 'Pressure Ulcer Stage 3', '380', 'Pressure Ulcer Stage 3', 1.037, 1.125, false, true, 'Coefficient increased in V28'),
('L89.014', 'Pressure ulcer of right elbow, stage 4', '157', 'Pressure Ulcer Stage 4', '379', 'Pressure Ulcer Stage 4', 1.352, 1.489, false, true, 'Coefficient increased in V28'),
('L89.153', 'Pressure ulcer of sacral region, stage 3', '158', 'Pressure Ulcer Stage 3', '380', 'Pressure Ulcer Stage 3', 1.037, 1.125, false, true, 'Coefficient increased in V28'),
('L89.154', 'Pressure ulcer of sacral region, stage 4', '157', 'Pressure Ulcer Stage 4', '379', 'Pressure Ulcer Stage 4', 1.352, 1.489, false, true, 'Coefficient increased in V28'),
('L89.213', 'Pressure ulcer of right hip, stage 3', '158', 'Pressure Ulcer Stage 3', '380', 'Pressure Ulcer Stage 3', 1.037, 1.125, false, true, 'Coefficient increased in V28'),
('L89.214', 'Pressure ulcer of right hip, stage 4', '157', 'Pressure Ulcer Stage 4', '379', 'Pressure Ulcer Stage 4', 1.352, 1.489, false, true, 'Coefficient increased in V28'),
('L89.313', 'Pressure ulcer of right buttock, stage 3', '158', 'Pressure Ulcer Stage 3', '380', 'Pressure Ulcer Stage 3', 1.037, 1.125, false, true, 'Coefficient increased in V28'),
('L89.314', 'Pressure ulcer of right buttock, stage 4', '157', 'Pressure Ulcer Stage 4', '379', 'Pressure Ulcer Stage 4', 1.352, 1.489, false, true, 'Coefficient increased in V28'),
('L89.612', 'Pressure ulcer of right heel, stage 2', '161', 'Pressure Ulcer Stage 2', '383', 'Pressure Ulcer Stage 2', 0.347, 0.369, false, true, 'New in V28 categorization'),
('L89.613', 'Pressure ulcer of right heel, stage 3', '158', 'Pressure Ulcer Stage 3', '380', 'Pressure Ulcer Stage 3', 1.037, 1.125, false, true, 'Coefficient increased in V28'),
('L89.614', 'Pressure ulcer of right heel, stage 4', '157', 'Pressure Ulcer Stage 4', '379', 'Pressure Ulcer Stage 4', 1.352, 1.489, false, true, 'Coefficient increased in V28')
ON CONFLICT (icd10_code) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- HEPATITIS (HCC 29-30 V24 -> HCC 56-57 V28)
-- =============================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('B18.0', 'Chronic viral hepatitis B with delta-agent', '29', 'Chronic Hepatitis', '56', 'Chronic Hepatitis', 0.192, 0.198, false, true, 'Coefficient increased in V28'),
('B18.1', 'Chronic viral hepatitis B without delta-agent', '29', 'Chronic Hepatitis', '56', 'Chronic Hepatitis', 0.192, 0.198, false, true, 'Coefficient increased in V28'),
('B18.2', 'Chronic viral hepatitis C', '29', 'Chronic Hepatitis', '56', 'Chronic Hepatitis', 0.192, 0.198, false, true, 'Coefficient increased in V28'),
('B19.20', 'Unspecified viral hepatitis C without hepatic coma', '29', 'Chronic Hepatitis', '56', 'Chronic Hepatitis', 0.192, 0.198, true, true, 'Document acute vs chronic'),
('K70.30', 'Alcoholic cirrhosis of liver without ascites', '28', 'Cirrhosis of Liver', '55', 'Cirrhosis of Liver', 0.363, 0.385, false, true, 'Coefficient increased in V28'),
('K70.31', 'Alcoholic cirrhosis of liver with ascites', '28', 'Cirrhosis of Liver', '55', 'Cirrhosis of Liver', 0.363, 0.385, false, true, 'Higher severity with ascites'),
('K74.60', 'Unspecified cirrhosis of liver', '28', 'Cirrhosis of Liver', '55', 'Cirrhosis of Liver', 0.363, 0.385, true, true, 'Document etiology'),
('K74.69', 'Other cirrhosis of liver', '28', 'Cirrhosis of Liver', '55', 'Cirrhosis of Liver', 0.363, 0.385, false, true, 'Coefficient increased in V28')
ON CONFLICT (icd10_code) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- SEIZURE DISORDERS (HCC 79 V24 -> HCC 212 V28)
-- =============================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('G40.001', 'Localization-related (focal) (partial) idiopathic epilepsy with seizures of localized onset, not intractable, with status epilepticus', '79', 'Seizure Disorders and Convulsions', '212', 'Seizure Disorders', 0.191, 0.205, false, true, 'New V28 category'),
('G40.009', 'Localization-related (focal) (partial) idiopathic epilepsy with seizures of localized onset, not intractable, without status epilepticus', '79', 'Seizure Disorders and Convulsions', '212', 'Seizure Disorders', 0.191, 0.205, false, true, 'New V28 category'),
('G40.011', 'Localization-related (focal) (partial) idiopathic epilepsy with seizures of localized onset, intractable, with status epilepticus', '79', 'Seizure Disorders and Convulsions', '212', 'Seizure Disorders', 0.191, 0.205, false, true, 'New V28 category'),
('G40.019', 'Localization-related (focal) (partial) idiopathic epilepsy with seizures of localized onset, intractable, without status epilepticus', '79', 'Seizure Disorders and Convulsions', '212', 'Seizure Disorders', 0.191, 0.205, false, true, 'New V28 category'),
('G40.309', 'Generalized idiopathic epilepsy, not intractable, without status epilepticus', '79', 'Seizure Disorders and Convulsions', '212', 'Seizure Disorders', 0.191, 0.205, false, true, 'New V28 category'),
('G40.319', 'Generalized idiopathic epilepsy, intractable, without status epilepticus', '79', 'Seizure Disorders and Convulsions', '212', 'Seizure Disorders', 0.191, 0.205, false, true, 'New V28 category'),
('G40.909', 'Epilepsy, unspecified, not intractable, without status epilepticus', '79', 'Seizure Disorders and Convulsions', '212', 'Seizure Disorders', 0.191, 0.205, true, true, 'Document specific type'),
('G40.919', 'Epilepsy, unspecified, intractable, without status epilepticus', '79', 'Seizure Disorders and Convulsions', '212', 'Seizure Disorders', 0.191, 0.205, false, true, 'New V28 category')
ON CONFLICT (icd10_code) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- MULTIPLE SCLEROSIS (HCC 77 V24 -> HCC 208 V28)
-- =============================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('G35', 'Multiple sclerosis', '77', 'Multiple Sclerosis', '208', 'Multiple Sclerosis', 0.422, 0.445, false, true, 'Coefficient increased in V28')
ON CONFLICT (icd10_code) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- PARKINSON'S DISEASE (HCC 78 V24 -> HCC 211 V28)
-- =============================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('G20', 'Parkinson disease', '78', 'Parkinson's and Huntington's Diseases', '211', 'Parkinson''s Disease', 0.606, 0.635, false, true, 'Coefficient increased in V28'),
('G21.0', 'Malignant neuroleptic syndrome', '78', 'Parkinson's and Huntington's Diseases', '211', 'Parkinson''s Disease', 0.606, 0.635, false, true, 'New V28 category'),
('G21.11', 'Neuroleptic induced parkinsonism', '78', 'Parkinson's and Huntington's Diseases', '211', 'Parkinson''s Disease', 0.606, 0.635, false, true, 'New V28 category'),
('G21.19', 'Other drug induced secondary parkinsonism', '78', 'Parkinson's and Huntington's Diseases', '211', 'Parkinson''s Disease', 0.606, 0.635, false, true, 'New V28 category'),
('G21.4', 'Vascular parkinsonism', '78', 'Parkinson's and Huntington's Diseases', '211', 'Parkinson''s Disease', 0.606, 0.635, false, true, 'New V28 category'),
('G21.8', 'Other secondary parkinsonism', '78', 'Parkinson's and Huntington's Diseases', '211', 'Parkinson''s Disease', 0.606, 0.635, false, true, 'New V28 category'),
('G21.9', 'Secondary parkinsonism, unspecified', '78', 'Parkinson's and Huntington's Diseases', '211', 'Parkinson''s Disease', 0.606, 0.635, true, true, 'Document etiology')
ON CONFLICT (icd10_code) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- COMMON HYPERTENSION (NOT HCC - but frequently documented)
-- =============================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('I10', 'Essential (primary) hypertension', NULL, NULL, NULL, NULL, NULL, NULL, true, false, 'HTN alone does not map to HCC; document complications (heart disease, CKD)'),
('I11.9', 'Hypertensive heart disease without heart failure', NULL, NULL, NULL, NULL, NULL, NULL, true, false, 'Document heart failure if present for HCC'),
('I12.9', 'Hypertensive chronic kidney disease with stage 1-4 CKD', NULL, NULL, NULL, NULL, NULL, NULL, true, false, 'CKD stage 3+ maps to HCC; HTN modifier alone does not'),
('I13.10', 'Hypertensive heart and CKD without HF, with stage 1-4 CKD', NULL, NULL, NULL, NULL, NULL, NULL, true, false, 'Document HF or CKD stage 5 for HCC')
ON CONFLICT (icd10_code) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- Add created_at and updated_at columns if not present
-- Note: This assumes the table has these columns. If not, they should be added.
