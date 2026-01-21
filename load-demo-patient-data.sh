#!/bin/bash

################################################################################
# Load Demo Patient Data
# Seeds patient-service demographics using existing FHIR patient IDs
################################################################################

set -e

DB_CONTAINER="${DB_CONTAINER:-hdim-demo-postgres}"
DB_NAME="${DB_NAME:-patient_db}"
DB_USER="${DB_USER:-healthdata}"
FHIR_URL="${FHIR_URL:-http://localhost:8085/fhir}"
TENANT_ID="${TENANT_ID:-acme-health}"
PATIENT_COUNT="${PATIENT_COUNT:-25}"
AUTH_USER_ID="${AUTH_USER_ID:-550e8400-e29b-41d4-a716-446655440010}"
AUTH_USERNAME="${AUTH_USERNAME:-demo_admin@hdim.ai}"
AUTH_ROLES="${AUTH_ROLES:-ADMIN,EVALUATOR}"
USE_TRUSTED_HEADERS="${USE_TRUSTED_HEADERS:-true}"

AUTH_HEADER=()
if [ "$USE_TRUSTED_HEADERS" = "true" ]; then
    VALIDATED_TS=$(date +%s)
    AUTH_HEADER=(
        -H "X-Auth-User-Id: $AUTH_USER_ID"
        -H "X-Auth-Username: $AUTH_USERNAME"
        -H "X-Auth-Roles: $AUTH_ROLES"
        -H "X-Auth-Tenant-Ids: $TENANT_ID"
        -H "X-Auth-Validated: gateway-${VALIDATED_TS}-dev"
        -H "X-Tenant-ID: $TENANT_ID"
    )
fi

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║         Loading Demo Patient Demographics                     ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

mapfile -t PATIENT_IDS < <(curl -s "${AUTH_HEADER[@]}" \
    "${FHIR_URL}/Patient?_count=${PATIENT_COUNT}" | jq -r '.entry[].resource.id')

if [ ${#PATIENT_IDS[@]} -eq 0 ]; then
    echo "✗ No FHIR patients found to seed patient demographics."
    exit 1
fi

TEMP_SQL="/tmp/demo-patient-demographics.sql"
{
    echo "BEGIN;"
    index=1
    for patient_id in "${PATIENT_IDS[@]}"; do
        first_name="Demo"
        last_name="Patient${index}"
        mrn="MRN-${index}"
        echo "INSERT INTO patient_demographics ("
        echo "  id, tenant_id, fhir_patient_id, mrn, first_name, last_name,"
        echo "  date_of_birth, gender, created_at, updated_at"
        echo ") VALUES ("
        echo "  gen_random_uuid(), '${TENANT_ID}', '${patient_id}', '${mrn}',"
        echo "  '${first_name}', '${last_name}', '1980-01-01', 'unknown', NOW(), NOW()"
        echo ") ON CONFLICT (fhir_patient_id) DO NOTHING;"
        index=$((index + 1))
    done
    echo "COMMIT;"
} > "$TEMP_SQL"

echo "Seeding patient demographics into ${DB_NAME}..."
docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < "$TEMP_SQL" > /dev/null

echo "✓ Seeded ${#PATIENT_IDS[@]} patient demographics for tenant ${TENANT_ID}"
