#!/bin/bash

# Compliance Data Integrity Validation Script
# ============================================
# Validates error data integrity in the database

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

echo "=========================================="
echo -e "${BLUE}Compliance Data Integrity Validation${NC}"
echo "=========================================="
echo ""

# Test 1: Unique IDs
echo -e "${YELLOW}Test 1: Unique Error IDs...${NC}"
UNIQUE_IDS=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(DISTINCT id) FROM compliance_errors;" 2>/dev/null | tr -d ' ')
TOTAL_IDS=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors;" 2>/dev/null | tr -d ' ')

if [ "$UNIQUE_IDS" -eq "$TOTAL_IDS" ] && [ "$TOTAL_IDS" -gt 0 ]; then
    echo -e "  ${GREEN}✓${NC} All ${TOTAL_IDS} error IDs are unique"
else
    if [ "$TOTAL_IDS" -eq 0 ]; then
        echo -e "  ${YELLOW}⚠${NC} No errors in database (run error scenarios first)"
    else
        echo -e "  ${RED}✗${NC} Found duplicate IDs: ${TOTAL_IDS} total, ${UNIQUE_IDS} unique"
    fi
fi
echo ""

# Test 2: Timestamp Accuracy
echo -e "${YELLOW}Test 2: Timestamp Accuracy...${NC}"
NULL_TIMESTAMPS=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors WHERE timestamp IS NULL;" 2>/dev/null | tr -d ' ')

if [ "$NULL_TIMESTAMPS" -eq 0 ]; then
    echo -e "  ${GREEN}✓${NC} All timestamps are populated"
    
    # Check timestamp range (should be recent)
    OLDEST=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
        "SELECT MIN(timestamp) FROM compliance_errors;" 2>/dev/null | tr -d ' ')
    NEWEST=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
        "SELECT MAX(timestamp) FROM compliance_errors;" 2>/dev/null | tr -d ' ')
    
    echo "  Oldest: ${OLDEST}"
    echo "  Newest: ${NEWEST}"
else
    echo -e "  ${RED}✗${NC} Found ${NULL_TIMESTAMPS} records with null timestamps"
fi
echo ""

# Test 3: Tenant IDs
echo -e "${YELLOW}Test 3: Tenant ID Validation...${NC}"
NULL_TENANTS=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors WHERE tenant_id IS NULL;" 2>/dev/null | tr -d ' ')

if [ "$NULL_TENANTS" -eq 0 ]; then
    echo -e "  ${GREEN}✓${NC} All tenant IDs are populated"
    
    TENANT_LIST=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
        "SELECT DISTINCT tenant_id FROM compliance_errors ORDER BY tenant_id;" 2>/dev/null)
    
    echo "  Unique tenants:"
    echo "$TENANT_LIST" | sed 's/^/    - /'
else
    echo -e "  ${RED}✗${NC} Found ${NULL_TENANTS} records with null tenant_id"
fi
echo ""

# Test 4: Service Names
echo -e "${YELLOW}Test 4: Service Name Validation...${NC}"
NULL_SERVICES=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors WHERE service IS NULL;" 2>/dev/null | tr -d ' ')

if [ "$NULL_SERVICES" -eq 0 ]; then
    echo -e "  ${GREEN}✓${NC} All service names are populated"
    
    SERVICE_LIST=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
        "SELECT service, COUNT(*) as count 
         FROM compliance_errors 
         GROUP BY service 
         ORDER BY count DESC;" 2>/dev/null)
    
    echo "  Services and error counts:"
    echo "$SERVICE_LIST" | sed 's/^/    - /'
else
    echo -e "  ${RED}✗${NC} Found ${NULL_SERVICES} records with null service"
fi
echo ""

# Test 5: Error Codes
echo -e "${YELLOW}Test 5: Error Code Validation...${NC}"
NULL_CODES=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors WHERE error_code IS NULL;" 2>/dev/null | tr -d ' ')

if [ "$NULL_CODES" -eq 0 ]; then
    echo -e "  ${GREEN}✓${NC} All error codes are populated"
    
    CODE_LIST=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
        "SELECT error_code, COUNT(*) as count 
         FROM compliance_errors 
         GROUP BY error_code 
         ORDER BY count DESC 
         LIMIT 10;" 2>/dev/null)
    
    echo "  Top error codes:"
    echo "$CODE_LIST" | sed 's/^/    - /'
else
    echo -e "  ${RED}✗${NC} Found ${NULL_CODES} records with null error_code"
fi
echo ""

# Test 6: Severity Levels
echo -e "${YELLOW}Test 6: Severity Level Validation...${NC}"
NULL_SEVERITY=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors WHERE severity IS NULL;" 2>/dev/null | tr -d ' ')

if [ "$NULL_SEVERITY" -eq 0 ]; then
    echo -e "  ${GREEN}✓${NC} All severity levels are populated"
    
    SEVERITY_LIST=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
        "SELECT severity, COUNT(*) as count 
         FROM compliance_errors 
         GROUP BY severity 
         ORDER BY 
           CASE severity 
             WHEN 'CRITICAL' THEN 1 
             WHEN 'ERROR' THEN 2 
             WHEN 'WARNING' THEN 3 
             WHEN 'INFO' THEN 4 
             ELSE 5 
           END;" 2>/dev/null)
    
    echo "  Severity distribution:"
    echo "$SEVERITY_LIST" | sed 's/^/    - /'
else
    echo -e "  ${RED}✗${NC} Found ${NULL_SEVERITY} records with null severity"
fi
echo ""

# Test 7: Message Content
echo -e "${YELLOW}Test 7: Message Content Validation...${NC}"
NULL_MESSAGES=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors WHERE message IS NULL OR message = '';" 2>/dev/null | tr -d ' ')

if [ "$NULL_MESSAGES" -eq 0 ]; then
    echo -e "  ${GREEN}✓${NC} All messages are populated"
    
    AVG_LENGTH=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
        "SELECT AVG(LENGTH(message))::INT FROM compliance_errors;" 2>/dev/null | tr -d ' ')
    
    echo "  Average message length: ${AVG_LENGTH} characters"
else
    echo -e "  ${RED}✗${NC} Found ${NULL_MESSAGES} records with empty messages"
fi
echo ""

# Test 8: JSONB Additional Data
echo -e "${YELLOW}Test 8: JSONB Additional Data...${NC}"
JSONB_COUNT=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors WHERE additional_data IS NOT NULL;" 2>/dev/null | tr -d ' ')
TOTAL_COUNT=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors;" 2>/dev/null | tr -d ' ')

if [ "$TOTAL_COUNT" -gt 0 ]; then
    PERCENTAGE=$((JSONB_COUNT * 100 / TOTAL_COUNT))
    echo "  Records with additional_data: ${JSONB_COUNT}/${TOTAL_COUNT} (${PERCENTAGE}%)"
    
    # Test JSONB query
    JSONB_VALID=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
        "SELECT COUNT(*) FROM compliance_errors 
         WHERE additional_data IS NOT NULL 
         AND jsonb_typeof(additional_data) = 'object';" 2>/dev/null | tr -d ' ')
    
    if [ "$JSONB_VALID" -eq "$JSONB_COUNT" ]; then
        echo -e "  ${GREEN}✓${NC} All JSONB data is valid"
    else
        echo -e "  ${YELLOW}⚠${NC} Some JSONB data may be invalid"
    fi
else
    echo -e "  ${YELLOW}⚠${NC} No errors to validate"
fi
echo ""

# Test 9: Required Fields Summary
echo -e "${YELLOW}Test 9: Required Fields Summary...${NC}"
REQUIRED_FIELDS_NULL=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors 
     WHERE timestamp IS NULL OR tenant_id IS NULL OR service IS NULL 
        OR operation IS NULL OR error_code IS NULL OR severity IS NULL 
        OR message IS NULL OR created_at IS NULL;" 2>/dev/null | tr -d ' ')

if [ "$REQUIRED_FIELDS_NULL" -eq 0 ]; then
    echo -e "  ${GREEN}✓${NC} All required fields are populated"
else
    echo -e "  ${RED}✗${NC} Found ${REQUIRED_FIELDS_NULL} records with null required fields"
fi
echo ""

# Summary
echo "=========================================="
echo -e "${GREEN}Data Integrity Validation Complete${NC}"
echo "=========================================="
echo ""
echo "Summary:"
echo "  Total errors: ${TOTAL_IDS}"
echo "  Unique IDs: ${UNIQUE_IDS}"
echo "  Required fields: $([ "$REQUIRED_FIELDS_NULL" -eq 0 ] && echo "All populated" || echo "${REQUIRED_FIELDS_NULL} missing")"
echo "  JSONB data: ${JSONB_COUNT} records"
echo ""
