#!/bin/bash
# Task 18: Git Repository Status Validation
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

VERSION="${1:-${VERSION:-}}"
[ -z "$VERSION" ] && { echo -e "${RED}ERROR: VERSION not specified${NC}"; exit 1; }

echo "Task 18: Git Repository Status Validation - Version: $VERSION"
cd "$(dirname "$0")/../.." || exit 1

REPORT_DIR="docs/releases/${VERSION}/validation"
mkdir -p "$REPORT_DIR"
REPORT_FILE="$REPORT_DIR/git-status-report.md"

cat > "$REPORT_FILE" <<'EOF'
# Git Repository Status Validation Report

## Overview
Validates git repository is ready for release tagging.

---

EOF

OVERALL_STATUS=0

# Check git status
if git diff --quiet; then
    echo -e "${GREEN}✓ No unstaged changes${NC}"
    echo "- ✅ **Unstaged Changes:** None" >> "$REPORT_FILE"
else
    UNSTAGED_COUNT=$(git diff --name-only | wc -l)
    echo -e "${YELLOW}⚠ $UNSTAGED_COUNT unstaged changes${NC}"
    echo "- ⚠️ **Unstaged Changes:** $UNSTAGED_COUNT files" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "**Files:**" >> "$REPORT_FILE"
    git diff --name-only | sed 's/^/  - /' >> "$REPORT_FILE"
fi

# Check for merge conflicts
if git diff --check > /dev/null 2>&1; then
    echo -e "${GREEN}✓ No merge conflicts${NC}"
    echo "- ✅ **Merge Conflicts:** None" >> "$REPORT_FILE"
else
    echo -e "${RED}✗ Merge conflicts detected${NC}"
    echo "- ❌ **Merge Conflicts:** Detected" >> "$REPORT_FILE"
    OVERALL_STATUS=1
fi

# Check recent commits follow conventional commits
RECENT_COMMITS=$(git log --oneline -5 --format="%s")
CONVENTIONAL_COUNT=$(echo "$RECENT_COMMITS" | grep -E "^(feat|fix|docs|chore|refactor|test|style):" | wc -l)

echo "" >> "$REPORT_FILE"
echo "## Recent Commits" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo '```' >> "$REPORT_FILE"
echo "$RECENT_COMMITS" >> "$REPORT_FILE"
echo '```' >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "- **Conventional Commits:** $CONVENTIONAL_COUNT out of 5" >> "$REPORT_FILE"

if [ $OVERALL_STATUS -eq 0 ]; then
    echo "" >> "$REPORT_FILE"
    echo "### ✅ Overall Status: READY FOR TAGGING" >> "$REPORT_FILE"
else
    echo "" >> "$REPORT_FILE"
    echo "### ❌ Overall Status: NOT READY" >> "$REPORT_FILE"
fi

echo ""
echo "Report: $REPORT_FILE"

exit $OVERALL_STATUS
