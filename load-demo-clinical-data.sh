#!/bin/bash

################################################################################
# Load Demo Clinical Data
# Creates realistic patient data, quality measures, and care gaps for demo
################################################################################

set -e

DB_CONTAINER="${DB_CONTAINER:-hdim-demo-postgres}"
QUALITY_DB_NAME="${QUALITY_DB_NAME:-quality_db}"
CAREGAP_DB_NAME="${CAREGAP_DB_NAME:-caregap_db}"
DB_USER="healthdata"
TENANT_ID="${TENANT_ID:-acme-health}"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║           Loading Demo Clinical Data                          ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Create SQL with demo data
cat > /tmp/demo-quality-results.sql << 'EOF'
-- Demo Clinical Data for Health Data in Motion Platform
-- Tenant: __TENANT_ID__

-- Sample Quality Measure Results (Depression Screening - CMS2)
INSERT INTO quality_measure_results (
    id, tenant_id, patient_id, measure_id, measure_name, measure_category,
    measure_year, numerator_compliant, denominator_eligible, denominator_elligible, compliance_rate,
    score, calculation_date, cql_library, created_at, created_by
) VALUES
-- Patient 1: High performer - compliant
(
    gen_random_uuid(), '__TENANT_ID__', 
    '550e8400-e29b-41d4-a716-446655440001'::uuid,
    'CMS2', 'Preventive Care and Screening: Screening for Depression and Follow-Up Plan',
    'Behavioral Health', 2025, true, true, true, 1.0, 100.0,
    '2025-11-20', 'CMS2v13', CURRENT_TIMESTAMP, 'system'
),
-- Patient 2: Non-compliant
(
    gen_random_uuid(), '__TENANT_ID__',
    '550e8400-e29b-41d4-a716-446655440002'::uuid,
    'CMS2', 'Preventive Care and Screening: Screening for Depression and Follow-Up Plan',
    'Behavioral Health', 2025, false, true, true, 0.0, 0.0,
    '2025-11-20', 'CMS2v13', CURRENT_TIMESTAMP, 'system'
),
-- Patient 3: Compliant
(
    gen_random_uuid(), '__TENANT_ID__',
    '550e8400-e29b-41d4-a716-446655440003'::uuid,
    'CMS2', 'Preventive Care and Screening: Screening for Depression and Follow-Up Plan',
    'Behavioral Health', 2025, true, true, true, 1.0, 100.0,
    '2025-11-20', 'CMS2v13', CURRENT_TIMESTAMP, 'system'
),
-- CMS134: Diabetes Care - HbA1c Testing
(
    gen_random_uuid(), '__TENANT_ID__',
    '550e8400-e29b-41d4-a716-446655440001'::uuid,
    'CMS134', 'Diabetes: Medical Attention for Nephropathy',
    'Chronic Disease Management', 2025, true, true, true, 1.0, 100.0,
    '2025-11-20', 'CMS134v12', CURRENT_TIMESTAMP, 'system'
),
(
    gen_random_uuid(), '__TENANT_ID__',
    '550e8400-e29b-41d4-a716-446655440004'::uuid,
    'CMS134', 'Diabetes: Medical Attention for Nephropathy',
    'Chronic Disease Management', 2025, false, true, true, 0.0, 0.0,
    '2025-11-20', 'CMS134v12', CURRENT_TIMESTAMP, 'system'
),
-- CMS122: Diabetes: Hemoglobin A1c (HbA1c) Poor Control
(
    gen_random_uuid(), '__TENANT_ID__',
    '550e8400-e29b-41d4-a716-446655440001'::uuid,
    'CMS122', 'Diabetes: Hemoglobin A1c (HbA1c) Poor Control (>9%)',
    'Chronic Disease Management', 2025, false, true, true, 0.0, 0.0,
    '2025-11-20', 'CMS122v12', CURRENT_TIMESTAMP, 'system'
),
-- CMS165: Controlling High Blood Pressure
(
    gen_random_uuid(), '__TENANT_ID__',
    '550e8400-e29b-41d4-a716-446655440005'::uuid,
    'CMS165', 'Controlling High Blood Pressure',
    'Chronic Disease Management', 2025, true, true, true, 1.0, 100.0,
    '2025-11-20', 'CMS165v12', CURRENT_TIMESTAMP, 'system'
),
(
    gen_random_uuid(), '__TENANT_ID__',
    '550e8400-e29b-41d4-a716-446655440002'::uuid,
    'CMS165', 'Controlling High Blood Pressure',
    'Chronic Disease Management', 2025, false, true, true, 0.0, 0.0,
    '2025-11-20', 'CMS165v12', CURRENT_TIMESTAMP, 'system'
);

DO $$
DECLARE
    total_results INTEGER;
    compliant_results INTEGER;
    compliance_pct NUMERIC(5,2);
BEGIN
    SELECT COUNT(*) INTO total_results FROM quality_measure_results WHERE tenant_id = '__TENANT_ID__';
    SELECT COUNT(*) INTO compliant_results FROM quality_measure_results 
        WHERE tenant_id = '__TENANT_ID__' AND numerator_compliant = true;
    
    compliance_pct := (compliant_results::NUMERIC / NULLIF(total_results, 0)) * 100;
    
    RAISE NOTICE '════════════════════════════════════════════════════════════════';
    RAISE NOTICE 'Quality Measure Demo Data Loaded Successfully!';
    RAISE NOTICE '════════════════════════════════════════════════════════════════';
    RAISE NOTICE 'Quality Measure Results: % total', total_results;
    RAISE NOTICE 'Compliant Results:       %', compliant_results;
    RAISE NOTICE 'Compliance Percent:     %', ROUND(compliance_pct, 1);
    RAISE NOTICE '════════════════════════════════════════════════════════════════';
END $$;

-- Display sample data
SELECT 
    measure_id,
    measure_name,
    COUNT(*) as total_patients,
    SUM(CASE WHEN numerator_compliant THEN 1 ELSE 0 END) as compliant,
    ROUND((AVG(compliance_rate)::numeric) * 100, 1) as compliance_pct
FROM quality_measure_results
WHERE tenant_id = '__TENANT_ID__'
GROUP BY measure_id, measure_name
ORDER BY measure_id;
EOF
sed -i "s/__TENANT_ID__/${TENANT_ID}/g" /tmp/demo-quality-results.sql

cat > /tmp/demo-care-gaps.sql << 'EOF'
-- Demo Clinical Data for Health Data in Motion Platform
-- Tenant: __TENANT_ID__

-- Care Gaps for non-compliant patients
INSERT INTO care_gaps (
    id, tenant_id, patient_id, measure_id, measure_name, gap_type, gap_category, description,
    priority, severity, status, recommendation, identified_date, due_date, created_at, updated_at, created_by
) VALUES
-- Depression screening gap
(
    gen_random_uuid(), '__TENANT_ID__', '550e8400-e29b-41d4-a716-446655440002',
    'CMS2', 'Depression Screening',
    'screening_overdue', 'behavioral_health',
    'Patient has not completed depression screening in past 12 months. Annual screening required per CMS2.',
    'HIGH', 'HIGH', 'OPEN',
    'Schedule depression screening (PHQ-9) at next visit. If positive, develop follow-up plan within 24 hours.',
    CURRENT_TIMESTAMP - INTERVAL '5 days',
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'
),
-- Diabetes nephropathy gap
(
    gen_random_uuid(), '__TENANT_ID__', '550e8400-e29b-41d4-a716-446655440004',
    'CMS134', 'Diabetes Nephropathy Screening',
    'test_overdue', 'chronic_disease',
    'Patient with diabetes has not had nephropathy screening (urine microalbumin or ACR) in past 12 months.',
    'HIGH', 'HIGH', 'OPEN',
    'Order urine microalbumin/creatinine ratio (ACR). If elevated, consider ACE inhibitor or ARB.',
    CURRENT_TIMESTAMP - INTERVAL '3 days',
    CURRENT_TIMESTAMP + INTERVAL '14 days',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'
),
-- Blood pressure control gap
(
    gen_random_uuid(), '__TENANT_ID__', '550e8400-e29b-41d4-a716-446655440002',
    'CMS165', 'Controlling High Blood Pressure',
    'control_inadequate', 'chronic_disease',
    'Patient with hypertension has blood pressure readings above goal (<140/90 mmHg).',
    'MEDIUM', 'MEDIUM', 'OPEN',
    'Review medication adherence. Consider intensifying antihypertensive therapy. Schedule follow-up in 2 weeks.',
    CURRENT_TIMESTAMP - INTERVAL '2 days',
    CURRENT_TIMESTAMP + INTERVAL '21 days',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'
),
-- Preventive care gap
(
    gen_random_uuid(), '__TENANT_ID__', '550e8400-e29b-41d4-a716-446655440003',
    'IMM-FLU', 'Influenza Vaccination',
    'vaccination_due', 'prevention',
    'Patient is eligible for seasonal influenza vaccination and has not received current season dose.',
    'LOW', 'LOW', 'OPEN',
    'Offer influenza vaccination at next visit. Discuss benefits and address any concerns.',
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    CURRENT_TIMESTAMP + INTERVAL '60 days',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'
),
-- HbA1c control gap
(
    gen_random_uuid(), '__TENANT_ID__', '550e8400-e29b-41d4-a716-446655440001',
    'CMS122', 'Diabetes HbA1c Poor Control',
    'control_poor', 'chronic_disease',
    'Patient with diabetes has HbA1c >9%, indicating poor glycemic control.',
    'HIGH', 'HIGH', 'OPEN',
    'Review diabetes management plan. Consider medication adjustment. Refer to diabetes educator if available.',
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    CURRENT_TIMESTAMP + INTERVAL '7 days',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'
);

DO $$
DECLARE
    total_gaps INTEGER;
    high_priority_gaps INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_gaps FROM care_gaps WHERE tenant_id = '__TENANT_ID__';
    SELECT COUNT(*) INTO high_priority_gaps FROM care_gaps 
        WHERE tenant_id = '__TENANT_ID__' AND priority = 'HIGH';

    RAISE NOTICE '════════════════════════════════════════════════════════════════';
    RAISE NOTICE 'Care Gap Demo Data Loaded Successfully!';
    RAISE NOTICE '════════════════════════════════════════════════════════════════';
    RAISE NOTICE 'Care Gaps Identified:    % total', total_gaps;
    RAISE NOTICE 'High Priority Gaps:      %', high_priority_gaps;
    RAISE NOTICE '════════════════════════════════════════════════════════════════';
END $$;

SELECT 
    gap_category,
    priority,
    COUNT(*) as gap_count
FROM care_gaps
WHERE tenant_id = '__TENANT_ID__'
GROUP BY gap_category, priority
ORDER BY 
    CASE priority 
        WHEN 'HIGH' THEN 1 
        WHEN 'MEDIUM' THEN 2 
        WHEN 'LOW' THEN 3 
    END,
    gap_category;
EOF
sed -i "s/__TENANT_ID__/${TENANT_ID}/g" /tmp/demo-care-gaps.sql

echo "Loading demo quality measure data into database..."
docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$QUALITY_DB_NAME" < /tmp/demo-quality-results.sql

echo "Loading demo care gap data into database..."
docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$CAREGAP_DB_NAME" < /tmp/demo-care-gaps.sql

echo ""
echo "✓ Demo clinical data loaded successfully!"
echo ""
echo "Next steps:"
echo "  1. Run: ./demo-full-system.sh"
echo "  2. Login as demo.doctor to see patient data"
echo "  3. View quality measures and care gaps"
