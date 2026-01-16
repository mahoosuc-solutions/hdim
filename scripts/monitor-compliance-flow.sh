#!/bin/bash

# Compliance Error Flow Monitoring Script
# ========================================
# Monitors the complete error flow from frontend to database

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
REFRESH_INTERVAL="${REFRESH_INTERVAL:-3}"

echo "=========================================="
echo -e "${BLUE}Compliance Error Flow Monitor${NC}"
echo "=========================================="
echo ""
echo "Monitoring complete error flow:"
echo "  Frontend → Backend → Database"
echo ""
echo "Press Ctrl+C to stop"
echo ""

# Function to get localStorage errors count (via browser console simulation)
# Note: This is a placeholder - actual implementation would need browser automation
get_frontend_error_count() {
    echo "N/A (requires browser DevTools)"
}

# Function to check backend sync endpoint
check_backend_sync() {
    local response=$(docker exec hdim-demo-gateway-clinical wget -q -O - \
        http://localhost:8080/api/v1/compliance/errors/stats 2>/dev/null || echo "{}")
    
    if echo "$response" | grep -q "total"; then
        local total=$(echo "$response" | grep -o '"total":[0-9]*' | cut -d: -f2)
        echo "${total:-0}"
    else
        echo "0"
    fi
}

# Function to get database error count
get_database_count() {
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
        "SELECT COUNT(*) FROM compliance_errors;" 2>/dev/null | tr -d ' '
}

# Function to get recent database errors
get_recent_errors() {
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c \
        "SELECT timestamp, service, error_code, severity, LEFT(message, 40) as message 
         FROM compliance_errors 
         ORDER BY timestamp DESC 
         LIMIT 5;" 2>/dev/null
}

# Function to check backend logs for sync activity
check_backend_sync_logs() {
    docker logs hdim-demo-gateway-clinical --tail 20 2>&1 | \
        grep -i -E "(compliance|sync|error)" | tail -3 || echo "No recent sync activity"
}

# Main monitoring loop
monitor_loop() {
    local last_db_count=0
    
    while true; do
        clear
        echo "=========================================="
        echo -e "${BLUE}Compliance Error Flow Monitor${NC}"
        echo "=========================================="
        echo "Last Update: $(date '+%Y-%m-%d %H:%M:%S')"
        echo ""
        
        # Frontend (placeholder)
        echo -e "${CYAN}1. Frontend (LocalStorage)${NC}"
        echo "   Status: Check browser DevTools → Application → Local Storage"
        echo "   Key: hdim-error-validation"
        echo ""
        
        # Backend Stats
        echo -e "${CYAN}2. Backend API${NC}"
        local backend_count=$(check_backend_sync)
        echo "   Total errors (from API): ${backend_count}"
        echo ""
        
        # Database
        echo -e "${CYAN}3. Database${NC}"
        local db_count=$(get_database_count)
        local new_errors=$((db_count - last_db_count))
        
        if [ "$new_errors" -gt 0 ]; then
            echo -e "   Total errors: ${db_count} ${GREEN}(+${new_errors} new)${NC}"
        else
            echo "   Total errors: ${db_count}"
        fi
        
        echo ""
        echo "   Recent errors:"
        get_recent_errors | head -8
        echo ""
        
        # Backend Logs
        echo -e "${CYAN}4. Backend Sync Activity${NC}"
        check_backend_sync_logs | sed 's/^/   /'
        echo ""
        
        # Flow Status
        echo -e "${CYAN}5. Flow Status${NC}"
        if [ "$db_count" -gt 0 ]; then
            echo -e "   ${GREEN}✓${NC} Errors flowing: Frontend → Backend → Database"
        else
            echo -e "   ${YELLOW}○${NC} Waiting for errors..."
        fi
        
        echo ""
        echo "=========================================="
        echo -e "${YELLOW}Press Ctrl+C to stop${NC}"
        echo ""
        
        last_db_count=$db_count
        sleep "$REFRESH_INTERVAL"
    done
}

# Check prerequisites
if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
    echo -e "${RED}Error: Database container '${DB_CONTAINER}' not running${NC}"
    exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -q "hdim-demo-gateway-clinical"; then
    echo -e "${RED}Error: Backend container 'hdim-demo-gateway-clinical' not running${NC}"
    exit 1
fi

# Start monitoring
monitor_loop
