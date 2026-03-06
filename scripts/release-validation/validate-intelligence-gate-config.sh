#!/bin/bash
# Validates required environment variables for intelligence deployment gate checks.
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ENV_NAME="${1:-unknown}"

# Required in all modes (endpoint checks + authz checks)
REQUIRED_VARS=(
  "EVENT_SERVICE_BASE_URL"
)

# Recommended for authz quality checks
RECOMMENDED_VARS=(
  "TENANT_ID"
  "OTHER_TENANT_ID"
  "AUTH_VALIDATED"
  "REVIEWER_ROLES"
  "VIEWER_ROLES"
)

# Required when deep checks are enabled
DEEP_REQUIRED_VARS=()
if [ "${CHECK_MIGRATIONS:-false}" = "true" ]; then
  DEEP_REQUIRED_VARS+=("DATABASE_URL")
fi
if [ "${CHECK_KAFKA_TOPICS:-false}" = "true" ]; then
  DEEP_REQUIRED_VARS+=("KAFKA_BOOTSTRAP_SERVERS")
fi

echo "Intelligence Gate Config Validation (${ENV_NAME})"

overall_status=0

for var_name in "${REQUIRED_VARS[@]}"; do
  if [ -z "${!var_name:-}" ]; then
    echo -e "${RED}✗ Missing required variable: ${var_name}${NC}"
    overall_status=1
  else
    echo -e "${GREEN}✓ ${var_name}${NC}"
  fi
done

for var_name in "${DEEP_REQUIRED_VARS[@]}"; do
  if [ -z "${!var_name:-}" ]; then
    echo -e "${RED}✗ Missing deep-check variable (${var_name}) while deep checks enabled${NC}"
    overall_status=1
  else
    echo -e "${GREEN}✓ ${var_name}${NC}"
  fi
done

for var_name in "${RECOMMENDED_VARS[@]}"; do
  if [ -z "${!var_name:-}" ]; then
    echo -e "${YELLOW}⚠ Recommended variable not set: ${var_name}${NC}"
  else
    echo -e "${GREEN}✓ ${var_name}${NC}"
  fi
done

if [ "${overall_status}" -eq 0 ]; then
  echo -e "${GREEN}Intelligence gate config validation PASSED (${ENV_NAME})${NC}"
else
  echo -e "${RED}Intelligence gate config validation FAILED (${ENV_NAME})${NC}"
fi

exit "${overall_status}"
