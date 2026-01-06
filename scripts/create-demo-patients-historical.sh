#!/bin/bash
#
# Create Demo Patients with Historical Evaluations
#
# This script creates 5 demo patients with comprehensive FHIR data
# showing care gap detection, intervention, and closure stories.
#
# Patients:
#   1. Maria Garcia     - Colonoscopy gap (OPEN for live demo)
#   2. Robert Chen      - Diabetes care gaps (MIXED - some open, some closed)
#   3. Angela Williams  - Breast cancer screening (CLOSED - success story)
#   4. James Thompson   - Cardiovascular gaps (OPEN for live demo)
#   5. Patricia Davis   - Preventive care (CLOSED - success story)
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
FHIR_RESOURCES="$PROJECT_ROOT/demo/fhir-resources"

# Configuration
FHIR_SERVICE_URL="${FHIR_SERVICE_URL:-http://localhost:8085/fhir}"
TENANT_ID="${TENANT_ID:-DEMO_TENANT}"

# Common headers for all requests
COMMON_HEADERS=(
  -H "Content-Type: application/fhir+json"
  -H "X-Tenant-ID: $TENANT_ID"
  -H "X-Auth-User-Id: 00000000-0000-0000-0000-000000000001"
  -H "X-Auth-Username: demo-admin"
  -H "X-Auth-Tenant-Ids: $TENANT_ID"
  -H "X-Auth-Roles: ADMIN"
  -H "X-Auth-Validated: gateway-dev-mode"
)

echo "=============================================="
echo "Creating Demo Patients with Historical Data"
echo "=============================================="
echo "FHIR Service: $FHIR_SERVICE_URL"
echo "Tenant ID: $TENANT_ID"
echo ""

# Function to create a FHIR resource
create_resource() {
  local resource_type=$1
  local file_path=$2
  local description=$3

  echo -n "Creating $description... "

  response=$(curl -s -w "\n%{http_code}" -X POST \
    "$FHIR_SERVICE_URL/$resource_type" \
    "${COMMON_HEADERS[@]}" \
    -d @"$file_path")

  http_code=$(echo "$response" | tail -n1)
  body=$(echo "$response" | sed '$d')

  if [[ "$http_code" == "201" ]] || [[ "$http_code" == "200" ]]; then
    echo "OK"
    return 0
  else
    echo "FAILED (HTTP $http_code)"
    echo "Response: $body"
    return 1
  fi
}

# Function to create resources from a bundle
create_bundle() {
  local file_path=$1
  local description=$2

  echo -n "Creating $description (bundle)... "

  # Extract each resource from the bundle and create individually
  # Using jq if available, otherwise skip
  if command -v jq &> /dev/null; then
    count=$(jq '.entry | length' "$file_path")
    for i in $(seq 0 $((count - 1))); do
      resource=$(jq ".entry[$i].resource" "$file_path")
      resource_type=$(echo "$resource" | jq -r '.resourceType')

      response=$(curl -s -w "\n%{http_code}" -X POST \
        "$FHIR_SERVICE_URL/$resource_type" \
        "${COMMON_HEADERS[@]}" \
        -d "$resource")

      http_code=$(echo "$response" | tail -n1)
      if [[ "$http_code" != "201" ]] && [[ "$http_code" != "200" ]]; then
        echo "FAILED creating $resource_type (HTTP $http_code)"
      fi
    done
    echo "OK ($count resources)"
  else
    echo "SKIPPED (jq not installed)"
  fi
}

# Check FHIR service is available
echo "Checking FHIR service availability..."
if ! curl -s -f "$FHIR_SERVICE_URL/metadata" > /dev/null 2>&1; then
  echo "WARNING: FHIR service may not be available at $FHIR_SERVICE_URL"
  echo "Continuing anyway..."
fi
echo ""

# ==========================================
# Patient 1: Maria Garcia (Colonoscopy)
# ==========================================
echo "--- Maria Garcia (Colonoscopy Gap - OPEN) ---"
create_resource "Patient" "$FHIR_RESOURCES/maria-garcia/patient.json" "Patient: Maria Garcia"
create_resource "Procedure" "$FHIR_RESOURCES/maria-garcia/procedure-colonoscopy-2019.json" "2019 Colonoscopy (last compliant)"
echo ""

# ==========================================
# Patient 2: Robert Chen (Diabetes)
# ==========================================
echo "--- Robert Chen (Diabetes Gaps - MIXED) ---"
create_resource "Patient" "$FHIR_RESOURCES/robert-chen/patient.json" "Patient: Robert Chen"
create_resource "Condition" "$FHIR_RESOURCES/robert-chen/condition-diabetes.json" "Diabetes condition"
create_bundle "$FHIR_RESOURCES/robert-chen/observations-hba1c.json" "HbA1c observations (4 entries)"
create_bundle "$FHIR_RESOURCES/robert-chen/procedure-eye-exam.json" "Eye exams (2 entries)"
echo ""

# ==========================================
# Patient 3: Angela Williams (Breast Cancer)
# ==========================================
echo "--- Angela Williams (BCS Gap - CLOSED, Success Story) ---"
create_resource "Patient" "$FHIR_RESOURCES/angela-williams/patient.json" "Patient: Angela Williams"
create_bundle "$FHIR_RESOURCES/angela-williams/procedures-mammogram.json" "Mammograms and follow-up (4 entries)"
echo ""

# ==========================================
# Patient 4: James Thompson (Cardiovascular)
# ==========================================
echo "--- James Thompson (CBP/SPC Gaps - OPEN) ---"
create_resource "Patient" "$FHIR_RESOURCES/james-thompson/patient.json" "Patient: James Thompson"
create_bundle "$FHIR_RESOURCES/james-thompson/conditions.json" "HTN and Hyperlipidemia conditions"
create_bundle "$FHIR_RESOURCES/james-thompson/observations-bp.json" "Blood pressure observations (3 entries)"
echo ""

# ==========================================
# Patient 5: Patricia Davis (Preventive Care)
# ==========================================
echo "--- Patricia Davis (AWC/FLU Gaps - CLOSED, Success Story) ---"
create_resource "Patient" "$FHIR_RESOURCES/patricia-davis/patient.json" "Patient: Patricia Davis"
create_bundle "$FHIR_RESOURCES/patricia-davis/encounters-wellness.json" "Wellness encounters (2 entries)"
create_resource "Immunization" "$FHIR_RESOURCES/patricia-davis/immunization-flu.json" "Flu vaccination"
echo ""

echo "=============================================="
echo "Demo Patient Creation Complete!"
echo "=============================================="
echo ""
echo "Summary:"
echo "  - Maria Garcia:    COL gap OPEN (for live demo)"
echo "  - Robert Chen:     3 diabetes gaps (1 OPEN, 2 CLOSED)"
echo "  - Angela Williams: BCS gap CLOSED (early cancer detection success)"
echo "  - James Thompson:  CBP/SPC gaps OPEN (for live demo)"
echo "  - Patricia Davis:  AWC/FLU gaps CLOSED (wellness success)"
echo ""
echo "Next steps:"
echo "  1. Log into Clinical Portal at http://localhost:4200"
echo "  2. Navigate to Patients page to see new patients"
echo "  3. Navigate to Care Gaps to see gap status"
echo "  4. Use Evaluations page to run measures on patients"
echo ""
