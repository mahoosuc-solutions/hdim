#!/bin/bash

# Compliance Database Monitoring Script
# =====================================
# Monitors the compliance_errors table in real-time for new error entries

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
REFRESH_INTERVAL="${REFRESH_INTERVAL:-2}"  # seconds

# Function to get current count
get_count() {
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
        "SELECT COUNT(*) FROM compliance_errors;" 2>/dev/null | tr -d ' '
}

# Function to get recent errors
get_recent_errors() {
    local limit="${1:-10}"
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c \
        "SELECT 
            id,
            timestamp,
            tenant_id,
            service,
            operation,
            error_code,
            severity,
            LEFT(message, 50) as message_preview
        FROM compliance_errors 
        ORDER BY timestamp DESC 
        LIMIT $limit;" 2>/dev/null
}

# Function to get error statistics
get_stats() {
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c \
        "SELECT 
            COUNT(*) as total_errors,
            COUNT(DISTINCT tenant_id) as unique_tenants,
            COUNT(DISTINCT service) as unique_services,
            COUNT(CASE WHEN severity = 'CRITICAL' THEN 1 END) as critical_count,
            COUNT(CASE WHEN severity = 'ERROR' THEN 1 END) as error_count,
            COUNT(CASE WHEN severity = 'WARNING' THEN 1 END) as warning_count,
            COUNT(CASE WHEN severity = 'INFO' THEN 1 END) as info_count,
            MAX(timestamp) as latest_error
        FROM compliance_errors;" 2>/dev/null
}

# Function to get errors by service
get_errors_by_service() {
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c \
        "SELECT 
            service,
            COUNT(*) as error_count,
            COUNT(CASE WHEN severity = 'CRITICAL' THEN 1 END) as critical,
            COUNT(CASE WHEN severity = 'ERROR' THEN 1 END) as errors,
            MAX(timestamp) as latest
        FROM compliance_errors 
        GROUP BY service 
        ORDER BY error_count DESC;" 2>/dev/null
}

# Function to get errors by tenant
get_errors_by_tenant() {
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c \
        "SELECT 
            tenant_id,
            COUNT(*) as error_count,
            MAX(timestamp) as latest
        FROM compliance_errors 
        GROUP BY tenant_id 
        ORDER BY error_count DESC 
        LIMIT 10;" 2>/dev/null
}

# Function to clear screen and display header
display_header() {
    clear
    echo "=========================================="
    echo -e "${BLUE}Compliance Database Monitor${NC}"
    echo "=========================================="
    echo "Database: ${DB_NAME}"
    echo "Container: ${DB_CONTAINER}"
    echo "Refresh: Every ${REFRESH_INTERVAL}s"
    echo "Last Update: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "=========================================="
    echo ""
}

# Function to display summary
display_summary() {
    echo -e "${CYAN}=== Summary Statistics ===${NC}"
    get_stats
    echo ""
}

# Function to display recent errors
display_recent_errors() {
    echo -e "${CYAN}=== Recent Errors (Last 10) ===${NC}"
    get_recent_errors 10
    echo ""
}

# Function to display errors by service
display_errors_by_service() {
    echo -e "${CYAN}=== Errors by Service ===${NC}"
    get_errors_by_service
    echo ""
}

# Function to display errors by tenant
display_errors_by_tenant() {
    echo -e "${CYAN}=== Errors by Tenant (Top 10) ===${NC}"
    get_errors_by_tenant
    echo ""
}

# Main monitoring loop
monitor_loop() {
    local last_count=0
    
    while true; do
        display_header
        
        local current_count=$(get_count)
        local new_errors=$((current_count - last_count))
        
        if [ "$new_errors" -gt 0 ]; then
            echo -e "${GREEN}✓${NC} New errors detected: ${new_errors} (Total: ${current_count})"
        elif [ "$new_errors" -lt 0 ]; then
            echo -e "${YELLOW}⚠${NC} Error count decreased (cleanup?) (Total: ${current_count})"
        else
            echo -e "${BLUE}○${NC} No new errors (Total: ${current_count})"
        fi
        echo ""
        
        display_summary
        display_recent_errors
        display_errors_by_service
        display_errors_by_tenant
        
        echo "=========================================="
        echo -e "${YELLOW}Press Ctrl+C to stop monitoring${NC}"
        echo ""
        
        last_count=$current_count
        sleep "$REFRESH_INTERVAL"
    done
}

# Check if container exists
if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
    echo -e "${RED}Error: Container '${DB_CONTAINER}' is not running${NC}"
    echo "Available containers:"
    docker ps --format '{{.Names}}' | grep -i postgres || echo "No PostgreSQL containers found"
    exit 1
fi

# Check if database exists
if ! docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${RED}Error: Cannot connect to database '${DB_NAME}'${NC}"
    exit 1
fi

# Check if table exists
if ! docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1 FROM compliance_errors LIMIT 1;" > /dev/null 2>&1; then
    echo -e "${RED}Error: Table 'compliance_errors' does not exist${NC}"
    echo "Run: ./scripts/create-compliance-database.sql or start gateway-clinical-service"
    exit 1
fi

# Start monitoring
echo -e "${GREEN}Starting compliance database monitor...${NC}"
echo "Press Ctrl+C to stop"
echo ""
sleep 2

monitor_loop
