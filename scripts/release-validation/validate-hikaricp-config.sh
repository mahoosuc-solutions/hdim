#!/bin/bash
# Task 10: HikariCP Connection Pool Validation
# Validates HikariCP timing formula and traffic tier configuration across all services
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
echo "Task 10: HikariCP Connection Pool Validation"
echo "Version: $VERSION"
echo "=========================================="
echo ""

# Navigate to backend directory
cd "$(dirname "$0")/../../backend" || exit 1

# Create output directory
REPORT_DIR="../docs/releases/${VERSION}/validation"
mkdir -p "$REPORT_DIR"
REPORT_FILE="$REPORT_DIR/hikaricp-config-report.md"

# Initialize report
cat > "$REPORT_FILE" <<EOF
# HikariCP Connection Pool Configuration Validation Report

**Release Version:** $VERSION
**Validation Date:** $(date '+%Y-%m-%d %H:%M:%S')
**Validator:** HikariCP Validation Script

---

## Overview

This report validates HikariCP connection pool configuration across all HDIM microservices.

**Timing Formula Requirement:**
\`\`\`
max-lifetime ≥ 6 × idle-timeout
\`\`\`

**Standard Configuration:**
- idle-timeout: 300,000ms (5 minutes)
- max-lifetime: 1,800,000ms (30 minutes) - 6× idle-timeout
- keepalive-time: 240,000ms (4 minutes) - must be < idle-timeout

**Traffic Tier Pool Sizes:**
- HIGH: maximum-pool-size=50
- MEDIUM: maximum-pool-size=20
- LOW: maximum-pool-size=10

---

EOF

# Track overall status
OVERALL_STATUS=0
TOTAL_SERVICES=0
TIMING_VIOLATIONS=0
POOL_SIZE_VIOLATIONS=0
KEEPALIVE_VIOLATIONS=0
LEAK_DETECTION_MISSING=0

echo "## Service-by-Service Validation" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "Checking HikariCP configuration in all services..."
echo ""

# Find all application.yml files in services
YML_FILES=$(find modules/services -name "application.yml" -type f | grep -v "test")

if [ -z "$YML_FILES" ]; then
    echo -e "${RED}ERROR: No application.yml files found${NC}"
    exit 1
fi

for YML_FILE in $YML_FILES; do
    # Extract service name
    SERVICE=$(echo "$YML_FILE" | sed 's|modules/services/||' | cut -d/ -f1)
    TOTAL_SERVICES=$((TOTAL_SERVICES + 1))

    echo "Checking: $SERVICE"

    # Extract HikariCP configuration
    if grep -q "hikari:" "$YML_FILE"; then
        # Extract values
        IDLE_TIMEOUT=$(grep -A 20 "hikari:" "$YML_FILE" | grep "idle-timeout:" | head -1 | awk '{print $2}' | tr -d '\r' || echo "0")
        MAX_LIFETIME=$(grep -A 20 "hikari:" "$YML_FILE" | grep "max-lifetime:" | head -1 | awk '{print $2}' | tr -d '\r' || echo "0")
        KEEPALIVE_TIME=$(grep -A 20 "hikari:" "$YML_FILE" | grep "keepalive-time:" | head -1 | awk '{print $2}' | tr -d '\r' || echo "0")
        MAX_POOL_SIZE=$(grep -A 20 "hikari:" "$YML_FILE" | grep "maximum-pool-size:" | head -1 | awk '{print $2}' | tr -d '\r' || echo "0")
        LEAK_THRESHOLD=$(grep -A 20 "hikari:" "$YML_FILE" | grep "leak-detection-threshold:" | head -1 | awk '{print $2}' | tr -d '\r' || echo "0")

        # Determine traffic tier from docs or naming convention
        TRAFFIC_TIER="UNKNOWN"
        if echo "$SERVICE" | grep -qE "patient|fhir|quality|cql|gateway"; then
            TRAFFIC_TIER="HIGH"
            EXPECTED_POOL_SIZE=50
        elif echo "$SERVICE" | grep -qE "care-gap|analytics|ehr-connector"; then
            TRAFFIC_TIER="MEDIUM"
            EXPECTED_POOL_SIZE=20
        else
            TRAFFIC_TIER="LOW"
            EXPECTED_POOL_SIZE=10
        fi

        # Validate timing formula: max-lifetime >= 6 * idle-timeout
        if [ "$IDLE_TIMEOUT" -gt 0 ] && [ "$MAX_LIFETIME" -gt 0 ]; then
            REQUIRED_MAX_LIFETIME=$((IDLE_TIMEOUT * 6))

            if [ "$MAX_LIFETIME" -ge "$REQUIRED_MAX_LIFETIME" ]; then
                TIMING_STATUS="✅"
                echo -e "${GREEN}✓ TIMING OK${NC} - $SERVICE (max-lifetime: ${MAX_LIFETIME}ms ≥ 6×${IDLE_TIMEOUT}ms)"
            else
                TIMING_STATUS="❌"
                echo -e "${RED}✗ TIMING FAIL${NC} - $SERVICE (max-lifetime: ${MAX_LIFETIME}ms < 6×${IDLE_TIMEOUT}ms = ${REQUIRED_MAX_LIFETIME}ms)"
                TIMING_VIOLATIONS=$((TIMING_VIOLATIONS + 1))
                OVERALL_STATUS=1
            fi
        else
            TIMING_STATUS="⚠️"
            echo -e "${YELLOW}⚠ TIMING UNKNOWN${NC} - $SERVICE (idle-timeout or max-lifetime not configured)"
            TIMING_VIOLATIONS=$((TIMING_VIOLATIONS + 1))
            OVERALL_STATUS=1
        fi

        # Validate keepalive-time < idle-timeout
        if [ "$KEEPALIVE_TIME" -gt 0 ] && [ "$IDLE_TIMEOUT" -gt 0 ]; then
            if [ "$KEEPALIVE_TIME" -lt "$IDLE_TIMEOUT" ]; then
                KEEPALIVE_STATUS="✅"
            else
                KEEPALIVE_STATUS="❌"
                echo -e "${RED}✗ KEEPALIVE FAIL${NC} - $SERVICE (keepalive: ${KEEPALIVE_TIME}ms ≥ idle-timeout: ${IDLE_TIMEOUT}ms)"
                KEEPALIVE_VIOLATIONS=$((KEEPALIVE_VIOLATIONS + 1))
                OVERALL_STATUS=1
            fi
        else
            KEEPALIVE_STATUS="⚠️"
            KEEPALIVE_VIOLATIONS=$((KEEPALIVE_VIOLATIONS + 1))
        fi

        # Validate pool size matches traffic tier
        if [ "$MAX_POOL_SIZE" -eq "$EXPECTED_POOL_SIZE" ] 2>/dev/null; then
            POOL_SIZE_STATUS="✅"
        else
            POOL_SIZE_STATUS="⚠️"
            echo -e "${YELLOW}⚠ POOL SIZE${NC} - $SERVICE ($TRAFFIC_TIER tier: expected $EXPECTED_POOL_SIZE, got $MAX_POOL_SIZE)"
            POOL_SIZE_VIOLATIONS=$((POOL_SIZE_VIOLATIONS + 1))
        fi

        # Validate leak detection configured
        if [ "$LEAK_THRESHOLD" -eq 60000 ] 2>/dev/null; then
            LEAK_STATUS="✅"
        else
            LEAK_STATUS="⚠️"
            LEAK_DETECTION_MISSING=$((LEAK_DETECTION_MISSING + 1))
        fi

        # Write to report
        if [ "$TIMING_STATUS" = "✅" ] && [ "$KEEPALIVE_STATUS" = "✅" ] && [ "$POOL_SIZE_STATUS" = "✅" ] && [ "$LEAK_STATUS" = "✅" ]; then
            echo "### ✅ $SERVICE" >> "$REPORT_FILE"
        else
            echo "### ⚠️ $SERVICE" >> "$REPORT_FILE"
        fi

        echo "" >> "$REPORT_FILE"
        echo "- **Traffic Tier:** $TRAFFIC_TIER" >> "$REPORT_FILE"
        echo "- **Idle Timeout:** ${IDLE_TIMEOUT}ms" >> "$REPORT_FILE"
        echo "- **Max Lifetime:** ${MAX_LIFETIME}ms $TIMING_STATUS" >> "$REPORT_FILE"
        echo "- **Keepalive Time:** ${KEEPALIVE_TIME}ms $KEEPALIVE_STATUS" >> "$REPORT_FILE"
        echo "- **Max Pool Size:** $MAX_POOL_SIZE (expected: $EXPECTED_POOL_SIZE) $POOL_SIZE_STATUS" >> "$REPORT_FILE"
        echo "- **Leak Detection:** ${LEAK_THRESHOLD}ms $LEAK_STATUS" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"

        # Add remediation if violations detected
        if [ "$TIMING_STATUS" != "✅" ] || [ "$KEEPALIVE_STATUS" != "✅" ] || [ "$POOL_SIZE_STATUS" != "✅" ] || [ "$LEAK_STATUS" != "✅" ]; then
            echo "**Remediation:**" >> "$REPORT_FILE"
            echo '```yaml' >> "$REPORT_FILE"
            echo "healthdata:" >> "$REPORT_FILE"
            echo "  database:" >> "$REPORT_FILE"
            echo "    hikari:" >> "$REPORT_FILE"
            [ "$TIMING_STATUS" != "✅" ] && echo "      idle-timeout: 300000        # 5 minutes" >> "$REPORT_FILE"
            [ "$TIMING_STATUS" != "✅" ] && echo "      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)" >> "$REPORT_FILE"
            [ "$KEEPALIVE_STATUS" != "✅" ] && echo "      keepalive-time: 240000      # 4 minutes (< idle-timeout)" >> "$REPORT_FILE"
            [ "$POOL_SIZE_STATUS" != "✅" ] && echo "      maximum-pool-size: $EXPECTED_POOL_SIZE  # $TRAFFIC_TIER traffic tier" >> "$REPORT_FILE"
            [ "$LEAK_STATUS" != "✅" ] && echo "      leak-detection-threshold: 60000  # 60 seconds" >> "$REPORT_FILE"
            echo '```' >> "$REPORT_FILE"
            echo "" >> "$REPORT_FILE"
        fi

    else
        echo -e "${YELLOW}⚠ NO HIKARI CONFIG${NC} - $SERVICE"
        echo "### ⚠️ $SERVICE" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "- **HikariCP Configuration:** Not found" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "**Note:** If this service uses a database, configure HikariCP in application.yml." >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        TIMING_VIOLATIONS=$((TIMING_VIOLATIONS + 1))
    fi
done

echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Summary
echo ""
echo "=========================================="
echo "Validation Summary"
echo "=========================================="
echo "Total Services: $TOTAL_SERVICES"
echo "Timing Formula Violations: $TIMING_VIOLATIONS"
echo "Pool Size Mismatches: $POOL_SIZE_VIOLATIONS"
echo "Keepalive Violations: $KEEPALIVE_VIOLATIONS"
echo "Leak Detection Missing: $LEAK_DETECTION_MISSING"
echo ""

echo "## Summary" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| Metric | Count | Status |" >> "$REPORT_FILE"
echo "|--------|-------|--------|" >> "$REPORT_FILE"
echo "| Total Services | $TOTAL_SERVICES | - |" >> "$REPORT_FILE"
echo "| Timing Formula Violations | $TIMING_VIOLATIONS | $([ $TIMING_VIOLATIONS -eq 0 ] && echo "✅" || echo "❌") |" >> "$REPORT_FILE"
echo "| Pool Size Mismatches | $POOL_SIZE_VIOLATIONS | $([ $POOL_SIZE_VIOLATIONS -eq 0 ] && echo "✅" || echo "⚠️") |" >> "$REPORT_FILE"
echo "| Keepalive Violations | $KEEPALIVE_VIOLATIONS | $([ $KEEPALIVE_VIOLATIONS -eq 0 ] && echo "✅" || echo "❌") |" >> "$REPORT_FILE"
echo "| Leak Detection Missing | $LEAK_DETECTION_MISSING | $([ $LEAK_DETECTION_MISSING -eq 0 ] && echo "✅" || echo "⚠️") |" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "## HikariCP Timing Formula" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "The timing formula prevents connection pool exhaustion:" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo '```' >> "$REPORT_FILE"
echo "max-lifetime ≥ 6 × idle-timeout" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "Standard values:" >> "$REPORT_FILE"
echo "- idle-timeout:   300,000ms (5 minutes)" >> "$REPORT_FILE"
echo "- max-lifetime: 1,800,000ms (30 minutes)" >> "$REPORT_FILE"
echo "- keepalive-time: 240,000ms (4 minutes, < idle-timeout)" >> "$REPORT_FILE"
echo '```' >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "## References" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "- **Database Config Guide:** backend/docs/DATABASE_CONFIG_ADOPTION_GUIDE.md" >> "$REPORT_FILE"
echo "- **HikariCP Documentation:** backend/modules/shared/infrastructure/database-config/README.md" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

if [ $OVERALL_STATUS -eq 0 ]; then
    echo -e "${GREEN}✓ VALIDATION PASSED${NC}"
    echo "" >> "$REPORT_FILE"
    echo "### ✅ Overall Status: PASSED" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "All HikariCP configuration checks passed. Connection pool configuration is optimal." >> "$REPORT_FILE"
else
    echo -e "${RED}✗ VALIDATION FAILED${NC}"
    echo "" >> "$REPORT_FILE"
    echo "### ❌ Overall Status: FAILED" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "HikariCP configuration issues detected. Review violations above and update application.yml files before release." >> "$REPORT_FILE"
fi

echo ""
echo "Report generated: $REPORT_FILE"
echo ""

exit $OVERALL_STATUS
