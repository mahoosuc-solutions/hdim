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
FLOATING_TAGS=0
IMMUTABLE_IMAGES=0

COMPOSE_FILES=(
  "docker-compose.yml"
  "docker-compose.production.yml"
  "docker-compose.prod.yml"
  "docker-compose.staging.yml"
)

# Check for hardcoded secrets in compose files
for compose_file in "${COMPOSE_FILES[@]}"; do
    if [ ! -f "$compose_file" ]; then
        continue
    fi

    # Ignore known development/demo placeholders and parameterized values
    if grep -nEi "(password|secret|api[_-]?key|token)\s*:\s*['\"]?[^\\$#\"']+" "$compose_file" \
        | grep -Ev "\\$\\{|CHANGE_ME|demo_|dev_|test_|staging_" >/dev/null; then
        echo -e "${RED}✗ Hardcoded secrets found in $compose_file${NC}"
        echo "- ❌ **Hardcoded Secrets:** Found in \`$compose_file\`" >> "$REPORT_FILE"
        HARDCODED_SECRETS=1
        OVERALL_STATUS=1
    else
        echo -e "${GREEN}✓ No hardcoded production secrets in $compose_file${NC}"
        echo "- ✅ **Hardcoded Secrets:** No production hardcoded values detected in \`$compose_file\`" >> "$REPORT_FILE"
    fi
done

# Check for floating :latest tags in production compose files
for compose_file in docker-compose.production.yml docker-compose.prod.yml; do
    if [ ! -f "$compose_file" ]; then
        continue
    fi
    if grep -nE "image:\s+[^#[:space:]]+:latest\b" "$compose_file" >/dev/null; then
        echo -e "${RED}✗ Floating image tags found in $compose_file${NC}"
        echo "- ❌ **Floating Tags:** Found \`:latest\` usage in \`$compose_file\`" >> "$REPORT_FILE"
        FLOATING_TAGS=1
        OVERALL_STATUS=1
    else
        echo -e "${GREEN}✓ No floating :latest tags in $compose_file${NC}"
        echo "- ✅ **Floating Tags:** None in \`$compose_file\`" >> "$REPORT_FILE"
    fi
done

# Check for immutable image reference policy in production compose
if [ -f "docker-compose.production.yml" ]; then
    IMAGE_LINES=$(grep -Ec "^[[:space:]]*image:" docker-compose.production.yml || true)
    IMMUTABLE_IMAGE_LINES=$(awk '
      /^[[:space:]]*image:/ {
        if ($0 ~ /\$\{[A-Z0-9_]+_IMAGE:\?/) {
          c++
        }
      }
      END { print c+0 }
    ' docker-compose.production.yml)
    if [ "$IMAGE_LINES" -eq 0 ] || [ "$IMAGE_LINES" -ne "$IMMUTABLE_IMAGE_LINES" ]; then
        echo -e "${RED}✗ Immutable image policy violation in docker-compose.production.yml${NC}"
        echo "- ❌ **Immutable Images:** All production images must use required \`*_IMAGE\` refs in \`docker-compose.production.yml\`" >> "$REPORT_FILE"
        IMMUTABLE_IMAGES=1
        OVERALL_STATUS=1
    else
        echo -e "${GREEN}✓ Immutable image refs enforced in docker-compose.production.yml${NC}"
        echo "- ✅ **Immutable Images:** Required \`*_IMAGE\` refs enforced in \`docker-compose.production.yml\`" >> "$REPORT_FILE"
    fi
fi

# Check digest examples exist in env template
if [ -f ".env.production.example" ]; then
    REQUIRED_IMAGE_VARS=(
      "AI_SALES_AGENT_IMAGE"
      "LIVE_CALL_SALES_AGENT_IMAGE"
      "COACHING_UI_IMAGE"
      "POSTGRES_IMAGE"
      "REDIS_IMAGE"
      "JAEGER_IMAGE"
      "PROMETHEUS_IMAGE"
      "ALERTMANAGER_IMAGE"
      "GRAFANA_IMAGE"
    )

    for var_name in "${REQUIRED_IMAGE_VARS[@]}"; do
        if ! grep -Eq "^${var_name}=.*@sha256:" .env.production.example; then
            echo -e "${RED}✗ Missing digest-pinned example for ${var_name} in .env.production.example${NC}"
            echo "- ❌ **Immutable Images:** Missing \`${var_name}\` digest example in \`.env.production.example\`" >> "$REPORT_FILE"
            IMMUTABLE_IMAGES=1
            OVERALL_STATUS=1
        fi
    done
fi

echo "" >> "$REPORT_FILE"

if [ $OVERALL_STATUS -eq 0 ]; then
    echo "### ✅ Overall Status: PASSED" >> "$REPORT_FILE"
else
    echo "### ❌ Overall Status: FAILED" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    if [ $HARDCODED_SECRETS -eq 1 ]; then
        echo "- **Remediation:** Use \${SECRET} placeholders for secrets and inject via env/secrets manager." >> "$REPORT_FILE"
    fi
    if [ $FLOATING_TAGS -eq 1 ]; then
        echo "- **Remediation:** Pin all production images to explicit versions or digests." >> "$REPORT_FILE"
    fi
    if [ $IMMUTABLE_IMAGES -eq 1 ]; then
        echo "- **Remediation:** Use required immutable \`*_IMAGE\` refs and digest-pinned values (\`@sha256:\`)." >> "$REPORT_FILE"
    fi
fi

echo "Report: $REPORT_FILE"
exit $OVERALL_STATUS
