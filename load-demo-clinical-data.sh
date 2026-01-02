#!/bin/bash

################################################################################
# Load Demo Clinical Data
# Creates realistic patient data, quality measures, and care gaps for demo
################################################################################

set -e

DB_CONTAINER="healthdata-postgres"
DB_NAME="healthdata_cql"
DB_USER="healthdata"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║           Loading Demo Clinical Data                          ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Create SQL with demo data
cat > /tmp/demo-clinical-data.sql << 'EOF'
-- Demo Clinical Data for Health Data in Motion Platform
-- Tenant: demo-clinic

-- Sample Quality Measure Results (Depression Screening - CMS2)
INSERT INTO quality_measure_results (
    id, tenant_id, patient_id, measure_id, measure_name, measure_category,
    measure_year, numerator_compliant, denominator_elligible, compliance_rate,
    score, calculation_date, cql_library, created_at, created_by
) VALUES
-- Patient 1: High performer - compliant
(
    gen_random_uuid(), 'demo-clinic', 
    '550e8400-e29b-41d4-a716-446655440001'::uuid,
    'CMS2', 'Preventive Care and Screening: Screening for Depression and Follow-Up Plan',
    'Behavioral Health', 2025, true, true, 1.0, 100.0,
    '2025-11-20', 'CMS2v13', CURRENT_TIMESTAMP, 'system'
),
-- Patient 2: Non-compliant
(
    gen_random_uuid(), 'demo-clinic',
    '550e8400-e29b-41d4-a716-446655440002'::uuid,
    'CMS2', 'Preventive Care and Screening: Screening for Depression and Follow-Up Plan',
    'Behavioral Health', 2025, false, true, 0.0, 0.0,
    '2025-11-20', 'CMS2v13', CURRENT_TIMESTAMP, 'system'
),
-- Patient 3: Compliant
(
    gen_random_uuid(), 'demo-clinic',
    '550e8400-e29b-41d4-a716-446655440003'::uuid,
    'CMS2', 'Preventive Care and Screening: Screening for Depression and Follow-Up Plan',
    'Behavioral Health', 2025, true, true, 1.0, 100.0,
    '2025-11-20', 'CMS2v13', CURRENT_TIMESTAMP, 'system'
),
-- CMS134: Diabetes Care - HbA1c Testing
(
    gen_random_uuid(), 'demo-clinic',
    '550e8400-e29b-41d4-a716-446655440001'::uuid,
    'CMS134', 'Diabetes: Medical Attention for Nephropathy',
    'Chronic Disease Management', 2025, true, true, 1.0, 100.0,
    '2025-11-20', 'CMS134v12', CURRENT_TIMESTAMP, 'system'
),
(
    gen_random_uuid(), 'demo-clinic',
    '550e8400-e29b-41d4-a716-446655440004'::uuid,
    'CMS134', 'Diabetes: Medical Attention for Nephropathy',
    'Chronic Disease Management', 2025, false, true, 0.0, 0.0,
    '2025-11-20', 'CMS134v12', CURRENT_TIMESTAMP, 'system'
),
-- CMS122: Diabetes: Hemoglobin A1c (HbA1c) Poor Control
(
    gen_random_uuid(), 'demo-clinic',
    '550e8400-e29b-41d4-a716-446655440001'::uuid,
    'CMS122', 'Diabetes: Hemoglobin A1c (HbA1c) Poor Control (>9%)',
    'Chronic Disease Management', 2025, false, true, 0.0, 0.0,
    '2025-11-20', 'CMS122v12', CURRENT_TIMESTAMP, 'system'
),
-- CMS165: Controlling High Blood Pressure
(
    gen_random_uuid(), 'demo-clinic',
    '550e8400-e29b-41d4-a716-446655440005'::uuid,
    'CMS165', 'Controlling High Blood Pressure',
    'Chronic Disease Management', 2025, true, true, 1.0, 100.0,
    '2025-11-20', 'CMS165v12', CURRENT_TIMESTAMP, 'system'
),
(
    gen_random_uuid(), 'demo-clinic',
    '550e8400-e29b-41d4-a716-446655440002'::uuid,
    'CMS165', 'Controlling High Blood Pressure',
    'Chronic Disease Management', 2025, false, true, 0.0, 0.0,
    '2025-11-20', 'CMS165v12', CURRENT_TIMESTAMP, 'system'
);

-- Care Gaps for non-compliant patients
INSERT INTO care_gaps (
    id, tenant_id, patient_id, category, gap_type, title, description,
    priority, status, quality_measure, recommendation, evidence,
    due_date, identified_date, created_at, updated_at
) VALUES
-- Depression screening gap
(
    gen_random_uuid(), 'demo-clinic', '550e8400-e29b-41d4-a716-446655440002',
    'behavioral_health', 'screening_overdue', 
    'Depression Screening Overdue',
    'Patient has not completed depression screening in past 12 months. Annual screening required per CMS2.',
    'high', 'open', 'CMS2',
    'Schedule depression screening (PHQ-9) at next visit. If positive, develop follow-up plan within 24 hours.',
    'Last documented screening: 15 months ago. Patient demographic: age 45, history of anxiety.',
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    CURRENT_TIMESTAMP - INTERVAL '5 days',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- Diabetes nephropathy gap
(
    gen_random_uuid(), 'demo-clinic', '550e8400-e29b-41d4-a716-446655440004',
    'chronic_disease', 'test_overdue',
    'Diabetes Nephropathy Screening Due',
    'Patient with diabetes has not had nephropathy screening (urine microalbumin or ACR) in past 12 months.',
    'high', 'open', 'CMS134',
    'Order urine microalbumin/creatinine ratio (ACR). If elevated, consider ACE inhibitor or ARB.',
    'Diagnosis: Type 2 Diabetes (E11.9). Last HbA1c: 7.8%. No documented kidney screening in 18 months.',
    CURRENT_TIMESTAMP + INTERVAL '14 days',
    CURRENT_TIMESTAMP - INTERVAL '3 days',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- Blood pressure control gap
(
    gen_random_uuid(), 'demo-clinic', '550e8400-e29b-41d4-a716-446655440002',
    'chronic_disease', 'control_inadequate',
    'Blood Pressure Not at Goal',
    'Patient with hypertension has blood pressure readings above goal (<140/90 mmHg).',
    'medium', 'open', 'CMS165',
    'Review medication adherence. Consider intensifying antihypertensive therapy. Schedule follow-up in 2 weeks.',
    'Recent BP: 152/94 mmHg. Current meds: Lisinopril 10mg daily. Patient reports good adherence.',
    CURRENT_TIMESTAMP + INTERVAL '21 days',
    CURRENT_TIMESTAMP - INTERVAL '2 days',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- Preventive care gap
(
    gen_random_uuid(), 'demo-clinic', '550e8400-e29b-41d4-a716-446655440003',
    'prevention', 'vaccination_due',
    'Influenza Vaccination Recommended',
    'Patient is eligible for seasonal influenza vaccination and has not received current season dose.',
    'low', 'open', NULL,
    'Offer influenza vaccination at next visit. Discuss benefits and address any concerns.',
    'Age 62, no documented flu shot this season. Patient has COPD - high-risk category.',
    CURRENT_TIMESTAMP + INTERVAL '60 days',
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- HbA1c control gap
(
    gen_random_uuid(), 'demo-clinic', '550e8400-e29b-41d4-a716-446655440001',
    'chronic_disease', 'control_poor',
    'HbA1c Above Goal',
    'Patient with diabetes has HbA1c >9%, indicating poor glycemic control.',
    'high', 'open', 'CMS122',
    'Review diabetes management plan. Consider medication adjustment. Refer to diabetes educator if available.',
    'Recent HbA1c: 9.4%. Diagnosis: Type 2 Diabetes (E11.9). Current meds: Metformin 1000mg BID.',
    CURRENT_TIMESTAMP + INTERVAL '7 days',
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- Summary data for reporting
DO $$
DECLARE
    total_results INTEGER;
    compliant_results INTEGER;
    compliance_pct NUMERIC(5,2);
    total_gaps INTEGER;
    high_priority_gaps INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_results FROM quality_measure_results WHERE tenant_id = 'demo-clinic';
    SELECT COUNT(*) INTO compliant_results FROM quality_measure_results 
        WHERE tenant_id = 'demo-clinic' AND numerator_compliant = true;
    SELECT COUNT(*) INTO total_gaps FROM care_gaps WHERE tenant_id = 'demo-clinic';
    SELECT COUNT(*) INTO high_priority_gaps FROM care_gaps 
        WHERE tenant_id = 'demo-clinic' AND priority = 'high';
    
    compliance_pct := (compliant_results::NUMERIC / NULLIF(total_results, 0)) * 100;
    
    RAISE NOTICE '════════════════════════════════════════════════════════════════';
    RAISE NOTICE 'Demo Data Loaded Successfully!';
    RAISE NOTICE '════════════════════════════════════════════════════════════════';
    RAISE NOTICE 'Quality Measure Results: % total', total_results;
    RAISE NOTICE 'Compliant Results:       % (%%)', compliant_results, ROUND(compliance_pct, 1);
    RAISE NOTICE 'Care Gaps Identified:    % total', total_gaps;
    RAISE NOTICE 'High Priority Gaps:      %', high_priority_gaps;
    RAISE NOTICE '════════════════════════════════════════════════════════════════';
END $$;

-- Display sample data
SELECT 
    measure_id,
    measure_name,
    COUNT(*) as total_patients,
    SUM(CASE WHEN numerator_compliant THEN 1 ELSE 0 END) as compliant,
    ROUND(AVG(compliance_rate) * 100, 1) as compliance_pct
FROM quality_measure_results
WHERE tenant_id = 'demo-clinic'
GROUP BY measure_id, measure_name
ORDER BY measure_id;

SELECT 
    category,
    priority,
    COUNT(*) as gap_count
FROM care_gaps
WHERE tenant_id = 'demo-clinic'
GROUP BY category, priority
ORDER BY 
    CASE priority 
        WHEN 'high' THEN 1 
        WHEN 'medium' THEN 2 
        WHEN 'low' THEN 3 
    END,
    category;
EOF

echo "Loading demo data into database..."
docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < /tmp/demo-clinical-data.sql

echo ""
echo "✓ Demo clinical data loaded successfully!"
echo ""
echo "Next steps:"
echo "  1. Run: ./demo-full-system.sh"
echo "  2. Login as demo.doctor to see patient data"
echo "  3. View quality measures and care gaps"
