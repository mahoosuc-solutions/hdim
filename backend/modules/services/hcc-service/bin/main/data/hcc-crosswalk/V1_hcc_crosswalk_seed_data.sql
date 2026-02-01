-- HCC V24/V28 Crosswalk Seed Data
-- Representative sample of high-impact CMS-HCC mappings for 2024
-- Note: This is sample data for development/testing. Production systems should use
-- official CMS data files (https://www.cms.gov/medicare/payment/medicare-advantage-rates-statistics)

-- =========================================================================
-- DIABETES MELLITUS (HCC 17-19 V24 -> HCC 35-37 V28)
-- =========================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('E1010', 'Type 1 diabetes mellitus with ketoacidosis without coma', '17', 'Diabetes with Acute Complications', '35', 'Diabetes with Severe Acute Complications', 0.318, 0.335, false, true, 'Reclassified to severe acute complications'),
('E1011', 'Type 1 diabetes mellitus with ketoacidosis with coma', '17', 'Diabetes with Acute Complications', '35', 'Diabetes with Severe Acute Complications', 0.318, 0.335, false, true, 'Reclassified to severe acute complications'),
('E1021', 'Type 1 diabetes mellitus with diabetic nephropathy', '18', 'Diabetes with Chronic Complications', '36', 'Diabetes with Chronic Complications', 0.302, 0.289, false, false, NULL),
('E1022', 'Type 1 diabetes mellitus with diabetic chronic kidney disease', '18', 'Diabetes with Chronic Complications', '36', 'Diabetes with Chronic Complications', 0.302, 0.289, false, false, NULL),
('E10311', 'Type 1 diabetes mellitus with unspecified diabetic retinopathy with macular edema', '18', 'Diabetes with Chronic Complications', '36', 'Diabetes with Chronic Complications', 0.302, 0.289, false, false, NULL),
('E10319', 'Type 1 diabetes mellitus with unspecified diabetic retinopathy without macular edema', '18', 'Diabetes with Chronic Complications', '36', 'Diabetes with Chronic Complications', 0.302, 0.289, false, false, NULL),
('E10321', 'Type 1 diabetes mellitus with mild nonproliferative diabetic retinopathy with macular edema', '18', 'Diabetes with Chronic Complications', '36', 'Diabetes with Chronic Complications', 0.302, 0.289, false, false, NULL),
('E1065', 'Type 1 diabetes mellitus with hyperglycemia', '19', 'Diabetes without Complication', '37', 'Diabetes without Complication', 0.104, 0.085, true, true, 'Lower coefficient in V28; document complications if present'),
('E109', 'Type 1 diabetes mellitus without complications', '19', 'Diabetes without Complication', '37', 'Diabetes without Complication', 0.104, 0.085, true, true, 'Document complications for higher HCC'),
('E1100', 'Type 2 diabetes mellitus with hyperosmolarity without NKHHC', '17', 'Diabetes with Acute Complications', '35', 'Diabetes with Severe Acute Complications', 0.318, 0.335, false, true, 'Reclassified to severe acute complications'),
('E1101', 'Type 2 diabetes mellitus with hyperosmolarity with coma', '17', 'Diabetes with Acute Complications', '35', 'Diabetes with Severe Acute Complications', 0.318, 0.335, false, true, 'Reclassified to severe acute complications'),
('E1121', 'Type 2 diabetes mellitus with diabetic nephropathy', '18', 'Diabetes with Chronic Complications', '36', 'Diabetes with Chronic Complications', 0.302, 0.289, false, false, NULL),
('E1122', 'Type 2 diabetes mellitus with diabetic chronic kidney disease', '18', 'Diabetes with Chronic Complications', '36', 'Diabetes with Chronic Complications', 0.302, 0.289, false, false, NULL),
('E1140', 'Type 2 diabetes mellitus with diabetic neuropathy, unspecified', '18', 'Diabetes with Chronic Complications', '36', 'Diabetes with Chronic Complications', 0.302, 0.289, false, false, NULL),
('E1141', 'Type 2 diabetes mellitus with diabetic mononeuropathy', '18', 'Diabetes with Chronic Complications', '36', 'Diabetes with Chronic Complications', 0.302, 0.289, false, false, NULL),
('E1142', 'Type 2 diabetes mellitus with diabetic polyneuropathy', '18', 'Diabetes with Chronic Complications', '36', 'Diabetes with Chronic Complications', 0.302, 0.289, false, false, NULL),
('E1143', 'Type 2 diabetes mellitus with diabetic autonomic (poly)neuropathy', '18', 'Diabetes with Chronic Complications', '36', 'Diabetes with Chronic Complications', 0.302, 0.289, false, false, NULL),
('E1151', 'Type 2 diabetes mellitus with diabetic peripheral angiopathy without gangrene', '18', 'Diabetes with Chronic Complications', '36', 'Diabetes with Chronic Complications', 0.302, 0.289, false, false, NULL),
('E1152', 'Type 2 diabetes mellitus with diabetic peripheral angiopathy with gangrene', '106', 'Diabetes with Peripheral Circulatory Complications', '38', 'Diabetes with Severe Complications', 0.462, 0.455, false, true, 'Merged into new severe complications category'),
('E1165', 'Type 2 diabetes mellitus with hyperglycemia', '19', 'Diabetes without Complication', '37', 'Diabetes without Complication', 0.104, 0.085, true, true, 'Lower coefficient; document complications'),
('E119', 'Type 2 diabetes mellitus without complications', '19', 'Diabetes without Complication', '37', 'Diabetes without Complication', 0.104, 0.085, true, true, 'Document complications for higher HCC');

-- =========================================================================
-- CONGESTIVE HEART FAILURE (HCC 85-87 V24 -> HCC 224-226 V28)
-- =========================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('I110', 'Hypertensive heart disease with heart failure', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, true, true, 'Specify ejection fraction for more accurate coding'),
('I130', 'Hypertensive heart and chronic kidney disease with heart failure and stage 1-4 CKD', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, false, true, 'New HCC category'),
('I132', 'Hypertensive heart and chronic kidney disease with heart failure and stage 5 CKD or ESRD', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, false, true, 'New HCC category'),
('I420', 'Dilated cardiomyopathy', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, false, true, 'New HCC category'),
('I5020', 'Unspecified systolic (congestive) heart failure', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, true, true, 'Document acute/chronic and EF'),
('I5021', 'Acute systolic (congestive) heart failure', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, false, true, 'New HCC category'),
('I5022', 'Chronic systolic (congestive) heart failure', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, false, true, 'New HCC category'),
('I5023', 'Acute on chronic systolic (congestive) heart failure', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, false, true, 'New HCC category'),
('I5030', 'Unspecified diastolic (congestive) heart failure', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, true, true, 'Document acute/chronic'),
('I5031', 'Acute diastolic (congestive) heart failure', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, false, true, 'New HCC category'),
('I5032', 'Chronic diastolic (congestive) heart failure', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, false, true, 'New HCC category'),
('I5033', 'Acute on chronic diastolic (congestive) heart failure', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, false, true, 'New HCC category'),
('I5040', 'Unspecified combined systolic and diastolic (congestive) heart failure', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, true, true, 'Document acute/chronic'),
('I5041', 'Acute combined systolic and diastolic (congestive) heart failure', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, false, true, 'New HCC category'),
('I5042', 'Chronic combined systolic and diastolic (congestive) heart failure', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, false, true, 'New HCC category'),
('I5043', 'Acute on chronic combined systolic and diastolic (congestive) heart failure', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, false, true, 'New HCC category'),
('I509', 'Heart failure, unspecified', '85', 'Congestive Heart Failure', '224', 'Heart Failure', 0.331, 0.351, true, true, 'Document type for specific coding');

-- =========================================================================
-- CHRONIC OBSTRUCTIVE PULMONARY DISEASE (HCC 111-112 V24 -> HCC 280 V28)
-- =========================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('J410', 'Simple chronic bronchitis', '111', 'Chronic Obstructive Pulmonary Disease', '280', 'Chronic Obstructive Pulmonary Disease', 0.346, 0.289, true, true, 'Specify with acute exacerbation if applicable'),
('J411', 'Mucopurulent chronic bronchitis', '111', 'Chronic Obstructive Pulmonary Disease', '280', 'Chronic Obstructive Pulmonary Disease', 0.346, 0.289, true, true, 'Specify with acute exacerbation if applicable'),
('J418', 'Mixed simple and mucopurulent chronic bronchitis', '111', 'Chronic Obstructive Pulmonary Disease', '280', 'Chronic Obstructive Pulmonary Disease', 0.346, 0.289, true, true, 'Specify with acute exacerbation if applicable'),
('J42', 'Unspecified chronic bronchitis', '111', 'Chronic Obstructive Pulmonary Disease', '280', 'Chronic Obstructive Pulmonary Disease', 0.346, 0.289, true, true, 'Document specific type'),
('J430', 'Unilateral pulmonary emphysema [MacLeod syndrome]', '111', 'Chronic Obstructive Pulmonary Disease', '280', 'Chronic Obstructive Pulmonary Disease', 0.346, 0.289, false, true, 'New V28 category'),
('J431', 'Panlobular emphysema', '111', 'Chronic Obstructive Pulmonary Disease', '280', 'Chronic Obstructive Pulmonary Disease', 0.346, 0.289, false, true, 'New V28 category'),
('J432', 'Centrilobular emphysema', '111', 'Chronic Obstructive Pulmonary Disease', '280', 'Chronic Obstructive Pulmonary Disease', 0.346, 0.289, false, true, 'New V28 category'),
('J438', 'Other emphysema', '111', 'Chronic Obstructive Pulmonary Disease', '280', 'Chronic Obstructive Pulmonary Disease', 0.346, 0.289, true, true, 'Document specific type'),
('J439', 'Emphysema, unspecified', '111', 'Chronic Obstructive Pulmonary Disease', '280', 'Chronic Obstructive Pulmonary Disease', 0.346, 0.289, true, true, 'Document specific type'),
('J440', 'COPD with acute lower respiratory infection', '111', 'Chronic Obstructive Pulmonary Disease', '280', 'Chronic Obstructive Pulmonary Disease', 0.346, 0.289, false, true, 'New V28 category'),
('J441', 'COPD with (acute) exacerbation', '111', 'Chronic Obstructive Pulmonary Disease', '280', 'Chronic Obstructive Pulmonary Disease', 0.346, 0.289, false, true, 'New V28 category'),
('J449', 'Chronic obstructive pulmonary disease, unspecified', '111', 'Chronic Obstructive Pulmonary Disease', '280', 'Chronic Obstructive Pulmonary Disease', 0.346, 0.289, true, true, 'Specify type and exacerbation status');

-- =========================================================================
-- CHRONIC KIDNEY DISEASE (HCC 136-138 V24 -> HCC 326-329 V28)
-- =========================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('N181', 'Chronic kidney disease, stage 1', NULL, NULL, NULL, NULL, NULL, NULL, true, false, 'Stage 1-2 CKD does not map to HCC'),
('N182', 'Chronic kidney disease, stage 2 (mild)', NULL, NULL, NULL, NULL, NULL, NULL, true, false, 'Stage 1-2 CKD does not map to HCC'),
('N183', 'Chronic kidney disease, stage 3 (moderate)', '138', 'CKD Moderate', '328', 'CKD Stage 3', 0.069, 0.069, false, true, 'New specific V28 category'),
('N184', 'Chronic kidney disease, stage 4 (severe)', '137', 'CKD Severe', '327', 'CKD Stage 4', 0.237, 0.289, false, true, 'Increased coefficient in V28'),
('N185', 'Chronic kidney disease, stage 5', '136', 'CKD Stage 5', '326', 'CKD Stage 5', 0.289, 0.306, false, true, 'Increased coefficient in V28'),
('N186', 'End stage renal disease', '136', 'CKD Stage 5', '326', 'CKD Stage 5', 0.289, 0.306, false, true, 'Increased coefficient in V28'),
('N189', 'Chronic kidney disease, unspecified', '138', 'CKD Moderate', '328', 'CKD Stage 3', 0.069, 0.069, true, true, 'Document specific stage'),
('Z940', 'Kidney transplant status', '136', 'CKD Stage 5', '326', 'CKD Stage 5', 0.289, 0.306, false, true, 'Transplant status maps to stage 5');

-- =========================================================================
-- MALIGNANT NEOPLASMS (HCC 8-12 V24 -> HCC 17-23 V28)
-- =========================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('C180', 'Malignant neoplasm of cecum', '12', 'Colorectal, Bladder, and Other Cancers', '23', 'Colorectal Cancer', 0.150, 0.189, false, true, 'New specific cancer categories in V28'),
('C181', 'Malignant neoplasm of appendix', '12', 'Colorectal, Bladder, and Other Cancers', '23', 'Colorectal Cancer', 0.150, 0.189, false, true, 'New specific cancer categories in V28'),
('C182', 'Malignant neoplasm of ascending colon', '12', 'Colorectal, Bladder, and Other Cancers', '23', 'Colorectal Cancer', 0.150, 0.189, false, true, 'New specific cancer categories in V28'),
('C183', 'Malignant neoplasm of hepatic flexure', '12', 'Colorectal, Bladder, and Other Cancers', '23', 'Colorectal Cancer', 0.150, 0.189, false, true, 'New specific cancer categories in V28'),
('C184', 'Malignant neoplasm of transverse colon', '12', 'Colorectal, Bladder, and Other Cancers', '23', 'Colorectal Cancer', 0.150, 0.189, false, true, 'New specific cancer categories in V28'),
('C185', 'Malignant neoplasm of splenic flexure', '12', 'Colorectal, Bladder, and Other Cancers', '23', 'Colorectal Cancer', 0.150, 0.189, false, true, 'New specific cancer categories in V28'),
('C186', 'Malignant neoplasm of descending colon', '12', 'Colorectal, Bladder, and Other Cancers', '23', 'Colorectal Cancer', 0.150, 0.189, false, true, 'New specific cancer categories in V28'),
('C187', 'Malignant neoplasm of sigmoid colon', '12', 'Colorectal, Bladder, and Other Cancers', '23', 'Colorectal Cancer', 0.150, 0.189, false, true, 'New specific cancer categories in V28'),
('C189', 'Malignant neoplasm of colon, unspecified', '12', 'Colorectal, Bladder, and Other Cancers', '23', 'Colorectal Cancer', 0.150, 0.189, true, true, 'Document specific site'),
('C19', 'Malignant neoplasm of rectosigmoid junction', '12', 'Colorectal, Bladder, and Other Cancers', '23', 'Colorectal Cancer', 0.150, 0.189, false, true, 'New V28 category'),
('C20', 'Malignant neoplasm of rectum', '12', 'Colorectal, Bladder, and Other Cancers', '23', 'Colorectal Cancer', 0.150, 0.189, false, true, 'New V28 category'),
('C220', 'Liver cell carcinoma', '9', 'Lung and Other Severe Cancers', '18', 'Liver and Other Severe GI Cancers', 0.960, 0.991, false, true, 'New specific cancer categories in V28'),
('C221', 'Intrahepatic bile duct carcinoma', '9', 'Lung and Other Severe Cancers', '18', 'Liver and Other Severe GI Cancers', 0.960, 0.991, false, true, 'New specific cancer categories in V28'),
('C340', 'Malignant neoplasm of main bronchus', '9', 'Lung and Other Severe Cancers', '17', 'Lung Cancer', 0.960, 0.897, false, true, 'New lung-specific category'),
('C3410', 'Malignant neoplasm of upper lobe, unspecified bronchus or lung', '9', 'Lung and Other Severe Cancers', '17', 'Lung Cancer', 0.960, 0.897, false, true, 'New lung-specific category'),
('C3411', 'Malignant neoplasm of upper lobe, right bronchus or lung', '9', 'Lung and Other Severe Cancers', '17', 'Lung Cancer', 0.960, 0.897, false, true, 'New lung-specific category'),
('C3412', 'Malignant neoplasm of upper lobe, left bronchus or lung', '9', 'Lung and Other Severe Cancers', '17', 'Lung Cancer', 0.960, 0.897, false, true, 'New lung-specific category'),
('C3430', 'Malignant neoplasm of lower lobe, unspecified bronchus or lung', '9', 'Lung and Other Severe Cancers', '17', 'Lung Cancer', 0.960, 0.897, false, true, 'New lung-specific category'),
('C3490', 'Malignant neoplasm of unspecified part of bronchus or lung', '9', 'Lung and Other Severe Cancers', '17', 'Lung Cancer', 0.960, 0.897, true, true, 'Document specific lobe'),
('C250', 'Malignant neoplasm of head of pancreas', '9', 'Lung and Other Severe Cancers', '18', 'Liver and Other Severe GI Cancers', 0.960, 0.991, false, true, 'New specific cancer categories in V28'),
('C251', 'Malignant neoplasm of body of pancreas', '9', 'Lung and Other Severe Cancers', '18', 'Liver and Other Severe GI Cancers', 0.960, 0.991, false, true, 'New specific cancer categories in V28'),
('C252', 'Malignant neoplasm of tail of pancreas', '9', 'Lung and Other Severe Cancers', '18', 'Liver and Other Severe GI Cancers', 0.960, 0.991, false, true, 'New specific cancer categories in V28'),
('C259', 'Malignant neoplasm of pancreas, unspecified', '9', 'Lung and Other Severe Cancers', '18', 'Liver and Other Severe GI Cancers', 0.960, 0.991, true, true, 'Document specific site'),
('C509', 'Malignant neoplasm of breast of unspecified site', '11', 'Breast, Prostate, and Other Cancers and Tumors', '22', 'Breast Cancer', 0.150, 0.180, true, true, 'Document laterality and site');

-- =========================================================================
-- MAJOR DEPRESSIVE DISORDER (HCC 59 V24 -> HCC 155 V28)
-- =========================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('F320', 'Major depressive disorder, single episode, mild', '59', 'Major Depressive, Bipolar, Paranoid Disorders', '155', 'Major Depression', 0.309, 0.325, false, true, 'New psychiatric category'),
('F321', 'Major depressive disorder, single episode, moderate', '59', 'Major Depressive, Bipolar, Paranoid Disorders', '155', 'Major Depression', 0.309, 0.325, false, true, 'New psychiatric category'),
('F322', 'Major depressive disorder, single episode, severe without psychotic features', '59', 'Major Depressive, Bipolar, Paranoid Disorders', '155', 'Major Depression', 0.309, 0.325, false, true, 'New psychiatric category'),
('F323', 'Major depressive disorder, single episode, severe with psychotic features', '59', 'Major Depressive, Bipolar, Paranoid Disorders', '155', 'Major Depression', 0.309, 0.325, false, true, 'New psychiatric category'),
('F324', 'Major depressive disorder, single episode, in partial remission', '59', 'Major Depressive, Bipolar, Paranoid Disorders', '155', 'Major Depression', 0.309, 0.325, false, true, 'New psychiatric category'),
('F329', 'Major depressive disorder, single episode, unspecified', '59', 'Major Depressive, Bipolar, Paranoid Disorders', '155', 'Major Depression', 0.309, 0.325, true, true, 'Document severity'),
('F330', 'Major depressive disorder, recurrent, mild', '59', 'Major Depressive, Bipolar, Paranoid Disorders', '155', 'Major Depression', 0.309, 0.325, false, true, 'New psychiatric category'),
('F331', 'Major depressive disorder, recurrent, moderate', '59', 'Major Depressive, Bipolar, Paranoid Disorders', '155', 'Major Depression', 0.309, 0.325, false, true, 'New psychiatric category'),
('F332', 'Major depressive disorder, recurrent severe without psychotic features', '59', 'Major Depressive, Bipolar, Paranoid Disorders', '155', 'Major Depression', 0.309, 0.325, false, true, 'New psychiatric category'),
('F333', 'Major depressive disorder, recurrent, severe with psychotic symptoms', '59', 'Major Depressive, Bipolar, Paranoid Disorders', '155', 'Major Depression', 0.309, 0.325, false, true, 'New psychiatric category'),
('F334', 'Major depressive disorder, recurrent, in remission', '59', 'Major Depressive, Bipolar, Paranoid Disorders', '155', 'Major Depression', 0.309, 0.325, false, true, 'New psychiatric category'),
('F339', 'Major depressive disorder, recurrent, unspecified', '59', 'Major Depressive, Bipolar, Paranoid Disorders', '155', 'Major Depression', 0.309, 0.325, true, true, 'Document severity');

-- =========================================================================
-- STROKE AND TIA (HCC 100-103 V24 -> HCC 221 V28)
-- =========================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('I6300', 'Cerebral infarction due to thrombosis of unspecified precerebral artery', '100', 'Ischemic or Unspecified Stroke', '221', 'Cerebrovascular Disease with or without TIA', 0.228, 0.259, true, true, 'Specify artery'),
('I6301', 'Cerebral infarction due to thrombosis of vertebral artery', '100', 'Ischemic or Unspecified Stroke', '221', 'Cerebrovascular Disease with or without TIA', 0.228, 0.259, false, true, 'New V28 category'),
('I6302', 'Cerebral infarction due to thrombosis of basilar artery', '100', 'Ischemic or Unspecified Stroke', '221', 'Cerebrovascular Disease with or without TIA', 0.228, 0.259, false, true, 'New V28 category'),
('I6303', 'Cerebral infarction due to thrombosis of carotid artery', '100', 'Ischemic or Unspecified Stroke', '221', 'Cerebrovascular Disease with or without TIA', 0.228, 0.259, false, true, 'New V28 category'),
('I6330', 'Cerebral infarction due to thrombosis of unspecified cerebral artery', '100', 'Ischemic or Unspecified Stroke', '221', 'Cerebrovascular Disease with or without TIA', 0.228, 0.259, true, true, 'Specify artery'),
('I6340', 'Cerebral infarction due to embolism of unspecified cerebral artery', '100', 'Ischemic or Unspecified Stroke', '221', 'Cerebrovascular Disease with or without TIA', 0.228, 0.259, true, true, 'Specify artery'),
('I6350', 'Cerebral infarction due to unspecified occlusion or stenosis of unsp cerebral artery', '100', 'Ischemic or Unspecified Stroke', '221', 'Cerebrovascular Disease with or without TIA', 0.228, 0.259, true, true, 'Specify artery and laterality'),
('I6389', 'Other cerebral infarction', '100', 'Ischemic or Unspecified Stroke', '221', 'Cerebrovascular Disease with or without TIA', 0.228, 0.259, false, true, 'New V28 category'),
('I639', 'Cerebral infarction, unspecified', '100', 'Ischemic or Unspecified Stroke', '221', 'Cerebrovascular Disease with or without TIA', 0.228, 0.259, true, true, 'Document type and location'),
('I6789', 'Other cerebrovascular disease', '103', 'Transient Ischemic Attack', '221', 'Cerebrovascular Disease with or without TIA', 0.186, 0.259, false, true, 'Merged into CVD category'),
('G450', 'Vertebro-basilar artery syndrome', '103', 'Transient Ischemic Attack', '221', 'Cerebrovascular Disease with or without TIA', 0.186, 0.259, false, true, 'TIA merged with stroke in V28'),
('G451', 'Carotid artery syndrome (hemispheric)', '103', 'Transient Ischemic Attack', '221', 'Cerebrovascular Disease with or without TIA', 0.186, 0.259, false, true, 'TIA merged with stroke in V28'),
('G458', 'Other transient cerebral ischemic attacks and related syndromes', '103', 'Transient Ischemic Attack', '221', 'Cerebrovascular Disease with or without TIA', 0.186, 0.259, false, true, 'TIA merged with stroke in V28'),
('G459', 'Transient cerebral ischemic attack, unspecified', '103', 'Transient Ischemic Attack', '221', 'Cerebrovascular Disease with or without TIA', 0.186, 0.259, true, true, 'Document specific syndrome');

-- =========================================================================
-- RHEUMATOID ARTHRITIS (HCC 40 V24 -> HCC 142 V28)
-- =========================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('M0500', 'Felty''s syndrome, unspecified site', '40', 'Rheumatoid Arthritis and Inflammatory Connective Tissue Disease', '142', 'Rheumatoid Arthritis and Specified Autoimmune Disorders', 0.374, 0.381, false, true, 'New autoimmune category'),
('M0510', 'Rheumatoid lung disease with rheumatoid arthritis of unspecified site', '40', 'Rheumatoid Arthritis and Inflammatory Connective Tissue Disease', '142', 'Rheumatoid Arthritis and Specified Autoimmune Disorders', 0.374, 0.381, false, true, 'New autoimmune category'),
('M0520', 'Rheumatoid vasculitis with rheumatoid arthritis of unspecified site', '40', 'Rheumatoid Arthritis and Inflammatory Connective Tissue Disease', '142', 'Rheumatoid Arthritis and Specified Autoimmune Disorders', 0.374, 0.381, false, true, 'New autoimmune category'),
('M0530', 'Rheumatoid heart disease with rheumatoid arthritis of unspecified site', '40', 'Rheumatoid Arthritis and Inflammatory Connective Tissue Disease', '142', 'Rheumatoid Arthritis and Specified Autoimmune Disorders', 0.374, 0.381, false, true, 'New autoimmune category'),
('M0540', 'Rheumatoid myopathy with rheumatoid arthritis of unspecified site', '40', 'Rheumatoid Arthritis and Inflammatory Connective Tissue Disease', '142', 'Rheumatoid Arthritis and Specified Autoimmune Disorders', 0.374, 0.381, false, true, 'New autoimmune category'),
('M0560', 'Rheumatoid arthritis of unspecified site with involvement of other organs and systems', '40', 'Rheumatoid Arthritis and Inflammatory Connective Tissue Disease', '142', 'Rheumatoid Arthritis and Specified Autoimmune Disorders', 0.374, 0.381, false, true, 'New autoimmune category'),
('M0570', 'Rheumatoid arthritis with rheumatoid factor of unspecified site without organ or systems involvement', '40', 'Rheumatoid Arthritis and Inflammatory Connective Tissue Disease', '142', 'Rheumatoid Arthritis and Specified Autoimmune Disorders', 0.374, 0.381, true, true, 'Document site and organ involvement'),
('M0580', 'Other rheumatoid arthritis with rheumatoid factor of unspecified site', '40', 'Rheumatoid Arthritis and Inflammatory Connective Tissue Disease', '142', 'Rheumatoid Arthritis and Specified Autoimmune Disorders', 0.374, 0.381, true, true, 'Document site'),
('M0590', 'Rheumatoid arthritis with rheumatoid factor, unspecified, unspecified site', '40', 'Rheumatoid Arthritis and Inflammatory Connective Tissue Disease', '142', 'Rheumatoid Arthritis and Specified Autoimmune Disorders', 0.374, 0.381, true, true, 'Document RF status and site'),
('M0600', 'Rheumatoid arthritis without rheumatoid factor, unspecified site', '40', 'Rheumatoid Arthritis and Inflammatory Connective Tissue Disease', '142', 'Rheumatoid Arthritis and Specified Autoimmune Disorders', 0.374, 0.381, true, true, 'Document site'),
('M0609', 'Rheumatoid arthritis without rheumatoid factor, multiple sites', '40', 'Rheumatoid Arthritis and Inflammatory Connective Tissue Disease', '142', 'Rheumatoid Arthritis and Specified Autoimmune Disorders', 0.374, 0.381, false, true, 'New autoimmune category');

-- =========================================================================
-- HIV/AIDS (HCC 1 V24 -> HCC 1 V28)
-- =========================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('B20', 'Human immunodeficiency virus [HIV] disease', '1', 'HIV/AIDS', '1', 'HIV/AIDS', 0.324, 0.334, false, false, 'No change in V28'),
('B97.35', 'Human immunodeficiency virus, type 2 [HIV 2] as the cause of diseases classified elsewhere', '1', 'HIV/AIDS', '1', 'HIV/AIDS', 0.324, 0.334, false, false, 'No change in V28'),
('O987', 'Human immunodeficiency virus [HIV] disease complicating pregnancy, childbirth and the puerperium', '1', 'HIV/AIDS', '1', 'HIV/AIDS', 0.324, 0.334, false, false, 'No change in V28'),
('Z21', 'Asymptomatic human immunodeficiency virus [HIV] infection status', '1', 'HIV/AIDS', '1', 'HIV/AIDS', 0.324, 0.334, false, false, 'No change in V28');

-- =========================================================================
-- VASCULAR DISEASE (HCC 107-108 V24 -> HCC 238-239 V28)
-- =========================================================================
INSERT INTO hcc.diagnosis_hcc_map (icd10_code, icd10_description, hcc_code_v24, hcc_name_v24, hcc_code_v28, hcc_name_v28, coefficient_v24, coefficient_v28, requires_specificity, changed_in_v28, v28_change_description) VALUES
('I7020', 'Unspecified atherosclerosis of native arteries of extremities', '108', 'Vascular Disease', '239', 'Vascular Disease', 0.288, 0.295, true, true, 'Document laterality and severity'),
('I70201', 'Unsp atherosclerosis of native arteries of extremities, right leg', '108', 'Vascular Disease', '239', 'Vascular Disease', 0.288, 0.295, false, true, 'New V28 category'),
('I70202', 'Unsp atherosclerosis of native arteries of extremities, left leg', '108', 'Vascular Disease', '239', 'Vascular Disease', 0.288, 0.295, false, true, 'New V28 category'),
('I70203', 'Unsp atherosclerosis of native arteries of extremities, bilateral legs', '108', 'Vascular Disease', '239', 'Vascular Disease', 0.288, 0.295, false, true, 'New V28 category'),
('I7021', 'Atherosclerosis of native arteries of extremities with intermittent claudication', '107', 'Vascular Disease with Complications', '238', 'Vascular Disease with Complications', 0.383, 0.392, false, true, 'New V28 category'),
('I7022', 'Atherosclerosis of native arteries of extremities with rest pain', '107', 'Vascular Disease with Complications', '238', 'Vascular Disease with Complications', 0.383, 0.392, false, true, 'New V28 category'),
('I70231', 'Athscl native arteries of right leg w ulceration of thigh', '107', 'Vascular Disease with Complications', '238', 'Vascular Disease with Complications', 0.383, 0.392, false, true, 'New V28 category'),
('I7024', 'Atherosclerosis of native arteries of extremities with gangrene', '107', 'Vascular Disease with Complications', '238', 'Vascular Disease with Complications', 0.383, 0.392, false, true, 'New V28 category'),
('I7025', 'Atherosclerosis of native arteries of other extremities with ulceration', '107', 'Vascular Disease with Complications', '238', 'Vascular Disease with Complications', 0.383, 0.392, false, true, 'New V28 category'),
('I739', 'Peripheral vascular disease, unspecified', '108', 'Vascular Disease', '239', 'Vascular Disease', 0.288, 0.295, true, true, 'Document specific condition');
