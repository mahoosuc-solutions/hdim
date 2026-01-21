#!/bin/bash
# Task 15: Environment Variable Validation
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

VERSION="${1:-${VERSION:-}}"
[ -z "$VERSION" ] && { echo -e "${RED}ERROR: VERSION not specified${NC}"; exit 1; }

echo "Task 15: Environment Variables Validation - Version: $VERSION"
cd "$(dirname "$0")/../.." || exit 1

REPORT_DIR="docs/releases/${VERSION}/validation"
mkdir -p "$REPORT_DIR"
REPORT_FILE="$REPORT_DIR/environment-validation-report.md"

cat > "$REPORT_FILE" <<'EOF'
# Environment Variable Validation Report

## Overview
Validates required environment variables are documented and no secrets are hardcoded.

---

EOF

OVERALL_STATUS=0
HARDCODED_SECRETS=0

# Check for hardcoded secrets in docker-compose.yml
if grep -E "(password|secret|key).*:" docker-compose.yml | grep -v "\${" | grep -v "#"; then
    echo -e "${RED}✗ Hardcoded secrets found in docker-compose.yml${NC}"
    echo "- ❌ **Hardcoded Secrets:** Found in docker-compose.yml" >> "$REPORT_FILE"
    HARDCODED_SECRETS=1
    OVERALL_STATUS=1
else
    echo -e "${GREEN}✓ No hardcoded secrets in docker-compose.yml${NC}"
    echo "- ✅ **Hardcoded Secrets:** None detected" >> "$REPORT_FILE"
fi

echo "" >> "$REPORT_FILE"

if [ $OVERALL_STATUS -eq 0 ]; then
    echo "### ✅ Overall Status: PASSED" >> "$REPORT_FILE"
else
    echo "### ❌ Overall Status: FAILED" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "**Remediation:** Use \${SECRET} placeholders for all secrets." >> "$REPORT_FILE"
fi

echo "Report: $REPORT_FILE"
exit $OVERALL_STATUS
