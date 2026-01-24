#!/bin/bash

# Accessibility Setup Verification Script
# Verifies that axe-core testing infrastructure is properly installed and configured

set -e

echo "🔍 Verifying Accessibility Testing Setup..."
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Track verification status
VERIFICATION_PASSED=true

# Function to check file exists
check_file() {
    local file=$1
    local description=$2

    if [ -f "$file" ]; then
        echo -e "${GREEN}✓${NC} $description exists"
        return 0
    else
        echo -e "${RED}✗${NC} $description missing"
        VERIFICATION_PASSED=false
        return 1
    fi
}

# Function to check directory exists
check_dir() {
    local dir=$1
    local description=$2

    if [ -d "$dir" ]; then
        echo -e "${GREEN}✓${NC} $description exists"
        return 0
    else
        echo -e "${RED}✗${NC} $description missing"
        VERIFICATION_PASSED=false
        return 1
    fi
}

# Function to check npm package installed
check_package() {
    local package=$1
    local dir=$2

    if [ -d "$dir/node_modules/$package" ]; then
        echo -e "${GREEN}✓${NC} $package installed"
        return 0
    else
        echo -e "${RED}✗${NC} $package not installed"
        VERIFICATION_PASSED=false
        return 1
    fi
}

# Function to count files matching pattern
count_files() {
    local pattern=$1
    local count=$(find apps/clinical-portal/src -name "$pattern" 2>/dev/null | wc -l)
    echo $count
}

echo "=== 1. Checking npm packages ==="
cd /mnt/wdblack/dev/projects/hdim-master
check_package "axe-core" "."
check_package "jest-axe" "."
echo ""

echo "=== 2. Checking test infrastructure files ==="
check_file "apps/clinical-portal/src/testing/accessibility.helper.ts" "Accessibility test helper"
check_file "apps/clinical-portal/src/testing/setup-accessibility-tests.ts" "Jest accessibility setup"
check_file "apps/clinical-portal/ACCESSIBILITY_TESTING.md" "Accessibility testing documentation"
echo ""

echo "=== 3. Checking accessibility test files ==="
A11Y_TEST_COUNT=$(count_files "*.a11y.spec.ts")
if [ $A11Y_TEST_COUNT -gt 0 ]; then
    echo -e "${GREEN}✓${NC} Found $A11Y_TEST_COUNT accessibility test files"
    find apps/clinical-portal/src -name "*.a11y.spec.ts" -type f | while read file; do
        echo "    - ${file#apps/clinical-portal/src/}"
    done
else
    echo -e "${YELLOW}⚠${NC} No accessibility test files found"
fi
echo ""

echo "=== 4. Checking Jest configuration ==="
if grep -q "setup-accessibility-tests.ts" apps/clinical-portal/jest.config.ts; then
    echo -e "${GREEN}✓${NC} Jest configured for accessibility testing"
else
    echo -e "${RED}✗${NC} Jest not configured for accessibility testing"
    VERIFICATION_PASSED=false
fi
echo ""

echo "=== 5. Checking accessibility implementation ==="
check_file "apps/clinical-portal/src/app/components/navigation/navigation.component.html" "Navigation component"

# Check for skip links implementation
if grep -q "skip-links" apps/clinical-portal/src/app/components/navigation/navigation.component.html; then
    echo -e "${GREEN}✓${NC} Skip navigation links implemented"
else
    echo -e "${YELLOW}⚠${NC} Skip navigation links not found"
fi

# Check for focus indicators in styles
if grep -q "focus-visible" apps/clinical-portal/src/styles.scss; then
    echo -e "${GREEN}✓${NC} Keyboard focus indicators implemented"
else
    echo -e "${YELLOW}⚠${NC} Keyboard focus indicators not found"
fi

# Check for ARIA labels
ARIA_LABEL_COUNT=$(grep -r "aria-label" apps/clinical-portal/src/app/pages 2>/dev/null | wc -l)
if [ $ARIA_LABEL_COUNT -gt 0 ]; then
    echo -e "${GREEN}✓${NC} Found $ARIA_LABEL_COUNT ARIA labels in components"
else
    echo -e "${YELLOW}⚠${NC} No ARIA labels found"
fi
echo ""

echo "=== 6. Summary ==="
echo ""
echo "Accessibility Test Files: $A11Y_TEST_COUNT"
echo "ARIA Labels: $ARIA_LABEL_COUNT"
echo ""

if [ "$VERIFICATION_PASSED" = true ]; then
    echo -e "${GREEN}✓ Accessibility testing setup verified successfully!${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Run tests: npx nx test clinical-portal --testPathPattern=a11y.spec.ts"
    echo "  2. View documentation: cat apps/clinical-portal/ACCESSIBILITY_TESTING.md"
    echo "  3. Add more accessibility tests to additional components"
    exit 0
else
    echo -e "${RED}✗ Accessibility testing setup incomplete${NC}"
    echo ""
    echo "Missing components detected. Please review the errors above."
    exit 1
fi
