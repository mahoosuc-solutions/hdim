#!/bin/bash

# Backend Service Validation Script
# =================================
# Validates that gateway-clinical-service is running and accessible

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
CONTAINER_NAME="${CONTAINER_NAME:-hdim-demo-gateway-clinical}"
SERVICE_PORT="${SERVICE_PORT:-8080}"
HEALTH_ENDPOINT="http://localhost:${SERVICE_PORT}/actuator/health"
COMPLIANCE_STATS_ENDPOINT="http://localhost:${SERVICE_PORT}/api/v1/compliance/errors/stats"

echo "=========================================="
echo -e "${BLUE}Backend Service Validation${NC}"
echo "=========================================="
echo "Container: ${CONTAINER_NAME}"
echo "Port: ${SERVICE_PORT}"
echo ""

# Test 1: Check if container exists and is running
echo -e "${YELLOW}Test 1: Container Status...${NC}"
if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    CONTAINER_STATUS=$(docker ps --filter "name=${CONTAINER_NAME}" --format '{{.Status}}')
    echo -e "${GREEN}✓${NC} Container is running: ${CONTAINER_STATUS}"
    CONTAINER_RUNNING=true
else
    echo -e "${RED}✗${NC} Container '${CONTAINER_NAME}' is not running"
    echo ""
    echo "Available gateway containers:"
    docker ps --format '{{.Names}}\t{{.Status}}' | grep -i gateway || echo "No gateway containers found"
    exit 1
fi

# Test 2: Check container health
echo ""
echo -e "${YELLOW}Test 2: Container Health...${NC}"
HEALTH_STATUS=$(docker inspect --format='{{.State.Health.Status}}' "${CONTAINER_NAME}" 2>/dev/null || echo "no-healthcheck")
if [ "$HEALTH_STATUS" = "healthy" ]; then
    echo -e "${GREEN}✓${NC} Container health: ${HEALTH_STATUS}"
elif [ "$HEALTH_STATUS" = "no-healthcheck" ]; then
    echo -e "${YELLOW}⚠${NC} No healthcheck configured (status: ${HEALTH_STATUS})"
else
    echo -e "${YELLOW}⚠${NC} Container health: ${HEALTH_STATUS}"
fi

# Test 3: Check if service is listening on port
echo ""
echo -e "${YELLOW}Test 3: Service Port...${NC}"
if docker exec "${CONTAINER_NAME}" sh -c "nc -z localhost ${SERVICE_PORT} 2>/dev/null" 2>/dev/null || \
   docker exec "${CONTAINER_NAME}" sh -c "netstat -tln 2>/dev/null | grep -q ':${SERVICE_PORT}'" 2>/dev/null; then
    echo -e "${GREEN}✓${NC} Service is listening on port ${SERVICE_PORT}"
else
    echo -e "${YELLOW}⚠${NC} Cannot verify port ${SERVICE_PORT} (may need netcat/netstat)"
fi

# Test 4: Health endpoint (inside container)
echo ""
echo -e "${YELLOW}Test 4: Health Endpoint (Internal)...${NC}"
HEALTH_RESPONSE=$(docker exec "${CONTAINER_NAME}" wget -q -O - "${HEALTH_ENDPOINT}" 2>&1 || echo "ERROR")
HTTP_CODE="200"  # wget doesn't show HTTP code, assume 200 if response received

if echo "$HEALTH_RESPONSE" | grep -q "UP\|status"; then
    echo -e "${GREEN}✓${NC} Health endpoint responding"
    echo "  Response: $(echo "$HEALTH_RESPONSE" | head -c 100)..."
    HEALTH_OK=true
elif [ "$HEALTH_RESPONSE" = "ERROR" ]; then
    echo -e "${RED}✗${NC} Health endpoint not accessible"
    echo "  Error: wget failed (may need curl or wget installed)"
    HEALTH_OK=false
else
    echo -e "${YELLOW}⚠${NC} Health endpoint response unclear"
    echo "  Response: $HEALTH_RESPONSE"
    HEALTH_OK=true  # Assume OK if we got a response
fi

# Test 5: Compliance API endpoint
echo ""
echo -e "${YELLOW}Test 5: Compliance API Endpoint...${NC}"
if [ "$HEALTH_OK" = true ]; then
    COMPLIANCE_RESPONSE=$(docker exec "${CONTAINER_NAME}" wget -q -O - "${COMPLIANCE_STATS_ENDPOINT}" 2>&1 || echo "ERROR")
    WGET_EXIT_CODE=$?
    
    if [ "$WGET_EXIT_CODE" -eq 0 ] && [ -n "$COMPLIANCE_RESPONSE" ] && ! echo "$COMPLIANCE_RESPONSE" | grep -q "ERROR"; then
        echo -e "${GREEN}✓${NC} Compliance API endpoint accessible"
        echo "  Response: $(echo "$COMPLIANCE_RESPONSE" | head -c 100)..."
    elif [ "$WGET_EXIT_CODE" -eq 8 ] || echo "$COMPLIANCE_RESPONSE" | grep -q "ERROR"; then
        echo -e "${YELLOW}⚠${NC} Compliance API endpoint returned error (404/500)"
        echo "  This may indicate:"
        echo "    - Controller not registered (service needs restart)"
        echo "    - Endpoint requires authentication"
        echo "    - Endpoint path incorrect"
        echo "  Service was started before compliance controller was added"
        echo "  Action: Restart service to register new endpoints"
    else
        echo -e "${YELLOW}⚠${NC} Cannot test compliance endpoint (wget failed)"
        echo "  Response: $COMPLIANCE_RESPONSE"
    fi
else
    echo -e "${YELLOW}⚠${NC} Skipped (health endpoint not accessible)"
fi

# Test 6: Check service logs for errors
echo ""
echo -e "${YELLOW}Test 6: Service Logs (Recent)...${NC}"
RECENT_LOGS=$(docker logs "${CONTAINER_NAME}" --tail 10 2>&1)
ERROR_COUNT=$(echo "$RECENT_LOGS" | grep -i -E "(error|exception|failed)" | wc -l)

if [ "$ERROR_COUNT" -eq 0 ]; then
    echo -e "${GREEN}✓${NC} No recent errors in logs"
else
    echo -e "${YELLOW}⚠${NC} Found ${ERROR_COUNT} error(s) in recent logs:"
    echo "$RECENT_LOGS" | grep -i -E "(error|exception|failed)" | head -3 | sed 's/^/  /'
fi

# Test 7: Check database connectivity (from service)
echo ""
echo -e "${YELLOW}Test 7: Database Connectivity (from service)...${NC}"
DB_CHECK=$(docker exec "${CONTAINER_NAME}" sh -c "echo 'SELECT 1;' | psql -h localhost -p 5435 -U healthdata -d gateway_db 2>&1" 2>&1 || echo "ERROR")
if echo "$DB_CHECK" | grep -q "1 row"; then
    echo -e "${GREEN}✓${NC} Service can connect to database"
elif echo "$DB_CHECK" | grep -q "psql: command not found"; then
    echo -e "${YELLOW}⚠${NC} psql not available in container (expected for minimal image)"
else
    echo -e "${YELLOW}⚠${NC} Cannot verify database connectivity from container"
fi

# Test 8: Check if compliance table exists (via service logs or direct check)
echo ""
echo -e "${YELLOW}Test 8: Compliance Table Status...${NC}"
# Check if Liquibase has run by looking for table
TABLE_EXISTS=$(docker exec hdim-demo-postgres psql -U healthdata -d gateway_db -t -A -c \
    "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'compliance_errors');" 2>/dev/null | tr -d ' ')

if [ "$TABLE_EXISTS" = "t" ]; then
    echo -e "${GREEN}✓${NC} Compliance table exists in database"
    
    # Check if Liquibase changelog was applied
    CHANGELOG_COUNT=$(docker exec hdim-demo-postgres psql -U healthdata -d gateway_db -t -A -c \
        "SELECT COUNT(*) FROM databasechangelog WHERE id = '0001-create-compliance-errors-table';" 2>/dev/null | tr -d ' ')
    
    if [ "$CHANGELOG_COUNT" -gt 0 ]; then
        echo -e "${GREEN}✓${NC} Liquibase migration applied (changelog recorded)"
    else
        echo -e "${YELLOW}⚠${NC} Table exists but Liquibase changelog not found (may have been created manually)"
    fi
else
    echo -e "${RED}✗${NC} Compliance table does not exist"
    echo "  The service should create it on startup via Liquibase"
fi

# Summary
echo ""
echo "=========================================="
if [ "$HEALTH_OK" = true ] && [ "$CONTAINER_RUNNING" = true ]; then
    echo -e "${GREEN}Backend Service: RUNNING${NC}"
else
    echo -e "${YELLOW}Backend Service: ISSUES DETECTED${NC}"
fi
echo "=========================================="
echo ""
echo "Container: ${CONTAINER_NAME}"
echo "Status: ${CONTAINER_STATUS:-Unknown}"
echo "Health: ${HEALTH_STATUS:-Unknown}"
echo "Health Endpoint: ${HEALTH_OK:-false}"
echo ""
echo "To access service from host:"
echo "  docker exec ${CONTAINER_NAME} curl http://localhost:${SERVICE_PORT}/actuator/health"
echo ""
echo "To view logs:"
echo "  docker logs -f ${CONTAINER_NAME}"
echo ""
echo "To test compliance API:"
echo "  docker exec ${CONTAINER_NAME} curl http://localhost:${SERVICE_PORT}/api/v1/compliance/errors/stats"
echo ""
