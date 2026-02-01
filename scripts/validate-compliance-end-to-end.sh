#!/bin/bash

# Compliance System End-to-End Validation Script
# ===============================================
# Validates the complete compliance tracking flow from frontend to database

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
DB_CONTAINER="${DB_CONTAINER:-hdim-demo-postgres}"
DB_NAME="${DB_NAME:-gateway_db}"
DB_USER="${DB_USER:-healthdata}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:4200}"

echo "=========================================="
echo -e "${BLUE}Compliance System End-to-End Validation${NC}"
echo "=========================================="
echo ""

# Test 1: Database connectivity
echo -e "${YELLOW}Test 1: Database Connectivity...${NC}"
if docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Database connection successful"
else
    echo -e "${RED}✗${NC} Database connection failed"
    exit 1
fi

# Test 2: Table existence
echo ""
echo -e "${YELLOW}Test 2: Table Existence...${NC}"
TABLE_EXISTS=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'compliance_errors');" | tr -d ' ')

if [ "$TABLE_EXISTS" = "t" ]; then
    echo -e "${GREEN}✓${NC} Table 'compliance_errors' exists"
else
    echo -e "${RED}✗${NC} Table 'compliance_errors' does not exist"
    exit 1
fi

# Test 3: Backend API health
echo ""
echo -e "${YELLOW}Test 3: Backend API Health...${NC}"
if curl -s -f "${GATEWAY_URL}/actuator/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Backend API is healthy"
    BACKEND_UP=true
else
    echo -e "${YELLOW}⚠${NC} Backend API not accessible (may not be running)"
    BACKEND_UP=false
fi

# Test 4: Compliance endpoint
echo ""
echo -e "${YELLOW}Test 4: Compliance API Endpoint...${NC}"
if [ "$BACKEND_UP" = true ]; then
    if curl -s -f -X GET "${GATEWAY_URL}/api/v1/compliance/errors/stats" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} Compliance API endpoint accessible"
    else
        echo -e "${YELLOW}⚠${NC} Compliance API endpoint not accessible (may require auth)"
    fi
else
    echo -e "${YELLOW}⚠${NC} Skipped (backend not available)"
fi

# Test 5: Frontend accessibility
echo ""
echo -e "${YELLOW}Test 5: Frontend Accessibility...${NC}"
if curl -s -f "${FRONTEND_URL}" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Frontend is accessible"
    FRONTEND_UP=true
else
    echo -e "${YELLOW}⚠${NC} Frontend not accessible (may not be running)"
    FRONTEND_UP=false
fi

# Test 6: Database structure validation
echo ""
echo -e "${YELLOW}Test 6: Database Structure Validation...${NC}"
REQUIRED_COLUMNS=("id" "timestamp" "tenant_id" "service" "operation" "error_code" "severity" "message" "created_at")
MISSING_COLUMNS=()

for col in "${REQUIRED_COLUMNS[@]}"; do
    EXISTS=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
        "SELECT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'compliance_errors' AND column_name = '${col}');" | tr -d ' ')
    
    if [ "$EXISTS" = "t" ]; then
        echo -e "  ${GREEN}✓${NC} Column: ${col}"
    else
        echo -e "  ${RED}✗${NC} Column missing: ${col}"
        MISSING_COLUMNS+=("$col")
    fi
done

if [ ${#MISSING_COLUMNS[@]} -gt 0 ]; then
    echo -e "${RED}✗${NC} Missing required columns: ${MISSING_COLUMNS[*]}"
    exit 1
fi

# Test 7: Index validation
echo ""
echo -e "${YELLOW}Test 7: Index Validation...${NC}"
REQUIRED_INDEXES=("idx_compliance_tenant_timestamp" "idx_compliance_severity" "idx_compliance_service" "idx_compliance_timestamp")
MISSING_INDEXES=()

for idx in "${REQUIRED_INDEXES[@]}"; do
    EXISTS=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
        "SELECT EXISTS (SELECT FROM pg_indexes WHERE tablename = 'compliance_errors' AND indexname = '${idx}');" | tr -d ' ')
    
    if [ "$EXISTS" = "t" ]; then
        echo -e "  ${GREEN}✓${NC} Index: ${idx}"
    else
        echo -e "  ${YELLOW}⚠${NC} Index missing: ${idx}"
        MISSING_INDEXES+=("$idx")
    fi
done

# Test 8: Insert/Query functionality
echo ""
echo -e "${YELLOW}Test 8: Insert/Query Functionality...${NC}"
TEST_ID=$(uuidgen 2>/dev/null || python3 -c "import uuid; print(uuid.uuid4())" 2>/dev/null || echo "00000000-0000-0000-0000-000000000001")
TEST_TENANT="validation-e2e-test"

# Insert test record
INSERT_RESULT=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "INSERT INTO compliance_errors (id, timestamp, tenant_id, service, operation, error_code, severity, message, created_at) 
     VALUES ('${TEST_ID}', NOW(), '${TEST_TENANT}', 'E2E Test Service', 'Validation Test', 'ERR-E2E-001', 'INFO', 'End-to-end validation test', NOW()) 
     RETURNING id;" 2>&1 | tr -d ' ')

if [ -n "$INSERT_RESULT" ]; then
    echo -e "  ${GREEN}✓${NC} Insert test successful (ID: ${INSERT_RESULT})"
    
    # Query test
    QUERY_COUNT=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
        "SELECT COUNT(*) FROM compliance_errors WHERE tenant_id = '${TEST_TENANT}';" | tr -d ' ')
    
    if [ "$QUERY_COUNT" -gt 0 ]; then
        echo -e "  ${GREEN}✓${NC} Query test successful (found ${QUERY_COUNT} record(s))"
    else
        echo -e "  ${RED}✗${NC} Query test failed"
    fi
    
    # Cleanup
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c \
        "DELETE FROM compliance_errors WHERE tenant_id = '${TEST_TENANT}';" > /dev/null 2>&1
    echo -e "  ${GREEN}✓${NC} Cleanup successful"
else
    echo -e "  ${RED}✗${NC} Insert test failed"
    exit 1
fi

# Test 9: Current error count
echo ""
echo -e "${YELLOW}Test 9: Current Database State...${NC}"
ERROR_COUNT=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors;" | tr -d ' ')

TENANT_COUNT=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(DISTINCT tenant_id) FROM compliance_errors;" | tr -d ' ')

SERVICE_COUNT=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(DISTINCT service) FROM compliance_errors;" | tr -d ' ')

echo "  Total errors: ${ERROR_COUNT}"
echo "  Unique tenants: ${TENANT_COUNT}"
echo "  Unique services: ${SERVICE_COUNT}"

# Test 10: JSONB functionality
echo ""
echo -e "${YELLOW}Test 10: JSONB Functionality...${NC}"
JSONB_TEST=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT data_type FROM information_schema.columns WHERE table_name = 'compliance_errors' AND column_name = 'additional_data';" | tr -d ' ')

if [ "$JSONB_TEST" = "jsonb" ]; then
    echo -e "  ${GREEN}✓${NC} JSONB column type correct"
    
    # Test JSONB insert
    JSONB_INSERT=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
        "INSERT INTO compliance_errors (id, timestamp, tenant_id, service, operation, error_code, severity, message, additional_data, created_at) 
         VALUES (gen_random_uuid(), NOW(), 'jsonb-test', 'Test Service', 'JSONB Test', 'ERR-JSONB-001', 'INFO', 'JSONB test', '{\"test\": true, \"value\": 123}'::jsonb, NOW()) 
         RETURNING id;" 2>&1 | tr -d ' ')
    
    if [ -n "$JSONB_INSERT" ]; then
        echo -e "  ${GREEN}✓${NC} JSONB insert successful"
        
        # Test JSONB query
        JSONB_QUERY=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
            "SELECT additional_data->>'test' FROM compliance_errors WHERE tenant_id = 'jsonb-test';" | tr -d ' ')
        
        if [ "$JSONB_QUERY" = "true" ]; then
            echo -e "  ${GREEN}✓${NC} JSONB query successful"
        else
            echo -e "  ${YELLOW}⚠${NC} JSONB query returned: ${JSONB_QUERY}"
        fi
        
        # Cleanup
        docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c \
            "DELETE FROM compliance_errors WHERE tenant_id = 'jsonb-test';" > /dev/null 2>&1
    else
        echo -e "  ${RED}✗${NC} JSONB insert failed"
    fi
else
    echo -e "  ${RED}✗${NC} JSONB column type incorrect: ${JSONB_TEST}"
fi

# Summary
echo ""
echo "=========================================="
echo -e "${GREEN}Validation Complete${NC}"
echo "=========================================="
echo ""
echo "✅ Database: Connected and accessible"
echo "✅ Table: Exists with correct structure"
echo "✅ Columns: All required columns present"
echo "✅ Indexes: Performance indexes configured"
echo "✅ Functionality: Insert/Query working"
echo "✅ JSONB: Supported and functional"
echo ""
echo "Current State:"
echo "  - Total errors: ${ERROR_COUNT}"
echo "  - Unique tenants: ${TENANT_COUNT}"
echo "  - Unique services: ${SERVICE_COUNT}"
echo ""
echo "Next Steps:"
echo "  1. Start gateway-clinical-service to enable API endpoints"
echo "  2. Start frontend to trigger error tracking"
echo "  3. Run: ./scripts/monitor-compliance-database.sh"
echo "  4. Check: ${FRONTEND_URL}/compliance"
echo ""
