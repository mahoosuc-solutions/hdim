#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "================================================================"
echo "   HDIM Deployment Validation Test Harness"
echo "================================================================"
echo ""

# Load environment variables
if [ -f .env ]; then
  export $(cat .env | grep -v '^#' | xargs)
fi

# Parse arguments
TIER="all"
REPORT_ONLY=false

while [[ $# -gt 0 ]]; do
  case $1 in
    --tier)
      TIER="$2"
      shift 2
      ;;
    --report)
      REPORT_ONLY=true
      shift
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: $0 [--tier smoke|functional|integration|performance|all] [--report]"
      exit 1
      ;;
  esac
done

# Check prerequisites
echo "${BLUE}Checking prerequisites...${NC}"
echo ""

if ! command -v node &> /dev/null; then
  echo -e "${RED}✗ Node.js not found${NC}"
  echo "Please install Node.js 18+ to continue"
  exit 1
fi
echo -e "${GREEN}✓ Node.js installed${NC} ($(node --version))"

# Check if we're in the right directory
if [ ! -f "package.json" ]; then
  echo -e "${YELLOW}⚠ package.json not found - installing dependencies...${NC}"
  cd ../.. # Go back to test-harness root
fi

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
  echo ""
  echo "${BLUE}Installing dependencies...${NC}"
  npm install
fi

# Check database connectivity (optional)
if command -v psql &> /dev/null && [ ! -z "$DB_HOST" ]; then
  echo -e "${GREEN}✓ PostgreSQL client available${NC}"
else
  echo -e "${YELLOW}ℹ PostgreSQL client not available - skipping connectivity check${NC}"
fi

# Check Docker (optional)
if command -v docker &> /dev/null; then
  echo -e "${GREEN}✓ Docker installed${NC}"

  # Show running containers
  CONTAINER_COUNT=$(docker ps -q | wc -l)
  echo -e "${GREEN}✓ $CONTAINER_COUNT Docker containers running${NC}"
else
  echo -e "${YELLOW}ℹ Docker not available - skipping service checks${NC}"
fi

echo ""
echo "================================================================"
echo "   Running Validation - Tier: $TIER"
echo "================================================================"
echo ""

# Navigate to validation directory
cd "$(dirname "$0")"

# Run tests based on tier
if [ "$TIER" = "smoke" ] || [ "$TIER" = "all" ]; then
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  ${BLUE}TIER 1: SMOKE TESTS${NC} (Quick deployment health check)"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""

  npm test -- --testPathPattern="smoke-tests" --verbose || {
    echo ""
    echo -e "${RED}✗ Smoke tests failed${NC}"
    exit 1
  }

  echo ""
  echo -e "${GREEN}✓ Smoke tests passed${NC}"
  echo ""
fi

if [ "$TIER" = "functional" ] || [ "$TIER" = "all" ]; then
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  ${BLUE}TIER 2: FUNCTIONAL TESTS${NC} (Comprehensive validation)"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""

  npm test -- --testPathPattern="(multi-tenant-isolation|api-contracts|database-integrity)" --verbose || {
    echo ""
    echo -e "${YELLOW}⚠ Some functional tests failed - review output above${NC}"
  }

  echo ""
  echo -e "${GREEN}✓ Functional tests complete${NC}"
  echo ""
fi

if [ "$TIER" = "integration" ] || [ "$TIER" = "all" ]; then
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  ${BLUE}TIER 3: INTEGRATION TESTS${NC} (End-to-end workflows)"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""

  npm test -- --testPathPattern="service-integration" --verbose || {
    echo ""
    echo -e "${YELLOW}⚠ Some integration tests failed - review output above${NC}"
  }

  echo ""
  echo -e "${GREEN}✓ Integration tests complete${NC}"
  echo ""
fi

if [ "$TIER" = "performance" ] || [ "$TIER" = "all" ]; then
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  ${BLUE}TIER 4: PERFORMANCE & LOAD TESTS${NC} (Baseline metrics)"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""

  npm test -- --testPathPattern="performance-load" --verbose || {
    echo ""
    echo -e "${YELLOW}⚠ Some performance tests did not meet SLA - review output above${NC}"
  }

  echo ""
  echo -e "${GREEN}✓ Performance tests complete${NC}"
  echo ""
fi

# Summary
echo ""
echo "================================================================"
echo "   ${GREEN}VALIDATION COMPLETE${NC}"
echo "================================================================"
echo ""
echo "Test results have been generated."
echo ""
echo "Next steps:"
echo "  1. Review test output above for any failures"
echo "  2. Check detailed logs in ./reports/"
echo "  3. Address any failures before deploying to production"
echo ""
echo "For detailed documentation, see:"
echo "  - HDIM_DEPLOYMENT_VALIDATION_PROMPT.md"
echo "  - README.md"
echo ""
