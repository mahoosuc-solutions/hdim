#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
CQL_ENGINE_URL="http://localhost:8081/cql-engine/api/v1/cql/libraries"
FHIR_SERVER_URL="http://localhost:8083/fhir"
CQL_USERNAME="cql-service-user"
CQL_PASSWORD="cql-service-dev-password-change-in-prod"
TENANT_ID="default"

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Counters
MEASURES_SUCCESS=0
MEASURES_FAILED=0
PATIENTS_SUCCESS=0
PATIENTS_FAILED=0

echo "========================================="
echo "Loading Sample Data into Clinical Portal"
echo "========================================="
echo ""

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo -e "${RED}Error: jq is not installed. Please install jq to continue.${NC}"
    exit 1
fi

# Check if files exist
if [ ! -f "$SCRIPT_DIR/hedis-measures.json" ]; then
    echo -e "${RED}Error: hedis-measures.json not found${NC}"
    exit 1
fi

if [ ! -f "$SCRIPT_DIR/sample-patients.json" ]; then
    echo -e "${RED}Error: sample-patients.json not found${NC}"
    exit 1
fi

# Load HEDIS measures
echo -e "${YELLOW}Loading HEDIS measures...${NC}"
echo "-------------------------------------"

while IFS= read -r measure; do
    NAME=$(echo "$measure" | jq -r '.name')
    VERSION=$(echo "$measure" | jq -r '.version')

    echo -n "Loading $NAME v$VERSION... "

    RESPONSE=$(curl -s -w "\n%{http_code}" \
        -u "$CQL_USERNAME:$CQL_PASSWORD" \
        -X POST \
        -H "X-Tenant-ID: $TENANT_ID" \
        -H "Content-Type: application/json" \
        -d "$measure" \
        "$CQL_ENGINE_URL")

    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
        echo -e "${GREEN}SUCCESS${NC}"
        ((MEASURES_SUCCESS++))
    else
        echo -e "${RED}FAILED (HTTP $HTTP_CODE)${NC}"
        echo "  Response: $BODY"
        ((MEASURES_FAILED++))
    fi
done < <(jq -c '.[]' "$SCRIPT_DIR/hedis-measures.json")

echo ""
echo -e "Measures Loaded: ${GREEN}$MEASURES_SUCCESS${NC}"
echo -e "Measures Failed: ${RED}$MEASURES_FAILED${NC}"
echo ""

# Load sample patients
echo -e "${YELLOW}Loading sample patients...${NC}"
echo "-------------------------------------"

while IFS= read -r patient; do
    IDENTIFIER=$(echo "$patient" | jq -r '.identifier[0].value')
    NAME=$(echo "$patient" | jq -r '.name[0].given[0] + " " + .name[0].family')

    echo -n "Loading $NAME ($IDENTIFIER)... "

    RESPONSE=$(curl -s -w "\n%{http_code}" \
        -X POST \
        -H "Content-Type: application/fhir+json" \
        -d "$patient" \
        "$FHIR_SERVER_URL/Patient")

    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
        echo -e "${GREEN}SUCCESS${NC}"
        ((PATIENTS_SUCCESS++))
    else
        echo -e "${RED}FAILED (HTTP $HTTP_CODE)${NC}"
        echo "  Response: $BODY"
        ((PATIENTS_FAILED++))
    fi
done < <(jq -c '.entry[].resource' "$SCRIPT_DIR/sample-patients.json")

echo ""
echo -e "Patients Loaded: ${GREEN}$PATIENTS_SUCCESS${NC}"
echo -e "Patients Failed: ${RED}$PATIENTS_FAILED${NC}"
echo ""

# Summary
echo "========================================="
echo "Summary"
echo "========================================="
echo -e "Total Measures Loaded: ${GREEN}$MEASURES_SUCCESS${NC} / $(($MEASURES_SUCCESS + $MEASURES_FAILED))"
echo -e "Total Patients Loaded: ${GREEN}$PATIENTS_SUCCESS${NC} / $(($PATIENTS_SUCCESS + $PATIENTS_FAILED))"
echo ""

if [ $MEASURES_FAILED -eq 0 ] && [ $PATIENTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}All data loaded successfully!${NC}"
    exit 0
else
    echo -e "${RED}Some data failed to load. Please check the errors above.${NC}"
    exit 1
fi
