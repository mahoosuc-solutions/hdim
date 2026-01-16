#!/bin/bash

################################################################################
# Demo Accounts Test Script
# Purpose: Verify all demo accounts can log in successfully
################################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
PASSWORD="${PASSWORD:-demo123}"

echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║              Testing Demo Mode Accounts                       ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Array of demo accounts
declare -a ACCOUNTS=(
    "demo.doctor:EVALUATOR:Dr. Sarah Chen"
    "demo.analyst:ANALYST:Michael Rodriguez"
    "demo.care:EVALUATOR:Jennifer Thompson"
    "demo.admin:ADMIN:David Johnson"
    "demo.viewer:VIEWER:Emily Martinez"
)

PASSED=0
FAILED=0

for account in "${ACCOUNTS[@]}"; do
    IFS=':' read -r username expected_role name <<< "$account"
    
    echo -e "${BLUE}Testing: ${name} (${username})${NC}"
    
    # Attempt login
    response=$(curl -s -X POST "${GATEWAY_URL}/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"${username}\",\"password\":\"${PASSWORD}\"}" \
        2>/dev/null || echo '{"error":"connection_failed"}')
    
    # Check if login was successful
    if echo "$response" | jq -e '.accessToken' > /dev/null 2>&1; then
        token=$(echo "$response" | jq -r '.accessToken')
        user_info=$(echo "$response" | jq -r '.username + " | Roles: " + (.roles | join(", ")) + " | Tenants: " + (.tenantIds | join(", "))')
        roles=$(echo "$response" | jq -r '.roles[]')
        
        # Check if expected role is present
        if echo "$roles" | grep -q "$expected_role"; then
            echo -e "${GREEN}  ✓ Login successful${NC}"
            echo -e "    ${user_info}"
            echo -e "    Token: ${token:0:50}..."
            PASSED=$((PASSED + 1))
        else
            echo -e "${RED}  ✗ Login successful but role mismatch${NC}"
            echo -e "    Expected: $expected_role"
            echo -e "    Got: $roles"
            FAILED=$((FAILED + 1))
        fi
    else
        echo -e "${RED}  ✗ Login failed${NC}"
        echo -e "    Response: $response"
        FAILED=$((FAILED + 1))
    fi
    
    echo ""
done

echo -e "${CYAN}════════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}Test Summary:${NC}"
echo -e "  ${GREEN}Passed: ${PASSED}${NC}"
echo -e "  ${RED}Failed: ${FAILED}${NC}"
echo -e "${CYAN}════════════════════════════════════════════════════════════════${NC}"

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All demo accounts working! Ready for video recording! 🎬${NC}"
    exit 0
else
    echo -e "${RED}✗ Some demo accounts failed. Please check configuration.${NC}"
    exit 1
fi
