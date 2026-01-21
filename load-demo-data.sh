#!/bin/bash

# HealthData in Motion - Demo Data Loader
# Loads sample patients and runs quality measure evaluations

set -e

echo "========================================="
echo "HealthData in Motion - Demo Data Loader"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

TENANT_ID="${TENANT_ID:-acme-health}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
AUTH_USERNAME="${AUTH_USERNAME:-demo.admin}"
AUTH_PASSWORD="${AUTH_PASSWORD:-demo123}"
QM_SERVICE="${QM_SERVICE:-http://localhost:8087/quality-measure}"
CQL_SERVICE="${CQL_SERVICE:-http://localhost:8081/cql-engine}"
FHIR_SERVICE="${FHIR_SERVICE:-http://localhost:8085/fhir}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-hdim-demo-postgres}"

AUTH_TOKEN=$(curl -s -X POST "${GATEWAY_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${AUTH_USERNAME}\",\"password\":\"${AUTH_PASSWORD}\"}" | jq -r '.accessToken' 2>/dev/null)

AUTH_HEADER=()
if [ -n "$AUTH_TOKEN" ] && [ "$AUTH_TOKEN" != "null" ]; then
  AUTH_HEADER=(-H "Authorization: Bearer $AUTH_TOKEN")
fi

# Function to check if service is available
check_service() {
    local service_url=$1
    local service_name=$2

    echo -n "Checking $service_name... "
    if curl -s -f "$service_url" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠ Not available${NC}"
        return 1
    fi
}

# Check all services
echo -e "${BLUE}Step 1: Verifying Services${NC}"
echo "-------------------------------------------"
check_service "$QM_SERVICE/_health" "Quality Measure Service" || true
check_service "$CQL_SERVICE/actuator/health" "CQL Engine Service" || true
check_service "$FHIR_SERVICE/metadata" "FHIR Service" || true
echo ""

# Load patients directly into database
echo -e "${BLUE}Step 2: Loading Sample Patients${NC}"
echo "-------------------------------------------"
docker exec "$POSTGRES_CONTAINER" psql -U healthdata -d healthdata_cql <<SQL
-- Insert sample patients if they don't exist
INSERT INTO patients (id, tenant_id, fhir_id, family_name, given_name, birth_date, gender, active, created_at, updated_at)
VALUES
    ('550e8400-e29b-41d4-a716-446655440001', '${TENANT_ID}', 'patient-001', 'Doe', 'John', '1959-11-14', 'male', true, NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440002', '${TENANT_ID}', 'patient-002', 'Smith', 'Jane', '1967-03-22', 'female', true, NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440003', '${TENANT_ID}', 'patient-003', 'Johnson', 'Robert', '1953-08-15', 'male', true, NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440004', '${TENANT_ID}', 'patient-004', 'Williams', 'Mary', '1975-12-03', 'female', true, NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440005', '${TENANT_ID}', 'patient-005', 'Brown', 'Michael', '1962-05-28', 'male', true, NOW(), NOW())
ON CONFLICT (fhir_id) DO NOTHING;

SELECT COUNT(*) as patient_count FROM patients WHERE tenant_id = '${TENANT_ID}';
SQL

echo -e "${GREEN}✓ Patients loaded${NC}"
echo ""

# Create sample custom measures
echo -e "${BLUE}Step 3: Creating Custom Quality Measures${NC}"
echo "-------------------------------------------"

# Diabetes HbA1c Control measure
echo "Creating CDC-A1C (Diabetes HbA1c Control) measure..."
curl -s -X POST "$QM_SERVICE/custom-measures" \
  -H "X-Tenant-ID: $TENANT_ID" "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CDC-A1C - Diabetes HbA1c Control",
    "description": "HEDIS measure for diabetes patients with HbA1c <8%",
    "category": "Diabetes",
    "year": 2025
  }' > /dev/null 2>&1 && echo -e "${GREEN}✓ CDC-A1C measure created${NC}" || echo -e "${YELLOW}⚠ Already exists${NC}"

# Hypertension Control measure
echo "Creating CBP (Controlling Blood Pressure) measure..."
curl -s -X POST "$QM_SERVICE/custom-measures" \
  -H "X-Tenant-ID: $TENANT_ID" "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CBP - Controlling Blood Pressure",
    "description": "HEDIS measure for hypertension control <140/90",
    "category": "Hypertension",
    "year": 2025
  }' > /dev/null 2>&1 && echo -e "${GREEN}✓ CBP measure created${NC}" || echo -e "${YELLOW}⚠ Already exists${NC}"

echo ""

# Generate sample evaluation results
echo -e "${BLUE}Step 4: Generating Sample Evaluation Results${NC}"
echo "-------------------------------------------"

# Insert sample results directly into database
docker exec "$POSTGRES_CONTAINER" psql -U healthdata -d healthdata_cql <<SQL
-- Insert sample quality measure results
INSERT INTO quality_measure_results (
    id, tenant_id, measure_id, measure_name, measure_category, measure_year,
    patient_id, calculation_date, numerator_compliant, denominator_elligible,
    compliance_rate, score, created_by, created_at, updated_at, version
)
VALUES
    -- John Doe - Compliant for Diabetes
    (gen_random_uuid(), '${TENANT_ID}', 'CDC-A1C', 'CDC-A1C - Diabetes HbA1c Control', 'Diabetes', 2025,
     'patient-001', NOW(), true, true, 100.0, 1.0, 'demo-loader', NOW(), NOW(), 1),

    -- Jane Smith - Non-compliant for Hypertension
    (gen_random_uuid(), '${TENANT_ID}', 'CBP', 'CBP - Controlling Blood Pressure', 'Hypertension', 2025,
     'patient-002', NOW(), false, true, 0.0, 0.0, 'demo-loader', NOW(), NOW(), 1),

    -- Robert Johnson - Compliant for Diabetes
    (gen_random_uuid(), '${TENANT_ID}', 'CDC-A1C', 'CDC-A1C - Diabetes HbA1c Control', 'Diabetes', 2025,
     'patient-003', NOW(), true, true, 100.0, 1.0, 'demo-loader', NOW(), NOW(), 1),

    -- Robert Johnson - Non-compliant for Hypertension
    (gen_random_uuid(), '${TENANT_ID}', 'CBP', 'CBP - Controlling Blood Pressure', 'Hypertension', 2025,
     'patient-003', NOW(), false, true, 0.0, 0.0, 'demo-loader', NOW(), NOW(), 1),

    -- Mary Williams - Compliant for both
    (gen_random_uuid(), '${TENANT_ID}', 'CDC-A1C', 'CDC-A1C - Diabetes HbA1c Control', 'Diabetes', 2025,
     'patient-004', NOW(), true, true, 100.0, 1.0, 'demo-loader', NOW(), NOW(), 1),
    (gen_random_uuid(), '${TENANT_ID}', 'CBP', 'CBP - Controlling Blood Pressure', 'Hypertension', 2025,
     'patient-004', NOW(), true, true, 100.0, 1.0, 'demo-loader', NOW(), NOW(), 1),

    -- Michael Brown - Not eligible for Diabetes, Non-compliant for Hypertension
    (gen_random_uuid(), '${TENANT_ID}', 'CDC-A1C', 'CDC-A1C - Diabetes HbA1c Control', 'Diabetes', 2025,
     'patient-005', NOW(), false, false, 0.0, 0.0, 'demo-loader', NOW(), NOW(), 1),
    (gen_random_uuid(), '${TENANT_ID}', 'CBP', 'CBP - Controlling Blood Pressure', 'Hypertension', 2025,
     'patient-005', NOW(), false, true, 0.0, 0.0, 'demo-loader', NOW(), NOW(), 1)
ON CONFLICT DO NOTHING;

-- Show results
SELECT
    measure_category,
    COUNT(*) as total_evaluations,
    SUM(CASE WHEN numerator_compliant THEN 1 ELSE 0 END) as compliant,
    SUM(CASE WHEN NOT numerator_compliant AND denominator_elligible THEN 1 ELSE 0 END) as non_compliant,
    SUM(CASE WHEN NOT denominator_elligible THEN 1 ELSE 0 END) as not_eligible,
    ROUND(AVG(CASE WHEN denominator_elligible THEN compliance_rate ELSE NULL END), 1) as avg_compliance
FROM quality_measure_results
WHERE tenant_id = '${TENANT_ID}'
GROUP BY measure_category;
SQL

echo -e "${GREEN}✓ Evaluation results generated${NC}"
echo ""

# Summary
echo "========================================="
echo -e "${GREEN}Demo Data Loaded Successfully!${NC}"
echo "========================================="
echo ""
echo "Next steps:"
echo "1. Open Clinical Portal: http://localhost:4200"
echo "2. Navigate to Dashboard to see stats"
echo "3. Go to Results page to view evaluations"
echo "4. Check Patients page to see loaded patients"
echo ""
echo "Demo Features Available:"
echo "  ✓ 5 sample patients with complex histories"
echo "  ✓ 8 evaluation results (mixed compliance)"
echo "  ✓ 2 quality measures (Diabetes & Hypertension)"
echo "  ✓ Interactive charts and visualizations"
echo "  ✓ Care gap identification"
echo ""
echo "For full demo script, see: DEMO_GUIDE.md"
echo ""
