#!/bin/bash
# Validate that Kubernetes deploy workflow uses digest-based image deployment.
#
# Rules enforced:
# 1. deploy-docker workflow must set images from repo_digest manifest values.
# 2. deploy-docker workflow must not use VERSION tag for kubectl set image.

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

WORKFLOW_FILE=".github/workflows/deploy-docker.yml"

if [ ! -f "$WORKFLOW_FILE" ]; then
  echo -e "${RED}ERROR: Workflow file not found: ${WORKFLOW_FILE}${NC}"
  exit 1
fi

failures=0

if ! grep -q "repo_digest=\$(jq -r" "$WORKFLOW_FILE"; then
  echo -e "${RED}✗ Missing repo_digest extraction from manifest in ${WORKFLOW_FILE}${NC}"
  failures=$((failures + 1))
else
  echo -e "${GREEN}✓ repo_digest extraction present${NC}"
fi

if ! grep -q 'kubectl set image deployment/\$service \$service=\$repo_digest' "$WORKFLOW_FILE"; then
  echo -e "${RED}✗ Missing digest-based kubectl set image command in ${WORKFLOW_FILE}${NC}"
  failures=$((failures + 1))
else
  echo -e "${GREEN}✓ Digest-based kubectl set image command present${NC}"
fi

if grep -q 'kubectl set image deployment/\$service .*:\$VERSION' "$WORKFLOW_FILE"; then
  echo -e "${RED}✗ Found tag-based kubectl set image command using :\$VERSION in ${WORKFLOW_FILE}${NC}"
  failures=$((failures + 1))
else
  echo -e "${GREEN}✓ No tag-based kubectl set image command found${NC}"
fi

if [ "$failures" -gt 0 ]; then
  echo -e "${RED}Kubernetes digest deploy validation failed (${failures} issue(s)).${NC}"
  exit 1
fi

echo -e "${GREEN}Kubernetes digest deploy validation passed.${NC}"
