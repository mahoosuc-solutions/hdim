#!/bin/bash

# HDIM UI Testing Script
# Runs automated UI tests to exercise the demo platform

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  HDIM Automated UI Testing${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Configuration
E2E_DIR="${E2E_DIR:-e2e}"
BASE_URL="${BASE_URL:-http://localhost:4200}"
API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
TEST_TYPE="${TEST_TYPE:-smoke}"

# Check if e2e directory exists
if [ ! -d "$E2E_DIR" ]; then
    echo -e "${RED}✗ E2E directory not found: $E2E_DIR${NC}"
    exit 1
fi

cd "$E2E_DIR"

# Check if dependencies are installed
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}⚠ Installing dependencies...${NC}"
    npm install
fi

# Check if Playwright browsers are installed
if [ ! -d "$HOME/.cache/ms-playwright" ]; then
    echo -e "${YELLOW}⚠ Installing Playwright browsers...${NC}"
    npx playwright install chromium || echo -e "${YELLOW}⚠ Browser installation skipped (may need sudo)${NC}"
fi

# Check service health
echo -e "${BLUE}Checking service health...${NC}"
SERVICES_HEALTHY=true

if ! curl -s "$BASE_URL" > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠ Clinical portal not accessible at $BASE_URL${NC}"
    SERVICES_HEALTHY=false
else
    echo -e "${GREEN}✓ Clinical portal accessible${NC}"
fi

# Run tests based on type
echo ""
echo -e "${BLUE}Running $TEST_TYPE tests...${NC}"
echo ""

case "$TEST_TYPE" in
    smoke)
        BASE_URL="$BASE_URL" API_BASE_URL="$API_BASE_URL" npx playwright test tests/smoke/smoke.spec.ts \
            --project=chromium \
            --reporter=list,html \
            --grep "@smoke" \
            || echo -e "${YELLOW}⚠ Some tests failed or services not ready${NC}"
        ;;
    api)
        # API health check tests only - skip global setup
        BASE_URL="$BASE_URL" API_BASE_URL="$API_BASE_URL" SKIP_GLOBAL_SETUP=true npx playwright test tests/smoke/smoke.spec.ts \
            --project=chromium \
            --reporter=list \
            --grep "SMOKE-070|SMOKE-071" \
            || echo -e "${YELLOW}⚠ API tests failed${NC}"
        ;;
    workflows)
        BASE_URL="$BASE_URL" API_BASE_URL="$API_BASE_URL" npx playwright test tests/workflows \
            --project=chromium \
            --reporter=list,html \
            || echo -e "${YELLOW}⚠ Some workflow tests failed${NC}"
        ;;
    all)
        BASE_URL="$BASE_URL" API_BASE_URL="$API_BASE_URL" npx playwright test \
            --project=chromium \
            --reporter=list,html \
            || echo -e "${YELLOW}⚠ Some tests failed${NC}"
        ;;
    *)
        echo -e "${RED}✗ Unknown test type: $TEST_TYPE${NC}"
        echo "Valid types: smoke, api, workflows, all"
        exit 1
        ;;
esac

# Show report
if [ -f "playwright-report/index.html" ]; then
    echo ""
    echo -e "${GREEN}✓ Test report generated${NC}"
    echo -e "${BLUE}View report:${NC} file://$(pwd)/playwright-report/index.html"
    echo ""
    echo -e "${BLUE}To view report:${NC}"
    echo "  npx playwright show-report"
fi

echo ""
echo -e "${GREEN}✓ UI testing completed${NC}"
echo ""
