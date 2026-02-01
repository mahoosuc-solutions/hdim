#!/bin/bash
# Database and Schema Validation Script
# Validates all databases, tables, and indexes for HDIM demo environment

set -e

DB_USER="healthdata"
DB_PASSWORD="demo_password_2024"
CONTAINER="hdim-demo-postgres"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=========================================="
echo "HDIM Database Validation"
echo "=========================================="
echo ""

# Expected databases from docker-compose
EXPECTED_DBS=(
    "healthdata_db"
    "gateway_db"
    "fhir_db"
    "cql_db"
    "patient_db"
    "quality_db"
    "caregap_db"
    "event_db"
    "healthdata_demo"
)

# Function to check if database exists
check_database() {
    local db_name=$1
    local exists=$(docker exec $CONTAINER psql -U $DB_USER -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$db_name'" 2>/dev/null)
    
    if [ "$exists" = "1" ]; then
        echo -e "${GREEN}✓${NC} Database: $db_name"
        return 0
    else
        echo -e "${RED}✗${NC} Database: $db_name (MISSING)"
        return 1
    fi
}

# Function to create database
create_database() {
    local db_name=$1
    echo -e "${BLUE}Creating database: $db_name${NC}"
    docker exec $CONTAINER psql -U $DB_USER -d postgres -c "CREATE DATABASE $db_name;" 2>/dev/null || true
}

# Function to list tables in database
list_tables() {
    local db_name=$1
    docker exec $CONTAINER psql -U $DB_USER -d "$db_name" -tAc "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename;" 2>/dev/null || echo ""
}

# Function to list indexes in database
list_indexes() {
    local db_name=$1
    docker exec $CONTAINER psql -U $DB_USER -d "$db_name" -tAc "SELECT indexname FROM pg_indexes WHERE schemaname='public' ORDER BY indexname;" 2>/dev/null || echo ""
}

# Function to count tables
count_tables() {
    local db_name=$1
    docker exec $CONTAINER psql -U $DB_USER -d "$db_name" -tAc "SELECT COUNT(*) FROM pg_tables WHERE schemaname='public';" 2>/dev/null || echo "0"
}

# Function to count indexes
count_indexes() {
    local db_name=$1
    docker exec $CONTAINER psql -U $DB_USER -d "$db_name" -tAc "SELECT COUNT(*) FROM pg_indexes WHERE schemaname='public';" 2>/dev/null || echo "0"
}

# Step 1: Validate/Create Databases
echo "Step 1: Validating Databases"
echo "----------------------------"
MISSING_DBS=()

for db in "${EXPECTED_DBS[@]}"; do
    if ! check_database "$db"; then
        MISSING_DBS+=("$db")
    fi
done

# Create missing databases
if [ ${#MISSING_DBS[@]} -gt 0 ]; then
    echo ""
    echo "Creating missing databases..."
    for db in "${MISSING_DBS[@]}"; do
        create_database "$db"
    done
    echo ""
    echo "Re-validating databases..."
    for db in "${EXPECTED_DBS[@]}"; do
        check_database "$db"
    done
fi

echo ""
echo "Step 2: Validating Tables and Indexes"
echo "--------------------------------------"

# Step 2: Validate Tables and Indexes for each database
TOTAL_TABLES=0
TOTAL_INDEXES=0
DATABASES_WITH_TABLES=0

for db in "${EXPECTED_DBS[@]}"; do
    if check_database "$db" > /dev/null 2>&1; then
        TABLE_COUNT=$(count_tables "$db" || echo "0")
        INDEX_COUNT=$(count_indexes "$db" || echo "0")
        
        if [ "$TABLE_COUNT" -gt 0 ]; then
            DATABASES_WITH_TABLES=$((DATABASES_WITH_TABLES + 1))
            TOTAL_TABLES=$((TOTAL_TABLES + TABLE_COUNT))
            TOTAL_INDEXES=$((TOTAL_INDEXES + INDEX_COUNT))
            
            echo ""
            echo -e "${BLUE}Database: $db${NC}"
            echo -e "  ${GREEN}✓${NC} Tables: $TABLE_COUNT"
            echo -e "  ${GREEN}✓${NC} Indexes: $INDEX_COUNT"
            
            # List tables if count is reasonable
            if [ "$TABLE_COUNT" -le 20 ]; then
                echo "  Tables:"
                list_tables "$db" | while read table; do
                    if [ ! -z "$table" ]; then
                        echo "    - $table"
                    fi
                done
            fi
        else
            echo ""
            echo -e "${YELLOW}⚠${NC} Database: $db (no tables yet - services may need to start)"
        fi
    fi
done

echo ""
echo "=========================================="
echo "Summary"
echo "=========================================="
echo "Databases: ${#EXPECTED_DBS[@]}"
echo "Databases with tables: $DATABASES_WITH_TABLES"
echo "Total tables: $TOTAL_TABLES"
echo "Total indexes: $TOTAL_INDEXES"
echo ""

# Step 3: Detailed validation for key databases
echo "Step 3: Detailed Validation"
echo "----------------------------"

# Check gateway_db (should have auth tables)
if check_database "gateway_db" > /dev/null 2>&1; then
    echo ""
    echo "Gateway Database (gateway_db):"
    GATEWAY_TABLES=$(list_tables "gateway_db")
    if echo "$GATEWAY_TABLES" | grep -q "user\|account\|role"; then
        echo -e "  ${GREEN}✓${NC} Auth tables present"
        echo "$GATEWAY_TABLES" | grep -E "user|account|role" | while read table; do
            if [ ! -z "$table" ]; then
                echo "    - $table"
            fi
        done
    else
        echo -e "  ${YELLOW}⚠${NC} Auth tables not found (may be created by services)"
    fi
fi

# Check fhir_db (should have FHIR resource tables)
if check_database "fhir_db" > /dev/null 2>&1; then
    echo ""
    echo "FHIR Database (fhir_db):"
    FHIR_TABLES=$(list_tables "fhir_db")
    if [ ! -z "$FHIR_TABLES" ]; then
        echo -e "  ${GREEN}✓${NC} FHIR tables present"
        TABLE_COUNT=$(count_tables "fhir_db")
        echo "    Total tables: $TABLE_COUNT"
    else
        echo -e "  ${YELLOW}⚠${NC} FHIR tables not found (may be created by services)"
    fi
fi

# Check patient_db
if check_database "patient_db" > /dev/null 2>&1; then
    echo ""
    echo "Patient Database (patient_db):"
    PATIENT_TABLES=$(list_tables "patient_db")
    if [ ! -z "$PATIENT_TABLES" ]; then
        echo -e "  ${GREEN}✓${NC} Patient tables present"
        TABLE_COUNT=$(count_tables "patient_db")
        echo "    Total tables: $TABLE_COUNT"
    else
        echo -e "  ${YELLOW}⚠${NC} Patient tables not found (may be created by services)"
    fi
fi

echo ""
echo "=========================================="
echo -e "${GREEN}Validation Complete${NC}"
echo "=========================================="
