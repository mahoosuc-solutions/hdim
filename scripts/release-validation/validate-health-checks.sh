#!/bin/bash
# Task 14: Health Check Configuration Validation
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

VERSION="${1:-${VERSION:-}}"
[ -z "$VERSION" ] && { echo -e "${RED}ERROR: VERSION not specified${NC}"; exit 1; }

echo "Task 14: Health Check Validation - Version: $VERSION"
cd "$(dirname "$0")/../.." || exit 1

REPORT_DIR="docs/releases/${VERSION}/validation"
mkdir -p "$REPORT_DIR"
REPORT_FILE="$REPORT_DIR/health-check-validation-report.md"

cat > "$REPORT_FILE" <<'EOF'
# Health Check Configuration Validation Report

## Overview
Validates health check configuration in docker-compose.yml.

**Target Configuration:**
- interval: 30s
- timeout: 10s
- retries: 3
- start-period: 60s+

---

EOF

OVERALL_STATUS=0
SERVICES_WITH_HEALTH_CHECKS=0
SERVICES_WITHOUT_HEALTH_CHECKS=0

# Parse docker-compose.yml for health checks
SERVICES=$(grep -E "^  [a-z-]+:" docker-compose.yml | sed 's/://g' | tr -d ' ')

for SERVICE in $SERVICES; do
    if grep -A 10 "^  $SERVICE:" docker-compose.yml | grep -q "healthcheck:"; then
        SERVICES_WITH_HEALTH_CHECKS=$((SERVICES_WITH_HEALTH_CHECKS + 1))
        echo -e "${GREEN}✓${NC} $SERVICE has health check"
        echo "- ✅ **$SERVICE:** Health check configured" >> "$REPORT_FILE"
    else
        SERVICES_WITHOUT_HEALTH_CHECKS=$((SERVICES_WITHOUT_HEALTH_CHECKS + 1))
        echo -e "${RED}✗${NC} $SERVICE missing health check"
        echo "- ⚠️ **$SERVICE:** No health check" >> "$REPORT_FILE"
    fi
done

echo "" >> "$REPORT_FILE"
echo "## Summary" >> "$REPORT_FILE"
echo "- Services with health checks: $SERVICES_WITH_HEALTH_CHECKS" >> "$REPORT_FILE"
echo "- Services without health checks: $SERVICES_WITHOUT_HEALTH_CHECKS" >> "$REPORT_FILE"

[ $SERVICES_WITHOUT_HEALTH_CHECKS -gt 0 ] && OVERALL_STATUS=1

if [ $OVERALL_STATUS -eq 0 ]; then
    echo "### ✅ Overall Status: PASSED" >> "$REPORT_FILE"
else
    echo "### ⚠️ Overall Status: WARNINGS" >> "$REPORT_FILE"
fi

echo "Report: $REPORT_FILE"
exit $OVERALL_STATUS
