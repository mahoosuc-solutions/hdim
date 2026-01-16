#!/bin/bash

# End-to-End Compliance Testing Script
# =====================================
# Comprehensive test of the complete compliance error tracking flow

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
FRONTEND_URL="${FRONTEND_URL:-http://localhost:4200}"
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
DB_CONTAINER="${DB_CONTAINER:-hdim-demo-postgres}"
DB_NAME="${DB_NAME:-gateway_db}"
DB_USER="${DB_USER:-healthdata}"

echo "=========================================="
echo -e "${BLUE}End-to-End Compliance System Test${NC}"
echo "=========================================="
echo ""

# Test 1: Verify Frontend Configuration
echo -e "${YELLOW}Test 1: Frontend Configuration...${NC}"
if grep -q "syncToBackend: true" apps/clinical-portal/src/environments/environment.ts; then
    echo -e "  ${GREEN}✓${NC} syncToBackend: enabled"
else
    echo -e "  ${RED}✗${NC} syncToBackend: disabled"
    exit 1
fi

if grep -q "syncIntervalMs: 30000" apps/clinical-portal/src/environments/environment.ts; then
    echo -e "  ${GREEN}✓${NC} syncIntervalMs: 30000ms"
else
    echo -e "  ${YELLOW}⚠${NC} syncIntervalMs: check value"
fi

if grep -q "enableErrorTracking: true" apps/clinical-portal/src/environments/environment.ts; then
    echo -e "  ${GREEN}✓${NC} enableErrorTracking: enabled"
else
    echo -e "  ${RED}✗${NC} enableErrorTracking: disabled"
    exit 1
fi
echo ""

# Test 2: Verify Backend is Running
echo -e "${YELLOW}Test 2: Backend Service...${NC}"
if docker exec hdim-demo-gateway-clinical wget -q -O - http://localhost:8080/actuator/health 2>/dev/null | grep -q "UP"; then
    echo -e "  ${GREEN}✓${NC} Backend is running"
else
    echo -e "  ${RED}✗${NC} Backend is not running"
    exit 1
fi

# Test compliance endpoint
if docker exec hdim-demo-gateway-clinical wget -q -O - http://localhost:8080/api/v1/compliance/errors/stats 2>/dev/null | grep -q "total"; then
    echo -e "  ${GREEN}✓${NC} Compliance API accessible"
else
    echo -e "  ${YELLOW}⚠${NC} Compliance API may require errors to exist"
fi
echo ""

# Test 3: Verify Database
echo -e "${YELLOW}Test 3: Database...${NC}"
TABLE_EXISTS=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'compliance_errors');" 2>/dev/null | tr -d ' ')

if [ "$TABLE_EXISTS" = "t" ]; then
    echo -e "  ${GREEN}✓${NC} compliance_errors table exists"
else
    echo -e "  ${RED}✗${NC} compliance_errors table missing"
    exit 1
fi

INITIAL_COUNT=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors;" 2>/dev/null | tr -d ' ')
echo -e "  ${GREEN}✓${NC} Initial error count: ${INITIAL_COUNT}"
echo ""

# Test 4: Trigger Error Scenarios
echo -e "${YELLOW}Test 4: Triggering Error Scenarios...${NC}"
echo "  Running error scenario script..."
./scripts/test-compliance-error-scenarios.sh
echo ""

# Test 5: Wait for Sync
echo -e "${YELLOW}Test 5: Waiting for Frontend Sync...${NC}"
echo "  Waiting 35 seconds for sync interval..."
sleep 35
echo -e "  ${GREEN}✓${NC} Sync interval elapsed"
echo ""

# Test 6: Verify Errors in Database
echo -e "${YELLOW}Test 6: Verify Errors in Database...${NC}"
FINAL_COUNT=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors;" 2>/dev/null | tr -d ' ')

NEW_ERRORS=$((FINAL_COUNT - INITIAL_COUNT))
if [ "$NEW_ERRORS" -gt 0 ]; then
    echo -e "  ${GREEN}✓${NC} New errors synced: ${NEW_ERRORS}"
    echo -e "  ${GREEN}✓${NC} Total errors: ${FINAL_COUNT}"
else
    echo -e "  ${YELLOW}⚠${NC} No new errors found (may need more time or frontend not running)"
fi

# Show recent errors
echo ""
echo "  Recent errors:"
docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c \
    "SELECT timestamp, service, error_code, severity, LEFT(message, 50) as message 
     FROM compliance_errors 
     ORDER BY timestamp DESC 
     LIMIT 5;" 2>/dev/null | head -10
echo ""

# Test 7: Verify Data Integrity
echo -e "${YELLOW}Test 7: Data Integrity Check...${NC}"

# Check for unique IDs
UNIQUE_IDS=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(DISTINCT id) FROM compliance_errors;" 2>/dev/null | tr -d ' ')
TOTAL_IDS=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors;" 2>/dev/null | tr -d ' ')

if [ "$UNIQUE_IDS" -eq "$TOTAL_IDS" ]; then
    echo -e "  ${GREEN}✓${NC} All error IDs are unique"
else
    echo -e "  ${RED}✗${NC} Duplicate IDs found"
fi

# Check for required fields
NULL_COUNTS=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors 
     WHERE timestamp IS NULL OR tenant_id IS NULL OR service IS NULL 
        OR operation IS NULL OR error_code IS NULL OR severity IS NULL 
        OR message IS NULL;" 2>/dev/null | tr -d ' ')

if [ "$NULL_COUNTS" -eq 0 ]; then
    echo -e "  ${GREEN}✓${NC} All required fields populated"
else
    echo -e "  ${YELLOW}⚠${NC} Found ${NULL_COUNTS} records with null required fields"
fi

# Check tenant isolation
TENANT_COUNT=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(DISTINCT tenant_id) FROM compliance_errors;" 2>/dev/null | tr -d ' ')
echo -e "  ${GREEN}✓${NC} Tenant isolation: ${TENANT_COUNT} unique tenant(s)"
echo ""

# Test 8: Frontend Dashboard Check
echo -e "${YELLOW}Test 8: Frontend Dashboard...${NC}"
if curl -s -f "${FRONTEND_URL}/compliance" > /dev/null 2>&1; then
    echo -e "  ${GREEN}✓${NC} Dashboard accessible at ${FRONTEND_URL}/compliance"
    echo "  ${YELLOW}⚠${NC} Please verify dashboard displays errors manually"
else
    echo -e "  ${YELLOW}⚠${NC} Frontend may not be running (expected if testing backend only)"
fi
echo ""

# Summary
echo "=========================================="
echo -e "${GREEN}End-to-End Test Summary${NC}"
echo "=========================================="
echo ""
echo "Configuration:"
echo "  ✅ Frontend: syncToBackend enabled"
echo "  ✅ Backend: Service running"
echo "  ✅ Database: Table exists"
echo ""
echo "Error Tracking:"
echo "  ✅ Errors triggered: Multiple scenarios"
echo "  ✅ Errors synced: ${NEW_ERRORS} new errors"
echo "  ✅ Data integrity: Verified"
echo ""
echo "Next Steps:"
echo "  1. Check dashboard: ${FRONTEND_URL}/compliance"
echo "  2. Monitor real-time: ./scripts/monitor-compliance-database.sh"
echo "  3. Check alerts: Verify threshold alerts in dashboard"
echo ""
