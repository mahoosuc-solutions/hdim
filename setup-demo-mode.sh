#!/bin/bash

################################################################################
# Demo Mode Setup Script
# Purpose: Initialize demo accounts for video demonstrations
################################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
DB_CONTAINER="healthdata-postgres"
DB_NAME="healthdata_cql"
DB_USER="healthdata"
SQL_FILE="backend/modules/services/gateway-service/src/main/resources/data-demo.sql"

echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║           Health Data in Motion - Demo Mode Setup             ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}✗ Docker is not running. Please start Docker first.${NC}"
    exit 1
fi

# Check if database container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
    echo -e "${RED}✗ Database container '${DB_CONTAINER}' is not running${NC}"
    echo -e "${YELLOW}  Start it with: docker compose up -d postgres${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker and database container are running${NC}"
echo ""

# Check if SQL file exists
if [ ! -f "$SQL_FILE" ]; then
    echo -e "${RED}✗ Demo SQL file not found: $SQL_FILE${NC}"
    exit 1
fi

echo -e "${BLUE}Loading demo accounts into database...${NC}"

# Execute SQL file
if docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < "$SQL_FILE" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Demo accounts loaded successfully${NC}"
else
    echo -e "${RED}✗ Failed to load demo accounts${NC}"
    exit 1
fi

echo ""
echo -e "${CYAN}════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}                    Demo Mode Ready! 🎬                         ${NC}"
echo -e "${CYAN}════════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}All demo accounts use password: ${GREEN}demo123${NC}"
echo ""
echo -e "${CYAN}Available Demo Accounts:${NC}"
echo ""

echo -e "${BLUE}┌─────────────────────────────────────────────────────────────┐${NC}"
echo -e "${BLUE}│${NC} ${GREEN}1. Doctor / Clinical Evaluator${NC}"
echo -e "${BLUE}│${NC}    Username: ${YELLOW}demo.doctor${NC}"
echo -e "${BLUE}│${NC}    Name:     Dr. Sarah Chen"
echo -e "${BLUE}│${NC}    Role:     EVALUATOR"
echo -e "${BLUE}│${NC}    Use for:  Patient care, quality measure evaluation"
echo -e "${BLUE}└─────────────────────────────────────────────────────────────┘${NC}"
echo ""

echo -e "${BLUE}┌─────────────────────────────────────────────────────────────┐${NC}"
echo -e "${BLUE}│${NC} ${GREEN}2. Data Analyst${NC}"
echo -e "${BLUE}│${NC}    Username: ${YELLOW}demo.analyst${NC}"
echo -e "${BLUE}│${NC}    Name:     Michael Rodriguez"
echo -e "${BLUE}│${NC}    Role:     ANALYST"
echo -e "${BLUE}│${NC}    Use for:  Data analysis, reporting, metrics"
echo -e "${BLUE}└─────────────────────────────────────────────────────────────┘${NC}"
echo ""

echo -e "${BLUE}┌─────────────────────────────────────────────────────────────┐${NC}"
echo -e "${BLUE}│${NC} ${GREEN}3. Care Evaluator${NC}"
echo -e "${BLUE}│${NC}    Username: ${YELLOW}demo.care${NC}"
echo -e "${BLUE}│${NC}    Name:     Jennifer Thompson"
echo -e "${BLUE}│${NC}    Role:     EVALUATOR"
echo -e "${BLUE}│${NC}    Use for:  Care gaps, patient evaluation"
echo -e "${BLUE}└─────────────────────────────────────────────────────────────┘${NC}"
echo ""

echo -e "${BLUE}┌─────────────────────────────────────────────────────────────┐${NC}"
echo -e "${BLUE}│${NC} ${GREEN}4. System Administrator${NC}"
echo -e "${BLUE}│${NC}    Username: ${YELLOW}demo.admin${NC}"
echo -e "${BLUE}│${NC}    Name:     David Johnson"
echo -e "${BLUE}│${NC}    Role:     ADMIN"
echo -e "${BLUE}│${NC}    Use for:  Full system access, configuration"
echo -e "${BLUE}└─────────────────────────────────────────────────────────────┘${NC}"
echo ""

echo -e "${BLUE}┌─────────────────────────────────────────────────────────────┐${NC}"
echo -e "${BLUE}│${NC} ${GREEN}5. Read-Only Viewer${NC}"
echo -e "${BLUE}│${NC}    Username: ${YELLOW}demo.viewer${NC}"
echo -e "${BLUE}│${NC}    Name:     Emily Martinez"
echo -e "${BLUE}│${NC}    Role:     VIEWER"
echo -e "${BLUE}│${NC}    Use for:  Stakeholder demos, view-only access"
echo -e "${BLUE}└─────────────────────────────────────────────────────────────┘${NC}"
echo ""

echo -e "${CYAN}Quick Test:${NC}"
echo ""
echo -e "${YELLOW}curl -X POST http://localhost:9000/api/v1/auth/login \\${NC}"
echo -e "${YELLOW}  -H 'Content-Type: application/json' \\${NC}"
echo -e "${YELLOW}  -d '{\"username\":\"demo.doctor\",\"password\":\"demo123\"}'${NC}"
echo ""

echo -e "${CYAN}════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}Ready to record your demo video! 🎥${NC}"
echo -e "${CYAN}════════════════════════════════════════════════════════════════${NC}"
