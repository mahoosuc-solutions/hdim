#!/bin/bash
# Wait for Services Script
# Waits for all services to be healthy before proceeding
# Usage: ./scripts/wait-for-services.sh [timeout_seconds]

set -e

# Default timeout: 5 minutes
TIMEOUT=${1:-300}
SLEEP_INTERVAL=5

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Waiting for services to be healthy...${NC}"
echo -e "${BLUE}Timeout: ${TIMEOUT}s${NC}"
echo ""

elapsed=0

# Services to check
SERVICES=(
    "PostgreSQL|tcp|localhost:5435"
    "Redis|tcp|localhost:6380"
    "Kafka|tcp|localhost:9094"
    "Gateway Service|http|http://localhost:8080/actuator/health"
    "CQL Engine|http|http://localhost:8081/actuator/health"
    "FHIR Service|http|http://localhost:8085/actuator/health"
)

# Check if service is ready
is_service_ready() {
    local name=$1
    local type=$2
    local target=$3

    if [ "$type" = "http" ]; then
        response=$(curl -s -o /dev/null -w "%{http_code}" "$target" 2>/dev/null || echo "000")
        [ "$response" == "200" ] && return 0
    else
        local host=${target%:*}
        local port=${target##*:}
        nc -z "$host" "$port" 2>/dev/null && return 0
    fi

    return 1
}

# Wait loop
while [ $elapsed -lt $TIMEOUT ]; do
    all_ready=true

    for entry in "${SERVICES[@]}"; do
        IFS='|' read -r service type target <<< "$entry"
        if ! is_service_ready "$service" "$type" "$target"; then
            all_ready=false
            echo -e "${YELLOW}⏳ Waiting for $service... (${elapsed}s/${TIMEOUT}s)${NC}"
            break
        fi
    done

    if $all_ready; then
        echo ""
        echo -e "${GREEN}✓ All services are ready!${NC}"
        echo ""

        # Run full health check
        ./scripts/health-check.sh
        exit 0
    fi

    sleep $SLEEP_INTERVAL
    elapsed=$((elapsed + SLEEP_INTERVAL))
done

echo ""
echo -e "${RED}✗ Timeout waiting for services after ${TIMEOUT}s${NC}"
echo -e "${RED}Some services may not be ready${NC}"
echo ""
echo "Check status with: make ps"
echo "Check logs with: make logs"
exit 1
