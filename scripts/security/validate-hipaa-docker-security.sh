#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${1:-docker-compose.production.yml}"

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker CLI is required" >&2
  exit 1
fi

echo "Validating compose file: ${COMPOSE_FILE}"
docker compose -f "${COMPOSE_FILE}" config >/tmp/compose-security-config.yaml

check_pattern() {
  local pattern="$1"
  local description="$2"
  if rg -q "${pattern}" /tmp/compose-security-config.yaml; then
    echo "PASS: ${description}"
  else
    echo "FAIL: ${description}" >&2
    return 1
  fi
}

check_pattern "no-new-privileges:true" "Containers enforce no-new-privileges"
check_pattern "read_only: true" "Read-only root filesystem configured"
check_pattern "cap_drop:" "Linux capabilities dropped"
if rg -q "target: /secrets" /tmp/compose-security-config.yaml && rg -q "read_only: true" /tmp/compose-security-config.yaml; then
  echo "PASS: Secrets mounted read-only"
else
  echo "FAIL: Secrets mounted read-only" >&2
  exit 1
fi

hardcoded_found=0
while IFS= read -r line; do
  if [[ "$line" != *'${'* ]]; then
    hardcoded_found=1
    echo "Potential hardcoded secret line: ${line}" >&2
  fi
done < <(rg -n "POSTGRES_PASSWORD=|ANTHROPIC_API_KEY=" "${COMPOSE_FILE}" || true)

if [[ "${hardcoded_found}" -eq 1 ]]; then
  echo "FAIL: hardcoded secrets detected in ${COMPOSE_FILE}" >&2
  exit 1
fi

echo "PASS: no obvious hardcoded secret values in ${COMPOSE_FILE}"

echo "HIPAA-oriented container hardening validation complete"
