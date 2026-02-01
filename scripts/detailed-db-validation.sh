#!/bin/bash
# Detailed Database Validation - Lists all tables and indexes

set -e

DB_USER="healthdata"
CONTAINER="hdim-demo-postgres"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Key databases to validate
KEY_DBS=(
    "gateway_db"
    "fhir_db"
    "cql_db"
    "patient_db"
    "quality_db"
    "caregap_db"
    "event_db"
    "healthdata_demo"
)

echo "=========================================="
echo "Detailed Database Validation"
echo "=========================================="
echo ""

for db in "${KEY_DBS[@]}"; do
    echo -e "${BLUE}═══════════════════════════════════════${NC}"
    echo -e "${BLUE}Database: $db${NC}"
    echo -e "${BLUE}═══════════════════════════════════════${NC}"
    echo ""
    
    # Get table count
    TABLE_COUNT=$(docker exec $CONTAINER psql -U $DB_USER -d "$db" -tAc "SELECT COUNT(*) FROM pg_tables WHERE schemaname='public';" 2>/dev/null || echo "0")
    INDEX_COUNT=$(docker exec $CONTAINER psql -U $DB_USER -d "$db" -tAc "SELECT COUNT(*) FROM pg_indexes WHERE schemaname='public';" 2>/dev/null || echo "0")
    
    echo -e "${GREEN}Tables: $TABLE_COUNT${NC}"
    echo -e "${GREEN}Indexes: $INDEX_COUNT${NC}"
    echo ""
    
    # List all tables with row counts
    echo "Tables:"
    docker exec $CONTAINER psql -U $DB_USER -d "$db" -c "
        SELECT 
            schemaname,
            tablename,
            pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
        FROM pg_tables 
        WHERE schemaname='public' 
        ORDER BY tablename;
    " 2>/dev/null || echo "  (Unable to list tables)"
    
    echo ""
    echo "Indexes:"
    docker exec $CONTAINER psql -U $DB_USER -d "$db" -c "
        SELECT 
            schemaname,
            tablename,
            indexname,
            indexdef
        FROM pg_indexes 
        WHERE schemaname='public' 
        ORDER BY tablename, indexname
        LIMIT 50;
    " 2>/dev/null || echo "  (Unable to list indexes)"
    
    # Check for primary keys
    echo ""
    echo "Primary Keys:"
    docker exec $CONTAINER psql -U $DB_USER -d "$db" -c "
        SELECT
            tc.table_name,
            kc.column_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kc
            ON tc.constraint_name = kc.constraint_name
        WHERE tc.constraint_type = 'PRIMARY KEY'
            AND tc.table_schema = 'public'
        ORDER BY tc.table_name, kc.ordinal_position;
    " 2>/dev/null || echo "  (Unable to list primary keys)"
    
    # Check for foreign keys
    echo ""
    echo "Foreign Keys:"
    docker exec $CONTAINER psql -U $DB_USER -d "$db" -c "
        SELECT
            tc.table_name,
            kcu.column_name,
            ccu.table_name AS foreign_table_name,
            ccu.column_name AS foreign_column_name
        FROM information_schema.table_constraints AS tc
        JOIN information_schema.key_column_usage AS kcu
            ON tc.constraint_name = kcu.constraint_name
        JOIN information_schema.constraint_column_usage AS ccu
            ON ccu.constraint_name = tc.constraint_name
        WHERE tc.constraint_type = 'FOREIGN KEY'
            AND tc.table_schema = 'public'
        ORDER BY tc.table_name;
    " 2>/dev/null || echo "  (Unable to list foreign keys)"
    
    echo ""
    echo ""
done

# Summary
echo "=========================================="
echo "Summary"
echo "=========================================="

TOTAL_TABLES=0
TOTAL_INDEXES=0

for db in "${KEY_DBS[@]}"; do
    TABLE_COUNT=$(docker exec $CONTAINER psql -U $DB_USER -d "$db" -tAc "SELECT COUNT(*) FROM pg_tables WHERE schemaname='public';" 2>/dev/null || echo "0")
    INDEX_COUNT=$(docker exec $CONTAINER psql -U $DB_USER -d "$db" -tAc "SELECT COUNT(*) FROM pg_indexes WHERE schemaname='public';" 2>/dev/null || echo "0")
    
    TOTAL_TABLES=$((TOTAL_TABLES + TABLE_COUNT))
    TOTAL_INDEXES=$((TOTAL_INDEXES + INDEX_COUNT))
    
    echo "$db: $TABLE_COUNT tables, $INDEX_COUNT indexes"
done

echo ""
echo "Total: $TOTAL_TABLES tables, $TOTAL_INDEXES indexes"
echo ""
