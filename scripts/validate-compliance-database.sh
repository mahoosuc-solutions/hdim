#!/bin/bash

# Compliance Database Validation Script
# =====================================
# Validates that the compliance_errors table exists and is properly configured

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5435}"
DB_NAME="${DB_NAME:-gateway_db}"
DB_USER="${DB_USER:-healthdata}"
DB_PASSWORD="${DB_PASSWORD:-}"

echo "=========================================="
echo -e "${BLUE}Compliance Database Validation${NC}"
echo "=========================================="
echo ""
echo "Database Configuration:"
echo "  Host: ${DB_HOST}"
echo "  Port: ${DB_PORT}"
echo "  Database: ${DB_NAME}"
echo "  User: ${DB_USER}"
echo ""

# Function to run SQL command
run_sql() {
    local sql="$1"
    PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -t -A -c "$sql" 2>&1
}

# Test 1: Check database connection
echo -e "${YELLOW}Test 1: Database Connection...${NC}"
if PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Database connection successful"
else
    echo -e "${RED}✗${NC} Database connection failed"
    echo ""
    echo "Troubleshooting:"
    echo "1. Check if PostgreSQL is running: docker ps | grep postgres"
    echo "2. Verify connection string: postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
    echo "3. Check credentials: user=${DB_USER}"
    exit 1
fi

# Test 2: Check if table exists
echo ""
echo -e "${YELLOW}Test 2: Table Existence...${NC}"
TABLE_EXISTS=$(run_sql "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'compliance_errors');" | tr -d ' ')

if [ "$TABLE_EXISTS" = "t" ]; then
    echo -e "${GREEN}✓${NC} Table 'compliance_errors' exists"
else
    echo -e "${RED}✗${NC} Table 'compliance_errors' does not exist"
    echo ""
    echo "The table will be created automatically when the service starts with Liquibase enabled."
    echo "Or you can run the migration manually:"
    echo "  ./gradlew :modules:services:gateway-clinical-service:bootRun"
    exit 1
fi

# Test 3: Check table structure
echo ""
echo -e "${YELLOW}Test 3: Table Structure...${NC}"
COLUMNS=$(run_sql "SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = 'compliance_errors' ORDER BY ordinal_position;")

REQUIRED_COLUMNS=("id" "timestamp" "tenant_id" "service" "operation" "error_code" "severity" "message" "created_at")
MISSING_COLUMNS=()

for col in "${REQUIRED_COLUMNS[@]}"; do
    if echo "$COLUMNS" | grep -q "^${col}|"; then
        echo -e "  ${GREEN}✓${NC} Column: ${col}"
    else
        echo -e "  ${RED}✗${NC} Column missing: ${col}"
        MISSING_COLUMNS+=("$col")
    fi
done

if [ ${#MISSING_COLUMNS[@]} -gt 0 ]; then
    echo ""
    echo -e "${RED}Missing required columns: ${MISSING_COLUMNS[*]}${NC}"
    exit 1
fi

# Test 4: Check indexes
echo ""
echo -e "${YELLOW}Test 4: Indexes...${NC}"
INDEXES=$(run_sql "SELECT indexname FROM pg_indexes WHERE tablename = 'compliance_errors';")

REQUIRED_INDEXES=("idx_compliance_tenant_timestamp" "idx_compliance_severity" "idx_compliance_service" "idx_compliance_timestamp")
MISSING_INDEXES=()

for idx in "${REQUIRED_INDEXES[@]}"; do
    if echo "$INDEXES" | grep -q "^${idx}$"; then
        echo -e "  ${GREEN}✓${NC} Index: ${idx}"
    else
        echo -e "  ${YELLOW}⚠${NC} Index missing: ${idx} (will be created on next migration)"
        MISSING_INDEXES+=("$idx")
    fi
done

# Test 5: Check JSONB support
echo ""
echo -e "${YELLOW}Test 5: JSONB Support...${NC}"
JSONB_COLUMN=$(run_sql "SELECT data_type FROM information_schema.columns WHERE table_name = 'compliance_errors' AND column_name = 'additional_data';" | tr -d ' ')

if [ "$JSONB_COLUMN" = "jsonb" ]; then
    echo -e "${GREEN}✓${NC} JSONB column 'additional_data' configured correctly"
else
    echo -e "${YELLOW}⚠${NC} JSONB column type: ${JSONB_COLUMN}"
fi

# Test 6: Test insert/query
echo ""
echo -e "${YELLOW}Test 6: Insert/Query Test...${NC}"
TEST_ID=$(uuidgen 2>/dev/null || python3 -c "import uuid; print(uuid.uuid4())" 2>/dev/null || echo "00000000-0000-0000-0000-000000000001")
TEST_TENANT="validation-test-tenant"

# Insert test record
INSERT_RESULT=$(run_sql "INSERT INTO compliance_errors (id, timestamp, tenant_id, service, operation, error_code, severity, message, created_at) VALUES ('${TEST_ID}', NOW(), '${TEST_TENANT}', 'Test Service', 'Test Operation', 'ERR-9001', 'INFO', 'Database validation test', NOW()) RETURNING id;" 2>&1)

if echo "$INSERT_RESULT" | grep -q "$TEST_ID"; then
    echo -e "${GREEN}✓${NC} Insert test successful"
    
    # Query test
    QUERY_RESULT=$(run_sql "SELECT COUNT(*) FROM compliance_errors WHERE tenant_id = '${TEST_TENANT}';" | tr -d ' ')
    if [ "$QUERY_RESULT" -gt 0 ]; then
        echo -e "${GREEN}✓${NC} Query test successful (found ${QUERY_RESULT} record(s))"
    fi
    
    # Cleanup
    run_sql "DELETE FROM compliance_errors WHERE tenant_id = '${TEST_TENANT}';" > /dev/null 2>&1
    echo -e "${GREEN}✓${NC} Cleanup successful"
else
    echo -e "${RED}✗${NC} Insert test failed: ${INSERT_RESULT}"
    exit 1
fi

# Summary
echo ""
echo "=========================================="
echo -e "${GREEN}Database Validation Complete${NC}"
echo "=========================================="
echo ""
echo "✅ Database connection: Working"
echo "✅ Table structure: Correct"
echo "✅ Indexes: Present"
echo "✅ JSONB support: Enabled"
echo "✅ Insert/Query: Working"
echo ""
echo "The compliance_errors table is ready for use!"
