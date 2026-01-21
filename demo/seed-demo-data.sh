#!/bin/bash
# HDIM Demo Data Seeder
# Seeds the demo environment with 10 realistic patient care gap scenarios
#
# Usage: ./seed-demo-data.sh [--reset]
#   --reset: Clear existing data before seeding
# Environment:
#   TENANT_ID: Tenant ID to seed (default: demo-tenant)
#   FHIR_PATIENT_BASE: FHIR base URL for authoritative Patient storage

set -e

# Configuration
TENANT_ID="${TENANT_ID:-acme-health}"
FHIR_PATIENT_BASE="${FHIR_PATIENT_BASE:-http://localhost:8085/fhir}"
CARE_GAP_BASE="${CARE_GAP_BASE:-http://localhost:8086/care-gap}"
AUTH_USER_ID="${AUTH_USER_ID:-550e8400-e29b-41d4-a716-446655440000}"
AUTH_USERNAME="${AUTH_USERNAME:-demo_user}"
AUTH_ROLES="${AUTH_ROLES:-ADMIN}"
AUTH_VALIDATED="${AUTH_VALIDATED:-gateway-dev}"
TENANT_HEADER="X-Tenant-ID: $TENANT_ID"
AUTH_HEADER_ARGS=(
    -H "X-Auth-User-Id: $AUTH_USER_ID"
    -H "X-Auth-Username: $AUTH_USERNAME"
    -H "X-Auth-Tenant-Ids: $TENANT_ID"
    -H "X-Auth-Roles: $AUTH_ROLES"
    -H "X-Auth-Validated: $AUTH_VALIDATED"
)

# Deterministic patient IDs (UUIDs) for validation and repeatability
PATIENT_001="550e8400-e29b-41d4-a716-446655440001"
PATIENT_002="550e8400-e29b-41d4-a716-446655440002"
PATIENT_003="550e8400-e29b-41d4-a716-446655440003"
PATIENT_004="550e8400-e29b-41d4-a716-446655440004"
PATIENT_005="550e8400-e29b-41d4-a716-446655440005"
PATIENT_006="550e8400-e29b-41d4-a716-446655440006"
PATIENT_007="550e8400-e29b-41d4-a716-446655440007"
PATIENT_008="550e8400-e29b-41d4-a716-446655440008"
PATIENT_009="550e8400-e29b-41d4-a716-446655440009"
PATIENT_010="550e8400-e29b-41d4-a716-446655440010"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   HDIM Care Gap Demo - Data Seeder${NC}"
echo -e "${BLUE}========================================${NC}"

RESET_DEMO=false
for arg in "$@"; do
    case "$arg" in
        --reset)
            RESET_DEMO=true
            ;;
    esac
done

if [ "$RESET_DEMO" = true ]; then
    echo -e "\n${YELLOW}Resetting care gap data before seeding...${NC}"
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    "$SCRIPT_DIR/reset-demo-targeted.sh"
fi

# Check if services are running
check_services() {
    echo -e "\n${YELLOW}Checking service availability...${NC}"

    services=("8080:Gateway" "8084:Patient" "8086:Care-Gap" "8087:Quality-Measure")

    for service in "${services[@]}"; do
        port="${service%%:*}"
        name="${service##*:}"

        if curl -s "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo -e "  ${GREEN}✓${NC} $name service (port $port)"
        else
            echo -e "  ${RED}✗${NC} $name service (port $port) - NOT AVAILABLE"
            echo -e "\n${RED}Error: Services not ready. Please run:${NC}"
            echo -e "  docker compose -f docker-compose.demo.yml up -d"
            echo -e "  # Wait 60 seconds for services to start"
            exit 1
        fi
    done
    if curl -s "$FHIR_PATIENT_BASE/metadata" > /dev/null 2>&1; then
        echo -e "  ${GREEN}✓${NC} FHIR (patient source) at $FHIR_PATIENT_BASE"
    else
        echo -e "  ${RED}✗${NC} FHIR (patient source) at $FHIR_PATIENT_BASE - NOT AVAILABLE"
        echo -e "\n${RED}Error: FHIR server not ready. Please run:${NC}"
        echo -e "  docker compose -f docker-compose.fhir-server.yml up -d"
        exit 1
    fi
    echo -e "${GREEN}All services are running!${NC}"
}

# Wait for services with retry
wait_for_services() {
    echo -e "\n${YELLOW}Waiting for services to be ready (max 120 seconds)...${NC}"

    max_attempts=24
    attempt=0

    while [ $attempt -lt $max_attempts ]; do
        if curl -s "http://localhost:8080/actuator/health" > /dev/null 2>&1 \
            && curl -s "$FHIR_PATIENT_BASE/metadata" > /dev/null 2>&1; then
            echo -e "${GREEN}Services are ready!${NC}"
            return 0
        fi

        attempt=$((attempt + 1))
        echo -e "  Attempt $attempt/$max_attempts - waiting..."
        sleep 5
    done

    echo -e "${RED}Services did not become ready in time.${NC}"
    exit 1
}

# Create a patient via FHIR service
create_patient() {
    local id=$1
    local first_name=$2
    local last_name=$3
    local birth_date=$4
    local gender=$5
    local mrn=$6

    echo -e "  Creating patient: ${BLUE}$first_name $last_name${NC}"

    curl -s -X POST "$FHIR_PATIENT_BASE/Patient" \
        -H "Content-Type: application/fhir+json" \
        "${AUTH_HEADER_ARGS[@]}" \
        -H "$TENANT_HEADER" \
        -d "{
            \"resourceType\": \"Patient\",
            \"id\": \"$id\",
            \"identifier\": [{
                \"system\": \"http://hdim.io/mrn\",
                \"value\": \"$mrn\"
            }],
            \"name\": [{
                \"family\": \"$last_name\",
                \"given\": [\"$first_name\"]
            }],
            \"gender\": \"$gender\",
            \"birthDate\": \"$birth_date\",
            \"address\": [{
                \"use\": \"home\",
                \"city\": \"Boston\",
                \"state\": \"MA\",
                \"postalCode\": \"02101\"
            }],
            \"telecom\": [
                {\"system\": \"phone\", \"value\": \"555-0100\", \"use\": \"home\"},
                {\"system\": \"email\", \"value\": \"${first_name,,}.${last_name,,}@example.com\"}
            ]
        }" > /dev/null 2>&1 || true
}

# Create a care gap
create_care_gap() {
    local patient_id=$1
    local measure_id=$2
    local measure_name=$3
    local priority=$4
    local days_overdue=$5
    local recommendation=$6
    local last_service_date=$7

    echo -e "    - Gap: ${YELLOW}$measure_name${NC} ($priority priority, $days_overdue days overdue)"

    curl -s -X POST "$CARE_GAP_BASE/api/v1/care-gaps" \
        -H "Content-Type: application/json" \
        "${AUTH_HEADER_ARGS[@]}" \
        -H "$TENANT_HEADER" \
        -d "{
            \"patientId\": \"$patient_id\",
            \"tenantId\": \"$TENANT_ID\",
            \"measureId\": \"$measure_id\",
            \"measureName\": \"$measure_name\",
            \"priority\": \"$priority\",
            \"status\": \"OPEN\",
            \"daysOverdue\": $days_overdue,
            \"recommendation\": \"$recommendation\",
            \"lastServiceDate\": \"$last_service_date\",
            \"evidence\": {
                \"source\": \"CQL Evaluation\",
                \"evaluatedAt\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"
            }
        }" > /dev/null 2>&1 || true
}

# Main seeding function
seed_demo_data() {
    echo -e "\n${BLUE}--- Seeding Demo Patients ---${NC}"

    # ============================================
    # PRIMARY PATIENT: Maria Garcia (Hero Patient)
    # ============================================
    create_patient "$PATIENT_001" "Maria" "Garcia" "1967-03-15" "female" "MRN-2024-4521"
    create_care_gap "$PATIENT_001" "COL" "Colorectal Cancer Screening" "HIGH" 127 \
        "Schedule colonoscopy - patient is 57F with family history. Last screening March 2014." \
        "2014-03-20"
    create_care_gap "$PATIENT_001" "BCS" "Breast Cancer Screening" "MEDIUM" 45 \
        "Schedule mammogram - annual screening due" \
        "2023-11-15"

    # ============================================
    # SECONDARY PATIENTS
    # ============================================

    # Patient 2: Robert Johnson - Breast Cancer Screening (male partner scenario for family)
    create_patient "$PATIENT_002" "Sarah" "Johnson" "1960-07-22" "female" "MRN-2024-4522"
    create_care_gap "$PATIENT_002" "BCS" "Breast Cancer Screening" "HIGH" 380 \
        "URGENT: Over 1 year overdue for mammogram. Dense breast tissue noted in prior exam." \
        "2023-01-10"

    # Patient 3: Linda Chen - Diabetes HbA1c
    create_patient "$PATIENT_003" "Linda" "Chen" "1955-11-08" "female" "MRN-2024-4523"
    create_care_gap "$PATIENT_003" "CDC" "Comprehensive Diabetes Care - HbA1c" "HIGH" 95 \
        "Schedule HbA1c test - last result was 8.2%, needs follow-up" \
        "2024-09-28"
    create_care_gap "$PATIENT_003" "EED" "Diabetic Eye Exam" "MEDIUM" 210 \
        "Schedule dilated eye exam - diabetic retinopathy screening due" \
        "2024-06-01"

    # Patient 4: Michael Williams - Statin Therapy
    create_patient "$PATIENT_004" "Michael" "Williams" "1962-04-30" "male" "MRN-2024-4524"
    create_care_gap "$PATIENT_004" "SPC" "Statin Therapy for CVD" "HIGH" 60 \
        "Review statin adherence - PDC below 80%. Consider medication reconciliation." \
        "2024-10-30"

    # Patient 5: Patricia Davis - Cervical Cancer Screening
    create_patient "$PATIENT_005" "Patricia" "Davis" "1978-09-12" "female" "MRN-2024-4525"
    create_care_gap "$PATIENT_005" "CCS" "Cervical Cancer Screening" "MEDIUM" 730 \
        "Schedule Pap test + HPV co-testing - overdue by 2 years" \
        "2022-12-30"

    # Patient 6: James Thompson - Annual Wellness Visit
    create_patient "$PATIENT_006" "James" "Thompson" "1950-02-28" "male" "MRN-2024-4526"
    create_care_gap "$PATIENT_006" "AWV" "Annual Wellness Visit" "MEDIUM" 400 \
        "Schedule Medicare Annual Wellness Visit - cognitive screening due" \
        "2023-11-25"
    create_care_gap "$PATIENT_006" "COL" "Colorectal Cancer Screening" "HIGH" 1825 \
        "CRITICAL: 5 years overdue for colonoscopy. High-risk due to age and family history." \
        "2019-12-30"

    # Patient 7: Barbara Martinez - Osteoporosis Screening
    create_patient "$PATIENT_007" "Barbara" "Martinez" "1958-06-18" "female" "MRN-2024-4527"
    create_care_gap "$PATIENT_007" "OSW" "Osteoporosis Screening" "MEDIUM" 180 \
        "Schedule DEXA scan - postmenopausal female with fracture risk factors" \
        "2024-07-01"

    # Patient 8: William Anderson - Depression Screening Follow-up
    create_patient "$PATIENT_008" "William" "Anderson" "1975-12-05" "male" "MRN-2024-4528"
    create_care_gap "$PATIENT_008" "DSF" "Depression Screening Follow-up" "HIGH" 30 \
        "PHQ-9 follow-up needed - initial screening score was 15 (moderate depression)" \
        "2024-11-30"

    # Patient 9: Susan Taylor - Diabetic Eye Exam
    create_patient "$PATIENT_009" "Susan" "Taylor" "1965-08-20" "female" "MRN-2024-4529"
    create_care_gap "$PATIENT_009" "EED" "Diabetic Eye Exam" "MEDIUM" 365 \
        "Annual diabetic retinopathy screening overdue" \
        "2023-12-30"
    create_care_gap "$PATIENT_009" "KED" "Kidney Health Evaluation for Diabetes" "HIGH" 90 \
        "Schedule uACR and eGFR - diabetic kidney disease screening" \
        "2024-10-01"

    # Patient 10: David Brown - Multiple Gaps (Complex Case)
    create_patient "$PATIENT_010" "David" "Brown" "1952-01-14" "male" "MRN-2024-4530"
    create_care_gap "$PATIENT_010" "KED" "Kidney Health Evaluation for Diabetes" "HIGH" 120 \
        "URGENT: CKD Stage 3 - needs nephrology referral and uACR monitoring" \
        "2024-09-01"
    create_care_gap "$PATIENT_010" "SPC" "Statin Therapy for CVD" "HIGH" 45 \
        "Statin adherence issue - 3 missed refills in past 6 months" \
        "2024-11-15"
    create_care_gap "$PATIENT_010" "BPD" "Controlling High Blood Pressure" "HIGH" 14 \
        "BP reading 158/95 at last visit - needs follow-up" \
        "2024-12-17"
}

# Create summary statistics
create_summary() {
    echo -e "\n${BLUE}--- Demo Data Summary ---${NC}"
    echo -e "  ${GREEN}✓${NC} 10 patients created"
    echo -e "  ${GREEN}✓${NC} 16 care gaps across 11 HEDIS measures"
    echo -e ""
    echo -e "  ${YELLOW}Gap Priority Distribution:${NC}"
    echo -e "    HIGH:   10 gaps (55%)"
    echo -e "    MEDIUM: 6 gaps (38%)"
    echo -e ""
    echo -e "  ${YELLOW}HEDIS Measures Represented:${NC}"
    echo -e "    COL - Colorectal Cancer Screening (2)"
    echo -e "    BCS - Breast Cancer Screening (2)"
    echo -e "    CDC - Comprehensive Diabetes Care (1)"
    echo -e "    CCS - Cervical Cancer Screening (1)"
    echo -e "    SPC - Statin Therapy for CVD (2)"
    echo -e "    AWV - Annual Wellness Visit (1)"
    echo -e "    OSW - Osteoporosis Screening (1)"
    echo -e "    DSF - Depression Screening Follow-up (1)"
    echo -e "    EED - Diabetic Eye Exam (2)"
    echo -e "    KED - Kidney Health Evaluation (2)"
    echo -e "    BPD - Controlling High Blood Pressure (1)"
}

# Main execution
main() {
    if [ "$1" == "--wait" ]; then
        wait_for_services
    else
        check_services
    fi

    seed_demo_data
    create_summary

    echo -e "\n${GREEN}========================================${NC}"
    echo -e "${GREEN}   Demo data seeded successfully!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo -e ""
    echo -e "Access the demo at: ${BLUE}http://localhost:4200${NC}"
    echo -e ""
    echo -e "Demo credentials:"
    echo -e "  Username: ${YELLOW}demo_admin${NC}"
    echo -e "  Password: ${YELLOW}demo123${NC}"
    echo -e ""
    echo -e "Hero patient for walkthrough: ${BLUE}Maria Garcia${NC} (MRN: MRN-2024-4521)"
}

main "$@"
