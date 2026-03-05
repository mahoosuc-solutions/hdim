#!/usr/bin/env bash
# Release Readiness Check for v2.9.0
# Validates that all documentation and config files are current.
# Usage: bash scripts/release-readiness-check.sh

set -uo pipefail

PASS=0
FAIL=0
ERRORS=()

check() {
  local desc="$1" file="$2" pattern="$3"
  if grep -qP "$pattern" "$file" 2>/dev/null; then
    printf "  PASS  %s\n" "$desc"
    ((PASS++))
  else
    printf "  FAIL  %s\n" "$desc"
    ERRORS+=("$desc ($file)")
    ((FAIL++))
  fi
}

echo "=== v2.9.0 Release Readiness Check ==="
echo ""

# --- docs/README.md (3 assertions) ---
echo "[docs/README.md]"
check "Timestamp updated to March 2026" \
  "docs/README.md" "March [0-9]+, 2026"
check "MCP Edge mentioned in body" \
  "docs/README.md" "MCP Edge"
check "Link to MCP Edge doc" \
  "docs/README.md" "mcp.*edge.*design|MCP_EDGE"

# --- docs/services/SERVICE_CATALOG.md (4 assertions) ---
echo ""
echo "[docs/services/SERVICE_CATALOG.md]"
check "MCP Edge section header" \
  "docs/services/SERVICE_CATALOG.md" "## MCP Edge"
check "mcp-edge-platform with port 3100" \
  "docs/services/SERVICE_CATALOG.md" "mcp-edge-platform.*3100"
check "mcp-edge-devops with port 3200" \
  "docs/services/SERVICE_CATALOG.md" "mcp-edge-devops.*3200"
check "Service count updated to 60" \
  "docs/services/SERVICE_CATALOG.md" "60 microservices"

# --- .github/CODEOWNERS (2 assertions) ---
echo ""
echo "[.github/CODEOWNERS]"
check "MCP Edge path ownership" \
  ".github/CODEOWNERS" "mcp-edge"
check "MCP Edge docker-compose ownership" \
  ".github/CODEOWNERS" "docker-compose\.mcp-edge"

# --- CONTRIBUTING.md (1 assertion) ---
echo ""
echo "[CONTRIBUTING.md]"
check "MCP Edge test command documented" \
  "CONTRIBUTING.md" "test:mcp-edge"

# --- .github/pull_request_template.md (1 assertion) ---
echo ""
echo "[.github/pull_request_template.md]"
check "MCP Edge validation checkbox" \
  ".github/pull_request_template.md" "mcp-edge"

# --- CLAUDE.md (4 assertions) ---
echo ""
echo "[CLAUDE.md]"
check "Timestamp updated to March 5" \
  "CLAUDE.md" "March 5, 2026"
check "MCP Edge port 3100 in ports table" \
  "CLAUDE.md" "3100.*MCP"
check "Version bumped to 4.2" \
  "CLAUDE.md" "Version: 4\.2"
check "v2.9.0 referenced" \
  "CLAUDE.md" "v2\.9\.0"

# --- CHANGELOG.md (1 assertion) ---
echo ""
echo "[CHANGELOG.md]"
check "1307 tests disambiguated as MCP Edge" \
  "CHANGELOG.md" "1,307 MCP Edge"

# --- Summary ---
echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="
if [ ${#ERRORS[@]} -gt 0 ]; then
  echo ""
  echo "Failures:"
  for e in "${ERRORS[@]}"; do
    echo "  - $e"
  done
  exit 1
fi
echo "All assertions passed. Release docs are ready."
exit 0
