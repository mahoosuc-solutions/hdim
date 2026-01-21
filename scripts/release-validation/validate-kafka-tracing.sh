#!/bin/bash
# Task 11: Kafka Trace Propagation Validation
# Validates Kafka configuration for trace propagation across all Kafka-enabled services
set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

VERSION="${1:-${VERSION:-}}"
if [ -z "$VERSION" ]; then
    echo -e "${RED}ERROR: VERSION not specified${NC}"
    echo "Usage: $0 v1.3.0"
    exit 1
fi

echo "=========================================="
echo "Task 11: Kafka Trace Propagation Validation"
echo "Version: $VERSION"
echo "=========================================="
echo ""

cd "$(dirname "$0")/../../backend" || exit 1

REPORT_DIR="../docs/releases/${VERSION}/validation"
mkdir -p "$REPORT_DIR"
REPORT_FILE="$REPORT_DIR/kafka-tracing-report.md"

cat > "$REPORT_FILE" <<EOF
# Kafka Trace Propagation Validation Report

**Release Version:** $VERSION
**Validation Date:** $(date '+%Y-%m-%d %H:%M:%S')

---

## Overview

Validates Kafka configuration for OpenTelemetry trace propagation across all 19 Kafka-enabled services.

**Required Configuration:**
- Producer: \`spring.json.add.type.headers: false\`
- Consumer: \`spring.json.use.type.headers: false\`
- Producer Interceptor: \`KafkaProducerTraceInterceptor\`
- Consumer Interceptor: \`KafkaConsumerTraceInterceptor\`

---

## Validation Results

EOF

OVERALL_STATUS=0
TYPE_HEADER_VIOLATIONS=0
INTERCEPTOR_MISSING=0

# Find all application.yml files with Kafka configuration
YML_FILES=$(find modules/services -name "application.yml" -type f | xargs grep -l "spring.kafka" | grep -v test)

echo "Checking Kafka configuration in services with Kafka..."
echo ""

for YML_FILE in $YML_FILES; do
    SERVICE=$(echo "$YML_FILE" | sed 's|modules/services/||' | cut -d/ -f1)
    echo "Checking: $SERVICE"

    # Check producer type headers
    if grep -A 20 "spring.kafka.producer" "$YML_FILE" | grep -q "spring.json.add.type.headers.*false"; then
        PRODUCER_TYPE_HEADERS="✅"
    else
        PRODUCER_TYPE_HEADERS="❌"
        TYPE_HEADER_VIOLATIONS=$((TYPE_HEADER_VIOLATIONS + 1))
        OVERALL_STATUS=1
    fi

    # Check consumer type headers
    if grep -A 20 "spring.kafka.consumer" "$YML_FILE" | grep -q "spring.json.use.type.headers.*false"; then
        CONSUMER_TYPE_HEADERS="✅"
    else
        CONSUMER_TYPE_HEADERS="❌"
        TYPE_HEADER_VIOLATIONS=$((TYPE_HEADER_VIOLATIONS + 1))
        OVERALL_STATUS=1
    fi

    # Check producer interceptor
    if grep -A 20 "spring.kafka.producer" "$YML_FILE" | grep -q "KafkaProducerTraceInterceptor"; then
        PRODUCER_INTERCEPTOR="✅"
    else
        PRODUCER_INTERCEPTOR="⚠️"
        INTERCEPTOR_MISSING=$((INTERCEPTOR_MISSING + 1))
    fi

    # Check consumer interceptor
    if grep -A 20 "spring.kafka.consumer" "$YML_FILE" | grep -q "KafkaConsumerTraceInterceptor"; then
        CONSUMER_INTERCEPTOR="✅"
    else
        CONSUMER_INTERCEPTOR="⚠️"
        INTERCEPTOR_MISSING=$((INTERCEPTOR_MISSING + 1))
    fi

    # Report results
    if [ "$PRODUCER_TYPE_HEADERS" = "✅" ] && [ "$CONSUMER_TYPE_HEADERS" = "✅" ]; then
        echo -e "${GREEN}✓ TYPE HEADERS OK${NC} - $SERVICE"
        echo "### ✅ $SERVICE" >> "$REPORT_FILE"
    else
        echo -e "${RED}✗ TYPE HEADERS FAIL${NC} - $SERVICE"
        echo "### ❌ $SERVICE" >> "$REPORT_FILE"
    fi

    echo "" >> "$REPORT_FILE"
    echo "- **Producer Type Headers Disabled:** $PRODUCER_TYPE_HEADERS" >> "$REPORT_FILE"
    echo "- **Consumer Type Headers Disabled:** $CONSUMER_TYPE_HEADERS" >> "$REPORT_FILE"
    echo "- **Producer Trace Interceptor:** $PRODUCER_INTERCEPTOR" >> "$REPORT_FILE"
    echo "- **Consumer Trace Interceptor:** $CONSUMER_INTERCEPTOR" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"

    if [ "$PRODUCER_TYPE_HEADERS" != "✅" ] || [ "$CONSUMER_TYPE_HEADERS" != "✅" ]; then
        echo "**Remediation:**" >> "$REPORT_FILE"
        echo '```yaml' >> "$REPORT_FILE"
        echo "spring:" >> "$REPORT_FILE"
        echo "  kafka:" >> "$REPORT_FILE"
        echo "    producer:" >> "$REPORT_FILE"
        echo "      properties:" >> "$REPORT_FILE"
        echo "        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException" >> "$REPORT_FILE"
        echo "        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor" >> "$REPORT_FILE"
        echo "    consumer:" >> "$REPORT_FILE"
        echo "      properties:" >> "$REPORT_FILE"
        echo "        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException" >> "$REPORT_FILE"
        echo "        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor" >> "$REPORT_FILE"
        echo '```' >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
    fi
done

echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Check logs for ClassNotFoundException errors
echo "## ClassNotFoundException Check" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "Checking service logs for ClassNotFoundException errors..."

if command -v docker &> /dev/null && docker compose ps &> /dev/null 2>&1; then
    # Get logs from all Kafka services
    KAFKA_SERVICES=$(docker compose ps --services | grep -E "event|kafka" || echo "")

    if [ -n "$KAFKA_SERVICES" ]; then
        ERRORS_FOUND=false

        for SVC in $KAFKA_SERVICES; do
            if docker compose logs "$SVC" 2>/dev/null | grep -q "ClassNotFoundException"; then
                echo -e "${RED}✗ ClassNotFoundException found in $SVC logs${NC}"
                echo "- ❌ **$SVC:** ClassNotFoundException detected" >> "$REPORT_FILE"
                ERRORS_FOUND=true
                OVERALL_STATUS=1
            fi
        done

        if [ "$ERRORS_FOUND" = false ]; then
            echo -e "${GREEN}✓ No ClassNotFoundException errors in logs${NC}"
            echo "- ✅ **Logs:** No ClassNotFoundException errors detected" >> "$REPORT_FILE"
        fi
    else
        echo -e "${YELLOW}⚠ No Kafka services running${NC}"
        echo "- ℹ️ **Logs:** No Kafka services running (check skipped)" >> "$REPORT_FILE"
    fi
else
    echo -e "${YELLOW}⚠ Docker not available${NC}"
    echo "- ℹ️ **Logs:** Docker not available (check skipped)" >> "$REPORT_FILE"
fi

echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Summary
echo ""
echo "=========================================="
echo "Validation Summary"
echo "=========================================="
echo "Type Header Violations: $TYPE_HEADER_VIOLATIONS"
echo "Interceptor Missing: $INTERCEPTOR_MISSING"
echo ""

echo "## Summary" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| Check | Status |" >> "$REPORT_FILE"
echo "|-------|--------|" >> "$REPORT_FILE"
echo "| Type Headers Disabled | $([ $TYPE_HEADER_VIOLATIONS -eq 0 ] && echo "✅ PASS" || echo "❌ FAIL ($TYPE_HEADER_VIOLATIONS violations)") |" >> "$REPORT_FILE"
echo "| Trace Interceptors | $([ $INTERCEPTOR_MISSING -eq 0 ] && echo "✅ CONFIGURED" || echo "⚠️ WARN ($INTERCEPTOR_MISSING missing)") |" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "## References" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "- **Distributed Tracing Guide:** backend/docs/DISTRIBUTED_TRACING_GUIDE.md" >> "$REPORT_FILE"
echo "- **Kafka Config:** CLAUDE.md (Kafka section)" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

if [ $OVERALL_STATUS -eq 0 ]; then
    echo -e "${GREEN}✓ VALIDATION PASSED${NC}"
    echo "### ✅ Overall Status: PASSED" >> "$REPORT_FILE"
else
    echo -e "${RED}✗ VALIDATION FAILED${NC}"
    echo "### ❌ Overall Status: FAILED" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "Kafka configuration issues detected. Type headers MUST be disabled to prevent ClassNotFoundException errors." >> "$REPORT_FILE"
fi

echo ""
echo "Report generated: $REPORT_FILE"
echo ""

exit $OVERALL_STATUS
