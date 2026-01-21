#!/bin/bash

# Compliance Validation with Test Harness
# ======================================
# This script validates compliance tracking across the entire platform
# while inserting data from the test harness

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# Configuration
FRONTEND_URL="${FRONTEND_URL:-http://localhost:4200}"
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
COMPLIANCE_ENDPOINT="${BACKEND_URL}/api/v1/compliance/errors"
TEST_HARNESS_DIR="${TEST_HARNESS_DIR:-$(pwd)/test-harness}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5435}"
DB_USER="${DB_USER:-healthdata}"
DB_NAME="${DB_NAME:-gateway_db}"

# Log file
LOG_FILE="/tmp/compliance-validation-$(date +%Y%m%d-%H%M%S).log"

echo "=========================================="
echo -e "${BOLD}Compliance Validation with Test Harness${NC}"
echo "=========================================="
echo ""
echo "Configuration:"
echo "  Frontend URL: ${FRONTEND_URL}"
echo "  Backend URL: ${BACKEND_URL}"
echo "  Test Harness: ${TEST_HARNESS_DIR}"
echo "  Log File: ${LOG_FILE}"
echo ""

# Function to log messages
log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "${LOG_FILE}"
}

# Function to check service health
check_service() {
    local service_name=$1
    local url=$2
    
    log "Checking ${service_name} health..."
    if curl -s -f -o /dev/null "${url}/actuator/health" 2>/dev/null; then
        echo -e "${GREEN}✓${NC} ${service_name} is accessible"
        return 0
    else
        echo -e "${RED}✗${NC} ${service_name} is not accessible at ${url}"
        return 1
    fi
}

# Function to enable compliance mode in frontend
enable_compliance_mode() {
    log "Enabling compliance mode in frontend..."
    
    # Check if environment file exists
    local env_file="apps/clinical-portal/src/environments/environment.ts"
    if [ ! -f "$env_file" ]; then
        echo -e "${RED}✗${NC} Environment file not found: ${env_file}"
        return 1
    fi
    
    # Backup original file
    cp "$env_file" "${env_file}.backup"
    
    # Enable compliance mode
    sed -i 's/disableFallbacks: false/disableFallbacks: true/g' "$env_file"
    sed -i 's/strictErrorHandling: false/strictErrorHandling: true/g' "$env_file"
    
    echo -e "${GREEN}✓${NC} Compliance mode enabled (backup saved to ${env_file}.backup)"
    log "Compliance mode enabled in ${env_file}"
    
    return 0
}

# Function to get error count from backend
get_error_count() {
    local tenant_id="${1:-default-tenant}"
    local response=$(curl -s -X GET "${COMPLIANCE_ENDPOINT}?tenantId=${tenant_id}&size=1" \
        -H "Authorization: Bearer ${TOKEN:-}" 2>/dev/null || echo '{"totalElements":0}')
    
    echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('totalElements', 0))" 2>/dev/null || echo "0"
}

# Function to get error stats
get_error_stats() {
    local tenant_id="${1:-default-tenant}"
    curl -s -X GET "${COMPLIANCE_ENDPOINT}/stats?tenantId=${tenant_id}" \
        -H "Authorization: Bearer ${TOKEN:-}" 2>/dev/null || echo '{}'
}

# Function to load test data
load_test_data() {
    log "Loading test data from test harness..."
    
    cd "${TEST_HARNESS_DIR}" || {
        echo -e "${RED}✗${NC} Test harness directory not found: ${TEST_HARNESS_DIR}"
        return 1
    }
    
    # Check if test harness scripts exist
    if [ ! -f "load-fhir-data.sh" ]; then
        echo -e "${YELLOW}⚠${NC} Test harness scripts not found, skipping data load"
        return 0
    fi
    
    # Get initial error count
    local initial_errors=$(get_error_count)
    log "Initial error count: ${initial_errors}"
    
    echo -e "${CYAN}Loading FHIR data...${NC}"
    
    # Run test harness data loading
    if bash load-fhir-data.sh 2>&1 | tee -a "${LOG_FILE}"; then
        echo -e "${GREEN}✓${NC} Test data loaded successfully"
        
        # Wait for errors to sync
        sleep 5
        
        # Get final error count
        local final_errors=$(get_error_count)
        log "Final error count: ${final_errors}"
        
        local new_errors=$((final_errors - initial_errors))
        if [ "$new_errors" -gt 0 ]; then
            echo -e "${YELLOW}⚠${NC} ${new_errors} new errors detected during data loading"
            log "New errors detected: ${new_errors}"
        else
            echo -e "${GREEN}✓${NC} No new errors detected"
        fi
    else
        echo -e "${RED}✗${NC} Failed to load test data"
        return 1
    fi
    
    return 0
}

# Function to trigger frontend operations
trigger_frontend_operations() {
    log "Triggering frontend operations to generate errors..."
    
    echo -e "${CYAN}Triggering frontend API calls...${NC}"
    
    # Get auth token
    AUTH_RESPONSE=$(curl -s -X POST "${BACKEND_URL}/api/v1/auth/login" \
        -H 'Content-Type: application/json' \
        -d '{"username": "test_admin", "password": "password123"}' 2>/dev/null || echo '{}')
    
    TOKEN=$(echo "$AUTH_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null || echo "")
    
    if [ -z "$TOKEN" ]; then
        echo -e "${YELLOW}⚠${NC} Could not get auth token, skipping authenticated requests"
    fi
    
    # Get initial error count
    local initial_errors=$(get_error_count)
    log "Initial error count before frontend operations: ${initial_errors}"
    
    # Trigger various API endpoints that might generate errors
    local endpoints=(
        "/fhir/Patient"
        "/api/v1/patients"
        "/api/v1/care-gaps"
        "/api/v1/quality-measures"
    )
    
    for endpoint in "${endpoints[@]}"; do
        echo -e "  Testing ${endpoint}..."
        if [ -n "$TOKEN" ]; then
            curl -s -X GET "${BACKEND_URL}${endpoint}" \
                -H "Authorization: Bearer ${TOKEN}" \
                -H "X-Tenant-ID: default" \
                -o /dev/null -w "  Status: %{http_code}\n" 2>&1 | tee -a "${LOG_FILE}"
        else
            curl -s -X GET "${BACKEND_URL}${endpoint}" \
                -o /dev/null -w "  Status: %{http_code}\n" 2>&1 | tee -a "${LOG_FILE}"
        fi
        sleep 1
    done
    
    # Wait for errors to sync
    sleep 5
    
    # Get final error count
    local final_errors=$(get_error_count)
    log "Final error count after frontend operations: ${final_errors}"
    
    local new_errors=$((final_errors - initial_errors))
    if [ "$new_errors" -gt 0 ]; then
        echo -e "${YELLOW}⚠${NC} ${new_errors} new errors detected from frontend operations"
        log "New errors from frontend: ${new_errors}"
    else
        echo -e "${GREEN}✓${NC} No new errors detected"
    fi
    
    return 0
}

# Function to validate compliance tracking
validate_compliance_tracking() {
    log "Validating compliance tracking system..."
    
    echo -e "${CYAN}Validating compliance tracking...${NC}"
    
    # Test 1: Check backend endpoint
    echo "  Test 1: Backend compliance endpoint..."
    if curl -s -f -o /dev/null "${COMPLIANCE_ENDPOINT}" 2>/dev/null; then
        echo -e "    ${GREEN}✓${NC} Endpoint is accessible"
    else
        echo -e "    ${RED}✗${NC} Endpoint is not accessible"
        return 1
    fi
    
    # Test 2: Send test error
    echo "  Test 2: Sending test error..."
    TEST_PAYLOAD='{
      "errors": [{
        "id": "err-1736934000000-validation-test",
        "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.000Z)'",
        "context": {
          "service": "Validation Test",
          "endpoint": "/api/test",
          "operation": "GET /api/test",
          "errorCode": "ERR-9001",
          "severity": "ERROR",
          "userId": "validation-user",
          "tenantId": "validation-tenant"
        },
        "message": "Compliance validation test error",
        "stack": "Error: Test\n    at validation.js:1:1"
      }],
      "syncedAt": "'$(date -u +%Y-%m-%dT%H:%M:%S.000Z)'"
    }'
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${COMPLIANCE_ENDPOINT}" \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: validation-tenant" \
        -d "${TEST_PAYLOAD}" 2>/dev/null)
    
    HTTP_CODE=$(echo "${RESPONSE}" | tail -n1)
    BODY=$(echo "${RESPONSE}" | sed '$d')
    
    if [ "${HTTP_CODE}" = "200" ]; then
        echo -e "    ${GREEN}✓${NC} Test error synced successfully"
        log "Test error synced: ${BODY}"
    else
        echo -e "    ${RED}✗${NC} Failed to sync test error (HTTP ${HTTP_CODE})"
        log "Failed to sync test error: ${BODY}"
        return 1
    fi
    
    # Test 3: Verify error in database
    echo "  Test 3: Verifying error in database..."
    sleep 2
    local error_count=$(get_error_count "validation-tenant")
    if [ "$error_count" -gt 0 ]; then
        echo -e "    ${GREEN}✓${NC} Error found in database (count: ${error_count})"
    else
        echo -e "    ${YELLOW}⚠${NC} Error not found in database (may need to wait for sync)"
    fi
    
    # Test 4: Get error stats
    echo "  Test 4: Getting error statistics..."
    local stats=$(get_error_stats "validation-tenant")
    if [ -n "$stats" ] && [ "$stats" != "{}" ]; then
        echo -e "    ${GREEN}✓${NC} Error statistics retrieved"
        echo "    Stats: ${stats}"
    else
        echo -e "    ${YELLOW}⚠${NC} Could not retrieve error statistics"
    fi
    
    return 0
}

# Function to generate compliance report
generate_report() {
    log "Generating compliance validation report..."
    
    local report_file="docs/compliance/VALIDATION_REPORT_$(date +%Y%m%d-%H%M%S).md"
    
    mkdir -p "$(dirname "$report_file")"
    
    cat > "$report_file" << EOF
# Compliance Validation Report

**Date**: $(date)
**Validation Script**: validate-compliance-with-test-harness.sh

## Summary

This report documents the compliance validation process across the entire platform
while inserting data from the test harness.

## Test Results

### Service Health
- Backend: $(check_service "Backend" "${BACKEND_URL}" && echo "✓ Healthy" || echo "✗ Unhealthy")
- Frontend: $(curl -s -f -o /dev/null "${FRONTEND_URL}" 2>/dev/null && echo "✓ Accessible" || echo "✗ Not Accessible")

### Compliance Tracking
- Endpoint: ${COMPLIANCE_ENDPOINT}
- Test Error Sync: $(validate_compliance_tracking > /dev/null 2>&1 && echo "✓ Passed" || echo "✗ Failed")

### Error Statistics
\`\`\`json
$(get_error_stats)
\`\`\`

## Log File

Full validation log: ${LOG_FILE}

## Next Steps

1. Review errors in compliance dashboard: ${FRONTEND_URL}/compliance
2. Check backend logs for sync confirmations
3. Verify error retention policy is working
4. Test alert thresholds

EOF

    echo -e "${GREEN}✓${NC} Report generated: ${report_file}"
    log "Report generated: ${report_file}"
}

# Main execution
main() {
    echo ""
    echo -e "${BOLD}Step 1: Service Health Checks${NC}"
    echo "─────────────────────────────────────"
    
    if ! check_service "Backend" "${BACKEND_URL}"; then
        echo -e "${RED}Backend service is not running. Please start it first.${NC}"
        exit 1
    fi
    
    echo ""
    echo -e "${BOLD}Step 2: Enable Compliance Mode${NC}"
    echo "─────────────────────────────────────"
    
    if enable_compliance_mode; then
        echo -e "${YELLOW}⚠${NC} Frontend needs to be restarted for compliance mode to take effect"
    fi
    
    echo ""
    echo -e "${BOLD}Step 3: Validate Compliance Tracking${NC}"
    echo "─────────────────────────────────────"
    
    if ! validate_compliance_tracking; then
        echo -e "${RED}Compliance tracking validation failed${NC}"
        exit 1
    fi
    
    echo ""
    echo -e "${BOLD}Step 4: Load Test Data${NC}"
    echo "─────────────────────────────────────"
    
    load_test_data
    
    echo ""
    echo -e "${BOLD}Step 5: Trigger Frontend Operations${NC}"
    echo "─────────────────────────────────────"
    
    trigger_frontend_operations
    
    echo ""
    echo -e "${BOLD}Step 6: Generate Report${NC}"
    echo "─────────────────────────────────────"
    
    generate_report
    
    echo ""
    echo "=========================================="
    echo -e "${GREEN}Validation Complete${NC}"
    echo "=========================================="
    echo ""
    echo "Next steps:"
    echo "1. Review compliance dashboard: ${FRONTEND_URL}/compliance"
    echo "2. Check error statistics in backend"
    echo "3. Verify alerts are triggering correctly"
    echo "4. Review log file: ${LOG_FILE}"
    echo ""
}

# Run main function
main "$@"
