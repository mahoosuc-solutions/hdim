#!/bin/bash
# Task 17: Version Matrix Validation
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

VERSION="${1:-${VERSION:-}}"
[ -z "$VERSION" ] && { echo -e "${RED}ERROR: VERSION not specified${NC}"; exit 1; }

echo "Task 17: Version Matrix Validation - Version: $VERSION"
cd "$(dirname "$0")/../.." || exit 1

VERSION_MATRIX_FILE="docs/releases/${VERSION}/VERSION_MATRIX_${VERSION}.md"

if [ ! -f "$VERSION_MATRIX_FILE" ]; then
    echo -e "${RED}✗ VERSION_MATRIX_${VERSION}.md not found${NC}"
    echo "Run: ./scripts/release-validation/generate-release-docs.sh $VERSION"
    exit 1
fi

echo -e "${GREEN}✓ VERSION_MATRIX_${VERSION}.md exists${NC}"

# Validate it contains required sections
REQUIRED_SECTIONS=("Microservices" "Infrastructure" "Dependencies")
OVERALL_STATUS=0

for SECTION in "${REQUIRED_SECTIONS[@]}"; do
    if grep -q "$SECTION" "$VERSION_MATRIX_FILE"; then
        echo -e "${GREEN}✓${NC} Section '$SECTION' found"
    else
        echo -e "${RED}✗${NC} Section '$SECTION' missing"
        OVERALL_STATUS=1
    fi
done

if [ $OVERALL_STATUS -eq 0 ]; then
    echo -e "${GREEN}✓ VALIDATION PASSED${NC}"
else
    echo -e "${RED}✗ VALIDATION FAILED${NC}"
fi

exit $OVERALL_STATUS
