#!/bin/bash
# Task 04: HIPAA Compliance Validation
# Validates HIPAA compliance for PHI handling, cache controls, and audit logging
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
echo "Task 04: HIPAA Compliance Validation"
echo "Version: $VERSION"
echo "=========================================="
echo ""

# Navigate to backend directory
cd "$(dirname "$0")/../../backend" || exit 1

# Create output directory
REPORT_DIR="../docs/releases/${VERSION}/validation"
mkdir -p "$REPORT_DIR"
REPORT_FILE="$REPORT_DIR/HIPAA_COMPLIANCE_REPORT.md"

# Initialize report
cat > "$REPORT_FILE" <<EOF
# HIPAA Compliance Validation Report

**Release Version:** $VERSION
**Validation Date:** $(date '+%Y-%m-%d %H:%M:%S')
**Validator:** HIPAA Compliance Script

---

## Overview

This report validates HIPAA compliance across all HDIM services for Protected Health Information (PHI) handling, cache controls, and audit logging.

**Compliance Requirements:**
- PHI cache TTL ≤ 5 minutes (300,000ms, recommended 2 minutes / 120,000ms)
- Cache-Control: no-store headers on all PHI responses
- @Audited annotations on PHI access methods
- Multi-tenant isolation tests

---

EOF

# Track overall status
OVERALL_STATUS=0
CACHE_TTL_VIOLATIONS=0
CACHE_CONTROL_VIOLATIONS=0
AUDIT_ANNOTATION_MISSING=0
TENANT_TEST_MISSING=0

echo "## Cache TTL Validation" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "Checking @Cacheable annotations for PHI cache TTL compliance..."
echo ""

# Find all @Cacheable annotations in service files
CACHEABLE_FILES=$(grep -r "@Cacheable" modules/services --include="*.java" | grep -v "test" | cut -d: -f1 | sort -u)

if [ -z "$CACHEABLE_FILES" ]; then
    echo -e "${YELLOW}⚠ No @Cacheable annotations found${NC}"
    echo "- **@Cacheable Annotations:** None found" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
else
    for FILE in $CACHEABLE_FILES; do
        # Check if file deals with PHI (Patient, Observation, Condition, etc.)
        if grep -q -E "(Patient|Observation|Condition|MedicationRequest|DiagnosticReport|AllergyIntolerance)" "$FILE"; then
            echo "Checking: $FILE"

            # Extract service name
            SERVICE=$(echo "$FILE" | sed 's|modules/services/||' | cut -d/ -f1)

            # Check application.yml for TTL setting
            YML_FILE="modules/services/${SERVICE}/src/main/resources/application.yml"

            if [ -f "$YML_FILE" ]; then
                # Extract TTL from spring.cache.redis.time-to-live
                if grep -q "time-to-live:" "$YML_FILE"; then
                    TTL=$(grep "time-to-live:" "$YML_FILE" | head -1 | awk '{print $2}' | tr -d '\r')

                    # Validate TTL is <= 300000 (5 minutes in ms)
                    if [ "$TTL" -le 300000 ] 2>/dev/null; then
                        if [ "$TTL" -le 120000 ]; then
                            echo -e "${GREEN}✓ COMPLIANT${NC} - $SERVICE TTL: ${TTL}ms (≤2 min recommended)"
                            echo "- ✅ **$SERVICE**: TTL ${TTL}ms (compliant, recommended)" >> "$REPORT_FILE"
                        else
                            echo -e "${GREEN}✓ COMPLIANT${NC} - $SERVICE TTL: ${TTL}ms (≤5 min required)"
                            echo "- ✅ **$SERVICE**: TTL ${TTL}ms (compliant, consider reducing to ≤120,000ms)" >> "$REPORT_FILE"
                        fi
                    else
                        echo -e "${RED}✗ VIOLATION${NC} - $SERVICE TTL: ${TTL}ms (exceeds 5 min limit)"
                        echo "- ❌ **$SERVICE**: TTL ${TTL}ms (EXCEEDS 300,000ms limit)" >> "$REPORT_FILE"
                        CACHE_TTL_VIOLATIONS=$((CACHE_TTL_VIOLATIONS + 1))
                        OVERALL_STATUS=1
                    fi
                else
                    echo -e "${YELLOW}⚠ WARNING${NC} - $SERVICE: No TTL configured"
                    echo "- ⚠️ **$SERVICE**: No time-to-live configured in application.yml" >> "$REPORT_FILE"
                    CACHE_TTL_VIOLATIONS=$((CACHE_TTL_VIOLATIONS + 1))
                    OVERALL_STATUS=1
                fi
            fi
        fi
    done
fi

echo "" >> "$REPORT_FILE"

if [ $CACHE_TTL_VIOLATIONS -eq 0 ]; then
    echo -e "${GREEN}✓ All PHI caches have compliant TTL settings${NC}"
    echo "**Summary:** All PHI caches comply with ≤5 minute TTL requirement." >> "$REPORT_FILE"
else
    echo -e "${RED}✗ Found $CACHE_TTL_VIOLATIONS cache TTL violations${NC}"
    echo "**Summary:** $CACHE_TTL_VIOLATIONS cache TTL violations detected." >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "**Remediation:**" >> "$REPORT_FILE"
    echo "- Set \`spring.cache.redis.time-to-live: 120000\` (2 minutes) in application.yml" >> "$REPORT_FILE"
    echo "- Maximum allowed: 300,000ms (5 minutes) per HIPAA-CACHE-COMPLIANCE.md" >> "$REPORT_FILE"
fi

echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Check Cache-Control headers
echo "## Cache-Control Header Validation" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "Checking for Cache-Control headers on PHI endpoints..."
echo ""

# Find controllers that return PHI data
PHI_CONTROLLERS=$(grep -r "Patient\|Observation\|Condition" modules/services --include="*Controller.java" | grep -v "test" | cut -d: -f1 | sort -u)

CHECKED_CONTROLLERS=0
for CONTROLLER in $PHI_CONTROLLERS; do
    CHECKED_CONTROLLERS=$((CHECKED_CONTROLLERS + 1))

    # Check if controller sets Cache-Control headers
    if grep -q "Cache-Control.*no-store" "$CONTROLLER"; then
        echo -e "${GREEN}✓ COMPLIANT${NC} - $(basename $CONTROLLER)"
        echo "- ✅ **$(basename $CONTROLLER)**: Cache-Control headers present" >> "$REPORT_FILE"
    else
        echo -e "${YELLOW}⚠ WARNING${NC} - $(basename $CONTROLLER): No Cache-Control headers found"
        echo "- ⚠️ **$(basename $CONTROLLER)**: Cache-Control headers not detected" >> "$REPORT_FILE"
        CACHE_CONTROL_VIOLATIONS=$((CACHE_CONTROL_VIOLATIONS + 1))
    fi
done

echo "" >> "$REPORT_FILE"

if [ $CACHE_CONTROL_VIOLATIONS -eq 0 ]; then
    echo -e "${GREEN}✓ All PHI controllers have Cache-Control headers${NC}"
    echo "**Summary:** All $CHECKED_CONTROLLERS PHI controllers set proper Cache-Control headers." >> "$REPORT_FILE"
else
    echo -e "${YELLOW}⚠ $CACHE_CONTROL_VIOLATIONS controllers missing Cache-Control headers${NC}"
    echo "**Summary:** $CACHE_CONTROL_VIOLATIONS out of $CHECKED_CONTROLLERS controllers may be missing Cache-Control headers." >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "**Remediation:**" >> "$REPORT_FILE"
    echo '```java' >> "$REPORT_FILE"
    echo 'response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");' >> "$REPORT_FILE"
    echo 'response.setHeader("Pragma", "no-cache");' >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
fi

echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Check @Audited annotations
echo "## Audit Logging Validation" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "Checking for @Audited annotations on PHI access methods..."
echo ""

# Find service methods that access PHI
PHI_SERVICES=$(grep -r "findPatient\|getPatient\|getObservation" modules/services --include="*Service.java" | grep -v "test" | cut -d: -f1 | sort -u)

CHECKED_SERVICES=0
for SERVICE in $PHI_SERVICES; do
    CHECKED_SERVICES=$((CHECKED_SERVICES + 1))

    # Check for @Audited annotation
    if grep -q "@Audited" "$SERVICE"; then
        echo -e "${GREEN}✓ COMPLIANT${NC} - $(basename $SERVICE)"
        echo "- ✅ **$(basename $SERVICE)**: @Audited annotations present" >> "$REPORT_FILE"
    else
        echo -e "${YELLOW}⚠ WARNING${NC} - $(basename $SERVICE): No @Audited annotations found"
        echo "- ⚠️ **$(basename $SERVICE)**: @Audited annotations not detected" >> "$REPORT_FILE"
        AUDIT_ANNOTATION_MISSING=$((AUDIT_ANNOTATION_MISSING + 1))
    fi
done

echo "" >> "$REPORT_FILE"

if [ $AUDIT_ANNOTATION_MISSING -eq 0 ]; then
    echo -e "${GREEN}✓ All PHI services have @Audited annotations${NC}"
    echo "**Summary:** All $CHECKED_SERVICES PHI services use @Audited for audit logging." >> "$REPORT_FILE"
else
    echo -e "${YELLOW}⚠ $AUDIT_ANNOTATION_MISSING services missing @Audited annotations${NC}"
    echo "**Summary:** $AUDIT_ANNOTATION_MISSING out of $CHECKED_SERVICES services may be missing @Audited annotations." >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "**Remediation:**" >> "$REPORT_FILE"
    echo '```java' >> "$REPORT_FILE"
    echo '@Audited(eventType = "PHI_ACCESS")' >> "$REPORT_FILE"
    echo 'public Patient getPatient(String patientId) { ... }' >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
fi

echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Check multi-tenant isolation tests
echo "## Multi-Tenant Isolation Testing" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "Checking for multi-tenant isolation tests..."
echo ""

# Find tenant isolation tests
TENANT_TESTS=$(find modules/services -name "*TenantAccessTest.java" -o -name "*TenantIsolationTest.java" 2>/dev/null)

if [ -z "$TENANT_TESTS" ]; then
    echo -e "${YELLOW}⚠ WARNING: No tenant isolation tests found${NC}"
    echo "- ⚠️ **Tenant Isolation Tests:** None found" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "**Recommendation:** Create tenant isolation tests to verify PHI data cannot cross tenant boundaries." >> "$REPORT_FILE"
    TENANT_TEST_MISSING=1
else
    TEST_COUNT=$(echo "$TENANT_TESTS" | wc -l)
    echo -e "${GREEN}✓ Found $TEST_COUNT tenant isolation test files${NC}"
    echo "- ✅ **Tenant Isolation Tests:** $TEST_COUNT test files found" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"

    for TEST_FILE in $TENANT_TESTS; do
        echo "  - $(basename $TEST_FILE)" >> "$REPORT_FILE"
    done
fi

echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Summary
echo ""
echo "=========================================="
echo "Validation Summary"
echo "=========================================="
echo "Cache TTL Violations: $CACHE_TTL_VIOLATIONS"
echo "Cache-Control Warnings: $CACHE_CONTROL_VIOLATIONS"
echo "Audit Annotation Warnings: $AUDIT_ANNOTATION_MISSING"
echo "Tenant Test Warnings: $TENANT_TEST_MISSING"
echo ""

echo "## Summary" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| Compliance Check | Status |" >> "$REPORT_FILE"
echo "|------------------|--------|" >> "$REPORT_FILE"
echo "| PHI Cache TTL ≤5 min | $([ $CACHE_TTL_VIOLATIONS -eq 0 ] && echo "✅ PASS" || echo "❌ FAIL ($CACHE_TTL_VIOLATIONS violations)") |" >> "$REPORT_FILE"
echo "| Cache-Control Headers | $([ $CACHE_CONTROL_VIOLATIONS -eq 0 ] && echo "✅ PASS" || echo "⚠️ WARN ($CACHE_CONTROL_VIOLATIONS warnings)") |" >> "$REPORT_FILE"
echo "| @Audited Annotations | $([ $AUDIT_ANNOTATION_MISSING -eq 0 ] && echo "✅ PASS" || echo "⚠️ WARN ($AUDIT_ANNOTATION_MISSING warnings)") |" >> "$REPORT_FILE"
echo "| Tenant Isolation Tests | $([ $TENANT_TEST_MISSING -eq 0 ] && echo "✅ PASS" || echo "⚠️ WARN (tests not found)") |" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "## References" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "- **HIPAA Compliance Guide:** backend/HIPAA-CACHE-COMPLIANCE.md" >> "$REPORT_FILE"
echo "- **Security Guide:** docs/PRODUCTION_SECURITY_GUIDE.md" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

if [ $OVERALL_STATUS -eq 0 ]; then
    echo -e "${GREEN}✓ HIPAA COMPLIANCE VALIDATED${NC}"
    echo "" >> "$REPORT_FILE"
    echo "### ✅ Overall Status: COMPLIANT" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "All critical HIPAA compliance checks passed. PHI handling meets regulatory requirements." >> "$REPORT_FILE"
else
    echo -e "${RED}✗ HIPAA COMPLIANCE ISSUES DETECTED${NC}"
    echo "" >> "$REPORT_FILE"
    echo "### ❌ Overall Status: NON-COMPLIANT" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "Critical HIPAA compliance violations detected. Review and remediate cache TTL issues before release." >> "$REPORT_FILE"
fi

echo ""
echo "Report generated: $REPORT_FILE"
echo ""

exit $OVERALL_STATUS
