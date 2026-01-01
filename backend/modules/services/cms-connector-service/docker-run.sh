#!/bin/bash

# CMS Connector Service - Docker Run Script
# Manages Docker Compose environment (dev/prod)

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT="${1:-dev}"
COMMAND="${2:-up}"

# Validate environment
if [[ ! "${ENVIRONMENT}" =~ ^(dev|prod)$ ]]; then
    echo -e "${RED}Error: Invalid environment '${ENVIRONMENT}'${NC}"
    echo "Usage: $0 [dev|prod] [up|down|logs|status]"
    exit 1
fi

# Validate command
if [[ ! "${COMMAND}" =~ ^(up|down|logs|status|build)$ ]]; then
    echo -e "${RED}Error: Invalid command '${COMMAND}'${NC}"
    echo "Usage: $0 [dev|prod] [up|down|logs|status|build]"
    exit 1
fi

# Select compose file
if [ "${ENVIRONMENT}" == "dev" ]; then
    COMPOSE_FILE="docker-compose.dev.yml"
    echo -e "${BLUE}Environment: Development${NC}"
else
    COMPOSE_FILE="docker-compose.prod.yml"
    echo -e "${BLUE}Environment: Production${NC}"
fi

# Check compose file exists
if [ ! -f "${COMPOSE_FILE}" ]; then
    echo -e "${RED}Error: ${COMPOSE_FILE} not found${NC}"
    exit 1
fi

echo ""

# Execute command
case "${COMMAND}" in
    up)
        echo -e "${YELLOW}Starting ${ENVIRONMENT} environment...${NC}"

        # Check for .env file
        if [ "${ENVIRONMENT}" == "prod" ] && [ ! -f ".env" ]; then
            echo -e "${YELLOW}Warning: .env file not found. Using default values.${NC}"
            echo "Create .env file with environment variables for production."
        fi

        docker-compose -f "${COMPOSE_FILE}" up -d

        echo ""
        echo -e "${GREEN}✓ ${ENVIRONMENT} environment started${NC}"
        echo ""

        # Show service status
        echo -e "${YELLOW}Service Status:${NC}"
        docker-compose -f "${COMPOSE_FILE}" ps
        echo ""

        if [ "${ENVIRONMENT}" == "dev" ]; then
            echo "Access points:"
            echo "  Application: http://localhost:8080"
            echo "  Actuator: http://localhost:8080/actuator"
            echo "  Database: localhost:5432"
            echo "  Redis: localhost:6379"
            echo ""
            echo "Useful commands:"
            echo "  Logs: docker-compose -f ${COMPOSE_FILE} logs -f cms-connector"
            echo "  Shell: docker-compose -f ${COMPOSE_FILE} exec cms-connector sh"
            echo "  DB: docker-compose -f ${COMPOSE_FILE} exec postgres psql -U cms_dev_user -d cms_development"
        fi
        ;;

    down)
        echo -e "${YELLOW}Stopping ${ENVIRONMENT} environment...${NC}"
        docker-compose -f "${COMPOSE_FILE}" down
        echo -e "${GREEN}✓ ${ENVIRONMENT} environment stopped${NC}"
        ;;

    logs)
        echo -e "${YELLOW}Showing ${ENVIRONMENT} logs...${NC}"
        SERVICE="${3:-cms-connector}"
        docker-compose -f "${COMPOSE_FILE}" logs -f --tail=100 "${SERVICE}"
        ;;

    status)
        echo -e "${YELLOW}${ENVIRONMENT} environment status:${NC}"
        docker-compose -f "${COMPOSE_FILE}" ps
        echo ""

        # Show application health
        if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Application is healthy${NC}"

            # Show detailed health
            HEALTH=$(curl -s http://localhost:8080/actuator/health)
            echo ""
            echo "Health Details:"
            echo "$HEALTH" | jq . 2>/dev/null || echo "$HEALTH"
        else
            echo -e "${RED}✗ Application is not responding${NC}"
        fi
        ;;

    build)
        echo -e "${YELLOW}Building Docker image...${NC}"
        docker-compose -f "${COMPOSE_FILE}" build
        echo -e "${GREEN}✓ Docker image built${NC}"
        ;;

esac

echo ""
