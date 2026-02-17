#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.demo.yml}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
DEMO_SEEDING_URL="${DEMO_SEEDING_URL:-http://localhost:8098}"
REPORT_DIR="${REPORT_DIR:-logs/mcp-reports}"
RUN_STRICT_GATE="${RUN_STRICT_GATE:-1}"
CURL_CONNECT_TIMEOUT="${CURL_CONNECT_TIMEOUT:-3}"
CURL_MAX_TIME="${CURL_MAX_TIME:-15}"

pass_count=0
warn_count=0
fail_count=0

ok() {
  echo "✅ $*"
  pass_count=$((pass_count + 1))
}

warn() {
  echo "⚠️  $*"
  warn_count=$((warn_count + 1))
}

fail() {
  echo "❌ $*"
  fail_count=$((fail_count + 1))
}

run_check() {
  local label="$1"
  shift
  if "$@" >/tmp/mcp-pretest.out 2>/tmp/mcp-pretest.err; then
    ok "${label}"
  else
    fail "${label}"
    echo "   stdout: $(tr '\n' ' ' </tmp/mcp-pretest.out | sed 's/[[:space:]]\+/ /g' | cut -c1-240)"
    echo "   stderr: $(tr '\n' ' ' </tmp/mcp-pretest.err | sed 's/[[:space:]]\+/ /g' | cut -c1-240)"
  fi
}

run_warn_check() {
  local label="$1"
  shift
  if "$@" >/tmp/mcp-pretest.out 2>/tmp/mcp-pretest.err; then
    ok "${label}"
  else
    warn "${label}"
    echo "   stdout: $(tr '\n' ' ' </tmp/mcp-pretest.out | sed 's/[[:space:]]\+/ /g' | cut -c1-240)"
    echo "   stderr: $(tr '\n' ' ' </tmp/mcp-pretest.err | sed 's/[[:space:]]\+/ /g' | cut -c1-240)"
  fi
}

echo "════════════════════════════════════════════════════════"
echo "MCP PRE-TEST CHECKLIST"
echo "════════════════════════════════════════════════════════"
echo "Repo: ${ROOT_DIR}"
echo "Compose: ${COMPOSE_FILE}"
echo "Gateway: ${GATEWAY_URL}"
echo "Seed URL: ${DEMO_SEEDING_URL}"
echo ""

run_check "Node modules installed (node_modules exists)" test -d node_modules
run_check "MCP test suite passes" npm run test:mcp
run_check "Docker compose status reachable" docker compose -f "${COMPOSE_FILE}" ps
run_check "Gateway health endpoint reachable" curl -fsS --connect-timeout "${CURL_CONNECT_TIMEOUT}" --max-time "${CURL_MAX_TIME}" "${GATEWAY_URL}/actuator/health"
run_check "FHIR metadata endpoint reachable" curl -fsS --connect-timeout "${CURL_CONNECT_TIMEOUT}" --max-time "${CURL_MAX_TIME}" "${GATEWAY_URL}/fhir/metadata"
run_check "Demo seeding health endpoint reachable" curl -fsS --connect-timeout "${CURL_CONNECT_TIMEOUT}" --max-time "${CURL_MAX_TIME}" "${DEMO_SEEDING_URL}/demo/actuator/health"

mkdir -p "${REPORT_DIR}"

run_check \
  "Release gate (permissive) generates artifact" \
  npm run mcp:release-gate -- --mode permissive --no-system-validate --request-timeout 5 --out-dir "${REPORT_DIR}"

if [[ "${RUN_STRICT_GATE}" == "1" ]]; then
  run_warn_check \
    "Release gate (strict) evaluated (warning allowed pre-test)" \
    npm run mcp:release-gate -- --mode strict --no-system-validate --request-timeout 5 --out-dir "${REPORT_DIR}"
fi

run_check \
  "Release evidence pack generated" \
  npm run mcp:evidence-pack -- --report-dir "${REPORT_DIR}" --output-dir "${REPORT_DIR}"

echo ""
echo "════════════════════════════════════════════════════════"
echo "SUMMARY"
echo "════════════════════════════════════════════════════════"
echo "Pass: ${pass_count}"
echo "Warn: ${warn_count}"
echo "Fail: ${fail_count}"

if [[ "${fail_count}" -gt 0 ]]; then
  echo ""
  echo "GO/NO-GO: NO-GO (fix failing checks before MCP live testing)"
  exit 1
fi

if [[ "${warn_count}" -gt 0 ]]; then
  echo ""
  echo "GO/NO-GO: CONDITIONAL GO (review warnings before MCP live testing)"
  exit 0
fi

echo ""
echo "GO/NO-GO: GO (ready for MCP live testing)"
exit 0
