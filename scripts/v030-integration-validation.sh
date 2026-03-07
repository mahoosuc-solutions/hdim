#!/usr/bin/env bash
# v0.3.0 Integration Validation Script
# Validates all 3 phases + cross-cutting concerns
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
PASS=0
FAIL=0
WARN=0

check() {
  local label="$1"
  local result="$2"
  if [ "$result" -eq 0 ]; then
    echo -e "  ${GREEN}PASS${NC} $label"
    ((PASS++))
  else
    echo -e "  ${RED}FAIL${NC} $label"
    ((FAIL++))
  fi
}

warn() {
  local label="$1"
  echo -e "  ${YELLOW}WARN${NC} $label"
  ((WARN++))
}

echo "============================================"
echo " v0.3.0 Integration Validation"
echo " $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================"
echo ""

# ---- Phase 2: Financial ROI (Backend) ----
echo "--- Phase 2: Financial ROI Tracking ---"

echo "  Building payer-workflows-service..."
cd /mnt/wdblack/dev/projects/hdim-master/backend
BUILD_RESULT=$(./gradlew :modules:services:payer-workflows-service:build -x test --no-daemon 2>&1 | tail -1)
if echo "$BUILD_RESULT" | grep -q "BUILD SUCCESSFUL"; then
  check "payer-workflows-service compiles" 0
else
  check "payer-workflows-service compiles" 1
fi

echo "  Running payer-workflows tests..."
TEST_OUTPUT=$(./gradlew :modules:services:payer-workflows-service:test --no-daemon 2>&1 | tail -5)
if echo "$TEST_OUTPUT" | grep -q "BUILD SUCCESSFUL"; then
  check "payer-workflows-service tests" 0
else
  check "payer-workflows-service tests" 1
fi

echo "  Entity-migration validation..."
EMV_OUTPUT=$(./gradlew :modules:services:payer-workflows-service:test --tests "*EntityMigrationValidationTest" --no-daemon 2>&1 | tail -3)
if echo "$EMV_OUTPUT" | grep -q "BUILD SUCCESSFUL"; then
  check "entity-migration validation" 0
else
  check "entity-migration validation" 1
fi

# Check ROI-specific files exist
for f in \
  "src/main/java/com/healthdata/payer/domain/RoiCalculation.java" \
  "src/main/java/com/healthdata/payer/service/RoiCalculationService.java" \
  "src/main/java/com/healthdata/payer/controller/RoiCalculatorController.java" \
  "src/main/java/com/healthdata/payer/repository/RoiCalculationRepository.java" \
  "src/test/java/com/healthdata/payer/service/RoiCalculationServiceTest.java" \
  "src/test/java/com/healthdata/payer/controller/RoiCalculatorControllerTest.java" \
  "src/main/resources/db/changelog/0051-add-financial-roi-tracking-fields-to-phase2-execution-tasks.xml" \
  "src/main/resources/db/changelog/0052-create-roi-calculations-table.xml"; do
  if [ -f "modules/services/payer-workflows-service/$f" ]; then
    check "exists: $(basename $f)" 0
  else
    check "exists: $(basename $f)" 1
  fi
done
echo ""

# ---- Phase 3: OCR Document Upload (Frontend) ----
echo "--- Phase 3: OCR Document Upload ---"
cd /mnt/wdblack/dev/projects/hdim-master

echo "  Running document-upload tests..."
DOC_TEST=$(npx nx test clinical-portal --run-in-band --testPathPattern="document-upload" --no-coverage 2>&1 | tail -3)
if echo "$DOC_TEST" | grep -q "Successfully ran"; then
  check "document-upload tests" 0
else
  check "document-upload tests" 1
fi

# Check document-upload files exist
for f in \
  "apps/clinical-portal/src/app/services/document-upload.service.ts" \
  "apps/clinical-portal/src/app/services/document-upload.service.spec.ts" \
  "apps/clinical-portal/src/app/components/document-upload/document-upload.component.ts" \
  "apps/clinical-portal/src/app/components/document-upload/document-upload.component.spec.ts" \
  "apps/clinical-portal/src/app/components/document-upload/document-upload.integration.spec.ts" \
  "apps/clinical-portal/src/app/components/document-upload/document-upload.component.a11y.spec.ts"; do
  if [ -f "$f" ]; then
    check "exists: $(basename $f)" 0
  else
    check "exists: $(basename $f)" 1
  fi
done

# HIPAA: no console.log in document-upload files
CONSOLE_HITS=$(grep -rn "console\.\(log\|error\|warn\|debug\)" apps/clinical-portal/src/app/components/document-upload/ apps/clinical-portal/src/app/services/document-upload.service.ts 2>/dev/null | grep -v "spec.ts" | grep -v "node_modules" | wc -l)
if [ "$CONSOLE_HITS" -eq 0 ]; then
  check "HIPAA: no console.log in document-upload source" 0
else
  check "HIPAA: no console.log in document-upload source" 1
fi
echo ""

# ---- Phase 4: MCP Edge Layer 2 ----
echo "--- Phase 4: MCP Edge Layer 2 (Clinical) ---"
cd /mnt/wdblack/dev/projects/hdim-master/mcp-edge-clinical

echo "  Running MCP Edge clinical tests..."
MCP_TEST=$(npm test 2>&1 | tail -5)
if echo "$MCP_TEST" | grep -qE "passing|Tests:.*passed"; then
  check "mcp-edge-clinical tests" 0
else
  check "mcp-edge-clinical tests" 1
fi

# Count tools across strategies
TOOL_COUNT=$(find lib/strategies -name "*.js" ! -name "index.js" ! -name "role-policies.js" 2>/dev/null | wc -l)
check "tool implementations (>= 20)" $([ "$TOOL_COUNT" -ge 20 ] && echo 0 || echo 1)

# PHI audit exists
if [ -f "lib/phi-audit.js" ]; then
  check "PHI audit module exists" 0
else
  check "PHI audit module exists" 1
fi

# RBAC role policies exist for all strategies
for strategy in composite high-value full-surface; do
  if [ -f "lib/strategies/$strategy/role-policies.js" ] 2>/dev/null; then
    check "RBAC: $strategy role-policies.js" 0
  else
    check "RBAC: $strategy role-policies.js" 1
  fi
done
echo ""

# ---- Cross-Cutting: Full Clinical Portal Suite ----
echo "--- Cross-Cutting: Clinical Portal Full Suite ---"
cd /mnt/wdblack/dev/projects/hdim-master

echo "  Running full clinical-portal test suite (this takes ~15 min)..."
FULL_TEST=$(npx nx test clinical-portal --run-in-band 2>&1 | grep -E "Test Suites:" | tail -1)
PASSED=$(echo "$FULL_TEST" | grep -oP '\d+ passed' | grep -oP '\d+')
FAILED=$(echo "$FULL_TEST" | grep -oP '\d+ failed' | grep -oP '\d+')
FAILED=${FAILED:-0}
echo "  Results: $PASSED passed, $FAILED failed"
check "clinical-portal suite (>= 178 passing)" $([ "${PASSED:-0}" -ge 178 ] && echo 0 || echo 1)
if [ "$FAILED" -gt 2 ]; then
  check "clinical-portal failures (<= 2 allowed)" 1
else
  check "clinical-portal failures (<= 2 allowed)" 0
fi
echo ""

# ---- Summary ----
echo "============================================"
echo " RESULTS"
echo "============================================"
echo -e " ${GREEN}PASS: $PASS${NC}"
echo -e " ${RED}FAIL: $FAIL${NC}"
echo -e " ${YELLOW}WARN: $WARN${NC}"
echo ""
if [ "$FAIL" -eq 0 ]; then
  echo -e " ${GREEN}v0.3.0 INTEGRATION VALIDATION: PASSED${NC}"
else
  echo -e " ${RED}v0.3.0 INTEGRATION VALIDATION: FAILED ($FAIL issues)${NC}"
fi
echo "============================================"
