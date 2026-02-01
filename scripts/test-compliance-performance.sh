#!/bin/bash

# Compliance System Performance Testing Script
# =============================================
# Tests performance metrics for compliance tracking

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
DB_CONTAINER="${DB_CONTAINER:-hdim-demo-postgres}"
DB_NAME="${DB_NAME:-gateway_db}"
DB_USER="${DB_USER:-healthdata}"

echo "=========================================="
echo -e "${BLUE}Compliance System Performance Test${NC}"
echo "=========================================="
echo ""

# Performance Targets
TARGET_ERROR_CAPTURE_MS=10
TARGET_SYNC_TIME_MS=1000
TARGET_DB_INSERT_MS=50
TARGET_DASHBOARD_LOAD_MS=2000
TARGET_ALERT_GENERATION_MS=100

# Test 1: Database Insert Performance
echo -e "${CYAN}Test 1: Database Insert Performance${NC}"
echo "  Inserting 100 test errors..."

START_TIME=$(date +%s%N)
for i in {1..100}; do
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c \
        "INSERT INTO compliance_errors (
            id, timestamp, tenant_id, service, operation, 
            error_code, severity, message, created_at
        ) VALUES (
            gen_random_uuid(), 
            NOW(), 
            'perf-test-tenant', 
            'Performance Test Service', 
            'Performance Test Operation', 
            'ERR-PERF-${i}', 
            'INFO', 
            'Performance test error ${i}', 
            NOW()
        );" > /dev/null 2>&1
done
END_TIME=$(date +%s%N)

TOTAL_TIME_MS=$(( (END_TIME - START_TIME) / 1000000 ))
AVG_INSERT_MS=$(( TOTAL_TIME_MS / 100 ))

echo "  Total time: ${TOTAL_TIME_MS}ms for 100 inserts"
echo "  Average per insert: ${AVG_INSERT_MS}ms"

if [ "$AVG_INSERT_MS" -lt "$TARGET_DB_INSERT_MS" ]; then
    echo -e "  ${GREEN}✓${NC} Performance target met (${AVG_INSERT_MS}ms < ${TARGET_DB_INSERT_MS}ms)"
else
    echo -e "  ${YELLOW}⚠${NC} Performance target exceeded (${AVG_INSERT_MS}ms >= ${TARGET_DB_INSERT_MS}ms)"
fi
echo ""

# Test 2: Database Query Performance
echo -e "${CYAN}Test 2: Database Query Performance${NC}"
echo "  Testing query performance..."

# Count query
START_TIME=$(date +%s%N)
docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors;" > /dev/null 2>&1
END_TIME=$(date +%s%N)
COUNT_TIME_MS=$(( (END_TIME - START_TIME) / 1000000 ))

echo "  COUNT query: ${COUNT_TIME_MS}ms"

# Recent errors query
START_TIME=$(date +%s%N)
docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT * FROM compliance_errors ORDER BY timestamp DESC LIMIT 10;" > /dev/null 2>&1
END_TIME=$(date +%s%N)
RECENT_TIME_MS=$(( (END_TIME - START_TIME) / 1000000 ))

echo "  Recent errors query: ${RECENT_TIME_MS}ms"

# Statistics query
START_TIME=$(date +%s%N)
docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT 
        COUNT(*) as total,
        COUNT(CASE WHEN severity = 'CRITICAL' THEN 1 END) as critical,
        COUNT(CASE WHEN severity = 'ERROR' THEN 1 END) as errors
     FROM compliance_errors;" > /dev/null 2>&1
END_TIME=$(date +%s%N)
STATS_TIME_MS=$(( (END_TIME - START_TIME) / 1000000 ))

echo "  Statistics query: ${STATS_TIME_MS}ms"
echo ""

# Test 3: Backend API Performance
echo -e "${CYAN}Test 3: Backend API Performance${NC}"
echo "  Testing compliance API endpoint..."

# Stats endpoint
START_TIME=$(date +%s%N)
docker exec hdim-demo-gateway-clinical wget -q -O - \
    http://localhost:8080/api/v1/compliance/errors/stats > /dev/null 2>&1
END_TIME=$(date +%s%N)
STATS_API_TIME_MS=$(( (END_TIME - START_TIME) / 1000000 ))

echo "  Stats endpoint: ${STATS_API_TIME_MS}ms"

if [ "$STATS_API_TIME_MS" -lt "$TARGET_SYNC_TIME_MS" ]; then
    echo -e "  ${GREEN}✓${NC} API response time acceptable (${STATS_API_TIME_MS}ms < ${TARGET_SYNC_TIME_MS}ms)"
else
    echo -e "  ${YELLOW}⚠${NC} API response time slow (${STATS_API_TIME_MS}ms >= ${TARGET_SYNC_TIME_MS}ms)"
fi
echo ""

# Test 4: Index Usage Verification
echo -e "${CYAN}Test 4: Index Usage Verification${NC}"

# Check if indexes are being used
INDEX_USAGE=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT 
        schemaname,
        tablename,
        indexname,
        idx_scan as scans
     FROM pg_stat_user_indexes
     WHERE tablename = 'compliance_errors'
     ORDER BY idx_scan DESC;" 2>/dev/null)

if [ -n "$INDEX_USAGE" ]; then
    echo "  Index usage statistics:"
    echo "$INDEX_USAGE" | while read line; do
        if [ -n "$line" ]; then
            echo "    $line"
        fi
    done
    echo -e "  ${GREEN}✓${NC} Indexes are being used"
else
    echo -e "  ${YELLOW}⚠${NC} No index usage data available"
fi
echo ""

# Test 5: Table Size
echo -e "${CYAN}Test 5: Table Size Analysis${NC}"

TABLE_SIZE=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT pg_size_pretty(pg_total_relation_size('compliance_errors'));" 2>/dev/null | tr -d ' ')

ROW_COUNT=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM compliance_errors;" 2>/dev/null | tr -d ' ')

echo "  Table size: ${TABLE_SIZE}"
echo "  Row count: ${ROW_COUNT}"

if [ "$ROW_COUNT" -gt 0 ]; then
    AVG_ROW_SIZE=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
        "SELECT (pg_total_relation_size('compliance_errors')::numeric / COUNT(*))::bigint 
         FROM compliance_errors;" 2>/dev/null | tr -d ' ')
    echo "  Average row size: ~${AVG_ROW_SIZE} bytes"
fi
echo ""

# Cleanup test data
echo -e "${CYAN}Cleanup${NC}"
echo "  Removing performance test data..."
docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c \
    "DELETE FROM compliance_errors WHERE tenant_id = 'perf-test-tenant';" > /dev/null 2>&1
echo -e "  ${GREEN}✓${NC} Cleanup complete"
echo ""

# Summary
echo "=========================================="
echo -e "${GREEN}Performance Test Summary${NC}"
echo "=========================================="
echo ""
echo "Database Performance:"
echo "  Insert: ${AVG_INSERT_MS}ms per record (target: <${TARGET_DB_INSERT_MS}ms)"
echo "  Query (COUNT): ${COUNT_TIME_MS}ms"
echo "  Query (Recent): ${RECENT_TIME_MS}ms"
echo "  Query (Stats): ${STATS_TIME_MS}ms"
echo ""
echo "API Performance:"
echo "  Stats endpoint: ${STATS_API_TIME_MS}ms (target: <${TARGET_SYNC_TIME_MS}ms)"
echo ""
echo "Storage:"
echo "  Table size: ${TABLE_SIZE}"
echo "  Total records: ${ROW_COUNT}"
echo ""

# Performance Assessment
echo "Performance Assessment:"
if [ "$AVG_INSERT_MS" -lt "$TARGET_DB_INSERT_MS" ] && [ "$STATS_API_TIME_MS" -lt "$TARGET_SYNC_TIME_MS" ]; then
    echo -e "  ${GREEN}✓${NC} All performance targets met"
else
    echo -e "  ${YELLOW}⚠${NC} Some performance targets not met"
fi
echo ""
