#!/bin/bash

# Comprehensive Demo Data Seeding Script
# Seeds all required data for service validation

set -uo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

DEMO_SEEDING_URL="${DEMO_SEEDING_URL:-http://localhost:8098}"
TENANT_ID="${TENANT_ID:-acme-health}"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Demo Data Seeding Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if demo-seeding-service is available
echo -e "${CYAN}Checking demo-seeding-service availability...${NC}"
if ! curl -sf "$DEMO_SEEDING_URL/demo/actuator/health" > /dev/null 2>&1; then
    echo -e "${RED}✗ Demo seeding service is not available at $DEMO_SEEDING_URL${NC}"
    echo -e "${YELLOW}Please start the demo-seeding-service first:${NC}"
    echo "  docker compose up -d demo-seeding-service"
    exit 1
fi
echo -e "${GREEN}✓ Demo seeding service is available${NC}"
echo ""

# Function to seed data
seed_data() {
    local name=$1
    local endpoint=$2
    local data=$3
    
    echo -n "Seeding: $name ... "
    
    response=$(curl -s -w "\n%{http_code}" -X POST "$endpoint" \
        -H "X-Tenant-ID: $TENANT_ID" \
        -H "Content-Type: application/json" \
        -d "$data" \
        2>&1)
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        echo -e "${GREEN}✓${NC} (HTTP $http_code)"
        
        # Try to extract useful info from response
        if command -v jq &> /dev/null; then
            if echo "$body" | jq -e '.patientsCreated' &> /dev/null; then
                count=$(echo "$body" | jq -r '.patientsCreated // 0')
                echo "  → Created $count patients"
            elif echo "$body" | jq -e '.patientCount' &> /dev/null; then
                count=$(echo "$body" | jq -r '.patientCount // 0')
                echo "  → Loaded $count patients"
            fi
        fi
        return 0
    else
        echo -e "${RED}✗${NC} (HTTP $http_code)"
        echo "  Error: $body"
        return 1
    fi
}

# Seed data using scenarios
echo -e "${CYAN}=== Seeding Data Using Scenarios ===${NC}"
echo ""

# Option 1: HEDIS Evaluation Scenario (recommended)
echo -e "${YELLOW}Loading HEDIS Evaluation Scenario...${NC}"
echo "  This scenario includes:"
echo "  - 5,000 synthetic patients"
echo "  - HEDIS quality measure evaluations"
echo "  - Care gaps for 30% of patients"
echo ""

seed_data \
    "HEDIS Evaluation Scenario" \
    "$DEMO_SEEDING_URL/demo/api/v1/demo/scenarios/hedis-evaluation" \
    "{}"

echo ""

# Option 2: Patient Journey Scenario (if needed)
read -p "Load Patient Journey Scenario (1,000 patients)? [y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    seed_data \
        "Patient Journey Scenario" \
        "$DEMO_SEEDING_URL/demo/api/v1/demo/scenarios/patient-journey" \
        "{}"
    echo ""
fi

# Option 3: Risk Stratification Scenario (if needed)
read -p "Load Risk Stratification Scenario (10,000 patients)? [y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    seed_data \
        "Risk Stratification Scenario" \
        "$DEMO_SEEDING_URL/demo/api/v1/demo/scenarios/risk-stratification" \
        "{}"
    echo ""
fi

# Alternative: Direct seeding
echo -e "${CYAN}=== Alternative: Direct Seeding ===${NC}"
echo ""
read -p "Seed additional data directly (100 patients, 30% care gaps)? [y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    seed_data \
        "Direct Patient Seeding" \
        "$DEMO_SEEDING_URL/demo/api/v1/demo/seed" \
        '{"count": 100, "careGapPercentage": 30}'
    echo ""
fi

# Summary
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Seeding Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${GREEN}✓ Data seeding completed${NC}"
echo ""
echo -e "${CYAN}Next Steps:${NC}"
echo "  1. Wait 30-60 seconds for data to propagate"
echo "  2. Run validation: ./scripts/validate-all-services-data.sh"
echo "  3. Test services in the clinical portal"
echo ""
