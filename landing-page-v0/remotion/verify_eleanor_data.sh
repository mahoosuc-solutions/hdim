#!/bin/bash
# Verification script for Eleanor Anderson test data

echo "╔════════════════════════════════════════════════════════════════════════╗"
echo "║   ELEANOR ANDERSON DATA VERIFICATION                                  ║"
echo "╚════════════════════════════════════════════════════════════════════════╝"
echo ""

echo "📊 Checking Patient Record..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
docker exec healthdata-postgres psql -U healthdata -d patient_db -c "
SELECT
    first_name || ' ' || last_name as full_name,
    mrn,
    DATE_PART('year', AGE(CURRENT_DATE, date_of_birth))::int as age,
    gender,
    CASE WHEN active THEN 'Active' ELSE 'Inactive' END as status
FROM patient_demographics
WHERE last_name = 'Anderson' AND first_name = 'Eleanor';
"

echo ""
echo "🩺 Checking Care Gap..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
docker exec healthdata-postgres psql -U healthdata -d caregap_db -c "
SELECT
    measure_name,
    priority,
    status,
    NOW()::date - due_date as days_overdue
FROM care_gaps
WHERE measure_id = 'BCS-E' AND status = 'OPEN';
"

echo ""
echo "📈 Checking Dashboard Statistics..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
docker exec healthdata-postgres psql -U healthdata -d caregap_db -c "
SELECT
    'Total Gaps' as metric,
    COUNT(*)::text as value
FROM care_gaps WHERE status = 'OPEN'
UNION ALL
SELECT
    'HIGH Priority' as metric,
    COUNT(*)::text as value
FROM care_gaps WHERE status = 'OPEN' AND priority = 'HIGH'
UNION ALL
SELECT
    'MEDIUM Priority' as metric,
    COUNT(*)::text as value
FROM care_gaps WHERE status = 'OPEN' AND priority = 'MEDIUM'
UNION ALL
SELECT
    'LOW Priority' as metric,
    COUNT(*)::text as value
FROM care_gaps WHERE status = 'OPEN' AND priority = 'LOW';
"

echo ""
echo "✅ VERIFICATION COMPLETE"
echo ""
echo "Next step: Open http://localhost:4200/care-gaps to verify in UI"
echo ""
