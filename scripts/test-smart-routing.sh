#!/bin/bash

# Smart Routing E2E Test Runner
# Tests the new gateway-edge infrastructure with smart routing

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration (production-like)
# Use --demo flag for demo compose mode (gateway-edge on port 18080)
GATEWAY_EDGE_URL="${GATEWAY_EDGE_URL:-http://localhost:8080}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8001}"
EXTERNAL_FHIR_URL="${EXTERNAL_FHIR_URL:-http://localhost:8088}"
JAEGER_URL="${JAEGER_URL:-http://localhost:16686}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Parse command line arguments FIRST (before pre-flight checks)
RUN_ALL=false
SPECIFIC_TEST=""
HEADED=false
DEBUG=false
DEMO_MODE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --all)
            RUN_ALL=true
            shift
            ;;
        --test)
            SPECIFIC_TEST="$2"
            shift 2
            ;;
        --headed)
            HEADED=true
            shift
            ;;
        --debug)
            DEBUG=true
            shift
            ;;
        --demo)
            DEMO_MODE=true
            shift
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  --all          Run all available tests"
            echo "  --test <name>  Run specific test file"
            echo "  --headed       Run in headed browser mode"
            echo "  --debug        Enable debug output"
            echo "  --demo         Use demo compose ports (gateway-edge on 18080)"
            echo "  --help         Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                           # Run smart routing tests only"
            echo "  $0 --all                     # Run all tests"
            echo "  $0 --demo                    # Run with demo compose ports"
            echo "  $0 --test smart-routing      # Run smart-routing tests"
            echo "  $0 --test gateway-auth       # Run gateway-auth tests"
            echo "  $0 --headed                  # Run with visible browser"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Apply demo mode port overrides BEFORE pre-flight checks
if [ "$DEMO_MODE" = true ]; then
    GATEWAY_EDGE_URL="http://localhost:18080"
    GATEWAY_URL="http://localhost:18080"  # Route through gateway-edge in demo mode
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   HDIM Smart Routing E2E Tests${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

if [ "$DEMO_MODE" = true ]; then
    echo -e "${YELLOW}Demo mode enabled - using demo compose ports${NC}"
    echo ""
fi

# Function to check if a URL is accessible
check_url() {
    local url=$1
    local name=$2
    if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -q "^[23]"; then
        echo -e "${GREEN}✓${NC} $name is accessible at $url"
        return 0
    else
        echo -e "${YELLOW}⚠${NC} $name is not accessible at $url"
        return 1
    fi
}

# Pre-flight checks
echo -e "${BLUE}Pre-flight Checks${NC}"
echo "-----------------------------------"

GATEWAY_EDGE_OK=false
GATEWAY_OK=false
EXTERNAL_FHIR_OK=false
JAEGER_OK=false

check_url "$GATEWAY_EDGE_URL/actuator/health" "Gateway-Edge" && GATEWAY_EDGE_OK=true
check_url "$GATEWAY_URL/actuator/health" "Gateway Service" && GATEWAY_OK=true
check_url "$EXTERNAL_FHIR_URL/fhir/metadata" "External FHIR" && EXTERNAL_FHIR_OK=true
check_url "$JAEGER_URL" "Jaeger UI" && JAEGER_OK=true

echo ""

# Determine which tests to run based on available services
TESTS_TO_RUN=()

if [ "$GATEWAY_EDGE_OK" = true ]; then
    TESTS_TO_RUN+=("smart-routing.e2e.spec.ts")
    TESTS_TO_RUN+=("gateway-auth.e2e.spec.ts")
fi

if [ "$GATEWAY_EDGE_OK" = true ] || [ "$GATEWAY_OK" = true ]; then
    TESTS_TO_RUN+=("service-health-check.e2e.spec.ts")
    TESTS_TO_RUN+=("multi-tenant-isolation.e2e.spec.ts")
fi

if [ ${#TESTS_TO_RUN[@]} -eq 0 ]; then
    echo -e "${RED}Error: No services available. Please start the Docker infrastructure.${NC}"
    echo ""
    echo "Start with:"
    echo "  docker compose --profile core up -d"
    echo "  docker compose -f docker-compose.fhir-server.yml up -d"
    echo ""
    echo "For demo mode:"
    echo "  docker compose -f docker-compose.demo.yml up -d"
    echo "  Then run: $0 --demo"
    exit 1
fi

echo -e "${BLUE}Tests to Run${NC}"
echo "-----------------------------------"
for test in "${TESTS_TO_RUN[@]}"; do
    echo "  - $test"
done
echo ""

# Build the playwright command
PLAYWRIGHT_CMD="npx playwright test"
PLAYWRIGHT_ARGS=""

if [ "$HEADED" = true ]; then
    PLAYWRIGHT_ARGS="$PLAYWRIGHT_ARGS --headed"
fi

if [ "$DEBUG" = true ]; then
    PLAYWRIGHT_ARGS="$PLAYWRIGHT_ARGS --debug"
fi

# Determine which tests to run
cd "$PROJECT_ROOT"

echo -e "${BLUE}Running Tests${NC}"
echo "-----------------------------------"

# Export environment variables for tests
export GATEWAY_EDGE_URL
export GATEWAY_URL
export EXTERNAL_FHIR_URL
export JAEGER_URL

if [ -n "$SPECIFIC_TEST" ]; then
    # Run specific test
    TEST_FILE="apps/clinical-portal-e2e/src/${SPECIFIC_TEST}.e2e.spec.ts"
    if [ ! -f "$TEST_FILE" ]; then
        TEST_FILE="apps/clinical-portal-e2e/src/${SPECIFIC_TEST}"
    fi
    echo "Running: $TEST_FILE"
    $PLAYWRIGHT_CMD "$TEST_FILE" $PLAYWRIGHT_ARGS
elif [ "$RUN_ALL" = true ]; then
    # Run all tests
    echo "Running all e2e tests..."
    $PLAYWRIGHT_CMD apps/clinical-portal-e2e/src/ $PLAYWRIGHT_ARGS
else
    # Run just the smart routing and gateway auth tests
    echo "Running smart routing and gateway auth tests..."
    $PLAYWRIGHT_CMD \
        apps/clinical-portal-e2e/src/smart-routing.e2e.spec.ts \
        apps/clinical-portal-e2e/src/gateway-auth.e2e.spec.ts \
        $PLAYWRIGHT_ARGS
fi

TEST_EXIT_CODE=$?

echo ""
echo -e "${BLUE}========================================${NC}"
if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}   Tests Passed!${NC}"
else
    echo -e "${RED}   Tests Failed (exit code: $TEST_EXIT_CODE)${NC}"
fi
echo -e "${BLUE}========================================${NC}"

# Show Jaeger link if available
if [ "$JAEGER_OK" = true ]; then
    echo ""
    echo -e "${BLUE}View traces in Jaeger:${NC} $JAEGER_URL"
fi

exit $TEST_EXIT_CODE
