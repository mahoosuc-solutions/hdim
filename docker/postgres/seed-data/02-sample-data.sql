-- HealthData-in-Motion Sample Data
-- Test data for CQL Engine Service development
-- Version: 1.0.0

\c healthdata_cql;

-- Sample CQL Libraries for testing
INSERT INTO cql_library (tenant_id, library_name, version, status, cql_content, description, publisher, active, created_at, updated_at)
VALUES
    ('tenant-1', 'BCS', '2024', 'ACTIVE', 'library BCS version ''2024''', 'Breast Cancer Screening', 'NCQA', true, NOW(), NOW()),
    ('tenant-1', 'CDC', '2024', 'ACTIVE', 'library CDC version ''2024''', 'Diabetes Screening', 'NCQA', true, NOW(), NOW()),
    ('tenant-1', 'AMM', '2024', 'ACTIVE', 'library AMM version ''2024''', 'Antidepressant Medication Management', 'NCQA', true, NOW(), NOW()),
    ('tenant-2', 'BCS', '2024', 'ACTIVE', 'library BCS version ''2024''', 'Breast Cancer Screening', 'NCQA', true, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Sample Value Sets
INSERT INTO value_set (tenant_id, oid, name, code_system, version, codes, description, publisher, status, active, created_at, updated_at)
VALUES
    ('tenant-1', '2.16.840.1.113883.3.464.1003.108.12.1001', 'Mammography', 'SNOMED', '2024-01', '["24606-6","87168-4","77065","77066","77067"]', 'Mammography procedure codes', 'NCQA', 'ACTIVE', true, NOW(), NOW()),
    ('tenant-1', '2.16.840.1.113883.3.464.1003.103.12.1001', 'Diabetes', 'SNOMED', '2024-01', '["44054006","73211009","11530004","359642000"]', 'Diabetes diagnosis codes', 'NCQA', 'ACTIVE', true, NOW(), NOW()),
    ('tenant-1', '2.16.840.1.113883.3.464.1003.196.12.1001', 'Depression', 'SNOMED', '2024-01', '["35489007","36923009","40379007","87512008"]', 'Depression diagnosis codes', 'NCQA', 'ACTIVE', true, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Sample CQL Evaluations for testing
INSERT INTO cql_evaluation (tenant_id, library_id, patient_id, status, evaluation_date, context_data, evaluation_result, duration_ms, created_at)
SELECT
    'tenant-1',
    (SELECT id FROM cql_library WHERE library_name = 'BCS' AND tenant_id = 'tenant-1' LIMIT 1),
    'patient-' || i,
    CASE WHEN i % 3 = 0 THEN 'SUCCESS' WHEN i % 3 = 1 THEN 'FAILED' ELSE 'PENDING' END,
    NOW() - (i || ' days')::INTERVAL,
    '{"age": ' || (40 + i) || ', "gender": "female"}',
    CASE WHEN i % 3 = 0 THEN '{"inDenominator": true, "inNumerator": ' || (CASE WHEN i % 2 = 0 THEN 'true' ELSE 'false' END) || '}' ELSE NULL END,
    CASE WHEN i % 3 = 0 THEN 100 + (i * 10) ELSE NULL END,
    NOW() - (i || ' days')::INTERVAL
FROM generate_series(1, 20) AS i
ON CONFLICT DO NOTHING;

-- Display sample data statistics
SELECT
    'CQL Libraries' AS table_name,
    COUNT(*) AS row_count
FROM cql_library
WHERE active = true
UNION ALL
SELECT
    'Value Sets' AS table_name,
    COUNT(*) AS row_count
FROM value_set
WHERE active = true
UNION ALL
SELECT
    'CQL Evaluations' AS table_name,
    COUNT(*) AS row_count
FROM cql_evaluation;

-- Success message
SELECT 'Sample data loaded successfully!' AS status;
