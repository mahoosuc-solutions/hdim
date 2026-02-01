#!/bin/bash

# Setup Test Harness Database
# Creates the healthdata database if it doesn't exist

set -euo pipefail

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}Setting up test harness database...${NC}"

# Find PostgreSQL container
POSTGRES_CONTAINER=$(docker ps --filter "name=postgres" --format "{{.Names}}" | head -1)

if [ -z "$POSTGRES_CONTAINER" ]; then
    echo -e "${RED}✗ PostgreSQL container not found${NC}"
    echo "Please start PostgreSQL first:"
    echo "  docker-compose up -d postgres"
    exit 1
fi

echo "Found PostgreSQL container: $POSTGRES_CONTAINER"

# Determine PostgreSQL user (try healthdata first, fallback to postgres)
PG_USER="healthdata"
if ! docker exec "$POSTGRES_CONTAINER" psql -U "$PG_USER" -d postgres -c "SELECT 1;" >/dev/null 2>&1; then
    PG_USER="postgres"
fi

echo "Using PostgreSQL user: $PG_USER"

# Check if database exists
DB_EXISTS=$(docker exec "$POSTGRES_CONTAINER" psql -U "$PG_USER" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='healthdata'" 2>/dev/null || echo "0")

if [ "$DB_EXISTS" = "1" ]; then
    echo -e "${GREEN}✓ Database 'healthdata' already exists${NC}"
else
    echo "Creating database 'healthdata'..."
    if docker exec "$POSTGRES_CONTAINER" psql -U "$PG_USER" -d postgres -c "CREATE DATABASE healthdata;" 2>&1; then
        echo -e "${GREEN}✓ Database 'healthdata' created successfully${NC}"
    else
        echo -e "${RED}✗ Failed to create database${NC}"
        exit 1
    fi
fi

# List all databases
echo ""
echo "Available databases:"
docker exec "$POSTGRES_CONTAINER" psql -U "$PG_USER" -d postgres -c "\l" 2>&1 | grep -E "(Name|healthdata|postgres)" || true

echo ""
echo -e "${GREEN}Database setup complete!${NC}"
echo ""
echo "You may need to restart services that connect to this database:"
echo "  docker-compose restart care-gap-service patient-service quality-measure-service"
