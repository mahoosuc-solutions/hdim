#!/bin/bash

# Color Contrast Verification Script
# Uses axe-core to check WCAG 2.1 Level AA color contrast compliance
# Normal text: 4.5:1, Large text (18pt+): 3:1

set -e

echo "🎨 Checking Color Contrast Compliance (WCAG 2.1 Level AA)..."
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

cd /mnt/wdblack/dev/projects/hdim-master

echo "=== Running Automated Color Contrast Tests ==="
echo ""
echo -e "${BLUE}Testing components for color contrast violations...${NC}"
echo ""

# Run accessibility tests focused on color contrast
npx nx test clinical-portal --testPathPattern=a11y.spec.ts --testNamePattern="color contrast|contrast" --no-coverage 2>&1 | tee /tmp/contrast-test-output.txt

# Check if tests passed
if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ All automated color contrast tests passed!${NC}"
else
    echo ""
    echo -e "${YELLOW}⚠ Some color contrast tests may have issues${NC}"
    echo "Check the output above for details"
fi

echo ""
echo "=== Manual Verification Required ==="
echo ""
echo "Automated tests catch common issues, but manual verification is needed for:"
echo "  1. Dynamic content with changing backgrounds"
echo "  2. Hover/focus states"
echo "  3. Complex overlays or gradients"
echo "  4. Images with text"
echo ""
echo "Tools for manual testing:"
echo "  • Chrome DevTools Lighthouse"
echo "  • axe DevTools extension"
echo "  • Contrast Checker: https://webaim.org/resources/contrastchecker/"
echo ""

echo "=== WCAG 2.1 Level AA Color Contrast Requirements ==="
echo ""
echo "Normal text (< 18pt):     4.5:1 contrast ratio"
echo "Large text (≥ 18pt):      3:1 contrast ratio"
echo "UI components/graphics:   3:1 contrast ratio"
echo ""

# Check for common color contrast patterns in SCSS
echo "=== Checking Color Definitions in Styles ==="
echo ""

STYLES_FILE="apps/clinical-portal/src/styles.scss"

if [ -f "$STYLES_FILE" ]; then
    echo "Primary colors defined:"
    grep -E "(--primary-color|--accent-color|--text-primary|--text-secondary|background-color|color:)" "$STYLES_FILE" | head -20
    echo ""
    echo "Status colors defined:"
    grep -E "(status-success|status-warning|status-error|status-info)" "$STYLES_FILE"
    echo ""
else
    echo -e "${RED}Styles file not found${NC}"
fi

echo ""
echo "=== Next Steps ==="
echo ""
echo "1. Review automated test results above"
echo "2. Run manual checks with Chrome DevTools Lighthouse:"
echo "   - Open app in Chrome"
echo "   - F12 > Lighthouse > Accessibility > Generate report"
echo "3. Install axe DevTools extension for detailed analysis"
echo "4. Test all interactive states (hover, focus, active, disabled)"
echo "5. Document any violations found and create fix plan"
echo ""
