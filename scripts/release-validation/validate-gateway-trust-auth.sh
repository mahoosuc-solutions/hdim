#!/bin/bash
# Task 12: Gateway Trust Authentication Validation
# Validates gateway trust authentication pattern across all backend services
set -euo pipefail

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
echo "Task 12: Gateway Trust Auth Validation"
echo "Version: $VERSION"
echo "=========================================="
echo ""

cd "$(dirname "$0")/../../backend" || exit 1

REPORT_DIR="../docs/releases/${VERSION}/validation"
mkdir -p "$REPORT_DIR"
REPORT_FILE="$REPORT_DIR/GATEWAY_TRUST_VALIDATION.md"

cat > "$REPORT_FILE" <<EOF
# Gateway Trust Authentication Validation Report

**Release Version:** $VERSION
**Validation Date:** $(date '+%Y-%m-%d %H:%M:%S')

---

## Overview

Validates gateway trust authentication pattern across all backend services.

**Requirements:**
1. SecurityConfig uses TrustedHeaderAuthFilter + TrustedTenantAccessFilter
2. Filter ordering: TrustedHeaderAuthFilter BEFORE UsernamePasswordAuthenticationFilter
3. All endpoints have @PreAuthorize annotations
4. Tests use GatewayTrustTestHeaders
5. No JWT validation in backend services

---

## Validation Results

EOF

OVERALL_STATUS=0
SERVICES_WITHOUT_TRUSTED_FILTER=0
FILTER_ORDER_VIOLATIONS=0
ENDPOINTS_WITHOUT_PREAUTHORIZE=0
TESTS_WITHOUT_GATEWAY_HEADERS=0
JWT_VALIDATION_FOUND=0

# Find all SecurityConfig files
SECURITY_CONFIGS=$(find modules/services -name "*SecurityConfig.java" -type f | grep -v test)

echo "Checking SecurityConfig files for gateway trust pattern..."
echo ""

for CONFIG in $SECURITY_CONFIGS; do
    SERVICE=$(echo "$CONFIG" | sed 's|modules/services/||' | cut -d/ -f1)
    echo "Checking: $SERVICE"

    # Check for TrustedHeaderAuthFilter
    if grep -q "TrustedHeaderAuthFilter" "$CONFIG"; then
        TRUSTED_FILTER="✅"
    else
        TRUSTED_FILTER="❌"
        SERVICES_WITHOUT_TRUSTED_FILTER=$((SERVICES_WITHOUT_TRUSTED_FILTER + 1))
        OVERALL_STATUS=1
    fi

    # Check for TrustedTenantAccessFilter
    if grep -q "TrustedTenantAccessFilter" "$CONFIG"; then
        TENANT_FILTER="✅"
    else
        TENANT_FILTER="❌"
        SERVICES_WITHOUT_TRUSTED_FILTER=$((SERVICES_WITHOUT_TRUSTED_FILTER + 1))
        OVERALL_STATUS=1
    fi

    # Check filter ordering (TrustedHeaderAuthFilter before UsernamePasswordAuthenticationFilter)
    if grep -B 5 -A 5 "addFilterBefore" "$CONFIG" | grep -q "TrustedHeaderAuthFilter.*UsernamePasswordAuthenticationFilter"; then
        FILTER_ORDER="✅"
    else
        FILTER_ORDER="⚠️"
        FILTER_ORDER_VIOLATIONS=$((FILTER_ORDER_VIOLATIONS + 1))
    fi

    # Check for JWT validation (should NOT be present in backend)
    if grep -q "JwtAuthenticationFilter\|JwtDecoder\|jwt()" "$CONFIG"; then
        JWT_VALIDATION="❌ (Found - should not be in backend)"
        JWT_VALIDATION_FOUND=$((JWT_VALIDATION_FOUND + 1))
        OVERALL_STATUS=1
    else
        JWT_VALIDATION="✅ (Not found - correct)"
    fi

    # Report
    if [ "$TRUSTED_FILTER" = "✅" ] && [ "$TENANT_FILTER" = "✅" ] && [ "$FILTER_ORDER" = "✅" ] && [ "$JWT_VALIDATION" = "✅ (Not found - correct)" ]; then
        echo -e "${GREEN}✓ COMPLIANT${NC} - $SERVICE"
        echo "### ✅ $SERVICE" >> "$REPORT_FILE"
    else
        echo -e "${RED}✗ NON-COMPLIANT${NC} - $SERVICE"
        echo "### ❌ $SERVICE" >> "$REPORT_FILE"
    fi

    echo "" >> "$REPORT_FILE"
    echo "- **TrustedHeaderAuthFilter:** $TRUSTED_FILTER" >> "$REPORT_FILE"
    echo "- **TrustedTenantAccessFilter:** $TENANT_FILTER" >> "$REPORT_FILE"
    echo "- **Filter Ordering:** $FILTER_ORDER" >> "$REPORT_FILE"
    echo "- **JWT Validation in Backend:** $JWT_VALIDATION" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
done

echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Check @PreAuthorize annotations on controllers
echo "## @PreAuthorize Annotation Coverage" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "Checking @PreAuthorize annotations on controller endpoints..."

CONTROLLERS=$(find modules/services -name "*Controller.java" -type f | grep -v test)
TOTAL_ENDPOINTS=0
ENDPOINTS_WITH_PREAUTHORIZE=0

for CONTROLLER in $CONTROLLERS; do
    # Count endpoints (methods with @GetMapping, @PostMapping, etc.)
    ENDPOINT_COUNT=$(grep -c "@.*Mapping" "$CONTROLLER" || echo "0")
    TOTAL_ENDPOINTS=$((TOTAL_ENDPOINTS + ENDPOINT_COUNT))

    # Count @PreAuthorize annotations
    PREAUTHORIZE_COUNT=$(grep -c "@PreAuthorize" "$CONTROLLER" || echo "0")
    ENDPOINTS_WITH_PREAUTHORIZE=$((ENDPOINTS_WITH_PREAUTHORIZE + PREAUTHORIZE_COUNT))
done

if [ $TOTAL_ENDPOINTS -gt 0 ]; then
    COVERAGE_PCT=$(awk "BEGIN {printf \"%.1f\", ($ENDPOINTS_WITH_PREAUTHORIZE / $TOTAL_ENDPOINTS) * 100}")

    echo "- **Total Endpoints:** $TOTAL_ENDPOINTS" >> "$REPORT_FILE"
    echo "- **Endpoints with @PreAuthorize:** $ENDPOINTS_WITH_PREAUTHORIZE" >> "$REPORT_FILE"
    echo "- **Coverage:** ${COVERAGE_PCT}%" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"

    if [ "$ENDPOINTS_WITH_PREAUTHORIZE" -eq "$TOTAL_ENDPOINTS" ]; then
        echo -e "${GREEN}✓ 100% @PreAuthorize coverage${NC}"
        echo "**Status:** ✅ 100% coverage" >> "$REPORT_FILE"
    else
        ENDPOINTS_WITHOUT_PREAUTHORIZE=$((TOTAL_ENDPOINTS - ENDPOINTS_WITH_PREAUTHORIZE))
        echo -e "${YELLOW}⚠ $ENDPOINTS_WITHOUT_PREAUTHORIZE endpoints missing @PreAuthorize${NC}"
        echo "**Status:** ⚠️ $ENDPOINTS_WITHOUT_PREAUTHORIZE endpoints missing annotations" >> "$REPORT_FILE"
        OVERALL_STATUS=1
    fi
fi

echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Check integration tests for GatewayTrustTestHeaders
echo "## Integration Test Header Usage" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "Checking integration tests for GatewayTrustTestHeaders..."

INTEGRATION_TESTS=$(find modules/services -name "*IntegrationTest.java" -o -name "*ControllerTest.java" | grep -v "target")
TESTS_WITH_GATEWAY_HEADERS=0
TOTAL_INTEGRATION_TESTS=0

for TEST in $INTEGRATION_TESTS; do
    TOTAL_INTEGRATION_TESTS=$((TOTAL_INTEGRATION_TESTS + 1))

    if grep -q "GatewayTrustTestHeaders" "$TEST"; then
        TESTS_WITH_GATEWAY_HEADERS=$((TESTS_WITH_GATEWAY_HEADERS + 1))
    fi
done

if [ $TOTAL_INTEGRATION_TESTS -gt 0 ]; then
    TESTS_WITHOUT_GATEWAY_HEADERS=$((TOTAL_INTEGRATION_TESTS - TESTS_WITH_GATEWAY_HEADERS))

    echo "- **Total Integration Tests:** $TOTAL_INTEGRATION_TESTS" >> "$REPORT_FILE"
    echo "- **Tests with GatewayTrustTestHeaders:** $TESTS_WITH_GATEWAY_HEADERS" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"

    if [ "$TESTS_WITHOUT_GATEWAY_HEADERS" -gt 0 ]; then
        echo -e "${YELLOW}⚠ $TESTS_WITHOUT_GATEWAY_HEADERS tests may not use GatewayTrustTestHeaders${NC}"
        echo "**Status:** ⚠️ Some tests may not use proper auth headers" >> "$REPORT_FILE"
    else
        echo -e "${GREEN}✓ All tests use GatewayTrustTestHeaders${NC}"
        echo "**Status:** ✅ All tests properly configured" >> "$REPORT_FILE"
    fi
fi

echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Summary
echo ""
echo "=========================================="
echo "Validation Summary"
echo "=========================================="
echo "Services without Trusted Filters: $SERVICES_WITHOUT_TRUSTED_FILTER"
echo "Filter Order Violations: $FILTER_ORDER_VIOLATIONS"
echo "Endpoints without @PreAuthorize: $ENDPOINTS_WITHOUT_PREAUTHORIZE"
echo "JWT Validation Found (should be 0): $JWT_VALIDATION_FOUND"
echo ""

echo "## Summary" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| Check | Status |" >> "$REPORT_FILE"
echo "|-------|--------|" >> "$REPORT_FILE"
echo "| Trusted Filter Configuration | $([ $SERVICES_WITHOUT_TRUSTED_FILTER -eq 0 ] && echo "✅ PASS" || echo "❌ FAIL") |" >> "$REPORT_FILE"
echo "| Filter Ordering | $([ $FILTER_ORDER_VIOLATIONS -eq 0 ] && echo "✅ PASS" || echo "⚠️ WARN") |" >> "$REPORT_FILE"
echo "| @PreAuthorize Coverage | $([ $ENDPOINTS_WITHOUT_PREAUTHORIZE -eq 0 ] && echo "✅ 100%" || echo "⚠️ $ENDPOINTS_WITHOUT_PREAUTHORIZE missing") |" >> "$REPORT_FILE"
echo "| No JWT Validation in Backend | $([ $JWT_VALIDATION_FOUND -eq 0 ] && echo "✅ PASS" || echo "❌ FAIL") |" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "## References" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "- **Gateway Trust Architecture:** backend/docs/GATEWAY_TRUST_ARCHITECTURE.md" >> "$REPORT_FILE"
echo "- **CLAUDE.md:** Gateway Trust Authentication section" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

if [ $OVERALL_STATUS -eq 0 ]; then
    echo -e "${GREEN}✓ VALIDATION PASSED${NC}"
    echo "### ✅ Overall Status: PASSED" >> "$REPORT_FILE"
else
    echo -e "${RED}✗ VALIDATION FAILED${NC}"
    echo "### ❌ Overall Status: FAILED" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "Gateway trust authentication issues detected. Review failures and update SecurityConfig files." >> "$REPORT_FILE"
fi

echo ""
echo "Report generated: $REPORT_FILE"
echo ""

exit $OVERALL_STATUS
