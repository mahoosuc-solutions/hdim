#!/bin/bash
# Task 09: Distributed Tracing OTLP Validation
# Validates OpenTelemetry OTLP configuration and Jaeger integration across all services
set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get version from argument or environment
VERSION="${1:-${VERSION:-}}"
if [ -z "$VERSION" ]; then
    echo -e "${RED}ERROR: VERSION not specified${NC}"
    echo "Usage: $0 v1.3.0"
    exit 1
fi

echo "=========================================="
echo "Task 09: Jaeger OTLP Integration Validation"
echo "Version: $VERSION"
echo "=========================================="
echo ""

# Navigate to project root
cd "$(dirname "$0")/../.." || exit 1

# Create output directory
REPORT_DIR="docs/releases/${VERSION}/validation"
mkdir -p "$REPORT_DIR"
REPORT_FILE="$REPORT_DIR/JAEGER_INTEGRATION_VALIDATION.md"

# Initialize report
cat > "$REPORT_FILE" <<EOF
# Distributed Tracing OTLP Integration Validation Report

**Release Version:** $VERSION
**Validation Date:** $(date '+%Y-%m-%d %H:%M:%S')
**Validator:** Jaeger OTLP Validation Script

---

## Overview

This report validates OpenTelemetry OTLP configuration and Jaeger integration across all HDIM microservices.

**Required Configuration:**
- OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
- OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
- _JAVA_OPTIONS: -Djava.net.preferIPv4Stack=true
- Jaeger UI accessible at http://localhost:16686
- ≥6 services sending traces to Jaeger

---

EOF

# Track overall status
OVERALL_STATUS=0
SERVICES_WITH_OTLP=0
SERVICES_MISSING_OTLP=0
ENDPOINT_VIOLATIONS=0
PROTOCOL_VIOLATIONS=0
IPV4_MISSING=0

echo "## OTLP Configuration Validation" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "Checking docker-compose.yml for OTLP environment variables..."
echo ""

# Check if docker-compose.yml exists
if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}ERROR: docker-compose.yml not found${NC}"
    echo "- ❌ **docker-compose.yml:** Not found" >> "$REPORT_FILE"
    exit 1
fi

# Extract all service names from docker-compose.yml
SERVICES=$(grep -E "^  [a-z-]+:" docker-compose.yml | sed 's/://g' | tr -d ' ' | grep -v "^postgres$\|^redis$\|^kafka$\|^zookeeper$\|^jaeger$")

TOTAL_SERVICES=$(echo "$SERVICES" | wc -l)

echo "Found $TOTAL_SERVICES services in docker-compose.yml"
echo ""

echo "### Service-by-Service OTLP Configuration" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

for SERVICE in $SERVICES; do
    echo "Checking: $SERVICE"

    # Extract service block from docker-compose
    SERVICE_BLOCK=$(awk "/^  $SERVICE:/,/^  [a-z-]+:/" docker-compose.yml | head -n -1)

    # Check for OTEL_EXPORTER_OTLP_ENDPOINT
    if echo "$SERVICE_BLOCK" | grep -q "OTEL_EXPORTER_OTLP_ENDPOINT"; then
        ENDPOINT=$(echo "$SERVICE_BLOCK" | grep "OTEL_EXPORTER_OTLP_ENDPOINT" | awk -F': ' '{print $2}' | tr -d '\r"')

        # Validate endpoint is correct
        if [ "$ENDPOINT" = "http://jaeger:4318/v1/traces" ]; then
            ENDPOINT_STATUS="✅"
        else
            ENDPOINT_STATUS="❌"
            ENDPOINT_VIOLATIONS=$((ENDPOINT_VIOLATIONS + 1))
            OVERALL_STATUS=1
        fi
    else
        ENDPOINT="Not configured"
        ENDPOINT_STATUS="❌"
        SERVICES_MISSING_OTLP=$((SERVICES_MISSING_OTLP + 1))
        OVERALL_STATUS=1
    fi

    # Check for OTEL_EXPORTER_OTLP_PROTOCOL
    if echo "$SERVICE_BLOCK" | grep -q "OTEL_EXPORTER_OTLP_PROTOCOL"; then
        PROTOCOL=$(echo "$SERVICE_BLOCK" | grep "OTEL_EXPORTER_OTLP_PROTOCOL" | awk -F': ' '{print $2}' | tr -d '\r"')

        if [ "$PROTOCOL" = "http/protobuf" ]; then
            PROTOCOL_STATUS="✅"
        else
            PROTOCOL_STATUS="❌"
            PROTOCOL_VIOLATIONS=$((PROTOCOL_VIOLATIONS + 1))
            OVERALL_STATUS=1
        fi
    else
        PROTOCOL="Not configured"
        PROTOCOL_STATUS="❌"
        PROTOCOL_VIOLATIONS=$((PROTOCOL_VIOLATIONS + 1))
        OVERALL_STATUS=1
    fi

    # Check for _JAVA_OPTIONS with IPv4 preference
    if echo "$SERVICE_BLOCK" | grep -q "_JAVA_OPTIONS.*preferIPv4Stack=true"; then
        IPV4_STATUS="✅"
    else
        IPV4_STATUS="❌"
        IPV4_MISSING=$((IPV4_MISSING + 1))
        OVERALL_STATUS=1
    fi

    # Count services with complete OTLP config
    if [ "$ENDPOINT_STATUS" = "✅" ] && [ "$PROTOCOL_STATUS" = "✅" ] && [ "$IPV4_STATUS" = "✅" ]; then
        echo -e "${GREEN}✓ COMPLETE${NC} - $SERVICE"
        SERVICES_WITH_OTLP=$((SERVICES_WITH_OTLP + 1))

        echo "#### ✅ $SERVICE" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "- **OTLP Endpoint:** $ENDPOINT $ENDPOINT_STATUS" >> "$REPORT_FILE"
        echo "- **OTLP Protocol:** $PROTOCOL $PROTOCOL_STATUS" >> "$REPORT_FILE"
        echo "- **IPv4 Preference:** $IPV4_STATUS" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
    else
        echo -e "${RED}✗ INCOMPLETE${NC} - $SERVICE"

        echo "#### ❌ $SERVICE" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "- **OTLP Endpoint:** $ENDPOINT $ENDPOINT_STATUS" >> "$REPORT_FILE"
        echo "- **OTLP Protocol:** $PROTOCOL $PROTOCOL_STATUS" >> "$REPORT_FILE"
        echo "- **IPv4 Preference:** $IPV4_STATUS" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"

        if [ "$ENDPOINT_STATUS" = "❌" ] || [ "$PROTOCOL_STATUS" = "❌" ] || [ "$IPV4_STATUS" = "❌" ]; then
            echo "**Remediation:**" >> "$REPORT_FILE"
            echo '```yaml' >> "$REPORT_FILE"
            echo "environment:" >> "$REPORT_FILE"
            [ "$ENDPOINT_STATUS" = "❌" ] && echo "  OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces" >> "$REPORT_FILE"
            [ "$PROTOCOL_STATUS" = "❌" ] && echo "  OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf" >> "$REPORT_FILE"
            [ "$IPV4_STATUS" = "❌" ] && echo '  _JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"' >> "$REPORT_FILE"
            echo '```' >> "$REPORT_FILE"
            echo "" >> "$REPORT_FILE"
        fi
    fi
done

echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Check if Jaeger is running and accessible
echo "## Jaeger UI Accessibility" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "Checking Jaeger UI accessibility..."
echo ""

if command -v curl &> /dev/null; then
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:16686 | grep -q "200"; then
        echo -e "${GREEN}✓ Jaeger UI accessible at http://localhost:16686${NC}"
        echo "- ✅ **Jaeger UI:** Accessible at http://localhost:16686" >> "$REPORT_FILE"
        JAEGER_ACCESSIBLE=true
    else
        echo -e "${YELLOW}⚠ Jaeger UI not accessible (may not be running)${NC}"
        echo "- ⚠️ **Jaeger UI:** Not accessible (http://localhost:16686)" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "**Note:** Start services with \`docker compose up -d\` to test Jaeger integration." >> "$REPORT_FILE"
        JAEGER_ACCESSIBLE=false
    fi
else
    echo -e "${YELLOW}⚠ curl not available, skipping Jaeger UI check${NC}"
    echo "- ⚠️ **Jaeger UI:** Check skipped (curl not available)" >> "$REPORT_FILE"
    JAEGER_ACCESSIBLE=false
fi

echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Check W3C Trace Context propagation (requires services running)
echo "## Trace Propagation Verification" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

if [ "$JAEGER_ACCESSIBLE" = true ]; then
    echo "Checking for active traces in Jaeger..."

    # Query Jaeger API for services
    JAEGER_SERVICES=$(curl -s http://localhost:16686/api/services 2>/dev/null | grep -o '"[^"]*"' | tr -d '"' | grep -v "^$" || echo "")

    if [ -n "$JAEGER_SERVICES" ]; then
        SERVICE_COUNT=$(echo "$JAEGER_SERVICES" | wc -l)
        echo -e "${GREEN}✓ Found $SERVICE_COUNT services sending traces to Jaeger${NC}"

        echo "- ✅ **Active Tracing Services:** $SERVICE_COUNT services detected" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "**Services:**" >> "$REPORT_FILE"
        for SVC in $JAEGER_SERVICES; do
            echo "  - $SVC" >> "$REPORT_FILE"
        done
        echo "" >> "$REPORT_FILE"

        if [ "$SERVICE_COUNT" -ge 6 ]; then
            echo -e "${GREEN}✓ Target met: ≥6 services sending traces${NC}"
            echo "**Status:** ✅ Target met (≥6 services required)" >> "$REPORT_FILE"
        else
            echo -e "${YELLOW}⚠ Below target: Only $SERVICE_COUNT services (≥6 required)${NC}"
            echo "**Status:** ⚠️ Below target ($SERVICE_COUNT < 6 services)" >> "$REPORT_FILE"
        fi
    else
        echo -e "${YELLOW}⚠ No traces found in Jaeger${NC}"
        echo "- ⚠️ **Active Traces:** None detected" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "**Note:** Generate traffic to services to produce traces." >> "$REPORT_FILE"
    fi
else
    echo "- ℹ️ **Trace Verification:** Skipped (Jaeger not accessible)" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "**Note:** Start services and generate traffic to verify trace propagation." >> "$REPORT_FILE"
fi

echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Summary
echo ""
echo "=========================================="
echo "Validation Summary"
echo "=========================================="
echo "Total Services: $TOTAL_SERVICES"
echo "Services with Complete OTLP Config: $SERVICES_WITH_OTLP"
echo "Services Missing OTLP Config: $SERVICES_MISSING_OTLP"
echo "Endpoint Violations: $ENDPOINT_VIOLATIONS"
echo "Protocol Violations: $PROTOCOL_VIOLATIONS"
echo "IPv4 Preference Missing: $IPV4_MISSING"
echo ""

echo "## Summary" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| Metric | Count | Status |" >> "$REPORT_FILE"
echo "|--------|-------|--------|" >> "$REPORT_FILE"
echo "| Total Services | $TOTAL_SERVICES | - |" >> "$REPORT_FILE"
echo "| Complete OTLP Config | $SERVICES_WITH_OTLP | $([ $SERVICES_WITH_OTLP -eq $TOTAL_SERVICES ] && echo "✅" || echo "⚠️") |" >> "$REPORT_FILE"
echo "| Missing OTLP Config | $SERVICES_MISSING_OTLP | $([ $SERVICES_MISSING_OTLP -eq 0 ] && echo "✅" || echo "❌") |" >> "$REPORT_FILE"
echo "| Endpoint Violations | $ENDPOINT_VIOLATIONS | $([ $ENDPOINT_VIOLATIONS -eq 0 ] && echo "✅" || echo "❌") |" >> "$REPORT_FILE"
echo "| Protocol Violations | $PROTOCOL_VIOLATIONS | $([ $PROTOCOL_VIOLATIONS -eq 0 ] && echo "✅" || echo "❌") |" >> "$REPORT_FILE"
echo "| IPv4 Preference Missing | $IPV4_MISSING | $([ $IPV4_MISSING -eq 0 ] && echo "✅" || echo "❌") |" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "## References" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "- **Distributed Tracing Guide:** backend/docs/DISTRIBUTED_TRACING_GUIDE.md" >> "$REPORT_FILE"
echo "- **Jaeger UI:** http://localhost:16686" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

if [ $OVERALL_STATUS -eq 0 ]; then
    echo -e "${GREEN}✓ VALIDATION PASSED${NC}"
    echo "" >> "$REPORT_FILE"
    echo "### ✅ Overall Status: PASSED" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "All OTLP configuration checks passed. Distributed tracing is properly configured." >> "$REPORT_FILE"
else
    echo -e "${RED}✗ VALIDATION FAILED${NC}"
    echo "" >> "$REPORT_FILE"
    echo "### ❌ Overall Status: FAILED" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "OTLP configuration issues detected. Review failures above and update docker-compose.yml before release." >> "$REPORT_FILE"
fi

echo ""
echo "Report generated: $REPORT_FILE"
echo ""

exit $OVERALL_STATUS
