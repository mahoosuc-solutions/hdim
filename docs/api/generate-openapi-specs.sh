#!/bin/bash

#
# OpenAPI Specification Generator for HDIM Platform v1.2.0
#
# This script fetches OpenAPI 3.0 specifications from running HDIM services
# and saves them to the docs/api directory for documentation and testing.
#
# Prerequisites:
# - Docker Compose services must be running
# - Services must have SpringDoc OpenAPI configured (springdoc-openapi-starter-webmvc-ui)
# - jq must be installed for JSON formatting (optional but recommended)
#
# Usage:
#   ./generate-openapi-specs.sh
#   ./generate-openapi-specs.sh --all           # Generate all services
#   ./generate-openapi-specs.sh --service quality-measure-service  # Specific service only
#

set -e  # Exit on error

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
VERSION="v1.2.0"
OUTPUT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$OUTPUT_DIR/../.." && pwd)"

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}HDIM Platform OpenAPI Generator${NC}"
echo -e "${BLUE}Version: $VERSION${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# Check if jq is installed
if command -v jq &> /dev/null; then
    HAS_JQ=true
    echo -e "${GREEN}✓${NC} jq detected - will format JSON output"
else
    HAS_JQ=false
    echo -e "${YELLOW}⚠${NC} jq not installed - JSON will not be formatted"
    echo -e "  Install: sudo apt install jq (Ubuntu) or brew install jq (macOS)"
fi

echo ""

# Service definitions: name, port, context-path
declare -A SERVICES=(
    ["gateway-service"]="8001|/"
    ["cql-engine-service"]="8081|/cql-engine"
    ["fhir-service"]="8085|/fhir"
    ["patient-service"]="8084|/patient"
    ["quality-measure-service"]="8087|/quality-measure"
    ["care-gap-service"]="8086|/care-gap"
)

# Parse command line arguments
SPECIFIC_SERVICE=""
if [ "$1" == "--service" ] && [ -n "$2" ]; then
    SPECIFIC_SERVICE="$2"
    echo -e "${BLUE}Mode:${NC} Generating OpenAPI spec for ${YELLOW}$SPECIFIC_SERVICE${NC} only"
else
    echo -e "${BLUE}Mode:${NC} Generating OpenAPI specs for ${YELLOW}all 6 core services${NC}"
fi

echo -e "${BLUE}Output Directory:${NC} $OUTPUT_DIR"
echo ""

# Function to check if service is running
check_service_health() {
    local service_name=$1
    local port=$2
    local context_path=$3

    # Remove leading slash from context path for health check
    local health_path="${context_path}/actuator/health"
    if [ "$context_path" == "/" ]; then
        health_path="/actuator/health"
    fi

    local health_url="http://localhost:${port}${health_path}"

    if curl -s -f -o /dev/null "$health_url"; then
        return 0  # Service is healthy
    else
        return 1  # Service is not healthy
    fi
}

# Function to generate OpenAPI spec for a service
generate_spec() {
    local service_name=$1
    local port=$2
    local context_path=$3

    echo -e "${BLUE}Processing:${NC} $service_name"
    echo -e "  Port: $port"
    echo -e "  Context Path: $context_path"

    # Check if service is running
    if ! check_service_health "$service_name" "$port" "$context_path"; then
        echo -e "  ${RED}✗ FAILED${NC} - Service not running or not healthy"
        echo -e "  ${YELLOW}→${NC} Start service: docker compose up -d $service_name"
        echo ""
        return 1
    fi

    # Construct OpenAPI endpoint URL
    local openapi_path="${context_path}/v3/api-docs"
    if [ "$context_path" == "/" ]; then
        openapi_path="/v3/api-docs"
    fi
    local openapi_url="http://localhost:${port}${openapi_path}"

    # Output file paths
    local output_json="$OUTPUT_DIR/openapi-${service_name}-${VERSION}.json"
    local output_yaml="$OUTPUT_DIR/openapi-${service_name}-${VERSION}.yaml"

    # Fetch JSON spec
    echo -e "  ${BLUE}→${NC} Fetching: $openapi_url"
    if curl -s -f "$openapi_url" > "$output_json.tmp"; then
        # Format JSON if jq is available
        if [ "$HAS_JQ" = true ]; then
            jq '.' "$output_json.tmp" > "$output_json"
            rm "$output_json.tmp"
        else
            mv "$output_json.tmp" "$output_json"
        fi

        echo -e "  ${GREEN}✓ SUCCESS${NC} - JSON saved: $(basename "$output_json")"

        # Get line count and size
        local line_count=$(wc -l < "$output_json")
        local file_size=$(du -h "$output_json" | cut -f1)
        echo -e "  ${BLUE}→${NC} Size: $file_size, Lines: $line_count"

        # Fetch YAML spec (optional)
        local openapi_yaml_url="${openapi_url}.yaml"
        if curl -s -f "$openapi_yaml_url" > "$output_yaml.tmp" 2>/dev/null; then
            mv "$output_yaml.tmp" "$output_yaml"
            echo -e "  ${GREEN}✓ SUCCESS${NC} - YAML saved: $(basename "$output_yaml")"
        else
            rm -f "$output_yaml.tmp"
            echo -e "  ${YELLOW}⚠${NC} YAML endpoint not available (optional)"
        fi

        echo ""
        return 0
    else
        rm -f "$output_json.tmp"
        echo -e "  ${RED}✗ FAILED${NC} - Could not fetch OpenAPI spec"
        echo -e "  ${YELLOW}→${NC} Check service logs: docker compose logs $service_name"
        echo ""
        return 1
    fi
}

# Main execution
SUCCESS_COUNT=0
FAILED_COUNT=0

if [ -n "$SPECIFIC_SERVICE" ]; then
    # Generate spec for specific service
    if [ -z "${SERVICES[$SPECIFIC_SERVICE]}" ]; then
        echo -e "${RED}ERROR:${NC} Unknown service: $SPECIFIC_SERVICE"
        echo -e "Available services: ${!SERVICES[@]}"
        exit 1
    fi

    IFS='|' read -r port context_path <<< "${SERVICES[$SPECIFIC_SERVICE]}"
    if generate_spec "$SPECIFIC_SERVICE" "$port" "$context_path"; then
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        FAILED_COUNT=$((FAILED_COUNT + 1))
    fi
else
    # Generate specs for all services
    for service_name in "${!SERVICES[@]}"; do
        IFS='|' read -r port context_path <<< "${SERVICES[$service_name]}"
        if generate_spec "$service_name" "$port" "$context_path"; then
            SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        else
            FAILED_COUNT=$((FAILED_COUNT + 1))
        fi
    done
fi

# Summary
echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}Generation Summary${NC}"
echo -e "${BLUE}======================================${NC}"
echo -e "${GREEN}✓ Successful:${NC} $SUCCESS_COUNT"
echo -e "${RED}✗ Failed:${NC} $FAILED_COUNT"
echo ""

if [ $SUCCESS_COUNT -gt 0 ]; then
    echo -e "${GREEN}Generated OpenAPI specifications:${NC}"
    ls -lh "$OUTPUT_DIR"/openapi-*-${VERSION}.json 2>/dev/null | awk '{print "  " $9 " (" $5 ")"}'
    echo ""
    echo -e "${BLUE}Next Steps:${NC}"
    echo -e "  1. Review generated specifications"
    echo -e "  2. Import into Postman/Insomnia for API testing"
    echo -e "  3. Generate API client libraries (optional)"
    echo -e "  4. Commit to repository for version control"
    echo ""
fi

if [ $FAILED_COUNT -gt 0 ]; then
    echo -e "${YELLOW}Troubleshooting:${NC}"
    echo -e "  • Ensure services are running: docker compose ps"
    echo -e "  • Check service health: docker compose logs <service-name>"
    echo -e "  • Verify ports are not blocked by firewall"
    echo -e "  • Ensure SpringDoc OpenAPI is configured in service"
    echo ""
fi

exit $FAILED_COUNT
