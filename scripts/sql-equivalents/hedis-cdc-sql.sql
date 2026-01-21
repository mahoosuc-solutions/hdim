-- SQL Equivalent for HEDIS-CDC (Comprehensive Diabetes Care - HbA1c Control)
-- This query replicates the CQL measure logic using traditional SQL

-- Parameters:
-- $1: patient_id (UUID)
-- $2: tenant_id (VARCHAR)
-- $3: measurement_period_start (DATE) - default: CURRENT_DATE - INTERVAL '1 year'
-- $4: measurement_period_end (DATE) - default: CURRENT_DATE

WITH measurement_period AS (
    SELECT 
        COALESCE($3, CURRENT_DATE - INTERVAL '1 year') as period_start,
        COALESCE($4, CURRENT_DATE) as period_end
),
patient_data AS (
    SELECT 
        p.id as patient_id,
        p.birth_date,
        EXTRACT(YEAR FROM AGE(mp.period_end, p.birth_date)) as age_at_end,
        EXTRACT(YEAR FROM AGE(mp.period_start, p.birth_date)) as age_at_start
    FROM patient.patients p
    CROSS JOIN measurement_period mp
    WHERE p.id = $1
),
-- Initial Population: Patients 18-75 years old
initial_population AS (
    SELECT 
        pd.patient_id,
        CASE 
            WHEN pd.age_at_start >= 18 AND pd.age_at_end <= 75
            THEN 1 ELSE 0
        END as in_initial_population
    FROM patient_data pd
),
-- Denominator: Initial population with diabetes diagnosis
denominator AS (
    SELECT 
        ip.patient_id,
        CASE 
            WHEN ip.in_initial_population = 1
            AND EXISTS (
                SELECT 1 
                FROM fhir.conditions c
                WHERE c.patient_id = ip.patient_id
                AND c.tenant_id = $2
                AND (
                    -- Type 1 Diabetes (E10.x)
                    c.code LIKE 'E10%' OR
                    -- Type 2 Diabetes (E11.x)
                    c.code LIKE 'E11%'
                )
                AND c.clinical_status = 'active'
                AND c.onset_date <= (SELECT period_end FROM measurement_period)
            )
            THEN 1 ELSE 0
        END as in_denominator
    FROM initial_population ip
),
-- Numerator: Patients with HbA1c <= 7.0% in measurement period
numerator AS (
    SELECT 
        d.patient_id,
        CASE 
            WHEN d.in_denominator = 1
            AND EXISTS (
                SELECT 1 
                FROM fhir.observations o
                WHERE o.patient_id = d.patient_id
                AND o.tenant_id = $2
                AND o.code = '4548-4'  -- HbA1c LOINC code
                AND o.effective_date_time >= (SELECT period_start FROM measurement_period)
                AND o.effective_date_time <= (SELECT period_end FROM measurement_period)
                AND o.status = 'final'
                AND o.value_numeric IS NOT NULL
                AND o.value_numeric <= 7.0
            )
            THEN 1 ELSE 0
        END as in_numerator
    FROM denominator d
)
-- Final Result
SELECT 
    n.patient_id,
    n.in_denominator,
    n.in_numerator,
    CASE 
        WHEN n.in_denominator = 1 AND n.in_numerator = 1 
        THEN true 
        ELSE false 
    END as compliant,
    CASE 
        WHEN n.in_denominator = 1 
        THEN 
            (SELECT value_numeric 
             FROM fhir.observations 
             WHERE patient_id = n.patient_id
             AND code = '4548-4'
             AND effective_date_time >= (SELECT period_start FROM measurement_period)
             AND effective_date_time <= (SELECT period_end FROM measurement_period)
             AND status = 'final'
             ORDER BY effective_date_time DESC
             LIMIT 1)
        ELSE NULL
    END as latest_hba1c_value
FROM numerator n;

-- Performance Notes:
-- This query uses:
-- 1. Index on fhir.conditions(patient_id, code, tenant_id)
-- 2. Index on fhir.observations(patient_id, code, effective_date_time, tenant_id)
-- 3. Index on patient.patients(id)
--
-- Expected execution time: 200-400ms for typical patient
-- With proper indexes: 50-150ms
