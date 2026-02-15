#!/bin/bash
# Validate AI Sales Operator flow end-to-end in staging containers.
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

CONTAINER_NAME="${AI_SALES_CONTAINER_NAME:-ai-sales-agent-staging}"
BASE_URL="http://127.0.0.1:8090"

echo "Validating AI Sales Operator flow in container: ${CONTAINER_NAME}"

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
  echo -e "${RED}✗ Container not running: ${CONTAINER_NAME}${NC}"
  echo "Start staging stack first:"
  echo "  docker compose -f docker-compose.staging.sales-agents.yml up -d ai-sales-agent"
  exit 1
fi

run_in_container() {
  docker exec "${CONTAINER_NAME}" sh -lc "$1"
}

require_http_code() {
  local actual="$1"
  local expected="$2"
  local label="$3"
  if [ "${actual}" = "${expected}" ]; then
    echo -e "${GREEN}✓ ${label}${NC}"
  else
    echo -e "${RED}✗ ${label} (expected ${expected}, got ${actual})${NC}"
    exit 1
  fi
}

# 1) Health check
HEALTH_CODE="$(run_in_container "curl -s -o /tmp/op_health.json -w '%{http_code}' ${BASE_URL}/health")"
require_http_code "${HEALTH_CODE}" "200" "Health endpoint reachable"

# 2) Operator state available
STATE_CODE="$(run_in_container "curl -s -o /tmp/op_state.json -w '%{http_code}' ${BASE_URL}/api/sales/operator/state")"
require_http_code "${STATE_CODE}" "200" "Operator state endpoint reachable"

# 3) Pending actions available
PENDING_CODE="$(run_in_container "curl -s -o /tmp/op_pending.json -w '%{http_code}' ${BASE_URL}/api/sales/operator/actions/pending")"
require_http_code "${PENDING_CODE}" "200" "Pending actions endpoint reachable"

ACTION_ID="$(run_in_container "python3 - <<'PY'
import json
from pathlib import Path
data = json.loads(Path('/tmp/op_pending.json').read_text() or '{}')
actions = data.get('actions', [])
print(actions[0]['id'] if actions else '')
PY
")"

if [ -z "${ACTION_ID}" ]; then
  echo -e "${YELLOW}! No pending actions found; seeding fallback validation skipped.${NC}"
else
  echo "Using pending action: ${ACTION_ID}"
fi

# 4) Viewer role must be denied
if [ -n "${ACTION_ID}" ]; then
  VIEWER_CODE="$(run_in_container "curl -s -o /tmp/op_viewer_resp.json -w '%{http_code}' -X POST ${BASE_URL}/api/sales/operator/actions/${ACTION_ID}/decision -H 'Content-Type: application/json' -H 'X-Operator-Role: viewer' -d '{\"decision\":\"approved\",\"operator_id\":\"viewer-check\"}'")"
  require_http_code "${VIEWER_CODE}" "403" "Viewer role denied for decision submission"
fi

# 5) Operator role can submit decision
if [ -n "${ACTION_ID}" ]; then
  OPERATOR_CODE="$(run_in_container "curl -s -o /tmp/op_operator_resp.json -w '%{http_code}' -X POST ${BASE_URL}/api/sales/operator/actions/${ACTION_ID}/decision -H 'Content-Type: application/json' -H 'X-Operator-Role: operator' -d '{\"decision\":\"approved\",\"operator_id\":\"operator-check\"}'")"
  require_http_code "${OPERATOR_CODE}" "200" "Operator role can submit decision"
fi

# 6) Decision history endpoint
DECISIONS_CODE="$(run_in_container "curl -s -o /tmp/op_decisions.json -w '%{http_code}' '${BASE_URL}/api/sales/operator/decisions?limit=5'")"
require_http_code "${DECISIONS_CODE}" "200" "Decision history endpoint reachable"

# 7) JSON export endpoint
EXPORT_JSON_CODE="$(run_in_container "curl -s -o /tmp/op_export.json -w '%{http_code}' '${BASE_URL}/api/sales/operator/activity/export?format=json&limit=5'")"
require_http_code "${EXPORT_JSON_CODE}" "200" "JSON export endpoint reachable"

# 8) CSV export endpoint
EXPORT_CSV_CODE="$(run_in_container "curl -s -o /tmp/op_export.csv -w '%{http_code}' '${BASE_URL}/api/sales/operator/activity/export?format=csv&limit=5'")"
require_http_code "${EXPORT_CSV_CODE}" "200" "CSV export endpoint reachable"

CSV_HEADER_OK="$(run_in_container "head -1 /tmp/op_export.csv | tr -d '\r' | grep -c '^action_id,action_title,decision,decided_at,operator$' || true")"
if [ "${CSV_HEADER_OK}" = "1" ]; then
  echo -e "${GREEN}✓ CSV export has expected header${NC}"
else
  echo -e "${RED}✗ CSV export header mismatch${NC}"
  exit 1
fi

echo -e "${GREEN}All AI Sales Operator flow checks passed.${NC}"
