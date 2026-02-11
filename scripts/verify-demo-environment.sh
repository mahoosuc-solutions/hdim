#!/bin/bash

#######################################################################
# HDIM Demo Environment Verification Script
#
# Validates that the demo environment is properly set up and services
# can communicate correctly for the Quality Measures → Care Gaps → Risk flow.
#
# Usage:
#   ./scripts/verify-demo-environment.sh [tenant-id]
#
# Prerequisites:
#   - docker compose running with demo services
#   - curl and jq installed
#
# Services tested:
#   - Patient Service (8084)
#   - FHIR Service (8085)
#   - Care Gap Service (8086)
#   - Quality Measure Service (8087)
#   - HCC Service (8105)
#######################################################################

set -e

# Configuration
TENANT_ID="${1:-acme-health}"
USER_ID="00000000-0000-0000-0000-000000000001"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Service URLs
PATIENT_SERVICE="http://localhost:8084/patient"
FHIR_SERVICE="http://localhost:8085/fhir"
CARE_GAP_SERVICE="http://localhost:8086/care-gap"
QUALITY_MEASURE_SERVICE="http://localhost:8087/quality-measure"
HCC_SERVICE="http://localhost:8105/hcc"
DEMO_SEEDING_SERVICE="http://localhost:8103/demo"

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_SKIPPED=0

# Print functions
print_header() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
}

print_test() {
    echo -e "\n${YELLOW}▶ $1${NC}"
}

print_pass() {
    echo -e "  ${GREEN}✓ PASS:${NC} $1"
    ((TESTS_PASSED++))
}

print_fail() {
    echo -e "  ${RED}✗ FAIL:${NC} $1"
    ((TESTS_FAILED++))
}

print_skip() {
    echo -e "  ${YELLOW}○ SKIP:${NC} $1"
    ((TESTS_SKIPPED++))
}

print_info() {
    echo -e "  ${BLUE}ℹ INFO:${NC} $1"
}

# Create authentication headers
create_headers() {
    echo "-H 'Content-Type: application/json' \
          -H 'X-Tenant-ID: ${TENANT_ID}' \
          -H 'X-Auth-User-Id: ${USER_ID}' \
          -H 'X-Auth-Username: verify-script' \
          -H 'X-Auth-Tenant-Ids: ${TENANT_ID}' \
          -H 'X-Auth-Roles: ADMIN,EVALUATOR' \
          -H 'X-Auth-Validated: gateway-verify-script'"
}

# Check if service is healthy
check_service_health() {
    local service_name=$1
    local health_url=$2

    if curl -sf --max-time 5 "${health_url}" > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

#######################################################################
# Main Verification
#######################################################################

print_header "HDIM Demo Environment Verification"
echo "Tenant: ${TENANT_ID}"
echo "Timestamp: $(date)"

#######################################################################
# Phase 1: Service Health Checks
#######################################################################

print_header "Phase 1: Service Health Checks"

declare -A SERVICES=(
    ["Patient Service"]="${PATIENT_SERVICE}/actuator/health"
    ["FHIR Service"]="${FHIR_SERVICE}/actuator/health"
    ["Care Gap Service"]="${CARE_GAP_SERVICE}/actuator/health"
    ["Quality Measure Service"]="${QUALITY_MEASURE_SERVICE}/actuator/health"
    ["HCC Service"]="${HCC_SERVICE}/actuator/health"
)

declare -A SERVICE_STATUS

for service in "${!SERVICES[@]}"; do
    print_test "Checking ${service}"
    if check_service_health "${service}" "${SERVICES[$service]}"; then
        print_pass "${service} is healthy"
        SERVICE_STATUS["${service}"]=1
    else
        print_fail "${service} is not responding"
        SERVICE_STATUS["${service}"]=0
    fi
done

#######################################################################
# Phase 2: Patient Data Verification
#######################################################################

print_header "Phase 2: Patient Data Verification"

if [[ "${SERVICE_STATUS["Patient Service"]}" == "1" ]]; then
    print_test "Counting patients in tenant"

    HEADERS=$(create_headers)
    PATIENT_RESPONSE=$(eval "curl -sf --max-time 10 ${HEADERS} '${PATIENT_SERVICE}/api/v1/patients?page=0&size=1'" 2>/dev/null || echo "{}")

    if echo "${PATIENT_RESPONSE}" | jq -e '.totalElements' > /dev/null 2>&1; then
        PATIENT_COUNT=$(echo "${PATIENT_RESPONSE}" | jq '.totalElements')
        print_pass "Found ${PATIENT_COUNT} patients"

        if [[ "${PATIENT_COUNT}" -gt 0 ]]; then
            # Get sample patient
            SAMPLE_PATIENT=$(echo "${PATIENT_RESPONSE}" | jq -r '.content[0].id' 2>/dev/null)
            if [[ -n "${SAMPLE_PATIENT}" && "${SAMPLE_PATIENT}" != "null" ]]; then
                print_info "Sample patient ID: ${SAMPLE_PATIENT}"
            fi
        else
            print_info "No patients found. Run demo seeding first."
        fi
    else
        print_fail "Could not parse patient response"
    fi
else
    print_skip "Patient Service not available"
fi

#######################################################################
# Phase 3: FHIR Resource Verification
#######################################################################

print_header "Phase 3: FHIR Resource Verification"

if [[ "${SERVICE_STATUS["FHIR Service"]}" == "1" ]]; then
    print_test "Counting FHIR Conditions"

    HEADERS=$(create_headers)
    CONDITION_RESPONSE=$(eval "curl -sf --max-time 10 ${HEADERS} '${FHIR_SERVICE}/Condition?_summary=count'" 2>/dev/null || echo "{}")

    if echo "${CONDITION_RESPONSE}" | jq -e '.total' > /dev/null 2>&1; then
        CONDITION_COUNT=$(echo "${CONDITION_RESPONSE}" | jq '.total')
        print_pass "Found ${CONDITION_COUNT} Condition resources"
    else
        print_info "Could not count Conditions (may need FHIR auth)"
    fi

    print_test "Counting FHIR Observations"
    OBSERVATION_RESPONSE=$(eval "curl -sf --max-time 10 ${HEADERS} '${FHIR_SERVICE}/Observation?_summary=count'" 2>/dev/null || echo "{}")

    if echo "${OBSERVATION_RESPONSE}" | jq -e '.total' > /dev/null 2>&1; then
        OBSERVATION_COUNT=$(echo "${OBSERVATION_RESPONSE}" | jq '.total')
        print_pass "Found ${OBSERVATION_COUNT} Observation resources"
    else
        print_info "Could not count Observations"
    fi
else
    print_skip "FHIR Service not available"
fi

#######################################################################
# Phase 4: Care Gap Verification
#######################################################################

print_header "Phase 4: Care Gap Verification"

if [[ "${SERVICE_STATUS["Care Gap Service"]}" == "1" ]]; then
    print_test "Counting care gaps"

    HEADERS=$(create_headers)
    CARE_GAP_RESPONSE=$(eval "curl -sf --max-time 10 ${HEADERS} '${CARE_GAP_SERVICE}/api/v1/care-gaps?page=0&size=1'" 2>/dev/null || echo "{}")

    if echo "${CARE_GAP_RESPONSE}" | jq -e '.totalElements' > /dev/null 2>&1; then
        CARE_GAP_COUNT=$(echo "${CARE_GAP_RESPONSE}" | jq '.totalElements')
        print_pass "Found ${CARE_GAP_COUNT} care gaps"

        if [[ "${CARE_GAP_COUNT}" -gt 0 ]]; then
            # Analyze sample gap
            SAMPLE_GAP=$(echo "${CARE_GAP_RESPONSE}" | jq -r '.content[0]' 2>/dev/null)
            if [[ -n "${SAMPLE_GAP}" ]]; then
                GAP_TYPE=$(echo "${SAMPLE_GAP}" | jq -r '.gapType // .measureId // "unknown"')
                GAP_PRIORITY=$(echo "${SAMPLE_GAP}" | jq -r '.priority // "unknown"')
                print_info "Sample gap: ${GAP_TYPE} (${GAP_PRIORITY} priority)"
            fi
        fi
    else
        print_fail "Could not parse care gap response"
    fi
else
    print_skip "Care Gap Service not available"
fi

#######################################################################
# Phase 5: HCC Service Verification
#######################################################################

print_header "Phase 5: HCC Risk Adjustment Verification"

if [[ "${SERVICE_STATUS["HCC Service"]}" == "1" ]]; then
    print_test "Testing HCC crosswalk API"

    # Test ICD-10 to HCC mapping
    CROSSWALK_RESPONSE=$(curl -sf --max-time 10 \
        "${HCC_SERVICE}/api/v1/hcc/crosswalk?icd10Codes=E1010,E1011,I10,J449" 2>/dev/null || echo "[]")

    if echo "${CROSSWALK_RESPONSE}" | jq -e '.[0]' > /dev/null 2>&1; then
        MAPPING_COUNT=$(echo "${CROSSWALK_RESPONSE}" | jq 'length')
        print_pass "Crosswalk returned ${MAPPING_COUNT} ICD-10 → HCC mappings"

        # Show sample mapping
        SAMPLE_MAPPING=$(echo "${CROSSWALK_RESPONSE}" | jq -r '.[0] | "\(.icd10Code) → V24:\(.hccCodeV24 // "none"), V28:\(.hccCodeV28 // "none")"' 2>/dev/null)
        print_info "Sample: ${SAMPLE_MAPPING}"
    else
        print_fail "Crosswalk API returned empty or invalid response"
    fi

    print_test "Testing HCC opportunities endpoint"
    HEADERS=$(create_headers)
    OPPORTUNITIES_RESPONSE=$(eval "curl -sf --max-time 10 ${HEADERS} '${HCC_SERVICE}/api/v1/hcc/opportunities?minUplift=0.1&limit=5'" 2>/dev/null || echo "[]")

    if echo "${OPPORTUNITIES_RESPONSE}" | jq -e '.' > /dev/null 2>&1; then
        OPP_COUNT=$(echo "${OPPORTUNITIES_RESPONSE}" | jq 'if type == "array" then length else 0 end')
        print_pass "Found ${OPP_COUNT} high-value RAF opportunities"
    else
        print_info "Could not retrieve opportunities (may need data)"
    fi
else
    print_skip "HCC Service not available"
fi

#######################################################################
# Phase 6: Integration Verification
#######################################################################

print_header "Phase 6: Cross-Service Integration"

# Test complete flow: Patient → Conditions → HCC
if [[ "${SERVICE_STATUS["Patient Service"]}" == "1" ]] && \
   [[ "${SERVICE_STATUS["FHIR Service"]}" == "1" ]] && \
   [[ "${SERVICE_STATUS["HCC Service"]}" == "1" ]] && \
   [[ -n "${SAMPLE_PATIENT}" ]] && \
   [[ "${SAMPLE_PATIENT}" != "null" ]]; then

    print_test "Testing integrated Patient → FHIR → HCC flow"

    # Get patient conditions from FHIR
    HEADERS=$(create_headers)
    PATIENT_CONDITIONS=$(eval "curl -sf --max-time 10 ${HEADERS} '${FHIR_SERVICE}/Condition?patient=${SAMPLE_PATIENT}'" 2>/dev/null || echo "{}")

    if echo "${PATIENT_CONDITIONS}" | jq -e '.entry' > /dev/null 2>&1; then
        CONDITION_COUNT=$(echo "${PATIENT_CONDITIONS}" | jq '.entry | length')
        print_pass "Patient ${SAMPLE_PATIENT} has ${CONDITION_COUNT} conditions in FHIR"

        # Extract ICD-10 codes
        ICD10_CODES=$(echo "${PATIENT_CONDITIONS}" | jq -r '[.entry[].resource.code.coding[] | select(.system | contains("icd")) | .code] | unique | join(",")' 2>/dev/null)

        if [[ -n "${ICD10_CODES}" ]]; then
            print_info "ICD-10 codes: ${ICD10_CODES}"
        fi
    else
        print_info "Could not retrieve patient conditions"
    fi
else
    print_skip "Cannot test integration (missing services or patient)"
fi

#######################################################################
# Summary
#######################################################################

print_header "Verification Summary"

TOTAL_TESTS=$((TESTS_PASSED + TESTS_FAILED + TESTS_SKIPPED))

echo ""
echo -e "  ${GREEN}Passed:${NC}  ${TESTS_PASSED}"
echo -e "  ${RED}Failed:${NC}  ${TESTS_FAILED}"
echo -e "  ${YELLOW}Skipped:${NC} ${TESTS_SKIPPED}"
echo -e "  ─────────────"
echo -e "  Total:   ${TOTAL_TESTS}"
echo ""

if [[ ${TESTS_FAILED} -eq 0 ]]; then
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  ✓ Demo environment verification PASSED${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
    exit 0
else
    echo -e "${RED}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${RED}  ✗ Demo environment verification FAILED${NC}"
    echo -e "${RED}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
    echo "To seed demo data, run:"
    echo "  curl -X POST 'http://localhost:8103/demo/api/v1/demo/scenarios/hedis-evaluation' -H 'X-Tenant-ID: ${TENANT_ID}'"
    exit 1
fi
