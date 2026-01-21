#!/bin/bash
# Task 01: Entity-Migration Synchronization Validation
# Validates that all JPA entities have corresponding Liquibase migrations across all 34 microservices
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
echo "Task 01: Entity-Migration Sync Validation"
echo "Version: $VERSION"
echo "=========================================="
echo ""

# Navigate to backend directory
cd "$(dirname "$0")/../../backend" || exit 1

# Create output directory
REPORT_DIR="../docs/releases/${VERSION}/validation"
mkdir -p "$REPORT_DIR"
REPORT_FILE="$REPORT_DIR/entity-migration-report.md"

# Initialize report
cat > "$REPORT_FILE" <<EOF
# Entity-Migration Synchronization Validation Report

**Release Version:** $VERSION
**Validation Date:** $(date '+%Y-%m-%d %H:%M:%S')
**Validator:** Entity-Migration Sync Script

---

## Overview

This report validates that all JPA entities have corresponding Liquibase migrations across all HDIM microservices, ensuring zero schema drift in production.

---

## Validation Results

EOF

# Track overall status
OVERALL_STATUS=0
TOTAL_SERVICES=0
PASSED_SERVICES=0
FAILED_SERVICES=0

echo "Running EntityMigrationValidationTest for all services..."
echo ""

# Find all services with EntityMigrationValidationTest
SERVICES=$(find modules/services -name "*EntityMigrationValidationTest.java" -type f | sed 's|modules/services/||' | sed 's|/src/.*||' | sort -u)

if [ -z "$SERVICES" ]; then
    echo -e "${YELLOW}WARNING: No EntityMigrationValidationTest found in any service${NC}"
    echo ""
    echo "### ⚠️ No Validation Tests Found" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "No EntityMigrationValidationTest classes found in any service." >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    exit 1
fi

echo "### Service-by-Service Results" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Test each service
for SERVICE in $SERVICES; do
    TOTAL_SERVICES=$((TOTAL_SERVICES + 1))
    echo "Testing: $SERVICE"

    # Run the validation test
    if ./gradlew :modules:services:${SERVICE}:test --tests "*EntityMigrationValidationTest" -q 2>&1 | tee /tmp/test-output-${SERVICE}.log | grep -q "BUILD SUCCESSFUL"; then
        echo -e "${GREEN}✓ PASS${NC} - $SERVICE"
        PASSED_SERVICES=$((PASSED_SERVICES + 1))

        echo "#### ✅ $SERVICE" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "- **Status:** PASSED" >> "$REPORT_FILE"
        echo "- **Entity-Migration Sync:** Validated" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
    else
        echo -e "${RED}✗ FAIL${NC} - $SERVICE"
        FAILED_SERVICES=$((FAILED_SERVICES + 1))
        OVERALL_STATUS=1

        echo "#### ❌ $SERVICE" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "- **Status:** FAILED" >> "$REPORT_FILE"
        echo "- **Error Details:**" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo '```' >> "$REPORT_FILE"
        tail -20 /tmp/test-output-${SERVICE}.log >> "$REPORT_FILE" 2>/dev/null || echo "No error details available" >> "$REPORT_FILE"
        echo '```' >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "- **Remediation:**" >> "$REPORT_FILE"
        echo "  1. Review entity annotations vs migration column types" >> "$REPORT_FILE"
        echo "  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping" >> "$REPORT_FILE"
        echo "  3. Update entity OR migration to match" >> "$REPORT_FILE"
        echo "  4. Re-run: \`./gradlew :modules:services:${SERVICE}:test --tests \"*EntityMigrationValidationTest\"\`" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
    fi

    # Clean up temp file
    rm -f /tmp/test-output-${SERVICE}.log
done

echo ""
echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Check ddl-auto settings
echo "Checking ddl-auto settings in application.yml files..."
echo ""

echo "### DDL Auto Configuration Check" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

DDL_AUTO_VIOLATIONS=0

for YML_FILE in $(find modules/services -name "application*.yml" -type f); do
    if grep -q "ddl-auto:" "$YML_FILE"; then
        DDL_AUTO_VALUE=$(grep "ddl-auto:" "$YML_FILE" | awk '{print $2}' | tr -d '\r')

        if [ "$DDL_AUTO_VALUE" != "validate" ]; then
            echo -e "${RED}✗ VIOLATION${NC} - $YML_FILE has ddl-auto: $DDL_AUTO_VALUE (must be 'validate')"
            DDL_AUTO_VIOLATIONS=$((DDL_AUTO_VIOLATIONS + 1))
            OVERALL_STATUS=1

            echo "- ❌ \`$YML_FILE\`: **ddl-auto: $DDL_AUTO_VALUE** (MUST be 'validate')" >> "$REPORT_FILE"
        else
            echo -e "${GREEN}✓ OK${NC} - $YML_FILE has ddl-auto: validate"
        fi
    fi
done

if [ $DDL_AUTO_VIOLATIONS -eq 0 ]; then
    echo -e "${GREEN}✓ All application.yml files have ddl-auto: validate${NC}"
    echo "" >> "$REPORT_FILE"
    echo "✅ **All services correctly configured with ddl-auto: validate**" >> "$REPORT_FILE"
else
    echo -e "${RED}✗ Found $DDL_AUTO_VIOLATIONS ddl-auto violations${NC}"
    echo "" >> "$REPORT_FILE"
    echo "**Remediation:**" >> "$REPORT_FILE"
    echo "- Change all ddl-auto values to 'validate' in the files listed above" >> "$REPORT_FILE"
    echo "- NEVER use 'create' or 'update' (causes data loss and schema drift)" >> "$REPORT_FILE"
fi

echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Summary
echo ""
echo "=========================================="
echo "Validation Summary"
echo "=========================================="
echo "Total Services Tested: $TOTAL_SERVICES"
echo "Passed: $PASSED_SERVICES"
echo "Failed: $FAILED_SERVICES"
echo "DDL Auto Violations: $DDL_AUTO_VIOLATIONS"

echo "## Summary" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| Metric | Count |" >> "$REPORT_FILE"
echo "|--------|-------|" >> "$REPORT_FILE"
echo "| Total Services Tested | $TOTAL_SERVICES |" >> "$REPORT_FILE"
echo "| Passed | $PASSED_SERVICES |" >> "$REPORT_FILE"
echo "| Failed | $FAILED_SERVICES |" >> "$REPORT_FILE"
echo "| DDL Auto Violations | $DDL_AUTO_VIOLATIONS |" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

if [ $OVERALL_STATUS -eq 0 ]; then
    echo -e "${GREEN}✓ VALIDATION PASSED${NC}"
    echo "" >> "$REPORT_FILE"
    echo "### ✅ Overall Status: PASSED" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "All entity-migration synchronization checks passed. Schema is ready for production deployment." >> "$REPORT_FILE"
else
    echo -e "${RED}✗ VALIDATION FAILED${NC}"
    echo "" >> "$REPORT_FILE"
    echo "### ❌ Overall Status: FAILED" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "Entity-migration synchronization issues detected. Review failures above and remediate before release." >> "$REPORT_FILE"
fi

echo ""
echo "Report generated: $REPORT_FILE"
echo ""

exit $OVERALL_STATUS
